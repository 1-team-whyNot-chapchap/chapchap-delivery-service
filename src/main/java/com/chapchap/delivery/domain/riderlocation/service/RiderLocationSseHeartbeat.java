package com.chapchap.delivery.domain.riderlocation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiderLocationSseHeartbeat {
    private final RiderLocationSseRegistry registry;

    @Scheduled(fixedDelayString = "${app.rider-location.sse-heartbeat-interval:15s}")
    public void sendHeartbeat() {
        registry.sendHeartbeat();
    }
}
