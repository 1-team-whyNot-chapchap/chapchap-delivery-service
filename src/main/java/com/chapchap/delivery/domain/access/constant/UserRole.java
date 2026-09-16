package com.chapchap.delivery.domain.access.constant;

public enum UserRole {
    CUSTOMER
    , RIDER
    , ADMIN
    , SUPER_ADMIN;

    public boolean isAdministrator() {
        return this == ADMIN || this == SUPER_ADMIN;
    }
}
