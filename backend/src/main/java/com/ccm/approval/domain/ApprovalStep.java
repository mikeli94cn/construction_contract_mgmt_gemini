package com.ccm.approval.domain;

import com.ccm.iam.domain.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "approval_steps")
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private ApprovalWorkflow workflow;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "step_name", nullable = false, length = 120)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_role", nullable = false, length = 60)
    private RoleName requiredRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ApprovalStepStatus status;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(name = "comments", length = 1000)
    private String comments;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    protected ApprovalStep() {
    }

    public ApprovalStep(
            int stepOrder,
            String stepName,
            RoleName requiredRole,
            ApprovalStepStatus status
    ) {
        this.stepOrder = stepOrder;
        this.stepName = stepName;
        this.requiredRole = requiredRole;
        this.status = status;
    }

    void assignWorkflow(ApprovalWorkflow workflow) {
        this.workflow = workflow;
    }

    void activateStep() {
        this.status = ApprovalStepStatus.PENDING;
    }

    void markApproved(String actorUsername, String comments) {
        this.status = ApprovalStepStatus.APPROVED;
        this.actorUsername = actorUsername;
        this.comments = comments;
        this.decidedAt = Instant.now();
    }

    void markRejected(String actorUsername, String comments) {
        this.status = ApprovalStepStatus.REJECTED;
        this.actorUsername = actorUsername;
        this.comments = comments;
        this.decidedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ApprovalWorkflow getWorkflow() {
        return workflow;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public String getStepName() {
        return stepName;
    }

    public RoleName getRequiredRole() {
        return requiredRole;
    }

    public ApprovalStepStatus getStatus() {
        return status;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getComments() {
        return comments;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
