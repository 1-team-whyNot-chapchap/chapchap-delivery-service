package com.chapchap.delivery.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Temporary controls for an isolated deployment-verification environment. */
@ConfigurationProperties(prefix = "app.operations.delivery-verification")
public record DeliveryVerificationProperties(
    boolean bypassAcknowledgementWindow,
    long autoAssignmentTargetDateOffsetDays,
    long riderAcknowledgementTargetDateOffsetDays
) {
}
