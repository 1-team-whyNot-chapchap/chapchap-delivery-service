package com.chapchap.delivery.domain.delivery.scheduler;

import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.service.DeliveryDelayDetectionService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeliveryDelayDetectionScheduler {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryDelayDetectionService delayDetectionService;

    public DeliveryDelayDetectionScheduler(
        DeliveryDelayDetectionService delayDetectionService
    ) {
        this.delayDetectionService = delayDetectionService;
    }

    @Scheduled(cron = "0 1 13 * * *", zone = "Asia/Seoul")
    public void detectLunchDelay() {
        delayDetectionService.detect(
            DeliverySlotCode.LUNCH
            , LocalDateTime.now(KST)
        );
    }

    @Scheduled(cron = "0 1 19 * * *", zone = "Asia/Seoul")
    public void detectDinnerDelay() {
        delayDetectionService.detect(
            DeliverySlotCode.DINNER
            , LocalDateTime.now(KST)
        );
    }
}
