package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderLeaveReasonDetailRequiredException extends BusinessException {
    public RiderLeaveReasonDetailRequiredException() { super(ErrorCode.RIDER_LEAVE_REASON_DETAIL_REQUIRED); }
}
