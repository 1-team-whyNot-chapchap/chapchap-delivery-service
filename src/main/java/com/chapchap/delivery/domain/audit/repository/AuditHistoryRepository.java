package com.chapchap.delivery.domain.audit.repository;

import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditHistoryRepository extends JpaRepository<AuditHistory, Long> {
}