package com.ccm.contract.api;

import com.ccm.contract.domain.ContractCategory;
import com.ccm.contract.domain.ContractStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ContractDtos {

    private ContractDtos() {
    }

    public record ContractLineItemRequest(
            @NotBlank(message = "Line item code is required")
            @Size(max = 50)
            String itemCode,

            @NotBlank(message = "Line item description is required")
            @Size(max = 500)
            String description,

            @NotBlank(message = "Unit of measure is required")
            @Size(max = 30)
            String unit,

            @NotNull(message = "Quantity is required")
            @DecimalMin(value = "0.0001", message = "Quantity must be positive")
            BigDecimal quantity,

            @NotNull(message = "Unit price (net) is required")
            @DecimalMin(value = "0.0001", message = "Unit price must be positive")
            BigDecimal unitPriceNet
    ) {
    }

    public record CreateContractRequest(
            @NotBlank(message = "Contract number is required")
            @Size(max = 60)
            String contractNo,

            @NotBlank(message = "Contract title is required")
            @Size(max = 300)
            String title,

            @NotNull(message = "Project ID is required")
            Long projectId,

            @NotNull(message = "Business partner ID is required")
            Long partnerId,

            Long parentContractId,

            @NotNull(message = "Contract category is required")
            ContractCategory category,

            @NotNull(message = "Tax rate is required")
            @DecimalMin(value = "0.0000", message = "Tax rate cannot be negative")
            @DecimalMax(value = "1.0000", message = "Tax rate cannot exceed 1.0000 (100%)")
            BigDecimal taxRate,

            @DecimalMin(value = "0.0000")
            @DecimalMax(value = "0.5000")
            BigDecimal retainageRate,

            @DecimalMin(value = "0.0000")
            @DecimalMax(value = "1.0000")
            BigDecimal advancePaymentRate,

            LocalDate signedDate,

            @NotNull(message = "Start date is required")
            LocalDate startDate,

            @NotNull(message = "End date is required")
            LocalDate endDate,

            @Min(value = 0)
            Integer defectLiabilityMonths,

            String scopeSummary,

            @NotEmpty(message = "At least one Bill of Quantities (BoQ) line item is required")
            @Valid
            List<ContractLineItemRequest> lineItems
    ) {
    }

    public record UpdateContractRequest(
            @NotBlank(message = "Contract title is required")
            @Size(max = 300)
            String title,

            @NotNull(message = "Business partner ID is required")
            Long partnerId,

            Long parentContractId,

            @NotNull(message = "Contract category is required")
            ContractCategory category,

            @NotNull(message = "Tax rate is required")
            @DecimalMin(value = "0.0000")
            @DecimalMax(value = "1.0000")
            BigDecimal taxRate,

            @DecimalMin(value = "0.0000")
            @DecimalMax(value = "0.5000")
            BigDecimal retainageRate,

            @DecimalMin(value = "0.0000")
            @DecimalMax(value = "1.0000")
            BigDecimal advancePaymentRate,

            LocalDate signedDate,

            @NotNull(message = "Start date is required")
            LocalDate startDate,

            @NotNull(message = "End date is required")
            LocalDate endDate,

            @Min(value = 0)
            Integer defectLiabilityMonths,

            String scopeSummary,

            @NotEmpty(message = "At least one Bill of Quantities (BoQ) line item is required")
            @Valid
            List<ContractLineItemRequest> lineItems
    ) {
    }

    public record ContractLineItemResponse(
            Long id,
            String itemCode,
            String description,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPriceNet,
            BigDecimal totalPriceNet,
            int sortOrder
    ) {
    }

    public record ContractResponse(
            Long id,
            String contractNo,
            String title,
            Long projectId,
            String projectCode,
            String projectName,
            Long partnerId,
            String partnerCode,
            String partnerName,
            Long parentContractId,
            String parentContractNo,
            ContractCategory category,
            String currency,
            BigDecimal originalAmountNet,
            BigDecimal taxRate,
            BigDecimal originalAmountTax,
            BigDecimal originalAmountGross,
            BigDecimal approvedChangeNet,
            BigDecimal approvedChangeGross,
            BigDecimal currentAmountNet,
            BigDecimal currentAmountGross,
            BigDecimal retainageRate,
            BigDecimal advancePaymentRate,
            LocalDate signedDate,
            LocalDate startDate,
            LocalDate endDate,
            int defectLiabilityMonths,
            ContractStatus status,
            String scopeSummary,
            List<ContractLineItemResponse> lineItems,
            Long version,
            Instant createdAt,
            Instant updatedAt,
            String createdBy
    ) {
    }
}
