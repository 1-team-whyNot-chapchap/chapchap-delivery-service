package com.chapchap.delivery.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.constant.AdminRecoveryReason;
import com.chapchap.delivery.domain.delivery.constant.AdminDeliveryFailureReason;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureStage;
import com.chapchap.delivery.domain.delivery.constant.DeliveryRecoveryResult;
import com.chapchap.delivery.domain.delivery.constant.DeliveryRefundReason;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.RequestHandoffType;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletion;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletionPhoto;
import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.repository.DeliveryAdminRecoveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionPhotoRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRecipientSnapshotRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.request.AdminDeliveryRecoveryRequest;
import com.chapchap.delivery.domain.delivery.request.RiderDeliveryCompletionRequest;
import com.chapchap.delivery.domain.delivery.request.RiderDeliveryFailureRequest;
import com.chapchap.delivery.global.exception.business.InvalidDeliveryInfoException;
import com.chapchap.delivery.global.kafka.producer.DeliveryEventRequestPublisher;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class AdminDeliveryRecoveryServiceTest {
    private static final Long ADMIN_ID = 100L;
    private static final Long GROUP_ID = 1L;
    private static final Long DELIVERY_ID = 2L;
    private static final Long RIDER_ID = 3L;
    private static final String DELIVERY_PUBLIC_ID = "delivery-1";

    @Mock private DeliveryAccessService accessService;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryGroupRepository groupRepository;
    @Mock private com.chapchap.delivery.domain.rider.repository.RiderRepository riderRepository;
    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private DeliveryAssignmentItemRepository assignmentItemRepository;
    @Mock private DeliveryAdminRecoveryRepository recoveryRepository;
    @Mock private DeliveryCompletionRepository completionRepository;
    @Mock private DeliveryCompletionPhotoRepository photoRepository;
    @Mock private DeliveryFailureRepository failureRepository;
    @Mock private DeliveryStatusHistoryRepository historyRepository;
    @Mock private DeliveryRecipientSnapshotRepository recipientRepository;
    @Mock private DeliveryExecutionSupport executionSupport;
    @Mock private DeliveryDelayService delayService;
    @Mock private DeliveryEventRequestPublisher eventPublisher;
    @Mock private EntityManager entityManager;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private DeliveryPhotoFileService photoFileService;
    @Mock private com.chapchap.delivery.domain.riderlocation.service.RiderLocationTrackingLifecycle trackingLifecycle;

    private AdminDeliveryRecoveryService recoveryService;
    private Delivery delivery;

    @BeforeEach
    void setUp() {
        recoveryService = new AdminDeliveryRecoveryService(
            accessService
            , deliveryRepository
            , groupRepository
            , riderRepository
            , assignmentRepository
            , assignmentItemRepository
            , recoveryRepository
            , completionRepository
            , photoRepository
            , failureRepository
            , historyRepository
            , recipientRepository
            , executionSupport
            , new DeliveryFailureValidator()
            , new DeliveryRefundReasonResolver()
            , delayService
            , eventPublisher
            , entityManager
            , transactionTemplate
            , photoFileService
            , trackingLifecycle
        );
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        lenient().doAnswer(invocation -> {
            java.util.function.Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    @DisplayName("관리자는 확인된 단말 장애의 사후 완료 결과를 기록한다")
    void recoversDeliveredResult() {
        delivery = configureLockedExecution();
        RiderDeliveryCompletionRequest completionRequest =
            new RiderDeliveryCompletionRequest(
                ActualHandoffType.DIRECT
                , null
                , null
                , null
            );
        AdminDeliveryRecoveryRequest request = new AdminDeliveryRecoveryRequest(
            DeliveryRecoveryResult.DELIVERED
            , AdminRecoveryReason.DEVICE_FAILURE
            , null
            , RIDER_ID
            , completionRequest
            , null
        );
        when(delivery.getStatus()).thenReturn(
            DeliveryStatus.DELIVERING
            , DeliveryStatus.DELIVERED
        );
        when(deliveryRepository.transitionStatus(
            DELIVERY_ID
            , DeliveryStatus.DELIVERING
            , DeliveryStatus.DELIVERED
        )).thenReturn(1);
        when(completionRepository.save(any())).thenReturn(mock(DeliveryCompletion.class));

        var response = recoveryService.recover(
            ADMIN_ID
            , UserRole.ADMIN
            , DELIVERY_PUBLIC_ID
            , request
            , null
        );

        assertThat(response.status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(response.recoveryResult()).isEqualTo(DeliveryRecoveryResult.DELIVERED);
        verify(recoveryRepository).save(any());
        verify(historyRepository).save(any());
        verify(eventPublisher).publishStateChanged(
            org.mockito.ArgumentMatchers.eq("DELIVERY_COMPLETED")
            , org.mockito.ArgumentMatchers.eq(delivery)
            , any()
        );
    }

    @Test
    @DisplayName("관리자 장애 복구의 실패 결과는 환불 근거를 발행한다")
    void recoversFailedResultAndPublishesRefund() {
        delivery = configureLockedExecution();
        RiderDeliveryFailureRequest failureRequest =
            new RiderDeliveryFailureRequest(
                DeliveryFailureStage.DURING_DELIVERY
                , DeliveryFailureCode.WEATHER_CONDITION
                , null
                , null
                , null
                , false
                , null
            );
        AdminDeliveryRecoveryRequest request = new AdminDeliveryRecoveryRequest(
            DeliveryRecoveryResult.FAILED
            , AdminRecoveryReason.NETWORK_FAILURE
            , null
            , RIDER_ID
            , null
            , failureRequest
        );
        when(delivery.getStatus()).thenReturn(
            DeliveryStatus.DELIVERING
            , DeliveryStatus.FAILED
        );
        when(deliveryRepository.transitionStatus(
            DELIVERY_ID
            , DeliveryStatus.DELIVERING
            , DeliveryStatus.FAILED
        )).thenReturn(1);

        var response = recoveryService.recover(
            ADMIN_ID
            , UserRole.ADMIN
            , DELIVERY_PUBLIC_ID
            , request
            , null
        );

        assertThat(response.status()).isEqualTo(DeliveryStatus.FAILED);
        ArgumentCaptor<DeliveryFailure> failureCaptor =
            ArgumentCaptor.forClass(DeliveryFailure.class);
        verify(failureRepository).save(failureCaptor.capture());
        assertThat(failureCaptor.getValue().getAdminReasonCode())
            .isEqualTo(AdminDeliveryFailureReason.SYSTEM_RECOVERY.name());
        verify(eventPublisher).publishRefundConfirmed(
            org.mockito.ArgumentMatchers.eq(delivery)
            , org.mockito.ArgumentMatchers.eq(DeliveryRefundReason.FORCE_MAJEURE_CANCELED)
            , any()
        );
    }

    @Test
    @DisplayName("관리자는 READY 배송을 출발 전 실패로만 복구할 수 있다")
    void recoversReadyAsBeforeDepartureFailure() {
        delivery = configureLockedExecution();
        AdminDeliveryRecoveryRequest request = new AdminDeliveryRecoveryRequest(
            DeliveryRecoveryResult.FAILED
            , AdminRecoveryReason.APP_FAILURE
            , null
            , RIDER_ID
            , null
            , new RiderDeliveryFailureRequest(
                DeliveryFailureStage.BEFORE_DEPARTURE
                , DeliveryFailureCode.VEHICLE_ISSUE
                , null
                , null
                , null
                , false
                , null
            )
        );
        when(delivery.getStatus()).thenReturn(DeliveryStatus.READY, DeliveryStatus.FAILED);
        when(deliveryRepository.transitionStatus(
            DELIVERY_ID, DeliveryStatus.READY, DeliveryStatus.FAILED
        )).thenReturn(1);

        var response = recoveryService.recover(
            ADMIN_ID, UserRole.ADMIN, DELIVERY_PUBLIC_ID, request, null
        );

        assertThat(response.status()).isEqualTo(DeliveryStatus.FAILED);
        verify(failureRepository).save(any());
        verify(historyRepository).save(any());
    }

    @Test
    @DisplayName("READY 배송을 완료로 복구하는 허용되지 않은 전이는 거절한다")
    void rejectsReadyToDeliveredRecovery() {
        delivery = configureLockedExecution();
        AdminDeliveryRecoveryRequest request = new AdminDeliveryRecoveryRequest(
            DeliveryRecoveryResult.DELIVERED
            , AdminRecoveryReason.DEVICE_FAILURE
            , null
            , RIDER_ID
            , new RiderDeliveryCompletionRequest(ActualHandoffType.DIRECT, null, null, null)
            , null
        );
        when(delivery.getStatus()).thenReturn(DeliveryStatus.READY);

        assertThatThrownBy(() -> recoveryService.recover(
            ADMIN_ID, UserRole.ADMIN, DELIVERY_PUBLIC_ID, request, null
        )).isInstanceOf(com.chapchap.delivery.global.exception.business.DeliveryStateConflictException.class);

        verify(completionRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 비대면 완료 복구는 제출 사진과 관리자 업로드 주체를 보존한다")
    void recoversNonDirectCompletionWithPhoto() {
        delivery = configureLockedExecution();
        when(delivery.getTermsAgreed()).thenReturn(true);
        when(delivery.getRequestHandoffType()).thenReturn(RequestHandoffType.DOORSTEP);
        when(delivery.getStatus()).thenReturn(
            DeliveryStatus.DELIVERING, DeliveryStatus.DELIVERING, DeliveryStatus.DELIVERED
        );
        when(deliveryRepository.transitionStatus(
            DELIVERY_ID, DeliveryStatus.DELIVERING, DeliveryStatus.DELIVERED
        )).thenReturn(1);
        DeliveryCompletion completion = mock(DeliveryCompletion.class);
        when(completionRepository.save(any())).thenReturn(completion);
        MockMultipartFile photo = new MockMultipartFile(
            "photo", "proof.jpg", "image/jpeg", new byte[] {1, 2, 3}
        );
        LocalDateTime uploadedAt = LocalDateTime.of(2026, 9, 7, 12, 0);
        when(photoFileService.store(DELIVERY_PUBLIC_ID, ADMIN_ID, photo)).thenReturn(
            new DeliveryPhotoFileService.StoredPhoto(
                "delivery-proof/key", "proof.jpg", "image/jpeg", 3L, ADMIN_ID, uploadedAt
            )
        );
        AdminDeliveryRecoveryRequest request = new AdminDeliveryRecoveryRequest(
            DeliveryRecoveryResult.DELIVERED
            , AdminRecoveryReason.DEVICE_FAILURE
            , null
            , RIDER_ID
            , new RiderDeliveryCompletionRequest(
                ActualHandoffType.DOORSTEP, "현관문 앞", null, null
            )
            , null
        );

        recoveryService.recover(ADMIN_ID, UserRole.ADMIN, DELIVERY_PUBLIC_ID, request, photo);

        ArgumentCaptor<DeliveryCompletionPhoto> photoCaptor =
            ArgumentCaptor.forClass(DeliveryCompletionPhoto.class);
        verify(photoRepository).save(photoCaptor.capture());
        assertThat(photoCaptor.getValue().getDeliveryCompletion()).isSameAs(completion);
        assertThat(photoCaptor.getValue().getStorageKey()).isEqualTo("delivery-proof/key");
        assertThat(photoCaptor.getValue().getUploadedBy()).isEqualTo(ADMIN_ID);
    }

    @Test
    @DisplayName("관리자 비대면 완료 복구는 증빙 사진이 없으면 거절한다")
    void rejectsNonDirectCompletionRecovery() {
        AdminDeliveryRecoveryRequest request = new AdminDeliveryRecoveryRequest(
            DeliveryRecoveryResult.DELIVERED
            , AdminRecoveryReason.DEVICE_FAILURE
            , null
            , RIDER_ID
            , new RiderDeliveryCompletionRequest(
                ActualHandoffType.DOORSTEP
                , "문 앞"
                , null
                , null
            )
            , null
        );

        assertThatThrownBy(
            () -> recoveryService.recover(
                ADMIN_ID
                , UserRole.ADMIN
                , DELIVERY_PUBLIC_ID
                , request
                , null
            )
        ).isInstanceOf(com.chapchap.delivery.global.exception.business.DeliveryHandoffInfoRequiredException.class);

        verify(deliveryRepository, never()).findByDeliveryPublicId(any());
    }

    @Test
    @DisplayName("OTHER 장애 사유에 상세 설명이 없으면 복구를 거절한다")
    void rejectsOtherRecoveryReasonWithoutDetail() {
        AdminDeliveryRecoveryRequest request = new AdminDeliveryRecoveryRequest(
            DeliveryRecoveryResult.DELIVERED
            , AdminRecoveryReason.OTHER
            , null
            , RIDER_ID
            , new RiderDeliveryCompletionRequest(
                ActualHandoffType.DIRECT
                , null
                , null
                , null
            )
            , null
        );

        assertThatThrownBy(
            () -> recoveryService.recover(
                ADMIN_ID
                , UserRole.ADMIN
                , DELIVERY_PUBLIC_ID
                , request
                , null
            )
        ).isInstanceOf(InvalidDeliveryInfoException.class);

        verify(deliveryRepository, never()).findByDeliveryPublicId(any());
    }

    private Delivery configureLockedExecution() {
        DeliveryGroup group = mock(DeliveryGroup.class);
        Delivery target = mock(Delivery.class);
        com.chapchap.delivery.domain.rider.entity.Rider rider =
            mock(com.chapchap.delivery.domain.rider.entity.Rider.class);
        DeliveryAssignment assignment = mock(DeliveryAssignment.class);
        DeliveryAssignmentItem item = mock(DeliveryAssignmentItem.class);

        when(group.getId()).thenReturn(GROUP_ID);
        lenient().when(target.getId()).thenReturn(DELIVERY_ID);
        when(target.getDeliveryPublicId()).thenReturn(DELIVERY_PUBLIC_ID);
        when(target.getDeliveryGroup()).thenReturn(group);
        lenient().when(target.getDeliveryVersion()).thenReturn(3);
        lenient().when(rider.getId()).thenReturn(RIDER_ID);
        when(deliveryRepository.findByDeliveryPublicId(DELIVERY_PUBLIC_ID))
            .thenReturn(Optional.of(target));
        when(groupRepository.findByIdForUpdate(GROUP_ID)).thenReturn(Optional.of(group));
        when(riderRepository.findAllByIdInForUpdate(List.of(RIDER_ID)))
            .thenReturn(List.of(rider));
        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID))
            .thenReturn(List.of(target));
        when(assignmentRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID))
            .thenReturn(List.of(assignment));
        when(assignmentItemRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID))
            .thenReturn(List.of(item));
        return target;
    }
}
