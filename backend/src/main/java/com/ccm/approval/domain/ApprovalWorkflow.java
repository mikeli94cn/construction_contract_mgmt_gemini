package com.ccm.approval.domain;

import com.ccm.common.domain.MoneyUtils;
import com.ccm.common.exception.BusinessRuleViolationException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "approval_workflows")
public class ApprovalWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private ApprovalTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "reference_no", nullable = false, length = 80)
    private String referenceNo;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "amount_gross", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountGross;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ApprovalWorkflowStatus status;

    @Column(name = "current_step_order", nullable = false)
    private int currentStepOrder;

    @Column(name = "submitted_by", nullable = false, length = 100)
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<ApprovalStep> steps = new ArrayList<>();

    protected ApprovalWorkflow() {
    }

    public ApprovalWorkflow(
            ApprovalTargetType targetType,
            Long targetId,
            Long projectId,
            String referenceNo,
            String title,
            BigDecimal amountGross,
            String submittedBy,
            List<ApprovalStep> orderedSteps
    ) {
        if (orderedSteps == null || orderedSteps.isEmpty()) {
            throw new BusinessRuleViolationException("Approval workflow must have at least one step");
        }
        this.targetType = targetType;
        this.targetId = targetId;
        this.projectId = projectId;
        this.referenceNo = referenceNo;
        this.title = title;
        this.amountGross = MoneyUtils.scaleMoney(amountGross);
        this.status = ApprovalWorkflowStatus.PENDING;
        this.currentStepOrder = 1;
        this.submittedBy = submittedBy;
        this.submittedAt = Instant.now();

        for (ApprovalStep step : orderedSteps) {
            step.assignWorkflow(this);
            this.steps.add(step);
        }
    }

    public ApprovalStep getActiveStep() {
        return steps.stream()
                .filter(s -> s.getStepOrder() == this.currentStepOrder)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("No active step found for order " + this.currentStepOrder));
    }

    public void applyDecision(
            ApprovalDecisionAction action,
            String actorUsername,
            Set<String> actorRoles,
            String comments
    ) {
        if (this.status != ApprovalWorkflowStatus.PENDING) {
            throw new BusinessRuleViolationException("Approval workflow is already completed with status " + this.status);
        }

        ApprovalStep activeStep = getActiveStep();
        String requiredRoleName = activeStep.getRequiredRole().name();
        boolean authorizedByRole = actorRoles.contains(requiredRoleName) || actorRoles.contains("ROLE_SYSTEM_ADMIN");
        if (!authorizedByRole) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Current step '" + activeStep.getStepName() + "' requires role " + requiredRoleName
            );
        }

        if (action == ApprovalDecisionAction.REJECT) {
            if (comments == null || comments.isBlank()) {
                throw new BusinessRuleViolationException("Comments are mandatory when rejecting an approval step");
            }
            activeStep.markRejected(actorUsername, comments.trim());
            this.status = ApprovalWorkflowStatus.REJECTED;
            this.completedAt = Instant.now();
            return;
        }

        // APPROVE
        activeStep.markApproved(actorUsername, comments == null ? null : comments.trim());

        int nextOrder = this.currentStepOrder + 1;
        ApprovalStep nextStep = steps.stream()
                .filter(s -> s.getStepOrder() == nextOrder)
                .findFirst()
                .orElse(null);

        if (nextStep != null) {
            this.currentStepOrder = nextOrder;
            nextStep.activateStep();
        } else {
            this.status = ApprovalWorkflowStatus.APPROVED;
            this.completedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public ApprovalTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getAmountGross() {
        return amountGross;
    }

    public ApprovalWorkflowStatus getStatus() {
        return status;
    }

    public int getCurrentStepOrder() {
        return currentStepOrder;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Long getVersion() {
        return version;
    }

    public List<ApprovalStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }
}
