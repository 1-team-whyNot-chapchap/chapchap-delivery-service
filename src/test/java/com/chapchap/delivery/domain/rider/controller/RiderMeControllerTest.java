package com.chapchap.delivery.domain.rider.controller;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.rider.constant.RiderScheduleSource;
import com.chapchap.delivery.domain.rider.response.RiderScheduleItemResponse;
import com.chapchap.delivery.domain.rider.response.RiderScheduleResponse;
import com.chapchap.delivery.domain.rider.service.RiderScheduleService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderMeControllerTest {

    private static final Long RIDER_ID = 10L;
    private static final Long ACTOR_ID = 10001L;
    private static final LocalDate DATE_FROM = LocalDate.of(2026, 8, 24);
    private static final LocalDate DATE_TO = LocalDate.of(2026, 8, 25);

    @Mock
    private RiderScheduleService riderScheduleService;

    @InjectMocks
    private RiderMeController riderMeController;

    @Test
    @DisplayName("기사가 본인의 실제 일정 목록을 조회한다")
    void getMySchedules() {
        // given
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(ACTOR_ID, UserRole.RIDER);

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

        // when
        ApiResponse<RiderScheduleResponse> response =
            riderMeController.getMySchedules(authenticatedUser, DATE_FROM, DATE_TO);

        // then
        verify(riderScheduleService).getMySchedules(ACTOR_ID, UserRole.RIDER, DATE_FROM, DATE_TO);

        assertThat(response.code()).isEqualTo("00");
        assertThat(response.message()).isEqualTo("SUCCESS");
        assertThat(response.data()).isEqualTo(serviceResponse);
        assertThat(response.data().riderId()).isEqualTo(RIDER_ID);
        assertThat(response.data().isDeliveryActive()).isTrue();
        assertThat(response.data().schedules()).hasSize(2);

        assertThat(response.data().schedules().get(0).date())
            .isEqualTo(LocalDate.of(2026, 8, 24));

        assertThat(response.data().schedules().get(0).deliverySlot())
            .isEqualTo(DeliverySlotCode.LUNCH);

        assertThat(response.data().schedules().get(0).isWorking())
            .isFalse();

        assertThat(response.data().schedules().get(0).source())
            .isEqualTo(RiderScheduleSource.DATE_EXCEPTION);

        assertThat(response.data().schedules().get(1).date())
            .isEqualTo(LocalDate.of(2026, 8, 25));

        assertThat(response.data().schedules().get(1).deliverySlot())
            .isEqualTo(DeliverySlotCode.DINNER);

        assertThat(response.data().schedules().get(1).isWorking())
            .isTrue();

        assertThat(response.data().schedules().get(1).source())
            .isEqualTo(RiderScheduleSource.WEEKLY_DEFAULT);
    }
}