package com.chapchap.delivery.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliveryDelayRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.global.kafka.producer.DeliveryEventRequestPublisher;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryDelayDetectionServiceTest {
    private static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 9, 6);

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryDelayRepository delayRepository;
    @Mock private DeliveryEventRequestPublisher eventPublisher;

    private DeliveryDelayDetectionService delayDetectionService;

    @BeforeEach
    void setUp() {
        delayDetectionService = new DeliveryDelayDetectionService(
            deliveryRepository
            , delayRepository
            , eventPublisher
        );
    }

    @Test
    @DisplayName("점심 종료 다음 분부터 미완료 배송의 지연을 한 번 기록한다")
    void detectLunchDelay() {
        Delivery delivery = delivery(1L, LocalTime.of(13, 0));
        LocalDateTime detectedAt = LocalDateTime.of(DELIVERY_DATE, LocalTime.of(13, 1));
        when(deliveryRepository.findUnfinishedByDeliveryDateAndSlotForUpdate(
            DELIVERY_DATE
            , DeliverySlotCode.LUNCH
            , List.of(DeliveryStatus.READY, DeliveryStatus.DELIVERING)
        )).thenReturn(List.of(delivery));
        when(delayRepository.insertIfAbsent(1L, detectedAt)).thenReturn(1);

        int detectedCount = delayDetectionService.detect(DeliverySlotCode.LUNCH, detectedAt);

        assertThat(detectedCount).isEqualTo(1);
        verify(delayRepository).insertIfAbsent(1L, detectedAt);
    }

    @Test
    @DisplayName("점심 13시 00분 59초에는 지연으로 기록하지 않는다")
    void doesNotDetectBeforeLunchBoundary() {
        Delivery delivery = delivery(1L, LocalTime.of(13, 0));
        LocalDateTime detectedAt = LocalDateTime.of(
            DELIVERY_DATE, LocalTime.of(13, 0, 59)
        );
        when(deliveryRepository.findUnfinishedByDeliveryDateAndSlotForUpdate(
            DELIVERY_DATE, DeliverySlotCode.LUNCH,
            List.of(DeliveryStatus.READY, DeliveryStatus.DELIVERING)
        )).thenReturn(List.of(delivery));

        assertThat(delayDetectionService.detect(DeliverySlotCode.LUNCH, detectedAt)).isZero();
        verify(delayRepository, never()).insertIfAbsent(any(), any());
        verify(eventPublisher, never()).publishDelayed(any(), any());
    }

    @Test
    @DisplayName("이미 지연이 기록된 배송은 중복 저장하지 않는다")
    void doesNotDuplicateDelay() {
        Delivery delivery = delivery(1L, LocalTime.of(13, 0));
        LocalDateTime detectedAt = LocalDateTime.of(DELIVERY_DATE, LocalTime.of(13, 1));
        when(deliveryRepository.findUnfinishedByDeliveryDateAndSlotForUpdate(
            DELIVERY_DATE
            , DeliverySlotCode.LUNCH
            , List.of(DeliveryStatus.READY, DeliveryStatus.DELIVERING)
        )).thenReturn(List.of(delivery));
        when(delayRepository.insertIfAbsent(1L, detectedAt)).thenReturn(0);

        int detectedCount = delayDetectionService.detect(DeliverySlotCode.LUNCH, detectedAt);

        assertThat(detectedCount).isZero();
        verify(eventPublisher, never()).publishDelayed(any(), any());
    }

    private Delivery delivery(Long deliveryId, LocalTime endTime) {
        Delivery delivery = mock(Delivery.class);
        DeliveryGroup group = mock(DeliveryGroup.class);
        DeliverySlot slot = mock(DeliverySlot.class);
        lenient().when(delivery.getId()).thenReturn(deliveryId);
        lenient().when(delivery.getDeliveryGroup()).thenReturn(group);
        lenient().when(group.getDeliveryDate()).thenReturn(DELIVERY_DATE);
        lenient().when(group.getSlot()).thenReturn(slot);
        lenient().when(slot.getEndTime()).thenReturn(endTime);
        return delivery;
    }
}
