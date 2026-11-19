package com.ccm.approval.api;

import com.ccm.approval.api.ApprovalDtos.ApprovalDecisionRequest;
import com.ccm.approval.api.ApprovalDtos.ApprovalWorkflowResponse;
import com.ccm.approval.application.ApprovalApplicationService;
import com.ccm.approval.domain.ApprovalTargetType;
import com.ccm.approval.domain.ApprovalWorkflowStatus;
import com.ccm.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approvals")
@Tag(name = "Approvals", description = "Multi-step approval workflow inbox and step decision endpoints")
public class ApprovalController {

    private final ApprovalApplicationService approvalApplicationService;

    public ApprovalController(ApprovalApplicationService approvalApplicationService) {
        this.approvalApplicationService = approvalApplicationService;
    }

    @GetMapping
    @Operation(summary = "List approval workflows filtered by project, target type, or status")
    public PageResponse<ApprovalWorkflowResponse> listWorkflows(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) ApprovalTargetType targetType,
            @RequestParam(required = false) ApprovalWorkflowStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return approvalApplicationService.listWorkflows(
                projectId,
                targetType,
                status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"))
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get approval workflow details and step timeline")
    public ApprovalWorkflowResponse getWorkflow(@PathVariable Long id) {
        return approvalApplicationService.getWorkflow(id);
    }

    @GetMapping("/by-target")
    @Operation(summary = "List approval workflow history for a specific target entity")
    public List<ApprovalWorkflowResponse> listByTarget(
            @RequestParam ApprovalTargetType targetType,
            @RequestParam Long targetId
    ) {
        return approvalApplicationService.listTargetWorkflows(targetType, targetId);
    }

    @PostMapping("/{id}/decision")
    @Operation(summary = "Approve or reject the currently active step of an approval workflow")
    public ApprovalWorkflowResponse makeDecision(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalDecisionRequest request
    ) {
        return approvalApplicationService.makeDecision(id, request);
    }
}
