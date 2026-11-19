package com.ccm.approval.application;

import com.ccm.approval.api.ApprovalDtos.ApprovalDecisionRequest;
import com.ccm.approval.api.ApprovalDtos.ApprovalStepResponse;
import com.ccm.approval.api.ApprovalDtos.ApprovalWorkflowResponse;
import com.ccm.approval.domain.ApprovalStep;
import com.ccm.approval.domain.ApprovalStepStatus;
import com.ccm.approval.domain.ApprovalTargetType;
import com.ccm.approval.domain.ApprovalWorkflow;
import com.ccm.approval.domain.ApprovalWorkflowCompletedEvent;
import com.ccm.approval.domain.ApprovalWorkflowStatus;
import com.ccm.approval.infrastructure.ApprovalWorkflowRepository;
import com.ccm.audit.application.AuditTrailService;
import com.ccm.common.api.PageResponse;
import com.ccm.common.exception.BusinessRuleViolationException;
import com.ccm.common.exception.ResourceNotFoundException;
import com.ccm.common.security.SecurityContextUtils;
import com.ccm.iam.domain.RoleName;
import com.ccm.project.application.ProjectAccessGuard;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalApplicationService {

    private static final BigDecimal CONTRACT_EXEC_THRESHOLD = new BigDecimal("5000000.0000");
    private static final BigDecimal CHANGE_ORDER_EXEC_THRESHOLD = new BigDecimal("500000.0000");

    private final ApprovalWorkflowRepository workflowRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final AuditTrailService auditTrailService;
    private final ApplicationEventPublisher eventPublisher;

    public ApprovalApplicationService(
            ApprovalWorkflowRepository workflowRepository,
            ProjectAccessGuard projectAccessGuard,
            AuditTrailService auditTrailService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.workflowRepository = workflowRepository;
        this.projectAccessGuard = projectAccessGuard;
        this.auditTrailService = auditTrailService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ApprovalWorkflowResponse startWorkflow(
            ApprovalTargetType targetType,
            Long targetId,
            Long projectId,
            String referenceNo,
            String title,
            BigDecimal amountGross
    ) {
        if (workflowRepository.existsByTargetTypeAndTargetIdAndStatus(
                targetType, targetId, ApprovalWorkflowStatus.PENDING
        )) {
            throw new BusinessRuleViolationException(
                    "An active pending approval workflow already exists for " + targetType + " #" + referenceNo
            );
        }

        List<ApprovalStep> steps = buildRuleBasedSteps(targetType, amountGross);
        String actor = SecurityContextUtils.getCurrentUsername();

        ApprovalWorkflow workflow = new ApprovalWorkflow(
                targetType,
                targetId,
                projectId,
                referenceNo,
                title,
                amountGross,
                actor,
                steps
        );
        ApprovalWorkflow saved = workflowRepository.saveAndFlush(workflow);

        auditTrailService.record(
                "APPROVAL",
                "ApprovalWorkflow",
                saved.getId(),
                "WORKFLOW_STARTED",
                "Started " + targetType + " approval workflow for " + referenceNo + " (" + steps.size() + " steps)",
                Map.of(
                        "targetType", targetType.name(),
                        "targetId", targetId,
                        "referenceNo", referenceNo,
                        "amountGross", amountGross,
                        "stepCount", steps.size()
                )
        );

        return toResponse(saved);
    }

    @Transactional
    public ApprovalWorkflowResponse makeDecision(Long workflowId, ApprovalDecisionRequest request) {
        ApprovalWorkflow workflow = workflowRepository.findWithStepsById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval workflow not found: " + workflowId));

        projectAccessGuard.assertCanViewProject(workflow.getProjectId());

        String actor = SecurityContextUtils.getCurrentUsername();
        Set<String> actorRoles = SecurityContextUtils.getCurrentRoles();
        int stepOrderBefore = workflow.getCurrentStepOrder();

        workflow.applyDecision(request.action(), actor, actorRoles, request.comments());
        ApprovalWorkflow saved = workflowRepository.saveAndFlush(workflow);

        auditTrailService.record(
                "APPROVAL",
                "ApprovalWorkflow",
                saved.getId(),
                "STEP_" + request.action().name() + "D",
                "Step #" + stepOrderBefore + " " + request.action().name() + "D on "
                        + saved.getTargetType() + " " + saved.getReferenceNo()
                        + " (workflowStatus=" + saved.getStatus() + ")",
                Map.of(
                        "workflowId", saved.getId(),
                        "targetType", saved.getTargetType().name(),
                        "targetId", saved.getTargetId(),
                        "referenceNo", saved.getReferenceNo(),
                        "stepOrder", stepOrderBefore,
                        "decision", request.action().name(),
                        "workflowStatus", saved.getStatus().name()
                )
        );

        if (saved.getStatus() == ApprovalWorkflowStatus.APPROVED
                || saved.getStatus() == ApprovalWorkflowStatus.REJECTED) {
            eventPublisher.publishEvent(new ApprovalWorkflowCompletedEvent(
                    saved.getId(),
                    saved.getTargetType(),
                    saved.getTargetId(),
                    saved.getStatus(),
                    actor,
                    request.comments()
            ));
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApprovalWorkflowResponse> listWorkflows(
            Long projectId,
            ApprovalTargetType targetType,
            ApprovalWorkflowStatus status,
            Pageable pageable
    ) {
        if (projectId != null) {
            projectAccessGuard.assertCanViewProject(projectId);
        }
        Page<ApprovalWorkflow> page;
        if (projectAccessGuard.hasGlobalReadAccess()) {
            page = workflowRepository.searchAllWorkflows(projectId, targetType, status, pageable);
        } else {
            Long currentUserId = projectAccessGuard.resolveCurrentUserId();
            if (currentUserId == null) {
                page = Page.empty(pageable);
            } else {
                page = workflowRepository.searchMemberWorkflows(
                        currentUserId, projectId, targetType, status, pageable
                );
            }
        }
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ApprovalWorkflowResponse getWorkflow(Long workflowId) {
        ApprovalWorkflow workflow = workflowRepository.findWithStepsById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval workflow not found: " + workflowId));
        projectAccessGuard.assertCanViewProject(workflow.getProjectId());
        return toResponse(workflow);
    }

    @Transactional(readOnly = true)
    public List<ApprovalWorkflowResponse> listTargetWorkflows(ApprovalTargetType targetType, Long targetId) {
        List<ApprovalWorkflow> list = workflowRepository.findByTargetTypeAndTargetIdOrderBySubmittedAtDesc(targetType, targetId);
        if (!list.isEmpty()) {
            projectAccessGuard.assertCanViewProject(list.get(0).getProjectId());
        }
        return list.stream().map(this::toResponse).toList();
    }

    private List<ApprovalStep> buildRuleBasedSteps(ApprovalTargetType targetType, BigDecimal amountGross) {
        List<ApprovalStep> steps = new ArrayList<>();
        BigDecimal absAmount = amountGross == null ? BigDecimal.ZERO : amountGross.abs();

        switch (targetType) {
            case CONTRACT -> {
                steps.add(new ApprovalStep(1, "Cost Engineer Commercial & BoQ Review",
                        RoleName.ROLE_COST_ENGINEER, ApprovalStepStatus.PENDING));
                steps.add(new ApprovalStep(2, "Project Manager Endorsement",
                        RoleName.ROLE_PROJECT_MANAGER, ApprovalStepStatus.WAITING));
                if (absAmount.compareTo(CONTRACT_EXEC_THRESHOLD) >= 0) {
                    steps.add(new ApprovalStep(3, "Executive Committee High-Value Contract Sign-Off",
                            RoleName.ROLE_EXECUTIVE_APPROVER, ApprovalStepStatus.WAITING));
                }
            }
            case CHANGE_ORDER -> {
                steps.add(new ApprovalStep(1, "Cost Engineer Variation Valuation",
                        RoleName.ROLE_COST_ENGINEER, ApprovalStepStatus.PENDING));
                steps.add(new ApprovalStep(2, "Project Manager Change Order Approval",
                        RoleName.ROLE_PROJECT_MANAGER, ApprovalStepStatus.WAITING));
                if (absAmount.compareTo(CHANGE_ORDER_EXEC_THRESHOLD) >= 0) {
                    steps.add(new ApprovalStep(3, "Executive Approver High-Value Variation Authorization",
                            RoleName.ROLE_EXECUTIVE_APPROVER, ApprovalStepStatus.WAITING));
                }
            }
            case PAYMENT_APPLICATION -> {
                steps.add(new ApprovalStep(1, "Cost Engineer Interim Measurement Certification",
                        RoleName.ROLE_COST_ENGINEER, ApprovalStepStatus.PENDING));
                steps.add(new ApprovalStep(2, "Finance Officer Payment & Retainage Verification",
                        RoleName.ROLE_FINANCE_OFFICER, ApprovalStepStatus.WAITING));
            }
            case FINAL_SETTLEMENT -> {
                steps.add(new ApprovalStep(1, "Cost Engineer Final Account Reconciliation",
                        RoleName.ROLE_COST_ENGINEER, ApprovalStepStatus.PENDING));
                steps.add(new ApprovalStep(2, "Finance Officer Final Balance & Retainage Clearance",
                        RoleName.ROLE_FINANCE_OFFICER, ApprovalStepStatus.WAITING));
                steps.add(new ApprovalStep(3, "Executive Approver Final Settlement Sign-Off",
                        RoleName.ROLE_EXECUTIVE_APPROVER, ApprovalStepStatus.WAITING));
            }
        }
        return steps;
    }

    public ApprovalWorkflowResponse toResponse(ApprovalWorkflow w) {
        List<ApprovalStepResponse> stepDtos = w.getSteps().stream()
                .map(s -> new ApprovalStepResponse(
                        s.getId(),
                        s.getStepOrder(),
                        s.getStepName(),
                        s.getRequiredRole().name(),
                        s.getStatus(),
                        s.getActorUsername(),
                        s.getComments(),
                        s.getDecidedAt()
                ))
                .toList();

        return new ApprovalWorkflowResponse(
                w.getId(),
                w.getTargetType(),
                w.getTargetId(),
                w.getProjectId(),
                w.getReferenceNo(),
                w.getTitle(),
                w.getAmountGross(),
                w.getStatus(),
                w.getCurrentStepOrder(),
                w.getSubmittedBy(),
                w.getSubmittedAt(),
                w.getCompletedAt(),
                w.getVersion(),
                stepDtos
        );
    }
}
