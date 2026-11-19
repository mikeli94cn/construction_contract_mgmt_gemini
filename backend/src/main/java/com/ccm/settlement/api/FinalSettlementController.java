package com.ccm.settlement.api;

import com.ccm.settlement.api.SettlementDtos.CreateFinalSettlementRequest;
import com.ccm.settlement.api.SettlementDtos.FinalSettlementResponse;
import com.ccm.settlement.application.FinalSettlementApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Final Settlement", description = "Final contract account reconciliation, warranty retainage release, and contract closure")
public class FinalSettlementController {

    private final FinalSettlementApplicationService settlementService;

    public FinalSettlementController(FinalSettlementApplicationService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/contracts/{contractId}/settlement")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'PROJECT_MANAGER', 'CONTRACT_MANAGER', 'COST_ENGINEER')")
    @Operation(summary = "Create a Final Settlement reconciliation for an ACTIVE contract")
    public FinalSettlementResponse createSettlement(
            @PathVariable Long contractId,
            @Valid @RequestBody CreateFinalSettlementRequest request
    ) {
        return settlementService.createFinalSettlement(contractId, request);
    }

    @GetMapping("/contracts/{contractId}/settlement")
    @Operation(summary = "Get the Final Settlement record for a contract if one exists")
    public ResponseEntity<FinalSettlementResponse> getContractSettlement(@PathVariable Long contractId) {
        return settlementService.getByContractId(contractId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/settlements/{id}/submit-approval")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'PROJECT_MANAGER', 'CONTRACT_MANAGER', 'COST_ENGINEER')")
    @Operation(summary = "Submit a Final Settlement for 3-step approval (locks contract in SETTLED upon approval)")
    public FinalSettlementResponse submitForApproval(@PathVariable Long id) {
        return settlementService.submitForApproval(id);
    }

    @PostMapping("/settlements/{id}/release-warranty-retainage")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FINANCE_OFFICER')")
    @Operation(summary = "Release remaining warranty retainage after DLP and close the contract")
    public FinalSettlementResponse releaseWarrantyRetainage(@PathVariable Long id) {
        return settlementService.releaseWarrantyRetainage(id);
    }
}
