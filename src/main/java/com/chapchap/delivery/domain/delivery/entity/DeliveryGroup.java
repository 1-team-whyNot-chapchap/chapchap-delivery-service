package com.chapchap.delivery.domain.delivery.entity;

import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
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
@Table(
    name = "delivery_groups"
    , uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_delivery_groups_date_slot"
            , columnNames = {
                "delivery_date"
                , "slot_id"
            }
        )
    }
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "slot_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private DeliverySlot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private DeliveryGroupStatus status;

    @Column(name = "auto_assignment_completed_at")
    private LocalDateTime autoAssignmentCompletedAt;

    @Column(name = "actual_started_at")
    private LocalDateTime actualStartedAt;

    @Column(name = "actual_finished_at")
    private LocalDateTime actualFinishedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public DeliveryGroup(
        LocalDate deliveryDate
        , DeliverySlot slot
    ) {
        this.deliveryDate = deliveryDate;
        this.slot = slot;
        this.status = DeliveryGroupStatus.WAITING_ASSIGNMENT;
    }

    public boolean isWaitingAutoAssignment() {
        return status == DeliveryGroupStatus.WAITING_ASSIGNMENT
            && autoAssignmentCompletedAt == null;
    }

    public void completeAutoAssignment(LocalDateTime completedAt) {
        this.status = DeliveryGroupStatus.WAITING_RIDER;
        this.autoAssignmentCompletedAt = completedAt;
    }

    public void completeManualAssignment() {
        this.status = DeliveryGroupStatus.WAITING_RIDER;
    }

    public boolean isWaitingRider() {
        return status == DeliveryGroupStatus.WAITING_RIDER;
    }

    public void returnToWaitingRider() {
        this.status = DeliveryGroupStatus.WAITING_RIDER;
    }

    public void readyToConfirm() {
        this.status = DeliveryGroupStatus.READY_TO_CONFIRM;
    }

    public void confirm() {
        if (status != DeliveryGroupStatus.READY_TO_CONFIRM) {
            throw new IllegalStateException("Only ready delivery groups can be confirmed");
        }

        this.status = DeliveryGroupStatus.CONFIRMED;
    }

    public boolean isIssueReview() {
        return status == DeliveryGroupStatus.ISSUE_REVIEW;
    }

    public void issueReview() {
        this.status = DeliveryGroupStatus.ISSUE_REVIEW;
    }
}
