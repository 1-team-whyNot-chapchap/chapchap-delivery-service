package com.chapchap.delivery.domain.riderlocation.repository;

import com.chapchap.delivery.domain.riderlocation.entity.RiderCurrentLocation;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiderCurrentLocationRepository extends JpaRepository<RiderCurrentLocation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT location FROM RiderCurrentLocation location WHERE location.riderId = :riderId")
    Optional<RiderCurrentLocation> findByRiderIdForUpdate(@Param("riderId") Long riderId);
}
