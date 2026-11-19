package com.ccm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ccm.audit.infrastructure.AuditLogRepository;
import com.ccm.contract.api.ContractDtos.ContractLineItemRequest;
import com.ccm.contract.api.ContractDtos.CreateContractRequest;
import com.ccm.contract.api.ContractDtos.UpdateContractRequest;
import com.ccm.contract.domain.ContractCategory;
import com.ccm.iam.api.IamDtos.LoginRequest;
import com.ccm.iam.domain.User;
import com.ccm.iam.infrastructure.OrganizationRepository;
import com.ccm.iam.infrastructure.UserRepository;
import com.ccm.partner.api.PartnerDtos.CreatePartnerRequest;
import com.ccm.partner.domain.PartnerStatus;
import com.ccm.partner.domain.PartnerType;
import com.ccm.project.api.ProjectDtos.AssignProjectMemberRequest;
import com.ccm.project.api.ProjectDtos.CreateProjectRequest;
import com.ccm.project.domain.ProjectRole;
import com.ccm.project.domain.ProjectStatus;
import com.fasterxml.jackson.databind.JsonNode;
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
class Slice1IntegrationTest {

    private static final String TEST_BOOTSTRAP_PASS = "TestOnly-Bootstrap-Secret-2026!";

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("ccm_test")
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

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void shouldExecuteFullSlice1FlowWithRoleAndResourceAuthorizationAndAuditTrail() throws Exception {
        // 1. Authenticate Project Manager (pm.alex) and Contract Manager (cm.elena)
        String pmToken = loginAndGetToken("pm.alex", TEST_BOOTSTRAP_PASS);
        String cmToken = loginAndGetToken("cm.elena", TEST_BOOTSTRAP_PASS);
        String auditorToken = loginAndGetToken("auditor.maya", TEST_BOOTSTRAP_PASS);

        // Verify /api/v1/auth/me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("pm.alex"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_PROJECT_MANAGER"));

        Long hqOrgId = organizationRepository.findByCode("ORG-HQ").orElseThrow().getId();

        // 2. Project Manager creates Project A (PRJ-BRIDGE-01)
        CreateProjectRequest createProjectA = new CreateProjectRequest(
                "PRJ-BRIDGE-01",
                "Harbor Suspension Bridge Phase 1",
                hqOrgId,
                "Harbor Bay Zone A",
                "USD",
                new BigDecimal("120000000.0000"),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2029, 6, 30),
                ProjectStatus.ACTIVE,
                "Main suspension bridge civil & steel works"
        );

        MvcResult projectAResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createProjectA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectCode").value("PRJ-BRIDGE-01"))
                .andExpect(jsonPath("$.approvedBudget").value(120000000.0))
                .andReturn();
        Long projectAId = objectMapper.readTree(projectAResult.getResponse().getContentAsString()).get("id").asLong();

        // 3. Verify resource-level isolation: cm.elena is NOT yet a member of Project A, so GET /api/v1/projects/{id} must return 403
        mockMvc.perform(get("/api/v1/projects/" + projectAId)
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isForbidden());

