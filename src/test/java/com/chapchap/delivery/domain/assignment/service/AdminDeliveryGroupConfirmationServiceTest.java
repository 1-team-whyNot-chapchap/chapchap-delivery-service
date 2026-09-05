package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentIssueRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.response.DeliveryGroupConfirmationResponse;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryRecipientSnapshot;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRecipientSnapshotRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.DeliveryGroupConfirmationConditionNotMetException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDeliveryGroupConfirmationServiceTest {
    @Mock private DeliveryGroupRepository deliveryGroupRepository;
    @Mock private RiderRepository riderRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryRecipientSnapshotRepository deliveryRecipientSnapshotRepository;
    @Mock private DeliveryAssignmentRepository deliveryAssignmentRepository;
    @Mock private DeliveryAssignmentItemRepository deliveryAssignmentItemRepository;
    @Mock private DeliveryAssignmentIssueRepository deliveryAssignmentIssueRepository;
    @Mock private DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;
    @Mock private AuditHistoryRepository auditHistoryRepository;
    @Mock private DeliveryAccessService deliveryAccessService;
    @Mock private RiderAssignmentEligibilityService riderAssignmentEligibilityService;

    @Test
    void confirmsAcknowledgedAssignmentsWithExactlyOneOwnerPerDelivery() {
        Fixture fixture = fixture(DeliveryAssignmentStatus.ACKNOWLEDGED);
        when(riderAssignmentEligibilityService.isEligible(
            fixture.rider(), LocalDate.of(2026, 9, 7), DeliverySlotCode.LUNCH, "DAEGU_JUNG_GU"
        )).thenReturn(true);

        DeliveryGroupConfirmationResponse response = service().confirm(1L, UserRole.ADMIN, 10L);

        assertThat(response.status()).isEqualTo(DeliveryGroupStatus.CONFIRMED);
        verify(fixture.assignment()).confirm(any(), any());
        verify(fixture.group()).confirm();
        verify(deliveryGroupStatusHistoryRepository).save(any());
        verify(auditHistoryRepository).save(any());
    }

    @Test
    void rejectsConfirmationWhenAnAssignmentIsNotAcknowledged() {
        DeliveryGroup group = mock(DeliveryGroup.class);
        Rider rider = mock(Rider.class);
        Delivery delivery = mock(Delivery.class);
        DeliveryAssignment assignment = mock(DeliveryAssignment.class);
        when(group.getStatus()).thenReturn(DeliveryGroupStatus.READY_TO_CONFIRM);
        when(group.getId()).thenReturn(10L);
        when(rider.getId()).thenReturn(20L);
        when(assignment.getStatus()).thenReturn(DeliveryAssignmentStatus.ASSIGNED);
        when(deliveryGroupRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(group));
        when(deliveryAssignmentRepository.findActiveRiderIdsByDeliveryGroupId(10L, DeliveryAssignmentStatus.REASSIGNED))
            .thenReturn(List.of(20L));
        when(riderRepository.findAllByIdInForUpdate(List.of(20L))).thenReturn(List.of(rider));
        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(10L)).thenReturn(List.of(delivery));
        when(deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(10L)).thenReturn(List.of(assignment));
        when(deliveryAssignmentIssueRepository.countUnresolvedByDeliveryGroupId(10L)).thenReturn(0L);

        assertThatThrownBy(() -> service().confirm(1L, UserRole.ADMIN, 10L))
            .isInstanceOf(DeliveryGroupConfirmationConditionNotMetException.class);

        verify(assignment, never()).confirm(any(), any());
        verify(group, never()).confirm();
    }

    private AdminDeliveryGroupConfirmationService service() {
        return new AdminDeliveryGroupConfirmationService(
            deliveryGroupRepository, riderRepository, deliveryRepository,
            deliveryRecipientSnapshotRepository,
            deliveryAssignmentRepository, deliveryAssignmentItemRepository,
            deliveryAssignmentIssueRepository, deliveryGroupStatusHistoryRepository,
            auditHistoryRepository, deliveryAccessService, riderAssignmentEligibilityService
        );
    }

    private Fixture fixture(DeliveryAssignmentStatus status) {
        return fixture(status, true);
    }

    private Fixture fixture(
        DeliveryAssignmentStatus status
        , boolean hasRecipientSnapshot
    ) {
        DeliveryGroup group = mock(DeliveryGroup.class);
        DeliverySlot slot = mock(DeliverySlot.class);
        Rider rider = mock(Rider.class);
        Delivery delivery = mock(Delivery.class);
        DeliveryAssignment assignment = mock(DeliveryAssignment.class);
        DeliveryAssignmentItem item = mock(DeliveryAssignmentItem.class);
        DeliveryRecipientSnapshot recipientSnapshot = mock(DeliveryRecipientSnapshot.class);
        when(group.getStatus()).thenReturn(DeliveryGroupStatus.READY_TO_CONFIRM);
        when(group.getId()).thenReturn(10L);
        when(group.getDeliveryDate()).thenReturn(LocalDate.of(2026, 9, 7));
        when(group.getSlot()).thenReturn(slot);
        when(slot.getCode()).thenReturn(DeliverySlotCode.LUNCH);
        when(rider.getId()).thenReturn(20L);
        when(delivery.getId()).thenReturn(30L);
        if (hasRecipientSnapshot) {
            when(recipientSnapshot.getDeliveryId()).thenReturn(30L);
        }
        when(delivery.getStatus()).thenReturn(DeliveryStatus.READY);
        when(delivery.getLunchboxQuantity()).thenReturn(1);
        when(delivery.getDeliveryAreaCode()).thenReturn("DAEGU_JUNG_GU");
        when(assignment.getId()).thenReturn(40L);
        when(assignment.getRider()).thenReturn(rider);
        when(assignment.getStatus()).thenReturn(status);
        when(assignment.isAcknowledged()).thenReturn(status == DeliveryAssignmentStatus.ACKNOWLEDGED);
        when(item.getAssignment()).thenReturn(assignment);
        when(item.getDelivery()).thenReturn(delivery);
        when(deliveryGroupRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(group));
        when(deliveryAssignmentRepository.findActiveRiderIdsByDeliveryGroupId(10L, DeliveryAssignmentStatus.REASSIGNED))
            .thenReturn(List.of(20L));
        when(riderRepository.findAllByIdInForUpdate(List.of(20L))).thenReturn(List.of(rider));
        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(10L)).thenReturn(List.of(delivery));
        when(deliveryRecipientSnapshotRepository.findAllByDeliveryIdIn(List.of(30L)))
            .thenReturn(hasRecipientSnapshot ? List.of(recipientSnapshot) : List.of());
        when(deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(10L)).thenReturn(List.of(assignment));
        when(deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(10L)).thenReturn(List.of(item));
        when(deliveryAssignmentIssueRepository.countUnresolvedByDeliveryGroupId(10L)).thenReturn(0L);
        return new Fixture(group, rider, assignment);
    }

    @Test
    void rejectsConfirmationWhenRecipientSnapshotIsMissing() {
        Fixture fixture = fixture(DeliveryAssignmentStatus.ACKNOWLEDGED, false);
        when(riderAssignmentEligibilityService.isEligible(
            fixture.rider(), LocalDate.of(2026, 9, 7), DeliverySlotCode.LUNCH, "DAEGU_JUNG_GU"
        )).thenReturn(true);
        assertThatThrownBy(() -> service().confirm(1L, UserRole.ADMIN, 10L))
            .isInstanceOf(DeliveryGroupConfirmationConditionNotMetException.class);

        verify(fixture.assignment(), never()).confirm(any(), any());
        verify(fixture.group(), never()).confirm();
    }

    private record Fixture(DeliveryGroup group, Rider rider, DeliveryAssignment assignment) {
    }
}
