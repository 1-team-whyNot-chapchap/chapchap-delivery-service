package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class DeliveryAssignmentNotFoundException extends BusinessException {
    public DeliveryAssignmentNotFoundException() {
        super(
            ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND
            , ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND.getMessage()
        );
    }
}
