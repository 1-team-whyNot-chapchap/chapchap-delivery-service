package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class OtherReasonDetailRequiredException extends BusinessException {
    public OtherReasonDetailRequiredException() {
        super(
            ErrorCode.OTHER_REASON_DETAIL_REQUIRED
            , ErrorCode.OTHER_REASON_DETAIL_REQUIRED.getMessage()
        );
    }
}