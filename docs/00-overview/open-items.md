# Open Items and Implementation Backlog

**Status:** Living document  
**Scope:** Agentic Order Service — requirements, architecture, implementation, and verification  
**Last reviewed:** 2026-10-14

## Purpose

This document tracks **unresolved decisions**, **deferred implementation work**, and **verification obligations** across the project. It is an index, not a replacement for the PRD, architecture documents, or ADRs. Decision acceptance and implementation completion are separate. Close a decision when explicitly accepted and recorded; close implementation work only with applicable test evidence. See the [system architecture](../02-architecture/system-architecture.md) for the authoritative ADR status matrix.

**Status legend:** `Open` = not yet decided; `Planned` = agreed requirement awaiting implementation; `Proposed / unverified` = documented approach not yet validated; `Done` = completed with evidence; `Resolved (decision)` = explicitly accepted policy, without an implementation claim; `Executed / review pending` = scoped automated verification passed, human acceptance outstanding.

## Priority Backlog

| ID | Priority | Status | Item | Next action / acceptance evidence | Source |
| --- | --- | --- | --- | --- | --- |
| OI-001 | High | Planned | **Strict REST JSON handling:** Reject unknown fields at the root and within nested `items[]`, client-supplied monetary values, read-only fields, and unsupported activation attempts. | Global strict JSON is Accepted for Increment 4; review the proposed Boot 4/Jackson 3 configuration and prove `400` with actual MVC tests. Nested Order API evidence remains deferred. | [REST API Design](../02-architecture/rest-api-design.md), [PRD](../01-requirements/product-requirements.md) |
| OI-002 | High | Planned | **Strict numeric deserialization:** Unknown-property rejection alone does **not** establish rejection of fractional JSON numbers for integer quantities (for example `1.5`). Also verify null, missing, negative, zero, and out-of-range values. | Disable unwanted numeric coercion or validate input explicitly; prove fractional quantity yields `400` and persists no order. | [REST API Design](../02-architecture/rest-api-design.md), PRD AC-005/AC-014 |
| OI-003 | High | Done (Increment 3) | **Accepted persistence design:** D1–D10 select generated UUIDs, autoApply converters, owned lazy/PERSIST-only items, unconstrained NUMERIC, all-customer lower(email) uniqueness, four tables/constraints/indexes and reference entity equality. | Increment 3 Reviewed – Accepted; 120 fast/21 PostgreSQL tests passed. Later application/concurrency gates remain separate. | [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md), [Increment 3](../04-agentic-coding/prompts/04-domain-model.md) |
| OI-004 | High | Proposed / unverified | **Concurrency enforcement:** Detailed ADR-0002 remains Proposed; READ COMMITTED, pessimistic locking, and TransactionTemplate are adopted. After scoped approval implement the protocol consistently across order placement, product updates/deactivation, and customer updates/deactivation/deletion. | Verify actual JPA locking SQL, deterministic lock order, fresh reads, transaction boundaries, and both winning orders in PostgreSQL/Testcontainers tests. Do **not** mark ADR-0002 implemented before evidence exists. | [ADR-0001](../03-decisions/ADR-0001-order-placement-concurrency.md), [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) |
| OI-005 | High | Planned | **Historical order snapshots:** Keep product name, unit price, line amount, subtotal, discount, and total unchanged after catalog or pricing-rule changes; show current customer details on retrieval. | Domain tests plus database-backed tests for renaming, repricing, deactivation, and historical retrieval. | [PRD](../01-requirements/product-requirements.md), [Order Item Snapshots](../02-architecture/order-item-snapshots.md) |
| OI-006 | High | Done (Increment 3) | **Accepted monetary storage:** Unconstrained NUMERIC, finite/scale ≤2 and invariant-specific non-negative checks; no artificial Order-total cap. Maximum line EUR 9,999,999,900.00; multiple products can exceed it. | Unconstrained columns, Hibernate validation, exact maximum line/two-line round trips and negative checks passed in Increment 3; Increment 3 Reviewed – Accepted. Additional overall order limits require approval. | [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md), [Domain Model](../02-architecture/domain-model.md) |
| OI-007 | Medium | Planned | **Database and API validation boundaries:** Enforce trimmed names (1–200), normalized email (≤254, case-insensitive unique including inactive customers), price range/scale, and consolidated quantity ≤10,000. | Validate create/update requests and concurrent email uniqueness with HTTP/database integration tests. | [PRD](../01-requirements/product-requirements.md), [REST API Design](../02-architecture/rest-api-design.md) |
| OI-008 | Medium | Resolved (decision) | **Name/email conventions:** Names use String.strip(), 1–200 Unicode code points, preserving case/Unicode. Approved ASCII email normalization/counting and inactive-inclusive uniqueness remain unchanged. | Implement and verify boundaries; SQL lower(email) enforcement, ASCII normalization/uniqueness and Unicode names passed scoped PostgreSQL tests; Increment 3 Reviewed – Accepted. Concurrent API handling remains Pending. | [PRD](../01-requirements/product-requirements.md), [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md) |
| OI-009 | Medium | Open | **Framework and persistence specifics:** Java 21 and Spring Boot 4.1.1 are Accepted. The foundation dependency graph is verified; PostgreSQL 17.11/image and Maven Wrapper/Failsafe are approved. Remaining provider behavior for domain mappings, strict binding, and concurrency still needs verification. JUnit 6 is Accepted with Boot-managed Jupiter/Platform versions; the earlier JUnit 5 selection is superseded. | Managed JUnit 6 foundation graph verified in Increment 2; record remaining feature dependency evidence under [ADR-0005](../03-decisions/ADR-0005-technology-stack.md), then verify strict binding and locking SQL. | [REST API Design](../02-architecture/rest-api-design.md), [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) |
| OI-010 | Medium | Open | **Transaction failure details:** ADR-0002 proposes at most two additional safe attempts and failure responses. Concrete timeout/budget/delay values, root-cause classification, and technical codes remain Open; ambient rejection and transaction-scoped fresh state are required. | Document values after evaluating ADR-0002 and prove full retry/rollback behavior. Never retry an uncertain commit automatically. | [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) |
| OI-011 | Medium | Open | **Unknown commit outcome:** No automatic replay is Accepted. ADR-0002 proposes 500 when possible or no response on transport failure. Reconciliation and detailed technical codes remain Open; no idempotency feature is approved. | Record the limitation/decision; fault-inject confirmed rollback vs. lost commit confirmation without creating duplicate orders. | [ADR-0001](../03-decisions/ADR-0001-order-placement-concurrency.md), [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) |
| OI-012 | Medium | Done (documentation) | **Testing documentation:** The authoritative [testing strategy](../05-testing/testing-strategy.md) consolidates quality gates and PostgreSQL scenarios. Test implementation remains pending under other items. | Strategy recorded during the documentation-baseline task; no executed-test evidence is claimed. | [PRD](../01-requirements/product-requirements.md), [REST API Design](../02-architecture/rest-api-design.md), [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) |

