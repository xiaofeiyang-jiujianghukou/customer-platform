package com.customer.canal.producer;

import com.alibaba.fastjson.JSON;
import com.customer.canal.config.RocketMQConfig;
import com.customer.canal.model.CustomerSyncMessage;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Component
public class MQMessageSender {

    private static final Logger log = LoggerFactory.getLogger(MQMessageSender.class);

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    private DefaultMQProducer producer;

    @PostConstruct
    public void init() throws MQClientException {
        log.info("[canal-sync-service] [INFO] 初始化RocketMQ Producer, nameServer:{}, group:{}",
                nameServer, producerGroup);
        producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(10000);
        producer.setRetryTimesWhenSendFailed(3);
        producer.setVipChannelEnabled(false);
        producer.start();
        log.info("[canal-sync-service] [INFO] RocketMQ Producer启动成功");
    }

    @PreDestroy
    public void destroy() {
        if (producer != null) {
            producer.shutdown();
        }
    }

    public void send(CustomerSyncMessage message) {
        long start = System.currentTimeMillis();
        try {
            String body = JSON.toJSONString(message);
            Message msg = new Message(
                    RocketMQConfig.TOPIC_CUSTOMER_SYNC,
                    body.getBytes(StandardCharsets.UTF_8));
            SendResult result = producer.send(msg);
            log.info("[canal-sync-service] [INFO] MQ发送成功: {}条变更, msgId:{}, 耗时:{}ms",
                    message.getChanges().size(), result.getMsgId(),
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[canal-sync-service] [ERROR] MQ发送失败: {}条变更, 耗时:{}ms",
                    message.getChanges().size(), System.currentTimeMillis() - start, e);
            throw new RuntimeException(e);
        }
    }
}