package com.chapchap.delivery.domain.delivery.response;

import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import java.time.OffsetDateTime;

public record RiderDeliveryCompletionResponse(
    String deliveryId
    , DeliveryStatus status
    , Integer deliveryVersion
    , ActualHandoffType actualHandoffType
    , OffsetDateTime completedAt
    , boolean hasCompletionPhoto
    , boolean delayed
) {
}
