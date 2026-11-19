package com.ccm.changeorder.domain;

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

@Entity
@Table(name = "change_orders")
public class ChangeOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "change_order_no", nullable = false, unique = true, length = 60)
    private String changeOrderNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 40)
    private ChangeOrderType changeType;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "amount_delta_net", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountDeltaNet;

    @Column(name = "tax_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "amount_delta_tax", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountDeltaTax;

    @Column(name = "amount_delta_gross", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountDeltaGross;

    @Column(name = "schedule_extension_days", nullable = false)
    private int scheduleExtensionDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ChangeOrderStatus status;

    @Column(name = "approved_at")
    private Instant approvedAt;

    protected ChangeOrder() {
    }

    public ChangeOrder(
            Contract contract,
            String changeOrderNo,
            ChangeOrderType changeType,
            String title,
            String reason,
            BigDecimal amountDeltaNet,
            Integer scheduleExtensionDays,
            String createdBy
    ) {
        if (contract.getStatus() != ContractStatus.ACTIVE && contract.getStatus() != ContractStatus.APPROVED) {
            throw new BusinessRuleViolationException(
                    "Change orders can only be created for APPROVED or ACTIVE contracts (current status: "
                            + contract.getStatus() + ")"
            );
        }
        int extDays = scheduleExtensionDays == null ? 0 : scheduleExtensionDays;
        BigDecimal scaledNet = MoneyUtils.scaleMoney(amountDeltaNet);

        if (changeType == ChangeOrderType.SCOPE_ADDITION && !MoneyUtils.isPositive(scaledNet)) {
            throw new BusinessRuleViolationException("SCOPE_ADDITION change order must have a positive net amount delta");
        }
        if (changeType == ChangeOrderType.SCOPE_DEDUCTION && !MoneyUtils.isNegative(scaledNet)) {
            throw new BusinessRuleViolationException("SCOPE_DEDUCTION change order must have a negative net amount delta");
        }
        if (changeType == ChangeOrderType.TIME_EXTENSION && extDays <= 0) {
            throw new BusinessRuleViolationException("TIME_EXTENSION change order must have positive schedule extension days");
        }

        BigDecimal rate = contract.getTaxRate();
        BigDecimal taxDelta = MoneyUtils.multiply(scaledNet, rate);
        BigDecimal grossDelta = MoneyUtils.add(scaledNet, taxDelta);

        if (MoneyUtils.isNegative(MoneyUtils.add(contract.getCurrentAmountGross(), grossDelta))) {
            throw new BusinessRuleViolationException("Change order deduction cannot reduce contract gross value below zero");
        }

        this.contract = contract;
        this.changeOrderNo = changeOrderNo.trim().toUpperCase();
        this.changeType = changeType;
        this.title = title.trim();
        this.reason = reason.trim();
        this.amountDeltaNet = scaledNet;
        this.taxRate = rate;
        this.amountDeltaTax = taxDelta;
        this.amountDeltaGross = grossDelta;
        this.scheduleExtensionDays = extDays;
        this.status = ChangeOrderStatus.DRAFT;
        setCreatedBy(createdBy);
    }

    public void submitForApproval() {
        if (this.status != ChangeOrderStatus.DRAFT && this.status != ChangeOrderStatus.REJECTED) {
            throw new BusinessRuleViolationException("Only DRAFT or REJECTED change orders can be submitted for approval");
        }
        if (this.contract.getStatus() != ContractStatus.ACTIVE && this.contract.getStatus() != ContractStatus.APPROVED) {
            throw new BusinessRuleViolationException("Parent contract is no longer in an active state for change orders");
        }
        this.status = ChangeOrderStatus.IN_APPROVAL;
    }

    public void markApproved() {
        if (this.status != ChangeOrderStatus.IN_APPROVAL) {
            throw new BusinessRuleViolationException("Only IN_APPROVAL change orders can be marked APPROVED");
        }
        this.status = ChangeOrderStatus.APPROVED;
        this.approvedAt = Instant.now();
    }

    public void markRejected() {
        if (this.status != ChangeOrderStatus.IN_APPROVAL) {
            throw new BusinessRuleViolationException("Only IN_APPROVAL change orders can be marked REJECTED");
        }
        this.status = ChangeOrderStatus.REJECTED;
    }

    public Long getId() {
        return id;
    }

    public Contract getContract() {
        return contract;
    }

    public String getChangeOrderNo() {
        return changeOrderNo;
    }

    public ChangeOrderType getChangeType() {
        return changeType;
    }

    public String getTitle() {
        return title;
    }

    public String getReason() {
        return reason;
    }

    public BigDecimal getAmountDeltaNet() {
        return amountDeltaNet;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public BigDecimal getAmountDeltaTax() {
        return amountDeltaTax;
    }

    public BigDecimal getAmountDeltaGross() {
        return amountDeltaGross;
    }

    public int getScheduleExtensionDays() {
        return scheduleExtensionDays;
    }

    public ChangeOrderStatus getStatus() {
        return status;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }
}
