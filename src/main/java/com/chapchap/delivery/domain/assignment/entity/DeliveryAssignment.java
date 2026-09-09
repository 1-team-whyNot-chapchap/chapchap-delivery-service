package com.chapchap.delivery.domain.assignment.entity;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentType;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.rider.entity.Rider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_assignments")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "delivery_group_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private DeliveryGroup deliveryGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "rider_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private Rider rider;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 10)
    private DeliveryAssignmentType assignmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeliveryAssignmentStatus status;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public DeliveryAssignment(
        DeliveryGroup deliveryGroup
        , Rider rider
        , LocalDateTime assignedAt
    ) {
        this(
            deliveryGroup
            , rider
            , DeliveryAssignmentType.AUTO
            , assignedAt
            , null
        );
    }

    public DeliveryAssignment(
        DeliveryGroup deliveryGroup
        , Rider rider
        , DeliveryAssignmentType assignmentType
        , LocalDateTime assignedAt
        , Long createdBy
    ) {
        this.deliveryGroup = deliveryGroup;
        this.rider = rider;
        this.assignmentType = assignmentType;
        this.status = DeliveryAssignmentStatus.ASSIGNED;
        this.assignedAt = assignedAt;
        this.createdBy = createdBy;
    }

    public void markNotified(LocalDateTime notifiedAt) {
        if (this.notifiedAt != null) {
            return;
        }

        this.notifiedAt = notifiedAt;
    }

    public boolean isAssigned() {
        return status == DeliveryAssignmentStatus.ASSIGNED;
    }

    public boolean isAcknowledged() {
        return status == DeliveryAssignmentStatus.ACKNOWLEDGED;
    }

    public boolean canReportIssue() {
        return status == DeliveryAssignmentStatus.ASSIGNED
            || status == DeliveryAssignmentStatus.ACKNOWLEDGED;
    }

    public void confirm(Long confirmedBy, LocalDateTime confirmedAt) {
        if (!isAcknowledged()) {
            throw new IllegalStateException("Only acknowledged assignments can be confirmed");
        }

        this.status = DeliveryAssignmentStatus.CONFIRMED;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = confirmedAt;
    }
}
