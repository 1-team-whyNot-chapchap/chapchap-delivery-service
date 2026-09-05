package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.event.RiderAssignmentAvailableEvent;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.request.AdminManualAssignmentItemRequest;
import com.chapchap.delivery.domain.assignment.request.AdminManualAssignmentsRequest;
import com.chapchap.delivery.domain.assignment.response.ManualAssignmentsResponse;
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
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentStateConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
class AdminManualAssignmentServiceTest {
    private static final Long ACTOR_ID = 1L;
    private static final Long GROUP_ID = 10L;
    private static final Long RIDER_ID = 20L;
    private static final String DELIVERY_PUBLIC_ID = "delivery-public-id";

    @Mock private DeliveryGroupRepository deliveryGroupRepository;
    @Mock private RiderRepository riderRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryAssignmentRepository deliveryAssignmentRepository;
    @Mock private DeliveryAssignmentItemRepository deliveryAssignmentItemRepository;
    @Mock private DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;
    @Mock private AuditHistoryRepository auditHistoryRepository;
    @Mock private DeliveryAccessService deliveryAccessService;
    @Mock private RiderAssignmentEligibilityService riderAssignmentEligibilityService;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void manualAssignmentAssignsEveryReadyDeliveryOnce() {
        Fixture fixture = mockWaitingGroup();
        DeliveryGroup group = fixture.group();
        Rider rider = fixture.rider();
        DeliveryAssignment savedAssignment = mock(DeliveryAssignment.class);
        when(rider.getId()).thenReturn(RIDER_ID);
        when(deliveryAssignmentRepository.save(any())).thenReturn(savedAssignment);
        when(savedAssignment.getId()).thenReturn(30L);
        when(riderAssignmentEligibilityService.isEligible(
            rider, LocalDate.of(2026, 9, 7), DeliverySlotCode.LUNCH, "DAEGU_JUNG_GU"
        )).thenReturn(true);

        ManualAssignmentsResponse response = service().assign(
            ACTOR_ID
            , UserRole.ADMIN
            , GROUP_ID
            , request(List.of(new AdminManualAssignmentItemRequest(
                RIDER_ID, List.of(DELIVERY_PUBLIC_ID), false, null, null
            )))
        );

        assertThat(response.status()).isEqualTo(DeliveryGroupStatus.WAITING_RIDER);
        assertThat(response.assignmentIds()).containsExactly(30L);
        verify(group).completeManualAssignment();
        verify(deliveryAssignmentItemRepository).saveAll(any());
        verify(auditHistoryRepository).save(any());
        verify(applicationEventPublisher).publishEvent(
            any(RiderAssignmentAvailableEvent.class)
        );
    }

    @Test
    void manualAssignmentRejectsDuplicateDeliveryAcrossAssignments() {
        DeliveryGroup group = mock(DeliveryGroup.class);
        when(group.isWaitingAutoAssignment()).thenReturn(true);
        when(deliveryGroupRepository.findByIdForUpdate(GROUP_ID)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service().assign(
            ACTOR_ID
            , UserRole.ADMIN
            , GROUP_ID
            , request(List.of(
                new AdminManualAssignmentItemRequest(RIDER_ID, List.of(DELIVERY_PUBLIC_ID), false, null, null),
                new AdminManualAssignmentItemRequest(RIDER_ID, List.of(DELIVERY_PUBLIC_ID), false, null, null)
            ))
        )).isInstanceOf(DeliveryAssignmentStateConflictException.class);

        verify(deliveryAssignmentRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(
            any(RiderAssignmentAvailableEvent.class)
        );
    }

    private AdminManualAssignmentService service() {
        return new AdminManualAssignmentService(
            deliveryGroupRepository, riderRepository, deliveryRepository,
            deliveryAssignmentRepository, deliveryAssignmentItemRepository,
            deliveryGroupStatusHistoryRepository, auditHistoryRepository,
            deliveryAccessService, riderAssignmentEligibilityService, applicationEventPublisher
        );
    }

    private Fixture mockWaitingGroup() {
        DeliveryGroup group = mock(DeliveryGroup.class);
        DeliverySlot slot = mock(DeliverySlot.class);
        Rider rider = mock(Rider.class);
        Delivery delivery = mock(Delivery.class);
        when(group.isWaitingAutoAssignment()).thenReturn(true);
        when(group.getDeliveryDate()).thenReturn(LocalDate.of(2026, 9, 7));
        when(group.getSlot()).thenReturn(slot);
        when(slot.getCode()).thenReturn(DeliverySlotCode.LUNCH);
        when(deliveryGroupRepository.findByIdForUpdate(GROUP_ID)).thenReturn(Optional.of(group));
        when(rider.getId()).thenReturn(RIDER_ID);
        when(riderRepository.findAllByIdInForUpdate(List.of(RIDER_ID))).thenReturn(List.of(rider));
        when(delivery.getDeliveryPublicId()).thenReturn(DELIVERY_PUBLIC_ID);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.READY);
        when(delivery.getLunchboxQuantity()).thenReturn(1);
        when(delivery.getDeliveryAreaCode()).thenReturn("DAEGU_JUNG_GU");
        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID)).thenReturn(List.of(delivery));
        when(deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID)).thenReturn(List.of());
        when(deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID)).thenReturn(List.of());
        return new Fixture(group, rider);
    }

    private AdminManualAssignmentsRequest request(List<AdminManualAssignmentItemRequest> assignments) {
        return new AdminManualAssignmentsRequest(assignments);
    }

    private record Fixture(DeliveryGroup group, Rider rider) {
    }
}
