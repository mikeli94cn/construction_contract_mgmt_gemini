package com.ccm.iam.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

public final class IamDtos {

    private IamDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Username is required") String username,
            @NotBlank(message = "Password is required") String password
    ) {
    }

    public record LoginResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt,
            UserProfileResponse user
    ) {
    }

    public record UserProfileResponse(
            Long id,
            String username,
            String email,
            String fullName,
            Long organizationId,
            String organizationCode,
            String organizationName,
            boolean enabled,
            Set<String> roles
    ) {
    }

    public record CreateUserRequest(
            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 100)
            String username,

            @NotBlank(message = "Email is required")
            @Email(message = "Valid email is required")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
            String password,

            @NotBlank(message = "Full name is required")
            @Size(max = 150)
            String fullName,

            @NotNull(message = "Organization ID is required")
            Long organizationId,

            @NotEmpty(message = "At least one role must be assigned")
            Set<String> roles
    ) {
    }

    public record CreateOrganizationRequest(
            @NotBlank(message = "Organization code is required")
            @Size(max = 50)
            String code,

            @NotBlank(message = "Organization name is required")
            @Size(max = 200)
            String name,

            Long parentId
    ) {
    }

    public record OrganizationResponse(
            Long id,
            String code,
            String name,
            Long parentId,
            boolean active
    ) {
    }
}
