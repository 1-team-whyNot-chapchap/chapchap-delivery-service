package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.DeliveryAreaCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryAreaCodeRepository extends JpaRepository<DeliveryAreaCode, Long> {
    Optional<DeliveryAreaCode> findByDistrictAndIsActiveTrue(
        String district
    );

    Optional<DeliveryAreaCode> findByAreaCodeAndIsActiveTrue(
        String areaCode
    );
}