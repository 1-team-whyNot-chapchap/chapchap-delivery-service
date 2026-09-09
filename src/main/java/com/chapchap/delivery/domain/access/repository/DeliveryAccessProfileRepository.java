package com.chapchap.delivery.domain.access.repository;

import com.chapchap.delivery.domain.access.entity.DeliveryAccessProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryAccessProfileRepository extends JpaRepository<DeliveryAccessProfile, Long> {
    Optional<DeliveryAccessProfile> findByAuthUserId(
        Long authUserId
    );
}