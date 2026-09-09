package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderLeaveRequestOverlapException extends BusinessException {
    public RiderLeaveRequestOverlapException() { super(ErrorCode.RIDER_LEAVE_REQUEST_OVERLAP); }
}
