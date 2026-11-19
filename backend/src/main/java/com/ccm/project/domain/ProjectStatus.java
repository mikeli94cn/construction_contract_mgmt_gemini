package com.ccm.project.domain;

public enum ProjectStatus {
    PLANNING,
    ACTIVE,
    ON_HOLD,
    COMPLETED,
    CLOSED;

    public boolean canAcceptNewContracts() {
        return this == PLANNING || this == ACTIVE;
    }

    public boolean isEditable() {
        return this != CLOSED;
    }
}
