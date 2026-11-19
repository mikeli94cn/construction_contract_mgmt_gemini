package com.ccm.partner.api;

import com.ccm.partner.domain.PartnerStatus;
import com.ccm.partner.domain.PartnerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class PartnerDtos {

    private PartnerDtos() {
    }

    public record CreatePartnerRequest(
            @NotBlank(message = "Partner code is required")
            @Size(max = 50)
            String partnerCode,

            @NotBlank(message = "Partner legal name is required")
            @Size(max = 250)
            String name,

            @NotNull(message = "Partner type is required")
            PartnerType partnerType,

            @NotBlank(message = "Tax registration number is required")
            @Size(max = 100)
            String taxRegistrationNo,

            @Size(max = 100)
            String qualificationLevel,

            @NotBlank(message = "Contact person is required")
            @Size(max = 150)
            String contactPerson,

            @NotBlank(message = "Contact phone is required")
            @Size(max = 50)
            String contactPhone,

            @NotBlank(message = "Contact email is required")
            @Email(message = "Valid contact email is required")
            @Size(max = 150)
            String contactEmail,

            @Size(max = 150)
            String bankName,

            @Size(max = 100)
            String bankAccountNo,

            PartnerStatus status
    ) {
    }

    public record UpdatePartnerRequest(
            @NotBlank(message = "Partner legal name is required")
            @Size(max = 250)
            String name,

            @NotNull(message = "Partner type is required")
            PartnerType partnerType,

            @Size(max = 100)
            String qualificationLevel,

            @NotBlank(message = "Contact person is required")
            @Size(max = 150)
            String contactPerson,

            @NotBlank(message = "Contact phone is required")
            @Size(max = 50)
            String contactPhone,

            @NotBlank(message = "Contact email is required")
            @Email(message = "Valid contact email is required")
            @Size(max = 150)
            String contactEmail,

            @Size(max = 150)
            String bankName,

            @Size(max = 100)
            String bankAccountNo,

            @NotNull(message = "Status is required")
            PartnerStatus status
    ) {
    }

    public record PartnerResponse(
            Long id,
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
            Long version,
            Instant createdAt,
            Instant updatedAt,
            String createdBy
    ) {
    }
}
