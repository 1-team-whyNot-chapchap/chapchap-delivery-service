package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryResultCorrectionReason;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.RequestHandoffType;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletion;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletionPhoto;
import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import com.chapchap.delivery.domain.delivery.entity.DeliveryResultCorrection;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionPhotoRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRecipientSnapshotRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryResultCorrectionRepository;
import com.chapchap.delivery.domain.delivery.request.AdminDeliveryResultCorrectionRequest;
import com.chapchap.delivery.global.exception.business.DeliveryResultNotCorrectableException;
import com.chapchap.delivery.global.exception.business.OtherReasonDetailRequiredException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDeliveryResultCorrectionServiceTest {
    @Mock private DeliveryAccessService accessService;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryCompletionRepository completionRepository;
    @Mock private DeliveryCompletionPhotoRepository photoRepository;
    @Mock private DeliveryFailureRepository failureRepository;
    @Mock private DeliveryRecipientSnapshotRepository recipientSnapshotRepository;
    @Mock private DeliveryResultCorrectionRepository correctionRepository;
    @Mock private AuditHistoryRepository auditHistoryRepository;

    private AdminDeliveryResultCorrectionService service;

    @BeforeEach
    void setUp() {
        service = new AdminDeliveryResultCorrectionService(
            accessService, deliveryRepository, completionRepository, photoRepository,
            failureRepository, recipientSnapshotRepository, correctionRepository,
            auditHistoryRepository, new ObjectMapper()
        );
        lenient().when(correctionRepository.saveAll(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("완료 정정은 원본 대신 정정 이력에 before/after 값을 추가한다")
    void correctCompletionAppendsCorrectionHistory() {
        Delivery delivery = deliveredDelivery(RequestHandoffType.DOORSTEP);
        DeliveryCompletion completion = completion(ActualHandoffType.DOORSTEP, "기존 위치");
        stubCompletion(delivery, completion);
        when(photoRepository.findByDeliveryCompletionId(20L))
            .thenReturn(Optional.of(mock(DeliveryCompletionPhoto.class)));

        service.correctCompletion(
            7L, UserRole.ADMIN, "delivery-1",
            request("storage_location", "새 위치", DeliveryResultCorrectionReason.DATA_ENTRY_ERROR, null)
        );

        ArgumentCaptor<List<DeliveryResultCorrection>> captor = ArgumentCaptor.forClass(List.class);
        verify(correctionRepository).saveAll(captor.capture());
        DeliveryResultCorrection correction = captor.getValue().get(0);
        assertThat(correction.getFieldName()).isEqualTo("storage_location");
        assertThat(correction.getBeforeValue()).isEqualTo("기존 위치");
        assertThat(correction.getAfterValue()).isEqualTo("새 위치");
        verify(auditHistoryRepository).save(any());
    }

    @Test
    @DisplayName("현재 유효값과 동일한 값으로는 정정 이력을 만들 수 없다")
    void rejectsNoOpCorrection() {
        Delivery delivery = deliveredDelivery(RequestHandoffType.DOORSTEP);
        DeliveryCompletion completion = completion(ActualHandoffType.DOORSTEP, "같은 위치");
        stubCompletion(delivery, completion);

        assertThatThrownBy(() -> service.correctCompletion(
            7L, UserRole.ADMIN, "delivery-1",
            request("storage_location", "같은 위치", DeliveryResultCorrectionReason.DATA_ENTRY_ERROR, null)
        )).isInstanceOf(DeliveryResultNotCorrectableException.class);

        verify(correctionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("실패 설명을 null로 정정하면 빈 문자열로 바꾸지 않고 null을 보존한다")
    void preservesNullFailureDetail() {
        Delivery delivery = failedDelivery();
        DeliveryFailure failure = mock(DeliveryFailure.class);
        when(failure.getFailureCode()).thenReturn(DeliveryFailureCode.ACCESS_DENIED);
        when(failure.getFailureDetail()).thenReturn("잘못 입력된 설명");
        when(deliveryRepository.findByDeliveryPublicIdForUpdate("delivery-1"))
            .thenReturn(Optional.of(delivery));
        when(failureRepository.findByDeliveryId(1L)).thenReturn(Optional.of(failure));
        when(correctionRepository.findAllByDelivery_IdOrderByIdAsc(1L)).thenReturn(List.of());

        service.correctFailure(
            7L, UserRole.ADMIN, "delivery-1",
            request("failure_detail", null, DeliveryResultCorrectionReason.DATA_ENTRY_ERROR, null)
        );

        ArgumentCaptor<List<DeliveryResultCorrection>> captor = ArgumentCaptor.forClass(List.class);
        verify(correctionRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getAfterValue()).isNull();
    }

    @Test
    @DisplayName("nullable 필드의 빈 문자열은 null과 같은 값으로 취급해 무의미한 정정을 막는다")
    void rejectsBlankCorrectionWhenCurrentValueIsNull() {
        Delivery delivery = failedDelivery();
        DeliveryFailure failure = mock(DeliveryFailure.class);
        when(failure.getFailureCode()).thenReturn(DeliveryFailureCode.ACCESS_DENIED);
        when(failure.getFailureDetail()).thenReturn(null);
        when(deliveryRepository.findByDeliveryPublicIdForUpdate("delivery-1"))
            .thenReturn(Optional.of(delivery));
        when(failureRepository.findByDeliveryId(1L)).thenReturn(Optional.of(failure));
        when(correctionRepository.findAllByDelivery_IdOrderByIdAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.correctFailure(
            7L, UserRole.ADMIN, "delivery-1",
            request("failure_detail", "   ", DeliveryResultCorrectionReason.DATA_ENTRY_ERROR, null)
        )).isInstanceOf(DeliveryResultNotCorrectableException.class);

        verify(correctionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("DIRECT 완료를 사진 없이 비대면 완료로 정정할 수 없다")
    void rejectsDirectToNonDirectWithoutPhotoEvidence() {
        Delivery delivery = deliveredDelivery(RequestHandoffType.DIRECT);
        DeliveryCompletion completion = completion(ActualHandoffType.DIRECT, null);
        stubCompletion(delivery, completion);
        when(photoRepository.findByDeliveryCompletionId(20L)).thenReturn(Optional.empty());

        AdminDeliveryResultCorrectionRequest request = new AdminDeliveryResultCorrectionRequest(
            List.of(
                new AdminDeliveryResultCorrectionRequest.Change("actual_handoff_type", "DOORSTEP"),
                new AdminDeliveryResultCorrectionRequest.Change("storage_location", "현관문 앞")
            ),
            DeliveryResultCorrectionReason.OPERATIONAL_REVIEW,
            null
        );

        assertThatThrownBy(() -> service.correctCompletion(
            7L, UserRole.ADMIN, "delivery-1", request
        )).isInstanceOf(DeliveryResultNotCorrectableException.class);

        verify(correctionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("OTHER 정정 사유는 상세 설명이 필수다")
    void otherReasonRequiresDetail() {
        Delivery delivery = failedDelivery();
        DeliveryFailure failure = mock(DeliveryFailure.class);
        when(failure.getFailureCode()).thenReturn(DeliveryFailureCode.ACCESS_DENIED);
        when(failure.getFailureDetail()).thenReturn(null);
        when(deliveryRepository.findByDeliveryPublicIdForUpdate("delivery-1"))
            .thenReturn(Optional.of(delivery));
        when(failureRepository.findByDeliveryId(1L)).thenReturn(Optional.of(failure));
        when(correctionRepository.findAllByDelivery_IdOrderByIdAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.correctFailure(
            7L, UserRole.ADMIN, "delivery-1",
            request("failure_code", "INVALID_ADDRESS", DeliveryResultCorrectionReason.OTHER, "  ")
        )).isInstanceOf(OtherReasonDetailRequiredException.class);
    }

    private void stubCompletion(Delivery delivery, DeliveryCompletion completion) {
        when(deliveryRepository.findByDeliveryPublicIdForUpdate("delivery-1"))
            .thenReturn(Optional.of(delivery));
        when(completionRepository.findByDeliveryId(1L)).thenReturn(Optional.of(completion));
        when(correctionRepository.findAllByDelivery_IdOrderByIdAsc(1L)).thenReturn(List.of());
    }

    private Delivery deliveredDelivery(RequestHandoffType requestedHandoffType) {
        Delivery delivery = mock(Delivery.class);
        lenient().when(delivery.getId()).thenReturn(1L);
        lenient().when(delivery.getDeliveryPublicId()).thenReturn("delivery-1");
        lenient().when(delivery.getStatus()).thenReturn(DeliveryStatus.DELIVERED);
        lenient().when(delivery.getTermsAgreed()).thenReturn(true);
        lenient().when(delivery.getRequestHandoffType()).thenReturn(requestedHandoffType);
        return delivery;
    }

    private Delivery failedDelivery() {
        Delivery delivery = mock(Delivery.class);
        lenient().when(delivery.getId()).thenReturn(1L);
        lenient().when(delivery.getDeliveryPublicId()).thenReturn("delivery-1");
        lenient().when(delivery.getStatus()).thenReturn(DeliveryStatus.FAILED);
        return delivery;
    }

    private DeliveryCompletion completion(ActualHandoffType handoffType, String storageLocation) {
        DeliveryCompletion completion = mock(DeliveryCompletion.class);
        lenient().when(completion.getId()).thenReturn(20L);
        lenient().when(completion.getActualHandoffType()).thenReturn(handoffType);
        lenient().when(completion.getStorageLocation()).thenReturn(storageLocation);
        return completion;
    }

    private AdminDeliveryResultCorrectionRequest request(
        String fieldName, String afterValue,
        DeliveryResultCorrectionReason reasonCode, String reasonDetail
    ) {
        return new AdminDeliveryResultCorrectionRequest(
            List.of(new AdminDeliveryResultCorrectionRequest.Change(fieldName, afterValue)),
            reasonCode, reasonDetail
        );
    }
}
