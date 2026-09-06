package com.chapchap.delivery.global.kafka.event;

import java.time.OffsetDateTime;

public record DeliveryEvent(
    String eventId
    , String eventType
    , int version
    , OffsetDateTime occurredAt
    , Long userId
    , DeliveryEventData data
) {
}
