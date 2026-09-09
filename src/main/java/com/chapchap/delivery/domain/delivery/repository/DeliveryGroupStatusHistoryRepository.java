package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.DeliveryGroupStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryGroupStatusHistoryRepository
    extends JpaRepository<DeliveryGroupStatusHistory, Long> {
}