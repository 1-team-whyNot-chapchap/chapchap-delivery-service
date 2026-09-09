package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class InvalidRequestException extends BusinessException {
    public InvalidRequestException() {
        super(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.getMessage());
    }
}
