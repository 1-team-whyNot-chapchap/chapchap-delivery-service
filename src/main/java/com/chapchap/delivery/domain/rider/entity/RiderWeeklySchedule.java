package com.chapchap.delivery.domain.rider.entity;

import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "rider_weekly_schedules"
    , uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_rider_weekly_schedules_rider_day_slot"
            , columnNames = {
                "rider_id"
                , "day_of_week"
                , "slot_id"
            }
        )
    }
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiderWeeklySchedule {
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

    @Column(name = "day_of_week", nullable = false)
    private Byte dayOfWeek;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "slot_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private DeliverySlot slot;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public RiderWeeklySchedule(
        Rider rider
        , Byte dayOfWeek
        , DeliverySlot slot
    ) {
        this.rider = rider;
        this.dayOfWeek = dayOfWeek;
        this.slot = slot;
    }

    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void restore() {
        this.deletedAt = null;
    }
}