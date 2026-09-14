package com.chapchap.delivery.domain.riderlocation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.riderlocation.response.RiderLocationResponse;
import com.chapchap.delivery.domain.riderlocation.service.RiderLocationService;
import com.chapchap.delivery.global.exception.ErrorCode;
import com.chapchap.delivery.global.security.CustomAccessDeniedHandler;
import com.chapchap.delivery.global.security.CustomAuthenticationEntryPoint;
import com.chapchap.delivery.global.security.SecurityConfig;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RiderLocationController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class RiderLocationControllerSecurityTest {
    private static final long USER_ID = 100L;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private RiderLocationService locationService;

    @Test
    @DisplayName("인증 기사는 정상 위치를 자신의 Rider 관계로 갱신할 수 있다")
    void riderCanUpdateOwnLocation() throws Exception {
        when(locationService.update(eq(USER_ID), eq(UserRole.RIDER), any())).thenReturn(response());

        mockMvc.perform(put("/api/delivery/rider/location")
                .header("X-User-Id", USER_ID)
                .header("X-User-Role", UserRole.RIDER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"latitude":37.5665,"longitude":126.9780,"accuracy":12.5,
                     "capturedAt":"2026-09-11T12:00:00+09:00"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.latitude").value(37.5665));

        verify(locationService).update(eq(USER_ID), eq(UserRole.RIDER), any());
    }

    @Test
    @DisplayName("고객은 기사 위치 갱신 API를 호출할 수 없다")
    void customerCannotUpdateRiderLocation() throws Exception {
        mockMvc.perform(put("/api/delivery/rider/location")
                .header("X-User-Id", USER_ID)
                .header("X-User-Role", UserRole.CUSTOMER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()));

        verify(locationService, never()).update(any(), any(), any());
    }

    @Test
    @DisplayName("좌표 범위가 잘못된 요청은 Service 호출 전에 거절한다")
    void rejectsInvalidCoordinates() throws Exception {
        mockMvc.perform(put("/api/delivery/rider/location")
                .header("X-User-Id", USER_ID)
                .header("X-User-Role", UserRole.RIDER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"latitude":91,"longitude":126.9780,"accuracy":12.5,
                     "capturedAt":"2026-09-11T12:00:00+09:00"}
                    """))
            .andExpect(status().isBadRequest());

        verify(locationService, never()).update(any(), any(), any());
    }

    private RiderLocationResponse response() {
        OffsetDateTime time = OffsetDateTime.of(2026, 9, 11, 12, 0, 0, 0, ZoneOffset.ofHours(9));
        return new RiderLocationResponse(
            new BigDecimal("37.5665"), new BigDecimal("126.9780"), new BigDecimal("12.5"), time, time
        );
    }
}
