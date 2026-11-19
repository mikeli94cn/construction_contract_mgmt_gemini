package com.ccm.audit.application;

import com.ccm.audit.api.AuditAndReportingDtos.AuditLogResponse;
import com.ccm.audit.api.AuditAndReportingDtos.ProjectFinancialSummaryResponse;
import com.ccm.audit.domain.AuditLog;
import com.ccm.audit.infrastructure.AuditLogRepository;
import com.ccm.common.api.PageResponse;
import com.ccm.common.domain.MoneyUtils;
import com.ccm.common.exception.ResourceNotFoundException;
import com.ccm.contract.domain.Contract;
import com.ccm.contract.domain.ContractStatus;
import com.ccm.contract.infrastructure.ContractRepository;
import com.ccm.payment.domain.PaymentApplicationStatus;
import com.ccm.payment.infrastructure.PaymentApplicationRepository;
import com.ccm.project.application.ProjectAccessGuard;
import com.ccm.project.domain.Project;
import com.ccm.project.infrastructure.ProjectRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingApplicationService {

    private final AuditLogRepository auditLogRepository;
    private final ProjectRepository projectRepository;
    private final ContractRepository contractRepository;
    private final PaymentApplicationRepository paymentRepository;
    private final ProjectAccessGuard projectAccessGuard;

    public ReportingApplicationService(
            AuditLogRepository auditLogRepository,
            ProjectRepository projectRepository,
            ContractRepository contractRepository,
            PaymentApplicationRepository paymentRepository,
            ProjectAccessGuard projectAccessGuard
    ) {
        this.auditLogRepository = auditLogRepository;
        this.projectRepository = projectRepository;
        this.contractRepository = contractRepository;
        this.paymentRepository = paymentRepository;
        this.projectAccessGuard = projectAccessGuard;
    }

    @Transactional(readOnly = true)
    public ProjectFinancialSummaryResponse getProjectFinancialSummary(Long projectId) {
        projectAccessGuard.assertCanViewProject(projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        List<Contract> contracts = contractRepository.findByProjectId(projectId);

        int totalContracts = contracts.size();
        int activeContracts = 0;
        int settledOrClosedContracts = 0;
        BigDecimal totalOriginalGross = MoneyUtils.ZERO;
        BigDecimal totalApprovedChangesGross = MoneyUtils.ZERO;
        BigDecimal totalCurrentCommittedGross = MoneyUtils.ZERO;
        BigDecimal totalCertifiedPaymentsGross = MoneyUtils.ZERO;
        BigDecimal totalPaidNetAmount = MoneyUtils.ZERO;
        BigDecimal totalRetainageHeld = MoneyUtils.ZERO;

        Set<PaymentApplicationStatus> certifiedStatuses = Set.of(
                PaymentApplicationStatus.APPROVED,
                PaymentApplicationStatus.PAID
        );

        for (Contract c : contracts) {
            if (c.getStatus() == ContractStatus.ACTIVE || c.getStatus() == ContractStatus.APPROVED) {
                activeContracts++;
            } else if (c.getStatus() == ContractStatus.SETTLED || c.getStatus() == ContractStatus.CLOSED) {
                settledOrClosedContracts++;
            }

            // Include committed contracts (APPROVED, ACTIVE, SETTLED, CLOSED) in budget commitment metrics
            if (c.getStatus() == ContractStatus.APPROVED
                    || c.getStatus() == ContractStatus.ACTIVE
                    || c.getStatus() == ContractStatus.SETTLED
                    || c.getStatus() == ContractStatus.CLOSED) {
                totalOriginalGross = MoneyUtils.add(totalOriginalGross, c.getOriginalAmountGross());
                totalApprovedChangesGross = MoneyUtils.add(totalApprovedChangesGross, c.getApprovedChangeGross());
                totalCurrentCommittedGross = MoneyUtils.add(totalCurrentCommittedGross, c.getCurrentAmountGross());
            }

            BigDecimal contractCertified = paymentRepository.sumCurrentPeriodGrossByContractAndStatuses(
                    c.getId(), certifiedStatuses
            );
            BigDecimal contractPaidNet = paymentRepository.sumNetPayableByContractAndStatuses(
                    c.getId(), Set.of(PaymentApplicationStatus.PAID)
            );
            BigDecimal contractRetainage = paymentRepository.sumRetainageWithheldByContractAndStatuses(
                    c.getId(), certifiedStatuses
            );

            totalCertifiedPaymentsGross = MoneyUtils.add(totalCertifiedPaymentsGross, contractCertified);
            totalPaidNetAmount = MoneyUtils.add(totalPaidNetAmount, contractPaidNet);
            totalRetainageHeld = MoneyUtils.add(totalRetainageHeld, contractRetainage);
        }

        BigDecimal remainingUncommitted = MoneyUtils.subtract(project.getApprovedBudget(), totalCurrentCommittedGross);

        return new ProjectFinancialSummaryResponse(
                project.getId(),
                project.getProjectCode(),
                project.getName(),
                project.getCurrency(),
                project.getStatus().name(),
                project.getApprovedBudget(),
                totalContracts,
                activeContracts,
                settledOrClosedContracts,
                totalOriginalGross,
                totalApprovedChangesGross,
                totalCurrentCommittedGross,
                remainingUncommitted,
                totalCertifiedPaymentsGross,
                totalPaidNetAmount,
                totalRetainageHeld
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> searchAuditLogs(
            String module,
            String entityType,
            String entityId,
            String actorUsername,
            Pageable pageable
    ) {
        Page<AuditLog> page = auditLogRepository.searchAuditLogs(
                normalize(module),
                normalize(entityType),
                normalize(entityId),
                normalize(actorUsername),
                pageable
        );
        return PageResponse.from(page.map(this::toAuditResponse));
    }

    private static String normalize(String val) {
        return (val == null || val.isBlank()) ? null : val.trim();
    }

    private AuditLogResponse toAuditResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getOccurredAt(),
                log.getActorUserId(),
                log.getActorUsername(),
                log.getModule(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getSummary(),
                log.getPayloadJson()
        );
    }
}
