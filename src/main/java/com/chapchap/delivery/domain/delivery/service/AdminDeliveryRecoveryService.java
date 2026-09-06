package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureStage;
import com.chapchap.delivery.domain.delivery.constant.DeliveryProcessedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryRecoveryResult;
import com.chapchap.delivery.domain.delivery.constant.DeliveryRefundReason;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryAdminRecovery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletion;
import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryAdminRecoveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.request.AdminDeliveryRecoveryRequest;
import com.chapchap.delivery.domain.delivery.request.RiderDeliveryCompletionRequest;
import com.chapchap.delivery.domain.delivery.request.RiderDeliveryFailureRequest;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryRecoveryResponse;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryHandoffInfoRequiredException;
import com.chapchap.delivery.global.exception.business.DeliveryNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryStateConflictException;
import com.chapchap.delivery.global.exception.business.InvalidDeliveryFailureReasonException;
import com.chapchap.delivery.global.exception.business.InvalidDeliveryInfoException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import com.chapchap.delivery.global.kafka.producer.DeliveryEventRequestPublisher;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AdminDeliveryRecoveryService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryAccessService accessService;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryGroupRepository groupRepository;
    private final RiderRepository riderRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryAssignmentItemRepository assignmentItemRepository;
    private final DeliveryAdminRecoveryRepository recoveryRepository;
    private final DeliveryCompletionRepository completionRepository;
    private final DeliveryFailureRepository failureRepository;
    private final DeliveryStatusHistoryRepository historyRepository;
    private final DeliveryExecutionSupport executionSupport;
    private final DeliveryFailureValidator failureValidator;
    private final DeliveryRefundReasonResolver refundReasonResolver;
    private final DeliveryDelayService delayService;
    private final DeliveryEventRequestPublisher eventPublisher;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public AdminDeliveryRecoveryService(
        DeliveryAccessService accessService
        , DeliveryRepository deliveryRepository
        , DeliveryGroupRepository groupRepository
        , RiderRepository riderRepository
        , DeliveryAssignmentRepository assignmentRepository
        , DeliveryAssignmentItemRepository assignmentItemRepository
        , DeliveryAdminRecoveryRepository recoveryRepository
        , DeliveryCompletionRepository completionRepository
        , DeliveryFailureRepository failureRepository
        , DeliveryStatusHistoryRepository historyRepository
        , DeliveryExecutionSupport executionSupport
        , DeliveryFailureValidator failureValidator
        , DeliveryRefundReasonResolver refundReasonResolver
        , DeliveryDelayService delayService
        , DeliveryEventRequestPublisher eventPublisher
        , EntityManager entityManager
        , TransactionTemplate transactionTemplate
    ) {
        this.accessService = accessService;
        this.deliveryRepository = deliveryRepository;
        this.groupRepository = groupRepository;
        this.riderRepository = riderRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentItemRepository = assignmentItemRepository;
        this.recoveryRepository = recoveryRepository;
        this.completionRepository = completionRepository;
        this.failureRepository = failureRepository;
        this.historyRepository = historyRepository;
        this.executionSupport = executionSupport;
        this.failureValidator = failureValidator;
        this.refundReasonResolver = refundReasonResolver;
        this.delayService = delayService;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
    }

    public AdminDeliveryRecoveryResponse recover(
        Long adminId
        , UserRole role
        , String deliveryPublicId
        , AdminDeliveryRecoveryRequest request
    ) {
        accessService.validateAdminAccess(adminId, role);
        validateRequest(request);

        return java.util.Objects.requireNonNull(
            transactionTemplate.execute(
                status -> recoverInTransaction(
                    adminId
                    , deliveryPublicId
                    , request
                )
            )
        );
    }

    private AdminDeliveryRecoveryResponse recoverInTransaction(
        Long adminId
        , String deliveryPublicId
        , AdminDeliveryRecoveryRequest request
    ) {

        Delivery reference = deliveryRepository.findByDeliveryPublicId(deliveryPublicId)
            .orElseThrow(DeliveryNotFoundException::new);
        DeliveryGroup group = groupRepository.findByIdForUpdate(
            reference.getDeliveryGroup().getId()
        ).orElseThrow(DeliveryNotFoundException::new);
        Rider actualRider = riderRepository.findAllByIdInForUpdate(
            List.of(request.actualRiderId())
        ).stream().findFirst().orElseThrow(RiderNotFoundException::new);
        List<Delivery> deliveries = deliveryRepository.findAllByDeliveryGroupIdForUpdate(
            group.getId()
        );
        Delivery delivery = deliveries.stream()
            .filter(item -> item.getDeliveryPublicId().equals(deliveryPublicId))
            .findFirst()
            .orElseThrow(DeliveryNotFoundException::new);
        List<DeliveryAssignment> assignments =
            assignmentRepository.findAllByDeliveryGroupIdForUpdate(group.getId());
        List<DeliveryAssignmentItem> items =
            assignmentItemRepository.findAllByDeliveryGroupIdForUpdate(group.getId());

        validateActualRider(actualRider, delivery, assignments, items);
        if (delivery.getStatus() != DeliveryStatus.DELIVERING) {
            throw new DeliveryStateConflictException();
        }

        LocalDateTime recoveredAt = LocalDateTime.now(KST);
        if (request.recoveryResult() == DeliveryRecoveryResult.DELIVERED) {
            recoverCompletion(
                adminId
                , delivery
                , request
                , recoveredAt
            );
        } else {
            recoverFailure(adminId, delivery, request, recoveredAt);
        }
        recoveryRepository.save(
            new DeliveryAdminRecovery(
                delivery
                , request.recoveryResult()
                , request.reasonCode()
                , request.reasonDetail()
                , actualRider.getId()
                , adminId
                , recoveredAt
            )
        );
        executionSupport.recalculateGroup(group, deliveries, recoveredAt);

        return new AdminDeliveryRecoveryResponse(
            deliveryPublicId
            , delivery.getStatus()
            , delivery.getDeliveryVersion()
            , request.recoveryResult()
            , actualRider.getId()
            , recoveredAt.atZone(KST).toOffsetDateTime()
        );
    }

    private void recoverCompletion(
        Long adminId
        , Delivery delivery
        , AdminDeliveryRecoveryRequest recoveryRequest
        , LocalDateTime recoveredAt
    ) {
        RiderDeliveryCompletionRequest request = recoveryRequest.completion();
        if (request.actualHandoffType() != ActualHandoffType.DIRECT) {
            throw new DeliveryHandoffInfoRequiredException();
        }

        transition(delivery, DeliveryStatus.DELIVERED);
        completionRepository.save(
            new DeliveryCompletion(
                delivery
                , request.actualHandoffType()
                , request.storageLocation()
                , toLocal(request.contactAttemptedAt())
                , request.contactResult()
                , adminId
                , DeliveryProcessedByType.ADMIN
                , recoveryRequest.reasonCode().name()
                , recoveryRequest.reasonDetail()
                , recoveredAt
            )
        );
        saveHistory(delivery, DeliveryStatus.DELIVERED, adminId, recoveredAt);
        if (delayService.recordCompletionDelay(delivery, recoveredAt)) {
            eventPublisher.publishRefundConfirmed(
                delivery
                , DeliveryRefundReason.DELIVERY_DELAYED
                , recoveredAt
            );
        }
        eventPublisher.publishStateChanged("DELIVERY_COMPLETED", delivery, recoveredAt);
    }

    private void recoverFailure(
        Long adminId
        , Delivery delivery
        , AdminDeliveryRecoveryRequest recoveryRequest
        , LocalDateTime recoveredAt
    ) {
        RiderDeliveryFailureRequest request = recoveryRequest.failure();
        if (request.failureStage() != DeliveryFailureStage.DURING_DELIVERY) {
            throw new InvalidDeliveryFailureReasonException();
        }
        failureValidator.validate(
            request.failureCode()
            , request.failureDetail()
            , request.contactAttemptedAt()
            , request.contactResult()
            , request.itemRecovered()
            , request.recoveredAt()
        );
        transition(delivery, DeliveryStatus.FAILED);
        failureRepository.save(
            new DeliveryFailure(
                delivery
                , request.failureStage()
                , request.failureCode()
                , request.failureDetail()
                , toLocal(request.contactAttemptedAt())
                , request.contactResult()
                , request.itemRecovered()
                , toLocal(request.recoveredAt())
                , adminId
                , DeliveryProcessedByType.ADMIN
                , recoveryRequest.reasonCode().name()
                , recoveryRequest.reasonDetail()
                , recoveredAt
            )
        );
        saveHistory(delivery, DeliveryStatus.FAILED, adminId, recoveredAt);
        eventPublisher.publishStateChanged("DELIVERY_FAILED", delivery, recoveredAt);
        eventPublisher.publishRefundConfirmed(
            delivery
            , refundReasonResolver.resolveFailure(request.failureCode())
            , recoveredAt
        );
    }

    private void transition(Delivery delivery, DeliveryStatus nextStatus) {
        if (
            deliveryRepository.transitionStatus(
                delivery.getId()
                , DeliveryStatus.DELIVERING
                , nextStatus
            ) != 1
        ) {
            throw new DeliveryStateConflictException();
        }
        entityManager.refresh(delivery);
    }

    private void saveHistory(
        Delivery delivery
        , DeliveryStatus nextStatus
        , Long adminId
        , LocalDateTime recoveredAt
    ) {
        historyRepository.save(
            new DeliveryStatusHistory(
                delivery
                , DeliveryStatus.DELIVERING
                , nextStatus
                , adminId
                , DeliveryChangedByType.ADMIN
                , recoveredAt
            )
        );
    }

    private void validateRequest(AdminDeliveryRecoveryRequest request) {
        if (request.reasonCode().requiresDetail() && isBlank(request.reasonDetail())) {
            throw new InvalidDeliveryInfoException();
        }
        boolean completion = request.completion() != null;
        boolean failure = request.failure() != null;
        if (
            request.recoveryResult() == DeliveryRecoveryResult.DELIVERED
                ? !completion || failure
                : completion || !failure
        ) {
            throw new InvalidDeliveryInfoException();
        }
        if (completion) {
            validateCompletionRequest(request.completion());
        }
    }

    private void validateCompletionRequest(RiderDeliveryCompletionRequest request) {
        if (
            request.actualHandoffType() != ActualHandoffType.DIRECT
                || !isBlank(request.storageLocation())
        ) {
            throw new DeliveryHandoffInfoRequiredException();
        }
    }

    private void validateActualRider(
        Rider actualRider
        , Delivery delivery
        , List<DeliveryAssignment> assignments
        , List<DeliveryAssignmentItem> items
    ) {
        boolean confirmed = assignments.stream().anyMatch(
            assignment ->
                assignment.getRider().getId().equals(actualRider.getId())
                    && assignment.getStatus() == DeliveryAssignmentStatus.CONFIRMED
                    && items.stream().anyMatch(
                    item ->
                        item.getAssignment().getId().equals(assignment.getId())
                            && item.getDelivery().getId().equals(delivery.getId())
                )
        );
        if (!confirmed) {
            throw new DeliveryAccessForbiddenException();
        }
    }

    private LocalDateTime toLocal(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(KST).toLocalDateTime();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
