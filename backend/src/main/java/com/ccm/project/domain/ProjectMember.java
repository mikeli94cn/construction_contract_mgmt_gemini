package com.ccm.project.domain;

import com.ccm.iam.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "project_members")
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_role", nullable = false, length = 50)
    private ProjectRole projectRole;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by", nullable = false, length = 100)
    private String assignedBy;

    protected ProjectMember() {
    }

    public ProjectMember(Project project, User user, ProjectRole projectRole, String assignedBy) {
        this.project = project;
        this.user = user;
        this.projectRole = projectRole;
        this.assignedAt = Instant.now();
        this.assignedBy = assignedBy == null || assignedBy.isBlank() ? "SYSTEM" : assignedBy;
    }

    public void updateRole(ProjectRole newRole, String assignedBy) {
        this.projectRole = newRole;
        this.assignedAt = Instant.now();
        this.assignedBy = assignedBy;
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public User getUser() {
        return user;
    }

    public ProjectRole getProjectRole() {
        return projectRole;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public String getAssignedBy() {
        return assignedBy;
    }
}
