package com.ccm.changeorder.infrastructure;

import com.ccm.changeorder.domain.ChangeOrder;
import com.ccm.changeorder.domain.ChangeOrderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeOrderRepository extends JpaRepository<ChangeOrder, Long> {

    boolean existsByChangeOrderNo(String changeOrderNo);

    boolean existsByContractIdAndStatus(Long contractId, ChangeOrderStatus status);

    @EntityGraph(attributePaths = {"contract", "contract.project"})
    Optional<ChangeOrder> findWithContractById(Long id);

    @EntityGraph(attributePaths = {"contract", "contract.project"})
    List<ChangeOrder> findByContractIdOrderByCreatedAtDesc(Long contractId);
}
