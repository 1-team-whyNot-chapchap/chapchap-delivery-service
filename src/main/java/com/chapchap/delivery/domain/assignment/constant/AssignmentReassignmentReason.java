package com.chapchap.delivery.domain.assignment.constant;

public enum AssignmentReassignmentReason {
    RIDER_ISSUE,
    RIDER_UNAVAILABLE,
    ACKNOWLEDGEMENT_OVERDUE,
    OPERATIONAL_ADJUSTMENT,
    OTHER;

    public boolean requiresDetail() {
        return this == OTHER;
    }
}
