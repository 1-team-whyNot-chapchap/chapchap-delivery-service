package com.chapchap.delivery.domain.rider.controller;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.rider.constant.RiderDeliveryActiveReason;
import com.chapchap.delivery.domain.rider.constant.RiderScheduleExceptionReason;
import com.chapchap.delivery.domain.rider.request.RiderDeliveryAreaCreateRequest;
import com.chapchap.delivery.domain.rider.request.RiderDeliveryAreaUpdateRequest;
import com.chapchap.delivery.domain.rider.request.RiderScheduleExceptionCreateRequest;
import com.chapchap.delivery.domain.rider.request.RiderScheduleExceptionUpdateRequest;
import com.chapchap.delivery.domain.rider.request.RiderUpdateRequest;
import com.chapchap.delivery.domain.rider.request.RiderWeeklyScheduleCreateRequest;
import com.chapchap.delivery.domain.rider.response.RiderDeliveryAreaResponse;
import com.chapchap.delivery.domain.rider.response.RiderScheduleExceptionResponse;
import com.chapchap.delivery.domain.rider.response.RiderWeeklyScheduleResponse;
import com.chapchap.delivery.domain.rider.service.RiderDeliveryAreaService;
import com.chapchap.delivery.domain.rider.service.RiderScheduleExceptionService;
import com.chapchap.delivery.domain.rider.service.RiderService;
import com.chapchap.delivery.domain.rider.service.RiderWeeklyScheduleService;
import com.chapchap.delivery.global.exception.ErrorCode;
import com.chapchap.delivery.global.security.CustomAccessDeniedHandler;
import com.chapchap.delivery.global.security.CustomAuthenticationEntryPoint;
import com.chapchap.delivery.global.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiderController.class)
@Import({
    SecurityConfig.class
    , CustomAuthenticationEntryPoint.class
    , CustomAccessDeniedHandler.class
})
class RiderControllerSecurityTest {
    private static final Long RIDER_ID = 10L;
    private static final Long ACTOR_ID = 9001L;
    private static final Long SCHEDULE_ID = 100L;
    private static final Long EXCEPTION_ID = 200L;
    private static final Long RIDER_AREA_ID = 300L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiderService riderService;

    @MockitoBean
    private RiderWeeklyScheduleService riderWeeklyScheduleService;

    @MockitoBean
    private RiderScheduleExceptionService riderScheduleExceptionService;

    @MockitoBean
    private RiderDeliveryAreaService riderDeliveryAreaService;

