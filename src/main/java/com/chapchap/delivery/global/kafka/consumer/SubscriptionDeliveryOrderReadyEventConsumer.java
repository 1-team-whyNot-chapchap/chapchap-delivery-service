package com.chapchap.delivery.global.kafka.consumer;

import com.chapchap.delivery.domain.delivery.service.DeliveryRegistrationService;
import com.chapchap.delivery.global.kafka.event.SubscriptionDeliveryOrderReadyEvent;
import com.chapchap.delivery.global.kafka.validator.SubscriptionDeliveryOrderReadyEventValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionDeliveryOrderReadyEventConsumer {

    private final SubscriptionDeliveryOrderReadyEventValidator validator;
    private final DeliveryRegistrationService deliveryRegistrationService;

    @KafkaListener(
        topics = "${kafka.topic.subscription-delivery-orders}"
        , groupId = "${kafka.consumer-group.subscription-delivery-orders}"
    )
    public void handleSubscriptionDeliveryOrderReady(
        SubscriptionDeliveryOrderReadyEvent event
        , @Header(KafkaHeaders.RECEIVED_KEY) String messageKey
    ) {
        if (!validator.supports(event)) {
            return;
        }

        validator.validate(messageKey, event);

        deliveryRegistrationService.register(event);
    }
}