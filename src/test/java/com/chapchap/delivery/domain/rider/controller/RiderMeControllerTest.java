package com.chapchap.delivery.domain.rider.controller;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueCode;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentType;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentIssue;
import com.chapchap.delivery.domain.assignment.request.RiderAssignmentIssueRequest;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentAcknowledgementResponse;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentIssueResponse;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentListItemResponse;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentListResponse;
import com.chapchap.delivery.domain.assignment.service.RiderAssignmentAcknowledgementService;
import com.chapchap.delivery.domain.assignment.service.RiderAssignmentIssueService;
import com.chapchap.delivery.domain.assignment.service.RiderAssignmentQueryService;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.rider.constant.RiderScheduleSource;
import com.chapchap.delivery.domain.rider.response.RiderScheduleItemResponse;
import com.chapchap.delivery.domain.rider.response.RiderScheduleResponse;
import com.chapchap.delivery.domain.rider.service.RiderScheduleService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderMeControllerTest {

    private static final Long RIDER_ID = 10L;
    private static final Long ACTOR_ID = 10001L;
    private static final LocalDate DATE_FROM = LocalDate.of(2026, 8, 24);
    private static final LocalDate DATE_TO = LocalDate.of(2026, 8, 25);

    @Mock
    private RiderScheduleService riderScheduleService;

    @Mock
    private RiderAssignmentAcknowledgementService riderAssignmentAcknowledgementService;

    @Mock
    private RiderAssignmentIssueService riderAssignmentIssueService;

    @Mock
    private RiderAssignmentQueryService riderAssignmentQueryService;

    @InjectMocks
    private RiderMeController riderMeController;

    @Test
    @DisplayName("기사가 본인의 실제 일정 목록을 조회한다")
    void getMySchedules() {
        // given
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(ACTOR_ID, UserRole.RIDER);

        RiderScheduleResponse serviceResponse =
            new RiderScheduleResponse(
                RIDER_ID
                , true
                , List.of(
                new RiderScheduleItemResponse(
                    LocalDate.of(2026, 8, 24)
                    , DeliverySlotCode.LUNCH
                    , false
                    , RiderScheduleSource.DATE_EXCEPTION
                )
                , new RiderScheduleItemResponse(
                    LocalDate.of(2026, 8, 25)
                    , DeliverySlotCode.DINNER
                    , true
                    , RiderScheduleSource.WEEKLY_DEFAULT
                )
            )
            );

        when(riderScheduleService.getMySchedules(ACTOR_ID, UserRole.RIDER, DATE_FROM, DATE_TO))
            .thenReturn(serviceResponse);

        // when
        ApiResponse<RiderScheduleResponse> response =
            riderMeController.getMySchedules(authenticatedUser, DATE_FROM, DATE_TO);

        // then
        verify(riderScheduleService).getMySchedules(ACTOR_ID, UserRole.RIDER, DATE_FROM, DATE_TO);

        assertThat(response.code()).isEqualTo("00");
        assertThat(response.message()).isEqualTo("SUCCESS");
        assertThat(response.data()).isEqualTo(serviceResponse);
        assertThat(response.data().riderId()).isEqualTo(RIDER_ID);
        assertThat(response.data().isDeliveryActive()).isTrue();
        assertThat(response.data().schedules()).hasSize(2);

        assertThat(response.data().schedules().getFirst().date())
            .isEqualTo(LocalDate.of(2026, 8, 24));

        assertThat(response.data().schedules().getFirst().deliverySlot())
            .isEqualTo(DeliverySlotCode.LUNCH);

        assertThat(response.data().schedules().get(0).isWorking())
            .isFalse();

        assertThat(response.data().schedules().get(0).source())
            .isEqualTo(RiderScheduleSource.DATE_EXCEPTION);

        assertThat(response.data().schedules().get(1).date())
            .isEqualTo(LocalDate.of(2026, 8, 25));

        assertThat(response.data().schedules().get(1).deliverySlot())
            .isEqualTo(DeliverySlotCode.DINNER);

        assertThat(response.data().schedules().get(1).isWorking())
            .isTrue();

        assertThat(response.data().schedules().get(1).source())
            .isEqualTo(RiderScheduleSource.WEEKLY_DEFAULT);
    }

    @Test
    @DisplayName("기사가 본인의 배정 목록을 조회한다")
    void getMyAssignments() {
        // given
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(ACTOR_ID, UserRole.RIDER);

        LocalDate deliveryDate = LocalDate.of(2026, 9, 5);
        DeliverySlotCode deliverySlot = DeliverySlotCode.LUNCH;
        DeliveryAssignmentStatus status = DeliveryAssignmentStatus.ASSIGNED;
        Pageable pageable = PageRequest.of(0, 20);

        RiderAssignmentListItemResponse item =
            new RiderAssignmentListItemResponse(
                1L
                , 10L
                , deliveryDate
                , deliverySlot
                , DeliveryAssignmentType.AUTO
                , status
                , OffsetDateTime.parse("2026-09-04T16:10:00+09:00")
                , OffsetDateTime.parse("2026-09-05T07:00:00+09:00")
                , null
                , 8
                , 36
                , false
                , false
            );

        RiderAssignmentListResponse serviceResponse =
            new RiderAssignmentListResponse(
                List.of(item)
                , 0
                , 20
                , 1
                , 1
                , false
            );

        when(
            riderAssignmentQueryService.getMyAssignments(
                ACTOR_ID
                , deliveryDate
                , deliverySlot
                , status
                , pageable
            )
        )
            .thenReturn(serviceResponse);

        // when
        ApiResponse<RiderAssignmentListResponse> response =
            riderMeController.getMyAssignments(
                authenticatedUser
                , deliveryDate
                , deliverySlot
                , status
                , pageable
            );

        // then
        verify(riderAssignmentQueryService)
            .getMyAssignments(
                ACTOR_ID
                , deliveryDate
                , deliverySlot
                , status
                , pageable
            );

        assertThat(response.code()).isEqualTo("00");
        assertThat(response.message()).isEqualTo("SUCCESS");
        assertThat(response.data()).isEqualTo(serviceResponse);
        assertThat(response.data().items()).hasSize(1);
        assertThat(response.data().page()).isZero();
        assertThat(response.data().size()).isEqualTo(20);
        assertThat(response.data().totalElements()).isEqualTo(1);
        assertThat(response.data().totalPages()).isEqualTo(1);
        assertThat(response.data().hasNext()).isFalse();
        assertThat(response.data().items().getFirst().assignmentId()).isEqualTo(1L);
        assertThat(response.data().items().getFirst().deliveryDate()).isEqualTo(deliveryDate);
        assertThat(response.data().items().getFirst().deliverySlot()).isEqualTo(deliverySlot);
        assertThat(response.data().items().getFirst().status()).isEqualTo(status);
    }

    @Test
    @DisplayName("기사가 본인의 배정을 확인한다")
    void acknowledgeAssignment() {
        // given
        Long assignmentId = 1L;
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(ACTOR_ID, UserRole.RIDER);

        DeliveryAssignment assignment = mock(DeliveryAssignment.class);

        when(riderAssignmentAcknowledgementService.acknowledge(ACTOR_ID, assignmentId))
            .thenReturn(assignment);

        when(assignment.getId()).thenReturn(assignmentId);
        when(assignment.getStatus()).thenReturn(DeliveryAssignmentStatus.ACKNOWLEDGED);
        when(assignment.getAcknowledgedAt())
            .thenReturn(LocalDateTime.of(2026, 9, 5, 7, 30));

        // when
        ApiResponse<RiderAssignmentAcknowledgementResponse> response =
            riderMeController.acknowledgeAssignment(authenticatedUser, assignmentId);

        // then
        verify(riderAssignmentAcknowledgementService).acknowledge(ACTOR_ID, assignmentId);

        assertThat(response.code()).isEqualTo("00");
        assertThat(response.message()).isEqualTo("SUCCESS");
        assertThat(response.data().assignmentId()).isEqualTo(assignmentId);
        assertThat(response.data().status()).isEqualTo(DeliveryAssignmentStatus.ACKNOWLEDGED);
        assertThat(response.data().acknowledgedAt())
            .isEqualTo(OffsetDateTime.parse("2026-09-05T07:30:00+09:00"));
    }

    @Test
    @DisplayName("배정 확인 응답은 KST 오프셋을 포함한다")
    void acknowledgementResponseIncludesKstOffset() {
        // given
        Long assignmentId = 1L;
        DeliveryAssignment assignment = mock(DeliveryAssignment.class);

        when(assignment.getId()).thenReturn(assignmentId);
        when(assignment.getStatus()).thenReturn(DeliveryAssignmentStatus.ACKNOWLEDGED);
        when(assignment.getAcknowledgedAt())
            .thenReturn(LocalDateTime.of(2026, 9, 5, 7, 30));

        // when
        RiderAssignmentAcknowledgementResponse response =
            RiderAssignmentAcknowledgementResponse.from(assignment);

        // then
        assertThat(response.assignmentId()).isEqualTo(assignmentId);
        assertThat(response.status()).isEqualTo(DeliveryAssignmentStatus.ACKNOWLEDGED);
        assertThat(response.acknowledgedAt())
            .isEqualTo(OffsetDateTime.parse("2026-09-05T07:30:00+09:00"));
    }

    @Test
    @DisplayName("기사가 본인의 배정에 이슈를 제기한다")
    void reportAssignmentIssue() {
        // given
        Long assignmentId = 1L;
        Long issueId = 10L;

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(ACTOR_ID, UserRole.RIDER);

        RiderAssignmentIssueRequest request =
            new RiderAssignmentIssueRequest(
                DeliveryAssignmentIssueCode.VEHICLE_UNAVAILABLE
                , "차량 시동이 걸리지 않습니다."
            );

        DeliveryAssignment assignment = mock(DeliveryAssignment.class);
        DeliveryAssignmentIssue issue = mock(DeliveryAssignmentIssue.class);

        when(
            riderAssignmentIssueService.reportIssue(
                ACTOR_ID
                , assignmentId
                , DeliveryAssignmentIssueCode.VEHICLE_UNAVAILABLE
                , "차량 시동이 걸리지 않습니다."
            )
        )
            .thenReturn(issue);

        when(issue.getId()).thenReturn(issueId);
        when(issue.getAssignment()).thenReturn(assignment);
        when(assignment.getId()).thenReturn(assignmentId);
        when(assignment.getStatus()).thenReturn(DeliveryAssignmentStatus.ISSUE_REPORTED);
        when(issue.getIssueCode()).thenReturn(DeliveryAssignmentIssueCode.VEHICLE_UNAVAILABLE);
        when(issue.getIssueDetail()).thenReturn("차량 시동이 걸리지 않습니다.");
        when(issue.getReportedAt())
            .thenReturn(LocalDateTime.of(2026, 9, 5, 7, 40));

        // when
        ApiResponse<RiderAssignmentIssueResponse> response =
            riderMeController.reportAssignmentIssue(
                authenticatedUser
                , assignmentId
                , request
            );

        // then
        verify(riderAssignmentIssueService)
            .reportIssue(
                ACTOR_ID
                , assignmentId
                , DeliveryAssignmentIssueCode.VEHICLE_UNAVAILABLE
                , "차량 시동이 걸리지 않습니다."
            );

        assertThat(response.code()).isEqualTo("00");
        assertThat(response.message()).isEqualTo("SUCCESS");
        assertThat(response.data().issueId()).isEqualTo(issueId);
        assertThat(response.data().assignmentId()).isEqualTo(assignmentId);
        assertThat(response.data().assignmentStatus())
            .isEqualTo(DeliveryAssignmentStatus.ISSUE_REPORTED);
        assertThat(response.data().issueCode())
            .isEqualTo(DeliveryAssignmentIssueCode.VEHICLE_UNAVAILABLE);
        assertThat(response.data().issueDetail())
            .isEqualTo("차량 시동이 걸리지 않습니다.");
        assertThat(response.data().reportedAt())
            .isEqualTo(OffsetDateTime.parse("2026-09-05T07:40:00+09:00"));
    }
}