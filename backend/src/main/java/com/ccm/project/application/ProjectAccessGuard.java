package com.ccm.project.application;

import com.ccm.common.security.SecurityContextUtils;
import com.ccm.iam.domain.User;
import com.ccm.iam.infrastructure.UserRepository;
import com.ccm.project.domain.ProjectRole;
import com.ccm.project.infrastructure.ProjectMemberRepository;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Resource-level authorization guard enforcing project membership & role constraints.
 */
@Component("projectAccessGuard")
public class ProjectAccessGuard {

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public ProjectAccessGuard(
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository
    ) {
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
    }

    public boolean hasGlobalReadAccess() {
        return SecurityContextUtils.hasAnyRole(
                "ROLE_SYSTEM_ADMIN",
                "ROLE_AUDITOR",
                "ROLE_EXECUTIVE_APPROVER",
                "ROLE_FINANCE_OFFICER"
        );
    }

    public Long resolveCurrentUserId() {
        Long uid = SecurityContextUtils.getCurrentUserId();
        if (uid != null) {
            return uid;
        }
        String username = SecurityContextUtils.getCurrentUsername();
        if ("SYSTEM".equals(username)) {
            return null;
        }
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElse(null);
    }

    public boolean canViewProject(Long projectId) {
        if (projectId == null) {
            return false;
        }
        if (hasGlobalReadAccess()) {
            return true;
        }
        Long userId = resolveCurrentUserId();
        return userId != null && projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    public boolean canManageProject(Long projectId) {
        if (projectId == null) {
            return false;
        }
        if (SecurityContextUtils.hasAnyRole("ROLE_SYSTEM_ADMIN")) {
            return true;
        }
        if (!SecurityContextUtils.hasAnyRole("ROLE_PROJECT_MANAGER")) {
            return false;
        }
        Long userId = resolveCurrentUserId();
        return userId != null && projectMemberRepository.existsByProjectIdAndUserIdAndProjectRoleIn(
                projectId,
                userId,
                Set.of(ProjectRole.PROJECT_MANAGER)
        );
    }

    public boolean canManageProjectContracts(Long projectId) {
        if (projectId == null) {
            return false;
        }
        if (SecurityContextUtils.hasAnyRole("ROLE_SYSTEM_ADMIN")) {
            return true;
        }
        if (!SecurityContextUtils.hasAnyRole("ROLE_CONTRACT_MANAGER", "ROLE_PROJECT_MANAGER")) {
            return false;
        }
        Long userId = resolveCurrentUserId();
        return userId != null && projectMemberRepository.existsByProjectIdAndUserIdAndProjectRoleIn(
                projectId,
                userId,
                Set.of(ProjectRole.PROJECT_MANAGER, ProjectRole.CONTRACT_MANAGER)
        );
    }

    public void assertCanViewProject(Long projectId) {
        if (!canViewProject(projectId)) {
            throw new AccessDeniedException("You do not have permission to view project " + projectId);
        }
    }

    public void assertCanManageProject(Long projectId) {
        if (!canManageProject(projectId)) {
            throw new AccessDeniedException("You do not have permission to manage project " + projectId);
        }
    }

    public void assertCanManageProjectContracts(Long projectId) {
        if (!canManageProjectContracts(projectId)) {
            throw new AccessDeniedException("You do not have permission to manage contracts in project " + projectId);
        }
    }
}
