package com.ccm.payment.api;

import com.ccm.payment.api.PaymentDtos.CreatePaymentApplicationRequest;
import com.ccm.payment.api.PaymentDtos.MarkPaymentPaidRequest;
import com.ccm.payment.api.PaymentDtos.PaymentApplicationResponse;
import com.ccm.payment.application.PaymentApplicationService;
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
@Tag(name = "Payments", description = "Interim Payment Certificates (IPC), retainage withholding, and disbursement endpoints")
public class PaymentController {

    private final PaymentApplicationService paymentApplicationService;

    public PaymentController(PaymentApplicationService paymentApplicationService) {
        this.paymentApplicationService = paymentApplicationService;
    }

    @PostMapping("/contracts/{contractId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'PROJECT_MANAGER', 'CONTRACT_MANAGER', 'COST_ENGINEER')")
    @Operation(summary = "Create a new progress payment application (IPC) for an ACTIVE contract")
    public PaymentApplicationResponse createPaymentApplication(
            @PathVariable Long contractId,
            @Valid @RequestBody CreatePaymentApplicationRequest request
    ) {
        return paymentApplicationService.createPaymentApplication(contractId, request);
    }

    @GetMapping("/contracts/{contractId}/payments")
    @Operation(summary = "List progress payment applications and retainage ledger for a contract")
    public List<PaymentApplicationResponse> listContractPayments(@PathVariable Long contractId) {
        return paymentApplicationService.listContractPayments(contractId);
    }

    @PostMapping("/payments/{id}/submit-approval")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'PROJECT_MANAGER', 'CONTRACT_MANAGER', 'COST_ENGINEER')")
    @Operation(summary = "Submit a DRAFT payment application for Cost Engineer & Finance Officer approval")
    public PaymentApplicationResponse submitForApproval(@PathVariable Long id) {
        return paymentApplicationService.submitForApproval(id);
    }

    @PostMapping("/payments/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FINANCE_OFFICER')")
    @Operation(summary = "Record bank disbursement reference and mark an APPROVED payment application as PAID")
    public PaymentApplicationResponse markPaid(
            @PathVariable Long id,
            @Valid @RequestBody MarkPaymentPaidRequest request
    ) {
        return paymentApplicationService.markPaid(id, request);
    }
}
