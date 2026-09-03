package com.chapchap.delivery.domain.rider.repository;

import com.chapchap.delivery.domain.rider.entity.RiderWeeklySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

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
}