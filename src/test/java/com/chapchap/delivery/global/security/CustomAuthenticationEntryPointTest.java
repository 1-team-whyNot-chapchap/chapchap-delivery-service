package com.chapchap.delivery.global.security;

import com.chapchap.delivery.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper =
        JsonMapper
            .builder()
            .build();

    private final CustomAuthenticationEntryPoint authenticationEntryPoint =
        new CustomAuthenticationEntryPoint(
            objectMapper
        );

    @Test
    @DisplayName("인증 정보가 없으면 401과 인증 필요 응답을 반환한다")
    void commence() throws Exception {
        // given
        MockHttpServletRequest request =
            new MockHttpServletRequest();

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        AuthenticationCredentialsNotFoundException exception =
            new AuthenticationCredentialsNotFoundException(
                "Authentication required"
            );

        // when
        authenticationEntryPoint.commence(
            request
            , response
            , exception
        );

        // then
        assertThat(response.getStatus())
            .isEqualTo(
                ErrorCode.AUTHENTICATION_REQUIRED
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
                    + ErrorCode.AUTHENTICATION_REQUIRED.getCode()
                    + "\""
            )
            .contains(
                "\"message\":\""
                    + ErrorCode.AUTHENTICATION_REQUIRED.getMessage()
                    + "\""
            )
            .contains(
                "\"data\":null"
            );
    }
}