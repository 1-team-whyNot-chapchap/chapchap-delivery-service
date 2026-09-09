package com.chapchap.delivery.domain.delivery.response;

import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.RequestHandoffType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CustomerDeliveryDetailResponse(
    String deliveryId
    , String orderId
    , LocalDate deliveryDate
    , DeliverySlotCode deliverySlot
    , DeliveryStatus status
    , boolean isDelayed
    , Integer delayMinutes
    , RequestHandoffType requestedHandoffType
    , ActualHandoffType actualHandoffType
    , OffsetDateTime completedAt
    , String storageLocation
    , String customerFailureMessage
    , boolean hasCompletionPhoto
    , Menu menu
) {
    public record Menu(
        String menuId
        , String menuName
        , Integer quantity
    ) {
    }
}
