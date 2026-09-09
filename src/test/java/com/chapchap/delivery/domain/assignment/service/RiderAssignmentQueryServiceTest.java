package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentType;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.repository.RiderAssignmentListProjection;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentListItemResponse;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentListResponse;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderAssignmentQueryServiceTest {

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private DeliveryAccessService deliveryAccessService;

    @Mock
    private RiderAssignmentListProjection projection;

    private RiderAssignmentQueryService riderAssignmentQueryService;

    @BeforeEach
    void setUp() {
        riderAssignmentQueryService =
            new RiderAssignmentQueryService(
                deliveryAssignmentRepository
                , deliveryAccessService
            );
    }

    @Test
    @DisplayName("기사가 본인의 배정 목록을 조회한다")
    void getMyAssignments() {
        // given
        Long authUserId = 10001L;

        Pageable pageable =
            PageRequest.of(
                0
                , 20
            );

        LocalDate deliveryDate =
            LocalDate.of(
                2026
                , 9
                , 5
            );

        DeliverySlotCode deliverySlot =
            DeliverySlotCode.LUNCH;

        DeliveryAssignmentStatus status =
            DeliveryAssignmentStatus.ACKNOWLEDGED;

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        when(
            projection.getAssignmentId()
        )
            .thenReturn(1L);

        when(
            projection.getDeliveryGroupId()
        )
            .thenReturn(10L);

        when(
            projection.getDeliveryDate()
        )
            .thenReturn(deliveryDate);

        when(
            projection.getDeliverySlot()
        )
            .thenReturn(deliverySlot);

        when(
            projection.getAssignmentType()
        )
            .thenReturn(
                DeliveryAssignmentType.AUTO
            );

        when(
            projection.getStatus()
        )
            .thenReturn(status);

        when(
            projection.getAssignedAt()
        )
            .thenReturn(
                LocalDateTime.of(
                    2026
                    , 9
                    , 4
                    , 16
                    , 10
                )
            );

        when(
            projection.getAcknowledgedAt()
        )
            .thenReturn(
                LocalDateTime.of(
                    2026
                    , 9
                    , 5
                    , 7
                    , 30
                )
            );

        when(
            projection.getStopCount()
        )
            .thenReturn(8L);

        when(
            projection.getLunchboxQuantity()
        )
            .thenReturn(36L);

        when(
            deliveryAssignmentRepository.findAllMine(
                authUserId
                , deliveryDate
                , deliverySlot
                , status
                , pageable
            )
        )
            .thenReturn(
                new PageImpl<>(
                    List.of(projection)
                    , pageable
                    , 1
                )
            );

        // when
        RiderAssignmentListResponse response =
            riderAssignmentQueryService.getMyAssignments(
                authUserId
                , deliveryDate
                , deliverySlot
                , status
                , pageable
            );

        // then
        verify(
            deliveryAssignmentRepository
        )
            .findAllMine(
                authUserId
                , deliveryDate
                , deliverySlot
                , status
                , pageable
            );

        assertThat(response.page())
            .isZero();

        assertThat(response.size())
            .isEqualTo(20);

        assertThat(response.totalElements())
            .isEqualTo(1);

        assertThat(response.totalPages())
            .isEqualTo(1);

        assertThat(response.hasNext())
            .isFalse();

        assertThat(response.items())
            .hasSize(1);

        RiderAssignmentListItemResponse item =
            response.items()
                .getFirst();

        assertThat(item.assignmentId())
            .isEqualTo(1L);

        assertThat(item.deliveryGroupId())
            .isEqualTo(10L);

        assertThat(item.deliveryDate())
            .isEqualTo(deliveryDate);

        assertThat(item.deliverySlot())
            .isEqualTo(
                DeliverySlotCode.LUNCH
            );

        assertThat(item.assignmentType())
            .isEqualTo(
                DeliveryAssignmentType.AUTO
            );

        assertThat(item.status())
            .isEqualTo(
                DeliveryAssignmentStatus.ACKNOWLEDGED
            );

        assertThat(item.assignedAt())
            .isEqualTo(
                OffsetDateTime.parse(
                    "2026-09-04T16:10:00+09:00"
                )
            );

        assertThat(item.acknowledgementAvailableAt())
            .isEqualTo(
                OffsetDateTime.parse(
                    "2026-09-05T07:00:00+09:00"
                )
            );

        assertThat(item.acknowledgedAt())
            .isEqualTo(
                OffsetDateTime.parse(
                    "2026-09-05T07:30:00+09:00"
                )
            );

        assertThat(item.stopCount())
            .isEqualTo(8);

        assertThat(item.lunchboxQuantity())
            .isEqualTo(36);

        assertThat(item.recommendedCapacityExceeded())
            .isFalse();

        assertThat(item.maximumCapacityExceeded())
            .isFalse();
    }

    @Test
    @DisplayName("권장 수용량을 초과하면 recommendedCapacityExceeded가 true다")
    void getMyAssignmentsMarksRecommendedCapacityExceeded() {
        // given
        Long authUserId = 10001L;

        Pageable pageable =
            PageRequest.of(
                0
                , 20
            );

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        when(
            projection.getAssignmentId()
        )
            .thenReturn(1L);

        when(
            projection.getDeliveryGroupId()
        )
            .thenReturn(10L);

        when(
            projection.getDeliveryDate()
        )
            .thenReturn(
                LocalDate.of(
                    2026
                    , 9
                    , 5
                )
            );

        when(
            projection.getDeliverySlot()
        )
            .thenReturn(
                DeliverySlotCode.LUNCH
            );

        when(
            projection.getAssignmentType()
        )
            .thenReturn(
                DeliveryAssignmentType.AUTO
            );

        when(
            projection.getStatus()
        )
            .thenReturn(
                DeliveryAssignmentStatus.ASSIGNED
            );

        when(
            projection.getAssignedAt()
        )
            .thenReturn(
                LocalDateTime.of(
                    2026
                    , 9
                    , 4
                    , 16
                    , 10
                )
            );

        when(
            projection.getAcknowledgedAt()
        )
            .thenReturn(null);

        when(
            projection.getStopCount()
        )
            .thenReturn(9L);

        when(
            projection.getLunchboxQuantity()
        )
            .thenReturn(36L);

        when(
            deliveryAssignmentRepository.findAllMine(
                authUserId
                , null
                , null
                , null
                , pageable
            )
        )
            .thenReturn(
                new PageImpl<>(
                    List.of(projection)
                    , pageable
                    , 1
                )
            );

        // when
        RiderAssignmentListResponse response =
            riderAssignmentQueryService.getMyAssignments(
                authUserId
                , null
                , null
                , null
                , pageable
            );

        // then
        RiderAssignmentListItemResponse item =
            response.items()
                .getFirst();

        assertThat(item.recommendedCapacityExceeded())
            .isTrue();

        assertThat(item.maximumCapacityExceeded())
            .isFalse();

        assertThat(item.acknowledgedAt())
            .isNull();
    }

    @Test
    @DisplayName("최대 수용량을 초과하면 maximumCapacityExceeded가 true다")
    void getMyAssignmentsMarksMaximumCapacityExceeded() {
        // given
        Long authUserId = 10001L;

        Pageable pageable =
            PageRequest.of(
                0
                , 20
            );

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(true);

        when(
            projection.getAssignmentId()
        )
            .thenReturn(1L);

        when(
            projection.getDeliveryGroupId()
        )
            .thenReturn(10L);

        when(
            projection.getDeliveryDate()
        )
            .thenReturn(
                LocalDate.of(
                    2026
                    , 9
                    , 5
                )
            );

        when(
            projection.getDeliverySlot()
        )
            .thenReturn(
                DeliverySlotCode.DINNER
            );

        when(
            projection.getAssignmentType()
        )
            .thenReturn(
                DeliveryAssignmentType.AUTO
            );

        when(
            projection.getStatus()
        )
            .thenReturn(
                DeliveryAssignmentStatus.ASSIGNED
            );

        when(
            projection.getAssignedAt()
        )
            .thenReturn(
                LocalDateTime.of(
                    2026
                    , 9
                    , 4
                    , 16
                    , 10
                )
            );

        when(
            projection.getAcknowledgedAt()
        )
            .thenReturn(null);

        when(
            projection.getStopCount()
        )
            .thenReturn(11L);

        when(
            projection.getLunchboxQuantity()
        )
            .thenReturn(42L);

        when(
            deliveryAssignmentRepository.findAllMine(
                authUserId
                , null
                , null
                , null
                , pageable
            )
        )
            .thenReturn(
                new PageImpl<>(
                    List.of(projection)
                    , pageable
                    , 1
                )
            );

        // when
        RiderAssignmentListResponse response =
            riderAssignmentQueryService.getMyAssignments(
                authUserId
                , null
                , null
                , null
                , pageable
            );

        // then
        RiderAssignmentListItemResponse item =
            response.items()
                .getFirst();

        assertThat(item.recommendedCapacityExceeded())
            .isTrue();

        assertThat(item.maximumCapacityExceeded())
            .isTrue();

        assertThat(item.acknowledgementAvailableAt())
            .isEqualTo(
                OffsetDateTime.parse(
                    "2026-09-05T13:00:00+09:00"
                )
            );
    }

    @Test
    @DisplayName("배송 접근 권한이 없으면 본인 배정 목록을 조회할 수 없다")
    void getMyAssignmentsThrowsForbiddenWhenRiderAccessIsNotAllowed() {
        // given
        Long authUserId = 10001L;

        Pageable pageable =
            PageRequest.of(
                0
                , 20
            );

        when(
            deliveryAccessService.isRiderAccessAllowed(
                authUserId
            )
        )
            .thenReturn(false);

        // when & then
        assertThatThrownBy(
            () ->
                riderAssignmentQueryService.getMyAssignments(
                    authUserId
                    , null
                    , null
                    , null
                    , pageable
                )
        )
            .isInstanceOf(
                DeliveryAccessForbiddenException.class
            );

        verify(
            deliveryAssignmentRepository
            , never()
        )
            .findAllMine(
                authUserId
                , null
                , null
                , null
                , pageable
            );
    }
}