    @Test
    @DisplayName("인증 정보가 없으면 기사 배달 활성 상태를 변경할 수 없다")
    void changeDeliveryActiveReturnsUnauthorizedWithoutAuthentication() throws Exception {
        // given
        String requestBody =
            """
            {
              "isDeliveryActive": false,
              "version": 3,
              "reasonCode": "TRAINING",
              "reasonDetail": "신규 기사 현장 교육"
            }
            """;

        // when & then
        mockMvc.perform(
                patch("/api/delivery/admin/riders/{riderId}/delivery-active", RIDER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));

        verify(riderService, never()).changeDeliveryActive(
            any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("RIDER 역할은 관리자 기사 배달 활성 상태 변경 API에 접근할 수 없다")
    void changeDeliveryActiveReturnsForbiddenForRider() throws Exception {
        // given
        String requestBody =
            """
            {
              "isDeliveryActive": false,
              "version": 3,
              "reasonCode": "TRAINING",
              "reasonDetail": "신규 기사 현장 교육"
            }
            """;

        // when & then
        mockMvc.perform(
                patch("/api/delivery/admin/riders/{riderId}/delivery-active", RIDER_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(riderService, never()).changeDeliveryActive(
            any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 기사 배달 활성 상태를 변경할 수 있다")
    void changeDeliveryActiveReturnsSuccessForAdmin() throws Exception {
        // given
        String requestBody =
            """
            {
              "isDeliveryActive": false,
              "version": 3,
              "reasonCode": "TRAINING",
              "reasonDetail": "신규 기사 현장 교육"
            }
            """;

        // when & then
        mockMvc.perform(
                patch("/api/delivery/admin/riders/{riderId}/delivery-active", RIDER_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"));

        verify(riderService).changeDeliveryActive(
            RIDER_ID
            , ACTOR_ID
            , UserRole.ADMIN
            , new RiderUpdateRequest(
                false
                , 3L
                , RiderDeliveryActiveReason.TRAINING
                , "신규 기사 현장 교육"
            )
        );
    }

    @Test
    @DisplayName("인증 정보가 없으면 주간 기본 일정을 등록할 수 없다")
    void createWeeklyScheduleReturnsUnauthorizedWithoutAuthentication() throws Exception {
        // given
        String requestBody =
            """
            {
              "dayOfWeek": 1,
              "deliverySlot": "LUNCH"
            }
            """;

        // when & then
        mockMvc.perform(
                post("/api/delivery/admin/riders/{riderId}/weekly-schedules", RIDER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));

        verify(riderWeeklyScheduleService, never()).createWeeklySchedule(
            any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("RIDER 역할은 주간 기본 일정 등록 API에 접근할 수 없다")
    void createWeeklyScheduleReturnsForbiddenForRider() throws Exception {
        // given
        String requestBody =
            """
            {
              "dayOfWeek": 1,
              "deliverySlot": "LUNCH"
            }
            """;

        // when & then
        mockMvc.perform(
                post("/api/delivery/admin/riders/{riderId}/weekly-schedules", RIDER_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(riderWeeklyScheduleService, never()).createWeeklySchedule(
            any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 주간 기본 일정을 등록할 수 있다")
    void createWeeklyScheduleReturnsSuccessForAdmin() throws Exception {
        // given
        RiderWeeklyScheduleResponse serviceResponse =
            new RiderWeeklyScheduleResponse(
                SCHEDULE_ID
                , RIDER_ID
                , (byte) 1
                , DeliverySlotCode.LUNCH
            );

        when(
            riderWeeklyScheduleService.createWeeklySchedule(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , new RiderWeeklyScheduleCreateRequest(
                    (byte) 1
                    , DeliverySlotCode.LUNCH
                )
            )
        ).thenReturn(serviceResponse);

        String requestBody =
            """
            {
              "dayOfWeek": 1,
              "deliverySlot": "LUNCH"
            }
            """;

        // when & then
        mockMvc.perform(
                post("/api/delivery/admin/riders/{riderId}/weekly-schedules", RIDER_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"))
            .andExpect(jsonPath("$.data.scheduleId").value(SCHEDULE_ID))
            .andExpect(jsonPath("$.data.riderId").value(RIDER_ID))
            .andExpect(jsonPath("$.data.dayOfWeek").value(1))
            .andExpect(jsonPath("$.data.deliverySlot").value("LUNCH"));

        verify(riderWeeklyScheduleService).createWeeklySchedule(
            RIDER_ID
            , ACTOR_ID
            , UserRole.ADMIN
            , new RiderWeeklyScheduleCreateRequest(
                (byte) 1
                , DeliverySlotCode.LUNCH
            )
        );
    }

    @Test
    @DisplayName("잘못된 요일로 주간 기본 일정을 등록하면 400을 반환한다")
    void createWeeklyScheduleReturnsBadRequestForInvalidDayOfWeek() throws Exception {
        // given
        String requestBody =
            """
            {
              "dayOfWeek": 0,
              "deliverySlot": "LUNCH"
            }
            """;

        // when & then
        mockMvc.perform(
                post("/api/delivery/admin/riders/{riderId}/weekly-schedules", RIDER_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));

        verify(riderWeeklyScheduleService, never()).createWeeklySchedule(
            any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 주간 기본 일정 목록을 조회할 수 있다")
    void getWeeklySchedulesReturnsSuccessForAdmin() throws Exception {
        // given
        List<RiderWeeklyScheduleResponse> serviceResponses =
            List.of(
                new RiderWeeklyScheduleResponse(
                    100L
                    , RIDER_ID
                    , (byte) 1
                    , DeliverySlotCode.LUNCH
                )
                , new RiderWeeklyScheduleResponse(
                    101L
                    , RIDER_ID
                    , (byte) 3
                    , DeliverySlotCode.DINNER
                )
            );

        when(
            riderWeeklyScheduleService.getWeeklySchedules(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
            )
        ).thenReturn(serviceResponses);

        // when & then
        mockMvc.perform(
                get("/api/delivery/admin/riders/{riderId}/weekly-schedules", RIDER_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].scheduleId").value(100L))
            .andExpect(jsonPath("$.data[0].dayOfWeek").value(1))
            .andExpect(jsonPath("$.data[0].deliverySlot").value("LUNCH"))
            .andExpect(jsonPath("$.data[1].scheduleId").value(101L))
            .andExpect(jsonPath("$.data[1].dayOfWeek").value(3))
            .andExpect(jsonPath("$.data[1].deliverySlot").value("DINNER"));

        verify(riderWeeklyScheduleService).getWeeklySchedules(
            RIDER_ID
            , ACTOR_ID
            , UserRole.ADMIN
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 주간 기본 일정을 삭제할 수 있다")
    void deleteWeeklyScheduleReturnsSuccessForAdmin() throws Exception {
        // when & then
        mockMvc.perform(
                delete(
                    "/api/delivery/admin/riders/{riderId}/weekly-schedules/{scheduleId}"
                    , RIDER_ID
                    , SCHEDULE_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"));

        verify(riderWeeklyScheduleService).deleteWeeklySchedule(
            RIDER_ID
            , SCHEDULE_ID
            , ACTOR_ID
            , UserRole.ADMIN
        );
    }

    @Test
    @DisplayName("인증 정보가 없으면 날짜별 예외 일정을 등록할 수 없다")
    void createScheduleExceptionReturnsUnauthorizedWithoutAuthentication() throws Exception {
        // given
        String requestBody =
            """
            {
              "scheduleDate": "2026-09-05",
              "deliverySlot": "LUNCH",
              "isWorking": false,
              "reasonCode": "TRAINING",
              "reasonDetail": "신규 기사 교육"
            }
            """;

        // when & then
        mockMvc.perform(
                post("/api/delivery/admin/riders/{riderId}/schedule-exceptions", RIDER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));

        verify(riderScheduleExceptionService, never()).createScheduleException(
            any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("RIDER 역할은 날짜별 예외 일정 등록 API에 접근할 수 없다")
    void createScheduleExceptionReturnsForbiddenForRider() throws Exception {
        // given
        String requestBody =
            """
            {
              "scheduleDate": "2026-09-05",
              "deliverySlot": "LUNCH",
              "isWorking": false,
              "reasonCode": "TRAINING",
              "reasonDetail": "신규 기사 교육"
            }
            """;

        // when & then
        mockMvc.perform(
                post("/api/delivery/admin/riders/{riderId}/schedule-exceptions", RIDER_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(riderScheduleExceptionService, never()).createScheduleException(
            any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 날짜별 예외 일정을 등록할 수 있다")
    void createScheduleExceptionReturnsSuccessForAdmin() throws Exception {
        // given
        LocalDate scheduleDate =
            LocalDate.of(
                2026
                , 9
                , 5
            );

        RiderScheduleExceptionCreateRequest request =
            new RiderScheduleExceptionCreateRequest(
                scheduleDate
                , DeliverySlotCode.LUNCH
                , false
                , RiderScheduleExceptionReason.TRAINING
                , "신규 기사 교육"
            );

        RiderScheduleExceptionResponse serviceResponse =
            new RiderScheduleExceptionResponse(
                EXCEPTION_ID
                , RIDER_ID
                , scheduleDate
                , DeliverySlotCode.LUNCH
                , false
                , RiderScheduleExceptionReason.TRAINING
                , "신규 기사 교육"
                , 0L
            );

        when(
            riderScheduleExceptionService.createScheduleException(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            )
        ).thenReturn(serviceResponse);

        String requestBody =
            """
            {
              "scheduleDate": "2026-09-05",
              "deliverySlot": "LUNCH",
              "isWorking": false,
              "reasonCode": "TRAINING",
              "reasonDetail": "신규 기사 교육"
            }
            """;

        // when & then
        mockMvc.perform(
                post("/api/delivery/admin/riders/{riderId}/schedule-exceptions", RIDER_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"))
            .andExpect(jsonPath("$.data.exceptionId").value(EXCEPTION_ID))
            .andExpect(jsonPath("$.data.riderId").value(RIDER_ID))
            .andExpect(jsonPath("$.data.scheduleDate").value("2026-09-05"))
            .andExpect(jsonPath("$.data.deliverySlot").value("LUNCH"))
            .andExpect(jsonPath("$.data.isWorking").value(false))
            .andExpect(jsonPath("$.data.reasonCode").value("TRAINING"))
            .andExpect(jsonPath("$.data.reasonDetail").value("신규 기사 교육"))
            .andExpect(jsonPath("$.data.version").value(0));

        verify(riderScheduleExceptionService).createScheduleException(
            RIDER_ID
            , ACTOR_ID
            , UserRole.ADMIN
            , request
        );
    }

    @Test
    @DisplayName("필수 값이 누락된 날짜별 예외 일정 등록 요청은 400을 반환한다")
    void createScheduleExceptionReturnsBadRequestForInvalidRequest() throws Exception {
        // given
        String requestBody =
            """
            {
              "deliverySlot": "LUNCH",
              "isWorking": false,
              "reasonCode": "TRAINING",
              "reasonDetail": "신규 기사 교육"
            }
            """;

        // when & then
        mockMvc.perform(
                post("/api/delivery/admin/riders/{riderId}/schedule-exceptions", RIDER_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));

        verify(riderScheduleExceptionService, never()).createScheduleException(
            any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 날짜별 예외 일정 목록을 조회할 수 있다")
    void getScheduleExceptionsReturnsSuccessForAdmin() throws Exception {
        // given
        LocalDate dateFrom =
            LocalDate.of(
                2026
                , 9
                , 1
            );

        LocalDate dateTo =
            LocalDate.of(
                2026
                , 9
                , 7
            );

        List<RiderScheduleExceptionResponse> serviceResponses =
            List.of(
                new RiderScheduleExceptionResponse(
                    200L
                    , RIDER_ID
                    , LocalDate.of(
                    2026
                    , 9
                    , 3
                )
                    , DeliverySlotCode.LUNCH
                    , false
                    , RiderScheduleExceptionReason.ANNUAL_LEAVE
                    , "연차"
                    , 0L
                )
                , new RiderScheduleExceptionResponse(
                    201L
                    , RIDER_ID
                    , LocalDate.of(
                    2026
                    , 9
                    , 5
                )
                    , DeliverySlotCode.DINNER
                    , false
                    , RiderScheduleExceptionReason.TRAINING
                    , "교육"
                    , 1L
                )
            );

        when(
            riderScheduleExceptionService.getScheduleExceptions(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , dateFrom
                , dateTo
            )
        ).thenReturn(serviceResponses);

        // when & then
        mockMvc.perform(
                get("/api/delivery/admin/riders/{riderId}/schedule-exceptions", RIDER_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .param("dateFrom", "2026-09-01")
                    .param("dateTo", "2026-09-07")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].exceptionId").value(200L))
            .andExpect(jsonPath("$.data[0].scheduleDate").value("2026-09-03"))
            .andExpect(jsonPath("$.data[0].deliverySlot").value("LUNCH"))
            .andExpect(jsonPath("$.data[0].reasonCode").value("ANNUAL_LEAVE"))
            .andExpect(jsonPath("$.data[1].exceptionId").value(201L))
            .andExpect(jsonPath("$.data[1].scheduleDate").value("2026-09-05"))
            .andExpect(jsonPath("$.data[1].deliverySlot").value("DINNER"))
            .andExpect(jsonPath("$.data[1].reasonCode").value("TRAINING"));

        verify(riderScheduleExceptionService).getScheduleExceptions(
            RIDER_ID
            , ACTOR_ID
            , UserRole.ADMIN
            , dateFrom
            , dateTo
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 날짜별 예외 일정을 수정할 수 있다")
    void updateScheduleExceptionReturnsSuccessForAdmin() throws Exception {
        // given
        RiderScheduleExceptionUpdateRequest request =
            new RiderScheduleExceptionUpdateRequest(
                true
                , RiderScheduleExceptionReason.SUBSTITUTE_WORK
                , "대체 근무로 변경"
                , 0L
            );

        RiderScheduleExceptionResponse serviceResponse =
            new RiderScheduleExceptionResponse(
                EXCEPTION_ID
                , RIDER_ID
                , LocalDate.of(
                2026
                , 9
                , 5
            )
                , DeliverySlotCode.LUNCH
                , true
                , RiderScheduleExceptionReason.SUBSTITUTE_WORK
                , "대체 근무로 변경"
                , 1L
            );

        when(
            riderScheduleExceptionService.updateScheduleException(
                RIDER_ID
                , EXCEPTION_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            )
        ).thenReturn(serviceResponse);

        String requestBody =
            """
            {
              "isWorking": true,
              "reasonCode": "SUBSTITUTE_WORK",
              "reasonDetail": "대체 근무로 변경",
              "version": 0
            }
            """;

        // when & then
        mockMvc.perform(
                patch(
                    "/api/delivery/admin/riders/{riderId}/schedule-exceptions/{exceptionId}"
                    , RIDER_ID
                    , EXCEPTION_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"))
            .andExpect(jsonPath("$.data.exceptionId").value(EXCEPTION_ID))
            .andExpect(jsonPath("$.data.riderId").value(RIDER_ID))
            .andExpect(jsonPath("$.data.isWorking").value(true))
            .andExpect(jsonPath("$.data.reasonCode").value("SUBSTITUTE_WORK"))
            .andExpect(jsonPath("$.data.reasonDetail").value("대체 근무로 변경"))
            .andExpect(jsonPath("$.data.version").value(1));

        verify(riderScheduleExceptionService).updateScheduleException(
            RIDER_ID
            , EXCEPTION_ID
            , ACTOR_ID
            , UserRole.ADMIN
            , request
        );
    }

    @Test
    @DisplayName("음수 version으로 날짜별 예외 일정을 수정하면 400을 반환한다")
    void updateScheduleExceptionReturnsBadRequestForInvalidVersion() throws Exception {
        // given
        String requestBody =
            """
            {
              "isWorking": true,
              "reasonCode": "SUBSTITUTE_WORK",
              "reasonDetail": "대체 근무로 변경",
              "version": -1
            }
            """;

        // when & then
        mockMvc.perform(
                patch(
                    "/api/delivery/admin/riders/{riderId}/schedule-exceptions/{exceptionId}"
                    , RIDER_ID
                    , EXCEPTION_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));

        verify(riderScheduleExceptionService, never()).updateScheduleException(
            any()
            , any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 날짜별 예외 일정을 삭제할 수 있다")
    void deleteScheduleExceptionReturnsSuccessForAdmin() throws Exception {
        // when & then
        mockMvc.perform(
                delete(
                    "/api/delivery/admin/riders/{riderId}/schedule-exceptions/{exceptionId}"
                    , RIDER_ID
                    , EXCEPTION_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"));

        verify(riderScheduleExceptionService).deleteScheduleException(
            RIDER_ID
            , EXCEPTION_ID
            , ACTOR_ID
            , UserRole.ADMIN
        );
    }

    @Test
    @DisplayName("인증 정보가 없으면 기사 담당 배송 지역을 등록할 수 없다")
    void createDeliveryAreaReturnsUnauthorizedWithoutAuthentication() throws Exception {
        // given
        String requestBody =
            """
            {
              "deliveryAreaCode": "DAEGU_JUNG_GU",
              "effectiveFrom": "2026-09-05",
              "effectiveTo": "2026-10-31",
              "isActive": true
            }
            """;

        // when & then
        mockMvc.perform(
                post("/api/delivery/admin/riders/{riderId}/delivery-areas", RIDER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));

        verify(riderDeliveryAreaService, never()).createDeliveryArea(
            any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("RIDER 역할은 기사 담당 배송 지역 등록 API에 접근할 수 없다")
    void createDeliveryAreaReturnsForbiddenForRider() throws Exception {
        // given
        String requestBody =
            """
            {
              "deliveryAreaCode": "DAEGU_JUNG_GU",
              "effectiveFrom": "2026-09-05",
              "effectiveTo": "2026-10-31",
              "isActive": true
            }
            """;

        // when & then
        mockMvc.perform(
                post("/api/delivery/admin/riders/{riderId}/delivery-areas", RIDER_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(riderDeliveryAreaService, never()).createDeliveryArea(
            any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 기사 담당 배송 지역을 등록할 수 있다")
    void createDeliveryAreaReturnsSuccessForAdmin() throws Exception {
        // given
        LocalDate effectiveFrom =
            LocalDate.of(
                2026
                , 9
                , 5
            );

        LocalDate effectiveTo =
            LocalDate.of(
                2026
                , 10
                , 31
            );

        RiderDeliveryAreaCreateRequest request =
            new RiderDeliveryAreaCreateRequest(
                "DAEGU_JUNG_GU"
                , effectiveFrom
                , effectiveTo
                , true
            );

        RiderDeliveryAreaResponse serviceResponse =
            new RiderDeliveryAreaResponse(
                RIDER_AREA_ID
                , RIDER_ID
                , "DAEGU_JUNG_GU"
                , effectiveFrom
                , effectiveTo
                , true
            );

        when(
            riderDeliveryAreaService.createDeliveryArea(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            )
        ).thenReturn(serviceResponse);

        String requestBody =
            """
            {
              "deliveryAreaCode": "DAEGU_JUNG_GU",
              "effectiveFrom": "2026-09-05",
              "effectiveTo": "2026-10-31",
              "isActive": true
            }
            """;

        // when & then
        mockMvc.perform(
                post("/api/delivery/admin/riders/{riderId}/delivery-areas", RIDER_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"))
            .andExpect(jsonPath("$.data.riderDeliveryAreaId").value(RIDER_AREA_ID))
            .andExpect(jsonPath("$.data.riderId").value(RIDER_ID))
            .andExpect(jsonPath("$.data.deliveryAreaCode").value("DAEGU_JUNG_GU"))
            .andExpect(jsonPath("$.data.effectiveFrom").value("2026-09-05"))
            .andExpect(jsonPath("$.data.effectiveTo").value("2026-10-31"))
            .andExpect(jsonPath("$.data.isActive").value(true));

        verify(riderDeliveryAreaService).createDeliveryArea(
            RIDER_ID
            , ACTOR_ID
            , UserRole.ADMIN
            , request
        );
    }

    @Test
    @DisplayName("필수 값이 누락된 기사 담당 배송 지역 등록 요청은 400을 반환한다")
    void createDeliveryAreaReturnsBadRequestForInvalidRequest() throws Exception {
        // given
        String requestBody =
            """
            {
              "deliveryAreaCode": "DAEGU_JUNG_GU",
              "effectiveTo": "2026-10-31",
              "isActive": true
            }
            """;

        // when & then
        mockMvc.perform(
                post("/api/delivery/admin/riders/{riderId}/delivery-areas", RIDER_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));

        verify(riderDeliveryAreaService, never()).createDeliveryArea(
            any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 기사 담당 배송 지역 목록을 조회할 수 있다")
    void getDeliveryAreasReturnsSuccessForAdmin() throws Exception {
        // given
        List<RiderDeliveryAreaResponse> serviceResponses =
            List.of(
                new RiderDeliveryAreaResponse(
                    300L
                    , RIDER_ID
                    , "DAEGU_JUNG_GU"
                    , LocalDate.of(
                    2026
                    , 9
                    , 1
                )
                    , null
                    , true
                )
                , new RiderDeliveryAreaResponse(
                    301L
                    , RIDER_ID
                    , "DAEGU_SUSEONG_GU"
                    , LocalDate.of(
                    2026
                    , 9
                    , 10
                )
                    , LocalDate.of(
                    2026
                    , 12
                    , 31
                )
                    , false
                )
            );

        when(
            riderDeliveryAreaService.getDeliveryAreas(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
            )
        ).thenReturn(serviceResponses);

        // when & then
        mockMvc.perform(
                get("/api/delivery/admin/riders/{riderId}/delivery-areas", RIDER_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].riderDeliveryAreaId").value(300L))
            .andExpect(jsonPath("$.data[0].deliveryAreaCode").value("DAEGU_JUNG_GU"))
            .andExpect(jsonPath("$.data[0].effectiveFrom").value("2026-09-01"))
            .andExpect(jsonPath("$.data[0].isActive").value(true))
            .andExpect(jsonPath("$.data[1].riderDeliveryAreaId").value(301L))
            .andExpect(jsonPath("$.data[1].deliveryAreaCode").value("DAEGU_SUSEONG_GU"))
            .andExpect(jsonPath("$.data[1].effectiveFrom").value("2026-09-10"))
            .andExpect(jsonPath("$.data[1].effectiveTo").value("2026-12-31"))
            .andExpect(jsonPath("$.data[1].isActive").value(false));

        verify(riderDeliveryAreaService).getDeliveryAreas(
            RIDER_ID
            , ACTOR_ID
            , UserRole.ADMIN
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 기사 담당 배송 지역을 수정할 수 있다")
    void updateDeliveryAreaReturnsSuccessForAdmin() throws Exception {
        // given
        LocalDate effectiveTo =
            LocalDate.of(
                2026
                , 10
                , 31
            );

        RiderDeliveryAreaUpdateRequest request =
            new RiderDeliveryAreaUpdateRequest(
                effectiveTo
                , false
            );

        RiderDeliveryAreaResponse serviceResponse =
            new RiderDeliveryAreaResponse(
                RIDER_AREA_ID
                , RIDER_ID
                , "DAEGU_JUNG_GU"
                , LocalDate.of(
                2026
                , 9
                , 5
            )
                , effectiveTo
                , false
            );

        when(
            riderDeliveryAreaService.updateDeliveryArea(
                RIDER_ID
                , RIDER_AREA_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            )
        ).thenReturn(serviceResponse);

        String requestBody =
            """
            {
              "effectiveTo": "2026-10-31",
              "isActive": false
            }
            """;

        // when & then
        mockMvc.perform(
                patch(
                    "/api/delivery/admin/riders/{riderId}/delivery-areas/{riderAreaId}"
                    , RIDER_ID
                    , RIDER_AREA_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"))
            .andExpect(jsonPath("$.data.riderDeliveryAreaId").value(RIDER_AREA_ID))
            .andExpect(jsonPath("$.data.riderId").value(RIDER_ID))
            .andExpect(jsonPath("$.data.deliveryAreaCode").value("DAEGU_JUNG_GU"))
            .andExpect(jsonPath("$.data.effectiveFrom").value("2026-09-05"))
            .andExpect(jsonPath("$.data.effectiveTo").value("2026-10-31"))
            .andExpect(jsonPath("$.data.isActive").value(false));

        verify(riderDeliveryAreaService).updateDeliveryArea(
            RIDER_ID
            , RIDER_AREA_ID
            , ACTOR_ID
            , UserRole.ADMIN
            , request
        );
    }

    @Test
    @DisplayName("활성 여부가 누락된 기사 담당 배송 지역 수정 요청은 400을 반환한다")
    void updateDeliveryAreaReturnsBadRequestWithoutIsActive() throws Exception {
        // given
        String requestBody =
            """
            {
              "effectiveTo": "2026-10-31"
            }
            """;

        // when & then
        mockMvc.perform(
                patch(
                    "/api/delivery/admin/riders/{riderId}/delivery-areas/{riderAreaId}"
                    , RIDER_ID
                    , RIDER_AREA_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));

        verify(riderDeliveryAreaService, never()).updateDeliveryArea(
            any()
            , any()
            , any()
            , any()
            , any()
        );
    }
}