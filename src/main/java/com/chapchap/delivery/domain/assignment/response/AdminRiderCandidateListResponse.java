package com.chapchap.delivery.domain.assignment.response;

import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import java.time.LocalDate;
import java.util.List;

public record AdminRiderCandidateListResponse(
    Long deliveryGroupId
    , LocalDate deliveryDate
    , DeliverySlotCode deliverySlot
    , List<Item> items
) {
    public record Item(
        Long riderId
        , Boolean isDeliveryActive
        , Boolean isEligible
        , int assignedStopCount
        , int assignedLunchboxQuantity
        , Boolean recommendedCapacityExceeded
        , Boolean maximumCapacityExceeded
        , Boolean isAreaMatched
    ) {
    }
}
