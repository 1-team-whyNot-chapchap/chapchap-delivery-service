package com.chapchap.delivery.domain.rider.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.entity.DeliveryAreaCode;
import com.chapchap.delivery.domain.delivery.repository.DeliveryAreaCodeRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.entity.RiderDeliveryArea;
import com.chapchap.delivery.domain.rider.repository.RiderDeliveryAreaRepository;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.rider.request.RiderDeliveryAreaCreateRequest;
import com.chapchap.delivery.domain.rider.request.RiderDeliveryAreaUpdateRequest;
import com.chapchap.delivery.domain.rider.response.RiderDeliveryAreaResponse;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.InvalidRiderDeliveryAreaException;
import com.chapchap.delivery.global.exception.business.RiderDeliveryAreaConflictException;
import com.chapchap.delivery.global.exception.business.RiderDeliveryAreaNotFoundException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderDeliveryAreaServiceTest {
    private static final Long RIDER_ID = 10L;
    private static final Long ACTOR_ID = 9001L;
    private static final Long RIDER_AREA_ID = 100L;

    private static final String DELIVERY_AREA_CODE = "DAEGU_JUNG_GU";

    private static final LocalDate EFFECTIVE_FROM =
        LocalDate.of(2026, 9, 5);

    private static final LocalDate EFFECTIVE_TO =
        LocalDate.of(2026, 10, 31);

    @Mock
    private RiderRepository riderRepository;

    @Mock
    private RiderDeliveryAreaRepository riderDeliveryAreaRepository;

    @Mock
    private DeliveryAreaCodeRepository deliveryAreaCodeRepository;

    @Mock
    private DeliveryAccessService deliveryAccessService;

    private RiderDeliveryAreaService createService() {
        return new RiderDeliveryAreaService(
            riderRepository
            , riderDeliveryAreaRepository
            , deliveryAreaCodeRepository
            , deliveryAccessService
        );
    }

    @Test
    @DisplayName("관리자가 기사 담당 배송 지역을 신규 등록한다")
    void createDeliveryArea() {
        // given
        RiderDeliveryAreaService service =
            createService();

        Rider rider =
            createRider();

        DeliveryAreaCode deliveryAreaCode =
            mock(DeliveryAreaCode.class);

        RiderDeliveryAreaCreateRequest request =
            createRequest(
                EFFECTIVE_TO
                , true
            );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            deliveryAreaCodeRepository.findByAreaCodeAndIsActiveTrue(
                DELIVERY_AREA_CODE
            )
        ).thenReturn(
            Optional.of(deliveryAreaCode)
        );

        when(
            riderDeliveryAreaRepository
                .findByRiderIdAndDeliveryAreaCodeAndEffectiveFrom(
                    RIDER_ID
                    , DELIVERY_AREA_CODE
                    , EFFECTIVE_FROM
                )
        ).thenReturn(
            Optional.empty()
        );

        when(
            riderDeliveryAreaRepository.save(
                any(RiderDeliveryArea.class)
            )
        ).thenAnswer(invocation -> {
            RiderDeliveryArea riderDeliveryArea =
                invocation.getArgument(0);

            ReflectionTestUtils.setField(
                riderDeliveryArea
                , "id"
                , RIDER_AREA_ID
            );

            return riderDeliveryArea;
        });

        // when
        RiderDeliveryAreaResponse response =
            service.createDeliveryArea(
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

        verify(riderRepository)
            .findByIdAndDeletedAtIsNull(
                RIDER_ID
            );

        verify(deliveryAreaCodeRepository)
            .findByAreaCodeAndIsActiveTrue(
                DELIVERY_AREA_CODE
            );

        verify(riderDeliveryAreaRepository)
            .findByRiderIdAndDeliveryAreaCodeAndEffectiveFrom(
                RIDER_ID
                , DELIVERY_AREA_CODE
                , EFFECTIVE_FROM
            );

        verify(riderDeliveryAreaRepository)
            .save(
                any(RiderDeliveryArea.class)
            );

        assertThat(response.riderDeliveryAreaId())
            .isEqualTo(RIDER_AREA_ID);

        assertThat(response.riderId())
            .isEqualTo(RIDER_ID);

        assertThat(response.deliveryAreaCode())
            .isEqualTo(DELIVERY_AREA_CODE);

        assertThat(response.effectiveFrom())
            .isEqualTo(EFFECTIVE_FROM);

        assertThat(response.effectiveTo())
            .isEqualTo(EFFECTIVE_TO);

        assertThat(response.isActive())
            .isTrue();
    }

    @Test
    @DisplayName("삭제된 동일 기사 담당 배송 지역은 복구한다")
    void createDeliveryAreaRestoresDeletedArea() {
        // given
        RiderDeliveryAreaService service =
            createService();

        Rider rider =
            createRider();

        RiderDeliveryArea existingArea =
            createDeliveryArea(
                rider
                , DELIVERY_AREA_CODE
                , EFFECTIVE_FROM
                , null
                , false
            );

        ReflectionTestUtils.setField(
            existingArea
            , "deletedAt"
            , LocalDateTime.of(2026, 9, 1, 10, 0)
        );

        RiderDeliveryAreaCreateRequest request =
            createRequest(
                EFFECTIVE_TO
                , true
            );

        stubRiderAndAreaCode(
            rider
        );

        when(
            riderDeliveryAreaRepository
                .findByRiderIdAndDeliveryAreaCodeAndEffectiveFrom(
                    RIDER_ID
                    , DELIVERY_AREA_CODE
                    , EFFECTIVE_FROM
                )
        ).thenReturn(
            Optional.of(existingArea)
        );

        // when
        RiderDeliveryAreaResponse response =
            service.createDeliveryArea(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            );

        // then
        assertThat(existingArea.getDeletedAt())
            .isNull();

        assertThat(existingArea.getEffectiveTo())
            .isEqualTo(EFFECTIVE_TO);

        assertThat(existingArea.getIsActive())
            .isTrue();

        assertThat(response.riderDeliveryAreaId())
            .isEqualTo(RIDER_AREA_ID);

        assertThat(response.isActive())
            .isTrue();

        verify(
            riderDeliveryAreaRepository
            , never()
        ).save(
            any(RiderDeliveryArea.class)
        );
    }

    @Test
    @DisplayName("동일한 기사 담당 배송 지역 등록 요청은 기존 결과를 반환한다")
    void createDeliveryAreaReturnsExistingAreaForSameRequest() {
        // given
        RiderDeliveryAreaService service =
            createService();

        Rider rider =
            createRider();

        RiderDeliveryArea existingArea =
            createDeliveryArea(
                rider
                , DELIVERY_AREA_CODE
                , EFFECTIVE_FROM
                , EFFECTIVE_TO
                , true
            );

        RiderDeliveryAreaCreateRequest request =
            createRequest(
                EFFECTIVE_TO
                , true
            );

        stubRiderAndAreaCode(
            rider
        );

        when(
            riderDeliveryAreaRepository
                .findByRiderIdAndDeliveryAreaCodeAndEffectiveFrom(
                    RIDER_ID
                    , DELIVERY_AREA_CODE
                    , EFFECTIVE_FROM
                )
        ).thenReturn(
            Optional.of(existingArea)
        );

        // when
        RiderDeliveryAreaResponse response =
            service.createDeliveryArea(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            );

        // then
        assertThat(response.riderDeliveryAreaId())
            .isEqualTo(RIDER_AREA_ID);

        assertThat(response.effectiveTo())
            .isEqualTo(EFFECTIVE_TO);

        assertThat(response.isActive())
            .isTrue();

        verify(
            riderDeliveryAreaRepository
            , never()
        ).save(
            any(RiderDeliveryArea.class)
        );
    }

    @Test
    @DisplayName("동일 키의 담당 배송 지역이 다른 내용으로 존재하면 충돌한다")
    void createDeliveryAreaThrowsConflictForDifferentExistingArea() {
        // given
        RiderDeliveryAreaService service =
            createService();

        Rider rider =
            createRider();

        RiderDeliveryArea existingArea =
            createDeliveryArea(
                rider
                , DELIVERY_AREA_CODE
                , EFFECTIVE_FROM
                , null
                , true
            );

        RiderDeliveryAreaCreateRequest request =
            createRequest(
                EFFECTIVE_TO
                , true
            );

        stubRiderAndAreaCode(
            rider
        );

        when(
            riderDeliveryAreaRepository
                .findByRiderIdAndDeliveryAreaCodeAndEffectiveFrom(
                    RIDER_ID
                    , DELIVERY_AREA_CODE
                    , EFFECTIVE_FROM
                )
        ).thenReturn(
            Optional.of(existingArea)
        );

        // when & then
        assertThatThrownBy(
            () -> service.createDeliveryArea(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            )
        ).isInstanceOf(
            RiderDeliveryAreaConflictException.class
        );

        verify(
            riderDeliveryAreaRepository
            , never()
        ).save(
            any(RiderDeliveryArea.class)
        );
    }

    @Test
    @DisplayName("담당 배송 지역 종료일이 시작일보다 빠르면 등록할 수 없다")
    void createDeliveryAreaThrowsExceptionForInvalidPeriod() {
        // given
        RiderDeliveryAreaService service =
            createService();

        RiderDeliveryAreaCreateRequest request =
            new RiderDeliveryAreaCreateRequest(
                DELIVERY_AREA_CODE
                , LocalDate.of(2026, 9, 10)
                , LocalDate.of(2026, 9, 9)
                , true
            );

        // when & then
        assertThatThrownBy(
            () -> service.createDeliveryArea(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            )
        ).isInstanceOf(
            InvalidRiderDeliveryAreaException.class
        );

        verifyNoInteractions(
            riderRepository
            , deliveryAreaCodeRepository
            , riderDeliveryAreaRepository
        );
    }

    @Test
    @DisplayName("존재하지 않는 기사에게 담당 배송 지역을 등록할 수 없다")
    void createDeliveryAreaThrowsExceptionWhenRiderNotFound() {
        // given
        RiderDeliveryAreaService service =
            createService();

        RiderDeliveryAreaCreateRequest request =
            createRequest(
                EFFECTIVE_TO
                , true
            );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> service.createDeliveryArea(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            )
        ).isInstanceOf(
            RiderNotFoundException.class
        );

        verifyNoInteractions(
            deliveryAreaCodeRepository
            , riderDeliveryAreaRepository
        );
    }

    @Test
    @DisplayName("유효하지 않은 배송 지역 코드는 기사 담당 지역으로 등록할 수 없다")
    void createDeliveryAreaThrowsExceptionForInvalidAreaCode() {
        // given
        RiderDeliveryAreaService service =
            createService();

        Rider rider =
            createRider();

        RiderDeliveryAreaCreateRequest request =
            createRequest(
                EFFECTIVE_TO
                , true
            );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            deliveryAreaCodeRepository.findByAreaCodeAndIsActiveTrue(
                DELIVERY_AREA_CODE
            )
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> service.createDeliveryArea(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            )
        ).isInstanceOf(
            InvalidRiderDeliveryAreaException.class
        );

        verifyNoInteractions(
            riderDeliveryAreaRepository
        );
    }

    @Test
    @DisplayName("관리자 접근 권한이 없으면 기사 담당 배송 지역을 등록할 수 없다")
    void createDeliveryAreaThrowsExceptionWhenAdminAccessDenied() {
        // given
        RiderDeliveryAreaService service =
            createService();

        RiderDeliveryAreaCreateRequest request =
            createRequest(
                EFFECTIVE_TO
                , true
            );

        doThrow(
            new DeliveryAccessForbiddenException()
        ).when(
            deliveryAccessService
        ).validateAdminAccess(
            ACTOR_ID
            , UserRole.RIDER
        );

        // when & then
        assertThatThrownBy(
            () -> service.createDeliveryArea(
                RIDER_ID
                , ACTOR_ID
                , UserRole.RIDER
                , request
            )
        ).isInstanceOf(
            DeliveryAccessForbiddenException.class
        );

        verifyNoInteractions(
            riderRepository
            , deliveryAreaCodeRepository
            , riderDeliveryAreaRepository
        );
    }

    @Test
    @DisplayName("관리자가 기사의 담당 배송 지역 목록을 조회한다")
    void getDeliveryAreas() {
        // given
        RiderDeliveryAreaService service =
            createService();

        Rider rider =
            createRider();

        RiderDeliveryArea firstArea =
            createDeliveryArea(
                rider
                , "DAEGU_JUNG_GU"
                , LocalDate.of(2026, 9, 1)
                , null
                , true
            );

        RiderDeliveryArea secondArea =
            createDeliveryArea(
                rider
                , "DAEGU_SUSEONG_GU"
                , LocalDate.of(2026, 9, 10)
                , LocalDate.of(2026, 12, 31)
                , false
            );

        ReflectionTestUtils.setField(
            secondArea
            , "id"
            , 101L
        );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderDeliveryAreaRepository.findAllByRiderIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            List.of(
                firstArea
                , secondArea
            )
        );

        // when
        List<RiderDeliveryAreaResponse> responses =
            service.getDeliveryAreas(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
            );

        // then
        assertThat(responses)
            .hasSize(2);

        assertThat(responses.get(0).riderDeliveryAreaId())
            .isEqualTo(RIDER_AREA_ID);

        assertThat(responses.get(0).deliveryAreaCode())
            .isEqualTo("DAEGU_JUNG_GU");

        assertThat(responses.get(0).isActive())
            .isTrue();

        assertThat(responses.get(1).riderDeliveryAreaId())
            .isEqualTo(101L);

        assertThat(responses.get(1).deliveryAreaCode())
            .isEqualTo("DAEGU_SUSEONG_GU");

        assertThat(responses.get(1).isActive())
            .isFalse();

        verify(riderDeliveryAreaRepository)
            .findAllByRiderIdAndDeletedAtIsNull(
                RIDER_ID
            );
    }

    @Test
    @DisplayName("등록된 담당 배송 지역이 없으면 빈 목록을 반환한다")
    void getDeliveryAreasReturnsEmptyList() {
        // given
        RiderDeliveryAreaService service =
            createService();

        Rider rider =
            createRider();

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderDeliveryAreaRepository.findAllByRiderIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            List.of()
        );

        // when
        List<RiderDeliveryAreaResponse> responses =
            service.getDeliveryAreas(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
            );

        // then
        assertThat(responses)
            .isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 기사의 담당 배송 지역은 조회할 수 없다")
    void getDeliveryAreasThrowsExceptionWhenRiderNotFound() {
        // given
        RiderDeliveryAreaService service =
            createService();

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> service.getDeliveryAreas(
                RIDER_ID
                , ACTOR_ID
                , UserRole.ADMIN
            )
        ).isInstanceOf(
            RiderNotFoundException.class
        );

        verifyNoInteractions(
            riderDeliveryAreaRepository
        );
    }

    @Test
    @DisplayName("관리자 접근 권한이 없으면 담당 배송 지역을 조회할 수 없다")
    void getDeliveryAreasThrowsExceptionWhenAdminAccessDenied() {
        // given
        RiderDeliveryAreaService service =
            createService();

        doThrow(
            new DeliveryAccessForbiddenException()
        ).when(
            deliveryAccessService
        ).validateAdminAccess(
            ACTOR_ID
            , UserRole.RIDER
        );

        // when & then
        assertThatThrownBy(
            () -> service.getDeliveryAreas(
                RIDER_ID
                , ACTOR_ID
                , UserRole.RIDER
            )
        ).isInstanceOf(
            DeliveryAccessForbiddenException.class
        );

        verifyNoInteractions(
            riderRepository
            , riderDeliveryAreaRepository
        );
    }

    @Test
    @DisplayName("관리자가 기사 담당 배송 지역의 유효기간과 활성 상태를 수정한다")
    void updateDeliveryArea() {
        // given
        RiderDeliveryAreaService service =
            createService();

        Rider rider =
            createRider();

        RiderDeliveryArea riderDeliveryArea =
            createDeliveryArea(
                rider
                , DELIVERY_AREA_CODE
                , EFFECTIVE_FROM
                , null
                , true
            );

        RiderDeliveryAreaUpdateRequest request =
            new RiderDeliveryAreaUpdateRequest(
                EFFECTIVE_TO
                , false
            );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderDeliveryAreaRepository.findByIdAndRiderIdAndDeletedAtIsNull(
                RIDER_AREA_ID
                , RIDER_ID
            )
        ).thenReturn(
            Optional.of(riderDeliveryArea)
        );

        // when
        RiderDeliveryAreaResponse response =
            service.updateDeliveryArea(
                RIDER_ID
                , RIDER_AREA_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            );

        // then
        assertThat(riderDeliveryArea.getEffectiveTo())
            .isEqualTo(EFFECTIVE_TO);

        assertThat(riderDeliveryArea.getIsActive())
            .isFalse();

        assertThat(response.riderDeliveryAreaId())
            .isEqualTo(RIDER_AREA_ID);

        assertThat(response.effectiveTo())
            .isEqualTo(EFFECTIVE_TO);

        assertThat(response.isActive())
            .isFalse();

        verify(
            riderDeliveryAreaRepository
            , never()
        ).save(
            any(RiderDeliveryArea.class)
        );
    }

    @Test
    @DisplayName("기사 담당 배송 지역 수정 내용이 동일하면 기존 결과를 반환한다")
    void updateDeliveryAreaReturnsExistingAreaForSameRequest() {
        // given
        RiderDeliveryAreaService service =
            createService();

        Rider rider =
            createRider();

        RiderDeliveryArea riderDeliveryArea =
            createDeliveryArea(
                rider
                , DELIVERY_AREA_CODE
                , EFFECTIVE_FROM
                , EFFECTIVE_TO
                , false
            );

        RiderDeliveryAreaUpdateRequest request =
            new RiderDeliveryAreaUpdateRequest(
                EFFECTIVE_TO
                , false
            );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderDeliveryAreaRepository.findByIdAndRiderIdAndDeletedAtIsNull(
                RIDER_AREA_ID
                , RIDER_ID
            )
        ).thenReturn(
            Optional.of(riderDeliveryArea)
        );

        // when
        RiderDeliveryAreaResponse response =
            service.updateDeliveryArea(
                RIDER_ID
                , RIDER_AREA_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            );

        // then
        assertThat(response.effectiveTo())
            .isEqualTo(EFFECTIVE_TO);

        assertThat(response.isActive())
            .isFalse();

        verify(
            riderDeliveryAreaRepository
            , never()
        ).save(
            any(RiderDeliveryArea.class)
        );
    }

    @Test
    @DisplayName("담당 배송 지역 수정 종료일이 시작일보다 빠르면 수정할 수 없다")
    void updateDeliveryAreaThrowsExceptionForInvalidPeriod() {
        // given
        RiderDeliveryAreaService service =
            createService();

        Rider rider =
            createRider();

        RiderDeliveryArea riderDeliveryArea =
            createDeliveryArea(
                rider
                , DELIVERY_AREA_CODE
                , EFFECTIVE_FROM
                , null
                , true
            );

        RiderDeliveryAreaUpdateRequest request =
            new RiderDeliveryAreaUpdateRequest(
                EFFECTIVE_FROM.minusDays(1)
                , false
            );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderDeliveryAreaRepository.findByIdAndRiderIdAndDeletedAtIsNull(
                RIDER_AREA_ID
                , RIDER_ID
            )
        ).thenReturn(
            Optional.of(riderDeliveryArea)
        );

        // when & then
        assertThatThrownBy(
            () -> service.updateDeliveryArea(
                RIDER_ID
                , RIDER_AREA_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            )
        ).isInstanceOf(
            InvalidRiderDeliveryAreaException.class
        );

        assertThat(riderDeliveryArea.getEffectiveTo())
            .isNull();

        assertThat(riderDeliveryArea.getIsActive())
            .isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 기사의 담당 배송 지역은 수정할 수 없다")
    void updateDeliveryAreaThrowsExceptionWhenRiderNotFound() {
        // given
        RiderDeliveryAreaService service =
            createService();

        RiderDeliveryAreaUpdateRequest request =
            new RiderDeliveryAreaUpdateRequest(
                EFFECTIVE_TO
                , false
            );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> service.updateDeliveryArea(
                RIDER_ID
                , RIDER_AREA_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            )
        ).isInstanceOf(
            RiderNotFoundException.class
        );

        verifyNoInteractions(
            riderDeliveryAreaRepository
        );
    }

    @Test
    @DisplayName("존재하지 않는 기사 담당 배송 지역은 수정할 수 없다")
    void updateDeliveryAreaThrowsExceptionWhenDeliveryAreaNotFound() {
        // given
        RiderDeliveryAreaService service =
            createService();

        Rider rider =
            createRider();

        RiderDeliveryAreaUpdateRequest request =
            new RiderDeliveryAreaUpdateRequest(
                EFFECTIVE_TO
                , false
            );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            riderDeliveryAreaRepository.findByIdAndRiderIdAndDeletedAtIsNull(
                RIDER_AREA_ID
                , RIDER_ID
            )
        ).thenReturn(
            Optional.empty()
        );

        // when & then
        assertThatThrownBy(
            () -> service.updateDeliveryArea(
                RIDER_ID
                , RIDER_AREA_ID
                , ACTOR_ID
                , UserRole.ADMIN
                , request
            )
        ).isInstanceOf(
            RiderDeliveryAreaNotFoundException.class
        );
    }

    @Test
    @DisplayName("관리자 접근 권한이 없으면 기사 담당 배송 지역을 수정할 수 없다")
    void updateDeliveryAreaThrowsExceptionWhenAdminAccessDenied() {
        // given
        RiderDeliveryAreaService service =
            createService();

        RiderDeliveryAreaUpdateRequest request =
            new RiderDeliveryAreaUpdateRequest(
                EFFECTIVE_TO
                , false
            );

        doThrow(
            new DeliveryAccessForbiddenException()
        ).when(
            deliveryAccessService
        ).validateAdminAccess(
            ACTOR_ID
            , UserRole.RIDER
        );

        // when & then
        assertThatThrownBy(
            () -> service.updateDeliveryArea(
                RIDER_ID
                , RIDER_AREA_ID
                , ACTOR_ID
                , UserRole.RIDER
                , request
            )
        ).isInstanceOf(
            DeliveryAccessForbiddenException.class
        );

        verifyNoInteractions(
            riderRepository
            , riderDeliveryAreaRepository
            , deliveryAreaCodeRepository
        );
    }

    private Rider createRider() {
        Rider rider =
            new Rider(
                10001L
            );

        ReflectionTestUtils.setField(
            rider
            , "id"
            , RIDER_ID
        );

        return rider;
    }

    private RiderDeliveryArea createDeliveryArea(
        Rider rider
        , String deliveryAreaCode
        , LocalDate effectiveFrom
        , LocalDate effectiveTo
        , Boolean isActive
    ) {
        RiderDeliveryArea riderDeliveryArea =
            new RiderDeliveryArea(
                rider
                , deliveryAreaCode
                , effectiveFrom
                , effectiveTo
                , isActive
            );

        ReflectionTestUtils.setField(
            riderDeliveryArea
            , "id"
            , RIDER_AREA_ID
        );

        return riderDeliveryArea;
    }

    private RiderDeliveryAreaCreateRequest createRequest(
        LocalDate effectiveTo
        , Boolean isActive
    ) {
        return new RiderDeliveryAreaCreateRequest(
            DELIVERY_AREA_CODE
            , EFFECTIVE_FROM
            , effectiveTo
            , isActive
        );
    }

    private void stubRiderAndAreaCode(
        Rider rider
    ) {
        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(
            Optional.of(rider)
        );

        when(
            deliveryAreaCodeRepository.findByAreaCodeAndIsActiveTrue(
                DELIVERY_AREA_CODE
            )
        ).thenReturn(
            Optional.of(
                mock(DeliveryAreaCode.class)
            )
        );
    }
}