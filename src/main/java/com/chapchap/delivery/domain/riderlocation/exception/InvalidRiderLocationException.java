package com.chapchap.delivery.domain.riderlocation.exception;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class InvalidRiderLocationException extends BusinessException {
    public InvalidRiderLocationException() { super(ErrorCode.INVALID_RIDER_LOCATION); }
}
