package com.chapchap.delivery.domain.rider.controller;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.rider.constant.RiderScheduleSource;
import com.chapchap.delivery.domain.rider.response.RiderScheduleItemResponse;
import com.chapchap.delivery.domain.rider.response.RiderScheduleResponse;
import com.chapchap.delivery.domain.rider.service.RiderScheduleService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiderMeController.class)
@Import({
    SecurityConfig.class
    , CustomAuthenticationEntryPoint.class
    , CustomAccessDeniedHandler.class
})
class RiderMeControllerSecurityTest {

    private static final Long RIDER_ID = 10L;
    private static final Long ACTOR_ID = 10001L;
    private static final LocalDate DATE_FROM = LocalDate.of(2026, 8, 24);
    private static final LocalDate DATE_TO = LocalDate.of(2026, 8, 25);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiderScheduleService riderScheduleService;

    @Test
    @DisplayName("인증 정보가 없으면 본인 실제 일정을 조회할 수 없다")
    void getMySchedulesReturnsUnauthorizedWithoutAuthentication() throws Exception {
        // when & then
        mockMvc.perform(
                get("/api/rider/me/schedules")
                    .param("dateFrom", "2026-08-24")
                    .param("dateTo", "2026-08-25")
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));

        verify(riderScheduleService, never()).getMySchedules(
            ACTOR_ID
            , UserRole.RIDER
            , DATE_FROM
            , DATE_TO
        );
    }

    @Test
    @DisplayName("CUSTOMER 역할은 기사 본인 실제 일정 조회 API에 접근할 수 없다")
    void getMySchedulesReturnsForbiddenForCustomer() throws Exception {
        // when & then
        mockMvc.perform(
                get("/api/rider/me/schedules")
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.CUSTOMER.name())
                    .param("dateFrom", "2026-08-24")
                    .param("dateTo", "2026-08-25")
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(riderScheduleService, never()).getMySchedules(
            ACTOR_ID
            , UserRole.RIDER
            , DATE_FROM
            , DATE_TO
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 기사 본인 실제 일정 조회 API에 접근할 수 없다")
    void getMySchedulesReturnsForbiddenForAdmin() throws Exception {
        // when & then
        mockMvc.perform(
                get("/api/rider/me/schedules")
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .param("dateFrom", "2026-08-24")
                    .param("dateTo", "2026-08-25")
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(riderScheduleService, never()).getMySchedules(
            ACTOR_ID
            , UserRole.RIDER
            , DATE_FROM
            , DATE_TO
        );
    }

    @Test
    @DisplayName("RIDER 역할은 본인의 실제 일정을 조회할 수 있다")
    void getMySchedulesReturnsSuccessForRider() throws Exception {
        // given
        RiderScheduleResponse serviceResponse =
            new RiderScheduleResponse(
                RIDER_ID
                , true
                , List.of(
                new RiderScheduleItemResponse(
                    LocalDate.of(2026, 8, 24)
                    , DeliverySlotCode.LUNCH
                    , false
                    , RiderScheduleSource.DATE_EXCEPTION
                )
                , new RiderScheduleItemResponse(
                    LocalDate.of(2026, 8, 25)
                    , DeliverySlotCode.DINNER
                    , true
                    , RiderScheduleSource.WEEKLY_DEFAULT
                )
            )
            );

        when(riderScheduleService.getMySchedules(ACTOR_ID, UserRole.RIDER, DATE_FROM, DATE_TO))
            .thenReturn(serviceResponse);

        // when & then
        mockMvc.perform(
                get("/api/rider/me/schedules")
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
                    .param("dateFrom", "2026-08-24")
                    .param("dateTo", "2026-08-25")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"))
            .andExpect(jsonPath("$.data.riderId").value(RIDER_ID))
            .andExpect(jsonPath("$.data.isDeliveryActive").value(true))
            .andExpect(jsonPath("$.data.schedules.length()").value(2))
            .andExpect(jsonPath("$.data.schedules[0].date").value("2026-08-24"))
            .andExpect(jsonPath("$.data.schedules[0].deliverySlot").value("LUNCH"))
            .andExpect(jsonPath("$.data.schedules[0].isWorking").value(false))
            .andExpect(jsonPath("$.data.schedules[0].source").value("DATE_EXCEPTION"))
            .andExpect(jsonPath("$.data.schedules[1].date").value("2026-08-25"))
            .andExpect(jsonPath("$.data.schedules[1].deliverySlot").value("DINNER"))
            .andExpect(jsonPath("$.data.schedules[1].isWorking").value(true))
            .andExpect(jsonPath("$.data.schedules[1].source").value("WEEKLY_DEFAULT"));

        verify(riderScheduleService).getMySchedules(
            ACTOR_ID
            , UserRole.RIDER
            , DATE_FROM
            , DATE_TO
        );
    }

    @Test
    @DisplayName("dateFrom이 없으면 본인 실제 일정 조회 요청은 400을 반환한다")
    void getMySchedulesReturnsBadRequestWithoutDateFrom() throws Exception {
        // when & then
        mockMvc.perform(
                get("/api/rider/me/schedules")
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
                    .param("dateTo", "2026-08-25")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));

        verify(riderScheduleService, never()).getMySchedules(
            ACTOR_ID
            , UserRole.RIDER
            , DATE_FROM
            , DATE_TO
        );
    }

    @Test
    @DisplayName("dateTo가 없으면 본인 실제 일정 조회 요청은 400을 반환한다")
    void getMySchedulesReturnsBadRequestWithoutDateTo() throws Exception {
        // when & then
        mockMvc.perform(
                get("/api/rider/me/schedules")
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
                    .param("dateFrom", "2026-08-24")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));

        verify(riderScheduleService, never()).getMySchedules(
            ACTOR_ID
            , UserRole.RIDER
            , DATE_FROM
            , DATE_TO
        );
    }

    @Test
    @DisplayName("날짜 형식이 잘못되면 본인 실제 일정 조회 요청은 400을 반환한다")
    void getMySchedulesReturnsBadRequestForInvalidDateFormat() throws Exception {
        // when & then
        mockMvc.perform(
                get("/api/rider/me/schedules")
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
                    .param("dateFrom", "2026-08-XX")
                    .param("dateTo", "2026-08-25")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));

        verify(riderScheduleService, never()).getMySchedules(
            ACTOR_ID
            , UserRole.RIDER
            , DATE_FROM
            , DATE_TO
        );
    }
}