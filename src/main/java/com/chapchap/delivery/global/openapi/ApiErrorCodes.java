package com.chapchap.delivery.global.openapi;

import com.chapchap.delivery.global.exception.ErrorCode;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodes {
    ErrorCode[] value() default {};
}
