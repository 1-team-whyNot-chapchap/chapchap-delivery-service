package com.chapchap.delivery.domain.rider.response;

import java.util.List;

public record RiderScheduleResponse(
    Long riderId
    , Boolean isDeliveryActive
    , List<RiderScheduleItemResponse> schedules
) {
}