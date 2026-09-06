package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletionPhoto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryCompletionPhotoRepository
    extends JpaRepository<DeliveryCompletionPhoto, Long> {

    Optional<DeliveryCompletionPhoto> findByDeliveryCompletionId(
        Long deliveryCompletionId
    );

    Optional<DeliveryCompletionPhoto> findByStorageKey(
        String storageKey
    );
}