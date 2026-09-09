package com.chapchap.delivery.domain.delivery.response;

import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AdminDeliveryGroupListItemResponse(
    Long deliveryGroupId
    , LocalDate deliveryDate
    , DeliverySlotCode deliverySlot
    , DeliveryGroupStatus status
    , int deliveryCount
    , int assignedDeliveryCount
    , int unassignedDeliveryCount
    , int unacknowledgedAssignmentCount
    , int unresolvedIssueCount
    , int delayedDeliveryCount
    , int failedDeliveryCount
    , OffsetDateTime autoAssignmentCompletedAt
    , OffsetDateTime actualStartedAt
    , OffsetDateTime actualFinishedAt
) {
}
