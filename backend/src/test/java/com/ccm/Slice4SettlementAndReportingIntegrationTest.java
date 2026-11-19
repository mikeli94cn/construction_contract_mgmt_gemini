package com.ccm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ccm.approval.api.ApprovalDtos.ApprovalDecisionRequest;
import com.ccm.approval.domain.ApprovalDecisionAction;
import com.ccm.changeorder.api.ChangeOrderDtos.CreateChangeOrderRequest;
import com.ccm.changeorder.domain.ChangeOrderType;
import com.ccm.contract.api.ContractDtos.ContractLineItemRequest;
import com.ccm.contract.api.ContractDtos.CreateContractRequest;
import com.ccm.contract.domain.ContractCategory;
import com.ccm.iam.api.IamDtos.LoginRequest;
import com.ccm.iam.domain.User;
import com.ccm.iam.infrastructure.OrganizationRepository;
import com.ccm.iam.infrastructure.UserRepository;
import com.ccm.partner.api.PartnerDtos.CreatePartnerRequest;
import com.ccm.partner.domain.PartnerStatus;
import com.ccm.partner.domain.PartnerType;
import com.ccm.payment.api.PaymentDtos.CreatePaymentApplicationRequest;
import com.ccm.payment.api.PaymentDtos.MarkPaymentPaidRequest;
import com.ccm.project.api.ProjectDtos.AssignProjectMemberRequest;
import com.ccm.project.api.ProjectDtos.CreateProjectRequest;
import com.ccm.project.domain.ProjectRole;
import com.ccm.project.domain.ProjectStatus;
import com.ccm.settlement.api.SettlementDtos.CreateFinalSettlementRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class Slice4SettlementAndReportingIntegrationTest {

    private static final String TEST_BOOTSTRAP_PASS = "TestOnly-Bootstrap-Secret-2026!";

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("ccm_test_s4")
            .withUsername("ccm_test_user")
            .withPassword("ccm_test_pwd");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("ccm.bootstrap.enabled", () -> "true");
        registry.add("ccm.bootstrap.admin-password", () -> TEST_BOOTSTRAP_PASS);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldExecuteFinalSettlementLockContractReleaseWarrantyAndGenerateFinancialReportsAndAuditTrail()
            throws Exception {
        String pmToken = login("pm.alex");
        String cmToken = login("cm.elena");
        String qsToken = login("qs.david");
        String finToken = login("fin.sarah");
        String execToken = login("exec.victor");
        String auditorToken = login("auditor.maya");

        Long hqOrgId = organizationRepository.findByCode("ORG-HQ").orElseThrow().getId();

        // 1. Create Project ($10,000,000 budget) & assign CM and QS
        MvcResult projRes = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProjectRequest(
                                "PRJ-SOLAR-04",
                                "Utility-Scale Solar & Substation Park",
                                hqOrgId,
                                "Mojave Energy Corridor",
                                "USD",
                                new BigDecimal("10000000.0000"),
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2027, 12, 31),
                                ProjectStatus.ACTIVE,
                                "150MW solar array & 230kV substation"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long projectId = objectMapper.readTree(projRes.getResponse().getContentAsString()).get("id").asLong();

        User elena = userRepository.findByUsername("cm.elena").orElseThrow();
        User david = userRepository.findByUsername("qs.david").orElseThrow();
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssignProjectMemberRequest(elena.getId(), ProjectRole.CONTRACT_MANAGER))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssignProjectMemberRequest(david.getId(), ProjectRole.COST_ENGINEER))))
                .andExpect(status().isCreated());

        // 2. Register Partner
        MvcResult partnerRes = mockMvc.perform(post("/api/v1/partners")
                        .header("Authorization", "Bearer " + cmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePartnerRequest(
                                "BP-GRID-401",
                                "Helios Grid & Substation Engineering Inc.",
                                PartnerType.GENERAL_CONTRACTOR,
                                "TAX-GRID-401",
                                "Class-A High Voltage",
                                "Carlos Mendoza",
                                "+1-555-4010",
                                "c.mendoza@heliosgrid.example",
                                "Bank of America",
                                "401401401",
                                PartnerStatus.ACTIVE
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long partnerId = objectMapper.readTree(partnerRes.getResponse().getContentAsString()).get("id").asLong();

        // 3. Create Contract ($1,000,000 net + 10% tax = $1,100,000 gross; 5% retainage; 12 months DLP), approve & activate
        MvcResult contractRes = mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + cmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateContractRequest(
                                "CNT-SOL-2026-001",
                                "230kV Step-Up Substation Turnkey EPC",
                                projectId,
                                partnerId,
                                null,
                                ContractCategory.MAIN_CONTRACT,
                                new BigDecimal("0.1000"),
                                new BigDecimal("0.0500"),
                                BigDecimal.ZERO,
                                LocalDate.of(2026, 1, 15),
                                LocalDate.of(2026, 2, 1),
                                LocalDate.of(2026, 11, 30),
                                12,
                                "Transformer bays, GIS switchgear, protection relays",
                                List.of(new ContractLineItemRequest("SUB-01", "230kV GIS & Transformer Package", "set",
                                        new BigDecimal("1.0000"), new BigDecimal("1000000.0000")))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long contractId = objectMapper.readTree(contractRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/submit-approval")
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk());
        Long contractWfId = getLatestWorkflowId(cmToken, "CONTRACT", contractId);
        approveStep(contractWfId, qsToken, "QS approved");
        approveStep(contractWfId, pmToken, "PM approved");
        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/activate")
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk());

        // 4. Create & Approve Change Order (+$100,000 net + 10% tax = +$110,000 gross -> Current Gross = $1,210,000)
        MvcResult coRes = mockMvc.perform(post("/api/v1/contracts/" + contractId + "/change-orders")
                        .header("Authorization", "Bearer " + cmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateChangeOrderRequest(
                                "CO-SOL-001",
                                ChangeOrderType.SCOPE_ADDITION,
                                "Harmonic Filter Bank Upgrade",
                                "Grid interconnection utility compliance requirement",
                                new BigDecimal("100000.0000"),
                                10
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long coId = objectMapper.readTree(coRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/change-orders/" + coId + "/submit-approval")
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk());
        Long coWfId = getLatestWorkflowId(cmToken, "CHANGE_ORDER", coId);
        approveStep(coWfId, qsToken, "QS approved variation");
        approveStep(coWfId, pmToken, "PM approved variation");

        // 5. Create, Approve & Pay IPC-SOL-001 ($1,000,000 cumulative gross -> $50,000 retainage held -> $950,000 net paid)
        MvcResult ipcRes = mockMvc.perform(post("/api/v1/contracts/" + contractId + "/payments")
                        .header("Authorization", "Bearer " + qsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePaymentApplicationRequest(
                                "IPC-SOL-001",
                                LocalDate.of(2026, 2, 1),
                                LocalDate.of(2026, 8, 31),
                                new BigDecimal("1000000.0000"),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                "Substation installation milestone"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long ipcId = objectMapper.readTree(ipcRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/payments/" + ipcId + "/submit-approval")
                        .header("Authorization", "Bearer " + qsToken))
                .andExpect(status().isOk());
        Long ipcWfId = getLatestWorkflowId(qsToken, "PAYMENT_APPLICATION", ipcId);
        approveStep(ipcWfId, qsToken, "Certified");
        approveStep(ipcWfId, finToken, "Verified");
        mockMvc.perform(post("/api/v1/payments/" + ipcId + "/mark-paid")
                        .header("Authorization", "Bearer " + finToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarkPaymentPaidRequest("WIRE-SOL-7701"))))
                .andExpect(status().isOk());

        // 6. Create Final Settlement (FS-SOL-001):
        // Final Measured Net = $1,080,000 -> Tax (10%) = $108,000 -> Final Contract Gross = $1,188,000
        // Previous Certified Gross = $1,000,000; Total Retainage Held = $50,000
        // Warranty Retainage Held = $20,000 -> Retainage Released Now = $30,000
        // Deductions & Claims = $8,000
        // Final Closing Payable = ($1,188,000 - $1,000,000) + $30,000 - $8,000 = $210,000
        CreateFinalSettlementRequest fsReq = new CreateFinalSettlementRequest(
                "FS-SOL-001",
                new BigDecimal("1080000.0000"),
                new BigDecimal("20000.0000"),
                new BigDecimal("8000.0000"),
                LocalDate.of(2026, 11, 15),
                "Final as-built measurement and punch-list deduction reconciliation"
        );

        MvcResult fsRes = mockMvc.perform(post("/api/v1/contracts/" + contractId + "/settlement")
                        .header("Authorization", "Bearer " + qsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fsReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.finalContractGross").value(1188000.0))
                .andExpect(jsonPath("$.totalPreviousCertifiedGross").value(1000000.0))
                .andExpect(jsonPath("$.totalRetainageHeld").value(50000.0))
                .andExpect(jsonPath("$.retainageReleasedNow").value(30000.0))
                .andExpect(jsonPath("$.warrantyRetainageHeld").value(20000.0))
                .andExpect(jsonPath("$.deductionsAndClaims").value(8000.0))
                .andExpect(jsonPath("$.finalClosingPayable").value(210000.0))
                .andExpect(jsonPath("$.warrantyExpiryDate").value("2027-11-15"))
                .andReturn();
        Long settlementId = objectMapper.readTree(fsRes.getResponse().getContentAsString()).get("id").asLong();

        // 7. Submit Final Settlement for 3-step approval (QS -> FIN -> EXEC)
        mockMvc.perform(post("/api/v1/settlements/" + settlementId + "/submit-approval")
                        .header("Authorization", "Bearer " + qsToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_APPROVAL"));

        Long fsWfId = getLatestWorkflowId(qsToken, "FINAL_SETTLEMENT", settlementId);
        approveStep(fsWfId, qsToken, "Final account quantities verified");
        approveStep(fsWfId, finToken, "Retainage and deductions reconciled");
        approveStep(fsWfId, execToken, "Executive final settlement approval granted");

        // Verify contract is now locked in SETTLED status
        mockMvc.perform(get("/api/v1/contracts/" + contractId)
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"));

        // Verify locked contract rejects new Change Orders or Progress Payments with 422
        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/change-orders")
                        .header("Authorization", "Bearer " + cmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateChangeOrderRequest(
                                "CO-SOL-002",
                                ChangeOrderType.SCOPE_ADDITION,
                                "Late Change Order after Settlement",
                                "Should be blocked",
                                new BigDecimal("10000.0000"),
                                0
                        ))))
                .andExpect(status().isUnprocessableEntity());

        // 8. Release Warranty Retainage after DLP -> FinalSettlement becomes WARRANTY_RELEASED and Contract becomes CLOSED
        mockMvc.perform(post("/api/v1/settlements/" + settlementId + "/release-warranty-retainage")
                        .header("Authorization", "Bearer " + finToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WARRANTY_RELEASED"))
                .andExpect(jsonPath("$.warrantyRetainageHeld").value(0.0))
                .andExpect(jsonPath("$.retainageReleasedNow").value(50000.0));

        mockMvc.perform(get("/api/v1/contracts/" + contractId)
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // 9. Verify Project Financial Summary Report
        // Approved Budget = $10,000,000; Original Contract Gross = $1,100,000; Approved Change Gross = $110,000
        // Current Committed Gross = $1,210,000; Remaining Uncommitted Budget = $8,790,000
        // Certified Payments Gross = $1,000,000; Paid Net = $950,000; Retainage Held = $50,000
        mockMvc.perform(get("/api/v1/reports/projects/" + projectId + "/financial-summary")
                        .header("Authorization", "Bearer " + auditorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectCode").value("PRJ-SOLAR-04"))
                .andExpect(jsonPath("$.approvedBudget").value(10000000.0))
                .andExpect(jsonPath("$.totalContracts").value(1))
                .andExpect(jsonPath("$.settledOrClosedContracts").value(1))
                .andExpect(jsonPath("$.totalOriginalContractGross").value(1100000.0))
                .andExpect(jsonPath("$.totalApprovedChangeOrdersGross").value(110000.0))
                .andExpect(jsonPath("$.totalCurrentCommittedGross").value(1210000.0))
                .andExpect(jsonPath("$.remainingUncommittedBudget").value(8790000.0))
                .andExpect(jsonPath("$.totalCertifiedPaymentsGross").value(1000000.0))
                .andExpect(jsonPath("$.totalPaidNetAmount").value(950000.0))
                .andExpect(jsonPath("$.totalRetainageHeld").value(50000.0));

        // 10. Verify Audit Log Explorer API
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + auditorToken)
                        .param("module", "SETTLEMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)));
    }

    private String login(String username) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, TEST_BOOTSTRAP_PASS))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private Long getLatestWorkflowId(String token, String targetType, Long targetId) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/approvals/by-target")
                        .header("Authorization", "Bearer " + token)
                        .param("targetType", targetType)
                        .param("targetId", String.valueOf(targetId)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get(0).get("id").asLong();
    }

    private void approveStep(Long workflowId, String token, String comment) throws Exception {
        mockMvc.perform(post("/api/v1/approvals/" + workflowId + "/decision")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApprovalDecisionRequest(ApprovalDecisionAction.APPROVE, comment))))
                .andExpect(status().isOk());
    }
}
