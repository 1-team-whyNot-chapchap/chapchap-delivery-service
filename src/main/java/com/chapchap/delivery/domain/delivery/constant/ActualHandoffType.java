package com.chapchap.delivery.domain.delivery.constant;

public enum ActualHandoffType {
    DIRECT
    , DOORSTEP
    , OTHER
    ;

    public boolean requiresStorageLocation() {
        return this == DOORSTEP
            || this == OTHER;
    }

    public boolean requiresPhoto() {
        return this == DOORSTEP
            || this == OTHER;
    }
}