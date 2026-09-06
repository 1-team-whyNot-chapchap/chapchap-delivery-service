package com.chapchap.delivery.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.response.RiderDeliveryStartResponse;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.DeliveryGroupStateConflictException;
import com.chapchap.delivery.global.kafka.producer.DeliveryEventRequestPublisher;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class RiderDeliveryStartServiceTest {
    private static final Long AUTH_USER_ID = 100L;
    private static final Long DELIVERY_ID = 1L;
    private static final Long DELIVERY_GROUP_ID = 10L;
    private static final Long RIDER_ID = 20L;
    private static final String DELIVERY_PUBLIC_ID =
        "0198c004-1000-7000-8000-000000000901";

    @Mock private DeliveryAccessService deliveryAccessService;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryGroupRepository deliveryGroupRepository;
    @Mock private RiderRepository riderRepository;
    @Mock private DeliveryAssignmentRepository deliveryAssignmentRepository;
    @Mock private DeliveryAssignmentItemRepository deliveryAssignmentItemRepository;
    @Mock private DeliveryStatusHistoryRepository deliveryStatusHistoryRepository;
    @Mock private DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;
    @Mock private DeliveryEventRequestPublisher deliveryEventRequestPublisher;
    @Mock private EntityManager entityManager;

    private RiderDeliveryStartService riderDeliveryStartService;

    @BeforeEach
    void setUp() {
        riderDeliveryStartService = new RiderDeliveryStartService(
            deliveryAccessService
            , deliveryRepository
            , deliveryGroupRepository
            , riderRepository
            , deliveryAssignmentRepository
            , deliveryAssignmentItemRepository
            , deliveryStatusHistoryRepository
            , new DeliveryExecutionSupport(
                deliveryGroupStatusHistoryRepository
            )
            , deliveryEventRequestPublisher
            , entityManager
        );
    }

    @Test
    @DisplayName("현재 담당 기사는 확정된 READY 배송을 시작한다")
    void startDelivery() {
        Fixture fixture = fixture(DeliveryGroupStatus.CONFIRMED);

        RiderDeliveryStartResponse response = riderDeliveryStartService.start(
            AUTH_USER_ID
            , DELIVERY_PUBLIC_ID
        );

        verify(deliveryRepository).transitionStatus(
            DELIVERY_ID, DeliveryStatus.READY, DeliveryStatus.DELIVERING
        );
        verify(deliveryStatusHistoryRepository).save(org.mockito.ArgumentMatchers.any());
        verify(fixture.deliveryGroup).startExecution(org.mockito.ArgumentMatchers.any());
        verify(deliveryGroupStatusHistoryRepository).save(org.mockito.ArgumentMatchers.any());

        assertThat(response.deliveryId()).isEqualTo(DELIVERY_PUBLIC_ID);
        assertThat(response.status()).isEqualTo(DeliveryStatus.DELIVERING);
        assertThat(response.deliveryVersion()).isEqualTo(2);
        assertThat(response.startedAt().getOffset().getTotalSeconds()).isEqualTo(32400);
    }

    @Test
    @DisplayName("최종 확정되지 않은 전체 배송은 시작할 수 없다")
    void startFailsWhenDeliveryGroupIsNotConfirmed() {
        fixture(DeliveryGroupStatus.READY_TO_CONFIRM);

        assertThatThrownBy(
            () -> riderDeliveryStartService.start(AUTH_USER_ID, DELIVERY_PUBLIC_ID)
        ).isInstanceOf(DeliveryGroupStateConflictException.class);
    }

    private Fixture fixture(DeliveryGroupStatus deliveryGroupStatus) {
        DeliveryGroup deliveryGroup = mock(DeliveryGroup.class);
        Delivery delivery = mock(Delivery.class);
        Rider rider = mock(Rider.class);
        DeliveryAssignment assignment = mock(DeliveryAssignment.class);
        DeliveryAssignmentItem assignmentItem = mock(DeliveryAssignmentItem.class);

        when(deliveryGroup.getId()).thenReturn(DELIVERY_GROUP_ID);
        when(deliveryGroup.getStatus()).thenReturn(deliveryGroupStatus);
        lenient().when(deliveryGroup.startExecution(org.mockito.ArgumentMatchers.any()))
            .thenReturn(true);
        when(delivery.getDeliveryGroup()).thenReturn(deliveryGroup);
        when(delivery.getId()).thenReturn(DELIVERY_ID);
        lenient().when(delivery.getDeliveryPublicId()).thenReturn(DELIVERY_PUBLIC_ID);
        when(delivery.getStatus()).thenReturn(
            DeliveryStatus.READY
            , DeliveryStatus.READY
            , DeliveryStatus.DELIVERING
        );
        lenient().when(delivery.getDeliveryVersion()).thenReturn(2);
        when(rider.getId()).thenReturn(RIDER_ID);
        lenient().when(rider.getIsDeliveryActive()).thenReturn(true);
        when(assignment.getRider()).thenReturn(rider);
        when(assignment.getStatus()).thenReturn(DeliveryAssignmentStatus.CONFIRMED);
        when(assignmentItem.getDelivery()).thenReturn(delivery);
        when(assignmentItem.getAssignment()).thenReturn(assignment);
        when(deliveryRepository.findByDeliveryPublicId(DELIVERY_PUBLIC_ID))
            .thenReturn(Optional.of(delivery));
        when(deliveryGroupRepository.findByIdForUpdate(DELIVERY_GROUP_ID))
            .thenReturn(Optional.of(deliveryGroup));
        when(riderRepository.findByAuthUserIdForUpdate(AUTH_USER_ID))
            .thenReturn(Optional.of(rider));
        lenient().when(deliveryRepository.transitionStatus(
            DELIVERY_ID, DeliveryStatus.READY, DeliveryStatus.DELIVERING
        )).thenReturn(1);
        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(DELIVERY_GROUP_ID))
            .thenReturn(List.of(delivery));
        when(deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(DELIVERY_GROUP_ID))
            .thenReturn(List.of(assignment));
        when(deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(DELIVERY_GROUP_ID))
            .thenReturn(List.of(assignmentItem));

        return new Fixture(deliveryGroup, delivery);
    }

    private record Fixture(
        DeliveryGroup deliveryGroup
        , Delivery delivery
    ) {
    }
}
