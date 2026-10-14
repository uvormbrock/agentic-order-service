# Incremental Implementation Plan

**Status:** Historical stage plan with scoped increments; Increment 1 accepted, Increment 2 accepted; Increment 3 Reviewed – Accepted; Increment 4 Executed – Pending Human Review; later increments require approval

## Accepted Foundation and Open Gates

[ADR-0003](../03-decisions/ADR-0003-application-architecture.md) accepts the modular monolith; [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md) accepts direct JPA and immutable values. [ADR-0005](../03-decisions/ADR-0005-technology-stack.md) fixes Java 21 / Spring Boot 4.1.1 and Flyway. [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) remains Proposed and unverified. No stage below authorizes code or migration execution by itself.

Before affected implementation: verify exact starter/dependency alignment and select the PostgreSQL test image. JUnit 6 is Accepted, replacing JUnit 5, and must use Boot 4.1.1-managed Jupiter/Platform versions without JUnit 5-retention overrides. D1–D10 identity/mapping/converter/relationship/fetch/numeric/name/email/schema/equality and local/test migration choices are Accepted under ADR-0004/0005. Confirm these decisions rather than reopening them; retain the executed Increment 3 SQL review and test evidence; Increment 3 human implementation acceptance is recorded. Production migration strategy and detailed concurrency timeout/error settings remain unresolved.

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

Plan environment-supplied credentials, OSIV disabled, READ COMMITTED order attempts, explicit ambient rejection, transaction-scoped fresh state, and managed-schema validation. Increment 2 metadata-only startup is historical; D10 permits V1 local/test domain schema execution after Increment 3 implementation authorization. Production migration execution/privileges stay Open; V1 has been created and verified in isolated PostgreSQL tests; no local-volume migration was performed in that verification.

## Completion and Reporting

Follow the [testing strategy](../05-testing/testing-strategy.md) and [AI workflow](../04-agentic-coding/ai-development-workflow.md). Every increment needs a reviewed diff and honest test evidence. Acceptance of a strategy and its implementation verification are separate; neither enables silent ADR status changes. Track remaining decisions in [open items](../00-overview/open-items.md).

## Approved Increment 2 Scope

The user approved a PostgreSQL foundation increment before aggregate/schema work: PostgreSQL 17.11 Compose, Boot-managed JPA/JDBC/Flyway, disabled OSIV, Hibernate validation/no SQL initialization, Docker-free smoke and isolated Testcontainers tests through Failsafe. For Increment 2, only local/test Flyway metadata initialization was authorized. That scoped approval superseded the earlier stage sketch for this increment; it does not approve transaction templates/ambient guards, domain entities, repositories, or migrations. The selected base package and Maven wrapper were accepted in Increment 1.

At Increment 2 acceptance, domain schema/mappings and concurrency were unresolved. D1–D10 now resolve the reviewed domain/persistence design; production migration strategy and detailed concurrency remain Open/Proposed. [Increment 2 record](../04-agentic-coding/prompts/03-postgresql-foundation.md) separates expected verification from actual results and human acceptance.

## Increment 3 — Domain Model & Persistence

