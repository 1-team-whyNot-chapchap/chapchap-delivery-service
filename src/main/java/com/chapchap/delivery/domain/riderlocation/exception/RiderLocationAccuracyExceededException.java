package com.chapchap.delivery.domain.riderlocation.exception;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderLocationAccuracyExceededException extends BusinessException {
    public RiderLocationAccuracyExceededException() { super(ErrorCode.RIDER_LOCATION_ACCURACY_EXCEEDED); }
}
