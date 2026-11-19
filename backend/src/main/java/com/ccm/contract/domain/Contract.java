package com.ccm.contract.domain;

import com.ccm.common.domain.AuditableEntity;
import com.ccm.common.domain.MoneyUtils;
import com.ccm.common.exception.BusinessRuleViolationException;
import com.ccm.partner.domain.BusinessPartner;
import com.ccm.project.domain.Project;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "contracts")
public class Contract extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_no", nullable = false, unique = true, length = 60)
    private String contractNo;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private BusinessPartner partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_contract_id")
    private Contract parentContract;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private ContractCategory category;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "original_amount_net", nullable = false, precision = 19, scale = 4)
    private BigDecimal originalAmountNet;

    @Column(name = "tax_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "original_amount_tax", nullable = false, precision = 19, scale = 4)
    private BigDecimal originalAmountTax;

    @Column(name = "original_amount_gross", nullable = false, precision = 19, scale = 4)
    private BigDecimal originalAmountGross;

    @Column(name = "approved_change_net", nullable = false, precision = 19, scale = 4)
    private BigDecimal approvedChangeNet = MoneyUtils.ZERO;

    @Column(name = "approved_change_gross", nullable = false, precision = 19, scale = 4)
    private BigDecimal approvedChangeGross = MoneyUtils.ZERO;

    @Column(name = "retainage_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal retainageRate;

    @Column(name = "advance_payment_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal advancePaymentRate;

    @Column(name = "signed_date")
    private LocalDate signedDate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "defect_liability_months", nullable = false)
    private int defectLiabilityMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ContractStatus status;

    @Column(name = "scope_summary", columnDefinition = "TEXT")
    private String scopeSummary;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ContractLineItem> lineItems = new ArrayList<>();

    protected Contract() {
    }

    public Contract(
            String contractNo,
            String title,
            Project project,
            BusinessPartner partner,
            Contract parentContract,
            ContractCategory category,
            BigDecimal taxRate,
            BigDecimal retainageRate,
            BigDecimal advancePaymentRate,
            LocalDate signedDate,
            LocalDate startDate,
            LocalDate endDate,
            Integer defectLiabilityMonths,
            String scopeSummary,
            List<ContractLineItem> items,
            String createdBy
    ) {
        if (!project.getStatus().canAcceptNewContracts()) {
            throw new BusinessRuleViolationException(
                    "Project " + project.getProjectCode() + " in status " + project.getStatus() + " cannot accept new contracts"
            );
        }
        if (!partner.getStatus().isEligibleForNewContract()) {
            throw new BusinessRuleViolationException(
                    "Business partner " + partner.getPartnerCode() + " is " + partner.getStatus() + " and ineligible for new contracts"
            );
        }
        if (parentContract != null && !parentContract.getProject().getId().equals(project.getId())) {
            throw new BusinessRuleViolationException("Parent contract must belong to the same project");
        }

        this.contractNo = contractNo.trim().toUpperCase();
        this.title = title.trim();
        this.project = project;
        this.partner = partner;
        this.parentContract = parentContract;
        this.category = category;
        this.currency = project.getCurrency();
        this.status = ContractStatus.DRAFT;
        this.approvedChangeNet = MoneyUtils.ZERO;
        this.approvedChangeGross = MoneyUtils.ZERO;

        applyEditableFieldsAndRecalculate(
                title,
                partner,
                parentContract,
                category,
                taxRate,
                retainageRate,
                advancePaymentRate,
                signedDate,
                startDate,
                endDate,
                defectLiabilityMonths,
                scopeSummary,
                items
        );
        setCreatedBy(createdBy);
    }

    public void updateDraft(
            String title,
            BusinessPartner partner,
            Contract parentContract,
            ContractCategory category,
            BigDecimal taxRate,
            BigDecimal retainageRate,
            BigDecimal advancePaymentRate,
            LocalDate signedDate,
            LocalDate startDate,
            LocalDate endDate,
            Integer defectLiabilityMonths,
            String scopeSummary,
            List<ContractLineItem> items
    ) {
        if (!this.status.isDraftEditable()) {
            throw new BusinessRuleViolationException(
                    "Contract " + this.contractNo + " is in status " + this.status
                            + ". Approved or active financial records cannot be modified directly; use a Change Order."
            );
        }
        if (!partner.getStatus().isEligibleForNewContract()) {
            throw new BusinessRuleViolationException(
                    "Business partner " + partner.getPartnerCode() + " is " + partner.getStatus() + " and ineligible"
            );
        }
        if (parentContract != null) {
            if (this.id != null && this.id.equals(parentContract.getId())) {
                throw new BusinessRuleViolationException("A contract cannot be its own parent contract");
            }
            if (!parentContract.getProject().getId().equals(this.project.getId())) {
                throw new BusinessRuleViolationException("Parent contract must belong to the same project");
            }
        }

        applyEditableFieldsAndRecalculate(
                title,
                partner,
                parentContract,
                category,
                taxRate,
                retainageRate,
                advancePaymentRate,
                signedDate,
                startDate,
                endDate,
                defectLiabilityMonths,
                scopeSummary,
                items
        );
    }

    private void applyEditableFieldsAndRecalculate(
            String title,
            BusinessPartner partner,
            Contract parentContract,
            ContractCategory category,
            BigDecimal taxRate,
            BigDecimal retainageRate,
            BigDecimal advancePaymentRate,
            LocalDate signedDate,
            LocalDate startDate,
            LocalDate endDate,
            Integer defectLiabilityMonths,
            String scopeSummary,
            List<ContractLineItem> items
    ) {
        if (items == null || items.isEmpty()) {
            throw new BusinessRuleViolationException("Contract must contain at least one Bill of Quantities (BoQ) line item");
        }
        if (startDate == null || endDate == null) {
            throw new BusinessRuleViolationException("Contract start date and end date are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleViolationException("Contract end date cannot be earlier than start date");
        }
        validateRate(taxRate, BigDecimal.ZERO, new BigDecimal("1.0000"), "Tax rate");
        BigDecimal resolvedRetainage = retainageRate == null ? new BigDecimal("0.0500") : retainageRate;
        validateRate(resolvedRetainage, BigDecimal.ZERO, new BigDecimal("0.5000"), "Retainage rate");
        BigDecimal resolvedAdvance = advancePaymentRate == null ? BigDecimal.ZERO : advancePaymentRate;
        validateRate(resolvedAdvance, BigDecimal.ZERO, new BigDecimal("1.0000"), "Advance payment rate");

        int resolvedDlp = defectLiabilityMonths == null ? 12 : defectLiabilityMonths;
        if (resolvedDlp < 0 || resolvedDlp > 120) {
            throw new BusinessRuleViolationException("Defect liability months must be between 0 and 120");
        }

        this.title = title.trim();
        this.partner = partner;
        this.parentContract = parentContract;
        this.category = category;
        this.taxRate = MoneyUtils.scaleRate(taxRate);
        this.retainageRate = MoneyUtils.scaleRate(resolvedRetainage);
        this.advancePaymentRate = MoneyUtils.scaleRate(resolvedAdvance);
        this.signedDate = signedDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.defectLiabilityMonths = resolvedDlp;
        this.scopeSummary = scopeSummary;

        this.lineItems.clear();
        BigDecimal netSum = MoneyUtils.ZERO;
        for (ContractLineItem item : items) {
            item.assignContract(this);
            this.lineItems.add(item);
            netSum = MoneyUtils.add(netSum, item.getTotalPriceNet());
        }

        this.originalAmountNet = netSum;
        this.originalAmountTax = MoneyUtils.multiply(this.originalAmountNet, this.taxRate);
        this.originalAmountGross = MoneyUtils.add(this.originalAmountNet, this.originalAmountTax);
    }

    private static void validateRate(BigDecimal rate, BigDecimal min, BigDecimal max, String label) {
        if (rate == null || rate.compareTo(min) < 0 || rate.compareTo(max) > 0) {
            throw new BusinessRuleViolationException(label + " must be between " + min + " and " + max);
        }
    }

    public void submitForApproval() {
        if (this.status != ContractStatus.DRAFT) {
            throw new BusinessRuleViolationException("Only a DRAFT contract can be submitted for approval");
        }
        this.status = ContractStatus.IN_APPROVAL;
    }

    public void markApproved() {
        if (this.status != ContractStatus.IN_APPROVAL) {
            throw new BusinessRuleViolationException("Only an IN_APPROVAL contract can be marked APPROVED");
        }
        this.status = ContractStatus.APPROVED;
    }

    public void returnToDraftOnRejection() {
        if (this.status != ContractStatus.IN_APPROVAL) {
            throw new BusinessRuleViolationException("Only an IN_APPROVAL contract can be returned to DRAFT on rejection");
        }
        this.status = ContractStatus.DRAFT;
    }

    public void activate() {
        if (this.status != ContractStatus.APPROVED) {
            throw new BusinessRuleViolationException("Only an APPROVED contract can be activated");
        }
        this.status = ContractStatus.ACTIVE;
    }

    public void markSettled() {
        if (this.status != ContractStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Only an ACTIVE contract can be marked SETTLED");
        }
        this.status = ContractStatus.SETTLED;
    }

    public void markClosed() {
        if (this.status != ContractStatus.SETTLED) {
            throw new BusinessRuleViolationException("Only a SETTLED contract can be marked CLOSED");
        }
        this.status = ContractStatus.CLOSED;
    }

    public void applyApprovedChangeOrderDelta(BigDecimal deltaNet, BigDecimal deltaGross) {
        if (this.status != ContractStatus.ACTIVE && this.status != ContractStatus.APPROVED) {
            throw new BusinessRuleViolationException("Change orders can only be applied to APPROVED or ACTIVE contracts");
        }
        BigDecimal newCurrentGross = MoneyUtils.add(getCurrentAmountGross(), deltaGross);
        if (MoneyUtils.isNegative(newCurrentGross)) {
            throw new BusinessRuleViolationException("Change order deduction cannot reduce current contract value below zero");
        }
        this.approvedChangeNet = MoneyUtils.add(this.approvedChangeNet, deltaNet);
        this.approvedChangeGross = MoneyUtils.add(this.approvedChangeGross, deltaGross);
    }

    public BigDecimal getCurrentAmountNet() {
        return MoneyUtils.add(this.originalAmountNet, this.approvedChangeNet);
    }

    public BigDecimal getCurrentAmountGross() {
        return MoneyUtils.add(this.originalAmountGross, this.approvedChangeGross);
    }

    public Long getId() {
        return id;
    }

    public String getContractNo() {
        return contractNo;
    }

    public String getTitle() {
        return title;
    }

    public Project getProject() {
        return project;
    }

    public BusinessPartner getPartner() {
        return partner;
    }

    public Contract getParentContract() {
        return parentContract;
    }

    public ContractCategory getCategory() {
        return category;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getOriginalAmountNet() {
        return originalAmountNet;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public BigDecimal getOriginalAmountTax() {
        return originalAmountTax;
    }

    public BigDecimal getOriginalAmountGross() {
        return originalAmountGross;
    }

    public BigDecimal getApprovedChangeNet() {
        return approvedChangeNet;
    }

    public BigDecimal getApprovedChangeGross() {
        return approvedChangeGross;
    }

    public BigDecimal getRetainageRate() {
        return retainageRate;
    }

    public BigDecimal getAdvancePaymentRate() {
        return advancePaymentRate;
    }

    public LocalDate getSignedDate() {
        return signedDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getDefectLiabilityMonths() {
        return defectLiabilityMonths;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public String getScopeSummary() {
        return scopeSummary;
    }

    public List<ContractLineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }
}
