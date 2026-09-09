package com.chapchap.delivery.domain.rider.controller;

import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_STATE_CONFLICT;
import static com.chapchap.delivery.global.exception.ErrorCode.INVALID_DELIVERY_INFO;
import static com.chapchap.delivery.global.exception.ErrorCode.OPTIMISTIC_LOCK_CONFLICT;
import static com.chapchap.delivery.global.exception.ErrorCode.OTHER_REASON_DETAIL_REQUIRED;
import static com.chapchap.delivery.global.exception.ErrorCode.RESOURCE_NOT_FOUND;
import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_NOT_FOUND;
import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_LEAVE_MANAGED_SCHEDULE_EXCEPTION;

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
import com.chapchap.delivery.global.openapi.ApiErrorCodes;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/admin/riders")
@Tag(name = "Admin Rider", description = "도시락 배송 기사의 배달 활성 상태, 근무 일정과 담당 지역을 관리합니다.")
public class RiderController {
    private final RiderService riderService;
    private final RiderWeeklyScheduleService riderWeeklyScheduleService;
    private final RiderScheduleExceptionService riderScheduleExceptionService;
    private final RiderDeliveryAreaService riderDeliveryAreaService;

    @PatchMapping("/{riderId}/delivery-active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Rider Delivery Availability", description = "기사의 도시락 배달 업무 가능 여부를 활성화하거나 비활성화합니다.")
    @ApiErrorCodes({RIDER_NOT_FOUND, OTHER_REASON_DETAIL_REQUIRED, OPTIMISTIC_LOCK_CONFLICT})
    public ApiResponse<Void> changeDeliveryActive(
        @PathVariable Long riderId
        , @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @Valid @RequestBody RiderUpdateRequest request
    ) {
        riderService.changeDeliveryActive(
            riderId
            , authenticatedUser.userId()
            , authenticatedUser.role()
            , request
        );

        return ApiResponse.success(null);
    }

    @PostMapping("/{riderId}/weekly-schedules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Rider Weekly Schedule", description = "도시락 배송 기사의 반복 주간 근무 시간대를 등록합니다.")
    @ApiErrorCodes({RIDER_NOT_FOUND, INVALID_DELIVERY_INFO})
    public ApiResponse<RiderWeeklyScheduleResponse> createWeeklySchedule(
        @PathVariable Long riderId
        , @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @Valid @RequestBody RiderWeeklyScheduleCreateRequest request
    ) {
        RiderWeeklyScheduleResponse response =
            riderWeeklyScheduleService.createWeeklySchedule(
                riderId
                , authenticatedUser.userId()
                , authenticatedUser.role()
                , request
            );

        return ApiResponse.success(response);
    }

    @GetMapping("/{riderId}/weekly-schedules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Rider Weekly Schedules", description = "도시락 배송 기사에게 등록된 반복 주간 근무 일정을 조회합니다.")
    @ApiErrorCodes(RIDER_NOT_FOUND)
    public ApiResponse<List<RiderWeeklyScheduleResponse>> getWeeklySchedules(
        @PathVariable Long riderId
        , @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<RiderWeeklyScheduleResponse> responses =
            riderWeeklyScheduleService.getWeeklySchedules(
                riderId
                , authenticatedUser.userId()
                , authenticatedUser.role()
            );

        return ApiResponse.success(responses);
    }

    @DeleteMapping("/{riderId}/weekly-schedules/{scheduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Rider Weekly Schedule", description = "도시락 배송 기사의 반복 주간 근무 일정 한 건을 삭제합니다.")
    @ApiErrorCodes({RIDER_NOT_FOUND, RESOURCE_NOT_FOUND})
    public ApiResponse<Void> deleteWeeklySchedule(
        @PathVariable Long riderId
        , @PathVariable Long scheduleId
        , @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        riderWeeklyScheduleService.deleteWeeklySchedule(
            riderId
            , scheduleId
            , authenticatedUser.userId()
            , authenticatedUser.role()
        );

        return ApiResponse.success(null);
    }

    @PostMapping("/{riderId}/schedule-exceptions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Rider Schedule Exception", description = "특정 날짜에 적용할 도시락 배송 기사의 근무 예외 일정을 등록합니다.")
    @ApiErrorCodes({
        RIDER_NOT_FOUND,
        INVALID_DELIVERY_INFO,
        DELIVERY_STATE_CONFLICT,
        OTHER_REASON_DETAIL_REQUIRED,
        OPTIMISTIC_LOCK_CONFLICT
    })
    public ApiResponse<RiderScheduleExceptionResponse> createScheduleException(
        @PathVariable Long riderId
        , @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @Valid @RequestBody RiderScheduleExceptionCreateRequest request
    ) {
        RiderScheduleExceptionResponse response =
            riderScheduleExceptionService.createScheduleException(
                riderId
                , authenticatedUser.userId()
                , authenticatedUser.role()
                , request
            );

        return ApiResponse.success(response);
    }

