package com.customer.es.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.customer.es.config.ElasticsearchProperties;
import com.customer.es.dto.CustomerNoResponse;
import com.customer.es.dto.CustomerQueryRequest;
import com.customer.es.entity.Customer;
import com.customer.es.model.CustomerSyncMessage;
import com.customer.es.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EsSyncService {

    private static final Logger log = LoggerFactory.getLogger(EsSyncService.class);

    private final ElasticsearchClient esClient;
    private final ElasticsearchProperties esProperties;
    private final CustomerRepository customerRepository;

    public EsSyncService(ElasticsearchClient esClient,
                         ElasticsearchProperties esProperties,
                         CustomerRepository customerRepository) {
        this.esClient = esClient;
        this.esProperties = esProperties;
        this.customerRepository = customerRepository;
    }

    /**
     * 消费一条MQ消息，从MySQL读取全量数据并写入ES。
     * 物理 DELETE → 直接从 ES 删除
     * INSERT / UPDATE → 读 MySQL 全量数据后，根据 status 判断：
     *   - status=1（逻辑删除）→ 从 ES 删除
     *   - status=0（有效）    → upsert 写入 ES
     */
    public void syncFromMessage(CustomerSyncMessage message) {
        List<CustomerSyncMessage.CustomerChange> changes = message.getChanges();

        // 物理删除：直接删 ES
        List<String> physicalDeleteNos = changes.stream()
                .filter(c -> "DELETE".equalsIgnoreCase(c.getEventType()))
                .map(CustomerSyncMessage.CustomerChange::getCustomerNo)
                .distinct()
                .collect(Collectors.toList());

        // 非删除事件：读 MySQL 后根据 status 判断
        List<String> queryNos = changes.stream()
                .filter(c -> !"DELETE".equalsIgnoreCase(c.getEventType()))
                .map(CustomerSyncMessage.CustomerChange::getCustomerNo)
                .distinct()
                .collect(Collectors.toList());

        long start = System.currentTimeMillis();
        List<BulkOperation> operations = new ArrayList<>();
        int upsertCount = 0;
        int logicDeleteCount = 0;

        // 1. INSERT / UPDATE → 读 MySQL，按 status 分类处理
        if (!queryNos.isEmpty()) {
            List<Customer> customers = customerRepository.findByCustomerNos(queryNos);
            for (Customer customer : customers) {
                if (customer.getStatus() != null && customer.getStatus() == 1) {
                    // 逻辑删除：从 ES 删
                    operations.add(BulkOperation.of(b -> b
                            .delete(del -> del
                                    .index(esProperties.getIndex())
                                    .id(customer.getCustomerNo()))));
                    logicDeleteCount++;
                } else {
                    // 有效数据：upsert 到 ES
                    operations.add(BulkOperation.of(b -> b
                            .update(upd -> upd
                                    .index(esProperties.getIndex())
                                    .id(customer.getCustomerNo())
                                    .action(a -> a
                                            .docAsUpsert(true)
                                            .doc(toEsDocument(customer))))));
                    upsertCount++;
                }
            }
        }

        // 2. 物理 DELETE
        for (String customerNo : physicalDeleteNos) {
            operations.add(BulkOperation.of(b -> b
                    .delete(del -> del
                            .index(esProperties.getIndex())
                            .id(customerNo))));
        }

        if (operations.isEmpty()) {
            return;
        }

        try {
            BulkResponse response = esClient.bulk(b -> b.operations(operations));
            if (response.errors()) {
                log.error("[es-sync-service] [ERROR] ES批量操作有错误: {}", response.items());
            }
            log.info("[es-sync-service] [INFO] ES批量操作完成: upsert={} 逻辑删除={} 物理删除={}, 耗时:{}ms",
                    upsertCount, logicDeleteCount, physicalDeleteNos.size(),
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[es-sync-service] [ERROR] ES批量操作失败", e);
        }
    }

    /**
     * 多条件组合分页查询，仅返回客户编号列表。
     */
    public CustomerNoResponse search(CustomerQueryRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        addTerm(boolBuilder, "customerNo", request.getCustomerNo());
        addTerm(boolBuilder, "status", request.getStatus());
        addTerm(boolBuilder, "salesId", request.getSalesId());
        addTerm(boolBuilder, "plannerId", request.getPlannerId());
        addTerm(boolBuilder, "source", request.getSource());
        addTerm(boolBuilder, "channel", request.getChannel());
        addTerm(boolBuilder, "bu", request.getBu());
        addTerm(boolBuilder, "product", request.getProduct());

        addMatch(boolBuilder, "name", request.getName());
        addMatch(boolBuilder, "phone", request.getPhone());

        addRange(boolBuilder, "id", request.getMinId(), request.getMaxId());
        addDateRange(boolBuilder, "createTime", request.getStartTime(), request.getEndTime());

        int from = (request.getPage() - 1) * request.getSize();
        SortOrder sortOrder = "asc".equalsIgnoreCase(request.getSortOrder())
                ? SortOrder.Asc : SortOrder.Desc;

        try {
            SearchResponse<Map> response = esClient.search(s -> s
                    .index(esProperties.getIndex())
                    .query(q -> q.bool(boolBuilder.build()))
                    .from(from)
                    .size(request.getSize())
                    .sort(sort -> sort.field(f -> f
                            .field(request.getSortField())
                            .order(sortOrder)))
                    .source(src -> src.filter(f -> f.includes("customerNo"))),
                    Map.class);

            List<String> customerNos = response.hits().hits().stream()
                    .map(Hit::source)
                    .map(source -> (String) source.get("customerNo"))
                    .collect(Collectors.toList());

            long total = response.hits().total() != null ? response.hits().total().value() : 0;

            return new CustomerNoResponse(customerNos, total, request.getPage(), request.getSize());

        } catch (Exception e) {
            log.error("[es-sync-service] [ERROR] ES查询失败", e);
            throw new RuntimeException("ES查询失败", e);
        }
    }

    private void addTerm(BoolQuery.Builder builder, String field, Object value) {
        if (value != null && !"".equals(value)) {
            builder.filter(Query.of(q -> q.term(t -> t.field(field).value(v -> v.stringValue(String.valueOf(value))))));
        }
    }

    private void addMatch(BoolQuery.Builder builder, String field, String value) {
        if (value != null && !value.isEmpty()) {
            builder.must(Query.of(q -> q.match(m -> m.field(field).query(value))));
        }
    }

    private void addRange(BoolQuery.Builder builder, String field, Object min, Object max) {
        if (min != null || max != null) {
            RangeQuery.Builder range = new RangeQuery.Builder().field(field);
            if (min != null) range.gte(JsonData.of(min));
            if (max != null) range.lte(JsonData.of(max));
            builder.filter(Query.of(q -> q.range(range.build())));
        }
    }

    private void addDateRange(BoolQuery.Builder builder, String field, String start, String end) {
        if (start != null || end != null) {
            RangeQuery.Builder range = new RangeQuery.Builder().field(field);
            if (start != null) range.gte(JsonData.of(start));
            if (end != null) range.lte(JsonData.of(end));
            builder.filter(Query.of(q -> q.range(range.build())));
        }
    }

    private Map<String, Object> toEsDocument(Customer customer) {
        Map<String, Object> doc = new java.util.LinkedHashMap<>();
        doc.put("id", customer.getId());
        doc.put("customerNo", customer.getCustomerNo());
        doc.put("name", customer.getName() != null ? customer.getName() : "");
        doc.put("phone", customer.getPhone() != null ? customer.getPhone() : "");
        doc.put("source", customer.getSource() != null ? customer.getSource() : "");
        doc.put("channel", customer.getChannel() != null ? customer.getChannel() : "");
        doc.put("bu", customer.getBu() != null ? customer.getBu() : "");
        doc.put("product", customer.getProduct() != null ? customer.getProduct() : "");
        doc.put("salesId", customer.getSalesId());
        doc.put("plannerId", customer.getPlannerId());
        doc.put("status", customer.getStatus());
        doc.put("createTime", customer.getCreateTime() != null ? customer.getCreateTime().toString() : null);
        doc.put("updateTime", customer.getUpdateTime() != null ? customer.getUpdateTime().toString() : null);
        return doc;
    }
}