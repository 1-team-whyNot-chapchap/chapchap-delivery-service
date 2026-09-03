package com.chapchap.delivery.global.security;

import com.chapchap.delivery.domain.access.constant.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAuthenticationFilterTest {

    private final GatewayAuthenticationFilter filter =
        new GatewayAuthenticationFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Gateway 사용자 ID와 ADMIN 역할이 정상이면 인증 정보를 생성한다")
    void authenticateAdminSuccess()
        throws Exception {

        MockHttpServletRequest request =
            new MockHttpServletRequest();

        request.addHeader(
            GatewayAuthenticationFilter.USER_ID_HEADER
            , "9001"
        );

        request.addHeader(
            GatewayAuthenticationFilter.USER_ROLE_HEADER
            , "ADMIN"
        );

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        MockFilterChain filterChain =
            new MockFilterChain();

        filter.doFilter(
            request
            , response
            , filterChain
        );

        Authentication authentication =
            SecurityContextHolder
                .getContext()
                .getAuthentication();

        assertThat(authentication)
            .isNotNull();

        assertThat(authentication.isAuthenticated())
            .isTrue();

        assertThat(authentication.getPrincipal())
            .isInstanceOfSatisfying(
                AuthenticatedUser.class
                , authenticatedUser -> {
                    assertThat(authenticatedUser.userId())
                        .isEqualTo(9001L);

                    assertThat(authenticatedUser.role())
                        .isEqualTo(UserRole.ADMIN);
                }
            );

        assertThat(authentication.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("인증 헤더가 없으면 인증 정보를 생성하지 않는다")
    void noAuthenticationHeaders() throws Exception {
        MockHttpServletRequest request =
            new MockHttpServletRequest();

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        MockFilterChain filterChain =
            new MockFilterChain();

        filter.doFilter(
            request
            , response
            , filterChain
        );

        Authentication authentication =
            SecurityContextHolder
                .getContext()
                .getAuthentication();

        assertThat(authentication)
            .isNull();
    }

    @Test
    @DisplayName("사용자 ID 헤더만 있으면 인증 정보를 생성하지 않는다")
    void roleHeaderMissing() throws Exception {
        MockHttpServletRequest request =
            new MockHttpServletRequest();

        request.addHeader(
            GatewayAuthenticationFilter.USER_ID_HEADER
            , "9001"
        );

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        MockFilterChain filterChain =
            new MockFilterChain();

        filter.doFilter(
            request
            , response
            , filterChain
        );

        assertThat(
            SecurityContextHolder
                .getContext()
                .getAuthentication()
        ).isNull();
    }

    @Test
    @DisplayName("사용자 ID가 양의 정수가 아니면 인증 정보를 생성하지 않는다")
    void invalidUserId() throws Exception {
        MockHttpServletRequest request =
            new MockHttpServletRequest();

        request.addHeader(
            GatewayAuthenticationFilter.USER_ID_HEADER
            , "invalid"
        );

        request.addHeader(
            GatewayAuthenticationFilter.USER_ROLE_HEADER
            , "ADMIN"
        );

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        MockFilterChain filterChain =
            new MockFilterChain();

        filter.doFilter(
            request
            , response
            , filterChain
        );

        assertThat(
            SecurityContextHolder
                .getContext()
                .getAuthentication()
        ).isNull();
    }

    @Test
    @DisplayName("지원하지 않는 역할이면 인증 정보를 생성하지 않는다")
    void invalidRole() throws Exception {
        MockHttpServletRequest request =
            new MockHttpServletRequest();

        request.addHeader(
            GatewayAuthenticationFilter.USER_ID_HEADER
            , "9001"
        );

        request.addHeader(
            GatewayAuthenticationFilter.USER_ROLE_HEADER
            , "OWNER"
        );

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        MockFilterChain filterChain =
            new MockFilterChain();

        filter.doFilter(
            request
            , response
            , filterChain
        );

        assertThat(
            SecurityContextHolder
                .getContext()
                .getAuthentication()
        ).isNull();
    }

    @Test
    @DisplayName("사용자 ID가 0 이하이면 인증 정보를 생성하지 않는다")
    void nonPositiveUserId() throws Exception {
        MockHttpServletRequest request =
            new MockHttpServletRequest();

        request.addHeader(
            GatewayAuthenticationFilter.USER_ID_HEADER
            , "0"
        );

        request.addHeader(
            GatewayAuthenticationFilter.USER_ROLE_HEADER
            , "ADMIN"
        );

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        MockFilterChain filterChain =
            new MockFilterChain();

        filter.doFilter(
            request
            , response
            , filterChain
        );

        assertThat(
            SecurityContextHolder
                .getContext()
                .getAuthentication()
        ).isNull();
    }
}