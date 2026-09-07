package com.chapchap.delivery.domain.assignment.controller;

import com.chapchap.delivery.domain.assignment.request.AdminEmergencyRiderReplacementRequest;
import com.chapchap.delivery.domain.assignment.response.AdminEmergencyRiderReplacementResponse;
import com.chapchap.delivery.domain.assignment.service.AdminEmergencyRiderReplacementService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/admin/assignments")
public class AdminAssignmentController {
    private final AdminEmergencyRiderReplacementService replacementService;

    @PostMapping("/{assignmentId}/emergency-rider-replacement")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminEmergencyRiderReplacementResponse> replaceRider(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable Long assignmentId
        , @Valid @RequestBody AdminEmergencyRiderReplacementRequest request
    ) {
        return ApiResponse.success(
            replacementService.replace(
                user.userId(), user.role(), assignmentId, request
            )
        );
    }
}
