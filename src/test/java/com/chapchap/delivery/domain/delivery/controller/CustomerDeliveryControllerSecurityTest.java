package com.chapchap.delivery.domain.delivery.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.delivery.service.DeliveryPhotoAccessService;
import com.chapchap.delivery.domain.delivery.service.CustomerDeliveryQueryService;
import com.chapchap.delivery.domain.delivery.response.CustomerDeliveryListResponse;
import java.util.List;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(CustomerDeliveryController.class)
@Import({
    SecurityConfig.class
    , CustomAuthenticationEntryPoint.class
    , CustomAccessDeniedHandler.class
})
class CustomerDeliveryControllerSecurityTest {
    private static final Long USER_ID = 100L;
    private static final String DELIVERY_ID = "delivery-1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryPhotoAccessService photoAccessService;

    @MockitoBean
    private CustomerDeliveryQueryService queryService;

    @Test
    @DisplayName("고객은 본인 배송 목록을 조회할 수 있다")
    void customerCanReadOwnDeliveries() throws Exception {
        when(queryService.getMyDeliveries(
            eq(USER_ID), eq(UserRole.CUSTOMER), any(), any(), any(), any(), any()
        )).thenReturn(new CustomerDeliveryListResponse(List.of(), 0, 20, 0, 0, false));

        mockMvc.perform(
                get("/api/delivery/customer/deliveries")
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", UserRole.CUSTOMER.name())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("기사는 고객 배송 목록을 조회할 수 없다")
    void riderCannotReadCustomerDeliveries() throws Exception {
        mockMvc.perform(
                get("/api/delivery/customer/deliveries")
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
            )
            .andExpect(status().isForbidden());

        verify(queryService, never()).getMyDeliveries(
            any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("고객 완료 사진 접근 API는 미인증 요청을 거절한다")
    void photoAccessReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(
                post(
                    "/api/delivery/customer/deliveries/{deliveryId}/completion-photo/access"
                    , DELIVERY_ID
                )
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.getCode()));

        verify(photoAccessService, never()).forCustomer(any(), any(), any());
    }

    @Test
    @DisplayName("기사는 고객 완료 사진 접근 API를 사용할 수 없다")
    void photoAccessReturnsForbiddenForRider() throws Exception {
        mockMvc.perform(
                post(
                    "/api/delivery/customer/deliveries/{deliveryId}/completion-photo/access"
                    , DELIVERY_ID
                )
                    .header("X-User-Id", USER_ID)
                    .header("X-User-Role", UserRole.RIDER.name())
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.DELIVERY_FORBIDDEN.getCode()));

        verify(photoAccessService, never()).forCustomer(any(), any(), any());
    }
}
