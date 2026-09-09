package com.chapchap.delivery.domain.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.service.RiderDeliveryAreaService;
import com.chapchap.delivery.domain.rider.service.RiderScheduleService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiderAssignmentEligibilityServiceTest {
    private static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 9, 7);
    private static final String AREA_CODE = "DAEGU_JUNG_GU";

    @Mock private DeliveryAccessService accessService;
    @Mock private RiderScheduleService scheduleService;
    @Mock private RiderDeliveryAreaService areaService;

    private RiderAssignmentEligibilityService service;

    @BeforeEach
    void setUp() {
        service = new RiderAssignmentEligibilityService(accessService, scheduleService, areaService);
    }

    @Test
    @DisplayName("활성·접근·근무·담당 지역을 모두 만족해야 자동 배정 후보가 된다")
    void eligibleWhenEveryConditionMatches() {
        Rider rider = rider(true);
        when(accessService.isRiderAccessAllowed(100L)).thenReturn(true);
        when(scheduleService.isWorking(10L, DELIVERY_DATE, DeliverySlotCode.LUNCH)).thenReturn(true);
        when(areaService.canDeliverToArea(10L, AREA_CODE, DELIVERY_DATE)).thenReturn(true);

        assertThat(service.isEligible(rider, DELIVERY_DATE, DeliverySlotCode.LUNCH, AREA_CODE))
            .isTrue();
    }

    @Test
    @DisplayName("배송업무 비활성 기사는 이후 조건을 조회하지 않고 제외한다")
    void inactiveRiderIsRejectedImmediately() {
        Rider rider = rider(false);

        assertThat(service.isEligible(rider, DELIVERY_DATE, DeliverySlotCode.LUNCH, AREA_CODE))
            .isFalse();

        verify(accessService, never()).isRiderAccessAllowed(100L);
        verify(scheduleService, never()).isWorking(10L, DELIVERY_DATE, DeliverySlotCode.LUNCH);
        verify(areaService, never()).canDeliverToArea(10L, AREA_CODE, DELIVERY_DATE);
    }

    @Test
    @DisplayName("지역 예외 수동 배정은 지역만 무시하고 접근·활성·근무 조건은 유지한다")
    void ignoringAreaStillRequiresAccessAndSchedule() {
        Rider rider = rider(true);
        when(accessService.isRiderAccessAllowed(100L)).thenReturn(true);
        when(scheduleService.isWorking(10L, DELIVERY_DATE, DeliverySlotCode.LUNCH)).thenReturn(true);

        assertThat(service.isEligibleIgnoringArea(rider, DELIVERY_DATE, DeliverySlotCode.LUNCH))
            .isTrue();

        verify(areaService, never()).canDeliverToArea(10L, AREA_CODE, DELIVERY_DATE);
    }

    private Rider rider(boolean active) {
        Rider rider = mock(Rider.class);
        lenient().when(rider.getId()).thenReturn(10L);
        lenient().when(rider.getAuthUserId()).thenReturn(100L);
        when(rider.getIsDeliveryActive()).thenReturn(active);
        return rider;
    }
}
