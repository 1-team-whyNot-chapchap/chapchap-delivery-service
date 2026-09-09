package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.service.RiderDeliveryAreaService;
import com.chapchap.delivery.domain.rider.service.RiderScheduleService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class RiderAssignmentEligibilityService {
    private final DeliveryAccessService deliveryAccessService;
    private final RiderScheduleService riderScheduleService;
    private final RiderDeliveryAreaService riderDeliveryAreaService;

    public RiderAssignmentEligibilityService(
        DeliveryAccessService deliveryAccessService
        , RiderScheduleService riderScheduleService
        , RiderDeliveryAreaService riderDeliveryAreaService
    ) {
        this.deliveryAccessService = deliveryAccessService;
        this.riderScheduleService = riderScheduleService;
        this.riderDeliveryAreaService = riderDeliveryAreaService;
    }

    public boolean isEligible(
        Rider rider
        , LocalDate deliveryDate
        , DeliverySlotCode deliverySlot
        , String deliveryAreaCode
    ) {
        if (!Boolean.TRUE.equals(rider.getIsDeliveryActive())) {
            return false;
        }

        if (!deliveryAccessService.isRiderAccessAllowed(rider.getAuthUserId())) {
            return false;
        }

        if (!riderScheduleService.isWorking(
            rider.getId()
            , deliveryDate
            , deliverySlot
        )) {
            return false;
        }

        return riderDeliveryAreaService.canDeliverToArea(
            rider.getId()
            , deliveryAreaCode
            , deliveryDate
        );
    }

    public boolean isEligibleIgnoringArea(
        Rider rider
        , LocalDate deliveryDate
        , DeliverySlotCode deliverySlot
    ) {
        if (!Boolean.TRUE.equals(rider.getIsDeliveryActive())) {
            return false;
        }

        if (!deliveryAccessService.isRiderAccessAllowed(rider.getAuthUserId())) {
            return false;
        }

        return riderScheduleService.isWorking(
            rider.getId()
            , deliveryDate
            , deliverySlot
        );
    }
}
