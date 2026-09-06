package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureStage;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.request.RiderDeliveryFailureRequest;
import com.chapchap.delivery.domain.delivery.response.RiderDeliveryFailureResponse;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryStateConflictException;
import com.chapchap.delivery.global.exception.business.InvalidDeliveryFailureReasonException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import com.chapchap.delivery.global.kafka.producer.DeliveryEventRequestPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import jakarta.persistence.EntityManager;

@Service
public class RiderDeliveryFailureService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryAccessService accessService;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryGroupRepository groupRepository;
    private final RiderRepository riderRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryAssignmentItemRepository assignmentItemRepository;
    private final DeliveryFailureRepository failureRepository;
    private final DeliveryStatusHistoryRepository historyRepository;
    private final DeliveryExecutionSupport executionSupport;
    private final DeliveryEventRequestPublisher eventPublisher;
    private final EntityManager entityManager;
    private final DeliveryRefundReasonResolver refundReasonResolver;
    private final DeliveryFailureValidator failureValidator;

    public RiderDeliveryFailureService(
        DeliveryAccessService accessService
        , DeliveryRepository deliveryRepository
        , DeliveryGroupRepository groupRepository
        , RiderRepository riderRepository
        , DeliveryAssignmentRepository assignmentRepository
        , DeliveryAssignmentItemRepository assignmentItemRepository
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
        this.riderRepository = riderRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentItemRepository = assignmentItemRepository;
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
        Long authUserId
        , String deliveryPublicId
        , RiderDeliveryFailureRequest request
    ) {
        accessService.validateRiderAccess(
            authUserId
            , UserRole.RIDER
        );

        validateRequest(request);

        Delivery reference =
            deliveryRepository.findByDeliveryPublicId(deliveryPublicId)
                .orElseThrow(DeliveryNotFoundException::new);

        DeliveryGroup group =
            groupRepository.findByIdForUpdate(
                    reference.getDeliveryGroup().getId()
                )
                .orElseThrow(DeliveryNotFoundException::new);

        Rider rider =
            riderRepository.findByAuthUserIdForUpdate(authUserId)
                .orElseThrow(RiderNotFoundException::new);

        List<Delivery> deliveries =
            deliveryRepository.findAllByDeliveryGroupIdForUpdate(
                group.getId()
            );

        Delivery delivery = deliveries.stream()
            .filter(candidate -> candidate.getDeliveryPublicId().equals(deliveryPublicId))
            .findFirst()
            .orElseThrow(DeliveryNotFoundException::new);

        assignmentRepository.findAllByDeliveryGroupIdForUpdate(
            group.getId()
        );

        List<DeliveryAssignmentItem> items =
            assignmentItemRepository.findAllByDeliveryGroupIdForUpdate(
                group.getId()
            );

        executionSupport.validateCurrentConfirmedAssignment(
            rider
            , delivery
            , items
        );

        if (!Boolean.TRUE.equals(rider.getIsDeliveryActive())) {
            throw new DeliveryAccessForbiddenException();
        }

        if (delivery.getStatus() == DeliveryStatus.FAILED) {
            DeliveryFailure failure =
                failureRepository.findByDeliveryId(delivery.getId())
                    .orElseThrow(
                        DeliveryStateConflictException::new
                    );

            if (!sameRequest(failure, request)) {
                throw new DeliveryStateConflictException();
            }
            return response(
                delivery
                , failure
            );
        }

        if (delivery.getStatus() != DeliveryStatus.DELIVERING) {
            throw new DeliveryStateConflictException();
        }

        LocalDateTime failedAt = LocalDateTime.now(KST);

        int changed = deliveryRepository.transitionStatus(
            delivery.getId(), DeliveryStatus.DELIVERING, DeliveryStatus.FAILED
        );
        if (changed != 1) {
            throw new DeliveryStateConflictException();
        }
        entityManager.refresh(delivery);

        DeliveryFailure failure =
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
                    , authUserId
                    , failedAt
                )
            );

        historyRepository.save(
            new DeliveryStatusHistory(
                delivery
                , DeliveryStatus.DELIVERING
                , DeliveryStatus.FAILED
                , authUserId
                , DeliveryChangedByType.RIDER
                , failedAt
            )
        );

        executionSupport.recalculateGroup(
            group
            , deliveries
            , failedAt
        );

        eventPublisher.publishStateChanged(
            "DELIVERY_FAILED"
            , delivery
            , failedAt
        );
        eventPublisher.publishRefundConfirmed(
            delivery
            , refundReasonResolver.resolveFailure(request.failureCode())
            , failedAt
        );

        return response(
            delivery
            , failure
        );
    }

    private boolean sameRequest(
        DeliveryFailure failure
        , RiderDeliveryFailureRequest request
    ) {
        return failure.getFailureStage() == request.failureStage()
            && failure.getFailureCode() == request.failureCode()
            && java.util.Objects.equals(failure.getFailureDetail(), request.failureDetail())
            && java.util.Objects.equals(failure.getContactAttemptedAt(), toLocal(request.contactAttemptedAt()))
            && java.util.Objects.equals(failure.getContactResult(), request.contactResult())
            && java.util.Objects.equals(failure.getItemRecovered(), request.itemRecovered())
            && java.util.Objects.equals(failure.getRecoveredAt(), toLocal(request.recoveredAt()));
    }

    private void validateRequest(
        RiderDeliveryFailureRequest request
    ) {
        if (
            request.failureStage()
                != DeliveryFailureStage.DURING_DELIVERY
        ) {
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
    }

    private LocalDateTime toLocal(
        OffsetDateTime value
    ) {
        return value == null
            ? null
            : value.atZoneSameInstant(KST)
            .toLocalDateTime();
    }

    private RiderDeliveryFailureResponse response(
        Delivery delivery
        , DeliveryFailure failure
    ) {
        return new RiderDeliveryFailureResponse(
            delivery.getDeliveryPublicId()
            , delivery.getStatus()
            , delivery.getDeliveryVersion()
            , failure.getFailureCode()
            , failure.getFailedAt()
            .atZone(KST)
            .toOffsetDateTime()
        );
    }
}
