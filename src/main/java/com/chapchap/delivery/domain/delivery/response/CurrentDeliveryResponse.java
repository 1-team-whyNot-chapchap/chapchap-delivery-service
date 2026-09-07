package com.chapchap.delivery.domain.delivery.response;

import com.chapchap.delivery.domain.delivery.constant.CurrentDeliveryDelayStatus;
import com.chapchap.delivery.domain.delivery.constant.CurrentDeliveryStatus;
import java.time.OffsetDateTime;

public record CurrentDeliveryResponse(
    CurrentDeliveryStatus status
    , CurrentDeliveryDelayStatus delayStatus
    , OffsetDateTime statusChangedAt
) {
}
