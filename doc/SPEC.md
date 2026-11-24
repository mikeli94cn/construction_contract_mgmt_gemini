# Construction Contract Management System — AI Coding Agent Instructions

You are a senior Java enterprise architect and full-stack engineer.

Build a construction contract management system using mature enterprise software engineering practices.

## Technology stack

- Java 21 LTS
- Spring Boot (select a currently supported compatible version)
- Spring MVC
- Spring Security
- Spring Data JPA / Hibernate
- MySQL 8.4 LTS
- Flyway
- Maven
- Vue 3, TypeScript, Vite, Element Plus
- Docker Compose
- JUnit 5, Mockito, Testcontainers
- OpenAPI

## Architecture

Use a modular monolith with package-by-feature organization.

Each business module should have clear API, application, domain, and infrastructure responsibilities.

Business modules:

1. Organization and user management
2. Project management
3. Business partner management
4. Contract management
5. Approval workflow
6. Document management
7. Change order management
8. Payment management
9. Final settlement
10. Audit and reporting

## Engineering requirements

- Follow SOLID principles pragmatically.
- Keep controllers thin.
- Use DTOs for public APIs.
- Enforce business rules on the server.
- Use BigDecimal for monetary amounts.
- Use database transactions for consistency.
- Use optimistic locking where appropriate.
- Implement role-based and resource-level authorization.
- Use Flyway migrations instead of automatic production schema updates.
- Provide meaningful automated tests.
- Never hard-code credentials.
- Never silently modify approved financial records.
- Document architectural decisions.

## Development process

Do not generate the entire system immediately.

First:

1. Analyze the business requirements and identify ambiguities.
2. Propose the system architecture.
3. Define business modules and their dependencies.
4. Design the initial database schema.
5. Define the MVP REST APIs.
6. Create a phased implementation plan.

Then implement one vertical slice at a time, beginning with authentication, project management, business partners, and contract creation.

For each completed slice:

- Implement backend functionality.
- Implement frontend functionality.
- Add database migrations.
- Add automated tests.
- Run the build and tests.
- Report what was verified and what remains incomplete.

Do not claim that code was tested unless the tests actually ran successfully.

Start by producing the architecture, database design, and implementation plan. Wait for my approval before generating the application code.
