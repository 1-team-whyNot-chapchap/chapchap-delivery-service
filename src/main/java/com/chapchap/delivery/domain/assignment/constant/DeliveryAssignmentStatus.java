package com.chapchap.delivery.domain.assignment.constant;

public enum DeliveryAssignmentStatus {
    ASSIGNED
    , ACKNOWLEDGED
    , ISSUE_REPORTED
    , CONFIRMED
    , REASSIGNED
    ;

    public boolean isActive() {
        return this != REASSIGNED;
    }
}