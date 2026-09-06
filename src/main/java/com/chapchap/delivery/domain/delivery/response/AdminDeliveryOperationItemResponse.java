package com.chapchap.delivery.domain.delivery.response;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.delivery.constant.AdminDeliveryOperationType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AdminDeliveryOperationItemResponse(
    AdminDeliveryOperationType type
    , Long deliveryGroupId
    , String deliveryId
    , Long assignmentId
    , Long riderId
    , LocalDate deliveryDate
    , DeliverySlotCode deliverySlot
    , DeliveryGroupStatus deliveryGroupStatus
    , DeliveryStatus deliveryStatus
    , DeliveryAssignmentStatus assignmentStatus
    , OffsetDateTime detectedAt
) {
}
