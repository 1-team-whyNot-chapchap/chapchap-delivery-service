package com.chapchap.delivery.domain.delivery.response;

import com.chapchap.delivery.domain.delivery.constant.IntegrationEventDirection;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventStatus;

import java.time.OffsetDateTime;

public record AdminIntegrationEventItemResponse(
    Long integrationEventRecordId
    , String eventId
    , IntegrationEventDirection direction
    , IntegrationEventStatus status
    , String eventType
    , String aggregateType
    , String aggregateId
    , String topic
    , String eventKey
    , Integer attemptCount
    , OffsetDateTime lastAttemptedAt
    , String errorCode
    , String errorMessage
) {
}
