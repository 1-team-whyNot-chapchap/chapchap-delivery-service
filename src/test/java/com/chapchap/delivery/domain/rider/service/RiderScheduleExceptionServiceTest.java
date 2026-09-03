package com.chapchap.delivery.domain.rider.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliverySlotRepository;
import com.chapchap.delivery.domain.rider.constant.RiderScheduleExceptionReason;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.entity.RiderScheduleException;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.rider.repository.RiderScheduleExceptionRepository;
import com.chapchap.delivery.domain.rider.request.RiderScheduleExceptionCreateRequest;
import com.chapchap.delivery.domain.rider.request.RiderScheduleExceptionUpdateRequest;
import com.chapchap.delivery.domain.rider.response.RiderScheduleExceptionResponse;
import com.chapchap.delivery.global.exception.business.*;
import com.chapchap.delivery.global.exception.technical.DeliverySlotConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
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
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class RiderScheduleExceptionServiceTest {
    private static final Long RIDER_ID =
        10L;

    private static final Long ACTOR_ID =
        9001L;

    private static final Long SLOT_ID =
        1L;

    private static final LocalDate SCHEDULE_DATE =
        LocalDate.of(
            2026
            , 9
            , 5
        );

    @Mock
    private RiderRepository riderRepository;

    @Mock
    private RiderScheduleExceptionRepository
        riderScheduleExceptionRepository;

    @Mock
    private DeliverySlotRepository deliverySlotRepository;

    @Mock
    private DeliveryAccessService deliveryAccessService;

    @Mock
    private Rider rider;

    @Mock
    private DeliverySlot deliverySlot;

    @InjectMocks
    private RiderScheduleExceptionService
        riderScheduleExceptionService;

    @Test
    @DisplayName("새로운 날짜별 예외 일정을 등록한다")
    void createScheduleException() {
        // given
        RiderScheduleExceptionCreateRequest request =
            createRequest();

        stubRiderAndDeliverySlot();

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
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
            riderScheduleExceptionRepository
                .findByRiderIdAndScheduleDateAndSlotId(
                    RIDER_ID
                    , SCHEDULE_DATE
                    , SLOT_ID
                )
        ).thenReturn(
            Optional.empty()
        );

        when(
            riderScheduleExceptionRepository.save(
                any(RiderScheduleException.class)
            )
        ).thenAnswer(
            invocation -> {
                RiderScheduleException scheduleException =
                    invocation.getArgument(0);

                ReflectionTestUtils.setField(
                    scheduleException
                    , "id"
                    , 100L
                );

                ReflectionTestUtils.setField(
                    scheduleException
                    , "version"
                    , 0L
                );

                return scheduleException;
            }
        );

        // when
        RiderScheduleExceptionResponse response =
            riderScheduleExceptionService
                .createScheduleException(
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

        verify(riderScheduleExceptionRepository)
            .save(
                any(RiderScheduleException.class)
            );

        assertThat(response.exceptionId())
            .isEqualTo(100L);

        assertThat(response.riderId())
            .isEqualTo(RIDER_ID);

        assertThat(response.scheduleDate())
            .isEqualTo(SCHEDULE_DATE);

        assertThat(response.deliverySlot())
            .isEqualTo(
                DeliverySlotCode.LUNCH
            );

        assertThat(response.isWorking())
            .isFalse();

        assertThat(response.reasonCode())
            .isEqualTo(
                RiderScheduleExceptionReason.TRAINING
            );

        assertThat(response.reasonDetail())
            .isEqualTo(
                "신규 기사 교육"
            );

        assertThat(response.version())
            .isZero();
    }

    @Test
    @DisplayName("삭제된 동일 날짜별 예외 일정이 있으면 기존 일정을 복구한다")
    void restoreScheduleException() {
        // given
        RiderScheduleExceptionCreateRequest request =
            createRequest();

        stubRiderAndDeliverySlot();

        RiderScheduleException scheduleException =
            new RiderScheduleException(
                rider
                , SCHEDULE_DATE
                , deliverySlot
                , true
                , RiderScheduleExceptionReason.SUBSTITUTE_WORK
                , "기존 대체 근무"
                , 8000L
            );

        ReflectionTestUtils.setField(
            scheduleException
            , "id"
            , 100L
        );

        ReflectionTestUtils.setField(
            scheduleException
            , "version"
            , 1L
        );

        scheduleException.delete(
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
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
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
            riderScheduleExceptionRepository
                .findByRiderIdAndScheduleDateAndSlotId(
                    RIDER_ID
                    , SCHEDULE_DATE
                    , SLOT_ID
                )
        ).thenReturn(
            Optional.of(scheduleException)
        );

        // when
        RiderScheduleExceptionResponse response =
            riderScheduleExceptionService
                .createScheduleException(
                    RIDER_ID
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                );

        // then
        assertThat(scheduleException.getDeletedAt())
            .isNull();

        assertThat(scheduleException.getIsWorking())
            .isFalse();

        assertThat(scheduleException.getReasonCode())
            .isEqualTo(
                RiderScheduleExceptionReason.TRAINING
            );

        assertThat(scheduleException.getReasonDetail())
            .isEqualTo(
                "신규 기사 교육"
            );

        assertThat(scheduleException.getCreatedBy())
            .isEqualTo(8000L);

        assertThat(response.exceptionId())
            .isEqualTo(100L);

        verify(
            riderScheduleExceptionRepository
            , never()
        ).save(
            any(RiderScheduleException.class)
        );
    }

    @Test
    @DisplayName("이미 활성 상태인 동일 예외 일정과 요청 내용이 같으면 중복 등록하지 않는다")
    void doesNotDuplicateSameActiveScheduleException() {
        // given
        RiderScheduleExceptionCreateRequest request =
            createRequest();

        stubRiderAndDeliverySlot();

        RiderScheduleException scheduleException =
            new RiderScheduleException(
                rider
                , SCHEDULE_DATE
                , deliverySlot
                , false
                , RiderScheduleExceptionReason.TRAINING
                , "신규 기사 교육"
                , ACTOR_ID
            );

        ReflectionTestUtils.setField(
            scheduleException
            , "id"
            , 100L
        );

        ReflectionTestUtils.setField(
            scheduleException
            , "version"
            , 0L
        );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
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
            riderScheduleExceptionRepository
                .findByRiderIdAndScheduleDateAndSlotId(
                    RIDER_ID
                    , SCHEDULE_DATE
                    , SLOT_ID
                )
        ).thenReturn(
            Optional.of(scheduleException)
        );

        // when
        RiderScheduleExceptionResponse response =
            riderScheduleExceptionService
                .createScheduleException(
                    RIDER_ID
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                );

        // then
        assertThat(response.exceptionId())
            .isEqualTo(100L);

        assertThat(response.isWorking())
            .isFalse();

        assertThat(response.reasonCode())
            .isEqualTo(
                RiderScheduleExceptionReason.TRAINING
            );

        verify(
            riderScheduleExceptionRepository
            , never()
        ).save(
            any(RiderScheduleException.class)
        );
    }

    @Test
    @DisplayName("이미 활성 상태인 동일 날짜와 시간대에 다른 내용으로 등록하면 충돌 예외가 발생한다")
    void throwsExceptionWhenActiveScheduleExceptionConflicts() {
        // given
        RiderScheduleExceptionCreateRequest request =
            createRequest();

        when(deliverySlot.getId())
            .thenReturn(SLOT_ID);

        RiderScheduleException scheduleException =
            new RiderScheduleException(
                rider
                , SCHEDULE_DATE
                , deliverySlot
                , true
                , RiderScheduleExceptionReason.SUBSTITUTE_WORK
                , "기존 대체 근무"
                , ACTOR_ID
            );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
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
            riderScheduleExceptionRepository
                .findByRiderIdAndScheduleDateAndSlotId(
                    RIDER_ID
                    , SCHEDULE_DATE
                    , SLOT_ID
                )
        ).thenReturn(
            Optional.of(scheduleException)
        );

        // when & then
        assertThatThrownBy(
            () -> riderScheduleExceptionService
                .createScheduleException(
                    RIDER_ID
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                )
        ).isInstanceOf(
            RiderScheduleExceptionConflictException.class
        );

        verify(
            riderScheduleExceptionRepository
            , never()
        ).save(
            any(RiderScheduleException.class)
        );
    }

    @Test
    @DisplayName("OTHER 사유에 상세 사유가 없으면 예외가 발생한다")
    void throwsExceptionWhenOtherReasonDetailMissing() {
        // given
        RiderScheduleExceptionCreateRequest request =
            new RiderScheduleExceptionCreateRequest(
                SCHEDULE_DATE
                , DeliverySlotCode.LUNCH
                , false
                , RiderScheduleExceptionReason.OTHER
                , null
            );

        // when & then
        assertThatThrownBy(
            () -> riderScheduleExceptionService
                .createScheduleException(
                    RIDER_ID
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                )
        ).isInstanceOf(
            OtherReasonDetailRequiredException.class
        );

        verifyNoInteractions(
            riderRepository
            , deliverySlotRepository
            , riderScheduleExceptionRepository
        );
    }

    @Test
    @DisplayName("존재하지 않는 기사에 날짜별 예외 일정을 등록하면 예외가 발생한다")
    void throwsExceptionWhenRiderNotFound() {
        // given
        RiderScheduleExceptionCreateRequest request =
            createRequest();

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> riderScheduleExceptionService
                .createScheduleException(
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
            , riderScheduleExceptionRepository
        );
    }

    @Test
    @DisplayName("배송 시간대 기준 정보가 없으면 기술 예외가 발생한다")
    void throwsExceptionWhenDeliverySlotNotFound() {
        // given
        RiderScheduleExceptionCreateRequest request =
            createRequest();

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
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
            () -> riderScheduleExceptionService
                .createScheduleException(
                    RIDER_ID
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                )
        ).isInstanceOf(
            DeliverySlotConfigurationException.class
        );

        verifyNoInteractions(
            riderScheduleExceptionRepository
        );
    }

    @Test
    @DisplayName("관리자 접근 검증에 실패하면 날짜별 예외 일정을 등록하지 않는다")
    void throwsExceptionWhenAdminAccessDenied() {
        // given
        RiderScheduleExceptionCreateRequest request =
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
            () -> riderScheduleExceptionService
                .createScheduleException(
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
            , riderScheduleExceptionRepository
        );
    }

    private RiderScheduleExceptionCreateRequest createRequest() {
        return new RiderScheduleExceptionCreateRequest(
            SCHEDULE_DATE
            , DeliverySlotCode.LUNCH
            , false
            , RiderScheduleExceptionReason.TRAINING
            , "신규 기사 교육"
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

    @Test
    @DisplayName("기간 내 날짜별 예외 일정 목록을 조회한다")
    void getScheduleExceptions() {
        // given
        LocalDate dateFrom =
            LocalDate.of(
                2026
                , 9
                , 1
            );

        LocalDate dateTo =
            LocalDate.of(
                2026
                , 9
                , 7
            );

        RiderScheduleException firstException =
            new RiderScheduleException(
                rider
                , LocalDate.of(
                2026
                , 9
                , 3
            )
                , deliverySlot
                , false
                , RiderScheduleExceptionReason.ANNUAL_LEAVE
                , "연차"
                , ACTOR_ID
            );

        RiderScheduleException secondException =
            new RiderScheduleException(
                rider
                , LocalDate.of(
                2026
                , 9
                , 5
            )
                , deliverySlot
                , false
                , RiderScheduleExceptionReason.TRAINING
                , "신규 기사 교육"
                , ACTOR_ID
            );

        ReflectionTestUtils.setField(
            firstException
            , "id"
            , 100L
        );

        ReflectionTestUtils.setField(
            firstException
            , "version"
            , 0L
        );

        ReflectionTestUtils.setField(
            secondException
            , "id"
            , 101L
        );

        ReflectionTestUtils.setField(
            secondException
            , "version"
            , 1L
        );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
        ).thenReturn(
            Optional.of(rider)
        );

        when(rider.getId())
            .thenReturn(RIDER_ID);

        when(deliverySlot.getCode())
            .thenReturn(
                DeliverySlotCode.LUNCH
            );

        when(
            riderScheduleExceptionRepository
                .findAllByRiderIdAndScheduleDateBetweenAndDeletedAtIsNullOrderByScheduleDateAsc(
                    RIDER_ID
                    , dateFrom
                    , dateTo
                )
        ).thenReturn(
            java.util.List.of(
                firstException
                , secondException
            )
        );

        // when
        java.util.List<RiderScheduleExceptionResponse> responses =
            riderScheduleExceptionService
                .getScheduleExceptions(
                    RIDER_ID
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , dateFrom
                    , dateTo
                );

        // then
        verify(deliveryAccessService)
            .validateAdminAccess(
                ACTOR_ID
                , UserRole.ADMIN
            );

        verify(riderScheduleExceptionRepository)
            .findAllByRiderIdAndScheduleDateBetweenAndDeletedAtIsNullOrderByScheduleDateAsc(
                RIDER_ID
                , dateFrom
                , dateTo
            );

        assertThat(responses)
            .hasSize(2);

        assertThat(responses.getFirst().exceptionId())
            .isEqualTo(100L);

        assertThat(responses.getFirst().riderId())
            .isEqualTo(RIDER_ID);

        assertThat(responses.getFirst().scheduleDate())
            .isEqualTo(
                LocalDate.of(
                    2026
                    , 9
                    , 3
                )
            );

        assertThat(responses.getFirst().deliverySlot())
            .isEqualTo(
                DeliverySlotCode.LUNCH
            );

        assertThat(responses.get(0).isWorking())
            .isFalse();

        assertThat(responses.get(0).reasonCode())
            .isEqualTo(
                RiderScheduleExceptionReason.ANNUAL_LEAVE
            );

        assertThat(responses.get(1).exceptionId())
            .isEqualTo(101L);

        assertThat(responses.get(1).scheduleDate())
            .isEqualTo(
                LocalDate.of(
                    2026
                    , 9
                    , 5
                )
            );

        assertThat(responses.get(1).reasonCode())
            .isEqualTo(
                RiderScheduleExceptionReason.TRAINING
            );

        assertThat(responses.get(1).version())
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("기간 내 날짜별 예외 일정이 없으면 빈 목록을 반환한다")
    void getScheduleExceptionsReturnsEmptyList() {
        // given
        LocalDate dateFrom =
            LocalDate.of(
                2026
                , 9
                , 1
            );

        LocalDate dateTo =
            LocalDate.of(
                2026
                , 9
                , 7
            );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderScheduleExceptionRepository
                .findAllByRiderIdAndScheduleDateBetweenAndDeletedAtIsNullOrderByScheduleDateAsc(
                    RIDER_ID
                    , dateFrom
                    , dateTo
                )
        ).thenReturn(
            java.util.List.of()
        );

        // when
        java.util.List<RiderScheduleExceptionResponse> responses =
            riderScheduleExceptionService
                .getScheduleExceptions(
                    RIDER_ID
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , dateFrom
                    , dateTo
                );

        // then
        assertThat(responses)
            .isEmpty();

        verify(riderScheduleExceptionRepository)
            .findAllByRiderIdAndScheduleDateBetweenAndDeletedAtIsNullOrderByScheduleDateAsc(
                RIDER_ID
                , dateFrom
                , dateTo
            );
    }

    @Test
    @DisplayName("존재하지 않는 기사의 날짜별 예외 일정을 조회하면 예외가 발생한다")
    void getScheduleExceptionsThrowsExceptionWhenRiderNotFound() {
        // given
        LocalDate dateFrom =
            LocalDate.of(
                2026
                , 9
                , 1
            );

        LocalDate dateTo =
            LocalDate.of(
                2026
                , 9
                , 7
            );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> riderScheduleExceptionService
                .getScheduleExceptions(
                    RIDER_ID
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , dateFrom
                    , dateTo
                )
        ).isInstanceOf(
            RiderNotFoundException.class
        );

        verifyNoInteractions(
            riderScheduleExceptionRepository
        );
    }

    @Test
    @DisplayName("관리자 접근 검증에 실패하면 날짜별 예외 일정을 조회하지 않는다")
    void getScheduleExceptionsThrowsExceptionWhenAdminAccessDenied() {
        // given
        LocalDate dateFrom =
            LocalDate.of(
                2026
                , 9
                , 1
            );

        LocalDate dateTo =
            LocalDate.of(
                2026
                , 9
                , 7
            );

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
            () -> riderScheduleExceptionService
                .getScheduleExceptions(
                    RIDER_ID
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , dateFrom
                    , dateTo
                )
        ).isInstanceOf(
            DeliveryAccessForbiddenException.class
        );

        verifyNoInteractions(
            riderRepository
            , riderScheduleExceptionRepository
        );
    }

    @Test
    @DisplayName("날짜별 예외 일정을 수정한다")
    void updateScheduleException() {
        // given
        RiderScheduleExceptionUpdateRequest request =
            new RiderScheduleExceptionUpdateRequest(
                true
                , RiderScheduleExceptionReason.SUBSTITUTE_WORK
                , "대체 근무로 변경"
                , 0L
            );

        RiderScheduleException scheduleException =
            new RiderScheduleException(
                rider
                , SCHEDULE_DATE
                , deliverySlot
                , false
                , RiderScheduleExceptionReason.TRAINING
                , "신규 기사 교육"
                , ACTOR_ID
            );

        ReflectionTestUtils.setField(
            scheduleException
            , "id"
            , 100L
        );

        ReflectionTestUtils.setField(
            scheduleException
            , "version"
            , 0L
        );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderScheduleExceptionRepository
                .findByIdAndRiderIdAndDeletedAtIsNull(
                    100L
                    , RIDER_ID
                )
        ).thenReturn(
            Optional.of(scheduleException)
        );

        when(rider.getId())
            .thenReturn(RIDER_ID);

        when(deliverySlot.getCode())
            .thenReturn(
                DeliverySlotCode.LUNCH
            );

        doAnswer(
            invocation -> {
                ReflectionTestUtils.setField(
                    scheduleException
                    , "version"
                    , 1L
                );

                return null;
            }
        ).when(
            riderScheduleExceptionRepository
        ).flush();

        // when
        RiderScheduleExceptionResponse response =
            riderScheduleExceptionService
                .updateScheduleException(
                    RIDER_ID
                    , 100L
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                );

        // then
        assertThat(scheduleException.getIsWorking())
            .isTrue();

        assertThat(scheduleException.getReasonCode())
            .isEqualTo(
                RiderScheduleExceptionReason.SUBSTITUTE_WORK
            );

        assertThat(scheduleException.getReasonDetail())
            .isEqualTo(
                "대체 근무로 변경"
            );

        assertThat(response.exceptionId())
            .isEqualTo(100L);

        assertThat(response.riderId())
            .isEqualTo(RIDER_ID);

        assertThat(response.isWorking())
            .isTrue();

        assertThat(response.reasonCode())
            .isEqualTo(
                RiderScheduleExceptionReason.SUBSTITUTE_WORK
            );

        assertThat(response.reasonDetail())
            .isEqualTo(
                "대체 근무로 변경"
            );

        assertThat(response.version())
            .isEqualTo(1L);

        verify(riderScheduleExceptionRepository)
            .flush();
    }

    @Test
    @DisplayName("수정 내용이 현재 값과 같으면 변경하지 않는다")
    void updateScheduleExceptionDoesNotChangeSameContent() {
        // given
        RiderScheduleExceptionUpdateRequest request =
            new RiderScheduleExceptionUpdateRequest(
                false
                , RiderScheduleExceptionReason.TRAINING
                , "신규 기사 교육"
                , 0L
            );

        RiderScheduleException scheduleException =
            new RiderScheduleException(
                rider
                , SCHEDULE_DATE
                , deliverySlot
                , false
                , RiderScheduleExceptionReason.TRAINING
                , "신규 기사 교육"
                , ACTOR_ID
            );

        ReflectionTestUtils.setField(
            scheduleException
            , "id"
            , 100L
        );

        ReflectionTestUtils.setField(
            scheduleException
            , "version"
            , 0L
        );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderScheduleExceptionRepository
                .findByIdAndRiderIdAndDeletedAtIsNull(
                    100L
                    , RIDER_ID
                )
        ).thenReturn(
            Optional.of(scheduleException)
        );

        when(rider.getId())
            .thenReturn(RIDER_ID);

        when(deliverySlot.getCode())
            .thenReturn(
                DeliverySlotCode.LUNCH
            );

        // when
        RiderScheduleExceptionResponse response =
            riderScheduleExceptionService
                .updateScheduleException(
                    RIDER_ID
                    , 100L
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                );

        // then
        assertThat(response.version())
            .isZero();

        verify(
            riderScheduleExceptionRepository
            , never()
        ).flush();
    }

    @Test
    @DisplayName("날짜별 예외 일정 version이 다르면 낙관적 락 충돌 예외가 발생한다")
    void updateScheduleExceptionThrowsExceptionWhenVersionConflicts() {
        // given
        RiderScheduleExceptionUpdateRequest request =
            new RiderScheduleExceptionUpdateRequest(
                true
                , RiderScheduleExceptionReason.SUBSTITUTE_WORK
                , "대체 근무"
                , 1L
            );

        RiderScheduleException scheduleException =
            new RiderScheduleException(
                rider
                , SCHEDULE_DATE
                , deliverySlot
                , false
                , RiderScheduleExceptionReason.TRAINING
                , "신규 기사 교육"
                , ACTOR_ID
            );

        ReflectionTestUtils.setField(
            scheduleException
            , "version"
            , 0L
        );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderScheduleExceptionRepository
                .findByIdAndRiderIdAndDeletedAtIsNull(
                    100L
                    , RIDER_ID
                )
        ).thenReturn(
            Optional.of(scheduleException)
        );

        // when & then
        assertThatThrownBy(
            () -> riderScheduleExceptionService
                .updateScheduleException(
                    RIDER_ID
                    , 100L
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                )
        ).isInstanceOf(
            OptimisticLockConflictException.class
        );

        verify(
            riderScheduleExceptionRepository
            , never()
        ).flush();
    }

    @Test
    @DisplayName("날짜별 예외 일정 수정 시 OTHER 상세 사유가 없으면 예외가 발생한다")
    void updateScheduleExceptionThrowsExceptionWhenOtherReasonDetailMissing() {
        // given
        RiderScheduleExceptionUpdateRequest request =
            new RiderScheduleExceptionUpdateRequest(
                false
                , RiderScheduleExceptionReason.OTHER
                , null
                , 0L
            );

        // when & then
        assertThatThrownBy(
            () -> riderScheduleExceptionService
                .updateScheduleException(
                    RIDER_ID
                    , 100L
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                )
        ).isInstanceOf(
            OtherReasonDetailRequiredException.class
        );

        verifyNoInteractions(
            riderRepository
            , riderScheduleExceptionRepository
        );
    }

    @Test
    @DisplayName("존재하지 않는 기사의 날짜별 예외 일정을 수정하면 예외가 발생한다")
    void updateScheduleExceptionThrowsExceptionWhenRiderNotFound() {
        // given
        RiderScheduleExceptionUpdateRequest request =
            new RiderScheduleExceptionUpdateRequest(
                true
                , RiderScheduleExceptionReason.SUBSTITUTE_WORK
                , "대체 근무"
                , 0L
            );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> riderScheduleExceptionService
                .updateScheduleException(
                    RIDER_ID
                    , 100L
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                )
        ).isInstanceOf(
            RiderNotFoundException.class
        );

        verifyNoInteractions(
            riderScheduleExceptionRepository
        );
    }

    @Test
    @DisplayName("존재하지 않는 날짜별 예외 일정을 수정하면 예외가 발생한다")
    void updateScheduleExceptionThrowsExceptionWhenScheduleExceptionNotFound() {
        // given
        RiderScheduleExceptionUpdateRequest request =
            new RiderScheduleExceptionUpdateRequest(
                true
                , RiderScheduleExceptionReason.SUBSTITUTE_WORK
                , "대체 근무"
                , 0L
            );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderScheduleExceptionRepository
                .findByIdAndRiderIdAndDeletedAtIsNull(
                    100L
                    , RIDER_ID
                )
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> riderScheduleExceptionService
                .updateScheduleException(
                    RIDER_ID
                    , 100L
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                )
        ).isInstanceOf(
            RiderScheduleExceptionNotFoundException.class
        );

        verify(
            riderScheduleExceptionRepository
            , never()
        ).flush();
    }

    @Test
    @DisplayName("관리자 접근 검증에 실패하면 날짜별 예외 일정을 수정하지 않는다")
    void updateScheduleExceptionThrowsExceptionWhenAdminAccessDenied() {
        // given
        RiderScheduleExceptionUpdateRequest request =
            new RiderScheduleExceptionUpdateRequest(
                true
                , RiderScheduleExceptionReason.SUBSTITUTE_WORK
                , "대체 근무"
                , 0L
            );

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
            () -> riderScheduleExceptionService
                .updateScheduleException(
                    RIDER_ID
                    , 100L
                    , ACTOR_ID
                    , UserRole.ADMIN
                    , request
                )
        ).isInstanceOf(
            DeliveryAccessForbiddenException.class
        );

        verifyNoInteractions(
            riderRepository
            , riderScheduleExceptionRepository
        );
    }

    @Test
    @DisplayName("날짜별 예외 일정을 삭제한다")
    void deleteScheduleException() {
        // given
        RiderScheduleException scheduleException =
            new RiderScheduleException(
                rider
                , SCHEDULE_DATE
                , deliverySlot
                , false
                , RiderScheduleExceptionReason.TRAINING
                , "신규 기사 교육"
                , ACTOR_ID
            );

        ReflectionTestUtils.setField(
            scheduleException
            , "id"
            , 100L
        );

        ReflectionTestUtils.setField(
            scheduleException
            , "version"
            , 0L
        );

        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderScheduleExceptionRepository
                .findByIdAndRiderIdAndDeletedAtIsNull(
                    100L
                    , RIDER_ID
                )
        ).thenReturn(
            Optional.of(scheduleException)
        );

        LocalDateTime before =
            LocalDateTime.now(
                java.time.ZoneId.of("Asia/Seoul")
            );

        // when
        riderScheduleExceptionService
            .deleteScheduleException(
                RIDER_ID
                , 100L
                , ACTOR_ID
                , UserRole.ADMIN
            );

        LocalDateTime after =
            LocalDateTime.now(
                java.time.ZoneId.of("Asia/Seoul")
            );

        // then
        verify(deliveryAccessService)
            .validateAdminAccess(
                ACTOR_ID
                , UserRole.ADMIN
            );

        verify(riderScheduleExceptionRepository)
            .findByIdAndRiderIdAndDeletedAtIsNull(
                100L
                , RIDER_ID
            );

        assertThat(scheduleException.getDeletedAt())
            .isNotNull();

        assertThat(scheduleException.getDeletedAt())
            .isBetween(
                before
                , after
            );

        verify(
            riderScheduleExceptionRepository
            , never()
        ).save(
            any(RiderScheduleException.class)
        );
    }

    @Test
    @DisplayName("존재하지 않는 기사의 날짜별 예외 일정을 삭제하면 예외가 발생한다")
    void deleteScheduleExceptionThrowsExceptionWhenRiderNotFound() {
        // given
        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> riderScheduleExceptionService
                .deleteScheduleException(
                    RIDER_ID
                    , 100L
                    , ACTOR_ID
                    , UserRole.ADMIN
                )
        ).isInstanceOf(
            RiderNotFoundException.class
        );

        verifyNoInteractions(
            riderScheduleExceptionRepository
        );
    }

    @Test
    @DisplayName("존재하지 않는 날짜별 예외 일정을 삭제하면 예외가 발생한다")
    void deleteScheduleExceptionThrowsExceptionWhenScheduleExceptionNotFound() {
        // given
        when(
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    RIDER_ID
                )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderScheduleExceptionRepository
                .findByIdAndRiderIdAndDeletedAtIsNull(
                    100L
                    , RIDER_ID
                )
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> riderScheduleExceptionService
                .deleteScheduleException(
                    RIDER_ID
                    , 100L
                    , ACTOR_ID
                    , UserRole.ADMIN
                )
        ).isInstanceOf(
            RiderScheduleExceptionNotFoundException.class
        );
    }

    @Test
    @DisplayName("관리자 접근 검증에 실패하면 날짜별 예외 일정을 삭제하지 않는다")
    void deleteScheduleExceptionThrowsExceptionWhenAdminAccessDenied() {
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
            () -> riderScheduleExceptionService
                .deleteScheduleException(
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
            , riderScheduleExceptionRepository
        );
    }
}