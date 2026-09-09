package com.chapchap.delivery.domain.rider.repository;

import com.chapchap.delivery.domain.rider.entity.RiderDeliveryArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
        SELECT COUNT(rda) > 0
        FROM RiderDeliveryArea rda
        WHERE rda.rider.id = :riderId
            AND rda.deliveryAreaCode = :deliveryAreaCode
            AND rda.isActive = true
            AND rda.deletedAt IS NULL
            AND rda.effectiveFrom <= :deliveryDate
            AND (
                    rda.effectiveTo IS NULL
                OR rda.effectiveTo >= :deliveryDate
            )
    """)
    boolean existsAvailableDeliveryArea(
        @Param("riderId") Long riderId
        , @Param("deliveryAreaCode") String deliveryAreaCode
        , @Param("deliveryDate") LocalDate deliveryDate
    );
}