package com.ccm.settlement.application;

import com.ccm.approval.application.ApprovalApplicationService;
import com.ccm.approval.domain.ApprovalTargetType;
import com.ccm.approval.domain.ApprovalWorkflowCompletedEvent;
import com.ccm.approval.domain.ApprovalWorkflowStatus;
import com.ccm.audit.application.AuditTrailService;
import com.ccm.changeorder.domain.ChangeOrderStatus;
import com.ccm.changeorder.infrastructure.ChangeOrderRepository;
import com.ccm.common.domain.MoneyUtils;
import com.ccm.common.exception.BusinessRuleViolationException;
import com.ccm.common.exception.DuplicateResourceException;
import com.ccm.common.exception.ResourceNotFoundException;
import com.ccm.common.security.SecurityContextUtils;
import com.ccm.contract.domain.Contract;
import com.ccm.contract.infrastructure.ContractRepository;
import com.ccm.payment.domain.PaymentApplicationStatus;
import com.ccm.payment.infrastructure.PaymentApplicationRepository;
import com.ccm.project.application.ProjectAccessGuard;
import com.ccm.settlement.api.SettlementDtos.CreateFinalSettlementRequest;
import com.ccm.settlement.api.SettlementDtos.FinalSettlementResponse;
import com.ccm.settlement.domain.FinalSettlement;
import com.ccm.settlement.infrastructure.FinalSettlementRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalSettlementApplicationService {

    private final FinalSettlementRepository settlementRepository;
    private final ContractRepository contractRepository;
    private final ChangeOrderRepository changeOrderRepository;
    private final PaymentApplicationRepository paymentRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final ApprovalApplicationService approvalApplicationService;
    private final AuditTrailService auditTrailService;

    public FinalSettlementApplicationService(
            FinalSettlementRepository settlementRepository,
            ContractRepository contractRepository,
            ChangeOrderRepository changeOrderRepository,
            PaymentApplicationRepository paymentRepository,
            ProjectAccessGuard projectAccessGuard,
            ApprovalApplicationService approvalApplicationService,
            AuditTrailService auditTrailService
    ) {
        this.settlementRepository = settlementRepository;
        this.contractRepository = contractRepository;
        this.changeOrderRepository = changeOrderRepository;
        this.paymentRepository = paymentRepository;
        this.projectAccessGuard = projectAccessGuard;
        this.approvalApplicationService = approvalApplicationService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public FinalSettlementResponse createFinalSettlement(
            Long contractId,
            CreateFinalSettlementRequest request
    ) {
        Contract contract = contractRepository.findWithDetailsById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));
        projectAccessGuard.assertCanViewProject(contract.getProject().getId());

        if (settlementRepository.existsByContractId(contractId)) {
            throw new DuplicateResourceException("A final settlement record already exists for contract " + contract.getContractNo());
        }
        String settlementNo = request.settlementNo().trim().toUpperCase();
        if (settlementRepository.existsBySettlementNo(settlementNo)) {
            throw new DuplicateResourceException("Settlement number already exists: " + settlementNo);
        }

        // Guard: no pending DRAFT or IN_APPROVAL change orders or payment applications
        if (changeOrderRepository.existsByContractIdAndStatus(contractId, ChangeOrderStatus.DRAFT)
                || changeOrderRepository.existsByContractIdAndStatus(contractId, ChangeOrderStatus.IN_APPROVAL)) {
            throw new BusinessRuleViolationException(
                    "Cannot initiate final settlement while DRAFT or IN_APPROVAL change orders exist on contract "
                            + contract.getContractNo()
            );
        }
        if (paymentRepository.existsByContractIdAndStatusIn(
                contractId,
                Set.of(PaymentApplicationStatus.DRAFT, PaymentApplicationStatus.IN_APPROVAL)
        )) {
            throw new BusinessRuleViolationException(
                    "Cannot initiate final settlement while DRAFT or IN_APPROVAL payment applications exist on contract "
                            + contract.getContractNo()
            );
        }

        Set<PaymentApplicationStatus> certifiedStatuses = Set.of(
                PaymentApplicationStatus.APPROVED,
                PaymentApplicationStatus.PAID
        );
        BigDecimal totalPreviousCertifiedGross = MoneyUtils.scaleMoney(
                paymentRepository.sumCurrentPeriodGrossByContractAndStatuses(contractId, certifiedStatuses)
        );
        BigDecimal totalRetainageHeld = MoneyUtils.scaleMoney(
                paymentRepository.sumRetainageWithheldByContractAndStatuses(contractId, certifiedStatuses)
        );

        FinalSettlement settlement = new FinalSettlement(
                contract,
                settlementNo,
                request.finalMeasuredNet(),
                totalPreviousCertifiedGross,
                totalRetainageHeld,
                request.warrantyRetainageHeld(),
                request.deductionsAndClaims(),
                request.completionDate(),
                request.remarks(),
                SecurityContextUtils.getCurrentUsername()
        );
        FinalSettlement saved = settlementRepository.saveAndFlush(settlement);

        auditTrailService.record(
                "SETTLEMENT",
                "FinalSettlement",
                saved.getId(),
                "FINAL_SETTLEMENT_CREATED",
                "Created final settlement " + saved.getSettlementNo() + " for contract " + contract.getContractNo()
                        + " (finalGross=" + saved.getFinalContractGross()
                        + ", closingPayable=" + saved.getFinalClosingPayable() + ")",
                Map.of(
                        "settlementNo", saved.getSettlementNo(),
                        "contractId", contract.getId(),
                        "finalContractGross", saved.getFinalContractGross(),
                        "totalPreviousCertifiedGross", saved.getTotalPreviousCertifiedGross(),
                        "totalRetainageHeld", saved.getTotalRetainageHeld(),
                        "finalClosingPayable", saved.getFinalClosingPayable()
                )
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Optional<FinalSettlementResponse> getByContractId(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));
        projectAccessGuard.assertCanViewProject(contract.getProject().getId());
        return settlementRepository.findByContractId(contractId).map(this::toResponse);
    }

    @Transactional
    public FinalSettlementResponse submitForApproval(Long settlementId) {
        FinalSettlement settlement = settlementRepository.findWithContractById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Final settlement not found: " + settlementId));
        projectAccessGuard.assertCanViewProject(settlement.getContract().getProject().getId());

        settlement.submitForApproval();
        FinalSettlement saved = settlementRepository.saveAndFlush(settlement);

        approvalApplicationService.startWorkflow(
                ApprovalTargetType.FINAL_SETTLEMENT,
                saved.getId(),
                saved.getContract().getProject().getId(),
                saved.getSettlementNo(),
                "Final Settlement " + saved.getSettlementNo() + " (" + saved.getContract().getContractNo() + ")",
                saved.getFinalContractGross()
        );

        auditTrailService.record(
                "SETTLEMENT",
                "FinalSettlement",
                saved.getId(),
                "FINAL_SETTLEMENT_SUBMITTED_FOR_APPROVAL",
                "Submitted final settlement " + saved.getSettlementNo() + " for 3-step approval",
                Map.of("settlementNo", saved.getSettlementNo(), "status", saved.getStatus().name())
        );

        return toResponse(saved);
    }

    @Transactional
    public FinalSettlementResponse releaseWarrantyRetainage(Long settlementId) {
        FinalSettlement settlement = settlementRepository.findWithContractById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Final settlement not found: " + settlementId));
        Contract contract = settlement.getContract();
        projectAccessGuard.assertCanViewProject(contract.getProject().getId());

        BigDecimal releasedWarrantyAmount = settlement.getWarrantyRetainageHeld();
        settlement.releaseWarrantyRetainage();
        contract.markClosed();

        FinalSettlement saved = settlementRepository.saveAndFlush(settlement);
        contractRepository.saveAndFlush(contract);

        auditTrailService.record(
                "SETTLEMENT",
                "FinalSettlement",
                saved.getId(),
                "WARRANTY_RETAINAGE_RELEASED_AND_CONTRACT_CLOSED",
                "Released warranty retainage (" + releasedWarrantyAmount + ") on " + saved.getSettlementNo()
                        + " and closed contract " + contract.getContractNo(),
                Map.of(
                        "settlementNo", saved.getSettlementNo(),
                        "contractNo", contract.getContractNo(),
                        "releasedWarrantyAmount", releasedWarrantyAmount,
                        "contractStatus", contract.getStatus().name()
                )
        );

        return toResponse(saved);
    }

    @EventListener
    @Transactional
    public void onApprovalWorkflowCompleted(ApprovalWorkflowCompletedEvent event) {
        if (event.targetType() != ApprovalTargetType.FINAL_SETTLEMENT) {
            return;
        }
        FinalSettlement settlement = settlementRepository.findWithContractById(event.targetId())
                .orElseThrow(() -> new ResourceNotFoundException("Final settlement not found: " + event.targetId()));
        Contract contract = settlement.getContract();

        if (event.finalStatus() == ApprovalWorkflowStatus.APPROVED) {
            settlement.markApproved();
            contract.markSettled();
            settlementRepository.saveAndFlush(settlement);
            contractRepository.saveAndFlush(contract);

            auditTrailService.record(
                    "SETTLEMENT",
                    "FinalSettlement",
                    settlement.getId(),
                    "FINAL_SETTLEMENT_APPROVED_AND_CONTRACT_SETTLED",
                    "Approved final settlement " + settlement.getSettlementNo()
                            + " and locked contract " + contract.getContractNo() + " in SETTLED status",
                    Map.of(
                            "settlementNo", settlement.getSettlementNo(),
                            "contractNo", contract.getContractNo(),
                            "finalContractGross", settlement.getFinalContractGross(),
                            "finalClosingPayable", settlement.getFinalClosingPayable()
                    )
            );
        } else if (event.finalStatus() == ApprovalWorkflowStatus.REJECTED) {
            settlement.markRejected();
            settlementRepository.saveAndFlush(settlement);

            auditTrailService.record(
                    "SETTLEMENT",
                    "FinalSettlement",
                    settlement.getId(),
                    "FINAL_SETTLEMENT_REJECTED",
                    "Rejected final settlement " + settlement.getSettlementNo(),
                    Map.of("settlementNo", settlement.getSettlementNo())
            );
        }
    }

    private FinalSettlementResponse toResponse(FinalSettlement s) {
        return new FinalSettlementResponse(
                s.getId(),
                s.getContract().getId(),
                s.getContract().getContractNo(),
                s.getContract().getProject().getId(),
                s.getSettlementNo(),
                s.getFinalMeasuredNet(),
                s.getTaxRate(),
                s.getFinalTaxAmount(),
                s.getFinalContractGross(),
                s.getTotalPreviousCertifiedGross(),
                s.getTotalRetainageHeld(),
                s.getRetainageReleasedNow(),
                s.getWarrantyRetainageHeld(),
                s.getDeductionsAndClaims(),
                s.getFinalClosingPayable(),
                s.getCompletionDate(),
                s.getWarrantyExpiryDate(),
                s.getStatus(),
                s.getApprovedAt(),
                s.getWarrantyReleasedAt(),
                s.getRemarks(),
                s.getVersion(),
                s.getCreatedAt(),
                s.getCreatedBy()
        );
    }
}
