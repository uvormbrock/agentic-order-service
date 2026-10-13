# Testing Strategy

**Status:** Approved quality requirements; test implementation pending

## Scope and Prerequisites

The [PRD acceptance criteria](../01-requirements/product-requirements.md) define behavior; [ADR-0001](../03-decisions/ADR-0001-order-placement-concurrency.md) defines concurrency outcomes. The [Proposed ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) defines technical verification scenarios. Resolve JUnit 5 versus Boot 4.1.1 managed JUnit 6 under [ADR-0005](../03-decisions/ADR-0005-technology-stack.md) before creating the test build.

## Quality Gate Applicability

Applicable gates are mandatory for the approved increment. All remaining gates are future obligations and must be reported as **Pending**, not Passed. A gate is Passed only with actual successful verification evidence; a required gate that cannot run remains Pending and blocks the corresponding readiness claim. Human acceptance does not manufacture missing evidence.

| Increment type | Mandatory applicable gates | Future gates |
| --- | --- | --- |
| Documentation only | Documentation links, structure, ADR IDs/statuses, preserved rules, diff checks and human review | Build, domain/application, persistence/migrations, and concurrency: Pending |
| Technical skeleton | Documentation; Java 21/Boot 4.1.1 dependency alignment; PostgreSQL context startup; OSIV disabled; tests for any implemented transaction guard/template | Unimplemented business, mappings/migrations, and concurrency: Pending |
| Domain and application | Documentation/build; relevant domain rules, HTTP validation/error behavior, transaction ownership and DTO materialization for implemented use cases | Unimplemented features and unrelated persistence/concurrency scenarios: Pending |
| Persistence and migrations | Documentation/build; approved migration/schema validation, PostgreSQL round trips, constraints/capacity, atomic rollback and fetching tests for affected mappings | Unimplemented workflows and full concurrency protocol: Pending |
| Concurrency | All prerequisite implemented-layer gates plus deterministic PostgreSQL races, retries and uncertain-outcome handling | Any uncovered required scenario: Pending; no full concurrency-verification claim |

Before declaring the MVP complete, every required implementation gate must have passing evidence and every implementation increment must have explicit human acceptance. ADR-0002 acceptance remains a separate explicit decision. No migrations or test code are authorized by this strategy.

## Quality Gates

| Gate | Required evidence |
| --- | --- |
| Documentation | Working links, unique ADR identifiers, correct statuses, preserved approved rules, reviewed diff |
| Build | Java 21 compile, compatible Boot 4.1.1 dependency graph, approved test-engine alignment |
| Domain | Pricing/consolidation/limits, EUR 99.99/100.00/100.05 discount cases, immutable snapshot behavior |
| HTTP | Actual request binding; unknown root/nested fields, price overrides, fractional/missing/null quantities, all approved boundary cases and error shape |
| Persistence | PostgreSQL round trips, reviewed schema/migrations, constraints, email uniqueness, monetary capacity, rollback, no lazy access after DTO return |
| Transaction infrastructure | Reject ambient coordinator entry, READ COMMITTED, fresh attempt contexts, shared helper transaction, commit-time exception observation |
| Concurrency | Deterministic PostgreSQL tests of the scenarios below, using actual JPA/provider paths |

Use unit tests for domain behavior and HTTP integration tests through real JSON binding. PostgreSQL/Testcontainers is required for persistence/concurrency evidence, not H2 or mocked repositories. Use reviewed migrations in disposable databases only after migration approval. A context smoke test does not establish locking correctness. No arbitrary coverage percentage replaces meaningful gates.

## Deterministic Concurrency Matrix

- Placement versus product repricing and renaming, each in both winning orders; inspect original names/prices and all totals.
- Placement versus product/customer deactivation, both orders; success before deactivation or 409 with no order afterward.
- Physical customer deletion versus first order, both orders; force deletion to wait, then prove its subsequent query sees the committed reference and deactivates. Delete-first placement yields 404.
- Multi-product orders with reversed request order/different customers and coordinated updates; inspect actual acquisition order and capture no incompatible mixture.
- Confirmed holder rollback, partial-save failure, and restrictive foreign keys; no partial orders or orphaned items.
- Controlled deadlock/lock timeout; confirmed full rollback, bounded attempts, fresh reads, exhaustion behavior, no individual-statement retries.
- Lost commit confirmation after real commit and HTTP transport loss; no automatic replay, no assumed rollback, no response required when transport fails.
- Concurrent normalized-email creation/update, including inactive records; database uniqueness and approved business errors.

Use independent transactions/connections/contexts, barriers/latches, and bounded lock observations and deadlines. Avoid arbitrary sleeps and an enclosing test transaction shared by workers. Always clean up workers/locks. Fault injection must distinguish actual commit success with lost confirmation from confirmed rollback.

## Evidence and Release Readiness

Record test locations/commands, selected PostgreSQL image, outcomes, and limitations in the increment report. Proposed command conventions are unit tests through Surefire and PostgreSQL `*IT` through Failsafe; no POM exists yet. Include serialization after the persistence context closes and prove it emits complete DTO data without additional queries.

No tests have been run or generated for this baseline. Passing tests does not automatically accept ADR-0002; explicit ADR acceptance and implementation verification are independent gates.
