package com.customer.canal.batch;

import com.customer.canal.config.SyncBatchProperties;
import com.customer.canal.model.CustomerSyncMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class CustomerNoCollector {

    private static final Logger log = LoggerFactory.getLogger(CustomerNoCollector.class);

    private final SyncBatchProperties properties;
    private final ReentrantLock lock = new ReentrantLock();

    /** 当前窗口收集的客户 → 事件类型（后覆盖前，同一窗口内以最后一次为准） */
    private final Map<String, String> customerEventMap = new LinkedHashMap<>();

    /** 当前窗口开始时间 */
    private long windowStartTime = System.currentTimeMillis();

    /** 本窗口接收到的总事件数（去重前），用于观测 */
    private int rawEventsInWindow = 0;

    public CustomerNoCollector(SyncBatchProperties properties) {
        this.properties = properties;
    }

    /**
     * 添加一个客户变更事件。
     */
    public List<CustomerSyncMessage.CustomerChange> add(String customerNo, String eventType) {
        lock.lock();
        try {
            customerEventMap.put(customerNo, eventType);
            rawEventsInWindow++;
            long now = System.currentTimeMillis();
            long elapsed = now - windowStartTime;

            // 数量触发：达到批量上限
            if (customerEventMap.size() >= properties.getSize()) {
                log.info("[canal-sync-service] [FLUSH-COUNT] 数量达到 {} 条 (去重后), 窗口 {}/{}ms, 接收 {} 条(去重前)",
                        customerEventMap.size(), elapsed, properties.getIntervalMs(), rawEventsInWindow);
                return doFlush();
            }

            // 时间触发：窗口到期
            if (elapsed >= properties.getIntervalMs()) {
                if (!customerEventMap.isEmpty()) {
                    log.info("[canal-sync-service] [FLUSH-TIME] 窗口到期 {}/{}ms, 收集 {} 条(去重后), 接收 {} 条(去重前)",
                            elapsed, properties.getIntervalMs(), customerEventMap.size(), rawEventsInWindow);
                    return doFlush();
                }
                windowStartTime = now;
                rawEventsInWindow = 0;
            }

            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 强制 flush 当前窗口。无论是否有数据，都重置窗口——避免空闲期间窗口持续膨胀。
     */
    public List<CustomerSyncMessage.CustomerChange> flush() {
        lock.lock();
        try {
            if (customerEventMap.isEmpty()) {
                // 空闲时重置窗口，防止下次事件到来时窗口变成 8000+ms
                windowStartTime = System.currentTimeMillis();
                rawEventsInWindow = 0;
                return null;
            }
            long elapsed = System.currentTimeMillis() - windowStartTime;
            log.info("[canal-sync-service] [FLUSH-FORCE] 剩余 {} 条(去重后), 窗口 {}/{}ms, 接收 {} 条(去重前)",
                    customerEventMap.size(), elapsed, properties.getIntervalMs(), rawEventsInWindow);
            return doFlush();
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return customerEventMap.size();
        } finally {
            lock.unlock();
        }
    }

    private List<CustomerSyncMessage.CustomerChange> doFlush() {
        List<CustomerSyncMessage.CustomerChange> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : customerEventMap.entrySet()) {
            result.add(new CustomerSyncMessage.CustomerChange(entry.getKey(), entry.getValue()));
        }
        customerEventMap.clear();
        windowStartTime = System.currentTimeMillis();
        rawEventsInWindow = 0;
        return result;
    }
}