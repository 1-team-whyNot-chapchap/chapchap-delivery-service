package com.chapchap.delivery.global.kafka.producer;

import com.chapchap.delivery.global.kafka.event.DeliveryOperationNotificationRequestedEvent;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class DeliveryOperationNotificationRequestedEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public DeliveryOperationNotificationRequestedEventProducer(
        KafkaTemplate<String, Object> kafkaTemplate
        , @Value("${app.kafka.topics.operation-notification}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public CompletableFuture<SendResult<String, Object>> send(
        Long recipientUserId
        , DeliveryOperationNotificationRequestedEvent event
    ) {
        return kafkaTemplate.send(topic, String.valueOf(recipientUserId), event);
    }

    public CompletableFuture<SendResult<String, Object>> sendToAdmin(
        DeliveryOperationNotificationRequestedEvent event
    ) {
        return kafkaTemplate.send(topic, "ADMIN", event);
    }
}
