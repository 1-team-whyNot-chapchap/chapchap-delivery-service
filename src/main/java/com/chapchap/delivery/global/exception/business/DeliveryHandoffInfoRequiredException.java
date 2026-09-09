package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class DeliveryHandoffInfoRequiredException extends BusinessException {
    public DeliveryHandoffInfoRequiredException() {
        super(ErrorCode.DELIVERY_HANDOFF_INFO_REQUIRED);
    }
}
