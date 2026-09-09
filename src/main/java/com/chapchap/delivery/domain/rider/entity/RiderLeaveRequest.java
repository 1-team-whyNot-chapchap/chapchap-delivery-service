package com.chapchap.delivery.domain.rider.entity;

import com.chapchap.delivery.domain.rider.constant.RiderLeaveRequestStatus;
import com.chapchap.delivery.domain.rider.constant.RiderLeaveSlotType;
import com.chapchap.delivery.domain.rider.constant.RiderLeaveType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rider_leave_requests")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiderLeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Rider rider;

    @Column(name = "leave_date", nullable = false)
    private LocalDate leaveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_slot", nullable = false, length = 16)
    private RiderLeaveSlotType leaveSlot;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 32)
    private RiderLeaveType leaveType;

    @Column(name = "reason_detail", length = 255)
    private String reasonDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RiderLeaveRequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_reason_detail", length = 500)
    private String reviewReasonDetail;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public RiderLeaveRequest(Rider rider, LocalDate leaveDate, RiderLeaveSlotType leaveSlot,
                             RiderLeaveType leaveType, String reasonDetail, LocalDateTime requestedAt) {
        this.rider = rider;
        this.leaveDate = leaveDate;
        this.leaveSlot = leaveSlot;
        this.leaveType = leaveType;
        this.reasonDetail = reasonDetail;
        this.status = RiderLeaveRequestStatus.PENDING;
        this.requestedAt = requestedAt;
    }

    public void approve(Long reviewerId, LocalDateTime reviewedAt) {
        requirePending();
        this.status = RiderLeaveRequestStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = reviewedAt;
        this.reviewReasonDetail = null;
    }

    public void reject(Long reviewerId, LocalDateTime reviewedAt, String reasonDetail) {
        requirePending();
        this.status = RiderLeaveRequestStatus.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = reviewedAt;
        this.reviewReasonDetail = reasonDetail;
    }

    public boolean overlaps(RiderLeaveSlotType candidate) {
        return leaveSlot == RiderLeaveSlotType.ALL_DAY
            || candidate == RiderLeaveSlotType.ALL_DAY
            || leaveSlot == candidate;
    }

    public boolean isPending() {
        return status == RiderLeaveRequestStatus.PENDING;
    }

    private void requirePending() {
        if (status != RiderLeaveRequestStatus.PENDING) {
            throw new IllegalStateException("Leave request is already reviewed");
        }
    }
}
