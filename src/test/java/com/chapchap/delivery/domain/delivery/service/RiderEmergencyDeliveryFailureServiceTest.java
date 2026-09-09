package com.chapchap.delivery.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.request.RiderEmergencyDeliveryFailureRequest;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.DeliveryStateConflictException;
import com.chapchap.delivery.global.kafka.producer.DeliveryEventRequestPublisher;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiderEmergencyDeliveryFailureServiceTest {
    private static final Long AUTH_USER_ID = 100L;
    private static final Long GROUP_ID = 10L;
    private static final Long ASSIGNMENT_ID = 20L;
    private static final Long RIDER_ID = 30L;

    @Mock private DeliveryAccessService accessService;
    @Mock private DeliveryGroupRepository groupRepository;
    @Mock private RiderRepository riderRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private DeliveryAssignmentItemRepository itemRepository;
    @Mock private DeliveryFailureRepository failureRepository;
    @Mock private DeliveryStatusHistoryRepository historyRepository;
    @Mock private DeliveryExecutionSupport executionSupport;
    @Mock private DeliveryEventRequestPublisher eventPublisher;
    @Mock private EntityManager entityManager;

    private RiderEmergencyDeliveryFailureService service;

    @BeforeEach
    void setUp() {
        service = new RiderEmergencyDeliveryFailureService(
            accessService
            , groupRepository
            , riderRepository
            , deliveryRepository
            , assignmentRepository
            , itemRepository
            , failureRepository
            , historyRepository
            , executionSupport
            , new DeliveryRefundReasonResolver()
            , eventPublisher
            , entityManager
        );
    }

    @Test
    @DisplayName("배송 시작 전에는 기사 긴급 일괄 실패를 허용하지 않는다")
    void rejectsEmergencyFailureBeforeDeliveryStarts() {
        DeliveryGroup group = mock(DeliveryGroup.class);
        DeliveryAssignment assignment = mock(DeliveryAssignment.class);
        when(group.getId()).thenReturn(GROUP_ID);
        when(group.getStatus()).thenReturn(DeliveryGroupStatus.CONFIRMED);
        when(assignment.getDeliveryGroup()).thenReturn(group);
        when(assignmentRepository.findMineById(ASSIGNMENT_ID, AUTH_USER_ID))
            .thenReturn(Optional.of(assignment));
        when(groupRepository.findByIdForUpdate(GROUP_ID)).thenReturn(Optional.of(group));

        assertThatThrownBy(
            () -> service.failRemaining(AUTH_USER_ID, ASSIGNMENT_ID, request())
        ).isInstanceOf(DeliveryStateConflictException.class);

        verify(deliveryRepository, never()).transitionStatus(any(), any(), any());
    }

    @Test
    @DisplayName("배송 중 기사 긴급 상황은 해당 기사에게 남은 미종결 대상만 실패 처리한다")
    void failsOnlyRemainingDeliveriesOfCurrentAssignment() {
        DeliveryGroup group = mock(DeliveryGroup.class);
        Rider rider = mock(Rider.class);
        DeliveryAssignment assignment = mock(DeliveryAssignment.class);
        DeliveryAssignment otherAssignment = mock(DeliveryAssignment.class);
        Delivery delivering = delivery(
            1L
            , "delivery-1"
            , DeliveryStatus.DELIVERING
            , false
        );

        Delivery ready = delivery(
            2L
            , "delivery-2"
            , DeliveryStatus.READY
            , false
        );

        Delivery finished = delivery(
            3L
            , "delivery-3"
            , DeliveryStatus.DELIVERED
            , true
        );

        // 다른 기사 Assignment의 배송이므로
        // 현재 서비스가 배송 자체를 조회해서는 안 된다.
        Delivery other = mock(Delivery.class);

        when(group.getId()).thenReturn(GROUP_ID);
        when(group.getStatus()).thenReturn(DeliveryGroupStatus.IN_PROGRESS);
        when(assignment.getId()).thenReturn(ASSIGNMENT_ID);
        when(assignment.getDeliveryGroup()).thenReturn(group);
        when(assignment.getRider()).thenReturn(rider);
        when(assignment.getStatus()).thenReturn(DeliveryAssignmentStatus.CONFIRMED);
        when(otherAssignment.getId()).thenReturn(99L);
        when(rider.getId()).thenReturn(RIDER_ID);
        when(rider.getIsDeliveryActive()).thenReturn(true);

        DeliveryAssignmentItem deliveringItem = item(assignment, delivering);
        DeliveryAssignmentItem readyItem = item(assignment, ready);
        DeliveryAssignmentItem finishedItem = item(assignment, finished);
        DeliveryAssignmentItem otherItem = item(otherAssignment, other);

        when(assignmentRepository.findMineById(ASSIGNMENT_ID, AUTH_USER_ID))
            .thenReturn(Optional.of(assignment));
        when(groupRepository.findByIdForUpdate(GROUP_ID)).thenReturn(Optional.of(group));
        when(riderRepository.findByAuthUserIdForUpdate(AUTH_USER_ID)).thenReturn(Optional.of(rider));
        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID))
            .thenReturn(List.of(delivering, ready, finished, other));
        when(assignmentRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID))
            .thenReturn(List.of(assignment, otherAssignment));
        when(itemRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID))
            .thenReturn(List.of(deliveringItem, readyItem, finishedItem, otherItem));
        when(deliveryRepository.transitionStatus(1L, DeliveryStatus.DELIVERING, DeliveryStatus.FAILED))
            .thenReturn(1);
        when(deliveryRepository.transitionStatus(2L, DeliveryStatus.READY, DeliveryStatus.FAILED))
            .thenReturn(1);

        var response = service.failRemaining(AUTH_USER_ID, ASSIGNMENT_ID, request());

        assertThat(response.failedCount()).isEqualTo(2);
        assertThat(response.deliveryIds()).containsExactly("delivery-1", "delivery-2");
        verify(deliveryRepository, never()).transitionStatus(
            eq(3L), any(DeliveryStatus.class), eq(DeliveryStatus.FAILED)
        );
        verify(deliveryRepository, never()).transitionStatus(
            eq(4L), any(DeliveryStatus.class), eq(DeliveryStatus.FAILED)
        );
        verify(failureRepository, org.mockito.Mockito.times(2)).save(any());
        verify(historyRepository, org.mockito.Mockito.times(2)).save(any());
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishStateChanged(
            eq("DELIVERY_FAILED"), any(Delivery.class), any()
        );
        verify(executionSupport).recalculateGroup(eq(group), any(), any());
    }

    private RiderEmergencyDeliveryFailureRequest request() {
        return new RiderEmergencyDeliveryFailureRequest(
            DeliveryFailureCode.RIDER_ACCIDENT
            , null
            , false
            , null
        );
    }

    private Delivery delivery(Long id, String publicId, DeliveryStatus status, boolean finished) {
        Delivery delivery = mock(Delivery.class);
        lenient().when(delivery.getId()).thenReturn(id);
        lenient().when(delivery.getDeliveryPublicId()).thenReturn(publicId);
        when(delivery.isFinished()).thenReturn(finished);
        if (!finished) {
            lenient().when(delivery.getStatus()).thenReturn(status);
        }
        return delivery;
    }

    private DeliveryAssignmentItem item(DeliveryAssignment assignment, Delivery delivery) {
        DeliveryAssignmentItem item = mock(DeliveryAssignmentItem.class);
        when(item.getAssignment()).thenReturn(assignment);
        lenient().when(item.getDelivery()).thenReturn(delivery);
        return item;
    }
}
