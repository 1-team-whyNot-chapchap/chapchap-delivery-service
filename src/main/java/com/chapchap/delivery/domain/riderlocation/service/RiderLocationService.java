package com.chapchap.delivery.domain.riderlocation.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.riderlocation.entity.RiderCurrentLocation;
import com.chapchap.delivery.domain.riderlocation.event.RiderLocationUpdatedEvent;
import com.chapchap.delivery.domain.riderlocation.exception.InvalidRiderLocationException;
import com.chapchap.delivery.domain.riderlocation.exception.RiderLocationAccuracyExceededException;
import com.chapchap.delivery.domain.riderlocation.exception.RiderLocationNotAvailableException;
import com.chapchap.delivery.domain.riderlocation.repository.RiderCurrentLocationRepository;
import com.chapchap.delivery.domain.riderlocation.request.RiderLocationUpdateRequest;
import com.chapchap.delivery.domain.riderlocation.response.RiderLocationResponse;
import com.chapchap.delivery.global.config.RiderLocationProperties;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RiderLocationService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryAccessService accessService;
    private final RiderRepository riderRepository;
    private final DeliveryAssignmentItemRepository assignmentItemRepository;
    private final RiderCurrentLocationRepository currentLocationRepository;
    private final RiderLocationProperties properties;
    private final Clock kstClock;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public RiderLocationResponse update(Long authUserId, UserRole role, RiderLocationUpdateRequest request) {
        accessService.validateRiderAccess(authUserId, role);
        Rider rider = riderRepository.findByAuthUserIdForUpdate(authUserId)
            .orElseThrow(RiderNotFoundException::new);
        if (!Boolean.TRUE.equals(rider.getIsDeliveryActive())) throw new DeliveryAccessForbiddenException();

        OffsetDateTime now = OffsetDateTime.now(kstClock);
        validateMeasurement(request, now);
        if (assignmentItemRepository.findDeliveringPublicIdsByRiderId(
            rider.getId(), DeliveryAssignmentStatus.CONFIRMED, DeliveryStatus.DELIVERING
        ).isEmpty()) {
            throw new RiderLocationNotAvailableException();
        }

        LocalDateTime capturedAt = request.capturedAt().atZoneSameInstant(KST).toLocalDateTime();
        LocalDateTime receivedAt = now.toLocalDateTime();
        RiderCurrentLocation location = currentLocationRepository.findByRiderIdForUpdate(rider.getId())
            .orElse(null);
        boolean fanOut = false;
        if (location == null) {
            location = currentLocationRepository.save(new RiderCurrentLocation(
                rider.getId(), request.latitude(), request.longitude(), request.accuracy(), capturedAt, receivedAt
            ));
            fanOut = true;
        } else {
            int comparison = capturedAt.compareTo(location.getCapturedAt());
            if (comparison > 0) {
                location.replaceWithNewMeasurement(
                    request.latitude(), request.longitude(), request.accuracy(), capturedAt, receivedAt
                );
                fanOut = true;
            } else if (comparison == 0) {
                location.refreshReceipt(receivedAt);
                fanOut = true;
            }
            // An out-of-order measurement intentionally leaves both DB and SSE untouched.
        }
        RiderLocationResponse response = RiderLocationResponse.from(location);
        if (fanOut) publisher.publishEvent(new RiderLocationUpdatedEvent(rider.getId(), response));
        return response;
    }

    private void validateMeasurement(RiderLocationUpdateRequest request, OffsetDateTime now) {
        if (request.capturedAt().isAfter(now.plus(properties.maxFutureSkew()))
            || request.capturedAt().isBefore(now.minus(properties.maxPositionAge()))) {
            throw new InvalidRiderLocationException();
        }
        if (request.accuracy().doubleValue() > properties.maxAccuracyMeters()) {
            throw new RiderLocationAccuracyExceededException();
        }
    }
}
