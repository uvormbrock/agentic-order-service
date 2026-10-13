# Incremental Implementation Plan

**Status:** Historical stage plan with scoped increments; Increment 1 accepted, Increment 2 verified and accepted; later increments require approval

## Accepted Foundation and Open Gates

[ADR-0003](../03-decisions/ADR-0003-application-architecture.md) accepts the modular monolith; [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md) accepts direct JPA and immutable values. [ADR-0005](../03-decisions/ADR-0005-technology-stack.md) fixes Java 21 / Spring Boot 4.1.1 and Flyway. [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) remains Proposed and unverified. No stage below authorizes code or migration execution by itself.

Before affected implementation: verify exact starter/dependency alignment and select the PostgreSQL test image. JUnit 6 is Accepted, replacing JUnit 5, and must use Boot 4.1.1-managed Jupiter/Platform versions without JUnit 5-retention overrides. Before persistence: approve detailed converter conventions, OrderItem identity, schema constraints, monetary precision/scale and original-result storage. Resolve exact email syntax/counting before validation implementation. Define migration execution strategy and timeout/error details before their respective increments.

The earlier Boot 3.5.16 dependency list and skeleton were proposals and are superseded by the accepted Boot 4.1.1 version. Base package/groupId and Maven Wrapper 3.9.9 were accepted in Increment 1; PostgreSQL 17.11 image selection was accepted for Increment 2. Later class names and mappings remain proposals. Do not instantiate empty services or fake successful endpoints.

## Proposed Stages

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

Plan environment-supplied credentials, OSIV disabled, READ COMMITTED order attempts, explicit ambient rejection, transaction-scoped fresh state, and managed-schema validation. Local/integration-test Flyway startup with metadata only is approved under Increment 2. Production migration execution and domain migrations remain Open/unapproved.

## Completion and Reporting

Follow the [testing strategy](../05-testing/testing-strategy.md) and [AI workflow](../04-agentic-coding/ai-development-workflow.md). Every increment needs a reviewed diff and honest test evidence. Acceptance of a strategy and its implementation verification are separate; neither enables silent ADR status changes. Track remaining decisions in [open items](../00-overview/open-items.md).

## Approved Increment 2 Scope

The user approved a PostgreSQL foundation increment before aggregate/schema work: PostgreSQL 17.11 Compose, Boot-managed JPA/JDBC/Flyway, disabled OSIV, Hibernate validation/no SQL initialization, Docker-free smoke and isolated Testcontainers tests through Failsafe. Only local/test Flyway metadata initialization is authorized. This scoped approval supersedes the earlier stage sketch for this increment; it does not approve transaction templates/ambient guards, domain entities, repositories, or migrations. The selected base package and Maven wrapper were accepted in Increment 1.

Production migration strategy and all detailed schema/mapping/concurrency gates remain Open or Proposed as before. [Increment 2 record](../04-agentic-coding/prompts/03-postgresql-foundation.md) separates expected verification from actual results and human acceptance.
