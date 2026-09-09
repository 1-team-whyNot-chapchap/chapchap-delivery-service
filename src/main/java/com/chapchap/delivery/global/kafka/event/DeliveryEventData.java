package com.chapchap.delivery.global.kafka.event;

public record DeliveryEventData(
    String deliveryId
    , Integer deliveryVersion
) {
}
