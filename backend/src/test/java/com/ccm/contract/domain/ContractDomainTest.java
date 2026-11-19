package com.ccm.contract.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ccm.common.exception.BusinessRuleViolationException;
import com.ccm.iam.domain.Organization;
import com.ccm.partner.domain.BusinessPartner;
import com.ccm.partner.domain.PartnerStatus;
import com.ccm.partner.domain.PartnerType;
import com.ccm.project.domain.Project;
import com.ccm.project.domain.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContractDomainTest {

    private Organization org;
    private Project activeProject;
    private BusinessPartner activePartner;

    @BeforeEach
    void setUp() {
        org = new Organization("ORG-HQ", "Apex HQ", null, "SYSTEM");
        activeProject = new Project(
                "PRJ-001",
                "Metro Line 4 Station",
                org,
                "Downtown District",
                "USD",
                new BigDecimal("50000000.0000"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2028, 12, 31),
                ProjectStatus.ACTIVE,
                "Subway station civil works",
                "pm.alex"
        );
        activePartner = new BusinessPartner(
                "BP-SUB-01",
                "Titan Structural Engineering Ltd.",
                PartnerType.SUBCONTRACTOR,
                "TAX-99887766",
                "Grade-A Civil",
                "John Builder",
                "+1-555-0199",
                "john@titan.example",
                "First National Bank",
                "ACCT-123456",
                PartnerStatus.ACTIVE,
                "cm.elena"
        );
    }

    @Test
    void shouldCalculateExactNetTaxAndGrossAmountsFromBoQLineItems() {
        List<ContractLineItem> items = List.of(
                new ContractLineItem("BOQ-101", "Excavation & Earthworks", "m3",
                        new BigDecimal("1250.5000"), new BigDecimal("40.0000"), 1),
                new ContractLineItem("BOQ-102", "C40 Reinforced Concrete", "m3",
                        new BigDecimal("800.0000"), new BigDecimal("187.4750"), 2)
        );
        // Item 1: 1250.5 * 40 = 50,020.0000
        // Item 2: 800 * 187.475 = 149,980.0000
        // Net Total = 200,000.0000
        // Tax @ 9% (0.0900) = 18,000.0000
        // Gross Total = 218,000.0000

        Contract contract = new Contract(
                "CNT-2026-001",
                "Station Foundation & Structural Concrete",
                activeProject,
                activePartner,
                null,
                ContractCategory.SUBCONTRACT,
                new BigDecimal("0.0900"),
                new BigDecimal("0.0500"),
                new BigDecimal("0.1000"),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 15),
                LocalDate.of(2027, 6, 30),
                24,
                "Foundation structural package",
                items,
                "cm.elena"
        );

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.DRAFT);
        assertThat(contract.getOriginalAmountNet()).isEqualByComparingTo("200000.0000");
        assertThat(contract.getOriginalAmountTax()).isEqualByComparingTo("18000.0000");
        assertThat(contract.getOriginalAmountGross()).isEqualByComparingTo("218000.0000");
        assertThat(contract.getCurrentAmountGross()).isEqualByComparingTo("218000.0000");
        assertThat(contract.getLineItems()).hasSize(2);
    }

    @Test
    void shouldRejectContractCreationWithBlacklistedOrSuspendedPartner() {
        BusinessPartner blacklistedPartner = new BusinessPartner(
                "BP-BAD",
                "Disqualified Vendor",
                PartnerType.SUPPLIER,
                "TAX-000111",
                null,
                "Bad Actor",
                "+1-555-0000",
                "bad@vendor.example",
                null,
                null,
                PartnerStatus.BLACKLISTED,
                "admin"
        );

        List<ContractLineItem> items = List.of(
                new ContractLineItem("BOQ-1", "Rebar Supply", "ton",
                        new BigDecimal("100.0000"), new BigDecimal("650.0000"), 1)
        );

        assertThatThrownBy(() -> new Contract(
                "CNT-2026-002",
                "Steel Rebar Supply",
                activeProject,
                blacklistedPartner,
                null,
                ContractCategory.SUPPLY_CONTRACT,
                new BigDecimal("0.0900"),
                new BigDecimal("0.0500"),
                BigDecimal.ZERO,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 8, 10),
                12,
                null,
                items,
                "cm.elena"
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("ineligible for new contracts");
    }

    @Test
    void shouldForbidSilentDirectModificationOnceContractIsInApprovalOrApproved() {
        List<ContractLineItem> initialItems = List.of(
                new ContractLineItem("BOQ-1", "Site Preparation", "lump",
                        new BigDecimal("1.0000"), new BigDecimal("100000.0000"), 1)
        );
        Contract contract = new Contract(
                "CNT-2026-003",
                "Site Preparation Package",
                activeProject,
                activePartner,
                null,
                ContractCategory.SUBCONTRACT,
                new BigDecimal("0.1000"),
                new BigDecimal("0.0500"),
                BigDecimal.ZERO,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 5),
                LocalDate.of(2026, 9, 5),
                12,
                "Initial scope",
                initialItems,
                "cm.elena"
        );

        contract.submitForApproval();
        contract.markApproved();

        List<ContractLineItem> tamperedItems = List.of(
                new ContractLineItem("BOQ-1", "Site Preparation", "lump",
                        new BigDecimal("1.0000"), new BigDecimal("250000.0000"), 1)
        );

        assertThatThrownBy(() -> contract.updateDraft(
                "Tampered Contract Title",
                activePartner,
                null,
                ContractCategory.SUBCONTRACT,
                new BigDecimal("0.1000"),
                new BigDecimal("0.0500"),
                BigDecimal.ZERO,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 5),
                LocalDate.of(2026, 9, 5),
                12,
                "Tampered scope",
                tamperedItems
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Approved or active financial records cannot be modified directly");
    }
}
