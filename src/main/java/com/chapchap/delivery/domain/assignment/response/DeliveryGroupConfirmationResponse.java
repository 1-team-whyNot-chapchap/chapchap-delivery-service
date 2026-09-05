package com.chapchap.delivery.domain.assignment.response;

import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;

import java.time.OffsetDateTime;

public record DeliveryGroupConfirmationResponse(
    Long deliveryGroupId
    , DeliveryGroupStatus status
    , OffsetDateTime confirmedAt
    , Long confirmedBy
) {
}
