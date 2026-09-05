package com.chapchap.delivery.domain.assignment.repository;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentType;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface RiderAssignmentListProjection {

    Long getAssignmentId();

    Long getDeliveryGroupId();

    LocalDate getDeliveryDate();

    DeliverySlotCode getDeliverySlot();

    DeliveryAssignmentType getAssignmentType();

    DeliveryAssignmentStatus getStatus();

    LocalDateTime getAssignedAt();

    LocalDateTime getAcknowledgedAt();

    Long getStopCount();

    Long getLunchboxQuantity();
}