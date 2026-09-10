package com.chapchap.delivery.domain.rider.response;

import com.chapchap.delivery.domain.rider.entity.Rider;

public record RiderDetailResponse(
    Long riderId
    , Boolean isDeliveryActive
    , Long version
) {

    public static RiderDetailResponse from(
        Rider rider
    ) {
        return new RiderDetailResponse(
            rider.getId()
            , rider.getIsDeliveryActive()
            , rider.getVersion()
        );
    }
}
