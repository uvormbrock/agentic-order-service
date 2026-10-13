# Java & Spring Boot Implementation Guidelines

**Status:** Accepted implementation guidance; no implementation authorization

**Scope:** Java 21 / Spring Boot 4.1.1 implementation for Agentic Order Service

## Authority and Scope

These accepted guidelines supplement the approved [coding guidelines](coding-guidelines.md). Read [AGENTS.md](../../AGENTS.md), [system architecture](../02-architecture/system-architecture.md), the [PRD](../01-requirements/product-requirements.md), [technology reference](../02-architecture/technology-stack.md), and relevant ADRs first. Accepted requirements/ADRs and the approved engineering workflow take precedence. Acceptance of this guidance does not approve a new implementation increment, dependency, schema change, or previously Open/Proposed subdecision. Explicitly Proposed framework details below remain subject to separate approval.

The scope is Java 21 and the imperative Spring Boot 4.1.1 application with Spring MVC and Spring Data JPA. Preserve Boot-managed dependency versions. Use the companion [persistence guidelines](persistence-guidelines.md) for database-specific guidance; do not add frameworks or features without approval.

## Java 21 Conventions

- Optimize for clarity, explicit intent, and maintainability over cleverness or unnecessary abstraction.
- Use conventional Java naming: `PascalCase` types, `camelCase` members, `UPPER_SNAKE_CASE` constants.
- Prefer immutable value objects and DTOs. Use records where their semantics fit; maintain defensive copies and invariant checks for mutable inputs.
- Do not treat Java records as a default representation for JPA entities. Directly mapped entities must preserve domain invariants while accommodating JPA/Hibernate construction, identity, and lifecycle requirements. Keep persistence-only compromises as narrow as practical.
- Keep business invariants in the appropriate domain types and controlled aggregate construction/mutation methods.
- Prefer constructor injection for Spring components. Avoid field injection in production code; test injection may use the established project convention.
- Use `Optional` for potentially absent repository/query results; do not use it indiscriminately for fields, request properties, or entity attributes. Avoid unchecked `Optional.get()`.
- Use streams for simple transformations; use loops when branching, order, or failure behavior is clearer.
- Make numeric limits, parsing rules, and state transitions explicit. Never use binary floating-point types for monetary amounts.
- Use specific, actionable exceptions and handle them at defined boundaries; never swallow exceptions or log and rethrow redundantly.
- Use type-safe collections and parameterized generics; avoid raw types.

## Architecture and Package Boundaries

- Preserve the accepted feature-oriented modular monolith and project package boundaries; do not replace them with a cross-cutting `controller/service/repository` directory structure.
- Keep HTTP binding/response concerns in the API layer; controllers delegate to application use cases and do not orchestrate persistence.
- Domain behavior must not depend on API/application packages, HTTP DTOs, or database access. Direct JPA metadata coupling is the accepted exception in [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md); it does not permit infrastructure calls.
- Cross-feature collaboration uses narrow, feature-owned contracts; avoid accessing another feature's repositories or managed entity graphs.
- Materialize complete transport-independent results inside application transactions. The API may map those results to HTTP response DTOs afterward without database access; application code must not depend on API-specific DTOs. Never expose managed entities/proxies or rely on OSIV.

## Spring MVC and Validation Proposals

- Prefer constructor-injected, focused `@RestController` components; keep controllers free of business rules.
- Use explicit request/response DTOs; Java records are appropriate when immutable data carriers suffice.
- Validate incoming DTOs with Jakarta Validation and `@Valid`, while enforcing business rules separately in the domain/application layer.
- Preserve the accepted [PRD](../01-requirements/product-requirements.md) HTTP contract and validation rules. [REST API design](../02-architecture/rest-api-design.md) distinguishes those requirements from Proposed binding/handler implementations. Verify unknown root/nested fields and numeric coercion through real JSON binding; annotations alone do not prove correct rejection.
- The API owns exception-to-HTTP translation. `@RestControllerAdvice` is a Proposed mechanism, not a selected concrete handler. Preserve the already accepted statuses and `code`/`message` envelope; exact code taxonomy remains Open. Distinguish business errors from infrastructure failures and do not expose internal details.
- Use typed, environment-specific configuration; never hardcode real database credentials or secrets.

## Logging and Operational Hygiene

- Use the project's existing SLF4J/Spring Boot logging facilities; log important transitions and failures with useful context.
- Do not log credentials, authentication tokens, personal data, or complete sensitive request payloads.
- Avoid adding caches, async execution, scheduled jobs, new filters, or observability dependencies without an explicit requirement and approval.

## Testing and Review

- Use Boot-managed JUnit **6** under [ADR-0005](../03-decisions/ADR-0005-technology-stack.md) and the [testing strategy](../05-testing/testing-strategy.md). Do not copy JUnit 5-retention overrides or obsolete Spring Boot test APIs; verify any proposed mocking/test API against Boot 4.1.1 before use.
- Favor fast, deterministic tests for domain logic and targeted validation. Cover invariants, boundary values, ordering guarantees, rejected state transitions, and absence of unintended state changes where applicable; avoid reliance on uncontrolled time or test execution order.
- Add PostgreSQL/Testcontainers integration tests for persistence behavior and transaction evidence.
- Preserve the Maven conventions: Surefire for `*Test`; Failsafe for `*IT` through `verify`.
- Use integration tests to verify actual HTTP binding and serialization where required; never claim an application-context smoke test proves an endpoint or business rule.
- Do not assume `@SpringBootTest` is necessary for every test; choose the smallest appropriate test scope supported by Spring Boot 4.1.1.
- Follow the [AI workflow](ai-development-workflow.md): approved plan before coding, incremental implementation, applicable automated checks, actual diff for human review, explicit human acceptance, and separate commit authorization. Self-review does not replace human review; unavailable/future gates are Pending.

## Explicit Exclusions / Pending Decisions

- Do not prescribe a generic retry loop, `Thread.sleep()` retry policy, or blanket `@Transactional` annotations for order placement. Accepted [transaction conventions](../02-architecture/transaction-locking.md) already govern application ownership, ambient rejection, DTO materialization, and no replay after uncertain commit. [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) remains Proposed and unverified; its detailed implementation and blocking Open choices need scoped approval and later PostgreSQL verification. Tests do not accept the ADR automatically.
- Do not translate arbitrary persistence/transaction failures into automatic retries. Retry eligibility requires a separately approved classification of confirmed rollback versus uncertain commit outcome; never replay a potentially committed order placement.
- Follow Accepted D1–D10 under ADR-0004/0005: generated UUIDs, field access/protected constructors, autoApply persistence converters, controlled relationships, reference entity equality, unconstrained NUMERIC checks and all-customer lower(email) uniqueness. Names use String.strip()/code points; EmailAddress retains its approved ASCII policy. Do not reopen these decisions. Concrete SQL declarations/compatibility need review/evidence; deferred list fetching, production and concurrency details remain unresolved. Separate implementation authorization is still required.

## Provenance

The user-supplied draft attributes general patterns to *Java Coding Standards* and *Spring Boot Development Patterns*. These sources are background material, not adopted repository instructions. This revision follows the supplied review feedback and authoritative project documents; it installs no skill and changes no ADR status.
