package com.chapchap.delivery.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RiderLocationProperties.class)
public class RiderLocationConfig {
}
