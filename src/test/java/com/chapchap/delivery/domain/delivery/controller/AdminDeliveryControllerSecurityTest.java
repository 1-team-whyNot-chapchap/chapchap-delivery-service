package com.chapchap.delivery.domain.delivery.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.delivery.constant.DeliveryRecoveryResult;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryRecoveryResponse;
import com.chapchap.delivery.domain.delivery.service.AdminDeliveryFailureService;
import com.chapchap.delivery.domain.delivery.service.AdminDeliveryRecoveryService;
import com.chapchap.delivery.domain.delivery.service.DeliveryPhotoAccessService;
import com.chapchap.delivery.global.exception.ErrorCode;
import com.chapchap.delivery.global.security.CustomAccessDeniedHandler;
import com.chapchap.delivery.global.security.CustomAuthenticationEntryPoint;
import com.chapchap.delivery.global.security.SecurityConfig;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminDeliveryController.class)
@Import({
    SecurityConfig.class
    , CustomAuthenticationEntryPoint.class
    , CustomAccessDeniedHandler.class
})
class AdminDeliveryControllerSecurityTest {
    private static final Long ADMIN_ID = 100L;
    private static final String DELIVERY_ID = "delivery-1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDeliveryFailureService failureService;

    @MockitoBean
    private DeliveryPhotoAccessService photoAccessService;

    @MockitoBean
    private AdminDeliveryRecoveryService recoveryService;

    @Test
    @DisplayName("관리자 복구 API는 미인증 요청을 거절한다")
    void recoveryReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(
                post("/api/delivery/admin/deliveries/{deliveryId}/recovery", DELIVERY_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(failedRecoveryRequest())
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()));

        verify(recoveryService, never()).recover(any(), any(), any(), any());
    }

    @Test
    @DisplayName("기사는 관리자 복구 API를 사용할 수 없다")
    void recoveryReturnsForbiddenForRider() throws Exception {
        mockMvc.perform(
                post("/api/delivery/admin/deliveries/{deliveryId}/recovery", DELIVERY_ID)
                    .header("X-User-Id", ADMIN_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(failedRecoveryRequest())
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()));

        verify(recoveryService, never()).recover(any(), any(), any(), any());
    }

    @Test
    @DisplayName("관리자는 장애 사후 실패 결과를 기록할 수 있다")
    void recoveryReturnsSuccessForAdmin() throws Exception {
        when(
            recoveryService.recover(
                eq(ADMIN_ID)
                , eq(UserRole.ADMIN)
                , eq(DELIVERY_ID)
                , any()
            )
        ).thenReturn(
            new AdminDeliveryRecoveryResponse(
                DELIVERY_ID
                , DeliveryStatus.FAILED
                , 3
                , DeliveryRecoveryResult.FAILED
                , 11L
                , OffsetDateTime.parse("2026-09-06T13:10:00+09:00")
            )
        );

        mockMvc.perform(
                post("/api/delivery/admin/deliveries/{deliveryId}/recovery", DELIVERY_ID)
                    .header("X-User-Id", ADMIN_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(failedRecoveryRequest())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.data.status").value("FAILED"))
            .andExpect(jsonPath("$.data.recoveryResult").value("FAILED"));
    }

    @Test
    @DisplayName("관리자 일반 대리 완료 API는 제공하지 않는다")
    void normalAdminCompletionEndpointDoesNotExist() throws Exception {
        mockMvc.perform(
                post("/api/delivery/admin/deliveries/{deliveryId}/complete", DELIVERY_ID)
                    .header("X-User-Id", ADMIN_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andExpect(status().isNotFound());
    }

    private String failedRecoveryRequest() {
        return """
            {
              "recoveryResult": "FAILED",
              "reasonCode": "DEVICE_FAILURE",
              "actualRiderId": 11,
              "failure": {
                "failureStage": "DURING_DELIVERY",
                "failureCode": "VEHICLE_ISSUE",
                "itemRecovered": false
              }
            }
        """;
    }
}
