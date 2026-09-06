package com.chapchap.delivery.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryDelay;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliveryDelayRepository;
import com.chapchap.delivery.global.kafka.producer.DeliveryEventRequestPublisher;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryDelayServiceTest {
    private static final Long DELIVERY_ID = 1L;
    private static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 9, 6);

    @Mock
    private DeliveryDelayRepository delayRepository;

    @Mock
    private DeliveryEventRequestPublisher eventPublisher;

    private Delivery delivery;
    private DeliveryDelay delay;
    private DeliveryDelayService delayService;

    @BeforeEach
    void setUp() {
        delivery = delivery(LocalTime.of(13, 0));
        delay = mock(DeliveryDelay.class);
        delayService = new DeliveryDelayService(
            delayRepository
            , eventPublisher
        );
    }

    @Test
    @DisplayName("점심 13시 00분 59초 완료는 지연이 아니다")
    void lunchCompletionBeforeBoundaryIsNotDelayed() {
        LocalDateTime completedAt = LocalDateTime.of(
            DELIVERY_DATE
            , LocalTime.of(13, 0, 59)
        );

        assertThat(delayService.recordCompletionDelay(delivery, completedAt)).isFalse();
        verify(delayRepository, never()).insertIfAbsent(DELIVERY_ID, completedAt);
        verify(eventPublisher, never()).publishDelayed(delivery, completedAt);
    }

    @Test
    @DisplayName("점심 13시 01분 완료는 1분 지연으로 확정한다")
    void lunchCompletionAtBoundaryIsDelayed() {
        LocalDateTime completedAt = LocalDateTime.of(
            DELIVERY_DATE
            , LocalTime.of(13, 1)
        );
        when(delayRepository.insertIfAbsent(DELIVERY_ID, completedAt)).thenReturn(1);
        when(delayRepository.findByDeliveryId(DELIVERY_ID)).thenReturn(Optional.of(delay));

        assertThat(delayService.recordCompletionDelay(delivery, completedAt)).isTrue();
        verify(delay).finalizeDelay(1);
        verify(eventPublisher).publishDelayed(delivery, completedAt);
    }

    @Test
    @DisplayName("스케줄러가 먼저 지연을 기록해도 완료 시 최종 지연 시간을 확정한다")
    void finalizesExistingDelayWithoutDuplicateEvent() {
        LocalDateTime completedAt = LocalDateTime.of(
            DELIVERY_DATE
            , LocalTime.of(13, 2)
        );
        when(delayRepository.insertIfAbsent(DELIVERY_ID, completedAt)).thenReturn(0);
        when(delayRepository.findByDeliveryId(DELIVERY_ID)).thenReturn(Optional.of(delay));

        assertThat(delayService.recordCompletionDelay(delivery, completedAt)).isTrue();
        verify(delay).finalizeDelay(2);
        verify(eventPublisher, never()).publishDelayed(delivery, completedAt);
    }

    @Test
    @DisplayName("저녁 19시 01분 완료도 1분 지연으로 확정한다")
    void dinnerCompletionAtBoundaryIsDelayed() {
        delivery = delivery(LocalTime.of(19, 0));
        LocalDateTime completedAt = LocalDateTime.of(
            DELIVERY_DATE
            , LocalTime.of(19, 1)
        );
        when(delayRepository.insertIfAbsent(DELIVERY_ID, completedAt)).thenReturn(1);
        when(delayRepository.findByDeliveryId(DELIVERY_ID)).thenReturn(Optional.of(delay));

        assertThat(delayService.recordCompletionDelay(delivery, completedAt)).isTrue();
        verify(delay).finalizeDelay(1);
    }

    private Delivery delivery(LocalTime endTime) {
        Delivery target = mock(Delivery.class);
        DeliveryGroup group = mock(DeliveryGroup.class);
        DeliverySlot slot = mock(DeliverySlot.class);
        lenient().when(target.getId()).thenReturn(DELIVERY_ID);
        lenient().when(target.getDeliveryGroup()).thenReturn(group);
        lenient().when(group.getDeliveryDate()).thenReturn(DELIVERY_DATE);
        lenient().when(group.getSlot()).thenReturn(slot);
        lenient().when(slot.getEndTime()).thenReturn(endTime);
        return target;
    }
}
