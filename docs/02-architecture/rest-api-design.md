# REST API Design — Agentic Order Service

**Status:** Draft — implementation pending  
**Scope:** Request validation, strict JSON handling, and API error responses  
**Related:** [Product Requirements](../01-requirements/product-requirements.md) · [Domain Model](domain-model.md) · [API error codes](api-error-codes.md)

## 1. Purpose

Capture the agreed REST request-validation requirements before implementing the Spring Boot API. This is an initial design note, **not** a complete API specification.

The Product Requirements Document (PRD) is authoritative for endpoint behavior and HTTP status codes. In particular, unsupported request fields and caller-supplied pricing fields must be **rejected with HTTP 400**, not silently ignored.

### Decision Scope

Accepted API requirements come from the PRD: endpoints, response fields, status semantics, strict request rejection, server-authoritative pricing, and validation limits. Separate API DTOs, application orchestration, and transaction-safe materialization are accepted architecture conventions.

Spring MVC, Jackson configuration, Bean Validation annotations/provider wiring, illustrative DTO field types, and concrete exception-handler classes remain Proposed implementation choices. Boot 4.1.1 is Accepted; choosing a framework mechanism does not change the HTTP contract. Use the [technology reference](technology-stack.md) for individual tool statuses.

## 2. API Boundary and DTOs

- Define dedicated request DTOs; do not deserialize external JSON directly into domain objects or JPA entities.
- Use separate response DTOs for externally exposed fields.
- For order creation, accept `customerId` and a nonempty `items` collection containing only `productId` and `quantity`.
- Generate IDs on the server; do not accept client-assigned record IDs.
- Resolve authoritative server-side unit prices and calculate line totals, subtotal, discount, and payable total exclusively on the server.

**Illustrative request DTOs (design sketch, not implementation):**

```java
public record OrderItemRequest(UUID productId, Integer quantity) {}

public record CreateOrderRequest(
    UUID customerId,
    List<OrderItemRequest> items
) {}
```

`Integer` is used here to allow validation to distinguish a missing quantity from a supplied numeric value. Exact DTO types and annotations will be finalized during implementation.

## 3. Strict JSON Deserialization

- Reject any unknown or unsupported JSON property with **400 Bad Request**, including properties nested inside `items`.
- Reject caller-supplied monetary fields such as `unitPrice`, `subtotal`, `discount`, or `total` rather than ignoring them.
- Reject read-only fields such as `id` in create/update bodies unless explicitly permitted by the PRD.
- Apply the same strictness to relevant product and customer requests.
- In the Proposed Jackson implementation, configure and verify unknown-property handling explicitly; **do not rely on framework defaults**.
- Spring Boot **4.1.1** is accepted in [ADR-0005](../03-decisions/ADR-0005-technology-stack.md). The exact strict-deserialization configuration must match its selected Jackson stack; do not reuse the unapproved Boot 3.5 skeleton configuration.

## 4. Request Validation

The Proposed Bean Validation/parsing approach must enforce the following Accepted PRD rules; no particular annotation or provider setting is selected here:

- Required `customerId`, `productId`, and order items.
- At least one order item.
- Positive, integral quantities; maximum **10,000 per product after duplicate consolidation**.
- Required names normalized with `String.strip()` to 1–200 Unicode code points.
- Email `String.strip()`/`Locale.ROOT` normalization, approved ASCII grammar, maximum 254 Unicode code points, and case-insensitive uniqueness across active/inactive customers.
- Product prices between EUR 0.00 and EUR 999,999.99 inclusive, with no more than two decimal places.
- No activation through unsupported request fields.

Validation must cover both **request structure** and **business rules**; field-level annotations alone are not sufficient for cross-item rules such as consolidated quantity limits.

## 5. Error Handling

Provide consistent JSON errors with at least:

```json
{
  "code": "UNSUPPORTED_FIELD",
  "message": "The request contains an unsupported field."
}
```

Specific machine-readable codes, names/statuses and category precedence are Human Accepted for Increment 4 in the central [API error catalog](api-error-codes.md); framework implementation remains pending. The PRD remains authoritative for HTTP statuses. Use its established semantics:

