package com.chapchap.delivery.domain.rider.controller;

import com.chapchap.delivery.domain.rider.response.RiderScheduleResponse;
import com.chapchap.delivery.domain.rider.service.RiderScheduleService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rider/me")
public class RiderMeController {

    private final RiderScheduleService riderScheduleService;

    @GetMapping("/schedules")
    @PreAuthorize("hasRole('RIDER')")
    public ApiResponse<RiderScheduleResponse> getMySchedules(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom
        , @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        RiderScheduleResponse response =
            riderScheduleService.getMySchedules(
                authenticatedUser.userId()
                , authenticatedUser.role()
                , dateFrom
                , dateTo
            );

        return ApiResponse.success(response);
    }
}