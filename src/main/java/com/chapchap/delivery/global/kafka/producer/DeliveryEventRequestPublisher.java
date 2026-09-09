package com.chapchap.delivery.global.kafka.producer;

import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.constant.DeliveryRefundReason;
import com.chapchap.delivery.global.kafka.event.DeliveryEvent;
import com.chapchap.delivery.global.kafka.event.DeliveryEventData;
import com.chapchap.delivery.global.kafka.event.DeliveryEventPublishRequested;
import com.chapchap.delivery.global.kafka.event.DeliveryRefundConfirmedData;
import com.chapchap.delivery.global.kafka.event.DeliveryRefundConfirmedEvent;
import com.chapchap.delivery.global.kafka.event.DeliveryRefundPublishRequested;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventRequestPublisher {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int SCHEMA_VERSION = 1;

    private final ApplicationEventPublisher eventPublisher;

    public DeliveryEventRequestPublisher(
        ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = eventPublisher;
    }

    public void publishStateChanged(
        String eventType
        , Delivery delivery
        , LocalDateTime occurredAt
    ) {
        publish(
            eventType
            , delivery
            , occurredAt
            , delivery.getDeliveryVersion()
        );
    }

    public void publishDelayed(
        Delivery delivery
        , LocalDateTime occurredAt
    ) {
        publish(
            "DELIVERY_DELAYED"
            , delivery
            , occurredAt
            , null
        );
    }

    public void publishRefundConfirmed(
        Delivery delivery
        , DeliveryRefundReason reasonCode
        , LocalDateTime confirmedAt
    ) {
        var occurredAt = confirmedAt.atZone(KST).toOffsetDateTime();
        eventPublisher.publishEvent(new DeliveryRefundPublishRequested(
            new DeliveryRefundConfirmedEvent(
                UUID.randomUUID().toString(), "DELIVERY_REFUND_CONFIRMED",
                SCHEMA_VERSION, occurredAt, delivery.getCustomerId(),
                new DeliveryRefundConfirmedData(
                    delivery.getDeliveryPublicId(), delivery.getSourceOrderId(),
                    occurredAt, reasonCode.name()
                )
            )
        ));
    }

    private void publish(
        String eventType
        , Delivery delivery
        , LocalDateTime occurredAt
        , Integer deliveryVersion
    ) {
        DeliveryEvent event = new DeliveryEvent(
            UUID.randomUUID().toString()
            , eventType
            , SCHEMA_VERSION
            , occurredAt.atZone(KST).toOffsetDateTime()
            , delivery.getCustomerId()
            , new DeliveryEventData(
                delivery.getDeliveryPublicId()
                , deliveryVersion
            )
        );

        eventPublisher.publishEvent(
            new DeliveryEventPublishRequested(event)
        );
    }
}
