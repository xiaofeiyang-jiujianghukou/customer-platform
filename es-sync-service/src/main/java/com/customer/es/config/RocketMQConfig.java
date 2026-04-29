package com.customer.es.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RocketMQConfig {

    public static final String TOPIC_CUSTOMER_SYNC = "customer_sync_topic";

    public static final String CONSUMER_GROUP = "es_sync_consumer_group";
}