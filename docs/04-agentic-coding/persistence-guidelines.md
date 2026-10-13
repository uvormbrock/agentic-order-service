# Persistence & PostgreSQL Implementation Guidelines

**Status:** Accepted implementation guidance; no implementation authorization

**Scope:** Spring Boot 4.1.1-managed Spring Data JPA, Hibernate, and Flyway with PostgreSQL 17.11

## Authority and Scope

These accepted guidelines supplement the approved [coding guidelines](coding-guidelines.md) and companion [Java/Spring guidelines](java-spring-guidelines.md). Read [AGENTS.md](../../AGENTS.md), [system architecture](../02-architecture/system-architecture.md), [technology reference](../02-architecture/technology-stack.md), [PRD](../01-requirements/product-requirements.md), and [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md) first. Accepted requirements/ADRs and approved engineering rules take precedence. Acceptance of this guidance does not approve a new implementation increment, schema, migration, dependency, or previously Open/Proposed subdecision. Explicitly Proposed recommendations below remain subject to their separate approval gates. [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) remains Proposed and unverified.

## Schema Ownership and Flyway

- Flyway owns versioned schema changes; Hibernate runs with `ddl-auto=validate`, not `create`, `create-drop`, or `update`.
- Create migrations only for reviewed, approved schema changes. Prefer explicit, reviewable SQL and document data backfill, constraints, and backward compatibility where relevant.
- Accepted D10 policy: applied migrations are immutable; add new versioned migrations for later changes. No automatic baseline/repair/clean; validate history/checksums. First domain migration is V1__create_order_domain_schema.sql under [ADR-0005](../03-decisions/ADR-0005-technology-stack.md).
- Do not rely on migrations being universally idempotent. Test actual migrations against disposable PostgreSQL databases, including realistic existing-data cases where applicable.
- Increment 2 metadata-only approval is historical. D10 now permits the approved domain migration in project-local/Testcontainers environments after explicit Increment 3 implementation authorization. Review concrete SQL against the PRD, preserve data/volumes and keep production execution/privileges Open. This documentation task executes no migration.
- Keep migrations and Hibernate mappings consistent; `validate` is evidence of mapping/schema compatibility only for entities actually present.

## Entities, Aggregates, and Relationships

