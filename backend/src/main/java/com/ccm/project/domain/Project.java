package com.ccm.project.domain;

import com.ccm.common.domain.AuditableEntity;
import com.ccm.common.domain.MoneyUtils;
import com.ccm.common.exception.BusinessRuleViolationException;
import com.ccm.iam.domain.Organization;
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
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "projects")
public class Project extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_code", nullable = false, unique = true, length = 50)
    private String projectCode;

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "approved_budget", nullable = false, precision = 19, scale = 4)
    private BigDecimal approvedBudget;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "planned_end_date", nullable = false)
    private LocalDate plannedEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProjectStatus status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    protected Project() {
    }

    public Project(
            String projectCode,
            String name,
            Organization organization,
            String location,
            String currency,
            BigDecimal approvedBudget,
            LocalDate startDate,
            LocalDate plannedEndDate,
            ProjectStatus status,
            String description,
            String createdBy
    ) {
        validateDatesAndBudget(startDate, plannedEndDate, approvedBudget);
        this.projectCode = projectCode.trim().toUpperCase();
        this.name = name.trim();
        this.organization = organization;
        this.location = location.trim();
        this.currency = (currency == null || currency.isBlank()) ? "USD" : currency.trim().toUpperCase();
        this.approvedBudget = MoneyUtils.scaleMoney(approvedBudget);
        this.startDate = startDate;
        this.plannedEndDate = plannedEndDate;
        this.status = status == null ? ProjectStatus.PLANNING : status;
        this.description = description;
        setCreatedBy(createdBy);
    }

    public void updateDetails(
            String name,
            String location,
            BigDecimal approvedBudget,
            LocalDate startDate,
            LocalDate plannedEndDate,
            ProjectStatus newStatus,
            String description
    ) {
        if (!this.status.isEditable()) {
            throw new BusinessRuleViolationException("Closed project cannot be modified: " + this.projectCode);
        }
        validateDatesAndBudget(startDate, plannedEndDate, approvedBudget);
        this.name = name.trim();
        this.location = location.trim();
        this.approvedBudget = MoneyUtils.scaleMoney(approvedBudget);
        this.startDate = startDate;
        this.plannedEndDate = plannedEndDate;
        if (newStatus != null) {
            this.status = newStatus;
        }
        this.description = description;
    }

    private static void validateDatesAndBudget(
            LocalDate startDate,
            LocalDate plannedEndDate,
            BigDecimal approvedBudget
    ) {
        if (!MoneyUtils.isPositive(approvedBudget)) {
            throw new BusinessRuleViolationException("Project approved budget must be greater than zero");
        }
        if (startDate == null || plannedEndDate == null) {
            throw new BusinessRuleViolationException("Project start date and planned end date are required");
        }
        if (plannedEndDate.isBefore(startDate)) {
            throw new BusinessRuleViolationException("Project planned end date cannot be earlier than start date");
        }
    }

    public Long getId() {
        return id;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public String getName() {
        return name;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getLocation() {
        return location;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getApprovedBudget() {
        return approvedBudget;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getPlannedEndDate() {
        return plannedEndDate;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }
}
