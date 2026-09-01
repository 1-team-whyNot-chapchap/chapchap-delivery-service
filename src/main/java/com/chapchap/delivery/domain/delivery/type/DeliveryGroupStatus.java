package com.chapchap.delivery.domain.delivery.type;

public enum DeliveryGroupStatus {
    WAITING_ASSIGNMENT
    , WAITING_RIDER
    , ISSUE_REVIEW
    , READY_TO_CONFIRM
    , CONFIRMED
    , IN_PROGRESS
    , COMPLETED
    , COMPLETED_WITH_FAILURE
    , FAILED
}
