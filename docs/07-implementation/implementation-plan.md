# Incremental Implementation Plan

**Status:** Historical stage plan with scoped increments; Increment 1 accepted, Increment 2 verified and accepted; later increments require approval

## Accepted Foundation and Open Gates

[ADR-0003](../03-decisions/ADR-0003-application-architecture.md) accepts the modular monolith; [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md) accepts direct JPA and immutable values. [ADR-0005](../03-decisions/ADR-0005-technology-stack.md) fixes Java 21 / Spring Boot 4.1.1 and Flyway. [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) remains Proposed and unverified. No stage below authorizes code or migration execution by itself.

Before affected implementation: verify exact starter/dependency alignment and select the PostgreSQL test image. JUnit 6 is Accepted, replacing JUnit 5, and must use Boot 4.1.1-managed Jupiter/Platform versions without JUnit 5-retention overrides. D1–D10 identity/mapping/converter/relationship/fetch/numeric/name/email/schema/equality and local/test migration choices are Accepted under ADR-0004/0005. Confirm these decisions rather than reopening them; review concrete SQL and obtain Increment 3 execution authorization. Production migration strategy and detailed concurrency timeout/error settings remain unresolved.

The earlier Boot 3.5.16 dependency list and skeleton were proposals and are superseded by the accepted Boot 4.1.1 version. Base package/groupId and Maven Wrapper 3.9.9 were accepted in Increment 1; PostgreSQL 17.11 image selection was accepted for Increment 2. Later class names beyond the reviewed Increment 3 scope remain illustrative; its D1–D10 mappings are Accepted. Do not instantiate empty services or fake successful endpoints.

## Proposed Stages

This earlier stage sketch is historical, not the current increment numbering. The reviewed single Increment 3 scope below supersedes it for Domain Model & Persistence; it does not authorize its execution or Order Placement.

| Stage | Scope | Approval and evidence |
| --- | --- | --- |
| 0 | Authoritative documentation | This task; links/statuses/rules checked, no code |
| 1 | One Maven module, Boot entry point, environment configuration, transaction template/ambient guard, OSIV disabled, PostgreSQL test wiring | Separate skeleton approval; compatible dependency graph and technical smoke/transaction-boundary tests, no business logic |
| 2 | Immutable values, direct aggregate mappings, reviewed Flyway schema and Spring Data repositories | Separate mapping/schema/migration approval; round trips, constraints/capacity and context lifecycle |
| 3 | Product/customer use cases and REST DTOs | PRD behavior, normalization, strict binding, deletion/deactivation tests |
| 4 | Order domain construction, pricing/consolidation/snapshots, narrow module access, transactional placement and bounded retries | Separate implementation scope; ADR-0002 policy respected, confirmed rollback and commit-boundary tests |
| 5 | Order individual/list DTOs and full deterministic concurrency matrix | Complete materialization, no lazy reads, PostgreSQL races/fault injection and acceptance evidence |

## Proposed Package Skeleton

Within one selected base package: `product`, `customer`, and `order`, each containing `api`, `application`, `domain`, and `persistence` only when needed. `shared/domain` holds Money; `customer/domain` holds EmailAddress. Configuration stays in `configuration`. No independent Maven submodules or generic CRUD layer.

Initial technical classes may include the Boot entry point, OrderTransactionConfiguration, AmbientTransactionGuard, PostgreSQL test configuration, and context/transaction infrastructure tests. Later use cases may introduce OrderPlacementCoordinator and narrow provider-owned access components. Names are illustrative, not generated code.

## Configuration and Dependency Planning

Boot 4.1.1 managed coordinates must be checked before POM approval. Include only required web/validation/JPA, PostgreSQL/Flyway support, and approved test dependencies. Hibernate/Jackson/provider versions should align with Boot unless an approved compatibility override is necessary. No implementation dependencies are added now.

Plan environment-supplied credentials, OSIV disabled, READ COMMITTED order attempts, explicit ambient rejection, transaction-scoped fresh state, and managed-schema validation. Increment 2 metadata-only startup is historical; D10 permits V1 local/test domain schema execution after Increment 3 implementation authorization. Production migration execution/privileges stay Open; no domain migration has yet been created/run.

## Completion and Reporting

Follow the [testing strategy](../05-testing/testing-strategy.md) and [AI workflow](../04-agentic-coding/ai-development-workflow.md). Every increment needs a reviewed diff and honest test evidence. Acceptance of a strategy and its implementation verification are separate; neither enables silent ADR status changes. Track remaining decisions in [open items](../00-overview/open-items.md).

## Approved Increment 2 Scope

The user approved a PostgreSQL foundation increment before aggregate/schema work: PostgreSQL 17.11 Compose, Boot-managed JPA/JDBC/Flyway, disabled OSIV, Hibernate validation/no SQL initialization, Docker-free smoke and isolated Testcontainers tests through Failsafe. For Increment 2, only local/test Flyway metadata initialization was authorized. That scoped approval superseded the earlier stage sketch for this increment; it does not approve transaction templates/ambient guards, domain entities, repositories, or migrations. The selected base package and Maven wrapper were accepted in Increment 1.

At Increment 2 acceptance, domain schema/mappings and concurrency were unresolved. D1–D10 now resolve the reviewed domain/persistence design; production migration strategy and detailed concurrency remain Open/Proposed. [Increment 2 record](../04-agentic-coding/prompts/03-postgresql-foundation.md) separates expected verification from actual results and human acceptance.

## Increment 3 — Domain Model & Persistence

Design D1–D10 is explicitly Accepted; [the single implementation prompt](../04-agentic-coding/prompts/04-domain-model.md#implementation-prompt--increment-3-pending-human-approval) remains Pending Human Approval. Implementation has not started. Internally sequence values/aggregate behavior, concrete SQL review/schema, direct mappings/repositories and PostgreSQL evidence within one increment, without phase-specific playbook files.

Review final SQL against the PRD and Accepted policies; report genuinely unselected requirements rather than inventing restrictions. After execution authorization, apply the accepted local/test migration policy, retain Java 21/Boot-managed JUnit 6 and verify domain boundaries, constraints, round trips, snapshots, numeric capacity, fetch/query counts and closed-context values. Human acceptance follows actual test evidence and complete Git diff; commit authorization is separate. No REST controllers, Order Placement orchestration, pessimistic locking/retries or ADR-0002 protocol are in this increment. Production execution/privileges and list/pagination fetch details stay deferred/Open.
