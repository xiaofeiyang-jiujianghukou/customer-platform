package com.customer.canal.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.customer.canal.batch.CustomerNoCollector;
import com.customer.canal.config.CanalProperties;
import com.customer.canal.model.CustomerSyncMessage;
import com.customer.canal.producer.MQMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class CanalClientRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CanalClientRunner.class);

    private final CanalProperties canalProperties;
    private final KeyFieldFilter keyFieldFilter;
    private final CustomerNoCollector collector;
    private final MQMessageSender mqSender;

    private volatile boolean running = true;

    public CanalClientRunner(CanalProperties canalProperties,
                             KeyFieldFilter keyFieldFilter,
                             CustomerNoCollector collector,
                             MQMessageSender mqSender) {
        this.canalProperties = canalProperties;
        this.keyFieldFilter = keyFieldFilter;
        this.collector = collector;
        this.mqSender = mqSender;
    }

    @Override
    public void run(String... args) {
        log.info("[canal-sync-service] [INFO] Canal同步服务启动中...");

        CanalConnector connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(canalProperties.getHost(), canalProperties.getPort()),
                canalProperties.getDestination(),
                canalProperties.getUsername(),
                canalProperties.getPassword());

        try {
            connector.connect();
            connector.subscribe(canalProperties.getSubscribe());
            connector.rollback();
            log.info("[canal-sync-service] [INFO] Canal连接成功, destination:{}, subscribe:{}",
                    canalProperties.getDestination(), canalProperties.getSubscribe());

            while (running) {
                try {
                    Message message = connector.getWithoutAck(canalProperties.getBatchSize());
                    long batchId = message.getId();
                    if (batchId == -1 || message.getEntries().isEmpty()) {
                        // 空批次也 flush（时间窗口可能到期）
                        flushAndSend();
                        sleep(100);
                        continue;
                    }

                    int filtered = 0;
                    int synced = 0;
                    for (CanalEntry.Entry entry : message.getEntries()) {
                        // 只处理 ROWDATA 类型的 entry
                        if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                            continue;
                        }

                        CanalEntry.RowChange rowChange;
                        try {
                            rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                        } catch (Exception e) {
                            continue;
                        }

                        CanalEntry.EventType eventType = rowChange.getEventType();
                        log.debug("[canal-sync-service] [batchId:{}] ROWDATA: schema={}, table={}, eventType={}, rows={}",
                                batchId, entry.getHeader().getSchemaName(),
                                entry.getHeader().getTableName(),
                                eventType, rowChange.getRowDatasCount());
                        for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                            if (!keyFieldFilter.shouldSync(rowData, eventType)) {
                                filtered++;
                                continue;
                            }
                            String customerNo = extractCustomerNo(rowData, eventType);
                            if (customerNo == null) {
                                continue;
                            }
                            List<CustomerSyncMessage.CustomerChange> batch = collector.add(
                                    customerNo, eventType.name());
                            synced++;
                            if (batch != null) {
                                mqSender.send(new CustomerSyncMessage(batch, System.currentTimeMillis()));
                            }
                        }
                    }

                    connector.ack(batchId);
                    log.info("[canal-sync-service] [INFO] [batchId:{}] 处理完成: {}条binlog, 同步{}条, 过滤{}条",
                            batchId, message.getEntries().size(), synced, filtered);

                } catch (Exception e) {
                    log.error("[canal-sync-service] [ERROR] Canal消费异常，休眠5秒后重试", e);
                    sleep(5000);
                }
            }
        } finally {
            connector.disconnect();
            log.info("[canal-sync-service] [INFO] Canal连接已断开");
        }
    }

    private void flushAndSend() {
        List<CustomerSyncMessage.CustomerChange> batch = collector.flush();
        if (batch != null) {
            mqSender.send(new CustomerSyncMessage(batch, System.currentTimeMillis()));
        }
    }

    private String extractCustomerNo(CanalEntry.RowData rowData, CanalEntry.EventType eventType) {
        // INSERT/UPDATE 从 afterColumns 取，DELETE 从 beforeColumns 取
        List<CanalEntry.Column> columns = (eventType == CanalEntry.EventType.DELETE)
                ? rowData.getBeforeColumnsList()
                : rowData.getAfterColumnsList();
        return KeyFieldFilter.getColumnValue(columns, "customer_no");
    }

    private void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}