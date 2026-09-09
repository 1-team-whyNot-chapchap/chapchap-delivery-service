package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class InvalidDeliveryPhotoInfoException extends BusinessException {
    public InvalidDeliveryPhotoInfoException() {
        super(ErrorCode.INVALID_DELIVERY_PHOTO_INFO);
    }
}
