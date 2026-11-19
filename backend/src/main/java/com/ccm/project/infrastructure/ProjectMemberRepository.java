package com.ccm.project.infrastructure;

import com.ccm.project.domain.ProjectMember;
import com.ccm.project.domain.ProjectRole;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    @EntityGraph(attributePaths = {"user", "project"})
    List<ProjectMember> findByProjectIdOrderByAssignedAtAsc(Long projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserIdAndProjectRoleIn(
            Long projectId,
            Long userId,
            Set<ProjectRole> roles
    );

    @Query("SELECT pm.project.id FROM ProjectMember pm WHERE pm.user.id = :userId")
    Set<Long> findProjectIdsByUserId(@Param("userId") Long userId);
}