Design D1–D10 is explicitly Accepted; [the single approved implementation prompt](../04-agentic-coding/prompts/04-domain-model.md#implementation-prompt--increment-3-approved-historical-record) has executed. The [Increment 3 execution record](../04-agentic-coding/prompts/04-domain-model.md#implementation-results--executed-pending-human-review) records the initial 25/14 results and the subsequent 120 fast/21 PostgreSQL tests passing after hardening. Status: Reviewed – Accepted. Internally sequence values/aggregate behavior, concrete SQL review/schema, direct mappings/repositories and PostgreSQL evidence within one increment, without phase-specific playbook files.

Review final SQL against the PRD and Accepted policies; report genuinely unselected requirements rather than inventing restrictions. After execution authorization, apply the accepted local/test migration policy, retain Java 21/Boot-managed JUnit 6 and verify domain boundaries, constraints, round trips, snapshots, numeric capacity, fetch/query counts and closed-context values. Human acceptance follows actual test evidence and complete Git diff; commit authorization is separate. No REST controllers, Order Placement orchestration, pessimistic locking/retries or ADR-0002 protocol are in this increment. Production execution/privileges and list/pagination fetch details stay deferred/Open.

## Increment 4 — Product & Customer Application Services and REST API

**Status:** Executed – Pending Human Review; explicit coding authorization recorded, persistent local migration separately blocked. The historical stage table and Increment 3 Reviewed – Accepted remain unchanged. [Playbook](../04-agentic-coding/prompts/05-product-customer-api.md) preserves the initial prompt/proposal and current A/B1–B11 and final edge D1–D8 approval records, [definitive implementation prompt](../04-agentic-coding/prompts/05-product-customer-api.md#current-implementation-prompt--increment-4), endpoint/test matrices and complete file scope.

Exactly ten endpoints: POST/list GET/individual GET/PUT/PATCH for Products and Customers. Preserve generated IDs, existing response fields, active/inactive retrieval and update, normalized values, 201/Location, 200 updates/deactivation and idempotent inactive PATCH. No reactivation, product deletion, Order endpoints/placement, pessimistic protocol or retries. Customer DELETE remains MVP FR-002/204, deferred; interim DELETE /customers/{id} is Accepted OPERATION_NOT_AVAILABLE/405 with accurate Allow and no reference check/deletion. Other unsupported methods use METHOD_NOT_ALLOWED.

Human Accepted: global strict DTO/JSON scalar types, canonical UUID/400 vs valid absent/404, ordinary decimal numeric price notation/no exponent or overprecision, precise PATCH mappings, one-error six-category precedence, specific [catalog](../02-architecture/api-error-codes.md) including safe technical/media/routing responses, @Transactional application writes/readOnly reads, Product/Customer @Version and confirmed CONCURRENT_MODIFICATION/409 without automatic retries. Domain is HTTP/Spring-transaction independent; materialize immutable results within transactions, respond only after commit, translate confirmed uniqueness/version failures outside the boundary.

[ADR-0006](../03-decisions/ADR-0006-product-customer-optimistic-locking.md) remains Proposed as a complete record: @Version direction and the finalized mapping/schema/no-op decisions Human Accepted, implementation/runtime verification pending. It replaces the original DynamicUpdate/column-scoped approach as the current recommendation. Preserve D1–D10/UUID identity/reference equality/converters/constraints and V1. The reviewed V2__add_product_customer_versions.sql proposal is recorded in ADR-0006; create it only during subsequently authorized implementation; apply in disposable Testcontainers, and in persistent local development only after the target/recovery safety gate below passes, not this documentation task. The exact boxed Long field with @Version/@Column(name="version", nullable=false), BIGINT/final NOT NULL, existing-row backfill 0 and no final database default are Human Accepted. A temporary default 0 may be used but must be removed after backfill; Hibernate initializes/maintains new versions. Provider/newness/SQL runtime verification and separate coding authorization remain Pending. Production migration strategy stays Open. No caller version or ETag/If-Match: versions do not detect all stale HTTP clients or replace ADR-0002.

| Remaining implementation gate | Review / evidence required |
| --- | --- |
| Version mapping and V2 | Apply accepted boxed Long/BIGINT/NOT NULL/backfill 0/no final default policy and reviewed four-statement V2 proposal after separate execution authorization. Prove newness/initialization/increments, V1→V2 existing-data and fresh-schema/repeated verification, no V1 changes |
| JSON/UUID processing | Lexeme-preserving full syntax/property/type/presence/value pipeline before lookup; accepted D1–D8 ordering/body/duplicates/trailing/media/case policies; actual Boot/Jackson/interceptor/decoder proof |
| Framework error rendering | Safe JSON for incompatible Accept/406, unsupported Content-Type/415, unknown routes/404 and error dispatch; no HTML/ProblemDetail or internal diagnostic leakage |
| Transaction/failure classification | Real proxy entry/completion, unique/optimistic cause translation at flush/commit, accepted narrow connection-establishment/pre-write 503 classifier; no false uncertain-outcome rollback claim |
| HTTP tests | Existing spring-test/MockMvcBuilders and mocked application services, no new dependency; real context/Testcontainers Failsafe and servlet error-dispatch transport proof |

Proposed files remain feature services/results/controllers/DTOs, own-repository findAll, shared API envelope/advice and field-aware application validation signal, central Jackson/request validation, application.yml, existing test dependencies unchanged, Product/Customer @Version, reviewed V2 and existing/new tests listed in the playbook. No generic CRUD abstraction, foreign persistence access or version response field.

After explicit implementation approval: application transactions/unit tests → reviewed V2/version mapping and PostgreSQL compatibility → lexically strict DTO/API/errors and actual MVC tests → full-context PostgreSQL API/uniqueness/optimistic tests → Java 21 Maven test/verify/dependency/coverage and documentation/diff review. Preserve Surefire/Failsafe, existing Increment 1–3 suites, JaCoCo, GitHub Actions and SonarQube. Unit, real MVC and PostgreSQL levels are mandatory; full ten-endpoint and new-decision matrix is in the [playbook](../04-agentic-coding/prompts/05-product-customer-api.md#updated-mandatory-test-matrix--runtime-results-pending) and [testing strategy](../05-testing/testing-strategy.md#increment-4-accepted-api-and-versioning-verification). Runtime gates Pending; human acceptance/commit authorization separate. No production execution or Increment 4 implementation is authorized now.

### Finalized ADR-0006 Scope and Verification Gate

All three technical selections are Human Accepted; [ADR-0006](../03-decisions/ADR-0006-product-customer-optimistic-locking.md#human-approved-jpa-mapping-and-flyway-v2) owns their exact declarations. Sequential inactive PATCH must return 200 with unchanged representation, no SQL UPDATE and no version increment; overlapping writes may produce confirmed CONCURRENT_MODIFICATION/409, without retry. Versions are Hibernate-owned/internal, with no public setter/client input/DTO field; UUID/equality choices stay unchanged.

Required PostgreSQL/Testcontainers scenarios: PUT vs PUT, PUT vs PATCH, PATCH vs PUT, PATCH vs PATCH, sequential repeated PATCH, stale entity after committed update and stale entity after committed deactivation. Use independent transactions/contexts and deterministic barriers/latches, actual version checks, fresh committed-state and confirmed rollback assertions. Unit-only evidence is insufficient. Test existing representative V1 data→V2 and fresh V1+V2, preserved records/constraints/FKs/indexes, Flyway history/unchanged V1 checksum, Hibernate validation, insert/version increment and repeated startup/validation without V2 reapplication or version reset. All runtime results remain Pending; no V2 file or execution is authorized here. Production deployment/permissions/rollout and ADR-0002 remain separate/Open.

### Final Request Policies and Definitive Execution Instruction

Human Accepted edge D1–D8 are resolved in [REST design](../02-architecture/rest-api-design.md#11-final-human-decisions-d1d8): absent/empty body distinctions; nested duplicate and trailing rejection; fixed field/path tie-breakers; upper/lower/mixed UUID case; routing/method/headers→path→body→application ordering; application/json UTF-8 only; confirmed establishment/pre-write 503 versus unknown/unclassified 500. No business-policy approval is outstanding for these eight choices; actual code/SQL/framework correctness still requires runtime evidence.

The [Current Implementation Prompt](../04-agentic-coding/prompts/05-product-customer-api.md#current-implementation-prompt--increment-4) is the sole definitive future instruction, self-contained and explicitly gated. It includes V2 creation and disposable Testcontainers execution after coding authorization; persistent local execution additionally requires the target/recovery safety gate below. The scope also includes fixture column/version adaptations in DomainSchemaIT/OrderPersistenceIT/CustomerPersistenceIT, V1+V2 history assertions in foundation/schema tests, real transaction/concurrency tests and no new Maven dependency. Preserve old constraint/checksum tests and all quality gates; production and Order protocol remain excluded. Implementation approval, complete ADR-0006 acceptance, human implementation acceptance and commit/push authorization are distinct.

### Increment 4 Execution Precedence and Persistent Database Safety

Only the [current implementation prompt](../04-agentic-coding/prompts/05-product-customer-api.md#persistent-local-database-safety-gate) is executable after explicit coding authorization; earlier sketches and approval prerequisites are supporting history. That authorization covers the reviewed implementation/test scope without another general approval cycle; ADR-0006 remains Proposed until separate status approval. Persistent local migration/startup must first establish the actual local target, read-only history/schema compatibility and usable documented recovery evidence. Missing recovery evidence blocks only local migration (Blocked — Human Action Required); disposable Testcontainers migration tests and remaining approved work continue. Never equate their success with local migration or use destructive recovery shortcuts.

### Increment 4 Actual Execution and Remaining Gates

[Increment 4 execution evidence](../04-agentic-coding/prompts/05-product-customer-api.md#implementation-results--executed-pending-human-review) records ten implemented endpoints, application transactions, strict input/error handling, Product/Customer versions and reviewed V2. Final verification passed 240 fast/44 PostgreSQL tests with fresh-run coverage and preserved prior functionality. No dependency/CI/V1 change was made. Persistent local migration is Blocked — Human Action Required pending target/history and usable recovery evidence; disposable migration verification completed independently. Earlier pending-implementation language in planning sketches is retained as pre-execution context and superseded for current execution status by this record. Human implementation acceptance and any commit/push/merge authorization remain separate; ADR-0002/0006 stay Proposed.
