package com.chapchap.delivery.domain.rider.controller;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueCode;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentType;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentIssue;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentDeliveryItemResponse;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentDetailResponse;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentListItemResponse;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentListResponse;
import com.chapchap.delivery.domain.assignment.service.RiderAssignmentAcknowledgementService;
import com.chapchap.delivery.domain.assignment.service.RiderAssignmentDetailService;
import com.chapchap.delivery.domain.assignment.service.RiderAssignmentIssueService;
import com.chapchap.delivery.domain.assignment.service.RiderAssignmentQueryService;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private static final Long ASSIGNMENT_ID = 1L;
    private static final Long ISSUE_ID = 10L;
    private static final LocalDate DATE_FROM = LocalDate.of(2026, 8, 24);
    private static final LocalDate DATE_TO = LocalDate.of(2026, 8, 25);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiderScheduleService riderScheduleService;

    @MockitoBean
    private RiderAssignmentAcknowledgementService riderAssignmentAcknowledgementService;

    @MockitoBean
    private RiderAssignmentIssueService riderAssignmentIssueService;

    @MockitoBean
    private RiderAssignmentQueryService riderAssignmentQueryService;

    @MockitoBean
    private RiderAssignmentDetailService riderAssignmentDetailService;

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

        when(
            riderScheduleService.getMySchedules(
                ACTOR_ID
                , UserRole.RIDER
                , DATE_FROM
                , DATE_TO
            )
        )
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

    @Test
    @DisplayName("인증 정보가 없으면 본인 배정 목록을 조회할 수 없다")
    void getMyAssignmentsReturnsUnauthorizedWithoutAuthentication() throws Exception {
        // when & then
        mockMvc.perform(
                get("/api/rider/me/assignments")
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));

        verify(riderAssignmentQueryService, never()).getMyAssignments(
            any()
            , any()
            , any()
            , any()
            , any(Pageable.class)
        );
    }

    @Test
    @DisplayName("CUSTOMER 역할은 기사 본인 배정 목록 조회 API에 접근할 수 없다")
    void getMyAssignmentsReturnsForbiddenForCustomer() throws Exception {
        // when & then
        mockMvc.perform(
                get("/api/rider/me/assignments")
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.CUSTOMER.name())
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(riderAssignmentQueryService, never()).getMyAssignments(
            any()
            , any()
            , any()
            , any()
            , any(Pageable.class)
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 기사 본인 배정 목록 조회 API에 접근할 수 없다")
    void getMyAssignmentsReturnsForbiddenForAdmin() throws Exception {
        // when & then
        mockMvc.perform(
                get("/api/rider/me/assignments")
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(riderAssignmentQueryService, never()).getMyAssignments(
            any()
            , any()
            , any()
            , any()
            , any(Pageable.class)
        );
    }

    @Test
    @DisplayName("RIDER 역할은 본인의 배정 목록을 조회할 수 있다")
    void getMyAssignmentsReturnsSuccessForRider() throws Exception {
        // given
        LocalDate deliveryDate =
            LocalDate.of(2026, 9, 5);

        RiderAssignmentListItemResponse item =
            new RiderAssignmentListItemResponse(
                ASSIGNMENT_ID
                , 10L
                , deliveryDate
                , DeliverySlotCode.LUNCH
                , DeliveryAssignmentType.AUTO
                , DeliveryAssignmentStatus.ASSIGNED
                , OffsetDateTime.parse("2026-09-04T16:10:00+09:00")
                , OffsetDateTime.parse("2026-09-05T07:00:00+09:00")
                , null
                , 8
                , 36
                , false
                , false
            );

        RiderAssignmentListResponse serviceResponse =
            new RiderAssignmentListResponse(
                List.of(item)
                , 0
                , 20
                , 1
                , 1
                , false
            );

        when(
            riderAssignmentQueryService.getMyAssignments(
                eq(ACTOR_ID)
                , eq(deliveryDate)
                , eq(DeliverySlotCode.LUNCH)
                , eq(DeliveryAssignmentStatus.ASSIGNED)
                , any(Pageable.class)
            )
        )
            .thenReturn(serviceResponse);

        // when & then
        mockMvc.perform(
                get("/api/rider/me/assignments")
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
                    .param("deliveryDate", "2026-09-05")
                    .param("deliverySlot", "LUNCH")
                    .param("status", "ASSIGNED")
                    .param("page", "0")
                    .param("size", "20")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].assignmentId").value(ASSIGNMENT_ID))
            .andExpect(jsonPath("$.data.items[0].deliveryGroupId").value(10L))
            .andExpect(jsonPath("$.data.items[0].deliveryDate").value("2026-09-05"))
            .andExpect(jsonPath("$.data.items[0].deliverySlot").value("LUNCH"))
            .andExpect(jsonPath("$.data.items[0].assignmentType").value("AUTO"))
            .andExpect(jsonPath("$.data.items[0].status").value("ASSIGNED"))
            .andExpect(jsonPath("$.data.items[0].stopCount").value(8))
            .andExpect(jsonPath("$.data.items[0].lunchboxQuantity").value(36))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(20))
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.totalPages").value(1))
            .andExpect(jsonPath("$.data.hasNext").value(false));

        verify(riderAssignmentQueryService).getMyAssignments(
            eq(ACTOR_ID)
            , eq(deliveryDate)
            , eq(DeliverySlotCode.LUNCH)
            , eq(DeliveryAssignmentStatus.ASSIGNED)
            , any(Pageable.class)
        );
    }

    @Test
    @DisplayName("인증 정보가 없으면 본인 배정을 확인할 수 없다")
    void acknowledgeAssignmentReturnsUnauthorizedWithoutAuthentication() throws Exception {
        // when & then
        mockMvc.perform(
                post(
                    "/api/rider/me/assignments/{assignmentId}/acknowledgement"
                    , ASSIGNMENT_ID
                )
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));

        verify(riderAssignmentAcknowledgementService, never()).acknowledge(
            any()
            , any()
        );
    }

    @Test
    @DisplayName("CUSTOMER 역할은 기사 본인 배정 확인 API에 접근할 수 없다")
    void acknowledgeAssignmentReturnsForbiddenForCustomer() throws Exception {
        // when & then
        mockMvc.perform(
                post(
                    "/api/rider/me/assignments/{assignmentId}/acknowledgement"
                    , ASSIGNMENT_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.CUSTOMER.name())
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(riderAssignmentAcknowledgementService, never()).acknowledge(
            any()
            , any()
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 기사 본인 배정 확인 API에 접근할 수 없다")
    void acknowledgeAssignmentReturnsForbiddenForAdmin() throws Exception {
        // when & then
        mockMvc.perform(
                post(
                    "/api/rider/me/assignments/{assignmentId}/acknowledgement"
                    , ASSIGNMENT_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(riderAssignmentAcknowledgementService, never()).acknowledge(
            any()
            , any()
        );
    }

    @Test
    @DisplayName("RIDER 역할은 본인의 배정을 확인할 수 있다")
    void acknowledgeAssignmentReturnsSuccessForRider() throws Exception {
        // given
        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        when(
            riderAssignmentAcknowledgementService.acknowledge(
                ACTOR_ID
                , ASSIGNMENT_ID
            )
        )
            .thenReturn(assignment);

        when(assignment.getId())
            .thenReturn(ASSIGNMENT_ID);

        when(assignment.getStatus())
            .thenReturn(
                DeliveryAssignmentStatus.ACKNOWLEDGED
            );

        when(assignment.getAcknowledgedAt())
            .thenReturn(
                LocalDateTime.of(
                    2026
                    , 9
                    , 5
                    , 7
                    , 30
                )
            );

        // when & then
        mockMvc.perform(
                post(
                    "/api/rider/me/assignments/{assignmentId}/acknowledgement"
                    , ASSIGNMENT_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"))
            .andExpect(jsonPath("$.data.assignmentId").value(ASSIGNMENT_ID))
            .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"))
            .andExpect(jsonPath("$.data.acknowledgedAt").value("2026-09-05T07:30:00+09:00"));

        verify(riderAssignmentAcknowledgementService).acknowledge(
            ACTOR_ID
            , ASSIGNMENT_ID
        );
    }

    @Test
    @DisplayName("인증 정보가 없으면 본인 배정에 이슈를 제기할 수 없다")
    void reportAssignmentIssueReturnsUnauthorizedWithoutAuthentication() throws Exception {
        // when & then
        mockMvc.perform(
                post(
                    "/api/rider/me/assignments/{assignmentId}/issues"
                    , ASSIGNMENT_ID
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "issueCode": "SCHEDULE_CONFLICT",
                          "issueDetail": "당일 일정 충돌"
                        }
                    """)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));

        verify(riderAssignmentIssueService, never()).reportIssue(
            any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("CUSTOMER 역할은 기사 본인 배정 이슈 제기 API에 접근할 수 없다")
    void reportAssignmentIssueReturnsForbiddenForCustomer() throws Exception {
        // when & then
        mockMvc.perform(
                post(
                    "/api/rider/me/assignments/{assignmentId}/issues"
                    , ASSIGNMENT_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.CUSTOMER.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "issueCode": "SCHEDULE_CONFLICT",
                          "issueDetail": "당일 일정 충돌"
                        }
                    """)
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(riderAssignmentIssueService, never()).reportIssue(
            any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 기사 본인 배정 이슈 제기 API에 접근할 수 없다")
    void reportAssignmentIssueReturnsForbiddenForAdmin() throws Exception {
        // when & then
        mockMvc.perform(
                post(
                    "/api/rider/me/assignments/{assignmentId}/issues"
                    , ASSIGNMENT_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "issueCode": "SCHEDULE_CONFLICT",
                          "issueDetail": "당일 일정 충돌"
                        }
                    """)
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(riderAssignmentIssueService, never()).reportIssue(
            any()
            , any()
            , any()
            , any()
        );
    }

    @Test
    @DisplayName("RIDER 역할은 본인의 배정에 이슈를 제기할 수 있다")
    void reportAssignmentIssueReturnsSuccessForRider() throws Exception {
        // given
        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        DeliveryAssignmentIssue issue =
            mock(DeliveryAssignmentIssue.class);

        when(
            riderAssignmentIssueService.reportIssue(
                ACTOR_ID
                , ASSIGNMENT_ID
                , DeliveryAssignmentIssueCode.SCHEDULE_CONFLICT
                , "당일 일정 충돌"
            )
        )
            .thenReturn(issue);

        when(issue.getId())
            .thenReturn(ISSUE_ID);

        when(issue.getAssignment())
            .thenReturn(assignment);

        when(assignment.getId())
            .thenReturn(ASSIGNMENT_ID);

        when(assignment.getStatus())
            .thenReturn(
                DeliveryAssignmentStatus.ISSUE_REPORTED
            );

        when(issue.getIssueCode())
            .thenReturn(
                DeliveryAssignmentIssueCode.SCHEDULE_CONFLICT
            );

        when(issue.getIssueDetail())
            .thenReturn(
                "당일 일정 충돌"
            );

        when(issue.getReportedAt())
            .thenReturn(
                LocalDateTime.of(
                    2026
                    , 9
                    , 5
                    , 7
                    , 40
                )
            );

        // when & then
        mockMvc.perform(
                post(
                    "/api/rider/me/assignments/{assignmentId}/issues"
                    , ASSIGNMENT_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "issueCode": "SCHEDULE_CONFLICT",
                          "issueDetail": "당일 일정 충돌"
                        }
                    """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"))
            .andExpect(jsonPath("$.data.issueId").value(ISSUE_ID))
            .andExpect(jsonPath("$.data.assignmentId").value(ASSIGNMENT_ID))
            .andExpect(jsonPath("$.data.assignmentStatus").value("ISSUE_REPORTED"))
            .andExpect(jsonPath("$.data.issueCode").value("SCHEDULE_CONFLICT"))
            .andExpect(jsonPath("$.data.issueDetail").value("당일 일정 충돌"))
            .andExpect(jsonPath("$.data.reportedAt").value("2026-09-05T07:40:00+09:00"));

        verify(riderAssignmentIssueService).reportIssue(
            ACTOR_ID
            , ASSIGNMENT_ID
            , DeliveryAssignmentIssueCode.SCHEDULE_CONFLICT
            , "당일 일정 충돌"
        );
    }

    @Test
    @DisplayName("인증 정보가 없으면 본인 배정 상세를 조회할 수 없다")
    void getMyAssignmentDetailReturnsUnauthorizedWithoutAuthentication() throws Exception {
        // when & then
        mockMvc.perform(
                get(
                    "/api/rider/me/assignments/{assignmentId}"
                    , ASSIGNMENT_ID
                )
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));

        verify(riderAssignmentDetailService, never()).getMyAssignmentDetail(
            any()
            , any()
        );
    }

    @Test
    @DisplayName("CUSTOMER 역할은 기사 본인 배정 상세 조회 API에 접근할 수 없다")
    void getMyAssignmentDetailReturnsForbiddenForCustomer() throws Exception {
        // when & then
        mockMvc.perform(
                get(
                    "/api/rider/me/assignments/{assignmentId}"
                    , ASSIGNMENT_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.CUSTOMER.name())
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(riderAssignmentDetailService, never()).getMyAssignmentDetail(
            any()
            , any()
        );
    }

    @Test
    @DisplayName("ADMIN 역할은 기사 본인 배정 상세 조회 API에 접근할 수 없다")
    void getMyAssignmentDetailReturnsForbiddenForAdmin() throws Exception {
        // when & then
        mockMvc.perform(
                get(
                    "/api/rider/me/assignments/{assignmentId}"
                    , ASSIGNMENT_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(riderAssignmentDetailService, never()).getMyAssignmentDetail(
            any()
            , any()
        );
    }

    @Test
    @DisplayName("RIDER 역할은 본인의 배정 상세를 조회할 수 있다")
    void getMyAssignmentDetailReturnsSuccessForRider() throws Exception {
        // given
        RiderAssignmentDeliveryItemResponse delivery =
            new RiderAssignmentDeliveryItemResponse(
                "11111111-1111-1111-1111-111111111111"
                , DeliveryStatus.READY
                , 2
                , "닭가슴살 도시락"
                , "홍길동"
                , "01012345678"
                , "06236"
                , "서울특별시 강남구 테헤란로 123"
                , "101동 1001호"
                , "공동현관 1234"
                , null
                , "문 앞에 놓아주세요."
                , true
            );

        RiderAssignmentDetailResponse serviceResponse =
            new RiderAssignmentDetailResponse(
                ASSIGNMENT_ID
                , DeliveryAssignmentStatus.ACKNOWLEDGED
                , LocalDate.of(2026, 9, 5)
                , DeliverySlotCode.LUNCH
                , 1
                , 2
                , List.of(delivery)
            );

        when(
            riderAssignmentDetailService.getMyAssignmentDetail(
                ACTOR_ID
                , ASSIGNMENT_ID
            )
        )
            .thenReturn(serviceResponse);

        // when & then
        mockMvc.perform(
                get(
                    "/api/rider/me/assignments/{assignmentId}"
                    , ASSIGNMENT_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"))
            .andExpect(jsonPath("$.data.assignmentId").value(ASSIGNMENT_ID))
            .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"))
            .andExpect(jsonPath("$.data.deliveryDate").value("2026-09-05"))
            .andExpect(jsonPath("$.data.deliverySlot").value("LUNCH"))
            .andExpect(jsonPath("$.data.stopCount").value(1))
            .andExpect(jsonPath("$.data.lunchboxQuantity").value(2))
            .andExpect(jsonPath("$.data.deliveries.length()").value(1))
            .andExpect(
                jsonPath("$.data.deliveries[0].deliveryId")
                    .value("11111111-1111-1111-1111-111111111111")
            )
            .andExpect(jsonPath("$.data.deliveries[0].status").value("READY"))
            .andExpect(jsonPath("$.data.deliveries[0].lunchboxQuantity").value(2))
            .andExpect(jsonPath("$.data.deliveries[0].menuName").value("닭가슴살 도시락"))
            .andExpect(jsonPath("$.data.deliveries[0].recipientName").value("홍길동"))
            .andExpect(jsonPath("$.data.deliveries[0].recipientPhone").value("01012345678"))
            .andExpect(jsonPath("$.data.deliveries[0].postalCode").value("06236"))
            .andExpect(
                jsonPath("$.data.deliveries[0].addressLine1")
                    .value("서울특별시 강남구 테헤란로 123")
            )
            .andExpect(jsonPath("$.data.deliveries[0].addressLine2").value("101동 1001호"))
            .andExpect(
                jsonPath("$.data.deliveries[0].entranceInformation")
                    .value("공동현관 1234")
            )
            .andExpect(jsonPath("$.data.deliveries[0].otherRequest").value("문 앞에 놓아주세요."))
            .andExpect(jsonPath("$.data.deliveries[0].termsAgreed").value(true));

        verify(riderAssignmentDetailService).getMyAssignmentDetail(
            ACTOR_ID
            , ASSIGNMENT_ID
        );
    }
}