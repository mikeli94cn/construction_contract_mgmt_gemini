package com.ccm.payment.api;

import com.ccm.payment.domain.PaymentApplicationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record CreatePaymentApplicationRequest(
            @NotBlank(message = "Payment application number is required")
            @Size(max = 60)
            String applicationNo,

            @NotNull(message = "Billing period start date is required")
            LocalDate periodStart,

            @NotNull(message = "Billing period end date is required")
            LocalDate periodEnd,

            @NotNull(message = "Cumulative completed gross amount is required")
            @DecimalMin(value = "0.0001", message = "Cumulative completed gross amount must be positive")
            BigDecimal cumulativeCompletedGross,

            @DecimalMin(value = "0.0000")
            BigDecimal advanceDeduction,

            @DecimalMin(value = "0.0000")
            BigDecimal otherDeductions,

            String remarks
    ) {
    }

    public record MarkPaymentPaidRequest(
            @NotBlank(message = "Bank disbursement payment reference is required")
            @Size(max = 120)
            String paymentReference
    ) {
    }

    public record PaymentApplicationResponse(
            Long id,
            Long contractId,
            String contractNo,
            Long projectId,
            String applicationNo,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal cumulativeCompletedGross,
            BigDecimal previousCertifiedGross,
            BigDecimal currentPeriodGross,
            BigDecimal retainageRate,
            BigDecimal retainageWithheld,
            BigDecimal advanceDeduction,
            BigDecimal otherDeductions,
            BigDecimal netPayableAmount,
            PaymentApplicationStatus status,
            Instant approvedAt,
            Instant paidAt,
            String paymentReference,
            String remarks,
            Long version,
            Instant createdAt,
            String createdBy
    ) {
    }
}
