package com.ccm.partner.application;

import com.ccm.audit.application.AuditTrailService;
import com.ccm.common.api.PageResponse;
import com.ccm.common.exception.DuplicateResourceException;
import com.ccm.common.exception.ResourceNotFoundException;
import com.ccm.common.security.SecurityContextUtils;
import com.ccm.partner.api.PartnerDtos.CreatePartnerRequest;
import com.ccm.partner.api.PartnerDtos.PartnerResponse;
import com.ccm.partner.api.PartnerDtos.UpdatePartnerRequest;
import com.ccm.partner.domain.BusinessPartner;
import com.ccm.partner.domain.PartnerStatus;
import com.ccm.partner.domain.PartnerType;
import com.ccm.partner.infrastructure.BusinessPartnerRepository;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessPartnerApplicationService {

    private final BusinessPartnerRepository businessPartnerRepository;
    private final AuditTrailService auditTrailService;

    public BusinessPartnerApplicationService(
            BusinessPartnerRepository businessPartnerRepository,
            AuditTrailService auditTrailService
    ) {
        this.businessPartnerRepository = businessPartnerRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public PartnerResponse createPartner(CreatePartnerRequest request) {
        String code = request.partnerCode().trim().toUpperCase();
        String taxNo = request.taxRegistrationNo().trim().toUpperCase();
        if (businessPartnerRepository.existsByPartnerCode(code)) {
            throw new DuplicateResourceException("Partner code already exists: " + code);
        }
        if (businessPartnerRepository.existsByTaxRegistrationNo(taxNo)) {
            throw new DuplicateResourceException("Tax registration number already exists: " + taxNo);
        }

        BusinessPartner partner = new BusinessPartner(
                code,
                request.name(),
                request.partnerType(),
                taxNo,
                request.qualificationLevel(),
                request.contactPerson(),
                request.contactPhone(),
                request.contactEmail(),
                request.bankName(),
                request.bankAccountNo(),
                request.status(),
                SecurityContextUtils.getCurrentUsername()
        );
        BusinessPartner saved = businessPartnerRepository.saveAndFlush(partner);

        auditTrailService.record(
                "PARTNER",
                "BusinessPartner",
                saved.getId(),
                "PARTNER_CREATED",
                "Registered business partner " + saved.getPartnerCode() + " (" + saved.getName() + ")",
                Map.of(
                        "partnerCode", saved.getPartnerCode(),
                        "partnerType", saved.getPartnerType().name(),
                        "status", saved.getStatus().name()
                )
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<PartnerResponse> listPartners(
            PartnerType partnerType,
            PartnerStatus status,
            String keyword,
            Pageable pageable
    ) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<BusinessPartner> page = businessPartnerRepository.searchPartners(
                partnerType,
                status,
                normalizedKeyword,
                pageable
        );
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PartnerResponse getPartner(Long id) {
        BusinessPartner partner = businessPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business partner not found: " + id));
        return toResponse(partner);
    }

    @Transactional
    public PartnerResponse updatePartner(Long id, UpdatePartnerRequest request) {
        BusinessPartner partner = businessPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business partner not found: " + id));

        partner.updateDetails(
                request.name(),
                request.partnerType(),
                request.qualificationLevel(),
                request.contactPerson(),
                request.contactPhone(),
                request.contactEmail(),
                request.bankName(),
                request.bankAccountNo(),
                request.status()
        );
        BusinessPartner saved = businessPartnerRepository.saveAndFlush(partner);

        auditTrailService.record(
                "PARTNER",
                "BusinessPartner",
                saved.getId(),
                "PARTNER_UPDATED",
                "Updated business partner " + saved.getPartnerCode() + " (status=" + saved.getStatus() + ")",
                Map.of(
                        "partnerCode", saved.getPartnerCode(),
                        "partnerType", saved.getPartnerType().name(),
                        "status", saved.getStatus().name()
                )
        );

        return toResponse(saved);
    }

    private PartnerResponse toResponse(BusinessPartner p) {
        return new PartnerResponse(
                p.getId(),
                p.getPartnerCode(),
                p.getName(),
                p.getPartnerType(),
                p.getTaxRegistrationNo(),
                p.getQualificationLevel(),
                p.getContactPerson(),
                p.getContactPhone(),
                p.getContactEmail(),
                p.getBankName(),
                p.getBankAccountNo(),
                p.getStatus(),
                p.getVersion(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                p.getCreatedBy()
        );
    }
}
