package com.chapchap.delivery.domain.assignment.response;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import java.time.OffsetDateTime;

public record AdminEmergencyRiderReplacementResponse(
    Long previousAssignmentId
    , DeliveryAssignmentStatus previousAssignmentStatus
    , Long newAssignmentId
    , DeliveryAssignmentStatus newAssignmentStatus
    , Long previousRiderId
    , Long newRiderId
    , OffsetDateTime replacedAt
) {
}
