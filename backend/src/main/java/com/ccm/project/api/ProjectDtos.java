package com.ccm.project.api;

import com.ccm.project.domain.ProjectRole;
import com.ccm.project.domain.ProjectStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record CreateProjectRequest(
            @NotBlank(message = "Project code is required")
            @Size(max = 50)
            String projectCode,

            @NotBlank(message = "Project name is required")
            @Size(max = 250)
            String name,

            @NotNull(message = "Organization ID is required")
            Long organizationId,

            @NotBlank(message = "Location is required")
            @Size(max = 255)
            String location,

            @Size(min = 3, max = 3)
            String currency,

            @NotNull(message = "Approved budget is required")
            @DecimalMin(value = "0.0001", message = "Approved budget must be positive")
            BigDecimal approvedBudget,

            @NotNull(message = "Start date is required")
            LocalDate startDate,

            @NotNull(message = "Planned end date is required")
            LocalDate plannedEndDate,

            ProjectStatus status,

            String description
    ) {
    }

    public record UpdateProjectRequest(
            @NotBlank(message = "Project name is required")
            @Size(max = 250)
            String name,

            @NotBlank(message = "Location is required")
            @Size(max = 255)
            String location,

            @NotNull(message = "Approved budget is required")
            @DecimalMin(value = "0.0001", message = "Approved budget must be positive")
            BigDecimal approvedBudget,

            @NotNull(message = "Start date is required")
            LocalDate startDate,

            @NotNull(message = "Planned end date is required")
            LocalDate plannedEndDate,

            @NotNull(message = "Status is required")
            ProjectStatus status,

            String description
    ) {
    }

    public record ProjectResponse(
            Long id,
            String projectCode,
            String name,
            Long organizationId,
            String organizationCode,
            String organizationName,
            String location,
            String currency,
            BigDecimal approvedBudget,
            LocalDate startDate,
            LocalDate plannedEndDate,
            ProjectStatus status,
            String description,
            Long version,
            Instant createdAt,
            Instant updatedAt,
            String createdBy
    ) {
    }

    public record AssignProjectMemberRequest(
            @NotNull(message = "User ID is required")
            Long userId,

            @NotNull(message = "Project role is required")
            ProjectRole projectRole
    ) {
    }

    public record ProjectMemberResponse(
            Long id,
            Long projectId,
            Long userId,
            String username,
            String fullName,
            String email,
            ProjectRole projectRole,
            Instant assignedAt,
            String assignedBy
    ) {
    }
}
