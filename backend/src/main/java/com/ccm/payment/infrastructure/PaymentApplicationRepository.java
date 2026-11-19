package com.ccm.payment.infrastructure;

import com.ccm.payment.domain.PaymentApplication;
import com.ccm.payment.domain.PaymentApplicationStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentApplicationRepository extends JpaRepository<PaymentApplication, Long> {

    boolean existsByApplicationNo(String applicationNo);

    boolean existsByContractIdAndStatusIn(Long contractId, Collection<PaymentApplicationStatus> statuses);

    @EntityGraph(attributePaths = {"contract", "contract.project"})
    Optional<PaymentApplication> findWithContractById(Long id);

    @EntityGraph(attributePaths = {"contract", "contract.project"})
    List<PaymentApplication> findByContractIdOrderByPeriodEndAscIdAsc(Long contractId);

    @Query("""
            SELECT COALESCE(SUM(p.currentPeriodGross), 0)
            FROM PaymentApplication p
            WHERE p.contract.id = :contractId
              AND p.status IN :statuses
            """)
    BigDecimal sumCurrentPeriodGrossByContractAndStatuses(
            @Param("contractId") Long contractId,
            @Param("statuses") Collection<PaymentApplicationStatus> statuses
    );

    @Query("""
            SELECT COALESCE(SUM(p.retainageWithheld), 0)
            FROM PaymentApplication p
            WHERE p.contract.id = :contractId
              AND p.status IN :statuses
            """)
    BigDecimal sumRetainageWithheldByContractAndStatuses(
            @Param("contractId") Long contractId,
            @Param("statuses") Collection<PaymentApplicationStatus> statuses
    );

    @Query("""
            SELECT COALESCE(SUM(p.netPayableAmount), 0)
            FROM PaymentApplication p
            WHERE p.contract.id = :contractId
              AND p.status IN :statuses
            """)
    BigDecimal sumNetPayableByContractAndStatuses(
            @Param("contractId") Long contractId,
            @Param("statuses") Collection<PaymentApplicationStatus> statuses
    );
}
