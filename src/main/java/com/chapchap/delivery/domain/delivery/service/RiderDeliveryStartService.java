package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.response.RiderDeliveryStartResponse;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryGroupStateConflictException;
import com.chapchap.delivery.global.exception.business.DeliveryNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryStateConflictException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import com.chapchap.delivery.global.kafka.producer.DeliveryEventRequestPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import jakarta.persistence.EntityManager;

@Service
public class RiderDeliveryStartService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryAccessService accessService;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryGroupRepository groupRepository;
    private final RiderRepository riderRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryAssignmentItemRepository assignmentItemRepository;
    private final DeliveryStatusHistoryRepository historyRepository;
    private final DeliveryExecutionSupport executionSupport;
    private final DeliveryEventRequestPublisher eventPublisher;
    private final EntityManager entityManager;

    public RiderDeliveryStartService(
        DeliveryAccessService accessService
        , DeliveryRepository deliveryRepository
        , DeliveryGroupRepository groupRepository
        , RiderRepository riderRepository
        , DeliveryAssignmentRepository assignmentRepository
        , DeliveryAssignmentItemRepository assignmentItemRepository
        , DeliveryStatusHistoryRepository historyRepository
        , DeliveryExecutionSupport executionSupport
        , DeliveryEventRequestPublisher eventPublisher
        , EntityManager entityManager
    ) {
        this.accessService = accessService;
        this.deliveryRepository = deliveryRepository;
        this.groupRepository = groupRepository;
        this.riderRepository = riderRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentItemRepository = assignmentItemRepository;
        this.historyRepository = historyRepository;
        this.executionSupport = executionSupport;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
    }

    @Transactional
    public RiderDeliveryStartResponse start(
        Long authUserId
        , String deliveryPublicId
    ) {
        accessService.validateRiderAccess(
            authUserId
            , UserRole.RIDER
        );

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

        validateRiderActive(rider);

        if (delivery.getStatus() == DeliveryStatus.DELIVERING) {
            DeliveryStatusHistory startHistory =
                historyRepository
                    .findFirstByDelivery_IdAndToStatusOrderByChangedAtAsc(
                        delivery.getId()
                        , DeliveryStatus.DELIVERING
                    )
                    .orElseThrow(DeliveryStateConflictException::new);

            return response(
                delivery
                , startHistory.getChangedAt()
            );
        }

        if (delivery.getStatus() != DeliveryStatus.READY) {
            throw new DeliveryStateConflictException();
        }

        validateGroupStatus(group);

        LocalDateTime startedAt =
            LocalDateTime.now(KST);

        int changed = deliveryRepository.transitionStatus(
            delivery.getId(), DeliveryStatus.READY, DeliveryStatus.DELIVERING
        );
        if (changed != 1) {
            throw new DeliveryStateConflictException();
        }
        entityManager.refresh(delivery);

        historyRepository.save(
            new DeliveryStatusHistory(
                delivery
                , DeliveryStatus.READY
                , DeliveryStatus.DELIVERING
                , authUserId
                , DeliveryChangedByType.RIDER
                , startedAt
            )
        );

        executionSupport.recalculateGroup(
            group
            , deliveries
            , startedAt
        );

        eventPublisher.publishStateChanged(
            "DELIVERY_STARTED"
            , delivery
            , startedAt
        );

        return response(
            delivery
            , startedAt
        );
    }

    private void validateRiderActive(
        Rider rider
    ) {
        if (!Boolean.TRUE.equals(rider.getIsDeliveryActive())) {
            throw new DeliveryAccessForbiddenException();
        }
    }

    private void validateGroupStatus(
        DeliveryGroup group
    ) {
        if (
            group.getStatus() != DeliveryGroupStatus.CONFIRMED
                && group.getStatus() != DeliveryGroupStatus.IN_PROGRESS
        ) {
            throw new DeliveryGroupStateConflictException();
        }
    }

    private RiderDeliveryStartResponse response(
        Delivery delivery
        , LocalDateTime startedAt
    ) {
        return new RiderDeliveryStartResponse(
            delivery.getDeliveryPublicId()
            , delivery.getStatus()
            , delivery.getDeliveryVersion()
            , startedAt.atZone(KST)
            .toOffsetDateTime()
        );
    }
}
