package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.DeliveryResultCorrection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryResultCorrectionRepository
    extends JpaRepository<DeliveryResultCorrection, Long> {

    List<DeliveryResultCorrection> findAllByDelivery_IdOrderByIdAsc(Long deliveryId);

    List<DeliveryResultCorrection> findAllByDelivery_IdInOrderByIdAsc(List<Long> deliveryIds);
}
