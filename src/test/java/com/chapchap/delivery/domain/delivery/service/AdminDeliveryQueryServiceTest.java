package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentIssueRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentIssue;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueCode;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueResolution;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.service.RiderAssignmentEligibilityService;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletion;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletionPhoto;
import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import com.chapchap.delivery.domain.delivery.entity.DeliveryResultCorrection;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryProcessedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureStage;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryResultType;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionPhotoRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryDelayRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRecipientSnapshotRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryResultCorrectionRepository;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryGroupDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AdminDeliveryQueryServiceTest {
    @Mock private DeliveryAccessService accessService;
    @Mock private DeliveryGroupRepository groupRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private DeliveryAssignmentItemRepository assignmentItemRepository;
    @Mock private DeliveryAssignmentIssueRepository issueRepository;
    @Mock private DeliveryRecipientSnapshotRepository snapshotRepository;
    @Mock private DeliveryDelayRepository delayRepository;
    @Mock private DeliveryCompletionRepository completionRepository;
    @Mock private DeliveryCompletionPhotoRepository photoRepository;
    @Mock private DeliveryFailureRepository failureRepository;
    @Mock private DeliveryStatusHistoryRepository statusHistoryRepository;
    @Mock private DeliveryResultCorrectionRepository correctionRepository;
    @Mock private RiderAssignmentEligibilityService eligibilityService;

    private AdminDeliveryQueryService service;

    @BeforeEach
    void setUp() {
        service = new AdminDeliveryQueryService(
            accessService, groupRepository, deliveryRepository, assignmentRepository,
            assignmentItemRepository, issueRepository, snapshotRepository, delayRepository,
            completionRepository, photoRepository, failureRepository, statusHistoryRepository,
            correctionRepository, eligibilityService
        );
    }

    @Test
    @DisplayName("관리자 접근을 검증한 뒤 안정적인 페이징 목록을 반환한다")
    void validatesAdminAndReturnsPagedGroups() {
        when(groupRepository.findAllForAdmin(
            any(), any(), any(), any()
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var response = service.getDeliveryGroups(
            7L, UserRole.ADMIN, null, null, null, PageRequest.of(0, 20)
        );

        verify(accessService).validateAdminAccess(7L, UserRole.ADMIN);
        assertThat(response.items()).isEmpty();
        assertThat(response.size()).isEqualTo(20);
    }

    @ParameterizedTest
    @CsvSource({
        "LUNCH,2026-09-06T09:30:00+09:00,2026-09-06T10:00:00+09:00",
        "DINNER,2026-09-06T15:30:00+09:00,2026-09-06T16:00:00+09:00"
    })
    @DisplayName("배송일·시간대로 관리자 확정 목표와 마감 시각을 계산한다")
    void calculatesConfirmationTimes(
        DeliverySlotCode slotCode
        , OffsetDateTime expectedTarget
        , OffsetDateTime expectedDeadline
    ) {
        DeliveryGroup group = group(1L, slotCode);
        when(groupRepository.findDetailById(1L)).thenReturn(Optional.of(group));
        when(deliveryRepository.findAllByDeliveryGroupIdIn(List.of(1L))).thenReturn(List.of());
        when(assignmentRepository.findAllByDeliveryGroupIdIn(List.of(1L))).thenReturn(List.of());
        when(assignmentItemRepository.findAllByDeliveryGroupIdIn(List.of(1L))).thenReturn(List.of());
        when(issueRepository.findAllByDeliveryGroupIdIn(List.of(1L))).thenReturn(List.of());

        var response = service.getDeliveryGroup(7L, UserRole.ADMIN, 1L);

        assertThat(response.confirmationTargetAt()).isEqualTo(expectedTarget);
        assertThat(response.confirmationDeadlineAt()).isEqualTo(expectedDeadline);
    }

    @Test
    @DisplayName("관리자 배송 상세에 완료·실패 원본 필드를 누락 없이 반환한다")
    void returnsOriginalCompletionAndFailureFields() {
        DeliveryGroup group = group(1L, DeliverySlotCode.LUNCH);
        Delivery delivery = mock(Delivery.class);
        lenient().when(delivery.getId()).thenReturn(10L);
        lenient().when(delivery.getDeliveryPublicId()).thenReturn("delivery-10");
        lenient().when(delivery.getSourceOrderId()).thenReturn("order-10");
        lenient().when(delivery.getCustomerId()).thenReturn(20L);
        lenient().when(delivery.getDeliveryGroup()).thenReturn(group);
        lenient().when(delivery.getStatus()).thenReturn(DeliveryStatus.FAILED);
        lenient().when(delivery.getDeliveryVersion()).thenReturn(3);
        lenient().when(delivery.getLunchboxQuantity()).thenReturn(2);
        DeliveryCompletion completion = mock(DeliveryCompletion.class);
        lenient().when(completion.getId()).thenReturn(30L);
        lenient().when(completion.getActualHandoffType()).thenReturn(ActualHandoffType.DIRECT);
        lenient().when(completion.getStorageLocation()).thenReturn("front");
        lenient().when(completion.getContactAttemptedAt()).thenReturn(LocalDateTime.of(2026, 9, 6, 12, 0));
        lenient().when(completion.getContactResult()).thenReturn("CONNECTED");
        lenient().when(completion.getProcessedBy()).thenReturn(40L);
        lenient().when(completion.getProcessedByType()).thenReturn(DeliveryProcessedByType.ADMIN);
        lenient().when(completion.getAdminReasonCode()).thenReturn("DEVICE_FAILURE");
        lenient().when(completion.getAdminReasonDetail()).thenReturn("detail");
        lenient().when(completion.getCompletedAt()).thenReturn(LocalDateTime.of(2026, 9, 6, 12, 10));
        DeliveryFailure failure = mock(DeliveryFailure.class);
        lenient().when(failure.getFailureStage()).thenReturn(DeliveryFailureStage.DURING_DELIVERY);
        lenient().when(failure.getFailureCode()).thenReturn(DeliveryFailureCode.VEHICLE_ISSUE);
        lenient().when(failure.getFailureDetail()).thenReturn("failure detail");
        lenient().when(failure.getContactAttemptedAt()).thenReturn(LocalDateTime.of(2026, 9, 6, 12, 20));
        lenient().when(failure.getContactResult()).thenReturn("NO_ANSWER");
        lenient().when(failure.getItemRecovered()).thenReturn(true);
        lenient().when(failure.getRecoveredAt()).thenReturn(LocalDateTime.of(2026, 9, 6, 12, 30));
        lenient().when(failure.getProcessedBy()).thenReturn(41L);
        lenient().when(failure.getProcessedByType()).thenReturn(DeliveryProcessedByType.ADMIN);
        lenient().when(failure.getAdminReasonCode()).thenReturn("OTHER");
        lenient().when(failure.getAdminReasonDetail()).thenReturn("admin detail");
        lenient().when(failure.getFailedAt()).thenReturn(LocalDateTime.of(2026, 9, 6, 12, 40));
        when(deliveryRepository.findDetailByDeliveryPublicId("delivery-10"))
            .thenReturn(Optional.of(delivery));
        when(delayRepository.findByDeliveryId(10L)).thenReturn(Optional.empty());
        when(completionRepository.findByDeliveryId(10L)).thenReturn(Optional.of(completion));
        when(failureRepository.findByDeliveryId(10L)).thenReturn(Optional.of(failure));
        when(photoRepository.findByDeliveryCompletionId(30L))
            .thenReturn(Optional.of(mock(DeliveryCompletionPhoto.class)));
        when(statusHistoryRepository.findAllByDelivery_IdOrderByChangedAtAsc(10L))
            .thenReturn(List.of());
        when(assignmentRepository.findAllByDeliveryId(10L)).thenReturn(List.of());

        var response = service.getDelivery(7L, UserRole.ADMIN, "delivery-10");

        assertThat(response.completion().contactResult()).isEqualTo("CONNECTED");
        assertThat(response.completion().adminReasonDetail()).isEqualTo("detail");
        assertThat(response.completion().hasCompletionPhoto()).isTrue();
        assertThat(response.failure().contactResult()).isEqualTo("NO_ANSWER");
        assertThat(response.failure().recoveredAt())
            .isEqualTo(OffsetDateTime.parse("2026-09-06T12:30:00+09:00"));
        assertThat(response.failure().adminReasonDetail()).isEqualTo("admin detail");
    }


    @Test
    @DisplayName("관리자 배송 상세은 원본과 최신 유효 정정값을 함께 반환한다")
    void returnsEffectiveCorrectedValuesAlongsideOriginals() {
        DeliveryGroup group = group(1L, DeliverySlotCode.LUNCH);
        Delivery delivery = mock(Delivery.class);
        lenient().when(delivery.getId()).thenReturn(10L);
        lenient().when(delivery.getDeliveryPublicId()).thenReturn("delivery-10");
        lenient().when(delivery.getSourceOrderId()).thenReturn("order-10");
        lenient().when(delivery.getCustomerId()).thenReturn(20L);
        lenient().when(delivery.getDeliveryGroup()).thenReturn(group);
        lenient().when(delivery.getStatus()).thenReturn(DeliveryStatus.DELIVERED);
        lenient().when(delivery.getDeliveryVersion()).thenReturn(3);
        lenient().when(delivery.getLunchboxQuantity()).thenReturn(2);

        DeliveryCompletion completion = mock(DeliveryCompletion.class);
        lenient().when(completion.getId()).thenReturn(30L);
        lenient().when(completion.getActualHandoffType()).thenReturn(ActualHandoffType.DOORSTEP);
        lenient().when(completion.getStorageLocation()).thenReturn("기존 위치");

        DeliveryResultCorrection handoffCorrection = mock(DeliveryResultCorrection.class);
        lenient().when(handoffCorrection.getResultType()).thenReturn(DeliveryResultType.COMPLETION);
        lenient().when(handoffCorrection.getFieldName()).thenReturn("actual_handoff_type");
        lenient().when(handoffCorrection.getAfterValue()).thenReturn("OTHER");
        DeliveryResultCorrection locationCorrection = mock(DeliveryResultCorrection.class);
        lenient().when(locationCorrection.getResultType()).thenReturn(DeliveryResultType.COMPLETION);
        lenient().when(locationCorrection.getFieldName()).thenReturn("storage_location");
        lenient().when(locationCorrection.getAfterValue()).thenReturn("정정 위치");

        when(deliveryRepository.findDetailByDeliveryPublicId("delivery-10"))
            .thenReturn(Optional.of(delivery));
        when(delayRepository.findByDeliveryId(10L)).thenReturn(Optional.empty());
        when(completionRepository.findByDeliveryId(10L)).thenReturn(Optional.of(completion));
        when(failureRepository.findByDeliveryId(10L)).thenReturn(Optional.empty());
        when(photoRepository.findByDeliveryCompletionId(30L)).thenReturn(Optional.empty());
        when(statusHistoryRepository.findAllByDelivery_IdOrderByChangedAtAsc(10L))
            .thenReturn(List.of());
        when(assignmentRepository.findAllByDeliveryId(10L)).thenReturn(List.of());
        when(correctionRepository.findAllByDelivery_IdOrderByIdAsc(10L))
            .thenReturn(List.of(handoffCorrection, locationCorrection));

        var response = service.getDelivery(7L, UserRole.ADMIN, "delivery-10");

        assertThat(response.completion().actualHandoffType()).isEqualTo(ActualHandoffType.DOORSTEP);
        assertThat(response.completion().storageLocation()).isEqualTo("기존 위치");
        assertThat(response.completion().effectiveActualHandoffType()).isEqualTo(ActualHandoffType.OTHER);
        assertThat(response.completion().effectiveStorageLocation()).isEqualTo("정정 위치");
    }

    @Test
    @DisplayName("관리자 배송 그룹 상세에 미해결·해결 배정 이슈 이력을 반환한다")
    void returnsAssignmentIssueHistoryForCurrentGroup() {
        DeliveryGroup currentGroup = group(1L, DeliverySlotCode.LUNCH);
        DeliveryGroup otherGroup = group(2L, DeliverySlotCode.LUNCH);
        DeliveryAssignmentIssue unresolved = issue(
            10L, 100L, currentGroup, DeliveryAssignmentIssueCode.CAPACITY_CONCERN,
            "도시락 수량 확인 필요", 200L, LocalDateTime.of(2026, 9, 5, 18, 0),
            null, null, null
        );
        DeliveryAssignmentIssue resolved = issue(
            11L, 101L, currentGroup, DeliveryAssignmentIssueCode.SCHEDULE_CONFLICT,
            "일정 충돌", 201L, LocalDateTime.of(2026, 9, 5, 18, 10),
            DeliveryAssignmentIssueResolution.REJECTED, 300L,
            LocalDateTime.of(2026, 9, 5, 18, 20)
        );
        DeliveryAssignmentIssue other = issue(
            12L, 102L, otherGroup, DeliveryAssignmentIssueCode.OTHER,
            "다른 그룹", 202L, LocalDateTime.of(2026, 9, 5, 18, 30),
            null, null, null
        );
        when(groupRepository.findDetailById(1L)).thenReturn(Optional.of(currentGroup));
        when(deliveryRepository.findAllByDeliveryGroupIdIn(List.of(1L))).thenReturn(List.of());
        when(assignmentRepository.findAllByDeliveryGroupIdIn(List.of(1L))).thenReturn(List.of());
        when(assignmentItemRepository.findAllByDeliveryGroupIdIn(List.of(1L))).thenReturn(List.of());
        when(issueRepository.findAllByDeliveryGroupIdIn(List.of(1L)))
            .thenReturn(List.of(unresolved, resolved, other));

        var response = service.getDeliveryGroup(7L, UserRole.ADMIN, 1L);

        assertThat(response.issues()).hasSize(2);
        assertThat(response.issues().get(0)).satisfies(issue -> {
            assertThat(issue.issueId()).isEqualTo(10L);
            assertThat(issue.assignmentId()).isEqualTo(100L);
            assertThat(issue.issueCode()).isEqualTo(DeliveryAssignmentIssueCode.CAPACITY_CONCERN);
            assertThat(issue.issueDetail()).isEqualTo("도시락 수량 확인 필요");
            assertThat(issue.reportedBy()).isEqualTo(200L);
            assertThat(issue.reportedAt())
                .isEqualTo(OffsetDateTime.parse("2026-09-05T18:00:00+09:00"));
            assertThat(issue.resolution()).isNull();
            assertThat(issue.resolvedBy()).isNull();
            assertThat(issue.resolvedAt()).isNull();
        });
        assertThat(response.issues().get(1)).satisfies(issue -> {
            assertThat(issue.issueId()).isEqualTo(11L);
            assertThat(issue.resolution()).isEqualTo(DeliveryAssignmentIssueResolution.REJECTED);
            assertThat(issue.resolvedBy()).isEqualTo(300L);
            assertThat(issue.resolvedAt())
                .isEqualTo(OffsetDateTime.parse("2026-09-05T18:20:00+09:00"));
        });
        assertThat(response.issues())
            .extracting(AdminDeliveryGroupDetailResponse.Issue::issueId)
            .doesNotContain(12L);
    }

    @Test
    @DisplayName("배정 이슈가 없으면 관리자 배송 그룹 상세은 빈 목록을 반환한다")
    void returnsEmptyIssueList() {
        DeliveryGroup group = group(1L, DeliverySlotCode.LUNCH);
        when(groupRepository.findDetailById(1L)).thenReturn(Optional.of(group));
        when(deliveryRepository.findAllByDeliveryGroupIdIn(List.of(1L))).thenReturn(List.of());
        when(assignmentRepository.findAllByDeliveryGroupIdIn(List.of(1L))).thenReturn(List.of());
        when(assignmentItemRepository.findAllByDeliveryGroupIdIn(List.of(1L))).thenReturn(List.of());
        when(issueRepository.findAllByDeliveryGroupIdIn(List.of(1L))).thenReturn(List.of());

        var response = service.getDeliveryGroup(7L, UserRole.ADMIN, 1L);

        assertThat(response.issues()).isEmpty();
    }

    @Test
    @DisplayName("모든 배송이 활성 배정 하나에만 포함되면 배정 준비가 완료된다")
    void exactlyOneActiveAssignmentPerDelivery() {
        Delivery first = delivery(1L, 1);
        Delivery second = delivery(2L, 1);

        assertThat(service.hasExactlyOneActiveAssignmentPerDelivery(
            List.of(first, second)
            , List.of(item(first, 10L, 100L, DeliveryAssignmentStatus.ASSIGNED),
                item(second, 10L, 100L, DeliveryAssignmentStatus.ASSIGNED))
        )).isTrue();
    }

    @Test
    @DisplayName("하나의 배송이 두 활성 배정에 중복되면 배정 준비가 아니다")
    void duplicateActiveAssignmentsAreRejected() {
        Delivery delivery = delivery(1L, 1);

        assertThat(service.hasExactlyOneActiveAssignmentPerDelivery(
            List.of(delivery)
            , List.of(item(delivery, 10L, 100L, DeliveryAssignmentStatus.ASSIGNED),
                item(delivery, 11L, 101L, DeliveryAssignmentStatus.ACKNOWLEDGED))
        )).isFalse();
    }

    @Test
    @DisplayName("미배정 배송이 하나라도 있으면 배정 준비가 아니다")
    void unassignedDeliveryIsRejected() {
        Delivery assigned = delivery(1L, 1);
        Delivery unassigned = delivery(2L, 1);

        assertThat(service.hasExactlyOneActiveAssignmentPerDelivery(
            List.of(assigned, unassigned)
            , List.of(item(assigned, 10L, 100L, DeliveryAssignmentStatus.ASSIGNED))
        )).isFalse();
    }

    @Test
    @DisplayName("단일 배정 8곳 36개는 수용량 이내다")
    void singleAssignmentWithinRecommendedCapacity() {
        assertThat(service.isCapacityValid(items(100L, 10L, 8, 36))).isTrue();
    }

    @Test
    @DisplayName("단일 배정 10곳 42개는 최대 수용량 경계다")
    void singleAssignmentAtMaximumCapacity() {
        assertThat(service.isCapacityValid(items(100L, 10L, 10, 42))).isTrue();
    }

    @Test
    @DisplayName("방문지 11곳 또는 도시락 43개면 최대 수용량을 초과한다")
    void singleAssignmentOverMaximumCapacity() {
        assertThat(service.isCapacityValid(items(100L, 10L, 11, 22))).isFalse();
        assertThat(service.isCapacityValid(items(100L, 10L, 10, 43))).isFalse();
    }

    @Test
    @DisplayName("같은 기사의 두 배정은 합산해 수용량을 검증한다")
    void assignmentsForSameRiderAreAggregated() {
        List<DeliveryAssignmentItem> items = new java.util.ArrayList<>();
        items.addAll(items(100L, 10L, 6, 25));
        items.addAll(items(100L, 11L, 6, 25));

        assertThat(service.isCapacityValid(items)).isFalse();
    }

    @Test
    @DisplayName("서로 다른 기사의 배정량은 독립적으로 검증한다")
    void assignmentsForDifferentRidersAreIndependent() {
        List<DeliveryAssignmentItem> items = new java.util.ArrayList<>();
        items.addAll(items(100L, 10L, 6, 25));
        items.addAll(items(101L, 11L, 6, 25));

        assertThat(service.isCapacityValid(items)).isTrue();
    }

    private List<DeliveryAssignmentItem> items(
        Long riderId
        , Long assignmentId
        , int stopCount
        , int totalQuantity
    ) {
        List<DeliveryAssignmentItem> result = new java.util.ArrayList<>();
        int base = totalQuantity / stopCount;
        int remainder = totalQuantity % stopCount;
        for (int index = 0; index < stopCount; index++) {
            result.add(item(
                delivery((long) assignmentId * 100 + index, base + (index < remainder ? 1 : 0))
                , assignmentId
                , riderId
                , DeliveryAssignmentStatus.ASSIGNED
            ));
        }
        return result;
    }

    private Delivery delivery(Long id, int quantity) {
        Delivery delivery = mock(Delivery.class);
        lenient().when(delivery.getId()).thenReturn(id);
        lenient().when(delivery.getLunchboxQuantity()).thenReturn(quantity);
        return delivery;
    }

    private DeliveryAssignmentItem item(
        Delivery delivery
        , Long assignmentId
        , Long riderId
        , DeliveryAssignmentStatus status
    ) {
        Rider rider = mock(Rider.class);
        lenient().when(rider.getId()).thenReturn(riderId);
        DeliveryAssignment assignment = mock(DeliveryAssignment.class);
        lenient().when(assignment.getRider()).thenReturn(rider);
        when(assignment.getStatus()).thenReturn(status);
        DeliveryAssignmentItem item = mock(DeliveryAssignmentItem.class);
        when(item.getAssignment()).thenReturn(assignment);
        when(item.getDelivery()).thenReturn(delivery);
        return item;
    }

    private DeliveryAssignmentIssue issue(
        Long issueId
        , Long assignmentId
        , DeliveryGroup group
        , DeliveryAssignmentIssueCode issueCode
        , String issueDetail
        , Long reportedBy
        , LocalDateTime reportedAt
        , DeliveryAssignmentIssueResolution resolution
        , Long resolvedBy
        , LocalDateTime resolvedAt
    ) {
        DeliveryAssignment assignment = mock(DeliveryAssignment.class);
        lenient().when(assignment.getId()).thenReturn(assignmentId);
        lenient().when(assignment.getDeliveryGroup()).thenReturn(group);
        DeliveryAssignmentIssue issue = mock(DeliveryAssignmentIssue.class);
        lenient().when(issue.getId()).thenReturn(issueId);
        lenient().when(issue.getAssignment()).thenReturn(assignment);
        lenient().when(issue.getIssueCode()).thenReturn(issueCode);
        lenient().when(issue.getIssueDetail()).thenReturn(issueDetail);
        lenient().when(issue.getReportedBy()).thenReturn(reportedBy);
        lenient().when(issue.getReportedAt()).thenReturn(reportedAt);
        lenient().when(issue.getResolution()).thenReturn(resolution);
        lenient().when(issue.getResolvedBy()).thenReturn(resolvedBy);
        lenient().when(issue.getResolvedAt()).thenReturn(resolvedAt);
        return issue;
    }

    private DeliveryGroup group(Long id, DeliverySlotCode slotCode) {
        DeliverySlot slot = mock(DeliverySlot.class);
        lenient().when(slot.getCode()).thenReturn(slotCode);
        DeliveryGroup group = mock(DeliveryGroup.class);
        lenient().when(group.getId()).thenReturn(id);
        lenient().when(group.getDeliveryDate()).thenReturn(LocalDate.of(2026, 9, 6));
        lenient().when(group.getSlot()).thenReturn(slot);
        lenient().when(group.getStatus()).thenReturn(DeliveryGroupStatus.READY_TO_CONFIRM);
        return group;
    }
}
