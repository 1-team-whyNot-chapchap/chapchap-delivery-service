package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderLeaveRequestStateConflictException extends BusinessException {
    public RiderLeaveRequestStateConflictException() { super(ErrorCode.RIDER_LEAVE_REQUEST_STATE_CONFLICT); }
}
