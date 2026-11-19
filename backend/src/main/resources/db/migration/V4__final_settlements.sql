-- =====================================================================
-- V4: Final Settlement & Warranty Retainage Release Schema
-- Database: MySQL 8.4 LTS
-- =====================================================================

CREATE TABLE final_settlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    settlement_no VARCHAR(60) NOT NULL,
    final_measured_net DECIMAL(19, 4) NOT NULL,
    tax_rate DECIMAL(7, 4) NOT NULL,
    final_tax_amount DECIMAL(19, 4) NOT NULL,
    final_contract_gross DECIMAL(19, 4) NOT NULL,
    total_previous_certified_gross DECIMAL(19, 4) NOT NULL,
    total_retainage_held DECIMAL(19, 4) NOT NULL,
    retainage_released_now DECIMAL(19, 4) NOT NULL,
    warranty_retainage_held DECIMAL(19, 4) NOT NULL,
    deductions_and_claims DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    final_closing_payable DECIMAL(19, 4) NOT NULL,
    completion_date DATE NOT NULL,
    warranty_expiry_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    approved_at TIMESTAMP(6) NULL,
    warranty_released_at TIMESTAMP(6) NULL,
    remarks TEXT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT uk_final_settlements_contract UNIQUE (contract_id),
    CONSTRAINT uk_final_settlements_no UNIQUE (settlement_no),
    CONSTRAINT fk_final_settlements_contract FOREIGN KEY (contract_id) REFERENCES contracts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
