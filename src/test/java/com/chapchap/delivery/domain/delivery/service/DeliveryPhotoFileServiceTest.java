package com.chapchap.delivery.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.chapchap.delivery.global.config.DeliveryPhotoStorageProperties;
import com.chapchap.delivery.global.exception.business.InvalidDeliveryPhotoInfoException;
import com.chapchap.delivery.global.storage.DeliveryPhotoStorage;
import java.io.InputStream;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class DeliveryPhotoFileServiceTest {
    @Mock
    private DeliveryPhotoStorage storage;

    @Test
    @DisplayName("허용된 사진은 MinIO에 저장하고 영구 완료 사진 메타데이터를 반환한다")
    void storesValidPhoto() {
        DeliveryPhotoFileService service = service(1024L, Set.of("image/jpeg"));
        MockMultipartFile photo = new MockMultipartFile(
            "photo"
            , "proof.jpg"
            , "image/jpeg"
            , new byte[]{1, 2, 3}
        );

        DeliveryPhotoFileService.StoredPhoto stored = service.store(
            "delivery-public-id"
            , 10L
            , photo
        );

        assertThat(stored.storageKey()).startsWith("delivery-proof/delivery-public-id/");
        assertThat(stored.originalFilename()).isEqualTo("proof.jpg");
        assertThat(stored.contentType()).isEqualTo("image/jpeg");
        assertThat(stored.fileSize()).isEqualTo(3L);
        assertThat(stored.uploadedBy()).isEqualTo(10L);
        assertThat(stored.uploadedAt()).isNotNull();

        verify(storage).upload(
            eq(stored.storageKey())
            , any(InputStream.class)
            , eq(3L)
            , eq("image/jpeg")
        );
    }

    @Test
    @DisplayName("허용하지 않은 MIME 타입은 저장 전에 거절한다")
    void rejectsUnsupportedContentType() {
        DeliveryPhotoFileService service = service(1024L, Set.of("image/jpeg"));
        MockMultipartFile photo = new MockMultipartFile(
            "photo"
            , "proof.gif"
            , "image/gif"
            , new byte[]{1, 2, 3}
        );

        assertThatThrownBy(
            () -> service.store("delivery-public-id", 10L, photo)
        ).isInstanceOf(InvalidDeliveryPhotoInfoException.class);

        verify(storage, never()).upload(any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("빈 사진은 저장 전에 거절한다")
    void rejectsEmptyPhoto() {
        DeliveryPhotoFileService service = service(1024L, Set.of("image/jpeg"));
        MockMultipartFile photo = new MockMultipartFile(
            "photo"
            , "empty.jpg"
            , "image/jpeg"
            , new byte[0]
        );

        assertThatThrownBy(
            () -> service.store("delivery-public-id", 10L, photo)
        ).isInstanceOf(InvalidDeliveryPhotoInfoException.class);

        verify(storage, never()).upload(any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("최대 파일 크기를 초과하면 저장 전에 거절한다")
    void rejectsOversizedPhoto() {
        DeliveryPhotoFileService service = service(2L, Set.of("image/jpeg"));
        MockMultipartFile photo = new MockMultipartFile(
            "photo"
            , "proof.jpg"
            , "image/jpeg"
            , new byte[]{1, 2, 3}
        );

        assertThatThrownBy(
            () -> service.store("delivery-public-id", 10L, photo)
        ).isInstanceOf(InvalidDeliveryPhotoInfoException.class);

        verify(storage, never()).upload(any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("원본 파일명에는 경로를 저장하지 않고 파일명만 보존한다")
    void stripsPathFromOriginalFilename() {
        DeliveryPhotoFileService service = service(1024L, Set.of("image/jpeg"));
        MockMultipartFile photo = new MockMultipartFile(
            "photo"
            , "C:\\fakepath\\proof.jpg"
            , "image/jpeg"
            , new byte[]{1, 2, 3}
        );

        DeliveryPhotoFileService.StoredPhoto stored = service.store(
            "delivery-public-id"
            , 10L
            , photo
        );

        assertThat(stored.originalFilename()).isEqualTo("proof.jpg");
    }

    private DeliveryPhotoFileService service(long maxFileSize, Set<String> allowedContentTypes) {
        return new DeliveryPhotoFileService(
            storage
            , new DeliveryPhotoStorageProperties(
                "http://localhost:9000"
                , "access"
                , "secret"
                , "delivery-photos"
                , 10
                , maxFileSize
                , allowedContentTypes
            )
        );
    }
}
