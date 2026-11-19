package com.ccm.iam.application;

import com.ccm.audit.application.AuditTrailService;
import com.ccm.common.exception.BusinessRuleViolationException;
import com.ccm.common.exception.DuplicateResourceException;
import com.ccm.common.exception.ResourceNotFoundException;
import com.ccm.common.security.SecurityContextUtils;
import com.ccm.iam.api.IamDtos.CreateOrganizationRequest;
import com.ccm.iam.api.IamDtos.CreateUserRequest;
import com.ccm.iam.api.IamDtos.LoginRequest;
import com.ccm.iam.api.IamDtos.LoginResponse;
import com.ccm.iam.api.IamDtos.OrganizationResponse;
import com.ccm.iam.api.IamDtos.UserProfileResponse;
import com.ccm.iam.domain.Organization;
import com.ccm.iam.domain.Role;
import com.ccm.iam.domain.RoleName;
import com.ccm.iam.domain.User;
import com.ccm.iam.infrastructure.OrganizationRepository;
import com.ccm.iam.infrastructure.RoleRepository;
import com.ccm.iam.infrastructure.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IamApplicationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuditTrailService auditTrailService;

    public IamApplicationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            AuditTrailService auditTrailService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public LoginResponse authenticate(LoginRequest request) {
        String normalizedUsername = request.username().trim().toLowerCase();
        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("User account is disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        JwtTokenService.IssuedToken issuedToken = jwtTokenService.issueToken(user);
        return new LoginResponse(
                issuedToken.token(),
                "Bearer",
                issuedToken.expiresAt(),
                toUserProfile(user)
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        String username = SecurityContextUtils.getCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return toUserProfile(user);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserProfile)
                .toList();
    }

    @Transactional
    public UserProfileResponse createUser(CreateUserRequest request) {
        String username = request.username().trim().toLowerCase();
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists: " + email);
        }

        Organization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + request.organizationId()));
        if (!organization.isActive()) {
            throw new BusinessRuleViolationException("Cannot assign user to an inactive organization");
        }

        Set<RoleName> roleNames = new HashSet<>();
        for (String roleStr : request.roles()) {
            try {
                roleNames.add(RoleName.valueOf(roleStr.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new BusinessRuleViolationException("Invalid role name: " + roleStr);
            }
        }
        Set<Role> roles = roleRepository.findByNameIn(roleNames);
        if (roles.size() != roleNames.size()) {
            throw new BusinessRuleViolationException("One or more specified roles do not exist in the system");
        }

        User user = new User(
                username,
                email,
                passwordEncoder.encode(request.password()),
                request.fullName(),
                organization,
                roles,
                SecurityContextUtils.getCurrentUsername()
        );
        User saved = userRepository.save(user);

        auditTrailService.record(
                "IAM",
                "User",
                saved.getId(),
                "USER_CREATED",
                "Created user " + saved.getUsername() + " in organization " + organization.getCode(),
                Map.of("username", saved.getUsername(), "roles", saved.getRoleNames())
        );

        return toUserProfile(saved);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listOrganizations() {
        return organizationRepository.findAll().stream()
                .map(this::toOrganizationResponse)
                .toList();
    }

    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        String code = request.code().trim().toUpperCase();
        if (organizationRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Organization code already exists: " + code);
        }
        if (request.parentId() != null && !organizationRepository.existsById(request.parentId())) {
            throw new ResourceNotFoundException("Parent organization not found: " + request.parentId());
        }

        Organization org = new Organization(
                code,
                request.name(),
                request.parentId(),
                SecurityContextUtils.getCurrentUsername()
        );
        Organization saved = organizationRepository.save(org);

        auditTrailService.record(
                "IAM",
                "Organization",
                saved.getId(),
                "ORGANIZATION_CREATED",
                "Created organization " + saved.getCode() + " (" + saved.getName() + ")",
                Map.of("code", saved.getCode(), "name", saved.getName())
        );

        return toOrganizationResponse(saved);
    }

    public UserProfileResponse toUserProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getOrganization().getId(),
                user.getOrganization().getCode(),
                user.getOrganization().getName(),
                user.isEnabled(),
                user.getRoleNames()
        );
    }

    private OrganizationResponse toOrganizationResponse(Organization org) {
        return new OrganizationResponse(
                org.getId(),
                org.getCode(),
                org.getName(),
                org.getParentId(),
                org.isActive()
        );
    }
}
