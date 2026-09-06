package com.chapchap.delivery.domain.assignment.controller;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.assignment.response.DeliveryGroupConfirmationResponse;
import com.chapchap.delivery.domain.assignment.response.ManualAssignmentsResponse;
import com.chapchap.delivery.domain.assignment.service.AdminAutoAssignmentService;
import com.chapchap.delivery.domain.assignment.service.AdminDeliveryGroupConfirmationService;
import com.chapchap.delivery.domain.assignment.service.AdminManualAssignmentService;
import com.chapchap.delivery.domain.delivery.service.AdminDeliveryQueryService;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryGroupListResponse;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.global.exception.ErrorCode;
import com.chapchap.delivery.global.security.CustomAccessDeniedHandler;
import com.chapchap.delivery.global.security.CustomAuthenticationEntryPoint;
import com.chapchap.delivery.global.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDeliveryGroupController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class AdminDeliveryGroupControllerSecurityTest {
    private static final Long ACTOR_ID = 1L;
    private static final Long GROUP_ID = 10L;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminAutoAssignmentService adminAutoAssignmentService;
    @MockitoBean private AdminManualAssignmentService adminManualAssignmentService;
    @MockitoBean private AdminDeliveryGroupConfirmationService adminDeliveryGroupConfirmationService;
    @MockitoBean private AdminDeliveryQueryService adminDeliveryQueryService;

    @Test
    @DisplayName("관리자는 전체 배송 목록을 조회할 수 있다")
    void adminCanReadDeliveryGroups() throws Exception {
        when(adminDeliveryQueryService.getDeliveryGroups(
            eq(ACTOR_ID), eq(UserRole.ADMIN), any(), any(), any(), any()
        )).thenReturn(new AdminDeliveryGroupListResponse(List.of(), 0, 20, 0, 0, false));

        mockMvc.perform(
                get("/api/delivery/admin/delivery-groups")
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", UserRole.ADMIN.name())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void rejectsUnauthenticatedManualAssignment() throws Exception {
        mockMvc.perform(post("/api/delivery/admin/delivery-groups/{id}/manual-assignments", GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assignments\":[]}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()));

        verify(adminManualAssignmentService, never()).assign(any(), any(), any(), any());
    }

    @Test
    void adminCanRunAutoAssignment() throws Exception {
        when(adminAutoAssignmentService.assign(ACTOR_ID, UserRole.ADMIN, GROUP_ID)).thenReturn(true);

        mockMvc.perform(post("/api/delivery/admin/delivery-groups/{id}/auto-assignment", GROUP_ID)
                .header("X-User-Id", ACTOR_ID)
                .header("X-User-Role", UserRole.ADMIN.name()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));

        verify(adminAutoAssignmentService).assign(ACTOR_ID, UserRole.ADMIN, GROUP_ID);
    }

    @Test
    void adminCanCreateValidManualAssignments() throws Exception {
        when(adminManualAssignmentService.assign(any(), any(), any(), any())).thenReturn(
            new ManualAssignmentsResponse(GROUP_ID, DeliveryGroupStatus.WAITING_RIDER, List.of(30L))
        );

        mockMvc.perform(post("/api/delivery/admin/delivery-groups/{id}/manual-assignments", GROUP_ID)
                .header("X-User-Id", ACTOR_ID)
                .header("X-User-Role", UserRole.ADMIN.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"assignments":[{"riderId":20,"deliveryIds":["0198c004-1000-7000-8000-000000000901"],"areaException":false}]}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("WAITING_RIDER"));
    }

    @Test
    void adminCanConfirmDeliveryGroup() throws Exception {
        when(adminDeliveryGroupConfirmationService.confirm(ACTOR_ID, UserRole.ADMIN, GROUP_ID)).thenReturn(
            new DeliveryGroupConfirmationResponse(
                GROUP_ID, DeliveryGroupStatus.CONFIRMED,
                OffsetDateTime.parse("2026-09-07T09:30:00+09:00"), ACTOR_ID
            )
        );

        mockMvc.perform(post("/api/delivery/admin/delivery-groups/{id}/confirmation", GROUP_ID)
                .header("X-User-Id", ACTOR_ID)
                .header("X-User-Role", UserRole.ADMIN.name()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void rejectsManualAssignmentWithInvalidDeliveryPublicId() throws Exception {
        mockMvc.perform(post("/api/delivery/admin/delivery-groups/{id}/manual-assignments", GROUP_ID)
                .header("X-User-Id", ACTOR_ID)
                .header("X-User-Role", UserRole.ADMIN.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"assignments":[{"riderId":20,"deliveryIds":["not-a-uuid"],"areaException":false}]}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REQUEST.getCode()));

        verify(adminManualAssignmentService, never()).assign(any(), any(), any(), any());
    }
}
