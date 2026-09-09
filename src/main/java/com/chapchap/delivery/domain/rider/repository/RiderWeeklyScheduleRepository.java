package com.chapchap.delivery.domain.rider.repository;

import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.rider.entity.RiderWeeklySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RiderWeeklyScheduleRepository
    extends JpaRepository<RiderWeeklySchedule, Long> {

    List<RiderWeeklySchedule> findAllByRiderIdAndDeletedAtIsNull(
        Long riderId
    );

    Optional<RiderWeeklySchedule> findByRiderIdAndDayOfWeekAndSlotId(
        Long riderId
        , Byte dayOfWeek
        , Long slotId
    );

    Optional<RiderWeeklySchedule> findByIdAndRiderIdAndDeletedAtIsNull(
        Long id
        , Long riderId
    );

    @Query("""
        SELECT COUNT(rws) > 0
        FROM RiderWeeklySchedule rws
        WHERE rws.rider.id = :riderId
            AND rws.dayOfWeek = :dayOfWeek
            AND rws.slot.code = :deliverySlot
            AND rws.deletedAt IS NULL
    """)
    boolean existsWorkingSchedule(
        @Param("riderId") Long riderId
        , @Param("dayOfWeek") Byte dayOfWeek
        , @Param("deliverySlot") DeliverySlotCode deliverySlot
    );
}