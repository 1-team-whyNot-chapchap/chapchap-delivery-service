package com.chapchap.delivery.domain.delivery.controller;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryOperationCountsResponse;
import com.chapchap.delivery.domain.delivery.service.AdminDeliveryOperationQueryService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDeliveryOperationController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class AdminDeliveryOperationControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminDeliveryOperationQueryService queryService;

    @Test
    void riderCannotReadOperationCounts() throws Exception {
        mockMvc.perform(get("/api/delivery/admin/delivery-operations/counts")
                .header("X-User-Id", 10L)
                .header("X-User-Role", UserRole.RIDER.name()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()));
        verify(queryService, never()).getCounts(any(), any(), any(), any());
    }

    @Test
    void adminCanReadOperationCounts() throws Exception {
        when(queryService.getCounts(eq(10L), eq(UserRole.ADMIN), any(), any()))
            .thenReturn(new AdminDeliveryOperationCountsResponse(1, 2, 3, 4));
        mockMvc.perform(get("/api/delivery/admin/delivery-operations/counts")
                .header("X-User-Id", 10L)
                .header("X-User-Role", UserRole.ADMIN.name()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.autoAssignmentFinalFailure").value(1))
            .andExpect(jsonPath("$.data.unresolvedDelivery").value(4));
    }
}
