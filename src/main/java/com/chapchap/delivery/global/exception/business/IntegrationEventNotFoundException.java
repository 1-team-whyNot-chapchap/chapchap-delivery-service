package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class IntegrationEventNotFoundException extends BusinessException {
    public IntegrationEventNotFoundException() {
        super(ErrorCode.INTEGRATION_EVENT_NOT_FOUND, ErrorCode.INTEGRATION_EVENT_NOT_FOUND.getMessage());
    }
}