        // Auditor has global read access, so GET /api/v1/projects/{id} succeeds
        mockMvc.perform(get("/api/v1/projects/" + projectAId)
                        .header("Authorization", "Bearer " + auditorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectCode").value("PRJ-BRIDGE-01"));

        // 4. PM assigns cm.elena as CONTRACT_MANAGER on Project A
        User elena = userRepository.findByUsername("cm.elena").orElseThrow();
        AssignProjectMemberRequest assignElena = new AssignProjectMemberRequest(
                elena.getId(),
                ProjectRole.CONTRACT_MANAGER
        );

        mockMvc.perform(post("/api/v1/projects/" + projectAId + "/members")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignElena)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("cm.elena"))
                .andExpect(jsonPath("$.projectRole").value("CONTRACT_MANAGER"));

        // Now cm.elena can view Project A
        mockMvc.perform(get("/api/v1/projects/" + projectAId)
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectCode").value("PRJ-BRIDGE-01"));

        // 5. Contract Manager registers an ACTIVE Business Partner and a BLACKLISTED Business Partner
        CreatePartnerRequest activePartnerReq = new CreatePartnerRequest(
                "BP-GC-100",
                "Pacific Heavy Civil Constructors Inc.",
                PartnerType.GENERAL_CONTRACTOR,
                "US-EIN-88-7654321",
                "Class-AAA Highway & Bridge",
                "Marcus Sterling",
                "+1-415-555-0142",
                "m.sterling@pacificcivil.example",
                "Wells Commercial Bank",
                "990011223344",
                PartnerStatus.ACTIVE
        );
        MvcResult activePartnerRes = mockMvc.perform(post("/api/v1/partners")
                        .header("Authorization", "Bearer " + cmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activePartnerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partnerCode").value("BP-GC-100"))
                .andReturn();
        Long activePartnerId = objectMapper.readTree(activePartnerRes.getResponse().getContentAsString()).get("id").asLong();

        CreatePartnerRequest blacklistedPartnerReq = new CreatePartnerRequest(
                "BP-SUB-999",
                "Defaulted Piling Co.",
                PartnerType.SUBCONTRACTOR,
                "US-EIN-00-9999999",
                "Revoked",
                "N/A",
                "+1-415-555-0199",
                "info@defaultedpiling.example",
                null,
                null,
                PartnerStatus.BLACKLISTED
        );
        MvcResult blacklistedPartnerRes = mockMvc.perform(post("/api/v1/partners")
                        .header("Authorization", "Bearer " + cmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blacklistedPartnerReq)))
                .andExpect(status().isCreated())
                .andReturn();
        Long blacklistedPartnerId = objectMapper.readTree(blacklistedPartnerRes.getResponse().getContentAsString()).get("id").asLong();

        // 6. Contract Manager creates a Main Contract with BoQ line items
        CreateContractRequest createContractReq = new CreateContractRequest(
                "CNT-HB-2026-001",
                "Main Pylon & Caisson Foundation Construction",
                projectAId,
                activePartnerId,
                null,
                ContractCategory.MAIN_CONTRACT,
                new BigDecimal("0.0800"),
                new BigDecimal("0.0500"),
                new BigDecimal("0.1000"),
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 15),
                LocalDate.of(2028, 10, 31),
                24,
                "Marine caisson foundation and concrete towers",
                List.of(
                        new ContractLineItemRequest("BOQ-01", "Marine Cofferdam & Dredging", "m3",
                                new BigDecimal("50000.0000"), new BigDecimal("120.0000")),
                        new ContractLineItemRequest("BOQ-02", "High-Performance Marine Concrete", "m3",
                                new BigDecimal("20000.0000"), new BigDecimal("450.0000"))
                )
        );
        // Net = 50,000*120 (6,000,000) + 20,000*450 (9,000,000) = 15,000,000.0000
        // Tax @ 8% = 1,200,000.0000
        // Gross = 16,200,000.0000

        MvcResult contractRes = mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + cmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createContractReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contractNo").value("CNT-HB-2026-001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.originalAmountNet").value(15000000.0))
                .andExpect(jsonPath("$.originalAmountTax").value(1200000.0))
                .andExpect(jsonPath("$.originalAmountGross").value(16200000.0))
                .andExpect(jsonPath("$.lineItems.length()").value(2))
                .andReturn();
        Long contractId = objectMapper.readTree(contractRes.getResponse().getContentAsString()).get("id").asLong();

        // 7. Attempt to create a contract with the BLACKLISTED partner -> must fail with 422
        CreateContractRequest invalidPartnerContractReq = new CreateContractRequest(
                "CNT-HB-2026-002",
                "Subcontract Piling",
                projectAId,
                blacklistedPartnerId,
                contractId,
                ContractCategory.SUBCONTRACT,
                new BigDecimal("0.0800"),
                new BigDecimal("0.0500"),
                BigDecimal.ZERO,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 12, 31),
                12,
                "Piling",
                List.of(new ContractLineItemRequest("P-01", "Bored Piles", "m",
                        new BigDecimal("1000.0000"), new BigDecimal("500.0000")))
        );

        mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + cmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPartnerContractReq)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Business Rule Violation"));

        // 8. Update DRAFT contract and verify recalculated totals and optimistic lock version increment
        UpdateContractRequest updateDraftReq = new UpdateContractRequest(
                "Main Pylon & Caisson Foundation Construction (Rev 1)",
                activePartnerId,
                null,
                ContractCategory.MAIN_CONTRACT,
                new BigDecimal("0.0800"),
                new BigDecimal("0.0500"),
                new BigDecimal("0.1000"),
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 15),
                LocalDate.of(2028, 10, 31),
                24,
                "Updated marine caisson quantities",
                List.of(
                        new ContractLineItemRequest("BOQ-01", "Marine Cofferdam & Dredging", "m3",
                                new BigDecimal("50000.0000"), new BigDecimal("120.0000")),
                        new ContractLineItemRequest("BOQ-02", "High-Performance Marine Concrete", "m3",
                                new BigDecimal("22000.0000"), new BigDecimal("450.0000")),
                        new ContractLineItemRequest("BOQ-03", "Cathodic Corrosion Protection System", "set",
                                new BigDecimal("1.0000"), new BigDecimal("100000.0000"))
                )
        );
        // New Net = 6,000,000 + 9,900,000 + 100,000 = 16,000,000.0000
        // New Tax @ 8% = 1,280,000.0000
        // New Gross = 17,280,000.0000

        mockMvc.perform(put("/api/v1/contracts/" + contractId)
                        .header("Authorization", "Bearer " + cmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDraftReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Main Pylon & Caisson Foundation Construction (Rev 1)"))
                .andExpect(jsonPath("$.originalAmountNet").value(16000000.0))
                .andExpect(jsonPath("$.originalAmountTax").value(1280000.0))
                .andExpect(jsonPath("$.originalAmountGross").value(17280000.0))
                .andExpect(jsonPath("$.lineItems.length()").value(3))
                .andExpect(jsonPath("$.version").value(1));

        // 9. Verify audit trail entries were recorded for Project, Partner, and Contract actions
        assertThat(auditLogRepository.count()).isGreaterThanOrEqualTo(5);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("accessToken").asText();
    }
}
