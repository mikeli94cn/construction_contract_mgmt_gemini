package com.ccm.payment.application;

import com.ccm.approval.application.ApprovalApplicationService;
import com.ccm.approval.domain.ApprovalTargetType;
import com.ccm.approval.domain.ApprovalWorkflowCompletedEvent;
import com.ccm.approval.domain.ApprovalWorkflowStatus;
import com.ccm.audit.application.AuditTrailService;
import com.ccm.common.domain.MoneyUtils;
import com.ccm.common.exception.BusinessRuleViolationException;
import com.ccm.common.exception.DuplicateResourceException;
import com.ccm.common.exception.ResourceNotFoundException;
import com.ccm.common.security.SecurityContextUtils;
import com.ccm.contract.domain.Contract;
import com.ccm.contract.infrastructure.ContractRepository;
import com.ccm.payment.api.PaymentDtos.CreatePaymentApplicationRequest;
import com.ccm.payment.api.PaymentDtos.MarkPaymentPaidRequest;
import com.ccm.payment.api.PaymentDtos.PaymentApplicationResponse;
import com.ccm.payment.domain.PaymentApplication;
import com.ccm.payment.domain.PaymentApplicationStatus;
import com.ccm.payment.infrastructure.PaymentApplicationRepository;
import com.ccm.project.application.ProjectAccessGuard;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentApplicationService {

    private final PaymentApplicationRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final ApprovalApplicationService approvalApplicationService;
    private final AuditTrailService auditTrailService;

    public PaymentApplicationService(
            PaymentApplicationRepository paymentRepository,
            ContractRepository contractRepository,
            ProjectAccessGuard projectAccessGuard,
            ApprovalApplicationService approvalApplicationService,
            AuditTrailService auditTrailService
    ) {
        this.paymentRepository = paymentRepository;
        this.contractRepository = contractRepository;
        this.projectAccessGuard = projectAccessGuard;
        this.approvalApplicationService = approvalApplicationService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public PaymentApplicationResponse createPaymentApplication(
            Long contractId,
            CreatePaymentApplicationRequest request
    ) {
        Contract contract = contractRepository.findWithDetailsById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));
        projectAccessGuard.assertCanViewProject(contract.getProject().getId());

        String appNo = request.applicationNo().trim().toUpperCase();
        if (paymentRepository.existsByApplicationNo(appNo)) {
            throw new DuplicateResourceException("Payment application number already exists: " + appNo);
        }

        if (paymentRepository.existsByContractIdAndStatusIn(
                contractId,
                Set.of(PaymentApplicationStatus.DRAFT, PaymentApplicationStatus.IN_APPROVAL)
        )) {
            throw new BusinessRuleViolationException(
                    "An open DRAFT or IN_APPROVAL payment application already exists for contract "
                            + contract.getContractNo() + ". Complete it before creating the next period application."
            );
        }

        BigDecimal previousCertifiedGross = MoneyUtils.scaleMoney(
                paymentRepository.sumCurrentPeriodGrossByContractAndStatuses(
                        contractId,
                        Set.of(PaymentApplicationStatus.APPROVED, PaymentApplicationStatus.PAID)
                )
        );

        PaymentApplication payment = new PaymentApplication(
                contract,
                appNo,
                request.periodStart(),
                request.periodEnd(),
                request.cumulativeCompletedGross(),
                previousCertifiedGross,
                request.advanceDeduction(),
                request.otherDeductions(),
                request.remarks(),
                SecurityContextUtils.getCurrentUsername()
        );
        PaymentApplication saved = paymentRepository.saveAndFlush(payment);

        auditTrailService.record(
                "PAYMENT",
                "PaymentApplication",
                saved.getId(),
                "PAYMENT_APPLICATION_CREATED",
                "Created payment application " + saved.getApplicationNo()
                        + " (currentGross=" + saved.getCurrentPeriodGross()
                        + ", retainage=" + saved.getRetainageWithheld()
                        + ", netPayable=" + saved.getNetPayableAmount() + ")",
                Map.of(
                        "applicationNo", saved.getApplicationNo(),
                        "contractId", contract.getId(),
                        "cumulativeCompletedGross", saved.getCumulativeCompletedGross(),
                        "previousCertifiedGross", saved.getPreviousCertifiedGross(),
                        "currentPeriodGross", saved.getCurrentPeriodGross(),
                        "retainageWithheld", saved.getRetainageWithheld(),
                        "netPayableAmount", saved.getNetPayableAmount()
                )
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentApplicationResponse> listContractPayments(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));
        projectAccessGuard.assertCanViewProject(contract.getProject().getId());

        return paymentRepository.findByContractIdOrderByPeriodEndAscIdAsc(contractId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PaymentApplicationResponse submitForApproval(Long paymentId) {
        PaymentApplication payment = paymentRepository.findWithContractById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment application not found: " + paymentId));
        projectAccessGuard.assertCanViewProject(payment.getContract().getProject().getId());

        payment.submitForApproval();
        PaymentApplication saved = paymentRepository.saveAndFlush(payment);

        approvalApplicationService.startWorkflow(
                ApprovalTargetType.PAYMENT_APPLICATION,
                saved.getId(),
                saved.getContract().getProject().getId(),
                saved.getApplicationNo(),
                "Interim Payment Certificate " + saved.getApplicationNo() + " (" + saved.getContract().getContractNo() + ")",
                saved.getCurrentPeriodGross()
        );

        auditTrailService.record(
                "PAYMENT",
                "PaymentApplication",
                saved.getId(),
                "PAYMENT_SUBMITTED_FOR_APPROVAL",
                "Submitted payment application " + saved.getApplicationNo() + " for certification & approval",
                Map.of("applicationNo", saved.getApplicationNo(), "status", saved.getStatus().name())
        );

        return toResponse(saved);
    }

    @Transactional
    public PaymentApplicationResponse markPaid(Long paymentId, MarkPaymentPaidRequest request) {
        PaymentApplication payment = paymentRepository.findWithContractById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment application not found: " + paymentId));
        projectAccessGuard.assertCanViewProject(payment.getContract().getProject().getId());

        payment.markPaid(request.paymentReference());
        PaymentApplication saved = paymentRepository.saveAndFlush(payment);

        auditTrailService.record(
                "PAYMENT",
                "PaymentApplication",
                saved.getId(),
                "PAYMENT_DISBURSED",
                "Disbursed payment application " + saved.getApplicationNo()
                        + " (netPayable=" + saved.getNetPayableAmount()
                        + ", ref=" + saved.getPaymentReference() + ")",
                Map.of(
                        "applicationNo", saved.getApplicationNo(),
                        "netPayableAmount", saved.getNetPayableAmount(),
                        "paymentReference", saved.getPaymentReference()
                )
        );

        return toResponse(saved);
    }

    @EventListener
    @Transactional
    public void onApprovalWorkflowCompleted(ApprovalWorkflowCompletedEvent event) {
        if (event.targetType() != ApprovalTargetType.PAYMENT_APPLICATION) {
            return;
        }
        PaymentApplication payment = paymentRepository.findWithContractById(event.targetId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment application not found: " + event.targetId()));

        if (event.finalStatus() == ApprovalWorkflowStatus.APPROVED) {
            payment.markApproved();
            paymentRepository.saveAndFlush(payment);

            auditTrailService.record(
                    "PAYMENT",
                    "PaymentApplication",
                    payment.getId(),
                    "PAYMENT_APPROVED",
                    "Approved payment application " + payment.getApplicationNo()
                            + " (netPayable=" + payment.getNetPayableAmount() + ")",
                    Map.of(
                            "applicationNo", payment.getApplicationNo(),
                            "currentPeriodGross", payment.getCurrentPeriodGross(),
                            "retainageWithheld", payment.getRetainageWithheld(),
                            "netPayableAmount", payment.getNetPayableAmount()
                    )
            );
        } else if (event.finalStatus() == ApprovalWorkflowStatus.REJECTED) {
            payment.markRejected();
            paymentRepository.saveAndFlush(payment);

            auditTrailService.record(
                    "PAYMENT",
                    "PaymentApplication",
                    payment.getId(),
                    "PAYMENT_REJECTED",
                    "Rejected payment application " + payment.getApplicationNo(),
                    Map.of("applicationNo", payment.getApplicationNo())
            );
        }
    }

    private PaymentApplicationResponse toResponse(PaymentApplication p) {
        return new PaymentApplicationResponse(
                p.getId(),
                p.getContract().getId(),
                p.getContract().getContractNo(),
                p.getContract().getProject().getId(),
                p.getApplicationNo(),
                p.getPeriodStart(),
                p.getPeriodEnd(),
                p.getCumulativeCompletedGross(),
                p.getPreviousCertifiedGross(),
                p.getCurrentPeriodGross(),
                p.getRetainageRate(),
                p.getRetainageWithheld(),
                p.getAdvanceDeduction(),
                p.getOtherDeductions(),
                p.getNetPayableAmount(),
                p.getStatus(),
                p.getApprovedAt(),
                p.getPaidAt(),
                p.getPaymentReference(),
                p.getRemarks(),
                p.getVersion(),
                p.getCreatedAt(),
                p.getCreatedBy()
        );
    }
}
