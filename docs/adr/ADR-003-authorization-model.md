# ADR-003: Role-Based and Resource-Level Authorization

## Status
Accepted

## Context
In a multi-project construction enterprise, having a role such as `CONTRACT_MANAGER` or `PROJECT_MANAGER` should not automatically grant write or read access to every project in the enterprise. Access must combine role capabilities (RBAC) with project membership (resource-level ACL).

## Decision
1. Authenticate API requests statelessly using signed JWT tokens via Spring Security OAuth2 Resource Server (`NimbusJwtDecoder`/`NimbusJwtEncoder`).
2. Enforce **Role-Based Access Control (RBAC)** using method security (`@PreAuthorize`) across the system roles:
   - `ROLE_SYSTEM_ADMIN`
   - `ROLE_PROJECT_MANAGER`
   - `ROLE_CONTRACT_MANAGER`
   - `ROLE_COST_ENGINEER`
   - `ROLE_FINANCE_OFFICER`
   - `ROLE_EXECUTIVE_APPROVER`
   - `ROLE_AUDITOR`
3. Enforce **Resource-Level Authorization** via a `ProjectAccessGuard` bean (`@projectAccessGuard.canViewProject(authentication, #projectId)` and `@projectAccessGuard.canManageProjectContracts(authentication, #projectId)`):
   - `ROLE_SYSTEM_ADMIN`, `ROLE_AUDITOR`, and `ROLE_EXECUTIVE_APPROVER` have global read visibility across projects (and `ROLE_FINANCE_OFFICER` for financial verification).
   - Project Managers, Contract Managers, and Cost Engineers must be assigned to the project in `project_members` to view or mutate project-scoped resources.

## Consequences
- Fine-grained multi-project isolation enforced consistently on the server.
