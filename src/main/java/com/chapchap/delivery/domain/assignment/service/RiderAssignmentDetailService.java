package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentDeliveryItemResponse;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentDetailResponse;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryRecipientSnapshot;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRecipientSnapshotRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentNotFoundException;
import com.chapchap.delivery.global.security.PersonalDataEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiderAssignmentDetailService {

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryAssignmentItemRepository deliveryAssignmentItemRepository;
    private final DeliveryRecipientSnapshotRepository deliveryRecipientSnapshotRepository;
    private final DeliveryAccessService deliveryAccessService;
    private final PersonalDataEncryptor personalDataEncryptor;

    @Transactional(readOnly = true)
    public RiderAssignmentDetailResponse getMyAssignmentDetail(
        Long authUserId
        , Long assignmentId
    ) {
        validateRiderAccess(
            authUserId
        );

        DeliveryAssignment assignment =
            deliveryAssignmentRepository.findMineById(
                    assignmentId
                    , authUserId
                )
                .orElseThrow(
                    DeliveryAssignmentNotFoundException::new
                );

        validateRiderActive(
            assignment
        );

        List<DeliveryAssignmentItem> assignmentItems =
            deliveryAssignmentItemRepository.findAllByAssignmentIdWithDelivery(
                assignmentId
            );

        Map<Long, DeliveryRecipientSnapshot> recipientSnapshotByDeliveryId =
            getRecipientSnapshotByDeliveryId(
                assignmentItems
            );

        List<RiderAssignmentDeliveryItemResponse> deliveries =
            assignmentItems.stream()
                .map(
                    assignmentItem ->
                        toDeliveryResponse(
                            assignment
                            , assignmentItem.getDelivery()
                            , recipientSnapshotByDeliveryId.get(
                                assignmentItem.getDelivery()
                                    .getId()
                            )
                        )
                )
                .toList();

        int lunchboxQuantity =
            assignmentItems.stream()
                .map(DeliveryAssignmentItem::getDelivery)
                .mapToInt(Delivery::getLunchboxQuantity)
                .sum();

        DeliveryGroup deliveryGroup =
            assignment.getDeliveryGroup();

        return new RiderAssignmentDetailResponse(
            assignment.getId()
            , assignment.getStatus()
            , deliveryGroup.getDeliveryDate()
            , deliveryGroup.getSlot()
            .getCode()
            , deliveries.size()
            , lunchboxQuantity
            , deliveries
        );
    }

    private void validateRiderAccess(
        Long authUserId
    ) {
        if (!deliveryAccessService.isRiderAccessAllowed(authUserId)) {
            throw new DeliveryAccessForbiddenException();
        }
    }

    private void validateRiderActive(
        DeliveryAssignment assignment
    ) {
        if (!Boolean.TRUE.equals(
            assignment.getRider()
                .getIsDeliveryActive()
        )) {
            throw new DeliveryAccessForbiddenException();
        }
    }

    private Map<Long, DeliveryRecipientSnapshot> getRecipientSnapshotByDeliveryId(
        List<DeliveryAssignmentItem> assignmentItems
    ) {
        if (assignmentItems.isEmpty()) {
            return Map.of();
        }

        List<Long> deliveryIds =
            assignmentItems.stream()
                .map(DeliveryAssignmentItem::getDelivery)
                .map(Delivery::getId)
                .toList();

        return deliveryRecipientSnapshotRepository.findAllByDeliveryIdIn(
                deliveryIds
            )
            .stream()
            .collect(
                Collectors.toMap(
                    DeliveryRecipientSnapshot::getDeliveryId
                    , Function.identity()
                )
            );
    }

    private RiderAssignmentDeliveryItemResponse toDeliveryResponse(
        DeliveryAssignment assignment
        , Delivery delivery
        , DeliveryRecipientSnapshot recipientSnapshot
    ) {
        boolean personalDataAccessible =
            isPersonalDataAccessible(
                assignment
                , delivery
            );

        if (!personalDataAccessible || recipientSnapshot == null) {
            return toDeliveryResponseWithoutPersonalData(
                delivery
            );
        }

        return new RiderAssignmentDeliveryItemResponse(
            delivery.getDeliveryPublicId()
            , delivery.getStatus()
            , delivery.getLunchboxQuantity()
            , delivery.getMenuNameSnapshot()
            , recipientSnapshot.getRecipientName()
            , personalDataEncryptor.decrypt(
            recipientSnapshot.getPhoneEncrypted()
        )
            , recipientSnapshot.getPostalCode()
            , recipientSnapshot.getBaseAddress()
            , recipientSnapshot.getDetailAddress()
            , decryptNullable(
            recipientSnapshot.getEntranceInfoEncrypted()
        )
            , delivery.getRequestHandoffType()
            , recipientSnapshot.getOtherRequest()
            , delivery.getTermsAgreed()
        );
    }

    private RiderAssignmentDeliveryItemResponse toDeliveryResponseWithoutPersonalData(
        Delivery delivery
    ) {
        return new RiderAssignmentDeliveryItemResponse(
            delivery.getDeliveryPublicId()
            , delivery.getStatus()
            , delivery.getLunchboxQuantity()
            , delivery.getMenuNameSnapshot()
            , null
            , null
            , null
            , null
            , null
            , null
            , delivery.getRequestHandoffType()
            , null
            , delivery.getTermsAgreed()
        );
    }

    private boolean isPersonalDataAccessible(
        DeliveryAssignment assignment
        , Delivery delivery
    ) {
        if (!assignment.getStatus().isActive()) {
            return false;
        }

        return !isDeliveryEnded(
            delivery.getStatus()
        );
    }

    private boolean isDeliveryEnded(
        DeliveryStatus status
    ) {
        return status == DeliveryStatus.DELIVERED
            || status == DeliveryStatus.FAILED;
    }

    private String decryptNullable(
        byte[] encryptedData
    ) {
        if (encryptedData == null) {
            return null;
        }

        return personalDataEncryptor.decrypt(
            encryptedData
        );
    }
}