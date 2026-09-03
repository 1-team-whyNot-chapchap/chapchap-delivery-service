package com.chapchap.delivery.domain.rider.response;

import com.chapchap.delivery.domain.rider.entity.RiderDeliveryArea;

import java.time.LocalDate;

public record RiderDeliveryAreaResponse(
    Long riderDeliveryAreaId
    , Long riderId
    , String deliveryAreaCode
    , LocalDate effectiveFrom
    , LocalDate effectiveTo
    , Boolean isActive
) {

    public static RiderDeliveryAreaResponse from(
        RiderDeliveryArea riderDeliveryArea
    ) {
        return new RiderDeliveryAreaResponse(
            riderDeliveryArea.getId()
            , riderDeliveryArea.getRider().getId()
            , riderDeliveryArea.getDeliveryAreaCode()
            , riderDeliveryArea.getEffectiveFrom()
            , riderDeliveryArea.getEffectiveTo()
            , riderDeliveryArea.getIsActive()
        );
    }
}