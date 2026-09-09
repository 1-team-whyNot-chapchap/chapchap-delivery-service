package com.chapchap.delivery.domain.delivery.response;

import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;

import java.time.OffsetDateTime;

public record RiderDeliveryFailureResponse(
    String deliveryId
    , DeliveryStatus status
    , Integer deliveryVersion
    , DeliveryFailureCode failureCode
    , OffsetDateTime failedAt
) {
}