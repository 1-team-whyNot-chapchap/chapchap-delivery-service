package com.chapchap.delivery.global.kafka.event;

public record DeliveryEventPublishRequested(
    DeliveryEvent event
) {
}
