package com.chapchap.delivery.domain.rider.repository;

import com.chapchap.delivery.domain.rider.constant.RiderLeaveRequestStatus;
import com.chapchap.delivery.domain.rider.entity.RiderLeaveRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RiderLeaveRequestRepository extends JpaRepository<RiderLeaveRequest, Long> {
    @Query("SELECT rlr FROM RiderLeaveRequest rlr JOIN FETCH rlr.rider WHERE rlr.id = :leaveRequestId")
    Optional<RiderLeaveRequest> findDetailById(@Param("leaveRequestId") Long leaveRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rlr FROM RiderLeaveRequest rlr JOIN FETCH rlr.rider WHERE rlr.id = :leaveRequestId")
    Optional<RiderLeaveRequest> findDetailByIdForUpdate(@Param("leaveRequestId") Long leaveRequestId);

    @Query("SELECT rlr FROM RiderLeaveRequest rlr WHERE rlr.rider.authUserId = :authUserId ORDER BY rlr.requestedAt DESC, rlr.id DESC")
    List<RiderLeaveRequest> findAllMine(@Param("authUserId") Long authUserId);

    @Query("SELECT rlr FROM RiderLeaveRequest rlr JOIN FETCH rlr.rider WHERE rlr.id = :leaveRequestId AND rlr.rider.authUserId = :authUserId")
    Optional<RiderLeaveRequest> findMineDetailById(@Param("leaveRequestId") Long leaveRequestId, @Param("authUserId") Long authUserId);

    @Query("""
        SELECT rlr FROM RiderLeaveRequest rlr
        JOIN FETCH rlr.rider r
        WHERE (:riderId IS NULL OR r.id = :riderId)
          AND (:status IS NULL OR rlr.status = :status)
          AND (:leaveSlot IS NULL OR rlr.leaveSlot = :leaveSlot)
          AND (:leaveDateFrom IS NULL OR rlr.leaveDate >= :leaveDateFrom)
          AND (:leaveDateTo IS NULL OR rlr.leaveDate <= :leaveDateTo)
        """)
    Page<RiderLeaveRequest> findAllForAdmin(@Param("riderId") Long riderId,
                                             @Param("status") RiderLeaveRequestStatus status,
                                             @Param("leaveSlot") com.chapchap.delivery.domain.rider.constant.RiderLeaveSlotType leaveSlot,
                                             @Param("leaveDateFrom") LocalDate leaveDateFrom,
                                             @Param("leaveDateTo") LocalDate leaveDateTo,
                                             Pageable pageable);

    @Query("""
        SELECT rlr FROM RiderLeaveRequest rlr
        WHERE rlr.rider.id = :riderId
          AND rlr.leaveDate = :leaveDate
          AND rlr.status IN :statuses
        """)
    List<RiderLeaveRequest> findActiveByRiderAndLeaveDate(@Param("riderId") Long riderId,
                                                            @Param("leaveDate") LocalDate leaveDate,
                                                            @Param("statuses") List<RiderLeaveRequestStatus> statuses);
}
