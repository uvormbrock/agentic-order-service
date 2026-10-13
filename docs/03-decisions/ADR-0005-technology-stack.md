# ADR-0005: Technology Stack

**Status:** Accepted baseline; dependency alignment and migration execution details Open
**Recorded:** 2026-10-13
**Implementation:** Not started

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

Prefer Spring Boot 4.1.1 dependency management for compatible libraries. Verify actual starter/artifact coordinates, Jackson behavior, Hibernate SQL, JDBC driver, Flyway PostgreSQL support, and Testcontainers modules before approving a POM. Record resolved versions and justify overrides. Maven tool/wrapper and PostgreSQL server/image versions remain open.

Flyway is accepted; when/how migrations run, versioning conventions, deployment privileges, validation/repair policy, and schema review strategy remain Open. No migration execution or database change is authorized by this ADR. Hibernate must not silently create/update schema in the intended managed-schema setup; exact validation configuration is planned, not generated.

## Test Framework Decision and Replacement History

The earlier user baseline selected **JUnit 5**. The prior review identified that Spring Boot 4.1.1 manages JUnit Jupiter/Platform **6.0.3** and left alignment unresolved.

On 2026-10-13, the user explicitly replaced the earlier JUnit 5 decision with **JUnit 6**. Use the Jupiter and Platform versions managed by Boot **4.1.1**; do not override them to retain JUnit 5. This resolves the selection question, not implementation verification. Java 21 and Spring Boot 4.1.1 remain unchanged; all other Open dependency and migration details remain Open.

## Consequences and Verification Criteria

The fixed runtime baseline reduces drift. Dependency overrides increase verification obligations. Boot 4 starter/module names and strict JSON behavior must be checked against selected versions. Compile and run on Java 21; inspect the dependency tree for conflicting engines/frameworks; prove context startup, HTTP binding, PostgreSQL migrations/mappings, and provider locking behavior through appropriate tests. No build or test implementation exists.

## History and Sources

The stack families were explicitly adopted by the user; Java 21 and Spring Boot 4.1.1 were explicitly confirmed during documentation approval on 2026-10-13. Other exact versions remain open only where unresolved.

- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html): inspected as version 4.1.1; Java 21 is supported.
- [Spring Boot managed dependencies](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html): inspected as version 4.1.1, including JUnit 6.0.3. These live pages may change; verify against the selected release before building.
