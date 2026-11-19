package com.ccm.project.application;

import com.ccm.audit.application.AuditTrailService;
import com.ccm.common.api.PageResponse;
import com.ccm.common.exception.BusinessRuleViolationException;
import com.ccm.common.exception.DuplicateResourceException;
import com.ccm.common.exception.ResourceNotFoundException;
import com.ccm.common.security.SecurityContextUtils;
import com.ccm.iam.domain.Organization;
import com.ccm.iam.domain.User;
import com.ccm.iam.infrastructure.OrganizationRepository;
import com.ccm.iam.infrastructure.UserRepository;
import com.ccm.project.api.ProjectDtos.AssignProjectMemberRequest;
import com.ccm.project.api.ProjectDtos.CreateProjectRequest;
import com.ccm.project.api.ProjectDtos.ProjectMemberResponse;
import com.ccm.project.api.ProjectDtos.ProjectResponse;
import com.ccm.project.api.ProjectDtos.UpdateProjectRequest;
import com.ccm.project.domain.Project;
import com.ccm.project.domain.ProjectMember;
import com.ccm.project.domain.ProjectRole;
import com.ccm.project.domain.ProjectStatus;
import com.ccm.project.infrastructure.ProjectMemberRepository;
import com.ccm.project.infrastructure.ProjectRepository;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectApplicationService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final AuditTrailService auditTrailService;

    public ProjectApplicationService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            ProjectAccessGuard projectAccessGuard,
            AuditTrailService auditTrailService
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.projectAccessGuard = projectAccessGuard;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        String code = request.projectCode().trim().toUpperCase();
        if (projectRepository.existsByProjectCode(code)) {
            throw new DuplicateResourceException("Project code already exists: " + code);
        }

        Organization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + request.organizationId()));
        if (!organization.isActive()) {
            throw new BusinessRuleViolationException("Cannot create project under an inactive organization");
        }

        String actor = SecurityContextUtils.getCurrentUsername();
        Project project = new Project(
                code,
                request.name(),
                organization,
                request.location(),
                request.currency(),
                request.approvedBudget(),
                request.startDate(),
                request.plannedEndDate(),
                request.status(),
                request.description(),
                actor
        );
        Project saved = projectRepository.saveAndFlush(project);

        // Automatically assign creator as PROJECT_MANAGER on the newly created project
        userRepository.findByUsername(actor).ifPresent(creator -> {
            ProjectMember creatorMembership = new ProjectMember(saved, creator, ProjectRole.PROJECT_MANAGER, actor);
            projectMemberRepository.save(creatorMembership);
        });

        auditTrailService.record(
                "PROJECT",
                "Project",
                saved.getId(),
                "PROJECT_CREATED",
                "Created project " + saved.getProjectCode() + " with budget " + saved.getApprovedBudget(),
                Map.of(
                        "projectCode", saved.getProjectCode(),
                        "approvedBudget", saved.getApprovedBudget(),
                        "status", saved.getStatus().name()
                )
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> listProjects(ProjectStatus status, String keyword, Pageable pageable) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<Project> page;
        if (projectAccessGuard.hasGlobalReadAccess()) {
            page = projectRepository.searchAllProjects(status, normalizedKeyword, pageable);
        } else {
            Long currentUserId = projectAccessGuard.resolveCurrentUserId();
            if (currentUserId == null) {
                page = Page.empty(pageable);
            } else {
                page = projectRepository.searchMemberProjects(currentUserId, status, normalizedKeyword, pageable);
            }
        }
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId) {
        projectAccessGuard.assertCanViewProject(projectId);
        Project project = projectRepository.findWithOrganizationById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request) {
        projectAccessGuard.assertCanManageProject(projectId);
        Project project = projectRepository.findWithOrganizationById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        project.updateDetails(
                request.name(),
                request.location(),
                request.approvedBudget(),
                request.startDate(),
                request.plannedEndDate(),
                request.status(),
                request.description()
        );
        Project saved = projectRepository.saveAndFlush(project);

        auditTrailService.record(
                "PROJECT",
                "Project",
                saved.getId(),
                "PROJECT_UPDATED",
                "Updated project " + saved.getProjectCode() + " (status=" + saved.getStatus() + ")",
                Map.of(
                        "projectCode", saved.getProjectCode(),
                        "approvedBudget", saved.getApprovedBudget(),
                        "status", saved.getStatus().name()
                )
        );

        return toResponse(saved);
    }

    @Transactional
    public ProjectMemberResponse assignMember(Long projectId, AssignProjectMemberRequest request) {
        projectAccessGuard.assertCanManageProject(projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        if (!project.getStatus().isEditable()) {
            throw new BusinessRuleViolationException("Cannot modify members on a closed project");
        }

        User user = userRepository.findWithDetailsById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));
        if (!user.isEnabled()) {
            throw new BusinessRuleViolationException("Cannot assign a disabled user to a project");
        }

        String actor = SecurityContextUtils.getCurrentUsername();
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, user.getId())
                .map(existing -> {
                    existing.updateRole(request.projectRole(), actor);
                    return existing;
                })
                .orElseGet(() -> new ProjectMember(project, user, request.projectRole(), actor));

        ProjectMember saved = projectMemberRepository.save(member);

        auditTrailService.record(
                "PROJECT",
                "ProjectMember",
                saved.getId(),
                "PROJECT_MEMBER_ASSIGNED",
                "Assigned user " + user.getUsername() + " as " + request.projectRole() + " on project " + project.getProjectCode(),
                Map.of(
                        "projectId", project.getId(),
                        "userId", user.getId(),
                        "projectRole", request.projectRole().name()
                )
        );

        return toMemberResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(Long projectId) {
        projectAccessGuard.assertCanViewProject(projectId);
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }
        return projectMemberRepository.findByProjectIdOrderByAssignedAtAsc(projectId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getProjectCode(),
                project.getName(),
                project.getOrganization().getId(),
                project.getOrganization().getCode(),
                project.getOrganization().getName(),
                project.getLocation(),
                project.getCurrency(),
                project.getApprovedBudget(),
                project.getStartDate(),
                project.getPlannedEndDate(),
                project.getStatus(),
                project.getDescription(),
                project.getVersion(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getCreatedBy()
        );
    }

    private ProjectMemberResponse toMemberResponse(ProjectMember member) {
        User u = member.getUser();
        return new ProjectMemberResponse(
                member.getId(),
                member.getProject().getId(),
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getEmail(),
                member.getProjectRole(),
                member.getAssignedAt(),
                member.getAssignedBy()
        );
    }
}
