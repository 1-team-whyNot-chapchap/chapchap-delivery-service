package com.chapchap.delivery.domain.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.response.AdminRiderCandidateListResponse;
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
import com.chapchap.delivery.domain.rider.service.RiderDeliveryAreaService;
import com.chapchap.delivery.global.exception.business.DeliveryGroupNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminRiderCandidateQueryServiceTest {
    private static final Long ACTOR_ID = 1L;
    private static final Long GROUP_ID = 10L;
    private static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 9, 12);

    @Mock private DeliveryAccessService deliveryAccessService;
    @Mock private DeliveryGroupRepository deliveryGroupRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryAssignmentRepository deliveryAssignmentRepository;
    @Mock private DeliveryAssignmentItemRepository deliveryAssignmentItemRepository;
    @Mock private RiderRepository riderRepository;
    @Mock private RiderAssignmentEligibilityService riderAssignmentEligibilityService;
    @Mock private RiderDeliveryAreaService riderDeliveryAreaService;

    @Test
    void returnsEligibleAndIneligibleCandidatesWithProjectedCapacity() {
        DeliveryGroup group = group();
        when(group.isWaitingAutoAssignment()).thenReturn(true);
        Delivery delivery = delivery(101L, "SEOUL_GANGNAM", 40);
        Rider eligibleRider = rider(20L, true);
        Rider inactiveRider = rider(21L, false);
        Rider leaveRider = rider(22L, true);

        when(deliveryGroupRepository.findDetailById(GROUP_ID)).thenReturn(Optional.of(group));
        when(deliveryRepository.findAllByDeliveryGroupIdIn(List.of(GROUP_ID))).thenReturn(List.of(delivery));
        when(deliveryAssignmentRepository.findAllByDeliveryGroupIdIn(List.of(GROUP_ID))).thenReturn(List.of());
        when(deliveryAssignmentItemRepository.findAllByDeliveryGroupIdIn(List.of(GROUP_ID))).thenReturn(List.of());
        when(riderRepository.findAllByDeletedAtIsNullOrderByIdAsc())
            .thenReturn(List.of(eligibleRider, inactiveRider, leaveRider));
        when(riderAssignmentEligibilityService.isEligibleIgnoringArea(
            eligibleRider, DELIVERY_DATE, DeliverySlotCode.LUNCH
        )).thenReturn(true);
        when(riderAssignmentEligibilityService.isEligibleIgnoringArea(
            inactiveRider, DELIVERY_DATE, DeliverySlotCode.LUNCH
        )).thenReturn(false);
        when(riderAssignmentEligibilityService.isEligibleIgnoringArea(
            leaveRider, DELIVERY_DATE, DeliverySlotCode.LUNCH
        )).thenReturn(false);
        when(riderDeliveryAreaService.canDeliverToArea(20L, "SEOUL_GANGNAM", DELIVERY_DATE)).thenReturn(true);
        when(riderDeliveryAreaService.canDeliverToArea(21L, "SEOUL_GANGNAM", DELIVERY_DATE)).thenReturn(true);
        when(riderDeliveryAreaService.canDeliverToArea(22L, "SEOUL_GANGNAM", DELIVERY_DATE)).thenReturn(true);

        AdminRiderCandidateListResponse response = service().getCandidates(
            ACTOR_ID, UserRole.ADMIN, GROUP_ID, null
        );

        assertThat(response.deliveryGroupId()).isEqualTo(GROUP_ID);
        assertThat(response.deliveryDate()).isEqualTo(DELIVERY_DATE);
        assertThat(response.deliverySlot()).isEqualTo(DeliverySlotCode.LUNCH);
        assertThat(response.items()).extracting(AdminRiderCandidateListResponse.Item::riderId)
            .containsExactly(20L, 21L, 22L);
        assertThat(response.items().getFirst())
            .extracting(
                AdminRiderCandidateListResponse.Item::isEligible,
                AdminRiderCandidateListResponse.Item::recommendedCapacityExceeded,
                AdminRiderCandidateListResponse.Item::maximumCapacityExceeded,
                AdminRiderCandidateListResponse.Item::isAreaMatched
            )
            .containsExactly(true, false, false, true);
        assertThat(response.items().get(1).isEligible()).isFalse();
        assertThat(response.items().get(2).isEligible()).isFalse();
        verify(deliveryAccessService).validateAdminAccess(ACTOR_ID, UserRole.ADMIN);
    }

    @Test
    void marksMaximumCapacityCandidateIneligible() {
        DeliveryGroup group = group();
        Delivery delivery = mock(Delivery.class);
        when(delivery.getDeliveryAreaCode()).thenReturn("SEOUL_GANGNAM");
        when(delivery.getLunchboxQuantity()).thenReturn(43);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.READY);
        Rider originalRider = mock(Rider.class);
        when(originalRider.getId()).thenReturn(19L);
        Rider rider = rider(20L, true);
        DeliveryAssignment assignment = mock(DeliveryAssignment.class);
        DeliveryAssignmentItem item = mock(DeliveryAssignmentItem.class);

        when(group.getStatus()).thenReturn(DeliveryGroupStatus.CONFIRMED);
        when(assignment.getId()).thenReturn(30L);
        when(assignment.getRider()).thenReturn(originalRider);
        when(assignment.getStatus()).thenReturn(DeliveryAssignmentStatus.CONFIRMED);
        when(item.getAssignment()).thenReturn(assignment);
        when(item.getDelivery()).thenReturn(delivery);

        when(deliveryGroupRepository.findDetailById(GROUP_ID)).thenReturn(Optional.of(group));
        when(deliveryRepository.findAllByDeliveryGroupIdIn(List.of(GROUP_ID))).thenReturn(List.of(delivery));
        when(deliveryAssignmentRepository.findAllByDeliveryGroupIdIn(List.of(GROUP_ID))).thenReturn(List.of(assignment));
        when(deliveryAssignmentItemRepository.findAllByDeliveryGroupIdIn(List.of(GROUP_ID))).thenReturn(List.of(item));
        when(riderRepository.findAllByDeletedAtIsNullOrderByIdAsc()).thenReturn(List.of(originalRider, rider));
        when(riderAssignmentEligibilityService.isEligibleIgnoringArea(
            rider, DELIVERY_DATE, DeliverySlotCode.LUNCH
        )).thenReturn(true);
        when(riderDeliveryAreaService.canDeliverToArea(20L, "SEOUL_GANGNAM", DELIVERY_DATE)).thenReturn(true);

        AdminRiderCandidateListResponse.Item candidate = service().getCandidates(
            ACTOR_ID, UserRole.ADMIN, GROUP_ID, 30L
        ).items().getFirst();

        assertThat(candidate.maximumCapacityExceeded()).isTrue();
        assertThat(candidate.isEligible()).isFalse();
    }

    @Test
    void excludesOriginalRiderAndRequiresAreaMatchForReplacement() {
        DeliveryGroup group = group();
        Delivery delivery = mock(Delivery.class);
        when(delivery.getDeliveryAreaCode()).thenReturn("SEOUL_GANGNAM");
        when(delivery.getLunchboxQuantity()).thenReturn(8);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.READY);
        Rider originalRider = mock(Rider.class);
        when(originalRider.getId()).thenReturn(20L);
        Rider areaMismatchRider = rider(21L, true);
        DeliveryAssignment assignment = mock(DeliveryAssignment.class);
        DeliveryAssignmentItem item = mock(DeliveryAssignmentItem.class);

        when(assignment.getId()).thenReturn(30L);
        when(assignment.getRider()).thenReturn(originalRider);
        when(assignment.getStatus()).thenReturn(DeliveryAssignmentStatus.CONFIRMED);
        when(item.getAssignment()).thenReturn(assignment);
        when(item.getDelivery()).thenReturn(delivery);
        when(deliveryGroupRepository.findDetailById(GROUP_ID)).thenReturn(Optional.of(group));
        when(group.getStatus()).thenReturn(DeliveryGroupStatus.CONFIRMED);
        when(deliveryRepository.findAllByDeliveryGroupIdIn(List.of(GROUP_ID))).thenReturn(List.of(delivery));
        when(deliveryAssignmentRepository.findAllByDeliveryGroupIdIn(List.of(GROUP_ID))).thenReturn(List.of(assignment));
        when(deliveryAssignmentItemRepository.findAllByDeliveryGroupIdIn(List.of(GROUP_ID))).thenReturn(List.of(item));
        when(riderRepository.findAllByDeletedAtIsNullOrderByIdAsc()).thenReturn(List.of(originalRider, areaMismatchRider));
        when(riderAssignmentEligibilityService.isEligibleIgnoringArea(
            areaMismatchRider, DELIVERY_DATE, DeliverySlotCode.LUNCH
        )).thenReturn(true);
        when(riderDeliveryAreaService.canDeliverToArea(21L, "SEOUL_GANGNAM", DELIVERY_DATE)).thenReturn(false);

        AdminRiderCandidateListResponse response = service().getCandidates(
            ACTOR_ID, UserRole.ADMIN, GROUP_ID, 30L
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst())
            .extracting(
                AdminRiderCandidateListResponse.Item::riderId,
                AdminRiderCandidateListResponse.Item::isEligible,
                AdminRiderCandidateListResponse.Item::isAreaMatched
            )
            .containsExactly(21L, false, false);
    }

    @Test
    void throwsExistingNotFoundPolicyWhenGroupDoesNotExist() {
        when(deliveryGroupRepository.findDetailById(GROUP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getCandidates(
            ACTOR_ID, UserRole.ADMIN, GROUP_ID, null
        )).isInstanceOf(DeliveryGroupNotFoundException.class);
    }

    private AdminRiderCandidateQueryService service() {
        return new AdminRiderCandidateQueryService(
            deliveryAccessService, deliveryGroupRepository, deliveryRepository, deliveryAssignmentRepository,
            deliveryAssignmentItemRepository, riderRepository, riderAssignmentEligibilityService,
            riderDeliveryAreaService
        );
    }

    private DeliveryGroup group() {
        DeliveryGroup group = mock(DeliveryGroup.class);
        DeliverySlot slot = mock(DeliverySlot.class);
        when(group.getId()).thenReturn(GROUP_ID);
        when(group.getDeliveryDate()).thenReturn(DELIVERY_DATE);
        when(group.getSlot()).thenReturn(slot);
        when(slot.getCode()).thenReturn(DeliverySlotCode.LUNCH);
        return group;
    }

    private Delivery delivery(Long id, String areaCode, int lunchboxQuantity) {
        Delivery delivery = mock(Delivery.class);
        when(delivery.getId()).thenReturn(id);
        when(delivery.getDeliveryAreaCode()).thenReturn(areaCode);
        when(delivery.getLunchboxQuantity()).thenReturn(lunchboxQuantity);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.READY);
        return delivery;
    }

    private Rider rider(Long riderId, boolean active) {
        Rider rider = mock(Rider.class);
        when(rider.getId()).thenReturn(riderId);
        when(rider.getIsDeliveryActive()).thenReturn(active);
        return rider;
    }
}
