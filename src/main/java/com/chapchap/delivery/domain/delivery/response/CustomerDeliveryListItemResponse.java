package com.chapchap.delivery.domain.delivery.response;

import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CustomerDeliveryListItemResponse(
    String deliveryId
    , String orderId
    , LocalDate deliveryDate
    , DeliverySlotCode deliverySlot
    , DeliveryStatus status
    , boolean isDelayed
    , OffsetDateTime completedAt
    , ActualHandoffType actualHandoffType
    , boolean hasCompletionPhoto
) {
}
