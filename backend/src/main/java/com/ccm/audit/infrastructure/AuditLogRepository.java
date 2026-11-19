package com.ccm.audit.infrastructure;

import com.ccm.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
            String entityType,
            String entityId,
            Pageable pageable
    );

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:module IS NULL OR a.module = :module)
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:entityId IS NULL OR a.entityId = :entityId)
              AND (:actorUsername IS NULL OR LOWER(a.actorUsername) LIKE LOWER(CONCAT('%', :actorUsername, '%')))
            """)
    Page<AuditLog> searchAuditLogs(
            @Param("module") String module,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            @Param("actorUsername") String actorUsername,
            Pageable pageable
    );
}
