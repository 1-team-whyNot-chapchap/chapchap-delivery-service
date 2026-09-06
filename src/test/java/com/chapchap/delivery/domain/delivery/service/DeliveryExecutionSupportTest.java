package com.chapchap.delivery.domain.delivery.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryExecutionSupportTest {
    private static final LocalDateTime CHANGED_AT = LocalDateTime.of(2026, 9, 6, 13, 1);

    @Mock private DeliveryGroupStatusHistoryRepository historyRepository;
    private DeliveryExecutionSupport support;

    @BeforeEach
    void setUp() { support = new DeliveryExecutionSupport(historyRepository); }

    @Test @DisplayName("READY + READY는 그룹을 변경하지 않는다")
    void readyReady() { verifyNoChange(DeliveryStatus.READY, DeliveryStatus.READY); }

    @Test @DisplayName("READY + DELIVERED는 아직 종료하지 않는다")
    void readyDelivered() { verifyNoChange(DeliveryStatus.READY, DeliveryStatus.DELIVERED); }

    @Test @DisplayName("READY + DELIVERING은 IN_PROGRESS로 계산한다")
    void readyDelivering() { verifyInProgress(DeliveryStatus.READY, DeliveryStatus.DELIVERING); }

    @Test @DisplayName("DELIVERING + DELIVERED는 IN_PROGRESS로 계산한다")
    void deliveringDelivered() { verifyInProgress(DeliveryStatus.DELIVERING, DeliveryStatus.DELIVERED); }

    @Test @DisplayName("모두 DELIVERED이면 COMPLETED로 계산한다")
    void deliveredDelivered() { verifyFinal(DeliveryGroupStatus.COMPLETED, DeliveryStatus.DELIVERED, DeliveryStatus.DELIVERED); }

    @Test @DisplayName("DELIVERED + FAILED이면 COMPLETED_WITH_FAILURE로 계산한다")
    void deliveredFailed() { verifyFinal(DeliveryGroupStatus.COMPLETED_WITH_FAILURE, DeliveryStatus.DELIVERED, DeliveryStatus.FAILED); }

    @Test @DisplayName("모두 FAILED이면 FAILED로 계산한다")
    void failedFailed() { verifyFinal(DeliveryGroupStatus.FAILED, DeliveryStatus.FAILED, DeliveryStatus.FAILED); }

    private void verifyNoChange(DeliveryStatus first, DeliveryStatus second) {
        DeliveryGroup group = group(DeliveryGroupStatus.IN_PROGRESS);
        support.recalculateGroup(group, deliveries(first, second), CHANGED_AT);
        verify(group, never()).startExecution(any());
        verify(group, never()).finishExecution(any(), any());
        verify(historyRepository, never()).save(any());
    }

    private void verifyInProgress(DeliveryStatus first, DeliveryStatus second) {
        DeliveryGroup group = group(DeliveryGroupStatus.CONFIRMED);
        when(group.startExecution(CHANGED_AT)).thenReturn(true);
        support.recalculateGroup(group, deliveries(first, second), CHANGED_AT);
        verify(group).startExecution(CHANGED_AT);
        verify(historyRepository).save(any());
    }

    private void verifyFinal(DeliveryGroupStatus expected, DeliveryStatus first, DeliveryStatus second) {
        DeliveryGroup group = group(DeliveryGroupStatus.IN_PROGRESS);
        when(group.finishExecution(expected, CHANGED_AT)).thenReturn(true);
        support.recalculateGroup(group, deliveries(first, second), CHANGED_AT);
        verify(group).finishExecution(expected, CHANGED_AT);
        verify(historyRepository).save(any());
    }

    private DeliveryGroup group(DeliveryGroupStatus status) {
        DeliveryGroup group = mock(DeliveryGroup.class);
        lenient().when(group.getStatus()).thenReturn(status);
        return group;
    }

    private List<Delivery> deliveries(DeliveryStatus first, DeliveryStatus second) {
        Delivery one = mock(Delivery.class);
        Delivery two = mock(Delivery.class);
        lenient().when(one.getStatus()).thenReturn(first);
        lenient().when(two.getStatus()).thenReturn(second);
        return List.of(one, two);
    }
}
