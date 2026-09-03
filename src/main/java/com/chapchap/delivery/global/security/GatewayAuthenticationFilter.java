package com.chapchap.delivery.global.security;

import com.chapchap.delivery.domain.access.constant.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class GatewayAuthenticationFilter
    extends OncePerRequestFilter {

    public static final String USER_ID_HEADER =
        "X-User-Id";

    public static final String USER_ROLE_HEADER =
        "X-User-Role";

    private static final String ROLE_PREFIX =
        "ROLE_";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request
        , @NonNull HttpServletResponse response
        , @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String userIdHeader =
            request.getHeader(USER_ID_HEADER);

        String userRoleHeader =
            request.getHeader(USER_ROLE_HEADER);

        if (
            !StringUtils.hasText(userIdHeader)
                || !StringUtils.hasText(userRoleHeader)
        ) {
            filterChain.doFilter(
                request
                , response
            );

            return;
        }

        AuthenticatedUser authenticatedUser =
            createAuthenticatedUser(
                userIdHeader
                , userRoleHeader
            );

        if (authenticatedUser == null) {
            filterChain.doFilter(
                request
                , response
            );

            return;
        }

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                authenticatedUser
                , null
                , List.of(
                new SimpleGrantedAuthority(
                    ROLE_PREFIX
                        + authenticatedUser.role().name()
                )
            )
            );

        SecurityContext securityContext =
            SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            authentication
        );

        SecurityContextHolder.setContext(
            securityContext
        );

        filterChain.doFilter(
            request
            , response
        );
    }

    private AuthenticatedUser createAuthenticatedUser(
        String userIdHeader
        , String userRoleHeader
    ) {
        try {
            long userId =
                Long.parseLong(
                    userIdHeader.trim()
                );

            if (userId <= 0) {
                return null;
            }

            UserRole role =
                UserRole.valueOf(
                    userRoleHeader.trim()
                );

            return new AuthenticatedUser(
                userId
                , role
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}