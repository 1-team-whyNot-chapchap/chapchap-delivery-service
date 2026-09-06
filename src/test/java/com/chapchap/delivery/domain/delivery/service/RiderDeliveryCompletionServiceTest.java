package com.chapchap.delivery.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.RequestHandoffType;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletion;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletionPhoto;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionPhotoRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryDelayRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRecipientSnapshotRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.request.RiderDeliveryCompletionRequest;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.DeliveryHandoffInfoRequiredException;
import com.chapchap.delivery.global.exception.business.DeliveryStateConflictException;
import com.chapchap.delivery.global.kafka.producer.DeliveryEventRequestPublisher;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class RiderDeliveryCompletionServiceTest {
    private static final Long AUTH_USER_ID = 100L;
    private static final Long DELIVERY_ID = 1L;
    private static final Long GROUP_ID = 10L;
    private static final String DELIVERY_PUBLIC_ID = "delivery-public-id";

    @Mock private DeliveryAccessService accessService;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryGroupRepository groupRepository;
    @Mock private RiderRepository riderRepository;
    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private DeliveryAssignmentItemRepository assignmentItemRepository;
    @Mock private DeliveryCompletionRepository completionRepository;
    @Mock private DeliveryDelayRepository delayRepository;
    @Mock private DeliveryStatusHistoryRepository historyRepository;
    @Mock private DeliveryExecutionSupport executionSupport;
    @Mock private DeliveryEventRequestPublisher eventPublisher;
    @Mock private EntityManager entityManager;
    @Mock private DeliveryCompletionPhotoRepository photoRepository;
    @Mock private DeliveryRecipientSnapshotRepository recipientRepository;
    @Mock private DeliveryPhotoFileService photoFileService;
    @Mock private DeliveryDelayService deliveryDelayService;
    @Mock private TransactionTemplate transactionTemplate;

    private RiderDeliveryCompletionService service;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        service = new RiderDeliveryCompletionService(
            accessService
            , deliveryRepository
            , groupRepository
            , riderRepository
            , assignmentRepository
            , assignmentItemRepository
            , completionRepository
            , delayRepository
            , historyRepository
            , executionSupport
            , eventPublisher
            , entityManager
            , photoRepository
            , recipientRepository
            , photoFileService
            , deliveryDelayService
            , transactionTemplate
        );
    }

    @Test
    @DisplayName("DOORSTEP 완료는 사진과 보관 장소를 저장하고 DELIVERED로 전환한다")
    void completesDoorstepDeliveryWithPhoto() {
        Delivery delivery = lockedDelivery();
        when(delivery.getStatus()).thenReturn(
            DeliveryStatus.DELIVERING
            , DeliveryStatus.DELIVERING
            , DeliveryStatus.DELIVERED
        );
        when(delivery.getTermsAgreed()).thenReturn(true);
        when(deliveryRepository.transitionStatus(
            DELIVERY_ID, DeliveryStatus.DELIVERING, DeliveryStatus.DELIVERED
        )).thenReturn(1);
        when(completionRepository.save(any(DeliveryCompletion.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(photoRepository.save(any(DeliveryCompletionPhoto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(delayRepository.findByDeliveryId(DELIVERY_ID)).thenReturn(Optional.empty());
        when(deliveryDelayService.recordCompletionDelay(eq(delivery), any())).thenReturn(false);

        MockMultipartFile photo = photo();
        DeliveryPhotoFileService.StoredPhoto storedPhoto = new DeliveryPhotoFileService.StoredPhoto(
            "delivery-proof/key"
            , "proof.jpg"
            , "image/jpeg"
            , 3L
            , AUTH_USER_ID
            , LocalDateTime.of(2026, 9, 6, 12, 0)
        );
        when(photoFileService.store(DELIVERY_PUBLIC_ID, AUTH_USER_ID, photo))
            .thenReturn(storedPhoto);

        var response = service.complete(
            AUTH_USER_ID
            , DELIVERY_PUBLIC_ID
            , new RiderDeliveryCompletionRequest(
                ActualHandoffType.DOORSTEP
                , "현관문 앞"
                , null
                , null
            )
            , photo
        );

        assertThat(response.status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(response.hasCompletionPhoto()).isTrue();
        verify(deliveryRepository).transitionStatus(
            DELIVERY_ID, DeliveryStatus.DELIVERING, DeliveryStatus.DELIVERED
        );
        verify(historyRepository).save(any());
        verify(eventPublisher).publishStateChanged(eq("DELIVERY_COMPLETED"), eq(delivery), any());

        ArgumentCaptor<DeliveryCompletionPhoto> photoCaptor =
            ArgumentCaptor.forClass(DeliveryCompletionPhoto.class);
        verify(photoRepository).save(photoCaptor.capture());
        assertThat(photoCaptor.getValue().getStorageKey()).isEqualTo("delivery-proof/key");
        assertThat(photoCaptor.getValue().getUploadedBy()).isEqualTo(AUTH_USER_ID);

        InOrder uploadOrder = inOrder(transactionTemplate, photoFileService);
        uploadOrder.verify(transactionTemplate).execute(any());
        uploadOrder.verify(photoFileService).store(DELIVERY_PUBLIC_ID, AUTH_USER_ID, photo);
        uploadOrder.verify(transactionTemplate).execute(any());
    }

    @Test
    @DisplayName("DIRECT 요청 고객을 부재로 DOORSTEP 완료할 때 연락 근거가 없으면 거절한다")
    void rejectsDirectFallbackToDoorstepWithoutContactEvidence() {
        Delivery delivery = lockedDelivery();
        when(delivery.getStatus()).thenReturn(DeliveryStatus.DELIVERING);
        when(delivery.getTermsAgreed()).thenReturn(true);
        when(delivery.getRequestHandoffType()).thenReturn(RequestHandoffType.DIRECT);

        MockMultipartFile photo = photo();

        assertThatThrownBy(
            () -> service.complete(
                AUTH_USER_ID
                , DELIVERY_PUBLIC_ID
                , new RiderDeliveryCompletionRequest(
                    ActualHandoffType.DOORSTEP
                    , "현관문 앞"
                    , null
                    , null
                )
                , photo
            )
        ).isInstanceOf(DeliveryHandoffInfoRequiredException.class);

        verify(photoFileService, never()).store(any(), any(), any());
        verify(deliveryRepository, never()).transitionStatus(any(), any(), any());
    }

    @ParameterizedTest(name = "DIRECT 부재 대체 완료 방식: {0}")
    @EnumSource(
        value = ActualHandoffType.class
        , names = {"DOORSTEP", "OTHER"}
    )
    @DisplayName("DIRECT 요청 고객은 연락 근거가 있으면 안전한 비대면 방식으로 대체 완료할 수 있다")
    void completesDirectFallbackWithContactEvidence(ActualHandoffType actualHandoffType) {
        Delivery delivery = lockedDelivery();
        when(delivery.getStatus()).thenReturn(
            DeliveryStatus.DELIVERING
            , DeliveryStatus.DELIVERING
            , DeliveryStatus.DELIVERED
        );
        when(delivery.getTermsAgreed()).thenReturn(true);
        when(delivery.getRequestHandoffType()).thenReturn(RequestHandoffType.DIRECT);
        when(deliveryRepository.transitionStatus(
            DELIVERY_ID, DeliveryStatus.DELIVERING, DeliveryStatus.DELIVERED
        )).thenReturn(1);
        when(completionRepository.save(any(DeliveryCompletion.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(photoRepository.save(any(DeliveryCompletionPhoto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(delayRepository.findByDeliveryId(DELIVERY_ID)).thenReturn(Optional.empty());
        when(deliveryDelayService.recordCompletionDelay(eq(delivery), any())).thenReturn(false);

        MockMultipartFile photo = photo();
        DeliveryPhotoFileService.StoredPhoto storedPhoto = new DeliveryPhotoFileService.StoredPhoto(
            "delivery-proof/fallback"
            , "proof.jpg"
            , "image/jpeg"
            , 3L
            , AUTH_USER_ID
            , LocalDateTime.of(2026, 9, 6, 12, 0)
        );
        when(photoFileService.store(DELIVERY_PUBLIC_ID, AUTH_USER_ID, photo))
            .thenReturn(storedPhoto);

        OffsetDateTime contactedAt = OffsetDateTime.of(
            2026, 9, 6, 11, 58, 0, 0, ZoneOffset.ofHours(9)
        );

        var response = service.complete(
            AUTH_USER_ID
            , DELIVERY_PUBLIC_ID
            , new RiderDeliveryCompletionRequest(
                actualHandoffType
                , "안전한 보관 장소"
                , contactedAt
                , "NO_ANSWER"
            )
            , photo
        );

        assertThat(response.status()).isEqualTo(DeliveryStatus.DELIVERED);
        verify(photoFileService).store(DELIVERY_PUBLIC_ID, AUTH_USER_ID, photo);
        verify(photoRepository).save(any(DeliveryCompletionPhoto.class));
    }

    @Test
    @DisplayName("DIRECT 완료에는 사진이나 보관 장소를 받을 수 없다")
    void rejectsPhotoForDirectCompletion() {
        MockMultipartFile photo = photo();

        assertThatThrownBy(
            () -> service.complete(
                AUTH_USER_ID
                , DELIVERY_PUBLIC_ID
                , new RiderDeliveryCompletionRequest(
                    ActualHandoffType.DIRECT
                    , null
                    , null
                    , null
                )
                , photo
            )
        ).isInstanceOf(DeliveryHandoffInfoRequiredException.class);

        verify(transactionTemplate, never()).execute(any());
        verify(photoFileService, never()).store(any(), any(), any());
    }

    @Test
    @DisplayName("비대면 완료는 사진이 없으면 거절한다")
    void rejectsDoorstepCompletionWithoutPhoto() {
        assertThatThrownBy(
            () -> service.complete(
                AUTH_USER_ID
                , DELIVERY_PUBLIC_ID
                , new RiderDeliveryCompletionRequest(
                    ActualHandoffType.DOORSTEP
                    , "현관문 앞"
                    , null
                    , null
                )
                , null
            )
        ).isInstanceOf(DeliveryHandoffInfoRequiredException.class);

        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    @DisplayName("사진 저장 뒤 DB 상태 전이가 실패하면 MinIO 사진을 보상 삭제한다")
    void deletesStoredPhotoWhenDatabaseTransitionFails() {
        Delivery delivery = lockedDelivery();
        when(delivery.getStatus()).thenReturn(
            DeliveryStatus.DELIVERING
            , DeliveryStatus.DELIVERING
        );
        when(delivery.getTermsAgreed()).thenReturn(true);
        when(deliveryRepository.transitionStatus(
            DELIVERY_ID, DeliveryStatus.DELIVERING, DeliveryStatus.DELIVERED
        )).thenReturn(0);

        MockMultipartFile photo = photo();
        DeliveryPhotoFileService.StoredPhoto storedPhoto = new DeliveryPhotoFileService.StoredPhoto(
            "delivery-proof/orphan"
            , "proof.jpg"
            , "image/jpeg"
            , 3L
            , AUTH_USER_ID
            , LocalDateTime.of(2026, 9, 6, 12, 0)
        );
        when(photoFileService.store(DELIVERY_PUBLIC_ID, AUTH_USER_ID, photo))
            .thenReturn(storedPhoto);

        assertThatThrownBy(
            () -> service.complete(
                AUTH_USER_ID
                , DELIVERY_PUBLIC_ID
                , new RiderDeliveryCompletionRequest(
                    ActualHandoffType.DOORSTEP
                    , "현관문 앞"
                    , null
                    , null
                )
                , photo
            )
        ).isInstanceOf(DeliveryStateConflictException.class);

        verify(photoFileService).delete("delivery-proof/orphan");
        verify(completionRepository, never()).save(any());
    }

    @Test
    @DisplayName("사진 업로드 사이에 같은 완료가 먼저 커밋되면 새 사진은 사용하지 않고 삭제한다")
    void deletesUnusedPhotoWhenConcurrentSameCompletionWins() {
        Delivery delivery = lockedDelivery();
        when(delivery.getStatus()).thenReturn(
            DeliveryStatus.DELIVERING
            , DeliveryStatus.DELIVERED
            , DeliveryStatus.DELIVERED
        );
        when(delivery.getTermsAgreed()).thenReturn(true);

        DeliveryCompletion existingCompletion = mock(DeliveryCompletion.class);
        when(existingCompletion.getId()).thenReturn(50L);
        when(existingCompletion.getActualHandoffType()).thenReturn(ActualHandoffType.DOORSTEP);
        when(existingCompletion.getStorageLocation()).thenReturn("현관문 앞");
        when(existingCompletion.getContactAttemptedAt()).thenReturn(null);
        when(existingCompletion.getContactResult()).thenReturn(null);
        when(existingCompletion.getCompletedAt()).thenReturn(
            LocalDateTime.of(2026, 9, 6, 12, 0)
        );
        when(completionRepository.findByDeliveryId(DELIVERY_ID))
            .thenReturn(Optional.of(existingCompletion));
        when(photoRepository.findByDeliveryCompletionId(50L))
            .thenReturn(Optional.of(mock(DeliveryCompletionPhoto.class)));
        when(delayRepository.findByDeliveryId(DELIVERY_ID)).thenReturn(Optional.empty());

        MockMultipartFile photo = photo();
        DeliveryPhotoFileService.StoredPhoto uploadedPhoto = new DeliveryPhotoFileService.StoredPhoto(
            "delivery-proof/concurrent-orphan"
            , "proof.jpg"
            , "image/jpeg"
            , 3L
            , AUTH_USER_ID
            , LocalDateTime.of(2026, 9, 6, 12, 0)
        );
        when(photoFileService.store(DELIVERY_PUBLIC_ID, AUTH_USER_ID, photo))
            .thenReturn(uploadedPhoto);

        var response = service.complete(
            AUTH_USER_ID
            , DELIVERY_PUBLIC_ID
            , new RiderDeliveryCompletionRequest(
                ActualHandoffType.DOORSTEP
                , "현관문 앞"
                , null
                , null
            )
            , photo
        );

        assertThat(response.status()).isEqualTo(DeliveryStatus.DELIVERED);
        verify(photoFileService).delete("delivery-proof/concurrent-orphan");
        verify(completionRepository, never()).save(any());
        verify(photoRepository, never()).save(any());
    }

    private Delivery lockedDelivery() {
        DeliveryGroup group = mock(DeliveryGroup.class);
        Delivery delivery = mock(Delivery.class);
        Rider rider = mock(Rider.class);

        when(group.getId()).thenReturn(GROUP_ID);
        lenient().when(delivery.getId()).thenReturn(DELIVERY_ID);
        when(delivery.getDeliveryPublicId()).thenReturn(DELIVERY_PUBLIC_ID);
        when(delivery.getDeliveryGroup()).thenReturn(group);
        lenient().when(delivery.getDeliveryVersion()).thenReturn(3);
        when(rider.getIsDeliveryActive()).thenReturn(true);

        when(deliveryRepository.findByDeliveryPublicId(DELIVERY_PUBLIC_ID))
            .thenReturn(Optional.of(delivery));
        when(groupRepository.findByIdForUpdate(GROUP_ID)).thenReturn(Optional.of(group));
        when(riderRepository.findByAuthUserIdForUpdate(AUTH_USER_ID)).thenReturn(Optional.of(rider));
        when(deliveryRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID))
            .thenReturn(List.of(delivery));
        when(assignmentRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID))
            .thenReturn(List.of());
        when(assignmentItemRepository.findAllByDeliveryGroupIdForUpdate(GROUP_ID))
            .thenReturn(List.of());

        return delivery;
    }

    private MockMultipartFile photo() {
        return new MockMultipartFile(
            "photo"
            , "proof.jpg"
            , "image/jpeg"
            , new byte[]{1, 2, 3}
        );
    }
}
