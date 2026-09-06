package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.AdminDeliveryOperationType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDeliveryOperationQueryServiceTest {
    @Mock private DeliveryAccessService accessService;
    @Mock private DeliveryGroupRepository groupRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryAssignmentRepository assignmentRepository;

    private AdminDeliveryOperationQueryService service;

    @BeforeEach
    void setUp() {
        service = new AdminDeliveryOperationQueryService(
            accessService, groupRepository, deliveryRepository, assignmentRepository
        );
    }

    @Test
    @DisplayName("현재 저장된 시각·상태 근거로 네 가지 운영 조건을 계산한다")
    void calculatesSupportedOperationTypes() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 6, 17, 1);
        DeliveryGroup tomorrowLunch = group(
            1L, LocalDate.of(2026, 9, 7), DeliverySlotCode.LUNCH,
            DeliveryGroupStatus.WAITING_ASSIGNMENT
        );
        DeliveryGroup todayLunch = group(
            2L, LocalDate.of(2026, 9, 6), DeliverySlotCode.LUNCH,
            DeliveryGroupStatus.CONFIRMED
        );
        Delivery late = delivery(
            10L, tomorrowLunch, DeliveryStatus.READY,
            LocalDateTime.of(2026, 9, 6, 16, 11)
        );
        Delivery unresolved = delivery(
            11L, todayLunch, DeliveryStatus.READY,
            LocalDateTime.of(2026, 9, 5, 15, 0)
        );
        DeliveryAssignment overdue = assignment(
            20L, todayLunch, DeliveryAssignmentStatus.ASSIGNED
        );
        when(groupRepository.findAllForOperations(null, null))
            .thenReturn(List.of(tomorrowLunch, todayLunch));
        when(deliveryRepository.findAllByDeliveryGroupIdIn(List.of(1L, 2L)))
            .thenReturn(List.of(late, unresolved));
        when(assignmentRepository.findAllByDeliveryGroupIdIn(List.of(1L, 2L)))
            .thenReturn(List.of(overdue));

        var result = service.operations(null, null, now);

        assertThat(result.get(AdminDeliveryOperationType.AUTO_ASSIGNMENT_FINAL_FAILURE)).hasSize(1);
        assertThat(result.get(AdminDeliveryOperationType.LATE_ORDER_REVIEW)).hasSize(1);
        assertThat(result.get(AdminDeliveryOperationType.ACKNOWLEDGEMENT_OVERDUE)).hasSize(1);
        assertThat(result.get(AdminDeliveryOperationType.UNRESOLVED_DELIVERY)).hasSize(1);
    }

    @Test
    @DisplayName("자동 배정 최종 실패는 17시를 초과한 시각부터만 조회한다")
    void autoAssignmentFinalFailureUsesStrictAfterBoundary() {
        DeliveryGroup target = group(
            1L, LocalDate.of(2026, 9, 7), DeliverySlotCode.LUNCH,
            DeliveryGroupStatus.WAITING_ASSIGNMENT
        );
        stubOperations(List.of(target), List.of(), List.of());

        assertThat(service.operations(
            null, null, LocalDateTime.of(2026, 9, 6, 16, 59, 59)
        )).doesNotContainKey(AdminDeliveryOperationType.AUTO_ASSIGNMENT_FINAL_FAILURE);
        assertThat(service.operations(
            null, null, LocalDateTime.of(2026, 9, 6, 17, 0)
        )).doesNotContainKey(AdminDeliveryOperationType.AUTO_ASSIGNMENT_FINAL_FAILURE);
        assertThat(service.operations(
            null, null, LocalDateTime.of(2026, 9, 6, 17, 0, 1)
        ).get(AdminDeliveryOperationType.AUTO_ASSIGNMENT_FINAL_FAILURE)).hasSize(1);
    }

    @Test
    @DisplayName("자동 배정 완료 또는 대기 상태가 아닌 그룹은 최종 실패에서 제외한다")
    void autoAssignmentFinalFailureRequiresWaitingAndIncomplete() {
        DeliveryGroup notWaiting = group(
            1L, LocalDate.of(2026, 9, 7), DeliverySlotCode.LUNCH,
            DeliveryGroupStatus.WAITING_RIDER
        );
        DeliveryGroup completed = group(
            2L, LocalDate.of(2026, 9, 7), DeliverySlotCode.LUNCH,
            DeliveryGroupStatus.WAITING_ASSIGNMENT
        );
        when(completed.getAutoAssignmentCompletedAt())
            .thenReturn(LocalDateTime.of(2026, 9, 6, 16, 50));
        stubOperations(List.of(notWaiting, completed), List.of(), List.of());

        assertThat(service.operations(
            null, null, LocalDateTime.of(2026, 9, 6, 17, 0, 1)
        )).doesNotContainKey(AdminDeliveryOperationType.AUTO_ASSIGNMENT_FINAL_FAILURE);
    }

    @Test
    @DisplayName("늦은 주문은 16시 10분을 초과해 생성된 배송만 조회한다")
    void lateOrderUsesStrictAfterBoundary() {
        DeliveryGroup group = group(
            1L, LocalDate.of(2026, 9, 7), DeliverySlotCode.LUNCH,
            DeliveryGroupStatus.WAITING_RIDER
        );
        stubOperations(
            List.of(group)
            , List.of(
                delivery(10L, group, DeliveryStatus.READY, LocalDateTime.of(2026, 9, 6, 16, 9, 59))
                , delivery(11L, group, DeliveryStatus.READY, LocalDateTime.of(2026, 9, 6, 16, 10))
                , delivery(12L, group, DeliveryStatus.READY, LocalDateTime.of(2026, 9, 6, 16, 10, 1))
            )
            , List.of()
        );

        assertThat(service.operations(
            null, null, LocalDateTime.of(2026, 9, 6, 17, 1)
        ).get(AdminDeliveryOperationType.LATE_ORDER_REVIEW))
            .extracting(item -> item.deliveryId())
            .containsExactly("delivery-12");
    }

    @Test
    @DisplayName("기사 미확인은 점심 9시와 저녁 15시 경계를 포함한다")
    void acknowledgementOverdueIncludesLunchAndDinnerBoundary() {
        DeliveryGroup lunch = group(
            1L, LocalDate.of(2026, 9, 6), DeliverySlotCode.LUNCH,
            DeliveryGroupStatus.WAITING_RIDER
        );
        DeliveryGroup dinner = group(
            2L, LocalDate.of(2026, 9, 6), DeliverySlotCode.DINNER,
            DeliveryGroupStatus.WAITING_RIDER
        );
        stubOperations(
            List.of(lunch, dinner), List.of(),
            List.of(
                assignment(10L, lunch, DeliveryAssignmentStatus.ASSIGNED),
                assignment(11L, dinner, DeliveryAssignmentStatus.ASSIGNED)
            )
        );

        assertThat(operationSize(
            AdminDeliveryOperationType.ACKNOWLEDGEMENT_OVERDUE,
            LocalDateTime.of(2026, 9, 6, 8, 59, 59)
        )).isZero();
        assertThat(operationSize(
            AdminDeliveryOperationType.ACKNOWLEDGEMENT_OVERDUE,
            LocalDateTime.of(2026, 9, 6, 9, 0)
        )).isEqualTo(1);
        assertThat(operationSize(
            AdminDeliveryOperationType.ACKNOWLEDGEMENT_OVERDUE,
            LocalDateTime.of(2026, 9, 6, 14, 59, 59)
        )).isEqualTo(1);
        assertThat(operationSize(
            AdminDeliveryOperationType.ACKNOWLEDGEMENT_OVERDUE,
            LocalDateTime.of(2026, 9, 6, 15, 0)
        )).isEqualTo(2);
    }

    @Test
    @DisplayName("미해결 배송은 시간대별 마감부터 READY와 DELIVERING만 조회한다")
    void unresolvedDeliveryUsesInclusiveSlotBoundariesAndStatuses() {
        DeliveryGroup lunch = group(
            1L, LocalDate.of(2026, 9, 6), DeliverySlotCode.LUNCH,
            DeliveryGroupStatus.IN_PROGRESS
        );
        DeliveryGroup dinner = group(
            2L, LocalDate.of(2026, 9, 6), DeliverySlotCode.DINNER,
            DeliveryGroupStatus.IN_PROGRESS
        );
        stubOperations(
            List.of(lunch, dinner)
            , List.of(
                delivery(10L, lunch, DeliveryStatus.READY, null)
                , delivery(11L, lunch, DeliveryStatus.DELIVERING, null)
                , delivery(12L, lunch, DeliveryStatus.DELIVERED, null)
                , delivery(13L, lunch, DeliveryStatus.FAILED, null)
                , delivery(20L, dinner, DeliveryStatus.READY, null)
                , delivery(21L, dinner, DeliveryStatus.DELIVERING, null)
                , delivery(22L, dinner, DeliveryStatus.DELIVERED, null)
                , delivery(23L, dinner, DeliveryStatus.FAILED, null)
            )
            , List.of()
        );

        assertThat(operationSize(
            AdminDeliveryOperationType.UNRESOLVED_DELIVERY,
            LocalDateTime.of(2026, 9, 6, 13, 29, 59)
        )).isZero();
        assertThat(operationSize(
            AdminDeliveryOperationType.UNRESOLVED_DELIVERY,
            LocalDateTime.of(2026, 9, 6, 13, 30)
        )).isEqualTo(2);
        assertThat(operationSize(
            AdminDeliveryOperationType.UNRESOLVED_DELIVERY,
            LocalDateTime.of(2026, 9, 6, 19, 29, 59)
        )).isEqualTo(2);
        assertThat(operationSize(
            AdminDeliveryOperationType.UNRESOLVED_DELIVERY,
            LocalDateTime.of(2026, 9, 6, 19, 30)
        )).isEqualTo(4);
    }

    @Test
    @DisplayName("운영 조회 count와 list는 동일한 판정 조건을 사용한다")
    void countMatchesListTotalElements() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        DeliveryGroup group = group(
            1L, today.plusDays(1), DeliverySlotCode.LUNCH,
            DeliveryGroupStatus.WAITING_RIDER
        );
        Delivery late = delivery(
            10L, group, DeliveryStatus.READY,
            LocalDateTime.of(today, java.time.LocalTime.of(16, 10, 1))
        );
        stubOperations(List.of(group), List.of(late), List.of());

        long count = service.getCounts(7L, UserRole.ADMIN, null, null).lateOrderReview();
        long totalElements = service.getOperations(
            7L, UserRole.ADMIN, AdminDeliveryOperationType.LATE_ORDER_REVIEW,
            null, null, PageRequest.of(0, 20)
        ).totalElements();

        assertThat(totalElements).isEqualTo(count);
    }

    private void stubOperations(
        List<DeliveryGroup> groups
        , List<Delivery> deliveries
        , List<DeliveryAssignment> assignments
    ) {
        List<Long> groupIds = groups.stream().map(DeliveryGroup::getId).toList();
        when(groupRepository.findAllForOperations(null, null)).thenReturn(groups);
        when(deliveryRepository.findAllByDeliveryGroupIdIn(groupIds)).thenReturn(deliveries);
        when(assignmentRepository.findAllByDeliveryGroupIdIn(groupIds)).thenReturn(assignments);
    }

    private int operationSize(AdminDeliveryOperationType type, LocalDateTime now) {
        return service.operations(null, null, now).getOrDefault(type, List.of()).size();
    }

    private DeliveryGroup group(
        Long id
        , LocalDate date
        , DeliverySlotCode slotCode
        , DeliveryGroupStatus status
    ) {
        DeliverySlot slot = mock(DeliverySlot.class);
        lenient().when(slot.getCode()).thenReturn(slotCode);
        DeliveryGroup group = mock(DeliveryGroup.class);
        lenient().when(group.getId()).thenReturn(id);
        lenient().when(group.getDeliveryDate()).thenReturn(date);
        lenient().when(group.getSlot()).thenReturn(slot);
        lenient().when(group.getStatus()).thenReturn(status);
        return group;
    }

    private Delivery delivery(
        Long id
        , DeliveryGroup group
        , DeliveryStatus status
        , LocalDateTime createdAt
    ) {
        Delivery delivery = mock(Delivery.class);
        lenient().when(delivery.getId()).thenReturn(id);
        lenient().when(delivery.getDeliveryPublicId()).thenReturn("delivery-" + id);
        lenient().when(delivery.getDeliveryGroup()).thenReturn(group);
        lenient().when(delivery.getStatus()).thenReturn(status);
        lenient().when(delivery.getCreatedAt()).thenReturn(createdAt);
        return delivery;
    }

    private DeliveryAssignment assignment(
        Long id
        , DeliveryGroup group
        , DeliveryAssignmentStatus status
    ) {
        Rider rider = mock(Rider.class);
        lenient().when(rider.getId()).thenReturn(100L);
        DeliveryAssignment assignment = mock(DeliveryAssignment.class);
        lenient().when(assignment.getId()).thenReturn(id);
        lenient().when(assignment.getDeliveryGroup()).thenReturn(group);
        lenient().when(assignment.getStatus()).thenReturn(status);
        lenient().when(assignment.getRider()).thenReturn(rider);
        return assignment;
    }
}
