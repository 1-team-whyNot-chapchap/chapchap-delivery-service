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
class RiderControllerTest {
    private static final Long RIDER_ID = 10L;
    private static final Long ACTOR_ID = 9001L;

    @Mock
    private RiderService riderService;

    @Mock
    private RiderWeeklyScheduleService riderWeeklyScheduleService;

    @Mock
    private RiderScheduleExceptionService riderScheduleExceptionService;

    @Mock
    private RiderDeliveryAreaService riderDeliveryAreaService;

    @InjectMocks
    private RiderController riderController;

    @Test
    @DisplayName("관리자가 기사의 배달 활성 상태를 변경한다")
    void changeDeliveryActive() {
        // given
        AuthenticatedUser authenticatedUser =
            createAdminUser();

        RiderUpdateRequest request =
            new RiderUpdateRequest(
                false
                , 3L
                , RiderDeliveryActiveReason.TRAINING
                , "신규 기사 현장 교육"
            );

        // when
        ApiResponse<Void> response =
            riderController.changeDeliveryActive(
                RIDER_ID
                , authenticatedUser
                , request
            );

        // then
        verify(riderService)
            .changeDeliveryActive(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            );

        assertThat(response.code())
            .isEqualTo("00");

        assertThat(response.message())
            .isEqualTo("SUCCESS");

        assertThat(response.data())
            .isNull();
    }

    @Test
    @DisplayName("관리자가 기사의 주간 기본 일정을 등록한다")
    void createWeeklySchedule() {
        // given
        AuthenticatedUser authenticatedUser =
            createAdminUser();

        RiderWeeklyScheduleCreateRequest request =
            new RiderWeeklyScheduleCreateRequest(
                (byte) 1
                , DeliverySlotCode.LUNCH
            );

        RiderWeeklyScheduleResponse serviceResponse =
            new RiderWeeklyScheduleResponse(
                100L
                , RIDER_ID
                , (byte) 1
                , DeliverySlotCode.LUNCH
            );

        when(
            riderWeeklyScheduleService.createWeeklySchedule(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            )
        ).thenReturn(
            serviceResponse
        );

        // when
        ApiResponse<RiderWeeklyScheduleResponse> response =
            riderController.createWeeklySchedule(
                RIDER_ID
                , authenticatedUser
                , request
            );

        // then
        verify(riderWeeklyScheduleService)
            .createWeeklySchedule(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            );

        assertThat(response.code())
            .isEqualTo("00");

        assertThat(response.message())
            .isEqualTo("SUCCESS");

        assertThat(response.data())
            .isEqualTo(serviceResponse);
    }

    @Test
    @DisplayName("관리자가 기사의 주간 기본 일정 목록을 조회한다")
    void getWeeklySchedules() {
        // given
        AuthenticatedUser authenticatedUser =
            createAdminUser();

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
        ).thenReturn(
            serviceResponses
        );

        // when
        ApiResponse<List<RiderWeeklyScheduleResponse>> response =
            riderController.getWeeklySchedules(
                RIDER_ID
                , authenticatedUser
            );

        // then
        verify(riderWeeklyScheduleService)
            .getWeeklySchedules(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
            );

        assertThat(response.code())
            .isEqualTo("00");

        assertThat(response.message())
            .isEqualTo("SUCCESS");

        assertThat(response.data())
            .isEqualTo(serviceResponses);

