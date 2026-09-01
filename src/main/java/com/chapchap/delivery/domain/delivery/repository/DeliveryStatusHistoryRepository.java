package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.DeliveryStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryStatusHistoryRepository
    extends JpaRepository<DeliveryStatusHistory, Long> {
}