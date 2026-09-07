package com.chapchap.delivery.global.security;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.global.config.InternalApiProperties;
import com.chapchap.delivery.global.exception.ErrorCode;
import com.chapchap.delivery.global.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {
    static final String SERVICE_HEADER = "X-Internal-Service";
    static final String API_KEY_HEADER = "X-Internal-Api-Key";
    static final String SCOPE_HEADER = "X-Internal-Scope";
    static final String CUSTOMER_SERVICE = "customer-service";
    static final String CURRENT_DELIVERY_SCOPE = "delivery.current.read";

    private final InternalApiProperties properties;
    private final ObjectMapper objectMapper;

    public InternalServiceAuthenticationFilter(
        InternalApiProperties properties, ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request
        , @NonNull HttpServletResponse response
        , @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!CUSTOMER_SERVICE.equals(request.getHeader(SERVICE_HEADER))
            || !validApiKey(request.getHeader(API_KEY_HEADER))) {
            writeError(response, ErrorCode.INTERNAL_SERVICE_AUTHENTICATION_FAILED);
            return;
        }
        if (!CURRENT_DELIVERY_SCOPE.equals(request.getHeader(SCOPE_HEADER))) {
            writeError(response, ErrorCode.CURRENT_DELIVERY_ACCESS_FORBIDDEN);
            return;
        }

        Long userId = parseUserId(request.getHeader(GatewayAuthenticationFilter.USER_ID_HEADER));
        if (userId == null) {
            writeError(response, ErrorCode.INVALID_INTERNAL_SUBJECT);
            return;
        }
        if (!UserRole.CUSTOMER.name().equals(
            request.getHeader(GatewayAuthenticationFilter.USER_ROLE_HEADER)
        )) {
            writeError(response, ErrorCode.CURRENT_DELIVERY_ACCESS_FORBIDDEN);
            return;
        }

        AuthenticatedUser principal = new AuthenticatedUser(userId, UserRole.CUSTOMER);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
            );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        filterChain.doFilter(request, response);
    }

    private boolean validApiKey(String supplied) {
        String expected = properties.apiKey();
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(supplied)) {
            return false;
        }
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8)
            , supplied.getBytes(StandardCharsets.UTF_8)
        );
    }

    private Long parseUserId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            long userId = Long.parseLong(value);
            return userId > 0 ? userId : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode)
        throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
            response.getWriter()
            , ApiResponse.error(errorCode.getCode(), errorCode.getMessage())
        );
    }
}
