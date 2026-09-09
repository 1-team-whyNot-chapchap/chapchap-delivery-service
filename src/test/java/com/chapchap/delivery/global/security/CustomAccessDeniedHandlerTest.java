package com.chapchap.delivery.global.security;

import com.chapchap.delivery.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper =
        JsonMapper
            .builder()
            .build();

    private final CustomAccessDeniedHandler accessDeniedHandler =
        new CustomAccessDeniedHandler(
            objectMapper
        );

    @Test
    @DisplayName("인증됐지만 접근 권한이 없으면 403과 접근 거부 응답을 반환한다")
    void handle() throws Exception {
        // given
        MockHttpServletRequest request =
            new MockHttpServletRequest();

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        AccessDeniedException exception =
            new AccessDeniedException(
                "Access denied"
            );

        // when
        accessDeniedHandler.handle(
            request
            , response
            , exception
        );

        // then
        assertThat(response.getStatus())
            .isEqualTo(
                ErrorCode.DELIVERY_FORBIDDEN
                    .getHttpStatus()
                    .value()
            );

        assertThat(response.getContentType())
            .startsWith(
                "application/json"
            );

        assertThat(response.getCharacterEncoding())
            .isEqualTo(
                "UTF-8"
            );

        assertThat(response.getContentAsString())
            .contains(
                "\"code\":\""
                    + ErrorCode.DELIVERY_FORBIDDEN.getCode()
                    + "\""
            )
            .contains(
                "\"message\":\""
                    + ErrorCode.DELIVERY_FORBIDDEN.getMessage()
                    + "\""
            )
            .contains(
                "\"data\":null"
            );
    }
}