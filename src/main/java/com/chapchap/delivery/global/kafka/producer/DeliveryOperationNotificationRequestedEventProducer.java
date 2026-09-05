package com.chapchap.delivery.global.kafka.producer;

import com.chapchap.delivery.global.kafka.event.DeliveryOperationNotificationRequestedEvent;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryOperationNotificationRequestedEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>> send(
        Long recipientUserId
        , DeliveryOperationNotificationRequestedEvent event
    ) {
        return kafkaTemplate.send(
            "delivery.operation.notification.requested.v1"
            , String.valueOf(recipientUserId)
            , event
        );
    }
}
