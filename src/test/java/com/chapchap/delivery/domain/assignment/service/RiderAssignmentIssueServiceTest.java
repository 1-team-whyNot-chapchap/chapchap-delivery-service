package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueCode;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentIssue;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentIssueRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroupStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentStateConflictException;
import com.chapchap.delivery.global.exception.business.InvalidAssignmentIssueReasonException;
import com.chapchap.delivery.global.exception.business.OtherReasonDetailRequiredException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderAssignmentIssueServiceTest {

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private DeliveryAssignmentIssueRepository deliveryAssignmentIssueRepository;

    @Mock
    private DeliveryGroupRepository deliveryGroupRepository;

    @Mock
    private DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;

    @Mock
    private DeliveryAccessService deliveryAccessService;

    @Mock
    private EntityManager entityManager;

    private RiderAssignmentIssueService riderAssignmentIssueService;

    @BeforeEach
    void setUp() {
        riderAssignmentIssueService =
            new RiderAssignmentIssueService(
                deliveryAssignmentRepository
                , deliveryAssignmentIssueRepository
                , deliveryGroupRepository
                , deliveryGroupStatusHistoryRepository
                , deliveryAccessService
                , entityManager
            );
    }

    @Test
    @DisplayName("기사가 배정 이슈를 제기하면 Assignment와 Group 상태를 변경한다")
    void reportIssueChangesAssignmentAndGroupToIssueReview() {
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
                , 40
            );

        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        Rider rider =
            mock(Rider.class);

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

        when(assignment.canReportIssue())
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.reportIssueIfReportable(
                assignmentId
                , DeliveryAssignmentStatus.ASSIGNED
                , DeliveryAssignmentStatus.ACKNOWLEDGED
                , DeliveryAssignmentStatus.ISSUE_REPORTED
            )
        )
            .thenReturn(1);

        when(deliveryGroup.isIssueReview())
            .thenReturn(false);

        when(deliveryGroup.getStatus())
            .thenReturn(
                DeliveryGroupStatus.WAITING_RIDER
            );

        when(
            deliveryAssignmentIssueRepository.save(
                any(DeliveryAssignmentIssue.class)
            )
        )
            .thenAnswer(
                invocation ->
                    invocation.getArgument(0)
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
            DeliveryAssignmentIssue result =
                riderAssignmentIssueService.reportIssue(
                    authUserId
                    , assignmentId
                    , DeliveryAssignmentIssueCode.SCHEDULE_CONFLICT
                    , "당일 일정 충돌"
                );

            // then
            assertThat(result)
                .isNotNull();
        }

        verify(deliveryAssignmentRepository)
            .reportIssueIfReportable(
                assignmentId
                , DeliveryAssignmentStatus.ASSIGNED
                , DeliveryAssignmentStatus.ACKNOWLEDGED
                , DeliveryAssignmentStatus.ISSUE_REPORTED
            );

        verify(entityManager)
            .refresh(assignment);

        verify(deliveryGroup)
            .issueReview();

        ArgumentCaptor<DeliveryAssignmentIssue> issueCaptor =
            ArgumentCaptor.forClass(
                DeliveryAssignmentIssue.class
            );

        verify(deliveryAssignmentIssueRepository)
            .save(
                issueCaptor.capture()
            );

        DeliveryAssignmentIssue savedIssue =
            issueCaptor.getValue();

        assertThat(savedIssue.getAssignment())
            .isEqualTo(assignment);

        assertThat(savedIssue.getIssueCode())
            .isEqualTo(
                DeliveryAssignmentIssueCode.SCHEDULE_CONFLICT
            );

        assertThat(savedIssue.getIssueDetail())
            .isEqualTo(
                "당일 일정 충돌"
            );

        assertThat(savedIssue.getReportedBy())
            .isEqualTo(authUserId);

        assertThat(savedIssue.getReportedAt())
            .isEqualTo(fixedNow);

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
                DeliveryGroupStatus.ISSUE_REVIEW
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
    @DisplayName("OTHER 이슈는 상세 사유가 없으면 거부한다")
    void reportIssueThrowsWhenOtherReasonDetailIsMissing() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        // when & then
        assertThatThrownBy(
            () ->
                riderAssignmentIssueService.reportIssue(
                    authUserId
                    , assignmentId
                    , DeliveryAssignmentIssueCode.OTHER
                    , " "
                )
        )
            .isInstanceOf(
                OtherReasonDetailRequiredException.class
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .findMineById(
                assignmentId
                , authUserId
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .reportIssueIfReportable(
                any()
                , any()
                , any()
                , any()
            );

        verify(
            deliveryAssignmentIssueRepository
            , never()
        )
            .save(any());
    }

    @Test
    @DisplayName("이슈 코드를 지정하지 않으면 거부한다")
    void reportIssueThrowsWhenIssueCodeIsNull() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        // when & then
        assertThatThrownBy(
            () ->
                riderAssignmentIssueService.reportIssue(
                    authUserId
                    , assignmentId
                    , null
                    , "사유"
                )
        )
            .isInstanceOf(
                InvalidAssignmentIssueReasonException.class
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .findMineById(
                assignmentId
                , authUserId
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .reportIssueIfReportable(
                any()
                , any()
                , any()
                , any()
            );

        verify(
            deliveryAssignmentIssueRepository
            , never()
        )
            .save(any());
    }

    @Test
    @DisplayName("이슈를 제기할 수 없는 Assignment 상태면 거부한다")
    void reportIssueThrowsStateConflictWhenAssignmentCannotReportIssue() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;
        Long deliveryGroupId = 10L;

        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        Rider rider =
            mock(Rider.class);

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

        when(assignment.canReportIssue())
            .thenReturn(false);

        // when & then
        assertThatThrownBy(
            () ->
                riderAssignmentIssueService.reportIssue(
                    authUserId
                    , assignmentId
                    , DeliveryAssignmentIssueCode.SCHEDULE_CONFLICT
                    , "당일 일정 충돌"
                )
        )
            .isInstanceOf(
                DeliveryAssignmentStateConflictException.class
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .reportIssueIfReportable(
                any()
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
            deliveryAssignmentIssueRepository
            , never()
        )
            .save(any());

        verify(
            deliveryGroup
            , never()
        )
            .issueReview();
    }

    @Test
    @DisplayName("그룹이 이미 ISSUE_REVIEW면 그룹 상태 이력을 중복 생성하지 않는다")
    void reportIssueDoesNotCreateDuplicateGroupHistoryWhenGroupIsAlreadyIssueReview() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;
        Long deliveryGroupId = 10L;

        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        Rider rider =
            mock(Rider.class);

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

        when(assignment.canReportIssue())
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.reportIssueIfReportable(
                assignmentId
                , DeliveryAssignmentStatus.ASSIGNED
                , DeliveryAssignmentStatus.ACKNOWLEDGED
                , DeliveryAssignmentStatus.ISSUE_REPORTED
            )
        )
            .thenReturn(1);

        when(deliveryGroup.isIssueReview())
            .thenReturn(true);

        when(
            deliveryAssignmentIssueRepository.save(
                any(DeliveryAssignmentIssue.class)
            )
        )
            .thenAnswer(
                invocation ->
                    invocation.getArgument(0)
            );

        // when
        DeliveryAssignmentIssue result =
            riderAssignmentIssueService.reportIssue(
                authUserId
                , assignmentId
                , DeliveryAssignmentIssueCode.CAPACITY_CONCERN
                , "배정 물량이 많습니다."
            );

        // then
        assertThat(result)
            .isNotNull();

        verify(deliveryAssignmentRepository)
            .reportIssueIfReportable(
                assignmentId
                , DeliveryAssignmentStatus.ASSIGNED
                , DeliveryAssignmentStatus.ACKNOWLEDGED
                , DeliveryAssignmentStatus.ISSUE_REPORTED
            );

        verify(entityManager)
            .refresh(assignment);

        verify(deliveryAssignmentIssueRepository)
            .save(
                any(DeliveryAssignmentIssue.class)
            );

        verify(
            deliveryGroup
            , never()
        )
            .issueReview();

        verify(
            deliveryGroupStatusHistoryRepository
            , never()
        )
            .save(any());
    }

    @Test
    @DisplayName("배송 비활성 기사는 이슈를 제기할 수 없다")
    void reportIssueThrowsForbiddenWhenRiderIsNotDeliveryActive() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;
        Long deliveryGroupId = 10L;

        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        Rider rider =
            mock(Rider.class);

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
                riderAssignmentIssueService.reportIssue(
                    authUserId
                    , assignmentId
                    , DeliveryAssignmentIssueCode.SCHEDULE_CONFLICT
                    , "당일 일정 충돌"
                )
        )
            .isInstanceOf(
                DeliveryAccessForbiddenException.class
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .reportIssueIfReportable(
                any()
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
            deliveryAssignmentIssueRepository
            , never()
        )
            .save(any());
    }

    @Test
    @DisplayName("조건부 UPDATE가 0건이면 다른 상태 변경이 먼저 처리된 것으로 보고 거부한다")
    void reportIssueThrowsStateConflictWhenConditionalUpdateAffectsNoRows() {
        // given
        Long authUserId = 100L;
        Long assignmentId = 1L;
        Long deliveryGroupId = 10L;

        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        Rider rider =
            mock(Rider.class);

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

        when(assignment.canReportIssue())
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.reportIssueIfReportable(
                assignmentId
                , DeliveryAssignmentStatus.ASSIGNED
                , DeliveryAssignmentStatus.ACKNOWLEDGED
                , DeliveryAssignmentStatus.ISSUE_REPORTED
            )
        )
            .thenReturn(0);

        // when & then
        assertThatThrownBy(
            () ->
                riderAssignmentIssueService.reportIssue(
                    authUserId
                    , assignmentId
                    , DeliveryAssignmentIssueCode.SCHEDULE_CONFLICT
                    , "당일 일정 충돌"
                )
        )
            .isInstanceOf(
                DeliveryAssignmentStateConflictException.class
            );

        verify(entityManager)
            .refresh(assignment);

        verify(
            deliveryAssignmentIssueRepository
            , never()
        )
            .save(any());

        verify(
            deliveryGroup
            , never()
        )
            .issueReview();

        verify(
            deliveryGroupStatusHistoryRepository
            , never()
        )
            .save(any());
    }
}