-- =====================================================================
-- V1: Initial Schema for IAM, Projects, Business Partners, Contracts & Audit Logs
-- Database: MySQL 8.4 LTS
-- =====================================================================

CREATE TABLE organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    parent_id BIGINT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_organizations_code UNIQUE (code),
    CONSTRAINT fk_organizations_parent FOREIGN KEY (parent_id) REFERENCES organizations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    organization_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_organization FOREIGN KEY (organization_id) REFERENCES organizations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_code VARCHAR(50) NOT NULL,
    name VARCHAR(250) NOT NULL,
    organization_id BIGINT NOT NULL,
    location VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    approved_budget DECIMAL(19, 4) NOT NULL,
    start_date DATE NOT NULL,
    planned_end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    description TEXT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT uk_projects_code UNIQUE (project_code),
    CONSTRAINT fk_projects_organization FOREIGN KEY (organization_id) REFERENCES organizations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_projects_status ON projects (status);
CREATE INDEX idx_projects_org ON projects (organization_id);

CREATE TABLE project_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    project_role VARCHAR(50) NOT NULL,
    assigned_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    assigned_by VARCHAR(100) NOT NULL,
    CONSTRAINT uk_project_members_project_user UNIQUE (project_id, user_id),
    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_project_members_user ON project_members (user_id);

CREATE TABLE business_partners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    partner_code VARCHAR(50) NOT NULL,
    name VARCHAR(250) NOT NULL,
    partner_type VARCHAR(40) NOT NULL,
    tax_registration_no VARCHAR(100) NOT NULL,
    qualification_level VARCHAR(100) NULL,
    contact_person VARCHAR(150) NOT NULL,
    contact_phone VARCHAR(50) NOT NULL,
    contact_email VARCHAR(150) NOT NULL,
    bank_name VARCHAR(150) NULL,
    bank_account_no VARCHAR(100) NULL,
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT uk_business_partners_code UNIQUE (partner_code),
    CONSTRAINT uk_business_partners_tax_no UNIQUE (tax_registration_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_business_partners_type_status ON business_partners (partner_type, status);

CREATE TABLE contracts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_no VARCHAR(60) NOT NULL,
    title VARCHAR(300) NOT NULL,
    project_id BIGINT NOT NULL,
    partner_id BIGINT NOT NULL,
    parent_contract_id BIGINT NULL,
    category VARCHAR(40) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    original_amount_net DECIMAL(19, 4) NOT NULL,
    tax_rate DECIMAL(7, 4) NOT NULL,
    original_amount_tax DECIMAL(19, 4) NOT NULL,
    original_amount_gross DECIMAL(19, 4) NOT NULL,
    approved_change_net DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    approved_change_gross DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    retainage_rate DECIMAL(7, 4) NOT NULL DEFAULT 0.0500,
    advance_payment_rate DECIMAL(7, 4) NOT NULL DEFAULT 0.0000,
    signed_date DATE NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    defect_liability_months INT NOT NULL DEFAULT 12,
    status VARCHAR(30) NOT NULL,
    scope_summary TEXT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT uk_contracts_contract_no UNIQUE (contract_no),
    CONSTRAINT fk_contracts_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_contracts_partner FOREIGN KEY (partner_id) REFERENCES business_partners (id),
    CONSTRAINT fk_contracts_parent FOREIGN KEY (parent_contract_id) REFERENCES contracts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_contracts_project_status ON contracts (project_id, status);
CREATE INDEX idx_contracts_partner ON contracts (partner_id);

CREATE TABLE contract_line_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    item_code VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    quantity DECIMAL(19, 4) NOT NULL,
    unit_price_net DECIMAL(19, 4) NOT NULL,
    total_price_net DECIMAL(19, 4) NOT NULL,
    sort_order INT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_contract_line_items_contract FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_contract_line_items_contract ON contract_line_items (contract_id, sort_order);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    actor_user_id BIGINT NULL,
    actor_username VARCHAR(100) NOT NULL,
    module VARCHAR(50) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    action VARCHAR(50) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    payload_json JSON NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_occurred_at ON audit_logs (occurred_at);

-- Seed system roles & root organization (no hard-coded user credentials)
INSERT INTO roles (name, description) VALUES
    ('ROLE_SYSTEM_ADMIN', 'System Administrator with full organizational governance'),
    ('ROLE_PROJECT_MANAGER', 'Project Manager responsible for project delivery and team membership'),
    ('ROLE_CONTRACT_MANAGER', 'Contract Manager responsible for drafting and administering contracts'),
    ('ROLE_COST_ENGINEER', 'Cost Engineer / Quantity Surveyor responsible for BoQ, change order, and payment measurement'),
    ('ROLE_FINANCE_OFFICER', 'Finance Officer responsible for payment verification and disbursement'),
    ('ROLE_EXECUTIVE_APPROVER', 'Executive Approver for high-value contracts, variations, and final settlements'),
    ('ROLE_AUDITOR', 'Internal/External Auditor with read-only access to all records and audit trails');

INSERT INTO organizations (code, name, parent_id, active, created_by) VALUES
    ('ORG-HQ', 'Apex Construction Group Headquarters', NULL, TRUE, 'SYSTEM');