## Increment 4 Accepted Decisions and Remaining Gates

**Increment 4 Executed — Pending Human Review.** [Current playbook](../04-agentic-coding/prompts/05-product-customer-api.md#approved-architecture-and-api-decisions--2026-10-14) supersedes the initial review where A/B1–B11 explicitly decide behavior. D1–D10 and Increment 3 acceptance remain intact; ADR-0002 remains Proposed/unverified.

| ID | Status | Decision / remaining action | Source |
| --- | --- | --- | --- |
| OI-013 | Resolved (behavior); technical verification Pending | Strict JSON types/no coercion, canonical UUIDs, ordinary decimal price syntax/no exponents, PATCH and six-category precedence accepted. Final D1–D8 settle body/duplicate/trailing/order/case/media policies; verify exact Boot/Jackson/interceptor/decoder with actual MVC tests, not repeat policy selection. | [REST design](../02-architecture/rest-api-design.md#10-human-accepted-increment-4-api-and-transaction-decisions) |
| OI-014 | Resolved (catalog); mechanics Open | All current code/status mappings accepted, including specific media/route/technical failures. Review safe incompatible-Accept/error-dispatch JSON, confirmed cause chains, selected establishment/pre-write outage classifier and deterministic processing; no broad SQL→409/503. | [API catalog](../02-architecture/api-error-codes.md) |
| OI-015 | Resolved (interim response); Planned — deferred implementation | Customer DELETE remains future MVP delete-or-deactivate/204 with Order coordination. Interim OPERATION_NOT_AVAILABLE/405 and accurate Allow accepted; no temporary reference check/delete. | PRD FR-002, AC-003/004/013; [ADR-0001](../03-decisions/ADR-0001-order-placement-concurrency.md), [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) |
| OI-016 | Resolved (transaction and test approach); verification Pending | Application writes @Transactional, readOnly reads, immutable materialization and outside-boundary error translation accepted. Verify proxies/commit causes; approved minimal builder-based MockMvc uses existing spring-test dependencies, no new test starter. | [Current plan](../07-implementation/implementation-plan.md#increment-4--product--customer-application-services-and-rest-api) |
| OI-017 | Resolved (direction and three technical decisions); verification Pending | Exact boxed Long mapping, BIGINT/NOT NULL/existing-row 0/no final default and sequential no-update/no-increment PATCH accepted. Complete ADR Proposed; reviewed V2 proposal execution approval, provider/newness and real seven-scenario conflict/no-op evidence remain pending. | [ADR-0006](../03-decisions/ADR-0006-product-customer-optimistic-locking.md) |

Resolved: all eight final Increment 4 edge policies (distinct from persistence D1–D10), exact version mapping/schema/no-op policy, reviewed V2 SQL proposal and no-new-dependency MockMvc approach. The [definitive current implementation prompt](../04-agentic-coding/prompts/05-product-customer-api.md#current-implementation-prompt--increment-4) includes their complete gated scope. Coding authorization was explicitly granted and the scoped implementation/verification executed; see the current evidence below. Remaining: explicit human implementation acceptance and, independently, complete ADR-0006 status acceptance; persistent local target/recovery/migration evidence; deferred Customer DELETE; Order ADR-0002 and production deployment/execution/permissions/rollout. Runtime Pending items are evidence obligations, not reopened accepted choices. Documentation approval alone closes no runtime gate; actual disposable migration evidence is recorded below, independently of the blocked persistent local gate.

## Additional Open Implementation Gates

- D1–D9 selections are Accepted; concrete SQL review and scoped runtime verification passed (Increment 3 Reviewed – Accepted) under [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md).
- D10 local/test execution/versioning/validation policy is Accepted; production execution/privileges remain Open under [ADR-0005](../03-decisions/ADR-0005-technology-stack.md).
- OI-006 now tracks evidence for Accepted unconstrained NUMERIC, not a precision selection. No new fixed total bound is approved; any additional overall order-size rule requires approval.
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

PostgreSQL 17.11 and `postgres:17.11-bookworm`, Maven Failsafe, and local/integration-test Flyway startup with metadata initialization are explicitly approved under [ADR-0005](../03-decisions/ADR-0005-technology-stack.md#increment-2-approval-and-history). These resolve the corresponding portions of OI-009 and the migration execution gate only for local/test environments. At that approval, production and domain schema/migration policies were unresolved. Later D1–D10 approval resolves reviewed local/test schema, versioning, no-automatic-repair policy and name conventions; production privileges/execution and OI-010/011/ADR-0002 remain unresolved.

The [Increment 2 record](../04-agentic-coding/prompts/03-postgresql-foundation.md) owns actual database foundation evidence; Increment 3 supplies scoped domain/persistence evidence; application/concurrency gates remain Pending. Earlier baseline statements about absent builds are historical, not the current Increment 1 result.

## Initial Increment 3 Domain Review History

The human reviewer explicitly accepted **D6 Java Money semantics** and **D7 EmailAddress validation** while consolidating Increment 3 as one Domain Model & Persistence increment. The [domain model](../02-architecture/domain-model.md#4-value-objects) records Money semantics; the [PRD](../01-requirements/product-requirements.md#approved-email-validation-policy) owns email grammar, deterministic normalization, and the 254 Unicode-code-point limit.

At that stage only Java Money/email behavior was resolved; SQL storage, names, identity, mappings and migrations were still awaiting review. The subsequent D1–D10 approval below supersedes that earlier Open state. No implementation or migration execution occurred; ADR-0002 remains Proposed and implementation-unverified.

See the single [Increment 3 planning, approved prompt and execution evidence](../04-agentic-coding/prompts/04-domain-model.md). The later explicit D1–D10 approval resolves the remaining selected design policies; unrelated Open questions are not closed.

## Accepted D1–D10 and Remaining Execution Gates

The human reviewer accepted Variant A for every D1–D10 decision. [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md#accepted-increment-3-selections-and-replacement-history) owns mappings/schema; [ADR-0005](../03-decisions/ADR-0005-technology-stack.md#accepted-increment-3-localtest-migration-policy) owns local/test migration policy. Design decisions are resolved; Increment 3 implementation and V1 migration/schema tests passed, followed by hardening to 120 fast/21 PostgreSQL tests. Increment 3 is Reviewed – Accepted after explicit human acceptance. See the [Increment 3 execution record](../04-agentic-coding/prompts/04-domain-model.md#implementation-results--executed-pending-human-review).

Concrete SQL was reviewed and scoped Java/PostgreSQL normalization, converter and NUMERIC compatibility tests passed. OI-005/007 still include later current-customer retrieval, HTTP and concurrency obligations, beyond the passed domain/persistence subset. If those expose a genuinely unselected setting or new requirement, stop for approval rather than silently selecting it. List/pagination fetching, production migration execution/privileges, deployment/security, enforcement tooling and detailed concurrency configuration stay deferred/Open. ADR-0002 is unchanged. No new total or overall order-size restriction is introduced.

### Increment 4 Persistent Local Migration Gate

Coding authorization has been granted; implementation and disposable verification executed, human review Pending. After authorization, persistent local migration requires actual target/history/schema and usable documented backup/restore evidence under the [current implementation prompt](../04-agentic-coding/prompts/05-product-customer-api.md#persistent-local-database-safety-gate). Repository inspection has not established a documented recovery procedure; this does not prove that no external backup exists. If no usable mechanism can be established at execution time, report local migration as Blocked — Human Action Required and request safe-recovery authorization while continuing disposable Testcontainers tests/other approved work. ADR-0006 acceptance is a separate status decision, not another general coding gate for expressly approved choices.

### Increment 4 Verification Update — Pending Human Review

[Increment 4 execution evidence](../04-agentic-coding/prompts/05-product-customer-api.md#implementation-results--executed-pending-human-review) supersedes the pre-execution Pending verification descriptions in OI-013/014/016/017 for the implemented scope: actual strict MVC/error, application proxy/commit and Product/Customer version/migration/race tests passed in the final 240-fast/44-integration run. These are execution results awaiting human acceptance, not ADR acceptance. OI-015 Customer deletion remains deferred; interim 405/Allow is implemented and tested. Persistent local migration remains Blocked — Human Action Required: no running target/history/schema or usable documented recovery procedure was established. Production execution, full Order protocol, separate ADR-0006 status acceptance and commit authorization remain outstanding.
