package com.chapchap.delivery.domain.assignment.controller;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.assignment.service.AdminAssignmentIssueReassignService;
import com.chapchap.delivery.domain.assignment.service.AdminAssignmentIssueRejectService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAssignmentIssueController.class)
@Import({
    SecurityConfig.class
    , CustomAuthenticationEntryPoint.class
    , CustomAccessDeniedHandler.class
})
class AdminAssignmentIssueControllerSecurityTest {

    private static final Long ACTOR_ID = 9001L;
    private static final Long ISSUE_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAssignmentIssueRejectService adminAssignmentIssueRejectService;

    @MockitoBean
    private AdminAssignmentIssueReassignService adminAssignmentIssueReassignService;

    @Test
    @DisplayName("인증 정보가 없으면 배정 이슈를 반려할 수 없다")
    void rejectAssignmentIssueReturnsUnauthorizedWithoutAuthentication() throws Exception {
        // given
        String requestBody =
            """
            {
              "reasonDetail": "기존 배정을 유지합니다."
            }
            """;

        // when & then
        mockMvc.perform(
                post(
                    "/api/admin/assignment-issues/{issueId}/reject"
                    , ISSUE_ID
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));

        verify(
            adminAssignmentIssueRejectService
            , never()
        )
            .rejectIssue(
                any()
                , any()
                , any()
                , any()
            );
    }

