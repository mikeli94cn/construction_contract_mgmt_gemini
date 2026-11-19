-- =====================================================================
-- V3: Change Order Management & Progress Payment Applications Schema
-- Database: MySQL 8.4 LTS
-- =====================================================================

CREATE TABLE change_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    change_order_no VARCHAR(60) NOT NULL,
    change_type VARCHAR(40) NOT NULL,
    title VARCHAR(300) NOT NULL,
    reason TEXT NOT NULL,
    amount_delta_net DECIMAL(19, 4) NOT NULL,
    tax_rate DECIMAL(7, 4) NOT NULL,
    amount_delta_tax DECIMAL(19, 4) NOT NULL,
    amount_delta_gross DECIMAL(19, 4) NOT NULL,
    schedule_extension_days INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    approved_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT uk_change_orders_no UNIQUE (change_order_no),
    CONSTRAINT fk_change_orders_contract FOREIGN KEY (contract_id) REFERENCES contracts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_change_orders_contract_status ON change_orders (contract_id, status);

CREATE TABLE payment_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    application_no VARCHAR(60) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    cumulative_completed_gross DECIMAL(19, 4) NOT NULL,
    previous_certified_gross DECIMAL(19, 4) NOT NULL,
    current_period_gross DECIMAL(19, 4) NOT NULL,
    retainage_rate DECIMAL(7, 4) NOT NULL,
    retainage_withheld DECIMAL(19, 4) NOT NULL,
    advance_deduction DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    other_deductions DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    net_payable_amount DECIMAL(19, 4) NOT NULL,
    status VARCHAR(30) NOT NULL,
    approved_at TIMESTAMP(6) NULL,
    paid_at TIMESTAMP(6) NULL,
    payment_reference VARCHAR(120) NULL,
    remarks TEXT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT uk_payment_applications_no UNIQUE (application_no),
    CONSTRAINT fk_payment_applications_contract FOREIGN KEY (contract_id) REFERENCES contracts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_payment_applications_contract_status ON payment_applications (contract_id, status);
