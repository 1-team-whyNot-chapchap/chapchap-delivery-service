package com.chapchap.delivery.domain.riderlocation.exception;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderLocationNotAvailableException extends BusinessException {
    public RiderLocationNotAvailableException() { super(ErrorCode.RIDER_LOCATION_NOT_AVAILABLE); }
}