- Preserve the approved direct JPA mapping strategy and aggregate boundaries rather than introducing a separate parallel persistence model by default.
- Apply the parent [construction, equality, and fetching conventions](coding-guidelines.md#jpa-hibernate-and-postgresql). D1–D9 mapping choices are Accepted in ADR-0004, including generated UUIDs, field access/protected constructors, persistence-package autoApply converters, PERSIST-only lazy Order/items ownership, reference entity equality and single-order fetching. Preserve the [domain model](../02-architecture/domain-model.md); do not ask for repeat approval of selected policies or confuse acceptance with implementation evidence.
- Use explicit table/column mappings and appropriate nullability, precision, scale, and uniqueness based on approved domain and schema decisions.
- Keep OrderItem lifecycle owned by Order, without an independent repository. Cross-aggregate references remain by identity; do not introduce managed Customer/Product graphs or cascading deletion of historical orders. Review concrete relationship/cascade/orphan-removal mappings before implementation.
- Prefer lazy loading where practical, especially for collections. Avoid returning managed entities to the REST boundary.
- Design fetch plans intentionally (`JOIN FETCH`, entity graphs, or approved projections) and verify that read DTOs are fully materialized before closing the persistence context; do not depend on OSIV.
- Do not assume an `Optional` return type, a surrogate primary key, automatic auditing, soft deletes, or global entity listeners are mandatory; each must serve an accepted requirement.

## PostgreSQL Schema and Query Design

- Choose SQL types from approved ranges and invariants; neither integer/UUID identities nor text-length representations are universal defaults. A line can reach EUR 9,999,999,900.00 and multi-product totals can exceed it. D6 selects unconstrained NUMERIC without fixed precision/scale, finite/scale ≤2 checks and invariant-specific non-negative/range checks. Verify physical capacity and exact hydration; do not substitute NUMERIC(19,2) or invent a total limit.
- Store monetary values exactly using the approved unconstrained NUMERIC/check policy; do not use float/double or database coercion to round invalid source values.
- Use database constraints to defend mandatory invariants: primary/foreign keys, approved uniqueness, non-null requirements, and check constraints where justified.
- Index validated access paths and foreign keys where necessary; inspect actual queries and execution plans before adding speculative composite, covering, or partial indexes.
- Preserve approved email uniqueness/normalization and active/inactive customer semantics. Do not infer partial uniqueness limited to active rows.
- Use PostgreSQL's actual transaction/locking behavior in verification; do not substitute H2 for concurrency evidence.
- Do not enable Supabase-specific RLS examples, database-wide settings, or extensions without an explicit application requirement and approval.

## Transactions and Concurrency

- Application use cases own transaction boundaries according to accepted architecture.
- Accepted [transaction conventions](../02-architecture/transaction-locking.md) require ambient-transaction rejection for order retry coordination, transaction-scoped state, complete materialization, and commit/rollback observation. Detailed retry classification, propagation, and lock paths require scoped approval before affected implementation; this guidance does not implement them.
- ADR-0002 remains **Proposed and implementation-unverified**: no generic `@Transactional` template may override the planned transaction coordinator, deterministic lock ordering, retry rules, or handling of uncertain commit outcomes.
- Do not implement unrestricted retries on all exceptions. Re-execution is acceptable only for explicitly classified, proven-rollback outcomes and according to the future approved policy.
- Never replay an order after an uncertain commit outcome or report it as confirmed rollback; a failed connection may prevent an HTTP response.
- Keep transactions focused and avoid unrelated external-service calls or file I/O inside them. Never shorten a required transaction by releasing locks or splitting an aggregate save before completion.
- Verify READ COMMITTED, locking, rollback, and race behavior against real PostgreSQL using controlled synchronization and independent connections/transactions when those capabilities are implemented.

## Connection Pooling and Operations

- Preserve Boot-managed DataSource/Hikari auto-configuration. The established Testcontainers `@ServiceConnection` setup is test infrastructure; it does not require a custom production DataSource or container-managed production database.
- Treat connection-pool sizes, database timeouts, caching, and batch configuration as workload-specific tuning, not hardcoded skill defaults.
- Use externally provided secrets for real environments. Local development may use ignored `.env`; Testcontainers may use disposable, non-secret fixture credentials.
- Use [config/postgres.properties](../../config/postgres.properties) as the single executable image source. Follow the [documented two-file Compose commands and shell-override checks](../02-architecture/technology-stack.md#postgresql-configuration-source-and-local-credentials); tests load the properties directly and do not depend on local Compose or `.env`.

## Verification Checklist for Increment 3 and Later

- Confirm Accepted D1–D10 in ADR-0004/0005, review concrete SQL declarations against the PRD and obtain implementation authorization. Do not reopen selected policies; report only genuine gaps such as a required unselected collation change. List/pagination, production and ADR-0002 details retain their deferred status.
- For approved mappings, test Flyway migration from empty schema and Hibernate validation against PostgreSQL.
- Test persistence round trips, FK/unique/check constraints, snapshot immutability, and approved fetching behavior with real PostgreSQL.
- Verify that DTO serialization after closing the persistence context performs no accidental lazy loads or additional queries.
- Add rollback and concurrency tests only as their relevant behavior is implemented and its architectural decisions approved.
- Follow the [testing strategy](../05-testing/testing-strategy.md): run `./mvnw verify` with command-local Java 21 and Docker for implemented integration gates. Future/unavailable gates remain Pending. Follow the [AI workflow](ai-development-workflow.md) for scope approval, automated checks, actual diff, human acceptance, and separate commit authorization; no automatic ADR status change.

## Explicit Exclusions

- No automatic adoption of a universal entity base class, repository base class, `GenerationType.IDENTITY`, default `@EnableJpaAuditing`, or `@Transactional` on every service method.
- No blanket `saveAll` batching, second-level cache, automatic index creation, SQL debug/bind-value logging, or database-wide `ALTER SYSTEM` changes.
- No assumption that PostgreSQL `ON CONFLICT`, RLS, `SKIP LOCKED`, or cursor pagination is required for this domain without a justified use case.

## Provenance

The user-supplied draft attributes general patterns to *JPA/Hibernate Patterns* and *PostgreSQL Patterns*. Those sources are background material, not adopted repository instructions. This revision follows the supplied review feedback and authoritative project documents; it installs no skill and selects no schema or concurrency protocol.
