package com.ccm.iam.api;

import com.ccm.iam.api.IamDtos.CreateOrganizationRequest;
import com.ccm.iam.api.IamDtos.OrganizationResponse;
import com.ccm.iam.application.IamApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Organization unit management")
public class OrganizationController {

    private final IamApplicationService iamApplicationService;

    public OrganizationController(IamApplicationService iamApplicationService) {
        this.iamApplicationService = iamApplicationService;
    }

    @GetMapping
    @Operation(summary = "List organization units")
    public List<OrganizationResponse> listOrganizations() {
        return iamApplicationService.listOrganizations();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Create a new organization unit")
    public OrganizationResponse createOrganization(@Valid @RequestBody CreateOrganizationRequest request) {
        return iamApplicationService.createOrganization(request);
    }
}
