package com.chapchap.delivery.domain.delivery.controller;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventStatus;
import com.chapchap.delivery.domain.delivery.response.AdminIntegrationEventListResponse;
import com.chapchap.delivery.domain.delivery.response.AdminIntegrationEventRepublishResponse;
import com.chapchap.delivery.domain.delivery.service.AdminIntegrationEventService;
import com.chapchap.delivery.global.exception.ErrorCode;
import com.chapchap.delivery.global.security.CustomAccessDeniedHandler;
import com.chapchap.delivery.global.security.CustomAuthenticationEntryPoint;
import com.chapchap.delivery.global.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminIntegrationEventController.class)
@Import({
    SecurityConfig.class
    , CustomAuthenticationEntryPoint.class
    , CustomAccessDeniedHandler.class
})
class AdminIntegrationEventControllerSecurityTest {
    private static final Long ADMIN_ID = 100L;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminIntegrationEventService service;

    @Test
    @DisplayName("이벤트 처리 기록 조회는 미인증 요청을 거절한다")
    void listReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/delivery/admin/integration-events"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()));

        verify(service, never()).getEvents(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("고객은 이벤트 처리 기록을 조회할 수 없다")
    void listReturnsForbiddenForCustomer() throws Exception {
        mockMvc.perform(
                get("/api/delivery/admin/integration-events")
                    .header("X-User-Id", ADMIN_ID)
                    .header("X-User-Role", UserRole.CUSTOMER.name())
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()));

        verify(service, never()).getEvents(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("관리자는 이벤트 처리 기록을 조회할 수 있다")
    void listReturnsSuccessForAdmin() throws Exception {
        when(service.getEvents(
            eq(ADMIN_ID), eq(UserRole.ADMIN), any(), any(), any(), any(), any(), any()
        )).thenReturn(new AdminIntegrationEventListResponse(
            List.of(), 0, 20, 0L, 0, false
        ));

        mockMvc.perform(
                get("/api/delivery/admin/integration-events")
                    .header("X-User-Id", ADMIN_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"));
    }

    @Test
    @DisplayName("기사는 이벤트 수동 재발행 API를 사용할 수 없다")
    void republishReturnsForbiddenForRider() throws Exception {
        mockMvc.perform(
                post("/api/delivery/admin/integration-events/{id}/republish", 1L)
                    .header("X-User-Id", ADMIN_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
            )
            .andExpect(status().isForbidden());

        verify(service, never()).republish(any(), any(), any());
    }

    @Test
    @DisplayName("관리자는 FAILED Event를 수동 재발행할 수 있다")
    void republishReturnsSuccessForAdmin() throws Exception {
        when(service.republish(ADMIN_ID, UserRole.ADMIN, 1L))
            .thenReturn(new AdminIntegrationEventRepublishResponse(
                1L, "event-1", IntegrationEventStatus.SUCCESS, 2,
                OffsetDateTime.parse("2026-09-07T12:00:00+09:00")
            ));

        mockMvc.perform(
                post("/api/delivery/admin/integration-events/{id}/republish", 1L)
                    .header("X-User-Id", ADMIN_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.data.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.attemptCount").value(2));
    }
}
