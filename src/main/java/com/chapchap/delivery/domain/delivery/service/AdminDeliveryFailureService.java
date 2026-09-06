package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.constant.DeliveryChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureStage;
import com.chapchap.delivery.domain.delivery.constant.DeliveryProcessedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.request.AdminDeliveryFailureRequest;
import com.chapchap.delivery.domain.delivery.response.RiderDeliveryFailureResponse;
import com.chapchap.delivery.global.exception.business.DeliveryNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryStateConflictException;
import com.chapchap.delivery.global.exception.business.InvalidDeliveryFailureReasonException;
import com.chapchap.delivery.global.kafka.producer.DeliveryEventRequestPublisher;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDeliveryFailureService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryAccessService accessService;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryGroupRepository groupRepository;
    private final DeliveryFailureRepository failureRepository;
    private final DeliveryStatusHistoryRepository historyRepository;
    private final DeliveryExecutionSupport executionSupport;
    private final DeliveryEventRequestPublisher eventPublisher;
    private final EntityManager entityManager;
    private final DeliveryRefundReasonResolver refundReasonResolver;
    private final DeliveryFailureValidator failureValidator;

    public AdminDeliveryFailureService(
        DeliveryAccessService accessService
        , DeliveryRepository deliveryRepository
        , DeliveryGroupRepository groupRepository
        , DeliveryFailureRepository failureRepository
        , DeliveryStatusHistoryRepository historyRepository
        , DeliveryExecutionSupport executionSupport
        , DeliveryEventRequestPublisher eventPublisher
        , EntityManager entityManager
        , DeliveryRefundReasonResolver refundReasonResolver
        , DeliveryFailureValidator failureValidator
    ) {
        this.accessService = accessService;
        this.deliveryRepository = deliveryRepository;
        this.groupRepository = groupRepository;
        this.failureRepository = failureRepository;
        this.historyRepository = historyRepository;
        this.executionSupport = executionSupport;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
        this.refundReasonResolver = refundReasonResolver;
        this.failureValidator = failureValidator;
    }

    @Transactional
    public RiderDeliveryFailureResponse fail(
        Long authUserId, UserRole role, String deliveryPublicId,
        AdminDeliveryFailureRequest request
    ) {
        accessService.validateAdminAccess(authUserId, role);
        validate(request);

        Delivery reference = deliveryRepository.findByDeliveryPublicId(deliveryPublicId)
            .orElseThrow(DeliveryNotFoundException::new);
        DeliveryGroup group = groupRepository.findByIdForUpdate(reference.getDeliveryGroup().getId())
            .orElseThrow(DeliveryNotFoundException::new);
        List<Delivery> deliveries = deliveryRepository.findAllByDeliveryGroupIdForUpdate(group.getId());
        Delivery delivery = deliveries.stream()
            .filter(candidate -> candidate.getDeliveryPublicId().equals(deliveryPublicId))
            .findFirst()
            .orElseThrow(DeliveryNotFoundException::new);

        if (delivery.getStatus() == DeliveryStatus.FAILED) {
            DeliveryFailure existing = failureRepository.findByDeliveryId(delivery.getId())
                .orElseThrow(DeliveryStateConflictException::new);
            if (!same(existing, request)) {
                throw new DeliveryStateConflictException();
            }
            return response(delivery, existing);
        }

        DeliveryStatus expected = expectedStatus(request.failureStage());
        if (delivery.getStatus() != expected
            || deliveryRepository.transitionStatus(delivery.getId(), expected, DeliveryStatus.FAILED) != 1) {
            throw new DeliveryStateConflictException();
        }
        entityManager.refresh(delivery);

        LocalDateTime failedAt = LocalDateTime.now(KST);
        DeliveryFailure failure = failureRepository.save(new DeliveryFailure(
            delivery, request.failureStage(), request.failureCode(), request.failureDetail(),
            toLocal(request.contactAttemptedAt()), request.contactResult(), request.itemRecovered(),
            toLocal(request.recoveredAt()), authUserId, DeliveryProcessedByType.ADMIN,
            request.adminReasonCode(), request.adminReasonDetail(), failedAt
        ));
        historyRepository.save(new DeliveryStatusHistory(
            delivery, expected, DeliveryStatus.FAILED, authUserId,
            DeliveryChangedByType.ADMIN, failedAt
        ));
        executionSupport.recalculateGroup(group, deliveries, failedAt);
        eventPublisher.publishStateChanged("DELIVERY_FAILED", delivery, failedAt);
        eventPublisher.publishRefundConfirmed(
            delivery
            , refundReasonResolver.resolveFailure(request.failureCode())
            , failedAt
        );
        return response(delivery, failure);
    }

    private DeliveryStatus expectedStatus(DeliveryFailureStage stage) {
        return stage == DeliveryFailureStage.BEFORE_DEPARTURE
            ? DeliveryStatus.READY : DeliveryStatus.DELIVERING;
    }

    private void validate(AdminDeliveryFailureRequest request) {
        failureValidator.validate(
            request.failureCode()
            , request.failureDetail()
            , request.contactAttemptedAt()
            , request.contactResult()
            , request.itemRecovered()
            , request.recoveredAt()
        );
        if ("OTHER".equals(request.adminReasonCode()) && blank(request.adminReasonDetail())) {
            throw new InvalidDeliveryFailureReasonException();
        }
    }

    private boolean same(DeliveryFailure f, AdminDeliveryFailureRequest r) {
        return f.getFailureStage() == r.failureStage() && f.getFailureCode() == r.failureCode()
            && Objects.equals(f.getFailureDetail(), r.failureDetail())
            && Objects.equals(f.getContactAttemptedAt(), toLocal(r.contactAttemptedAt()))
            && Objects.equals(f.getContactResult(), r.contactResult())
            && Objects.equals(f.getItemRecovered(), r.itemRecovered())
            && Objects.equals(f.getRecoveredAt(), toLocal(r.recoveredAt()))
            && Objects.equals(f.getAdminReasonCode(), r.adminReasonCode())
            && Objects.equals(f.getAdminReasonDetail(), r.adminReasonDetail());
    }

    private LocalDateTime toLocal(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(KST).toLocalDateTime();
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    private RiderDeliveryFailureResponse response(Delivery d, DeliveryFailure f) {
        return new RiderDeliveryFailureResponse(
            d.getDeliveryPublicId(), d.getStatus(), d.getDeliveryVersion(), f.getFailureCode(),
            f.getFailedAt().atZone(KST).toOffsetDateTime()
        );
    }
}
