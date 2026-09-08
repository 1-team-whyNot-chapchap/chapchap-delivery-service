package com.chapchap.delivery.domain.delivery.controller;

import static com.chapchap.delivery.global.exception.ErrorCode.COMPLETION_PHOTO_REQUIRED;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_HANDOFF_INFO_REQUIRED;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_NOT_FOUND;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_RESULT_CORRECTION_NO_CHANGE;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_RESULT_NOT_CORRECTABLE;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_STATE_CONFLICT;
import static com.chapchap.delivery.global.exception.ErrorCode.INVALID_DELIVERY_FAILURE_REASON;
import static com.chapchap.delivery.global.exception.ErrorCode.INVALID_DELIVERY_INFO;
import static com.chapchap.delivery.global.exception.ErrorCode.INVALID_DELIVERY_PHOTO_INFO;
import static com.chapchap.delivery.global.exception.ErrorCode.OTHER_REASON_DETAIL_REQUIRED;

import com.chapchap.delivery.domain.delivery.request.AdminDeliveryFailureRequest;
import com.chapchap.delivery.domain.delivery.request.AdminDeliveryRecoveryRequest;
import com.chapchap.delivery.domain.delivery.request.AdminDeliveryResultCorrectionRequest;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryRecoveryResponse;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryResultCorrectionResponse;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryDetailResponse;
import com.chapchap.delivery.domain.delivery.response.DeliveryPhotoAccessResponse;
import com.chapchap.delivery.domain.delivery.response.RiderDeliveryFailureResponse;
import com.chapchap.delivery.domain.delivery.service.AdminDeliveryFailureService;
import com.chapchap.delivery.domain.delivery.service.AdminDeliveryRecoveryService;
import com.chapchap.delivery.domain.delivery.service.AdminDeliveryQueryService;
import com.chapchap.delivery.domain.delivery.service.AdminDeliveryResultCorrectionService;
import com.chapchap.delivery.domain.delivery.service.DeliveryPhotoAccessService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.openapi.ApiErrorCodes;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/admin/deliveries")
@Tag(name = "Admin Delivery", description = "고객별 도시락 배송의 조회, 복구, 실패 처리와 결과 정정을 관리합니다.")
public class AdminDeliveryController {
    private final AdminDeliveryFailureService failureService;
    private final DeliveryPhotoAccessService photoAccessService;
    private final AdminDeliveryRecoveryService recoveryService;
    private final AdminDeliveryQueryService queryService;
    private final AdminDeliveryResultCorrectionService correctionService;

    @GetMapping("/{deliveryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Delivery", description = "고객별 도시락 배송의 상태, 기사 배정과 처리 이력을 상세 조회합니다.")
    @ApiErrorCodes({DELIVERY_NOT_FOUND, INVALID_DELIVERY_INFO})
    public ApiResponse<AdminDeliveryDetailResponse> getDelivery(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable String deliveryId
    ) {
        return ApiResponse.success(
            queryService.getDelivery(user.userId(), user.role(), deliveryId)
        );
    }

    @PostMapping("/{deliveryId}/completion-photo/access")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Issue Completion Photo Access URL", description = "현재 유효한 도시락 배송 완료 사진의 10분 만료 접근 URL을 발급합니다.")
    @ApiErrorCodes({DELIVERY_NOT_FOUND, COMPLETION_PHOTO_REQUIRED})
    public ApiResponse<DeliveryPhotoAccessResponse> photoAccess(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable String deliveryId
    ) {
        return ApiResponse.success(
            photoAccessService.forAdmin(
                user.userId()
                , user.role()
                , deliveryId
            )
        );
    }

    @PostMapping(
        value = "/{deliveryId}/recovery"
        , consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Recover Delivery", description = "운영 장애로 처리하지 못한 도시락 배송을 완료 또는 실패 상태로 복구합니다.")
    @ApiErrorCodes({
        DELIVERY_NOT_FOUND,
        INVALID_DELIVERY_INFO,
        INVALID_DELIVERY_PHOTO_INFO,
        DELIVERY_HANDOFF_INFO_REQUIRED,
        INVALID_DELIVERY_FAILURE_REASON,
        DELIVERY_STATE_CONFLICT
    })
    public ApiResponse<AdminDeliveryRecoveryResponse> recover(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable String deliveryId
        , @Valid @RequestPart("request") AdminDeliveryRecoveryRequest request
        , @RequestPart(value = "photo", required = false) MultipartFile photo
    ) {
        return ApiResponse.success(
            recoveryService.recover(
                user.userId()
                , user.role()
                , deliveryId
                , request
                , photo
            )
        );
    }

    @PostMapping("/{deliveryId}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fail Delivery", description = "관리자가 도시락 배송을 실패로 처리하고 실패 사유를 기록합니다.")
    @ApiErrorCodes({
        DELIVERY_NOT_FOUND,
        INVALID_DELIVERY_FAILURE_REASON,
        DELIVERY_STATE_CONFLICT
    })
    public ApiResponse<RiderDeliveryFailureResponse> fail(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable String deliveryId
        , @Valid @RequestBody AdminDeliveryFailureRequest request
    ) {
        return ApiResponse.success(
            failureService.fail(user.userId(), user.role(), deliveryId, request)
        );
    }

    @PostMapping("/{deliveryId}/completion-corrections")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Correct Completion Result", description = "완료된 도시락 배송 결과의 정정 이력을 추가합니다.")
    @ApiErrorCodes({
        DELIVERY_NOT_FOUND,
        DELIVERY_RESULT_NOT_CORRECTABLE,
        DELIVERY_RESULT_CORRECTION_NO_CHANGE,
        OTHER_REASON_DETAIL_REQUIRED
    })
    public ApiResponse<AdminDeliveryResultCorrectionResponse> correctCompletion(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable String deliveryId
        , @Valid @RequestBody AdminDeliveryResultCorrectionRequest request
    ) {
        return ApiResponse.success(correctionService.correctCompletion(
            user.userId(), user.role(), deliveryId, request
        ));
    }

    @PostMapping("/{deliveryId}/failure-corrections")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Correct Failure Result", description = "실패한 도시락 배송 결과의 정정 이력을 추가합니다.")
    @ApiErrorCodes({
        DELIVERY_NOT_FOUND,
        DELIVERY_RESULT_NOT_CORRECTABLE,
        DELIVERY_RESULT_CORRECTION_NO_CHANGE,
        OTHER_REASON_DETAIL_REQUIRED
    })
    public ApiResponse<AdminDeliveryResultCorrectionResponse> correctFailure(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable String deliveryId
        , @Valid @RequestBody AdminDeliveryResultCorrectionRequest request
    ) {
        return ApiResponse.success(correctionService.correctFailure(
            user.userId(), user.role(), deliveryId, request
        ));
    }
}
