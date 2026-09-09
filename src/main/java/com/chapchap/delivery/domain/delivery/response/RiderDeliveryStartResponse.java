package com.chapchap.delivery.domain.delivery.response;

import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import java.time.OffsetDateTime;

public record RiderDeliveryStartResponse(
    String deliveryId
    , DeliveryStatus status
    , Integer deliveryVersion
    , OffsetDateTime startedAt
) {
}
