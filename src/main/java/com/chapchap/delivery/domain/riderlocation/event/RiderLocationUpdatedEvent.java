package com.chapchap.delivery.domain.riderlocation.event;

import com.chapchap.delivery.domain.riderlocation.response.RiderLocationResponse;

public record RiderLocationUpdatedEvent(Long riderId, RiderLocationResponse location) {
}
