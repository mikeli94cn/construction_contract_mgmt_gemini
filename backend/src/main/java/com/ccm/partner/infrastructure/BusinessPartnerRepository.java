package com.ccm.partner.infrastructure;

import com.ccm.partner.domain.BusinessPartner;
import com.ccm.partner.domain.PartnerStatus;
import com.ccm.partner.domain.PartnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessPartnerRepository extends JpaRepository<BusinessPartner, Long> {

    boolean existsByPartnerCode(String partnerCode);

    boolean existsByTaxRegistrationNo(String taxRegistrationNo);

    @Query("""
            SELECT bp FROM BusinessPartner bp
            WHERE (:partnerType IS NULL OR bp.partnerType = :partnerType)
              AND (:status IS NULL OR bp.status = :status)
              AND (:keyword IS NULL OR LOWER(bp.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(bp.partnerCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(bp.taxRegistrationNo) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<BusinessPartner> searchPartners(
            @Param("partnerType") PartnerType partnerType,
            @Param("status") PartnerStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
