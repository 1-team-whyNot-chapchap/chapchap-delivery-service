package com.chapchap.delivery.domain.rider.repository;

import com.chapchap.delivery.domain.rider.entity.Rider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RiderRepository extends JpaRepository<Rider, Long> {
    Optional<Rider> findByIdAndDeletedAtIsNull(Long id);

    Optional<Rider> findByAuthUserIdAndDeletedAtIsNull(Long authUserId);

    Optional<Rider> findByAuthUserId(Long authUserId);

    List<Rider> findAllByDeletedAtIsNullOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM Rider r
        WHERE r.id IN :riderIds
          AND r.deletedAt IS NULL
        ORDER BY r.id ASC
    """)
    List<Rider> findAllByIdInForUpdate(
        @Param("riderIds") Collection<Long> riderIds
    );
}