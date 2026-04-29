package com.customer.canal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sync.batch")
public class SyncBatchProperties {

    /**
     * 批量大小，达到后立即发送。
     * canal 每批约 6 条，1000ms 窗收集约 148-171 条。设为 155，
     * 快速到达时 COUNT 先触发（~940ms），慢速时 TIME 先到期（1000ms），两者自然交替。
     */
    private int size = 155;

    /** 时间窗口（毫秒），即使未达到批量大小也发送 */
    private long intervalMs = 1000;

    /** 去重窗口（毫秒），与时间窗口同步 */
    private long dedupWindowMs = 1000;

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getIntervalMs() { return intervalMs; }
    public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
    public long getDedupWindowMs() { return dedupWindowMs; }
    public void setDedupWindowMs(long dedupWindowMs) { this.dedupWindowMs = dedupWindowMs; }
}
