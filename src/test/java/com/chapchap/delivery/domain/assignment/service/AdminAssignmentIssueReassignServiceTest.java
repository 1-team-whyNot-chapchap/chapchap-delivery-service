package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueResolution;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentType;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentIssue;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentIssueRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.AssignmentConditionNotMetException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentStateConflictException;
import com.chapchap.delivery.global.exception.business.DeliveryCapacityExceededException;
import com.chapchap.delivery.global.exception.business.OtherReasonDetailRequiredException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAssignmentIssueReassignServiceTest {

    private static final Long ACTOR_ID = 9001L;
    private static final Long ISSUE_ID = 100L;
    private static final Long DELIVERY_GROUP_ID = 200L;
    private static final Long ORIGINAL_ASSIGNMENT_ID = 300L;
    private static final Long ORIGINAL_RIDER_ID = 10L;
    private static final Long NEW_RIDER_ID = 20L;
    private static final Long NEW_ASSIGNMENT_ID = 400L;
    private static final Long DELIVERY_ID = 1000L;

    private static final LocalDate DELIVERY_DATE =
        LocalDate.of(
            2026
            , 9
            , 6
        );

    @Mock
    private DeliveryAssignmentIssueRepository deliveryAssignmentIssueRepository;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private DeliveryAssignmentItemRepository deliveryAssignmentItemRepository;

    @Mock
    private DeliveryGroupRepository deliveryGroupRepository;

    @Mock
    private DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private RiderRepository riderRepository;

    @Mock
    private AuditHistoryRepository auditHistoryRepository;

    @Mock
    private DeliveryAccessService deliveryAccessService;

    @Mock
    private RiderAssignmentEligibilityService riderAssignmentEligibilityService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private AdminAssignmentIssueReassignService adminAssignmentIssueReassignService;

    @BeforeEach
    void setUp() {
        adminAssignmentIssueReassignService =
            new AdminAssignmentIssueReassignService(
                deliveryAssignmentIssueRepository
                , deliveryAssignmentRepository
                , deliveryAssignmentItemRepository
                , deliveryGroupRepository
                , deliveryGroupStatusHistoryRepository
                , deliveryRepository
                , riderRepository
                , auditHistoryRepository
                , deliveryAccessService
                , riderAssignmentEligibilityService
                , entityManager
                , applicationEventPublisher
            );
    }

    @Test
    @DisplayName("이슈를 다른 기사에게 재배정하면 기존 Assignment를 종료하고 동일 배송 목록의 새 MANUAL Assignment를 생성한다")
    void reassignIssueCreatesNewManualAssignmentWithSameDeliveries() {
        // given
        ReassignFixture fixture =
            mockValidFixture();

        when(fixture.issue().getId())
            .thenReturn(ISSUE_ID);

        when(
            deliveryAssignmentIssueRepository.resolveIfUnresolved(
                eq(ISSUE_ID)
                , eq(DeliveryAssignmentIssueResolution.REASSIGNED)
                , eq(ACTOR_ID)
                , any(LocalDateTime.class)
            )
        )
            .thenReturn(1);

        when(
            deliveryAssignmentRepository.reassignIfIssueReported(
                ORIGINAL_ASSIGNMENT_ID
                , DeliveryAssignmentStatus.ISSUE_REPORTED
                , DeliveryAssignmentStatus.REASSIGNED
            )
        )
            .thenReturn(1);

        DeliveryAssignment savedAssignment =
            mock(DeliveryAssignment.class);

        when(savedAssignment.getId())
            .thenReturn(NEW_ASSIGNMENT_ID);

        when(
            deliveryAssignmentRepository.save(
                any(DeliveryAssignment.class)
            )
        )
            .thenReturn(savedAssignment);

        when(
            deliveryAssignmentIssueRepository.countUnresolvedByDeliveryGroupId(
                DELIVERY_GROUP_ID
            )
        )
            .thenReturn(0L);

        when(fixture.deliveryGroup().getStatus())
            .thenReturn(DeliveryGroupStatus.ISSUE_REVIEW);

        // when
        DeliveryAssignment result =
            adminAssignmentIssueReassignService.reassignIssue(
                ACTOR_ID
                , UserRole.ADMIN
                , ISSUE_ID
                , NEW_RIDER_ID
                , "OTHER"
                , "  차량 사용 불가로 재배정합니다.  "
            );

        // then
        assertThat(result)
            .isSameAs(savedAssignment);

        ArgumentCaptor<DeliveryAssignment> assignmentCaptor =
            ArgumentCaptor.forClass(
                DeliveryAssignment.class
            );

        verify(deliveryAssignmentRepository)
            .save(
                assignmentCaptor.capture()
            );

        DeliveryAssignment newAssignment =
            assignmentCaptor.getValue();

        assertThat(newAssignment.getDeliveryGroup())
            .isSameAs(fixture.deliveryGroup());

        assertThat(newAssignment.getRider())
            .isSameAs(fixture.newRider());

        assertThat(newAssignment.getAssignmentType())
            .isEqualTo(DeliveryAssignmentType.MANUAL);

        assertThat(newAssignment.getStatus())
            .isEqualTo(DeliveryAssignmentStatus.ASSIGNED);

        assertThat(newAssignment.getCreatedBy())
            .isEqualTo(ACTOR_ID);

        assertThat(newAssignment.getAssignedAt())
            .isNotNull();

        ArgumentCaptor<Iterable<DeliveryAssignmentItem>> itemCaptor =
            iterableCaptor();

        verify(deliveryAssignmentItemRepository)
            .saveAll(
                itemCaptor.capture()
            );

        List<DeliveryAssignmentItem> savedItems =
            toList(
                itemCaptor.getValue()
            );

        assertThat(savedItems)
            .hasSize(1);

        assertThat(savedItems.getFirst().getAssignment())
            .isSameAs(savedAssignment);

        assertThat(savedItems.getFirst().getDelivery())
            .isSameAs(fixture.originalDelivery());

        verify(fixture.deliveryGroup())
            .returnToWaitingRider();

        ArgumentCaptor<AuditHistory> auditCaptor =
            ArgumentCaptor.forClass(
                AuditHistory.class
            );

        verify(auditHistoryRepository)
            .save(
                auditCaptor.capture()
            );

        AuditHistory auditHistory =
            auditCaptor.getValue();

        assertThat(auditHistory.getEntityId())
            .isEqualTo(ISSUE_ID);

        assertThat(auditHistory.getAction())
            .isEqualTo(
                "ASSIGNMENT_ISSUE_REASSIGNED"
            );

        assertThat(auditHistory.getActorId())
            .isEqualTo(ACTOR_ID);

        assertThat(auditHistory.getReasonCode())
            .isEqualTo("OTHER");

        assertThat(auditHistory.getReasonDetail())
            .isEqualTo(
                "차량 사용 불가로 재배정합니다."
            );

        verify(entityManager)
            .refresh(
                fixture.issue()
            );

        verify(entityManager)
            .refresh(
                fixture.originalAssignment()
            );
    }

    @Test
    @DisplayName("기존 기사와 동일한 기사에게는 재배정할 수 없다")
    void reassignIssueFailsWhenNewRiderIsSameAsOriginalRider() {
        // given
        DeliveryAssignmentIssue issue =
            mock(DeliveryAssignmentIssue.class);

        DeliveryAssignment originalAssignment =
            mock(DeliveryAssignment.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        Rider originalRider =
            mock(Rider.class);

        when(
            deliveryAssignmentIssueRepository.findByIdAndDeletedAtIsNull(
                ISSUE_ID
            )
        )
            .thenReturn(
                Optional.of(issue)
            );

        when(issue.getAssignment())
            .thenReturn(originalAssignment);

        when(originalAssignment.getId())
            .thenReturn(ORIGINAL_ASSIGNMENT_ID);

        when(originalAssignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(originalAssignment.getRider())
            .thenReturn(originalRider);

        when(deliveryGroup.getId())
            .thenReturn(DELIVERY_GROUP_ID);

        when(originalRider.getId())
            .thenReturn(ORIGINAL_RIDER_ID);

        when(
            deliveryGroupRepository.findByIdForUpdate(
                DELIVERY_GROUP_ID
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        when(
            riderRepository.findAllByIdInForUpdate(
                List.of(
                    ORIGINAL_RIDER_ID
                    , ORIGINAL_RIDER_ID
                )
            )
        )
            .thenReturn(
                List.of(originalRider)
            );

        // when & then
        assertThatThrownBy(
            () ->
                adminAssignmentIssueReassignService.reassignIssue(
                    ACTOR_ID
                    , UserRole.ADMIN
                    , ISSUE_ID
                    , ORIGINAL_RIDER_ID
                    , "OTHER"
                    , "동일 기사 재배정"
                )
        )
            .isInstanceOf(
                AssignmentConditionNotMetException.class
            );

        verify(
            deliveryRepository
            , never()
        )
            .findAllByDeliveryGroupIdForUpdate(
                any()
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .save(
                any()
            );
    }

    @Test
    @DisplayName("새 기사가 배송 조건을 충족하지 못하면 재배정할 수 없다")
    void reassignIssueFailsWhenNewRiderIsNotEligible() {
        // given
        ReassignFixture fixture =
            mockFixtureBeforeEligibility();

        when(
            riderAssignmentEligibilityService.isEligible(
                fixture.newRider()
                , DELIVERY_DATE
                , DeliverySlotCode.LUNCH
                , "DAEGU_JUNG_GU"
            )
        )
            .thenReturn(false);

        // when & then
        assertThatThrownBy(
            () ->
                adminAssignmentIssueReassignService.reassignIssue(
                    ACTOR_ID
                    , UserRole.ADMIN
                    , ISSUE_ID
                    , NEW_RIDER_ID
                    , "OTHER"
                    , "근무 조건 확인"
                )
        )
            .isInstanceOf(
                AssignmentConditionNotMetException.class
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
            deliveryAssignmentRepository
            , never()
        )
            .save(
                any()
            );
    }

    @Test
    @DisplayName("새 기사의 기존 배정이 42개 도시락이면 추가 재배정을 할 수 없다")
    void reassignIssueFailsWhenNewRiderCapacityIsExceeded() {
        // given
        ReassignFixture fixture =
            mockFixtureBeforeEligibility();

        when(fixture.originalDelivery().getLunchboxQuantity())
            .thenReturn(1);

        when(
            riderAssignmentEligibilityService.isEligible(
                fixture.newRider()
                , DELIVERY_DATE
                , DeliverySlotCode.LUNCH
                , "DAEGU_JUNG_GU"
            )
        )
            .thenReturn(true);

        DeliveryAssignment currentAssignment =
            mock(DeliveryAssignment.class);

        DeliveryAssignmentItem currentItem =
            mock(DeliveryAssignmentItem.class);

        Delivery currentDelivery =
            mock(Delivery.class);

        when(currentAssignment.getId())
            .thenReturn(500L);

        when(currentAssignment.getStatus())
            .thenReturn(DeliveryAssignmentStatus.ASSIGNED);

        when(currentAssignment.getRider())
            .thenReturn(fixture.newRider());

        when(currentItem.getAssignment())
            .thenReturn(currentAssignment);

        when(currentItem.getDelivery())
            .thenReturn(currentDelivery);

        when(currentDelivery.getLunchboxQuantity())
            .thenReturn(42);

        when(currentDelivery.getId())
            .thenReturn(1001L);

        when(
            deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(
                DELIVERY_GROUP_ID
            )
        )
            .thenReturn(
                List.of(
                    fixture.originalAssignment()
                    , currentAssignment
                )
            );

        when(
            deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(
                DELIVERY_GROUP_ID
            )
        )
            .thenReturn(
                List.of(
                    fixture.originalItem()
                    , currentItem
                )
            );

        // when & then
        assertThatThrownBy(
            () ->
                adminAssignmentIssueReassignService.reassignIssue(
                    ACTOR_ID
                    , UserRole.ADMIN
                    , ISSUE_ID
                    , NEW_RIDER_ID
                    , "OTHER"
                    , "수용량 검증"
                )
        )
            .isInstanceOf(
                DeliveryCapacityExceededException.class
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
            deliveryAssignmentRepository
            , never()
        )
            .save(
                any()
            );
    }

    @Test
    @DisplayName("배송이 이미 시작된 Group은 재배정할 수 없다")
    void reassignIssueFailsAfterDeliveryStarted() {
        // given
        DeliveryAssignmentIssue issue =
            mock(DeliveryAssignmentIssue.class);

        DeliveryAssignment originalAssignment =
            mock(DeliveryAssignment.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        Rider originalRider =
            mock(Rider.class);

        when(
            deliveryAssignmentIssueRepository.findByIdAndDeletedAtIsNull(
                ISSUE_ID
            )
        )
            .thenReturn(
                Optional.of(issue)
            );

        when(issue.getAssignment())
            .thenReturn(originalAssignment);

        when(originalAssignment.getId())
            .thenReturn(ORIGINAL_ASSIGNMENT_ID);

        when(originalAssignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(originalAssignment.getRider())
            .thenReturn(originalRider);

        when(originalRider.getId())
            .thenReturn(ORIGINAL_RIDER_ID);

        when(deliveryGroup.getId())
            .thenReturn(DELIVERY_GROUP_ID);

        when(deliveryGroup.getActualStartedAt())
            .thenReturn(
                LocalDateTime.of(
                    2026
                    , 9
                    , 6
                    , 10
                    , 0
                )
            );

        when(
            deliveryGroupRepository.findByIdForUpdate(
                DELIVERY_GROUP_ID
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        // when & then
        assertThatThrownBy(
            () ->
                adminAssignmentIssueReassignService.reassignIssue(
                    ACTOR_ID
                    , UserRole.ADMIN
                    , ISSUE_ID
                    , NEW_RIDER_ID
                    , "OTHER"
                    , "재배정"
                )
        )
            .isInstanceOf(
                DeliveryAssignmentStateConflictException.class
            );

        verify(
            riderRepository
            , never()
        )
            .findAllByIdInForUpdate(
                any()
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .save(
                any()
            );
    }

    @Test
    @DisplayName("다른 관리자가 먼저 이슈를 해결했다면 재배정 상태 충돌이 발생한다")
    void reassignIssueFailsWhenIssueWasResolvedConcurrently() {
        // given
        ReassignFixture fixture =
            mockValidFixture();

        when(
            deliveryAssignmentIssueRepository.resolveIfUnresolved(
                eq(ISSUE_ID)
                , eq(DeliveryAssignmentIssueResolution.REASSIGNED)
                , eq(ACTOR_ID)
                , any(LocalDateTime.class)
            )
        )
            .thenReturn(0);

        // when & then
        assertThatThrownBy(
            () ->
                adminAssignmentIssueReassignService.reassignIssue(
                    ACTOR_ID
                    , UserRole.ADMIN
                    , ISSUE_ID
                    , NEW_RIDER_ID
                    , "OTHER"
                    , "재배정"
                )
        )
            .isInstanceOf(
                DeliveryAssignmentStateConflictException.class
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .reassignIfIssueReported(
                any()
                , any()
                , any()
            );

        verify(
            deliveryAssignmentRepository
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
    }

    @Test
    @DisplayName("OTHER 재배정 사유에 상세 설명이 없으면 재배정할 수 없다")
    void reassignIssueFailsWhenOtherReasonDetailIsMissing() {
        // when & then
        assertThatThrownBy(
            () ->
                adminAssignmentIssueReassignService.reassignIssue(
                    ACTOR_ID
                    , UserRole.ADMIN
                    , ISSUE_ID
                    , NEW_RIDER_ID
                    , "OTHER"
                    , "   "
                )
        )
            .isInstanceOf(
                OtherReasonDetailRequiredException.class
            );

        verify(
            deliveryAssignmentIssueRepository
            , never()
        )
            .findByIdAndDeletedAtIsNull(
                any()
            );
    }

    private ReassignFixture mockValidFixture() {
        ReassignFixture fixture =
            mockFixtureBeforeEligibility();

        when(
            riderAssignmentEligibilityService.isEligible(
                fixture.newRider()
                , DELIVERY_DATE
                , DeliverySlotCode.LUNCH
                , "DAEGU_JUNG_GU"
            )
        )
            .thenReturn(true);

        when(fixture.originalDelivery().getLunchboxQuantity())
            .thenReturn(1);

        return fixture;
    }

    private ReassignFixture mockFixtureBeforeEligibility() {
        DeliveryAssignmentIssue issue =
            mock(DeliveryAssignmentIssue.class);

        DeliveryAssignment originalAssignment =
            mock(DeliveryAssignment.class);

        DeliveryAssignmentItem originalItem =
            mock(DeliveryAssignmentItem.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        DeliverySlot deliverySlot =
            mock(DeliverySlot.class);

        Rider originalRider =
            mock(Rider.class);

        Rider newRider =
            mock(Rider.class);

        Delivery originalDelivery =
            mock(Delivery.class);

        when(
            deliveryAssignmentIssueRepository.findByIdAndDeletedAtIsNull(
                ISSUE_ID
            )
        )
            .thenReturn(
                Optional.of(issue)
            );

        when(issue.getAssignment())
            .thenReturn(originalAssignment);

        when(originalAssignment.getId())
            .thenReturn(ORIGINAL_ASSIGNMENT_ID);

        when(originalAssignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(originalAssignment.getRider())
            .thenReturn(originalRider);

        when(originalAssignment.getStatus())
            .thenReturn(DeliveryAssignmentStatus.ISSUE_REPORTED);

        when(originalRider.getId())
            .thenReturn(ORIGINAL_RIDER_ID);

        when(newRider.getId())
            .thenReturn(NEW_RIDER_ID);

        when(deliveryGroup.getId())
            .thenReturn(DELIVERY_GROUP_ID);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(DELIVERY_DATE);

        when(deliveryGroup.getSlot())
            .thenReturn(deliverySlot);

        when(deliveryGroup.isIssueReview())
            .thenReturn(true);

        when(deliverySlot.getCode())
            .thenReturn(DeliverySlotCode.LUNCH);

        when(originalItem.getAssignment())
            .thenReturn(originalAssignment);

        when(originalItem.getDelivery())
            .thenReturn(originalDelivery);

        when(originalDelivery.getStatus())
            .thenReturn(DeliveryStatus.READY);

        when(originalDelivery.getId())
            .thenReturn(DELIVERY_ID);

        when(originalDelivery.getDeliveryAreaCode())
            .thenReturn("DAEGU_JUNG_GU");

        when(
            deliveryGroupRepository.findByIdForUpdate(
                DELIVERY_GROUP_ID
            )
        )
            .thenReturn(
                Optional.of(deliveryGroup)
            );

        when(
            riderRepository.findAllByIdInForUpdate(
                List.of(
                    ORIGINAL_RIDER_ID
                    , NEW_RIDER_ID
                )
            )
        )
            .thenReturn(
                List.of(
                    originalRider
                    , newRider
                )
            );

        when(
            deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(
                DELIVERY_GROUP_ID
            )
        )
            .thenReturn(
                List.of(originalAssignment)
            );

        when(
            deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(
                DELIVERY_GROUP_ID
            )
        )
            .thenReturn(
                List.of(originalItem)
            );

        return new ReassignFixture(
            issue
            , originalAssignment
            , originalItem
            , deliveryGroup
            , newRider
            , originalDelivery
        );
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Iterable<DeliveryAssignmentItem>> iterableCaptor() {
        return ArgumentCaptor.forClass(
            Iterable.class
        );
    }

    private List<DeliveryAssignmentItem> toList(
        Iterable<DeliveryAssignmentItem> items
    ) {
        List<DeliveryAssignmentItem> result =
            new java.util.ArrayList<>();

        items.forEach(
            result::add
        );

        return result;
    }

    private record ReassignFixture(
        DeliveryAssignmentIssue issue
        , DeliveryAssignment originalAssignment
        , DeliveryAssignmentItem originalItem
        , DeliveryGroup deliveryGroup
        , Rider newRider
        , Delivery originalDelivery
    ) {
    }
}
