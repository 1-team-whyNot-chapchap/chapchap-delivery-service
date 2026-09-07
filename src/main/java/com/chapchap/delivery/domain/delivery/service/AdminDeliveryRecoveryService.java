package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.constant.AdminDeliveryFailureReason;
import com.chapchap.delivery.domain.delivery.constant.DeliveryChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureStage;
import com.chapchap.delivery.domain.delivery.constant.DeliveryProcessedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryRecoveryResult;
import com.chapchap.delivery.domain.delivery.constant.DeliveryRefundReason;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.RequestHandoffType;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryAdminRecovery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletion;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletionPhoto;
import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryAdminRecoveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionPhotoRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRecipientSnapshotRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.request.AdminDeliveryRecoveryRequest;
import com.chapchap.delivery.domain.delivery.request.RiderDeliveryCompletionRequest;
import com.chapchap.delivery.domain.delivery.request.RiderDeliveryFailureRequest;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryRecoveryResponse;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
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
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
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
    private final DeliveryCompletionPhotoRepository photoRepository;
    private final DeliveryFailureRepository failureRepository;
    private final DeliveryStatusHistoryRepository historyRepository;
    private final DeliveryRecipientSnapshotRepository recipientRepository;
    private final DeliveryExecutionSupport executionSupport;
    private final DeliveryFailureValidator failureValidator;
    private final DeliveryRefundReasonResolver refundReasonResolver;
    private final DeliveryDelayService delayService;
    private final DeliveryEventRequestPublisher eventPublisher;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final DeliveryPhotoFileService photoFileService;

    public AdminDeliveryRecoveryService(
        DeliveryAccessService accessService
        , DeliveryRepository deliveryRepository
        , DeliveryGroupRepository groupRepository
        , RiderRepository riderRepository
        , DeliveryAssignmentRepository assignmentRepository
        , DeliveryAssignmentItemRepository assignmentItemRepository
        , DeliveryAdminRecoveryRepository recoveryRepository
        , DeliveryCompletionRepository completionRepository
        , DeliveryCompletionPhotoRepository photoRepository
        , DeliveryFailureRepository failureRepository
        , DeliveryStatusHistoryRepository historyRepository
        , DeliveryRecipientSnapshotRepository recipientRepository
        , DeliveryExecutionSupport executionSupport
        , DeliveryFailureValidator failureValidator
        , DeliveryRefundReasonResolver refundReasonResolver
        , DeliveryDelayService delayService
        , DeliveryEventRequestPublisher eventPublisher
        , EntityManager entityManager
        , TransactionTemplate transactionTemplate
        , DeliveryPhotoFileService photoFileService
    ) {
        this.accessService = accessService;
        this.deliveryRepository = deliveryRepository;
        this.groupRepository = groupRepository;
        this.riderRepository = riderRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentItemRepository = assignmentItemRepository;
        this.recoveryRepository = recoveryRepository;
        this.completionRepository = completionRepository;
        this.photoRepository = photoRepository;
        this.failureRepository = failureRepository;
        this.historyRepository = historyRepository;
        this.recipientRepository = recipientRepository;
        this.executionSupport = executionSupport;
        this.failureValidator = failureValidator;
        this.refundReasonResolver = refundReasonResolver;
        this.delayService = delayService;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
        this.photoFileService = photoFileService;
    }

    public AdminDeliveryRecoveryResponse recover(
        Long adminId
        , UserRole role
        , String deliveryPublicId
        , AdminDeliveryRecoveryRequest request
        , MultipartFile photo
    ) {
        accessService.validateAdminAccess(adminId, role);
        validateRequest(request, photo);

        DeliveryPhotoFileService.StoredPhoto storedPhoto = null;
        if (
            request.recoveryResult() == DeliveryRecoveryResult.DELIVERED
                && request.completion().actualHandoffType() != ActualHandoffType.DIRECT
        ) {
            transactionTemplate.executeWithoutResult(
                status -> preparePhotoRecoveryInTransaction(deliveryPublicId, request)
            );
            storedPhoto = photoFileService.store(deliveryPublicId, adminId, photo);
        }

        try {
            DeliveryPhotoFileService.StoredPhoto finalStoredPhoto = storedPhoto;
            return Objects.requireNonNull(
                transactionTemplate.execute(
                    status -> recoverInTransaction(
                        adminId
                        , deliveryPublicId
                        , request
                        , finalStoredPhoto
                    )
                )
            );
        } catch (RuntimeException exception) {
            deleteUploadedPhotoAfterFailure(storedPhoto, exception);
            throw exception;
        }
    }

    private AdminDeliveryRecoveryResponse recoverInTransaction(
        Long adminId
        , String deliveryPublicId
        , AdminDeliveryRecoveryRequest request
        , DeliveryPhotoFileService.StoredPhoto storedPhoto
    ) {

        LockedRecoveryContext context = lockAndValidateRecoveryContext(
            deliveryPublicId, request.actualRiderId()
        );
        DeliveryGroup group = context.group();
        Rider actualRider = context.actualRider();
        List<Delivery> deliveries = context.deliveries();
        Delivery delivery = context.delivery();
        DeliveryStatus originalStatus = delivery.getStatus();
        validateTransition(originalStatus, request.recoveryResult());

        LocalDateTime recoveredAt = LocalDateTime.now(KST);
        if (request.recoveryResult() == DeliveryRecoveryResult.DELIVERED) {
            recoverCompletion(
                adminId
                , delivery
                , request
                , recoveredAt
                , storedPhoto
            );
        } else {
            recoverFailure(adminId, delivery, request, recoveredAt, originalStatus);
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

    private void preparePhotoRecoveryInTransaction(
        String deliveryPublicId
        , AdminDeliveryRecoveryRequest request
    ) {
        LockedRecoveryContext context = lockAndValidateRecoveryContext(
            deliveryPublicId, request.actualRiderId()
        );
        validateTransition(context.delivery().getStatus(), request.recoveryResult());
        validateHandoff(context.delivery(), request.completion());
    }

    private LockedRecoveryContext lockAndValidateRecoveryContext(
        String deliveryPublicId
        , Long actualRiderId
    ) {
        Delivery reference = deliveryRepository.findByDeliveryPublicId(deliveryPublicId)
            .orElseThrow(DeliveryNotFoundException::new);
        DeliveryGroup group = groupRepository.findByIdForUpdate(
            reference.getDeliveryGroup().getId()
        ).orElseThrow(DeliveryNotFoundException::new);
        Rider actualRider = riderRepository.findAllByIdInForUpdate(
            List.of(actualRiderId)
        ).stream().findFirst().orElseThrow(RiderNotFoundException::new);
        List<Delivery> deliveries = deliveryRepository.findAllByDeliveryGroupIdForUpdate(
            group.getId()
        );
        Delivery delivery = deliveries.stream()
            .filter(item -> item.getDeliveryPublicId().equals(deliveryPublicId))
            .findFirst()
            .orElseThrow(DeliveryNotFoundException::new);
        assignmentRepository.findAllByDeliveryGroupIdForUpdate(group.getId());
        List<DeliveryAssignmentItem> items =
            assignmentItemRepository.findAllByDeliveryGroupIdForUpdate(group.getId());
        validateActualRider(actualRider, delivery, items);
        return new LockedRecoveryContext(group, actualRider, deliveries, delivery);
    }

    private void recoverCompletion(
        Long adminId
        , Delivery delivery
        , AdminDeliveryRecoveryRequest recoveryRequest
        , LocalDateTime recoveredAt
        , DeliveryPhotoFileService.StoredPhoto storedPhoto
    ) {
        RiderDeliveryCompletionRequest request = recoveryRequest.completion();
        validateHandoff(delivery, request);
        DeliveryCompletion completion = completionRepository.save(
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
        if (storedPhoto != null) {
            photoRepository.save(
                new DeliveryCompletionPhoto(
                    completion
                    , storedPhoto.storageKey()
                    , storedPhoto.originalFilename()
                    , storedPhoto.contentType()
                    , storedPhoto.fileSize()
                    , storedPhoto.uploadedBy()
                    , storedPhoto.uploadedAt()
                )
            );
        }
        transition(delivery, DeliveryStatus.DELIVERING, DeliveryStatus.DELIVERED);
        saveHistory(
            delivery, DeliveryStatus.DELIVERING, DeliveryStatus.DELIVERED, adminId, recoveredAt
        );
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
        , DeliveryStatus originalStatus
    ) {
        RiderDeliveryFailureRequest request = recoveryRequest.failure();
        DeliveryFailureStage requiredStage = originalStatus == DeliveryStatus.READY
            ? DeliveryFailureStage.BEFORE_DEPARTURE
            : DeliveryFailureStage.DURING_DELIVERY;
        if (request.failureStage() != requiredStage) {
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
        transition(delivery, originalStatus, DeliveryStatus.FAILED);
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
                , AdminDeliveryFailureReason.SYSTEM_RECOVERY.name()
                , recoveryRequest.reasonDetail()
                , recoveredAt
            )
        );
        saveHistory(delivery, originalStatus, DeliveryStatus.FAILED, adminId, recoveredAt);
        eventPublisher.publishStateChanged("DELIVERY_FAILED", delivery, recoveredAt);
        eventPublisher.publishRefundConfirmed(
            delivery
            , refundReasonResolver.resolveFailure(request.failureCode())
            , recoveredAt
        );
    }

    private void transition(
        Delivery delivery
        , DeliveryStatus expectedStatus
        , DeliveryStatus nextStatus
    ) {
        if (
            deliveryRepository.transitionStatus(
                delivery.getId()
                , expectedStatus
                , nextStatus
            ) != 1
        ) {
            throw new DeliveryStateConflictException();
        }
        entityManager.refresh(delivery);
    }

    private void saveHistory(
        Delivery delivery
        , DeliveryStatus previousStatus
        , DeliveryStatus nextStatus
        , Long adminId
        , LocalDateTime recoveredAt
    ) {
        historyRepository.save(
            new DeliveryStatusHistory(
                delivery
                , previousStatus
                , nextStatus
                , adminId
                , DeliveryChangedByType.ADMIN
                , recoveredAt
            )
        );
    }

    private void validateRequest(
        AdminDeliveryRecoveryRequest request
        , MultipartFile photo
    ) {
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
            validateCompletionRequest(request.completion(), photo);
        } else if (hasPhoto(photo)) {
            throw new DeliveryHandoffInfoRequiredException();
        }
    }

    private void validateCompletionRequest(
        RiderDeliveryCompletionRequest request
        , MultipartFile photo
    ) {
        if (request.actualHandoffType() == ActualHandoffType.DIRECT) {
            if (!isBlank(request.storageLocation()) || hasPhoto(photo)) {
                throw new DeliveryHandoffInfoRequiredException();
            }
            return;
        }
        if (isBlank(request.storageLocation()) || !hasPhoto(photo)) {
            throw new DeliveryHandoffInfoRequiredException();
        }
    }

    private void validateTransition(
        DeliveryStatus currentStatus
        , DeliveryRecoveryResult result
    ) {
        boolean allowed = result == DeliveryRecoveryResult.DELIVERED
            ? currentStatus == DeliveryStatus.DELIVERING
            : currentStatus == DeliveryStatus.READY
                || currentStatus == DeliveryStatus.DELIVERING;
        if (!allowed) {
            throw new DeliveryStateConflictException();
        }
    }

    private void validateHandoff(
        Delivery delivery
        , RiderDeliveryCompletionRequest request
    ) {
        if (request.actualHandoffType() == ActualHandoffType.DIRECT) {
            return;
        }
        if (!Boolean.TRUE.equals(delivery.getTermsAgreed())) {
            throw new DeliveryHandoffInfoRequiredException();
        }
        RequestHandoffType requested = delivery.getRequestHandoffType();
        if (
            requested == RequestHandoffType.DIRECT
                && (request.contactAttemptedAt() == null || request.contactResult() == null)
        ) {
            throw new DeliveryHandoffInfoRequiredException();
        }
        if (request.actualHandoffType() == ActualHandoffType.OTHER) {
            if (requested == RequestHandoffType.DIRECT) {
                return;
            }
            if (requested != RequestHandoffType.OTHER) {
                throw new DeliveryHandoffInfoRequiredException();
            }
            var snapshot = recipientRepository.findById(delivery.getId())
                .orElseThrow(DeliveryHandoffInfoRequiredException::new);
            if (isBlank(snapshot.getOtherRequest())) {
                throw new DeliveryHandoffInfoRequiredException();
            }
        }
    }

    private void deleteUploadedPhotoAfterFailure(
        DeliveryPhotoFileService.StoredPhoto storedPhoto
        , RuntimeException originalException
    ) {
        if (storedPhoto == null) {
            return;
        }
        try {
            photoFileService.delete(storedPhoto.storageKey());
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
            log.warn(
                "Failed to delete unused admin recovery photo. storageKey={}",
                storedPhoto.storageKey(), cleanupException
            );
        }
    }

    private boolean hasPhoto(MultipartFile photo) {
        return photo != null && !photo.isEmpty();
    }

    private void validateActualRider(
        Rider actualRider
        , Delivery delivery
        , List<DeliveryAssignmentItem> items
    ) {
        executionSupport.validateCurrentConfirmedAssignment(actualRider, delivery, items);
    }

    private LocalDateTime toLocal(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(KST).toLocalDateTime();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record LockedRecoveryContext(
        DeliveryGroup group
        , Rider actualRider
        , List<Delivery> deliveries
        , Delivery delivery
    ) {
    }
}
