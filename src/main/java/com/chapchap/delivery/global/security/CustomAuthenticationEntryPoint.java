package com.chapchap.delivery.global.security;

import com.chapchap.delivery.global.exception.ErrorCode;
import com.chapchap.delivery.global.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint
    implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
        HttpServletRequest request
        , HttpServletResponse response
        , AuthenticationException exception
    ) throws IOException, ServletException {

        ErrorCode errorCode =
            ErrorCode.AUTHENTICATION_REQUIRED;

        response.setStatus(
            errorCode
                .getHttpStatus()
                .value()
        );

        response.setContentType(
            MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding(
            StandardCharsets.UTF_8.name()
        );

        objectMapper.writeValue(
            response.getWriter()
            , ApiResponse.error(
                errorCode.getCode()
                , errorCode.getMessage()
            )
        );
    }
}