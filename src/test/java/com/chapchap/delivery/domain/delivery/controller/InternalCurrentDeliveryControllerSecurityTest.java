package com.chapchap.delivery.domain.delivery.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.delivery.constant.CurrentDeliveryDelayStatus;
import com.chapchap.delivery.domain.delivery.constant.CurrentDeliveryStatus;
import com.chapchap.delivery.domain.delivery.response.CurrentDeliveryResponse;
import com.chapchap.delivery.domain.delivery.service.CurrentDeliveryQueryService;
import com.chapchap.delivery.global.security.CustomAccessDeniedHandler;
import com.chapchap.delivery.global.security.CustomAuthenticationEntryPoint;
import com.chapchap.delivery.global.security.SecurityConfig;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InternalCurrentDeliveryController.class)
@Import({
    SecurityConfig.class,
    CustomAuthenticationEntryPoint.class,
    CustomAccessDeniedHandler.class
})
@TestPropertySource(properties = {
    "app.internal-api.api-key=internal-test-key",
    "app.internal-api.customer-ai-api-key=customer-ai-test-key"
})
class InternalCurrentDeliveryControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private CurrentDeliveryQueryService queryService;

    @Test
    @DisplayName("Customer-Service의 유효한 서비스 자격과 CUSTOMER subject로 현재 배송을 조회한다")
    void customerServiceCanReadCurrentDelivery() throws Exception {
        OffsetDateTime changedAt = OffsetDateTime.parse("2026-09-08T08:30:00+09:00");
        when(queryService.getCurrent(25L, UserRole.CUSTOMER)).thenReturn(
            new CurrentDeliveryResponse(
                CurrentDeliveryStatus.DELIVERING,
                CurrentDeliveryDelayStatus.NOT_DELAYED,
                changedAt
            )
        );

        mockMvc.perform(get("/internal/deliveries/current")
                .header("X-Internal-Service", "customer-service")
                .header("X-Internal-Api-Key", "internal-test-key")
                .header("X-Internal-Scope", "delivery.current.read")
                .header("X-User-Id", "25")
                .header("X-User-Role", "CUSTOMER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.data.status").value("DELIVERING"))
            .andExpect(jsonPath("$.data.delayStatus").value("NOT_DELAYED"));

        verify(queryService).getCurrent(25L, UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("Customer-AI의 전용 서비스 자격과 CUSTOMER subject로 현재 배송을 조회한다")
    void customerAiCanReadCurrentDelivery() throws Exception {
        when(queryService.getCurrent(25L, UserRole.CUSTOMER)).thenReturn(
            new CurrentDeliveryResponse(
                CurrentDeliveryStatus.READY,
                CurrentDeliveryDelayStatus.UNKNOWN,
                null
            )
        );

        mockMvc.perform(get("/internal/deliveries/current")
                .header("X-Internal-Service", "customer-ai")
                .header("X-Internal-Api-Key", "customer-ai-test-key")
                .header("X-Internal-Scope", "delivery.status.read")
                .header("X-User-Id", "25")
                .header("X-User-Role", "CUSTOMER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.data.status").value("READY"))
            .andExpect(jsonPath("$.data.delayStatus").value("UNKNOWN"));

        verify(queryService).getCurrent(25L, UserRole.CUSTOMER);
    }
}
