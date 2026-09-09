package com.chapchap.delivery.domain.rider.repository;

import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.rider.entity.RiderScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RiderScheduleExceptionRepository extends JpaRepository<RiderScheduleException, Long> {
    Optional<RiderScheduleException> findByIdAndDeletedAtIsNull(
        Long id
    );

    Optional<RiderScheduleException> findByIdAndRiderIdAndDeletedAtIsNull(
        Long id
        , Long riderId
    );

    List<RiderScheduleException>
    findAllByRiderIdAndScheduleDateBetweenAndDeletedAtIsNullOrderByScheduleDateAsc(
        Long riderId
        , LocalDate startDate
        , LocalDate endDate
    );

    Optional<RiderScheduleException> findByRiderIdAndScheduleDateAndSlotId(
        Long riderId
        , LocalDate scheduleDate
        , Long slotId
    );

    @Query("""
        SELECT rse
        FROM RiderScheduleException rse
        WHERE rse.rider.id = :riderId
            AND rse.scheduleDate = :scheduleDate
            AND rse.slot.code = :deliverySlot
            AND rse.deletedAt IS NULL
    """)
    Optional<RiderScheduleException> findWorkingDecision(
        @Param("riderId") Long riderId
        , @Param("scheduleDate") LocalDate scheduleDate
        , @Param("deliverySlot") DeliverySlotCode deliverySlot
    );
}