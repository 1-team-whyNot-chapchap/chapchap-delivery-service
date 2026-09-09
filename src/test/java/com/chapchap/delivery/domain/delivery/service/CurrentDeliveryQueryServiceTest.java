package com.chapchap.delivery.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.delivery.constant.CurrentDeliveryDelayStatus;
import com.chapchap.delivery.domain.delivery.constant.CurrentDeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.entity.DeliveryStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryDelayRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.global.exception.business.CurrentDeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.CurrentDeliveryDataConflictException;
import com.chapchap.delivery.global.exception.business.CurrentDeliveryNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CurrentDeliveryQueryServiceTest {
    private DeliveryRepository deliveryRepository;
    private DeliveryDelayRepository delayRepository;
    private DeliveryStatusHistoryRepository historyRepository;
    private CurrentDeliveryQueryService service;

    @BeforeEach
    void setUp() {
        deliveryRepository = mock(DeliveryRepository.class);
        delayRepository = mock(DeliveryDelayRepository.class);
        historyRepository = mock(DeliveryStatusHistoryRepository.class);
        service = new CurrentDeliveryQueryService(
            deliveryRepository, delayRepository, historyRepository
        );
    }

    @Test
    @DisplayName("현재 배송은 종료 배송보다 배송 중 대상을 우선한다")
    void prioritizesDelivering() {
        Delivery delivering = delivery(1L, DeliveryStatus.DELIVERING, LocalTime.MAX);
        Delivery completed = delivery(2L, DeliveryStatus.DELIVERED, LocalTime.MIN);
        DeliveryStatusHistory history = history(DeliveryStatus.DELIVERING, LocalDateTime.now().minusMinutes(5));
        when(deliveryRepository.findAllForCurrentState(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of(completed, delivering));
        when(historyRepository.findAllByDelivery_IdOrderByChangedAtAsc(1L)).thenReturn(List.of(history));
        when(delayRepository.findByDeliveryId(1L)).thenReturn(Optional.empty());

        var response = service.getCurrent(10L, UserRole.CUSTOMER);

        assertThat(response.status()).isEqualTo(CurrentDeliveryStatus.DELIVERING);
        assertThat(response.delayStatus()).isEqualTo(CurrentDeliveryDelayStatus.NOT_DELAYED);
        assertThat(response.statusChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("내부 완료 상태는 공개 COMPLETED로 변환한다")
    void mapsDeliveredToCompleted() {
        Delivery delivered = delivery(1L, DeliveryStatus.DELIVERED, LocalTime.MIN);
        LocalDateTime changedAt = LocalDate.now().atStartOfDay();
        DeliveryStatusHistory deliveredHistory = history(DeliveryStatus.DELIVERED, changedAt);
        when(deliveryRepository.findAllForCurrentState(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of(delivered));
        when(historyRepository.findAllByDelivery_IdOrderByChangedAtAsc(1L))
            .thenReturn(List.of(deliveredHistory));
        when(delayRepository.findByDeliveryId(1L)).thenReturn(Optional.empty());

        var response = service.getCurrent(10L, UserRole.CUSTOMER);

        assertThat(response.status()).isEqualTo(CurrentDeliveryStatus.COMPLETED);
    }

    @Test
    @DisplayName("동일 최우선 배송이 복수이면 임의 선택하지 않는다")
    void rejectsAmbiguousCandidates() {
        Delivery first = delivery(1L, DeliveryStatus.DELIVERING, LocalTime.NOON);
        Delivery second = delivery(2L, DeliveryStatus.DELIVERING, LocalTime.NOON);
        when(deliveryRepository.findAllForCurrentState(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.getCurrent(10L, UserRole.CUSTOMER))
            .isInstanceOf(CurrentDeliveryDataConflictException.class);
    }

    @Test
    @DisplayName("고객 역할이 아니면 현재 배송을 조회할 수 없다")
    void rejectsNonCustomer() {
        assertThatThrownBy(() -> service.getCurrent(10L, UserRole.ADMIN))
            .isInstanceOf(CurrentDeliveryAccessForbiddenException.class);
    }

    @Test
    @DisplayName("대표 현재 배송 후보가 없으면 전용 404 오류를 반환한다")
    void rejectsMissingCurrentDelivery() {
        when(deliveryRepository.findAllForCurrentState(
            org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of());

        assertThatThrownBy(() -> service.getCurrent(10L, UserRole.CUSTOMER))
            .isInstanceOf(CurrentDeliveryNotFoundException.class);
    }

    private Delivery delivery(Long id, DeliveryStatus status, LocalTime endTime) {
        Delivery delivery = mock(Delivery.class);
        DeliveryGroup group = mock(DeliveryGroup.class);
        DeliverySlot slot = mock(DeliverySlot.class);
        when(delivery.getId()).thenReturn(id);
        when(delivery.getStatus()).thenReturn(status);
        when(delivery.getDeliveryGroup()).thenReturn(group);
        when(group.getDeliveryDate()).thenReturn(LocalDate.now().plusDays(1));
        when(group.getSlot()).thenReturn(slot);
        when(slot.getStartTime()).thenReturn(endTime);
        when(slot.getEndTime()).thenReturn(endTime);
        return delivery;
    }

    private DeliveryStatusHistory history(DeliveryStatus status, LocalDateTime changedAt) {
        DeliveryStatusHistory history = mock(DeliveryStatusHistory.class);
        when(history.getToStatus()).thenReturn(status);
        when(history.getChangedAt()).thenReturn(changedAt);
        return history;
    }
}
