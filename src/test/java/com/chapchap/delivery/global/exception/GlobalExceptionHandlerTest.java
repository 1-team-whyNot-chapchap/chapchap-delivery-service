package com.chapchap.delivery.global.exception;

import com.chapchap.delivery.global.exception.business.DeliveryRegistrationException;
import com.chapchap.delivery.global.exception.technical.PersonalDataEncryptionException;
import com.chapchap.delivery.global.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
        new GlobalExceptionHandler();

    @Test
    @DisplayName("BusinessException 발생 시 해당 업무 오류 코드를 반환한다")
    void handleBusinessException() {
        DeliveryRegistrationException exception =
            new DeliveryRegistrationException(
                "orderId 형식이 올바르지 않습니다."
            );

        ResponseEntity<ApiResponse<Void>> response =
            handler.handleBusinessException(exception);

        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(response.getBody())
            .isNotNull();

        assertThat(response.getBody().code())
            .isEqualTo("DELIVERY_003");

        assertThat(response.getBody().message())
            .isEqualTo("orderId 형식이 올바르지 않습니다.");

        assertThat(response.getBody().data())
            .isNull();
    }

    @Test
    @DisplayName("TechnicalException 발생 시 내부 메시지를 숨기고 공통 서버 오류를 반환한다")
    void handleTechnicalException() {
        PersonalDataEncryptionException exception =
            new PersonalDataEncryptionException(
                "AES 암호화 처리 중 내부 오류가 발생했습니다."
            );

        ResponseEntity<ApiResponse<Void>> response =
            handler.handleTechnicalException(exception);

        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(response.getBody())
            .isNotNull();

        assertThat(response.getBody().code())
            .isEqualTo("COMMON_002");

        assertThat(response.getBody().message())
            .isEqualTo("서버 내부 오류가 발생했습니다.");

        assertThat(response.getBody().data())
            .isNull();
    }

    @Test
    @DisplayName("예상하지 못한 예외 발생 시 공통 서버 오류를 반환한다")
    void handleException() {
        Exception exception =
            new RuntimeException("내부 구현 오류");

        ResponseEntity<ApiResponse<Void>> response =
            handler.handleException(exception);

        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(response.getBody())
            .isNotNull();

        assertThat(response.getBody().code())
            .isEqualTo("COMMON_002");

        assertThat(response.getBody().message())
            .isEqualTo("서버 내부 오류가 발생했습니다.");

        assertThat(response.getBody().data())
            .isNull();
    }
}