package com.chapchap.delivery.domain.riderlocation.service;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.riderlocation.event.DeliveryTrackingEndedEvent;
import com.chapchap.delivery.domain.riderlocation.event.RiderLocationUpdatedEvent;
import com.chapchap.delivery.domain.riderlocation.repository.RiderCurrentLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

/** Sends only after the location/status transaction has committed. */
@Component
@RequiredArgsConstructor
public class RiderLocationTrackingEventListener {
    private final DeliveryAssignmentItemRepository assignmentItemRepository;
    private final RiderCurrentLocationRepository currentLocationRepository;
    private final RiderLocationSseRegistry registry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLocationUpdated(RiderLocationUpdatedEvent event) {
        assignmentItemRepository.findDeliveringPublicIdsByRiderId(
            event.riderId(), DeliveryAssignmentStatus.CONFIRMED, DeliveryStatus.DELIVERING
        ).forEach(deliveryPublicId -> registry.sendLocation(deliveryPublicId, event.location()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTrackingEnded(DeliveryTrackingEndedEvent event) {
        registry.endTracking(event.deliveryPublicId(), event.status().name());
        assignmentItemRepository.findConfirmedRiderIdsByDeliveryId(
            event.deliveryId(), DeliveryAssignmentStatus.CONFIRMED
        ).forEach(riderId -> {
            if (assignmentItemRepository.findDeliveringPublicIdsByRiderId(
                riderId, DeliveryAssignmentStatus.CONFIRMED, DeliveryStatus.DELIVERING
            ).isEmpty()) {
                currentLocationRepository.deleteById(riderId);
            }
        });
    }
}
