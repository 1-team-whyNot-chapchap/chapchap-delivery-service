package com.chapchap.delivery.global.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.delivery-photo")
public record DeliveryPhotoStorageProperties(
    Duration presignedGetExpiration
    , long maxFileSizeBytes
) {
}
