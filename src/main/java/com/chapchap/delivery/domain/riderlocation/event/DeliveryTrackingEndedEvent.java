package com.chapchap.delivery.domain.riderlocation.event;

public record DeliveryTrackingEndedEvent(Long deliveryId, String deliveryPublicId) {
}
