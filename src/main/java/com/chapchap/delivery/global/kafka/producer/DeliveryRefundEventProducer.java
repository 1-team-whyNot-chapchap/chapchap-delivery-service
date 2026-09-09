package com.chapchap.delivery.global.kafka.producer;

import com.chapchap.delivery.global.kafka.event.DeliveryRefundConfirmedEvent;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class DeliveryRefundEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public DeliveryRefundEventProducer(
        KafkaTemplate<String, Object> kafkaTemplate
        , @Value("${app.kafka.topics.delivery-refund-event}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public CompletableFuture<SendResult<String, Object>> send(DeliveryRefundConfirmedEvent event) {
        return kafkaTemplate.send(topic, event.data().deliveryId(), event);
    }

    public String topic() { return topic; }
}
