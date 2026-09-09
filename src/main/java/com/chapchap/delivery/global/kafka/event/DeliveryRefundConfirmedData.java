package com.chapchap.delivery.global.kafka.event;

import java.time.OffsetDateTime;

public record DeliveryRefundConfirmedData(
    String deliveryId
    , String orderId
    , OffsetDateTime confirmedAt
    , String reasonCode
) {
}
