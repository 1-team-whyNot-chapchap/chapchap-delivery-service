package com.chapchap.delivery.domain.riderlocation.response;

import com.chapchap.delivery.domain.riderlocation.entity.RiderCurrentLocation;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record RiderLocationResponse(
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal accuracy,
    OffsetDateTime capturedAt,
    OffsetDateTime receivedAt
) {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static RiderLocationResponse from(RiderCurrentLocation location) {
        return new RiderLocationResponse(
            location.getLatitude(), location.getLongitude(), location.getAccuracyM(),
            location.getCapturedAt().atZone(KST).toOffsetDateTime(),
            location.getReceivedAt().atZone(KST).toOffsetDateTime()
        );
    }
}
