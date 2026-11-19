package com.ccm.approval.api;

import com.ccm.approval.domain.ApprovalDecisionAction;
import com.ccm.approval.domain.ApprovalStepStatus;
import com.ccm.approval.domain.ApprovalTargetType;
import com.ccm.approval.domain.ApprovalWorkflowStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApprovalDtos {

    private ApprovalDtos() {
    }

    public record ApprovalDecisionRequest(
            @NotNull(message = "Decision action (APPROVE or REJECT) is required")
            ApprovalDecisionAction action,

            @Size(max = 1000)
            String comments
    ) {
    }

    public record ApprovalStepResponse(
            Long id,
            int stepOrder,
            String stepName,
            String requiredRole,
            ApprovalStepStatus status,
            String actorUsername,
            String comments,
            Instant decidedAt
    ) {
    }

    public record ApprovalWorkflowResponse(
            Long id,
            ApprovalTargetType targetType,
            Long targetId,
            Long projectId,
            String referenceNo,
            String title,
            BigDecimal amountGross,
            ApprovalWorkflowStatus status,
            int currentStepOrder,
            String submittedBy,
            Instant submittedAt,
            Instant completedAt,
            Long version,
            List<ApprovalStepResponse> steps
    ) {
    }
}
