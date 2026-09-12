package com.chapchap.delivery.global.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rider-location")
public record RiderLocationProperties(
    double maxAccuracyMeters,
    Duration maxFutureSkew,
    Duration maxPositionAge,
    Duration sseHeartbeatInterval,
    Duration sseTimeout
) {
}
