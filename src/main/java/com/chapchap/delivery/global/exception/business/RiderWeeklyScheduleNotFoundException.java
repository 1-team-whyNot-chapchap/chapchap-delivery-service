package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderWeeklyScheduleNotFoundException extends BusinessException {
    public RiderWeeklyScheduleNotFoundException() {
        super(
            ErrorCode.RESOURCE_NOT_FOUND
        );
    }
}