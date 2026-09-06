package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.DeliveryStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryStatusHistoryRepository extends JpaRepository<DeliveryStatusHistory, Long> {
    Optional<DeliveryStatusHistory> findFirstByDelivery_IdAndToStatusOrderByChangedAtAsc(
        Long deliveryId
        , DeliveryStatus toStatus
    );
}