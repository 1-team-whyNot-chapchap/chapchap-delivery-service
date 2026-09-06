package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class IntegrationEventNotRepublishableException extends BusinessException {
    public IntegrationEventNotRepublishableException() {
        super(
            ErrorCode.INTEGRATION_EVENT_NOT_REPUBLISHABLE,
            ErrorCode.INTEGRATION_EVENT_NOT_REPUBLISHABLE.getMessage()
        );
    }
}
