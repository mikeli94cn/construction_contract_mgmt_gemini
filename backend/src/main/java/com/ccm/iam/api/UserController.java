package com.ccm.iam.api;

import com.ccm.iam.api.IamDtos.CreateUserRequest;
import com.ccm.iam.api.IamDtos.UserProfileResponse;
import com.ccm.iam.application.IamApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Enterprise user administration")
public class UserController {

    private final IamApplicationService iamApplicationService;

    public UserController(IamApplicationService iamApplicationService) {
        this.iamApplicationService = iamApplicationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'PROJECT_MANAGER', 'AUDITOR')")
    @Operation(summary = "List users in the enterprise directory")
    public List<UserProfileResponse> listUsers() {
        return iamApplicationService.listUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Create a new user and assign organization and system roles")
    public UserProfileResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return iamApplicationService.createUser(request);
    }
}
