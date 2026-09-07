package com.chapchap.delivery.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.global.config.InternalApiProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

class InternalServiceAuthenticationFilterTest {
    private static final String API_KEY = "internal-test-key";
    private final InternalServiceAuthenticationFilter filter =
        new InternalServiceAuthenticationFilter(
            new InternalApiProperties(API_KEY), new ObjectMapper()
        );

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("정상 내부 헤더는 CUSTOMER subject 인증을 생성한다")
    void authenticatesValidCurrentDeliveryRequest() throws Exception {
        MockHttpServletRequest request = validRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOfSatisfying(
            AuthenticatedUser.class, principal -> {
                assertThat(principal.userId()).isEqualTo(25L);
                assertThat(principal.role()).isEqualTo(UserRole.CUSTOMER);
            }
        );
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("내부 API 키가 없거나 다르면 DELIVERY_034를 반환한다")
    void rejectsInvalidApiKey() throws Exception {
        MockHttpServletRequest request = validRequest();
        request.removeHeader(InternalServiceAuthenticationFilter.API_KEY_HEADER);
        request.addHeader(InternalServiceAuthenticationFilter.API_KEY_HEADER, "wrong");

        assertError(request, 401, "DELIVERY_034");
    }

    @Test
    @DisplayName("scope가 다르면 DELIVERY_035를 반환한다")
    void rejectsInvalidScope() throws Exception {
        MockHttpServletRequest request = validRequest();
        request.removeHeader(InternalServiceAuthenticationFilter.SCOPE_HEADER);
        request.addHeader(InternalServiceAuthenticationFilter.SCOPE_HEADER, "delivery.other.read");

        assertError(request, 403, "DELIVERY_035");
    }

    @Test
    @DisplayName("subject ID가 없거나 양수가 아니면 DELIVERY_038을 반환한다")
    void rejectsInvalidSubject() throws Exception {
        MockHttpServletRequest request = validRequest();
        request.removeHeader(GatewayAuthenticationFilter.USER_ID_HEADER);

        assertError(request, 400, "DELIVERY_038");
    }

    @Test
    @DisplayName("subject 역할이 CUSTOMER가 아니면 DELIVERY_035를 반환한다")
    void rejectsNonCustomerSubject() throws Exception {
        MockHttpServletRequest request = validRequest();
        request.removeHeader(GatewayAuthenticationFilter.USER_ROLE_HEADER);
        request.addHeader(GatewayAuthenticationFilter.USER_ROLE_HEADER, "ADMIN");

        assertError(request, 403, "DELIVERY_035");
    }

    private MockHttpServletRequest validRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/deliveries/current");
        request.addHeader(InternalServiceAuthenticationFilter.SERVICE_HEADER, "customer-service");
        request.addHeader(InternalServiceAuthenticationFilter.API_KEY_HEADER, API_KEY);
        request.addHeader(InternalServiceAuthenticationFilter.SCOPE_HEADER, "delivery.current.read");
        request.addHeader(GatewayAuthenticationFilter.USER_ID_HEADER, "25");
        request.addHeader(GatewayAuthenticationFilter.USER_ROLE_HEADER, "CUSTOMER");
        return request;
    }

    private void assertError(
        MockHttpServletRequest request, int status, String code
    ) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentAsString()).contains("\"code\":\"" + code + "\"");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
