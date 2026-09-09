package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderLeaveScheduleConflictException extends BusinessException {
    public RiderLeaveScheduleConflictException() { super(ErrorCode.RIDER_LEAVE_SCHEDULE_CONFLICT); }
}
