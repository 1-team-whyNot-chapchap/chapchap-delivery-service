package com.chapchap.delivery.domain.rider.repository;

import com.chapchap.delivery.domain.rider.entity.Rider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RiderRepository extends JpaRepository<Rider, Long> {
    Optional<Rider> findByIdAndDeletedAtIsNull(Long id);

    Optional<Rider> findByAuthUserIdAndDeletedAtIsNull(Long authUserId);

    Optional<Rider> findByAuthUserId(Long authUserId);
}