package com.ccm.changeorder.application;

import com.ccm.approval.application.ApprovalApplicationService;
import com.ccm.approval.domain.ApprovalTargetType;
import com.ccm.approval.domain.ApprovalWorkflowCompletedEvent;
import com.ccm.approval.domain.ApprovalWorkflowStatus;
import com.ccm.audit.application.AuditTrailService;
import com.ccm.changeorder.api.ChangeOrderDtos.ChangeOrderResponse;
import com.ccm.changeorder.api.ChangeOrderDtos.CreateChangeOrderRequest;
import com.ccm.changeorder.domain.ChangeOrder;
import com.ccm.changeorder.infrastructure.ChangeOrderRepository;
import com.ccm.common.exception.DuplicateResourceException;
import com.ccm.common.exception.ResourceNotFoundException;
import com.ccm.common.security.SecurityContextUtils;
import com.ccm.contract.domain.Contract;
import com.ccm.contract.infrastructure.ContractRepository;
import com.ccm.project.application.ProjectAccessGuard;
import java.util.List;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeOrderApplicationService {

    private final ChangeOrderRepository changeOrderRepository;
    private final ContractRepository contractRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final ApprovalApplicationService approvalApplicationService;
    private final AuditTrailService auditTrailService;

    public ChangeOrderApplicationService(
            ChangeOrderRepository changeOrderRepository,
            ContractRepository contractRepository,
            ProjectAccessGuard projectAccessGuard,
            ApprovalApplicationService approvalApplicationService,
            AuditTrailService auditTrailService
    ) {
        this.changeOrderRepository = changeOrderRepository;
        this.contractRepository = contractRepository;
        this.projectAccessGuard = projectAccessGuard;
        this.approvalApplicationService = approvalApplicationService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public ChangeOrderResponse createChangeOrder(Long contractId, CreateChangeOrderRequest request) {
        Contract contract = contractRepository.findWithDetailsById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));
        projectAccessGuard.assertCanManageProjectContracts(contract.getProject().getId());

        String coNo = request.changeOrderNo().trim().toUpperCase();
        if (changeOrderRepository.existsByChangeOrderNo(coNo)) {
            throw new DuplicateResourceException("Change order number already exists: " + coNo);
        }

        ChangeOrder changeOrder = new ChangeOrder(
                contract,
                coNo,
                request.changeType(),
                request.title(),
                request.reason(),
                request.amountDeltaNet(),
                request.scheduleExtensionDays(),
                SecurityContextUtils.getCurrentUsername()
        );
        ChangeOrder saved = changeOrderRepository.saveAndFlush(changeOrder);

        auditTrailService.record(
                "CHANGE_ORDER",
                "ChangeOrder",
                saved.getId(),
                "CHANGE_ORDER_CREATED",
                "Created change order " + saved.getChangeOrderNo() + " on contract " + contract.getContractNo()
                        + " (grossDelta=" + saved.getAmountDeltaGross() + ")",
                Map.of(
                        "changeOrderNo", saved.getChangeOrderNo(),
                        "contractId", contract.getId(),
                        "changeType", saved.getChangeType().name(),
                        "amountDeltaGross", saved.getAmountDeltaGross()
                )
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ChangeOrderResponse> listContractChangeOrders(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));
        projectAccessGuard.assertCanViewProject(contract.getProject().getId());
        return changeOrderRepository.findByContractIdOrderByCreatedAtDesc(contractId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ChangeOrderResponse submitForApproval(Long changeOrderId) {
        ChangeOrder co = changeOrderRepository.findWithContractById(changeOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Change order not found: " + changeOrderId));
        projectAccessGuard.assertCanManageProjectContracts(co.getContract().getProject().getId());

        co.submitForApproval();
        ChangeOrder saved = changeOrderRepository.saveAndFlush(co);

        approvalApplicationService.startWorkflow(
                ApprovalTargetType.CHANGE_ORDER,
                saved.getId(),
                saved.getContract().getProject().getId(),
                saved.getChangeOrderNo(),
                saved.getTitle(),
                saved.getAmountDeltaGross()
        );

        auditTrailService.record(
                "CHANGE_ORDER",
                "ChangeOrder",
                saved.getId(),
                "CHANGE_ORDER_SUBMITTED_FOR_APPROVAL",
                "Submitted change order " + saved.getChangeOrderNo() + " for approval",
                Map.of("changeOrderNo", saved.getChangeOrderNo(), "status", saved.getStatus().name())
        );

        return toResponse(saved);
    }

    @EventListener
    @Transactional
    public void onApprovalWorkflowCompleted(ApprovalWorkflowCompletedEvent event) {
        if (event.targetType() != ApprovalTargetType.CHANGE_ORDER) {
            return;
        }
        ChangeOrder co = changeOrderRepository.findWithContractById(event.targetId())
                .orElseThrow(() -> new ResourceNotFoundException("Change order not found: " + event.targetId()));
        Contract contract = co.getContract();

        if (event.finalStatus() == ApprovalWorkflowStatus.APPROVED) {
            co.markApproved();
            contract.applyApprovedChangeOrderDelta(co.getAmountDeltaNet(), co.getAmountDeltaGross());
            changeOrderRepository.saveAndFlush(co);
            contractRepository.saveAndFlush(contract);

            auditTrailService.record(
                    "CHANGE_ORDER",
                    "ChangeOrder",
                    co.getId(),
                    "CHANGE_ORDER_APPROVED",
                    "Approved change order " + co.getChangeOrderNo()
                            + "; updated contract " + contract.getContractNo()
                            + " current gross value to " + contract.getCurrentAmountGross(),
                    Map.of(
                            "changeOrderNo", co.getChangeOrderNo(),
                            "contractId", contract.getId(),
                            "amountDeltaGross", co.getAmountDeltaGross(),
                            "newContractCurrentGross", contract.getCurrentAmountGross()
                    )
            );
        } else if (event.finalStatus() == ApprovalWorkflowStatus.REJECTED) {
            co.markRejected();
            changeOrderRepository.saveAndFlush(co);

            auditTrailService.record(
                    "CHANGE_ORDER",
                    "ChangeOrder",
                    co.getId(),
                    "CHANGE_ORDER_REJECTED",
                    "Rejected change order " + co.getChangeOrderNo(),
                    Map.of("changeOrderNo", co.getChangeOrderNo())
            );
        }
    }

    private ChangeOrderResponse toResponse(ChangeOrder co) {
        return new ChangeOrderResponse(
                co.getId(),
                co.getContract().getId(),
                co.getContract().getContractNo(),
                co.getContract().getProject().getId(),
                co.getChangeOrderNo(),
                co.getChangeType(),
                co.getTitle(),
                co.getReason(),
                co.getAmountDeltaNet(),
                co.getTaxRate(),
                co.getAmountDeltaTax(),
                co.getAmountDeltaGross(),
                co.getScheduleExtensionDays(),
                co.getStatus(),
                co.getApprovedAt(),
                co.getVersion(),
                co.getCreatedAt(),
                co.getCreatedBy()
        );
    }
}
