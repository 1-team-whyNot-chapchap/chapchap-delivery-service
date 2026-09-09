package com.chapchap.delivery.domain.assignment.response;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;

import java.time.LocalDate;
import java.util.List;

public record RiderAssignmentDetailResponse(
    Long assignmentId
    , DeliveryAssignmentStatus status
    , LocalDate deliveryDate
    , DeliverySlotCode deliverySlot
    , Integer stopCount
    , Integer lunchboxQuantity
    , List<RiderAssignmentDeliveryItemResponse> deliveries
) {
}