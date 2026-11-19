package com.ccm.project.api;

import com.ccm.common.api.PageResponse;
import com.ccm.project.api.ProjectDtos.AssignProjectMemberRequest;
import com.ccm.project.api.ProjectDtos.CreateProjectRequest;
import com.ccm.project.api.ProjectDtos.ProjectMemberResponse;
import com.ccm.project.api.ProjectDtos.ProjectResponse;
import com.ccm.project.api.ProjectDtos.UpdateProjectRequest;
import com.ccm.project.application.ProjectApplicationService;
import com.ccm.project.domain.ProjectStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Construction project lifecycle and team membership endpoints")
public class ProjectController {

    private final ProjectApplicationService projectApplicationService;

    public ProjectController(ProjectApplicationService projectApplicationService) {
        this.projectApplicationService = projectApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Create a new construction project")
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectApplicationService.createProject(request);
    }

    @GetMapping
    @Operation(summary = "List construction projects visible to the authenticated user")
    public PageResponse<ProjectResponse> listProjects(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return projectApplicationService.listProjects(
                status,
                keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get construction project details")
    public ProjectResponse getProject(@PathVariable Long id) {
        return projectApplicationService.getProject(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update construction project details and lifecycle status")
    public ProjectResponse updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return projectApplicationService.updateProject(id, request);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assign or update a user's project-level role on a project")
    public ProjectMemberResponse assignMember(
            @PathVariable Long id,
            @Valid @RequestBody AssignProjectMemberRequest request
    ) {
        return projectApplicationService.assignMember(id, request);
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List assigned team members for a project")
    public List<ProjectMemberResponse> listMembers(@PathVariable Long id) {
        return projectApplicationService.listMembers(id);
    }
}
