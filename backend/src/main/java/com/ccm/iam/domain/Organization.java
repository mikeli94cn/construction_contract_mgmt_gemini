package com.ccm.iam.domain;

import com.ccm.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizations")
public class Organization extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Organization() {
    }

    public Organization(String code, String name, Long parentId, String createdBy) {
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.parentId = parentId;
        this.active = true;
        setCreatedBy(createdBy);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public Long getParentId() {
        return parentId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
