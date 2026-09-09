package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryDelay;
import com.chapchap.delivery.domain.delivery.repository.DeliveryDelayRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chapchap.delivery.global.kafka.producer.DeliveryEventRequestPublisher;

@Service
public class DeliveryDelayDetectionService {
    private static final List<DeliveryStatus> UNFINISHED_STATUSES =
        List.of(
            DeliveryStatus.READY
            , DeliveryStatus.DELIVERING
        );

    private final DeliveryRepository deliveryRepository;
    private final DeliveryDelayRepository delayRepository;
    private final DeliveryEventRequestPublisher eventPublisher;

    public DeliveryDelayDetectionService(
        DeliveryRepository deliveryRepository
        , DeliveryDelayRepository delayRepository
        , DeliveryEventRequestPublisher eventPublisher
    ) {
        this.deliveryRepository = deliveryRepository;
        this.delayRepository = delayRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public int detect(
        DeliverySlotCode slotCode
        , LocalDateTime detectedAt
    ) {
        List<Delivery> unfinishedDeliveries =
            deliveryRepository.findUnfinishedByDeliveryDateAndSlotForUpdate(
                detectedAt.toLocalDate()
                , slotCode
                , UNFINISHED_STATUSES
            );

        int detectedCount = 0;

        for (Delivery delivery : unfinishedDeliveries) {
            LocalDateTime delayedFrom = LocalDateTime.of(
                delivery.getDeliveryGroup().getDeliveryDate()
                , delivery.getDeliveryGroup().getSlot().getEndTime()
            ).plusMinutes(1);

            if (detectedAt.isBefore(delayedFrom)) {
                continue;
            }

            if (delayRepository.insertIfAbsent(delivery.getId(), detectedAt) != 1) {
                continue;
            }
            eventPublisher.publishDelayed(
                delivery
                , detectedAt
            );
            detectedCount++;
        }

        return detectedCount;
    }
}
