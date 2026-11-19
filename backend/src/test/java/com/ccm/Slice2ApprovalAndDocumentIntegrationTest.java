package com.ccm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ccm.approval.api.ApprovalDtos.ApprovalDecisionRequest;
import com.ccm.approval.domain.ApprovalDecisionAction;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
class Slice2ApprovalAndDocumentIntegrationTest {

    private static final String TEST_BOOTSTRAP_PASS = "TestOnly-Bootstrap-Secret-2026!";

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("ccm_test_s2")
            .withUsername("ccm_test_user")
            .withPassword("ccm_test_pwd");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("ccm.bootstrap.enabled", () -> "true");
        registry.add("ccm.bootstrap.admin-password", () -> TEST_BOOTSTRAP_PASS);
        registry.add("ccm.storage.local-path", () -> "./target/test-ccm-docs-slice2");
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
    void shouldEnforceMultiStepApprovalWorkflowContractImmutabilityAndVersionedDocuments() throws Exception {
        String pmToken = login("pm.alex");
        String cmToken = login("cm.elena");
        String qsToken = login("qs.david");
        String execToken = login("exec.victor");

        Long hqOrgId = organizationRepository.findByCode("ORG-HQ").orElseThrow().getId();

        // 1. Create Project & assign cm.elena (CONTRACT_MANAGER) and qs.david (COST_ENGINEER)
        CreateProjectRequest projReq = new CreateProjectRequest(
                "PRJ-TUNNEL-02",
                "Cross-Harbor Immersed Tube Tunnel",
                hqOrgId,
                "East Bay Corridor",
                "USD",
                new BigDecimal("250000000.0000"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2030, 3, 31),
                ProjectStatus.ACTIVE,
                "Immersed tunnel engineering & construction"
        );
        MvcResult projRes = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projReq)))
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
        CreatePartnerRequest partnerReq = new CreatePartnerRequest(
                "BP-TUNNEL-01",
                "Atlas Tunneling Consortium LLC",
                PartnerType.GENERAL_CONTRACTOR,
                "TAX-TUNNEL-2026",
                "Grade-AAA Underground Works",
                "Dr. Henrik Lindqvist",
                "+1-415-555-0800",
                "h.lindqvist@atlastunnel.example",
                "Citibank Corporate",
                "8877665544",
                PartnerStatus.ACTIVE
        );
        MvcResult partnerRes = mockMvc.perform(post("/api/v1/partners")
                        .header("Authorization", "Bearer " + cmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partnerReq)))
                .andExpect(status().isCreated())
                .andReturn();
        Long partnerId = objectMapper.readTree(partnerRes.getResponse().getContentAsString()).get("id").asLong();

        // 3. Create High-Value Contract ($10,000,000 net + 10% tax = $11,000,000 gross >= $5M threshold)
        CreateContractRequest contractReq = new CreateContractRequest(
                "CNT-TUN-2026-001",
                "TBM Boring & Precast Segment Lining Package",
                projectId,
                partnerId,
                null,
                ContractCategory.MAIN_CONTRACT,
                new BigDecimal("0.1000"),
                new BigDecimal("0.0500"),
                new BigDecimal("0.1000"),
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2029, 5, 1),
                24,
                "Twin-bore TBM excavation and precast segments",
                List.of(new ContractLineItemRequest("TBM-01", "Tunnel Boring & Segment Installation", "m",
                        new BigDecimal("2000.0000"), new BigDecimal("5000.0000")))
        );

        MvcResult contractRes = mockMvc.perform(post("/api/v1/contracts")
                        .header("Authorization", "Bearer " + cmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalAmountGross").value(11000000.0))
                .andReturn();
        Long contractId = objectMapper.readTree(contractRes.getResponse().getContentAsString()).get("id").asLong();

        // 4. Submit contract for approval -> creates 3-step workflow (QS -> PM -> EXEC)
        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/submit-approval")
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_APPROVAL"));

        MvcResult wfListRes = mockMvc.perform(get("/api/v1/approvals/by-target")
                        .header("Authorization", "Bearer " + cmToken)
                        .param("targetType", "CONTRACT")
                        .param("targetId", String.valueOf(contractId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].steps.length()").value(3))
                .andReturn();
        Long workflowId1 = objectMapper.readTree(wfListRes.getResponse().getContentAsString()).get(0).get("id").asLong();

        // 5. Reject Step 1 as Cost Engineer -> contract returns to DRAFT
        mockMvc.perform(post("/api/v1/approvals/" + workflowId1 + "/decision")
                        .header("Authorization", "Bearer " + qsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApprovalDecisionRequest(ApprovalDecisionAction.REJECT, "Please clarify segment waterproofing rate"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/v1/contracts/" + contractId)
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        // 6. Re-submit contract for approval and complete all 3 approval steps
        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/submit-approval")
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_APPROVAL"));

        MvcResult wfListRes2 = mockMvc.perform(get("/api/v1/approvals/by-target")
                        .header("Authorization", "Bearer " + cmToken)
                        .param("targetType", "CONTRACT")
                        .param("targetId", String.valueOf(contractId)))
                .andExpect(status().isOk())
                .andReturn();
        Long workflowId2 = objectMapper.readTree(wfListRes2.getResponse().getContentAsString()).get(0).get("id").asLong();

        // Wrong role check: PM tries to approve Step 1 (which requires ROLE_COST_ENGINEER) -> 403 Forbidden
        mockMvc.perform(post("/api/v1/approvals/" + workflowId2 + "/decision")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApprovalDecisionRequest(ApprovalDecisionAction.APPROVE, "Premature PM approval"))))
                .andExpect(status().isForbidden());

        // Step 1: Cost Engineer approves
        mockMvc.perform(post("/api/v1/approvals/" + workflowId2 + "/decision")
                        .header("Authorization", "Bearer " + qsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApprovalDecisionRequest(ApprovalDecisionAction.APPROVE, "BoQ rates verified against estimate"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStepOrder").value(2));

        // Step 2: Project Manager approves
        mockMvc.perform(post("/api/v1/approvals/" + workflowId2 + "/decision")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApprovalDecisionRequest(ApprovalDecisionAction.APPROVE, "Endorsed for execution"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStepOrder").value(3));

        // Step 3: Executive Approver approves -> workflow APPROVED, contract APPROVED
        mockMvc.perform(post("/api/v1/approvals/" + workflowId2 + "/decision")
                        .header("Authorization", "Bearer " + execToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApprovalDecisionRequest(ApprovalDecisionAction.APPROVE, "Executive board authorization granted"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/v1/contracts/" + contractId)
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 7. Activate the approved contract
        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/activate")
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 8. Verify strict financial immutability: attempting PUT /api/v1/contracts/{id} on ACTIVE contract returns 422
        UpdateContractRequest illegalUpdate = new UpdateContractRequest(
                "Unauthorized Direct Modification",
                partnerId,
                null,
                ContractCategory.MAIN_CONTRACT,
                new BigDecimal("0.1000"),
                new BigDecimal("0.0500"),
                new BigDecimal("0.1000"),
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2029, 5, 1),
                24,
                "Tampered scope",
                List.of(new ContractLineItemRequest("TBM-01", "Tampered", "m",
                        new BigDecimal("2000.0000"), new BigDecimal("9999.0000")))
        );

        mockMvc.perform(put("/api/v1/contracts/" + contractId)
                        .header("Authorization", "Bearer " + cmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalUpdate)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                        "Approved or active financial records cannot be modified directly")));

        // 9. Upload two versions of a signed contract PDF and verify version incrementing & SHA-256 download
        byte[] v1Bytes = "Signed Contract PDF Content Revision 1".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile fileV1 = new MockMultipartFile(
                "file",
                "CNT-TUN-2026-001-Signed.pdf",
                "application/pdf",
                v1Bytes
        );

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(fileV1)
                        .param("projectId", String.valueOf(projectId))
                        .param("ownerType", "CONTRACT")
                        .param("ownerId", String.valueOf(contractId))
                        .param("documentType", "SIGNED_CONTRACT")
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNo").value(1))
                .andExpect(jsonPath("$.sha256Checksum").isNotEmpty());

        byte[] v2Bytes = "Signed Contract PDF Content Revision 2 (With Addendum)".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile fileV2 = new MockMultipartFile(
                "file",
                "CNT-TUN-2026-001-Signed.pdf",
                "application/pdf",
                v2Bytes
        );

        MvcResult uploadV2Res = mockMvc.perform(multipart("/api/v1/documents")
                        .file(fileV2)
                        .param("projectId", String.valueOf(projectId))
                        .param("ownerType", "CONTRACT")
                        .param("ownerId", String.valueOf(contractId))
                        .param("documentType", "SIGNED_CONTRACT")
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNo").value(2))
                .andReturn();
        JsonNode docV2Node = objectMapper.readTree(uploadV2Res.getResponse().getContentAsString());
        Long docV2Id = docV2Node.get("id").asLong();
        String expectedSha256 = docV2Node.get("sha256Checksum").asText();

        MvcResult downloadRes = mockMvc.perform(get("/api/v1/documents/" + docV2Id + "/download")
                        .header("Authorization", "Bearer " + cmToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Document-SHA256", expectedSha256))
                .andReturn();
        assertThat(downloadRes.getResponse().getContentAsByteArray()).isEqualTo(v2Bytes);
    }

    private String login(String username) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, TEST_BOOTSTRAP_PASS))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
