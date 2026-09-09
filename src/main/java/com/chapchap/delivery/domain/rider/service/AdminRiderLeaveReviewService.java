package com.chapchap.delivery.domain.rider.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.audit.constant.AuditActorType;
import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliverySlotRepository;
import com.chapchap.delivery.domain.rider.constant.RiderLeaveRequestStatus;
import com.chapchap.delivery.domain.rider.constant.RiderLeaveSlotType;
import com.chapchap.delivery.domain.rider.constant.RiderScheduleExceptionReason;
import com.chapchap.delivery.domain.rider.entity.RiderLeaveRequest;
import com.chapchap.delivery.domain.rider.entity.RiderScheduleException;
import com.chapchap.delivery.domain.rider.repository.RiderLeaveRequestRepository;
import com.chapchap.delivery.domain.rider.repository.RiderScheduleExceptionRepository;
import com.chapchap.delivery.domain.rider.response.AdminRiderLeaveRequestListResponse;
import com.chapchap.delivery.domain.rider.response.RiderLeaveRequestResponse;
import com.chapchap.delivery.global.exception.business.RiderLeaveRequestNotFoundException;
import com.chapchap.delivery.global.exception.business.RiderLeaveRequestStateConflictException;
import com.chapchap.delivery.global.exception.business.RiderLeaveReviewDeadlinePassedException;
import com.chapchap.delivery.global.exception.business.RiderLeaveScheduleConflictException;
import com.chapchap.delivery.global.exception.technical.DeliverySlotConfigurationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminRiderLeaveReviewService {
    private final RiderLeaveRequestRepository leaveRequestRepository;
    private final RiderScheduleExceptionRepository scheduleExceptionRepository;
    private final DeliverySlotRepository deliverySlotRepository;
    private final DeliveryAccessService deliveryAccessService;
    private final AuditHistoryRepository auditHistoryRepository;
    private final Clock kstClock;

    public AdminRiderLeaveReviewService(RiderLeaveRequestRepository leaveRequestRepository,
                                        RiderScheduleExceptionRepository scheduleExceptionRepository,
                                        DeliverySlotRepository deliverySlotRepository,
                                        DeliveryAccessService deliveryAccessService,
                                        AuditHistoryRepository auditHistoryRepository, Clock kstClock) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.scheduleExceptionRepository = scheduleExceptionRepository;
        this.deliverySlotRepository = deliverySlotRepository;
        this.deliveryAccessService = deliveryAccessService;
        this.auditHistoryRepository = auditHistoryRepository;
        this.kstClock = kstClock;
    }

    @Transactional(readOnly = true)
    public AdminRiderLeaveRequestListResponse getAll(Long actorId, UserRole role, Long riderId,
                                                       RiderLeaveRequestStatus status, RiderLeaveSlotType leaveSlot,
                                                       LocalDate leaveDateFrom, LocalDate leaveDateTo, Pageable pageable) {
        deliveryAccessService.validateAdminAccess(actorId, role);
        Page<RiderLeaveRequestResponse> page = leaveRequestRepository.findAllForAdmin(
            riderId, status, leaveSlot, leaveDateFrom, leaveDateTo, pageable
        ).map(RiderLeaveRequestResponse::from);
        return AdminRiderLeaveRequestListResponse.from(page);
    }

    @Transactional(readOnly = true)
    public RiderLeaveRequestResponse getDetail(Long actorId, UserRole role, Long leaveRequestId) {
        deliveryAccessService.validateAdminAccess(actorId, role);
        return leaveRequestRepository.findDetailById(leaveRequestId).map(RiderLeaveRequestResponse::from)
            .orElseThrow(RiderLeaveRequestNotFoundException::new);
    }

    @Transactional
    public RiderLeaveRequestResponse approve(Long actorId, UserRole role, Long leaveRequestId) {
        deliveryAccessService.validateAdminAccess(actorId, role);
        RiderLeaveRequest request = leaveRequestRepository.findDetailByIdForUpdate(leaveRequestId)
            .orElseThrow(RiderLeaveRequestNotFoundException::new);
        validateReviewable(request);

        List<DeliverySlotCode> slotCodes = affectedSlots(request.getLeaveSlot());
        List<RiderScheduleException> existing = scheduleExceptionRepository.findAllByRiderAndDateAndSlotInForUpdate(
            request.getRider().getId(), request.getLeaveDate(), slotCodes
        );
        if (!existing.isEmpty()) {
            throw new RiderLeaveScheduleConflictException();
        }

        LocalDateTime reviewedAt = LocalDateTime.now(kstClock);
        List<RiderScheduleException> exceptions = slotCodes.stream().map(slotCode -> new RiderScheduleException(
            request.getRider(), request.getLeaveDate(), slot(slotCode), false, mapReason(request),
            request.getReasonDetail(), request.getId(), actorId
        )).toList();
        scheduleExceptionRepository.saveAll(exceptions);
        request.approve(actorId, reviewedAt);
        auditHistoryRepository.save(AuditHistory.record(
            "RIDER_LEAVE_REQUEST", request.getId(), "RIDER_LEAVE_APPROVED", actorId, AuditActorType.ADMIN,
            request.getLeaveType().name(), request.getReasonDetail(), "{\"status\":\"PENDING\"}",
            "{\"status\":\"APPROVED\"}", reviewedAt
        ));
        return RiderLeaveRequestResponse.from(request);
    }

    @Transactional
    public RiderLeaveRequestResponse reject(Long actorId, UserRole role, Long leaveRequestId, String reasonDetail) {
        deliveryAccessService.validateAdminAccess(actorId, role);
        RiderLeaveRequest request = leaveRequestRepository.findDetailByIdForUpdate(leaveRequestId)
            .orElseThrow(RiderLeaveRequestNotFoundException::new);
        validateReviewable(request);
        LocalDateTime reviewedAt = LocalDateTime.now(kstClock);
        request.reject(actorId, reviewedAt, reasonDetail.trim());
        auditHistoryRepository.save(AuditHistory.record(
            "RIDER_LEAVE_REQUEST", request.getId(), "RIDER_LEAVE_REJECTED", actorId, AuditActorType.ADMIN,
            request.getLeaveType().name(), reasonDetail.trim(), "{\"status\":\"PENDING\"}",
            "{\"status\":\"REJECTED\"}", reviewedAt
        ));
        return RiderLeaveRequestResponse.from(request);
    }

    private void validateReviewable(RiderLeaveRequest request) {
        if (!request.isPending()) throw new RiderLeaveRequestStateConflictException();
        if (LocalDate.now(kstClock).isAfter(request.getLeaveDate().minusDays(2))) {
            throw new RiderLeaveReviewDeadlinePassedException();
        }
    }

    private List<DeliverySlotCode> affectedSlots(RiderLeaveSlotType leaveSlot) {
        return switch (leaveSlot) {
            case ALL_DAY -> List.of(DeliverySlotCode.LUNCH, DeliverySlotCode.DINNER);
            case LUNCH -> List.of(DeliverySlotCode.LUNCH);
            case DINNER -> List.of(DeliverySlotCode.DINNER);
        };
    }

    private DeliverySlot slot(DeliverySlotCode slotCode) {
        return deliverySlotRepository.findByCodeAndDeletedAtIsNull(slotCode)
            .orElseThrow(() -> new DeliverySlotConfigurationException(slotCode));
    }

    private RiderScheduleExceptionReason mapReason(RiderLeaveRequest request) {
        return RiderScheduleExceptionReason.valueOf(request.getLeaveType().name());
    }
}
