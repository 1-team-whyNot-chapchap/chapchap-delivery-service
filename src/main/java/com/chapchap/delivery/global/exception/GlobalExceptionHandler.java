package com.chapchap.delivery.global.exception;

import com.chapchap.delivery.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
        BusinessException exception
    ) {
        log.debug(
            "Business exception: code={}, message={}"
            , exception.getErrorCode().getCode()
            , exception.getMessage()
        );

        return createErrorResponse(
            exception.getErrorCode()
            , exception.getMessage()
        );
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleObjectOptimisticLockingFailureException(
        ObjectOptimisticLockingFailureException exception
    ) {
        log.debug(
            "Optimistic lock conflict occurred"
            , exception
        );

        ErrorCode errorCode =
            ErrorCode.OPTIMISTIC_LOCK_CONFLICT;

        return createErrorResponse(
            errorCode
            , errorCode.getMessage()
        );
    }

    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ApiResponse<Void>> handleTechnicalException(
        TechnicalException exception
    ) {
        log.error(
            "Technical exception occurred"
            , exception
        );

        ErrorCode errorCode =
            ErrorCode.INTERNAL_SERVER_ERROR;

        return createErrorResponse(
            errorCode
            , errorCode.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException exception
    ) {
        return createInvalidRequestResponse();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException exception
    ) {
        return createInvalidRequestResponse();
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException exception
    ) {
        return createInvalidRequestResponse();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
        MissingServletRequestParameterException exception
    ) {
        return createInvalidRequestResponse();
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
        NoResourceFoundException exception
    ) {
        ErrorCode errorCode =
            ErrorCode.RESOURCE_NOT_FOUND;

        return createErrorResponse(
            errorCode
            , errorCode.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
        Exception exception
    ) {
        log.error(
            "Unexpected exception occurred"
            , exception
        );

        ErrorCode errorCode =
            ErrorCode.INTERNAL_SERVER_ERROR;

        return createErrorResponse(
            errorCode
            , errorCode.getMessage()
        );
    }

    private ResponseEntity<ApiResponse<Void>> createInvalidRequestResponse() {
        ErrorCode errorCode =
            ErrorCode.INVALID_REQUEST;

        return createErrorResponse(
            errorCode
            , errorCode.getMessage()
        );
    }

    private ResponseEntity<ApiResponse<Void>> createErrorResponse(
        ErrorCode errorCode
        , String message
    ) {
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(
                ApiResponse.error(
                    errorCode.getCode()
                    , message
                )
            );
    }
}