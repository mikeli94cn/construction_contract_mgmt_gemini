-- =====================================================================
-- V2: Approval Workflow Engine & Document Management Schema
-- Database: MySQL 8.4 LTS
-- =====================================================================

CREATE TABLE approval_workflows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    reference_no VARCHAR(80) NOT NULL,
    title VARCHAR(300) NOT NULL,
    amount_gross DECIMAL(19, 4) NOT NULL,
    status VARCHAR(30) NOT NULL,
    current_step_order INT NOT NULL DEFAULT 1,
    submitted_by VARCHAR(100) NOT NULL,
    submitted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_approval_workflows_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_approval_workflows_target ON approval_workflows (target_type, target_id);
CREATE INDEX idx_approval_workflows_project_status ON approval_workflows (project_id, status);

CREATE TABLE approval_steps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    step_name VARCHAR(120) NOT NULL,
    required_role VARCHAR(60) NOT NULL,
    status VARCHAR(30) NOT NULL,
    actor_username VARCHAR(100) NULL,
    comments VARCHAR(1000) NULL,
    decided_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_approval_steps_workflow FOREIGN KEY (workflow_id) REFERENCES approval_workflows (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_approval_steps_workflow_order ON approval_steps (workflow_id, step_order);
CREATE INDEX idx_approval_steps_role_status ON approval_steps (required_role, status);

CREATE TABLE documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    owner_type VARCHAR(50) NOT NULL,
    owner_id BIGINT NOT NULL,
    document_type VARCHAR(60) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256_checksum VARCHAR(64) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    version_no INT NOT NULL DEFAULT 1,
    uploaded_by VARCHAR(100) NOT NULL,
    uploaded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_documents_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_documents_owner ON documents (owner_type, owner_id, version_no);
CREATE INDEX idx_documents_project ON documents (project_id);
