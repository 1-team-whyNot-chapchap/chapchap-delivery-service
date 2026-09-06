package com.chapchap.delivery.domain.delivery.response;

import java.util.List;

public record RiderEmergencyDeliveryFailureResponse(
    Long assignmentId
    , int failedCount
    , List<String> deliveryIds
) {
}
