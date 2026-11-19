package com.ccm.settlement.api;

import com.ccm.settlement.domain.FinalSettlementStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class SettlementDtos {

    private SettlementDtos() {
    }

    public record CreateFinalSettlementRequest(
            @NotBlank(message = "Settlement number is required")
            @Size(max = 60)
            String settlementNo,

            @NotNull(message = "Final measured net amount is required")
            @DecimalMin(value = "0.0001", message = "Final measured net amount must be positive")
            BigDecimal finalMeasuredNet,

            @DecimalMin(value = "0.0000")
            BigDecimal warrantyRetainageHeld,

            @DecimalMin(value = "0.0000")
            BigDecimal deductionsAndClaims,

            LocalDate completionDate,

            String remarks
    ) {
    }

    public record FinalSettlementResponse(
            Long id,
            Long contractId,
            String contractNo,
            Long projectId,
            String settlementNo,
            BigDecimal finalMeasuredNet,
            BigDecimal taxRate,
            BigDecimal finalTaxAmount,
            BigDecimal finalContractGross,
            BigDecimal totalPreviousCertifiedGross,
            BigDecimal totalRetainageHeld,
            BigDecimal retainageReleasedNow,
            BigDecimal warrantyRetainageHeld,
            BigDecimal deductionsAndClaims,
            BigDecimal finalClosingPayable,
            LocalDate completionDate,
            LocalDate warrantyExpiryDate,
            FinalSettlementStatus status,
            Instant approvedAt,
            Instant warrantyReleasedAt,
            String remarks,
            Long version,
            Instant createdAt,
            String createdBy
    ) {
    }
}
