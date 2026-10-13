# ADR-0005: Technology Stack

**Status:** Accepted baseline and Increment 3 local/test migration policy; production details Open
**Recorded:** 2026-10-13
**Implementation:** Increments 1 and 2 accepted; Increment 3 not started; domain/persistence/concurrency verification Pending

## Operational Reference

The [technology stack reference](../02-architecture/technology-stack.md) inventories roles and version-management policies without replacing this decision record or its status/history.

## Accepted Decision

- Java **21** and Spring Boot **4.1.1** are selected, not open version questions.
- One Maven module, Spring Data JPA / Hibernate, PostgreSQL, and Flyway.
- **JUnit 6** and Testcontainers are Accepted for tests. Use JUnit Jupiter and Platform versions managed by Spring Boot **4.1.1**, without overrides to retain JUnit 5.
- TransactionTemplate, READ COMMITTED, and deterministic pessimistic locking are the adopted technical baseline. The complete [ADR-0002](ADR-0002-transaction-locking.md) remains Proposed and implementation-unverified.

The prior Spring Boot 3.5.16 skeleton suggestion was unapproved and is not the selected baseline. Do not copy its dependency coordinates/configuration into a Boot 4 build without checking compatibility.

## Rationale and Alternatives

Java 21 and Spring Boot support the selected imperative application style. PostgreSQL supplies the target locking/constraint semantics; Testcontainers exercises the actual database. Flyway provides Git-versioned schema evolution. A different database or build framework adds no demonstrated benefit; H2 cannot establish PostgreSQL concurrency correctness. Alternative dependency overrides require compatibility evidence, not guesswork.

## Dependency and Migration Policy

Prefer Spring Boot 4.1.1 dependency management for compatible libraries. Verify actual starter/artifact coordinates, Jackson behavior, Hibernate SQL, JDBC driver, Flyway PostgreSQL support, and Testcontainers modules before approving a POM. Record resolved versions and justify overrides. The original baseline left Maven tool/wrapper and PostgreSQL server/image versions Open; the subsequent approvals below resolve those selections.

Flyway is Accepted; the local/test versioning/execution policy is now selected by the Increment 3 approval below. Production execution/privileges remain Open. The original baseline authorized no migrations; Increment 2 subsequently permitted metadata only, and the latest Increment 3 design permits the specified domain migration after implementation authorization. Hibernate remains ddl-auto=validate and must not create/update schema.

## Test Framework Decision and Replacement History

The earlier user baseline selected **JUnit 5**. The prior review identified that Spring Boot 4.1.1 manages JUnit Jupiter/Platform **6.0.3** and left alignment unresolved.

On 2026-10-13, the user explicitly replaced the earlier JUnit 5 decision with **JUnit 6**. Use the Jupiter and Platform versions managed by Boot **4.1.1**; do not override them to retain JUnit 5. This resolves the selection question, not implementation verification. Java 21 and Spring Boot 4.1.1 remain unchanged; all other Open dependency and migration details remain Open.

## Consequences and Verification Criteria

The fixed runtime baseline reduces drift. Dependency overrides increase verification obligations. Boot 4 starter/module names and strict JSON behavior must be checked against selected versions. Compile and run on Java 21; inspect the dependency tree for conflicting engines/frameworks; prove context startup, HTTP binding, PostgreSQL migrations/mappings, and provider locking behavior through appropriate tests. Increment 1 build/test evidence is recorded in its prompt record; database foundation evidence is recorded separately below.

## History and Sources

The stack families were explicitly adopted by the user; Java 21 and Spring Boot 4.1.1 were explicitly confirmed during documentation approval on 2026-10-13. Other exact versions remain open only where unresolved.

- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html): inspected as version 4.1.1; Java 21 is supported.
- [Spring Boot managed dependencies](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html): inspected as version 4.1.1, including JUnit 6.0.3. These live pages may change; verify against the selected release before building.

## Increment 2 Approval and History

The user explicitly approved PostgreSQL **17.11**, official image `postgres:17.11-bookworm`, Spring Data JPA/Hibernate, the PostgreSQL JDBC driver, Flyway PostgreSQL support, Testcontainers, and Maven Failsafe integration-test execution. Maven **3.9.9** via Apache Maven Wrapper was previously accepted during Increment 1. Dependencies use Boot 4.1.1 management; this approval adds no overrides.

For the historical Increment 2 scope, Flyway startup was approved only in the `local` and `integration-test` profiles, including initialization of `flyway_schema_history` in project-specific development/disposable test databases. No application migration scripts or domain tables are authorized. Default/production migration execution is disabled; production strategy, privileges, migration conventions, and schema decisions remain Open.

Rationale: use the same explicit PostgreSQL image locally and in isolated real-database tests; exercise Boot integration without inventing domain schema. Alternatives: PostgreSQL 18, external local database management, deployment-only Flyway execution, or Surefire-only integration testing. Consequences: `verify` requires Docker, image tags remain mutable, development data persists in a named volume, and Flyway metadata is the only authorized schema addition.

Verification criteria: real server version and JDBC `SELECT 1`, JPA startup with validation, disabled OSIV, zero application migrations and only Flyway metadata, Docker-free smoke test, isolated Testcontainers cleanup, and local Compose startup. Results are tracked in the [Increment 2 record](../04-agentic-coding/prompts/03-postgresql-foundation.md); approval alone is not passing evidence. ADR-0002 remains Proposed and implementation-unverified.

## Accepted Increment 3 Local/Test Migration Policy

The human reviewer explicitly accepted D10 with D1–D9; [ADR-0004](ADR-0004-persistence-mapping.md#accepted-increment-3-selections-and-replacement-history) owns the mapping/schema choices. This extends Increment 2's metadata-only scope for subsequent authorized Increment 3 implementation; it does not execute a migration or accept implementation.

- First domain script: `src/main/resources/db/migration/V1__create_order_domain_schema.sql`, creating customers, products, orders and order_items with the approved constraints/indexes. Review concrete SQL against the PRD before recording final declarations.
- Flyway owns schema creation/versioned changes; Hibernate remains `ddl-auto=validate`. Applied migrations are immutable; subsequent changes need new versioned migrations.
- Validate migration history/checksums. No automatic baseline, repair or clean; clean remains disabled.
- Permit execution in existing project-specific local development and isolated PostgreSQL/Testcontainers integration environments once implementation is explicitly authorized. Default/production execution remains disabled; production execution and production privileges stay Open.
- Preserve existing development data/volumes; never erase/recreate them. Test fresh migration, schema validation, history/checksums and repeated startup.

Rationale: versioned DDL adds reviewed domain schema to the accepted database foundation without Hibernate schema mutation. Alternatives: metadata-only startup remains the historical foundation stage; mutable applied scripts, automatic repair/clean and production execution were not selected. Consequences: approved local startup will apply V1, so concrete DDL and existing database compatibility must be reviewed; checksum failures stop rather than repair.

Verification is Pending until Increment 3 executes against real PostgreSQL. No new dependencies, version overrides, migrations, database operations or changes to Proposed ADR-0002 are made by this documentation task. Earlier scopes and decision history above are preserved; the new selection supersedes only now-resolved local/test migration/versioning questions.
