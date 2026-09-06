package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryDelay;
import com.chapchap.delivery.domain.delivery.repository.DeliveryDelayRepository;
import com.chapchap.delivery.global.kafka.producer.DeliveryEventRequestPublisher;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class DeliveryDelayService {
    private final DeliveryDelayRepository delayRepository;
    private final DeliveryEventRequestPublisher eventPublisher;

    public DeliveryDelayService(
        DeliveryDelayRepository delayRepository
        , DeliveryEventRequestPublisher eventPublisher
    ) {
        this.delayRepository = delayRepository;
        this.eventPublisher = eventPublisher;
    }

    public boolean recordCompletionDelay(
        Delivery delivery
        , LocalDateTime completedAt
    ) {
        LocalDateTime delayedFrom = LocalDateTime.of(
            delivery.getDeliveryGroup().getDeliveryDate()
            , delivery.getDeliveryGroup().getSlot().getEndTime()
        ).plusMinutes(1);

        if (completedAt.isBefore(delayedFrom)) {
            return false;
        }

        int inserted = delayRepository.insertIfAbsent(
            delivery.getId()
            , completedAt
        );
        DeliveryDelay delay = delayRepository.findByDeliveryId(delivery.getId())
            .orElseThrow();
        delay.finalizeDelay(calculateDelayMinutes(delayedFrom, completedAt));
        if (inserted == 1) {
            eventPublisher.publishDelayed(delivery, completedAt);
        }
        return true;
    }

    private int calculateDelayMinutes(
        LocalDateTime delayedFrom
        , LocalDateTime completedAt
    ) {
        long seconds = Duration.between(delayedFrom, completedAt).getSeconds();
        return Math.toIntExact((seconds / 60L) + 1L);
    }
}
