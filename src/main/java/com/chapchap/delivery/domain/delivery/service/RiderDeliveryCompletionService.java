package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryRefundReason;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.RequestHandoffType;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletion;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletionPhoto;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionPhotoRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryDelayRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRecipientSnapshotRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.request.RiderDeliveryCompletionRequest;
import com.chapchap.delivery.domain.delivery.response.RiderDeliveryCompletionResponse;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryHandoffInfoRequiredException;
import com.chapchap.delivery.global.exception.business.DeliveryNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryStateConflictException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import com.chapchap.delivery.global.kafka.producer.DeliveryEventRequestPublisher;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class RiderDeliveryCompletionService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryAccessService accessService;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryGroupRepository groupRepository;
    private final RiderRepository riderRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryAssignmentItemRepository assignmentItemRepository;
    private final DeliveryCompletionRepository completionRepository;
    private final DeliveryDelayRepository delayRepository;
    private final DeliveryStatusHistoryRepository historyRepository;
    private final DeliveryExecutionSupport executionSupport;
    private final DeliveryEventRequestPublisher eventPublisher;
    private final EntityManager entityManager;
    private final DeliveryCompletionPhotoRepository photoRepository;
    private final DeliveryRecipientSnapshotRepository recipientRepository;
    private final DeliveryPhotoFileService photoFileService;
    private final DeliveryDelayService deliveryDelayService;
    private final TransactionTemplate transactionTemplate;

    public RiderDeliveryCompletionService(
        DeliveryAccessService accessService
        , DeliveryRepository deliveryRepository
        , DeliveryGroupRepository groupRepository
        , RiderRepository riderRepository
        , DeliveryAssignmentRepository assignmentRepository
        , DeliveryAssignmentItemRepository assignmentItemRepository
        , DeliveryCompletionRepository completionRepository
        , DeliveryDelayRepository delayRepository
        , DeliveryStatusHistoryRepository historyRepository
        , DeliveryExecutionSupport executionSupport
        , DeliveryEventRequestPublisher eventPublisher
        , EntityManager entityManager
        , DeliveryCompletionPhotoRepository photoRepository
        , DeliveryRecipientSnapshotRepository recipientRepository
        , DeliveryPhotoFileService photoFileService
        , DeliveryDelayService deliveryDelayService
        , TransactionTemplate transactionTemplate
    ) {
        this.accessService = accessService;
        this.deliveryRepository = deliveryRepository;
        this.groupRepository = groupRepository;
        this.riderRepository = riderRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentItemRepository = assignmentItemRepository;
        this.completionRepository = completionRepository;
        this.delayRepository = delayRepository;
        this.historyRepository = historyRepository;
        this.executionSupport = executionSupport;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
        this.photoRepository = photoRepository;
        this.recipientRepository = recipientRepository;
        this.photoFileService = photoFileService;
        this.deliveryDelayService = deliveryDelayService;
        this.transactionTemplate = transactionTemplate;
    }

    public RiderDeliveryCompletionResponse complete(
        Long authUserId
        , String deliveryPublicId
        , RiderDeliveryCompletionRequest request
        , MultipartFile photo
    ) {
        accessService.validateRiderAccess(authUserId, UserRole.RIDER);
        validateRequest(request, photo);

        if (request.actualHandoffType() == ActualHandoffType.DIRECT) {
            CompletionResult result = executeCompletionTransaction(
                authUserId
                , deliveryPublicId
                , request
                , null
            );
            return result.response();
        }

        CompletionPreparation preparation = Objects.requireNonNull(
            transactionTemplate.execute(
                status -> preparePhotoCompletionInTransaction(
                    authUserId
                    , deliveryPublicId
                    , request
                )
            )
        );
        if (preparation.completedResponse() != null) {
            return preparation.completedResponse();
        }

        DeliveryPhotoFileService.StoredPhoto storedPhoto =
            photoFileService.store(deliveryPublicId, authUserId, photo);

        try {
            CompletionResult result = executeCompletionTransaction(
                authUserId
                , deliveryPublicId
                , request
                , storedPhoto
            );
            if (!result.storedPhotoPersisted()) {
                deleteUnusedUploadedPhotoAfterSuccess(storedPhoto);
            }
            return result.response();
        } catch (RuntimeException exception) {
            deleteUploadedPhotoAfterFailure(storedPhoto, exception);
            throw exception;
        }
    }

    private CompletionPreparation preparePhotoCompletionInTransaction(
        Long authUserId
        , String deliveryPublicId
        , RiderDeliveryCompletionRequest request
    ) {
        LockedCompletionContext context = lockAndValidateExecutionContext(
            authUserId
            , deliveryPublicId
        );
        Delivery delivery = context.delivery();
        DeliveryStatus currentStatus = delivery.getStatus();

        if (currentStatus == DeliveryStatus.DELIVERED) {
            return new CompletionPreparation(
                completedResponseForSameRequest(delivery, request)
            );
        }
        if (currentStatus != DeliveryStatus.DELIVERING) {
            throw new DeliveryStateConflictException();
        }

        validateHandoff(delivery, request);
        return new CompletionPreparation(null);
    }

    private CompletionResult executeCompletionTransaction(
        Long authUserId
        , String deliveryPublicId
        , RiderDeliveryCompletionRequest request
        , DeliveryPhotoFileService.StoredPhoto storedPhoto
    ) {
        return Objects.requireNonNull(
            transactionTemplate.execute(
                status -> completeInTransaction(
                    authUserId
                    , deliveryPublicId
                    , request
                    , storedPhoto
                )
            )
        );
    }

    private CompletionResult completeInTransaction(
        Long authUserId
        , String deliveryPublicId
        , RiderDeliveryCompletionRequest request
        , DeliveryPhotoFileService.StoredPhoto storedPhoto
    ) {
        LockedCompletionContext context = lockAndValidateExecutionContext(
            authUserId
            , deliveryPublicId
        );
        DeliveryGroup group = context.group();
        List<Delivery> deliveries = context.deliveries();
        Delivery delivery = context.delivery();
        DeliveryStatus currentStatus = delivery.getStatus();

        if (currentStatus == DeliveryStatus.DELIVERED) {
            return new CompletionResult(
                completedResponseForSameRequest(delivery, request)
                , false
            );
        }
        if (currentStatus != DeliveryStatus.DELIVERING) {
            throw new DeliveryStateConflictException();
        }

        validateHandoff(delivery, request);

        LocalDateTime completedAt = LocalDateTime.now(KST);
        int changed = deliveryRepository.transitionStatus(
            delivery.getId()
            , DeliveryStatus.DELIVERING
            , DeliveryStatus.DELIVERED
        );
        if (changed != 1) {
            throw new DeliveryStateConflictException();
        }
        entityManager.refresh(delivery);

        DeliveryCompletion completion = completionRepository.save(
            new DeliveryCompletion(
                delivery
                , request.actualHandoffType()
                , request.storageLocation()
                , toLocal(request.contactAttemptedAt())
                , request.contactResult()
                , authUserId
                , completedAt
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
        historyRepository.save(
            new DeliveryStatusHistory(
                delivery
                , DeliveryStatus.DELIVERING
                , DeliveryStatus.DELIVERED
                , authUserId
                , DeliveryChangedByType.RIDER
                , completedAt
            )
        );
        executionSupport.recalculateGroup(group, deliveries, completedAt);
        boolean delayed = deliveryDelayService.recordCompletionDelay(delivery, completedAt);
        if (delayed) {
            eventPublisher.publishRefundConfirmed(
                delivery
                , DeliveryRefundReason.DELIVERY_DELAYED
                , completedAt
            );
        }
        eventPublisher.publishStateChanged(
            "DELIVERY_COMPLETED"
            , delivery
            , completedAt
        );

        return new CompletionResult(
            response(delivery, completion, storedPhoto != null)
            , storedPhoto != null
        );
    }

    private LockedCompletionContext lockAndValidateExecutionContext(
        Long authUserId
        , String deliveryPublicId
    ) {
        Delivery reference = deliveryRepository.findByDeliveryPublicId(deliveryPublicId)
            .orElseThrow(DeliveryNotFoundException::new);
        DeliveryGroup group = groupRepository.findByIdForUpdate(reference.getDeliveryGroup().getId())
            .orElseThrow(DeliveryNotFoundException::new);
        Rider rider = riderRepository.findByAuthUserIdForUpdate(authUserId)
            .orElseThrow(RiderNotFoundException::new);
        List<Delivery> deliveries = deliveryRepository.findAllByDeliveryGroupIdForUpdate(group.getId());
        Delivery delivery = deliveries.stream()
            .filter(candidate -> candidate.getDeliveryPublicId().equals(deliveryPublicId))
            .findFirst()
            .orElseThrow(DeliveryNotFoundException::new);
        assignmentRepository.findAllByDeliveryGroupIdForUpdate(group.getId());
        List<DeliveryAssignmentItem> items =
            assignmentItemRepository.findAllByDeliveryGroupIdForUpdate(group.getId());
        executionSupport.validateCurrentConfirmedAssignment(rider, delivery, items);
        validateActive(rider);

        return new LockedCompletionContext(group, deliveries, delivery);
    }

    private RiderDeliveryCompletionResponse completedResponseForSameRequest(
        Delivery delivery
        , RiderDeliveryCompletionRequest request
    ) {
        DeliveryCompletion completion = completionRepository.findByDeliveryId(delivery.getId())
            .orElseThrow(DeliveryStateConflictException::new);
        if (!sameRequest(completion, request)) {
            throw new DeliveryStateConflictException();
        }
        boolean hasPhoto = photoRepository.findByDeliveryCompletionId(completion.getId()).isPresent();
        return response(delivery, completion, hasPhoto);
    }

    private void validateRequest(
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
        RequestHandoffType requestedHandoffType = delivery.getRequestHandoffType();
        if (
            requestedHandoffType == RequestHandoffType.DIRECT
                && (
                    request.contactAttemptedAt() == null
                        || isBlank(request.contactResult())
                )
        ) {
            throw new DeliveryHandoffInfoRequiredException();
        }
        if (request.actualHandoffType() == ActualHandoffType.OTHER) {
            if (requestedHandoffType == RequestHandoffType.DIRECT) {
                return;
            }
            if (requestedHandoffType != RequestHandoffType.OTHER) {
                throw new DeliveryHandoffInfoRequiredException();
            }
            var snapshot = recipientRepository.findById(delivery.getId())
                .orElseThrow(DeliveryHandoffInfoRequiredException::new);
            if (isBlank(snapshot.getOtherRequest())) {
                throw new DeliveryHandoffInfoRequiredException();
            }
        }
    }

    private boolean sameRequest(
        DeliveryCompletion completion
        , RiderDeliveryCompletionRequest request
    ) {
        return completion.getActualHandoffType() == request.actualHandoffType()
            && Objects.equals(completion.getStorageLocation(), request.storageLocation())
            && Objects.equals(
                completion.getContactAttemptedAt()
                , toLocal(request.contactAttemptedAt())
            )
            && Objects.equals(completion.getContactResult(), request.contactResult());
    }

    private void validateActive(Rider rider) {
        if (!Boolean.TRUE.equals(rider.getIsDeliveryActive())) {
            throw new DeliveryAccessForbiddenException();
        }
    }

    private void deleteUploadedPhotoAfterFailure(
        DeliveryPhotoFileService.StoredPhoto storedPhoto
        , RuntimeException originalException
    ) {
        try {
            photoFileService.delete(storedPhoto.storageKey());
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }

    private void deleteUnusedUploadedPhotoAfterSuccess(
        DeliveryPhotoFileService.StoredPhoto storedPhoto
    ) {
        try {
            photoFileService.delete(storedPhoto.storageKey());
        } catch (RuntimeException cleanupException) {
            log.warn(
                "Failed to delete unused delivery completion photo. storageKey={}",
                storedPhoto.storageKey(),
                cleanupException
            );
        }
    }

    private LocalDateTime toLocal(java.time.OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(KST).toLocalDateTime();
    }

    private boolean hasPhoto(MultipartFile photo) {
        return photo != null && !photo.isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private RiderDeliveryCompletionResponse response(
        Delivery delivery
        , DeliveryCompletion completion
        , boolean hasPhoto
    ) {
        return new RiderDeliveryCompletionResponse(
            delivery.getDeliveryPublicId()
            , delivery.getStatus()
            , delivery.getDeliveryVersion()
            , completion.getActualHandoffType()
            , completion.getCompletedAt().atZone(KST).toOffsetDateTime()
            , hasPhoto
            , delayRepository.findByDeliveryId(delivery.getId()).isPresent()
        );
    }

    private record LockedCompletionContext(
        DeliveryGroup group
        , List<Delivery> deliveries
        , Delivery delivery
    ) {
    }

    private record CompletionPreparation(
        RiderDeliveryCompletionResponse completedResponse
    ) {
    }

    private record CompletionResult(
        RiderDeliveryCompletionResponse response
        , boolean storedPhotoPersisted
    ) {
    }
}
