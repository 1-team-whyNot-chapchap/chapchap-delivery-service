package com.chapchap.delivery.domain.assignment.response;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record RiderAssignmentAcknowledgementResponse(
    Long assignmentId
    , DeliveryAssignmentStatus status
    , OffsetDateTime acknowledgedAt
) {
    private static final ZoneId KST =
        ZoneId.of("Asia/Seoul");

    public static RiderAssignmentAcknowledgementResponse from(
        DeliveryAssignment assignment
    ) {
        return new RiderAssignmentAcknowledgementResponse(
            assignment.getId()
            , assignment.getStatus()
            , assignment.getAcknowledgedAt()
            .atZone(KST)
            .toOffsetDateTime()
        );
    }
}