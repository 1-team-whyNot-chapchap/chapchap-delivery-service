package com.chapchap.delivery.domain.delivery.constant;

public enum AdminDeliveryFailureReason {
    RIDER_REPORT_CONFIRMED,
    CUSTOMER_REPORT_CONFIRMED,
    OPERATIONAL_REVIEW,
    SYSTEM_RECOVERY,
    OTHER;

    public boolean requiresDetail() {
        return this == OTHER;
    }
}
