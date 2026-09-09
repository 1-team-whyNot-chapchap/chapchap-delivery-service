package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderScheduleExceptionConflictException extends BusinessException {
    public RiderScheduleExceptionConflictException() {
        super(
            ErrorCode.DELIVERY_STATE_CONFLICT
        );
    }
}