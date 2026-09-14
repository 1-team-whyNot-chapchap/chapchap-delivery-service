package com.chapchap.delivery.domain.riderlocation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.riderlocation.service.CustomerRiderLocationStreamService;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest(CustomerRiderLocationStreamController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class CustomerRiderLocationStreamControllerSecurityTest {
    private static final long USER_ID = 100L;
    private static final String DELIVERY_ID = "delivery-101";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CustomerRiderLocationStreamService streamService;

    @Test
    @DisplayName("고객은 Authorization으로 본인 배송 위치 SSE를 연다")
    void customerCanOpenRiderLocationStream() throws Exception {
        when(streamService.open(USER_ID, UserRole.CUSTOMER, DELIVERY_ID)).thenReturn(new SseEmitter(60_000L));

        mockMvc.perform(get("/api/delivery/customer/deliveries/{deliveryId}/rider-location/stream", DELIVERY_ID)
                .header("X-User-Id", USER_ID)
                .header("X-User-Role", UserRole.CUSTOMER.name())
                .header("Accept", "text/event-stream"))
            .andExpect(request().asyncStarted())
            .andExpect(status().isOk());

        verify(streamService).open(USER_ID, UserRole.CUSTOMER, DELIVERY_ID);
    }

    @Test
    @DisplayName("기사는 고객 위치 SSE를 열 수 없다")
    void riderCannotOpenCustomerStream() throws Exception {
        mockMvc.perform(get("/api/delivery/customer/deliveries/{deliveryId}/rider-location/stream", DELIVERY_ID)
                .header("X-User-Id", USER_ID)
                .header("X-User-Role", UserRole.RIDER.name()))
            .andExpect(status().isForbidden())
            .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                result.getResponse().getContentAsString()
            ).contains(ErrorCode.DELIVERY_FORBIDDEN.getCode()));

        verify(streamService, never()).open(any(), any(), any());
    }
}
