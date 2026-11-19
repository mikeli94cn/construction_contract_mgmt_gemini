package com.ccm.partner.domain;

public enum PartnerStatus {
    ACTIVE,
    SUSPENDED,
    BLACKLISTED;

    public boolean isEligibleForNewContract() {
        return this == ACTIVE;
    }
}
