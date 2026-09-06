package com.chapchap.delivery.domain.delivery.response;

import com.chapchap.delivery.domain.delivery.constant.IntegrationEventStatus;
import java.time.OffsetDateTime;

public record AdminIntegrationEventRepublishResponse(
    Long integrationEventRecordId
    , String eventId
    , IntegrationEventStatus status
    , Integer attemptCount
    , OffsetDateTime lastAttemptedAt
) {
}
