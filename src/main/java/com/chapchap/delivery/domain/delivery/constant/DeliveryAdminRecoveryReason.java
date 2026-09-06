package com.chapchap.delivery.domain.delivery.constant;

public enum DeliveryAdminRecoveryReason {
    DEVICE_FAILURE
    , NETWORK_FAILURE
    , APP_FAILURE
    , SERVER_FAILURE
    , OTHER
    ;

    public boolean requiresDetail() {
        return this == OTHER;
    }
}
