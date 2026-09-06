package com.chapchap.delivery.domain.delivery.response;

public record AdminDeliveryOperationCountsResponse(
    long autoAssignmentFinalFailure
    , long lateOrderReview
    , long acknowledgementOverdue
    , long unresolvedDelivery
) {
}
