package com.chapchap.delivery.global.config;

import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.delivery-photo")
public record DeliveryPhotoStorageProperties(
    String endpoint
    , String accessKey
    , String secretKey
    , String bucket
    , Duration presignedGetExpiration
    , long maxFileSizeBytes
    , Set<String> allowedContentTypes
) {
}
