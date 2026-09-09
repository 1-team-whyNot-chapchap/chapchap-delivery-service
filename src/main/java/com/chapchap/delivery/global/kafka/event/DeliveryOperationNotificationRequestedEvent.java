package com.chapchap.delivery.global.kafka.event;

import java.time.OffsetDateTime;

public record DeliveryOperationNotificationRequestedEvent(
    String eventId
    , String eventType
    , Integer version
    , OffsetDateTime occurredAt
    , Long userId
    , DeliveryOperationNotificationData data
) {
}
