package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroupStatusHistory;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentStateConflictException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderAssignmentAcknowledgementServiceTest {

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private DeliveryGroupRepository deliveryGroupRepository;

    @Mock
    private DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;

    @Mock
    private DeliveryAccessService deliveryAccessService;

    @Mock
    private EntityManager entityManager;

    private RiderAssignmentAcknowledgementService riderAssignmentAcknowledgementService;

    @BeforeEach
    void setUp() {
        riderAssignmentAcknowledgementService =
            new RiderAssignmentAcknowledgementService(
                deliveryAssignmentRepository
                , deliveryGroupRepository
                , deliveryGroupStatusHistoryRepository
                , deliveryAccessService
                , entityManager
            );
    }

    @Test
    @DisplayName("이미 확인한 배정은 기존 결과를 그대로 반환한다")
    void acknowledgeReturnsExistingAcknowledgementWhenAlreadyAcknowledged() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;
        Long deliveryGroupId = 10L;

        DeliveryAssignment assignment =
            org.mockito.Mockito.mock(
                DeliveryAssignment.class
            );

        DeliveryGroup deliveryGroup =
            org.mockito.Mockito.mock(
                DeliveryGroup.class
            );

        Rider rider =
            org.mockito.Mockito.mock(
                Rider.class
            );

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                assignmentId
                , authUserId
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(deliveryGroup.getId())
            .thenReturn(deliveryGroupId);

        when(
            deliveryGroupRepository.findByIdForUpdate(
                deliveryGroupId
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(true);

        when(assignment.isAcknowledged())
            .thenReturn(true);

        // when
        DeliveryAssignment result =
            riderAssignmentAcknowledgementService.acknowledge(
                authUserId
                , assignmentId
            );

        // then
        assertThat(result)
            .isEqualTo(assignment);

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .acknowledgeIfAssigned(
                any()
                , any()
                , any()
                , any()
                , any()
            );

        verify(
            entityManager
            , never()
        )
            .refresh(any());

        verify(
            deliveryGroupStatusHistoryRepository
            , never()
        )
            .save(any());
    }

    @Test
    @DisplayName("ASSIGNED 상태가 아니면 배정 확인을 거부한다")
    void acknowledgeThrowsStateConflictWhenAssignmentIsNotAssigned() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;
        Long deliveryGroupId = 10L;

        DeliveryAssignment assignment =
            org.mockito.Mockito.mock(
                DeliveryAssignment.class
            );

        DeliveryGroup deliveryGroup =
            org.mockito.Mockito.mock(
                DeliveryGroup.class
            );

        Rider rider =
            org.mockito.Mockito.mock(
                Rider.class
            );

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                assignmentId
                , authUserId
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(deliveryGroup.getId())
            .thenReturn(deliveryGroupId);

        when(
            deliveryGroupRepository.findByIdForUpdate(
                deliveryGroupId
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(true);

        when(assignment.isAcknowledged())
            .thenReturn(false);

        when(assignment.isAssigned())
            .thenReturn(false);

        // when & then
        assertThatThrownBy(
            () ->
                riderAssignmentAcknowledgementService.acknowledge(
                    authUserId
                    , assignmentId
                )
        )
            .isInstanceOf(
                DeliveryAssignmentStateConflictException.class
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .acknowledgeIfAssigned(
                any()
                , any()
                , any()
                , any()
                , any()
            );

        verify(
            entityManager
            , never()
        )
            .refresh(any());
    }

    @Test
    @DisplayName("배송 접근 권한이 없으면 배정 확인을 거부한다")
    void acknowledgeThrowsForbiddenWhenRiderAccessIsNotAllowed() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(false);

        // when & then
        assertThatThrownBy(
            () ->
                riderAssignmentAcknowledgementService.acknowledge(
                    authUserId
                    , assignmentId
                )
        )
            .isInstanceOf(
                DeliveryAccessForbiddenException.class
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .findMineById(
                assignmentId
                , authUserId
            );
    }

    @Test
    @DisplayName("본인의 배정이 아니면 찾을 수 없는 배정으로 처리한다")
    void acknowledgeThrowsNotFoundWhenAssignmentIsNotMine() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                assignmentId
                , authUserId
            )
        )
            .thenReturn(
                Optional.empty()
            );

        // when & then
        assertThatThrownBy(
            () ->
                riderAssignmentAcknowledgementService.acknowledge(
                    authUserId
                    , assignmentId
                )
        )
            .isInstanceOf(
                DeliveryAssignmentNotFoundException.class
            );

        verify(
            deliveryGroupRepository
            , never()
        )
            .findByIdForUpdate(any());
    }

    @Test
    @DisplayName("배송 비활성 기사는 배정 확인을 할 수 없다")
    void acknowledgeThrowsForbiddenWhenRiderIsNotDeliveryActive() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;
        Long deliveryGroupId = 10L;

        DeliveryAssignment assignment =
            org.mockito.Mockito.mock(
                DeliveryAssignment.class
            );

        DeliveryGroup deliveryGroup =
            org.mockito.Mockito.mock(
                DeliveryGroup.class
            );

        Rider rider =
            org.mockito.Mockito.mock(
                Rider.class
            );

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                assignmentId
                , authUserId
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(deliveryGroup.getId())
            .thenReturn(deliveryGroupId);

        when(
            deliveryGroupRepository.findByIdForUpdate(
                deliveryGroupId
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(false);

        // when & then
        assertThatThrownBy(
            () ->
                riderAssignmentAcknowledgementService.acknowledge(
                    authUserId
                    , assignmentId
                )
        )
            .isInstanceOf(
                DeliveryAccessForbiddenException.class
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .acknowledgeIfAssigned(
                any()
                , any()
                , any()
                , any()
                , any()
            );
    }

    @Test
    @DisplayName("배송일이 오늘이 아니면 배정 확인을 거부한다")
    void acknowledgeThrowsStateConflictWhenDeliveryDateIsNotToday() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;
        Long deliveryGroupId = 10L;

        ZoneId kst =
            ZoneId.of("Asia/Seoul");

        LocalDateTime fixedNow =
            LocalDateTime.of(
                2026
                , 9
                , 5
                , 7
                , 30
            );

        DeliveryAssignment assignment =
            org.mockito.Mockito.mock(
                DeliveryAssignment.class
            );

        DeliveryGroup deliveryGroup =
            org.mockito.Mockito.mock(
                DeliveryGroup.class
            );

        Rider rider =
            org.mockito.Mockito.mock(
                Rider.class
            );

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                assignmentId
                , authUserId
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(deliveryGroup.getId())
            .thenReturn(deliveryGroupId);

        when(
            deliveryGroupRepository.findByIdForUpdate(
                deliveryGroupId
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(true);

        when(assignment.isAcknowledged())
            .thenReturn(false);

        when(assignment.isAssigned())
            .thenReturn(true);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(
                fixedNow.toLocalDate()
                    .plusDays(1)
            );

        try (
            MockedStatic<LocalDateTime> localDateTimeMock =
                mockStatic(LocalDateTime.class)
        ) {
            localDateTimeMock
                .when(
                    () ->
                        LocalDateTime.now(kst)
                )
                .thenReturn(fixedNow);

            // when & then
            assertThatThrownBy(
                () ->
                    riderAssignmentAcknowledgementService.acknowledge(
                        authUserId
                        , assignmentId
                    )
            )
                .isInstanceOf(
                    DeliveryAssignmentStateConflictException.class
                );
        }

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .acknowledgeIfAssigned(
                any()
                , any()
                , any()
                , any()
                , any()
            );
    }

    @Test
    @DisplayName("모든 기사 배정이 확인되면 그룹을 READY_TO_CONFIRM으로 변경한다")
    void acknowledgeChangesGroupToReadyToConfirmWhenAllAssignmentsAreAcknowledged() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;
        Long deliveryGroupId = 10L;

        ZoneId kst =
            ZoneId.of("Asia/Seoul");

        LocalDateTime fixedNow =
            LocalDateTime.of(
                2026
                , 9
                , 5
                , 7
                , 30
            );

        DeliveryAssignment assignment =
            org.mockito.Mockito.mock(
                DeliveryAssignment.class
            );

        DeliveryGroup deliveryGroup =
            org.mockito.Mockito.mock(
                DeliveryGroup.class
            );

        DeliverySlot slot =
            org.mockito.Mockito.mock(
                DeliverySlot.class
            );

        Rider rider =
            org.mockito.Mockito.mock(
                Rider.class
            );

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                assignmentId
                , authUserId
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(deliveryGroup.getId())
            .thenReturn(deliveryGroupId);

        when(
            deliveryGroupRepository.findByIdForUpdate(
                deliveryGroupId
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(true);

        when(assignment.isAcknowledged())
            .thenReturn(
                false
                , true
            );

        when(assignment.isAssigned())
            .thenReturn(true);

        when(assignment.getStatus())
            .thenReturn(
                DeliveryAssignmentStatus.ACKNOWLEDGED
            );

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(
                fixedNow.toLocalDate()
            );

        when(deliveryGroup.getSlot())
            .thenReturn(slot);

        when(slot.getCode())
            .thenReturn(
                DeliverySlotCode.LUNCH
            );

        when(
            deliveryAssignmentRepository.acknowledgeIfAssigned(
                assignmentId
                , authUserId
                , DeliveryAssignmentStatus.ASSIGNED
                , DeliveryAssignmentStatus.ACKNOWLEDGED
                , fixedNow
            )
        )
            .thenReturn(1);

        when(
            deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(
                deliveryGroupId
            )
        )
            .thenReturn(
                List.of(assignment)
            );

        when(deliveryGroup.isWaitingRider())
            .thenReturn(true);

        try (
            MockedStatic<LocalDateTime> localDateTimeMock =
                mockStatic(LocalDateTime.class)
        ) {
            localDateTimeMock
                .when(
                    () ->
                        LocalDateTime.now(kst)
                )
                .thenReturn(fixedNow);

            // when
            DeliveryAssignment result =
                riderAssignmentAcknowledgementService.acknowledge(
                    authUserId
                    , assignmentId
                );

            // then
            assertThat(result)
                .isEqualTo(assignment);
        }

        verify(deliveryAssignmentRepository)
            .acknowledgeIfAssigned(
                assignmentId
                , authUserId
                , DeliveryAssignmentStatus.ASSIGNED
                , DeliveryAssignmentStatus.ACKNOWLEDGED
                , fixedNow
            );

        verify(entityManager)
            .refresh(assignment);

        verify(deliveryGroup)
            .readyToConfirm();

        ArgumentCaptor<DeliveryGroupStatusHistory> historyCaptor =
            ArgumentCaptor.forClass(
                DeliveryGroupStatusHistory.class
            );

        verify(deliveryGroupStatusHistoryRepository)
            .save(
                historyCaptor.capture()
            );

        DeliveryGroupStatusHistory history =
            historyCaptor.getValue();

        assertThat(history.getDeliveryGroup())
            .isEqualTo(deliveryGroup);

        assertThat(history.getFromStatus())
            .isEqualTo(
                DeliveryGroupStatus.WAITING_RIDER
            );

        assertThat(history.getToStatus())
            .isEqualTo(
                DeliveryGroupStatus.READY_TO_CONFIRM
            );

        assertThat(history.getChangedBy())
            .isNull();

        assertThat(history.getChangedByType())
            .isEqualTo(
                DeliveryGroupChangedByType.SYSTEM
            );

        assertThat(history.getChangedAt())
            .isEqualTo(fixedNow);
    }

    @Test
    @DisplayName("다른 기사가 아직 확인하지 않았다면 그룹 상태를 변경하지 않는다")
    void acknowledgeDoesNotChangeGroupWhenOtherAssignmentIsNotAcknowledged() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;
        Long deliveryGroupId = 10L;

        ZoneId kst =
            ZoneId.of("Asia/Seoul");

        LocalDateTime fixedNow =
            LocalDateTime.of(
                2026
                , 9
                , 5
                , 7
                , 30
            );

        DeliveryAssignment assignment =
            org.mockito.Mockito.mock(
                DeliveryAssignment.class
            );

        DeliveryAssignment otherAssignment =
            org.mockito.Mockito.mock(
                DeliveryAssignment.class
            );

        DeliveryGroup deliveryGroup =
            org.mockito.Mockito.mock(
                DeliveryGroup.class
            );

        DeliverySlot slot =
            org.mockito.Mockito.mock(
                DeliverySlot.class
            );

        Rider rider =
            org.mockito.Mockito.mock(
                Rider.class
            );

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                assignmentId
                , authUserId
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(deliveryGroup.getId())
            .thenReturn(deliveryGroupId);

        when(
            deliveryGroupRepository.findByIdForUpdate(
                deliveryGroupId
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(true);

        when(assignment.isAcknowledged())
            .thenReturn(
                false
                , true
            );

        when(assignment.isAssigned())
            .thenReturn(true);

        when(assignment.getStatus())
            .thenReturn(
                DeliveryAssignmentStatus.ACKNOWLEDGED
            );

        when(otherAssignment.getStatus())
            .thenReturn(
                DeliveryAssignmentStatus.ASSIGNED
            );

        when(otherAssignment.isAcknowledged())
            .thenReturn(false);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(
                fixedNow.toLocalDate()
            );

        when(deliveryGroup.getSlot())
            .thenReturn(slot);

        when(slot.getCode())
            .thenReturn(
                DeliverySlotCode.LUNCH
            );

        when(
            deliveryAssignmentRepository.acknowledgeIfAssigned(
                assignmentId
                , authUserId
                , DeliveryAssignmentStatus.ASSIGNED
                , DeliveryAssignmentStatus.ACKNOWLEDGED
                , fixedNow
            )
        )
            .thenReturn(1);

        when(
            deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(
                deliveryGroupId
            )
        )
            .thenReturn(
                List.of(
                    assignment
                    , otherAssignment
                )
            );

        try (
            MockedStatic<LocalDateTime> localDateTimeMock =
                mockStatic(LocalDateTime.class)
        ) {
            localDateTimeMock
                .when(
                    () ->
                        LocalDateTime.now(kst)
                )
                .thenReturn(fixedNow);

            // when
            DeliveryAssignment result =
                riderAssignmentAcknowledgementService.acknowledge(
                    authUserId
                    , assignmentId
                );

            // then
            assertThat(result)
                .isEqualTo(assignment);
        }

        verify(entityManager)
            .refresh(assignment);

        verify(
            deliveryGroup
            , never()
        )
            .readyToConfirm();

        verify(
            deliveryGroupStatusHistoryRepository
            , never()
        )
            .save(any());
    }

    @Test
    @DisplayName("점심 배정은 07시 이전에 확인할 수 없다")
    void acknowledgeThrowsStateConflictBeforeLunchAcknowledgementStart() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;
        Long deliveryGroupId = 10L;

        ZoneId kst =
            ZoneId.of("Asia/Seoul");

        LocalDateTime fixedNow =
            LocalDateTime.of(
                2026
                , 9
                , 5
                , 6
                , 59
            );

        DeliveryAssignment assignment =
            org.mockito.Mockito.mock(
                DeliveryAssignment.class
            );

        DeliveryGroup deliveryGroup =
            org.mockito.Mockito.mock(
                DeliveryGroup.class
            );

        DeliverySlot slot =
            org.mockito.Mockito.mock(
                DeliverySlot.class
            );

        Rider rider =
            org.mockito.Mockito.mock(
                Rider.class
            );

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                assignmentId
                , authUserId
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(deliveryGroup.getId())
            .thenReturn(deliveryGroupId);

        when(
            deliveryGroupRepository.findByIdForUpdate(
                deliveryGroupId
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(true);

        when(assignment.isAcknowledged())
            .thenReturn(false);

        when(assignment.isAssigned())
            .thenReturn(true);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(
                fixedNow.toLocalDate()
            );

        when(deliveryGroup.getSlot())
            .thenReturn(slot);

        when(slot.getCode())
            .thenReturn(
                DeliverySlotCode.LUNCH
            );

        try (
            MockedStatic<LocalDateTime> localDateTimeMock =
                mockStatic(LocalDateTime.class)
        ) {
            localDateTimeMock
                .when(
                    () ->
                        LocalDateTime.now(kst)
                )
                .thenReturn(fixedNow);

            // when & then
            assertThatThrownBy(
                () ->
                    riderAssignmentAcknowledgementService.acknowledge(
                        authUserId
                        , assignmentId
                    )
            )
                .isInstanceOf(
                    DeliveryAssignmentStateConflictException.class
                );
        }

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .acknowledgeIfAssigned(
                any()
                , any()
                , any()
                , any()
                , any()
            );
    }

    @Test
    @DisplayName("저녁 배정은 13시 이전에 확인할 수 없다")
    void acknowledgeThrowsStateConflictBeforeDinnerAcknowledgementStart() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;
        Long deliveryGroupId = 10L;

        ZoneId kst =
            ZoneId.of("Asia/Seoul");

        LocalDateTime fixedNow =
            LocalDateTime.of(
                2026
                , 9
                , 5
                , 12
                , 59
            );

        DeliveryAssignment assignment =
            org.mockito.Mockito.mock(
                DeliveryAssignment.class
            );

        DeliveryGroup deliveryGroup =
            org.mockito.Mockito.mock(
                DeliveryGroup.class
            );

        DeliverySlot slot =
            org.mockito.Mockito.mock(
                DeliverySlot.class
            );

        Rider rider =
            org.mockito.Mockito.mock(
                Rider.class
            );

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                assignmentId
                , authUserId
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(deliveryGroup.getId())
            .thenReturn(deliveryGroupId);

        when(
            deliveryGroupRepository.findByIdForUpdate(
                deliveryGroupId
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(true);

        when(assignment.isAcknowledged())
            .thenReturn(false);

        when(assignment.isAssigned())
            .thenReturn(true);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(
                fixedNow.toLocalDate()
            );

        when(deliveryGroup.getSlot())
            .thenReturn(slot);

        when(slot.getCode())
            .thenReturn(
                DeliverySlotCode.DINNER
            );

        try (
            MockedStatic<LocalDateTime> localDateTimeMock =
                mockStatic(LocalDateTime.class)
        ) {
            localDateTimeMock
                .when(
                    () ->
                        LocalDateTime.now(kst)
                )
                .thenReturn(fixedNow);

            // when & then
            assertThatThrownBy(
                () ->
                    riderAssignmentAcknowledgementService.acknowledge(
                        authUserId
                        , assignmentId
                    )
            )
                .isInstanceOf(
                    DeliveryAssignmentStateConflictException.class
                );
        }

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .acknowledgeIfAssigned(
                any()
                , any()
                , any()
                , any()
                , any()
            );
    }

    @Test
    @DisplayName("동시 요청으로 이미 확인 처리됐다면 최신 상태를 읽고 정상 반환한다")
    void acknowledgeReturnsLatestAssignmentWhenConditionalUpdateAffectsNoRowsButAlreadyAcknowledged() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;
        Long deliveryGroupId = 10L;

        ZoneId kst =
            ZoneId.of("Asia/Seoul");

        LocalDateTime fixedNow =
            LocalDateTime.of(
                2026
                , 9
                , 5
                , 7
                , 30
            );

        DeliveryAssignment assignment =
            org.mockito.Mockito.mock(
                DeliveryAssignment.class
            );

        DeliveryGroup deliveryGroup =
            org.mockito.Mockito.mock(
                DeliveryGroup.class
            );

        DeliverySlot slot =
            org.mockito.Mockito.mock(
                DeliverySlot.class
            );

        Rider rider =
            org.mockito.Mockito.mock(
                Rider.class
            );

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                assignmentId
                , authUserId
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(deliveryGroup.getId())
            .thenReturn(deliveryGroupId);

        when(
            deliveryGroupRepository.findByIdForUpdate(
                deliveryGroupId
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(true);

        when(assignment.isAcknowledged())
            .thenReturn(
                false
                , true
            );

        when(assignment.isAssigned())
            .thenReturn(true);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(
                fixedNow.toLocalDate()
            );

        when(deliveryGroup.getSlot())
            .thenReturn(slot);

        when(slot.getCode())
            .thenReturn(
                DeliverySlotCode.LUNCH
            );

        when(
            deliveryAssignmentRepository.acknowledgeIfAssigned(
                assignmentId
                , authUserId
                , DeliveryAssignmentStatus.ASSIGNED
                , DeliveryAssignmentStatus.ACKNOWLEDGED
                , fixedNow
            )
        )
            .thenReturn(0);

        try (
            MockedStatic<LocalDateTime> localDateTimeMock =
                mockStatic(LocalDateTime.class)
        ) {
            localDateTimeMock
                .when(
                    () ->
                        LocalDateTime.now(kst)
                )
                .thenReturn(fixedNow);

            // when
            DeliveryAssignment result =
                riderAssignmentAcknowledgementService.acknowledge(
                    authUserId
                    , assignmentId
                );

            // then
            assertThat(result)
                .isEqualTo(assignment);
        }

        verify(entityManager)
            .refresh(assignment);

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .findAllByDeliveryGroupIdForUpdate(
                deliveryGroupId
            );

        verify(
            deliveryGroup
            , never()
        )
            .readyToConfirm();

        verify(
            deliveryGroupStatusHistoryRepository
            , never()
        )
            .save(any());
    }

    @Test
    @DisplayName("조건부 수정에 실패하고 최신 상태도 확인 완료가 아니면 상태 충돌로 처리한다")
    void acknowledgeThrowsStateConflictWhenConditionalUpdateAffectsNoRowsAndLatestStateIsNotAcknowledged() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;
        Long deliveryGroupId = 10L;

        ZoneId kst =
            ZoneId.of("Asia/Seoul");

        LocalDateTime fixedNow =
            LocalDateTime.of(
                2026
                , 9
                , 5
                , 7
                , 30
            );

        DeliveryAssignment assignment =
            org.mockito.Mockito.mock(
                DeliveryAssignment.class
            );

        DeliveryGroup deliveryGroup =
            org.mockito.Mockito.mock(
                DeliveryGroup.class
            );

        DeliverySlot slot =
            org.mockito.Mockito.mock(
                DeliverySlot.class
            );

        Rider rider =
            org.mockito.Mockito.mock(
                Rider.class
            );

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                assignmentId
                , authUserId
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(deliveryGroup.getId())
            .thenReturn(deliveryGroupId);

        when(
            deliveryGroupRepository.findByIdForUpdate(
                deliveryGroupId
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(true);

        when(assignment.isAcknowledged())
            .thenReturn(false);

        when(assignment.isAssigned())
            .thenReturn(true);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(
                fixedNow.toLocalDate()
            );

        when(deliveryGroup.getSlot())
            .thenReturn(slot);

        when(slot.getCode())
            .thenReturn(
                DeliverySlotCode.LUNCH
            );

        when(
            deliveryAssignmentRepository.acknowledgeIfAssigned(
                assignmentId
                , authUserId
                , DeliveryAssignmentStatus.ASSIGNED
                , DeliveryAssignmentStatus.ACKNOWLEDGED
                , fixedNow
            )
        )
            .thenReturn(0);

        try (
            MockedStatic<LocalDateTime> localDateTimeMock =
                mockStatic(LocalDateTime.class)
        ) {
            localDateTimeMock
                .when(
                    () ->
                        LocalDateTime.now(kst)
                )
                .thenReturn(fixedNow);

            // when & then
            assertThatThrownBy(
                () ->
                    riderAssignmentAcknowledgementService.acknowledge(
                        authUserId
                        , assignmentId
                    )
            )
                .isInstanceOf(
                    DeliveryAssignmentStateConflictException.class
                );
        }

        verify(entityManager)
            .refresh(assignment);

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .findAllByDeliveryGroupIdForUpdate(
                deliveryGroupId
            );

        verify(
            deliveryGroupStatusHistoryRepository
            , never()
        )
            .save(any());
    }
}