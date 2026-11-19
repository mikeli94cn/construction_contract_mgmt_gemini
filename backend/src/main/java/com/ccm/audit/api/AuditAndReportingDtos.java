package com.ccm.audit.api;

import java.math.BigDecimal;
import java.time.Instant;

public final class AuditAndReportingDtos {

    private AuditAndReportingDtos() {
    }

    public record AuditLogResponse(
            Long id,
            Instant occurredAt,
            Long actorUserId,
            String actorUsername,
            String module,
            String entityType,
            String entityId,
            String action,
            String summary,
            String payloadJson
    ) {
    }

    public record ProjectFinancialSummaryResponse(
            Long projectId,
            String projectCode,
            String projectName,
            String currency,
            String projectStatus,
            BigDecimal approvedBudget,
            int totalContracts,
            int activeContracts,
            int settledOrClosedContracts,
            BigDecimal totalOriginalContractGross,
            BigDecimal totalApprovedChangeOrdersGross,
            BigDecimal totalCurrentCommittedGross,
            BigDecimal remainingUncommittedBudget,
            BigDecimal totalCertifiedPaymentsGross,
            BigDecimal totalPaidNetAmount,
            BigDecimal totalRetainageHeld
    ) {
    }
}
