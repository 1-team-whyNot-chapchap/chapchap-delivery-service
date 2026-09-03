package com.chapchap.delivery.domain.rider.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.rider.constant.RiderScheduleSource;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.entity.RiderScheduleException;
import com.chapchap.delivery.domain.rider.entity.RiderWeeklySchedule;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.rider.repository.RiderScheduleExceptionRepository;
import com.chapchap.delivery.domain.rider.repository.RiderWeeklyScheduleRepository;
import com.chapchap.delivery.domain.rider.response.RiderScheduleResponse;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderScheduleServiceTest {

    private static final Long RIDER_ID = 10L;
    private static final Long ACTOR_ID = 10001L;

    private static final LocalDate DATE_FROM =
        LocalDate.of(2026, 8, 24);

    private static final LocalDate DATE_TO =
        LocalDate.of(2026, 8, 25);

    @Mock
    private RiderRepository riderRepository;

    @Mock
    private RiderWeeklyScheduleRepository riderWeeklyScheduleRepository;

    @Mock
    private RiderScheduleExceptionRepository riderScheduleExceptionRepository;

    @Mock
    private DeliveryAccessService deliveryAccessService;

    @Test
    @DisplayName("주간 기본 일정을 날짜별 실제 일정으로 변환한다")
    void getMySchedulesFromWeeklySchedules() {
        // given
        RiderScheduleService service =
            createService();

        Rider rider =
            createRider(
                true
            );

        RiderWeeklySchedule mondayLunch =
            createWeeklySchedule(
                (byte) 1
                , DeliverySlotCode.LUNCH
            );

        RiderWeeklySchedule tuesdayDinner =
            createWeeklySchedule(
                (byte) 2
                , DeliverySlotCode.DINNER
            );

        stubRider(
            rider
        );

        when(
            riderWeeklyScheduleRepository.findAllByRiderIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            List.of(
                mondayLunch
                , tuesdayDinner
            )
        );

        when(
            riderScheduleExceptionRepository
                .findAllByRiderIdAndScheduleDateBetweenAndDeletedAtIsNullOrderByScheduleDateAsc(
                    RIDER_ID
                    , DATE_FROM
                    , DATE_TO
                )
        ).thenReturn(
            List.of()
        );

        // when
        RiderScheduleResponse response =
            service.getMySchedules(
                ACTOR_ID
                , UserRole.RIDER
                , DATE_FROM
                , DATE_TO
            );

        // then
        assertThat(response.riderId())
            .isEqualTo(RIDER_ID);

        assertThat(response.isDeliveryActive())
            .isTrue();

        assertThat(response.schedules())
            .hasSize(2);

        assertThat(response.schedules().getFirst().date())
            .isEqualTo(LocalDate.of(2026, 8, 24));

        assertThat(response.schedules().getFirst().deliverySlot())
            .isEqualTo(DeliverySlotCode.LUNCH);

        assertThat(response.schedules().get(0).isWorking())
            .isTrue();

        assertThat(response.schedules().get(0).source())
            .isEqualTo(RiderScheduleSource.WEEKLY_DEFAULT);

        assertThat(response.schedules().get(1).date())
            .isEqualTo(LocalDate.of(2026, 8, 25));

        assertThat(response.schedules().get(1).deliverySlot())
            .isEqualTo(DeliverySlotCode.DINNER);

        assertThat(response.schedules().get(1).source())
            .isEqualTo(RiderScheduleSource.WEEKLY_DEFAULT);
    }

    @Test
    @DisplayName("날짜별 예외 일정은 동일 날짜와 시간대의 주간 기본 일정보다 우선한다")
    void getMySchedulesPrioritizesDateException() {
        // given
        RiderScheduleService service =
            createService();

        Rider rider =
            createRider(
                true
            );

        RiderWeeklySchedule weeklySchedule =
            createWeeklySchedule(
                (byte) 1
                , DeliverySlotCode.LUNCH
            );

        RiderScheduleException scheduleException =
            createScheduleException(
                LocalDate.of(2026, 8, 24)
                , DeliverySlotCode.LUNCH
                , false
            );

        stubRider(
            rider
        );

        when(
            riderWeeklyScheduleRepository.findAllByRiderIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            List.of(
                weeklySchedule
            )
        );

        when(
            riderScheduleExceptionRepository
                .findAllByRiderIdAndScheduleDateBetweenAndDeletedAtIsNullOrderByScheduleDateAsc(
                    RIDER_ID
                    , DATE_FROM
                    , DATE_TO
                )
        ).thenReturn(
            List.of(
                scheduleException
            )
        );

        // when
        RiderScheduleResponse response =
            service.getMySchedules(
                ACTOR_ID
                , UserRole.RIDER
                , DATE_FROM
                , DATE_TO
            );

        // then
        assertThat(response.schedules())
            .hasSize(1);

        assertThat(response.schedules().getFirst().date())
            .isEqualTo(LocalDate.of(2026, 8, 24));

        assertThat(response.schedules().getFirst().deliverySlot())
            .isEqualTo(DeliverySlotCode.LUNCH);

        assertThat(response.schedules().getFirst().isWorking())
            .isFalse();

        assertThat(response.schedules().getFirst().source())
            .isEqualTo(RiderScheduleSource.DATE_EXCEPTION);
    }

    @Test
    @DisplayName("주간 기본 일정이 없어도 근무 예외 일정이 있으면 실제 일정에 포함한다")
    void getMySchedulesIncludesWorkingDateExceptionWithoutWeeklySchedule() {
        // given
        RiderScheduleService service =
            createService();

        Rider rider =
            createRider(
                true
            );

        RiderScheduleException scheduleException =
            createScheduleException(
                LocalDate.of(2026, 8, 25)
                , DeliverySlotCode.DINNER
                , true
            );

        stubRider(
            rider
        );

        when(
            riderWeeklyScheduleRepository.findAllByRiderIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            List.of()
        );

        when(
            riderScheduleExceptionRepository
                .findAllByRiderIdAndScheduleDateBetweenAndDeletedAtIsNullOrderByScheduleDateAsc(
                    RIDER_ID
                    , DATE_FROM
                    , DATE_TO
                )
        ).thenReturn(
            List.of(
                scheduleException
            )
        );

        // when
        RiderScheduleResponse response =
            service.getMySchedules(
                ACTOR_ID
                , UserRole.RIDER
                , DATE_FROM
                , DATE_TO
            );

        // then
        assertThat(response.schedules())
            .hasSize(1);

        assertThat(response.schedules().getFirst().date())
            .isEqualTo(LocalDate.of(2026, 8, 25));

        assertThat(response.schedules().getFirst().deliverySlot())
            .isEqualTo(DeliverySlotCode.DINNER);

        assertThat(response.schedules().getFirst().isWorking())
            .isTrue();

        assertThat(response.schedules().getFirst().source())
            .isEqualTo(RiderScheduleSource.DATE_EXCEPTION);
    }

    @Test
    @DisplayName("기본 일정과 날짜별 예외가 모두 없으면 실제 일정 목록은 비어 있다")
    void getMySchedulesReturnsEmptySchedules() {
        // given
        RiderScheduleService service =
            createService();

        Rider rider =
            createRider(
                true
            );

        stubRider(
            rider
        );

        when(
            riderWeeklyScheduleRepository.findAllByRiderIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            List.of()
        );

        when(
            riderScheduleExceptionRepository
                .findAllByRiderIdAndScheduleDateBetweenAndDeletedAtIsNullOrderByScheduleDateAsc(
                    RIDER_ID
                    , DATE_FROM
                    , DATE_TO
                )
        ).thenReturn(
            List.of()
        );

        // when
        RiderScheduleResponse response =
            service.getMySchedules(
                ACTOR_ID
                , UserRole.RIDER
                , DATE_FROM
                , DATE_TO
            );

        // then
        assertThat(response.schedules())
            .isEmpty();
    }

    @Test
    @DisplayName("배송업무 비활성 기사도 현재 활성 상태와 실제 일정을 조회할 수 있다")
    void getMySchedulesReturnsInactiveState() {
        // given
        RiderScheduleService service =
            createService();

        Rider rider =
            createRider(
                false
            );

        RiderWeeklySchedule weeklySchedule =
            createWeeklySchedule(
                (byte) 1
                , DeliverySlotCode.LUNCH
            );

        stubRider(
            rider
        );

        when(
            riderWeeklyScheduleRepository.findAllByRiderIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            List.of(
                weeklySchedule
            )
        );

        when(
            riderScheduleExceptionRepository
                .findAllByRiderIdAndScheduleDateBetweenAndDeletedAtIsNullOrderByScheduleDateAsc(
                    RIDER_ID
                    , DATE_FROM
                    , DATE_TO
                )
        ).thenReturn(
            List.of()
        );

        // when
        RiderScheduleResponse response =
            service.getMySchedules(
                ACTOR_ID
                , UserRole.RIDER
                , DATE_FROM
                , DATE_TO
            );

        // then
        assertThat(response.isDeliveryActive())
            .isFalse();

        assertThat(response.schedules())
            .hasSize(1);
    }

    @Test
    @DisplayName("현재 사용자와 연결된 기사 프로필이 없으면 일정을 조회할 수 없다")
    void getMySchedulesThrowsExceptionWhenRiderNotFound() {
        // given
        RiderScheduleService service =
            createService();

        when(
            riderRepository.findByAuthUserIdAndDeletedAtIsNull(
                ACTOR_ID
            )
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> service.getMySchedules(
                ACTOR_ID
                , UserRole.RIDER
                , DATE_FROM
                , DATE_TO
            )
        ).isInstanceOf(
            RiderNotFoundException.class
        );

        verifyNoInteractions(
            riderWeeklyScheduleRepository
            , riderScheduleExceptionRepository
        );
    }

    @Test
    @DisplayName("Delivery 접근이 허용되지 않은 기사는 본인 일정을 조회할 수 없다")
    void getMySchedulesThrowsExceptionWhenRiderAccessDenied() {
        // given
        RiderScheduleService service =
            createService();

        doThrow(
            new DeliveryAccessForbiddenException()
        ).when(
            deliveryAccessService
        ).validateRiderAccess(
            ACTOR_ID
            , UserRole.RIDER
        );

        // when & then
        assertThatThrownBy(
            () -> service.getMySchedules(
                ACTOR_ID
                , UserRole.RIDER
                , DATE_FROM
                , DATE_TO
            )
        ).isInstanceOf(
            DeliveryAccessForbiddenException.class
        );

        verifyNoInteractions(
            riderRepository
            , riderWeeklyScheduleRepository
            , riderScheduleExceptionRepository
        );
    }

    private RiderScheduleService createService() {
        return new RiderScheduleService(
            riderRepository
            , riderWeeklyScheduleRepository
            , riderScheduleExceptionRepository
            , deliveryAccessService
        );
    }

    private Rider createRider(
        Boolean isDeliveryActive
    ) {
        Rider rider =
            mock(Rider.class);

        when(
            rider.getId()
        ).thenReturn(
            RIDER_ID
        );

        when(
            rider.getIsDeliveryActive()
        ).thenReturn(
            isDeliveryActive
        );

        return rider;
    }

    private RiderWeeklySchedule createWeeklySchedule(
        Byte dayOfWeek
        , DeliverySlotCode deliverySlotCode
    ) {
        DeliverySlot deliverySlot =
            mock(DeliverySlot.class);

        when(
            deliverySlot.getCode()
        ).thenReturn(
            deliverySlotCode
        );

        RiderWeeklySchedule weeklySchedule =
            mock(RiderWeeklySchedule.class);

        when(
            weeklySchedule.getDayOfWeek()
        ).thenReturn(
            dayOfWeek
        );

        when(
            weeklySchedule.getSlot()
        ).thenReturn(
            deliverySlot
        );

        return weeklySchedule;
    }

    private RiderScheduleException createScheduleException(
        LocalDate scheduleDate
        , DeliverySlotCode deliverySlotCode
        , Boolean isWorking
    ) {
        DeliverySlot deliverySlot =
            mock(DeliverySlot.class);

        when(
            deliverySlot.getCode()
        ).thenReturn(
            deliverySlotCode
        );

        RiderScheduleException scheduleException =
            mock(RiderScheduleException.class);

        when(
            scheduleException.getScheduleDate()
        ).thenReturn(
            scheduleDate
        );

        when(
            scheduleException.getSlot()
        ).thenReturn(
            deliverySlot
        );

        when(
            scheduleException.getIsWorking()
        ).thenReturn(
            isWorking
        );

        return scheduleException;
    }

    private void stubRider(
        Rider rider
    ) {
        when(
            riderRepository.findByAuthUserIdAndDeletedAtIsNull(
                ACTOR_ID
            )
        ).thenReturn(
            Optional.of(rider)
        );
    }
}