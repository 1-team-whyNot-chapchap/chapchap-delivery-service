package com.chapchap.delivery.domain.riderlocation.event;

import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;

public record DeliveryTrackingEndedEvent(Long deliveryId, String deliveryPublicId, DeliveryStatus status) {
}
