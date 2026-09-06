package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.chapchap.delivery.domain.delivery.constant.IntegrationEventDirection;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface IntegrationEventRecordRepository
    extends JpaRepository<IntegrationEventRecord, Long> {

    boolean existsByEventId(String eventId);

    boolean existsByBusinessKey(String businessKey);

    @Query("""
        SELECT record
        FROM IntegrationEventRecord record
        WHERE (:direction IS NULL OR record.direction = :direction)
          AND (:status IS NULL OR record.status = :status)
          AND (:eventType IS NULL OR record.eventType = :eventType)
          AND (:from IS NULL OR record.lastAttemptedAt >= :from)
          AND (:to IS NULL OR record.lastAttemptedAt < :to)
    """)
    Page<IntegrationEventRecord> findAllForAdmin(
        @Param("direction") IntegrationEventDirection direction
        , @Param("status") IntegrationEventStatus status
        , @Param("eventType") String eventType
        , @Param("from") LocalDateTime from
        , @Param("to") LocalDateTime to
        , Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT record FROM IntegrationEventRecord record WHERE record.id = :id")
    Optional<IntegrationEventRecord> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        SELECT record
        FROM IntegrationEventRecord record
        WHERE record.direction = :direction
          AND record.status = :status
          AND record.eventType <> :excludedEventType
        ORDER BY record.id ASC
    """)
    List<IntegrationEventRecord> findFailedForAdminNotification(
        @Param("direction") IntegrationEventDirection direction
        , @Param("status") IntegrationEventStatus status
        , @Param("excludedEventType") String excludedEventType
    );

    List<IntegrationEventRecord> findAllByDirectionAndStatusOrderByIdAsc(
        IntegrationEventDirection direction, IntegrationEventStatus status
    );
}
