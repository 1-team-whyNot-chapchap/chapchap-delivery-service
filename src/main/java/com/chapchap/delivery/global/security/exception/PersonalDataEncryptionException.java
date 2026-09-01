package com.chapchap.delivery.global.security.exception;

public class PersonalDataEncryptionException extends RuntimeException {

    public PersonalDataEncryptionException(
        String message
        , Throwable cause
    ) {
        super(message, cause);
    }
}