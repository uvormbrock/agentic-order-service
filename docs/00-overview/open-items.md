# Open Items and Implementation Backlog

**Status:** Living document  
**Scope:** Agentic Order Service — requirements, architecture, implementation, and verification  
**Last reviewed:** 2026-10-12

## Purpose

This document tracks **unresolved decisions**, **deferred implementation work**, and **verification obligations** across the project. It is an index, not a replacement for the PRD, architecture documents, or ADRs. An item is only closed after its decision or implementation is recorded in the authoritative document and, where applicable, verified by tests.

**Status legend:** `Open` = not yet decided; `Planned` = agreed requirement awaiting implementation; `Proposed / unverified` = documented approach not yet validated; `Done` = completed with evidence.

## Priority Backlog

| ID | Priority | Status | Item | Next action / acceptance evidence | Source |
| --- | --- | --- | --- | --- | --- |
| OI-001 | High | Planned | **Strict REST JSON handling:** Reject unknown fields at the root and within nested `items[]`, client-supplied monetary values, read-only fields, and unsupported activation attempts. | Select a Spring Boot/Jackson-compatible configuration; prove `400` with HTTP-level integration tests. | [REST API Design](../02-architecture/rest-api-design.md), [PRD](../01-requirements/product-requirements.md) |
| OI-002 | High | Planned | **Strict numeric deserialization:** Unknown-property rejection alone does **not** establish rejection of fractional JSON numbers for integer quantities (for example `1.5`). Also verify null, missing, negative, zero, and out-of-range values. | Disable unwanted numeric coercion or validate input explicitly; prove fractional quantity yields `400` and persists no order. | [REST API Design](../02-architecture/rest-api-design.md), PRD AC-005/AC-014 |
| OI-003 | High | Open | **Persistence design:** Decide `OrderItem` identity, domain-to-JPA mapping, foreign keys, uniqueness constraints, and how original product names and monetary results are stored. | Document the schema/mapping; implement Flyway migrations and PostgreSQL-backed integration tests. | [Domain Model](../02-architecture/domain-model.md), [Order Item Snapshots](../02-architecture/order-item-snapshots.md) |
| OI-004 | High | Proposed / unverified | **Concurrency enforcement:** Implement the protocol in ADR-0002 consistently across order placement, product updates/deactivation, and customer updates/deactivation/deletion. | Verify actual JPA locking SQL, deterministic lock order, fresh reads, transaction boundaries, and both winning orders in PostgreSQL/Testcontainers tests. Do **not** mark ADR-0002 implemented before evidence exists. | [ADR-0001](../03-decisions/0001-order-placement-concurrency.md), [ADR-0002](../03-decisions/0002-transaction-and-locking-strategy.md) |
| OI-005 | High | Planned | **Historical order snapshots:** Keep product name, unit price, line amount, subtotal, discount, and total unchanged after catalog or pricing-rule changes; show current customer details on retrieval. | Domain tests plus database-backed tests for renaming, repricing, deactivation, and historical retrieval. | [PRD](../01-requirements/product-requirements.md), [Order Item Snapshots](../02-architecture/order-item-snapshots.md) |
| OI-006 | High | Open | **Monetary storage capacity:** A single line may total **EUR 9,999,999,900.00** (EUR 999,999.99 × 10,000); orders with several different products may total more. | Define sufficient SQL `NUMERIC` precision/scale and Java arithmetic behavior; decide whether any overall item-count/total limits are needed. Do not invent limits without approval. | [Domain Model](../02-architecture/domain-model.md), [PRD](../01-requirements/product-requirements.md) |
| OI-007 | Medium | Planned | **Database and API validation boundaries:** Enforce trimmed names (1–200), normalized email (≤254, case-insensitive unique including inactive customers), price range/scale, and consolidated quantity ≤10,000. | Validate create/update requests and concurrent email uniqueness with HTTP/database integration tests. | [PRD](../01-requirements/product-requirements.md), [REST API Design](../02-architecture/rest-api-design.md) |
| OI-008 | Medium | Open | **Exact email syntax and character counting:** Decide allowed basic email syntax, handling of internationalized/quoted forms, and the convention for counting Unicode characters in limits. | Choose a documented, testable MVP convention and update the PRD before implementation. Existing trim/lowercase/uniqueness rules stay unchanged. | [PRD](../01-requirements/product-requirements.md) |
| OI-009 | Medium | Open | **Framework and persistence specifics:** Select compatible Java 21, Spring Boot, Jackson, JPA provider, PostgreSQL driver/server, and Testcontainers versions. | Record versions, verify strict binding and generated locking SQL against the actual stack. | [REST API Design](../02-architecture/rest-api-design.md), [ADR-0002](../03-decisions/0002-transaction-and-locking-strategy.md) |
| OI-010 | Medium | Open | **Transaction failure policy details:** Set bounded lock/transaction/request timeouts, maximum two additional safe retries, bounded delays, exception classification, and technical error codes. | Document values after evaluating ADR-0002 and prove full retry/rollback behavior. Never retry an uncertain commit automatically. | [ADR-0002](../03-decisions/0002-transaction-and-locking-strategy.md) |
| OI-011 | Medium | Open | **Unknown commit outcome:** Decide practical reconciliation and client-facing handling when confirmation is lost. No idempotency feature is approved for the MVP. | Record the limitation/decision; fault-inject confirmed rollback vs. lost commit confirmation without creating duplicate orders. | [ADR-0001](../03-decisions/0001-order-placement-concurrency.md), [ADR-0002](../03-decisions/0002-transaction-and-locking-strategy.md) |
| OI-012 | Medium | Planned | **Testing documentation:** Consolidate unit, HTTP integration, persistence, and concurrency test strategy without duplicating detailed acceptance criteria. | Create a test strategy in `docs/05-testing/` when implementation begins; link tests to PRD acceptance criteria and ADR verification scenarios. | [PRD](../01-requirements/product-requirements.md), [REST API Design](../02-architecture/rest-api-design.md), [ADR-0002](../03-decisions/0002-transaction-and-locking-strategy.md) |

