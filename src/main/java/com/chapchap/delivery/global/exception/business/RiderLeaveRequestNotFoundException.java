package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderLeaveRequestNotFoundException extends BusinessException {
    public RiderLeaveRequestNotFoundException() {
        super(ErrorCode.RIDER_LEAVE_REQUEST_NOT_FOUND);
    }
}
