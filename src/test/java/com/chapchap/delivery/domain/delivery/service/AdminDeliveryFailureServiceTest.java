package com.chapchap.delivery.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureStage;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.request.AdminDeliveryFailureRequest;
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
class AdminDeliveryFailureServiceTest {
    private static final Long ADMIN_ID = 7L;
    private static final Long DELIVERY_ID = 1L;
    private static final Long GROUP_ID = 10L;
    private static final String PUBLIC_ID = "delivery-public-id";

    @Mock private DeliveryAccessService accessService;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryGroupRepository groupRepository;
    @Mock private DeliveryFailureRepository failureRepository;
    @Mock private DeliveryStatusHistoryRepository historyRepository;
    @Mock private DeliveryExecutionSupport executionSupport;
    @Mock private DeliveryEventRequestPublisher eventPublisher;
    @Mock private EntityManager entityManager;

    private AdminDeliveryFailureService service;

    @BeforeEach
    void setUp() {
        service = new AdminDeliveryFailureService(
            accessService
            , deliveryRepository
            , groupRepository
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
    @DisplayName("관리자는 READY 대상을 출발 전 실패 처리할 수 있다")
    void adminCanFailReadyDeliveryBeforeDeparture() {
        DeliveryGroup group = mock(DeliveryGroup.class);
        Delivery delivery = mock(Delivery.class);
        when(group.getId()).thenReturn(GROUP_ID);
        when(delivery.getId()).thenReturn(DELIVERY_ID);
        when(delivery.getDeliveryPublicId()).thenReturn(PUBLIC_ID);
        when(delivery.getDeliveryGroup()).thenReturn(group);
        when(delivery.getStatus()).thenReturn(
            DeliveryStatus.READY, DeliveryStatus.READY, DeliveryStatus.FAILED
        );
        when(delivery.getDeliveryVersion()).thenReturn(2);
        when(deliveryRepository.findByDeliveryPublicId(PUBLIC_ID)).thenReturn(Optional.of(delivery));
        when(groupRepository.findByIdForUpdate(GROUP_ID)).thenReturn(Optional.of(group));
        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID)).thenReturn(List.of(delivery));
        when(deliveryRepository.transitionStatus(
            DELIVERY_ID, DeliveryStatus.READY, DeliveryStatus.FAILED
        )).thenReturn(1);
        when(failureRepository.save(any(DeliveryFailure.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.fail(
            ADMIN_ID, UserRole.ADMIN, PUBLIC_ID, request("OPERATION_REVIEW", null)
        );

        assertThat(response.status()).isEqualTo(DeliveryStatus.FAILED);
        verify(accessService).validateAdminAccess(ADMIN_ID, UserRole.ADMIN);
        verify(historyRepository).save(any());
        verify(executionSupport).recalculateGroup(eq(group), any(), any());
        verify(eventPublisher).publishStateChanged(eq("DELIVERY_FAILED"), eq(delivery), any());
    }

    @Test
    @DisplayName("관리자 사유 OTHER에는 상세 설명이 필요하다")
    void otherAdminReasonRequiresDetail() {
        assertThatThrownBy(
            () -> service.fail(
                ADMIN_ID, UserRole.ADMIN, PUBLIC_ID, request("OTHER", null)
            )
        ).isInstanceOf(InvalidDeliveryFailureReasonException.class);

        verify(deliveryRepository, never()).findByDeliveryPublicId(any());
    }

    private AdminDeliveryFailureRequest request(String adminReasonCode, String adminReasonDetail) {
        return new AdminDeliveryFailureRequest(
            DeliveryFailureStage.BEFORE_DEPARTURE
            , DeliveryFailureCode.ACCESS_DENIED
            , null
            , null
            , null
            , false
            , null
            , adminReasonCode
            , adminReasonDetail
        );
    }
}
