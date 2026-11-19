package com.ccm.partner.api;

import com.ccm.common.api.PageResponse;
import com.ccm.partner.api.PartnerDtos.CreatePartnerRequest;
import com.ccm.partner.api.PartnerDtos.PartnerResponse;
import com.ccm.partner.api.PartnerDtos.UpdatePartnerRequest;
import com.ccm.partner.application.BusinessPartnerApplicationService;
import com.ccm.partner.domain.PartnerStatus;
import com.ccm.partner.domain.PartnerType;
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
@RequestMapping("/api/v1/partners")
@Tag(name = "Business Partners", description = "Owners, general contractors, subcontractors, suppliers, and consultants directory")
public class BusinessPartnerController {

    private final BusinessPartnerApplicationService businessPartnerApplicationService;

    public BusinessPartnerController(BusinessPartnerApplicationService businessPartnerApplicationService) {
        this.businessPartnerApplicationService = businessPartnerApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CONTRACT_MANAGER', 'PROJECT_MANAGER')")
    @Operation(summary = "Register a new business partner")
    public PartnerResponse createPartner(@Valid @RequestBody CreatePartnerRequest request) {
        return businessPartnerApplicationService.createPartner(request);
    }

    @GetMapping
    @Operation(summary = "Search and list business partners")
    public PageResponse<PartnerResponse> listPartners(
            @RequestParam(required = false) PartnerType partnerType,
            @RequestParam(required = false) PartnerStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return businessPartnerApplicationService.listPartners(
                partnerType,
                status,
                keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get business partner details")
    public PartnerResponse getPartner(@PathVariable Long id) {
        return businessPartnerApplicationService.getPartner(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CONTRACT_MANAGER')")
    @Operation(summary = "Update business partner details or qualification/active status")
    public PartnerResponse updatePartner(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePartnerRequest request
    ) {
        return businessPartnerApplicationService.updatePartner(id, request);
    }
}
