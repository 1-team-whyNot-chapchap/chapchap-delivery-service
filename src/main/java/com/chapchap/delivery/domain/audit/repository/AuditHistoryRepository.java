package com.chapchap.delivery.domain.audit.repository;

import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditHistoryRepository extends JpaRepository<AuditHistory, Long> {
    @Query("""
        SELECT ah
        FROM AuditHistory ah
        WHERE (:entityType IS NULL OR ah.entityType = :entityType)
          AND (:entityId IS NULL OR ah.entityId = :entityId)
    """)
    Page<AuditHistory> findAllForAdmin(
        @Param("entityType") String entityType
        , @Param("entityId") Long entityId
        , Pageable pageable
    );
}
