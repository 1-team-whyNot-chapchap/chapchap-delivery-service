package com.chapchap.delivery.domain.rider.repository;

import com.chapchap.delivery.domain.rider.entity.RiderScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;

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
}