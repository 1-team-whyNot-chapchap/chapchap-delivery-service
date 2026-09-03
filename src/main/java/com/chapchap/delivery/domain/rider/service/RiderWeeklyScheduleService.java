package com.chapchap.delivery.domain.rider.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliverySlotRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.entity.RiderWeeklySchedule;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.rider.repository.RiderWeeklyScheduleRepository;
import com.chapchap.delivery.domain.rider.request.RiderWeeklyScheduleCreateRequest;
import com.chapchap.delivery.domain.rider.response.RiderWeeklyScheduleResponse;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import com.chapchap.delivery.global.exception.business.RiderWeeklyScheduleNotFoundException;
import com.chapchap.delivery.global.exception.technical.DeliverySlotConfigurationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RiderWeeklyScheduleService {
    private static final ZoneId KST =
        ZoneId.of("Asia/Seoul");

    private final RiderRepository riderRepository;
    private final RiderWeeklyScheduleRepository riderWeeklyScheduleRepository;
    private final DeliverySlotRepository deliverySlotRepository;

    private final DeliveryAccessService deliveryAccessService;

    @Transactional
    public RiderWeeklyScheduleResponse createWeeklySchedule(
        Long riderId
        , Long actorId
        , UserRole actorRole
        , RiderWeeklyScheduleCreateRequest request
    ) {
        deliveryAccessService.validateAdminAccess(
            actorId
            , actorRole
        );

        Rider rider =
            riderRepository
                .findByIdAndDeletedAtIsNull(riderId)
                .orElseThrow(RiderNotFoundException::new);

        DeliverySlot deliverySlot =
            deliverySlotRepository
                .findByCodeAndDeletedAtIsNull(
                    request.deliverySlot()
                )
                .orElseThrow(
                    () -> new DeliverySlotConfigurationException(
                        request.deliverySlot()
                    )
                );

        Optional<RiderWeeklySchedule> existingSchedule =
            riderWeeklyScheduleRepository
                .findByRiderIdAndDayOfWeekAndSlotId(
                    riderId
                    , request.dayOfWeek()
                    , deliverySlot.getId()
                );

        if (existingSchedule.isPresent()) {
            RiderWeeklySchedule schedule =
                existingSchedule.get();

            if (schedule.getDeletedAt() != null) {
                schedule.restore();
            }

            return RiderWeeklyScheduleResponse.from(
                schedule
            );
        }

        RiderWeeklySchedule schedule =
            new RiderWeeklySchedule(
                rider
                , request.dayOfWeek()
                , deliverySlot
            );

        RiderWeeklySchedule savedSchedule =
            riderWeeklyScheduleRepository.save(
                schedule
            );

        return RiderWeeklyScheduleResponse.from(
            savedSchedule
        );
    }

    @Transactional(readOnly = true)
    public List<RiderWeeklyScheduleResponse> getWeeklySchedules(
        Long riderId
        , Long actorId
        , UserRole actorRole
    ) {
        deliveryAccessService.validateAdminAccess(
            actorId
            , actorRole
        );

        riderRepository
            .findByIdAndDeletedAtIsNull(riderId)
            .orElseThrow(RiderNotFoundException::new);

        return riderWeeklyScheduleRepository
            .findAllByRiderIdAndDeletedAtIsNull(riderId)
            .stream()
            .map(RiderWeeklyScheduleResponse::from)
            .toList();
    }

    @Transactional
    public void deleteWeeklySchedule(
        Long riderId
        , Long scheduleId
        , Long actorId
        , UserRole actorRole
    ) {
        deliveryAccessService.validateAdminAccess(
            actorId
            , actorRole
        );

        riderRepository
            .findByIdAndDeletedAtIsNull(riderId)
            .orElseThrow(RiderNotFoundException::new);

        RiderWeeklySchedule schedule =
            riderWeeklyScheduleRepository
                .findByIdAndRiderIdAndDeletedAtIsNull(
                    scheduleId
                    , riderId
                )
                .orElseThrow(
                    RiderWeeklyScheduleNotFoundException::new
                );

        schedule.delete(
            LocalDateTime.now(KST)
        );
    }
}