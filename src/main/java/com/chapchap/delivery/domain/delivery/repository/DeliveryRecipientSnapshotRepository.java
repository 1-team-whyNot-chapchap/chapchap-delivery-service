package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.DeliveryRecipientSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRecipientSnapshotRepository
    extends JpaRepository<DeliveryRecipientSnapshot, Long> {
}