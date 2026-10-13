# Open Items and Implementation Backlog

**Status:** Living document  
**Scope:** Agentic Order Service — requirements, architecture, implementation, and verification  
**Last reviewed:** 2026-10-13

## Purpose

This document tracks **unresolved decisions**, **deferred implementation work**, and **verification obligations** across the project. It is an index, not a replacement for the PRD, architecture documents, or ADRs. Decision acceptance and implementation completion are separate. Close a decision when explicitly accepted and recorded; close implementation work only with applicable test evidence. See the [system architecture](../02-architecture/system-architecture.md) for the authoritative ADR status matrix.

**Status legend:** `Open` = not yet decided; `Planned` = agreed requirement awaiting implementation; `Proposed / unverified` = documented approach not yet validated; `Done` = completed with evidence.

## Priority Backlog

| ID | Priority | Status | Item | Next action / acceptance evidence | Source |
| --- | --- | --- | --- | --- | --- |
| OI-001 | High | Planned | **Strict REST JSON handling:** Reject unknown fields at the root and within nested `items[]`, client-supplied monetary values, read-only fields, and unsupported activation attempts. | Select a Spring Boot/Jackson-compatible configuration; prove `400` with HTTP-level integration tests. | [REST API Design](../02-architecture/rest-api-design.md), [PRD](../01-requirements/product-requirements.md) |
| OI-002 | High | Planned | **Strict numeric deserialization:** Unknown-property rejection alone does **not** establish rejection of fractional JSON numbers for integer quantities (for example `1.5`). Also verify null, missing, negative, zero, and out-of-range values. | Disable unwanted numeric coercion or validate input explicitly; prove fractional quantity yields `400` and persists no order. | [REST API Design](../02-architecture/rest-api-design.md), PRD AC-005/AC-014 |
| OI-003 | High | Open | **Persistence details:** Direct JPA and immutable values are accepted. Converter conventions remain Proposed; OrderItem identity, concrete mappings/schema, and monetary storage remain Open. ADR-0002 proposes restrictive referential and normalized-email constraints. | Approve mapping/schema and Flyway execution strategy, then separately approve migrations and verify PostgreSQL round trips; see [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md). | [Domain Model](../02-architecture/domain-model.md), [Order Item Snapshots](../02-architecture/order-item-snapshots.md) |
| OI-004 | High | Proposed / unverified | **Concurrency enforcement:** Detailed ADR-0002 remains Proposed; READ COMMITTED, pessimistic locking, and TransactionTemplate are adopted. After scoped approval implement the protocol consistently across order placement, product updates/deactivation, and customer updates/deactivation/deletion. | Verify actual JPA locking SQL, deterministic lock order, fresh reads, transaction boundaries, and both winning orders in PostgreSQL/Testcontainers tests. Do **not** mark ADR-0002 implemented before evidence exists. | [ADR-0001](../03-decisions/ADR-0001-order-placement-concurrency.md), [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) |
| OI-005 | High | Planned | **Historical order snapshots:** Keep product name, unit price, line amount, subtotal, discount, and total unchanged after catalog or pricing-rule changes; show current customer details on retrieval. | Domain tests plus database-backed tests for renaming, repricing, deactivation, and historical retrieval. | [PRD](../01-requirements/product-requirements.md), [Order Item Snapshots](../02-architecture/order-item-snapshots.md) |
| OI-006 | High | Open | **Monetary storage capacity:** A single line may total **EUR 9,999,999,900.00** (EUR 999,999.99 × 10,000); orders with several different products may total more. | Define sufficient SQL `NUMERIC` precision/scale and Java arithmetic behavior; decide whether any overall item-count/total limits are needed. Do not invent limits without approval. | [Domain Model](../02-architecture/domain-model.md), [PRD](../01-requirements/product-requirements.md) |
| OI-007 | Medium | Planned | **Database and API validation boundaries:** Enforce trimmed names (1–200), normalized email (≤254, case-insensitive unique including inactive customers), price range/scale, and consolidated quantity ≤10,000. | Validate create/update requests and concurrent email uniqueness with HTTP/database integration tests. | [PRD](../01-requirements/product-requirements.md), [REST API Design](../02-architecture/rest-api-design.md) |
| OI-008 | Medium | Open | **Exact email syntax and character counting:** Decide allowed basic email syntax, handling of internationalized/quoted forms, and the convention for counting Unicode characters in limits. | Choose a documented, testable MVP convention and update the PRD before implementation. Existing trim/lowercase/uniqueness rules stay unchanged. | [PRD](../01-requirements/product-requirements.md) |
| OI-009 | Medium | Open | **Framework and persistence specifics:** Java 21 and Spring Boot 4.1.1 are Accepted. The foundation dependency graph is verified; PostgreSQL 17.11/image and Maven Wrapper/Failsafe are approved. Remaining provider behavior for domain mappings, strict binding, and concurrency still needs verification. JUnit 6 is Accepted with Boot-managed Jupiter/Platform versions; the earlier JUnit 5 selection is superseded. | Managed JUnit 6 foundation graph verified in Increment 2; record remaining feature dependency evidence under [ADR-0005](../03-decisions/ADR-0005-technology-stack.md), then verify strict binding and locking SQL. | [REST API Design](../02-architecture/rest-api-design.md), [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) |
| OI-010 | Medium | Open | **Transaction failure details:** ADR-0002 proposes at most two additional safe attempts and failure responses. Concrete timeout/budget/delay values, root-cause classification, and technical codes remain Open; ambient rejection and transaction-scoped fresh state are required. | Document values after evaluating ADR-0002 and prove full retry/rollback behavior. Never retry an uncertain commit automatically. | [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) |
| OI-011 | Medium | Open | **Unknown commit outcome:** No automatic replay is Accepted. ADR-0002 proposes 500 when possible or no response on transport failure. Reconciliation and detailed technical codes remain Open; no idempotency feature is approved. | Record the limitation/decision; fault-inject confirmed rollback vs. lost commit confirmation without creating duplicate orders. | [ADR-0001](../03-decisions/ADR-0001-order-placement-concurrency.md), [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) |
| OI-012 | Medium | Done (documentation) | **Testing documentation:** The authoritative [testing strategy](../05-testing/testing-strategy.md) consolidates quality gates and PostgreSQL scenarios. Test implementation remains pending under other items. | Strategy recorded during the documentation-baseline task; no executed-test evidence is claimed. | [PRD](../01-requirements/product-requirements.md), [REST API Design](../02-architecture/rest-api-design.md), [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) |

