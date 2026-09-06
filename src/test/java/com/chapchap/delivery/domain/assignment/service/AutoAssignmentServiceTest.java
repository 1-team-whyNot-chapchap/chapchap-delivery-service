package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.event.RiderAssignmentAvailableEvent;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroupStatusHistory;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AutoAssignmentServiceTest {
    @Mock
    private DeliveryGroupRepository deliveryGroupRepository;

    @Mock
    private RiderRepository riderRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private DeliveryAssignmentItemRepository deliveryAssignmentItemRepository;

    @Mock
    private DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;

    @Mock
    private RiderAssignmentEligibilityService riderAssignmentEligibilityService;

    private AutoAssignmentService autoAssignmentService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @BeforeEach
    void setUp() {
        autoAssignmentService =
            new AutoAssignmentService(
                deliveryGroupRepository
                , riderRepository
                , deliveryRepository
                , deliveryAssignmentRepository
                , deliveryAssignmentItemRepository
                , deliveryGroupStatusHistoryRepository
                , riderAssignmentEligibilityService
                , applicationEventPublisher
            );
    }

    @Test
    void assignSuccess() {
        Long deliveryGroupId = 1L;
        Long riderId = 10L;
        LocalDate deliveryDate = LocalDate.of(2026, 9, 5);

        DeliverySlot slot = mock(DeliverySlot.class);
        DeliveryGroup deliveryGroup = mock(DeliveryGroup.class);
        Rider rider = mock(Rider.class);
        Delivery delivery = mock(Delivery.class);

        when(slot.getCode())
            .thenReturn(DeliverySlotCode.LUNCH);

        when(deliveryGroup.isWaitingAutoAssignment())
            .thenReturn(true);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(deliveryDate);

        when(deliveryGroup.getSlot())
            .thenReturn(slot);

        when(rider.getId())
            .thenReturn(riderId);

        when(delivery.getStatus())
            .thenReturn(DeliveryStatus.READY);

        when(delivery.getDeliveryAreaCode())
            .thenReturn("DAEGU_JUNG_GU");

        when(delivery.getLunchboxQuantity())
            .thenReturn(2);

        when(deliveryGroupRepository.findByIdForUpdate(deliveryGroupId))
            .thenReturn(Optional.of(deliveryGroup));

        when(riderRepository.findAllByDeletedAtIsNullOrderByIdAsc())
            .thenReturn(List.of(rider));

        when(riderRepository.findAllByIdInForUpdate(List.of(riderId)))
            .thenReturn(List.of(rider));

        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of(delivery));

        when(deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of());

        when(deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of());

        when(
            riderAssignmentEligibilityService.isEligible(
                rider
                , deliveryDate
                , DeliverySlotCode.LUNCH
                , "DAEGU_JUNG_GU"
            )
        )
            .thenReturn(true);

        when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        boolean result =
            autoAssignmentService.assign(
                deliveryGroupId
            );

        assertThat(result).isTrue();

        verify(deliveryAssignmentRepository)
            .save(any(DeliveryAssignment.class));

        verify(deliveryAssignmentItemRepository)
            .saveAll(any());

        verify(deliveryGroup)
            .completeAutoAssignment(any(LocalDateTime.class));

        verify(applicationEventPublisher)
            .publishEvent(any(RiderAssignmentAvailableEvent.class));

        ArgumentCaptor<DeliveryGroupStatusHistory> historyCaptor =
            ArgumentCaptor.forClass(
                DeliveryGroupStatusHistory.class
            );

        verify(deliveryGroupStatusHistoryRepository)
            .save(historyCaptor.capture());

        DeliveryGroupStatusHistory history =
            historyCaptor.getValue();

        assertThat(history.getDeliveryGroup())
            .isEqualTo(deliveryGroup);

        assertThat(history.getFromStatus())
            .isEqualTo(DeliveryGroupStatus.WAITING_ASSIGNMENT);

        assertThat(history.getToStatus())
            .isEqualTo(DeliveryGroupStatus.WAITING_RIDER);

        assertThat(history.getChangedBy())
            .isNull();

        assertThat(history.getChangedByType())
            .isEqualTo(DeliveryGroupChangedByType.SYSTEM);

        assertThat(history.getChangedAt())
            .isNotNull();
    }

    @Test
    void assignFailsWhenNoRiderCandidate() {
        Long deliveryGroupId = 1L;

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        when(deliveryGroup.isWaitingAutoAssignment())
            .thenReturn(true);

        when(deliveryGroupRepository.findByIdForUpdate(deliveryGroupId))
            .thenReturn(Optional.of(deliveryGroup));

        when(riderRepository.findAllByDeletedAtIsNullOrderByIdAsc())
            .thenReturn(List.of());

        boolean result =
            autoAssignmentService.assign(
                deliveryGroupId
            );

        assertThat(result).isFalse();

        verify(deliveryAssignmentRepository, never())
            .save(any(DeliveryAssignment.class));

        verify(deliveryAssignmentItemRepository, never())
            .saveAll(any());

        verify(deliveryGroup, never())
            .completeAutoAssignment(any(LocalDateTime.class));

        verify(deliveryGroupStatusHistoryRepository, never())
            .save(any(DeliveryGroupStatusHistory.class));
    }
    @Test
    void assignFailsWhenDeliveryIsNotReady() {
        Long deliveryGroupId = 1L;
        Long riderId = 10L;

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        Rider rider =
            mock(Rider.class);

        Delivery delivery =
            mock(Delivery.class);

        when(deliveryGroup.isWaitingAutoAssignment())
            .thenReturn(true);

        when(rider.getId())
            .thenReturn(riderId);

        when(delivery.getStatus())
            .thenReturn(DeliveryStatus.DELIVERING);

        when(deliveryGroupRepository.findByIdForUpdate(deliveryGroupId))
            .thenReturn(Optional.of(deliveryGroup));

        when(riderRepository.findAllByDeletedAtIsNullOrderByIdAsc())
            .thenReturn(List.of(rider));

        when(riderRepository.findAllByIdInForUpdate(List.of(riderId)))
            .thenReturn(List.of(rider));

        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of(delivery));

        boolean result =
            autoAssignmentService.assign(
                deliveryGroupId
            );

        assertThat(result).isFalse();

        verify(deliveryAssignmentRepository, never())
            .save(any(DeliveryAssignment.class));

        verify(deliveryAssignmentItemRepository, never())
            .saveAll(any());

        verify(deliveryGroup, never())
            .completeAutoAssignment(any(LocalDateTime.class));

        verify(deliveryGroupStatusHistoryRepository, never())
            .save(any(DeliveryGroupStatusHistory.class));
    }

    @Test
    void assignFailsWithoutPartialSaveWhenSomeDeliveryCannotBeAssigned() {
        Long deliveryGroupId = 1L;
        Long riderId = 10L;
        LocalDate deliveryDate = LocalDate.of(2026, 9, 5);

        DeliverySlot slot =
            mock(DeliverySlot.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        Rider rider =
            mock(Rider.class);

        Delivery firstDelivery =
            mock(Delivery.class);

        Delivery secondDelivery =
            mock(Delivery.class);

        when(slot.getCode())
            .thenReturn(DeliverySlotCode.LUNCH);

        when(deliveryGroup.isWaitingAutoAssignment())
            .thenReturn(true);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(deliveryDate);

        when(deliveryGroup.getSlot())
            .thenReturn(slot);

        when(rider.getId())
            .thenReturn(riderId);

        when(firstDelivery.getStatus())
            .thenReturn(DeliveryStatus.READY);

        when(firstDelivery.getDeliveryAreaCode())
            .thenReturn("DAEGU_JUNG_GU");

        when(firstDelivery.getLunchboxQuantity())
            .thenReturn(40);

        when(secondDelivery.getStatus())
            .thenReturn(DeliveryStatus.READY);

        when(secondDelivery.getDeliveryAreaCode())
            .thenReturn("DAEGU_JUNG_GU");

        when(secondDelivery.getLunchboxQuantity())
            .thenReturn(3);

        when(deliveryGroupRepository.findByIdForUpdate(deliveryGroupId))
            .thenReturn(Optional.of(deliveryGroup));

        when(riderRepository.findAllByDeletedAtIsNullOrderByIdAsc())
            .thenReturn(List.of(rider));

        when(riderRepository.findAllByIdInForUpdate(List.of(riderId)))
            .thenReturn(List.of(rider));

        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(
                List.of(
                    firstDelivery
                    , secondDelivery
                )
            );

        when(deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of());

        when(deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of());

        when(
            riderAssignmentEligibilityService.isEligible(
                rider
                , deliveryDate
                , DeliverySlotCode.LUNCH
                , "DAEGU_JUNG_GU"
            )
        )
            .thenReturn(true);

        boolean result =
            autoAssignmentService.assign(
                deliveryGroupId
            );

        assertThat(result).isFalse();

        verify(deliveryAssignmentRepository, never())
            .save(any(DeliveryAssignment.class));

        verify(deliveryAssignmentItemRepository, never())
            .saveAll(any());

        verify(deliveryGroup, never())
            .completeAutoAssignment(any(LocalDateTime.class));

        verify(deliveryGroupStatusHistoryRepository, never())
            .save(any(DeliveryGroupStatusHistory.class));
    }

    @Test
    void assignFailsWhenActiveAssignmentAlreadyExists() {
        Long deliveryGroupId = 1L;
        Long riderId = 10L;

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        Rider rider =
            mock(Rider.class);

        Delivery delivery =
            mock(Delivery.class);

        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        DeliveryAssignmentItem assignmentItem =
            mock(DeliveryAssignmentItem.class);

        when(deliveryGroup.isWaitingAutoAssignment())
            .thenReturn(true);

        when(rider.getId())
            .thenReturn(riderId);

        when(delivery.getStatus())
            .thenReturn(DeliveryStatus.READY);

        when(assignment.getId())
            .thenReturn(100L);

        when(assignment.getStatus())
            .thenReturn(DeliveryAssignmentStatus.ASSIGNED);

        when(assignmentItem.getAssignment())
            .thenReturn(assignment);

        when(deliveryGroupRepository.findByIdForUpdate(deliveryGroupId))
            .thenReturn(Optional.of(deliveryGroup));

        when(riderRepository.findAllByDeletedAtIsNullOrderByIdAsc())
            .thenReturn(List.of(rider));

        when(riderRepository.findAllByIdInForUpdate(List.of(riderId)))
            .thenReturn(List.of(rider));

        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of(delivery));

        when(deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of(assignment));

        when(deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of(assignmentItem));

        boolean result =
            autoAssignmentService.assign(
                deliveryGroupId
            );

        assertThat(result).isFalse();

        verify(deliveryAssignmentRepository, never())
            .save(any(DeliveryAssignment.class));

        verify(deliveryAssignmentItemRepository, never())
            .saveAll(any());

        verify(deliveryGroup, never())
            .completeAutoAssignment(any(LocalDateTime.class));

        verify(deliveryGroupStatusHistoryRepository, never())
            .save(any(DeliveryGroupStatusHistory.class));
    }

    @Test
    void assignDistributesDeliveriesByCurrentVisitCount() {
        Long deliveryGroupId = 1L;
        LocalDate deliveryDate = LocalDate.of(2026, 9, 5);

        DeliverySlot slot =
            mock(DeliverySlot.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        Rider firstRider =
            mock(Rider.class);

        Rider secondRider =
            mock(Rider.class);

        Delivery firstDelivery =
            mock(Delivery.class);

        Delivery secondDelivery =
            mock(Delivery.class);

        when(slot.getCode())
            .thenReturn(DeliverySlotCode.LUNCH);

        when(deliveryGroup.isWaitingAutoAssignment())
            .thenReturn(true);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(deliveryDate);

        when(deliveryGroup.getSlot())
            .thenReturn(slot);

        when(firstRider.getId())
            .thenReturn(10L);

        when(secondRider.getId())
            .thenReturn(20L);

        when(firstDelivery.getStatus())
            .thenReturn(DeliveryStatus.READY);

        when(firstDelivery.getDeliveryAreaCode())
            .thenReturn("DAEGU_JUNG_GU");

        when(firstDelivery.getLunchboxQuantity())
            .thenReturn(2);

        when(secondDelivery.getStatus())
            .thenReturn(DeliveryStatus.READY);

        when(secondDelivery.getDeliveryAreaCode())
            .thenReturn("DAEGU_JUNG_GU");

        when(secondDelivery.getLunchboxQuantity())
            .thenReturn(2);

        when(deliveryGroupRepository.findByIdForUpdate(deliveryGroupId))
            .thenReturn(Optional.of(deliveryGroup));

        when(riderRepository.findAllByDeletedAtIsNullOrderByIdAsc())
            .thenReturn(
                List.of(
                    firstRider
                    , secondRider
                )
            );

        when(
            riderRepository.findAllByIdInForUpdate(
                List.of(
                    10L
                    , 20L
                )
            )
        )
            .thenReturn(
                List.of(
                    firstRider
                    , secondRider
                )
            );

        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(
                List.of(
                    firstDelivery
                    , secondDelivery
                )
            );

        when(deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of());

        when(deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of());

        when(
            riderAssignmentEligibilityService.isEligible(
                any(Rider.class)
                , any(LocalDate.class)
                , any(DeliverySlotCode.class)
                , any(String.class)
            )
        )
            .thenReturn(true);

        when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        boolean result =
            autoAssignmentService.assign(
                deliveryGroupId
            );

        assertThat(result).isTrue();

        ArgumentCaptor<DeliveryAssignment> assignmentCaptor =
            ArgumentCaptor.forClass(
                DeliveryAssignment.class
            );

        verify(deliveryAssignmentRepository, times(2))
            .save(assignmentCaptor.capture());

        List<DeliveryAssignment> savedAssignments =
            assignmentCaptor.getAllValues();

        assertThat(savedAssignments)
            .hasSize(2);

        assertThat(savedAssignments.get(0).getRider())
            .isEqualTo(firstRider);

        assertThat(savedAssignments.get(1).getRider())
            .isEqualTo(secondRider);
    }
    @Test
    void assignPrefersRiderWhoStaysWithinRecommendedCapacity() {
        Long deliveryGroupId = 1L;
        LocalDate deliveryDate = LocalDate.of(2026, 9, 5);

        DeliverySlot slot = mock(DeliverySlot.class);
        DeliveryGroup deliveryGroup = mock(DeliveryGroup.class);
        Rider firstRider = mock(Rider.class);
        Rider secondRider = mock(Rider.class);
        Delivery firstDelivery = mock(Delivery.class);
        Delivery secondDelivery = mock(Delivery.class);
        Delivery thirdDelivery = mock(Delivery.class);

        when(slot.getCode()).thenReturn(DeliverySlotCode.LUNCH);
        when(deliveryGroup.isWaitingAutoAssignment()).thenReturn(true);
        when(deliveryGroup.getDeliveryDate()).thenReturn(deliveryDate);
        when(deliveryGroup.getSlot()).thenReturn(slot);
        when(firstRider.getId()).thenReturn(10L);
        when(secondRider.getId()).thenReturn(20L);

        for (Delivery delivery : List.of(firstDelivery, secondDelivery, thirdDelivery)) {
            when(delivery.getStatus()).thenReturn(DeliveryStatus.READY);
            when(delivery.getDeliveryAreaCode()).thenReturn("DAEGU_JUNG_GU");
        }
        when(firstDelivery.getLunchboxQuantity()).thenReturn(30);
        when(secondDelivery.getLunchboxQuantity()).thenReturn(1);
        when(thirdDelivery.getLunchboxQuantity()).thenReturn(10);

        when(deliveryGroupRepository.findByIdForUpdate(deliveryGroupId))
            .thenReturn(Optional.of(deliveryGroup));
        when(riderRepository.findAllByDeletedAtIsNullOrderByIdAsc())
            .thenReturn(List.of(firstRider, secondRider));
        when(riderRepository.findAllByIdInForUpdate(List.of(10L, 20L)))
            .thenReturn(List.of(firstRider, secondRider));
        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of(firstDelivery, secondDelivery, thirdDelivery));
        when(deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of());
        when(deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of());
        when(
            riderAssignmentEligibilityService.isEligible(
                any(Rider.class)
                , any(LocalDate.class)
                , any(DeliverySlotCode.class)
                , any(String.class)
            )
        ).thenReturn(true);
        when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        List<DeliveryAssignmentItem> savedItems = new ArrayList<>();
        when(deliveryAssignmentItemRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<DeliveryAssignmentItem> items = invocation.getArgument(0);
            items.forEach(savedItems::add);
            return savedItems;
        });

        assertThat(autoAssignmentService.assign(deliveryGroupId)).isTrue();

        DeliveryAssignmentItem thirdItem = savedItems.stream()
            .filter(item -> item.getDelivery() == thirdDelivery)
            .findFirst()
            .orElseThrow();

        assertThat(thirdItem.getAssignment().getRider()).isEqualTo(secondRider);
    }

    @Test
    void assignUsesMaximumCapacityOnlyWhenRecommendedCapacityCannotBeMaintained() {
        Long deliveryGroupId = 1L;
        LocalDate deliveryDate = LocalDate.of(2026, 9, 5);

        DeliverySlot slot = mock(DeliverySlot.class);
        DeliveryGroup deliveryGroup = mock(DeliveryGroup.class);
        Rider rider = mock(Rider.class);
        List<Delivery> deliveries = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            Delivery delivery = mock(Delivery.class);
            when(delivery.getStatus()).thenReturn(DeliveryStatus.READY);
            when(delivery.getDeliveryAreaCode()).thenReturn("DAEGU_JUNG_GU");
            when(delivery.getLunchboxQuantity()).thenReturn(1);
            deliveries.add(delivery);
        }

        when(slot.getCode()).thenReturn(DeliverySlotCode.LUNCH);
        when(deliveryGroup.isWaitingAutoAssignment()).thenReturn(true);
        when(deliveryGroup.getDeliveryDate()).thenReturn(deliveryDate);
        when(deliveryGroup.getSlot()).thenReturn(slot);
        when(rider.getId()).thenReturn(10L);
        when(deliveryGroupRepository.findByIdForUpdate(deliveryGroupId))
            .thenReturn(Optional.of(deliveryGroup));
        when(riderRepository.findAllByDeletedAtIsNullOrderByIdAsc())
            .thenReturn(List.of(rider));
        when(riderRepository.findAllByIdInForUpdate(List.of(10L)))
            .thenReturn(List.of(rider));
        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(deliveries);
        when(deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of());
        when(deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId))
            .thenReturn(List.of());
        when(
            riderAssignmentEligibilityService.isEligible(
                rider
                , deliveryDate
                , DeliverySlotCode.LUNCH
                , "DAEGU_JUNG_GU"
            )
        ).thenReturn(true);
        when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        List<DeliveryAssignmentItem> savedItems = new ArrayList<>();
        when(deliveryAssignmentItemRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<DeliveryAssignmentItem> items = invocation.getArgument(0);
            items.forEach(savedItems::add);
            return savedItems;
        });

        assertThat(autoAssignmentService.assign(deliveryGroupId)).isTrue();
        assertThat(savedItems).hasSize(9);
    }

}