# ADR-002: Financial Precision, Immutability of Approved Records, and Optimistic Locking

## Status
Accepted

## Context
Construction contracts involve multi-million-dollar commitments, tax rates, retainage deductions, and progress certifications. Floating-point inaccuracies, silent post-approval edits, or concurrent modification race conditions can cause severe financial and audit failures.

## Decision
1. **BigDecimal & DECIMAL(19,4)**: All monetary amounts use `java.math.BigDecimal` scaled to 4 decimal places (`DECIMAL(19,4)` in MySQL 8.4) for database storage and 2–4 decimal places with `RoundingMode.HALF_UP` for currency calculations via a centralized `MoneyUtils` helper.
2. **Strict Immutability of Approved Financial Records**: Once a Contract, Change Order, Payment Application, or Final Settlement leaves `DRAFT`/`REJECTED` into `IN_APPROVAL`, `APPROVED`, `ACTIVE`, `PAID`, or `SETTLED`, direct mutation of financial fields is rejected by the domain entity (`BusinessRuleViolationException`). Post-approval adjustments must happen via approved Change Orders or subsequent Payment/Settlement records.
3. **Optimistic Locking**: Every mutable aggregate entity includes a JPA `@Version` (`BIGINT NOT NULL DEFAULT 0`) column to detect concurrent modifications and fail fast with HTTP 409 Conflict.
4. **Append-Only Audit Log**: Every state change and financial mutation writes an immutable record to `audit_logs`.

## Consequences
- Eliminates floating-point rounding drift and silent financial tampering.
- Guarantees full traceability for internal and external construction auditors.
