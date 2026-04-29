package com.customer.service.service;

import com.customer.service.client.CustomerNoResponse;
import com.customer.service.client.CustomerQueryRequest;
import com.customer.service.client.EsSyncClient;
import com.customer.service.dto.CustomerPageResponse;
import com.customer.service.entity.Customer;
import com.customer.service.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final EsSyncClient esSyncClient;
    private final CustomerRepository customerRepository;

    public CustomerService(EsSyncClient esSyncClient, CustomerRepository customerRepository) {
        this.esSyncClient = esSyncClient;
        this.customerRepository = customerRepository;
    }

    /**
     * 多条件组合分页查询：
     * 1. 通过 Feign 调用 es-sync-service 获取客户编号列表
     * 2. 根据客户编号批量查询 MySQL 获取全量数据
     * 3. 组装分页结果返回
     */
    public CustomerPageResponse page(CustomerQueryRequest request) {
        long start = System.currentTimeMillis();

        // 1. 调用 ES 服务获取客户编号分页结果
        CustomerNoResponse esResponse = esSyncClient.page(request);

        List<String> customerNos = esResponse.getCustomerNos();
        if (customerNos == null || customerNos.isEmpty()) {
            log.info("[customer-service] [INFO] ES查询无结果, 耗时:{}ms",
                    System.currentTimeMillis() - start);
            return new CustomerPageResponse(List.of(), esResponse.getTotal(),
                    esResponse.getPage(), esResponse.getSize());
        }

        // 2. 批量查询 MySQL 获取全量数据
        List<Customer> customers = customerRepository.findByCustomerNos(customerNos);

        log.info("[customer-service] [INFO] 查询完成: ES返回{}条, MySQL返回{}条, 耗时:{}ms",
                customerNos.size(), customers.size(), System.currentTimeMillis() - start);

        // 3. 返回分页结果
        return new CustomerPageResponse(customers, esResponse.getTotal(),
                esResponse.getPage(), esResponse.getSize());
    }

    /**
     * 生成测试数据：1000条操作，40%新增、40%更新、20%逻辑删除。
     */
    public Map<String, Object> generateTestData() {
        long start = System.currentTimeMillis();
        int total = 1000;
        int insertCount = (int) (total * 0.4);   // 400
        int updateCount = (int) (total * 0.4);   // 400
        int deleteCount = (int) (total * 0.2);   // 200

        Random rng = new Random();
        LocalDateTime now = LocalDateTime.now();

        // 1. 先获取已有有效客户（在新增之前），用于后续更新和删除
        List<String> existingNos = new ArrayList<>(
                customerRepository.findActiveCustomerNos(updateCount + deleteCount));
        Collections.shuffle(existingNos, rng);

        int actualUpdate = Math.min(updateCount, existingNos.size());
        int actualDelete = Math.min(deleteCount, Math.max(0, existingNos.size() - actualUpdate));

        // 2. 新增（VA + 8位0 + 递增序号，从数据库已有最大值之后开始）
        long maxSeq = customerRepository.getMaxVaSeq();
        for (int i = 0; i < insertCount; i++) {
            Customer c = new Customer();
            c.setCustomerNo(String.format("VA00000000%d", maxSeq + i + 1));
            c.setName("测试用户" + (i + 1));
            c.setPhone("138" + String.format("%08d", rng.nextInt(100_000_000)));
            c.setSource(SOURCES[rng.nextInt(SOURCES.length)]);
            c.setChannel(CHANNELS[rng.nextInt(CHANNELS.length)]);
            c.setBu(BUS[rng.nextInt(BUS.length)]);
            c.setProduct(PRODUCTS[rng.nextInt(PRODUCTS.length)]);
            c.setSalesId((long) (rng.nextInt(100) + 1));
            c.setPlannerId((long) (rng.nextInt(50) + 1));
            c.setStatus(0);
            c.setCreateTime(now);
            c.setUpdateTime(now);
            customerRepository.insert(c);
        }
        log.info("[customer-service] [INFO] 新增完成, count:{}", insertCount);

        // 3. 更新（仅操作已有数据）
        List<String> updateNos = existingNos.subList(0, actualUpdate);
        for (String customerNo : updateNos) {
            Customer c = new Customer();
            c.setCustomerNo(customerNo);
            c.setName("更新用户" + rng.nextInt(10000));
            c.setPhone("139" + String.format("%08d", rng.nextInt(100_000_000)));
            c.setUpdateTime(now);
            customerRepository.updateByCustomerNo(c);
        }
        log.info("[customer-service] [INFO] 更新完成, count:{}", actualUpdate);

        // 4. 逻辑删除
        List<String> deleteNos = existingNos.subList(actualUpdate, actualUpdate + actualDelete);
        for (String customerNo : deleteNos) {
            customerRepository.logicalDeleteByCustomerNo(customerNo);
        }
        log.info("[customer-service] [INFO] 逻辑删除完成, count:{}", actualDelete);

        long elapsed = System.currentTimeMillis() - start;
        log.info("[customer-service] [INFO] 测试数据生成完成, 新增:{}, 更新:{}, 删除:{}, 耗时:{}ms",
                insertCount, actualUpdate, actualDelete, elapsed);

        return Map.of(
                "total", total,
                "inserted", insertCount,
                "updated", actualUpdate,
                "deleted", actualDelete,
                "elapsedMs", elapsed);
    }

    public Customer getByCustomerNo(String customerNo) {
        return customerRepository.findByCustomerNo(customerNo);
    }

    public void updateCustomer(String customerNo, Customer update) {
        Customer existing = customerRepository.findByCustomerNo(customerNo);
        if (existing == null) {
            throw new IllegalArgumentException("客户不存在: " + customerNo);
        }
        update.setCustomerNo(customerNo);
        update.setUpdateTime(java.time.LocalDateTime.now());
        customerRepository.updateByCustomerNo(update);
    }

    private static final String[] SOURCES = {"官网", "APP", "小程序", "电话", "门店"};
    private static final String[] CHANNELS = {"直销", "渠道A", "渠道B", "渠道C"};
    private static final String[] BUS = {"BU1", "BU2", "BU3"};
    private static final String[] PRODUCTS = {"产品A", "产品B", "产品C", "产品D", "产品E"};
}