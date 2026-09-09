package com.chapchap.delivery.domain.assignment.scheduler;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentAcknowledgementReminderStage;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.service.RiderAcknowledgementNotificationService;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class RiderAcknowledgementNotificationSchedulerTest {

    private static final ZoneId KST =
        ZoneId.of("Asia/Seoul");

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private RiderAcknowledgementNotificationService riderAcknowledgementNotificationService;

    private RiderAcknowledgementNotificationScheduler riderAcknowledgementNotificationScheduler;

    @BeforeEach
    void setUp() {
        riderAcknowledgementNotificationScheduler =
            new RiderAcknowledgementNotificationScheduler(
                deliveryAssignmentRepository
                , riderAcknowledgementNotificationService
            );
    }

    @Test
    @DisplayName("07시 스케줄러는 오늘 점심 ASSIGNED 배정을 조회하고 확인 오픈 알림을 발행한다")
    void publishLunchAcknowledgementOpenedPublishesForLunchAssignments() {
        // given
        LocalDate today =
            LocalDate.now(KST);

        when(
            deliveryAssignmentRepository.findIdsForAcknowledgementPending(
                today
                , DeliverySlotCode.LUNCH
                , DeliveryAssignmentStatus.ASSIGNED
            )
        )
            .thenReturn(
                List.of(
                    1L
                    , 2L
                    , 3L
                )
            );

        // when
        riderAcknowledgementNotificationScheduler.publishLunchAcknowledgementOpened();

        // then
        verify(deliveryAssignmentRepository)
            .findIdsForAcknowledgementPending(
                today
                , DeliverySlotCode.LUNCH
                , DeliveryAssignmentStatus.ASSIGNED
            );

        InOrder inOrder =
            inOrder(
                riderAcknowledgementNotificationService
            );

        inOrder.verify(
                riderAcknowledgementNotificationService
            )
            .publishOpened(1L);

        inOrder.verify(
                riderAcknowledgementNotificationService
            )
            .publishOpened(2L);

        inOrder.verify(
                riderAcknowledgementNotificationService
            )
            .publishOpened(3L);
    }

    @Test
    @DisplayName("13시 스케줄러는 오늘 저녁 ASSIGNED 배정을 조회하고 확인 오픈 알림을 발행한다")
    void publishDinnerAcknowledgementOpenedPublishesForDinnerAssignments() {
        // given
        LocalDate today =
            LocalDate.now(KST);

        when(
            deliveryAssignmentRepository.findIdsForAcknowledgementPending(
                today
                , DeliverySlotCode.DINNER
                , DeliveryAssignmentStatus.ASSIGNED
            )
        )
            .thenReturn(
                List.of(
                    10L
                    , 20L
                )
            );

        // when
        riderAcknowledgementNotificationScheduler.publishDinnerAcknowledgementOpened();

        // then
        verify(deliveryAssignmentRepository)
            .findIdsForAcknowledgementPending(
                today
                , DeliverySlotCode.DINNER
                , DeliveryAssignmentStatus.ASSIGNED
            );

        InOrder inOrder =
            inOrder(
                riderAcknowledgementNotificationService
            );

        inOrder.verify(
                riderAcknowledgementNotificationService
            )
            .publishOpened(10L);

        inOrder.verify(
                riderAcknowledgementNotificationService
            )
            .publishOpened(20L);
    }

    @Test
    @DisplayName("확인 오픈 대상 배정이 없으면 알림 발행 서비스를 호출하지 않는다")
    void publishAcknowledgementOpenedDoesNothingWhenNoAssignmentsExist() {
        // given
        LocalDate today =
            LocalDate.now(KST);

        when(
            deliveryAssignmentRepository.findIdsForAcknowledgementPending(
                today
                , DeliverySlotCode.LUNCH
                , DeliveryAssignmentStatus.ASSIGNED
            )
        )
            .thenReturn(
                List.of()
            );

        // when
        riderAcknowledgementNotificationScheduler.publishLunchAcknowledgementOpened();

        // then
        verify(deliveryAssignmentRepository)
            .findIdsForAcknowledgementPending(
                today
                , DeliverySlotCode.LUNCH
                , DeliveryAssignmentStatus.ASSIGNED
            );

        verify(
            riderAcknowledgementNotificationService
            , never()
        )
            .publishOpened(
                any()
            );
    }

    @Test
    @DisplayName("08시 스케줄러는 오늘 점심 ASSIGNED 배정에 FIRST 리마인더를 발행한다")
    void publishLunchFirstReminderPublishesFirstReminder() {
        // given
        LocalDate today =
            LocalDate.now(KST);

        when(
            deliveryAssignmentRepository.findIdsForAcknowledgementPending(
                today
                , DeliverySlotCode.LUNCH
                , DeliveryAssignmentStatus.ASSIGNED
            )
        )
            .thenReturn(
                List.of(
                    1L
                    , 2L
                )
            );

        // when
        riderAcknowledgementNotificationScheduler.publishLunchFirstReminder();

        // then
        verify(deliveryAssignmentRepository)
            .findIdsForAcknowledgementPending(
                today
                , DeliverySlotCode.LUNCH
                , DeliveryAssignmentStatus.ASSIGNED
            );

        InOrder inOrder =
            inOrder(
                riderAcknowledgementNotificationService
            );

        inOrder.verify(
                riderAcknowledgementNotificationService
            )
            .publishReminder(
                1L
                , DeliveryAssignmentAcknowledgementReminderStage.FIRST
            );

        inOrder.verify(
                riderAcknowledgementNotificationService
            )
            .publishReminder(
                2L
                , DeliveryAssignmentAcknowledgementReminderStage.FIRST
            );
    }

    @Test
    @DisplayName("08시 30분 스케줄러는 오늘 점심 ASSIGNED 배정에 FINAL 리마인더를 발행한다")
    void publishLunchFinalReminderPublishesFinalReminder() {
        // given
        LocalDate today =
            LocalDate.now(KST);

        when(
            deliveryAssignmentRepository.findIdsForAcknowledgementPending(
                today
                , DeliverySlotCode.LUNCH
                , DeliveryAssignmentStatus.ASSIGNED
            )
        )
            .thenReturn(
                List.of(
                    10L
                    , 20L
                )
            );

        // when
        riderAcknowledgementNotificationScheduler.publishLunchFinalReminder();

        // then
        verify(deliveryAssignmentRepository)
            .findIdsForAcknowledgementPending(
                today
                , DeliverySlotCode.LUNCH
                , DeliveryAssignmentStatus.ASSIGNED
            );

        InOrder inOrder =
            inOrder(
                riderAcknowledgementNotificationService
            );

        inOrder.verify(
                riderAcknowledgementNotificationService
            )
            .publishReminder(
                10L
                , DeliveryAssignmentAcknowledgementReminderStage.FINAL
            );

        inOrder.verify(
                riderAcknowledgementNotificationService
            )
            .publishReminder(
                20L
                , DeliveryAssignmentAcknowledgementReminderStage.FINAL
            );
    }

    @Test
    @DisplayName("14시 스케줄러는 오늘 저녁 ASSIGNED 배정에 FIRST 리마인더를 발행한다")
    void publishDinnerFirstReminderPublishesFirstReminder() {
        // given
        LocalDate today =
            LocalDate.now(KST);

        when(
            deliveryAssignmentRepository.findIdsForAcknowledgementPending(
                today
                , DeliverySlotCode.DINNER
                , DeliveryAssignmentStatus.ASSIGNED
            )
        )
            .thenReturn(
                List.of(
                    100L
                    , 200L
                )
            );

        // when
        riderAcknowledgementNotificationScheduler.publishDinnerFirstReminder();

        // then
        verify(deliveryAssignmentRepository)
            .findIdsForAcknowledgementPending(
                today
                , DeliverySlotCode.DINNER
                , DeliveryAssignmentStatus.ASSIGNED
            );

        InOrder inOrder =
            inOrder(
                riderAcknowledgementNotificationService
            );

        inOrder.verify(
                riderAcknowledgementNotificationService
            )
            .publishReminder(
                100L
                , DeliveryAssignmentAcknowledgementReminderStage.FIRST
            );

        inOrder.verify(
                riderAcknowledgementNotificationService
            )
            .publishReminder(
                200L
                , DeliveryAssignmentAcknowledgementReminderStage.FIRST
            );
    }

    @Test
    @DisplayName("14시 30분 스케줄러는 오늘 저녁 ASSIGNED 배정에 FINAL 리마인더를 발행한다")
    void publishDinnerFinalReminderPublishesFinalReminder() {
        // given
        LocalDate today =
            LocalDate.now(KST);

        when(
            deliveryAssignmentRepository.findIdsForAcknowledgementPending(
                today
                , DeliverySlotCode.DINNER
                , DeliveryAssignmentStatus.ASSIGNED
            )
        )
            .thenReturn(
                List.of(
                    1000L
                    , 2000L
                )
            );

        // when
        riderAcknowledgementNotificationScheduler.publishDinnerFinalReminder();

        // then
        verify(deliveryAssignmentRepository)
            .findIdsForAcknowledgementPending(
                today
                , DeliverySlotCode.DINNER
                , DeliveryAssignmentStatus.ASSIGNED
            );

        InOrder inOrder =
            inOrder(
                riderAcknowledgementNotificationService
            );

        inOrder.verify(
                riderAcknowledgementNotificationService
            )
            .publishReminder(
                1000L
                , DeliveryAssignmentAcknowledgementReminderStage.FINAL
            );

        inOrder.verify(
                riderAcknowledgementNotificationService
            )
            .publishReminder(
                2000L
                , DeliveryAssignmentAcknowledgementReminderStage.FINAL
            );
    }
}