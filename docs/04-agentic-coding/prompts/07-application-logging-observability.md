# Increment 6 — Application Logging & Observability

**Status:** Increment 6 — Functionally Stabilized, Frozen — Full Acceptance Pending

**Date:** 2026-10-16

**Authorization:** L1–L6 remain Human Architecture Approved. The human freeze decision below supersedes all earlier execution authorizations: no further Increment 6 implementation, remediation or quality-optimization work is authorized. Full acceptance remains Pending.

**Historical planning precedence (superseded by the current approval record below):** The [final lifecycle review](#final-l2-lifecycle-design-review--2026-10-16) is current for the two terminal-boundary/registration decisions. [Async HTTP Architecture Refinement — 2026-10-16](#async-http-architecture-refinement--2026-10-16) retains the human-decided async scope and remaining design. Earlier inconsistent lifecycle/status-access claims are historical where superseded. **L2 Lifecycle Design — Reviewed, Pending Human Approval**; no implementation is authorized.

## Objective

Introduce coherent, secure, transaction-aware application logging and HTTP request correlation using existing infrastructure. Improve diagnosis without changing Product, Customer or Order behavior. This is a technical cross-cutting increment, not a new business feature or proof of production readiness.

## Context and Authority

The [PRD](../../01-requirements/product-requirements.md), [system architecture](../../02-architecture/system-architecture.md), [transaction conventions](../../02-architecture/transaction-locking.md), [coding guidelines](../coding-guidelines.md), [Java/Spring guidance](../java-spring-guidelines.md), [testing strategy](../../05-testing/testing-strategy.md) and [AI workflow](../ai-development-workflow.md) remain authoritative. Preserve [Increment 5 human acceptance](06-order-placement-concurrency.md#final-human-acceptance--2026-10-16), its G1–G6 decisions and all historical prompts. Complete ADR-0002 and ADR-0006 remain Proposed. The original proposal is preserved below; the current Human Architecture Approval and Execution record governs L1–L6.

## Historical Approved Planning Scope

Read-only source/dependency/configuration inspection; this proposal; concise mandatory logging guidance in root AGENTS.md; navigation and current planning/backlog updates. No Java, test, POM, configuration, CI or database changes; no builds, tests, formatting, PIT or SonarQube execution. Architecture approval, implementation authorization and later implementation acceptance are separate gates.

## Current Baseline — Inspected Facts

- Inspection branch: `develop`; clean initial working tree; HEAD `9345fa122df05fd5850ae183834f1b5c141cf3f7`. Increment 5 delivery `52da5583d97b0b3828a10b58b717abcb81c1a3e9` is retained in history, followed by SonarQube remediation and the separate committed Spotless migration. There are no uncommitted formatting changes to alter. This proposal does not retrospectively grant acceptance to another task.
- Java 21, Spring Boot 4.1.1 and Maven Wrapper 3.9.9 remain unchanged. Root POM has the approved `logback.version=1.6.5` security override. Cached published starter POMs show `spring-boot-starter-data-jpa → spring-boot-starter → spring-boot-starter-logging`; no exclusion in the project removes it. The starter's original Logback version is superseded by the project override.
- Existing Surefire `OrderTest` XML classpath records `spring-boot-starter-logging:4.1.1`, `slf4j-api:2.0.18`, `logback-classic/core:1.6.5`, `log4j-to-slf4j:2.25.5`, and `jul-to-slf4j:2.0.18`. This is preserved execution evidence inspected read-only, not a newly resolved dependency graph or a new runtime-provider test. No additional logging dependency is presently justified.
- `src/main/resources/application.yml` sets application name, JPA validation/OSIV, SQL initialization and Flyway defaults. Local and integration-test profiles configure database/Flyway behavior. None defines application log levels, MDC patterns, JSON logging or access logging. No repository Logback customization was found. Framework logging already exists; application logging is not globally absent.
- Source inspection found no explicit application logger/MDC calls or application `System.out`/`System.err`/stack-trace printing. `OrderPlacementConcurrencyIT` enables `org.hibernate.SQL=DEBUG` for test evidence; that is not a production logging policy and does not justify bind-parameter logging.
- `ApiWebConfiguration` registers `ApiRequestInterceptor` for strict media/path validation and the strict request converter. It has no request correlation or completion logging. Preserve its processing precedence and JSON rules.
- `ApiExceptionHandler.handle/classify` selects fixed safe error codes and delegates to `ApiErrorResponseWriter`; `ApiErrorController` covers servlet fallback. There is no explicit application diagnostic event ownership. Preserve fixed response bodies, statuses, Allow behavior and incompatible-Accept handling.
- `OrderPlacementCoordinator.place/executeAttempt/retryAfterRollback` owns independent TransactionTemplate attempts, Customer-first/Product-ordered locks, structured retry classification and commit observation. A successful `executeAttempt` return is outside template completion/cleanup. `save`, `flush` and callback return are not commit evidence. Missing/inactive decisions originate inside the attempt; no entities should be accessed after it ends.
- `CustomerApplicationService.delete` is a void, proxied READ COMMITTED operation: locked Customer, separate Order reference query, then physical delete, deactivation or referenced-inactive no-op. Its method body returns before the proxy completes the transaction. Do not add a new result/HTTP contract solely for logging.
- Relevant existing tests include `OrderPlacementCoordinatorTest`, `OrderRetryPolicyTest`, `OrderPlacementConcurrencyIT`, `CustomerDeletionTest`, `OrderApiTest/IT`, `CustomerApiIT`, `ApiErrorHandlingTest`, `ApiExceptionClassificationTest`, `ApiErrorDispatchIT`, `ConfiguredErrorPathIT` and `ExpectedApiErrors`. They protect business/error/completion behavior, not an approved logging contract. `ApiTestSupport` constructs MockMvc without registering servlet filters automatically; future correlation tests must explicitly include the actual filter.
- Existing Java/Spring guidance requires SLF4J, safe logging and explicit approval for new filters/observability dependencies. The new AGENTS.md section establishes safety expectations, not acceptance of L1–L6. Deployment, log retention/access and operational guarantees remain Open.

## Proposed Architecture

**All choices in the following sections remain Proposed.** Use owner-local SLF4J loggers and parameterized, stable event identifiers with a small allowlist of typed fields. Keep Logback and existing Boot bridges; no custom logging framework, AOP logging, blanket entry/exit tracing or new dependency. Domain entities and value objects should not become logging infrastructure owners.

Prefer one small servlet correlation/completion filter in `shared.api` over extending the validation interceptor: it can surround MVC processing, including requests rejected before controller invocation. Register explicitly through existing configuration without double registration. Keep validation in the interceptor. An interceptor-only alternative has a narrower boundary and needs separate coverage for unresolved routes, early failures and error dispatch.

Use the central error boundary for unexpected technical failures and owner-local events for Order outcomes. Do not duplicate the same exception through controller, service, repository and HTTP logs. Completion/access events contain status/timing only, not a second stack trace. Known terminal Order technical/exhaustion outcomes belong to the coordinator; the handler must suppress duplicate diagnostics only for categories whose coordinator paths are all demonstrably covered, including interrupted backoff. Other unexpected exceptions remain handler-owned. The fallback controller must coordinate ownership with advice/error dispatch rather than emit another diagnostic for the same failure. L3 must approve this ownership rule before implementation.

No request/response body wrappers or generic payload capture. No managed entity, lazy getter, new query or state refresh for logging. Technical correlation may be read from MDC without introducing HTTP dependencies into feature application operations.

## Proposed Logging Event Matrix

Common safe fields, if approved: fixed event ID, generated request ID when present, bounded outcome/reason enum and monotonic elapsed time. Optional fields below are additive allowlists, not permission to serialize an object. Global exclusions: secrets, headers, bodies, PII, raw resource IDs, raw paths/queries, entity state, SQL parameters and exception messages/stacks unless separately reviewed. Test labels refer to the matrix below.

| Proposed event | Owner and trigger | Level | Additional safe fields | Explicit exclusions | Transaction/outcome semantics | Required tests |
| --- | --- | --- | --- | --- | --- | --- |
| HTTP_REQUEST_COMPLETED | Correlation filter at terminal request completion | INFO for 2xx/3xx, DEBUG for expected 4xx, WARN for 5xx | Allowlisted method, route template or OTHER, final status, duration | Actual URI, query, headers, payload, exception details | HTTP outcome only; HTTP 5xx does not establish DB rollback; once per logical request | H1–H4 |
| HTTP_TECHNICAL_FAILURE | Advice or fallback, for an otherwise unowned unexpected technical exception | ERROR | Fixed API code, allowlisted failure category | Raw throwable message/stack, cause graph, input | One diagnostic owner; do not log expected validation/404 as ERROR | U1/H3/P3 |
| ORDER_COMMITTED | Coordinator after successful TransactionTemplate return | INFO | Attempt number, consolidated item count | Customer/Product/Order IDs, prices, names, entities | Confirmed DB commit; not a promise that the HTTP response reached the client | U2/P1 |
| ORDER_REJECTED | Coordinator after confirmed rollback for missing/inactive resources | DEBUG | Fixed reason enum | Input and referenced identities, entity state | Expected rejection, not a technical ERROR; one event rather than per-item logs | U2/P2 |
| ORDER_ATTEMPT_ROLLED_BACK | Coordinator for a transient candidate after rollback completion and released-resource guard | DEBUG | Attempt number, approved SQLSTATE when known | Localized SQL errors, constraint text, SQL values | Log only observed rollback; eligibility still requires accepted fail-closed policy | U2/P2 |
| ORDER_RETRY_SCHEDULED | Coordinator after eligibility, attempt/budget checks, immediately before backoff | WARN | Attempt/next-attempt, approved SQLSTATE, 25/50ms delay | SQL text, exception graph, payload | A scheduling decision, not proof the next attempt started; never before cleanup | U2/P2 |
| ORDER_RETRY_EXHAUSTED | Coordinator when accepted attempt/budget policy forbids replay | ERROR | Attempt count, limit/budget reason | Raw exceptions and business data | Confirmed rollback, operation not completed; preserve existing safe 503 | U2/H3/P2 |
| ORDER_OUTCOME_UNKNOWN | Coordinator when successful commit/full rollback cannot be established | ERROR | Attempt number, fixed UNKNOWN outcome category | Any claim of rollback, identifiers, raw exception | Possibly committed; no replay, no success event; preserve existing safe 500 where possible | U2/H3/P3 |
| CUSTOMER_DELETED | Customer application operation, captured physical-delete branch, post-commit callback | INFO | Fixed PHYSICAL_DELETE outcome | Customer identity/email/name, entity object | Only after commit; no success event on rollback/unknown completion | U3/P4 |
| CUSTOMER_DEACTIVATED_BY_DELETE | Same owner, active/referenced branch, post-commit callback | INFO | Fixed DEACTIVATED outcome | Customer/order data, reference IDs | Confirmed committed deactivation, no extra reference query | U3/P4 |
| CUSTOMER_DELETE_NOOP | Same owner, referenced/inactive branch, post-commit callback | DEBUG | Fixed ALREADY_INACTIVE outcome | Customer data | Successful completion without mutation; do not claim a write/version increment | U3/P4 |

Attempt start and individual lock failures may have DEBUG diagnostics only if needed under L3: attempt ordinal and CUSTOMER/PRODUCT phase are safe; no resource identifier, entity dump or new failure classifier. WARN is for a retry actually scheduled, not every RuntimeException from a lock provider. Expected business logging is deliberately low volume; no per-item or mandatory per-class logs. ERROR means inability to complete or unexpected/uncertain outcome, not simply an exception being present.

## Proposed HTTP Request Correlation — L2

**Initial proposal — historical where superseded:** The [Async HTTP refinement](#async-http-architecture-refinement--2026-10-16) records the subsequent human scope decision and is the current L2 design proposal. Earlier suggestions to leave ASYNC unsupported or omit relevant request-worker propagation are superseded; the original text remains for traceability.

Recommendation: generate a fresh server-side UUID for every incoming logical HTTP request; ignore inbound request-ID values rather than trust/echo them or introduce new 400 errors. Candidate header `X-Request-ID`, MDC key `requestId`, canonical lowercase UUID representation, exactly 36 ASCII characters (maximum 36). Header behavior is a proposed public contract addition, not already accepted. Alternative: accept a strictly validated bounded inbound ID from a trusted gateway; this needs an explicit trust boundary, character/length policy and tests. Another alternative is internal-only IDs without a response header. No client-supplied value enters a log before validation.

Store the server ID in a request attribute so error redispatch reuses it. Proposed response behavior: one `X-Request-ID` on normal and application-managed error responses, set before the response is committed; no error-body, Location, Allow or media contract changes. Do not promise a header after a connection abort or failure outside the application's filter boundary. CORS exposure is not implicitly authorized.

Bind only the owned MDC key for each dispatch and remove it in `finally` on success and exception paths. For nested dispatch, restore the enclosing owned scope; at the outer request boundary leave no request ID on a reused thread. Do not clear unrelated MDC entries. Do not use MDC as business state or as a retry/transaction signal.

REQUEST, ERROR and ASYNC behavior requires explicit filter registration and completion ownership. A once-per-dispatch filter is not automatically once-per-logical-request completion. For synchronous MVC, handled errors complete normally; unhandled errors must defer final status logging to error dispatch rather than report a premature success. Reuse the request ID on nested/later ERROR and deduplicate terminal events with request-owned state. Transport aborts should use a safe HTTP_REQUEST_ABORTED/WARN diagnostic with unknown delivery, not fabricate a completed response/status.

The current business endpoints are synchronous; test executors are not application async propagation. Recommend no new executor/tracing propagation in this increment. Nevertheless, if servlet async is entered, do not log completion when the initial dispatch returns: explicit async-dispatch MDC scoping plus a minimal servlet lifecycle completion/error/timeout strategy must be specified and tested under L2, or that mode must be documented as unsupported before implementation approval. Restore/clear context on every participating thread. Do not claim inherited MDC across executors. Async completion must not produce duplicate terminal logs or convert timeout into a known database outcome.

Route field: choose from the fixed API templates (`/products/{id}`, `/customers/{id}`, `/orders/{id}` and their collections), with OTHER for unmapped/fallback routes. Never log raw URL or query text. Normalize methods to a fixed allowlist with OTHER fallback; do not log arbitrary attacker-controlled tokens. Prefer a duration measured by a monotonic clock.

## Transaction and Retry Safety — L4

Preserve accepted G1–G4: ambient rejection; fresh READ COMMITTED template attempts; Customer-first and canonically ordered individual Product locks; three total attempts; transaction-local 2s PostgreSQL lock timeout; 25/50ms backoff; 10s monotonic scheduling budget; full rollback and released resources before replay. No numeric/logging setting changes these policies. The budget is not a hard HTTP deadline; logging adds overhead and must not introduce a new time guarantee.

| Point in current flow | Permitted proposed observation |
| --- | --- |
| Before/inside attempt | DEBUG attempt-start after validation/ambient guard; never success. A lock-call RuntimeException alone does not establish configured lock timeout or retry eligibility. |
| save/flush or template callback return | No committed-success event. Preserve exception propagation and atomicity. |
| Template throws, completion ROLLED_BACK and resource guard passes | Rollback event. Retry candidate only through existing OrderRetryPolicy, including consistent SQLSTATE graph, positive timeout attribution and unsafe/cyclic-chain rejection. |
| Retry attempt/budget checks pass | WARN scheduled retry immediately before existing backoff. A pause interruption or elapsed budget must not generate a fictitious next attempt. Preserve original interruption and failure mapping. |
| Attempts/budget exhausted after safe rollback | One terminal exhaustion event; preserve safe 503 and no replay. |
| Transaction completion unknown or potentially committed | One UNKNOWN event; never “failed and rolled back”, never replay; no reconciliation mechanism added. |
| TransactionTemplate returns successfully | Order commit-confirmed INFO after completion/resource cleanup; use already materialized safe primitives, not entities or further queries. |

Customer deletion cannot log committed success inside its proxied method body. Recommend capture only the fixed outcome enum within the existing transaction and register a minimal post-commit observation; never change its void signature, take additional locks, or query after completion. The callback must perform no business operation. Rollback and unknown completion suppress all three success/no-op events. Registration/callback behavior, including calls participating in an outer transaction, needs U3/P4 evidence; no success fallback if synchronization is unavailable.

Logging must remain best effort and outside retry decisions. Bound metadata construction; avoid side-effecting/throwing getters and lazy argument evaluation. A narrow protection around application event emission must prevent ordinary logger/appender RuntimeExceptions from replacing a result, masking the original failure, escaping a post-commit callback or entering the retry catch as a new business failure. Do not catch arbitrary application work, swallow the original business exception, catch all Throwable, recursively log a logger failure or promise survival of JVM-fatal failures. No custom durable logging/outbox or exactly-once audit guarantee. Verify this failure boundary with a deliberately failing test appender.

## Security and Data Minimization — L5

Allowlist only fixed event/reason/category labels, canonical server request ID, approved SQLSTATE, attempt/delay/count, monotonic duration, bounded HTTP status/method/template and fixed deletion outcome. Business resource identifiers are excluded initially; adding one requires an explicit diagnostic need and privacy decision. Request IDs are correlation metadata, not authentication, authorization or an audit identity; absence outside HTTP is allowed.

Prohibit passwords, tokens/API keys, authorization/cookies, whole headers or bodies, customer names/emails, payload fragments, raw SQL values, arbitrary paths/query strings and persistence/entity dumps. Parameterized logging alone does not sanitize hostile values. Canonical ID validation and fixed labels prevent CR/LF/control-character injection; test hostile input and ensure it is absent from output, not merely escaped in the response.

Default technical diagnostics contain fixed safe categories, not `Throwable.toString`, localized messages, getCause chains or raw stack traces. A throwable can leak SQL/input even through a parameterized logger call. Detailed stack traces require a separate reviewed redaction/allowlist and restricted operational policy; do not silently enable them in local DEBUG. Existing framework/driver logs may expose messages independently: review effective logger levels and error-path output under L5, document residual risk, and do not claim application-field allowlists sanitize all third-party logs. Do not silence all infrastructure errors just to pass a security assertion.

Keep Hibernate SQL and bind-parameter TRACE disabled in ordinary local/production operation; the existing test-only SQL setting remains separate evidence. Collectors, log access, encrypted transport/storage, retention duration, deletion procedures and incident access need an operator-approved policy before production. No arbitrary retention period or compliance/security guarantee is selected here. Console logs are operational diagnostics, not a durable audit trail.

## Proposed Configuration — L1

Keep the current Boot/SLF4J/Logback stack and approved security versions. Recommend console plain text with stable key/value event fields and the approved MDC field in the console pattern; root/application INFO by default, no payload logging or SQL/bind TRACE. Local troubleshooting may temporarily enable targeted application DEBUG without enabling secrets or raw exception output. TRACE stays disabled normally. Framework noise should be reviewed per logger, preserving useful startup/failure warnings rather than globally muting the framework.

Prefer Boot YAML level/pattern settings over a custom Logback configuration unless tests demonstrate a need. No file appender or rotation policy is needed for the proposed console-only application output; deployment logging infrastructure must own rotation, retention and access before production. Production packaging/deployment is not authorized here. Tests may attach an in-memory Logback appender using already available classes, restoring logger levels/appenders and thread state afterward.

Structured JSON output is an alternative if a concrete consumer requires it; even potentially built-in options need an approved schema/privacy/configuration decision. Distributed tracing, OpenTelemetry, metrics, dashboards, remote aggregation and asynchronous logging are separate optional work, not implied by existing transitive libraries or this increment's title.

## Mandatory Future Test Matrix — L6

All tests below are proposed requirements, not implemented or executed results. Assert event identifiers, levels, allowed fields, counts and outcome timing; human-readable sentences are not a brittle public API. Use existing JUnit/Spring/Logback facilities without an unapproved test library.

| ID / level | Concrete existing or proposed owner | Required evidence |
| --- | --- | --- |
| U1 / fast | Existing ApiExceptionClassificationTest and ApiErrorHandlingTest; proposed ApplicationLoggingTest if needed | Expected 400/404 not ERROR; safe unexpected-failure category; one diagnostic owner; no raw messages/stacks/PII; concurrent appender isolation and restoration. |
| U2 / fast | OrderPlacementCoordinatorTest and OrderRetryPolicyTest | Before commit no success; confirmed completion → one success; rollback/cleanup before scheduling; accurate attempt/delay/exhaustion; pause interruption/budget expiry; unknown commit → no success/replay; failing emitter cannot change result, cause or retry count. Keep classifier assertions unchanged. |
| U3 / fast | CustomerDeletionTest | Three branch classifications; no success during transactional body; callback emits only on confirmed commit; rollback/unknown suppress; callback logging failure does not alter business outcome. Do not pretend a mock transaction proves real commit. |
| H1 / MVC | Proposed RequestCorrelationFilterTest with actual filter registration | Generated canonical ID/header; ignored hostile/multiple/oversized inbound IDs under approved contract; no new 400; MDC visible inside processing and absent afterward on the same thread, also after failures. |
| H2 / MVC + real servlet | Proposed HttpRequestLoggingTest; ApiErrorDispatchIT and ConfiguredErrorPathIT | Normal, early validation, unmapped route, nested/later ERROR, abort and approved ASYNC behavior; one terminal event; final status; same ID across dispatch; no cross-request/thread leakage. MockMvc alone is insufficient for container redispatch. |
| H3 / MVC | ApiErrorHandlingTest, OrderApiTest, CustomerApiTest and ExpectedApiErrors | Exact existing JSON/status/Allow/Location/media contracts preserved; extra header only if L2 approved; no duplicate technical diagnostics; HTTP 5xx never interpreted as automatic DB rollback. |
| H4 / security | Proposed RequestCorrelationFilterTest / HttpRequestLoggingTest | Bodies, authorization/cookies, email, password-like input, path IDs, queries and CR/LF canaries absent from captured application output; known route-template/method fallback; safe 4xx flood behavior. Include framework-output review separately. |
| P1 / PostgreSQL | OrderPlacementConcurrencyIT and OrderApiIT | Real committed rows plus success event after completion; callback/flush failure emits no commit event; response failure after commit does not retrospectively mean rollback. |
| P2 / PostgreSQL | OrderPlacementConcurrencyIT | Real lock timeout/rollback, fresh attempts, unchanged SQL lock order/resource release, retry/exhaustion counts, atomicity; no extra logging query or entity access. |
| P3 / PostgreSQL | Existing lost-confirmation/commit-failure cases and ApiErrorDispatchIT | May be committed plus UNKNOWN log, one attempt/no replay/no false rollback or success; preserve existing safe response and diagnostic deduplication. |
| P4 / PostgreSQL | CustomerApiIT and OrderPlacementConcurrencyIT | Physical delete, referenced deactivation and inactive no-op logged only after actual commit; rollback/failed commit/outer transaction suppress false success; both first-order race winner orders unchanged. |
| A1 / architecture | Source/dependency/diff review, existing Maven checks | SLF4J only; no application println/printStackTrace/payload dumps, domain logger dependency, foreign persistence access, new dependency or unapproved HTTP/configuration behavior; no migrations, CI or locking changes. |

Future execution after separate authorization: Java 21 Maven Wrapper `spotless:check`, `validate`, focused fast tests, then `clean verify` with disposable PostgreSQL; report exact tests/failures/errors/skips and JaCoCo evidence. Run applicable existing PIT only if authorized for that implementation; preserve XML outcomes and compare matching identities, with TIMED_OUT distinct from KILLED and PostgreSQL *IT still excluded. No new numeric mutation gate. Document unavailable gates as Pending; human review/acceptance and any commit/push remain separate.

## Scope and Exclusions

Candidate implementation scope is correlation/completion logging, safe centralized technical events and transaction-aware Order/Customer DELETE events, approved configuration and corresponding tests. No new business feature/REST endpoint, schema/migration, database operation, monetary change, lock/retry-policy change, idempotency/reconciliation mechanism, authentication feature, external aggregation service, dashboard, tracing/OpenTelemetry, deployment or unapproved dependency/CI change. No logger in every class, generic request-body capture or mandatory entry/exit logging.

## Human Architecture Decision Gates

L1–L6 were **Human Architecture Approved — 2026-10-16**, followed by separate implementation authorization. The recommendation/alternative columns below preserve the proposal rationale; the current approval and execution record supersedes their earlier pending-approval wording. Runtime verification and implementation acceptance remain separate.

| Gate | Recommended decision | Alternatives and consequences | Required tests/evidence | Status |
| --- | --- | --- | --- | --- |
| L1 — Framework/configuration | Existing SLF4J/Logback, no new dependency; console key/value text, default INFO and targeted local DEBUG | JSON if a consumer needs it; custom Logback only for a proven configuration gap. Retention/production access remains operational work | Current dependency evidence, effective output/pattern, U1/A1 | Human Architecture Approved — 2026-10-16 |
| L2 — Correlation/HTTP contract | Async HTTP scope Human Decided; [final lifecycle review](#final-l2-lifecycle-design-review--2026-10-16) selects A: requestDestroyed terminal ownership and A: narrowly scoped startAsync observation wrapper | Reference-container-qualified completion, transparent overload delegation and conservative status provenance; no header or implementation approval follows | Focused F1–F6 acceptance evidence below plus existing refined H/P matrix | Human Architecture Approved — 2026-10-16 |
| L3 — Taxonomy/levels/ownership | Proposed matrix; INFO confirmed significant completion, DEBUG expected rejection/no-op, WARN actual scheduled retry, ERROR terminal/unknown failures; single technical diagnostic owner | Fewer INFO access events/targeted DEBUG if volume warrants; no blanket per-layer exception logging. Validate coordinator/advice/fallback ownership before suppressing duplicates | U1/U2/H2/H3 and event counts for all terminal branches | Human Architecture Approved — 2026-10-16 |
| L4 — Transaction/retry semantics | Order success outside successful template completion; Customer outcome observation post-commit; safe best-effort emission, no semantic or retry-policy change | TransactionSynchronization vs another existing supported completion mechanism, only with equivalent actual timing/outer-transaction evidence; never log success after save/flush | U2/U3/P1–P4; appender-failure tests, no false completion or replay | Human Architecture Approved — 2026-10-16 |
| L5 — Security/privacy | Explicit field allowlist; no resource IDs/PII/bodies/headers/raw throwable contents; sanitize untrusted metadata; review third-party output and operational access | Reviewed restricted diagnostics/identifiers only for a concrete need; no automatic permission from DEBUG. Retention/access controls require operator approval | H4/U1/A1; exception/SQL canaries; recorded framework-output risks | Human Architecture Approved — 2026-10-16 |
| L6 — Verification/acceptance | U/H/P/A matrix with actual events/timing/privacy evidence and unchanged regression contracts; no logging sentence or numeric PIT gate | Narrower implementation must explicitly defer affected tests and capability claims, not report them Passed | Exact future results, complete diff, safety/Spotless checks, human acceptance; pending gates explicit | Human Architecture Approved — 2026-10-16 |

## Proposed File and Responsibility Matrix

Paths are future candidates, not files created by this planning task. Resolve exact minimal scope after L1–L6 approval.

| Existing file or candidate | Future responsibility |
| --- | --- |
| src/main/java/com/example/agenticorderservice/shared/api/RequestCorrelationFilter.java (proposed) | Bounded generated correlation, owned MDC cleanup and approved terminal HTTP events/dispatch lifecycle |
| src/main/java/com/example/agenticorderservice/configuration/ApiWebConfiguration.java | Explicit filter registration if suitable; preserve existing interceptor/converter. A dedicated small configuration class is an alternative only if clearer |
| src/main/java/com/example/agenticorderservice/shared/api/ApiExceptionHandler.java and ApiErrorController.java | Safe technical diagnostic ownership/deduplication without changing response classification or envelope |
| src/main/java/com/example/agenticorderservice/order/application/OrderPlacementCoordinator.java | Approved post-completion/rollback/retry/terminal events, protected against logger failures |
| src/main/java/com/example/agenticorderservice/customer/application/CustomerApplicationService.java | Capture existing delete decision, post-commit observation without new result type/query |
| src/main/resources/application.yml and application-local.yml | Approved levels/pattern/MDC visibility; no secrets or migration setting changes |
| src/test/java/com/example/agenticorderservice/shared/api/RequestCorrelationFilterTest.java and HttpRequestLoggingTest.java (proposed) | H1/H2/H4, with real filter and servlet coverage where required |
| Existing test classes named in U/H/P/A matrix; src/test/resources/application-integration-test.yml if needed | Outcome/timing/privacy/regression evidence and isolated appender setup |
| AGENTS.md, coding-guidelines.md, this playbook, index, implementation-plan.md, open-items.md | Concise rules, approved decisions and actual later evidence; preserve historical records |

No Maven, Makefile, CI, entity/repository or migration change is proposed. Domain models do not need logging dependencies. Do not add a shared logging abstraction unless concrete repetition warrants a minimal, approved solution.

## Future Implementation Prompt — Non-Executable Outline

**Do not execute before explicit human architecture approval and separate implementation authorization.**

After all L1–L6 choices are explicitly recorded, replace this outline with one complete, reviewed implementation prompt while retaining its history. That future prompt must name the approved contract/taxonomy/files; preserve Increment 5 G1–G6, safe API errors and completion semantics; implement only approved correlation and safe event owners; protect original outcomes from logging failures; test the full applicable U/H/P/A matrix with existing dependencies; preserve Spotless and quality workflows; capture actual regression/coverage and any separately authorized PIT evidence; report residual operational risks and complete diff. No migration, persistent database operation, unapproved dependency, commit, push or deployment follows from this outline.

## Result — Planning Only

Repository/source and preserved dependency evidence inspected read-only. This proposal, root agent guidance and navigation/planning references are the only task changes. No logging code, runtime configuration or tests implemented; no build, formatter, test, PIT, SonarQube or database command executed. Runtime logging behavior, privacy assertions, request lifecycle and transaction-event timing remain unverified. Architecture and implementation approvals are pending independently. Documentation verification checked 122 relative links/anchors, balanced fenced blocks, pending L1–L6 status, unchanged Increment 5/ADR records and the exact original request. Git whitespace checks cover tracked additions and the untracked playbook; no unrelated file changes or staged content are present.

## Lessons Learned

The existing Boot stack already supplies the logging abstraction/provider; adding a framework would not address missing event ownership. Order and proxied Customer deletion have different observable completion boundaries. Framework logs already exist and require separate privacy review. These are inspection/design observations, not lessons from executed logging tests or production operation.

## References

- [Spring OncePerRequestFilter lifecycle documentation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/OncePerRequestFilter.html): dispatcher registration and completion behavior require explicit design/testing; the class name alone is insufficient.
- [Logback MDC documentation](https://logback.qos.ch/manual/mdc.html): MDC is thread-associated; propagation/cleanup across executor boundaries requires explicit care.

## Prompt — Immutable Planning Request

The following is the exact human planning request. It authorizes this documentation task only; it is not a future implementation prompt.

````text
# Increment 6 — Application Logging & Observability
## Architecture Planning, AGENTS.md Guidelines and Documentation Only

You are acting as a Senior Java Software Architect responsible for preparing **Increment 6 — Application Logging & Observability** in the `agentic-order-service` project.

This is an architecture and planning task only.

**Do not implement application logging yet.**

## 1. Project Context

Repository:

`/Users/rookie/Documents/Development/Projekte/agentic-order-service`

Expected branch: `develop`

Last accepted implementation:

**Increment 5 — Order Placement & Concurrency**

Status: **Reviewed – Accepted, Committed & Pushed**

Accepted commit:

`52da5583d97b0b3828a10b58b717abcb81c1a3e9`

The application is a Java 21 / Spring Boot 4.1.1 / Maven / PostgreSQL backend with:

- Product REST operations.
- Customer REST operations, including coordinated deletion.
- Order creation, retrieval and listing.
- Explicit transactional order placement.
- PostgreSQL pessimistic locking.
- Bounded transaction retries.
- Strict HTTP validation and centralized API errors.
- Unit, MVC and PostgreSQL integration tests.
- JaCoCo and PIT mutation testing.

The functional MVP scope has been implemented and accepted.

Increment 6 is a **technical cross-cutting increment**, not an extension of the business feature set.

A separate Spotless / Palantir Java Format migration may currently be present as uncommitted working-tree changes.

**Preserve those changes exactly. Do not stage, revert, overwrite, format or commit them.**

## 2. Preconditions and Repository Inspection

Before making documentation changes:

1. Read `AGENTS.md` in full.
2. Inspect current branch, HEAD, staged, unstaged and untracked files.
3. Inspect the existing project documentation structure.
4. Read the original PRD and relevant architecture documentation.
5. Review the accepted Increment 5 playbook and its final acceptance record.
6. Inspect Spring Boot dependencies and existing logging configuration.
7. Inspect the central exception handler, HTTP interceptor and OrderPlacementCoordinator.
8. Inspect applicable Java/Spring, coding, testing and security guidelines.
9. Inspect the implementation plan, open items and prompt index.

Determine the existing logging capabilities before proposing new tools or dependencies.

Spring Boot commonly provides SLF4J and Logback through its starters. Verify whether this is true for this repository rather than assuming it.

Do not execute builds, integration tests, mutation testing, database operations or source-code formatting for this documentation-only task.

If repository state or authoritative instructions conflict with the requested scope, stop and report the conflict.

## 3. Update AGENTS.md — Mandatory Logging Guidance

Extend the existing `AGENTS.md` with a concise, enforceable section titled:

`Logging and Observability`

Preserve the existing structure, instructions and architectural boundaries.

The new guidance must establish the following principles.

### 3.1 Logging Is a Cross-Cutting Responsibility

Every future implementation must consider whether significant application events, failures or retry decisions require logging.

Do not require a logger in every class or method.

Avoid redundant logs and noisy entry/exit logging.

Prefer structured, meaningful events that support production diagnosis and operational monitoring.

### 3.2 Use the Existing Logging Abstraction

Prefer the project's existing Spring Boot logging infrastructure, subject to repository verification.

Use SLF4J through the established framework.

Do not introduce a new logging framework without an explicit architecture decision.

Do not use `System.out.println`, `System.err.println` or stack-trace printing for application logging.

Use parameterized logging rather than string concatenation.

### 3.3 Log Levels

Define clear initial expectations:

- TRACE: very detailed diagnostics; normally disabled.
- DEBUG: development and troubleshooting details.
- INFO: significant completed application or operational events.
- WARN: recoverable abnormal conditions, including approved transient retries.
- ERROR: unexpected technical failures or operations that cannot complete.

Log level must reflect the actual outcome, not merely the presence of an exception.

Expected 400 validation failures and ordinary 404 business results must not automatically generate ERROR logs.

Avoid duplicate logging of the same exception at multiple layers.

### 3.4 Transaction-Aware Logging

Logging must preserve the accepted Increment 5 transaction guarantees.

In particular:

- Never log an Order as successfully committed immediately after `save()` or `flush()`.
- A confirmed success event requires successful transaction completion.
- Distinguish confirmed rollback, retry scheduling, retry exhaustion and unknown commit outcome.
- Never describe an uncertain outcome as a confirmed failure or rollback.
- Logging must not cause automatic transaction replay.
- Logging must not alter transactional boundaries, locking order, retries or business behavior.
- Logging failures must not compromise business transactions.

### 3.5 Security and Privacy

Do not log:

- Passwords, authentication tokens, API keys or secrets.
- Authorization or Cookie headers.
- Complete HTTP request or response bodies.
- Customer email addresses or other unnecessary personal data.
- Raw SQL parameters containing confidential data.
- Sensitive exception contents without review.

Prefer explicit allowlists of safe operational metadata.

Avoid logging arbitrary request paths when they may contain sensitive identifiers.

Never add request-body logging as a generic troubleshooting mechanism.

Apply data minimization to identifiers and correlation metadata.

### 3.6 Request Correlation

Require an explicit, documented correlation strategy for HTTP requests.

Consider MDC and a generated request identifier.

Correlation information must be removed at request completion to prevent context leakage across reused servlet threads.

Do not assume MDC automatically propagates across asynchronous tasks.

Header trust, request-ID validation, allowed characters/length, response-header behavior and asynchronous propagation must be decided in the architecture review.

### 3.7 Testing

Logging behavior must be testable where it protects operational or security guarantees.

Future logging changes should include checks for:

- Correct log levels and safe event content.
- No secrets or sensitive payloads.
- Correlation creation and cleanup.
- Correct post-commit success semantics.
- Accurate retry and unknown-outcome reporting.
- Absence of duplicate exception logging where practical.

Do not make exact human-readable log sentences a brittle project-wide API contract unless explicitly approved.

### 3.8 Agent Responsibilities

Explicitly instruct coding agents to:

- Follow the accepted logging design.
- Include relevant logging and observability considerations in implementation planning.
- Avoid excessive logging.
- Never invent logging semantics independently when an architecture decision remains open.
- Preserve established transaction and privacy guarantees.

Keep the AGENTS.md section concise. Detailed architectural decisions and examples belong in the Increment 6 playbook.

## 4. Create the Increment 6 Playbook

Create:

`docs/04-agentic-coding/prompts/07-application-logging-observability.md`

Title:

`# Increment 6 — Application Logging & Observability`

Initial status:

**Proposed — Pending Human Architecture Review**

Date:

2026-10-16

This document must be a complete, reviewable architecture proposal, not an executable implementation instruction.

Use the established playbook structure and project terminology.

Include the following sections.

### 4.1 Objective

Introduce coherent, secure and transaction-aware application logging and HTTP request correlation.

Improve operational diagnostics without changing business behavior.

Explicitly exclude new Product, Customer or Order business features.

### 4.2 Current Baseline

Document the actual inspected repository state:

- Existing logging dependencies and providers.
- Current application logging configuration.
- Existing HTTP request interception.
- Existing exception handling.
- Existing Order placement/retry implementation.
- Existing tests relevant to logging.
- Existing documentation requirements.

Distinguish confirmed facts from assumptions.

Do not claim logging is completely absent merely because application classes currently contain few explicit logger statements.

### 4.3 Proposed Architecture

Evaluate a minimal design using the existing Spring Boot infrastructure.

Consider:

- SLF4J logger usage.
- Existing Logback implementation.
- HTTP request/response access logging through a filter or interceptor.
- MDC request correlation.
- Application event logging.
- Centralized exception logging.
- Retry and transaction-completion logging.
- Environment-specific logging levels.

Prefer straightforward, testable components over a custom observability framework.

Do not introduce new dependencies without demonstrating a concrete requirement.

### 4.4 Logging Event Matrix

Propose a reviewable event matrix.

For each relevant event define:

- Event name or identifier.
- Owning component.
- Trigger.
- Proposed log level.
- Safe fields.
- Fields explicitly excluded.
- Transaction/outcome semantics.
- Required tests.

At minimum evaluate:

1. Incoming HTTP request completion.
2. Unhandled technical exceptions.
3. Successful Order placement after confirmed commit.
4. Rejected Order placement due to missing/inactive resources.
5. Confirmed rollback eligible for retry.
6. Retry attempt scheduled.
7. Retry exhaustion.
8. Unknown or potentially committed transaction outcome.
9. Customer DELETE resulting in physical deletion.
10. Customer DELETE resulting in deactivation.
11. Already inactive Customer DELETE no-op.

Avoid logging expected business events more than necessary.

The matrix must remain **Proposed** until approved by the human.

### 4.5 HTTP Request Correlation

Propose a minimal request correlation contract.

Evaluate:

- Request-ID generation.
- Whether inbound request-ID headers are accepted.
- Header name and validation rules.
- Maximum identifier length.
- MDC key.
- Response correlation header.
- Cleanup on success and exception paths.
- Servlet async/error dispatch behavior.
- Propagation across explicit executor boundaries if relevant.
- Risks of logging raw URL paths and query parameters.

Do not silently choose unsettled public HTTP header behavior.

### 4.6 Transaction and Retry Safety

Examine the existing OrderPlacementCoordinator and accepted G1–G4 decisions.

Specify exactly when the system may log:

- Attempt started.
- Lock acquisition failure.
- Confirmed rollback.
- Retry scheduled.
- Retry exhausted.
- Commit confirmed.
- Commit outcome unknown.

Avoid any success log before successful TransactionTemplate completion.

Consider how to handle logging around failures without masking the original exception or interfering with transaction cleanup.

Do not propose logging customer/product state from unmanaged or stale entities.

### 4.7 Security and Data Minimization

Define explicit allowlisted diagnostic fields and prohibited content.

Cover:

- Secrets and credentials.
- Customer PII.
- HTTP request/response content.
- Exception stack traces.
- SQL logging and Hibernate bind parameters.
- Log injection.
- Request-ID validation.
- Operational retention and access-control considerations.

Do not expose private input through diagnostic messages.

### 4.8 Configuration

Propose minimal environment-specific configuration.

Consider:

- Default INFO level.
- Application DEBUG overrides.
- Framework logging noise.
- Console logging.
- Local development versus production.
- Whether plain-text or structured JSON logging is appropriate for the current project.
- Rotation and retention responsibility.
- Avoidance of unnecessary dependencies.

Treat JSON logging, distributed tracing, OpenTelemetry, metrics and dashboards as separate optional decisions unless there is a demonstrated current requirement.

### 4.9 Test Matrix

Define mandatory tests at appropriate levels:

**Unit**

- Log-level selection.
- Safe event data.
- Retry/outcome classification.
- Post-commit logging coordination where relevant.

**HTTP/MVC**

- Correlation identifier generation.
- Header behavior.
- MDC cleanup.
- Exception response preservation.
- No sensitive request-body logging.

**PostgreSQL Integration**

- Real successful commit logging.
- Rollback/retry behavior.
- Unknown commit confirmation.
- No misleading success events.
- No changes to accepted concurrency behavior.

**Architecture**

- SLF4J usage.
- No application `System.out` logging.
- No new dependency without approval.
- No sensitive logging patterns.
- No unintended API contract changes.

Identify concrete test owners or proposed test classes where useful.

Do not execute or implement these tests during planning.

### 4.10 Scope and Exclusions

Explicitly exclude:

- Business behavior changes.
- New REST business endpoints.
- Database migrations.
- Changes to monetary calculations.
- Changes to locking/retry policy.
- New idempotency/reconciliation mechanisms.
- New authentication features.
- External log aggregation services.
- Observability dashboards.
- OpenTelemetry or distributed tracing unless separately approved.
- Production deployment.
- Unapproved dependency or CI changes.

### 4.11 Human Architecture Decision Gates

Define explicit approval gates covering at least:

- L1: Framework and configuration.
- L2: Request correlation and HTTP contract.
- L3: Event taxonomy and log levels.
- L4: Transaction/retry outcome semantics.
- L5: Security and privacy controls.
- L6: Verification and acceptance criteria.

For each gate provide:

- Recommended decision.
- Alternatives where relevant.
- Consequences.
- Tests or evidence required.
- Status: Pending Human Decision.

Do not mark any gate Accepted without an explicit subsequent human decision.

### 4.12 Proposed File and Responsibility Matrix

Identify likely files and components affected by a future implementation.

Examples:

- Shared HTTP correlation filter/interceptor.
- Shared exception handling.
- OrderPlacementCoordinator.
- CustomerApplicationService.
- Application configuration.
- Relevant tests.
- AGENTS.md/coding guidelines.

Do not create placeholder classes or implementation files during this planning task.

### 4.13 Future Implementation Prompt

Provide a clearly labeled, non-executable draft or an outline for the future implementation phase.

It must state:

**Do not execute before explicit human architecture approval and separate implementation authorization.**

No implementation authorization is implied by creating the playbook.

## 5. Update the Documentation Index

Inspect:

`docs/04-agentic-coding/prompts/00-index.md`

Add the new Increment 6 entry using the existing index convention.

It must link to:

`07-application-logging-observability.md`

Display the status:

**Proposed — Pending Human Architecture Review**

Preserve the existing Increment 1–5 entries and their acceptance history.

Do not change Increment 5 from Reviewed – Accepted.

## 6. Update Current Project Planning

Review:

- `docs/07-implementation/implementation-plan.md`
- `docs/00-overview/open-items.md`

Add Increment 6 as a proposed technical cross-cutting increment.

Distinguish clearly:

- The functional MVP implementation is accepted through Increment 5.
- Logging and observability are proposed operational-quality improvements.
- Architecture and implementation approval are still pending.
- Spotless formatting is a separate ongoing quality task.

Preserve earlier decisions and chronological history.

Do not represent the existence of an Increment 6 planning document as permission to implement it.

## 7. Verification of Documentation Changes

This task is documentation-only.

Verify:

- All new links and anchors resolve.
- The index points to the correct playbook.
- The Increment 6 status is consistent across current documents.
- Increment 5 remains Reviewed – Accepted.
- ADR-0002 and ADR-0006 remain Proposed unless independently changed by an explicitly authorized decision.
- Existing historical prompts are preserved.
- Existing uncommitted Spotless changes are untouched.
- No production Java, test Java, Maven, CI or database files have been modified.

Run safe documentation/Git checks, including:

```bash
git diff --check
git diff --stat
git status --short
```

Account for untracked new files explicitly.

Do not run `spotless:apply`, builds, PIT, SonarQube or database tests.

## 8. Git Restrictions

Do not:

- Stage files.
- Commit.
- Push.
- Merge.
- Rebase.
- Switch branches.
- Deploy.
- Modify persistent databases.
- Delete or overwrite historical evidence.
- Discard unrelated working-tree changes.

A later human decision will authorize subsequent implementation and Git actions separately.

## 9. Final Deliverable

Provide:

1. Actual logging infrastructure found in the repository.
2. The new AGENTS.md requirements.
3. Increment 6 architecture summary.
4. Proposed logging event matrix.
5. Explicit unresolved L1–L6 gates.
6. Files created and modified.
7. Documentation links and status verification.
8. Confirmation that Spotless and Increment 5 changes were preserved.
9. Final Git status.
10. Recommended next step for human architecture review.

Do not claim implementation or verification of runtime logging behavior.

Final status:

**Increment 6 — Proposed, Pending Human Architecture Review**

Await explicit human approval before implementing application logging.
````


## Async HTTP Architecture Refinement — 2026-10-16

**L2 scope: Async HTTP support — Human Decided.** The human explicitly requires synchronous and asynchronous HTTP processing, supported Spring MVC request lifecycles and correlation on relevant request-owned application executor boundaries. Arbitrary independent background-job tracing is excluded. This scope decision does not authorize code, a new executor framework, tracing platform or a new business endpoint.

**L2 Async HTTP Scope — Human Decided; Detailed Architecture Pending Human Review.** Overall Increment 6 remains **Proposed — Pending Human Architecture Review**. L1, L3, L4, L5 and L6 remain **Pending Human Decision**. The initial proposal and immutable planning request above are preserved. This section supersedes the earlier sync-only/unsupported-async alternatives, omission of relevant worker propagation, filter-owned terminal emission and corresponding H2/file-scope sketches. It is an architecture refinement, not an implementation prompt or runtime verification claim.

### Verified Repository and Framework Context

Inspection remains on `develop` at `9345fa122df05fd5850ae183834f1b5c141cf3f7`. Existing uncommitted AGENTS/index/planning updates are preserved. POM and existing report classpaths identify Boot 4.1.1, Spring Web/MVC 7.0.9 and embedded Tomcat 11.0.26; the installed Tomcat JAR manifest identifies its Jakarta Servlet packages as specification 6.1. SLF4J is 2.0.18. Local `javap` inspection confirms AsyncContext/AsyncListener/ServletRequestListener APIs without compiling or running project code.

No production Callable/DeferredResult endpoint, application executor, async configuration or custom filter registration was found. Current endpoints are synchronous; this is a repository fact, not permission to drop async support. `ApiWebConfiguration` currently supplies only validation interception and request conversion. Boot documents an auto-configured MVC AsyncTaskExecutor when no custom executor overrides it; the actual running executor instance/configuration was not inspected in this task. Future wiring must confirm it rather than assume a pool, virtual-thread mode or limits.

Verified public semantics and their design implications:

| API / source | Relevant fact | Consequence for this proposal |
| --- | --- | --- |
| [OncePerRequestFilter, Spring 7.0.9](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/web/filter/OncePerRequestFilter.html) | Async/error participation needs dispatcher registration and appropriate hooks; nested ERROR differs from later ERROR | Explicit REQUEST/ASYNC/ERROR mapping, async support, async/error skip overrides and nested-error scoping; no assumption that the class name provides logical-request completion |
| [Servlet 6.1 specification](https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1.html) and [AsyncContext](https://tomcat.apache.org/tomcat-11.0-doc/servletapi/jakarta/servlet/AsyncContext.html) | Dispatch and completion are container operations; isAsyncStarted is not a final-response predicate; repeated startAsync creates another cycle and listener registration must follow it | Initial filter exit/false isAsyncStarted never alone claims final success. Register observation at startAsync and re-register at onStartAsync, not only after the chain returns |
| [AsyncListener](https://tomcat.apache.org/tomcat-11.0-doc/servletapi/jakarta/servlet/AsyncListener.html) | onTimeout/onError precede container handling; onComplete follows processed completion | Timeout/error record observations, not terminal success/failure or a replacement response; completion captures metadata without another HTTP log owner |
| [ServletRequestListener](https://tomcat.apache.org/tomcat-11.0-doc/servletapi/jakarta/servlet/ServletRequestListener.html) | Request scope entry/exit has public listener callbacks | Prefer requestDestroyed as the single finalization boundary for this application, rather than competing filter/async/error loggers |
| Tomcat 11.0.26 [sync scope exit](https://github.com/apache/tomcat/blob/11.0.26/java/org/apache/catalina/core/StandardHostValve.java) and [async completion](https://github.com/apache/tomcat/blob/11.0.26/java/org/apache/catalina/core/AsyncContextImpl.java) | Reference-container source orders synchronous destruction after error handling, and async completion listeners before request destruction | Compatibility evidence for the selected boundary, not permission to call Tomcat internals. Real-server tests must prove it in this application; porting/upgrading requires revalidation |
| [Spring MVC async processing](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html) and [WebAsyncManager 7.0.9](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/web/context/request/async/WebAsyncManager.html) | Callable runs on an AsyncTaskExecutor; DeferredResult is produced separately; both resume via dispatch and existing exception resolution | Retain Spring's async coordinator/resolvers and result/error behavior. Decorate relevant task execution, not business results or transaction contexts |
| [CallableProcessingInterceptor](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/web/context/request/async/CallableProcessingInterceptor.html) and [DeferredResultProcessingInterceptor](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/web/context/request/async/DeferredResultProcessingInterceptor.html) | Hooks have different caller/worker/container threads; Callable post-processing can occur after HTTP completion; hook exceptions may affect processing | Do not clean a worker's MDC from a container callback or use DeferredResult hooks as producer-thread propagation. Prefer lexical task scopes; observational hooks, if needed, preserve default timeout/error decisions |
| [Boot 4.1.1 task execution](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html) and [Logback MDC](https://logback.qos.ch/manual/mdc.html) | Executor selection is configurable; MDC is thread-associated | Do not replace the configured execution policy or copy a whole MDC map. Integrate only owned requestId on explicit request-related task boundaries |

These are document/API/source observations. No claim is made that the proposed filter/listeners/executor adapter have been implemented or demonstrated at runtime. The Tomcat Servlet API web pages may display a newer 11.0 patch; the inspected local manifest and version-tagged source establish the project's actual reference version.

### Preferred Minimal Components and Registration

Choose a small application-local design using supported APIs, with no new dependency or executor pool:

1. A `ServletRequestListener` creates `RequestLogState` and the server UUID in `requestInitialized`, before application filters; stores it under one private request attribute. It alone invokes terminal finalization in `requestDestroyed`. Registration is explicit once through Boot servlet-listener facilities. No session, static registry or database state.
2. `RequestCorrelationFilter` binds/restores owned MDC around each REQUEST/ASYNC/ERROR dispatch, sets the proposed response header when possible and records bounded dispatch/status observations. Explicit async-supported filter registration surrounds MVC, including early failures; configuration must keep the whole relevant chain async-supported. OncePerRequestFilter hooks include nested ERROR. It never emits a terminal event from finally.
3. One request-state AsyncListener records cycles, timeout/error/abort observations and onComplete status. It never dispatches, completes, cancels, sends errors, chooses a timeout response or emits a terminal event. Registering only in filter finally risks missing a fast completion.
4. A metadata-only request wrapper delegates both startAsync overloads unchanged and attaches the listener immediately after successful first startAsync, before returning its context to downstream code. It never reads/wraps/caches bodies, rewrites parameters or changes supplied request/response semantics. Later cycles re-register in onStartAsync before returning. Use a cycle generation, not AsyncContext object identity (a context can be reused), to avoid duplicate registration by the wrapper and callback. Preserve underlying startAsync exceptions. This narrowly scoped wrapper is for lifecycle observation only and requires focused wrapper/registration tests.
5. A small owned-key MDC scope helper and a task-context adapter support workers; the safe event emitter is shared by these technical components only where repetition warrants it. No observability framework or blanket AOP.

The filter-only alternative is rejected because synchronous unwinding can precede ERROR handling and async completion. Emitting in both filter finally and onComplete with just an AtomicBoolean can freeze an intermediate 200 before error handling; deduplication alone is insufficient. The public request-scope exit callback gives one terminal arbiter, while AsyncListener provides intermediate facts. No private Tomcat method is invoked. Required H/P tests make ordering and registration failures blockers for implementation acceptance, rather than a silent dependency on source inspection alone.

### Request-ID Contract — Still Proposed

Create one new server UUID in requestInitialized, canonical lowercase ASCII, exactly 36 characters. Ignore all inbound X-Request-ID values, including duplicate, oversized or hostile values; do not echo them or add validation errors. Reuse the request attribute across same-application REQUEST/ASYNC/ERROR cycles; never derive identity from a thread or a client's header. MDC key `requestId`; proposed response header `X-Request-ID`. No CORS exposure, JSON field, status, Location, Allow or Content-Type change is implied.

The filter sets/reasserts that same header before downstream processing on every participating dispatch while the response is uncommitted. Existing ApiErrorResponseWriter uses resetBuffer, which retains headers. A later complete response reset or container handling can remove headers, so reassert after such processing only while mutable; test ERROR and async paths. Do not wrap bodies or modify flush/commit behavior merely to guarantee a header. If already committed, connection lost, or failure outside the registered boundary, do not force a header, rewrite the response or promise its delivery. Cross-context dispatch, protocol upgrades and arbitrary background-job tracing are outside this same-application HTTP contract and require separate design if introduced.

### State, Concurrency and Final-Status Ownership

RequestLogState contains only server requestId, monotonic start, allowlisted method/template, cycle/dispatch bookkeeping, bounded flags/categories, an immutable latest status observation with provenance, a terminal-event claim and a separate diagnostic-owner claim. Never store a request body, raw exception graph, entity or transaction resource. No callback retains live Servlet objects beyond their valid lifecycle; worker context handles contain only the ID and lifecycle-closed token, not HttpServletRequest/Response.

Use a short synchronized state transition (or one atomic immutable-state CAS loop) for all observations and final snapshot creation. Terminal closure and event claim occur in the same transition. Do not hold the state lock while calling a logger, MDC provider, application code, response method or executor. Capture external metadata first while valid, then publish bounded observations. A winner seals one immutable terminal snapshot, releases the lock and attempts emission once; callback updates after closure cannot reopen or replace it. Logger failure does not reset the claim or trigger a second attempt. Distinguish at-most-one emission attempt from guaranteed log persistence.

Capture status via public getStatus during valid dispatch unwind and async onComplete, tagged with cycle and dispatch nesting. A later ERROR response supersedes an earlier provisional status; an outer/nested unwind must not overwrite a resolved newer observation with stale data. Do not read a recycled response in requestDestroyed: it uses the already captured snapshot. Status is considered established only by an authoritative final normal/ERROR dispatch or completed async response with no unresolved error/abort or observation gap. Within a cycle, completed-async observation outranks dispatch unwind; resolved ERROR outranks the earlier REQUEST/ASYNC success candidate. Cycle generation and a dispatch/error-resolution token reject stale outer or earlier-cycle updates. Conflicting observations that cannot be ordered safely degrade to UNKNOWN rather than last-writer-wins success. Otherwise record UNKNOWN; never inherit an initial/default 200 as proof. Missing metadata does not justify inspecting an unsafe object.

A timeout/error invalidates earlier success candidates. A later normal/ERROR redispatch can establish recovery and a newer final status. onComplete can establish an otherwise normal direct async completion, but must not turn an unresolved timeout/error plus a leftover default 200 into recovered success: prefer UNKNOWN without positive recovery evidence. A reliable final non-2xx status may still be reported with its observed error/timeout category. Ordinary server completion reports no observed delivery failure, not proof of client receipt; UNKNOWN/ABORTED distinguishes observable uncertainty rather than pretending every normal response was acknowledged.

requestDestroyed is the final observation barrier on the supported reference container: normal error resolution and completion observations precede it. It marks CLOSED and owns one terminal event even when the final status is UNKNOWN. No dispatch, Spring completion interceptor or AsyncListener independently emits a terminal event. Real-container acceptance must verify scope destruction occurs once and after the needed observations; no synthetic fallback claims HTTP success if the lifecycle contract cannot be established.

### HTTP Terminal State Machine

State is lifecycle plus sticky observation flags, not a single scalar that loses an earlier timeout or error. Final outcomes are COMPLETED, COMPLETED_AFTER_TIMEOUT, COMPLETED_AFTER_ERROR, ABORTED or UNKNOWN; “completed” means server lifecycle completion, never acknowledged remote delivery or committed business success. A recovered timeout/error with final 2xx remains a flagged WARN completion, not a plain successful INFO. Unresolved error/timeout suppresses a definitive completed-success claim even if an old status was 200. Proposed event names/levels remain subject to L3.

| Observation | State transition / metadata | Terminal action |
| --- | --- | --- |
| Initial requestInitialized / REQUEST | Create ACTIVE state once; bind MDC only for dispatch; set header before commit | None |
| Normal sync return, including handled 400/404/500 or early validation/unmapped route | Record final-candidate status/template; leave state active until request-scope exit | None in filter finally |
| startAsync / onStartAsync | ACTIVE → ASYNC_WAIT; increment cycle and attach/re-attach listener once per cycle | None; no success at initial return |
| ASYNC redispatch | ASYNC_WAIT → DISPATCHING; same ID and new per-thread scope; may return to ASYNC_WAIT for another cycle | Record only, not terminal |
| ERROR redispatch | ACTIVE/ASYNC_WAIT → ERROR_HANDLING; preserve flags and ID; final error rendering supplies newer status | Record only, no second diagnostic if already claimed |
| Nested ERROR | Stack/nesting-aware ERROR observation within current dispatch; restore enclosing MDC on return | Never log completion during nested unwind |
| onTimeout | Set timeoutObserved, remain waiting/handling; framework may recover, dispatch or complete | No immediate terminal event or new timeout response |
| onError | Set errorObserved/transport category, remain waiting/handling; existing container/MVC processing continues | No immediate terminal claim or duplicate exception logging |
| onComplete, including another thread | Mark async completion observed; capture valid final status and close cycle, preserving earlier flags | No emission; requestDestroyed remains sole arbiter |
| Observable abort/disconnect/cancellation | Set bounded transport-abort or unresolved-cancellation observation; preserve status only if reliable | At scope exit: ABORTED or UNKNOWN, never infer rollback or remote delivery |
| requestDestroyed after supported completion/error handling | Seal CLOSED snapshot using all prior observations; atomically claim once | Emit exactly one application-owned terminal attempt: completion/flagged completion/ABORTED/UNKNOWN |
| Unexpected observation/callback failure | Record OBSERVATION_INCOMPLETE if safely possible; preserve original application/Servlet flow | At scope exit prefer UNKNOWN over invented status; protect callback/emitter from ordinary RuntimeExceptions |
| Duplicate/late callback after CLOSED | Do not reopen state, mutate response or emit again | None; cannot revise a previously emitted event |
| No observable scope termination (e.g. process failure or indefinitely active async request) | Remains unclosed/unobserved | No fabricated completion; explicitly missing terminal evidence |

State operations linearize concurrent notifications. Error/timeout facts observed before final closure cannot be lost by racing completion, and terminal/diagnostic claims cannot both be reset by loser paths. The reference lifecycle must not deliver a valid earlier-cycle failure after logical scope destruction; adversarial late notifications are ignored after closure. This is not an ability to predict future disconnects. Test valid callback races and deliberately late callbacks separately. No artificial timeout, new cancellation policy, business retry or forced complete/dispatch is introduced to manufacture a terminal log. Do not equate every IOException, cancelled Future or onError with a proven client disconnect: absent a verified transport-specific signal, use UNKNOWN rather than claiming a remote abort.

### Owned MDC and Worker Propagation

Only `requestId` is modified. On entering a filter, listener callback, task or diagnostic emission scope, save the prior owned-key value in that invocation's local scope, bind the known canonical server ID, and restore the prior value or remove the key in finally. Nested same-thread ERROR restores the enclosing request scope. Top-level servlet/worker tasks remove their own ID when no enclosing scope exists. Unrelated MDC entries remain untouched; do not copy the whole map, call MDC.clear or use InheritableThreadLocal. A previous legitimate outer value is restored; reused-thread tests must show no new request-owned value is left behind. Pre-existing unrelated context corruption is not fixed by indiscriminate clearing.

Preferred Callable propagation: configure Spring MVC through AsyncSupportConfigurer to use a narrowly scoped delegating AsyncTaskExecutor adapter around the already selected Boot-managed executor. The adapter reads the private request attribute through currently bound ServletRequestAttributes/RequestContextHolder on the submitting request thread, captures only the known request ID and closed token, and wraps Runnable/Callable execution in the same finally-safe MDC scope, and forwards execute/submit/completable operations and returned futures to the delegate. No new pool, scheduler, queue policy, timeout or cancellation behavior; do not register an extra default Executor bean that changes Boot auto-configuration. Actual executor selection and wrapper coverage, including per-request WebAsyncTask executor overrides if used, must be proven. Such overrides require the same explicit integration rather than an untrue propagation claim.

Callable returns and exceptions flow unchanged through WebAsyncManager and existing resolvers. A CallableProcessingInterceptor is a supported alternative, but the preferred adapter keeps binding/restoration lexical around actual work and avoids confusing container afterCompletion with worker termination. Do not rely on an afterCompletion callback on another thread to clean the worker's MDC. MVC interceptors are not required merely to duplicate listener signals; if an observational hook is needed, it must preserve RESULT_NONE/default continuation and never select a result or exception.

DeferredResult completion does not imply its producing thread inherited request context. For request-owned producer work, capture a minimal correlation handle while still on the request/submission path and wrap the producer/completion Runnable or Callable on its existing executor (or explicitly enter the same scope in an existing external callback). The protected region includes producing the result and calling setResult/setErrorResult; always restore on normal return, exception or interruption. Do not create a new executor or globally decorate unrelated jobs. Request-owned AsyncContext.start(Runnable) work uses the same explicitly captured Runnable scope; container scheduling alone does not propagate MDC. Test that path without proxying AsyncContext or altering its dispatch/complete semantics. DeferredResultProcessingInterceptor/onCompletion can observe request lifecycle on container threads; they cannot retroactively correlate arbitrary producer work. A producer/callback that has not been wrapped has no propagation guarantee and must be integrated before claiming that request path supported.

After request closure, reject reuse of captured correlation for newly started work; already-running request work retains its scope until its own finally executes, even if the HTTP timeout/cancellation happened first. Late setResult failure must not revive the request or emit a second terminal event. Logging cannot forcibly clean another running thread or guarantee a task stops on cancellation; that is an explicit lifecycle limit, not a new cancellation mechanism. No transaction, EntityManager, request object, unrelated MDC field or security credential propagates with the correlation handle. Submission without a current owned request state does not copy an arbitrary MDC value; explicitly pass the captured handle for nested request-owned producer work outside the servlet thread. A failing MDC provider can make correlation unavailable: preserve the application result, mark observation incomplete where possible and report the loss, without promising provider-independent cleanup.

### Error Handling, Diagnostics and Privacy

Current ApiRequestInterceptor validation, ApiExceptionHandler classification, ApiErrorController fallback and ApiErrorResponseWriter rendering remain the owners of their existing behavior. Correlation components only observe and add the proposed header; they never call sendError, reset a body, change resolver precedence, consume input or translate a new exception. Status observations do not change safe JSON/Allow/Location/Content-Type semantics. Order transaction completion/retry classification remains independent of all HTTP flags.

| Event responsibility | Proposed owner and deduplication |
| --- | --- |
| Terminal HTTP completion/abort/unknown | Request-scope listener only, through the sealed terminal claim; HTTP abort is a terminal variant, not a second terminal event from the filter |
| Unexpected technical exception | Existing advice; fallback only if no earlier diagnostic claim. Preserve the earlier coordinator-owned Order terminal-category proposal; no new cross-feature HTTP dependency |
| Async timeout | AsyncListener records only; at scope exit the coordinator emits one safe WARN timeout diagnostic only if advice/fallback has not already claimed that failure |
| Async failure | Listener records category only; advice/fallback owns resolved technical errors; scope-exit coordinator claims a bounded pending diagnostic only if still unowned |
| Transport abort | Terminal ABORTED/WARN variant; no extra raw stack trace, no duplicate filter/error-controller diagnostic |

Use one atomic diagnostic-owner slot separate from terminal emission. Advice/fallback can claim a technical diagnostic when translating an actual failure; listener callbacks never compete to log the same exception early. At terminal finalization, emit at most one still-unowned pending timeout/async diagnostic. If multiple bounded categories were observed, preserve flags but select one primary category deterministically (transport abort → async error → timeout), not callback arrival order. Detailed L3 severity/taxonomy and interaction with coordinator-owned Order events remain pending review; no accepted classifier/error-envelope change is implied.

Safe terminal fields are fixed event ID, generated request ID, allowlisted method/template or OTHER, reliable status or UNKNOWN, monotonic duration and fixed outcome category. No Authorization/Cookie, customer PII, secrets, resource IDs, raw URI/query, body, SQL bind values, unreviewed exception message/stack or attacker-provided request ID. Parameterized logging is not sanitization. Application-owned deduplication does not suppress every framework/server log; review framework privacy separately under L5.

A failing appender must not throw from filter/listener/task-finally into the Servlet/application flow, change the HTTP state machine, override a result/exception, or trigger retry/dispatch. Seal state before emission and isolate ordinary logging/metadata RuntimeExceptions; do not catch arbitrary business work, recursively log emitter failure, reset an emission claim or claim protection against JVM-fatal failures. Test logger/MDC failure paths explicitly. Exactly one emission attempt is the goal when an observable final callback and functioning application execution permit it; durable or exactly-once collector delivery is not promised.

### Revised Mandatory H/P Evidence — Not Executed

These additions supersede the initial H2 sync-only/optional-ASYNC wording and extend H1/H3/H4. Existing U1–U3/P1–P4/A1 and Increment 5 regressions remain mandatory for applicable future implementation. Use current dependencies, test-only endpoints/servlets and the existing random-port embedded-server approach; do not add production endpoints or change business contracts to obtain async coverage.

| ID / proposed test owner | Required cases and assertions |
| --- | --- |
| H5 — RequestCorrelationFilterTest | Sync 2xx, expected 400/404, unexpected 500, unmapped route, pre-controller validation; canonical server ID/header, ignored hostile/multiple/oversized inbound IDs, safe JSON unchanged; owned-key cleanup/unrelated-key preservation |
| H6 — proposed AsyncHttpCorrelationTest | Callable success/exception and DeferredResult success/setErrorResult; worker and ASYNC dispatch share ID; no terminal event when initial dispatch returns; actual final status and one terminal attempt |
| H7 — same owner | More than one async cycle, immediate completion, completion on another thread; listener registration/re-registration once per cycle, no AsyncContext identity shortcut; REQUEST/ASYNC/ERROR and nested ERROR stable ID |
| H8 — same owner + ApiErrorDispatchIT/ConfiguredErrorPathIT | Timeout→completion, timeout→ERROR, error→completion; concurrent timeout/error/completion observations; final snapshot retains flags, no plain successful INFO after observed error/timeout; deterministic category and diagnostic ownership |
| H9 — proposed RequestCorrelationContextTest | Callable worker scope, explicitly wrapped DeferredResult producer and AsyncContext.start work; task submission vs execution, two unrelated sequential requests on the same threads, nested previous-key restoration, no unrelated MDC copying; exception/interruption/cancel-before-start and cancel-while-running cleanup; late producer cannot reopen CLOSED |
| H10 — same async owner | Response already committed, missing/changed header after reset, observable real or injected transport abort, unresolved status → UNKNOWN/ABORTED; never rewrite committed output or infer client delivery |
| H11 — proposed RequestLogStateTest | Race multiple callback threads and duplicate/late destruction; exactly one atomic terminal claim, separate single diagnostic claim; no premature emit in timeout/error/filter-finally; no event if no final lifecycle signal; flags cannot be lost before closure |
| H12 — shared logging safety tests | Failing appender/metadata/MDC hooks, hostile ID/header/body/PII/query canaries; only allowlisted fields; original exception/result/status/cancellation preserved, no recursive logging or duplicate diagnostic |
| P5 — random-port async tests without database where possible | Actual embedded Tomcat REQUEST/ASYNC/ERROR/nested ERROR, completion/destruction ordering, asyncSupported registration for the whole chain, managed MVC executor selection; MockMvc alone is insufficient |
| P6 — disposable PostgreSQL OrderApiIT/OrderPlacementConcurrencyIT | Correlated test-only async invocation with accepted transaction boundary: post-commit Order success vs HTTP completion distinguished; real rollback/retry/unknown commit unchanged, no context/resource propagation or false success |
| P7 — disposable PostgreSQL CustomerApiIT/concurrency tests | Logging/correlation does not change DELETE physical/deactivate/no-op behavior or either first-order race winner; MDC/terminal handling cannot interfere with post-commit observations |
| P8 — reference-container safety fixture | Bounded disconnect/timeout/error/reset scenarios and missing-final-notification simulation; demonstrate what is observable, explicitly record nondeterministic/unobservable delivery limits, no assumed detection of every client disconnect |

Use latches, barriers and bounded Future/HTTP timeouts with explicit milestones (async entered, producer released, redispatch observed, scope destroyed, worker restored); no arbitrary sleeps or test-order dependencies. Disconnect tests distinguish controlled failure injection from proof about actual networks. Callback races need deterministic state-unit tests plus real-container sequencing evidence. Test appenders/listeners restore logger/thread state and own only test resources. No tests are executed in this documentation task, and no new framework/library is approved.

### Remaining Detailed L2 Decisions and Limitations

Human approval is still required for: the generated UUID/header/MDC contract; explicit filter/listener registration and ordering; the requestDestroyed terminal arbiter and metadata-only startAsync wrapper; executor adapter selection and all supported producer boundaries; status provenance/UNKNOWN/recovered-timeout categories; callback failure protection; required reference-container evidence. L3 event ownership/levels, L4 transaction logging, L5 privacy/operational controls and L6 complete acceptance remain independently pending. Async support itself is already decided and must not be offered as optional.

The scope covers same-application Servlet async and relevant Callable/DeferredResult worker execution. Other executor overrides must be explicitly integrated; arbitrary jobs, cross-context forwarding, protocol upgrades, distributed tracing and durable audit delivery are not implicitly supported. Process termination, unobservable disconnects, missing terminal callbacks and non-cooperative workers can prevent complete evidence. Neither response commitment nor HTTP 2xx proves remote delivery, and HTTP completion never proves DB rollback/commit. No new HTTP timeout/error taxonomy, transaction replay or operational recovery feature is selected.

Only this playbook is refined by the targeted review. Other previously uncommitted documentation is preserved, as are the initial planning proposal and immutable request (apart from the expressly updated active L2 summary and visible historical-precedence notice). No Java/configuration/POM/test/CI/migration or accepted Increment 5 record change; no implementation prompt, build, test, formatter, PIT, SonarQube or database execution, staging, commit, push or branch operation is performed.


## Final L2 Lifecycle Design Review — 2026-10-16

**L2 Lifecycle Design — Reviewed, Pending Human Approval.** Overall Increment 6 remains **Proposed — Pending Human Architecture Review**. Async HTTP scope remains Human Decided. This bounded review selects one preferred pair of mechanisms; it does not accept L2, reopen L1/L3–L6, authorize implementation or replace the earlier proposal/history. Where inconsistent, the qualifications below supersede the earlier lifecycle descriptions.

| Decision | Selected approach | Main justification | Remaining verification |
| --- | --- | --- | --- |
| Terminal boundary | **A — Retain requestDestroyed as sole terminal owner** | One public request-scope exit callback; selected Tomcat orders it after ordinary application error handling and async completion observers. Avoids competing sync/async terminal emitters | F1/F2/F4: actual embedded-server ordering, status provenance and abnormal paths |
| Async registration | **A — Retain the narrowly scoped startAsync wrapper** | Uniform observation before downstream code receives the new context, including native Servlet async below MVC; Spring-only hooks do not cover that scope | F3/F5/F6: overload transparency, pre-exposure registration, repeated cycles and observer failures |

### A. Terminal Boundary: Evidence and Limits

**Public contract:** ServletRequestListener reports application request-scope entry/exit. It is not a transport-delivery acknowledgment and provides no response object. Async timeout/error notifications are intermediate, not terminal. A container may continue through error handling before completing the request. These API facts do not themselves specify every vendor's callback ordering or prove final HTTP wire status. See [ServletRequestListener](https://tomcat.apache.org/tomcat-11.0-doc/servletapi/jakarta/servlet/ServletRequestListener.html) and [Servlet 6.1 lifecycle specification](https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1.html).

**Observed in Tomcat 11.0.26 source, not newly executed:** [StandardHostValve.invoke](https://github.com/apache/tomcat/blob/11.0.26/java/org/apache/catalina/core/StandardHostValve.java) invokes request destruction after its synchronous chain/error handling when not async. [AsyncContextImpl.fireOnComplete](https://github.com/apache/tomcat/blob/11.0.26/java/org/apache/catalina/core/AsyncContextImpl.java) notifies completion listeners, then invokes request destruction in finally before clearing its Servlet references. Its timeout/error paths notify observers before possible error rendering/completion. Normal supported sync, implicit async completion after redispatch and explicit async completion therefore have a destruction path; no ordinary successful async path without that callback was established in this inspection. These are reference-version observations, not a portable promise about another container or every fault path.

An unhandled synchronous exception may cause later ERROR dispatch before destruction on this Tomcat path; nested ERROR must finish unwinding before the owner seals state. Async error/timeout may be handled, dispatched, recovered or completed; neither notification triggers finalization. Registration through Boot's `ServletListenerRegistrationBean` delegates to public `ServletContext.addListener` (verified from the existing Boot 4.1.1 JAR API/bytecode). It does not wrap callbacks in a different lifecycle or turn startup ordering into a universal callback-order guarantee. Register once, without duplicate annotation registration; verify listener order relative to other application listeners in F1/F2.

**Selected interpretation:** “terminal” means the observed end of this application's request scope. It does not guarantee that all connector flushing or transport error detection has finished. Destruction can precede a later transport failure or an uninstrumented container status change; it can also arrive without a usable final response snapshot. Abort/error with disabled I/O may prevent error rendering. Unknown transport/status observations require UNKNOWN/ABORTED, not an inferred 200, rollback or remote receipt. Shutdown, forced termination, initialization/listener failures and unobservable connection loss may prevent a usable callback altogether; no terminal log is fabricated in their absence. Graceful shutdown behavior is a test obligation, not guaranteed by the observed normal path.

**Reliable status conditions:** Use only a valid dispatch-time observation of a completed normal/resolved ERROR/ASYNC response, or an immutable committed-status snapshot captured while legally processing that response. It must belong to the current cycle/resolution token, survive the existing stale-observation rules and have no unresolved error/abort or observation gap. Mere request destruction, isAsyncStarted=false, response commitment or a default status does not establish successful delivery. If later raw async work can still change a provisional snapshot and no valid later dispatch observes it, final status is UNKNOWN. A transport abort may leave a known committed status, but its outcome remains ABORTED/UNKNOWN, never successful delivery.

**Correction to earlier status collection:** Do not plan to retrieve the final response through AsyncContext.getResponse in onComplete: the [Servlet 6.1 AsyncContext API](https://github.com/jakartaee/servlet/blob/6.1.0/api/src/main/java/jakarta/servlet/AsyncContext.java) restricts access after complete/dispatch. The explicit supplied-request/response listener overload also cautions against reading/writing those supplied objects after additional wrapping. Therefore onComplete records completion only; it is not an unconditional final-status sampling hook. requestDestroyed likewise reads only bounded stored state, never a retained/recycled response. Native direct async completion without a trustworthy status snapshot remains supported with UNKNOWN logging, not false precision. This qualifies the earlier onComplete status-sampling/table entries without adding another response/context wrapper.

### B. Async Registration: Necessity, Alternatives and Compliance

The wrapper is retained for a precise **before-context-exposure invariant**, not because Servlet mandates a wrapper or because every fast completion defeats filter registration. [AsyncContext.complete](https://github.com/jakartaee/servlet/blob/6.1.0/api/src/main/java/jakarta/servlet/AsyncContext.java) defers its effect/listener notification when called during the starting container dispatch; timeout accounting also begins after dispatch return. Registration in the original filter's outbound finally can therefore observe ordinary fast completion. The previous blanket rapid-completion rationale was too broad.

However, that is not proof of equivalent observation for every native asynchronous error path while downstream code/work is active. The bounded review did not establish a blanket public guarantee postponing all such error observations until filter unwind. Retaining the wrapper establishes registration before conforming downstream code receives/publishes the context and starts its request-owned work. This is a conservative design ordering, not a claim that an early-error race was reproduced. It is only valid if async start passes through the registered filter/wrapper; intentional unwrapping/bypass or uncontrolled concurrent access to an unexposed request/context is not magically instrumented and must not be claimed supported without explicit integration.

| Candidate | Assessment for the required scope |
| --- | --- |
| Filter-only outbound registration | Adequate for the specified deferred completion/timeout cases; no equivalent universal pre-exposure registration proof for all native error/work paths. Not selected as the sole observation mechanism |
| WebAsyncManager / CallableProcessingInterceptor / DeferredResultProcessingInterceptor | Supported MVC-specific observation points, but do not intercept arbitrary Servlet startAsync below MVC or automatically propagate producer MDC. Keep existing Spring processing; not a substitute for the scope-wide start observation |
| ServletRequestListener alone | Supplies initialization/finalization, not an async-start callback; cannot attach the first AsyncListener by itself |
| Wrapper + AsyncListener + request-scope owner | **Selected minimal hybrid:** public APIs, consistent start observation across MVC/native paths; carries a transparency and registration test burden |

Servlet compliance constraints, from [ServletRequest.startAsync contracts](https://github.com/jakartaee/servlet/blob/6.1.0/api/src/main/java/jakarta/servlet/ServletRequest.java):

- No-argument overload calls the corresponding underlying no-argument method, then observes its returned context. Do not silently convert it to the two-argument form with this wrapper/a cached response; that would change original/wrapped identity and potentially dispatch semantics.
- Two-argument overload passes the exact caller-supplied request/response unchanged. The container retains authority to validate that they are the in-scope objects or permitted wrappers; preserve its exceptions and hasOriginalRequestAndResponse behavior. Never unwrap, substitute, cache or later reuse those arguments for logging.
- Invoke underlying startAsync once and return the exact context, not a proxy. Delegate first; do not hide its IllegalStateException (unsupported async, invalid cycle, closed response or other contract violation). Observation errors after successful delegation must not replace the successful start with a logging failure: mark observation incomplete safely, with UNKNOWN later.
- Register the first observer immediately before returning the context, using addListener(listener); do not supply stale response objects for later status reads. Registration remains inside the initiating container dispatch. The whole applicable chain/Servlet must be async-supported; the wrapper cannot make an unsupported request legal.
- On each later startAsync, the prior observer re-registers itself in onStartAsync with event.getAsyncContext. Reinitialization clears old registrations; a context object may be reused. A request-owned cycle/registration token prevents the wrapper from registering a duplicate after that callback. No-op initial onStartAsync is not a substitute for attaching the first observer.
- Wrap each participating dispatch once; preserve/delegate request attributes and existing downstream wrappers. No body/input access, response wrapper, async-context proxy, timeout modification, forced dispatch/complete or error resolver change. Callbacks only publish observations and scope their owned MDC key; no terminal emission from them.

### Resulting Responsibilities and Cross-Check

The request-scope listener creates the UUID/state before application processing and alone claims terminal emission at destruction. The filter owns header/dispatch metadata and finally-safe per-thread MDC; the transparent start wrapper owns initial observer attachment; AsyncListener records cycle/error/timeout/completion and re-registers. Existing worker adapters retain the earlier Callable/DeferredResult/AsyncContext.start MDC design. No new task framework or business/transaction dependency follows.

All observers feed the same bounded synchronized/atomic state. Final closure and event claim remain one transition; emission occurs outside the state lock, once even if the appender fails. No duplicate terminal event is emitted by a filter, Spring interceptor or async callback. Error/timeout observed before closure cannot become a fabricated success; missing/invalid status evidence degrades to UNKNOWN. Earlier independent diagnostic ownership, safe fields, request-ID/header proposal, thread cleanup and Increment 5 guarantees remain unchanged. No mechanism depends on making logging affect a business result, exception, cancellation or transaction replay.

### Focused Acceptance Evidence — Required, Not Executed

| ID | Required real-container or focused evidence |
| --- | --- |
| F1 | Register through Boot once; prove initialization before filter/work and one destruction after sync 2xx, handled 400/404/500, unhandled ERROR and nested ERROR. Verify final valid status vs an unobserved container fallback, without treating scope exit as delivery acknowledgment |
| F2 | Native Servlet and MVC Callable/DeferredResult: explicit/implicit completion, multiple cycles, timeout→completion/ERROR, error→completion and fast complete/dispatch before original return. Prove observer notifications and resolution precede destruction on Tomcat 11.0.26; no success at initial filter exit |
| F3 | Both overloads: exact returned context and original/supplied request/response identity, original/wrapped flags, existing nested wrappers, wrapper delegation and one underlying start call. Valid and invalid start cases retain original behavior; no observation-layer IllegalStateException becomes an API error |
| F4 | Committed response, unresolved exception, native completion with only provisional status, observable abort and graceful shutdown; UNKNOWN where no trustworthy snapshot exists. No getResponse after complete/dispatch, no supplied-object sampling, no recycled reference access; missing destruction yields missing evidence rather than invented completion |
| F5 | Listener attached before the start call returns; begin native worker activity immediately, exercise both overloads and repeated onStartAsync registration. Include error/complete races and duplicate wrapper/callback attempts. Use barriers/latches and bounded waits, not arbitrary sleeps; distinguish a source-order argument from demonstrated runtime coverage |
| F6 | Atomic duplicate/late callback tests; MDC cleanup on all participating threads; failing observer/appender preserves original start/result/exception. No terminal duplicate, raw exception/PII/payload logging, response-body/header-contract or transaction change |

These tests are implementation-acceptance prerequisites. If the actual registration/ordering or wrapper-transparency proof fails, report the precise blocker and revisit these two choices through human review; do not silently install another terminal owner or remove async support. No build, Servlet experiment or runtime test was performed for this review. Public API guarantees, selected-version source observations and remaining runtime assumptions are deliberately distinguished above.



## Current Human Architecture Approval and Execution — 2026-10-16

**L1–L6 — Human Architecture Approved, 2026-10-16.** The human separately authorized execution of the complete archived prompt. L1 uses existing SLF4J/Logback and bounded console metadata; L2 adopts generated UUID/X-Request-ID/requestId, REQUEST/ASYNC/ERROR, requestDestroyed ownership, transparent startAsync observation and explicit worker scopes with conservative UNKNOWN semantics; L3 adopts the event matrix and single diagnostic ownership; L4 requires confirmed Order completion and Customer DELETE post-commit observation without transaction-policy changes; L5 adopts the safe-field allowlist and separate framework/operational privacy limits; L6 requires the complete applicable U/H/P/A and F1–F6 verification. The final L2 lifecycle qualifications govern earlier inconsistent proposals.

All earlier Proposed/Pending statements describe the historical planning/review state; this current record supersedes them for L1–L6 only. Architecture approval and execution authorization do not constitute implementation acceptance. Increment 5 remains Reviewed – Accepted; complete ADR-0002 and ADR-0006 remain Proposed. No PIT execution is authorized.

**Execution ledger:** Three correction rounds used of three allowed. Round 1 corrected the test fixture's Boot LocalServerPort import; the sandboxed Mockito attachment failure was environmental and the unchanged rerun outside the sandbox passed five real-container tests. Round 2 declared ServletException in the expanded fixture; 58 tests then ran with one Mockito restubbing error. Round 3 used doReturn for the throwing stub, recorded async-completion/cycle metadata and added focused state/context evidence; the final targeted run passed 62 tests with zero failures/errors/skips. No fourth correction round is authorized. Full verification evidence and remaining acceptance gaps are recorded below after execution.

## Increment 6 Implementation Prompt — 2026-10-16

- **Status:** Recorded — Not Executed
- **Architecture:** L1–L6 Human Approved
- **Implementation authorization:** Granted for a separate subsequent execution
- **Implementation acceptance:** Pending
- **Execution restriction:** This documentation task does not authorize execution

````markdown
# Increment 6 — Implement Application Logging & Observability

## Role

Act as a Senior Java Software Engineer and Software Architect specializing in Spring Boot, PostgreSQL transactions, Jakarta Servlet asynchronous processing, concurrent request lifecycles, SLF4J and automated testing.

Implement the **human-approved Increment 6 — Application Logging & Observability**.

Prioritize correctness, maintainability, limited complexity and efficient execution.

Do not restart the architecture discovery process.

## 1. Authorization and Project Context

Repository:

`/Users/rookie/Documents/Development/Projekte/agentic-order-service`

Expected branch: `develop`

Primary authoritative playbook:

`docs/04-agentic-coding/prompts/07-application-logging-observability.md`

Read:

- `AGENTS.md`
- Increment 6 playbook, including the Async HTTP refinement and Final L2 Lifecycle Design Review
- Relevant Java/Spring and coding guidelines
- Transaction and locking conventions
- Testing strategy
- Existing HTTP error-handling and MVC configuration
- Existing Order and Customer application services
- Existing relevant unit and integration tests

The human has approved **L1–L6 as the target architecture**.

This authorization includes implementing the approved runtime logging, configuration, request correlation, synchronous and asynchronous HTTP support, and required automated tests.

The implementation is **not yet accepted**.

All runtime guarantees remain subject to verification.

### Existing baseline

- Java 21
- Spring Boot 4.1.1
- Maven Wrapper 3.9.9
- PostgreSQL
- SLF4J / Logback
- Spotless / Palantir Java Format
- Existing JaCoCo, SonarQube and PIT quality infrastructure

Increment 5 is already accepted.

Preserve all accepted Increment 5 G1–G6 decisions and behavior.

## 2. Confirm the Repository State

Before editing:

1. Verify branch and HEAD.
2. Inspect staged, unstaged and untracked changes.
3. Read the approved architecture.
4. Identify the minimal implementation files.
5. Confirm the existing logging dependencies and active Maven configuration.

Preserve all unrelated work.

Do not silently overwrite, revert or incorporate unrelated changes.

If the actual repository state or approved architecture differs materially from the expected context, stop and report the discrepancy.

## 3. Record Human Architecture Approval

Update the active decision records in the Increment 6 playbook.

Record:

**L1–L6 — Human Architecture Approved, 2026-10-16**

The approved decisions are:

### L1 — Framework and Configuration

- Use existing SLF4J and Logback infrastructure.
- No additional logging dependency unless a specific technical blocker is demonstrated and approved.
- Console logging with stable, bounded event metadata.
- Default INFO level.
- Targeted DEBUG configuration where appropriate.
- No unrestricted SQL bind logging, payload logging or framework suppression.

### L2 — Request Correlation and Async HTTP Lifecycle

- Support synchronous and asynchronous HTTP processing.
- Generate one server-owned UUID per logical HTTP request.
- Use response header `X-Request-ID`.
- Use MDC key `requestId`.
- Ignore incoming `X-Request-ID` values.
- Support REQUEST, ASYNC and ERROR dispatches.
- Preserve the same ID across supported dispatches and request-owned worker execution.
- Restore MDC on every participating thread.
- Use the selected `ServletRequestListener.requestDestroyed()` terminal boundary.
- Use the narrowly scoped `startAsync()` observation wrapper, subject to Servlet compliance and runtime evidence.
- Preserve conservative final-status provenance and UNKNOWN/ABORTED outcomes.
- Never confuse server lifecycle completion with confirmed client receipt.
- Never emit more than one application-owned terminal event attempt per observably terminated logical request.

### L3 — Event Taxonomy and Ownership

- Implement the approved event matrix and log levels.
- Avoid duplicate exception diagnostics.
- Expected 400/404 conditions must not automatically produce ERROR logs.
- Do not introduce entry/exit logs in every method.
- Keep event names stable and safe.

### L4 — Transaction-Aware Logging

- Only report committed Order creation after confirmed transaction completion.
- Use confirmed post-commit observation for Customer DELETE outcomes.
- Distinguish confirmed rollback, retry scheduling, retry exhaustion and unknown commit result.
- Preserve all accepted locking, retry and transaction policies.
- Logging failures must not change business outcomes or cause transaction replay.

### L5 — Security and Privacy

- Use the approved safe-field allowlist.
- Never log credentials, secrets, customer PII, full payloads, raw paths/queries, SQL bind values or unreviewed exception content.
- Treat framework-generated diagnostic output separately from application-owned logs.
- Do not claim a production retention or audit guarantee.

### L6 — Verification and Acceptance

- Implement applicable U1–U3, H1–H12, P1–P8 and A1 evidence described in the playbook.
- Use real embedded-server integration tests where Servlet lifecycle behavior cannot be proven by MockMvc.
- Preserve and rerun the existing required regression suite.
- Report actual test results and remaining limitations.

Record that architectural approval does not constitute implementation acceptance.

Preserve historical proposals, earlier prompts and decision history.

## 4. Implementation Strategy

Use three bounded implementation phases.

The phases are sequencing boundaries, **not permission to exceed the correction budget**.

### Phase A — Core HTTP Correlation

Implement the approved minimal components:

- Request-scoped correlation state.
- Server-generated UUID.
- Servlet request listener.
- Request correlation filter.
- Servlet async observation.
- Narrow request wrapper where required by the approved architecture.
- MDC scope management.
- Approved Callable and DeferredResult context integration.
- Safe terminal event emission.

Use established public Spring and Servlet APIs.

Do not invoke private Tomcat implementation methods.

Do not create a custom servlet container framework.

Do not introduce new executor pools or alter application timeout, queueing, scheduling or cancellation policies.

### Critical lifecycle requirements

The implementation must handle:

- Initial REQUEST.
- Normal synchronous completion.
- Early validation failures.
- Handled and unhandled exceptions.
- ERROR dispatch.
- Nested ERROR dispatch.
- Async request start.
- Async redispatch.
- Multiple async cycles.
- Async completion.
- Async timeout.
- Async error.
- Observable transport abort.
- Incomplete or unknown outcome.

A timeout or error callback is an observation, not automatically the final HTTP outcome.

Use a concurrency-safe state model.

Seal one immutable terminal snapshot before attempting its log emission.

Do not perform arbitrary application work or invoke logging while holding the state lock.

Do not read a response object after its valid lifecycle.

Do not assume the default HTTP 200 status is a verified final response status.

Do not fabricate a completed request when termination was never observed.

### Request wrapper requirements

Preserve the actual Servlet contract for:

- `startAsync()`
- `startAsync(ServletRequest, ServletResponse)`

Preserve applicable supplied request/response object semantics, wrapper legality, exception behavior and subsequent async cycles.

Attach observers without consuming request bodies, changing parameters or rewriting response behavior.

Ensure listener registration does not occur twice per cycle.

**If real-container evidence shows that the wrapper or requestDestroyed terminal design cannot meet the approved guarantees, stop and report the architectural blocker.**

Do not independently introduce a larger custom framework or substitute a materially different architecture.

### MDC requirements

- Modify only the owned `requestId` key.
- Restore previous same-thread values.
- Preserve unrelated MDC entries.
- Do not use `MDC.clear()` as a blanket cleanup mechanism.
- Do not use inheritable thread locals.
- Propagate only approved request correlation metadata.
- Never propagate an EntityManager, transaction context, security credential or live Servlet request through a correlation handle.
- Integrate existing Spring MVC async execution rather than replacing the executor policy.
- Explicitly scope DeferredResult producer work.
- Reject reuse of captured correlation for new work after request closure.
- Preserve completion/error behavior when correlation infrastructure fails.

## 5. Phase B — Application Events

Add the approved event logging to the existing application components.

### HTTP

Implement safe completion diagnostics and centralized unexpected-technical-failure diagnostics.

Maintain existing exception ownership and safe API responses.

Never duplicate a technical exception in the controller, service, advice and fallback layers.

### Order Placement

Implement:

- `ORDER_COMMITTED`
- `ORDER_REJECTED`
- `ORDER_ATTEMPT_ROLLED_BACK`
- `ORDER_RETRY_SCHEDULED`
- `ORDER_RETRY_EXHAUSTED`
- `ORDER_OUTCOME_UNKNOWN`

Use the approved levels, fields and ownership rules.

Do not modify:

- TransactionTemplate boundaries.
- Lock acquisition order.
- Retry classification.
- Retry count or timing.
- SQLSTATE interpretation.
- Unknown-commit handling.
- Business exceptions or response contracts.

A successful `save()`, `flush()` or transaction callback return is not proof of commit.

Only emit `ORDER_COMMITTED` after successful transaction completion.

Never replay an uncertain transaction because of a logging problem.

### Customer DELETE

Implement:

- `CUSTOMER_DELETED`
- `CUSTOMER_DEACTIVATED_BY_DELETE`
- `CUSTOMER_DELETE_NOOP`

Observe confirmed transaction completion.

Do not emit a successful event from inside the proxied transactional method before the transaction completes.

Do not change method signatures, HTTP contracts or persistence behavior merely to support logging.

Do not introduce additional persistence queries, entity refreshes or locks.

### Emission Failure Isolation

Protect the existing business flow from ordinary logging and appender failures.

A failing logger must not:

- Convert success into failure.
- Mask the original exception.
- Trigger a new Order retry.
- Alter transaction outcomes.
- Trigger servlet redispatch.
- Emit a duplicate terminal event.

Keep the protection narrow.

Do not catch arbitrary business failures or all `Throwable` values.

## 6. Phase C — Configuration, Tests and Verification

Add minimal Spring Boot logging configuration.

Preserve existing environment, database, Flyway and testing configuration.

Use the project's existing logging and test dependencies.

### Fast Tests

Cover:

- Correct event names, levels and safe fields.
- Expected versus unexpected errors.
- Post-commit versus pre-commit event timing.
- Rollback and retry outcomes.
- Unknown commit outcomes.
- Failing appenders.
- No duplicate technical diagnostics.
- MDC scoping and cleanup.

### HTTP Tests

Implement the approved H1–H12 requirements, including:

- Normal synchronous requests.
- Validation and fallback errors.
- Request-ID response contract.
- Hostile incoming headers.
- Callable success/failure.
- DeferredResult success/failure.
- Multiple async cycles.
- ERROR and ASYNC redispatch.
- Timeout and error recovery.
- Callback races.
- MDC cleanup across reused threads.
- Response already committed.
- UNKNOWN/ABORTED classifications.
- No sensitive diagnostic content.

Use test-only endpoints, servlets or fixtures where needed.

Do not add production business endpoints merely to test async behavior.

### Real Servlet Container Tests

Use the actual embedded Tomcat runtime.

Explicitly prove:

- Filter and listener registration.
- Request lifecycle ordering.
- Final status observation.
- requestDestroyed finalization.
- AsyncListener registration and re-registration.
- Both startAsync overloads.
- Wrapper transparency.
- Correct handling of ERROR redispatch.
- No premature terminal logs.
- No duplicate terminal logs.

MockMvc alone is insufficient for these lifecycle guarantees.

If a transport-delivery condition cannot be reproduced reliably, distinguish the limitation from a passed test.

### PostgreSQL Regression

Run applicable real PostgreSQL integration tests for:

- Confirmed Order commit.
- Lock timeout and retry.
- Retry exhaustion.
- Unknown commit result.
- Customer physical deletion.
- Customer deactivation.
- Customer no-op deletion.
- Existing concurrency races.

Do not alter established business behavior to satisfy logging tests.

Use disposable test resources, never persistent development data.

## 7. Mandatory Quality Gates

Before requesting acceptance, run:

```bash
./mvnw -B -ntp spotless:check
./mvnw -B -ntp validate
```

Then execute appropriate targeted fast tests and integration tests.

After the implementation is stable, execute the full verification using the confirmed Java 21 installation:

```bash
./mvnw -B -ntp clean verify
```

Inspect Surefire and Failsafe XML reports.

Report actual:

- Fast test executions.
- PostgreSQL integration test executions.
- Failures.
- Errors.
- Skips.
- JaCoCo coverage.

Compare results with the accepted baseline where meaningful.

Do not claim that a test passed merely because it existed before this increment.

### SonarQube

Inspect the repository's existing SonarQube workflow and configuration.

If an authorized, already available local analysis can be executed without external credentials, publication or unrelated setup, use it.

Otherwise document SonarQube as a separate pending verification gate.

Do not invent a clean SonarQube result.

Do not run PIT without separate authorization.

Do not modify CI workflows, quality thresholds or dependencies to make failing checks pass.

## 8. Strict Iteration Budget

This implementation has a maximum of **three correction rounds** after the initial implementation.

A correction round consists of:

1. Review concrete failures or findings.
2. Identify root causes.
3. Apply a bounded correction.
4. Run targeted verification.
5. Update the issue ledger.

Do not start a correction round without a concrete issue.

Do not perform an unlimited sequence of speculative improvements.

Track:

- Initial implementation.
- Correction round 1.
- Correction round 2.
- Correction round 3.

After round 3:

- If all hard gates pass, prepare the final report.
- If a hard gate fails, stop and report the unresolved blocker.
- Do not weaken acceptance criteria.
- Do not silently skip required tests.
- Do not launch a fourth correction round.

Use fast targeted tests during development and the full required regression suite before final acceptance.

A full verification failure must be diagnosed and corrected within the remaining budget.

## 9. Hard Stop Conditions

Stop and report immediately if:

- The approved Servlet architecture proves incompatible with the actual runtime.
- The request wrapper violates the Servlet contract.
- Request finalization produces false success after an observed failure.
- MDC leaks between unrelated requests.
- A logging failure changes an Order retry or business outcome.
- Existing HTTP contracts are broken.
- Previously accepted transactional behavior changes.
- A new dependency or material architecture change becomes necessary.
- A persistent database would need modification.
- The correction budget is exhausted with blocking failures remaining.

Do not work around such failures by weakening tests or changing the accepted architecture without human approval.

## 10. Documentation and Final Git Review

Update the Increment 6 playbook with:

- Implementation files and responsibilities.
- Actual test evidence.
- Verified runtime behavior.
- Known limitations.
- Quality gate outcomes.
- Outstanding SonarQube/PIT or operational issues.
- Correction round count.

Set implementation status to:

**Implemented — Pending Human Review**

only if the implementation and mandatory local verification succeed.

Do not mark Increment 6 fully Accepted.

Preserve Increment 5 history and ADR statuses.

Inspect:

```bash
git diff --check
git diff --stat
git status --short
```

Review source and configuration changes for unintended business behavior.

Do not stage, commit, push, merge, rebase or deploy.

## 11. Final Report

Provide a concise, evidence-based report.

### A. Implementation

- Components added.
- Existing components modified.
- Logging events implemented.
- Async lifecycle design actually realized.
- Any divergence from the approved architecture.

### B. Tests

- Fast test counts.
- Integration test counts.
- Servlet async test outcomes.
- PostgreSQL regression results.
- Coverage.
- Failing or skipped tests.

### C. Quality

- Spotless.
- Maven validate.
- Clean verify.
- SonarQube status.
- Security/privacy review.
- Remaining risks.

### D. Iterations and Efficiency

- Initial implementation outcome.
- Number of correction rounds.
- Main issues found.
- How they were resolved.
- Approximate elapsed execution time, if reliably available.
- Explicit unresolved findings.

### E. Git

- Current branch and HEAD.
- Changed and new files.
- Final working-tree status.
- Confirmation that nothing was staged, committed or pushed.

### F. Decision

Finish with exactly one appropriate outcome:

**Increment 6 — Implemented, Pending Human Review**

or

**Increment 6 — Implementation Blocked, Human Decision Required**

Do not claim implementation acceptance on behalf of the human.
````

## Implementation Execution Evidence — 2026-10-16

**Increment 6 — Implementation Blocked, Human Decision Required**

L1–L6 remain Human Architecture Approved. Implementation acceptance is Pending. Execution began under the separate human authorization following the archived prompt. The complete archived implementation request is unchanged. Initial repository state was `develop` at `9345fa122df05fd5850ae183834f1b5c141cf3f7`, with four modified documentation files and this untracked playbook only. Those changes were preserved and current Increment 6 status references reconciled. Nothing was staged, committed, pushed, merged, rebased or deployed.

### Implemented Responsibilities

- `configuration/RequestLoggingConfiguration`: one explicit Boot servlet listener/filter registration; async-supported REQUEST/ASYNC/ERROR; MVC adapter around the existing named `applicationTaskExecutor`, without creating a pool or changing scheduling policy.
- `shared/api/RequestLogState`, `RequestLoggingListener`, `RequestCorrelationFilter`: server-owned UUID, response X-Request-ID, bounded synchronized metadata, dispatch status observations, initial/later-cycle async observation, separate diagnostic and terminal claims. requestDestroyed alone seals and emits terminal metadata; no response access from onComplete or destruction. Native direct async completion without a trustworthy final dispatch remains UNKNOWN. Completed async cycles are recorded explicitly.
- `shared/api/RequestCorrelationContext`, `CorrelatedTaskExecutor`: owned-key MDC restoration and explicit Runnable/Callable scopes; MVC Callable integration and explicit DeferredResult/AsyncContext.start producer wrapping. Closed handles do not rebind correlation for newly started work. No request, entity, transaction or credential is copied into worker context.
- `shared/logging/SafeEvent`: narrow RuntimeException isolation around emission; no recursive diagnostic or Throwable catch.
- `ApiExceptionHandler`, `ApiErrorController`, `HttpFailureDiagnostic`: fixed safe unexpected-failure category, shared request diagnostic claim, expected 4xx excluded. `OrderFailure` carries coordinator diagnostic-attempt ownership, not a durability guarantee.
- `OrderPlacementCoordinator`: ORDER_COMMITTED after template completion; expected rejection, confirmed transient rollback, actual retry scheduling, exhaustion and UNKNOWN observations. The accepted classifier, lock order, transaction boundaries, retry limits/delays and SQLSTATE interpretation are unchanged.
- `CustomerApplicationService`: capture physical-delete/deactivate/no-op branch and emit only from afterCommit, including outer transaction participation; no additional query, lock or entity access from the callback.
- `application.yml`: console requestId visibility and root/application INFO; database/Flyway settings unchanged.
- Tests: new `HttpRequestLoggingTest`, `RequestLogStateTest` and test-owned `EventCapture`; extended `OrderPlacementCoordinatorTest`, `CustomerDeletionTest`, `OrderPlacementConcurrencyIT`. Existing assertions were preserved.

Java source paths above are relative to `src/main/java/com/example/agenticorderservice/`; test classes are under corresponding `src/test/java/` packages.

### Actual Verification

All Maven commands used command-local `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` and `./mvnw -B -ntp`. Environment inspection reported Java 21.0.6 and Maven 3.9.9. Mockito attachment and embedded-server/Testcontainers execution required running outside the filesystem/process sandbox; the initial sandbox attachment failure is not runtime compatibility evidence.

| Executed command / evidence | Actual result |
| --- | --- |
| `spotless:apply` during authorized implementation | Formatted changed/new Java; final diff reviewed for unrelated formatter changes. |
| `-Dtest=HttpRequestLoggingTest test`, after round 1 | Exit 0; 5 tests, zero failures/errors/skips on Tomcat 11.0.26. |
| Expanded targeted run after round 2 | Exit 1; 58 tests, zero assertion failures, one Mockito restubbing error. Preserved as failed evidence, not a passing gate. |
| `-Dtest=HttpRequestLoggingTest,RequestLogStateTest,OrderPlacementCoordinatorTest,CustomerDeletionTest test`, after round 3 | Exit 0; 62 tests, zero failures/errors/skips. |
| `spotless:check validate` | Exit 0; both explicit formatting check and lifecycle enforcement passed. |
| `-Dit.test=OrderPlacementConcurrencyIT failsafe:integration-test failsafe:verify` | Exit 0; 27 real PostgreSQL test executions, zero failures/errors/skips. |
| `clean verify` | Exit 0; Surefire XML: **506** tests; Failsafe XML: **123** tests; zero failures/errors/skips in both. Maven duration 1 minute 1 second. |
| Fresh JaCoCo XML | Lines **1156/1197 (96.57%)**; branches **640/697 (91.82%)**; instructions **5432/5570 (97.52%)**. Coverage is not proof that every required lifecycle scenario was tested. |
| SonarQube | Pending. Existing CI scanner publishes externally with SONAR_TOKEN; no credential-free project-wide local analysis was established. No new analysis/publication or clean-result claim. |
| PIT | Not executed; separate authorization required. No numeric mutation gate introduced. |

The accepted historical baseline records 474 fast and 122 PostgreSQL executions; the current fresh run adds 32 fast executions and one PostgreSQL scenario. Test count differences alone do not prove matrix completion.

Local raw evidence is preserved in `/tmp/increment6-environment.log`, `/tmp/increment6-http-initial.log`, `/tmp/increment6-http-round1.log`, `/tmp/increment6-http-round1-runtime.log`, `/tmp/increment6-targeted-phasebc.log`, `/tmp/increment6-targeted-round2.log`, `/tmp/increment6-targeted-round3.log`, `/tmp/increment6-quality.log`, `/tmp/increment6-postgresql-targeted.log` and `/tmp/increment6-clean-verify.log`. Surefire/Failsafe XML and JaCoCo HTML/XML are in ignored `target/`. These local artifacts are not durable repository evidence and can be lost on cleanup/reboot; retain them externally before further builds if required. The actual complete tracked/untracked diff is exported to `/tmp/increment6-complete.diff` for review.

### Verified Scope and Remaining Blocking Evidence

- **Verified:** 14 real HTTP executions cover synchronous completion, both startAsync overloads, exact supplied/original context identities on tested paths, redispatch, two cycles, timeout recovery, later ERROR rendering, unhandled servlet error, MVC Callable/DeferredResult success/failure and generated response IDs. Async completion metadata precedes terminal destruction on the tested paths. Canonical IDs are not derived from inbound headers; application output uses safe route/method/category values.
- **Verified:** 12 focused state/context tests cover duplicate closure races, stale outer observation, unresolved error/UNKNOWN, timeout/error flags, explicit producer scoping and reused worker cleanup, cancel-before-start, closed handles, observer failure, original start exception, nested ERROR/committed output simulation, MDC-provider failure and advice/fallback diagnostic deduplication. These do not substitute for every real-container case.
- **Verified:** existing and extended PostgreSQL tests exercise three real lock-timeout attempts, two scheduled retries, terminal exhaustion, lost commit confirmation with committed rows/no replay, normal Order commit and Customer deletion/deactivation/no-op with outer transaction commit/rollback. Existing concurrent winner schedules and full HTTP/domain/persistence regressions pass. Only disposable Testcontainers databases were used; no persistent development database or migration was touched.
- **Blocking verification gaps:** the complete approved F/H/P matrix is not yet established. Dedicated deterministic real-container graceful-shutdown/observable-disconnect/reset tests, explicit native async error/timeout-to-ERROR/completion race coverage, cancel-while-running cleanup and the correlated async real-PostgreSQL placement path (P6) remain Pending. Tests currently demonstrate only a subset of F1–F6/H5–H12/P5–P8; do not treat the passing regression build as full logging acceptance.
- **Blocking implementation detail:** Order events currently overload `countOrDelay` and omit the approved fixed rejection/exhaustion reason fields and explicit next-attempt field. The event names and outcome timing are implemented, but this does not fully realize the approved metadata matrix. No unapproved interpretation or reduced acceptance criterion is selected.
- **Test-quality limitation:** CustomerDeletionTest invokes synchronization hooks directly as a fast callback test. Its manual rollback-then-afterCommit sequence is not a realistic lifecycle and should be split into independent commit/rollback/unknown scenarios. The real PostgreSQL outer-transaction test separately proves commit/rollback behavior; it does not erase this unit-fixture limitation.

No approved Servlet mechanism was proven incompatible by the executed tests. The blocker is incomplete approved implementation/evidence after the bounded correction budget, not a proposal to reopen L1–L6. **All three correction rounds are used. Stop further source/test changes and obtain a human decision authorizing a bounded follow-up to finish these items.** Do not begin a fourth correction round under this authorization.

### Problems, Lessons and Safety Review

The first two corrections addressed test-fixture compilation errors (wrong Boot annotation package; missing ServletException declaration). The third corrected Mockito restubbing of a throwing method; completion metadata and focused state/context evidence were included in that bounded round. The earlier sandboxed Mockito failure required an environmental rerun, not dependency changes. Spending the finite budget on fixture defects left mandatory scenarios incomplete; a green broad build must not conceal that gap.

Application-owned events do not include payloads, raw paths/queries, customer identifiers/emails, SQL parameters or raw throwable contents. Security canaries are test-only values. Framework/Tomcat/Hibernate diagnostics remain outside that allowlist: actual lock-timeout logs include database relation/error text; other driver errors may include constraint values. No blanket infrastructure suppression or production privacy guarantee is claimed. Retention/access/audit delivery, unobservable disconnects, abrupt process failure, arbitrary unwrapped producers, cross-context requests, protocol upgrades and per-request custom MVC executors remain operational/integration limits. No custom executor override exists in current production endpoints.

Increment 5 remains Reviewed – Accepted. ADR-0002 and ADR-0006 remain Proposed. POM, Maven Wrapper, Makefile, CI, migrations, domain invariants and accepted transaction/retry policies were not changed. Implementation and human acceptance remain incomplete despite successful local regression checks.

### Final Documentation and Git Checks

Final checks: 126 relative links/anchors resolved; changed Markdown fences balanced; archived 579-line implementation prompt matches attachment SHA-256 2d907616cc906b28e209e862895e2e30464d649bf496c44a5c1d99fd698884da; git diff --check and untracked-file whitespace checks passed. Token/private-key-pattern scan found no matches in changed/new files; this is not a security guarantee. Existing Markdown hard line breaks are preserved. Branch/HEAD unchanged and index empty; POM, Makefile, CI, migrations and Wrapper unchanged. 25 modified/new files including pre-existing documentation.

## Increment 6 Acceptance Gap Closure Prompt — 2026-10-16

- **Status:** Authorized — Execution Started
- **Architecture:** L1–L6 Human Approved
- **Scope:** C1–C6 acceptance gap closure
- **Correction budget:** One planned implementation pass plus at most three correction rounds
- **Implementation acceptance:** Pending
- **Git:** No staging, commit or push authorized

````markdown
# Increment 6 — Acceptance Gap Closure
## Authorized Implementation, Verification and Documentation

### Role

Act as a Senior Java Software Engineer, Spring Boot Architect and Quality Engineer.

Your task is to close the confirmed acceptance gaps in **Increment 6 — Application Logging & Observability**.

This is a narrowly scoped follow-up to the previous implementation.

**Do not restart architecture planning, redesign the logging system or expand the feature scope.**

## 1. Repository and Context

Repository:

`/Users/rookie/Documents/Development/Projekte/agentic-order-service`

Expected branch: `develop`

Authoritative playbook:

`docs/04-agentic-coding/prompts/07-application-logging-observability.md`

Read:

1. `AGENTS.md`.
2. The complete Increment 6 playbook.
3. Approved L1–L6 architecture decisions.
4. Final L2 Lifecycle Design Review.
5. Archived original implementation prompt.
6. Previous implementation results and correction history.
7. The latest Acceptance Gap Assessment.
8. Relevant production code and existing tests.

Preserve all accepted Increment 5 guarantees.

### Previous verified baseline

The prior implementation reported:

- 506 fast tests passed.
- 123 PostgreSQL integration tests passed.
- Zero failures, errors or skips.
- JaCoCo: 96.57% lines, 91.82% branches.
- Spotless and Maven validate passed.
- SonarQube pending.
- PIT not executed.
- Three correction rounds exhausted.
- No commit or push.

These results are historical evidence, not acceptance of the remaining gaps.

## 2. Mandatory Prompt Archival — Before Implementation

Before changing production code, tests or configuration:

1. Inspect the Git working tree.
2. Open the Increment 6 playbook.
3. Append a section titled:

   `## Increment 6 Acceptance Gap Closure Prompt — 2026-10-16`

4. Record:

   - **Status:** Authorized — Execution Started
   - **Architecture:** L1–L6 Human Approved
   - **Scope:** C1–C6 acceptance gap closure
   - **Correction budget:** One planned implementation pass plus at most three correction rounds
   - **Implementation acceptance:** Pending
   - **Git:** No staging, commit or push authorized

5. Archive **this complete prompt verbatim**, including all sections and restrictions, inside a suitable Markdown code fence.

6. Verify that the archived content is complete and its code fences are balanced.

7. Preserve all earlier prompts, decisions, implementation results and gap-assessment evidence.

**Do not begin implementation until archival has succeeded.**

If the full text of this prompt is unavailable, stop and request it. Do not reconstruct or summarize it.

Do not alter this prompt's historical copy during implementation.

## 3. Authorization and Boundaries

The human explicitly authorizes:

- Targeted production corrections for C1 and C2.
- Required test corrections and additions for C1–C5.
- Minimal additional production corrections only if a test exposes a genuine defect within the already approved architecture.
- Local Maven builds, formatting corrections and automated tests.
- Disposable PostgreSQL integration-test execution.
- Documentation updates for C6.

The human does **not** authorize:

- New business features.
- Database migrations.
- Changes to transaction boundaries, locking order or retry policy.
- A new logging framework or dependency.
- New production REST endpoints.
- An alternative Servlet architecture.
- Changes to CI quality thresholds.
- External deployment.
- Persistent database modifications.
- Git staging, commits or pushes.

If remediation requires a material architecture change, stop and request human approval.

## 4. C1 — State-Machine Correctness

**Priority: BLOCKER. Implement and verify first.**

Relevant files:

- `RequestLogState.java`
- `RequestCorrelationFilter.java`
- `RequestLogStateTest.java`

### G1 — Stale status must not discard failure observations

The gap assessment identified an implementation defect in `RequestLogState.observe()`.

A dispatch with an older status token can fail after a nested dispatch recorded a newer status.

The current stale-status guard can discard that failure and incorrectly retain a successful `COMPLETED` outcome.

Correct this behavior without weakening valid nested ERROR resolution.

Requirements:

- Separate failure observations from status-precedence decisions.
- Preserve sticky error/failure evidence.
- Do not let a stale successful status overwrite a newer resolved status.
- Do not silently discard a genuine failure solely because its dispatch token is older.
- Use conservative UNKNOWN when a conflict cannot be resolved safely.
- Preserve the existing immutable terminal snapshot and single terminal emission claim.
- Do not introduce blocking operations under the state lock.

Add deterministic tests for:

1. Outer dispatch failure after nested status observation.
2. Nested ERROR recovery with authoritative final status.
3. Stale successful outer unwind.
4. Concurrent failure and completion observations.
5. Duplicate and late callback behavior.
6. Exactly one terminal emission claim.

The previously reported source-level counterexample must become an executable regression test.

### G2 — Async generation and duplicate observer handling

The gap assessment found insufficient generation ownership in `AsyncListener.onStartAsync()`.

Requirements:

- Associate each observer registration with a specific async generation.
- Prevent duplicate callbacks from incorrectly advancing a generation.
- Preserve genuine subsequent async cycles.
- Do not use AsyncContext object identity as a cycle identifier.
- Preserve correct listener re-registration.
- Keep the approved startAsync wrapper and Servlet contracts intact.

Test:

- Duplicate synthetic callback.
- Genuine next async cycle.
- Reused AsyncContext instance.
- Competing callback observations.
- No duplicate registration.
- No duplicate terminal emission.

**C1 exit gate:** All targeted state-machine tests pass and the original false-COMPLETED counterexample is eliminated.

Do not proceed to unrelated fixes while a known C1 correctness defect remains unresolved.

## 5. C2 — Complete Approved Order Logging Metadata

Relevant files:

- `OrderPlacementCoordinator.java`
- `OrderPlacementCoordinatorTest.java`
- Relevant existing PostgreSQL integration tests

Fix the incomplete event metadata identified in G6.

Use explicit, stable, typed or bounded safe fields.

### Required event metadata

**ORDER_COMMITTED**

- Current attempt.
- Explicit consolidated item count.
- Confirmed post-commit timing.

**ORDER_REJECTED**

- Current attempt.
- Fixed safe missing/inactive-resource reason.
- No raw resource IDs.

**ORDER_ATTEMPT_ROLLED_BACK**

- Current attempt.
- Approved SQLSTATE only when positively known and safely classified.
- No arbitrary exception-chain extraction.

**ORDER_RETRY_SCHEDULED**

- Current attempt.
- Next attempt.
- Delay in milliseconds.
- Approved SQLSTATE only when safely available.

**ORDER_RETRY_EXHAUSTED**

- Attempt count.
- Fixed approved exhaustion category distinguishing applicable attempt-limit, budget or interruption outcomes.

**ORDER_OUTCOME_UNKNOWN**

- Attempt.
- Explicit fixed UNKNOWN category.
- Never claim confirmed rollback or failure.

Preserve existing event names and approved log levels.

Do not add new retry classifiers.

Do not change actual retry scheduling, backoff, transaction completion or business error mapping.

Use owner-local logging rather than introducing an unnecessary shared abstraction.

### Tests

Assert:

- Correct reason categories.
- Correct attempt transitions.
- Exact existing retry delays: 25 ms and 50 ms.
- No fictitious additional retry.
- Correct exhaustion reason.
- Proper UNKNOWN classification.
- No sensitive log content.
- Correct event ownership and timing.

**C2 exit gate:** The approved Order event contract is satisfied without changing transaction or retry behavior.

## 6. C3 — Customer Callback Test Correctness

Relevant files:

- `CustomerDeletionTest.java`
- Existing Customer/PostgreSQL integration tests

The previous unit fixture invoked rollback completion and then afterCommit on the same callback, an impossible normal transaction lifecycle.

Replace that fixture with independent scenarios.

Required scenarios:

1. Confirmed commit.
2. Confirmed rollback.
3. Unknown or failed completion.
4. Physical deletion.
5. Deactivation.
6. Already-inactive no-op.
7. Outer transaction deferral.
8. Failing post-commit logging appender.
9. No success event before commit.
10. No success event after unsuccessful completion.

Use realistic transaction synchronization sequencing.

Distinguish unit simulation from actual PostgreSQL transaction evidence.

A failure occurring after confirmed commit must not be misrepresented as database rollback.

Do not modify production Customer DELETE behavior unless these tests expose a concrete defect.

**C3 exit gate:** Independent transaction lifecycle scenarios demonstrate correct post-commit-only emission.

## 7. C4 — Servlet Lifecycle and Safety Evidence

Relevant existing files:

- `HttpRequestLoggingTest.java`
- `RequestLogStateTest.java`
- `EventCapture.java`

Proposed new test file:

`src/test/java/com/example/agenticorderservice/shared/api/HttpRequestLifecycleIT.java`

Implement the missing acceptance evidence identified by G3 and G4.

### Required runtime scenarios

- Synchronous success and error.
- Nested ERROR handling.
- Callable success and failure.
- DeferredResult success and error result.
- Native Servlet async start.
- Both startAsync overloads.
- Multiple async cycles.
- Listener registration before context exposure.
- Async timeout followed by completion.
- Async timeout followed by ERROR.
- Native async onError.
- Competing callback observations.
- Direct async completion.
- Committed response.
- Response reset.
- Graceful shutdown with an active request.
- Observable or injected disconnect.
- UNKNOWN/ABORTED classification.
- No premature terminal event.
- Exactly one terminal event attempt.

Use real embedded Tomcat tests for lifecycle guarantees.

MockMvc alone is insufficient.

### Deterministic concurrency tests

Prefer:

- CountDownLatch.
- CyclicBarrier or Phaser where appropriate.
- Explicit lifecycle milestones.
- Bounded Future timeouts.
- Controlled cancellation.

Do not use arbitrary sleeps as synchronization.

Separate deterministic injected transport failures from nondeterministic real-network disconnect behavior.

Do not claim detection of every disconnect or proof of remote response delivery.

### Worker propagation

Verify:

- Actual Callable worker MDC correlation.
- Failure-path correlation using observations captured outside the worker.
- Request-owned DeferredResult producer correlation.
- AsyncContext.start propagation where explicitly supported.
- No EntityManager or ambient transaction propagation.
- Cleanup after worker exceptions.
- Cancellation before execution.
- Cancellation while running.
- Sequential requests reusing the same worker thread.
- No unrelated MDC field copying.
- Closed correlation handles cannot start new correlated work.

Exercise previously uncovered executor forwarding methods where applicable:

- `execute`
- `submitCompletable` overloads

Do not expand production executor capabilities beyond the approved design solely to improve coverage.

### Failure isolation

Test ordinary failures in:

- HTTP terminal logging.
- Customer post-commit logging.
- MDC binding/restoration.
- Async observer callbacks.

These must not alter application results, exception semantics, transaction decisions or Servlet dispatch behavior.

### Privacy

Capture application-owned diagnostics across relevant log owners.

Assert absence of:

- Authorization headers.
- Cookies.
- Credentials.
- Customer email addresses.
- Request bodies.
- Raw resource identifiers.
- Raw request paths and query strings.
- SQL parameter values.
- Raw exception messages and stack traces.
- CR/LF injection canaries.

Review framework-generated output separately and document residual risks.

**C4 exit gate:** The applicable H/F/P5/P8 requirements have inspectable evidence; any inherently unobservable transport limits are explicitly documented, not falsely marked VERIFIED.

## 8. C5 — Correlated Async PostgreSQL Transaction Evidence

Proposed new test:

`src/test/java/com/example/agenticorderservice/order/application/AsyncTransactionLoggingIT.java`

Reuse existing PostgreSQL test infrastructure.

Do not create new production endpoints.

A test-only async endpoint or servlet may be introduced when necessary.

Use real application services, not mocked Order or Customer services, for transaction-completion evidence.

Required assertions:

1. A server-generated request ID reaches the request-owned async worker.
2. The same correlation ID appears in the relevant application event and terminal HTTP event.
3. No ambient transaction exists on the async worker before the business service begins.
4. No EntityManager or transaction resource is accidentally propagated by correlation code.
5. Order success is not emitted before confirmed transaction completion.
6. Exactly one Order success event follows confirmed commit.
7. HTTP response completion remains separate from database commit.
8. Rollback/retry/UNKNOWN behavior remains unchanged.
9. Customer DELETE outcomes are emitted only after confirmed commit.
10. Existing concurrency race guarantees remain intact.

Reuse existing race tests and targeted assertions.

Do not duplicate the entire concurrency suite in the new fixture.

Do not introduce application-level retries or synchronization solely to make logging tests pass.

**C5 exit gate:** P1/P3/P6/P7 behavior is verified using genuine transaction boundaries.

## 9. C6 — Evidence Closure and Documentation

Update only the necessary Increment 6 documentation and directly affected status references.

Record:

- The archived follow-up prompt.
- The original gap findings G1–G7.
- C1–C6 implementation outcomes.
- Exact corrected files.
- New or modified tests.
- Requirement-to-evidence traceability.
- Test and quality-gate outcomes.
- Remaining operational limitations.
- Correction-round usage.
- Residual SonarQube/PIT status.
- Human acceptance still pending.

Do not overwrite or rewrite historical evidence.

Do not mark Increment 6 Accepted.

Use:

**Increment 6 — Acceptance Gap Closure Implemented, Pending Human Review**

only if mandatory local verification succeeds.

Otherwise record:

**Increment 6 — Acceptance Gap Closure Blocked, Human Decision Required**

## 10. Quality Gates and Execution Order

Use targeted checks after each package.

### Suggested sequence

1. C1 implementation and targeted state tests.
2. C2 logging metadata and targeted Order tests.
3. C3 Customer callback tests.
4. C4 Servlet lifecycle and safety tests.
5. C5 real PostgreSQL async transaction tests.
6. C6 documentation and evidence.
7. Final complete verification.

Compile new test fixtures before running expensive integration suites.

Use existing Maven test selection conventions.

Do not modify Maven configuration merely to avoid failing tests.

### Final mandatory verification

Run:

```bash
./mvnw -B -ntp spotless:check
./mvnw -B -ntp validate
./mvnw -B -ntp clean verify
```

Use Java 21.

Confirm disposable PostgreSQL resources are available before integration execution.

Inspect Surefire and Failsafe XML reports.

Report exact:

- Unit/fast test executions.
- PostgreSQL integration test executions.
- Failures.
- Errors.
- Skips.
- JaCoCo line and branch coverage.
- Applicable per-class coverage for new logging components.
- Relevant HTTP lifecycle tests.
- Relevant transaction/correlation tests.

Compare against the previous 506 + 123 successful regression baseline.

### SonarQube and PIT

SonarQube remains an explicit pending verification gate unless an already authorized and configured local analysis is available.

Do not require new external credentials or modify quality thresholds.

PIT is not authorized in this task.

Do not claim either gate passed without execution evidence.

## 11. Correction Budget and Efficiency

This follow-up has:

**One planned implementation pass across C1–C6, followed by at most three correction rounds in total.**

These are new, explicitly authorized rounds for this separate gap-closure task.

Do not count the previous exhausted rounds as available budget.

A correction round requires:

1. A concrete failing test or finding.
2. Root-cause identification.
3. A bounded fix.
4. Targeted verification.
5. An updated issue ledger.

Track each round explicitly.

Do not perform speculative refactoring.

Do not rerun the entire regression suite after every small change.

Complete all targeted exit gates before the final full verification.

If the three correction rounds are exhausted and a hard acceptance requirement remains unsatisfied, stop and report the blocker.

Do not weaken assertions, remove tests or redefine expected outcomes merely to obtain green results.

## 12. Hard Stop Conditions

Stop immediately if:

- The selected Servlet architecture proves incompatible with the runtime.
- The startAsync wrapper violates the Servlet contract.
- A request can still produce a false successful terminal log after unresolved failure.
- MDC leaks between unrelated requests.
- Logging changes business behavior.
- Logging failures trigger transaction replay.
- Post-commit logging occurs before confirmed commit.
- Existing HTTP responses or API contracts regress.
- A material new architecture decision is necessary.
- Persistent database modifications would be required.
- The authorized correction budget is exhausted with hard blockers remaining.

Explain the specific failure and do not independently redesign the system.

## 13. Final Git and Repository Integrity

Before reporting completion:

```bash
git diff --check
git diff --stat
git status --short
```

Include untracked files in the final inventory.

Verify:

- No unrelated modifications.
- No unapproved dependencies.
- No migrations or CI changes.
- No changes to accepted locking/retry policy.
- No hidden failing or disabled tests.
- No staged files.
- No commits or pushes.

Do not stage, commit, push, rebase, merge, switch branches or deploy.

## 14. Final Report

Provide:

### A. Implementation Summary

For each package C1–C6:

- Status: COMPLETE / PARTIAL / BLOCKED.
- Exact affected files.
- Implemented change.
- Remaining limitation.

### B. Acceptance Evidence

A compact matrix mapping U1–U3, H1–H12, P1–P8, F1–F6 and A1 to actual test evidence.

Use VERIFIED only for inspectable results.

### C. Verification

- Spotless.
- Maven validate.
- Full clean verify.
- Exact test counts.
- JaCoCo.
- SonarQube status.
- PIT status.
- Privacy/security results.

### D. Efficiency

- Planned implementation outcome.
- Correction rounds used.
- Main causes of correction.
- Evidence of targeted versus full-suite execution.
- Remaining efficiency improvements.

### E. Git Integrity

- Branch.
- HEAD.
- Modified and untracked files.
- Confirmation that no staging, commit or push occurred.

### F. Human Decision

Finish with exactly one:

**Increment 6 — Acceptance Gap Closure Implemented, Pending Human Review**

or

**Increment 6 — Acceptance Gap Closure Blocked, Human Decision Required**

Do not declare final human acceptance.
````

## Acceptance Gap Closure Execution — 2026-10-16

**Increment 6 — Acceptance Gap Closure Blocked, Human Decision Required**

The separately authorized follow-up prompt above was archived verbatim before source/test edits: 652 lines, SHA-256 `995fd671662c01666ed90d3358147913125bcc33a1451fcc490c2330ac912292`. Its historical copy and the original 579-line implementation prompt remain unchanged. L1–L6 remain Human Architecture Approved; implementation acceptance is Pending. Increment 5 remains Reviewed – Accepted; ADR-0002 and ADR-0006 remain Proposed.

### Assessment Findings Retained

| Finding | Original assessment | Follow-up outcome |
| --- | --- | --- |
| G1 | Older dispatch tokens could discard a later failure and retain false COMPLETED. | Failure facts now precede stale-status filtering. Executable counterexample and concurrent completion regression pass. |
| G2 | An observer had no independent async generation, allowing duplicate starts to advance/re-register. | Observer generation is immutable; advancing returns the claimed generation; a distinct observer handles the next cycle. Duplicate/competing callbacks and context reuse tests pass. |
| G3 | Real shutdown, disconnect/reset and native error/race evidence incomplete. | Ten of eleven lifecycle executions pass; native error recovery still has a final-status precision mismatch described below. |
| G4 | Worker failure assertions, executor methods, cancellation and logging/privacy isolation insufficient. | Failure observations asserted outside worker; execute/completable methods, running cancellation, HTTP appender failure and hostile metadata/body tests added. MDC mutation/restoration fault coverage is still incomplete. |
| G5 | Async HTTP and real PostgreSQL evidence were separate; Order commit checkpoints weak. | Thirteen combined async PostgreSQL tests pass, with genuine commit/rollback/unknown boundaries, empty incoming transaction resources and HTTP completion held separately. Existing 27 concurrency executions pass. |
| G6 | countOrDelay hid field meanings; safe reason/transition fields absent. | Explicit consolidatedItemCount, reason, attempts, nextAttempt, delayMs and fixed UNKNOWN added. No second SQLSTATE extractor or retry classifier introduced. |
| G7 | Unit fixture invoked rollback completion followed by afterCommit on one callback. | Replaced with independent Spring-managed synchronization sequences; actual PostgreSQL tests additionally exercise all three Customer branches under commit/rollback/lost confirmation. |

### Packages and Files

| Package | Status | Actual changes / limitation |
| --- | --- | --- |
| C1 | COMPLETE (targeted exit gate) | RequestLogState.java and RequestCorrelationFilter.java: retain failure evidence, generation-bound async observers and stale-completion rejection. RequestLogStateTest.java adds deterministic negative and concurrent cases. Broader native recovery precision remains C4-blocked. |
| C2 | COMPLETE (targeted exit gate) | OrderPlacementCoordinator.java and OrderPlacementCoordinatorTest.java: explicit bounded metadata and commit checkpoint. Fixed exhaustion reasons are ATTEMPT_LIMIT, SCHEDULING_BUDGET, BACKOFF_INTERRUPTED and BACKOFF_FAILED. No business/retry classification change. |
| C3 | COMPLETE (targeted exit gate) | CustomerDeletionTest.java uses independent COMMIT/ROLLBACK/UNKNOWN cases for physical deletion, deactivation and no-op plus a failing appender. Production Customer DELETE is unchanged. |
| C4 | BLOCKED | HttpRequestLoggingTest.java, RequestLogStateTest.java, EventCapture.java and new HttpRequestLifecycleIT.java extend actual worker observations, cancellation, root application capture, callbacks, reset/committed output, injected I/O failure and graceful shutdown. One real native recovery assertion remains failing. Additional detailed wrapper/MDC failure assertions remain Pending. |
| C5 | COMPLETE (targeted package cases; full acceptance not established) | New AsyncTransactionLoggingIT.java: four Order cases and nine Customer cases against real PostgreSQL. Commit/lost-confirmation injection uses a test-only JpaTransactionManager, after real doCommit and before successful Spring completion callbacks. No production endpoint or persistent database is used. Broader P1 post-commit response-failure evidence remains outside these cases. |
| C6 | COMPLETE (blocked execution record) | This playbook, index, open items and implementation plan record actual evidence and blockers. No acceptance claim. |

Source names above are in their existing packages under src/main/java or src/test/java/com/example/agenticorderservice. Full changed-file inventory is included below.

SQLSTATE is omitted from application events: the existing approved policy returns eligibility as a boolean, without exposing a safely classified state. The conditional safe-field allowance does not authorize reading the first exception-chain state or adding a parallel classifier. Existing retry policy, lock ordering, transaction boundaries, delays, attempt limits and HTTP business error mapping remain unchanged.

### Actual Execution and Correction Ledger

All commands used Java 21 through command-local JAVA_HOME and Maven Wrapper 3.9.9. Runtime tests ran with permission for local sockets, Mockito attachment and disposable Testcontainers. Docker availability was checked read-only (server 29.1.3). No Compose command or persistent database operation occurred.

| Step | Command / result |
| --- | --- |
| C1 | `-Dtest=RequestLogStateTest,HttpRequestLoggingTest test`: exit 0, 29 tests, zero failures/errors/skips. |
| Initial C2 check | Failed formatting because the Customer file was being edited/formatted concurrently. No Order tests executed. This sequencing error was corrected by serializing edits and verification. |
| Initial C3 | 14 tests, one error from an ambiguous generic AssertJ call in the new fixture; preserved as failed evidence. |
| Correction round 1 | Assign TransactionTemplate result to a typed String local. `-Dtest=OrderPlacementCoordinatorTest,OrderRetryPolicyTest,CustomerDeletionTest test`: exit 0, 94 tests, zero failures/errors/skips (36 Order coordinator, 44 retry-policy, 14 Customer). |
| Initial C4 compilation | Ambiguous Boot/Jakarta DispatcherType import; no runtime tests executed. |
| Correction round 2 | Explicit Boot imports. C4 fast tests: 34 passed (15 HTTP, 19 state/context). Initial lifecycle run: 11 executions, two timeout errors. |
| Correction round 3 | Correct native test fixtures to throw during actual Servlet ASYNC redispatch, rather than assume AsyncContext.start Runnable failure directly fires onError. Final fixture `test-compile`: exit 0. Final targeted integration run: 51 executions, one assertion failure, zero errors/skips. |
| Final targeted breakdown | OrderPlacementConcurrencyIT: 27/27 passed. AsyncTransactionLoggingIT: 13/13 passed. HttpRequestLifecycleIT: 10/11 passed, one failure. |
| Final complete verification | NOT EXECUTED: targeted C4 exit gate failed and all three correction rounds are used. No fourth round or weakening of the assertion. |

Planned implementation plus **three of three correction rounds used**. Counts above overlap across targeted runs and must not be summed as a new complete regression count. The prior 506-fast/123-integration green run remains historical evidence only.

The latest `test-compile` passed lifecycle formatting enforcement. `spotless:apply` was used during approved changes. Separate final `spotless:check`, `validate` and `clean verify` are Pending because execution stopped at the failed targeted gate. No fresh JaCoCo report was generated; 96.57% line/91.82% branch coverage and the preserved per-class report belong to the prior implementation, not this follow-up. Current whole-project and per-class coverage remain Pending. SonarQube remains Pending; PIT was not authorized or executed.

### Exact Remaining Runtime Blocker

`HttpRequestLifecycleIT.actualServletLifecyclePreservesConservativeStatusAndOneTerminalAttempt`, parameter `native-error`, expects:

```text
status=502 outcome=COMPLETED_AFTER_ERROR
```

The actual terminal event was:

```text
event=HTTP_REQUEST_COMPLETED method=GET route=OTHER status=UNKNOWN outcome=UNKNOWN durationMs=2 cycles=1 asyncCompleted=true
```

This is a conservative unknown result, not observed false success or transaction replay. The assertion remains unchanged and failing. The existing evidence does not establish whether recovery status was unavailable at the approved observation boundary or whether dispatch/failure provenance discards the recovered status. No architecture incompatibility is claimed without that proof. Further work requires explicit human authorization: investigate the ordering/provenance using the failing fixture, then make a bounded correction only within approved L2 or request a decision if the mechanism is actually incompatible.

The runtime source inspection explaining the previous two timeouts was read-only: Tomcat 11.0.26 RunnableWrapper catches a Runnable failure and sets error metadata; it does not directly fire onError there. ASYNC servlet dispatch failure exercises actual observer error handling. Tests do not invoke private Tomcat methods.

### Requirement-to-Evidence Traceability

HTTP = HttpRequestLoggingTest; STATE = RequestLogStateTest; LIFE = HttpRequestLifecycleIT; ASYNC-PG = AsyncTransactionLoggingIT; PG = OrderPlacementConcurrencyIT; ORDER = OrderPlacementCoordinatorTest; DELETE = CustomerDeletionTest. References below name the actual executed test scenario, not merely a related class.

| ID | Current status | Inspectable evidence / remaining gap |
| --- | --- | --- |
| U1 | PARTIALLY VERIFIED | STATE adviceAndFallbackShareOneSafeDiagnostic; root application privacy capture. Full concurrent diagnostic/failure-isolation combinations remain incomplete. |
| U2 | VERIFIED (targeted) | ORDER rejection reasons, explicit retry transitions, budget/interruption categories, commitCheckpointPrecedesSuccessEmission; unchanged retry-policy tests; unknown and failing-appender cases. |
| U3 | VERIFIED (targeted) | DELETE independentSpringCompletionSequencesEmitOnlyAfterCommit and failingPostCommitAppenderPreservesSuccessfulTransactionResult; actual Customer commit/rollback/unknown evidence in ASYNC-PG. |
| H1 | PARTIALLY VERIFIED | HTTP canonical response ID and hostile inbound ID; STATE hostile duplicate/oversized/CRLF headers and body plus MDC restoration. Real-container oversized/duplicate-header variants not independently exercised. |
| H2 | BLOCKED | HTTP normal/404/500, actual ERROR handling and LIFE nested scope pass; native recovery provenance mismatch unresolved. |
| H3 | PARTIALLY VERIFIED | Existing safe envelopes preserved in targeted HTTP tests; ASYNC-PG UNKNOWN asserts no duplicate HTTP technical event. Complete existing HTTP regression suite not rerun. |
| H4 | VERIFIED (targeted application allowlist) | Root application capture in HTTP; STATE hostileHeadersAndBodyNeverEnterApplicationDiagnostics; ORDER privacy assertions. Framework logs explicitly separate. |
| H5 | PARTIALLY VERIFIED | Real HTTP early invalid-path 400, 404/500, generated header; remaining real hostile-header variants and complete regression Pending. |
| H6 | VERIFIED (targeted) | HTTP captures Callable/DeferredResult worker ID outside worker; raw selected executor cleanup checked; LIFE gated async work has no premature terminal event. |
| H7 | PARTIALLY VERIFIED | HTTP both overloads/two cycles and STATE generation/idempotency/context-reuse tests pass. Full real wrapper-invalid/original-flag cases incomplete. |
| H8 | BLOCKED | LIFE timeout-complete, timeout-error pass; STATE competing callbacks retain failure; native error recovery final-status assertion fails. |
| H9 | VERIFIED (targeted) | STATE everyExecutorForwardingMethodScopesOnlyOwnedMetadataAcrossReusedWorker, running cancellation, cancel-before-start and closed handles; HTTP actual selected worker cleanup; ASYNC-PG resource isolation. |
| H10 | PARTIALLY VERIFIED | LIFE committed, reset, direct completion and injected-disconnect UNKNOWN pass. Real-network delivery/disconnect guarantees are not claimed. |
| H11 | VERIFIED (targeted state claims) | STATE executable outer failure regression, concurrent completion/failure, duplicate closure and generation claims; late closure cannot emit twice. Native status recovery remains H8/F4-blocked. |
| H12 | PARTIALLY VERIFIED | STATE terminal-appender failure, failing MDC get, observer registration failure; DELETE appender failure; root application privacy assertions. MDC put/restoration failure coverage remains incomplete. |
| P1 | PARTIALLY VERIFIED | ASYNC-PG beforeCommit/afterCompletion checkpoints see no success; exactly one success before released HTTP completion with committed rows. Post-commit response-failure case not independently tested. |
| P2 | VERIFIED (targeted) | Existing PG real lock-timeout/rollback/fresh attempts/exhaustion/SQLSTATE safety cases pass; ASYNC-PG RETRY keeps same policy. |
| P3 | VERIFIED (targeted) | PG actualCommitWithLostConfirmationNeverReplays plus ASYNC-PG UNKNOWN: committed row, one attempt, no success/replay, correlated UNKNOWN and no duplicate HTTP technical diagnostic. |
| P4 | VERIFIED (targeted) | ASYNC-PG nine Customer branch/completion combinations; existing PG outer-transaction and both race winner schedules pass. |
| P5 | BLOCKED | Actual Tomcat REQUEST/ASYNC/ERROR registration and nested scope tested; native recovered-status mismatch unresolved. |
| P6 | VERIFIED (targeted) | ASYNC-PG real services, generated worker/application/terminal ID, incoming transaction/resource absence, commit checkpoints, rollback/retry/UNKNOWN and distinct HTTP completion. |
| P7 | VERIFIED (targeted) | ASYNC-PG Customer outcomes plus existing PG first-order race schedules pass without business-policy changes. |
| P8 | PARTIALLY VERIFIED | LIFE graceful shutdown with active async request, reset, committed response and deterministic injected I/O failure pass; no universal disconnect claim. |
| F1 | PARTIALLY VERIFIED | Existing HTTP/error fixtures and LIFE nested correlation scope inside actual ERROR dispatch. Arbitrary native nested container redispatch is not claimed. |
| F2 | BLOCKED | Async completion metadata, cycles, direct completion and timeout handling pass; native error recovery precision fails. |
| F3 | PARTIALLY VERIFIED | Real exact supplied/original objects, both overloads and unit delegation/original exception behavior. Real invalid-wrapper and hasOriginalRequestAndResponse assertions incomplete. |
| F4 | BLOCKED | UNKNOWN direct/injected failure, committed/reset output and graceful shutdown pass; known recovered-status provenance unresolved. |
| F5 | PARTIALLY VERIFIED | Generation-bound observers, duplicate/competing registration and context reuse pass; gated native work demonstrates no premature completion. Full wrapper/invalid-cycle permutations incomplete. |
| F6 | PARTIALLY VERIFIED | Duplicate/late claims, cancellation/worker cleanup, failing HTTP/Customer appenders and observer failure pass. Full MDC mutation/restoration failure evidence Pending. |
| A1 | VERIFIED (diff scope) | No POM/dependency/CI/migration/Makefile/Wrapper/domain or locking/retry-policy change. Production corrections are confined to approved HTTP state/observer and Order event metadata files. |

### Evidence, Safety and Remaining Limitations

Evidence root: `/tmp/increment6-closure-_4l5pjyf`. It contains baseline reports copied before execution, initial file fingerprints/status, all package/round logs, final targeted Surefire/Failsafe report copies and this follow-up's final diff/status. Local temporary evidence is not durable archival storage. Preserve it externally before cleanup if needed.

Application-owned captured output excludes tested Authorization/Cookie/body/PII/raw-path/query/exception canaries and throwable proxies. This does not sanitize framework diagnostics: native fixture errors can appear in Tomcat's own exception logging; Hibernate/driver diagnostics may include SQL/constraint values. No broad framework suppression, retention/access guarantee or production readiness is introduced.

Injected IOException is controlled failure evidence, not proof of every client disconnect. UNKNOWN is used where transport/final status is not trustworthy; no positive ABORTED classification or remote delivery acknowledgment is claimed. Test-driven nested filter scoping runs within a genuine Tomcat ERROR dispatch; it is not claimed as a distinct naturally generated nested container redispatch. Abrupt process failure, missing terminal notification, permanently unavailable MDC backends, unwrapped producers and custom executor overrides remain limits requiring explicit operational/integration treatment.

Execution stopped after the third correction round. No source/test changes or further test/build execution are authorized under this exhausted follow-up. The next human decision must address the failing native recovery fixture and any remaining detailed matrix evidence, rather than silently accept a smaller contract.

### Exact Follow-Up File Inventory

- `docs/04-agentic-coding/prompts/07-application-logging-observability.md`
- `docs/00-overview/open-items.md`
- `docs/04-agentic-coding/prompts/00-index.md`
- `docs/07-implementation/implementation-plan.md`
- `src/main/java/com/example/agenticorderservice/order/application/OrderPlacementCoordinator.java`
- `src/main/java/com/example/agenticorderservice/shared/api/RequestCorrelationFilter.java`
- `src/main/java/com/example/agenticorderservice/shared/api/RequestLogState.java`
- `src/test/java/com/example/agenticorderservice/customer/application/CustomerDeletionTest.java`
- `src/test/java/com/example/agenticorderservice/order/application/AsyncTransactionLoggingIT.java`
- `src/test/java/com/example/agenticorderservice/order/application/OrderPlacementCoordinatorTest.java`
- `src/test/java/com/example/agenticorderservice/shared/api/HttpRequestLifecycleIT.java`
- `src/test/java/com/example/agenticorderservice/shared/api/HttpRequestLoggingTest.java`
- `src/test/java/com/example/agenticorderservice/shared/api/RequestLogStateTest.java`
- `src/test/java/com/example/agenticorderservice/shared/logging/EventCapture.java`

All other pre-existing changes were preserved. No staging, commit, push, branch operation, deployment or persistent database migration was performed.


## Increment 6 — Native Async Error Fixture Correction — 2026-10-16

**Status:** Separately Authorized — Execution Started

**Scope:** One native-error fixture correction and conditional verification; prior exhausted budgets remain unchanged. Implementation acceptance is Pending. No production changes or Git writes authorized.

````markdown
# Increment 6 — Native Async Error Fixture Correction

## Authorization

The human authorizes exactly one narrowly scoped correction to the `native-error` test fixture, based on the completed root cause analysis.

This is a separate, explicitly authorized follow-up to the exhausted Acceptance Gap Closure correction budget.

Do not reopen the architecture or begin another general correction cycle.

## Repository

`/Users/rookie/Documents/Development/Projekte/agentic-order-service`

Expected branch: `develop`

Authoritative playbook:

`docs/04-agentic-coding/prompts/07-application-logging-observability.md`

Target file:

`src/test/java/com/example/agenticorderservice/shared/api/HttpRequestLifecycleIT.java`

## 1. Archive This Prompt First

Before changing any test code:

- Append this complete prompt verbatim to the Increment 6 playbook.
- Title the section `Increment 6 — Native Async Error Fixture Correction — 2026-10-16`.
- Record the separate human authorization and its limited scope.
- Preserve all previous prompts and correction history.
- Do not modify the archived original prompts.

If the full prompt cannot be archived exactly, stop before implementation.

## 2. Confirmed Root Cause

The previous read-only analysis established that the `native-error` fixture invokes a second `AsyncContext.dispatch()` after the existing cycle has already been dispatched.

Tomcat 11.0.26 rejects the operation with an `IllegalStateException`.

Consequently, the recovery handler never sets the intended HTTP 502 status.

This is a test-fixture defect, not a demonstrated production logging defect.

## 3. Required Correction

Modify only `HttpRequestLifecycleIT.java`.

For the `native-error` scenario:

1. Begin a new legal Servlet async cycle during the first ASYNC dispatch, before triggering the intended native error.
2. Preserve Servlet request/response identity and `startAsync()` contract requirements.
3. Re-register the fixture AsyncListener through `onStartAsync()` as required.
4. Ensure the error callback dispatches the new active cycle to the recovery handler.
5. Preserve the existing production correlation and terminal logging mechanisms.
6. Use deterministic synchronization and bounded waits.
7. Do not introduce arbitrary sleeps or timing-dependent assumptions.

Do not modify production Java classes, dependencies, Maven configuration or existing quality thresholds.

## 4. Mandatory Assertions

The corrected test must verify:

- The recovery dispatch was actually entered.
- The client received HTTP 502.
- The response body is `recovered`.
- Exactly one intended error callback occurred.
- Exactly one intended completion callback occurred.
- Exactly one terminal HTTP log event was emitted.
- The request ID remained unchanged.
- Async generation and listener registration behaved correctly.
- No callback `IllegalStateException` occurred.
- The terminal event reports:

`status=502 outcome=COMPLETED_AFTER_ERROR`

Do not change that expected terminal outcome merely to make the test pass.

## 5. Verification

First compile and execute the targeted fixture using the repository's existing Maven conventions.

If the targeted test passes, run the complete `HttpRequestLifecycleIT` class.

If all lifecycle tests pass, run:

```bash
./mvnw -B -ntp spotless:check
./mvnw -B -ntp validate
./mvnw -B -ntp clean verify
```

Use Java 21 and the existing disposable PostgreSQL integration-test infrastructure.

Inspect Surefire and Failsafe XML reports and report exact test counts, failures, errors and skips.

Report JaCoCo coverage and the status of the remaining Increment 6 acceptance requirements.

SonarQube remains pending unless separately authorized and available. Do not run PIT again.

## 6. Strict Stop Conditions

Stop if:

- A valid new async cycle cannot be established using supported Servlet APIs.
- The fixture requires a production code change.
- The correction changes application error handling.
- The recovered HTTP status remains unobservable.
- The targeted test fails after this one correction and verification attempt.
- A material new architectural decision becomes necessary.

Do not perform additional speculative correction rounds.

Report the precise remaining failure instead.

## 7. Documentation

Update the Increment 6 playbook with:

- The confirmed fixture root cause.
- The exact correction.
- Actual targeted and complete regression evidence.
- Remaining acceptance gaps.
- Updated implementation status.

Do not erase previous unsuccessful results or exhausted correction budgets.

Do not declare Increment 6 finally accepted.

## 8. Git Restrictions

Do not stage, commit, push, merge, rebase, switch branches or deploy.

Preserve unrelated modifications and historical evidence.

Report final Git status.

## 9. Final Report

Provide:

1. Exact test-fixture change.
2. Servlet lifecycle explanation.
3. Targeted test results.
4. Complete regression results, if reached.
5. JaCoCo results, if reached.
6. Remaining acceptance gaps.
7. Confirmation of no production changes.
8. Final Git status.

Finish with either:

**Increment 6 — Native Async Fixture Corrected, Pending Human Review**

or

**Increment 6 — Native Async Fixture Correction Blocked, Human Decision Required**
````

## Native Async Fixture Correction Execution — 2026-10-16

**Increment 6 — Native Async Fixture Correction Blocked, Human Decision Required**

This separately authorized single correction does not reopen the previously exhausted three-round budget. The complete 160-line authorization was archived verbatim before test edits (SHA-256 `190f7a3f8ea35be1b2bc5e9ee5814cb83e34fc4e59901a9e3b0d38274ff6b46c`). Earlier prompts, failed runs, acceptance gaps and architecture approvals remain unchanged. L1–L6 remain Human Approved; implementation acceptance is Pending. Increment 5 remains Reviewed – Accepted; ADR-0002/0006 remain Proposed.

### Confirmed Root Cause and Exact Correction

The prior fixture dispatched the already-dispatched original async cycle from onError. Tomcat 11.0.26 rejected that callback with IllegalStateException, so the intended 502 recovery handler was not reached. This establishes a fixture defect, not a demonstrated production status-observation defect.

Only HttpRequestLifecycleIT.java was changed in this follow-up, besides this playbook:

- Give native-error a dedicated `nativeAsyncErrorRecoversInNewCycle` test entry point, preserving the other nine parameterized scenarios and graceful-shutdown test.
- In the first native-error ASYNC dispatch, call `request.startAsync(request, response)` before the intended ServletException. Record exact supplied request/response identity for assertion outside the server thread.
- Re-register the fixture listener through onStartAsync for that new cycle. Record restart/registration counts, dispatch correlation IDs and callback RuntimeExceptions.
- Add bounded recovery-entry observation and assertions for client 502/body `recovered`, three correlated Servlet entries, one restart/two fixture registrations, unchanged request/response identity, no callback exception, two logged async generations, exactly one error/completion and terminal event. Preserve `status=502 outcome=COMPLETED_AFTER_ERROR`.

No production logging, application error handling, dependency, Maven, schema, migration, CI or transaction behavior was changed. The correction uses public Servlet APIs and adds no sleeps.

### Actual Verification and New Blocker

Java 21 was selected through command-local JAVA_HOME; Maven Wrapper remains 3.9.9. Exact commands, logs and XML are preserved under `/tmp/increment6-native-fixture-528qhy37`.

| Gate | Actual evidence |
| --- | --- |
| Target-file formatting | Scoped `spotless:apply`: exit 0. Hash comparison confirms only the authorized test file and playbook changed. |
| Compilation and targeted fixture | `-Dit.test=HttpRequestLifecycleIT#nativeAsyncErrorRecoversInNewCycle test-compile failsafe:integration-test failsafe:verify`: compilation and lifecycle formatting check passed; Maven exit 1. Failsafe XML: **1 test, 1 failure, 0 errors, 0 skips**, suite time 13.40 seconds. Surefire tests were not invoked. |
| Precise JUnit failure | `Missing request-owned event: HTTP_REQUEST_COMPLETED`, at verifyLifecycle line 112, after the bounded event wait. |
| Container evidence | The new cycle reached the intended ServletException and a subsequent recovery Servlet entry. Before recovery output, the application emitted INTERNAL_SERVER_ERROR. Recovery `response.getWriter()` at line 229 threw `IllegalStateException: getOutputStream() has already been called for this response`. |
| Conditional complete lifecycle class | NOT EXECUTED: targeted fixture failed. |
| Conditional final spotless:check / validate / clean verify | NOT EXECUTED: the single-attempt stop condition applies. |
| Fresh JaCoCo | NOT GENERATED. Existing coverage XML is historical, not evidence for this attempted correction. |
| SonarQube / PIT | SonarQube Pending; PIT not authorized or executed. |

The original illegal redispatch is replaced by a new legal async cycle; recovery entry is evidenced by its getWriter stack frame. However, normal container/application fallback processing has already selected the response output stream. ApiErrorResponseWriter uses getOutputStream for its error envelope; the fixture then attempts the incompatible writer API. The intended recovery body is not produced. No terminal completion event was captured within the bounded wait. The evidence does not establish the final client status/body, successful final classification, callback counts, request/response identity assertions or complete lifecycle ordering: those assertions were not reached after the event wait failed. There is no claim of passing 502 recovery or proof that changing only the output API would resolve the remaining lifecycle issue.

**Stopped after one correction and one targeted verification attempt.** No additional test edits or speculative correction round was performed. Further work needs separate human authorization; this record does not authorize changing error handling or production logging.

### Remaining Acceptance Requirements

The prior requirement-to-evidence matrix remains applicable with the following current qualifications: H2/H8/P5/F2/F4 native recovered-status evidence remains Blocked; exact supplied-object/runtime-wrapper coverage (H7/F3), MDC mutation/restoration failure coverage (H12/F6), the independent post-commit response-failure case (P1) and other previously recorded partial evidence remain Pending. Previously passed targeted C1/C2/C3 and async PostgreSQL cases are preserved evidence, not a new regression run. Full regression, fresh whole-project/per-class coverage and complete acceptance remain Pending. No persistent database or Docker Compose operation occurred.

### Repository and Evidence Integrity

Branch remains develop at `9345fa122df05fd5850ae183834f1b5c141cf3f7`. Pre-existing modified/untracked files were preserved. This follow-up changes only the fixture and this playbook; production files are byte-identical to their starting state. Nothing was staged, committed, pushed, merged, rebased or deployed.

The evidence directory contains initial status/hashes and original fixture/playbook copies, previous test/coverage reports copied before execution, formatting and targeted Maven logs, copied targeted Failsafe reports, exact follow-up and complete Git diffs, and final status. This is temporary local storage, not durable archival storage. The complete archived authorization and all previous prompt bytes remain preserved. Final whitespace, Markdown links/anchors and fence checks are reported separately from unexecuted Maven gates.


## Increment 6 — Final Stabilization and Release Readiness — 2026-10-16

**Status:** Authorized — Execution Started

**Scope:** Minimal fixture separation, targeted and conditional complete verification, final documentation.

**Budget:** One planned fixture correction and at most one additional targeted correction. Previous budgets remain historical and exhausted. Implementation acceptance is Pending; no Git writes or production deployment authorized.

````markdown
# Increment 6 — Final Stabilization and Release Readiness
## One Bounded Execution — No Further Review Loops

### Role

Act as a Senior Java Software Engineer and Release Quality Engineer.

Complete the remaining verification of **Increment 6 — Application Logging & Observability** using the simplest Servlet-compliant solution.

Optimize for:

1. Correctness and safety.
2. Minimal implementation complexity.
3. Short execution time.
4. Clear acceptance evidence.
5. A definitive completion or blocker report.

**Do not initiate another architecture review or open-ended investigation.**

## 1. Repository

`/Users/rookie/Documents/Development/Projekte/agentic-order-service`

Expected branch: `develop`

Authoritative playbook:

`docs/04-agentic-coding/prompts/07-application-logging-observability.md`

Read `AGENTS.md`, the active approved L1–L6 decisions, the latest execution records and the relevant Servlet test implementation.

Previous correction budgets and findings remain historical records.

This is a new, explicitly authorized final stabilization task.

## 2. Archive This Prompt First

Before editing source or tests:

- Append this complete prompt verbatim to the Increment 6 playbook.
- Heading: `## Increment 6 — Final Stabilization and Release Readiness — 2026-10-16`
- Record authorization, scope and execution budget.
- Preserve all historical prompts and failed attempts.
- Verify that the archived text is complete.

If exact archival is impossible, stop.

Then proceed directly to the authorized work.

## 3. Scope and Execution Budget

This task authorizes:

- One planned, minimal test-fixture correction.
- At most one additional targeted correction if a concrete defect is demonstrated.
- Targeted tests.
- Full Maven verification.
- Final documentation and status updates.

Do not perform speculative refactoring.

Do not introduce new frameworks, production endpoints, database migrations or dependencies.

Do not alter accepted Order/Customer business behavior, transaction boundaries, locking or retry policies.

Do not weaken security or privacy guarantees.

If a material production defect or architecture incompatibility is discovered, stop and report it.

## 4. Correct the Native Async Error Test

Target:

`src/test/java/com/example/agenticorderservice/shared/api/HttpRequestLifecycleIT.java`

The existing fixture incorrectly combines:

1. A native unhandled async exception that allows container error-page processing to finish.
2. A later attempt to replace the completed response with HTTP 502.

Separate these into two valid scenarios.

### Scenario A — Native Unhandled Async Error

Exercise the genuine native Servlet error lifecycle.

Verify:

- The error is observed.
- No false successful HTTP completion is logged.
- Request correlation remains consistent where observable.
- No sensitive exception content is logged by application-owned events.
- MDC cleanup remains correct.
- Exactly one terminal event attempt is observed when the request scope is destroyed.

A final UNKNOWN status is acceptable **only when the response status is genuinely unobservable under the approved provenance rules**.

Do not change a proven failure into a successful result merely to satisfy a test.

If request destruction does not occur, or a terminal event remains missing, determine whether the fixture itself leaves an invalid or unfinished async lifecycle.

Do not add production lifecycle workarounds to manufacture a terminal event.

### Scenario B — Successful Pre-Commit Recovery

Use a test-owned, Servlet-compliant recovery boundary.

The test must arrange for the production correlation filter to observe a genuine failed dispatch or approved error observation while the response is still mutable.

Catch and handle the error before Tomcat finishes its error-page response.

Start a new async cycle only where required and valid.

Dispatch to a test-only recovery handler that returns:

- HTTP status 502.
- Exact response body `recovered`.

The test must demonstrate that recovery is genuinely observed by the existing logging state machine.

Assert:

- Client status 502.
- Exact response body.
- Same server-generated request ID.
- Exactly one terminal event.
- Terminal status 502.
- Outcome `COMPLETED_AFTER_ERROR`.
- No duplicate listeners or terminal events.
- No callback exceptions.
- No change to production HTTP behavior.

**Do not simulate recovery by directly manipulating RequestLogState or injecting artificial success metadata.**

The existing `502 / COMPLETED_AFTER_ERROR` requirement remains applicable to this valid pre-commit recovery scenario.

It is not a guarantee that native `onError()` can replace an already committed container error response.

### Keep It Minimal

Prefer a small change to the existing test fixture.

Do not develop a general-purpose test framework.

Do not add a production response wrapper, error resolver or additional terminal owner.

If the two scenarios cannot be implemented correctly within the permitted scope, stop with a concrete blocker.

## 5. Targeted Verification First

Compile the modified fixture.

Run the two scenarios individually using the existing Maven/Failsafe conventions.

Then run the complete `HttpRequestLifecycleIT` class.

Do not run full integration tests until the targeted lifecycle tests pass.

Confirm:

- No failing lifecycle assertions.
- No unexpected callback exceptions.
- No false successful completion.
- No missing terminal event for observably destroyed requests.
- Valid response and status observations.

If a targeted test fails:

- Diagnose the concrete failure.
- Use the single permitted additional correction only if it is small and within the approved fixture scope.
- Rerun the targeted test once.

If it still fails, stop.

Do not start another investigation chain.

## 6. Complete Quality Verification

After all targeted lifecycle tests pass, run the existing required gates:

```bash
./mvnw -B -ntp spotless:check
./mvnw -B -ntp validate
./mvnw -B -ntp clean verify
```

Use Java 21 and existing disposable PostgreSQL test infrastructure.

Report:

- Exact Surefire test count.
- Exact Failsafe test count.
- Failures, errors and skips.
- JaCoCo line and branch coverage.
- Logging-specific coverage where available.
- Any regressions compared with the accepted Increment 5 baseline.

Inspect actual XML reports, not only Maven's final BUILD SUCCESS message.

Do not disable failing tests or change quality thresholds.

### SonarQube and PIT

The previous PIT run was independently executed and reported:

- 697 generated mutations.
- 572 killed.
- 82% mutation score.
- 86% test strength.

Preserve this as separate historical evidence, not as proof of the final amended code.

Do not rerun PIT.

SonarQube remains pending unless an already configured, explicitly authorized local check can run without credentials, publication or new setup.

Do not claim SonarQube passed without current evidence.

## 7. Release-Readiness Boundary

Distinguish:

**Increment 6 acceptance readiness**

from

**whole-application production readiness**.

A successful Increment 6 verification means that the approved logging changes and regressions are ready for human review.

It does not automatically authorize production deployment.

Do not declare the entire application production-ready without separate evidence for deployment configuration, secret handling, database operations, security, observability operations, backup/recovery and other relevant production controls.

Do not begin deployment preparations or infrastructure changes.

## 8. Final Documentation

Update the active Increment 6 status and record:

- The corrected test scenarios.
- The original fixture root causes.
- Actual targeted tests.
- Full regression evidence.
- JaCoCo coverage.
- Any remaining acceptance limitations.
- Historical PIT results, clearly identified as a different execution snapshot.
- SonarQube status.
- Final Git state.

Preserve all historical prompts and failed test evidence.

If all mandatory Increment 6 acceptance requirements have inspectable evidence, use:

**Increment 6 — Implementation Verified, Pending Human Acceptance**

Otherwise use:

**Increment 6 — Stabilization Blocked, Human Decision Required**

Do not mark Increment 6 human-accepted.

## 9. Git Restrictions

Do not:

- Stage files.
- Commit.
- Push.
- Merge.
- Rebase.
- Switch branches.
- Deploy.
- Modify persistent databases.

Preserve unrelated working-tree changes.

Perform:

```bash
git diff --check
git diff --stat
git status --short
```

Account for untracked files.

## 10. Final Report

Provide a concise report covering:

1. Exact fixture changes.
2. Results of both async error scenarios.
3. Complete lifecycle-test result.
4. Full Maven verification counts.
5. JaCoCo coverage.
6. Remaining Increment 6 acceptance gaps, if any.
7. SonarQube and PIT status.
8. Production-readiness limitations.
9. Correction budget used.
10. Final Git status.

Finish with exactly one:

**Increment 6 — Implementation Verified, Pending Human Acceptance**

or

**Increment 6 — Stabilization Blocked, Human Decision Required**

Do not start further work after producing this report.
````

## Final Stabilization Execution and Readiness — 2026-10-16

**Increment 6 — Stabilization Blocked, Human Decision Required**

The authorized fixture stabilization and all executed regression gates are green. This status reflects remaining mandatory acceptance evidence listed below, not a remaining failed native-error test. No material production defect or architecture incompatibility was demonstrated. Implementation acceptance remains Pending; L1–L6 remain Human Approved, Increment 5 remains Reviewed – Accepted and complete ADR-0002/0006 remain Proposed.

The complete 311-line stabilization authorization was archived verbatim before test edits (SHA-256 `d6c4248480ae7aef65ffbffea6f2626060ccdc472766541663f3d1638548f76c`). This is one separately authorized planned correction plus one additional whitespace-only correction. Earlier exhausted budgets, unsuccessful runs and immutable prompts remain historical and unchanged.

### Corrected Fixture and Servlet Ownership

HttpRequestLifecycleIT.java now separates two scenarios:

- `nativeUnhandledAsyncErrorCompletesWithoutFalseSuccess`: a genuine native ASYNC ServletException reaches container error handling. Its fixture onError completes the existing cycle instead of illegally redispatching it or attempting to replace the rendered response. The client receives 500; one error callback, one completion callback, one registration and one terminal event are asserted. The terminal event is UNKNOWN because onError invalidates prior error-page observations and no subsequent authoritative dispatch observes final status. No response is sampled from onComplete/destruction. Client-visible 500 does not itself establish that missing post-error logging observation or remote delivery guarantees.
- `preCommitAsyncRecoveryPreservesObservedFailure`: a small test-owned filter runs immediately outside the production correlation filter, only handling the fixture's ASYNC precommit-recovery ServletException. The genuine exception crosses the production filter and its failure observation before the test boundary catches it. The boundary records an uncommitted response, starts a new legal cycle using the exact current request/response, and dispatches to the fixture recovery handler. It prevents this test exception from entering Tomcat error-page rendering; no RequestLogState manipulation or synthetic success metadata occurs. The client gets exact body `recovered` and status 502; the terminal event retains COMPLETED_AFTER_ERROR. The test asserts three same-ID Servlet entries, two generations, one restart/two fixture registrations, no callback failures, one completion and one terminal event. No native onError callback is expected for this application-handled recovery.

Both scenarios assert correlation, privacy canaries, no throwable payloads and restored MDC outside the inner production dispatch scope. This is request-dispatch MDC cleanup evidence; existing fast tests separately cover reused executor workers. The nine other parameterized scenarios and graceful-shutdown scenario remain present. The fixture now has 12 executions, rather than 11, because the formerly combined case is split into two.

The original failure was an illegal second dispatch of an already-dispatched cycle. The prior correction established a fresh cycle but allowed Tomcat to finish application error rendering before recovery. ApiErrorResponseWriter had selected getOutputStream; recovery then attempted getWriter, and request-owned terminal observation was missing within the test bound. These failures are preserved. The new tests neither replace an already committed response nor claim that native onError can do so. No production error resolver, response wrapper or terminal owner was added.

### Actual Commands and Quality Results

All Maven execution used Java 21 through command-local JAVA_HOME, the existing Maven Wrapper 3.9.9 and unchanged build configuration. Exact commands, stdout/stderr logs, exit results, XML and coverage reports are retained at `/tmp/increment6-stabilization-7fbkt_6l`.

| Gate | Actual result |
| --- | --- |
| Initial scoped formatting | Exit 0 but two layout violations remained; this was not treated as a passed check. |
| First targeted command | Exit 1 at Spotless before compilation/tests. One additional correction applied exactly the two reported whitespace changes. |
| Native error retry, including test-compile | Exit 0; 1 test, 0 failures/errors/skips. Native-error terminal: UNKNOWN, cycles=1, asyncCompleted=true. |
| Individual pre-commit recovery, including test-compile | Exit 0; 1 test, 0 failures/errors/skips. Terminal: status=502, outcome=COMPLETED_AFTER_ERROR, cycles=2, asyncCompleted=true. |
| Complete HttpRequestLifecycleIT | Exit 0; 12 tests, 0 failures/errors/skips. |
| spotless:check | Exit 0. |
| validate | Exit 0. |
| clean verify — Surefire | Exit 0; **528 tests, 0 failures, 0 errors, 0 skips**. |
| clean verify — Failsafe | **148 tests, 0 failures, 0 errors, 0 skips**: 136 PostgreSQL executions plus 12 Servlet lifecycle executions. |
| Fresh JaCoCo lines | **1176/1213 = 96.95%**. |
| Fresh JaCoCo branches | **657/707 = 92.93%**. |

Counts were parsed from the fresh Surefire/Failsafe XML after clean verify. Individual/class runs overlap final totals and must not be added to them. Final reports include all 27 OrderPlacementConcurrencyIT cases and all 13 AsyncTransactionLoggingIT cases. Existing application/API/persistence regressions pass; no accepted Increment 5 behavior, thresholds, dependencies, migrations, transaction boundaries, locking or retry policy was changed. The accepted Increment 5 baseline (474 fast/122 PostgreSQL, 684/696 lines and 523/561 branches) has a different scope/denominator and must not be presented as an equivalent coverage comparison or new mutation evidence.

### Logging-Specific JaCoCo Evidence

The following are actual class counters from the fresh combined unit/integration coverage XML. Generated nested records and other nested classes are listed separately in verification-summary.json; line coverage alone does not establish fault-path or lifecycle completeness.

| Production class | Covered/total lines | Covered/total branches |
| --- | --- | --- |
| HttpFailureDiagnostic | 10/10 (100.00%) | 15/16 (93.75%) |
| RequestLogState | 52/52 (100.00%) | 53/58 (91.38%) |
| RequestCorrelationFilter | 22/31 (70.97%) | 7/10 (70.00%) |
| RequestCorrelationContext | 16/16 (100.00%) | 8/8 (100.00%) |
| CorrelatedTaskExecutor | 9/9 (100.00%) | No branches |
| RequestLoggingListener | 25/27 (92.59%) | 15/16 (93.75%) |
| SafeEvent | 4/4 (100.00%) | No branches |
| RequestLoggingConfiguration | 11/11 (100.00%) | No branches |

### Current Acceptance Evidence and Remaining Gaps

This record supersedes prior native-error failure statements for the currently executable fixture. Earlier execution sections and matrix statuses remain historical. The authorized scenario split now provides passing native error completion and genuine pre-commit 502 recovery evidence for H2/H8/P5/F2/F4. The complete current suite also supplies the previously missing full HTTP/business regression evidence for H3, plus fresh whole-project coverage. No new production-code workaround was required.

However, a green suite does not create assertions that are absent. The existing approved matrix still has these inspectable evidence gaps:

| Requirement | Still missing or partial evidence |
| --- | --- |
| U1 | Complete concurrent diagnostic-ownership and failure-isolation combinations. Existing advice/fallback diagnostic tests pass; they do not establish every concurrent combination. |
| H1/H5 | Real-container duplicate/oversized hostile-header variants; existing state/mock canary cases and ordinary real HTTP validation pass. |
| H7/F3/F5 | Complete real-container invalid-wrapper and hasOriginalRequestAndResponse contracts and remaining invalid-cycle permutations. Current restart identity, both overloads and unit exception/delegation tests pass. |
| H12/F6 | MDC put/remove/restoration fault injection. Existing normal restoration, failing MDC get, appender failure and observer-registration failure tests pass. |
| P1 | Independent post-commit response-failure case. Actual commit checkpoints, transaction unknown/rollback and HTTP completion separation pass, including async PostgreSQL cases. |
| F1 | Naturally generated nested container ERROR redispatch is not independently established; the current test exercises nested filter scope inside a genuine ERROR dispatch. |

These requirements were not waived by fixture stabilization. This task does not authorize expanding into another general test-hardening cycle. Their resolution or an explicit human scope decision remains necessary before claiming all mandatory Increment 6 acceptance evidence complete. No remaining failure of the two corrected async scenarios is claimed. Controlled I/O injection and conservative UNKNOWN remain qualified evidence, not universal client-disconnect detection or transport delivery acknowledgment.

### Historical PIT and Current SonarQube Status

The human-provided historical summary reported 697 generated, 572 killed, 82% mutation score and 86% test strength. Read-only inspection of the existing PIT-all-20261016T191406Z-YZl37K XML clarifies the console label: **566 KILLED, 6 TIMED_OUT, 93 SURVIVED, 32 NO_COVERAGE = 697**. The console's 572 detections include timeouts: 572/697 = 82.07%; strict KILLED/generated is 566/697 = 81.21%. Its 86% test-strength display is preserved as historical output. TIMED_OUT is not KILLED. This older run predates final fixture stabilization and is not proof of the current snapshot. No historical evidence file was changed; PIT was not rerun and no numeric mutation threshold was introduced.

SonarQube remains **Pending**: no current local analysis or external publication was executed. A clean regression build is not a SonarQube or mutation-testing result.

### Release Boundary, Budget and Git Safety

All executed stabilization and regression gates passed after one additional whitespace-only correction; **one planned correction plus one of one additional corrections used**. No further correction round or investigation was initiated. No production changes were made during this task. The current technical fixture failure is resolved; full Increment 6 acceptance readiness is withheld only for the matrix evidence gaps above. No human acceptance is recorded.

Whole-application production readiness remains separate: deployment configuration, secrets, database operations, backup/recovery, security and observability access/retention controls need their own evidence and authorization. No deployment preparation, persistent database migration, Compose action, infrastructure change, new dependency or CI change occurred.

Branch remains develop at `9345fa122df05fd5850ae183834f1b5c141cf3f7`. Only HttpRequestLifecycleIT.java and directly affected Increment 6 status/evidence documentation changed in this stabilization task; unrelated modified/untracked files are preserved. Nothing was staged, committed, pushed, merged, rebased or deployed. Complete current and task-only diffs, original file fingerprints, archived reports and final status are preserved with the execution evidence. Temporary evidence storage is not durable archival storage.

Final documentation/Git checks passed: git diff --check; untracked-file whitespace checks; 114 relative Markdown links/anchors and balanced fences; exact new prompt archival and preservation of all previous prompt bytes. Final working tree contains 13 modified and 14 untracked files, including pre-existing implementation changes; the index is empty and HEAD is unchanged. The only five files changed by stabilization are this playbook, its index, open-items.md, implementation-plan.md and HttpRequestLifecycleIT.java.

## Increment 6 — Human Freeze Decision — 2026-10-16

**Increment 6 — Functionally Stabilized, Frozen — Full Acceptance Pending**

The human explicitly freezes Increment 6 at its current verified implementation state. This is a conscious project-management decision, not confirmation that every original acceptance requirement is satisfied. No further implementation, remediation or quality-optimization work is authorized. This decision supersedes earlier active execution authorizations and blocked-status requests; all historical prompts, architecture decisions, failed attempts, correction budgets and execution records remain preserved.

### Preserved Verified Baseline

The following results are the existing final stabilization evidence, inspected without rerunning any build or test:

| Verification | Result |
| --- | --- |
| Surefire | 528 passed |
| Failsafe | 148 passed |
| Total | 676 passed |
| Failures / errors / skips | 0 / 0 / 0 |
| PostgreSQL integration tests | 136 passed |
| Servlet lifecycle tests | 12 passed |
| Spotless | Passed |
| Maven validate | Passed |
| JaCoCo lines | 1176/1213 — 96.95% |
| JaCoCo branches | 657/707 — 92.93% |

Both previously problematic scenarios pass: native unhandled async error returns HTTP 500 with one conservative UNKNOWN terminal event; pre-commit recovery returns HTTP 502, exact body `recovered`, and one COMPLETED_AFTER_ERROR terminal event. See the [final stabilization execution](#final-stabilization-execution-and-readiness--2026-10-16) for commands, XML evidence, status provenance and logging-specific coverage.

Historical PIT remains separate: 697 generated, 566 KILLED, 6 TIMED_OUT, 82% reported mutation score and 86% reported test strength. The reported detection percentage includes timeouts; TIMED_OUT is not KILLED. That run is a different execution snapshot, not fresh verification of the frozen implementation. **SonarQube remains Pending.**

### Outstanding Acceptance and Operational Limits

The freeze does not waive, downgrade or close the [detailed acceptance matrix](#requirement-to-evidence-traceability) or its [latest remaining-gap assessment](#current-acceptance-evidence-and-remaining-gaps). MDC mutation/restoration failures, invalid Servlet request-wrapper contracts, HTTP response failure after confirmed database commit and all other partially verified/unverified requirements remain visible obligations. Previously resolved async fixture failures remain resolved; missing evidence is not recast as a new failing test.

Operational logging retention/access controls, deployment security, production readiness and database backup/recovery remain separate outstanding risks and gates. Functional stabilization is supported by the preserved green baseline; full acceptance has not been granted; whole-application production readiness and deployment are not authorized. Increment 5 remains Reviewed – Accepted; ADR-0002/0006 remain Proposed and L1–L6 architecture decisions are unchanged.

### Reopening Conditions and Documentation-Only Scope

Only a new explicit human decision may reopen Increment 6. A future reopening must start from the existing acceptance-gap matrix and remaining risks, without restarting architecture planning. No automatic resumption of correction rounds or new implementation budget follows from this freeze.

This task changes only the playbook, playbook index, implementation plan and open-items status references. No source, tests, configuration, dependencies or historical evidence are changed. No build, test, formatter, PIT, SonarQube, Docker, PostgreSQL or deployment execution is performed. No staging, commit, push, merge, rebase or branch change is authorized.

## Separately Authorized SonarQube Remediation — 2026-10-17

The human explicitly requested fixing all current SonarQube issues after the freeze. This authorizes only the reported findings and their necessary regression verification, not general Increment 6 development, closure of outstanding acceptance gaps or production deployment. The current status remains **Functionally Stabilized, Frozen — Full Acceptance Pending**. All earlier prompts, freeze decisions and verification records are preserved.

At the clean develop revision `8b350c54b5532589cea60758c336f1a7cdafb260`, SonarQube MCP returned 31 OPEN/CONFIRMED issues for project uvormbrock_agentic-order-service, branch develop. The project key was verified from the existing GitHub workflow. All 31 reported findings were addressed locally without suppressions, issue-status changes, assertion removal or quality-threshold changes:

| Rule | Count | Correction |
| --- | --- | --- |
| S1165 | 1 | Make diagnostic ownership immutable constructor state. Backoff copies preserve reason, immediate cause, original stack and suppressed exceptions; the original failure remains unchanged. |
| S3776 | 1 | Extract rejection reason and backoff handling from retryAfterRollback. Transaction boundaries, classifier, lock ordering, attempts, budget and delays remain unchanged. |
| S3358 | 5 | Replace nested ternaries with named status/diagnostic/level and rejection helpers. |
| S1192 | 2 | Name the requestId MDC key and OTHER fallback literals. |
| S1186 | 8 | Document intentionally empty fixture lifecycle/transaction/appender hooks. |
| S5838 | 9 | Use equivalent typed AssertJ string, hash and Comparable assertions. |
| S5778 | 5 | Construct validated command input before exception assertions; only the intended operation remains inside each throwing lambda. |

The interrupted-backoff test additionally verifies diagnostic ownership and preservation of the original cause, stack trace and suppressed exceptions. The first full verification caught an immediate-cause regression from an extra wrapper; it was corrected without weakening the existing InterruptedException assertion. The following focused run passed 80 coordinator/retry-policy tests. Final Java 21 Maven Wrapper `clean verify` passed **528 Surefire and 148 Failsafe executions, 0 failures/errors/skips**. This includes the 12 Servlet lifecycle and 136 PostgreSQL executions. Spotless formatting was applied and its lifecycle check passed; git diff --check passed. No POM, dependencies, CI, migrations, locking/retry policy, persistent database or deployment configuration changed.

Fresh JaCoCo: **1188/1226 lines = 96.90%; 658/709 branches = 92.81%**. These counters belong to this remediation snapshot; the frozen baseline above remains historical evidence with its original counters. PIT was not rerun.

SonarQube MCP snippet analysis of all 13 changed Java files no longer reports the 31 fetched findings. Its pathless snippets emit an S1598 package/path artifact. The coordinator also reports the unchanged eight-parameter constructor S107; baseline snippet analysis confirms that warning already existed. These snippet limitations were not used to restructure unrelated code. A complete CI/SonarQube branch reanalysis remains Pending; no cloud-zero or passed-quality-gate claim is made, and no analysis was published.

Evidence is preserved at `/tmp/increment6-sonar-remediation-awjy37wh`: issue inventory/local checks, initial failed and final successful Maven logs, focused-test log, final XML and JaCoCo reports, complete Git diff and final status. This is temporary storage, not durable archival storage. Changes comprise 13 Java files and this execution-record addendum; the initial working tree was clean. No files were staged, committed or pushed. Remaining acceptance-matrix and operational limitations remain unchanged; implementation acceptance is Pending.
