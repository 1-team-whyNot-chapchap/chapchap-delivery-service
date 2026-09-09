package com.chapchap.delivery.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureStage;
import com.chapchap.delivery.domain.delivery.constant.DeliveryRefundReason;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.request.RiderDeliveryFailureRequest;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.InvalidDeliveryFailureReasonException;
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
class RiderDeliveryFailureServiceTest {
    private static final Long AUTH_USER_ID = 100L;
    private static final Long DELIVERY_ID = 1L;
    private static final Long GROUP_ID = 10L;
    private static final String PUBLIC_ID = "delivery-public-id";

    @Mock private DeliveryAccessService accessService;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryGroupRepository groupRepository;
    @Mock private RiderRepository riderRepository;
    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private DeliveryAssignmentItemRepository assignmentItemRepository;
    @Mock private DeliveryFailureRepository failureRepository;
    @Mock private DeliveryStatusHistoryRepository historyRepository;
    @Mock private DeliveryExecutionSupport executionSupport;
    @Mock private DeliveryEventRequestPublisher eventPublisher;
    @Mock private EntityManager entityManager;

    private RiderDeliveryFailureService service;

    @BeforeEach
    void setUp() {
        service = new RiderDeliveryFailureService(
            accessService
            , deliveryRepository
            , groupRepository
            , riderRepository
            , assignmentRepository
            , assignmentItemRepository
            , failureRepository
            , historyRepository
            , executionSupport
            , eventPublisher
            , entityManager
            , new DeliveryRefundReasonResolver()
            , new DeliveryFailureValidator()
        );
    }

    @Test
    @DisplayName("기사는 배송 중인 본인 대상만 DURING_DELIVERY 실패 처리한다")
    void riderFailsDeliveringTarget() {
        DeliveryGroup group = mock(DeliveryGroup.class);
        Delivery delivery = mock(Delivery.class);
        Rider rider = mock(Rider.class);
        when(group.getId()).thenReturn(GROUP_ID);
        when(delivery.getId()).thenReturn(DELIVERY_ID);
        when(delivery.getDeliveryPublicId()).thenReturn(PUBLIC_ID);
        when(delivery.getDeliveryGroup()).thenReturn(group);
        when(delivery.getStatus()).thenReturn(
            DeliveryStatus.DELIVERING
            , DeliveryStatus.DELIVERING
            , DeliveryStatus.FAILED
        );
        when(delivery.getDeliveryVersion()).thenReturn(3);
        when(rider.getIsDeliveryActive()).thenReturn(true);
        when(deliveryRepository.findByDeliveryPublicId(PUBLIC_ID)).thenReturn(Optional.of(delivery));
        when(groupRepository.findByIdForUpdate(GROUP_ID)).thenReturn(Optional.of(group));
        when(riderRepository.findByAuthUserIdForUpdate(AUTH_USER_ID)).thenReturn(Optional.of(rider));
        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID)).thenReturn(List.of(delivery));
        when(assignmentRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID)).thenReturn(List.of());
        when(assignmentItemRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID)).thenReturn(List.of());
        when(deliveryRepository.transitionStatus(
            DELIVERY_ID, DeliveryStatus.DELIVERING, DeliveryStatus.FAILED
        )).thenReturn(1);
        when(failureRepository.save(any(DeliveryFailure.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.fail(AUTH_USER_ID, PUBLIC_ID, request(DeliveryFailureStage.DURING_DELIVERY));

        assertThat(response.status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(response.failureCode()).isEqualTo(DeliveryFailureCode.ACCESS_DENIED);
        verify(historyRepository).save(any());
        verify(executionSupport).recalculateGroup(eq(group), any(), any());
        verify(eventPublisher).publishStateChanged(eq("DELIVERY_FAILED"), eq(delivery), any());
        verify(eventPublisher).publishRefundConfirmed(
            eq(delivery), eq(DeliveryRefundReason.DELIVERY_FAILED), any()
        );
    }

    @Test
    @DisplayName("기사는 출발 전 BEFORE_DEPARTURE 실패를 처리할 수 없다")
    void riderCannotFailBeforeDeparture() {
        assertThatThrownBy(
            () -> service.fail(AUTH_USER_ID, PUBLIC_ID, request(DeliveryFailureStage.BEFORE_DEPARTURE))
        ).isInstanceOf(InvalidDeliveryFailureReasonException.class);

        verify(deliveryRepository, never()).findByDeliveryPublicId(any());
    }

    private RiderDeliveryFailureRequest request(DeliveryFailureStage stage) {
        return new RiderDeliveryFailureRequest(
            stage
            , DeliveryFailureCode.ACCESS_DENIED
            , null
            , null
            , null
            , false
            , null
        );
    }
}
