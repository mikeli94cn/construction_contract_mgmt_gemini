package com.ccm.partner.domain;

import com.ccm.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "business_partners")
public class BusinessPartner extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_code", nullable = false, unique = true, length = 50)
    private String partnerCode;

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", nullable = false, length = 40)
    private PartnerType partnerType;

    @Column(name = "tax_registration_no", nullable = false, unique = true, length = 100)
    private String taxRegistrationNo;

    @Column(name = "qualification_level", length = 100)
    private String qualificationLevel;

    @Column(name = "contact_person", nullable = false, length = 150)
    private String contactPerson;

    @Column(name = "contact_phone", nullable = false, length = 50)
    private String contactPhone;

    @Column(name = "contact_email", nullable = false, length = 150)
    private String contactEmail;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "bank_account_no", length = 100)
    private String bankAccountNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PartnerStatus status;

    protected BusinessPartner() {
    }

    public BusinessPartner(
            String partnerCode,
            String name,
            PartnerType partnerType,
            String taxRegistrationNo,
            String qualificationLevel,
            String contactPerson,
            String contactPhone,
            String contactEmail,
            String bankName,
            String bankAccountNo,
            PartnerStatus status,
            String createdBy
    ) {
        this.partnerCode = partnerCode.trim().toUpperCase();
        this.name = name.trim();
        this.partnerType = partnerType;
        this.taxRegistrationNo = taxRegistrationNo.trim().toUpperCase();
        this.qualificationLevel = qualificationLevel;
        this.contactPerson = contactPerson.trim();
        this.contactPhone = contactPhone.trim();
        this.contactEmail = contactEmail.trim().toLowerCase();
        this.bankName = bankName;
        this.bankAccountNo = bankAccountNo;
        this.status = status == null ? PartnerStatus.ACTIVE : status;
        setCreatedBy(createdBy);
    }

    public void updateDetails(
            String name,
            PartnerType partnerType,
            String qualificationLevel,
            String contactPerson,
            String contactPhone,
            String contactEmail,
            String bankName,
            String bankAccountNo,
            PartnerStatus status
    ) {
        this.name = name.trim();
        this.partnerType = partnerType;
        this.qualificationLevel = qualificationLevel;
        this.contactPerson = contactPerson.trim();
        this.contactPhone = contactPhone.trim();
        this.contactEmail = contactEmail.trim().toLowerCase();
        this.bankName = bankName;
        this.bankAccountNo = bankAccountNo;
        if (status != null) {
            this.status = status;
        }
    }

    public Long getId() {
        return id;
    }

    public String getPartnerCode() {
        return partnerCode;
    }

    public String getName() {
        return name;
    }

    public PartnerType getPartnerType() {
        return partnerType;
    }

    public String getTaxRegistrationNo() {
        return taxRegistrationNo;
    }

    public String getQualificationLevel() {
        return qualificationLevel;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getBankName() {
        return bankName;
    }

    public String getBankAccountNo() {
        return bankAccountNo;
    }

    public PartnerStatus getStatus() {
        return status;
    }
}