    @Test
    @DisplayName("RIDER 역할은 관리자 배정 이슈 반려 API에 접근할 수 없다")
    void rejectAssignmentIssueReturnsForbiddenForRider() throws Exception {
        // given
        String requestBody =
            """
            {
              "reasonDetail": "기존 배정을 유지합니다."
            }
            """;

        // when & then
        mockMvc.perform(
                post(
                    "/api/admin/assignment-issues/{issueId}/reject"
                    , ISSUE_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(
            adminAssignmentIssueRejectService
            , never()
        )
            .rejectIssue(
                any()
                , any()
                , any()
                , any()
            );
    }

    @Test
    @DisplayName("CUSTOMER 역할은 관리자 배정 이슈 반려 API에 접근할 수 없다")
    void rejectAssignmentIssueReturnsForbiddenForCustomer() throws Exception {
        // given
        String requestBody =
            """
            {
              "reasonDetail": "기존 배정을 유지합니다."
            }
            """;

        // when & then
        mockMvc.perform(
                post(
                    "/api/admin/assignment-issues/{issueId}/reject"
                    , ISSUE_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.CUSTOMER.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(
            adminAssignmentIssueRejectService
            , never()
        )
            .rejectIssue(
                any()
                , any()
                , any()
                , any()
            );
    }

    @Test
    @DisplayName("ADMIN 역할은 배정 이슈를 반려할 수 있다")
    void rejectAssignmentIssueReturnsSuccessForAdmin() throws Exception {
        // given
        String requestBody =
            """
            {
              "reasonDetail": "기존 배정을 유지합니다."
            }
            """;

        // when & then
        mockMvc.perform(
                post(
                    "/api/admin/assignment-issues/{issueId}/reject"
                    , ISSUE_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"));

        verify(adminAssignmentIssueRejectService)
            .rejectIssue(
                ACTOR_ID
                , UserRole.ADMIN
                , ISSUE_ID
                , "기존 배정을 유지합니다."
            );
    }

    @Test
    @DisplayName("반려 설명이 공백이면 400을 반환한다")
    void rejectAssignmentIssueReturnsBadRequestForBlankReasonDetail() throws Exception {
        // given
        String requestBody =
            """
            {
              "reasonDetail": "   "
            }
            """;

        // when & then
        mockMvc.perform(
                post(
                    "/api/admin/assignment-issues/{issueId}/reject"
                    , ISSUE_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));

        verify(
            adminAssignmentIssueRejectService
            , never()
        )
            .rejectIssue(
                any()
                , any()
                , any()
                , any()
            );
    }

    @Test
    @DisplayName("인증 정보가 없으면 배정 이슈를 재배정할 수 없다")
    void reassignAssignmentIssueReturnsUnauthorizedWithoutAuthentication() throws Exception {
        // given
        String requestBody =
            """
            {
              "newRiderId": 20,
              "reasonCode": "OTHER",
              "reasonDetail": "차량 사용 불가로 다른 기사에게 재배정합니다."
            }
            """;

        // when & then
        mockMvc.perform(
                post(
                    "/api/admin/assignment-issues/{issueId}/reassign"
                    , ISSUE_ID
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));

        verify(
            adminAssignmentIssueReassignService
            , never()
        )
            .reassignIssue(
                any()
                , any()
                , any()
                , any()
                , any()
                , any()
            );
    }

    @Test
    @DisplayName("RIDER 역할은 관리자 배정 이슈 재배정 API에 접근할 수 없다")
    void reassignAssignmentIssueReturnsForbiddenForRider() throws Exception {
        // given
        String requestBody =
            """
            {
              "newRiderId": 20,
              "reasonCode": "OTHER",
              "reasonDetail": "차량 사용 불가로 다른 기사에게 재배정합니다."
            }
            """;

        // when & then
        mockMvc.perform(
                post(
                    "/api/admin/assignment-issues/{issueId}/reassign"
                    , ISSUE_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(
            adminAssignmentIssueReassignService
            , never()
        )
            .reassignIssue(
                any()
                , any()
                , any()
                , any()
                , any()
                , any()
            );
    }

    @Test
    @DisplayName("CUSTOMER 역할은 관리자 배정 이슈 재배정 API에 접근할 수 없다")
    void reassignAssignmentIssueReturnsForbiddenForCustomer() throws Exception {
        // given
        String requestBody =
            """
            {
              "newRiderId": 20,
              "reasonCode": "OTHER",
              "reasonDetail": "차량 사용 불가로 다른 기사에게 재배정합니다."
            }
            """;

        // when & then
        mockMvc.perform(
                post(
                    "/api/admin/assignment-issues/{issueId}/reassign"
                    , ISSUE_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.CUSTOMER.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.DELIVERY_FORBIDDEN.getMessage()));

        verify(
            adminAssignmentIssueReassignService
            , never()
        )
            .reassignIssue(
                any()
                , any()
                , any()
                , any()
                , any()
                , any()
            );
    }

    @Test
    @DisplayName("ADMIN 역할은 배정 이슈를 다른 기사에게 재배정할 수 있다")
    void reassignAssignmentIssueReturnsSuccessForAdmin() throws Exception {
        // given
        Long newRiderId =
            20L;

        String requestBody =
            """
            {
              "newRiderId": 20,
              "reasonCode": "OTHER",
              "reasonDetail": "차량 사용 불가로 다른 기사에게 재배정합니다."
            }
            """;

        // when & then
        mockMvc.perform(
                post(
                    "/api/admin/assignment-issues/{issueId}/reassign"
                    , ISSUE_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00"))
            .andExpect(jsonPath("$.message").value("SUCCESS"));

        verify(adminAssignmentIssueReassignService)
            .reassignIssue(
                ACTOR_ID
                , UserRole.ADMIN
                , ISSUE_ID
                , newRiderId
                , "OTHER"
                , "차량 사용 불가로 다른 기사에게 재배정합니다."
            );
    }

    @Test
    @DisplayName("재배정 사유 코드가 공백이면 400을 반환한다")
    void reassignAssignmentIssueReturnsBadRequestForBlankReasonCode() throws Exception {
        // given
        String requestBody =
            """
            {
              "newRiderId": 20,
              "reasonCode": "   ",
              "reasonDetail": "재배정합니다."
            }
            """;

        // when & then
        mockMvc.perform(
                post(
                    "/api/admin/assignment-issues/{issueId}/reassign"
                    , ISSUE_ID
                )
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_REQUEST.getMessage()));

        verify(
            adminAssignmentIssueReassignService
            , never()
        )
            .reassignIssue(
                any()
                , any()
                , any()
                , any()
                , any()
                , any()
            );
    }
}