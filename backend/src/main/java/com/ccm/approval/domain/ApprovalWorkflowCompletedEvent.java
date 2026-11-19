package com.ccm.approval.domain;

public record ApprovalWorkflowCompletedEvent(
        Long workflowId,
        ApprovalTargetType targetType,
        Long targetId,
        ApprovalWorkflowStatus finalStatus,
        String actorUsername,
        String comments
) {
}