## Additional Open Implementation Gates

- Value-object converter acceptance and detailed schema review: [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md).
- Flyway migration execution/versioning/privilege/validation strategy: [ADR-0005](../03-decisions/ADR-0005-technology-stack.md). Flyway itself is Accepted.
- Monetary precision/scale, overall order size and total limits: OI-006; a decision that no extra limits are needed is also possible.
- Test framework selection is resolved: JUnit 6 explicitly replaces JUnit 5, using Boot 4.1.1-managed Jupiter/Platform versions. Java 21 and Boot 4.1.1 remain Accepted; foundation build verification passed, later implementation gates remain Pending.

## Test Evidence Required Before Declaring the MVP Complete

- [ ] Reject unknown JSON fields in root and nested objects (`400`).
- [ ] Reject caller-supplied `unitPrice`, `subtotal`, `discount`, and `total` (`400`).
- [ ] Reject fractional quantity such as `1.5` (`400`), not silently coerce it to an integer.
- [ ] Verify name/email/price/quantity boundary cases and normalized email uniqueness across active/inactive customers.
- [ ] Verify historical `productName`, `unitPrice`, `lineTotal`, `subtotal`, `discount`, and `total` survive product edits and later rule changes.
- [ ] Verify atomic rollback leaves no order or orphaned items.
- [ ] Verify concurrent rename/reprice/deactivation and customer deletion versus placement, including multiple products and both valid operation orders.
- [ ] Verify deadlock/lock-timeout handling, bounded retries, and no automatic replay after an uncertain commit.
- [ ] Verify database numeric capacity for the documented maximum line value and the approved decision on whether overall order limits are needed.

## Deferred Documentation

These folders may remain sparse until there is real implementation or test evidence; **do not create placeholder documents to make the vault look complete**.

- `docs/04-agentic-coding/`: agent plans, reviewed diffs, rejected proposals, corrections, and lessons learned.
- `docs/05-testing/testing-strategy.md` is the authoritative strategy; `docs/05-testing/` may later contain reproducible execution and concurrency evidence.
- `docs/06-operations/`: Docker, local development, CI/CD, deployment and troubleshooting.

## Maintenance Rules

1. Reference the authoritative PRD/ADR/design document instead of copying its full content here.
2. When a decision is made, update its authoritative document first; then change the item's status and link to the decision.
3. When an implementation is verified, include the test location or command and test result before marking `Done`.
4. Preserve ADR-0001 as Accepted and ADR-0002 as Proposed until explicit human acceptance. Test verification alone does not change ADR status; record implementation evidence separately.
5. Do not turn an open question into a requirement without human approval.

## Increment 2 Scoped Decision Update

PostgreSQL 17.11 and `postgres:17.11-bookworm`, Maven Failsafe, and local/integration-test Flyway startup with metadata initialization are explicitly approved under [ADR-0005](../03-decisions/ADR-0005-technology-stack.md#increment-2-approval-and-history). These resolve the corresponding portions of OI-009 and the migration execution gate only for local/test environments. Production migration execution, actual domain migration/schema choices, privileges, and repair strategy remain Open. OI-003/006/008/010/011 and ADR-0002 retain their unresolved scope.

The [Increment 2 record](../04-agentic-coding/prompts/03-postgresql-foundation.md) owns actual database foundation evidence; complete domain/persistence/concurrency verification remains Pending. Earlier baseline statements about absent builds are historical, not the current Increment 1 result.
