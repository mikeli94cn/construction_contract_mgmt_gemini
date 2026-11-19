package com.ccm.approval.infrastructure;

import com.ccm.approval.domain.ApprovalTargetType;
import com.ccm.approval.domain.ApprovalWorkflow;
import com.ccm.approval.domain.ApprovalWorkflowStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {

    @EntityGraph(attributePaths = {"steps"})
    Optional<ApprovalWorkflow> findWithStepsById(Long id);

    @EntityGraph(attributePaths = {"steps"})
    List<ApprovalWorkflow> findByTargetTypeAndTargetIdOrderBySubmittedAtDesc(
            ApprovalTargetType targetType,
            Long targetId
    );

    boolean existsByTargetTypeAndTargetIdAndStatus(
            ApprovalTargetType targetType,
            Long targetId,
            ApprovalWorkflowStatus status
    );

    @EntityGraph(attributePaths = {"steps"})
    @Query("""
            SELECT w FROM ApprovalWorkflow w
            WHERE (:projectId IS NULL OR w.projectId = :projectId)
              AND (:targetType IS NULL OR w.targetType = :targetType)
              AND (:status IS NULL OR w.status = :status)
            """)
    Page<ApprovalWorkflow> searchAllWorkflows(
            @Param("projectId") Long projectId,
            @Param("targetType") ApprovalTargetType targetType,
            @Param("status") ApprovalWorkflowStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"steps"})
    @Query("""
            SELECT w FROM ApprovalWorkflow w
            WHERE EXISTS (
                SELECT 1 FROM ProjectMember pm
                WHERE pm.project.id = w.projectId AND pm.user.id = :userId
            )
              AND (:projectId IS NULL OR w.projectId = :projectId)
              AND (:targetType IS NULL OR w.targetType = :targetType)
              AND (:status IS NULL OR w.status = :status)
            """)
    Page<ApprovalWorkflow> searchMemberWorkflows(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId,
            @Param("targetType") ApprovalTargetType targetType,
            @Param("status") ApprovalWorkflowStatus status,
            Pageable pageable
    );
}
