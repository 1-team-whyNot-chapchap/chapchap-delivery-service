package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionPhotoRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.response.DeliveryPhotoAccessResponse;
import com.chapchap.delivery.global.config.DeliveryPhotoStorageProperties;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.CompletionPhotoRequiredException;
import com.chapchap.delivery.global.exception.business.DeliveryNotFoundException;
import com.chapchap.delivery.global.storage.DeliveryPhotoStorage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryPhotoAccessService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final DeliveryAccessService accessService;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryCompletionRepository completionRepository;
    private final DeliveryCompletionPhotoRepository photoRepository;
    private final DeliveryPhotoStorage storage;
    private final DeliveryPhotoStorageProperties properties;

    public DeliveryPhotoAccessService(
        DeliveryAccessService accessService, DeliveryRepository deliveryRepository,
        DeliveryCompletionRepository completionRepository,
        DeliveryCompletionPhotoRepository photoRepository, DeliveryPhotoStorage storage,
        DeliveryPhotoStorageProperties properties
    ) {
        this.accessService=accessService; this.deliveryRepository=deliveryRepository;
        this.completionRepository=completionRepository; this.photoRepository=photoRepository;
        this.storage=storage; this.properties=properties;
    }

    @Transactional(readOnly = true)
    public DeliveryPhotoAccessResponse forAdmin(Long userId, UserRole role, String publicId) {
        accessService.validateAdminAccess(userId, role);
        return issue(publicId, null);
    }

    @Transactional(readOnly = true)
    public DeliveryPhotoAccessResponse forCustomer(Long userId, UserRole role, String publicId) {
        if (role != UserRole.CUSTOMER) throw new DeliveryAccessForbiddenException();
        return issue(publicId, userId);
    }

    private DeliveryPhotoAccessResponse issue(String publicId, Long customerId) {
        Delivery delivery=deliveryRepository.findByDeliveryPublicId(publicId)
            .orElseThrow(DeliveryNotFoundException::new);
        if (customerId!=null && !delivery.getCustomerId().equals(customerId))
            throw new DeliveryAccessForbiddenException();
        var completion=completionRepository.findByDeliveryId(delivery.getId())
            .orElseThrow(CompletionPhotoRequiredException::new);
        var photo=photoRepository.findByDeliveryCompletionId(completion.getId())
            .orElseThrow(CompletionPhotoRequiredException::new);
        LocalDateTime now=LocalDateTime.now(KST);
        return new DeliveryPhotoAccessResponse(
            storage.createPresignedGetUrl(photo.getStorageKey(),
                Duration.ofMinutes(properties.presignedUrlMinutes())),
            now.plusMinutes(properties.presignedUrlMinutes()).atZone(KST).toOffsetDateTime());
    }
}
