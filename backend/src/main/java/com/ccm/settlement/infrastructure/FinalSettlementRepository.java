package com.ccm.settlement.infrastructure;

import com.ccm.settlement.domain.FinalSettlement;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinalSettlementRepository extends JpaRepository<FinalSettlement, Long> {

    boolean existsBySettlementNo(String settlementNo);

    boolean existsByContractId(Long contractId);

    @EntityGraph(attributePaths = {"contract", "contract.project"})
    Optional<FinalSettlement> findWithContractById(Long id);

    @EntityGraph(attributePaths = {"contract", "contract.project"})
    Optional<FinalSettlement> findByContractId(Long contractId);
}
