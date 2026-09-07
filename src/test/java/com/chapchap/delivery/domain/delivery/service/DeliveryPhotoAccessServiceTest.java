package com.chapchap.delivery.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletion;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletionPhoto;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionPhotoRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.global.config.DeliveryPhotoStorageProperties;
import com.chapchap.delivery.global.exception.business.CompletionPhotoRequiredException;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryNotFoundException;
import com.chapchap.delivery.global.storage.DeliveryPhotoStorage;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryPhotoAccessServiceTest {
    private static final String PUBLIC_ID = "delivery-public-id";

    @Mock private DeliveryAccessService accessService;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryCompletionRepository completionRepository;
    @Mock private DeliveryCompletionPhotoRepository photoRepository;
    @Mock private DeliveryPhotoStorage storage;

    private DeliveryPhotoAccessService service;

    @BeforeEach
    void setUp() {
        service = new DeliveryPhotoAccessService(
            accessService
            , deliveryRepository
            , completionRepository
            , photoRepository
            , storage
            , new DeliveryPhotoStorageProperties(
                "http://localhost:9000"
                , "access"
                , "secret"
                , "delivery-photos"
                , Duration.ofMinutes(5)
                , 10_485_760L
                , Set.of("image/jpeg")
            )
        );
    }

    @Test
    @DisplayName("고객은 본인 배송의 완료 사진만 제한 URL로 조회한다")
    void customerCanAccessOwnCompletionPhoto() {
        Delivery delivery = mock(Delivery.class);
        DeliveryGroup group = currentGroup(delivery);
        DeliveryCompletion completion = mock(DeliveryCompletion.class);
        DeliveryCompletionPhoto photo = mock(DeliveryCompletionPhoto.class);
        when(delivery.getId()).thenReturn(1L);
        when(delivery.getCustomerId()).thenReturn(100L);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.DELIVERED);
        when(completion.getId()).thenReturn(2L);
        when(photo.getStorageKey()).thenReturn("delivery-proof/key");
        when(deliveryRepository.findByDeliveryPublicId(PUBLIC_ID)).thenReturn(Optional.of(delivery));
        when(completionRepository.findByDeliveryId(1L)).thenReturn(Optional.of(completion));
        when(photoRepository.findByDeliveryCompletionId(2L)).thenReturn(Optional.of(photo));
        when(storage.createPresignedGetUrl("delivery-proof/key", Duration.ofMinutes(5)))
            .thenReturn("https://minio.example/signed");

        var response = service.forCustomer(100L, UserRole.CUSTOMER, PUBLIC_ID);

        assertThat(response.accessUrl()).isEqualTo("https://minio.example/signed");
        assertThat(response.expiresAt()).isNotNull();
    }

    @Test
    @DisplayName("고객은 다른 고객의 완료 사진에 접근할 수 없다")
    void customerCannotAccessAnotherCustomersPhoto() {
        Delivery delivery = mock(Delivery.class);
        currentGroup(delivery);
        when(delivery.getCustomerId()).thenReturn(200L);
        when(deliveryRepository.findByDeliveryPublicId(PUBLIC_ID)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(
            () -> service.forCustomer(100L, UserRole.CUSTOMER, PUBLIC_ID)
        ).isInstanceOf(DeliveryNotFoundException.class);

        verify(storage, never()).createPresignedGetUrl(any(), any());
    }

    @Test
    @DisplayName("완료 사진이 없는 배송은 사진 접근 URL을 발급하지 않는다")
    void failsWhenCompletionPhotoDoesNotExist() {
        Delivery delivery = mock(Delivery.class);
        currentGroup(delivery);
        DeliveryCompletion completion = mock(DeliveryCompletion.class);
        when(delivery.getId()).thenReturn(1L);
        when(delivery.getCustomerId()).thenReturn(100L);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.DELIVERED);
        when(completion.getId()).thenReturn(2L);
        when(deliveryRepository.findByDeliveryPublicId(PUBLIC_ID)).thenReturn(Optional.of(delivery));
        when(completionRepository.findByDeliveryId(1L)).thenReturn(Optional.of(completion));
        when(photoRepository.findByDeliveryCompletionId(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> service.forCustomer(100L, UserRole.CUSTOMER, PUBLIC_ID)
        ).isInstanceOf(CompletionPhotoRequiredException.class);

        verify(storage, never()).createPresignedGetUrl(any(), any());
    }

    @Test
    @DisplayName("관리자 사진 조회는 관리자 접근 검증을 선행한다")
    void adminAccessIsValidated() {
        Delivery delivery = mock(Delivery.class);
        currentGroup(delivery);
        DeliveryCompletion completion = mock(DeliveryCompletion.class);
        DeliveryCompletionPhoto photo = mock(DeliveryCompletionPhoto.class);
        when(delivery.getId()).thenReturn(1L);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.DELIVERED);
        when(completion.getId()).thenReturn(2L);
        when(photo.getStorageKey()).thenReturn("delivery-proof/key");
        when(deliveryRepository.findByDeliveryPublicId(PUBLIC_ID)).thenReturn(Optional.of(delivery));
        when(completionRepository.findByDeliveryId(1L)).thenReturn(Optional.of(completion));
        when(photoRepository.findByDeliveryCompletionId(2L)).thenReturn(Optional.of(photo));
        when(storage.createPresignedGetUrl("delivery-proof/key", Duration.ofMinutes(5)))
            .thenReturn("https://minio.example/signed");

        service.forAdmin(7L, UserRole.ADMIN, PUBLIC_ID);

        verify(accessService).validateAdminAccess(7L, UserRole.ADMIN);
    }

    @Test
    @DisplayName("삭제된 배송 그룹의 사진 URL은 발급하지 않는다")
    void deletedGroupCannotIssuePhotoUrl() {
        Delivery delivery = mock(Delivery.class);
        DeliveryGroup group = currentGroup(delivery);
        when(group.getDeletedAt()).thenReturn(java.time.LocalDateTime.now());
        when(deliveryRepository.findByDeliveryPublicId(PUBLIC_ID)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.forAdmin(7L, UserRole.ADMIN, PUBLIC_ID))
            .isInstanceOf(DeliveryNotFoundException.class);

        verify(storage, never()).createPresignedGetUrl(any(), any());
    }

    @Test
    @DisplayName("완료 상태가 아닌 배송의 사진 URL은 발급하지 않는다")
    void nonDeliveredCannotIssuePhotoUrl() {
        Delivery delivery = mock(Delivery.class);
        currentGroup(delivery);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.DELIVERING);
        when(deliveryRepository.findByDeliveryPublicId(PUBLIC_ID)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.forAdmin(7L, UserRole.ADMIN, PUBLIC_ID))
            .isInstanceOf(CompletionPhotoRequiredException.class);

        verify(storage, never()).createPresignedGetUrl(any(), any());
    }

    private DeliveryGroup currentGroup(Delivery delivery) {
        DeliveryGroup group = mock(DeliveryGroup.class);
        when(delivery.getDeliveryGroup()).thenReturn(group);
        return group;
    }
}
