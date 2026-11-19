package com.ccm.contract.application;

import com.ccm.approval.application.ApprovalApplicationService;
import com.ccm.approval.domain.ApprovalTargetType;
import com.ccm.approval.domain.ApprovalWorkflowCompletedEvent;
import com.ccm.approval.domain.ApprovalWorkflowStatus;
import com.ccm.audit.application.AuditTrailService;
import com.ccm.common.api.PageResponse;
import com.ccm.common.exception.DuplicateResourceException;
import com.ccm.common.exception.ResourceNotFoundException;
import com.ccm.common.security.SecurityContextUtils;
import com.ccm.contract.api.ContractDtos.ContractLineItemRequest;
import com.ccm.contract.api.ContractDtos.ContractLineItemResponse;
import com.ccm.contract.api.ContractDtos.ContractResponse;
import com.ccm.contract.api.ContractDtos.CreateContractRequest;
import com.ccm.contract.api.ContractDtos.UpdateContractRequest;
import com.ccm.contract.domain.Contract;
import com.ccm.contract.domain.ContractCategory;
import com.ccm.contract.domain.ContractLineItem;
import com.ccm.contract.domain.ContractStatus;
import com.ccm.contract.infrastructure.ContractRepository;
import com.ccm.partner.domain.BusinessPartner;
import com.ccm.partner.infrastructure.BusinessPartnerRepository;
import com.ccm.project.application.ProjectAccessGuard;
import com.ccm.project.domain.Project;
import com.ccm.project.infrastructure.ProjectRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractApplicationService {

    private final ContractRepository contractRepository;
    private final ProjectRepository projectRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final ApprovalApplicationService approvalApplicationService;
    private final AuditTrailService auditTrailService;

    public ContractApplicationService(
            ContractRepository contractRepository,
            ProjectRepository projectRepository,
            BusinessPartnerRepository businessPartnerRepository,
            ProjectAccessGuard projectAccessGuard,
            ApprovalApplicationService approvalApplicationService,
            AuditTrailService auditTrailService
    ) {
        this.contractRepository = contractRepository;
        this.projectRepository = projectRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.projectAccessGuard = projectAccessGuard;
        this.approvalApplicationService = approvalApplicationService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public ContractResponse createContract(CreateContractRequest request) {
        projectAccessGuard.assertCanManageProjectContracts(request.projectId());

        String contractNo = request.contractNo().trim().toUpperCase();
        if (contractRepository.existsByContractNo(contractNo)) {
            throw new DuplicateResourceException("Contract number already exists: " + contractNo);
        }

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + request.projectId()));
        BusinessPartner partner = businessPartnerRepository.findById(request.partnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Business partner not found: " + request.partnerId()));

        Contract parentContract = null;
        if (request.parentContractId() != null) {
            parentContract = contractRepository.findById(request.parentContractId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent contract not found: " + request.parentContractId()));
        }

        List<ContractLineItem> items = buildLineItems(request.lineItems());
        String actor = SecurityContextUtils.getCurrentUsername();

        Contract contract = new Contract(
                contractNo,
                request.title(),
                project,
                partner,
                parentContract,
                request.category(),
                request.taxRate(),
                request.retainageRate(),
                request.advancePaymentRate(),
                request.signedDate(),
                request.startDate(),
                request.endDate(),
                request.defectLiabilityMonths(),
                request.scopeSummary(),
                items,
                actor
        );
        Contract saved = contractRepository.saveAndFlush(contract);

        auditTrailService.record(
                "CONTRACT",
                "Contract",
                saved.getId(),
                "CONTRACT_CREATED",
                "Created draft contract " + saved.getContractNo() + " on project " + project.getProjectCode()
                        + " with gross value " + saved.getOriginalAmountGross(),
                Map.of(
                        "contractNo", saved.getContractNo(),
                        "projectId", project.getId(),
                        "partnerId", partner.getId(),
                        "originalAmountNet", saved.getOriginalAmountNet(),
                        "originalAmountGross", saved.getOriginalAmountGross(),
                        "status", saved.getStatus().name()
                )
        );

        return toDetailResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ContractResponse> listContracts(
            Long projectId,
            Long partnerId,
            ContractCategory category,
            ContractStatus status,
            String keyword,
            Pageable pageable
    ) {
        if (projectId != null) {
            projectAccessGuard.assertCanViewProject(projectId);
        }
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<Contract> page;
        if (projectAccessGuard.hasGlobalReadAccess()) {
            page = contractRepository.searchAllContracts(projectId, partnerId, category, status, normalizedKeyword, pageable);
        } else {
            Long currentUserId = projectAccessGuard.resolveCurrentUserId();
            if (currentUserId == null) {
                page = Page.empty(pageable);
            } else {
                page = contractRepository.searchMemberContracts(
                        currentUserId, projectId, partnerId, category, status, normalizedKeyword, pageable
                );
            }
        }
        return PageResponse.from(page.map(this::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    public ContractResponse getContract(Long id) {
        Contract contract = contractRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + id));
        projectAccessGuard.assertCanViewProject(contract.getProject().getId());
        return toDetailResponse(contract);
    }

    @Transactional
    public ContractResponse updateDraftContract(Long id, UpdateContractRequest request) {
        Contract contract = contractRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + id));
        projectAccessGuard.assertCanManageProjectContracts(contract.getProject().getId());

        BusinessPartner partner = businessPartnerRepository.findById(request.partnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Business partner not found: " + request.partnerId()));

        Contract parentContract = null;
        if (request.parentContractId() != null) {
            parentContract = contractRepository.findById(request.parentContractId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent contract not found: " + request.parentContractId()));
        }

        List<ContractLineItem> items = buildLineItems(request.lineItems());

        contract.updateDraft(
                request.title(),
                partner,
                parentContract,
                request.category(),
                request.taxRate(),
                request.retainageRate(),
                request.advancePaymentRate(),
                request.signedDate(),
                request.startDate(),
                request.endDate(),
                request.defectLiabilityMonths(),
                request.scopeSummary(),
                items
        );
        Contract saved = contractRepository.saveAndFlush(contract);

        auditTrailService.record(
                "CONTRACT",
                "Contract",
                saved.getId(),
                "CONTRACT_DRAFT_UPDATED",
                "Updated draft contract " + saved.getContractNo() + " (gross=" + saved.getOriginalAmountGross() + ")",
                Map.of(
                        "contractNo", saved.getContractNo(),
                        "originalAmountNet", saved.getOriginalAmountNet(),
                        "originalAmountGross", saved.getOriginalAmountGross(),
                        "lineItemCount", saved.getLineItems().size()
                )
        );

        return toDetailResponse(saved);
    }

    @Transactional
    public ContractResponse submitForApproval(Long id) {
        Contract contract = contractRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + id));
        projectAccessGuard.assertCanManageProjectContracts(contract.getProject().getId());

        contract.submitForApproval();
        Contract saved = contractRepository.saveAndFlush(contract);

        approvalApplicationService.startWorkflow(
                ApprovalTargetType.CONTRACT,
                saved.getId(),
                saved.getProject().getId(),
                saved.getContractNo(),
                saved.getTitle(),
                saved.getOriginalAmountGross()
        );

        auditTrailService.record(
                "CONTRACT",
                "Contract",
                saved.getId(),
                "CONTRACT_SUBMITTED_FOR_APPROVAL",
                "Submitted contract " + saved.getContractNo() + " for multi-step approval",
                Map.of("contractNo", saved.getContractNo(), "status", saved.getStatus().name())
        );

        return toDetailResponse(saved);
    }

    @Transactional
    public ContractResponse activateContract(Long id) {
        Contract contract = contractRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + id));
        projectAccessGuard.assertCanManageProjectContracts(contract.getProject().getId());

        contract.activate();
        Contract saved = contractRepository.saveAndFlush(contract);

        auditTrailService.record(
                "CONTRACT",
                "Contract",
                saved.getId(),
                "CONTRACT_ACTIVATED",
                "Activated approved contract " + saved.getContractNo(),
                Map.of("contractNo", saved.getContractNo(), "status", saved.getStatus().name())
        );

        return toDetailResponse(saved);
    }

    @EventListener
    @Transactional
    public void onApprovalWorkflowCompleted(ApprovalWorkflowCompletedEvent event) {
        if (event.targetType() != ApprovalTargetType.CONTRACT) {
            return;
        }
        Contract contract = contractRepository.findWithDetailsById(event.targetId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + event.targetId()));

        if (event.finalStatus() == ApprovalWorkflowStatus.APPROVED) {
            contract.markApproved();
            contractRepository.saveAndFlush(contract);
            auditTrailService.record(
                    "CONTRACT",
                    "Contract",
                    contract.getId(),
                    "CONTRACT_APPROVED",
                    "Contract " + contract.getContractNo() + " approved via workflow #" + event.workflowId(),
                    Map.of("contractNo", contract.getContractNo(), "workflowId", event.workflowId())
            );
        } else if (event.finalStatus() == ApprovalWorkflowStatus.REJECTED) {
            contract.returnToDraftOnRejection();
            contractRepository.saveAndFlush(contract);
            auditTrailService.record(
                    "CONTRACT",
                    "Contract",
                    contract.getId(),
                    "CONTRACT_APPROVAL_REJECTED",
                    "Contract " + contract.getContractNo() + " returned to DRAFT after rejection on workflow #" + event.workflowId(),
                    Map.of("contractNo", contract.getContractNo(), "workflowId", event.workflowId())
            );
        }
    }

    private List<ContractLineItem> buildLineItems(List<ContractLineItemRequest> itemRequests) {
        List<ContractLineItem> items = new ArrayList<>();
        int index = 1;
        for (ContractLineItemRequest req : itemRequests) {
            items.add(new ContractLineItem(
                    req.itemCode(),
                    req.description(),
                    req.unit(),
                    req.quantity(),
                    req.unitPriceNet(),
                    index++
            ));
        }
        return items;
    }

    private ContractResponse toSummaryResponse(Contract c) {
        return buildResponse(c, List.of());
    }

    private ContractResponse toDetailResponse(Contract c) {
        List<ContractLineItemResponse> items = c.getLineItems().stream()
                .map(li -> new ContractLineItemResponse(
                        li.getId(),
                        li.getItemCode(),
                        li.getDescription(),
                        li.getUnit(),
                        li.getQuantity(),
                        li.getUnitPriceNet(),
                        li.getTotalPriceNet(),
                        li.getSortOrder()
                ))
                .toList();
        return buildResponse(c, items);
    }

    private ContractResponse buildResponse(Contract c, List<ContractLineItemResponse> items) {
        Contract parent = c.getParentContract();
        return new ContractResponse(
                c.getId(),
                c.getContractNo(),
                c.getTitle(),
                c.getProject().getId(),
                c.getProject().getProjectCode(),
                c.getProject().getName(),
                c.getPartner().getId(),
                c.getPartner().getPartnerCode(),
                c.getPartner().getName(),
                parent != null ? parent.getId() : null,
                parent != null ? parent.getContractNo() : null,
                c.getCategory(),
                c.getCurrency(),
                c.getOriginalAmountNet(),
                c.getTaxRate(),
                c.getOriginalAmountTax(),
                c.getOriginalAmountGross(),
                c.getApprovedChangeNet(),
                c.getApprovedChangeGross(),
                c.getCurrentAmountNet(),
                c.getCurrentAmountGross(),
                c.getRetainageRate(),
                c.getAdvancePaymentRate(),
                c.getSignedDate(),
                c.getStartDate(),
                c.getEndDate(),
                c.getDefectLiabilityMonths(),
                c.getStatus(),
                c.getScopeSummary(),
                items,
                c.getVersion(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getCreatedBy()
        );
    }
}
