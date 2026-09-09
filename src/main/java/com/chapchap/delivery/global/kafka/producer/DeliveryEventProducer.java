package com.chapchap.delivery.global.kafka.producer;

import com.chapchap.delivery.global.kafka.event.DeliveryEvent;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public DeliveryEventProducer(
        KafkaTemplate<String, Object> kafkaTemplate
        , @Value("${app.kafka.topics.delivery-event}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public String topic() {
        return topic;
    }

    public CompletableFuture<SendResult<String, Object>> send(
        DeliveryEvent event
    ) {
        return kafkaTemplate.send(
            topic
            , event.data().deliveryId()
            , event
        );
    }
}
