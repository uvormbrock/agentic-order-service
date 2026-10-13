# Testing Strategy

**Status:** Approved quality requirements; foundation tests implemented; domain/persistence/concurrency tests Pending

## Scope and Prerequisites

The [PRD acceptance criteria](../01-requirements/product-requirements.md) define behavior; [ADR-0001](../03-decisions/ADR-0001-order-placement-concurrency.md) defines concurrency outcomes. The [Proposed ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) defines technical verification scenarios. JUnit 6 is Accepted under [ADR-0005](../03-decisions/ADR-0005-technology-stack.md), explicitly replacing JUnit 5. Use Jupiter and Platform versions managed by Spring Boot 4.1.1 without JUnit 5-retention overrides; foundation build verification passed; domain and concurrency verification remain Pending.

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
| Build | Java 21 compile, compatible Boot 4.1.1 dependency graph, Boot 4.1.1-managed JUnit 6 API/engine/Platform alignment |
| Domain | Pricing/consolidation/limits, EUR 99.99/100.00/100.05 discount cases, immutable snapshot behavior |
| HTTP | Actual request binding; unknown root/nested fields, price overrides, fractional/missing/null quantities, all approved boundary cases and error shape |
| Persistence | PostgreSQL round trips, reviewed schema/migrations, constraints, email uniqueness, monetary capacity, rollback, no lazy access after DTO return |
| Transaction infrastructure | Reject ambient coordinator entry, READ COMMITTED, fresh attempt contexts, shared helper transaction, commit-time exception observation |
| Concurrency | Deterministic PostgreSQL tests of the scenarios below, using actual JPA/provider paths |

Use unit tests for domain behavior and HTTP integration tests through real JSON binding. PostgreSQL/Testcontainers is required for persistence/concurrency evidence, not H2 or mocked repositories. Use reviewed migrations in disposable databases only after migration approval. A context smoke test does not establish locking correctness. No arbitrary coverage percentage replaces meaningful gates.

## Architecture Boundary Verification

These are mandatory obligations for implemented feature code, not evidence that boundaries already exist. For a documentation-only increment they remain Pending; a technical skeleton verifies only boundaries actually introduced. Check against [system architecture](../02-architecture/system-architecture.md).

- Controllers depend on application operations, not repositories or domain orchestration; HTTP-level tests cover delegation and translation.
- Domain code does not depend on API/application packages or persistence components and performs no database access. Direct JPA metadata coupling is the accepted exception, not permission to call infrastructure.
- Cross-feature access uses feature-owned narrow public contracts; no imports/access to foreign persistence packages or managed entity graphs.
- REST request/response types expose DTOs, never managed JPA entities/proxies. Verify complete serialization after transaction/context closure without new queries.
- No circular constructor dependencies, recursive use-case call chains, or forbidden layer cycles. Narrow feature contracts may reference features in both directions without creating those execution/wiring cycles; a blanket ban on all feature-level references in both directions would contradict the accepted design.

Inspect actual imports/dependency paths and wiring, with targeted tests where applicable. The concrete automated enforcement mechanism remains **Open**. Do not introduce ArchUnit, Spring Modulith, or other dependencies without separate approval. Manual inspection must be reported as review evidence, not as an automated test result; future/unavailable automated checks remain Pending.

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

Record test locations/commands, selected PostgreSQL image, outcomes, and limitations in the increment report. Increment 2 explicitly accepts Surefire for Docker-free tests and Failsafe for PostgreSQL `*IT` during `verify`. Include serialization after the persistence context closes and prove it emits complete DTO data without additional queries.

Foundation tests have been run as recorded in Increment 2; no domain/mapping/concurrency test evidence is claimed. Passing tests does not automatically accept ADR-0002; explicit ADR acceptance and implementation verification are independent gates.

## Increment 2 Test Responsibilities

`AgenticOrderServiceApplicationTest` retains MVC/Validation assertions while excluding DataSource, Hibernate JPA, and Flyway auto-configuration only through its test annotation. It verifies web-context wiring, not persistence readiness, and must run without Docker through `./mvnw test`.

`PostgreSqlFoundationIT` uses the full application context, the `integration-test` profile, and a Spring-managed PostgreSQL bean with `@ServiceConnection`. It verifies JDBC/server version, EntityManagerFactory with zero entities, disabled OSIV, validation/no SQL initialization, zero application migrations, and only Flyway metadata. Failsafe executes it through `./mvnw verify`; Docker unavailability fails startup, with no disabled-without-Docker skip. Context teardown stops the test database; reuse is disabled. Credentials/ports are independent of Compose.

No entity round trips, application migrations, domain rules, transaction guards, or concurrency behavior are verified by this increment. Those gates remain Pending. Actual results: [Increment 2 execution record](../04-agentic-coding/prompts/03-postgresql-foundation.md).
