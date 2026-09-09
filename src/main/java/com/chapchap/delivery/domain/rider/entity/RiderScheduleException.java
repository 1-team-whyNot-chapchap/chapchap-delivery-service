package com.chapchap.delivery.domain.rider.entity;

import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.rider.constant.RiderScheduleExceptionReason;
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
    name = "rider_schedule_exceptions"
    , uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_rider_schedule_exceptions_rider_date_slot"
            , columnNames = {
                "rider_id"
                , "schedule_date"
                , "slot_id"
            }
        )
    }
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiderScheduleException {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "rider_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private Rider rider;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "slot_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private DeliverySlot slot;

    @Column(name = "is_working", nullable = false)
    private Boolean isWorking;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 32)
    private RiderScheduleExceptionReason reasonCode;

    @Column(name = "reason_detail", length = 255)
    private String reasonDetail;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public RiderScheduleException(
        Rider rider
        , LocalDate scheduleDate
        , DeliverySlot slot
        , Boolean isWorking
        , RiderScheduleExceptionReason reasonCode
        , String reasonDetail
        , Long createdBy
    ) {
        this.rider = rider;
        this.scheduleDate = scheduleDate;
        this.slot = slot;
        this.isWorking = isWorking;
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
        this.createdBy = createdBy;
    }

    public void change(
        Boolean isWorking
        , RiderScheduleExceptionReason reasonCode
        , String reasonDetail
    ) {
        this.isWorking = isWorking;
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
    }

    public void delete(
        LocalDateTime deletedAt
    ) {
        this.deletedAt = deletedAt;
    }

    public void restore(
        Boolean isWorking
        , RiderScheduleExceptionReason reasonCode
        , String reasonDetail
    ) {
        this.isWorking = isWorking;
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
        this.deletedAt = null;
    }
}