        assertThat(response.data())
            .hasSize(2);
    }

    @Test
    @DisplayName("관리자가 기사의 주간 기본 일정을 삭제한다")
    void deleteWeeklySchedule() {
        // given
        Long scheduleId =
            100L;

        AuthenticatedUser authenticatedUser =
            createAdminUser();

        // when
        ApiResponse<Void> response =
            riderController.deleteWeeklySchedule(
                RIDER_ID
                , scheduleId
                , authenticatedUser
            );

        // then
        verify(riderWeeklyScheduleService)
            .deleteWeeklySchedule(
                RIDER_ID
                , scheduleId
                , ACTOR_ID
                , UserRole.ADMIN
            );

        assertThat(response.code())
            .isEqualTo("00");

        assertThat(response.message())
            .isEqualTo("SUCCESS");

        assertThat(response.data())
            .isNull();
    }

    @Test
    @DisplayName("관리자가 기사의 날짜별 예외 일정을 등록한다")
    void createScheduleException() {
        // given
        AuthenticatedUser authenticatedUser =
            createAdminUser();

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
                200L
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
        ).thenReturn(
            serviceResponse
        );

        // when
        ApiResponse<RiderScheduleExceptionResponse> response =
            riderController.createScheduleException(
                RIDER_ID
                , authenticatedUser
                , request
            );

        // then
        verify(riderScheduleExceptionService)
            .createScheduleException(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            );

        assertThat(response.code())
            .isEqualTo("00");

        assertThat(response.message())
            .isEqualTo("SUCCESS");

        assertThat(response.data())
            .isEqualTo(serviceResponse);

        assertThat(response.data().exceptionId())
            .isEqualTo(200L);

        assertThat(response.data().scheduleDate())
            .isEqualTo(scheduleDate);

        assertThat(response.data().deliverySlot())
            .isEqualTo(DeliverySlotCode.LUNCH);
    }

    @Test
    @DisplayName("관리자가 기사의 날짜별 예외 일정 목록을 조회한다")
    void getScheduleExceptions() {
        // given
        AuthenticatedUser authenticatedUser =
            createAdminUser();

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
        ).thenReturn(
            serviceResponses
        );

        // when
        ApiResponse<List<RiderScheduleExceptionResponse>> response =
            riderController.getScheduleExceptions(
                RIDER_ID
                , dateFrom
                , dateTo
                , authenticatedUser
            );

        // then
        verify(riderScheduleExceptionService)
            .getScheduleExceptions(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , dateFrom
                , dateTo
            );

        assertThat(response.code())
            .isEqualTo("00");

        assertThat(response.message())
            .isEqualTo("SUCCESS");

        assertThat(response.data())
            .isEqualTo(serviceResponses);

        assertThat(response.data())
            .hasSize(2);
    }

    @Test
    @DisplayName("관리자가 기사의 날짜별 예외 일정을 수정한다")
    void updateScheduleException() {
        // given
        Long exceptionId =
            200L;

        AuthenticatedUser authenticatedUser =
            createAdminUser();

        LocalDate scheduleDate =
            LocalDate.of(
                2026
                , 9
                , 5
            );

        RiderScheduleExceptionUpdateRequest request =
            new RiderScheduleExceptionUpdateRequest(
                true
                , RiderScheduleExceptionReason.SUBSTITUTE_WORK
                , "대체 근무로 변경"
                , 0L
            );

        RiderScheduleExceptionResponse serviceResponse =
            new RiderScheduleExceptionResponse(
                exceptionId
                , RIDER_ID
                , scheduleDate
                , DeliverySlotCode.LUNCH
                , true
                , RiderScheduleExceptionReason.SUBSTITUTE_WORK
                , "대체 근무로 변경"
                , 1L
            );

        when(
            riderScheduleExceptionService.updateScheduleException(
                RIDER_ID
                , exceptionId
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            )
        ).thenReturn(
            serviceResponse
        );

        // when
        ApiResponse<RiderScheduleExceptionResponse> response =
            riderController.updateScheduleException(
                RIDER_ID
                , exceptionId
                , authenticatedUser
                , request
            );

        // then
        verify(riderScheduleExceptionService)
            .updateScheduleException(
                RIDER_ID
                , exceptionId
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            );

        assertThat(response.code())
            .isEqualTo("00");

        assertThat(response.message())
            .isEqualTo("SUCCESS");

        assertThat(response.data())
            .isEqualTo(serviceResponse);

        assertThat(response.data().isWorking())
            .isTrue();

        assertThat(response.data().reasonCode())
            .isEqualTo(RiderScheduleExceptionReason.SUBSTITUTE_WORK);

        assertThat(response.data().version())
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("관리자가 기사의 날짜별 예외 일정을 삭제한다")
    void deleteScheduleException() {
        // given
        Long exceptionId =
            200L;

        AuthenticatedUser authenticatedUser =
            createAdminUser();

        // when
        ApiResponse<Void> response =
            riderController.deleteScheduleException(
                RIDER_ID
                , exceptionId
                , authenticatedUser
            );

        // then
        verify(riderScheduleExceptionService)
            .deleteScheduleException(
                RIDER_ID
                , exceptionId
                , ACTOR_ID
                , UserRole.ADMIN
            );

        assertThat(response.code())
            .isEqualTo("00");

        assertThat(response.message())
            .isEqualTo("SUCCESS");

        assertThat(response.data())
            .isNull();
    }

    @Test
    @DisplayName("관리자가 기사의 담당 배송 지역을 등록한다")
    void createDeliveryArea() {
        // given
        AuthenticatedUser authenticatedUser =
            createAdminUser();

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
                300L
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
        ).thenReturn(
            serviceResponse
        );

        // when
        ApiResponse<RiderDeliveryAreaResponse> response =
            riderController.createDeliveryArea(
                RIDER_ID
                , authenticatedUser
                , request
            );

        // then
        verify(riderDeliveryAreaService)
            .createDeliveryArea(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            );

        assertThat(response.code())
            .isEqualTo("00");

        assertThat(response.message())
            .isEqualTo("SUCCESS");

        assertThat(response.data())
            .isEqualTo(serviceResponse);

        assertThat(response.data().riderDeliveryAreaId())
            .isEqualTo(300L);

        assertThat(response.data().deliveryAreaCode())
            .isEqualTo("DAEGU_JUNG_GU");

        assertThat(response.data().effectiveFrom())
            .isEqualTo(effectiveFrom);

        assertThat(response.data().effectiveTo())
            .isEqualTo(effectiveTo);

        assertThat(response.data().isActive())
            .isTrue();
    }

    @Test
    @DisplayName("관리자가 기사의 담당 배송 지역 목록을 조회한다")
    void getDeliveryAreas() {
        // given
        AuthenticatedUser authenticatedUser =
            createAdminUser();

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
        ).thenReturn(
            serviceResponses
        );

        // when
        ApiResponse<List<RiderDeliveryAreaResponse>> response =
            riderController.getDeliveryAreas(
                RIDER_ID
                , authenticatedUser
            );

        // then
        verify(riderDeliveryAreaService)
            .getDeliveryAreas(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
            );

        assertThat(response.code())
            .isEqualTo("00");

        assertThat(response.message())
            .isEqualTo("SUCCESS");

        assertThat(response.data())
            .isEqualTo(serviceResponses);

        assertThat(response.data())
            .hasSize(2);

        assertThat(response.data().get(0).deliveryAreaCode())
            .isEqualTo("DAEGU_JUNG_GU");

        assertThat(response.data().get(1).deliveryAreaCode())
            .isEqualTo("DAEGU_SUSEONG_GU");
    }

    @Test
    @DisplayName("관리자가 기사의 담당 배송 지역을 수정한다")
    void updateDeliveryArea() {
        // given
        Long riderAreaId =
            300L;

        AuthenticatedUser authenticatedUser =
            createAdminUser();

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

        RiderDeliveryAreaUpdateRequest request =
            new RiderDeliveryAreaUpdateRequest(
                effectiveTo
                , false
            );

        RiderDeliveryAreaResponse serviceResponse =
            new RiderDeliveryAreaResponse(
                riderAreaId
                , RIDER_ID
                , "DAEGU_JUNG_GU"
                , effectiveFrom
                , effectiveTo
                , false
            );

        when(
            riderDeliveryAreaService.updateDeliveryArea(
                RIDER_ID
                , riderAreaId
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            )
        ).thenReturn(
            serviceResponse
        );

        // when
        ApiResponse<RiderDeliveryAreaResponse> response =
            riderController.updateDeliveryArea(
                RIDER_ID
                , riderAreaId
                , authenticatedUser
                , request
            );

        // then
        verify(riderDeliveryAreaService)
            .updateDeliveryArea(
                RIDER_ID
                , riderAreaId
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            );

        assertThat(response.code())
            .isEqualTo("00");

        assertThat(response.message())
            .isEqualTo("SUCCESS");

        assertThat(response.data())
            .isEqualTo(serviceResponse);

        assertThat(response.data().riderDeliveryAreaId())
            .isEqualTo(riderAreaId);

        assertThat(response.data().effectiveTo())
            .isEqualTo(effectiveTo);

        assertThat(response.data().isActive())
            .isFalse();
    }

    private AuthenticatedUser createAdminUser() {
        return new AuthenticatedUser(
            ACTOR_ID
            , UserRole.ADMIN
        );
    }
}