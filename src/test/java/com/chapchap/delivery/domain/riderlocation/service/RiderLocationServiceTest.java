package com.chapchap.delivery.domain.riderlocation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.riderlocation.entity.RiderCurrentLocation;
import com.chapchap.delivery.domain.riderlocation.event.RiderLocationUpdatedEvent;
import com.chapchap.delivery.domain.riderlocation.exception.RiderLocationAccuracyExceededException;
import com.chapchap.delivery.domain.riderlocation.exception.RiderLocationNotAvailableException;
import com.chapchap.delivery.domain.riderlocation.repository.RiderCurrentLocationRepository;
import com.chapchap.delivery.domain.riderlocation.request.RiderLocationUpdateRequest;
import com.chapchap.delivery.global.config.RiderLocationProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RiderLocationServiceTest {
    private static final long AUTH_USER_ID = 100L;
    private static final long RIDER_ID = 10L;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 9, 11, 12, 0, 0, 0, ZoneOffset.ofHours(9));

    @Mock private DeliveryAccessService accessService;
    @Mock private RiderRepository riderRepository;
    @Mock private DeliveryAssignmentItemRepository assignmentItemRepository;
    @Mock private RiderCurrentLocationRepository currentLocationRepository;
    @Mock private ApplicationEventPublisher publisher;
    @Mock private Rider rider;

    private RiderLocationService service;

    @BeforeEach
    void setUp() {
        RiderLocationProperties properties = new RiderLocationProperties(
            50, Duration.ofSeconds(60), Duration.ofSeconds(90), Duration.ofSeconds(15), Duration.ofHours(1)
        );
        Clock fixedClock = Clock.fixed(NOW.toInstant(), KST);
        service = new RiderLocationService(
            accessService, riderRepository, assignmentItemRepository, currentLocationRepository,
            properties, fixedClock, publisher
        );
        lenient().when(rider.getId()).thenReturn(RIDER_ID);
        when(rider.getIsDeliveryActive()).thenReturn(true);
        when(riderRepository.findByAuthUserIdForUpdate(AUTH_USER_ID)).thenReturn(Optional.of(rider));
        lenient().when(assignmentItemRepository.findDeliveringPublicIdsByRiderId(
            RIDER_ID, DeliveryAssignmentStatus.CONFIRMED, DeliveryStatus.DELIVERING
        )).thenReturn(List.of("delivery-1"));
    }

    @Test
    @DisplayName("최초의 유효 위치를 저장하고 갱신 이벤트를 발행한다")
    void savesFirstValidLocationAndPublishesEvent() {
        RiderLocationUpdateRequest request = requestAt(NOW.minusSeconds(5));
        when(currentLocationRepository.findByRiderIdForUpdate(RIDER_ID)).thenReturn(Optional.empty());
        when(currentLocationRepository.save(any(RiderCurrentLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(AUTH_USER_ID, UserRole.RIDER, request);

        assertThat(response.latitude()).isEqualByComparingTo("37.5665000");
        assertThat(response.longitude()).isEqualByComparingTo("126.9780000");
        verify(accessService).validateRiderAccess(AUTH_USER_ID, UserRole.RIDER);
        verify(currentLocationRepository).save(any(RiderCurrentLocation.class));
        ArgumentCaptor<RiderLocationUpdatedEvent> event = ArgumentCaptor.forClass(RiderLocationUpdatedEvent.class);
        verify(publisher).publishEvent(event.capture());
        assertThat(event.getValue().riderId()).isEqualTo(RIDER_ID);
        assertThat(event.getValue().location().capturedAt()).isEqualTo(request.capturedAt());
    }

    @Test
    @DisplayName("기존 위치보다 과거인 측정값은 저장과 SSE 발행을 하지 않는다")
    void ignoresOutOfOrderMeasurement() {
        RiderCurrentLocation stored = new RiderCurrentLocation(
            RIDER_ID, new BigDecimal("37.5700000"), new BigDecimal("126.9800000"), new BigDecimal("10.00"),
            NOW.minusSeconds(10).toLocalDateTime(), NOW.minusSeconds(10).toLocalDateTime()
        );
        when(currentLocationRepository.findByRiderIdForUpdate(RIDER_ID)).thenReturn(Optional.of(stored));

        var response = service.update(AUTH_USER_ID, UserRole.RIDER, requestAt(NOW.minusSeconds(20)));

        assertThat(response.latitude()).isEqualByComparingTo("37.5700000");
        verify(currentLocationRepository, never()).save(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("허용 정확도를 초과한 위치는 거절한다")
    void rejectsInaccurateMeasurement() {
        RiderLocationUpdateRequest inaccurate = new RiderLocationUpdateRequest(
            new BigDecimal("37.5665000"), new BigDecimal("126.9780000"), new BigDecimal("50.01"), NOW
        );

        assertThatThrownBy(() -> service.update(AUTH_USER_ID, UserRole.RIDER, inaccurate))
            .isInstanceOf(RiderLocationAccuracyExceededException.class);

        verify(currentLocationRepository, never()).findByRiderIdForUpdate(any());
    }

    @Test
    @DisplayName("배송 중인 확정 배정이 없으면 위치를 저장하지 않는다")
    void rejectsWhenThereIsNoDeliveringAssignment() {
        when(assignmentItemRepository.findDeliveringPublicIdsByRiderId(
            RIDER_ID, DeliveryAssignmentStatus.CONFIRMED, DeliveryStatus.DELIVERING
        )).thenReturn(List.of());

        assertThatThrownBy(() -> service.update(AUTH_USER_ID, UserRole.RIDER, requestAt(NOW)))
            .isInstanceOf(RiderLocationNotAvailableException.class);

        verify(currentLocationRepository, never()).findByRiderIdForUpdate(any());
    }

    private RiderLocationUpdateRequest requestAt(OffsetDateTime capturedAt) {
        return new RiderLocationUpdateRequest(
            new BigDecimal("37.5665000"), new BigDecimal("126.9780000"), new BigDecimal("12.50"), capturedAt
        );
    }
}
