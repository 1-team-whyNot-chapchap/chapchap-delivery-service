package com.chapchap.delivery.domain.rider.controller;

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
@RequestMapping("/api/admin/riders")
public class RiderController {
    private final RiderService riderService;
    private final RiderWeeklyScheduleService riderWeeklyScheduleService;
    private final RiderScheduleExceptionService riderScheduleExceptionService;
    private final RiderDeliveryAreaService riderDeliveryAreaService;

    @PatchMapping("/{riderId}/delivery-active")
    @PreAuthorize("hasRole('ADMIN')")
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