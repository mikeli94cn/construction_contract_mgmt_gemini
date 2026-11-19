package com.ccm.project.infrastructure;

import com.ccm.project.domain.Project;
import com.ccm.project.domain.ProjectStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByProjectCode(String projectCode);

    @EntityGraph(attributePaths = {"organization"})
    Optional<Project> findWithOrganizationById(Long id);

    @EntityGraph(attributePaths = {"organization"})
    @Query("""
            SELECT p FROM Project p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.projectCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Project> searchAllProjects(
            @Param("status") ProjectStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"organization"})
    @Query("""
            SELECT p FROM Project p
            WHERE EXISTS (
                SELECT 1 FROM ProjectMember pm
                WHERE pm.project = p AND pm.user.id = :userId
            )
              AND (:status IS NULL OR p.status = :status)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.projectCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Project> searchMemberProjects(
            @Param("userId") Long userId,
            @Param("status") ProjectStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
