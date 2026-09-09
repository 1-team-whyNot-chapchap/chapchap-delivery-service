package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderLeaveManagedScheduleExceptionException extends BusinessException {
    public RiderLeaveManagedScheduleExceptionException() { super(ErrorCode.RIDER_LEAVE_MANAGED_SCHEDULE_EXCEPTION); }
}
