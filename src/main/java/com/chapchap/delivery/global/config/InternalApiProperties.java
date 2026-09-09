package com.chapchap.delivery.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.internal-api")
public record InternalApiProperties(String apiKey, String customerAiApiKey) {
}
