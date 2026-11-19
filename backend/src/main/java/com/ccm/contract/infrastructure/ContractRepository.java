package com.ccm.contract.infrastructure;

import com.ccm.contract.domain.Contract;
import com.ccm.contract.domain.ContractCategory;
import com.ccm.contract.domain.ContractStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    boolean existsByContractNo(String contractNo);

    List<Contract> findByProjectId(Long projectId);

    @EntityGraph(attributePaths = {"project", "partner", "parentContract", "lineItems"})
    Optional<Contract> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"project", "partner", "parentContract"})
    @Query("""
            SELECT c FROM Contract c
            WHERE (:projectId IS NULL OR c.project.id = :projectId)
              AND (:partnerId IS NULL OR c.partner.id = :partnerId)
              AND (:category IS NULL OR c.category = :category)
              AND (:status IS NULL OR c.status = :status)
              AND (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(c.contractNo) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Contract> searchAllContracts(
            @Param("projectId") Long projectId,
            @Param("partnerId") Long partnerId,
            @Param("category") ContractCategory category,
            @Param("status") ContractStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"project", "partner", "parentContract"})
    @Query("""
            SELECT c FROM Contract c
            WHERE EXISTS (
                SELECT 1 FROM ProjectMember pm
                WHERE pm.project = c.project AND pm.user.id = :userId
            )
              AND (:projectId IS NULL OR c.project.id = :projectId)
              AND (:partnerId IS NULL OR c.partner.id = :partnerId)
              AND (:category IS NULL OR c.category = :category)
              AND (:status IS NULL OR c.status = :status)
              AND (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(c.contractNo) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Contract> searchMemberContracts(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId,
            @Param("partnerId") Long partnerId,
            @Param("category") ContractCategory category,
            @Param("status") ContractStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
