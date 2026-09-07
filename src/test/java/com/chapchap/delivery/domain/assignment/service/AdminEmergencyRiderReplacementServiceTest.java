package com.chapchap.delivery.domain.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.EmergencyRiderReplacementReason;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.event.RiderAssignmentReassignedEvent;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.request.AdminEmergencyRiderReplacementRequest;
import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentStateConflictException;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AdminEmergencyRiderReplacementServiceTest {
    @Mock private DeliveryAccessService accessService;
    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private DeliveryAssignmentItemRepository itemRepository;
    @Mock private DeliveryGroupRepository groupRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private RiderRepository riderRepository;
    @Mock private RiderAssignmentEligibilityService eligibilityService;
    @Mock private AuditHistoryRepository auditRepository;
    @Mock private EntityManager entityManager;
    @Mock private ApplicationEventPublisher eventPublisher;

    private AdminEmergencyRiderReplacementService service;

    @BeforeEach
    void setUp() {
        service = new AdminEmergencyRiderReplacementService(
            accessService, assignmentRepository, itemRepository, groupRepository,
            deliveryRepository, riderRepository, eligibilityService, auditRepository,
            entityManager, eventPublisher
        );
    }

    @Test
    @DisplayName("확정 후 긴급 교체는 기존 배정을 종료하고 동일 대상의 신규 배정을 생성한다")
    void replacesConfirmedAssignmentBeforeStart() {
        long assignmentId = 30L;
        DeliveryGroup group = mock(DeliveryGroup.class);
        DeliverySlot slot = mock(DeliverySlot.class);
        Rider previousRider = rider(10L);
        Rider newRider = rider(20L);
        DeliveryAssignment original = mock(DeliveryAssignment.class);
        Delivery delivery = mock(Delivery.class);
        DeliveryAssignmentItem item = mock(DeliveryAssignmentItem.class);
        DeliveryAssignment replacement = mock(DeliveryAssignment.class);

        when(original.getId()).thenReturn(assignmentId);
        when(original.getDeliveryGroup()).thenReturn(group);
        when(original.getRider()).thenReturn(previousRider);
        when(original.getStatus()).thenReturn(
            DeliveryAssignmentStatus.CONFIRMED,
            DeliveryAssignmentStatus.CONFIRMED,
            DeliveryAssignmentStatus.REASSIGNED
        );
        when(group.getId()).thenReturn(40L);
        when(group.getStatus()).thenReturn(DeliveryGroupStatus.CONFIRMED);
        when(group.getDeliveryDate()).thenReturn(LocalDate.of(2026, 9, 7));
        when(group.getSlot()).thenReturn(slot);
        when(slot.getCode()).thenReturn(DeliverySlotCode.LUNCH);
        when(delivery.getId()).thenReturn(50L);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.READY);
        when(delivery.getDeliveryAreaCode()).thenReturn("DAEGU_JUNG_GU");
        when(delivery.getLunchboxQuantity()).thenReturn(2);
        when(item.getAssignment()).thenReturn(original);
        when(item.getDelivery()).thenReturn(delivery);
        when(replacement.getId()).thenReturn(60L);
        when(replacement.getStatus()).thenReturn(DeliveryAssignmentStatus.ASSIGNED);

        when(assignmentRepository.findByIdAndDeletedAtIsNull(assignmentId))
            .thenReturn(Optional.of(original));
        when(groupRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(group));
        when(riderRepository.findAllByIdInForUpdate(List.of(10L, 20L)))
            .thenReturn(List.of(previousRider, newRider));
        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(40L))
            .thenReturn(List.of(delivery));
        when(assignmentRepository.findAllByDeliveryGroupIdForUpdate(40L))
            .thenReturn(List.of(original));
        when(itemRepository.findAllByDeliveryGroupIdForUpdate(40L)).thenReturn(List.of(item));
        when(eligibilityService.isEligible(newRider, LocalDate.of(2026, 9, 7), DeliverySlotCode.LUNCH, "DAEGU_JUNG_GU"))
            .thenReturn(true);
        when(assignmentRepository.reassignIfConfirmed(
            assignmentId, DeliveryAssignmentStatus.CONFIRMED, DeliveryAssignmentStatus.REASSIGNED
        )).thenReturn(1);
        when(assignmentRepository.save(any(DeliveryAssignment.class))).thenReturn(replacement);

        var response = service.replace(
            900L, UserRole.ADMIN, assignmentId,
            new AdminEmergencyRiderReplacementRequest(
                20L, EmergencyRiderReplacementReason.OTHER, "  기사 건강 문제  "
            )
        );

        assertThat(response.previousAssignmentStatus()).isEqualTo(DeliveryAssignmentStatus.REASSIGNED);
        assertThat(response.newAssignmentStatus()).isEqualTo(DeliveryAssignmentStatus.ASSIGNED);
        assertThat(response.previousRiderId()).isEqualTo(10L);
        assertThat(response.newRiderId()).isEqualTo(20L);
        verify(itemRepository).saveAll(any());
        ArgumentCaptor<AuditHistory> audit = ArgumentCaptor.forClass(AuditHistory.class);
        verify(auditRepository).save(audit.capture());
        assertThat(audit.getValue().getReasonDetail()).isEqualTo("기사 건강 문제");
        verify(eventPublisher).publishEvent(any(RiderAssignmentReassignedEvent.class));
    }

    @Test
    @DisplayName("전체 배송이 확정 상태가 아니면 긴급 기사 교체를 거절한다")
    void rejectsNonConfirmedGroup() {
        DeliveryAssignment assignment = mock(DeliveryAssignment.class);
        DeliveryGroup group = mock(DeliveryGroup.class);
        Rider rider = rider(10L);
        when(assignment.getDeliveryGroup()).thenReturn(group);
        when(assignment.getRider()).thenReturn(rider);
        when(group.getId()).thenReturn(40L);
        when(group.getStatus()).thenReturn(DeliveryGroupStatus.IN_PROGRESS);
        when(assignmentRepository.findByIdAndDeletedAtIsNull(30L))
            .thenReturn(Optional.of(assignment));
        when(groupRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.replace(
            900L, UserRole.ADMIN, 30L,
            new AdminEmergencyRiderReplacementRequest(
                20L, EmergencyRiderReplacementReason.OTHER, "기사 건강 문제"
            )
        )).isInstanceOf(DeliveryAssignmentStateConflictException.class);
    }

    @Test
    @DisplayName("긴급 기사 교체의 OTHER 사유에는 상세 설명이 필요하다")
    void otherReasonRequiresDetail() {
        assertThatThrownBy(() -> service.replace(
            900L, UserRole.ADMIN, 30L,
            new AdminEmergencyRiderReplacementRequest(
                20L, EmergencyRiderReplacementReason.OTHER, "   "
            )
        )).isInstanceOf(com.chapchap.delivery.global.exception.business.OtherReasonDetailRequiredException.class);
    }

    private Rider rider(Long id) {
        Rider rider = mock(Rider.class);
        when(rider.getId()).thenReturn(id);
        return rider;
    }
}
