package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentCapacity;
import lombok.Getter;

@Getter
class RiderAssignmentLoad {
    private int visitCount;
    private int lunchboxQuantity;

    RiderAssignmentLoad(
        int visitCount
        , int lunchboxQuantity
    ) {
        this.visitCount = visitCount;
        this.lunchboxQuantity = lunchboxQuantity;
    }

    public boolean canAssign(Integer additionalLunchboxQuantity) {
        return visitCount + 1 <= DeliveryAssignmentCapacity.MAX_VISIT_COUNT
            && lunchboxQuantity + additionalLunchboxQuantity <= DeliveryAssignmentCapacity.MAX_LUNCHBOX_QUANTITY;
    }

    public void add(Integer additionalLunchboxQuantity) {
        visitCount++;
        lunchboxQuantity += additionalLunchboxQuantity;
    }
}