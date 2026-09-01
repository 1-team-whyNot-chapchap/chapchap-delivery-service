package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationEventRecordRepository
    extends JpaRepository<IntegrationEventRecord, Long> {

    boolean existsByEventId(String eventId);
}