package com.chapchap.delivery.domain.rider.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.rider.constant.RiderLeaveRequestStatus;
import com.chapchap.delivery.domain.rider.constant.RiderLeaveSlotType;
import com.chapchap.delivery.domain.rider.constant.RiderLeaveType;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.entity.RiderLeaveRequest;
import com.chapchap.delivery.domain.rider.repository.RiderLeaveRequestRepository;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.rider.request.RiderLeaveRequestCreateRequest;
import com.chapchap.delivery.global.exception.business.RiderLeaveRequestDeadlinePassedException;
import com.chapchap.delivery.global.exception.business.RiderLeaveRequestOverlapException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RiderLeaveRequestServiceTest {
    private final RiderRepository riderRepository = mock(RiderRepository.class);
    private final RiderLeaveRequestRepository leaveRepository = mock(RiderLeaveRequestRepository.class);
    private final DeliveryAccessService accessService = mock(DeliveryAccessService.class);
    private final AuditHistoryRepository auditRepository = mock(AuditHistoryRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-15T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final RiderLeaveRequestService service = new RiderLeaveRequestService(
        riderRepository, leaveRepository, accessService, auditRepository, clock
    );

    @Test
    void acceptsRequestOnD5AndRecordsPendingAudit() {
        Rider rider = rider(10L, 100L);
        when(riderRepository.findByAuthUserIdForUpdate(100L)).thenReturn(Optional.of(rider));
        when(leaveRepository.findActiveByRiderAndLeaveDate(eq(10L), eq(LocalDate.of(2026, 9, 20)), any()))
            .thenReturn(List.of());
        when(leaveRepository.save(any())).thenAnswer(invocation -> {
            RiderLeaveRequest value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 44L);
            return value;
        });

        var response = service.create(100L, UserRole.RIDER, new RiderLeaveRequestCreateRequest(
            LocalDate.of(2026, 9, 20), RiderLeaveSlotType.LUNCH, RiderLeaveType.ANNUAL_LEAVE, null
        ));

        assertThat(response.leaveRequestId()).isEqualTo(44L);
        assertThat(response.status()).isEqualTo(RiderLeaveRequestStatus.PENDING);
        verify(auditRepository).save(any());
    }

    @Test
    void rejectsRequestFromD4() {
        assertThatThrownBy(() -> service.create(100L, UserRole.RIDER, new RiderLeaveRequestCreateRequest(
            LocalDate.of(2026, 9, 19), RiderLeaveSlotType.LUNCH, RiderLeaveType.SICK_LEAVE, null
        ))).isInstanceOf(RiderLeaveRequestDeadlinePassedException.class);
        verifyNoInteractions(riderRepository, leaveRepository, auditRepository);
    }

    @Test
    void rejectsAllDayAndLunchOverlap() {
        Rider rider = rider(10L, 100L);
        RiderLeaveRequest existing = new RiderLeaveRequest(rider, LocalDate.of(2026, 9, 20),
            RiderLeaveSlotType.ALL_DAY, RiderLeaveType.ANNUAL_LEAVE, null, java.time.LocalDateTime.now(clock));
        when(riderRepository.findByAuthUserIdForUpdate(100L)).thenReturn(Optional.of(rider));
        when(leaveRepository.findActiveByRiderAndLeaveDate(eq(10L), eq(LocalDate.of(2026, 9, 20)), any()))
            .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.create(100L, UserRole.RIDER, new RiderLeaveRequestCreateRequest(
            LocalDate.of(2026, 9, 20), RiderLeaveSlotType.LUNCH, RiderLeaveType.ANNUAL_LEAVE, null
        ))).isInstanceOf(RiderLeaveRequestOverlapException.class);
    }

    private Rider rider(Long id, Long authUserId) {
        Rider rider = new Rider(authUserId);
        ReflectionTestUtils.setField(rider, "id", id);
        return rider;
    }
}
