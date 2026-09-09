package com.chapchap.delivery.domain.assignment.entity;

import com.chapchap.delivery.domain.delivery.entity.Delivery;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "delivery_assignment_items"
    , uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_delivery_assignment_items_assignment_delivery"
            , columnNames = {
                "assignment_id"
                , "delivery_id"
            }
        )
    }
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAssignmentItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
        name = "assignment_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private DeliveryAssignment assignment;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
        name = "delivery_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private Delivery delivery;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public DeliveryAssignmentItem(
        DeliveryAssignment assignment
        , Delivery delivery
    ) {
        this.assignment = assignment;
        this.delivery = delivery;
    }
}