    @GetMapping("/{riderId}/schedule-exceptions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Rider Schedule Exceptions", description = "조회 기간에 포함된 도시락 배송 기사의 근무 예외 일정을 조회합니다.")
    @ApiErrorCodes(RIDER_NOT_FOUND)
    public ApiResponse<List<RiderScheduleExceptionResponse>> getScheduleExceptions(
        @PathVariable Long riderId
        , @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom
        , @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
        , @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<RiderScheduleExceptionResponse> responses =
            riderScheduleExceptionService.getScheduleExceptions(
                riderId
                , authenticatedUser.userId()
                , authenticatedUser.role()
                , dateFrom
                , dateTo
            );

        return ApiResponse.success(responses);
    }

    @PatchMapping("/{riderId}/schedule-exceptions/{exceptionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Rider Schedule Exception", description = "등록된 도시락 배송 기사 근무 예외 일정의 내용을 수정합니다.")
    @ApiErrorCodes({
        RIDER_NOT_FOUND,
        RESOURCE_NOT_FOUND,
        INVALID_DELIVERY_INFO,
        DELIVERY_STATE_CONFLICT,
        OTHER_REASON_DETAIL_REQUIRED,
        OPTIMISTIC_LOCK_CONFLICT,
        RIDER_LEAVE_MANAGED_SCHEDULE_EXCEPTION
    })
    public ApiResponse<RiderScheduleExceptionResponse> updateScheduleException(
        @PathVariable Long riderId
        , @PathVariable Long exceptionId
        , @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @Valid @RequestBody RiderScheduleExceptionUpdateRequest request
    ) {
        RiderScheduleExceptionResponse response =
            riderScheduleExceptionService.updateScheduleException(
                riderId
                , exceptionId
                , authenticatedUser.userId()
                , authenticatedUser.role()
                , request
            );

        return ApiResponse.success(response);
    }

    @DeleteMapping("/{riderId}/schedule-exceptions/{exceptionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Rider Schedule Exception", description = "등록된 도시락 배송 기사 근무 예외 일정 한 건을 삭제합니다.")
    @ApiErrorCodes({RIDER_NOT_FOUND, RESOURCE_NOT_FOUND, OPTIMISTIC_LOCK_CONFLICT, RIDER_LEAVE_MANAGED_SCHEDULE_EXCEPTION})
    public ApiResponse<Void> deleteScheduleException(
        @PathVariable Long riderId
        , @PathVariable Long exceptionId
        , @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        riderScheduleExceptionService.deleteScheduleException(
            riderId
            , exceptionId
            , authenticatedUser.userId()
            , authenticatedUser.role()
        );

        return ApiResponse.success(null);
    }

    @PostMapping("/{riderId}/delivery-areas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Rider Delivery Area", description = "기사에게 도시락 배송을 담당할 지역을 등록합니다.")
    @ApiErrorCodes({RIDER_NOT_FOUND, INVALID_DELIVERY_INFO, DELIVERY_STATE_CONFLICT})
    public ApiResponse<RiderDeliveryAreaResponse> createDeliveryArea(
        @PathVariable Long riderId
        , @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @Valid @RequestBody RiderDeliveryAreaCreateRequest request
    ) {
        RiderDeliveryAreaResponse response =
            riderDeliveryAreaService.createDeliveryArea(
                riderId
                , authenticatedUser.userId()
                , authenticatedUser.role()
                , request
            );

        return ApiResponse.success(response);
    }

    @GetMapping("/{riderId}/delivery-areas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Rider Delivery Areas", description = "기사에게 등록된 도시락 배송 담당 지역을 조회합니다.")
    @ApiErrorCodes(RIDER_NOT_FOUND)
    public ApiResponse<List<RiderDeliveryAreaResponse>> getDeliveryAreas(
        @PathVariable Long riderId
        , @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<RiderDeliveryAreaResponse> responses =
            riderDeliveryAreaService.getDeliveryAreas(
                riderId
                , authenticatedUser.userId()
                , authenticatedUser.role()
            );

        return ApiResponse.success(responses);
    }

    @PatchMapping("/{riderId}/delivery-areas/{riderAreaId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Rider Delivery Area", description = "기사의 도시락 배송 담당 지역 활성 상태 등 관리 정보를 수정합니다.")
    @ApiErrorCodes({RIDER_NOT_FOUND, RESOURCE_NOT_FOUND, INVALID_DELIVERY_INFO, DELIVERY_STATE_CONFLICT})
    public ApiResponse<RiderDeliveryAreaResponse> updateDeliveryArea(
        @PathVariable Long riderId
        , @PathVariable Long riderAreaId
        , @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @Valid @RequestBody RiderDeliveryAreaUpdateRequest request
    ) {
        RiderDeliveryAreaResponse response =
            riderDeliveryAreaService.updateDeliveryArea(
                riderId
                , riderAreaId
                , authenticatedUser.userId()
                , authenticatedUser.role()
                , request
            );

        return ApiResponse.success(response);
    }
}
