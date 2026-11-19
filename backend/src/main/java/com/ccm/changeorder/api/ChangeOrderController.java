package com.ccm.changeorder.api;

import com.ccm.changeorder.api.ChangeOrderDtos.ChangeOrderResponse;
import com.ccm.changeorder.api.ChangeOrderDtos.CreateChangeOrderRequest;
import com.ccm.changeorder.application.ChangeOrderApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
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
@Tag(name = "Change Orders", description = "Contract variation orders, scope/cost adjustments, and approval integration")
public class ChangeOrderController {

    private final ChangeOrderApplicationService changeOrderApplicationService;

    public ChangeOrderController(ChangeOrderApplicationService changeOrderApplicationService) {
        this.changeOrderApplicationService = changeOrderApplicationService;
    }

    @PostMapping("/contracts/{contractId}/change-orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'PROJECT_MANAGER', 'CONTRACT_MANAGER', 'COST_ENGINEER')")
    @Operation(summary = "Create a new DRAFT change order for an active or approved contract")
    public ChangeOrderResponse createChangeOrder(
            @PathVariable Long contractId,
            @Valid @RequestBody CreateChangeOrderRequest request
    ) {
        return changeOrderApplicationService.createChangeOrder(contractId, request);
    }

    @GetMapping("/contracts/{contractId}/change-orders")
    @Operation(summary = "List change orders for a contract")
    public List<ChangeOrderResponse> listChangeOrders(@PathVariable Long contractId) {
        return changeOrderApplicationService.listContractChangeOrders(contractId);
    }

    @PostMapping("/change-orders/{id}/submit-approval")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'PROJECT_MANAGER', 'CONTRACT_MANAGER', 'COST_ENGINEER')")
    @Operation(summary = "Submit a DRAFT change order to the multi-step approval workflow engine")
    public ChangeOrderResponse submitForApproval(@PathVariable Long id) {
        return changeOrderApplicationService.submitForApproval(id);
    }
}
