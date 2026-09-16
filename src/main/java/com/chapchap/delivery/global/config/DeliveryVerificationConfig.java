package com.chapchap.delivery.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DeliveryVerificationProperties.class)
public class DeliveryVerificationConfig {
}
