package com.ccm.settlement.domain;

import com.ccm.common.domain.AuditableEntity;
import com.ccm.common.domain.MoneyUtils;
import com.ccm.common.exception.BusinessRuleViolationException;
import com.ccm.contract.domain.Contract;
import com.ccm.contract.domain.ContractStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "final_settlements")
public class FinalSettlement extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false, unique = true)
    private Contract contract;

    @Column(name = "settlement_no", nullable = false, unique = true, length = 60)
    private String settlementNo;

    @Column(name = "final_measured_net", nullable = false, precision = 19, scale = 4)
    private BigDecimal finalMeasuredNet;

    @Column(name = "tax_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "final_tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal finalTaxAmount;

    @Column(name = "final_contract_gross", nullable = false, precision = 19, scale = 4)
    private BigDecimal finalContractGross;

    @Column(name = "total_previous_certified_gross", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalPreviousCertifiedGross;

    @Column(name = "total_retainage_held", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalRetainageHeld;

    @Column(name = "retainage_released_now", nullable = false, precision = 19, scale = 4)
    private BigDecimal retainageReleasedNow;

    @Column(name = "warranty_retainage_held", nullable = false, precision = 19, scale = 4)
    private BigDecimal warrantyRetainageHeld;

    @Column(name = "deductions_and_claims", nullable = false, precision = 19, scale = 4)
    private BigDecimal deductionsAndClaims;

    @Column(name = "final_closing_payable", nullable = false, precision = 19, scale = 4)
    private BigDecimal finalClosingPayable;

    @Column(name = "completion_date", nullable = false)
    private LocalDate completionDate;

    @Column(name = "warranty_expiry_date", nullable = false)
    private LocalDate warrantyExpiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private FinalSettlementStatus status;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "warranty_released_at")
    private Instant warrantyReleasedAt;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    protected FinalSettlement() {
    }

    public FinalSettlement(
            Contract contract,
            String settlementNo,
            BigDecimal finalMeasuredNet,
            BigDecimal totalPreviousCertifiedGross,
            BigDecimal totalRetainageHeld,
            BigDecimal warrantyRetainageHeld,
            BigDecimal deductionsAndClaims,
            LocalDate completionDate,
            String remarks,
            String createdBy
    ) {
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Final settlement can only be created for an ACTIVE contract (current status: "
                            + contract.getStatus() + ")"
            );
        }
        if (!MoneyUtils.isPositive(finalMeasuredNet)) {
            throw new BusinessRuleViolationException("Final measured net amount must be positive");
        }

        BigDecimal scaledNet = MoneyUtils.scaleMoney(finalMeasuredNet);
        BigDecimal rate = contract.getTaxRate();
        BigDecimal tax = MoneyUtils.multiply(scaledNet, rate);
        BigDecimal finalGross = MoneyUtils.add(scaledNet, tax);

        BigDecimal scaledPrevCertified = MoneyUtils.scaleMoney(totalPreviousCertifiedGross);
        BigDecimal scaledTotalRetainage = MoneyUtils.scaleMoney(totalRetainageHeld);
        BigDecimal scaledWarrantyRetainage = MoneyUtils.scaleMoney(warrantyRetainageHeld);
        BigDecimal scaledDeductions = MoneyUtils.scaleMoney(deductionsAndClaims);

        if (MoneyUtils.isNegative(scaledWarrantyRetainage)) {
            throw new BusinessRuleViolationException("Warranty retainage held cannot be negative");
        }
        if (scaledWarrantyRetainage.compareTo(scaledTotalRetainage) > 0) {
            throw new BusinessRuleViolationException(
                    "Warranty retainage held (" + scaledWarrantyRetainage
                            + ") cannot exceed total retainage held (" + scaledTotalRetainage + ")"
            );
        }
        if (MoneyUtils.isNegative(scaledDeductions)) {
            throw new BusinessRuleViolationException("Deductions and claims cannot be negative");
        }

        BigDecimal releasedNow = MoneyUtils.subtract(scaledTotalRetainage, scaledWarrantyRetainage);
        BigDecimal remainingContractBalance = MoneyUtils.subtract(finalGross, scaledPrevCertified);
        BigDecimal closingPayable = MoneyUtils.subtract(
                MoneyUtils.add(remainingContractBalance, releasedNow),
                scaledDeductions
        );

        LocalDate resolvedCompletion = completionDate == null ? LocalDate.now() : completionDate;
        LocalDate expiry = resolvedCompletion.plusMonths(contract.getDefectLiabilityMonths());

        this.contract = contract;
        this.settlementNo = settlementNo.trim().toUpperCase();
        this.finalMeasuredNet = scaledNet;
        this.taxRate = rate;
        this.finalTaxAmount = tax;
        this.finalContractGross = finalGross;
        this.totalPreviousCertifiedGross = scaledPrevCertified;
        this.totalRetainageHeld = scaledTotalRetainage;
        this.retainageReleasedNow = releasedNow;
        this.warrantyRetainageHeld = scaledWarrantyRetainage;
        this.deductionsAndClaims = scaledDeductions;
        this.finalClosingPayable = closingPayable;
        this.completionDate = resolvedCompletion;
        this.warrantyExpiryDate = expiry;
        this.status = FinalSettlementStatus.DRAFT;
        this.remarks = remarks;
        setCreatedBy(createdBy);
    }

    public void submitForApproval() {
        if (this.status != FinalSettlementStatus.DRAFT && this.status != FinalSettlementStatus.REJECTED) {
            throw new BusinessRuleViolationException("Only DRAFT or REJECTED final settlements can be submitted for approval");
        }
        this.status = FinalSettlementStatus.IN_APPROVAL;
    }

    public void markApproved() {
        if (this.status != FinalSettlementStatus.IN_APPROVAL) {
            throw new BusinessRuleViolationException("Only IN_APPROVAL final settlements can be marked APPROVED");
        }
        this.status = FinalSettlementStatus.APPROVED;
        this.approvedAt = Instant.now();
    }

    public void markRejected() {
        if (this.status != FinalSettlementStatus.IN_APPROVAL) {
            throw new BusinessRuleViolationException("Only IN_APPROVAL final settlements can be marked REJECTED");
        }
        this.status = FinalSettlementStatus.REJECTED;
    }

    public void releaseWarrantyRetainage() {
        if (this.status != FinalSettlementStatus.APPROVED) {
            throw new BusinessRuleViolationException(
                    "Warranty retainage can only be released on an APPROVED final settlement (current status: "
                            + this.status + ")"
            );
        }
        this.retainageReleasedNow = this.totalRetainageHeld;
        this.warrantyRetainageHeld = MoneyUtils.ZERO;
        this.status = FinalSettlementStatus.WARRANTY_RELEASED;
        this.warrantyReleasedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Contract getContract() {
        return contract;
    }

    public String getSettlementNo() {
        return settlementNo;
    }

    public BigDecimal getFinalMeasuredNet() {
        return finalMeasuredNet;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public BigDecimal getFinalTaxAmount() {
        return finalTaxAmount;
    }

    public BigDecimal getFinalContractGross() {
        return finalContractGross;
    }

    public BigDecimal getTotalPreviousCertifiedGross() {
        return totalPreviousCertifiedGross;
    }

    public BigDecimal getTotalRetainageHeld() {
        return totalRetainageHeld;
    }

    public BigDecimal getRetainageReleasedNow() {
        return retainageReleasedNow;
    }

    public BigDecimal getWarrantyRetainageHeld() {
        return warrantyRetainageHeld;
    }

    public BigDecimal getDeductionsAndClaims() {
        return deductionsAndClaims;
    }

    public BigDecimal getFinalClosingPayable() {
        return finalClosingPayable;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public LocalDate getWarrantyExpiryDate() {
        return warrantyExpiryDate;
    }

    public FinalSettlementStatus getStatus() {
        return status;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getWarrantyReleasedAt() {
        return warrantyReleasedAt;
    }

    public String getRemarks() {
        return remarks;
    }
}