| HTTP status | Examples |
| --- | --- |
| `400` | Malformed JSON, unknown fields, caller-provided prices or totals, invalid quantities, missing required fields, invalid names/email/price |
| `404` | Referenced customer or product does not exist; requested resource not found |
| `409` | Inactive customer or product referenced by a new order; duplicate normalized customer email |
| `405` | Unsupported resource methods, as defined by the PRD |

Technical transaction failures and uncertain commit outcomes are **not** automatically mapped to business-validation conflicts; the accepted business policy is defined in [ADR-0001](../03-decisions/ADR-0001-order-placement-concurrency.md). [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) proposes technical locking, retries, and error handling; it remains Proposed and implementation-unverified, including handling where no HTTP response is possible.

### Exception-to-Response Ownership

The API boundary owns exception-to-HTTP-response translation and the consistent error envelope. Controllers delegate to application operations; domain/application code must not construct HTTP responses or depend on status codes. A concrete shared handler class or framework advice configuration remains Proposed.

Domain and application failures express invalid input, missing references, or business conflicts; translate them to the existing PRD statuses. Persistence/infrastructure failures reach the application transaction boundary for rollback or safe retry classification before API translation. Map a known normalized-email constraint violation to its existing business conflict; do not treat every database exception as a domain conflict.

For Increment 4, safe technical 500 and classified temporary-outage 503 responses are Accepted in the central catalog. Detailed Order retry/timeout/reconciliation behavior remains Proposed/Open in ADR-0002. Unknown commit outcome must not be represented as confirmed rollback and may prevent any response. Keep SQL/internal details out of messages; no additional response field or automatic write replay is selected.

## 6. Integration-Test Checklist

Tests must exercise actual HTTP/JSON binding and validation (not merely invoke controller methods). Spring MVC binding is the Proposed framework path; choosing the test harness remains a separate implementation detail.

| Scenario | Expected result |
| --- | --- |
| Valid order request | `201 Created` |
| `unitPrice` inside an order item | `400 Bad Request` |
| `subtotal`, `discount`, or `total` in the root order request | `400 Bad Request` |
| Unknown root property, e.g. `foo` | `400 Bad Request` |
| Unknown nested property inside `items[]` | `400 Bad Request` |
| Client-assigned `id` | `400 Bad Request` |
| Missing or null `customerId` | `400 Bad Request` |
| Missing or empty `items` | `400 Bad Request` |
| `quantity` is `0` or negative | `400 Bad Request` |
| `quantity` is fractional, e.g. `1.5` | `400 Bad Request` |
| Duplicate product items exceed 10,000 combined | `400 Bad Request`; no order persisted |
| Invalid request is rejected | No order or order items persisted |
| Unknown fields in product/customer request | `400 Bad Request` |
| Client attempts to reactivate a record | `400 Bad Request` |
| Product/customer name has 1 or 200 Unicode code points after String.strip() | Accepted on create/update with otherwise valid input; stored name is trimmed. |
| Product/customer name is missing, empty after trimming, or has 201 Unicode code points | `400 Bad Request` |
| Email ` Alice@Example.com ` | Stored as `alice@example.com` on create/update. |
| Otherwise valid email has 254 Unicode code points after normalization | Accepted on create/update. |
| Email is missing, syntactically invalid, or has 255 Unicode code points after normalization | `400 Bad Request` |
| Another active or inactive customer owns `alice@example.com`; submit `ALICE@example.com` | `409 Conflict` on create/update. |
| Email differs only by provider-specific dots or plus addressing | Do not collapse distinct addresses using provider-specific rules. |
| Product price is EUR 0.00 or EUR 999,999.99 | Accepted on create/update. |
| Product price is EUR -0.01, EUR 1,000,000.00, or EUR 1.001 | `400 Bad Request` |
| Quantity is 1 or 10,000; duplicate quantities 6,000 and 4,000 | Accepted for eligible references; duplicates consolidate to one item with quantity 10,000. |
| Quantity is 10,001 or duplicate quantities are 6,000 and 4,001 | `400 Bad Request`; no order persisted. |

Also verify that every error response contains `code` and `message`.

## 7. Open Technical Decisions

