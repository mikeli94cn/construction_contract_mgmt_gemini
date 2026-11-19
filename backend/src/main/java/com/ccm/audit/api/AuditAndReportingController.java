package com.ccm.audit.api;

import com.ccm.audit.api.AuditAndReportingDtos.AuditLogResponse;
import com.ccm.audit.api.AuditAndReportingDtos.ProjectFinancialSummaryResponse;
import com.ccm.audit.application.ReportingApplicationService;
import com.ccm.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Audit & Financial Reporting", description = "Executive project financial summaries and immutable audit trail explorer")
public class AuditAndReportingController {

    private final ReportingApplicationService reportingService;

    public AuditAndReportingController(ReportingApplicationService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/reports/projects/{projectId}/financial-summary")
    @Operation(summary = "Compute aggregated project financial summary across budget, contracts, change orders, payments, and retainage")
    public ProjectFinancialSummaryResponse getProjectFinancialSummary(@PathVariable Long projectId) {
        return reportingService.getProjectFinancialSummary(projectId);
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'AUDITOR', 'EXECUTIVE_APPROVER')")
    @Operation(summary = "Search and browse immutable enterprise audit trail logs")
    public PageResponse<AuditLogResponse> searchAuditLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String actorUsername,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return reportingService.searchAuditLogs(
                module,
                entityType,
                entityId,
                actorUsername,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"))
        );
    }
}
