package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.global.config.DeliveryPhotoStorageProperties;
import com.chapchap.delivery.global.exception.TechnicalException;
import com.chapchap.delivery.global.exception.business.InvalidDeliveryPhotoInfoException;
import com.chapchap.delivery.global.storage.DeliveryPhotoStorage;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DeliveryPhotoFileService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryPhotoStorage storage;
    private final DeliveryPhotoStorageProperties properties;

    public DeliveryPhotoFileService(
        DeliveryPhotoStorage storage
        , DeliveryPhotoStorageProperties properties
    ) {
        this.storage = storage;
        this.properties = properties;
    }

    public StoredPhoto store(
        String deliveryPublicId
        , Long uploadedBy
        , MultipartFile photo
    ) {
        validate(photo);

        String contentType = photo.getContentType();
        String storageKey = "delivery-proof/" + deliveryPublicId + "/" + UUID.randomUUID();
        LocalDateTime uploadedAt = LocalDateTime.now(KST);

        try (InputStream inputStream = photo.getInputStream()) {
            storage.upload(
                storageKey
                , inputStream
                , photo.getSize()
                , contentType
            );
        } catch (IOException exception) {
            throw new TechnicalException("Failed to read delivery completion photo", exception);
        }

        return new StoredPhoto(
            storageKey
            , normalizeFilename(photo.getOriginalFilename())
            , contentType
            , photo.getSize()
            , uploadedBy
            , uploadedAt
        );
    }

    public void delete(String storageKey) {
        storage.delete(storageKey);
    }

    private void validate(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new InvalidDeliveryPhotoInfoException();
        }

        String contentType = photo.getContentType();
        if (
            contentType == null
                || !properties.allowedContentTypes().contains(contentType)
                || photo.getSize() <= 0
                || photo.getSize() > properties.maxFileSizeBytes()
        ) {
            throw new InvalidDeliveryPhotoInfoException();
        }
    }

    private String normalizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return null;
        }
        String normalized = originalFilename.replace('\\', '/');
        int separatorIndex = normalized.lastIndexOf('/');
        String filename = separatorIndex >= 0
            ? normalized.substring(separatorIndex + 1)
            : normalized;
        if (filename.isBlank()) {
            return null;
        }
        return filename.length() <= 255 ? filename : filename.substring(filename.length() - 255);
    }

    public record StoredPhoto(
        String storageKey
        , String originalFilename
        , String contentType
        , long fileSize
        , Long uploadedBy
        , LocalDateTime uploadedAt
    ) {
    }
}
