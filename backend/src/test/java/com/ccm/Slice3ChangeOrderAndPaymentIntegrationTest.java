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
class Slice3ChangeOrderAndPaymentIntegrationTest {

    private static final String TEST_BOOTSTRAP_PASS = "TestOnly-Bootstrap-Secret-2026!";

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("ccm_test_s3")
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
    void shouldProcessApprovedChangeOrdersAndSequentialProgressPaymentsWithRetainageAndOverpaymentProtection()
            throws Exception {
        String pmToken = login("pm.alex");
        String cmToken = login("cm.elena");
        String qsToken = login("qs.david");
        String finToken = login("fin.sarah");

        Long hqOrgId = organizationRepository.findByCode("ORG-HQ").orElseThrow().getId();

        // 1. Create Project and assign CM and QS
        MvcResult projRes = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProjectRequest(
                                "PRJ-AIRPORT-03",
                                "International Airport Terminal 3 Expansion",
                                hqOrgId,
                                "Airport Zone",
                                "USD",
                                new BigDecimal("80000000.0000"),
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2028, 12, 31),
                                ProjectStatus.ACTIVE,
                                "Terminal 3 concourse steel & MEP"
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
                                "BP-MEP-301",
                                "Vanguard MEP Systems Corp.",
                                PartnerType.SUBCONTRACTOR,
                                "TAX-MEP-301",
                                "Class-A HVAC & Electrical",
                                "Rachel Zane",
                                "+1-555-3010",
                                "r.zane@vanguardmep.example",
                                "JPMorgan Chase",
                                "301301301",
                                PartnerStatus.ACTIVE
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long partnerId = objectMapper.readTree(partnerRes.getResponse().getContentAsString()).get("id").asLong();

        // 3. Create Contract ($2,000,000 net + 10% tax = $2,200,000 gross; 5% retainage), approve and activate
        MvcResult contractRes = mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + cmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateContractRequest(
                                "CNT-AIR-2026-001",
                                "Terminal 3 Concourse HVAC & Smoke Extraction System",
                                projectId,
                                partnerId,
                                null,
                                ContractCategory.SUBCONTRACT,
                                new BigDecimal("0.1000"),
                                new BigDecimal("0.0500"),
                                new BigDecimal("0.0500"),
                                LocalDate.of(2026, 2, 1),
                                LocalDate.of(2026, 2, 15),
                                LocalDate.of(2027, 11, 30),
                                24,
                                "HVAC chillers, ductwork, and BMS integration",
                                List.of(new ContractLineItemRequest("MEP-01", "Chiller Plant & Ductwork", "lump",
                                        new BigDecimal("1.0000"), new BigDecimal("2000000.0000")))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalAmountGross").value(2200000.0))
                .andReturn();
        Long contractId = objectMapper.readTree(contractRes.getResponse().getContentAsString()).get("id").asLong();

        // Submit, approve (2 steps: QS -> PM since < $5M), and activate
        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/submit-approval")
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk());
        Long contractWfId = getLatestWorkflowId(cmToken, "CONTRACT", contractId);
        approveStep(contractWfId, qsToken, "QS verified");
        approveStep(contractWfId, pmToken, "PM approved");

        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/activate")
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 4. Create & Approve Change Order (+$200,000 net + 10% tax = +$220,000 gross)
        CreateChangeOrderRequest coReq = new CreateChangeOrderRequest(
                "CO-AIR-001",
                ChangeOrderType.SCOPE_ADDITION,
                "Additional Cleanroom HEPA Filtration Units",
                "Airport authority medical quarantine zone requirement",
                new BigDecimal("200000.0000"),
                15
        );
        MvcResult coRes = mockMvc.perform(post("/api/v1/contracts/" + contractId + "/change-orders")
                        .header("Authorization", "Bearer " + cmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(coReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountDeltaNet").value(200000.0))
                .andExpect(jsonPath("$.amountDeltaGross").value(220000.0))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        Long coId = objectMapper.readTree(coRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/change-orders/" + coId + "/submit-approval")
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_APPROVAL"));

        Long coWfId = getLatestWorkflowId(cmToken, "CHANGE_ORDER", coId);
        approveStep(coWfId, qsToken, "Change order priced fairly");
        approveStep(coWfId, pmToken, "Change order approved");

        // Verify Contract currentAmountGross is now $2,200,000 + $220,000 = $2,420,000
        mockMvc.perform(get("/api/v1/contracts/" + contractId)
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedChangeNet").value(200000.0))
                .andExpect(jsonPath("$.approvedChangeGross").value(220000.0))
                .andExpect(jsonPath("$.currentAmountNet").value(2200000.0))
                .andExpect(jsonPath("$.currentAmountGross").value(2420000.0));

        // 5. Create Payment Application #1 (IPC-001): Cumulative = $1,000,000 gross
        // Previous certified = $0 -> Current Period = $1,000,000 -> Retainage (5%) = $50,000 -> Advance ded = $50,000 -> Net Payable = $900,000
        CreatePaymentApplicationRequest ipc1Req = new CreatePaymentApplicationRequest(
                "IPC-AIR-001",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                new BigDecimal("1000000.0000"),
                new BigDecimal("50000.0000"),
                BigDecimal.ZERO,
                "March 2026 progress measurement"
        );
        MvcResult ipc1Res = mockMvc.perform(post("/api/v1/contracts/" + contractId + "/payments")
                        .header("Authorization", "Bearer " + qsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ipc1Req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.previousCertifiedGross").value(0.0))
                .andExpect(jsonPath("$.currentPeriodGross").value(1000000.0))
                .andExpect(jsonPath("$.retainageWithheld").value(50000.0))
                .andExpect(jsonPath("$.netPayableAmount").value(900000.0))
                .andReturn();
        Long ipc1Id = objectMapper.readTree(ipc1Res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/payments/" + ipc1Id + "/submit-approval")
                        .header("Authorization", "Bearer " + qsToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_APPROVAL"));

        Long ipc1WfId = getLatestWorkflowId(qsToken, "PAYMENT_APPLICATION", ipc1Id);
        approveStep(ipc1WfId, qsToken, "Measured quantities certified");
        approveStep(ipc1WfId, finToken, "Retainage & tax verified");

        // Finance Officer records bank payment disbursement
        mockMvc.perform(post("/api/v1/payments/" + ipc1Id + "/mark-paid")
                        .header("Authorization", "Bearer " + finToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarkPaymentPaidRequest("WIRE-20260405-9901"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentReference").value("WIRE-20260405-9901"));

        // 6. Create Payment Application #2 (IPC-002): Cumulative = $2,000,000 gross
        // Server automatically sets previousCertifiedGross = $1,000,000 -> Current Period = $1,000,000 -> Retainage (5%) = $50,000 -> Net Payable = $950,000
        CreatePaymentApplicationRequest ipc2Req = new CreatePaymentApplicationRequest(
                "IPC-AIR-002",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                new BigDecimal("2000000.0000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "April 2026 progress measurement"
        );
        MvcResult ipc2Res = mockMvc.perform(post("/api/v1/contracts/" + contractId + "/payments")
                        .header("Authorization", "Bearer " + qsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ipc2Req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.previousCertifiedGross").value(1000000.0))
                .andExpect(jsonPath("$.currentPeriodGross").value(1000000.0))
                .andExpect(jsonPath("$.retainageWithheld").value(50000.0))
                .andExpect(jsonPath("$.netPayableAmount").value(950000.0))
                .andReturn();
        Long ipc2Id = objectMapper.readTree(ipc2Res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/payments/" + ipc2Id + "/submit-approval")
                        .header("Authorization", "Bearer " + qsToken))
                .andExpect(status().isOk());
        Long ipc2WfId = getLatestWorkflowId(qsToken, "PAYMENT_APPLICATION", ipc2Id);
        approveStep(ipc2WfId, qsToken, "April IPC certified");
        approveStep(ipc2WfId, finToken, "Approved for payment");

        // 7. Over-certification protection: attempt IPC-003 with cumulative = $2,500,000 (> current contract gross $2,420,000) -> 422
        CreatePaymentApplicationRequest overPayReq = new CreatePaymentApplicationRequest(
                "IPC-AIR-003",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("2500000.0000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Over-billed claim"
        );
        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/payments")
                        .header("Authorization", "Bearer " + qsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overPayReq)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                        "cannot exceed current contract gross value")));
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
