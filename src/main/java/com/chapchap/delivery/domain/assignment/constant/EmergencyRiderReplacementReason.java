package com.chapchap.delivery.domain.assignment.constant;

public enum EmergencyRiderReplacementReason {
    RIDER_ACCIDENT,
    RIDER_HEALTH_ISSUE,
    VEHICLE_ISSUE,
    URGENT_OPERATIONAL_CHANGE,
    OTHER;

    public boolean requiresDetail() {
        return this == OTHER;
    }
}
