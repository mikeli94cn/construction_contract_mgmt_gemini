package com.ccm.iam.infrastructure;

import com.ccm.iam.domain.Role;
import com.ccm.iam.domain.RoleName;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
    Set<Role> findByNameIn(Collection<RoleName> names);
}
