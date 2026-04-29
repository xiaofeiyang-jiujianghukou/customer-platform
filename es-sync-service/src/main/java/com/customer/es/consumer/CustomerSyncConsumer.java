package com.customer.es.consumer;

import com.customer.es.model.CustomerSyncMessage;
import com.customer.es.service.EsSyncService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = "customer_sync_topic",
        consumerGroup = "es_sync_consumer_group"
)
public class CustomerSyncConsumer implements RocketMQListener<CustomerSyncMessage> {

    private static final Logger log = LoggerFactory.getLogger(CustomerSyncConsumer.class);

    private final EsSyncService esSyncService;

    public CustomerSyncConsumer(EsSyncService esSyncService) {
        this.esSyncService = esSyncService;
    }

    @Override
    public void onMessage(CustomerSyncMessage message) {
        log.info("[es-sync-service] [INFO] 收到MQ消息: {}条变更", message.getChanges().size());
        esSyncService.syncFromMessage(message);
    }
}