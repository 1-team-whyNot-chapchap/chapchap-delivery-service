package com.chapchap.delivery.domain.rider.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.audit.constant.AuditActorType;
import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.rider.constant.RiderLeaveRequestStatus;
import com.chapchap.delivery.domain.rider.constant.RiderLeaveType;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.entity.RiderLeaveRequest;
import com.chapchap.delivery.domain.rider.repository.RiderLeaveRequestRepository;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.rider.request.RiderLeaveRequestCreateRequest;
import com.chapchap.delivery.domain.rider.response.RiderLeaveRequestResponse;
import com.chapchap.delivery.global.exception.business.RiderLeaveReasonDetailRequiredException;
import com.chapchap.delivery.global.exception.business.RiderLeaveRequestDeadlinePassedException;
import com.chapchap.delivery.global.exception.business.RiderLeaveRequestNotFoundException;
import com.chapchap.delivery.global.exception.business.RiderLeaveRequestOverlapException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RiderLeaveRequestService {
    private static final List<RiderLeaveRequestStatus> ACTIVE_STATUSES =
        List.of(RiderLeaveRequestStatus.PENDING, RiderLeaveRequestStatus.APPROVED);

    private final RiderRepository riderRepository;
    private final RiderLeaveRequestRepository leaveRequestRepository;
    private final DeliveryAccessService deliveryAccessService;
    private final AuditHistoryRepository auditHistoryRepository;
    private final Clock kstClock;

    public RiderLeaveRequestService(RiderRepository riderRepository, RiderLeaveRequestRepository leaveRequestRepository,
                                    DeliveryAccessService deliveryAccessService, AuditHistoryRepository auditHistoryRepository,
                                    Clock kstClock) {
        this.riderRepository = riderRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.deliveryAccessService = deliveryAccessService;
        this.auditHistoryRepository = auditHistoryRepository;
        this.kstClock = kstClock;
    }

    @Transactional
    public RiderLeaveRequestResponse create(Long authUserId, UserRole role, RiderLeaveRequestCreateRequest request) {
        deliveryAccessService.validateRiderAccess(authUserId, role);
        validateReason(request.leaveType(), request.reasonDetail());
        LocalDate now = LocalDate.now(kstClock);
        if (now.isAfter(request.leaveDate().minusDays(5))) {
            throw new RiderLeaveRequestDeadlinePassedException();
        }

        // A rider row is the existing per-rider concurrency boundary. It serializes overlapping requests.
        Rider rider = riderRepository.findByAuthUserIdForUpdate(authUserId).orElseThrow(RiderNotFoundException::new);
        boolean overlaps = leaveRequestRepository.findActiveByRiderAndLeaveDate(rider.getId(), request.leaveDate(), ACTIVE_STATUSES)
            .stream().anyMatch(existing -> existing.overlaps(request.leaveSlot()));
        if (overlaps) {
            throw new RiderLeaveRequestOverlapException();
        }

        LocalDateTime requestedAt = LocalDateTime.now(kstClock);
        RiderLeaveRequest saved = leaveRequestRepository.save(new RiderLeaveRequest(
            rider, request.leaveDate(), request.leaveSlot(), request.leaveType(), normalize(request.reasonDetail()), requestedAt
        ));
        auditHistoryRepository.save(AuditHistory.record(
            "RIDER_LEAVE_REQUEST", saved.getId(), "RIDER_LEAVE_REQUESTED", authUserId, AuditActorType.RIDER,
            request.leaveType().name(), normalize(request.reasonDetail()), null, "{\"status\":\"PENDING\"}", requestedAt
        ));
        return RiderLeaveRequestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RiderLeaveRequestResponse> getMine(Long authUserId, UserRole role) {
        deliveryAccessService.validateRiderAccess(authUserId, role);
        return leaveRequestRepository.findAllMine(authUserId).stream().map(RiderLeaveRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RiderLeaveRequestResponse getMineDetail(Long authUserId, UserRole role, Long leaveRequestId) {
        deliveryAccessService.validateRiderAccess(authUserId, role);
        return leaveRequestRepository.findMineDetailById(leaveRequestId, authUserId)
            .map(RiderLeaveRequestResponse::from).orElseThrow(RiderLeaveRequestNotFoundException::new);
    }

    private void validateReason(RiderLeaveType leaveType, String reasonDetail) {
        if (leaveType == RiderLeaveType.OTHER && normalize(reasonDetail) == null) {
            throw new RiderLeaveReasonDetailRequiredException();
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
