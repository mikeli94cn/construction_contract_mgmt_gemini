package com.ccm.iam.api;

import com.ccm.iam.api.IamDtos.LoginRequest;
import com.ccm.iam.api.IamDtos.LoginResponse;
import com.ccm.iam.api.IamDtos.UserProfileResponse;
import com.ccm.iam.application.IamApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "JWT authentication and current user profile endpoints")
public class AuthController {

    private final IamApplicationService iamApplicationService;

    public AuthController(IamApplicationService iamApplicationService) {
        this.iamApplicationService = iamApplicationService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user with username and password and issue a JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return iamApplicationService.authenticate(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Return current authenticated user profile and assigned roles")
    public UserProfileResponse me() {
        return iamApplicationService.getCurrentUserProfile();
    }
}
