package com.ccm.iam.infrastructure;

import com.ccm.iam.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"organization", "roles"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"organization", "roles"})
    Optional<User> findWithDetailsById(Long id);

    @Override
    @EntityGraph(attributePaths = {"organization", "roles"})
    List<User> findAll();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
