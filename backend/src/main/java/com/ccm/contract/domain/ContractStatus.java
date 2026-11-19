package com.ccm.contract.domain;

public enum ContractStatus {
    DRAFT,
    IN_APPROVAL,
    APPROVED,
    ACTIVE,
    SETTLED,
    CLOSED,
    TERMINATED;

    public boolean isDraftEditable() {
        return this == DRAFT;
    }
}
