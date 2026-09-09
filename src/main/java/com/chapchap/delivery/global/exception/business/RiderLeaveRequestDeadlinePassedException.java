package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderLeaveRequestDeadlinePassedException extends BusinessException {
    public RiderLeaveRequestDeadlinePassedException() { super(ErrorCode.RIDER_LEAVE_REQUEST_DEADLINE_PASSED); }
}
