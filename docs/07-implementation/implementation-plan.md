# Incremental Implementation Plan

**Status:** Proposed execution plan; documentation baseline approved, implementation not authorized

## Accepted Foundation and Open Gates

[ADR-0003](../03-decisions/ADR-0003-application-architecture.md) accepts the modular monolith; [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md) accepts direct JPA and immutable values. [ADR-0005](../03-decisions/ADR-0005-technology-stack.md) fixes Java 21 / Spring Boot 4.1.1 and Flyway. [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) remains Proposed and unverified. No stage below authorizes code or migration execution by itself.

Before affected implementation: resolve the JUnit 5 / Boot-managed JUnit 6 mismatch, exact starter/dependency alignment and PostgreSQL test image. Before persistence: approve detailed converter conventions, OrderItem identity, schema constraints, monetary precision/scale and original-result storage. Resolve exact email syntax/counting before validation implementation. Define migration execution strategy and timeout/error details before their respective increments.

The earlier Boot 3.5.16 dependency list and skeleton were proposals and are superseded by the accepted Boot 4.1.1 version. Base package/groupId, exact class names, wrapper version, and image tag remain proposals. Do not instantiate empty services or fake successful endpoints.

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

Plan environment-supplied credentials, OSIV disabled, READ COMMITTED order attempts, explicit ambient rejection, transaction-scoped fresh state, and managed-schema validation. Flyway is selected; automatic startup migration versus deployment-time execution remains open, so neither is authorized here.

## Completion and Reporting

Follow the [testing strategy](../05-testing/testing-strategy.md) and [AI workflow](../04-agentic-coding/ai-development-workflow.md). Every increment needs a reviewed diff and honest test evidence. Acceptance of a strategy and its implementation verification are separate; neither enables silent ADR status changes. Track remaining decisions in [open items](../00-overview/open-items.md).
