package com.chapchap.delivery.domain.riderlocation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.riderlocation.entity.RiderCurrentLocation;
import com.chapchap.delivery.domain.riderlocation.repository.RiderCurrentLocationRepository;
import com.chapchap.delivery.global.config.RiderLocationProperties;
import com.chapchap.delivery.global.exception.business.DeliveryNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryStateConflictException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class CustomerRiderLocationStreamServiceTest {
    private static final long CUSTOMER_ID = 1L;
    private static final long RIDER_ID = 10L;
    private static final long DELIVERY_ID = 101L;
    private static final String DELIVERY_PUBLIC_ID = "delivery-101";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryAssignmentItemRepository assignmentItemRepository;
    @Mock private RiderCurrentLocationRepository currentLocationRepository;
    @Mock private RiderLocationSseRegistry registry;
    @Mock private Delivery delivery;

    private CustomerRiderLocationStreamService service;

    @BeforeEach
    void setUp() {
        RiderLocationProperties properties = new RiderLocationProperties(
            50, Duration.ofSeconds(60), Duration.ofSeconds(90), Duration.ofSeconds(15), Duration.ofHours(1)
        );
        Clock clock = Clock.fixed(Instant.parse("2026-09-11T03:00:00Z"), KST);
        service = new CustomerRiderLocationStreamService(
            deliveryRepository, assignmentItemRepository, currentLocationRepository, registry, properties, clock
        );
    }

    @Test
    @DisplayName("본인의 배송 중 배송은 연결 직후 현재 위치를 조회해 SSE를 연다")
    void opensStreamForCustomerDeliveringDelivery() throws Exception {
        SseEmitter emitter = new SseEmitter();
        RiderCurrentLocation location = new RiderCurrentLocation(
            RIDER_ID, new BigDecimal("37.5665000"), new BigDecimal("126.9780000"), new BigDecimal("12.50"),
            LocalDateTime.of(2026, 9, 11, 12, 0), LocalDateTime.of(2026, 9, 11, 12, 0)
        );
        givenDeliveringDelivery(CUSTOMER_ID);
        when(registry.subscribe(DELIVERY_PUBLIC_ID, Duration.ofHours(1).toMillis())).thenReturn(emitter);
        when(currentLocationRepository.findById(RIDER_ID)).thenReturn(Optional.of(location));

        SseEmitter result = service.open(CUSTOMER_ID, UserRole.CUSTOMER, DELIVERY_PUBLIC_ID);

        assertThat(result).isSameAs(emitter);
        verify(registry).subscribe(DELIVERY_PUBLIC_ID, Duration.ofHours(1).toMillis());
        verify(currentLocationRepository).findById(RIDER_ID);
    }

    @Test
    @DisplayName("배송 중이 아닌 배송은 위치 SSE 연결을 거절한다")
    void rejectsNonDeliveringDelivery() throws Exception {
        when(deliveryRepository.findDetailByDeliveryPublicId(DELIVERY_PUBLIC_ID)).thenReturn(Optional.of(delivery));
        when(delivery.getCustomerId()).thenReturn(CUSTOMER_ID);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.READY);

        assertThatThrownBy(() -> service.open(CUSTOMER_ID, UserRole.CUSTOMER, DELIVERY_PUBLIC_ID))
            .isInstanceOf(DeliveryStateConflictException.class);

        verify(registry, never()).subscribe(eq(DELIVERY_PUBLIC_ID), eq(Duration.ofHours(1).toMillis()));
    }

    @Test
    @DisplayName("다른 고객의 배송은 존재하지 않는 배송처럼 처리한다")
    void hidesDeliveryFromOtherCustomer() throws Exception {
        givenDeliveringDelivery(CUSTOMER_ID + 1);

        assertThatThrownBy(() -> service.open(CUSTOMER_ID, UserRole.CUSTOMER, DELIVERY_PUBLIC_ID))
            .isInstanceOf(DeliveryNotFoundException.class);

        verify(registry, never()).subscribe(eq(DELIVERY_PUBLIC_ID), eq(Duration.ofHours(1).toMillis()));
    }

    private void givenDeliveringDelivery(long ownerId) {
        when(deliveryRepository.findDetailByDeliveryPublicId(DELIVERY_PUBLIC_ID)).thenReturn(Optional.of(delivery));
        when(delivery.getCustomerId()).thenReturn(ownerId);
        lenient().when(delivery.getStatus()).thenReturn(DeliveryStatus.DELIVERING);
        lenient().when(delivery.getId()).thenReturn(DELIVERY_ID);
        lenient().when(assignmentItemRepository.findConfirmedRiderIdsByDeliveryId(
            DELIVERY_ID, DeliveryAssignmentStatus.CONFIRMED
        )).thenReturn(java.util.List.of(RIDER_ID));
    }
}
