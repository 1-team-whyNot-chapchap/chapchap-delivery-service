package com.chapchap.delivery.domain.rider.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.audit.constant.AuditActorType;
import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.rider.constant.RiderDeliveryActiveReason;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.rider.request.RiderUpdateRequest;
import com.chapchap.delivery.global.exception.business.OptimisticLockConflictException;
import com.chapchap.delivery.global.exception.business.OtherReasonDetailRequiredException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RiderService {

    private static final ZoneId KST =
        ZoneId.of("Asia/Seoul");

    private static final String RIDER_ENTITY_TYPE =
        "RIDER";

    private static final String DELIVERY_ACTIVE_CHANGED_ACTION =
        "DELIVERY_ACTIVE_CHANGED";

    private final RiderRepository riderRepository;
    private final AuditHistoryRepository auditHistoryRepository;

    private final DeliveryAccessService deliveryAccessService;

    @Transactional
    public void changeDeliveryActive(
        Long riderId
        , Long actorId
        , UserRole actorRole
        , RiderUpdateRequest request
    ) {
        deliveryAccessService.validateAdminAccess(
            actorId
            , actorRole
        );

        Rider rider =
            riderRepository
                .findByIdAndDeletedAtIsNull(riderId)
                .orElseThrow(RiderNotFoundException::new);

        validateVersion(
            rider
            , request.version()
        );

        validateReason(
            request.reasonCode()
            , request.reasonDetail()
        );

        if (Objects.equals(
            rider.getIsDeliveryActive()
            , request.isDeliveryActive()
        )) {
            return;
        }

        boolean beforeDeliveryActive =
            rider.getIsDeliveryActive();

        rider.changeDeliveryActive(
            request.isDeliveryActive()
        );

        LocalDateTime occurredAt =
            LocalDateTime.now(KST);

        AuditHistory auditHistory =
            AuditHistory.record(
                RIDER_ENTITY_TYPE
                , rider.getId()
                , DELIVERY_ACTIVE_CHANGED_ACTION
                , actorId
                , AuditActorType.ADMIN
                , request.reasonCode().name()
                , normalizeReasonDetail(
                    request.reasonDetail()
                )
                , createDeliveryActiveJson(
                    beforeDeliveryActive
                )
                , createDeliveryActiveJson(
                    request.isDeliveryActive()
                )
                , occurredAt
            );

        auditHistoryRepository.save(auditHistory);
    }

    private void validateVersion(
        Rider rider
        , Long requestVersion
    ) {
        if (!Objects.equals(
            rider.getVersion()
            , requestVersion
        )) {
            throw new OptimisticLockConflictException();
        }
    }

    private void validateReason(
        RiderDeliveryActiveReason reasonCode
        , String reasonDetail
    ) {
        if (
            reasonCode == RiderDeliveryActiveReason.OTHER
                && (
                reasonDetail == null
                    || reasonDetail.isBlank()
            )
        ) {
            throw new OtherReasonDetailRequiredException();
        }
    }

    private String normalizeReasonDetail(
        String reasonDetail
    ) {
        if (reasonDetail == null) {
            return null;
        }

        String normalized =
            reasonDetail.trim();

        return normalized.isEmpty()
            ? null
            : normalized;
    }

    private String createDeliveryActiveJson(
        boolean isDeliveryActive
    ) {
        return """
            {"isDeliveryActive":%s}
            """.formatted(isDeliveryActive)
            .trim();
    }
}