package com.ccm.contract.domain;

import com.ccm.common.domain.MoneyUtils;
import com.ccm.common.exception.BusinessRuleViolationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

@Entity
@Table(name = "contract_line_items")
public class ContractLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price_net", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPriceNet;

    @Column(name = "total_price_net", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalPriceNet;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    protected ContractLineItem() {
    }

    public ContractLineItem(
            String itemCode,
            String description,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPriceNet,
            int sortOrder
    ) {
        if (!MoneyUtils.isPositive(quantity)) {
            throw new BusinessRuleViolationException("Line item quantity must be greater than zero: " + itemCode);
        }
        if (!MoneyUtils.isPositive(unitPriceNet)) {
            throw new BusinessRuleViolationException("Line item unit price must be greater than zero: " + itemCode);
        }
        this.itemCode = itemCode.trim();
        this.description = description.trim();
        this.unit = unit.trim();
        this.quantity = MoneyUtils.scaleMoney(quantity);
        this.unitPriceNet = MoneyUtils.scaleMoney(unitPriceNet);
        this.totalPriceNet = MoneyUtils.multiply(this.quantity, this.unitPriceNet);
        this.sortOrder = sortOrder;
    }

    void assignContract(Contract contract) {
        this.contract = contract;
    }

    public Long getId() {
        return id;
    }

    public Contract getContract() {
        return contract;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getDescription() {
        return description;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPriceNet() {
        return unitPriceNet;
    }

    public BigDecimal getTotalPriceNet() {
        return totalPriceNet;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
