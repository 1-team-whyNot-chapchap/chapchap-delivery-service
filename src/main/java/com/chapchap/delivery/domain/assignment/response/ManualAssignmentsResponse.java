package com.chapchap.delivery.domain.assignment.response;

import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;

import java.util.List;

public record ManualAssignmentsResponse(
    Long deliveryGroupId
    , DeliveryGroupStatus status
    , List<Long> assignmentIds
) {
}
