package com.chapchap.delivery.domain.delivery.constant;

public enum DeliveryResultCorrectionReason {
    DATA_ENTRY_ERROR
    , CUSTOMER_REPORT
    , OPERATIONAL_REVIEW
    , OTHER
    ;

    public boolean requiresDetail() {
        return this == OTHER;
    }
}
