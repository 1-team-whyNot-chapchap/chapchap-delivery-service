package com.chapchap.delivery.domain.assignment.response;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentType;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RiderAssignmentListItemResponse(
    Long assignmentId
    , Long deliveryGroupId
    , LocalDate deliveryDate
    , DeliverySlotCode deliverySlot
    , DeliveryAssignmentType assignmentType
    , DeliveryAssignmentStatus status
    , OffsetDateTime assignedAt
    , OffsetDateTime acknowledgementAvailableAt
    , OffsetDateTime acknowledgedAt
    , int stopCount
    , int lunchboxQuantity
    , boolean recommendedCapacityExceeded
    , boolean maximumCapacityExceeded
) {
}