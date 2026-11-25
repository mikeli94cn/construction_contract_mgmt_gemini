# Construction Contract Management System (CCM Enterprise)

An enterprise construction contract management modular monolith built with **Java 21 LTS**, **Spring Boot 3.4**, **MySQL 8.4 LTS**, **Flyway**, **Vue 3**, **TypeScript**, **Vite**, and **Element Plus**.

## Architecture & Business Modules

Organized as a **modular monolith with package-by-feature** (`com.ccm.<module>.{api,application,domain,infrastructure}`):

1. **`iam`**: Organization units, Users, 7 Enterprise Roles (`SYSTEM_ADMIN`, `PROJECT_MANAGER`, `CONTRACT_MANAGER`, `COST_ENGINEER`, `FINANCE_OFFICER`, `EXECUTIVE_APPROVER`, `AUDITOR`), stateless JWT authentication (`NimbusJwtEncoder`/`NimbusJwtDecoder`).
2. **`project`**: Construction project lifecycle (`PLANNING`, `ACTIVE`, `ON_HOLD`, `COMPLETED`, `CLOSED`), project budgets, and `ProjectAccessGuard` enforcing resource-level project membership authorization (`project_members`).
3. **`partner`**: Business partners (`OWNER`, `GENERAL_CONTRACTOR`, `SUBCONTRACTOR`, `SUPPLIER`, `CONSULTANT`), tax ID uniqueness, and active/suspended/blacklisted eligibility checks.
4. **`contract`**: Contracts (`MAIN_CONTRACT`, `SUBCONTRACT`, `SUPPLY_CONTRACT`, `CONSULTING_CONTRACT`), itemized Bill of Quantities (`contract_line_items`), exact `BigDecimal` (`DECIMAL(19,4)`) net/tax/gross calculations, `@Version` optimistic locking, and strict post-approval financial immutability.
5. **`approval`**: Rule-based multi-step approval workflow engine routing Contracts, Change Orders, Payment Applications, and Final Settlements across roles and monetary thresholds.
6. **`document`**: Polymorphic versioned document attachments with SHA-256 checksum integrity verification.
7. **`changeorder`**: Contract variation orders (`SCOPE_ADDITION`, `SCOPE_DEDUCTION`, `PRICE_ADJUSTMENT`, `TIME_EXTENSION`) that atomically update a contract's approved change totals upon final workflow approval.
8. **`payment`**: Interim Payment Certificates (IPCs) with sequential billing validation, automatic previous-certified deduction, retainage withholding ledger, over-certification protection, and bank disbursement tracking.
9. **`settlement`**: Final account reconciliation, retainage release, warranty retainage holdback, Defect Liability Period tracking, and contract locking (`SETTLED` $\rightarrow$ `CLOSED`).
10. **`audit`**: Immutable append-only `audit_logs` and executive project financial aggregation reports.

Architectural Decision Records are stored in [`docs/adr/`](./docs/adr/).

## Running Automated Tests

### Backend (JUnit 5 + Mockito + Testcontainers MySQL 8.4)
```bash
cd backend
mvn test
```

### Frontend (Vitest + Vue TypeScript Check + Vite Build)
```bash
cd frontend
npm test
npm run build
```

## Running with Docker Compose
1. Copy `.env.example` to `.env` and supply strong database and JWT secrets:
   ```bash
   cp .env.example .env
   ```
2. Start the stack:
   ```bash
   docker compose up --build
   ```
   - Frontend UI: `http://localhost:8023`
   - Backend API & OpenAPI Swagger UI: `http://localhost:8013/swagger-ui.html`
