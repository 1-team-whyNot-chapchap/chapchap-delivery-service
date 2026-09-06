package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletion;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletionPhoto;
import com.chapchap.delivery.domain.delivery.entity.DeliveryDelay;
import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionPhotoRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryDelayRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.InvalidDeliveryInfoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerDeliveryQueryServiceTest {
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryCompletionRepository completionRepository;
    @Mock private DeliveryFailureRepository failureRepository;
    @Mock private DeliveryDelayRepository delayRepository;
    @Mock private DeliveryCompletionPhotoRepository photoRepository;

    private CustomerDeliveryQueryService service;

    @BeforeEach
    void setUp() {
        service = new CustomerDeliveryQueryService(
            deliveryRepository, completionRepository, failureRepository,
            delayRepository, photoRepository
        );
    }

    @Test
    @DisplayName("고객 상세는 내부 실패 코드 대신 고객용 공통 안내를 반환한다")
    void detailDoesNotExposeInternalFailureCode() {
        Delivery delivery = mock(Delivery.class);
        DeliveryGroup group = mock(DeliveryGroup.class);
        DeliverySlot slot = mock(DeliverySlot.class);
        DeliveryFailure failure = mock(DeliveryFailure.class);
        when(delivery.getId()).thenReturn(1L);
        when(delivery.getCustomerId()).thenReturn(100L);
        when(delivery.getDeliveryPublicId()).thenReturn("delivery-1");
        when(delivery.getSourceOrderId()).thenReturn("order-1");
        when(delivery.getDeliveryGroup()).thenReturn(group);
        when(group.getDeliveryDate()).thenReturn(LocalDate.of(2026, 9, 6));
        when(group.getSlot()).thenReturn(slot);
        when(slot.getCode()).thenReturn(DeliverySlotCode.LUNCH);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.FAILED);
        when(delivery.getLunchboxQuantity()).thenReturn(2);
        when(failure.getDelivery()).thenReturn(delivery);
        when(deliveryRepository.findDetailByDeliveryPublicId("delivery-1"))
            .thenReturn(Optional.of(delivery));
        when(completionRepository.findAllByDeliveryIdIn(anyList())).thenReturn(List.of());
        when(failureRepository.findAllByDeliveryIdIn(anyList())).thenReturn(List.of(failure));
        when(delayRepository.findAllByDeliveryIdIn(anyList())).thenReturn(List.of());

        var response = service.getMyDelivery(100L, UserRole.CUSTOMER, "delivery-1");

        assertThat(response.customerFailureMessage())
            .isEqualTo("배송을 완료하지 못했습니다. 고객센터로 문의해 주세요.");
    }

    @Test
    @DisplayName("고객은 다른 고객의 배송 상세를 조회할 수 없다")
    void cannotReadAnotherCustomersDelivery() {
        Delivery delivery = mock(Delivery.class);
        when(delivery.getCustomerId()).thenReturn(200L);
        when(deliveryRepository.findDetailByDeliveryPublicId("delivery-1"))
            .thenReturn(Optional.of(delivery));

        assertThatThrownBy(
            () -> service.getMyDelivery(100L, UserRole.CUSTOMER, "delivery-1")
        ).isInstanceOf(DeliveryAccessForbiddenException.class);

        verify(completionRepository, never()).findAllByDeliveryIdIn(anyList());
    }

    @Test
    @DisplayName("고객 역할이 아니면 목록 쿼리를 실행하지 않는다")
    void rejectsNonCustomerBeforeQuery() {
        assertThatThrownBy(() -> service.getMyDeliveries(
            10L, UserRole.RIDER, null, null, null, null, PageRequest.of(0, 20)
        )).isInstanceOf(DeliveryAccessForbiddenException.class);

        verify(deliveryRepository, never()).findAllForCustomer(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("고객 목록은 시작일이 종료일보다 늦으면 Bad Request 예외를 발생시킨다")
    void rejectsInvalidDateRange() {
        assertThatThrownBy(() -> service.getMyDeliveries(
            10L, UserRole.CUSTOMER,
            LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 6),
            null, null, PageRequest.of(0, 20)
        )).isInstanceOf(InvalidDeliveryInfoException.class);

        verify(deliveryRepository, never()).findAllForCustomer(
            any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("고객 목록 필터와 정규화된 pageable을 Repository에 전달한다")
    void passesAllListFiltersToRepository() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        PageRequest requested = PageRequest.of(
            2, 30, org.springframework.data.domain.Sort.Direction.ASC, "deliveryDate"
        );
        when(deliveryRepository.findAllForCustomer(
            org.mockito.ArgumentMatchers.eq(10L)
            , org.mockito.ArgumentMatchers.eq(from)
            , org.mockito.ArgumentMatchers.eq(to)
            , org.mockito.ArgumentMatchers.eq(DeliverySlotCode.DINNER)
            , org.mockito.ArgumentMatchers.eq(DeliveryStatus.READY)
            , any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), requested, 0));

        service.getMyDeliveries(
            10L, UserRole.CUSTOMER, from, to,
            DeliverySlotCode.DINNER, DeliveryStatus.READY, requested
        );

        org.mockito.ArgumentCaptor<Pageable> captor =
            org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(deliveryRepository).findAllForCustomer(
            org.mockito.ArgumentMatchers.eq(10L)
            , org.mockito.ArgumentMatchers.eq(from)
            , org.mockito.ArgumentMatchers.eq(to)
            , org.mockito.ArgumentMatchers.eq(DeliverySlotCode.DINNER)
            , org.mockito.ArgumentMatchers.eq(DeliveryStatus.READY)
            , captor.capture()
        );
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(30);
        assertThat(captor.getValue().getSort().getOrderFor("deliveryGroup.deliveryDate"))
            .isNotNull();
        assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    @DisplayName("비대면 완료 배송의 완료·지연·사진 정보를 매핑한다")
    void mapsDoorstepCompletionWithDelayAndPhoto() {
        Delivery delivery = completedDelivery(1L, "delivery-1");
        DeliveryCompletion completion = completion(
            10L, delivery, ActualHandoffType.DOORSTEP,
            LocalDateTime.of(2026, 9, 6, 12, 20)
        );
        DeliveryDelay delay = mock(DeliveryDelay.class);
        when(delay.getDelivery()).thenReturn(delivery);
        when(delay.getDelayMinutes()).thenReturn(15);
        DeliveryCompletionPhoto photo = mock(DeliveryCompletionPhoto.class);
        when(photo.getDeliveryCompletion()).thenReturn(completion);
        stubDetail(delivery, completion, delay, List.of(photo));

        var response = service.getMyDelivery(
            100L, UserRole.CUSTOMER, "delivery-1"
        );

        assertThat(response.completedAt())
            .isEqualTo(OffsetDateTime.parse("2026-09-06T12:20:00+09:00"));
        assertThat(response.actualHandoffType()).isEqualTo(ActualHandoffType.DOORSTEP);
        assertThat(response.isDelayed()).isTrue();
        assertThat(response.delayMinutes()).isEqualTo(15);
        assertThat(response.hasCompletionPhoto()).isTrue();
    }

    @Test
    @DisplayName("직접 전달 완료에 사진이 없으면 사진 존재 여부를 false로 반환한다")
    void mapsDirectCompletionWithoutPhoto() {
        Delivery delivery = completedDelivery(1L, "delivery-1");
        DeliveryCompletion completion = completion(
            10L, delivery, ActualHandoffType.DIRECT,
            LocalDateTime.of(2026, 9, 6, 12, 0)
        );
        stubDetail(delivery, completion, null, List.of());

        var response = service.getMyDelivery(
            100L, UserRole.CUSTOMER, "delivery-1"
        );

        assertThat(response.actualHandoffType()).isEqualTo(ActualHandoffType.DIRECT);
        assertThat(response.isDelayed()).isFalse();
        assertThat(response.delayMinutes()).isNull();
        assertThat(response.hasCompletionPhoto()).isFalse();
    }

    private Delivery completedDelivery(Long id, String publicId) {
        DeliveryGroup group = mock(DeliveryGroup.class);
        DeliverySlot slot = mock(DeliverySlot.class);
        lenient().when(group.getDeliveryDate()).thenReturn(LocalDate.of(2026, 9, 6));
        lenient().when(group.getSlot()).thenReturn(slot);
        lenient().when(slot.getCode()).thenReturn(DeliverySlotCode.LUNCH);
        Delivery delivery = mock(Delivery.class);
        lenient().when(delivery.getId()).thenReturn(id);
        lenient().when(delivery.getCustomerId()).thenReturn(100L);
        lenient().when(delivery.getDeliveryPublicId()).thenReturn(publicId);
        lenient().when(delivery.getDeliveryGroup()).thenReturn(group);
        lenient().when(delivery.getStatus()).thenReturn(DeliveryStatus.DELIVERED);
        lenient().when(delivery.getLunchboxQuantity()).thenReturn(2);
        return delivery;
    }

    private DeliveryCompletion completion(
        Long id
        , Delivery delivery
        , ActualHandoffType handoffType
        , LocalDateTime completedAt
    ) {
        DeliveryCompletion completion = mock(DeliveryCompletion.class);
        lenient().when(completion.getId()).thenReturn(id);
        lenient().when(completion.getDelivery()).thenReturn(delivery);
        lenient().when(completion.getActualHandoffType()).thenReturn(handoffType);
        lenient().when(completion.getCompletedAt()).thenReturn(completedAt);
        return completion;
    }

    private void stubDetail(
        Delivery delivery
        , DeliveryCompletion completion
        , DeliveryDelay delay
        , List<DeliveryCompletionPhoto> photos
    ) {
        when(deliveryRepository.findDetailByDeliveryPublicId(delivery.getDeliveryPublicId()))
            .thenReturn(Optional.of(delivery));
        when(completionRepository.findAllByDeliveryIdIn(List.of(delivery.getId())))
            .thenReturn(List.of(completion));
        when(failureRepository.findAllByDeliveryIdIn(List.of(delivery.getId())))
            .thenReturn(List.of());
        when(delayRepository.findAllByDeliveryIdIn(List.of(delivery.getId())))
            .thenReturn(delay == null ? List.of() : List.of(delay));
        when(photoRepository.findAllByDeliveryCompletionIdIn(List.of(completion.getId())))
            .thenReturn(photos);
    }
}
