# ADR-001: Modular Monolith with Package-by-Feature Organization

## Status
Accepted

## Context
Construction contract management encompasses tightly coupled financial invariants across Projects, Partners, Contracts, Approvals, Change Orders, Progress Payments, Final Settlements, Documents, and Audit Logging. Distributed microservices introduce operational and distributed-transaction complexity without proportional benefit at this stage, whereas an unstructured monolith risks spaghetti dependencies.

## Decision
1. Build the backend as a **Modular Monolith** under `com.ccm`, organized by business feature module (`common`, `iam`, `project`, `partner`, `contract`, `approval`, `document`, `changeorder`, `payment`, `settlement`, `audit`).
2. Within each module, separate responsibilities into four explicit sub-packages:
   - `api`: Thin REST controllers and public request/response DTO records with Bean Validation.
   - `application`: Transactional use-case services, authorization checks, and orchestration.
   - `domain`: Entities, value objects, state machines, and business rule validation.
   - `infrastructure`: Spring Data JPA repositories and persistence adapters.
3. Enforce a Directed Acyclic Graph (DAG) of dependencies between modules.

## Consequences
- Single deployable artifact with strong ACID database transactions across financial aggregates.
- Clear boundaries make future extraction of individual modules (e.g., Document storage or Reporting) straightforward.
