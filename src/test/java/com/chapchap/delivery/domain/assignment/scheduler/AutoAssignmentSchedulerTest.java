package com.chapchap.delivery.domain.assignment.scheduler;

import com.chapchap.delivery.domain.assignment.service.AutoAssignmentService;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.global.config.DeliveryVerificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoAssignmentSchedulerTest {
    @Mock
    private DeliveryGroupRepository deliveryGroupRepository;

    @Mock
    private AutoAssignmentService autoAssignmentService;

    private AutoAssignmentScheduler autoAssignmentScheduler;

    @BeforeEach
    void setUp() {
        autoAssignmentScheduler =
            new AutoAssignmentScheduler(
                deliveryGroupRepository
                , autoAssignmentService
                , new DeliveryVerificationProperties(false, 1, 0)
            );
    }

    @Test
    void autoAssignmentCronCanBeOverriddenForDeploymentVerification() throws NoSuchMethodException {
        Method method = AutoAssignmentScheduler.class.getMethod("runAutoAssignment");
        Scheduled[] schedules = method.getAnnotationsByType(Scheduled.class);

        assertThat(schedules)
            .extracting(Scheduled::cron)
            .containsExactly(
                "${app.scheduler.auto-assignment.interval-cron:0 10,20,30,40,50 16 * * *}",
                "${app.scheduler.auto-assignment.final-cron:0 0 17 * * *}"
            );
    }

    @Test
    void runAutoAssignment() {
        when(
            deliveryGroupRepository.findAutoAssignmentTargetIds(
                any(LocalDate.class)
                , any(DeliveryGroupStatus.class)
            )
        )
            .thenReturn(
                List.of(
                    1L
                    , 2L
                    , 3L
                )
            );

        autoAssignmentScheduler.runAutoAssignment();

        ArgumentCaptor<LocalDate> deliveryDateCaptor =
            ArgumentCaptor.forClass(
                LocalDate.class
            );

        verify(deliveryGroupRepository)
            .findAutoAssignmentTargetIds(
                deliveryDateCaptor.capture()
                , eq(DeliveryGroupStatus.WAITING_ASSIGNMENT)
            );

        assertThat(deliveryDateCaptor.getValue())
            .isEqualTo(
                LocalDate.now(ZoneId.of("Asia/Seoul"))
                    .plusDays(1)
            );

        verify(autoAssignmentService)
            .assign(1L);

        verify(autoAssignmentService)
            .assign(2L);

        verify(autoAssignmentService)
            .assign(3L);
    }

    @Test
    void runAutoAssignmentUsesConfiguredTargetDateOffsetForDeploymentVerification() {
        autoAssignmentScheduler =
            new AutoAssignmentScheduler(
                deliveryGroupRepository
                , autoAssignmentService
                , new DeliveryVerificationProperties(false, 0, 0)
            );

        when(
            deliveryGroupRepository.findAutoAssignmentTargetIds(
                any(LocalDate.class)
                , any(DeliveryGroupStatus.class)
            )
        ).thenReturn(List.of());

        autoAssignmentScheduler.runAutoAssignment();

        verify(deliveryGroupRepository)
            .findAutoAssignmentTargetIds(
                eq(LocalDate.now(ZoneId.of("Asia/Seoul")))
                , eq(DeliveryGroupStatus.WAITING_ASSIGNMENT)
            );
    }
}
