package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentAcknowledgementTime;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentCapacity;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.repository.RiderAssignmentListProjection;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentListItemResponse;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentListResponse;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
public class RiderAssignmentQueryService {
    private static final ZoneId KST =
        ZoneId.of("Asia/Seoul");

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryAccessService deliveryAccessService;

    public RiderAssignmentQueryService(
        DeliveryAssignmentRepository deliveryAssignmentRepository
        , DeliveryAccessService deliveryAccessService
    ) {
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.deliveryAccessService = deliveryAccessService;
    }

    @Transactional(readOnly = true)
    public RiderAssignmentListResponse getMyAssignments(
        Long authUserId
        , LocalDate deliveryDate
        , DeliverySlotCode deliverySlot
        , DeliveryAssignmentStatus status
        , Pageable pageable
    ) {
        validateRiderAccess(
            authUserId
        );

        Page<RiderAssignmentListItemResponse> page =
            deliveryAssignmentRepository.findAllMine(
                    authUserId
                    , deliveryDate
                    , deliverySlot
                    , status
                    , pageable
                )
                .map(
                    this::toResponse
                );

        return RiderAssignmentListResponse.from(
            page
        );
    }

    private void validateRiderAccess(Long authUserId) {
        if (!deliveryAccessService.isRiderAccessAllowed(authUserId)) {
            throw new DeliveryAccessForbiddenException();
        }
    }

    private RiderAssignmentListItemResponse toResponse(
        RiderAssignmentListProjection projection
    ) {
        int stopCount =
            Math.toIntExact(
                projection.getStopCount()
            );

        int lunchboxQuantity =
            Math.toIntExact(
                projection.getLunchboxQuantity()
            );

        return new RiderAssignmentListItemResponse(
            projection.getAssignmentId()
            , projection.getDeliveryGroupId()
            , projection.getDeliveryDate()
            , projection.getDeliverySlot()
            , projection.getAssignmentType()
            , projection.getStatus()
            , toOffsetDateTime(
            projection.getAssignedAt()
        )
            , getAcknowledgementAvailableAt(
            projection.getDeliveryDate()
            , projection.getDeliverySlot()
        )
            , toOffsetDateTime(
            projection.getAcknowledgedAt()
        )
            , stopCount
            , lunchboxQuantity
            , isRecommendedCapacityExceeded(
            stopCount
            , lunchboxQuantity
        )
            , isMaximumCapacityExceeded(
            stopCount
            , lunchboxQuantity
        )
        );
    }

    private OffsetDateTime getAcknowledgementAvailableAt(
        LocalDate deliveryDate
        , DeliverySlotCode deliverySlot
    ) {
        LocalTime start =
            DeliveryAssignmentAcknowledgementTime.getStart(
                deliverySlot
            );

        return LocalDateTime.of(
                deliveryDate
                , start
            )
            .atZone(KST)
            .toOffsetDateTime();
    }

    private OffsetDateTime toOffsetDateTime(
        LocalDateTime dateTime
    ) {
        if (dateTime == null) {
            return null;
        }

        return dateTime
            .atZone(KST)
            .toOffsetDateTime();
    }

    private boolean isRecommendedCapacityExceeded(
        int stopCount
        , int lunchboxQuantity
    ) {
        return stopCount
            > DeliveryAssignmentCapacity.RECOMMENDED_VISIT_COUNT
            || lunchboxQuantity
            > DeliveryAssignmentCapacity.RECOMMENDED_LUNCHBOX_QUANTITY;
    }

    private boolean isMaximumCapacityExceeded(
        int stopCount
        , int lunchboxQuantity
    ) {
        return stopCount
            > DeliveryAssignmentCapacity.MAX_VISIT_COUNT
            || lunchboxQuantity
            > DeliveryAssignmentCapacity.MAX_LUNCHBOX_QUANTITY;
    }
}