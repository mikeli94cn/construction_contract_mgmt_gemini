package com.ccm.contract.api;

import com.ccm.common.api.PageResponse;
import com.ccm.contract.api.ContractDtos.ContractResponse;
import com.ccm.contract.api.ContractDtos.CreateContractRequest;
import com.ccm.contract.api.ContractDtos.UpdateContractRequest;
import com.ccm.contract.application.ContractApplicationService;
import com.ccm.contract.domain.ContractCategory;
import com.ccm.contract.domain.ContractStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contracts")
@Tag(name = "Contracts", description = "Construction contract creation, BoQ line items, and financial status endpoints")
public class ContractController {

    private final ContractApplicationService contractApplicationService;

    public ContractController(ContractApplicationService contractApplicationService) {
        this.contractApplicationService = contractApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'PROJECT_MANAGER', 'CONTRACT_MANAGER')")
    @Operation(summary = "Create a new construction contract in DRAFT status with BoQ line items")
    public ContractResponse createContract(@Valid @RequestBody CreateContractRequest request) {
        return contractApplicationService.createContract(request);
    }

    @GetMapping
    @Operation(summary = "Search and list construction contracts visible to the authenticated user")
    public PageResponse<ContractResponse> listContracts(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) ContractCategory category,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return contractApplicationService.listContracts(
                projectId,
                partnerId,
                category,
                status,
                keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full contract details including Bill of Quantities (BoQ) line items")
    public ContractResponse getContract(@PathVariable Long id) {
        return contractApplicationService.getContract(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'PROJECT_MANAGER', 'CONTRACT_MANAGER')")
    @Operation(summary = "Update a DRAFT contract and its BoQ line items (rejected once approved/active)")
    public ContractResponse updateDraftContract(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContractRequest request
    ) {
        return contractApplicationService.updateDraftContract(id, request);
    }

    @PostMapping("/{id}/submit-approval")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'PROJECT_MANAGER', 'CONTRACT_MANAGER')")
    @Operation(summary = "Submit a DRAFT contract to the multi-step approval workflow engine")
    public ContractResponse submitForApproval(@PathVariable Long id) {
        return contractApplicationService.submitForApproval(id);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'PROJECT_MANAGER', 'CONTRACT_MANAGER')")
    @Operation(summary = "Transition an APPROVED contract to ACTIVE for execution")
    public ContractResponse activateContract(@PathVariable Long id) {
        return contractApplicationService.activateContract(id);
    }
}
