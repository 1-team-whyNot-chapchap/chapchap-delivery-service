package com.chapchap.delivery.global.exception.technical;

import com.chapchap.delivery.global.exception.TechnicalException;

public class InvalidAuthUserEventException
    extends TechnicalException {

    public InvalidAuthUserEventException(String message) {
        super(message);
    }

    public InvalidAuthUserEventException(
        String message
        , Throwable cause
    ) {
        super(
            message
            , cause
        );
    }
}