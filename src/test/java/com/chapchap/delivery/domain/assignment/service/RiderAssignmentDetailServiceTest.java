package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentDeliveryItemResponse;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentDetailResponse;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryRecipientSnapshot;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRecipientSnapshotRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentNotFoundException;
import com.chapchap.delivery.global.security.PersonalDataEncryptor;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderAssignmentDetailServiceTest {

    private static final Long AUTH_USER_ID = 10001L;
    private static final Long ASSIGNMENT_ID = 1L;
    private static final Long DELIVERY_ID = 101L;
    private static final LocalDate DELIVERY_DATE =
        LocalDate.of(2026, 9, 5);

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private DeliveryAssignmentItemRepository deliveryAssignmentItemRepository;

    @Mock
    private DeliveryRecipientSnapshotRepository deliveryRecipientSnapshotRepository;

    @Mock
    private DeliveryAccessService deliveryAccessService;

    @Mock
    private PersonalDataEncryptor personalDataEncryptor;

    private RiderAssignmentDetailService riderAssignmentDetailService;

    @BeforeEach
    void setUp() {
        riderAssignmentDetailService =
            new RiderAssignmentDetailService(
                deliveryAssignmentRepository
                , deliveryAssignmentItemRepository
                , deliveryRecipientSnapshotRepository
                , deliveryAccessService
                , personalDataEncryptor
            );
    }

    @Test
    @DisplayName("현재 담당 기사이고 배송이 진행 중이면 개인정보를 복호화해서 반환한다")
    void getMyAssignmentDetailReturnsDecryptedPersonalDataWhenAccessible() {
        // given
        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        DeliveryAssignmentItem assignmentItem =
            mock(DeliveryAssignmentItem.class);

        Delivery delivery =
            mock(Delivery.class);

        DeliveryRecipientSnapshot recipientSnapshot =
            mock(DeliveryRecipientSnapshot.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        DeliverySlot deliverySlot =
            mock(DeliverySlot.class);

        Rider rider =
            mock(Rider.class);

        byte[] phoneEncrypted =
            new byte[] {1, 2, 3};

        byte[] entranceInfoEncrypted =
            new byte[] {4, 5, 6};

        when(
            deliveryAccessService.isRiderAccessAllowed(
                AUTH_USER_ID
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                ASSIGNMENT_ID
                , AUTH_USER_ID
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(true);

        when(assignment.getStatus())
            .thenReturn(
                DeliveryAssignmentStatus.ACKNOWLEDGED
            );

        when(assignment.getId())
            .thenReturn(ASSIGNMENT_ID);

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(DELIVERY_DATE);

        when(deliveryGroup.getSlot())
            .thenReturn(deliverySlot);

        when(deliverySlot.getCode())
            .thenReturn(
                DeliverySlotCode.LUNCH
            );

        when(
            deliveryAssignmentItemRepository.findAllByAssignmentIdWithDelivery(
                ASSIGNMENT_ID
            )
        )
            .thenReturn(
                List.of(assignmentItem)
            );

        when(assignmentItem.getDelivery())
            .thenReturn(delivery);

        when(delivery.getId())
            .thenReturn(DELIVERY_ID);

        when(delivery.getDeliveryPublicId())
            .thenReturn(
                "11111111-1111-1111-1111-111111111111"
            );

        when(delivery.getStatus())
            .thenReturn(
                DeliveryStatus.READY
            );

        when(delivery.getLunchboxQuantity())
            .thenReturn(2);

        when(delivery.getMenuNameSnapshot())
            .thenReturn(
                "닭가슴살 도시락"
            );

        when(delivery.getTermsAgreed())
            .thenReturn(true);

        when(
            deliveryRecipientSnapshotRepository.findAllByDeliveryIdIn(
                List.of(DELIVERY_ID)
            )
        )
            .thenReturn(
                List.of(recipientSnapshot)
            );

        when(recipientSnapshot.getDeliveryId())
            .thenReturn(DELIVERY_ID);

        when(recipientSnapshot.getRecipientName())
            .thenReturn(
                "홍길동"
            );

        when(recipientSnapshot.getPhoneEncrypted())
            .thenReturn(phoneEncrypted);

        when(recipientSnapshot.getPostalCode())
            .thenReturn(
                "06236"
            );

        when(recipientSnapshot.getBaseAddress())
            .thenReturn(
                "서울특별시 강남구 테헤란로 123"
            );

        when(recipientSnapshot.getDetailAddress())
            .thenReturn(
                "101동 1001호"
            );

        when(recipientSnapshot.getEntranceInfoEncrypted())
            .thenReturn(entranceInfoEncrypted);

        when(recipientSnapshot.getOtherRequest())
            .thenReturn(
                "문 앞에 놓아주세요."
            );

        when(
            personalDataEncryptor.decrypt(
                phoneEncrypted
            )
        )
            .thenReturn(
                "01012345678"
            );

        when(
            personalDataEncryptor.decrypt(
                entranceInfoEncrypted
            )
        )
            .thenReturn(
                "공동현관 1234"
            );

        // when
        RiderAssignmentDetailResponse response =
            riderAssignmentDetailService.getMyAssignmentDetail(
                AUTH_USER_ID
                , ASSIGNMENT_ID
            );

        // then
        assertThat(response.assignmentId())
            .isEqualTo(ASSIGNMENT_ID);

        assertThat(response.status())
            .isEqualTo(
                DeliveryAssignmentStatus.ACKNOWLEDGED
            );

        assertThat(response.deliveryDate())
            .isEqualTo(DELIVERY_DATE);

        assertThat(response.deliverySlot())
            .isEqualTo(
                DeliverySlotCode.LUNCH
            );

        assertThat(response.stopCount())
            .isEqualTo(1);

        assertThat(response.lunchboxQuantity())
            .isEqualTo(2);

        assertThat(response.deliveries())
            .hasSize(1);

        RiderAssignmentDeliveryItemResponse deliveryResponse =
            response.deliveries()
                .getFirst();

        assertThat(deliveryResponse.deliveryId())
            .isEqualTo(
                "11111111-1111-1111-1111-111111111111"
            );

        assertThat(deliveryResponse.status())
            .isEqualTo(
                DeliveryStatus.READY
            );

        assertThat(deliveryResponse.lunchboxQuantity())
            .isEqualTo(2);

        assertThat(deliveryResponse.menuName())
            .isEqualTo(
                "닭가슴살 도시락"
            );

        assertThat(deliveryResponse.recipientName())
            .isEqualTo(
                "홍길동"
            );

        assertThat(deliveryResponse.recipientPhone())
            .isEqualTo(
                "01012345678"
            );

        assertThat(deliveryResponse.postalCode())
            .isEqualTo(
                "06236"
            );

        assertThat(deliveryResponse.addressLine1())
            .isEqualTo(
                "서울특별시 강남구 테헤란로 123"
            );

        assertThat(deliveryResponse.addressLine2())
            .isEqualTo(
                "101동 1001호"
            );

        assertThat(deliveryResponse.entranceInformation())
            .isEqualTo(
                "공동현관 1234"
            );

        assertThat(deliveryResponse.otherRequest())
            .isEqualTo(
                "문 앞에 놓아주세요."
            );

        assertThat(deliveryResponse.termsAgreed())
            .isTrue();

        verify(personalDataEncryptor)
            .decrypt(phoneEncrypted);

        verify(personalDataEncryptor)
            .decrypt(entranceInfoEncrypted);
    }

    @Test
    @DisplayName("재배정된 과거 Assignment는 개인정보를 반환하지 않는다")
    void getMyAssignmentDetailDoesNotReturnPersonalDataWhenReassigned() {
        // given
        DeliveryAssignment assignment =
            mockAssignment(
                DeliveryAssignmentStatus.REASSIGNED
            );

        Delivery delivery =
            mockDelivery(
                DeliveryStatus.READY
            );

        DeliveryRecipientSnapshot recipientSnapshot =
            mockRecipientSnapshot();

        mockAssignmentItemsAndSnapshot(
            assignment
            , delivery
            , recipientSnapshot
        );

        // when
        RiderAssignmentDetailResponse response =
            riderAssignmentDetailService.getMyAssignmentDetail(
                AUTH_USER_ID
                , ASSIGNMENT_ID
            );

        // then
        RiderAssignmentDeliveryItemResponse deliveryResponse =
            response.deliveries()
                .getFirst();

        assertPersonalDataIsNull(
            deliveryResponse
        );

        verify(
            personalDataEncryptor
            , never()
        )
            .decrypt(any());
    }

    @Test
    @DisplayName("배송 완료된 Delivery는 현재 담당 Assignment라도 개인정보를 반환하지 않는다")
    void getMyAssignmentDetailDoesNotReturnPersonalDataWhenDelivered() {
        // given
        DeliveryAssignment assignment =
            mockAssignment(
                DeliveryAssignmentStatus.ACKNOWLEDGED
            );

        Delivery delivery =
            mockDelivery(
                DeliveryStatus.DELIVERED
            );

        DeliveryRecipientSnapshot recipientSnapshot =
            mockRecipientSnapshot();

        mockAssignmentItemsAndSnapshot(
            assignment
            , delivery
            , recipientSnapshot
        );

        // when
        RiderAssignmentDetailResponse response =
            riderAssignmentDetailService.getMyAssignmentDetail(
                AUTH_USER_ID
                , ASSIGNMENT_ID
            );

        // then
        RiderAssignmentDeliveryItemResponse deliveryResponse =
            response.deliveries()
                .getFirst();

        assertPersonalDataIsNull(
            deliveryResponse
        );

        assertThat(deliveryResponse.status())
            .isEqualTo(
                DeliveryStatus.DELIVERED
            );

        verify(
            personalDataEncryptor
            , never()
        )
            .decrypt(any());
    }

    @Test
    @DisplayName("배송 실패한 Delivery는 현재 담당 Assignment라도 개인정보를 반환하지 않는다")
    void getMyAssignmentDetailDoesNotReturnPersonalDataWhenFailed() {
        // given
        DeliveryAssignment assignment =
            mockAssignment(
                DeliveryAssignmentStatus.ACKNOWLEDGED
            );

        Delivery delivery =
            mockDelivery(
                DeliveryStatus.FAILED
            );

        DeliveryRecipientSnapshot recipientSnapshot =
            mockRecipientSnapshot();

        mockAssignmentItemsAndSnapshot(
            assignment
            , delivery
            , recipientSnapshot
        );

        // when
        RiderAssignmentDetailResponse response =
            riderAssignmentDetailService.getMyAssignmentDetail(
                AUTH_USER_ID
                , ASSIGNMENT_ID
            );

        // then
        RiderAssignmentDeliveryItemResponse deliveryResponse =
            response.deliveries()
                .getFirst();

        assertPersonalDataIsNull(
            deliveryResponse
        );

        assertThat(deliveryResponse.status())
            .isEqualTo(
                DeliveryStatus.FAILED
            );

        verify(
            personalDataEncryptor
            , never()
        )
            .decrypt(any());
    }

    @Test
    @DisplayName("Recipient Snapshot이 없으면 개인정보를 노출하지 않는다")
    void getMyAssignmentDetailDoesNotReturnPersonalDataWhenSnapshotIsMissing() {
        // given
        DeliveryAssignment assignment =
            mockAssignment(
                DeliveryAssignmentStatus.ASSIGNED
            );

        Delivery delivery =
            mockDelivery(
                DeliveryStatus.READY
            );

        DeliveryAssignmentItem assignmentItem =
            mock(DeliveryAssignmentItem.class);

        when(
            deliveryAssignmentItemRepository.findAllByAssignmentIdWithDelivery(
                ASSIGNMENT_ID
            )
        )
            .thenReturn(
                List.of(assignmentItem)
            );

        when(assignmentItem.getDelivery())
            .thenReturn(delivery);

        when(
            deliveryRecipientSnapshotRepository.findAllByDeliveryIdIn(
                List.of(DELIVERY_ID)
            )
        )
            .thenReturn(
                List.of()
            );

        // when
        RiderAssignmentDetailResponse response =
            riderAssignmentDetailService.getMyAssignmentDetail(
                AUTH_USER_ID
                , ASSIGNMENT_ID
            );

        // then
        assertThat(response.deliveries())
            .hasSize(1);

        RiderAssignmentDeliveryItemResponse deliveryResponse =
            response.deliveries()
                .getFirst();

        assertPersonalDataIsNull(
            deliveryResponse
        );

        verify(
            personalDataEncryptor
            , never()
        )
            .decrypt(any());
    }

    @Test
    @DisplayName("본인의 Assignment가 아니면 상세 조회할 수 없다")
    void getMyAssignmentDetailThrowsNotFoundWhenAssignmentIsNotMine() {
        // given
        when(
            deliveryAccessService.isRiderAccessAllowed(
                AUTH_USER_ID
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                ASSIGNMENT_ID
                , AUTH_USER_ID
            )
        )
            .thenReturn(
                Optional.empty()
            );

        // when & then
        assertThatThrownBy(
            () ->
                riderAssignmentDetailService.getMyAssignmentDetail(
                    AUTH_USER_ID
                    , ASSIGNMENT_ID
                )
        )
            .isInstanceOf(
                DeliveryAssignmentNotFoundException.class
            );

        verify(
            deliveryAssignmentItemRepository
            , never()
        )
            .findAllByAssignmentIdWithDelivery(
                any()
            );

        verify(
            deliveryRecipientSnapshotRepository
            , never()
        )
            .findAllByDeliveryIdIn(
                any()
            );

        verify(
            personalDataEncryptor
            , never()
        )
            .decrypt(any());
    }

    @Test
    @DisplayName("배송 접근 권한이 없으면 Assignment 상세 조회를 거부한다")
    void getMyAssignmentDetailThrowsForbiddenWhenRiderAccessIsNotAllowed() {
        // given
        when(
            deliveryAccessService.isRiderAccessAllowed(
                AUTH_USER_ID
            )
        )
            .thenReturn(false);

        // when & then
        assertThatThrownBy(
            () ->
                riderAssignmentDetailService.getMyAssignmentDetail(
                    AUTH_USER_ID
                    , ASSIGNMENT_ID
                )
        )
            .isInstanceOf(
                DeliveryAccessForbiddenException.class
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .findMineById(
                any()
                , any()
            );

        verify(
            deliveryAssignmentItemRepository
            , never()
        )
            .findAllByAssignmentIdWithDelivery(
                any()
            );

        verify(
            personalDataEncryptor
            , never()
        )
            .decrypt(any());
    }

    @Test
    @DisplayName("배송 비활성 기사는 Assignment 상세 조회를 할 수 없다")
    void getMyAssignmentDetailThrowsForbiddenWhenRiderIsNotDeliveryActive() {
        // given
        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        Rider rider =
            mock(Rider.class);

        when(
            deliveryAccessService.isRiderAccessAllowed(
                AUTH_USER_ID
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                ASSIGNMENT_ID
                , AUTH_USER_ID
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(false);

        // when & then
        assertThatThrownBy(
            () ->
                riderAssignmentDetailService.getMyAssignmentDetail(
                    AUTH_USER_ID
                    , ASSIGNMENT_ID
                )
        )
            .isInstanceOf(
                DeliveryAccessForbiddenException.class
            );

        verify(
            deliveryAssignmentItemRepository
            , never()
        )
            .findAllByAssignmentIdWithDelivery(
                any()
            );

        verify(
            deliveryRecipientSnapshotRepository
            , never()
        )
            .findAllByDeliveryIdIn(
                any()
            );

        verify(
            personalDataEncryptor
            , never()
        )
            .decrypt(any());
    }

    private DeliveryAssignment mockAssignment(
        DeliveryAssignmentStatus status
    ) {
        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        DeliverySlot deliverySlot =
            mock(DeliverySlot.class);

        Rider rider =
            mock(Rider.class);

        when(
            deliveryAccessService.isRiderAccessAllowed(
                AUTH_USER_ID
            )
        )
            .thenReturn(true);

        when(
            deliveryAssignmentRepository.findMineById(
                ASSIGNMENT_ID
                , AUTH_USER_ID
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(true);

        when(assignment.getId())
            .thenReturn(ASSIGNMENT_ID);

        when(assignment.getStatus())
            .thenReturn(status);

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(DELIVERY_DATE);

        when(deliveryGroup.getSlot())
            .thenReturn(deliverySlot);

        when(deliverySlot.getCode())
            .thenReturn(
                DeliverySlotCode.LUNCH
            );

        return assignment;
    }

    private Delivery mockDelivery(
        DeliveryStatus status
    ) {
        Delivery delivery =
            mock(Delivery.class);

        when(delivery.getId())
            .thenReturn(DELIVERY_ID);

        when(delivery.getDeliveryPublicId())
            .thenReturn(
                "11111111-1111-1111-1111-111111111111"
            );

        when(delivery.getStatus())
            .thenReturn(status);

        when(delivery.getLunchboxQuantity())
            .thenReturn(2);

        when(delivery.getMenuNameSnapshot())
            .thenReturn(
                "닭가슴살 도시락"
            );

        when(delivery.getTermsAgreed())
            .thenReturn(true);

        return delivery;
    }

    private DeliveryRecipientSnapshot mockRecipientSnapshot() {
        DeliveryRecipientSnapshot recipientSnapshot =
            mock(DeliveryRecipientSnapshot.class);

        when(recipientSnapshot.getDeliveryId())
            .thenReturn(DELIVERY_ID);

        return recipientSnapshot;
    }

    private void mockAssignmentItemsAndSnapshot(
        DeliveryAssignment assignment
        , Delivery delivery
        , DeliveryRecipientSnapshot recipientSnapshot
    ) {
        DeliveryAssignmentItem assignmentItem =
            mock(DeliveryAssignmentItem.class);

        when(
            deliveryAssignmentItemRepository.findAllByAssignmentIdWithDelivery(
                ASSIGNMENT_ID
            )
        )
            .thenReturn(
                List.of(assignmentItem)
            );

        when(assignmentItem.getDelivery())
            .thenReturn(delivery);

        when(
            deliveryRecipientSnapshotRepository.findAllByDeliveryIdIn(
                List.of(DELIVERY_ID)
            )
        )
            .thenReturn(
                List.of(recipientSnapshot)
            );
    }

    private void assertPersonalDataIsNull(
        RiderAssignmentDeliveryItemResponse response
    ) {
        assertThat(response.recipientName())
            .isNull();

        assertThat(response.recipientPhone())
            .isNull();

        assertThat(response.postalCode())
            .isNull();

        assertThat(response.addressLine1())
            .isNull();

        assertThat(response.addressLine2())
            .isNull();

        assertThat(response.entranceInformation())
            .isNull();

        assertThat(response.otherRequest())
            .isNull();
    }
}