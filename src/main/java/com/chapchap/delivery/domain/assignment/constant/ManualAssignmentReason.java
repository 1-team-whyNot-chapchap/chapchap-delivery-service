package com.chapchap.delivery.domain.assignment.constant;

public enum ManualAssignmentReason {
    AUTO_ASSIGNMENT_FAILED,
    LATE_ORDER,
    AREA_EXCEPTION,
    OPERATIONAL_ADJUSTMENT,
    OTHER;

    public boolean requiresDetail() {
        return this == OTHER;
    }
}
