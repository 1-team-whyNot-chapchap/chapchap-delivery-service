package com.chapchap.delivery.global.exception.technical;

import com.chapchap.delivery.global.exception.TechnicalException;

public class PersonalDataEncryptionException
    extends TechnicalException {

    public PersonalDataEncryptionException(String message) {
        super(message);
    }

    public PersonalDataEncryptionException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}