package com.customer.canal.model;

import java.io.Serializable;
import java.util.List;

public class CustomerSyncMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 变更明细列表 */
    private List<CustomerChange> changes;

    /** 消息时间戳 */
    private long timestamp;

    public CustomerSyncMessage() {
    }

    public CustomerSyncMessage(List<CustomerChange> changes, long timestamp) {
        this.changes = changes;
        this.timestamp = timestamp;
    }

    public List<CustomerChange> getChanges() { return changes; }
    public void setChanges(List<CustomerChange> changes) { this.changes = changes; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "CustomerSyncMessage{" +
                "changes=" + changes +
                ", timestamp=" + timestamp +
                '}';
    }

    public static class CustomerChange implements Serializable {

        private static final long serialVersionUID = 1L;

        private String customerNo;
        private String eventType; // INSERT / UPDATE / DELETE

        public CustomerChange() {
        }

        public CustomerChange(String customerNo, String eventType) {
            this.customerNo = customerNo;
            this.eventType = eventType;
        }

        public String getCustomerNo() { return customerNo; }
        public void setCustomerNo(String customerNo) { this.customerNo = customerNo; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        @Override
        public String toString() {
            return "{" + customerNo + ":" + eventType + "}";
        }
    }
}