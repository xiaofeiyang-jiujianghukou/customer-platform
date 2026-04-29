package com.customer.canal.canal;

import com.alibaba.otter.canal.protocol.CanalEntry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KeyFieldFilter {

    /**
     * 判断是否需要同步该行变更（INSERT/UPDATE/DELETE 全量收集）。
     */
    public boolean shouldSync(CanalEntry.RowData rowData, CanalEntry.EventType eventType) {
        return eventType == CanalEntry.EventType.INSERT
                || eventType == CanalEntry.EventType.UPDATE
                || eventType == CanalEntry.EventType.DELETE;
    }

    /**
     * 从行数据中提取指定列的值。
     */
    public static String getColumnValue(List<CanalEntry.Column> columns, String columnName) {
        for (CanalEntry.Column column : columns) {
            if (column.getName().equalsIgnoreCase(columnName)) {
                return column.getValue();
            }
        }
        return null;
    }
}