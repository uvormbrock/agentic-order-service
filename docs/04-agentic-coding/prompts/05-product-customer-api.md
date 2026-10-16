# Increment 4 — Product & Customer Application Services and REST API

## Status

**Planning:** Finalized — Explicit Human Coding Authorization Recorded
**Implementation:** Reviewed – Accepted
**Human acceptance:** 2026-10-15 (Europe/Berlin)
**Recorded:** 2026-10-14

**Current acceptance evidence:** [Final Human Acceptance — 2026-10-15](#final-human-acceptance--2026-10-15). Earlier planning and execution statuses below are historical and superseded only for current implementation acceptance.

**Execution entry point:** [Current Implementation Prompt — Increment 4](#current-implementation-prompt--increment-4). Execute only after separate explicit human coding authorization; historical prompts are audit records.

**Execution precedence:** Only [Current Implementation Prompt — Increment 4](#current-implementation-prompt--increment-4) is executable after explicit human coding authorization. All other planning prompts, design sketches, test matrices and earlier future-implementation instructions are non-executable supporting/history records. The current prompt supersedes historical DynamicUpdate, unversioned/column-scoped writes, no-migration and extra MVC-test-dependency proposals; those records cannot override its authorized scope.

## Objective

Prepare the ten Product/Customer operations for review, reusing the accepted Increment 3 domain and persistence design. Separate accepted requirements, proposed mechanisms, implementation authorization, automated evidence and human acceptance.

## Context

Read [AGENTS.md](../../../AGENTS.md), [PRD](../../01-requirements/product-requirements.md), [system architecture](../../02-architecture/system-architecture.md), [domain model](../../02-architecture/domain-model.md), [REST design](../../02-architecture/rest-api-design.md), [transaction conventions](../../02-architecture/transaction-locking.md), [technology reference](../../02-architecture/technology-stack.md), [coding](../coding-guidelines.md), [Java/Spring](../java-spring-guidelines.md), [persistence](../persistence-guidelines.md), [testing](../../05-testing/testing-strategy.md), [implementation plan](../../07-implementation/implementation-plan.md) and [open items](../../00-overview/open-items.md). Relevant ADRs are linked by the system architecture; ADR-0002 remains Proposed/unverified and D1–D10 Variant A remain Accepted.

Increments 1–3 have human acceptance. Inspected implementation already provides Product/Customer generated UUIDs, controlled update/deactivate operations, immutable Money/EmailAddress, converters, restricted repositories (save/findById), V1 constraints and PostgreSQL fixtures. Reuse them rather than recreating domain rules. Current source uses generic domain validation exceptions; it has no application services, controllers or HTTP DTOs. The existing database index `uq_customers_email` covers all customers. Current Boot-managed JUnit 6, Maven Wrapper 3.9.9, PostgreSQL 17.11, approved security overrides, Surefire/Failsafe, JaCoCo, GitHub Actions and SonarQube must remain intact.

## Approved Scope

This task approves **documentation and planning only**: the API error catalog, this playbook, REST design, implementation plan, open-items index and playbook navigation. No implementation approval is inferred from the endpoint scope or accepted directions below.

Accepted directions A–D: global strict JSON; specific machine-readable error codes; defer customer DELETE without removing it from the MVP; mandatory unit, actual HTTP/MVC and PostgreSQL testing. Exactly ten intended endpoints are listed below. Exclude Order APIs/placement, customer deletion implementation, product deletion/reactivation, locking, retries, transaction coordinator and other business operations. No migration execution is authorized now; the current future implementation prompt includes V2 after separate coding authorization and supersedes the historical no-migration proposal.

## Prompt

### Original Planning Prompt — Immutable Historical Record

The complete human-supplied planning specification follows verbatim. Record later approved revisions separately; never rewrite this block to describe subsequent execution.

````text
# Increment 4 — Product & Customer REST API: Documentation and Implementation Planning

## Role and Objective

You are acting as a senior Java software architect in the existing **Agentic Order Service** repository.

Increment 1 (Technical Bootstrap), Increment 2 (PostgreSQL Foundation), and Increment 3 (Domain Model & Persistence) have been reviewed and accepted.

Your task is to prepare **Increment 4 — Product & Customer Application Services and REST API** for human architecture review and subsequent implementation approval.

**This is a documentation-only planning task. Do not implement application code.**

Follow `AGENTS.md` and all applicable architectural, coding, persistence, testing, and agentic-coding guidelines.

Do not assume that this prompt overrides existing accepted architectural decisions.

## 1. Read and Review Existing Documentation

Read at minimum:

- `AGENTS.md`
- `docs/00-overview/open-items.md`
- `docs/01-requirements/product-requirements.md`
- `docs/02-architecture/system-architecture.md`
- `docs/02-architecture/domain-model.md`
- `docs/02-architecture/rest-api-design.md`
- `docs/02-architecture/technology-stack.md`
- Relevant ADRs, especially ADR-0001 through ADR-0005
- `docs/04-agentic-coding/coding-guidelines.md`
- `docs/04-agentic-coding/java-spring-guidelines.md`
- `docs/04-agentic-coding/persistence-guidelines.md`
- `docs/05-testing/testing-strategy.md`
- `docs/07-implementation/implementation-plan.md`

Inspect the existing Increment 3 implementation and tests to identify reusable aggregates, value objects, repositories, domain operations, and validation rules.

Do not reopen accepted D1–D10 decisions or introduce conflicting behavior.

## 2. Approved Increment 4 Scope

Increment 4 covers **exactly these ten REST endpoints**.

### Product API

| Method | Endpoint | Behavior |
|---|---|---|
| POST | `/products` | Create an initially active product |
| GET | `/products` | List all products, including inactive |
| GET | `/products/{id}` | Retrieve a product by ID |
| PUT | `/products/{id}` | Update editable product details |
| PATCH | `/products/{id}` | Deactivate a product |

### Customer API

| Method | Endpoint | Behavior |
|---|---|---|
| POST | `/customers` | Create an initially active customer |
| GET | `/customers` | List all customers, including inactive |
| GET | `/customers/{id}` | Retrieve a customer by ID |
| PUT | `/customers/{id}` | Update editable customer details |
| PATCH | `/customers/{id}` | Deactivate a customer |

Implementing these operations must eventually satisfy the existing PRD, including its documented success statuses and response fields.

### Explicitly Excluded

Do not plan implementation of:

- `DELETE /customers/{id}` in Increment 4
- Order creation, retrieval, or listing
- Order Placement orchestration
- Pessimistic locking or retry orchestration
- ADR-0002 concurrency protocol
- Product deletion
- Reactivation of inactive records
- Additional business operations not present in the PRD

**Important:** Customer Delete remains an MVP requirement and is deferred, not canceled.

Document the scope limitation and the expected interim behavior of unsupported HTTP methods without silently altering the PRD.

## 3. Accepted Decision A — Global Strict JSON Deserialization

The user has approved centrally configured strict JSON deserialization.

Requirements:

- Reject unknown JSON properties with HTTP 400.
- Reject read-only identifiers in request bodies.
- Reject unsupported activation fields.
- Reject unsupported properties in nested request objects.
- Never deserialize HTTP requests directly into JPA entities or domain aggregates.
- Use dedicated request and response DTOs.
- Preserve server-authoritative business rules.
- Explicitly test numeric type handling and coercion, including invalid product prices.

Review the concrete configuration mechanism for **Spring Boot 4.1.1 and its selected Jackson stack**.

Do not assume Spring Boot 3.x or Jackson 2.x configuration behavior.

Identify exact configuration options and necessary dependencies, if any, but do not change dependencies or implement configuration yet.

Distinguish accepted HTTP behavior from technical implementation choices still requiring human approval.

## 4. Accepted Decision B — Specific API Error Codes

The user has approved **specific, machine-readable error codes**, not a minimal set of generic codes.

Create a dedicated documentation file:

`docs/02-architecture/api-error-codes.md`

This file must become the central reference for REST API error responses.

Use a consistent minimum JSON envelope:

```json
{
  "code": "INVALID_PRODUCT_PRICE",
  "message": "The product price must be between EUR 0.00 and EUR 999,999.99."
}
```

The PRD remains authoritative for HTTP status codes.

### Proposed Error-Code Catalog

Document and review the following initial catalog:

| Error code | HTTP status | Meaning |
|---|---|---|
| `MALFORMED_JSON` | 400 | Invalid JSON syntax or structure |
| `UNKNOWN_FIELD` | 400 | Unknown request property |
| `INVALID_FIELD_TYPE` | 400 | Incorrect JSON field type |
| `REQUIRED_FIELD_MISSING` | 400 | Required request field missing or null |
| `INVALID_NAME` | 400 | Invalid customer or product name |
| `INVALID_EMAIL` | 400 | Invalid customer email |
| `INVALID_PRODUCT_PRICE` | 400 | Price outside accepted range or precision |
| `UNSUPPORTED_FIELD` | 400 | Recognized but prohibited request field |
| `PRODUCT_NOT_FOUND` | 404 | Requested product does not exist |
| `CUSTOMER_NOT_FOUND` | 404 | Requested customer does not exist |
| `DUPLICATE_EMAIL` | 409 | Normalized email already belongs to another customer |
| `METHOD_NOT_ALLOWED` | 405 | Unsupported HTTP method for a resource |

These names are a **proposed technical catalog**, not blanket approval of every code and edge-case mapping.

For each error code document:

1. Exact code name
2. HTTP status
3. Triggering conditions
4. Safe example response
5. Relevant endpoint(s)
6. Relevant PRD requirement or acceptance criterion
7. Any ambiguous or unresolved mapping

Review whether additional codes are needed for Increment 4, including invalid UUID path parameters, rejected reactivation, malformed or incomplete PATCH requests, and unsupported request fields.

Do not invent error codes for future Order Placement or uncertain transaction outcomes. These belong to later decisions.

### Error Handling Architecture

Propose central exception-to-HTTP mapping at the API boundary, using Spring MVC mechanisms compatible with Boot 4.1.1.

Domain and application layers must not depend on HTTP status codes.

Distinguish:

- Request parsing and binding errors
- Bean Validation errors
- Domain validation errors
- Missing resources
- Business conflicts
- Persistence constraint violations

A known normalized-email uniqueness violation must map to HTTP 409 even when detected by PostgreSQL rather than an application-level precheck.

Do not classify every database exception as a business conflict.

Never disclose SQL statements, stack traces, internal infrastructure details, or sensitive diagnostics in public error messages.

Keep technical failure responses and uncertain commit outcomes explicitly unresolved where the accepted architecture requires further decisions.

## 5. Accepted Decision C — Defer Customer Delete

`DELETE /customers/{id}` is excluded from Increment 4.

Reason: Correct handling of customers referenced by orders requires concurrency-safe coordination with the future Order Placement transaction policy.

Do not introduce a temporary check-then-delete implementation.

Do not change the accepted MVP requirement.

Record the deferral clearly in the implementation plan and open-items documentation, including the relationship to ADR-0001 and proposed ADR-0002.

## 6. Accepted Decision D — Three Mandatory Testing Levels

All three levels are required:

### Level 1 — Unit Tests

Test application-service operations and domain/application error handling in isolation.

Include normal cases, boundary cases, invalid inputs, and inactive-record behavior.

### Level 2 — HTTP/API Tests

Exercise actual Spring MVC request handling, JSON binding, validation, response DTOs, status codes, headers, and the centralized error envelope.

Do not treat direct controller-method invocation as sufficient proof of the HTTP contract.

### Level 3 — PostgreSQL Integration Tests

Use the existing PostgreSQL/Testcontainers infrastructure.

Verify real persistence behavior, uniqueness constraints, updates, deactivation, transactions, and absence of unintended state changes after rejected requests.

Use the existing JUnit 6 setup and Boot-managed versions.

### Mandatory Test Matrix

Prepare a comprehensive test matrix for all ten endpoints.

Include at least:

- Successful creation and server-generated IDs
- Correct `201 Created` and `Location` headers
- Individual retrieval and complete response fields
- Listing active and inactive records
- Empty lists returning `[]`
- Correct `200 OK` updates
- PUT replacing editable details without reactivation
- Correct PATCH deactivation
- Repeated deactivation being idempotent
- Missing and unknown resource IDs
- Missing required fields
- Unknown JSON fields
- Client-assigned IDs
- Unsupported `active` fields
- Attempts to reactivate records
- Incorrect JSON field types and numeric coercion
- Valid and invalid name boundaries using Unicode code points
- `String.strip()` name normalization
- Email normalization with `Locale.ROOT`
- Email syntax and 254-code-point limit
- Case-insensitive uniqueness across active and inactive customers
- Preservation of provider-specific email dots and plus tags
- Product price boundaries and strict two-decimal-place enforcement
- Valid EUR 0.00 and EUR 999,999.99
- Rejection of negative, excessive, or overprecision prices
- Consistent error `code` and `message` in every error response
- No unintended database changes for rejected requests

Preserve the existing Maven Surefire/Failsafe separation, JaCoCo, GitHub Actions, and SonarQube Cloud integration.

Do not weaken existing tests or quality gates.

Coverage remains an objective metric, not a substitute for meaningful test cases.

## 7. Design and Implementation Planning

Prepare the Increment 4 architecture and implementation scope.

Define:

- Application-service responsibilities
- Proposed controller boundaries
- DTO types and mappings
- Bean Validation responsibilities
- Domain validation responsibilities
- Error and exception translation responsibilities
- Repository access patterns
- Transaction boundaries for Product and Customer operations
- Expected changes to existing code
- Expected new classes and packages
- Explicit exclusions and deferred concerns

Avoid generic CRUD abstractions and unnecessary framework layers.

Prefer feature-oriented organization consistent with the existing modular monolith.

Do not add dependencies or migrations without explicit approval.

Do not change accepted ADR statuses.

## 8. Required Documentation Deliverables

Create or update the following documentation, preserving existing accepted content:

**A. New file:**

`docs/02-architecture/api-error-codes.md`

A detailed, reviewable error-code catalog, including examples, trigger conditions, mappings, and unresolved questions.

**B. New file:**

`docs/04-agentic-coding/prompts/05-product-customer-api.md`

A self-contained Increment 4 playbook that separates planning, approval, implementation, verification, and human review.

The future implementation instructions must remain explicitly **Pending Human Approval**.

**C. Update:**

`docs/02-architecture/rest-api-design.md`

Incorporate the accepted direction for global strict JSON handling and specific error codes. Cross-reference the new error-code catalog.

Do not misrepresent proposed technical mappings as accepted.

**D. Update:**

`docs/07-implementation/implementation-plan.md`

Add a current, clearly identified Increment 4 proposal.

Do not confuse the historical Proposed Stages table with the current increment numbering.

**E. Update:**

`docs/00-overview/open-items.md`

Record remaining technical decisions and the deferred Customer Delete work.

**F. Update if necessary:**

`docs/04-agentic-coding/prompts/00-index.md`

Register the new playbook.

Do not modify unrelated files.

## 9. Documentation Quality Requirements

All documentation must be written in English.

Ensure that:

- Approved decisions are clearly distinguished from proposals.
- The PRD is authoritative.
- No new business rule is silently introduced.
- Earlier accepted Increment 3 decisions remain unchanged.
- Internal links are valid.
- No contradictory API status mappings are introduced.
- Every planned operation is traceable to the PRD.
- No endpoint is silently added or omitted.
- The new error-code document is discoverable from the API design and agentic-coding playbook.

If a decision cannot be derived from accepted requirements, identify it as **Open — Human Decision Required** rather than guessing.

## 10. Execution Restrictions

This task authorizes documentation and planning only.

Do not:

- Implement Java application or test code
- Change `pom.xml`
- Change GitHub Actions
- Modify Flyway migrations
- Add dependencies
- Execute production migrations
- Change ADR statuses
- Mark Increment 4 as accepted
- Commit or push changes

## 11. Final Report

At completion, report:

1. Files created and modified
2. Summary of the proposed Increment 4 architecture
3. Confirmation of all ten endpoints
4. Proposed error-code catalog and unresolved mappings
5. Complete planned test matrix summary
6. Decisions requiring human approval
7. Documentation consistency checks performed
8. Actual verification results, with no unverified success claims
9. Confirmation that no implementation code, migration, dependency, or commit was changed

Finish with the status:

**Increment 4 — Documentation Prepared, Pending Human Architecture Review and Approval**

Wait for explicit human approval before implementing Increment 4.
````

## Approved Architecture and API Decisions — 2026-10-14

The human's subsequent documentation-update specification explicitly accepts A and B1–B11. This record supersedes the initial Proposed design only for those decisions. The original planning prompt above and initial review evidence below remain immutable history. Increment 3 remains Reviewed – Accepted; D1–D10 and existing API fields/business rules remain intact. No implementation is authorized.

| Decision | Human-accepted direction | Remaining implementation gate |
| --- | --- | --- |
| A / B2 | Central strict JSON: unknown/forbidden fields rejected recursively, dedicated DTOs, string/number/boolean types without coercion; required missing/null distinguished | Boot 4.1.1/Jackson 3.1.7 exact processing and configuration evidence |
| B1 | Canonical 36-character UUID paths; INVALID_RESOURCE_ID/400 vs valid absent feature NOT_FOUND/404 | D5/D6 now resolve hex case/outer ordering; adapter and HTTP proof Pending |
| B3 | Price JSON numbers in ordinary decimal notation; reject exponent notation and more than two supplied decimal places; BigDecimal with no float/double conversion | Preserve token lexemes through parsing; Money/D6 unchanged |
| B4 | PATCH only {"active":false}; approved missing/null/type/true/extra-field mappings; repeated deactivation/200; no rejected mutation | HTTP binding and committed-state verification |
| B5 | One code/message; syntax/root → unknown/forbidden → type → required → business value → lookup/conflict | D4 selects field/path order; deterministic processing proof Pending |
| B6 / B10 | Catalog 415/406/route 404/technical 500; 503 only classified temporary outage; safe JSON messages, no automatic replay or uncertain-outcome rollback claim | Central rendering including incompatible Accept/error dispatch; outage and commit-cause classifier |
| B7 | Interim Customer DELETE uses OPERATION_NOT_AVAILABLE/405, fixed message and accurate Allow; future MVP DELETE/204 retained | No deletion/reference check now; future coordination with Order Placement |
| B8 / B9 | Product/Customer @Version; confirmed conflict CONCURRENT_MODIFICATION/409; no silent write retry | Complete ADR-0006 Proposed; mapping/schema/no-op choices now finalized below; SQL/implementation/runtime evidence pending; no ETag/If-Match |
| B11 | Application writes @Transactional, reads readOnly=true; immutable results inside transaction; API translation/success outside completion | Proxy/self-invocation, flush/commit failures and no lazy serialization proof |

The [central catalog](../../02-architecture/api-error-codes.md) is authoritative for accepted names/statuses/messages and category precedence; the [REST design](../../02-architecture/rest-api-design.md#10-human-accepted-increment-4-api-and-transaction-decisions) owns the detailed input contract. [ADR-0006](../../03-decisions/ADR-0006-product-customer-optimistic-locking.md) is **Proposed**, with human-approved @Version and finalized technical choices, but implementation/runtime verification Pending. Existing ADR-0002 is unchanged and Proposed/unverified.

## Finalized ADR-0006 Technical Decisions — 2026-10-14

Human Accepted three additional details: exact boxed Long @Version/@Column(name="version", nullable=false) field on Product/Customer, Hibernate-only version lifecycle/no public setter/client/DTO property; V2 BIGINT/final NOT NULL, existing rows 0, temporary default 0 permitted then removed, no final database default; sequential inactive PATCH/200 with unchanged representation and no UPDATE/version increment. These replace the preceding candidate mapping/default/no-op questions, without rewriting the original planning prompt or historical initial proposal. UUID identity/reference equality and D1–D10 remain intact.

[ADR-0006](../../03-decisions/ADR-0006-product-customer-optimistic-locking.md) stays Proposed as a complete record; implementation/runtime verification and reviewed V2 proposal runtime verification remain pending. No Java/SQL file or migration execution is authorized. Production deployment/permissions/rollout, Order Placement ADR-0002 and stale HTTP client preconditions remain separate/unresolved. No automatic retry.

## Final Edge Decisions D1–D8 — Human Accepted

Recorded 2026-10-14 after the final technical review. These identifiers refer to Increment 4 request/API edge decisions, not Increment 3 D1–D10 persistence decisions. The [REST decision record](../../02-architecture/rest-api-design.md#11-final-human-decisions-d1d8) owns their details: body classifications, duplicate/trailing rejection, fixed same-category order, mixed UUID hex case, HTTP→path→body→application order, application/json UTF-8 only and narrow database failure classification. The eight choices are resolved; runtime proof remains Pending. The complete ADR-0006 stays Proposed. The following prompt supersedes earlier DynamicUpdate/no-migration/new-test-starter proposals without rewriting their historical text.

## Current Implementation Prompt — Increment 4

### Authorization, Authority and Status

**Pending Human Implementation Approval. Do not execute this prompt until the human explicitly authorizes Increment 4 coding.** Documentation approval, accepted technical details and this prompt do not grant execution permission. Complete ADR-0006 acceptance is separate; keep ADR-0002/ADR-0006 Proposed until explicit status approval. After authorized implementation, use Executed – Pending Human Review until explicit acceptance. No commit, push or merge without separate authorization. Once explicitly authorized, execute the full current scope: ten endpoints, application services/results/DTOs, strict input/error handling, Product/Customer version mappings, reviewed V2, authorized development/test verification, new/adapted tests and necessary execution-evidence documentation. Do not request another general planning or coding approval for these steps. Complete ADR-0006 acceptance is a separate status decision, not an additional coding prerequisite for the expressly approved choices. The persistent-database safety gate below is a specific exception; a genuine accepted-policy conflict or potentially destructive operation requires stopping the affected work. Authorization excludes production deployment/migration, destructive cleanup, V1/POM/dependency/CI/quality-gate/Sonar changes, Order functionality, Customer DELETE implementation, automatic write retries, D1–D10 changes and ADR status changes.

Read AGENTS.md, PRD, system/domain/transaction/REST/technology documents, ADR-0001 through ADR-0006, coding/Java-Spring/persistence guidelines, testing strategy and current plan/open items. Preserve Increment 3 Reviewed – Accepted and D1–D10. This is the definitive current execution instruction; earlier DynamicUpdate, column-scoped-only writes, no-migration and new-MVC-test-starter proposals are superseded. Original planning prompts/history remain immutable. Stop only for a genuine conflict with accepted policy or an unsupported technical requirement; do not reopen resolved decisions.

Keep Java 21, Boot 4.1.1, Maven Wrapper 3.9.9, Boot-managed JUnit 6, PostgreSQL 17.11/Testcontainers and existing security overrides. Do not change dependencies/POM, CI, JaCoCo, SonarQube, V1 or PRD.

### Exactly Ten Business Endpoints

| Method | Path | Accepted behavior / success |
| --- | --- | --- |
| POST | /products | Required name/price, generated UUID, active; 201 Product and Location /products/{id} |
| GET | /products | 200 array of all active/inactive Products; [] if empty |
| GET | /products/{id} | 200 Product including inactive; canonical absent UUID → PRODUCT_NOT_FOUND/404 |
| PUT | /products/{id} | Required name/price replace editable details; preserve UUID/activity; 200 Product |
| PATCH | /products/{id} | Only {"active":false}; 200 inactive Product; sequential repeat is unchanged/no UPDATE/no version increment |
| POST | /customers | Required name/email, generated UUID, active; 201 Customer and Location /customers/{id} |
| GET | /customers | 200 array of all active/inactive Customers; [] if empty |
| GET | /customers/{id} | 200 Customer including inactive; canonical absent UUID → CUSTOMER_NOT_FOUND/404 |
| PUT | /customers/{id} | Required name/email replace editable details; preserve UUID/activity; 200 Customer |
| PATCH | /customers/{id} | Only {"active":false}; 200 inactive Customer; sequential repeat is unchanged/no UPDATE/no version increment |

Product response fields: id/name/price/active. Customer: id/name/email/active. Same fields in lists and mutation results. Prices are gross EUR JSON numbers, not serialized Money graphs. No version field or managed entities/proxies exposed.

Customer DELETE is deferred, not canceled: interim DELETE /customers/{id} → 405 OPERATION_NOT_AVAILABLE, message "Customer deletion is not available in this API version.", accurate Allow, no resource/order lookup or deletion logic. Future MVP still requires delete-if-unreferenced/otherwise-deactivate and 204. Product DELETE/other unsupported methods use METHOD_NOT_ALLOWED. Do not register a successful DELETE business handler or advertise DELETE in Allow. Include actual implicit HEAD/OPTIONS support when deriving Allow.

Exclude Order APIs/placement, customer deletion coordination, explicit pessimistic locks, TransactionTemplate/ambient guard/retries, ADR-0002 protocol, reactivation, physical product deletion, production migrations/rollout, ETag/If-Match, new pagination/sorting/filter guarantees and additional business functions.

### Business Validation — Preserve Existing Domain Rules

Names: required String.strip(), 1–200 Unicode code points, preserve valid Unicode/case/umlauts/accents; supplementary characters count once. Preserve the existing domain rejection of invalid/unpaired surrogates. No ASCII-only or lowercase name policy.

Email: required String.strip() then Locale.ROOT lowercase, at most 254 normalized code points, approved ASCII grammar with exactly one @. Local part permits ASCII letters/digits/dots and punctuation below, with no leading/trailing/consecutive dots. Domain requires at least two nonempty ASCII letter/digit labels; hyphens only internally. No quoted/internationalized email, DNS check or extra independent local/label limits. Preserve literal dots/plus tags and uniqueness across active/inactive customers; same owner's unchanged normalized email is allowed.

```text
Allowed local punctuation: !#$%&'*+-/=?^_`{|}~
```

Price: numeric JSON in ordinary decimal notation, EUR 0.00–999999.99 inclusive, at most two explicitly supplied decimal places; BigDecimal throughout. Accept 149, 149.9, 149.99; reject 149.990, 10.000, 1.4999e2 and 1e3 as INVALID_PRODUCT_PRICE. Quoted price is INVALID_FIELD_TYPE. Preserve numeric lexemes before any rounding/canonicalization, never convert through float/double or strip trailing zeros to accept invalid input. Reuse Money/EmailAddress and aggregate operations; do not change accepted Money/D6 behavior.

PATCH: missing/null active → REQUIRED_FIELD_MISSING; true → UNSUPPORTED_FIELD; string/number active → INVALID_FIELD_TYPE; recognized editable/read-only extra fields → UNSUPPORTED_FIELD; random extras → UNKNOWN_FIELD. Read-only id/version and active in POST/PUT are rejected. Rejected requests cause no mutation. Concurrent writes may produce confirmed 409 conflicts without automatic retry; sequential repeated inactive PATCH must succeed/no-op.

### Strict Input Pipeline and Final Eight Decisions

Use dedicated request/response records, a small central strict request decoder and an MVC read converter supporting only the request DTOs. No domain/JPA request binding and no general JSON/schema framework.

Accepted outer order:

1. HTTP routing/method/relevant Content-Type and Accept checks.
2. Path syntax validation, before potentially lenient UUID conversion and before body argument decoding.
3. Complete body validation below.
4. Application resource lookup and business behavior.

Register ApiRequestInterceptor.preHandle for the API boundary: check relevant media/Accept/UTF-8 requirements, then canonical path syntax before argument conversion. Routing/method resolution precedes this hook; method/routing failures use the central error boundary. Validating UUIDs only inside a controller after @RequestBody conversion is too late. No resource lookup in these transport hooks.

Final edge policies:

- POST/PUT/PATCH no body → REQUIRED_FIELD_MISSING/400; whitespace-only → MALFORMED_JSON/400; JSON null/array root → MALFORMED_JSON/400; {} → REQUIRED_FIELD_MISSING/400.
- Duplicate object keys at every depth → MALFORMED_JSON/400, detected before tree construction loses them.
- After the root object only JSON whitespace is allowed; another value/nonwhitespace content → MALFORMED_JSON/400.
- Fixed field order: Product name then price; Customer name then email; PATCH active. Multiple unknown/forbidden paths ordered lexicographically.
- UUID text must be 36 characters with hex groups 8-4-4-4-12; upper/lower/mixed-case hex accepted, serialize lowercase. No shortened parsing. Malformed path → INVALID_RESOURCE_ID/400; valid missing resource → feature NOT_FOUND/404.
- Write media is application/json encoded in UTF-8, optional charset=UTF-8. Reject other media/encodings, including merge-patch+json/vendor+json/other application/*+json, with 415. Do not inherit a permissive +json fallback or auto-detect a different body encoding. Successful JSON uses application/json; safe error JSON still applies to incompatible Accept/406.
- Strict String/number/Boolean tokens, no scalar coercion. Missing/null required fields are separate from wrong types.
- Confirmed connection establishment failure or confirmed pre-write unavailability → 503; unknown commit or unclassified technical failure → 500; confirmed optimistic conflict and known uniqueness → their distinct 409 codes. Classify typed causes and phase, not all SQL/timeouts/connections.

D1 body examples apply after routing/header/path checks pass; combined errors follow the accepted outer order rather than choosing the body error first. Use RequestBodyAdviceAdapter.handleEmptyBody, scoped to the dedicated write DTOs, to raise a typed REQUIRED_FIELD_MISSING for a genuinely absent body: MVC may bypass a converter for empty input. Do not parse the framework missing-body exception message or let JSON null become an absent-body result. Whitespace-only input remains malformed.

Body precedence (independent of property order): syntax/root (including duplicates/trailing) → unknown/forbidden properties → invalid field types → missing/null required → invalid business values → application lookup/conflicts. Fully parse before selecting a structural/type error. Capture numeric text through Jackson streaming and construct exact-decimal tree/DTO values without an intermediate floating-point representation. Perform all field validation before service invocation; approved domain invariants remain independently protected through aggregate methods. Empty strings reach name/email business validation, not a scalar-coercion substitute.

Keep global unknown/coercion Jackson safeguards, but do not rely on fail-fast binding/Bean Validation ordering as proof. Wire Jackson safeguards through JsonMapperBuilderCustomizer and register StrictRequestHttpMessageConverter through ServerHttpMessageConvertersCustomizer/ServerBuilder.addCustomConverter. Preserve Boot defaults for responses: the strict converter must report canWrite=false for every type and precede the ordinary reader only for the dedicated write DTOs. Do not replace or clear the default converter list. Explicitly prove exact Boot 4.1.1/Jackson 3.1.7 wiring and recursive unknown-field rejection when a nested DTO is introduced; current test-only nested cases do not add an Order endpoint.

### Complete Accepted API Error Catalog

Exactly one code/message JSON object; no error array or additional response fields. Use the safe fixed messages below. Do not expose SQL, constraints, stack traces, parser internals, sensitive input or exception messages. Domain/application failures remain HTTP-independent; central API translation owns status/envelope.

| Code | HTTP | Trigger | Safe message |
| --- | --- | --- | --- |
| `MALFORMED_JSON` | 400 | Invalid syntax/root, whitespace-only/null/array root, duplicate keys at any depth or nonwhitespace trailing content; P/C POST/PUT/PATCH | The request body must be a valid JSON object. |
| `UNKNOWN_FIELD` | 400 | Unknown property in a request DTO, recursively when nested DTOs exist; P/C POST/PUT/PATCH | The request contains an unknown field. |
| `UNSUPPORTED_FIELD` | 400 | Recognized forbidden property: id/version; active in POST/PUT; editable details in PATCH; PATCH active=true | The request contains a field or activation value that this operation does not permit. |
| `INVALID_FIELD_TYPE` | 400 | Wrong token type: numeric/boolean name/email, string/boolean price, string/number active, wrong field object/array shape; P/C writes | A request field has an invalid JSON type. |
| `REQUIRED_FIELD_MISSING` | 400 | Absent body/{} or missing/null name or price (P POST/PUT), name or email (C POST/PUT), active (P/C PATCH) | A required request field is missing or null. |
| `INVALID_NAME` | 400 | Textual name fails accepted String.strip()/1–200 Unicode-code-point policy; P/C POST/PUT | The name must contain 1 to 200 Unicode code points after stripping surrounding whitespace. |
| `INVALID_EMAIL` | 400 | Textual email fails approved ASCII grammar or normalized 254-code-point limit; C POST/PUT | The email must satisfy the documented email policy and normalization limit. |
| `INVALID_PRODUCT_PRICE` | 400 | Numeric price outside EUR 0.00–999,999.99, more than two supplied decimal places or exponent notation; P POST/PUT | The product price must be between EUR 0.00 and EUR 999,999.99, use ordinary decimal notation and have at most two decimal places. |
| `INVALID_RESOURCE_ID` | 400 | Noncanonical/malformed UUID path text; P/C individual GET/PUT/PATCH | The resource identifier must use canonical UUID text representation. |
| `PRODUCT_NOT_FOUND` | 404 | Valid canonical UUID identifies no stored Product; P individual GET/PUT/PATCH | The product was not found. |
| `CUSTOMER_NOT_FOUND` | 404 | Valid canonical UUID identifies no stored Customer; C individual GET/PUT/PATCH | The customer was not found. |
| `ENDPOINT_NOT_FOUND` | 404 | No matching endpoint; API routing boundary | The requested endpoint was not found. |
| `METHOD_NOT_ALLOWED` | 405 | Unsupported method on implemented resources, including Product DELETE; excludes deferred Customer DELETE | This HTTP method is not supported for this resource. |
| `OPERATION_NOT_AVAILABLE` | 405 | DELETE /customers/{id} in the interim API version, without deletion/reference-check logic | Customer deletion is not available in this API version. |
| `NOT_ACCEPTABLE` | 406 | Requested success representation cannot be supplied under Accept; API boundary | The requested response representation is not supported. |
| `DUPLICATE_EMAIL` | 409 | Another active/inactive customer owns the normalized email; C POST/PUT, including confirmed uq_customers_email/23505 at flush/commit | The email address already belongs to another customer. |
| `CONCURRENT_MODIFICATION` | 409 | Confirmed JPA optimistic-lock conflict for P/C PUT/PATCH, including completion failures | The resource was modified concurrently. Please reload and retry. |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Unsupported request Content-Type on API writes | The request media type is not supported. |
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected technical/service/persistence failure, including uncertain completion when a response is possible | An unexpected server error occurred. |
| `SERVICE_UNAVAILABLE` | 503 | Clearly classified temporary dependency outage; applicable API operations | A required service is temporarily unavailable. |

ApiExceptionHandler, ApiErrorResponseWriter and ApiErrorController provide central typed advice, direct JSON writing and servlet error-dispatch fallback. They must handle parsing/binding/validation, routing/method/media/Accept and service completion failures. Write the fixed code/message envelope and status directly through the shared writer with Content-Type application/json, without invoking negotiated response converters for errors or recursively negotiating an error against incompatible Accept; no HTML/default ProblemDetail fallback. Preserve Location/Allow where appropriate. A committed/lost HTTP connection may prevent a response; never promise a body in that case.

### Application, Transactions and Persistence

Feature controllers call only their application operations, never repositories. Application owns own-feature domain/repositories, returns transport-neutral immutable results and never imports API DTOs. Constructor injection; no foreign persistence, generic CRUD bases or recursive/circular service wiring.

Expose externally proxied public application write methods with @Transactional; get/list with @Transactional(readOnly=true). No controller transaction, no Spring transaction API in domain, no self-invoked annotation substitute. HTTP invocation must reach the application-owned outer transaction. Construct immutable results within it, allow flush/commit completion, and only then produce HTTP success; serialization must do no lazy database access with OSIV disabled. Existing repositories retain save/findById and add only findAll as needed; create via repository save, managed updates via controlled domain operations/dirty checking.

Use narrow cause-chain/SQLException-chain classifiers outside the application transaction: confirmed 23505 plus structured uq_customers_email → DUPLICATE_EMAIL; confirmed JPA/Spring optimistic-lock failures, including flush/commit wrappers → CONCURRENT_MODIFICATION. No localized-message parsing, broad IllegalArgumentException-to-400, broad integrity-to-409 or connection-to-503. Confirmed establishment/pre-write outages use 503; ambiguous completion/unclassified failures use 500. Let failures end the transaction; never continue a rollback-only context or automatically retry writes. Known rollback permits no-change assertions; lost commit confirmation does not.

### Exact Version Mapping and Reviewed V2

Apply to Product/Customer only:

```java
@Version
@Column(name = "version", nullable = false)
private Long version;
```

No manual primitive/zero initialization: new boxed version starts null for provider/newness handling; Hibernate owns initialization/increments. No public setter/client/DTO version. Preserve UUID generation/reference equality/converters/domain rules/D1–D10. No Order/OrderItem version or ETag/If-Match; entity versions do not detect every stale HTTP client.

Create src/main/resources/db/migration/V2__add_product_customer_versions.sql after execution authorization, using this reviewed proposal:

```sql
ALTER TABLE products
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE customers
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE products
    ALTER COLUMN version DROP DEFAULT;

ALTER TABLE customers
    ALTER COLUMN version DROP DEFAULT;
```

After explicit coding authorization, create the reviewed V2 and execute it in disposable, project-managed Testcontainers databases using the existing PostgreSQL 17.11 image/infrastructure. These disposable databases do not require a persistent-database backup.

#### Persistent Local Database Safety Gate

Before any migration of an existing persistent local database (including automatic Flyway execution during local-profile startup):

1. Identify the exact target database/environment from actual repository configuration and runtime evidence; confirm local development, not production or an unknown remote target. Do not infer identity from a profile name alone.
2. Inspect its Flyway history and schema read-only; verify expected V1 state/checksum and compatibility. Stop affected migration on an unexpected schema/checksum conflict.
3. Establish whether an existing usable, documented backup or verified restoration mechanism exists. If available, follow its documented safe procedure and verify backup integrity sufficiently to support the rollback plan. Never invent backup paths, credentials, restore commands or success evidence.
4. If no usable recovery mechanism can be established, do not migrate or start the new local mappings with Flyway against that persistent database. Report this gate as **Blocked — Human Action Required** and request explicit authorization for a safe recovery approach. Continue the remaining approved implementation and disposable Testcontainers verification; this does not block the entire increment.
5. Only after target/recovery evidence is established, apply and verify V2 before starting the new mappings against existing V1 data. Preserve credentials, records, constraints, named volume, V1 and its checksum. Record actual local evidence separately from disposable test evidence.

Never use flyway clean, automatic flyway repair, unapproved flyway baseline, docker compose down -v, volume deletion, table/database drop/recreation, existing-version resets or V2 against an unidentified target. Production execution remains excluded. Hibernate remains validate, OSIV false, SQL initialization disabled and default/production Flyway disabled. Successful Testcontainers migration does not establish persistent local migration success.

### Complete Current File Scope

Production Java paths relative to src/main/java/com/example/agenticorderservice:

- product/application/ProductApplicationService.java, ProductResult.java, ProductNotFoundException.java.
- customer/application/CustomerApplicationService.java, CustomerResult.java, CustomerNotFoundException.java.
- product/api/ProductController.java, ProductDetailsRequest.java, DeactivateProductRequest.java, ProductResponse.java.
- customer/api/CustomerController.java, CustomerDetailsRequest.java, DeactivateCustomerRequest.java, CustomerResponse.java.
- shared/api/StrictJsonRequestDecoder.java, StrictRequestHttpMessageConverter.java, ApiRequestInterceptor.java, ApiInputException.java, ApiError.java, ApiExceptionHandler.java, ApiErrorResponseWriter.java, ApiErrorController.java; these are focused helpers, not a generic validation framework.
- shared/application/InputValidationException.java for contextual HTTP-independent input failures; never hide programming defects as field errors.
- configuration/JacksonConfiguration.java, ApiWebConfiguration.java for existing Boot mapper/converter/hooks wiring.
- product/domain/Product.java and customer/domain/Customer.java: exact version metadata and necessary no-op-safe use; preserve existing invariant behavior.
- product/persistence/ProductRepository.java, customer/persistence/CustomerRepository.java: minimal list operations only.
- src/main/resources/application.yml: explicit approved strict binding configuration; preserve existing database/profile defaults.
- src/main/resources/db/migration/V2__add_product_customer_versions.sql: reviewed V2 only; V1 untouched.

Tests relative to src/test/java/com/example/agenticorderservice:

- product/application/ProductApplicationServiceTest.java; customer/application/CustomerApplicationServiceTest.java.
- product/api/ProductApiTest.java; customer/api/CustomerApiTest.java; shared/api/StrictJsonRequestDecoderTest.java; shared/api/ApiErrorHandlingTest.java.
- product/api/ProductApiIT.java; customer/api/CustomerApiIT.java; persistence/OptimisticLockingIT.java; persistence/VersionMigrationIT.java; shared/api/ApiErrorDispatchIT.java.
- Adapt existing persistence/DomainSchemaIT.java, order/persistence/OrderPersistenceIT.java, customer/persistence/CustomerPersistenceIT.java and PostgreSqlFoundationIT.java for explicit direct-SQL column lists/version values and V1+V2 history. Extend ProductPersistenceIT/CustomerPersistenceIT as necessary for version/newness assertions; do not remove existing constraints/round trips or weaken checks.
- Reuse PostgreSqlTestConfiguration and existing image source; only narrowly required test-fixture support may be adapted. No local .env/Compose DB dependency for tests.
- Update this existing playbook's results/evidence/status and existing testing/implementation/open-item indexes where actual execution requires it; never rewrite recorded prompts/history.

Preserve pom.xml and all CI/coverage/security configuration. Do not infer permission for unrelated files/dependencies/domain operations. Small helper naming can be adjusted within these responsibilities if needed, with the actual diff explaining it; architectural changes still require approval.

### Required Tests and Incremental Execution

No new Maven test dependency: use existing JUnit 6/Mockito/Spring Test and MockMvcBuilders.webAppContextSetup, with test-only database auto-configuration exclusions and mocked application services for fast MVC. Real PostgreSQL ITs use actual application proxies and existing @ServiceConnection/Testcontainers. Use a real RANDOM_PORT context and Java 21 HTTP client where servlet error dispatch requires transport proof. No test-wide rollback transaction may hide real commit behavior.

1. Inspect starting Git state and immutable V1/accepted code; record baseline evidence. Implement application operations/results and isolated units without adding unapproved abstractions.
2. Add reviewed V2 and exact version mapping; adapt explicit SQL fixtures/history expectations before running the full suite. Prove V1-only representative data→V2 and fresh V1+V2, zero backfill, final BIGINT/NOT NULL/no default, preserved data/constraints/FKs/indexes/V1 checksum, Hibernate validation/newness/insert/increment and repeated migration/startup without reapplication/reset. Apply and verify persistent local migration only after the Persistent Local Database Safety Gate passes; otherwise report that gate separately as Blocked — Human Action Required and continue disposable tests and other approved work.
3. Implement header/path/decoder/DTO/controller/error boundaries and actual MVC tests; all ten endpoints, exact response fields/statuses/Location/Allow, all applicable 20 codes, inactive updates/lists/empty arrays, canonical UUID cases and each accepted D1–D8.
4. Add full-context PostgreSQL/API and controlled failure tests: inactive-inclusive normalized uniqueness, same-owner email update, committed-state preservation for rejected mutations, flush/commit conflict translation, actual success-after-commit, no serialization SQL, classified pre-write 503 versus ambiguous/unclassified 500, no replay/false rollback assertion.
5. Deterministic races for each aggregate: PUT/PUT, PUT/PATCH, PATCH/PUT, PATCH/PATCH, sequential repeated PATCH, stale entity after committed update, stale entity after committed deactivation. Independent transactions/contexts, actual Hibernate predicates, barriers/latches and bounded deadlines, no arbitrary sleeps. Winner state and loser confirmed rollback verified through fresh reads. Sequential repeat must be 200/unchanged with zero actual UPDATE/version increment; concurrent no-op/conflict depends on observed versions/writes.
6. Cover name/email Unicode/code-point/grammar/provider cases and exact price range/lexical/type boundaries. Cover missing/whitespace/null/array/{}, nested duplicate keys, extra trailing values, property-order permutations, mixed headers/path/body, all forbidden id/active/version attempts, +json/non-UTF-8 rejection and incompatible Accept safe JSON. Preserve old constraint tests: direct SQL fixtures must include a version so they fail on the intended constraint, not new nullability.
7. Preserve all existing Increment 1–3 tests, Surefire *Test/Failsafe *IT separation, JaCoCo and Sonar pipeline; coverage does not replace meaningful tests. Docker unavailable means blocked/Pending integration evidence, never a skip reported Passed. Full Order protocol remains unverified.

### Verification, Evidence and Human Review

Run with command-local Java 21 and existing wrapper, without system configuration changes:

```sh
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp test
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp verify
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp dependency:tree
git diff --check
git diff --cached --check
```

Inspect resolved dependency alignment, Surefire/Failsafe reports, coverage, schema/default/history/checksum/version evidence, actual SQL update counts, transaction outcomes, headers/error envelopes and serialization. Validate internal links/statuses and whitespace for new files too. Stop only verification containers you started; preserve local database/volume and never print secrets.

Record actual files/commands/resolved versions/test counts/results, disposable migration evidence and separate persistent-local target/recovery/migration evidence or Blocked — Human Action Required, problems/limitations and remaining risks separately from expectations. Future/unavailable gates remain Pending. Preserve original prompts/earlier evidence, add execution results/lessons only when observed, and mark Executed – Pending Human Review. Present complete staged/unstaged/new-file Git diff for human review. Obtain explicit human acceptance before completion, separate commit/push authorization, and no subsequent increment without approval.

## Final Prompt Preparation Result

Final edge D1–D8 and reviewed V2 SQL are recorded; the definitive prompt/file scope/test plan is prepared, not executed. Documentation checks passed: 355 internal links/anchors, ten unique business endpoints, twenty identical catalog codes/statuses, Markdown tables/fences, original prompt/historical material preservation, and staged/unstaged/new-file whitespace. Only the nine relevant documentation files changed in this task; source/tests/POM/CI/PRD/V1 and prior accepted records remain intact, V2 absent. Actual runtime results remain Pending; coding, migration execution, ADR acceptance and commit/push require their explicit authorization gates.

## Final Playbook Hardening Result — 2026-10-14

Documentation-only consistency checks passed: 362 internal Markdown links/anchors; ten unique endpoints; twenty unchanged error-code/status/safe-message contracts; preserved A/B1–B11, final D1–D8, D1–D10 and Increment 3 acceptance; unchanged ADR-0002/ADR-0006 statuses. Original prompt and entire historical proposal/evidence tail are unchanged. Tracked, staged and new-file whitespace checks passed; the complete documentation diff was reviewed. Eleven existing documentation files were refined; no implementation, dependency, CI, migration or database operation was performed.

The current prompt is the sole executable contract after explicit coding authorization. Persistent local migration/startup requires actual target/history/schema and established usable recovery evidence; repository inspection has not established a documented backup/restore mechanism. If unavailable during execution, report only local migration as Blocked — Human Action Required and continue disposable Testcontainers verification and other approved work. No backup or runtime success is claimed here. Implementation remains Pending Human Implementation Approval; runtime gates remain Pending.

## Implementation Results — Executed Pending Human Review

**Status: Increment 4 — Executed, Pending Human Review.** The human explicitly authorized the complete Current Implementation Prompt on 2026-10-14. The original prompt, finalized execution contract, historical proposals and prior evidence remain unchanged; their earlier pending-authorization wording records the gate before this authorization. No human implementation acceptance or commit/push/merge authorization has been granted.

### Actual Implementation

Implemented all ten Product/Customer endpoints with feature-owned proxied application transactions, transport-neutral immutable results and separate REST records. Responses are materialized inside application transactions; success follows proxy completion. Added the exact boxed Long @Version mappings and reviewed four-statement V2, preserving V1, identity/equality, converters and D1–D10. Repeated inactive PATCH does not issue UPDATE or increment versions. Customer DELETE remains unavailable with the exact 405/Allow contract and no deletion/reference lookup implementation.

Strict UTF-8 request conversion is input-only, ahead of the default reader for the dedicated DTOs; response converters remain available. Complete streaming parsing preserves number spelling and rejects nested duplicate keys/trailing values before deterministic category/field selection. Header/path checks precede body decoding. DTO-scoped missing-body advice handles absent input, including MVC's internal octet-stream marker for absent Content-Type; actual untyped/octet-stream bodies remain rejected. Safe direct JSON writing handles incompatible Accept and servlet fallback. Confirmed uniqueness requires SQLSTATE 23505 and structured uq_customers_email; confirmed optimistic failures become 409. Connection-establishment failures require both phase and typed connection evidence; unknown completion remains 500 without replay or rollback claims.

Validation of feature-specific price/email values stays in the respective application feature; shared input-failure/name handling does not import foreign domain or persistence packages. The two test-support classes centralize only existing MockMvc/Testcontainers fixtures. The smoke test retains its original assertions with mocked application services because its database auto-configuration is deliberately excluded. Base application.yml, dependencies/POM, CI, coverage and Sonar settings are unchanged.

### Actual Files Created or Modified

```text
Modified source/test files:
src/main/java/com/example/agenticorderservice/customer/domain/Customer.java
src/main/java/com/example/agenticorderservice/customer/persistence/CustomerRepository.java
src/main/java/com/example/agenticorderservice/product/domain/Product.java
src/main/java/com/example/agenticorderservice/product/persistence/ProductRepository.java
src/test/java/com/example/agenticorderservice/AgenticOrderServiceApplicationTest.java
src/test/java/com/example/agenticorderservice/PostgreSqlFoundationIT.java
src/test/java/com/example/agenticorderservice/customer/persistence/CustomerPersistenceIT.java
src/test/java/com/example/agenticorderservice/order/persistence/OrderPersistenceIT.java
src/test/java/com/example/agenticorderservice/persistence/DomainSchemaIT.java

New source/test files:
src/main/java/com/example/agenticorderservice/configuration/ApiWebConfiguration.java
src/main/java/com/example/agenticorderservice/configuration/JacksonConfiguration.java
src/main/java/com/example/agenticorderservice/customer/api/CustomerController.java
src/main/java/com/example/agenticorderservice/customer/api/CustomerDetailsRequest.java
src/main/java/com/example/agenticorderservice/customer/api/CustomerResponse.java
src/main/java/com/example/agenticorderservice/customer/api/DeactivateCustomerRequest.java
src/main/java/com/example/agenticorderservice/customer/application/CustomerApplicationService.java
src/main/java/com/example/agenticorderservice/customer/application/CustomerNotFoundException.java
src/main/java/com/example/agenticorderservice/customer/application/CustomerResult.java
src/main/java/com/example/agenticorderservice/product/api/DeactivateProductRequest.java
src/main/java/com/example/agenticorderservice/product/api/ProductController.java
src/main/java/com/example/agenticorderservice/product/api/ProductDetailsRequest.java
src/main/java/com/example/agenticorderservice/product/api/ProductResponse.java
src/main/java/com/example/agenticorderservice/product/application/ProductApplicationService.java
src/main/java/com/example/agenticorderservice/product/application/ProductNotFoundException.java
src/main/java/com/example/agenticorderservice/product/application/ProductResult.java
src/main/java/com/example/agenticorderservice/shared/api/ApiError.java
src/main/java/com/example/agenticorderservice/shared/api/ApiErrorController.java
src/main/java/com/example/agenticorderservice/shared/api/ApiErrorResponseWriter.java
src/main/java/com/example/agenticorderservice/shared/api/ApiExceptionHandler.java
src/main/java/com/example/agenticorderservice/shared/api/ApiInputException.java
src/main/java/com/example/agenticorderservice/shared/api/ApiRequestInterceptor.java
src/main/java/com/example/agenticorderservice/shared/api/StrictJsonRequestDecoder.java
src/main/java/com/example/agenticorderservice/shared/api/StrictRequestHttpMessageConverter.java
src/main/java/com/example/agenticorderservice/shared/application/InputValidationException.java
src/main/resources/db/migration/V2__add_product_customer_versions.sql
src/test/java/com/example/agenticorderservice/customer/api/CustomerApiIT.java
src/test/java/com/example/agenticorderservice/customer/api/CustomerApiTest.java
src/test/java/com/example/agenticorderservice/customer/application/CustomerApplicationServiceTest.java
src/test/java/com/example/agenticorderservice/persistence/OptimisticLockingIT.java
src/test/java/com/example/agenticorderservice/persistence/VersionMigrationIT.java
src/test/java/com/example/agenticorderservice/product/api/ProductApiIT.java
src/test/java/com/example/agenticorderservice/product/api/ProductApiTest.java
src/test/java/com/example/agenticorderservice/product/application/ProductApplicationServiceTest.java
src/test/java/com/example/agenticorderservice/shared/api/ApiErrorDispatchIT.java
src/test/java/com/example/agenticorderservice/shared/api/ApiErrorHandlingTest.java
src/test/java/com/example/agenticorderservice/shared/api/ApiTestSupport.java
src/test/java/com/example/agenticorderservice/shared/api/PostgreSqlApiTestSupport.java
src/test/java/com/example/agenticorderservice/shared/api/StrictJsonRequestDecoderTest.java

Documentation updated for actual execution: this playbook, prompt index, testing strategy, implementation plan and open-items index. Pre-existing documentation changes were preserved.
```

### Verification Evidence

Final complete verification finished successfully on 2026-10-14 at 15:07:33 Europe/Berlin using Java 21.0.6 and Maven Wrapper 3.9.9:

```sh
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -v
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp test
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp verify
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp dependency:tree
git diff --check
git diff --cached --check
```

- Final documentation/Git audit: **366 internal links/anchors**, preserved Markdown tables/fences and immutable prompts/history, unchanged ADR statuses and protected files; staged/unstaged/untracked whitespace checks passed. .env and personal Obsidian workspace state remain ignored/untracked. Starting and final read-only container inventories were empty; no verification container remains running. Current task changed 14 existing files and created 39 source/test/migration files; unrelated pre-existing documentation changes were preserved.
- Final verify: **240 Surefire tests and 44 Failsafe PostgreSQL tests, 284 total; zero failures, errors or skips**. The separate fast-test command also passed during implementation (232 tests before the last precedence cases); final verify includes the full current 240-test fast suite. All original 120 fast/21 integration tests remain covered.
- Fresh JaCoCo execution data was generated by one complete current verify run: **95.14% line coverage (411/432), 85.15% branch coverage (304/357), 94.64% instruction coverage (2559/2704), 98.66% method coverage**. Reports: target/site/jacoco/index.html and jacoco.xml. These are local suite results, not a claim that remote SonarQube passed.
- Resolved unchanged stack: Boot 4.1.1, Spring 7.0.9, Hibernate 7.4.5.Final, Flyway 12.4.0, PostgreSQL JDBC 42.7.14, Jackson 3.1.7, Testcontainers 2.0.5, Jupiter/Platform 6.0.3; existing Tomcat 11.0.26/Logback 1.6.5 overrides preserved. Dependency-tree command passed; POM hash unchanged.
- Actual PostgreSQL 17.11 with central postgres:17.11-bookworm image, isolated Spring-managed disposable containers, random mapped ports and no Compose/.env dependency. JDBC connectivity, Hibernate validation, OSIV disabled, SQL initialization disabled, and V1+V2 history passed.
- V1-only active/inactive Product/Customer rows and referenced Order/OrderItem snapshots upgraded to V2 in a unique disposable schema: zero backfill, BIGINT/NOT NULL, no final defaults, unchanged V1 checksum, constraints/indexes and records. Repeated migrate executes zero migrations and preserves a nonzero version. Fresh V1+V2 contexts validate all four entities. V1 SHA-256 remains 0abf5d1f9944c213d6c57972ca2d5644e97acb02a66638290846f3859f9cc25e.
- All seven optimistic scenarios for both aggregates passed with independent persistence contexts/transactions, barriers/latches and deadlines. Captured Hibernate UPDATE SQL includes the version predicate. Real overlapping HTTP PUTs rendezvous inside the outer transaction: one committed 200, one confirmed commit-conflict 409; fresh reads match the winner. Sequential PATCH repeats record zero entity UPDATEs, one SELECT and unchanged version.
- Real normalized inactive-inclusive uniqueness failures produce the exact 409 at completion, preserve the losing resource and allow same-owner email updates. Rejected mutations preserve committed state. Actual HTTP serialization adds no SQL beyond the use-case SELECT.
- Real servlet/HTTP incompatible-Accept, unknown-route and error-dispatch JSON checks passed. A deliberately stopped class-owned disposable database with evicted pool handles proves pre-write acquisition failure/503. Controlled tests separately cover uncertain completion/unclassified errors/500 without exposing diagnostics or replaying writes.

### Persistent Local Migration — Blocked — Human Action Required

Repository configuration identifies a loopback Compose service and named development volume, but the read-only runtime container inventory was empty. No running persistent development target/history/schema or usable documented backup/restore mechanism was established. No local-profile startup, persistent migration, backup, repair, reset, container/volume deletion or production operation was performed. Safe recovery authorization was requested; the persistent gate remains blocked pending actual target and usable recovery evidence. Successful disposable tests do not establish local migration success. The remaining implementation and disposable verification completed independently.

### Problems Encountered

- Sandbox restrictions prevented Mockito agent attachment/Docker access; approved verification commands were rerun outside the sandbox. No system Java/Maven configuration changed.
- A test mock was restubbed using when on an already throwing method; corrected with doThrow. Compilation escaping errors in test JSON fixtures were corrected before verification.
- Flyway's newly created disposable schema adds a synthetic SCHEMA history entry. Upgrade verification explicitly checks that entry and the exact SQL versions 1/2 rather than conflating metadata with application migrations.
- Stopped PostgreSQL can leave stale pool handles whose first failure occurs after transaction work begins; a conservative 500 is then appropriate. The 503 fixture evicts those handles to prove acquisition-phase failure, without broadening all SQL/connection failures to 503.

### Lessons Learned — Observed

- MVC can bypass readers for empty input or use an internal default media marker; test absent bodies with and without Content-Type rather than relying solely on decoder units.
- Streaming syntax completion, original numeric lexemes and deterministic validation stages are necessary to preserve accepted precedence independently of property order.
- Only real PostgreSQL commit and HTTP tests establish uniqueness/version failure translation and success-after-completion. Mocks and flush-only assertions do not replace them.
- Persistent migration recovery is independent from disposable migration correctness and must be reported separately.

### Remaining Risks and Review Gates

Explicit human implementation acceptance is Pending. Persistent local V2 execution is Blocked — Human Action Required; no recoverable local rollout is claimed. Production migration/rollout, Customer deletion, Order placement/API, pessimistic locking/retry protocol and ADR-0002 verification remain deferred. ADR-0002 and ADR-0006 remain Proposed; tests do not accept either ADR. New-code SonarQube/CI results require the user's separately authorized commit/push workflow. No commit, push, merge, deployment or next increment was performed.

## Current Increment 4 Design — Pending Implementation Approval

**Non-executable supporting record:** Superseded for execution by the Current Implementation Prompt above. Earlier review/prerequisite language does not add general approval gates after explicit coding authorization. Preserve this record for auditability.

### Endpoint and Ownership Matrix

Exactly ten business endpoints. Product response id/name/price/active and Customer id/name/email/active remain unchanged; no version field exposed. Lists include inactive records, with no new pagination/sorting guarantee.

| Endpoint | Application owner | Success / traceability |
| --- | --- | --- |
| POST /products | ProductApplicationService.create | 201, Location /products/{id}, generated ID, active; FR-001/006, AC-001/012 |
| GET /products | ProductApplicationService.list | 200 array including inactive, [] when empty; FR-001/006, AC-013 |
| GET /products/{id} | ProductApplicationService.get | 200 complete active/inactive record; absent canonical ID 404; AC-013 |
| PUT /products/{id} | ProductApplicationService.update | 200, replace name/price, preserve ID/activity; BR-001/011–013, AC-001/011/012 |
| PATCH /products/{id} | ProductApplicationService.deactivate | 200 inactive; repeated false succeeds; BR-012, AC-002/012 |
| POST /customers | CustomerApplicationService.create | 201, Location /customers/{id}, generated ID, active; FR-002/006, AC-003/012 |
| GET /customers | CustomerApplicationService.list | 200 including inactive, []; FR-002/006, AC-013 |
| GET /customers/{id} | CustomerApplicationService.get | 200 complete active/inactive record; absent canonical ID 404; AC-013 |
| PUT /customers/{id} | CustomerApplicationService.update | 200, replace name/email, preserve ID/activity; BR-011/012, AC-003/011/012 |
| PATCH /customers/{id} | CustomerApplicationService.deactivate | 200 inactive, repeat succeeds; BR-012, AC-012 |

Customer DELETE is deferred and returns the Accepted interim OPERATION_NOT_AVAILABLE/405 with accurate Allow, not generic METHOD_NOT_ALLOWED. It introduces no eleventh implemented business operation and performs no lookup/check-then-delete. Product DELETE uses METHOD_NOT_ALLOWED/405. Unknown routes use ENDPOINT_NOT_FOUND; Order endpoints stay unimplemented. Full PRD Customer DELETE/204 remains future MVP work tied to ADR-0001/Proposed ADR-0002.

### Boundaries, Validation and Processing

Retain the original feature-owned controller → application → own domain/persistence organization; application results are transport-neutral immutable carriers, API responses are separate DTOs. Reuse existing aggregates/values/converters and controlled mutations; no foreign repositories, JPA entity serialization, generic CRUD service, broad interfaces or circular wiring.

Propose String name/email, BigDecimal price and boxed Boolean active DTO fields. Existing domain normalization/Unicode/email/range rules stay authoritative. Required validation cannot default missing booleans to false. Typed domain/application signals remain HTTP-independent; do not globally map IllegalArgumentException/NullPointerException or parse their text as client errors.

Global feature flags/customization are candidate mechanisms, not proof of the Accepted behavior. The previous plain fail-fast DTO binding proposal is insufficient alone: fully validate JSON syntax/root, scan every property, then enforce type/presence/business-value stages before use-case lookup. Retain original numeric token text to reject e/E and excessive explicitly supplied fractional digits, including 149.990/10.000; only then create BigDecimal/Money. Accept 149/149.9/149.99; reject numeric strings/booleans as INVALID_FIELD_TYPE. No rounding/zero-stripping/intermediate double or change to Money/D6.

Human Accepted deterministic tie-breaker: Product name→price, Customer name→email, PATCH active; unknown/forbidden property paths in lexicographic order. These choices are resolved under D4; implementation verification remains Pending. Category precedence is Accepted and must be independent of JSON field order; test mixed-category payloads and swapped order. PATCH true is forbidden, nonboolean active a type error. Canonical UUID shape must be checked before lenient UUID parsing; D6 requires routing/method/headers first, path syntax second, body third, lookup last; verify a pre-argument path hook before body decoding.

Central API advice/error dispatch must handle all accepted catalog codes, negotiation/media/routing and commit-time failures without HTML/ProblemDetail fallback, unsafe diagnostics or recursion for incompatible Accept. The minimal decoder/converter, typed classifier/safe writer and existing-dependency MockMvc approach are selected; exact wiring and runtime evidence remain Pending, not new behavioral decision gates. Do not add libraries to solve this without approval.

### Transactions and Constraint Handling

Human Accepted: externally invoked proxied Product/Customer writes @Transactional; reads @Transactional(readOnly=true). Controllers own no transaction; domain has no Spring transaction API. Materialize required immutable results within application transactions, map API responses afterward without lazy access; HTTP success follows completed commit. Self-invoked annotated methods do not substitute for a reviewed proxy boundary.

Known uq_customers_email/23505 maps to DUPLICATE_EMAIL/409 at API boundary after rollback/transaction completion; same customer's normalized email is allowed. Confirmed JPA version conflicts map to CONCURRENT_MODIFICATION/409, including flush/commit causes. Unrelated SQL/technical failures use safe 500, and only clearly classified temporary outages 503. No automatic write retry or continued rollback-only context. Confirmed rollback leaves no changes; uncertain completion may have committed and failed transport may prevent a response.

### Optimistic Locking and V2 Review

@Version is Human Accepted for Product/Customer and replaces DynamicUpdate/unversioned column-specific writes as the selected conflict strategy. Complete [ADR-0006](../../03-decisions/ADR-0006-product-customer-optimistic-locking.md) remains Proposed. Reviewed proposal filename is V2__add_product_customer_versions.sql; its exact SQL is in the current prompt and ADR-0006, with runtime verification Pending. The exact boxed Long @Version/@Column(name="version", nullable=false) mapping, BIGINT/final NOT NULL, existing-row backfill 0 and absence of a final database default are Human Accepted. Temporary default 0 may be used but must be removed after backfill; Hibernate owns new versions. Sequential repeated inactive PATCH is 200 with unchanged representation and no SQL UPDATE/version increment. Verify provider initialization/newness, concrete SQL, migration timing/permissions and concurrent conflict/no-op behavior; do not reopen the selected policies. Do not edit V1 or create/execute V2 before explicit approval.

D1–D10 identity/equality/converters/domain constraints and Increment 3 history are preserved. Entity versions are internal; no Order/OrderItem @Version or response-version field selected. Optimistic locking does not detect every stale HTTP client after a fresh service read; ETag/If-Match is excluded without new approval. It does not replace multi-product Order read/lock/deletion coordination; ADR-0002 remains deferred/unverified.

### Proposed Implementation Files and Dependencies

Use the complete file scope in the **Current Implementation Prompt above** for feature services/results, dedicated controllers/requests/responses, shared API envelope/advice, field-aware application validation signal, own-repository findAll, application.yml and three-level tests. Class names/precise pipeline adapter remain reviewable technical proposals. Changes to that earlier scope:

| File / responsibility | Current proposal and approval boundary |
| --- | --- |
| product/domain/Product.java; customer/domain/Customer.java | Use the Human Accepted exact boxed Long mapping only after implementation approval; no public setter/client/DTO version or ID/equality change |
| src/main/resources/db/migration/V2__add_product_customer_versions.sql | Reviewed SQL proposal and accepted BIGINT/NOT NULL/backfill 0/no final default policy; explicit implementation approval and runtime proof required. Never edit V1. No file created now. |
| configuration/JacksonConfiguration.java and API request-validation adapter | Exact lexical/precedence-aware processing design must be reviewed; feature flags alone insufficient |
| shared/api/ApiExceptionHandler.java | Accepted catalog/JSON envelope, special Customer DELETE, optimistic/unique commit causes, safe technical/media/routing responses |
| pom.xml | Preserve unchanged; existing test dependencies support builder-based MockMvc; no new test starter |
| ProductApiTest/CustomerApiTest and ProductApiIT/CustomerApiIT | Extend the matrices below with every accepted mapping and real PostgreSQL @Version races |
| DomainSchemaIT/ProductPersistenceIT/CustomerPersistenceIT | Extend existing assertions for approved V2/version behavior without weakening original tests/history; existing test infrastructure reused |

No production dependency/version/CI/quality-gate change is approved. Production migrations, Order API/placement, customer deletion, explicit pessimistic locks, coordinator/retries and ADR-0002 remain excluded.

## Updated Mandatory Test Matrix — Runtime Results Pending

U = isolated application-service unit tests; H = actual DispatcherServlet/JSON/validation/response tests, no Docker; P = real PostgreSQL/Testcontainers full-context integration tests via Failsafe. Keep Surefire/Failsafe, JUnit 6, JaCoCo and Sonar/GitHub Actions intact. Direct controller calls, mock conflicts and coverage totals are insufficient for H/P claims.

| Operations / cases | U evidence | H evidence | P evidence |
| --- | --- | --- | --- |
| POST /products, POST /customers | Valid create/domain/result; missing/invalid input | 201, correct Location/generated ID/complete fields/active; rejected create exact code | Generated identity/normalized values persisted, no rejected rows; C unique conflict at completion |
| GET /products, GET /customers | Complete active/inactive list and [] | 200 exact resource fields/[] | Fresh committed lists, no active filtering, materialized serialization/no queries |
| GET /products/{id}, GET /customers/{id} | Found/missing lookup signal | Canonical absent ID 404 feature code; malformed/short UUID 400 INVALID_RESOURCE_ID | Correct committed fields including inactive; no mutation |
| PUT /products/{id}, PUT /customers/{id} | Both fields replaced, ID/activity unchanged, invalid mutation prevented | 200 exact response; missing/type/business/forbidden mappings | Persisted full edits, inactive retained; self-owned email allowed, others 409; real stale-version conflicts |
| PATCH /products/{id}, PATCH /customers/{id} | Controlled/repeated deactivation, no reactivation | false/200; {}, null, true, string/number, name/foo extras exact accepted codes | Rows retained; rejected request unchanged; sequential repeat 200/unchanged/no SQL UPDATE/no version increment; concurrent paths follow actual versions |
| Unknown/id/active/extra/nested fields | No use-case mutation on rejection | 400 UNKNOWN_FIELD vs UNSUPPORTED_FIELD; test-only nested DTO when needed, no Order endpoint | Before/after state unchanged |
| Strict scalars | Application value validation remains independent | String price/boolean price, numeric name, boolean email, string/number active → INVALID_FIELD_TYPE; null/missing → REQUIRED_FIELD_MISSING | No inserts/updates on rejected requests |
| Price lexical/range/scale | Existing Money/range semantics unchanged | 149/149.9/149.99, 0/999999.99 valid; negative/excessive/149.990/10.000/1.4999e2/1e3 → INVALID_PRODUCT_PRICE | Exact hydration and no rounded/partial state |
| Names/emails | strip/code-point/Unicode limits; ASCII grammar/Locale.ROOT/provider distinctions | Existing 1/200/201 name and 254/255 email cases; preserve accents/supplementary characters | Normalization, converter/constraints, active/inactive case-insensitive uniqueness |
| Error precedence | Deterministic field/category behavior | All six categories, mixed-category payloads, property-order permutations, same-category ties; exactly one code/message | Rejected writes have no committed effects |
| Unsupported methods/DELETE | No deletion use case | Customer DELETE OPERATION_NOT_AVAILABLE/405 vs Product DELETE METHOD_NOT_ALLOWED/405; accurate Allow | No reference query/delete and no changed rows |
| Media/route errors | Not domain logic | 406 NOT_ACCEPTABLE, 415 UNSUPPORTED_MEDIA_TYPE, unknown route 404 ENDPOINT_NOT_FOUND; incompatible Accept still safe JSON | Full-context path and no writes |
| Confirmed optimistic conflicts | Application failure propagation, no retry | CONCURRENT_MODIFICATION/409, fixed safe message | Independent contexts with stale versions/overlapping PUT/PUT and PUT/PATCH in both completion orders; actual version predicates, winner retained, full loser rollback, deactivation irreversible |
| V2 compatibility | Provider/model configuration as applicable | No entity version exposed or ETag implied | V1 existing data→V2 and fresh V1+V2; BIGINT/NOT NULL/backfill 0/no final default, unchanged V1 checksum/data/constraints, Hibernate validation/newness/increment and repeated migration/startup without version reset |
| Technical/outage faults | Confirmed exception categories propagate, no broad SQL catch | Controlled 500 INTERNAL_SERVER_ERROR vs classified 503 SERVICE_UNAVAILABLE; no internal/sensitive leakage | Confirmed rollback leaves no changes; lost completion confirmation does not assert rollback or trigger replay |
| Commit timing/materialization | Results constructed in application transaction | Success only after commit, API translation outside; no lazy serialization | Real unique/optimistic completion exceptions, fresh state reads, OSIV false; no test-wide rollback masking commit |

Use barriers/latches, independent transactions and bounded deadlines for real races; no arbitrary sleep/order assumptions. Duplicate normalized-email collisions need real database proof, not only prechecks. A sequential inactive PATCH repeat must satisfy 200 with no UPDATE/version increment; do not assert all overlapping no-op calls necessarily conflict. All runtime tests and migration evidence remain Pending. No Order protocol verification is claimed.

### Mandatory Optimistic-Locking Matrix

Run each case for Product and Customer using PostgreSQL/Testcontainers, independent transactions/persistence contexts and actual Hibernate version predicates. Coordinate overlaps with barriers/latches and bounded deadlines, never sleeps; check fresh committed state and rollback after confirmed failure. HTTP tests verify corresponding 200/409 responses, while units cannot establish provider correctness.

| Scenario | Required verification |
| --- | --- |
| PUT vs PUT | Stale dirty update rejected, no silent lost update; winner preserved. |
| PUT vs PATCH | Concurrent deactivation cannot be undone; verify controlled completion orders. |
| PATCH vs PUT | A stale update cannot restore active status; inspect normalized details/activity. |
| PATCH vs PATCH | Conflict/no-op behavior follows observed version and actual write ordering; no blanket outcome assumption. |
| Sequential repeated PATCH | Second HTTP 200/unchanged inactive representation; no SQL UPDATE and no version increment. |
| Stale entity after committed update | Confirmed optimistic-lock failure, including flush/commit translation. |
| Stale entity after committed deactivation | No reactivation or stale overwrite; loser rollback preserves committed inactivity. |

Migration checks: representative existing V1 records and fresh V1+V2, preservation of records/constraints/FKs/indexes, unchanged V1 checksum/correct Flyway history, no final default, Hibernate validation/insert/version behavior and repeated startup/migrate/validate without V2 reapplication or resetting versions. Runtime evidence remains Pending.

## Future Implementation Prerequisites — Pending Human Approval

**Non-executable supporting record:** Superseded by the Current Implementation Prompt. This earlier prerequisite list adds no general approval gates after explicit coding authorization.

1. Review the complete ADR-0006 (Proposed) and authorize implementation separately. Exact boxed Long/BIGINT/non-null/backfill 0/no final default/no-op choices are already Human Accepted; reviewed V2 SQL/runtime proof and provider/newness verification remain pending, not repeat design selection; production permissions are excluded.
2. Implement and prove the selected decoder/writer and Human Accepted D1–D8 only after coding authorization; exact Boot/Jackson/proxy/cause/dispatch behavior requires runtime verification.
3. Obtain explicit approval to execute the current implementation prompt/file scope. Use existing MockMvc dependencies; no new dependency is selected. No source/migration generation before that authorization.
4. Then implement incrementally: application transactions/units → approved V2/version mappings and PostgreSQL evidence → strict DTO/controller/error pipeline/MVC tests → full-context API/version/uniqueness tests → Java 21 wrapper test/verify/dependency inspection/coverage/documentation/diff review.
5. Record actual results separately, status Executed – Pending Human Review; obtain explicit acceptance and separate commit authorization. No automatic conflict retry, Order work or production migration. Preserve existing suites/gates/data/volumes.

## Current Documentation Result

A/B1–B11 incorporated; catalog and current proposal updated, ADR-0006 created as Proposed, architecture/transaction/domain/test navigation aligned. Original prompt, initial proposal/evidence and Accepted Increment 3/D1–D10 preserved. Documentation verification: 339 internal links/anchors passed, all 20 Accepted code/status pairs and exactly ten endpoint rows checked, tables/fences and staged/unstaged/new-file whitespace passed. File hashes confirm original prompt, source/tests/POM/CI/PRD/V1 preserved and no V2 exists. ADR-0001–0005 statuses unchanged, ADR-0006 Proposed. Implementation, HTTP/PostgreSQL/version/migration verification remain Pending. No application code, dependency, CI or migration was changed or executed.

## Lessons Learned — Decision Update

Documentation observation: field-type strictness, numeric lexical preservation and whole-request error precedence are separate obligations; optimistic transaction versions do not replace stale-client preconditions or the future Order locking protocol. Explain proxy completion before response/conflict mapping, special deferred-operation errors, and additive V2 planning while preserving immutable V1/history. These are planning observations, not invented runtime lessons.

### Final Technical-Decision Documentation Result

Mapping, migration policy and sequential no-op requirements are resolved. Documentation checks passed for 344 internal links/anchors, Markdown tables/fences, staged/unstaged/new-file whitespace, exact accepted mapping and the seven concurrency cases. Only seven authorized documentation files changed; all other files and immutable prompt/history were preserved. Migration, optimistic-lock and HTTP tests have not run in this documentation-only task. The earlier 339-link check remains evidence of the preceding update. Original prompt/history and existing changes are preserved; Increment 4 stays Pending Human Implementation Approval.

## Historical Planning Proposal and Evidence — Superseded Where Explicitly Decided

The following preserves the first documentation review. Its DynamicUpdate recommendation, Open scalar/catalog/transaction decisions, no-migration proposal and earlier future instructions are historical, superseded by the Human Accepted record/current prerequisites above. Unresolved tooling and technical mechanics remain unresolved. It does not authorize implementation or override current decisions.

#### Initial Proposed Design — Pending Human Approval


#### Endpoint and Ownership Matrix

The PRD is authoritative. Product responses contain id/name/price/active; Customer responses id/name/email/active, identically for individual/list/create/update/deactivation results. Prices are JSON decimal EUR amounts, not serialized Money objects.

| Endpoint | Feature application operation | Accepted success/behavior | Traceability |
| --- | --- | --- | --- |
| POST /products | ProductApplicationService.create | 201, generated ID, Location /products/{id}, initially active | FR-001/006, AC-001/012 |
| GET /products | ProductApplicationService.list | 200, all active/inactive products; [] if empty | FR-001/006, AC-013 |
| GET /products/{id} | ProductApplicationService.get | 200, active or inactive record; absent UUID 404 | FR-001/006, AC-013 |
| PUT /products/{id} | ProductApplicationService.update | 200, replace name/price, keep ID/activity | BR-001/011–013, AC-001/011/012 |
| PATCH /products/{id} | ProductApplicationService.deactivate | 200, body {"active":false}; repeat succeeds | BR-012, AC-002/012 |
| POST /customers | CustomerApplicationService.create | 201, generated ID, Location /customers/{id}, initially active | FR-002/006, AC-003/012 |
| GET /customers | CustomerApplicationService.list | 200, all active/inactive customers; [] if empty | FR-002/006, AC-013 |
| GET /customers/{id} | CustomerApplicationService.get | 200, active or inactive record; absent UUID 404 | FR-002/006, AC-013 |
| PUT /customers/{id} | CustomerApplicationService.update | 200, replace name/email, keep ID/activity | BR-011/012, AC-003/011/012 |
| PATCH /customers/{id} | CustomerApplicationService.deactivate | 200, body {"active":false}; repeat succeeds | BR-012, AC-012 |

No pagination, sorting guarantee, filtering or hidden active-only repository scope is added. Customer DELETE is still PRD FR-002/AC-003/004/013: future unreferenced physical deletion or referenced deactivation/204. Proposed interim DELETE /customers/{id} returns 405 after the resource is implemented, with envelope/Allow header, as unsupported in this increment. Explicitly review this interim behavior. Order routes remain unimplemented, with no invented interim success behavior. Standard MVC HEAD/OPTIONS are transport behavior, not extra use cases.

#### Responsibilities and Boundaries

| Layer | Proposed responsibility |
| --- | --- |
| product/api, customer/api | Focused controllers, dedicated request/response records, UUID binding, structural presence/type validation, status/Location; no repository access |
| Own feature application | Concrete create/get/list/update/deactivate service; application-owned transactions, domain calls, repository lookup/save, missing-resource errors, immutable materialized result records |
| Domain | Existing Product.update/deactivate, Customer.update/deactivate, Money and EmailAddress remain authoritative for normalization/invariants; no HTTP types |
| Own persistence | Existing repositories plus only findAll for the accepted all-record lists; no generic CRUD superclass, foreign repository access, delete API or locking queries |
| shared/api | Proposed minimal ApiError carrier and one API-owned advice for both features; central catalog owns names/mappings |
| configuration | Proposed centrally wired Jackson configuration; preserve Boot auto-configuration and current profile/OSIV/migration settings |

Use separate application result records ProductResult(UUID, String, BigDecimal, boolean) and CustomerResult(UUID, String, String, boolean); response records carry the PRD fields. Controllers map results without database access. Input arguments may be ordinary typed fields; no unnecessary application command-interface hierarchy. Constructor injection; no cross-feature calls are needed for these operations. No recursive services or circular constructor dependencies.

Use request records ProductDetailsRequest(String name, BigDecimal price), CustomerDetailsRequest(String name, String email) for POST/PUT within their respective features, and DeactivateProductRequest/DeactivateCustomerRequest(Boolean active) for PATCH. Do not accept id/active in details requests or editable details in PATCH. Null and missing values must stay distinguishable from primitive defaults for required validation. Bean Validation supports presence/shape, not replacement of exact Unicode/email/price domain policy. Use domain factories/operations after field-aware validation; never bind JSON into aggregates. Avoid global IllegalArgumentException-to-400 handling or message parsing; validate fields in known context and propagate programming/infrastructure failures separately. Propose a small HTTP-independent InputValidationException with a field/category for known name/email/price input failures, translated only at the API boundary. The exact adapter to existing domain exceptions requires review; preserve aggregate operations and never infer categories from message text.

#### Transactions and Constraint Handling

Propose externally invoked Spring-proxied `@Transactional` application methods: read-only get/list; read-write create/update/deactivate. Each operation owns one database transaction; do not delegate to self-invoked annotated helpers. Load/create own aggregate, validate before mutation, save creation through the existing repository; managed updates use controlled domain operations and dirty checking. Materialize results before context closure, but return HTTP success only after the proxy completes commit. A successful flush does not prove commit. No transaction retry or ambient Order guard is implemented.

Database uniqueness remains authoritative: do not add an email precheck or query unless justified; accept unchanged self-owned email, reject another active/inactive owner's normalized email. Inspect the root SQLSTATE and named index `uq_customers_email` (23505) even at commit; API advice maps confirmed conflict to catalog 409 outside the transactional proxy. Do not continue failed contexts or reinterpret all integrity failures. No migration is expected.

**Open — Human Decision Required: concurrent activity preservation.** Hibernate's default whole-row update may overwrite a concurrently committed deactivation with stale active=true. Even PUT followed by another PUT can lose detail changes; no new conflict/version business behavior is selected. Recommend a narrowly scoped Hibernate `@DynamicUpdate` mapping enhancement on Product/Customer so PUT's SQL writes editable dirty columns without stale active, with PostgreSQL tests for overlapping PUT/PATCH in both completion orders and SQL inspection. See the [Hibernate DynamicUpdate reference](https://docs.hibernate.org/orm/7.4/javadocs/org/hibernate/annotations/DynamicUpdate.html) for dirty-column SQL and its runtime-generation cost. This is a Proposed implementation refinement requiring explicit approval, not a silently changed D1–D10 selection or verified guarantee. Alternative: reviewed column-scoped repository writes after domain validation, with careful context/result refresh; broader optimistic versioning or explicit pessimistic coordination is deferred and not approved here. Investigate actual generated SQL and result freshness before claiming success. Report incompatible behavior rather than extending ADR-0002. Full Order Placement/mutation coordination remains a later gate.

#### Global JSON and Errors

Follow the [source-inspected Boot/Jackson proposal](../../02-architecture/rest-api-design.md#boot-411--jackson-317-source-inspection). Explicit global unknown-field rejection is Accepted; exact properties, coercion policy, request records and advice require approval. Preserve BigDecimal source scale before Money conversion. Reject overprecision rather than rounding; number token prices are proposed, numeric strings/booleans/objects/arrays rejected. Names/emails must not accept numeric/boolean coercion. Deactivation permits only literal false; unknown and recognized prohibited fields have distinct Proposed codes.

The [central error catalog](../../02-architecture/api-error-codes.md) specifies twelve initial names and proposed INVALID_RESOURCE_ID; approve names, precedence, PATCH/UUID/405 mappings and safe technical failure policy before implementation. Framework 415/406 and uncertain commit outcomes are Open; do not silently turn them into business 400/409 or publish SQL diagnostics. Domain/application exceptions are HTTP-independent; common advice owns exception-to-response translation.

#### Dependencies and Framework Verification

Existing production webmvc/validation/Jackson/JPA dependencies suffice for the proposal. Recommend the Boot-managed test-scope `org.springframework.boot:spring-boot-starter-webmvc-test` for Boot 4 MockMvc/WebMvcTest support, subject to explicit dependency approval. Its coordinate was found in the locally cached Boot 4.1.1 BOM; no download/runtime proof is claimed. Keep spring-boot-starter-test unless a separately reviewed change removes redundancy; no independent Jackson/JUnit/Spring override. Unit tests can use existing test dependencies. If the MVC starter is not approved, revisit the HTTP harness without silently adding dependencies.

#### Proposed Implementation Files

Paths below are relative to `src/main/java/com/example/agenticorderservice/`, except fully specified paths. They describe the minimal candidate scope, not generated files.

| Proposed file(s) | Change / purpose |
| --- | --- |
| product/application/ProductApplicationService.java, ProductResult.java, ProductNotFoundException.java | Five use cases, materialized result, HTTP-independent missing-resource signal |
| customer/application/CustomerApplicationService.java, CustomerResult.java, CustomerNotFoundException.java | Five use cases, materialized result, HTTP-independent missing-resource signal |
| product/api/ProductController.java, ProductDetailsRequest.java, DeactivateProductRequest.java, ProductResponse.java | Product MVC transport boundary |
| customer/api/CustomerController.java, CustomerDetailsRequest.java, DeactivateCustomerRequest.java, CustomerResponse.java | Customer MVC transport boundary |
| shared/application/InputValidationException.java | Minimal HTTP-independent signal for known field/category validation failures; no generic validation framework |
| shared/api/ApiError.java, ApiExceptionHandler.java | Shared API-only envelope and central typed translation; add a small field-validation helper here only if reviewed code needs it |
| configuration/JacksonConfiguration.java | Global reviewed Jackson 3 coercion customization |
| product/persistence/ProductRepository.java; customer/persistence/CustomerRepository.java | Add findAll only; preserve restricted repository surface |
| product/domain/Product.java; customer/domain/Customer.java | Only if the proposed concurrent write-set refinement is explicitly approved; preserve domain behavior, IDs and accepted mapping choices |
| src/main/resources/application.yml | Explicit global reviewed JSON properties; preserve OSIV/profile/database defaults |
| pom.xml | Only approved Boot-managed MVC test starter in test scope; retain existing plugins/version overrides |
| src/test/java/com/example/agenticorderservice/product/application/ProductApplicationServiceTest.java | Isolated application operations, validation and inactive cases |
| src/test/java/com/example/agenticorderservice/customer/application/CustomerApplicationServiceTest.java | Same plus email normalization/uniqueness translation responsibility |
| src/test/java/com/example/agenticorderservice/product/api/ProductApiTest.java | Real MVC dispatcher/binding/validation/controller/advice with stub application, no Docker |
| src/test/java/com/example/agenticorderservice/customer/api/CustomerApiTest.java | Same, customer normalization/errors |
| src/test/java/com/example/agenticorderservice/product/api/ProductApiIT.java | MVC/full Boot context plus existing PostgreSQL Testcontainers, transactions and unchanged-state evidence |
| src/test/java/com/example/agenticorderservice/customer/api/CustomerApiIT.java | Same plus real uniqueness/commit conflict evidence |
| src/test/java/com/example/agenticorderservice/configuration/JacksonConfigurationTest.java | Global type/unknown-field tests, including a test-only nested carrier (not an Order endpoint) |
| This playbook and relevant existing indexes | Actual execution/diff/test/human-review evidence after separate authorization |

New classes are proposals; no generic repository/service abstraction or framework is needed. Existing Order code/tests, converters, V1, CI workflow and central PostgreSQL properties stay unchanged. Preserve smoke tests; no integration skip when Docker is unavailable.

### Planned Test Matrix — All Results Pending

U = isolated application unit test (`*Test`); H = actual MVC binding/validation test (`*Test`, no Docker); P = full application/MVC plus real PostgreSQL Testcontainers (`*IT`, Failsafe). H never means direct controller invocation. P asserts committed state through fresh transactions; avoid test-wide rollback transactions that hide commit failures. Required tests are mandatory once implementation is approved; coverage does not substitute for them.

#### Per-Endpoint Matrix

| Endpoint | U responsibilities | H responsibilities | P responsibilities / PRD |
| --- | --- | --- | --- |
| POST /products | Valid/boundary fields, domain invocation, generated result | 201, Location matches ID, exact fields, initially active; full input-error matrix | Generated UUID, normalized name/exact price persisted; rejected create adds no row; AC-001/011/012 |
| GET /products | All-record result, empty list | 200/[], complete fields, active and inactive | Both states returned, serialization after transaction without queries; AC-013 |
| GET /products/{id} | Found active/inactive; absent signal | 200 fields, absent UUID 404; malformed UUID proposed 400 | Stored values retrieved, missing lookup unchanged DB; AC-013 |
| PUT /products/{id} | Replace both editable fields, keep ID/status; invalid update leaves state | 200 full response, required fields, reject id/active | Fresh committed name/price, inactive remains false; invalid input no change; approved PUT/PATCH safety check; AC-001/011/012 |
| PATCH /products/{id} | Deactivate active/inactive idempotently, missing signal | 200 inactive; {}, null, true, wrong types and extra fields rejected | Row retained, repeat works; invalid PATCH unchanged; approved concurrent write-set check; AC-002/012 |
| POST /customers | Domain name/email handling, result; conflict responsibility | 201/Location/ID, complete fields/active, customer error matrix | Normalized email stored; duplicate active/inactive case returns 409 with no added row; AC-003/011/012 |
| GET /customers | All-record result, [] | 200 all fields/statuses, [] | All stored records including inactive, no lazy response queries; AC-013 |
| GET /customers/{id} | Found active/inactive; absent signal | 200 or 404; invalid UUID proposed 400 | Current stored details, missing lookup no mutation; AC-013 |
| PUT /customers/{id} | Replace name/email, preserve ID/activity; same email accepted | 200 response; incomplete/prohibited fields rejected | Own normalized email allowed; other's active/inactive email 409, both old fields/activity preserved; commit-time conflict; AC-003/011/012 |
| PATCH /customers/{id} | Active-to-inactive/repeat, no deletion | 200 false, body/type/unknown/reactivation errors | Row/email retained, repeat no new row; invalid PATCH unchanged; approved write-set check; AC-012 |

#### Shared Boundary and Negative Matrix

| Cases | Levels / expected evidence |
| --- | --- |
| Missing/null name, price/email, body and PATCH active | H/P 400 with approved code/message; no insert/mutation. U verifies invalid inputs before mutation. |
| Names stripped with tabs/Java Unicode whitespace; lengths 1/200/201 code points; blank, supplementary character, case/accents/umlauts | U/H/P preserve valid text, count supplementary character once; 201/blank rejected. Reuse accepted unpaired-surrogate rejection; no new ASCII restriction. |
| Email ASCII grammar, exact 254/255 limit, String.strip plus Locale.ROOT, repeated normalization | U/H/P approved grammar and 254 accepted/255 rejected; quoted/internationalized/invalid-dot/domain cases rejected. No DNS/new local-part length limits. |
| Provider a.b/ab/a+tag and punctuation/internal hyphen variants | U/H/P valid distinct normalized addresses; no provider collapse. |
| Duplicate email with different case, active or inactive owner; update self-owned email | U tests application behavior, H mapping, P database 23505/uq_customers_email enforcement and 409 at transaction completion; deterministic concurrent unique conflict where applicable. |
| Product 0, 0.0, 0.00, 999999.99, -0.01, 1000000.00, 1.001, 10.000 | U/H/P exact accepted limits/scale; reject overprecision before rounding/storage. No double conversion. |
| Price integer/decimal versus string, boolean, array/object/null; numeric/boolean name/email; wrong PATCH type | H with actual mapper, U validation as relevant, P no unintended state; approve coercion mappings first. Scientific notation/duplicate keys remain decision cases, not invented expected outcomes. |
| Unknown fields, id injection, active on POST/PUT, PATCH editable fields/true | H/P reject whole body with 400/approved specific code; no partial changes. Test root rejection everywhere; nested strictness through a test-only DTO and wrong nested shapes, not a future Order endpoint. |
| Empty/malformed JSON, invalid root shape, absent/invalid UUID | H/P agreed 400 mappings, well-formed missing UUID 404. Multiple-error precedence is an approval gate. |
| Repeated PATCH, PUT of inactive records, lists with inactive and no records | U/H/P preserve irreversible deactivation, ID and rows; no hidden active filter. |
| Product DELETE and interim customer DELETE | H/P 405/envelope/accurate Allow; no delete method, no check-then-delete; customer deletion remains deferred requirement. |
| All error responses and serialization | H/P approved code plus safe nonempty message; no SQL/stack traces/passwords/personal values. Verify complete response fields after application transaction, OSIV false and no serialization SQL. Technical/media errors require approved policy. |
| Rejected creates/updates/deactivations | P compare before/after rows and fresh loaded editable fields/activity; uniqueness rollback restores previous values. |
| Boundaries and suite preservation | Review imports: controllers never repositories, application never API DTOs, no foreign persistence/cycles. Keep existing 141 tests and Surefire/Failsafe/JaCoCo/CI/Sonar; do not change thresholds or claim future concurrency gates Passed. |

### Approval Gates and Proposed Sequence

1. Review endpoint scope/interim customer DELETE, catalog names/UUID/PATCH/multi-error/media/technical mappings, DTO/numeric coercion behavior and source-scale treatment.
2. Approve concrete application transaction mechanisms, concurrent activity-preserving write approach, HTTP harness and test dependency. ADR-0002 remains Proposed; full locking/retries not included.
3. Explicitly authorize implementation files/scope. Then implement application operations/results with isolated unit tests, global JSON/advice/DTO/controller behavior with actual MVC tests, and PostgreSQL HTTP/transaction tests incrementally. No migration/dependency beyond expressly approved items.
4. Run Java 21 `./mvnw -B -ntp test`, `./mvnw -B -ntp verify`, inspect dependency tree, Surefire/Failsafe/JaCoCo reports and Git diff/whitespace/documentation links. Docker must be available for P; preserve local DB/volumes, use existing Testcontainers image/configuration. No production migration or autonomous data operation.
5. Record actual commands/results/version changes/issues separately from expected results; unavailable gates Pending. Show complete diff including new/staged/unstaged files. Mark implementation Executed – Pending Human Review, wait for explicit acceptance; no commit/push without explicit authorization.

### Future Implementation Instructions — Pending Human Approval

Implement only the reviewed ten-endpoint design after the human explicitly approves the gates above and authorizes execution. Read the authoritative documents and exact approved refinements again, preserve D1–D10/PRD/version decisions and reuse Increment 3 code. Follow the sequence and mandatory matrix above. Report a genuine conflict before dependent work; do not silently choose an Open mapping or add a dependency. Exclude DELETE/customer-delete coordination, Order APIs/placement, locks, retries, ADR-0002 protocol and migrations. Present complete diff, actual test evidence and residual risks, then wait for human acceptance and separate commit authorization. These instructions are a proposed future scope, not execution permission or a replacement of the original prompt.

### Result

Planning completed on 2026-10-14: prepared six scoped documentation files, inspected existing implementation/tests/POM and release-specific cached Boot/Jackson sources, and identified approval gates. Existing version-documentation changes were preserved. No application/test code, POM, migration, database or CI file was changed; no build/API/database test was executed in this documentation task. Implementation and all matrix results remain Pending.

#### Verification Evidence

Documentation links/anchors, Markdown tables/fences, ADR identifiers/statuses, untouched accepted requirements/implementation and Git whitespace/diff checks are validated during this planning task. Final checks: internal Markdown links/anchors passed (305 checked); all six scoped files passed table/fence review and tracked/untracked whitespace checks. All five ADR identifiers/statuses, PRD, source/tests, POM, V1 and CI remained unchanged relative to task start, verified by file hashes. Original prompt content matches the supplied attachment. This is not runtime JSON/HTTP/concurrency proof.

#### Problems Encountered

The target playbook existed as an empty untracked file and is populated in place. Source review found a potential stale-activity overwrite with default Hibernate updates; the write-set approach requires review. Boot 4 MVC test support requires an explicitly approved test dependency. Existing historical stale implementation statements in the system/technology documents are not rewritten by this scoped task; current Increment 3 acceptance is recorded in their later status/history sections and authoritative ADRs. No evidence is invented to close these gaps.

### Lessons Learned

Planning observations: unknown-property rejection alone does not establish numeric/coercion correctness; transaction completion must be observed before HTTP success and conflict translation; an accepted irreversible-deactivation rule also requires review of actual SQL write sets. Runtime implementation lessons remain Pending.


## Final Human Acceptance — 2026-10-15

**Status: Reviewed – Accepted.** The human reviewer explicitly confirms successful review of the implementation and tests, passing automated tests, successful SonarQube analysis with zero reported findings, and the implementation committed to `develop`. This acceptance covers the Product & Customer API increment and its reviewed follow-up corrections and test hardening. It supersedes earlier pending-review statements without rewriting their historical evidence or prompts.

### Acceptance Evidence

- Git inspection on 2026-10-15: local `develop` and the locally recorded `origin/develop` both point to **`8e66d8c1cc454c5300a1622ca3adc4c3d6d75f6e`** (`test: use explicit map assertions for transaction outcomes`). The working tree was clean before this documentation update. No remote fetch was performed.
- Initial API implementation: `4c4fa66cb58503df6b83bb8c92ab8e8bb0cac474`; subsequent schema-isolation and SonarQube corrections are included in the accepted branch history. Test hardening is committed as `54eb491324fe9cc1bf9ef9c706ca481e537dd986`; the final map-assertion correction is the branch-tip commit above.
- Existing local verification log `/tmp/increment4-sonar-map-final-verify.log` records Java 21 Maven Wrapper `./mvnw -B -ntp verify`, **BUILD SUCCESS** on 2026-10-14 at 18:02:25 Europe/Berlin: **309 Surefire tests and 94 Failsafe integration tests**, zero failures, errors or skips. This is the final pre-commit run, not a newly executed build; the human confirms the committed implementation passes. Temporary logs are supplementary local evidence, not version-controlled artifacts.
- Read-only SonarQube MCP verification on 2026-10-15 for `uvormbrock_agentic-order-service`, branch `develop`: **0 OPEN/CONFIRMED issues** and quality gate **OK**, consistent with the human's zero-findings confirmation. This records the queried analysis state; no issue status was changed. No analysis-to-commit association was independently established by these responses.

No build was rerun for this documentation-only acceptance update. Earlier test counts and execution evidence remain historical rather than being replaced by the final run. A clean SonarQube result and passing tests do **not** constitute mutation-testing evidence, a mutation score, or a security guarantee.

### Deferred Functionality and Verification

Customer DELETE/delete-or-deactivate coordination, Order REST endpoints and placement, TransactionTemplate/ambient guard, deterministic pessimistic locking and retries remain deferred. ADR-0002 stays **Proposed and implementation-unverified**; full Order concurrency and uncertain-commit fault-injection obligations remain Pending. ADR-0006 remains **Proposed as a complete record**; accepted mapping/version subdecisions and verified Product/Customer optimistic behavior do not silently change that status.

Persistent local V2 migration remains separately **Blocked — Human Action Required** pending target/history and usable recovery evidence; disposable Testcontainers verification does not close it. Production migration/deployment/permissions and concrete automated architecture-enforcement tooling remain Open. Mutation testing has not been executed or authorized by this acceptance. No later increment or new commit is authorized by this record.
