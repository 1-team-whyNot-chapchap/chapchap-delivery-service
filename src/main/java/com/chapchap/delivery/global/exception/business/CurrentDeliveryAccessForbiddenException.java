package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class CurrentDeliveryAccessForbiddenException extends BusinessException {
    public CurrentDeliveryAccessForbiddenException() {
        super(ErrorCode.CURRENT_DELIVERY_ACCESS_FORBIDDEN);
    }
}
