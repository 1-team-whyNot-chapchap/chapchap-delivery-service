package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class DeliveryCapacityExceededException extends BusinessException {
    public DeliveryCapacityExceededException() {
        super(
            ErrorCode.DELIVERY_CAPACITY_EXCEEDED
            , ErrorCode.DELIVERY_CAPACITY_EXCEEDED.getMessage()
        );
    }
}