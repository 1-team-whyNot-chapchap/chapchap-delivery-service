package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderNotFoundException extends BusinessException {
    public RiderNotFoundException() {
        super(
            ErrorCode.RIDER_NOT_FOUND
            , ErrorCode.RIDER_NOT_FOUND.getMessage()
        );
    }
}