## Test Evidence Required Before Declaring the MVP Complete

- [ ] Reject unknown JSON fields in root and nested objects (`400`).
- [ ] Reject caller-supplied `unitPrice`, `subtotal`, `discount`, and `total` (`400`).
- [ ] Reject fractional quantity such as `1.5` (`400`), not silently coerce it to an integer.
- [ ] Verify name/email/price/quantity boundary cases and normalized email uniqueness across active/inactive customers.
- [ ] Verify historical `productName`, `unitPrice`, `lineTotal`, `subtotal`, `discount`, and `total` survive product edits and later rule changes.
- [ ] Verify atomic rollback leaves no order or orphaned items.
- [ ] Verify concurrent rename/reprice/deactivation and customer deletion versus placement, including multiple products and both valid operation orders.
- [ ] Verify deadlock/lock-timeout handling, bounded retries, and no automatic replay after an uncertain commit.
- [ ] Verify database numeric capacity for the documented maximum line value and chosen overall order limits.

## Deferred Documentation

These folders may remain sparse until there is real implementation or test evidence; **do not create placeholder documents to make the vault look complete**.

- `docs/04-agentic-coding/`: agent plans, reviewed diffs, rejected proposals, corrections, and lessons learned.
- `docs/05-testing/`: test strategy, reproducible execution instructions, concurrency test evidence.
- `docs/06-operations/`: Docker, local development, CI/CD, deployment and troubleshooting.

## Maintenance Rules

1. Reference the authoritative PRD/ADR/design document instead of copying its full content here.
2. When a decision is made, update its authoritative document first; then change the item's status and link to the decision.
3. When an implementation is verified, include the test location or command and test result before marking `Done`.
4. Preserve ADR-0001 as the accepted business policy and ADR-0002 as **Proposed / implementation-unverified** until the required verification is complete.
5. Do not turn an open question into a requirement without human approval.
