package com.chapchap.delivery.domain.rider.repository;

import com.chapchap.delivery.domain.rider.entity.RiderDeliveryArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RiderDeliveryAreaRepository extends JpaRepository<RiderDeliveryArea, Long> {
    Optional<RiderDeliveryArea> findByIdAndDeletedAtIsNull(
        Long id
    );

    Optional<RiderDeliveryArea> findByIdAndRiderIdAndDeletedAtIsNull(
        Long id
        , Long riderId
    );

    List<RiderDeliveryArea> findAllByRiderIdAndDeletedAtIsNull(
        Long riderId
    );

    Optional<RiderDeliveryArea> findByRiderIdAndDeliveryAreaCodeAndEffectiveFrom(
        Long riderId
        , String deliveryAreaCode
        , LocalDate effectiveFrom
    );
}