package com.chapchap.delivery.global.kafka.consumer;

import com.chapchap.delivery.domain.delivery.service.DeliveryRegistrationService;
import com.chapchap.delivery.domain.delivery.service.IntegrationEventIgnoreService;
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
    private final IntegrationEventIgnoreService ignoreService;

    @KafkaListener(
        topics = "${kafka.topic.subscription-delivery-orders}"
        , groupId = "${kafka.consumer-group.subscription-delivery-orders}"
        , properties = "spring.json.value.default.type=com.chapchap.delivery.global.kafka.event.SubscriptionDeliveryOrderReadyEvent"
    )
    public void handleSubscriptionDeliveryOrderReady(
        SubscriptionDeliveryOrderReadyEvent event
        , @Header(KafkaHeaders.RECEIVED_KEY) String messageKey
    ) {
        validator.validate(messageKey, event);

        if (!validator.supports(event)) {
            ignoreService.ignore(
                event.eventId()
                , event.eventType()
                , "ORDER"
                , event.data().orderId()
                , event.occurredAt()
            );
            return;
        }

        deliveryRegistrationService.register(event);
    }
}
