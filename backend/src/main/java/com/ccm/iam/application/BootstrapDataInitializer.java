package com.ccm.iam.application;

import com.ccm.iam.domain.Organization;
import com.ccm.iam.domain.Role;
import com.ccm.iam.domain.RoleName;
import com.ccm.iam.domain.User;
import com.ccm.iam.infrastructure.OrganizationRepository;
import com.ccm.iam.infrastructure.RoleRepository;
import com.ccm.iam.infrastructure.UserRepository;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bootstraps initial system accounts without hard-coding credentials in source code or SQL migrations.
 * Reads password from CCM_BOOTSTRAP_ADMIN_PASSWORD or generates a random runtime password if unset.
 */
@Component
public class BootstrapDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapDataInitializer.class);

    private final boolean bootstrapEnabled;
    private final String configuredAdminPassword;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapDataInitializer(
            @Value("${ccm.bootstrap.enabled:true}") boolean bootstrapEnabled,
            @Value("${ccm.bootstrap.admin-password:}") String configuredAdminPassword,
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.bootstrapEnabled = bootstrapEnabled;
        this.configuredAdminPassword = configuredAdminPassword;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled || userRepository.count() > 0) {
            return;
        }
        Organization hq = organizationRepository.findByCode("ORG-HQ")
                .orElseGet(() -> organizationRepository.save(
                        new Organization("ORG-HQ", "Apex Construction Group Headquarters", null, "SYSTEM")
                ));

        String initialPassword = (configuredAdminPassword != null && !configuredAdminPassword.isBlank())
                ? configuredAdminPassword
                : UUID.randomUUID().toString();

        String encoded = passwordEncoder.encode(initialPassword);

        createBootstrapUser("admin", "admin@ccm.enterprise", "System Administrator", hq, encoded,
                Set.of(RoleName.ROLE_SYSTEM_ADMIN));
        createBootstrapUser("pm.alex", "alex.pm@ccm.enterprise", "Alex Mercer (Project Manager)", hq, encoded,
                Set.of(RoleName.ROLE_PROJECT_MANAGER));
        createBootstrapUser("cm.elena", "elena.cm@ccm.enterprise", "Elena Rostova (Contract Manager)", hq, encoded,
                Set.of(RoleName.ROLE_CONTRACT_MANAGER));
        createBootstrapUser("qs.david", "david.qs@ccm.enterprise", "David Chen (Cost Engineer)", hq, encoded,
                Set.of(RoleName.ROLE_COST_ENGINEER));
        createBootstrapUser("fin.sarah", "sarah.fin@ccm.enterprise", "Sarah Jenkins (Finance Officer)", hq, encoded,
                Set.of(RoleName.ROLE_FINANCE_OFFICER));
        createBootstrapUser("exec.victor", "victor.exec@ccm.enterprise", "Victor Vance (Executive Approver)", hq, encoded,
                Set.of(RoleName.ROLE_EXECUTIVE_APPROVER));
        createBootstrapUser("auditor.maya", "maya.audit@ccm.enterprise", "Maya Lin (Internal Auditor)", hq, encoded,
                Set.of(RoleName.ROLE_AUDITOR));

        if (configuredAdminPassword == null || configuredAdminPassword.isBlank()) {
            log.info("Initialized bootstrap accounts (admin, pm.alex, cm.elena, qs.david, fin.sarah, exec.victor, auditor.maya) with generated ephemeral password: {}", initialPassword);
        } else {
            log.info("Initialized bootstrap accounts using configured CCM_BOOTSTRAP_ADMIN_PASSWORD.");
        }
    }

    private void createBootstrapUser(
            String username,
            String email,
            String fullName,
            Organization org,
            String encodedPassword,
            Set<RoleName> roleNames
    ) {
        Set<Role> roles = roleRepository.findByNameIn(roleNames);
        User user = new User(username, email, encodedPassword, fullName, org, roles, "SYSTEM");
        userRepository.save(user);
    }
}
