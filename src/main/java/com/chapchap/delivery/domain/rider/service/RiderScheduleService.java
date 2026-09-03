package com.chapchap.delivery.domain.rider.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.rider.constant.RiderScheduleSource;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.entity.RiderScheduleException;
import com.chapchap.delivery.domain.rider.entity.RiderWeeklySchedule;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.rider.repository.RiderScheduleExceptionRepository;
import com.chapchap.delivery.domain.rider.repository.RiderWeeklyScheduleRepository;
import com.chapchap.delivery.domain.rider.response.RiderScheduleItemResponse;
import com.chapchap.delivery.domain.rider.response.RiderScheduleResponse;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RiderScheduleService {

    private final RiderRepository riderRepository;
    private final RiderWeeklyScheduleRepository riderWeeklyScheduleRepository;
    private final RiderScheduleExceptionRepository riderScheduleExceptionRepository;
    private final DeliveryAccessService deliveryAccessService;

    public RiderScheduleService(
        RiderRepository riderRepository
        , RiderWeeklyScheduleRepository riderWeeklyScheduleRepository
        , RiderScheduleExceptionRepository riderScheduleExceptionRepository
        , DeliveryAccessService deliveryAccessService
    ) {
        this.riderRepository = riderRepository;
        this.riderWeeklyScheduleRepository = riderWeeklyScheduleRepository;
        this.riderScheduleExceptionRepository = riderScheduleExceptionRepository;
        this.deliveryAccessService = deliveryAccessService;
    }

    @Transactional(readOnly = true)
    public RiderScheduleResponse getMySchedules(
        Long actorId
        , UserRole actorRole
        , LocalDate dateFrom
        , LocalDate dateTo
    ) {
        deliveryAccessService.validateRiderAccess(
            actorId
            , actorRole
        );

        Rider rider =
            riderRepository.findByAuthUserIdAndDeletedAtIsNull(
                    actorId
                )
                .orElseThrow(
                    RiderNotFoundException::new
                );

        List<RiderWeeklySchedule> weeklySchedules =
            riderWeeklyScheduleRepository.findAllByRiderIdAndDeletedAtIsNull(
                rider.getId()
            );

        List<RiderScheduleException> scheduleExceptions =
            riderScheduleExceptionRepository
                .findAllByRiderIdAndScheduleDateBetweenAndDeletedAtIsNullOrderByScheduleDateAsc(
                    rider.getId()
                    , dateFrom
                    , dateTo
                );

        Map<WeeklyScheduleKey, RiderWeeklySchedule> weeklyScheduleMap =
            createWeeklyScheduleMap(
                weeklySchedules
            );

        Map<DateScheduleKey, RiderScheduleException> scheduleExceptionMap =
            createScheduleExceptionMap(
                scheduleExceptions
            );

        List<RiderScheduleItemResponse> schedules =
            createActualSchedules(
                dateFrom
                , dateTo
                , weeklyScheduleMap
                , scheduleExceptionMap
            );

        return new RiderScheduleResponse(
            rider.getId()
            , rider.getIsDeliveryActive()
            , schedules
        );
    }

    private Map<WeeklyScheduleKey, RiderWeeklySchedule> createWeeklyScheduleMap(
        List<RiderWeeklySchedule> weeklySchedules
    ) {
        Map<WeeklyScheduleKey, RiderWeeklySchedule> result =
            new HashMap<>();

        for (RiderWeeklySchedule weeklySchedule : weeklySchedules) {
            result.put(
                new WeeklyScheduleKey(
                    weeklySchedule.getDayOfWeek()
                    , weeklySchedule.getSlot().getCode()
                )
                , weeklySchedule
            );
        }

        return result;
    }

    private Map<DateScheduleKey, RiderScheduleException> createScheduleExceptionMap(
        List<RiderScheduleException> scheduleExceptions
    ) {
        Map<DateScheduleKey, RiderScheduleException> result =
            new HashMap<>();

        for (RiderScheduleException scheduleException : scheduleExceptions) {
            result.put(
                new DateScheduleKey(
                    scheduleException.getScheduleDate()
                    , scheduleException.getSlot().getCode()
                )
                , scheduleException
            );
        }

        return result;
    }

    private List<RiderScheduleItemResponse> createActualSchedules(
        LocalDate dateFrom
        , LocalDate dateTo
        , Map<WeeklyScheduleKey, RiderWeeklySchedule> weeklyScheduleMap
        , Map<DateScheduleKey, RiderScheduleException> scheduleExceptionMap
    ) {
        List<RiderScheduleItemResponse> schedules =
            new ArrayList<>();

        for (
            LocalDate date = dateFrom;
            !date.isAfter(dateTo);
            date = date.plusDays(1)
        ) {
            for (DeliverySlotCode deliverySlot : DeliverySlotCode.values()) {
                DateScheduleKey dateScheduleKey =
                    new DateScheduleKey(
                        date
                        , deliverySlot
                    );

                RiderScheduleException scheduleException =
                    scheduleExceptionMap.get(
                        dateScheduleKey
                    );

                if (scheduleException != null) {
                    schedules.add(
                        new RiderScheduleItemResponse(
                            date
                            , deliverySlot
                            , scheduleException.getIsWorking()
                            , RiderScheduleSource.DATE_EXCEPTION
                        )
                    );

                    continue;
                }

                WeeklyScheduleKey weeklyScheduleKey =
                    new WeeklyScheduleKey(
                        (byte) date.getDayOfWeek().getValue()
                        , deliverySlot
                    );

                if (weeklyScheduleMap.containsKey(weeklyScheduleKey)) {
                    schedules.add(
                        new RiderScheduleItemResponse(
                            date
                            , deliverySlot
                            , true
                            , RiderScheduleSource.WEEKLY_DEFAULT
                        )
                    );
                }
            }
        }

        return schedules;
    }

    private record WeeklyScheduleKey(
        Byte dayOfWeek
        , DeliverySlotCode deliverySlot
    ) {
    }

    private record DateScheduleKey(
        LocalDate date
        , DeliverySlotCode deliverySlot
    ) {
    }
}