- Concrete lexeme-preserving strict-deserialization/precedence configuration and verification; Java 21, Boot 4.1.1 and the approved Jackson 3.1.7 security override are already selected.
- DTO field types, Bean Validation annotations, and exception-to-HTTP mapping.
- Catalog names/statuses, single-error category precedence and PATCH mappings are Accepted; concrete classification/ordering/error-rendering mechanisms and unselected edge cases require verification.
- Concrete integration-test setup under the [testing strategy](../05-testing/testing-strategy.md), using Accepted JUnit 6 and Boot 4.1.1-managed Jupiter/Platform versions. PostgreSQL/Testcontainers is adopted; the protocol remains unverified.
- Technical implementation and verification of the Proposed ADR-0002 strategy under accepted ADR-0001, including timeouts, retry delays, and uncertain commit reconciliation.

## 8. Implementation Reminder

**Before declaring the REST API complete:** prove with HTTP-level integration tests that unknown fields—including nested fields and attempted price overrides—return **400**. A server that calculates prices correctly but silently accepts those invalid requests **does not meet the PRD**.

## 9. Increment 4 Accepted Direction and Proposed Mechanisms

**Architecture decisions documented; Increment 4 implementation Reviewed – Accepted (2026-10-15).** See the [Increment 4 playbook](../04-agentic-coding/prompts/05-product-customer-api.md) for the ten-endpoint contract, test matrix, file proposal and approval gates. Earlier Order examples/checklists above remain later MVP obligations, not Increment 4 scope.

