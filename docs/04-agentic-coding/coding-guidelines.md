# Coding Guidelines

**Status:** Approved engineering baseline; no implementation authorization

## Required Context

Read [system architecture](../02-architecture/system-architecture.md), [PRD](../01-requirements/product-requirements.md), and relevant ADRs. Java 21 / Spring Boot 4.1.1 are accepted in [ADR-0005](../03-decisions/ADR-0005-technology-stack.md). Proposed details remain subject to approval.

## Supplementary Implementation Guidelines

The following accepted documents provide specialized implementation guidance:

- [Java and Spring Boot guidelines](java-spring-guidelines.md): Java conventions, feature boundaries, HTTP DTOs, validation, logging, and testing.
- [Persistence and PostgreSQL guidelines](persistence-guidelines.md): aggregate mapping, fetching, schema/constraint review, Flyway scope, and database verification.

Both are **Accepted implementation guidance**, subordinate to accepted requirements/ADRs and these approved coding/workflow rules. Their acceptance does not approve implementation, dependencies, schema changes, or migrations. Explicitly Proposed/Open subdecisions retain their existing status and approval gates, including ADR-0002.

## Java and Feature Boundaries

Use English names/comments, ordinary Java conventions, explicit types at boundaries, constructor injection, and small cohesive classes. Use records where appropriate for immutable DTOs and value carriers, not automatically for JPA entities. Avoid generic CRUD bases, unnecessary interfaces, broad utility packages, and new dependencies without approval. Put feature behavior in product/customer/order packages; keep cross-feature access narrow and explicit.

Money uses decimal arithmetic and monetary equality; approved limits and rounding come from the PRD. Normalize email using the approved policy; do not silently choose unresolved syntax/counting rules. Controllers validate/translate transport and do not calculate discounts. Aggregate methods govern valid mutations; keep persistence access out of pricing behavior.

## Spring and Transaction Conventions

Application use cases own transactions. Order retries use TransactionTemplate around entire attempts with an ambient-transaction guard. Module helpers must participate, not independently commit. Disable OSIV and extended persistence contexts; fully materialize DTOs inside application transactions. Observe completion before success/retry. No self-invoked transaction annotation or repository retry loop substitutes for the boundary. Follow [transaction conventions](../02-architecture/transaction-locking.md).

## JPA, Hibernate, and PostgreSQL

Directly map aggregates with controlled mutation and separate DTOs. No independent OrderItem repository or customer/product cascade deletion of historical data. Avoid managed cross-aggregate graphs; keep references by identity. Fresh locked reads are authoritative. Verify actual lock SQL, fetch behavior, autoflush order, and persistence-context lifecycle on PostgreSQL. Do not treat annotations as proof of concurrency behavior.

Follow [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md) for Accepted/Proposed mapping distinctions. Enforce compatible constraints without inventing precision or limits. Validate inputs before persistence; do not let column coercion round invalid prices. Handle safe domain errors separately from SQL failures and never expose internal database messages to clients.

### Construction, Mutation, and Invariants

Provide a JPA-compatible public or protected no-argument constructor, preferring protected access for provider construction. Expose domain construction and mutation through operations that validate aggregate invariants; do not generate unrestricted setters. Order owns item additions and consolidation; do not expose a mutable item collection that bypasses the root. Account for JPA hydration bypassing normal construction, with appropriate constraints and round-trip tests.

### Entity Equality, Hashing, and Logging

Choose an entity equality/hash-code policy only after its identity and lifecycle are approved. Do not base hash codes on mutable fields or a generated ID that changes while an entity is stored in a hash collection. Transient entities must not become equal merely because both IDs are null. Verify managed/detached and proxy behavior for the selected policy. Value-object equality remains value-based.

Equality, hashing, toString, and logging methods must not traverse lazy associations or perform database access. Avoid recursively printing entity graphs or logging sensitive customer data. Do not automatically generate entity-wide equality/hash/toString methods.

### Fetching and Responses

Never serialize managed JPA entities. Materialize complete immutable response DTOs inside application transactions; no proxies, lazy collections, or persistence streams escape. Use explicit use-case fetch plans (appropriate queries, projections, or entity graphs) and inspect query counts to detect N+1 behavior. Do not solve N+1 with blanket eager relationships, especially across aggregate boundaries. Verify serialization after the persistence context closes requires no queries.

### Constraint-Aware Failures and Mapping Approval

Classify root SQLSTATE and the known constraint rather than localized messages or broad exception types. Map a confirmed normalized-email uniqueness violation to the PRD's business conflict; do not map every integrity failure to 409 or retry it. Unexpected foreign-key/check failures may indicate a defect and require safe technical handling. Let invalidating failures reach the transaction boundary so partial work rolls back; do not swallow failures or expose SQL details.

Do not invent entity identity, relationship/cascade mappings, column precision, or schema limits to make code compile. Preserve the Open OrderItem identity/schema decisions and Proposed converter details in ADR-0004; obtain approval for concrete mappings before implementation.

## Maven and Configuration

One Maven module; Java 21 compilation; Boot 4.1.1 alignment. Prefer managed dependency versions and explain overrides. Use Accepted JUnit 6 with Boot 4.1.1-managed Jupiter and Platform versions; do not override them to retain JUnit 5. Keep credentials external and never commit secrets. No automatic schema update or autonomous migration execution. Flyway and local/integration-test metadata initialization are accepted; domain migration scripts require separate approval, and production execution/privileges remain Open under [ADR-0005](../03-decisions/ADR-0005-technology-stack.md).

## Completion Criteria

Keep diffs focused, run applicable automated gates, and report exact evidence with unavailable gates marked Pending. Present the actual diff and obtain explicit human acceptance; self-review is insufficient. Do not commit without explicit authorization. Changes to rules/architecture require explicit approval and corresponding authoritative document updates. See [testing strategy](../05-testing/testing-strategy.md) and [AI workflow](ai-development-workflow.md).
