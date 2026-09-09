package com.chapchap.delivery.domain.audit.controller;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.audit.response.AdminAuditHistoryListResponse;
import com.chapchap.delivery.domain.audit.service.AdminAuditHistoryQueryService;
import com.chapchap.delivery.global.exception.ErrorCode;
import com.chapchap.delivery.global.security.CustomAccessDeniedHandler;
import com.chapchap.delivery.global.security.CustomAuthenticationEntryPoint;
import com.chapchap.delivery.global.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuditHistoryController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class AdminAuditHistoryControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminAuditHistoryQueryService queryService;

    @Test
    void customerCannotReadAuditHistories() throws Exception {
        mockMvc.perform(get("/api/delivery/admin/audit-histories")
                .header("X-User-Id", 10L)
                .header("X-User-Role", UserRole.CUSTOMER.name()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()));
        verify(queryService, never()).getAuditHistories(any(), any(), any(), any(), any());
    }

    @Test
    void adminCanReadAuditHistories() throws Exception {
        when(queryService.getAuditHistories(eq(10L), eq(UserRole.ADMIN), any(), any(), any()))
            .thenReturn(new AdminAuditHistoryListResponse(List.of(), 0, 20, 0, 0, false));
        mockMvc.perform(get("/api/delivery/admin/audit-histories")
                .header("X-User-Id", 10L)
                .header("X-User-Role", UserRole.ADMIN.name()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray());
    }
}
