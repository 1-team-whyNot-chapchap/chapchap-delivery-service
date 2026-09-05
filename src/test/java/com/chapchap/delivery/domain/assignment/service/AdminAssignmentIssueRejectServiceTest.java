package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueResolution;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentIssue;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentIssueRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.audit.constant.AuditActorType;
import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroupStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentStateConflictException;
import com.chapchap.delivery.global.exception.business.InvalidAssignmentIssueReasonException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAssignmentIssueRejectServiceTest {

    private static final Long ACTOR_ID = 90001L;
    private static final Long ISSUE_ID = 1L;
    private static final Long ASSIGNMENT_ID = 10L;
    private static final Long DELIVERY_GROUP_ID = 100L;

    @Mock
    private DeliveryAssignmentIssueRepository deliveryAssignmentIssueRepository;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private DeliveryGroupRepository deliveryGroupRepository;

    @Mock
    private DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;

    @Mock
    private AuditHistoryRepository auditHistoryRepository;

    @Mock
    private DeliveryAccessService deliveryAccessService;

    @Mock
    private EntityManager entityManager;

    private AdminAssignmentIssueRejectService adminAssignmentIssueRejectService;

    @BeforeEach
    void setUp() {
        adminAssignmentIssueRejectService =
            new AdminAssignmentIssueRejectService(
                deliveryAssignmentIssueRepository
                , deliveryAssignmentRepository
                , deliveryGroupRepository
                , deliveryGroupStatusHistoryRepository
                , auditHistoryRepository
                , deliveryAccessService
                , entityManager
            );
    }

    @Test
    @DisplayName("마지막 미해결 이슈를 반려하면 Assignment를 ASSIGNED로 복귀시키고 Group을 WAITING_RIDER로 변경한다")
    void rejectIssueReturnsAssignmentAndGroupToWaitingStateWhenLastIssueIsResolved() {
        // given
        DeliveryAssignmentIssue issue =
            mock(DeliveryAssignmentIssue.class);

        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        when(
            deliveryAssignmentIssueRepository.findByIdAndDeletedAtIsNull(
                ISSUE_ID
            )
        )
            .thenReturn(
                Optional.of(issue)
            );

        when(issue.getAssignment())
            .thenReturn(assignment);

        when(issue.getId())
            .thenReturn(ISSUE_ID);

        when(assignment.getId())
            .thenReturn(ASSIGNMENT_ID);

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(deliveryGroup.getId())
            .thenReturn(DELIVERY_GROUP_ID);

        when(
            deliveryGroupRepository.findByIdForUpdate(
                DELIVERY_GROUP_ID
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        when(
            deliveryAssignmentIssueRepository.resolveIfUnresolved(
                eq(ISSUE_ID)
                , eq(DeliveryAssignmentIssueResolution.REJECTED)
                , eq(ACTOR_ID)
                , any()
            )
        )
            .thenReturn(1);

        when(
            deliveryAssignmentRepository.rejectIssueIfReported(
                ASSIGNMENT_ID
                , DeliveryAssignmentStatus.ISSUE_REPORTED
                , DeliveryAssignmentStatus.ASSIGNED
            )
        )
            .thenReturn(1);

        when(
            deliveryAssignmentIssueRepository.countUnresolvedByDeliveryGroupId(
                DELIVERY_GROUP_ID
            )
        )
            .thenReturn(0L);

        when(deliveryGroup.isIssueReview())
            .thenReturn(true);

        when(deliveryGroup.getStatus())
            .thenReturn(
                DeliveryGroupStatus.ISSUE_REVIEW
            );

        // when
        DeliveryAssignmentIssue result =
            adminAssignmentIssueRejectService.rejectIssue(
                ACTOR_ID
                , UserRole.ADMIN
                , ISSUE_ID
                , "  배정 유지가 가능하다고 판단했습니다.  "
            );

        // then
        assertThat(result)
            .isSameAs(issue);

        verify(deliveryGroup)
            .returnToWaitingRider();

        ArgumentCaptor<DeliveryGroupStatusHistory> statusHistoryCaptor =
            ArgumentCaptor.forClass(
                DeliveryGroupStatusHistory.class
            );

        verify(deliveryGroupStatusHistoryRepository)
            .save(
                statusHistoryCaptor.capture()
            );

        DeliveryGroupStatusHistory statusHistory =
            statusHistoryCaptor.getValue();

        assertThat(statusHistory.getDeliveryGroup())
            .isSameAs(deliveryGroup);

        assertThat(statusHistory.getFromStatus())
            .isEqualTo(
                DeliveryGroupStatus.ISSUE_REVIEW
            );

        assertThat(statusHistory.getToStatus())
            .isEqualTo(
                DeliveryGroupStatus.WAITING_RIDER
            );

        assertThat(statusHistory.getChangedBy())
            .isNull();

        assertThat(statusHistory.getChangedByType())
            .isEqualTo(
                DeliveryGroupChangedByType.SYSTEM
            );

        ArgumentCaptor<AuditHistory> auditHistoryCaptor =
            ArgumentCaptor.forClass(
                AuditHistory.class
            );

        verify(auditHistoryRepository)
            .save(
                auditHistoryCaptor.capture()
            );

        AuditHistory auditHistory =
            auditHistoryCaptor.getValue();

        assertThat(auditHistory.getEntityType())
            .isEqualTo(
                "DELIVERY_ASSIGNMENT_ISSUE"
            );

        assertThat(auditHistory.getEntityId())
            .isEqualTo(ISSUE_ID);

        assertThat(auditHistory.getAction())
            .isEqualTo(
                "ASSIGNMENT_ISSUE_REJECTED"
            );

        assertThat(auditHistory.getActorId())
            .isEqualTo(ACTOR_ID);

        assertThat(auditHistory.getActorType())
            .isEqualTo(
                AuditActorType.ADMIN
            );

        assertThat(auditHistory.getReasonCode())
            .isEqualTo(
                "REJECTED"
            );

        assertThat(auditHistory.getReasonDetail())
            .isEqualTo(
                "배정 유지가 가능하다고 판단했습니다."
            );

        assertThat(auditHistory.getBeforeValueJson())
            .isEqualTo(
                "{\"resolution\":null,\"assignmentStatus\":\"ISSUE_REPORTED\"}"
            );

        assertThat(auditHistory.getAfterValueJson())
            .isEqualTo(
                "{\"resolution\":\"REJECTED\",\"assignmentStatus\":\"ASSIGNED\"}"
            );

        verify(entityManager)
            .refresh(issue);

        verify(entityManager)
            .refresh(assignment);
    }

    @Test
    @DisplayName("다른 미해결 이슈가 남아 있으면 Group은 ISSUE_REVIEW를 유지한다")
    void rejectIssueKeepsIssueReviewWhenAnotherUnresolvedIssueExists() {
        // given
        DeliveryAssignmentIssue issue =
            mockIssueAndLockedGroup();

        DeliveryAssignment assignment =
            issue.getAssignment();

        when(issue.getId())
            .thenReturn(ISSUE_ID);

        when(assignment.getId())
            .thenReturn(ASSIGNMENT_ID);

        when(
            deliveryAssignmentIssueRepository.resolveIfUnresolved(
                eq(ISSUE_ID)
                , eq(DeliveryAssignmentIssueResolution.REJECTED)
                , eq(ACTOR_ID)
                , any()
            )
        )
            .thenReturn(1);

        when(
            deliveryAssignmentRepository.rejectIssueIfReported(
                ASSIGNMENT_ID
                , DeliveryAssignmentStatus.ISSUE_REPORTED
                , DeliveryAssignmentStatus.ASSIGNED
            )
        )
            .thenReturn(1);

        when(
            deliveryAssignmentIssueRepository.countUnresolvedByDeliveryGroupId(
                DELIVERY_GROUP_ID
            )
        )
            .thenReturn(1L);

        // when
        adminAssignmentIssueRejectService.rejectIssue(
            ACTOR_ID
            , UserRole.ADMIN
            , ISSUE_ID
            , "기존 배정을 유지합니다."
        );

        // then
        DeliveryGroup deliveryGroup =
            assignment.getDeliveryGroup();

        verify(
            deliveryGroup
            , never()
        )
            .returnToWaitingRider();

        verify(
            deliveryGroupStatusHistoryRepository
            , never()
        )
            .save(
                any()
            );

        verify(auditHistoryRepository)
            .save(
                any(AuditHistory.class)
            );

        verify(entityManager)
            .refresh(issue);

        verify(entityManager)
            .refresh(assignment);
    }

    @Test
    @DisplayName("이미 해결된 이슈를 다시 반려하면 상태 충돌이 발생한다")
    void rejectIssueThrowsConflictWhenIssueIsAlreadyResolved() {
        // given
        DeliveryAssignmentIssue issue =
            mockIssueAndLockedGroup();

        when(
            deliveryAssignmentIssueRepository.resolveIfUnresolved(
                eq(ISSUE_ID)
                , eq(DeliveryAssignmentIssueResolution.REJECTED)
                , eq(ACTOR_ID)
                , any()
            )
        )
            .thenReturn(0);

        // when & then
        assertThatThrownBy(
            () ->
                adminAssignmentIssueRejectService.rejectIssue(
                    ACTOR_ID
                    , UserRole.ADMIN
                    , ISSUE_ID
                    , "반려합니다."
                )
        )
            .isInstanceOf(
                DeliveryAssignmentStateConflictException.class
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .rejectIssueIfReported(
                any()
                , any()
                , any()
            );

        verify(
            deliveryAssignmentIssueRepository
            , never()
        )
            .countUnresolvedByDeliveryGroupId(
                any()
            );

        verify(
            auditHistoryRepository
            , never()
        )
            .save(
                any()
            );

        verify(
            entityManager
            , never()
        )
            .refresh(
                any()
            );
    }

    @Test
    @DisplayName("Issue 해결 후 Assignment 상태가 이미 바뀌었다면 상태 충돌이 발생한다")
    void rejectIssueThrowsConflictWhenAssignmentStateChangedConcurrently() {
        // given
        DeliveryAssignmentIssue issue =
            mockIssueAndLockedGroup();

        DeliveryAssignment assignment =
            issue.getAssignment();

        when(assignment.getId())
            .thenReturn(ASSIGNMENT_ID);

        when(
            deliveryAssignmentIssueRepository.resolveIfUnresolved(
                eq(ISSUE_ID)
                , eq(DeliveryAssignmentIssueResolution.REJECTED)
                , eq(ACTOR_ID)
                , any()
            )
        )
            .thenReturn(1);

        when(
            deliveryAssignmentRepository.rejectIssueIfReported(
                ASSIGNMENT_ID
                , DeliveryAssignmentStatus.ISSUE_REPORTED
                , DeliveryAssignmentStatus.ASSIGNED
            )
        )
            .thenReturn(0);

        // when & then
        assertThatThrownBy(
            () ->
                adminAssignmentIssueRejectService.rejectIssue(
                    ACTOR_ID
                    , UserRole.ADMIN
                    , ISSUE_ID
                    , "반려합니다."
                )
        )
            .isInstanceOf(
                DeliveryAssignmentStateConflictException.class
            );

        verify(
            deliveryAssignmentIssueRepository
            , never()
        )
            .countUnresolvedByDeliveryGroupId(
                any()
            );

        verify(
            deliveryGroupStatusHistoryRepository
            , never()
        )
            .save(
                any()
            );

        verify(
            auditHistoryRepository
            , never()
        )
            .save(
                any()
            );

        verify(
            entityManager
            , never()
        )
            .refresh(
                any()
            );
    }

    @Test
    @DisplayName("이슈가 존재하지 않으면 반려할 수 없다")
    void rejectIssueThrowsNotFoundWhenIssueDoesNotExist() {
        // given
        when(
            deliveryAssignmentIssueRepository.findByIdAndDeletedAtIsNull(
                ISSUE_ID
            )
        )
            .thenReturn(
                Optional.empty()
            );

        // when & then
        assertThatThrownBy(
            () ->
                adminAssignmentIssueRejectService.rejectIssue(
                    ACTOR_ID
                    , UserRole.ADMIN
                    , ISSUE_ID
                    , "반려합니다."
                )
        )
            .isInstanceOf(
                DeliveryAssignmentNotFoundException.class
            );

        verify(
            deliveryGroupRepository
            , never()
        )
            .findByIdForUpdate(
                any()
            );

        verify(
            deliveryAssignmentIssueRepository
            , never()
        )
            .resolveIfUnresolved(
                any()
                , any()
                , any()
                , any()
            );

        verify(
            auditHistoryRepository
            , never()
        )
            .save(
                any()
            );
    }

    @Test
    @DisplayName("관리자 접근 권한이 없으면 이슈를 반려할 수 없다")
    void rejectIssueThrowsForbiddenWhenAdminAccessIsDenied() {
        // given
        doThrow(
            new DeliveryAccessForbiddenException()
        )
            .when(deliveryAccessService)
            .validateAdminAccess(
                ACTOR_ID
                , UserRole.RIDER
            );

        // when & then
        assertThatThrownBy(
            () ->
                adminAssignmentIssueRejectService.rejectIssue(
                    ACTOR_ID
                    , UserRole.RIDER
                    , ISSUE_ID
                    , "반려합니다."
                )
        )
            .isInstanceOf(
                DeliveryAccessForbiddenException.class
            );

        verify(
            deliveryAssignmentIssueRepository
            , never()
        )
            .findByIdAndDeletedAtIsNull(
                any()
            );

        verify(
            deliveryGroupRepository
            , never()
        )
            .findByIdForUpdate(
                any()
            );

        verify(
            auditHistoryRepository
            , never()
        )
            .save(
                any()
            );
    }

    @Test
    @DisplayName("반려 설명이 공백이면 이슈를 반려할 수 없다")
    void rejectIssueThrowsWhenReasonDetailIsBlank() {
        // when & then
        assertThatThrownBy(
            () ->
                adminAssignmentIssueRejectService.rejectIssue(
                    ACTOR_ID
                    , UserRole.ADMIN
                    , ISSUE_ID
                    , "   "
                )
        )
            .isInstanceOf(
                InvalidAssignmentIssueReasonException.class
            );

        verify(
            deliveryAssignmentIssueRepository
            , never()
        )
            .findByIdAndDeletedAtIsNull(
                any()
            );

        verify(
            deliveryGroupRepository
            , never()
        )
            .findByIdForUpdate(
                any()
            );

        verify(
            auditHistoryRepository
            , never()
        )
            .save(
                any()
            );
    }

    private DeliveryAssignmentIssue mockIssueAndLockedGroup() {
        DeliveryAssignmentIssue issue =
            mock(DeliveryAssignmentIssue.class);

        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        when(
            deliveryAssignmentIssueRepository.findByIdAndDeletedAtIsNull(
                ISSUE_ID
            )
        )
            .thenReturn(
                Optional.of(issue)
            );

        when(issue.getAssignment())
            .thenReturn(assignment);

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(deliveryGroup.getId())
            .thenReturn(DELIVERY_GROUP_ID);

        when(
            deliveryGroupRepository.findByIdForUpdate(
                DELIVERY_GROUP_ID
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        return issue;
    }
}