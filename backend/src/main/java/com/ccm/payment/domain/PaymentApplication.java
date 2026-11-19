package com.ccm.payment.domain;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "payment_applications")
public class PaymentApplication extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "application_no", nullable = false, unique = true, length = 60)
    private String applicationNo;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "cumulative_completed_gross", nullable = false, precision = 19, scale = 4)
    private BigDecimal cumulativeCompletedGross;

    @Column(name = "previous_certified_gross", nullable = false, precision = 19, scale = 4)
    private BigDecimal previousCertifiedGross;

    @Column(name = "current_period_gross", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPeriodGross;

    @Column(name = "retainage_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal retainageRate;

    @Column(name = "retainage_withheld", nullable = false, precision = 19, scale = 4)
    private BigDecimal retainageWithheld;

    @Column(name = "advance_deduction", nullable = false, precision = 19, scale = 4)
    private BigDecimal advanceDeduction;

    @Column(name = "other_deductions", nullable = false, precision = 19, scale = 4)
    private BigDecimal otherDeductions;

    @Column(name = "net_payable_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal netPayableAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentApplicationStatus status;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "payment_reference", length = 120)
    private String paymentReference;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    protected PaymentApplication() {
    }

    public PaymentApplication(
            Contract contract,
            String applicationNo,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal cumulativeCompletedGross,
            BigDecimal previousCertifiedGross,
            BigDecimal advanceDeduction,
            BigDecimal otherDeductions,
            String remarks,
            String createdBy
    ) {
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Progress payment applications can only be created for ACTIVE contracts (current status: "
                            + contract.getStatus() + ")"
            );
        }
        if (periodStart == null || periodEnd == null || periodEnd.isBefore(periodStart)) {
            throw new BusinessRuleViolationException("Invalid payment application billing period dates");
        }

        BigDecimal scaledCumulative = MoneyUtils.scaleMoney(cumulativeCompletedGross);
        BigDecimal scaledPrevious = MoneyUtils.scaleMoney(previousCertifiedGross);
        BigDecimal currentContractGross = contract.getCurrentAmountGross();

        if (scaledCumulative.compareTo(currentContractGross) > 0) {
            throw new BusinessRuleViolationException(
                    "Cumulative completed gross amount (" + scaledCumulative
                            + ") cannot exceed current contract gross value (" + currentContractGross + ")"
            );
        }
        if (scaledCumulative.compareTo(scaledPrevious) <= 0) {
            throw new BusinessRuleViolationException(
                    "Cumulative completed gross amount (" + scaledCumulative
                            + ") must be strictly greater than previous certified gross amount (" + scaledPrevious + ")"
            );
        }

        BigDecimal currentGross = MoneyUtils.subtract(scaledCumulative, scaledPrevious);
        BigDecimal rate = contract.getRetainageRate();
        BigDecimal retainage = MoneyUtils.multiply(currentGross, rate);
        BigDecimal scaledAdvanceDed = MoneyUtils.scaleMoney(advanceDeduction);
        BigDecimal scaledOtherDed = MoneyUtils.scaleMoney(otherDeductions);

        if (MoneyUtils.isNegative(scaledAdvanceDed) || MoneyUtils.isNegative(scaledOtherDed)) {
            throw new BusinessRuleViolationException("Payment deductions cannot be negative");
        }

        BigDecimal netPayable = MoneyUtils.subtract(
                MoneyUtils.subtract(MoneyUtils.subtract(currentGross, retainage), scaledAdvanceDed),
                scaledOtherDed
        );
        if (MoneyUtils.isNegative(netPayable)) {
            throw new BusinessRuleViolationException(
                    "Total deductions exceed current period gross amount, resulting in a negative payable amount"
            );
        }

        this.contract = contract;
        this.applicationNo = applicationNo.trim().toUpperCase();
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.cumulativeCompletedGross = scaledCumulative;
        this.previousCertifiedGross = scaledPrevious;
        this.currentPeriodGross = currentGross;
        this.retainageRate = rate;
        this.retainageWithheld = retainage;
        this.advanceDeduction = scaledAdvanceDed;
        this.otherDeductions = scaledOtherDed;
        this.netPayableAmount = netPayable;
        this.status = PaymentApplicationStatus.DRAFT;
        this.remarks = remarks;
        setCreatedBy(createdBy);
    }

    public void submitForApproval() {
        if (this.status != PaymentApplicationStatus.DRAFT && this.status != PaymentApplicationStatus.REJECTED) {
            throw new BusinessRuleViolationException("Only DRAFT or REJECTED payment applications can be submitted for approval");
        }
        if (this.contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Contract is not ACTIVE");
        }
        if (this.cumulativeCompletedGross.compareTo(this.contract.getCurrentAmountGross()) > 0) {
            throw new BusinessRuleViolationException("Cumulative completed gross exceeds current contract gross value");
        }
        this.status = PaymentApplicationStatus.IN_APPROVAL;
    }

    public void markApproved() {
        if (this.status != PaymentApplicationStatus.IN_APPROVAL) {
            throw new BusinessRuleViolationException("Only IN_APPROVAL payment applications can be marked APPROVED");
        }
        this.status = PaymentApplicationStatus.APPROVED;
        this.approvedAt = Instant.now();
    }

    public void markRejected() {
        if (this.status != PaymentApplicationStatus.IN_APPROVAL) {
            throw new BusinessRuleViolationException("Only IN_APPROVAL payment applications can be marked REJECTED");
        }
        this.status = PaymentApplicationStatus.REJECTED;
    }

    public void markPaid(String paymentReference) {
        if (this.status != PaymentApplicationStatus.APPROVED) {
            throw new BusinessRuleViolationException(
                    "Only APPROVED payment applications can be marked PAID (current status: " + this.status + ")"
            );
        }
        if (paymentReference == null || paymentReference.isBlank()) {
            throw new BusinessRuleViolationException("Bank disbursement payment reference is required");
        }
        this.status = PaymentApplicationStatus.PAID;
        this.paymentReference = paymentReference.trim();
        this.paidAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Contract getContract() {
        return contract;
    }

    public String getApplicationNo() {
        return applicationNo;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public BigDecimal getCumulativeCompletedGross() {
        return cumulativeCompletedGross;
    }

    public BigDecimal getPreviousCertifiedGross() {
        return previousCertifiedGross;
    }

    public BigDecimal getCurrentPeriodGross() {
        return currentPeriodGross;
    }

    public BigDecimal getRetainageRate() {
        return retainageRate;
    }

    public BigDecimal getRetainageWithheld() {
        return retainageWithheld;
    }

    public BigDecimal getAdvanceDeduction() {
        return advanceDeduction;
    }

    public BigDecimal getOtherDeductions() {
        return otherDeductions;
    }

    public BigDecimal getNetPayableAmount() {
        return netPayableAmount;
    }

    public PaymentApplicationStatus getStatus() {
        return status;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getRemarks() {
        return remarks;
    }
}
