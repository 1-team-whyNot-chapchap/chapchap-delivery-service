package com.chapchap.delivery.domain.assignment.entity;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueCode;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueResolution;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_assignment_issues")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAssignmentIssue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "assignment_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private DeliveryAssignment assignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_code", nullable = false, length = 32)
    private DeliveryAssignmentIssueCode issueCode;

    @Column(name = "issue_detail", length = 500)
    private String issueDetail;

    @Column(name = "reported_by", nullable = false)
    private Long reportedBy;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private DeliveryAssignmentIssueResolution resolution;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public DeliveryAssignmentIssue(
        DeliveryAssignment assignment
        , DeliveryAssignmentIssueCode issueCode
        , String issueDetail
        , Long reportedBy
        , LocalDateTime reportedAt
    ) {
        this.assignment = assignment;
        this.issueCode = issueCode;
        this.issueDetail = issueDetail;
        this.reportedBy = reportedBy;
        this.reportedAt = reportedAt;
    }
}