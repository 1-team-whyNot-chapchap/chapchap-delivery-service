package com.chapchap.delivery.domain.riderlocation.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.riderlocation.event.DeliveryTrackingEndedEvent;
import com.chapchap.delivery.domain.riderlocation.event.RiderLocationUpdatedEvent;
import com.chapchap.delivery.domain.riderlocation.repository.RiderCurrentLocationRepository;
import com.chapchap.delivery.domain.riderlocation.response.RiderLocationResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiderLocationTrackingEventListenerTest {
    private static final long RIDER_ID = 10L;
    private static final long DELIVERY_ID = 101L;
    private static final RiderLocationResponse LOCATION = new RiderLocationResponse(
        new BigDecimal("37.5665000"), new BigDecimal("126.9780000"), new BigDecimal("12.50"),
        OffsetDateTime.of(2026, 9, 11, 12, 0, 0, 0, ZoneOffset.ofHours(9)),
        OffsetDateTime.of(2026, 9, 11, 12, 0, 5, 0, ZoneOffset.ofHours(9))
    );

    @Mock private DeliveryAssignmentItemRepository assignmentItemRepository;
    @Mock private RiderCurrentLocationRepository currentLocationRepository;
    @Mock private RiderLocationSseRegistry registry;

    private RiderLocationTrackingEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new RiderLocationTrackingEventListener(
            assignmentItemRepository, currentLocationRepository, registry
        );
    }

    @Test
    @DisplayName("배송 중인 모든 배송에만 최신 위치를 fan-out한다")
    void fansOutOnlyDeliveringDeliveries() {
        when(assignmentItemRepository.findDeliveringPublicIdsByRiderId(
            RIDER_ID, DeliveryAssignmentStatus.CONFIRMED, DeliveryStatus.DELIVERING
        )).thenReturn(List.of("delivery-101", "delivery-102"));

        listener.onLocationUpdated(new RiderLocationUpdatedEvent(RIDER_ID, LOCATION));

        verify(registry).sendLocation("delivery-101", LOCATION);
        verify(registry).sendLocation("delivery-102", LOCATION);
    }

    @Test
    @DisplayName("한 배송만 종료돼도 다른 배송이 진행 중이면 기사 위치는 유지한다")
    void retainsCurrentLocationWhenOtherDeliveryIsStillDelivering() {
        when(assignmentItemRepository.findConfirmedRiderIdsByDeliveryId(
            DELIVERY_ID, DeliveryAssignmentStatus.CONFIRMED
        )).thenReturn(List.of(RIDER_ID));
        when(assignmentItemRepository.findDeliveringPublicIdsByRiderId(
            RIDER_ID, DeliveryAssignmentStatus.CONFIRMED, DeliveryStatus.DELIVERING
        )).thenReturn(List.of("delivery-102"));

        listener.onTrackingEnded(new DeliveryTrackingEndedEvent(DELIVERY_ID, "delivery-101", DeliveryStatus.DELIVERED));

        verify(registry).endTracking("delivery-101", "DELIVERED");
        verify(currentLocationRepository, never()).deleteById(RIDER_ID);
    }

    @Test
    @DisplayName("마지막 배송이 종료되면 기사 현재 위치를 삭제한다")
    void deletesCurrentLocationAfterLastDeliveringDeliveryEnds() {
        when(assignmentItemRepository.findConfirmedRiderIdsByDeliveryId(
            DELIVERY_ID, DeliveryAssignmentStatus.CONFIRMED
        )).thenReturn(List.of(RIDER_ID));
        when(assignmentItemRepository.findDeliveringPublicIdsByRiderId(
            RIDER_ID, DeliveryAssignmentStatus.CONFIRMED, DeliveryStatus.DELIVERING
        )).thenReturn(List.of());

        listener.onTrackingEnded(new DeliveryTrackingEndedEvent(DELIVERY_ID, "delivery-102", DeliveryStatus.DELIVERED));

        verify(registry).endTracking("delivery-102", "DELIVERED");
        verify(currentLocationRepository).deleteById(RIDER_ID);
    }
}
