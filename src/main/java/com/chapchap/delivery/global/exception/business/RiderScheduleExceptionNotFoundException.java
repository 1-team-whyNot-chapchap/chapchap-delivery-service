package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderScheduleExceptionNotFoundException extends BusinessException {
    public RiderScheduleExceptionNotFoundException() {
        super(
            ErrorCode.RESOURCE_NOT_FOUND
        );
    }
}