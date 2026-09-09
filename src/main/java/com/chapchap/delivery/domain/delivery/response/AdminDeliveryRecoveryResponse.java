package com.chapchap.delivery.domain.delivery.response;

import com.chapchap.delivery.domain.delivery.constant.DeliveryRecoveryResult;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import java.time.OffsetDateTime;

public record AdminDeliveryRecoveryResponse(
    String deliveryId
    , DeliveryStatus status
    , Integer deliveryVersion
    , DeliveryRecoveryResult recoveryResult
    , Long actualRiderId
    , OffsetDateTime recoveredAt
) {
}
