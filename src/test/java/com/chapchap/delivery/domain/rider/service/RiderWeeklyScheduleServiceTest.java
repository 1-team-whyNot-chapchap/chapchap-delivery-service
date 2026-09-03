package com.chapchap.delivery.domain.rider.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliverySlotRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.entity.RiderWeeklySchedule;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.rider.repository.RiderWeeklyScheduleRepository;
import com.chapchap.delivery.domain.rider.request.RiderWeeklyScheduleCreateRequest;
import com.chapchap.delivery.domain.rider.response.RiderWeeklyScheduleResponse;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import com.chapchap.delivery.global.exception.business.RiderWeeklyScheduleNotFoundException;
import com.chapchap.delivery.global.exception.technical.DeliverySlotConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderWeeklyScheduleServiceTest {
    private static final Long RIDER_ID =
        10L;

    private static final Long ACTOR_ID =
        9001L;

    private static final Long SLOT_ID =
        1L;

    @Mock
    private RiderRepository riderRepository;

    @Mock
    private RiderWeeklyScheduleRepository riderWeeklyScheduleRepository;

    @Mock
    private DeliverySlotRepository deliverySlotRepository;

    @Mock
    private DeliveryAccessService deliveryAccessService;

    @Mock
    private Rider rider;

    @Mock
    private DeliverySlot deliverySlot;

    @InjectMocks
    private RiderWeeklyScheduleService riderWeeklyScheduleService;

    @Test
    @DisplayName("새로운 주간 기본 일정을 등록한다")
    void createWeeklySchedule() {
        // given
        RiderWeeklyScheduleCreateRequest request =
            createRequest();

        stubRiderAndDeliverySlot();

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(RIDER_ID)
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            deliverySlotRepository
                .findByCodeAndDeletedAtIsNull(
                    DeliverySlotCode.LUNCH
                )
        ).thenReturn(
            Optional.of(deliverySlot)
        );

        when(
            riderWeeklyScheduleRepository
                .findByRiderIdAndDayOfWeekAndSlotId(
                    RIDER_ID
                    , (byte) 1
                    , SLOT_ID
                )
        ).thenReturn(
            Optional.empty()
        );

        when(
            riderWeeklyScheduleRepository.save(
                any(RiderWeeklySchedule.class)
            )
        ).thenAnswer(
            invocation -> {
                RiderWeeklySchedule schedule =
                    invocation.getArgument(0);

                ReflectionTestUtils.setField(
                    schedule
                    , "id"
                    , 100L
                );

                return schedule;
            }
        );

        // when
        RiderWeeklyScheduleResponse response =
            riderWeeklyScheduleService.createWeeklySchedule(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            );

        // then
        verify(deliveryAccessService)
            .validateAdminAccess(
                ACTOR_ID
                , UserRole.ADMIN
            );

        verify(riderWeeklyScheduleRepository)
            .save(
                any(RiderWeeklySchedule.class)
            );

        assertThat(response.scheduleId())
            .isEqualTo(100L);

        assertThat(response.riderId())
            .isEqualTo(RIDER_ID);

        assertThat(response.dayOfWeek())
            .isEqualTo((byte) 1);

        assertThat(response.deliverySlot())
            .isEqualTo(
                DeliverySlotCode.LUNCH
            );
    }

    @Test
    @DisplayName("삭제된 동일 주간 일정이 있으면 기존 일정을 복구한다")
    void restoreWeeklySchedule() {
        // given
        RiderWeeklyScheduleCreateRequest request =
            createRequest();

        stubRiderAndDeliverySlot();

        RiderWeeklySchedule schedule =
            new RiderWeeklySchedule(
                rider
                , (byte) 1
                , deliverySlot
            );

        ReflectionTestUtils.setField(
            schedule
            , "id"
            , 100L
        );

        schedule.delete(
            LocalDateTime.of(
                2026
                , 9
                , 1
                , 10
                , 0
            )
        );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(RIDER_ID)
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            deliverySlotRepository
                .findByCodeAndDeletedAtIsNull(
                    DeliverySlotCode.LUNCH
                )
        ).thenReturn(
            Optional.of(deliverySlot)
        );

        when(
            riderWeeklyScheduleRepository
                .findByRiderIdAndDayOfWeekAndSlotId(
                    RIDER_ID
                    , (byte) 1
                    , SLOT_ID
                )
        ).thenReturn(
            Optional.of(schedule)
        );

        // when
        RiderWeeklyScheduleResponse response =
            riderWeeklyScheduleService.createWeeklySchedule(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            );

        // then
        assertThat(schedule.getDeletedAt())
            .isNull();

        assertThat(response.scheduleId())
            .isEqualTo(100L);

        verify(
            riderWeeklyScheduleRepository
            , never()
        ).save(
            any(RiderWeeklySchedule.class)
        );
    }

    @Test
    @DisplayName("이미 활성 상태인 동일 일정이 있으면 중복 등록하지 않는다")
    void doesNotDuplicateActiveWeeklySchedule() {
        // given
        RiderWeeklyScheduleCreateRequest request =
            createRequest();

        stubRiderAndDeliverySlot();

        RiderWeeklySchedule schedule =
            new RiderWeeklySchedule(
                rider
                , (byte) 1
                , deliverySlot
            );

        ReflectionTestUtils.setField(
            schedule
            , "id"
            , 100L
        );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(RIDER_ID)
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            deliverySlotRepository
                .findByCodeAndDeletedAtIsNull(
                    DeliverySlotCode.LUNCH
                )
        ).thenReturn(
            Optional.of(deliverySlot)
        );

        when(
            riderWeeklyScheduleRepository
                .findByRiderIdAndDayOfWeekAndSlotId(
                    RIDER_ID
                    , (byte) 1
                    , SLOT_ID
                )
        ).thenReturn(
            Optional.of(schedule)
        );

        // when
        RiderWeeklyScheduleResponse response =
            riderWeeklyScheduleService.createWeeklySchedule(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            );

        // then
        assertThat(response.scheduleId())
            .isEqualTo(100L);

        assertThat(schedule.getDeletedAt())
            .isNull();

        verify(
            riderWeeklyScheduleRepository
            , never()
        ).save(
            any(RiderWeeklySchedule.class)
        );
    }

    @Test
    @DisplayName("존재하지 않는 기사에 일정을 등록하면 예외가 발생한다")
    void throwsExceptionWhenRiderNotFound() {
        // given
        RiderWeeklyScheduleCreateRequest request =
            createRequest();

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(RIDER_ID)
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> riderWeeklyScheduleService
                .createWeeklySchedule(
                    RIDER_ID
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                )
        ).isInstanceOf(
            RiderNotFoundException.class
        );

        verifyNoInteractions(
            deliverySlotRepository
            , riderWeeklyScheduleRepository
        );
    }

    @Test
    @DisplayName("배송 시간대 기준 정보가 없으면 기술 예외가 발생한다")
    void throwsExceptionWhenDeliverySlotNotFound() {
        // given
        RiderWeeklyScheduleCreateRequest request =
            createRequest();

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(RIDER_ID)
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            deliverySlotRepository
                .findByCodeAndDeletedAtIsNull(
                    DeliverySlotCode.LUNCH
                )
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> riderWeeklyScheduleService
                .createWeeklySchedule(
                    RIDER_ID
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                )
        ).isInstanceOf(
            DeliverySlotConfigurationException.class
        );

        verifyNoInteractions(
            riderWeeklyScheduleRepository
        );
    }

    @Test
    @DisplayName("관리자 접근 검증에 실패하면 일정을 등록하지 않는다")
    void throwsExceptionWhenAdminAccessDenied() {
        // given
        RiderWeeklyScheduleCreateRequest request =
            createRequest();

        doThrow(
            new DeliveryAccessForbiddenException()
        ).when(
            deliveryAccessService
        ).validateAdminAccess(
            ACTOR_ID
            , UserRole.ADMIN
        );

        // when & then
        assertThatThrownBy(
            () -> riderWeeklyScheduleService
                .createWeeklySchedule(
                    RIDER_ID
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                )
        ).isInstanceOf(
            DeliveryAccessForbiddenException.class
        );

        verifyNoInteractions(
            riderRepository
            , deliverySlotRepository
            , riderWeeklyScheduleRepository
        );
    }

    @Test
    @DisplayName("기사의 현재 유효한 주간 기본 일정 목록을 조회한다")
    void getWeeklySchedules() {
        // given
        DeliverySlot dinnerSlot =
            org.mockito.Mockito.mock(
                DeliverySlot.class
            );

        when(rider.getId())
            .thenReturn(RIDER_ID);

        when(deliverySlot.getCode())
            .thenReturn(
                DeliverySlotCode.LUNCH
            );

        when(dinnerSlot.getCode())
            .thenReturn(
                DeliverySlotCode.DINNER
            );

        RiderWeeklySchedule lunchSchedule =
            new RiderWeeklySchedule(
                rider
                , (byte) 1
                , deliverySlot
            );

        RiderWeeklySchedule dinnerSchedule =
            new RiderWeeklySchedule(
                rider
                , (byte) 3
                , dinnerSlot
            );

        ReflectionTestUtils.setField(
            lunchSchedule
            , "id"
            , 100L
        );

        ReflectionTestUtils.setField(
            dinnerSchedule
            , "id"
            , 101L
        );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(RIDER_ID)
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderWeeklyScheduleRepository
                .findAllByRiderIdAndDeletedAtIsNull(
                    RIDER_ID
                )
        ).thenReturn(
            java.util.List.of(
                lunchSchedule
                , dinnerSchedule
            )
        );

        // when
        java.util.List<RiderWeeklyScheduleResponse> responses =
            riderWeeklyScheduleService.getWeeklySchedules(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
            );

        // then
        verify(deliveryAccessService)
            .validateAdminAccess(
                ACTOR_ID
                , UserRole.ADMIN
            );

        assertThat(responses)
            .hasSize(2);

        assertThat(responses.getFirst().scheduleId())
            .isEqualTo(100L);

        assertThat(responses.getFirst().riderId())
            .isEqualTo(RIDER_ID);

        assertThat(responses.get(0).dayOfWeek())
            .isEqualTo((byte) 1);

        assertThat(responses.get(0).deliverySlot())
            .isEqualTo(
                DeliverySlotCode.LUNCH
            );

        assertThat(responses.get(1).scheduleId())
            .isEqualTo(101L);

        assertThat(responses.get(1).dayOfWeek())
            .isEqualTo((byte) 3);

        assertThat(responses.get(1).deliverySlot())
            .isEqualTo(
                DeliverySlotCode.DINNER
            );
    }

    @Test
    @DisplayName("주간 기본 일정이 없으면 빈 목록을 반환한다")
    void getWeeklySchedulesWhenEmpty() {
        // given
        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(RIDER_ID)
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderWeeklyScheduleRepository
                .findAllByRiderIdAndDeletedAtIsNull(
                    RIDER_ID
                )
        ).thenReturn(
            java.util.List.of()
        );

        // when
        java.util.List<RiderWeeklyScheduleResponse> responses =
            riderWeeklyScheduleService.getWeeklySchedules(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
            );

        // then
        assertThat(responses)
            .isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 기사의 주간 일정을 조회하면 예외가 발생한다")
    void getWeeklySchedulesThrowsExceptionWhenRiderNotFound() {
        // given
        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(RIDER_ID)
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> riderWeeklyScheduleService
                .getWeeklySchedules(
                    RIDER_ID
                    , ACTOR_ID
                    , UserRole.ADMIN
                )
        ).isInstanceOf(
            RiderNotFoundException.class
        );

        verifyNoInteractions(
            riderWeeklyScheduleRepository
        );
    }

    @Test
    @DisplayName("관리자 접근 검증에 실패하면 주간 일정을 조회하지 않는다")
    void getWeeklySchedulesThrowsExceptionWhenAdminAccessDenied() {
        // given
        doThrow(
            new DeliveryAccessForbiddenException()
        ).when(
            deliveryAccessService
        ).validateAdminAccess(
            ACTOR_ID
            , UserRole.ADMIN
        );

        // when & then
        assertThatThrownBy(
            () -> riderWeeklyScheduleService
                .getWeeklySchedules(
                    RIDER_ID
                    , ACTOR_ID
                    , UserRole.ADMIN
                )
        ).isInstanceOf(
            DeliveryAccessForbiddenException.class
        );

        verifyNoInteractions(
            riderRepository
            , riderWeeklyScheduleRepository
        );
    }

    @Test
    @DisplayName("주간 기본 일정을 삭제 처리한다")
    void deleteWeeklySchedule() {
        // given
        RiderWeeklySchedule schedule =
            new RiderWeeklySchedule(
                rider
                , (byte) 1
                , deliverySlot
            );

        ReflectionTestUtils.setField(
            schedule
            , "id"
            , 100L
        );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(RIDER_ID)
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderWeeklyScheduleRepository
                .findByIdAndRiderIdAndDeletedAtIsNull(
                    100L
                    , RIDER_ID
                )
        ).thenReturn(
            Optional.of(schedule)
        );

        // when
        riderWeeklyScheduleService.deleteWeeklySchedule(
            RIDER_ID
            , 100L
            , ACTOR_ID
            , UserRole.ADMIN
        );

        // then
        verify(deliveryAccessService)
            .validateAdminAccess(
                ACTOR_ID
                , UserRole.ADMIN
            );

        assertThat(schedule.getDeletedAt())
            .isNotNull();

        verify(
            riderWeeklyScheduleRepository
            , never()
        ).save(
            any(RiderWeeklySchedule.class)
        );
    }

    @Test
    @DisplayName("존재하지 않는 기사의 주간 일정을 삭제하면 예외가 발생한다")
    void deleteWeeklyScheduleThrowsExceptionWhenRiderNotFound() {
        // given
        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(RIDER_ID)
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> riderWeeklyScheduleService
                .deleteWeeklySchedule(
                    RIDER_ID
                    , 100L
                    , ACTOR_ID
                    , UserRole.ADMIN
                )
        ).isInstanceOf(
            RiderNotFoundException.class
        );

        verifyNoInteractions(
            riderWeeklyScheduleRepository
        );
    }

    @Test
    @DisplayName("존재하지 않는 주간 기본 일정을 삭제하면 예외가 발생한다")
    void deleteWeeklyScheduleThrowsExceptionWhenScheduleNotFound() {
        // given
        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(RIDER_ID)
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderWeeklyScheduleRepository
                .findByIdAndRiderIdAndDeletedAtIsNull(
                    100L
                    , RIDER_ID
                )
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> riderWeeklyScheduleService
                .deleteWeeklySchedule(
                    RIDER_ID
                    , 100L
                    , ACTOR_ID
                    , UserRole.ADMIN
                )
        ).isInstanceOf(
            RiderWeeklyScheduleNotFoundException.class
        );
    }

    private RiderWeeklyScheduleCreateRequest createRequest() {
        return new RiderWeeklyScheduleCreateRequest(
            (byte) 1
            , DeliverySlotCode.LUNCH
        );
    }

    @Test
    @DisplayName("관리자 접근 검증에 실패하면 주간 일정을 삭제하지 않는다")
    void deleteWeeklyScheduleThrowsExceptionWhenAdminAccessDenied() {
        // given
        doThrow(
            new DeliveryAccessForbiddenException()
        ).when(
            deliveryAccessService
        ).validateAdminAccess(
            ACTOR_ID
            , UserRole.ADMIN
        );

        // when & then
        assertThatThrownBy(
            () -> riderWeeklyScheduleService
                .deleteWeeklySchedule(
                    RIDER_ID
                    , 100L
                    , ACTOR_ID
                    , UserRole.ADMIN
                )
        ).isInstanceOf(
            DeliveryAccessForbiddenException.class
        );

        verifyNoInteractions(
            riderRepository
            , riderWeeklyScheduleRepository
        );
    }

    private void stubRiderAndDeliverySlot() {
        when(rider.getId())
            .thenReturn(RIDER_ID);

        when(deliverySlot.getId())
            .thenReturn(SLOT_ID);

        when(deliverySlot.getCode())
            .thenReturn(
                DeliverySlotCode.LUNCH
            );
    }
}