The human accepted: (A) centrally configured global strict JSON deserialization; (B) specific machine-readable error codes with at least `code`/`message`; (C) deferral of customer DELETE; and (D) mandatory application-unit, actual MVC/API and real PostgreSQL test levels. The subsequent Human Accepted A/B1–B11 decisions below resolve catalog mappings and API behavior; scoped framework/migration implementation and human acceptance are recorded in [final Increment 4 acceptance](../04-agentic-coding/prompts/05-product-customer-api.md#final-human-acceptance--2026-10-15). Persistent local migration remains separately blocked.

### Scope and Interim HTTP Behavior

Increment 4 covers POST/collection GET/individual GET/PUT/PATCH for each of `/products` and `/customers`. PATCH accepts only `{"active": false}`; `active` is prohibited in POST/PUT, not universally prohibited. PUT requires both editable fields and retains current activity. Reads and updates include inactive records. Customer DELETE remains an MVP requirement but is deferred because it must coordinate safely with Order Placement under ADR-0001 and Proposed ADR-0002. Do not add a check-then-delete operation.

Accepted interim behavior: DELETE /customers/{id} returns 405/OPERATION_NOT_AVAILABLE with the fixed catalog message; product DELETE and other unsupported methods use 405/METHOD_NOT_ALLOWED. Both require an accurate `Allow` header. No customer existence/reference query or temporary deletion is implemented. This is temporary lack of implementation, not removal of PRD customer DELETE/204. HEAD/OPTIONS may remain standard MVC capabilities; they are not additional business endpoints. Order endpoints remain unimplemented; their interim routing/error behavior is not a promised contract in this increment.

### Boot 4.1.1 / Jackson 3.1.7 Source Inspection

Inspected cached release sources and configuration metadata on 2026-10-14; no dependencies were downloaded and no runtime binding tests were executed. Jackson's selected version is the approved security override in the [technology reference](technology-stack.md#approved-security-version-overrides--2026-10-14).

Proposed configuration: explicitly set `spring.jackson.deserialization.fail-on-unknown-properties=true` and `spring.jackson.deserialization.accept-float-as-int=false`. Use Boot's `org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer` for additional `tools.jackson.databind` coercion settings. Do not use Jackson 2 builder APIs, switch to Jackson 2 defaults, or replace Boot's mapper/converters unnecessarily.

Proposed strict DTO types: String name/email, BigDecimal price, boxed Boolean active. Reject number/boolean-to-String and String-to-number/boolean coercion using `withCoercionConfig` with `LogicalType.Textual`, `LogicalType.Float`, `LogicalType.Boolean` and `CoercionAction.Fail` for inappropriate shapes; reject empty-string conversion as well. Candidate coercion settings (all Proposed): for `LogicalType.Textual`, fail `Integer`, `Float` and `Boolean`; for BigDecimal/`LogicalType.Float`, fail `String`, `EmptyString` and `Boolean`; for `LogicalType.Boolean`, fail `String`, `EmptyString`, `Integer` and `Float`. Keep empty textual name/email values available to their own domain validation rather than misclassifying them as type errors. Unknown-field rejection cannot distinguish a recognized prohibited `id` from an arbitrary unknown property without request-type-aware classification. Strict scalar types and ordinary decimal price notation are now Accepted; the exact configuration still requires implementation and MVC verification. These initial configuration suggestions alone do not establish accepted whole-request error precedence or source-token lexical checks. `accept-float-as-int=false` does not establish BigDecimal price precision or solve every coercion path. Preserve source BigDecimal scale, reject `10.000`, then reuse Money; do not round or strip trailing zeros before validation. No primitive default may hide a missing/null PATCH field. Wrong object/array/root shapes must return 400, and global unknown-property handling must apply recursively without ignore annotations.

No new production dependency is expected: existing webmvc/validation/Jackson artifacts provide these APIs. The earlier candidate spring-boot-starter-webmvc-test is not needed for the approved builder-based MockMvc setup. Existing spring-test provides MockMvc/MockMvcBuilders and @MockitoBean; preserve the POM. Boot slice/auto-configuration annotations would require separately approved tooling; do not copy Boot 3 test imports.

### Central Translation and Validation

Propose one API-owned `@RestControllerAdvice`, optionally extending Spring MVC's `ResponseEntityExceptionHandler`, to translate typed parsing/binding failures, Bean Validation results, domain/application failures, missing records and known uniqueness violations. Domain/application code remains HTTP-independent. Use typed field/context classification rather than parsing exception text or catching all IllegalArgumentException as a client error. Separate unknown fields from recognized prohibited fields and enforce the Accepted six-category catalog precedence; detailed processing mechanics are proposed below.

Bean Validation enforces required presence/DTO shape; existing domain types remain authoritative for code-point name counting, strip/lowercase email policy and exact Money/range validation. Generic `@Size`, `@Email` and `@Digits` alone cannot establish these exact rules (including overprecision trailing zeros). Reject invalid input before mutation and preserve aggregate atomic updates. A confirmed PostgreSQL `23505` for `uq_customers_email` maps to duplicate email/409 even if raised during commit; do not map arbitrary integrity failures to 409 or continue a rollback-only transaction. Catch transaction completion failures outside the proxied service boundary, with HTTP mapping at the API boundary and no retries.

Public messages are safe templates, never exception messages, SQL, rejected sensitive values or stack traces. The catalog now accepts technical/media/negotiation/routing mappings; exact safe rendering and outage classification require verification. Uncertain completion does not establish rollback. No Order retry/reconciliation implementation is approved.

Sources: [Spring MVC mapper customization](https://docs.spring.io/spring-boot/how-to/spring-mvc.html), [Boot testing reference](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html), and release-specific cached `spring-boot-jackson-4.1.1-sources.jar`, `spring-boot-dependencies-4.1.1.pom`, `jackson-databind-3.1.7-sources.jar`. Live documentation may change; verify imports/settings against the pinned artifacts during the authorized implementation.

## 10. Human-Accepted Increment 4 API and Transaction Decisions

Recorded 2026-10-14; [playbook decision record](../04-agentic-coding/prompts/05-product-customer-api.md#approved-architecture-and-api-decisions--2026-10-14). This section supersedes the earlier proposal only where expressly decided. The PRD's existing business rules, response fields and final customer DELETE/204 remain intact.

### Input Contract

- Global strict DTO binding: unknown/recognized forbidden properties, including future nested DTO properties, return 400. Never deserialize JPA/domain objects. Read-only IDs are forbidden in create/update bodies.
- String fields require JSON strings, price JSON numbers and active JSON booleans, without string/number/boolean coercion. Wrong types use INVALID_FIELD_TYPE; missing/null required fields use REQUIRED_FIELD_MISSING. Monetary handling uses BigDecimal without intermediate float/double.
- Price uses ordinary decimal notation, EUR 0.00–999,999.99 and at most two explicitly supplied decimal places. Accept 149, 149.9, 149.99; reject 149.990/10.000 and 1.4999e2/1e3 as INVALID_PRODUCT_PRICE. Numeric strings remain INVALID_FIELD_TYPE. Preserve original numeric lexemes to check exponent/scale before constructing Money; no changes to Money or D6 are authorized.
- Path UUIDs must have 36 characters and standard hyphens; reject lenient/short forms with INVALID_RESOURCE_ID/400 before UUID parsing. A valid absent UUID produces the feature's NOT_FOUND/404. D5 accepts upper/lower/mixed hexadecimal case with lowercase serialization; no new UUID generation/version restriction is selected.
- PATCH accepts only {"active":false}. Empty/missing/null active uses REQUIRED_FIELD_MISSING; true uses UNSUPPORTED_FIELD; string/number active uses INVALID_FIELD_TYPE; editable/read-only extra fields use UNSUPPORTED_FIELD, random extra fields UNKNOWN_FIELD. Repeated inactive PATCH succeeds/200; rejected requests mutate nothing.

Exactly one code/message follows the Accepted precedence in the [catalog](api-error-codes.md#accepted-error-precedence), independent of incoming property order. Selected minimal approach: a small central strict decoder/MVC read converter fully parses the JSON object, retains price token spelling, scans properties, then checks types, presence and business values before application lookup/mutation. HTTP routing/method/headers and canonical path validation run first under D6. Deterministic field/category processing must precede fail-fast typed binding/Bean Validation. Human Accepted same-category order is name→price/name→email/active; unknown/forbidden paths sorted lexicographically. Do not claim that enabling unknown-property rejection alone proves precedence. D1–D8 below finalize duplicate-key, body/path/media and tie-breaker policies. Exact parser/buffering/DTO wiring remains subject to runtime verification.

### Transactions, Optimistic Conflicts and Failures

Accepted: externally invoked Product/Customer application-service write methods use @Transactional, reads @Transactional(readOnly=true); controllers do not own transactions, and domain code does not use Spring transaction APIs. Materialize immutable application results within the transaction; response mapping/serialization performs no later persistence access. API error mapping runs outside the application transaction. Commit must succeed before HTTP success, and confirmed uniqueness/optimistic failures at flush/commit map to the catalog. Proxy/self-invocation and interceptor ordering require implementation verification.

Human accepted @Version on Product/Customer, replacing the earlier unversioned write-set/DynamicUpdate proposal. [ADR-0006](../03-decisions/ADR-0006-product-customer-optimistic-locking.md) is Proposed as a complete record: its direction is human-approved, but the exact mapping/schema choices and four-statement V2 SQL are reviewed; scoped implementation/runtime verification passed and human acceptance is recorded in [final Increment 4 acceptance](../04-agentic-coding/prompts/05-product-customer-api.md#final-human-acceptance--2026-10-15). The original design phase planned V2 without modifying V1; actual V2 implementation and disposable verification are documented in the final Increment 4 acceptance record. Confirmed version conflicts use CONCURRENT_MODIFICATION/409 with no automatic retry. This does not provide stale-HTTP-client protection without a separately approved precondition mechanism; no ETag/If-Match, version response field or Order version is selected.

Use INTERNAL_SERVER_ERROR/500 for unexpected technical failures and SERVICE_UNAVAILABLE/503 only for clearly classified temporary dependency outages. Neither proves rollback. Known rollback means no attempt changes remain; uncertain completion may have committed, and failed transport can prevent any response. Full Order concurrency remains ADR-0001/Proposed ADR-0002, not replaced by Product/Customer versions.

### Framework and Verification Boundaries

The selected central MVC advice/direct JSON writer/error-dispatch design is specified in the current implementation prompt; scoped wiring and runtime evidence are human accepted; see [final Increment 4 acceptance](../04-agentic-coding/prompts/05-product-customer-api.md#final-human-acceptance--2026-10-15). It must emit the Accepted JSON envelope for 406 even when Accept excludes JSON, 415 for unsupported Content-Type, 404 for unknown endpoints and failures outside controller invocation. Verify explicit JSON error rendering/fallback without recursive negotiation or HTML/ProblemDetail leakage. The response contract is Accepted; precise wiring is implemented with passing scoped verification and human acceptance.

The approved minimal MockMvc setup uses existing spring-boot-starter-test/spring-test with MockMvcBuilders.webAppContextSetup; no new Maven test dependency is required or selected. Mandatory tests span isolated application operations, real MVC binding/headers/statuses, and actual PostgreSQL constraints/version conflicts/migration/rollback; see the updated [test strategy](../05-testing/testing-strategy.md#increment-4-accepted-api-and-versioning-verification) and playbook matrix. No runtime evidence is claimed by this update.

## 11. Final Human Decisions D1–D8

**Status:** Human Accepted behavior; scoped implementation and runtime verification Reviewed – Accepted. These Increment 4 edge-policy identifiers are separate from the Accepted Increment 3 D1–D10 persistence decisions. The [definitive implementation prompt](../04-agentic-coding/prompts/05-product-customer-api.md#current-implementation-prompt--increment-4) is not execution authorization.

| Increment 4 decision | Accepted policy |
| --- | --- |
| D1 — body | POST/PUT/PATCH: absent body or {} → REQUIRED_FIELD_MISSING/400; whitespace-only, JSON null root or array root → MALFORMED_JSON/400. |
| D2 — duplicates | Duplicate object keys at any depth → MALFORMED_JSON/400; detect before tree construction collapses them. |
| D3 — trailing content | Only JSON whitespace after the complete root object; second value or other nonwhitespace → MALFORMED_JSON/400. |
| D4 — same-category order | Product name→price, Customer name→email, PATCH active; multiple unknown/forbidden paths lexicographically. Preserve six-category order independent of JSON input order. |
| D5 — UUID case | Exact 36-character 8-4-4-4-12 hexadecimal structure; accept upper/lower/mixed case, serialize lowercase. Reject malformed path with INVALID_RESOURCE_ID/400 before lenient parsing. |
| D6 — outer order | Routing/method/relevant headers (Content-Type/Accept) → path syntax → complete body validation → application lookup/business behavior. No resource lookup before validation. |
| D7 — media | Writes accept only application/json encoded in UTF-8, with optional explicit charset=UTF-8. Reject other media, including merge-patch+json/vendor+json/all other application/*+json, with UNSUPPORTED_MEDIA_TYPE/415. Successful responses application/json; 406 still uses the safe JSON envelope. |
| D8 — technical classification | Confirmed connection establishment failure or confirmed pre-write unavailability → SERVICE_UNAVAILABLE/503; unknown commit or unclassified technical failure → INTERNAL_SERVER_ERROR/500; confirmed optimistic conflict → CONCURRENT_MODIFICATION/409; confirmed uq_customers_email uniqueness → DUPLICATE_EMAIL/409. No automatic retries or unconfirmed rollback claims. |

The technical design uses feature DTOs, a small central decoder with original numeric tokens, typed API classification and safe direct JSON writing, externally proxied application transactions and immutable results. Do not build a generic JSON/schema framework. The selected extension points from source review are Boot ServerHttpMessageConvertersCustomizer/ServerBuilder.addCustomConverter and JsonMapperBuilderCustomizer; exact registration/UTF-8/header/path precedence and error dispatch require runtime tests. Avoid plain fail-fast binding as the sole implementation. ApiRequestInterceptor.preHandle must validate relevant headers, then path syntax before the body converter executes, not after @RequestBody argument resolution. D1 body classifications apply after the earlier routing/header/path stages pass. MVC can bypass a converter for absent input; use DTO-scoped RequestBodyAdviceAdapter.handleEmptyBody with a typed missing-field signal rather than message parsing, and distinguish JSON null/whitespace as malformed.

The safe error writer must bypass success content negotiation when rendering accepted errors, including incompatible Accept/406, and share its envelope with the framework error-dispatch fallback. Do not leak SQL/constraint/parser details or fall back to HTML/default ProblemDetail. Classifiers inspect typed cause chains and structured SQLSTATE/constraint evidence, including flush/commit causes and operation phase. No blanket SQLException/timeout/connection→503/409 mapping.

Eight behavioral choices are resolved. Scoped code wiring and runtime evidence are human accepted; complete ADR-0006 stays Proposed and Increment 4 is Reviewed – Accepted. See [final Increment 4 acceptance](../04-agentic-coding/prompts/05-product-customer-api.md#final-human-acceptance--2026-10-15). Production migration rollout and ADR-0002 are unaffected.
