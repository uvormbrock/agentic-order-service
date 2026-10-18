# API Error Codes

**Precedence — 2026-10-18:** The [approved Customer DELETE target](#approved-customer-delete-target--2026-10-18) supersedes earlier DELETE/405 and delete-or-deactivate contract statements for future implementation only. Existing Increment 4/5 sections and their verification statements retain their historical scope; they are not evidence for the new contract. Current runtime DELETE still follows delete-or-deactivate/204.

**Status:** Authoritative human-accepted catalog; Increment 4/5 acceptance records preserved; Customer DELETE target approved 2026-10-18, Phase 2 implementation and verification pending

**Scope:** Existing Product/Customer/Order catalog and historical contract decisions; the separately labeled Customer DELETE target below is not yet implemented

**Authority:** [PRD](../01-requirements/product-requirements.md) · [REST design](rest-api-design.md) · [approved Increment 4 record](../04-agentic-coding/prompts/05-product-customer-api.md#approved-architecture-and-api-decisions--2026-10-14)

## Contract and Ownership

Return exactly one `code` and one safe `message` per failed request, as a JSON object. No error array or extra response fields are selected. The PRD remains authoritative for business operations/statuses; the human's Increment 4 decisions accept the catalog below, strict input rules and additional technical statuses without canceling PRD requirements. This document replaces the initial Proposed catalog; exact framework wiring and unselected edge cases remain technical review items.

```json
{"code":"INTERNAL_SERVER_ERROR","message":"An unexpected server error occurred."}
```

Use fixed safe templates. Never expose SQL, stack traces, constraint names, parser internals, raw exception messages or sensitive input. HTTP mapping belongs to the API boundary, outside application transactions; domain/application exceptions contain no HTTP types.

## Accepted Catalog

P = Product; C = Customer. POST/PUT require their editable fields; PATCH permits only `{"active":false}`. Each example is a complete safe minimum response. Scoped implementation and runtime verification are recorded in [final Increment 4 acceptance](../04-agentic-coding/prompts/05-product-customer-api.md#final-human-acceptance--2026-10-15); this does not prove every possible failure permutation or fault-injection scenario.

| Code | HTTP | Trigger / applicable endpoints | Safe example response | Requirement / decision trace |
| --- | --- | --- | --- | --- |
| `MALFORMED_JSON` | 400 | Invalid syntax/root, whitespace-only/null/array root, duplicate keys at any depth or nonwhitespace trailing content; P/C POST/PUT/PATCH | `{"code":"MALFORMED_JSON","message":"The request body must be a valid JSON object."}` | FR-006, PRD Error Responses/400; B5 category 1. D1: whitespace-only/null/array root malformed; an absent body or {} uses REQUIRED_FIELD_MISSING. |
| `UNKNOWN_FIELD` | 400 | Unknown property in a request DTO, recursively when nested DTOs exist; P/C POST/PUT/PATCH | `{"code":"UNKNOWN_FIELD","message":"The request contains an unknown field."}` | FR-006; A/B4/B5. Current P/C requests are flat; Order DTOs remain deferred. |
| `UNSUPPORTED_FIELD` | 400 | Recognized forbidden property: id/version; active in POST/PUT; editable details in PATCH; PATCH active=true | `{"code":"UNSUPPORTED_FIELD","message":"The request contains a field or activation value that this operation does not permit."}` | BR-012, AC-012/016; A/B4/B5. PATCH false is allowed, true is not. |
| `INVALID_FIELD_TYPE` | 400 | Wrong token type: numeric/boolean name/email, string/boolean price, string/number active, wrong field object/array shape; P/C writes | `{"code":"INVALID_FIELD_TYPE","message":"A request field has an invalid JSON type."}` | A/B2; FR-006 malformed request. No implicit scalar coercion. |
| `REQUIRED_FIELD_MISSING` | 400 | Absent body/{} or missing/null name or price (P POST/PUT), name or email (C POST/PUT), active (P/C PATCH) | `{"code":"REQUIRED_FIELD_MISSING","message":"A required request field is missing or null."}` | BR-011, AC-011/012; B2/B4. Empty textual values instead reach business validation. |
| `INVALID_NAME` | 400 | Textual name fails accepted String.strip()/1–200 Unicode-code-point policy; P/C POST/PUT | `{"code":"INVALID_NAME","message":"The name must contain 1 to 200 Unicode code points after stripping surrounding whitespace."}` | BR-011, AC-011. Preserve valid Unicode/case; no UTF-16 length substitute. |
| `INVALID_EMAIL` | 400 | Textual email fails approved ASCII grammar or normalized 254-code-point limit; C POST/PUT | `{"code":"INVALID_EMAIL","message":"The email must satisfy the documented email policy and normalization limit."}` | BR-011, AC-011; String.strip()/Locale.ROOT, provider dots/plus tags preserved. |
| `INVALID_PRODUCT_PRICE` | 400 | Numeric price outside EUR 0.00–999,999.99, more than two supplied decimal places or exponent notation; P POST/PUT | `{"code":"INVALID_PRODUCT_PRICE","message":"The product price must be between EUR 0.00 and EUR 999,999.99, use ordinary decimal notation and have at most two decimal places."}` | BR-001/013, AC-001/011; B3 adds the accepted API lexical rule. Wrong token type instead uses INVALID_FIELD_TYPE. |
| `INVALID_RESOURCE_ID` | 400 | Noncanonical/malformed UUID path text; P/C individual GET/PUT/PATCH | `{"code":"INVALID_RESOURCE_ID","message":"The resource identifier must use canonical UUID text representation."}` | B1; 36 characters, standard hyphens, no lenient shortened forms. D5/D6: upper/lower/mixed hex accepted; validate path before body, lookup last. |
| `PRODUCT_NOT_FOUND` | 404 | Valid canonical UUID identifies no stored Product; P individual GET/PUT/PATCH | `{"code":"PRODUCT_NOT_FOUND","message":"The product was not found."}` | FR-001/006, AC-013; B1. Inactive is not missing. |
| `CUSTOMER_NOT_FOUND` | 404 | Valid canonical UUID identifies no stored Customer; C individual GET/PUT/PATCH | `{"code":"CUSTOMER_NOT_FOUND","message":"The customer was not found."}` | FR-002/006, AC-013; B1. Inactive is not missing; DELETE is unavailable, not a lookup operation. |
| `ENDPOINT_NOT_FOUND` | 404 | No matching endpoint; API routing boundary | `{"code":"ENDPOINT_NOT_FOUND","message":"The requested endpoint was not found."}` | B6. Distinct from absent Product/Customer; introduces no Order endpoint. |
| `METHOD_NOT_ALLOWED` | 405 | Unsupported method on implemented resources, including Product DELETE; excludes deferred Customer DELETE | `{"code":"METHOD_NOT_ALLOWED","message":"This HTTP method is not supported for this resource."}` | FR-006, AC-016; B7 specialization below. Accurate Allow header required. |
| `OPERATION_NOT_AVAILABLE` | 405 | DELETE /customers/{id} in the interim API version, without deletion/reference-check logic | `{"code":"OPERATION_NOT_AVAILABLE","message":"Customer deletion is not available in this API version."}` | B7. Full MVP DELETE/204 remains FR-002, AC-003/004/013; accurate Allow required. |
| `NOT_ACCEPTABLE` | 406 | Requested success representation cannot be supplied under Accept; API boundary | `{"code":"NOT_ACCEPTABLE","message":"The requested response representation is not supported."}` | B6. JSON error envelope still required; incompatible Accept handling must be verified. |
| `DUPLICATE_EMAIL` | 409 | Another active/inactive customer owns the normalized email; C POST/PUT, including confirmed uq_customers_email/23505 at flush/commit | `{"code":"DUPLICATE_EMAIL","message":"The email address already belongs to another customer."}` | BR-011, AC-011; B11. Unchanged self-owned normalized email succeeds; not every integrity failure is a duplicate. |
| `CONCURRENT_MODIFICATION` | 409 | Confirmed JPA optimistic-lock conflict for P/C PUT/PATCH, including completion failures | `{"code":"CONCURRENT_MODIFICATION","message":"The resource was modified concurrently. Please reload and retry."}` | B8/B9/B11; BR-012 activity protection. No automatic retry; no ETag feature implied. |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Unsupported request Content-Type on API writes | `{"code":"UNSUPPORTED_MEDIA_TYPE","message":"The request media type is not supported."}` | B6. D6/D7: headers precede paths/bodies; only application/json with UTF-8 accepted, optional charset=UTF-8; reject +json variants. |
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected technical/service/persistence failure, including uncertain completion when a response is possible | `{"code":"INTERNAL_SERVER_ERROR","message":"An unexpected server error occurred."}` | B6/B10. Does not assert rollback or authorize retries; failed transport can prevent a response. |
| `SERVICE_UNAVAILABLE` | 503 | Clearly classified temporary dependency outage; applicable API operations | `{"code":"SERVICE_UNAVAILABLE","message":"A required service is temporarily unavailable."}` | B10. D8: confirmed establishment/pre-write unavailability only; scoped typed cause/phase verification passed; not all SQL/timeout/connection failures. |

## Accepted Error Precedence

For request validation, select exactly one error by category:

1. Malformed JSON syntax/root structure.
2. Unknown or forbidden request properties.
3. Invalid JSON field types.
4. Missing or null required fields.
5. Invalid business values.
6. Missing resources or business conflicts.

Selection within a category must be deterministic. A default fail-fast mapper or unordered Bean Validation set does not prove this ordering: a type error early in the document must not hide an unknown field later, and later syntax corruption still wins. PATCH true is a forbidden activation value; a string/number active is a type error.

**Selected minimal processing approach (scoped runtime verification passed; implementation human accepted):** fully parse/check the root before validation, preserve numeric token spelling, inspect all request properties, then run type/presence/value checks in explicit stages. Human Accepted stable DTO field order: Product name → price, Customer name → email, PATCH active; unknown/forbidden property paths sorted lexicographically so JSON input order does not determine the error. D1–D8 in the [REST design](rest-api-design.md#11-final-human-decisions-d1d8) resolve the former edge questions. Outer order is routing/method/headers → path syntax → the body categories above → application lookup/business behavior. Tie-breakers and duplicate/trailing policies are Accepted; scoped decoder/interceptor runtime evidence is recorded in [final Increment 4 acceptance](../04-agentic-coding/prompts/05-product-customer-api.md#final-human-acceptance--2026-10-15).

## Transaction and Failure Classification

Accepted B11: proxied application write methods use @Transactional, reads @Transactional(readOnly=true); controllers own no transaction and domain uses no Spring transaction API. Materialize immutable application results inside the transaction, commit before HTTP success and translate errors outside the proxy boundary. Verify internal/self-invoked paths and interceptor completion; flush alone is not completion.

Only a confirmed known normalized-email uniqueness violation maps to DUPLICATE_EMAIL. Only a confirmed Product/Customer optimistic-lock failure maps to CONCURRENT_MODIFICATION. Preserve cause chains and allow rollback; never continue a rollback-only context, parse localized exception messages or classify all database errors as 409/503. Known completed rollback permits asserting no write persisted; uncertain completion does not. No automatic write replay. Detailed Order retries/timeouts/reconciliation remain with Proposed [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md).

## Verification Obligations

- Exact Boot 4.1.1/Jackson 3 pipeline/customization: precedence before typed DTO binding, nested rejection, lexeme preservation without float/double, canonical UUID enforcement before lenient UUID parsing.
- Confirm safe central JSON error rendering for incompatible Accept, unsupported Content-Type, unknown routes, framework error dispatch and failures raised outside controller advice. The Accepted 406/415/404 envelopes are not optional; do not silently fall back to HTML/ProblemDetail or recursive negotiation errors.
- Prove Human Accepted D1–D8 body/duplicate/trailing/tie-breaker/UUID-case/header-path-body/media/outage policies; they are no longer Open choices. No extra lexical/business restrictions.
- Verify precise optimistic-lock/commit cause translation and the selected narrow establishment/pre-write outage classifier. Do not expose diagnostics or infer rollback from 500/503.
- Test every applicable catalog code through actual MVC and real PostgreSQL where persistence matters; controlled faults must distinguish confirmed rollback from lost completion confirmation. Scoped implementation/runtime evidence and human acceptance are recorded in [final Increment 4 acceptance](../04-agentic-coding/prompts/05-product-customer-api.md#final-human-acceptance--2026-10-15). Real lost-commit-confirmation fault injection and full Order protocol verification remain deferred.

## Human-Accepted Increment 5 Order Extensions — 2026-10-16

The [final G1–G6 decision record](../04-agentic-coding/prompts/06-order-placement-concurrency.md#final-human-architecture-decisions--2026-10-16) accepts the following five additional codes and exact fixed messages. These are contract decisions, not implemented enum values or verified responses. Earlier Product/Customer-only applicability statements describe the Increment 4 implementation scope and do not override these accepted extensions. No other new code or response field is selected.

| Code | HTTP | Exact message | Trigger |
| --- | ---: | --- | --- |
| INVALID_ORDER_ITEMS | 400 | The order must contain at least one item. | Empty items array after type/presence checks. |
| INVALID_ORDER_QUANTITY | 400 | Order item quantities must be positive integers and the consolidated quantity must not exceed 10,000. | Nonpositive/out-of-range integer quantity or invalid/overflowing consolidated quantity. |
| CUSTOMER_INACTIVE | 409 | The customer is inactive. | Placement references an existing inactive Customer. |
| PRODUCT_INACTIVE | 409 | The product is inactive. | Placement references an existing inactive Product. |
| ORDER_NOT_FOUND | 404 | The order was not found. | Valid canonical order ID has no stored Order. |

Reuse existing exact messages/statuses for INVALID_RESOURCE_ID, CUSTOMER_NOT_FOUND, PRODUCT_NOT_FOUND, MALFORMED_JSON, UNKNOWN_FIELD, UNSUPPORTED_FIELD, INVALID_FIELD_TYPE, REQUIRED_FIELD_MISSING, METHOD_NOT_ALLOWED and technical INTERNAL_SERVER_ERROR/500 and SERVICE_UNAVAILABLE/503. Existing media/route envelope policies continue. Do not add an unknown-outcome code, diagnostic field or blind retry instruction. Return exactly code/message; INTERNAL_SERVER_ERROR does not assert rollback. Confirmed transient full-rollback exhaustion uses SERVICE_UNAVAILABLE; unknown commit uses INTERNAL_SERVER_ERROR if a response remains possible.

### Order Input Classification and Precedence

Apply complete syntax/root/duplicate/trailing validation first, then recursively unknown/forbidden properties, types, missing/null required fields, business values, then application resource findings. Preserve earlier routing/method/headers → path → body → application precedence. Scalar coercion is prohibited. Quantity must be a JSON integer token: 1.0, exponent tokens, strings and booleans use INVALID_FIELD_TYPE; invalid integer values/sums use INVALID_ORDER_QUANTITY. Parse without narrowing overflow. Missing/null customerId/items/productId/quantity (and null required item) uses REQUIRED_FIELD_MISSING; an item object/array shape mismatch uses INVALID_FIELD_TYPE. Empty items uses INVALID_ORDER_ITEMS, after earlier categories. Malformed canonical UUID text uses INVALID_RESOURCE_ID, including body references. Do not manufacture a new lexical restriction beyond the accepted integer-token rule.

| DTO level | Allowed properties | Recognized forbidden properties (UNSUPPORTED_FIELD) |
| --- | --- | --- |
| Order root | customerId, items | id, version, customer, currency, subtotal, discount, total, unitPrice |
| Order item | productId, quantity | id, version, productName, unitPrice, lineTotal, subtotal, discount, total, currency |

These server-owned fields derive from the PRD response/pricing contract; any other unrecognized property uses UNKNOWN_FIELD. Inspect all nested fields before selecting a later type/value/resource error. Do not ignore caller-supplied monetary data at either level.

Deterministic ordering: customerId before items; productId before quantity within each item; indices ascending; unknown/forbidden paths lexicographic. Application resource findings use Customer-first, then distinct Products sorted by canonical lowercase UUID text, across missing/inactive cases; do not prioritize all missing Products ahead of an inactive Customer. Validate request syntax and values before any lookup; retrieve all authoritative facts under the approved consistent locking protocol.

### Customer DELETE Transition and Verification

Increment 5 includes DELETE /customers/{id}: existing unreferenced → physical deletion/204/no body; referenced → deactivation/204/no body, including already inactive without reactivation; absent → CUSTOMER_NOT_FOUND/404. The Increment 4 interim OPERATION_NOT_AVAILABLE/405 is replaced only by subsequently authorized implementation, not by this documentation. Product DELETE and Order PUT/PATCH/DELETE remain METHOD_NOT_ALLOWED/405 with accurate Allow. No unrelated endpoint behavior is changed.

Extend literal error-contract tests to these five templates and all reused Order triggers, actual MVC malformed/forbidden/type/presence/value precedence and real PostgreSQL commit/race/rollback paths. All new runtime evidence remains Pending. No accepted Increment 4 historical response or test result is rewritten.

## Approved Customer DELETE Target — 2026-10-18

**Approved target contract:** DELETE /customers/{id} returns 409 when orders reference an active or inactive customer, without changing any customer field or order. The following is the exact shared `ApiError` response; the envelope remains only `code` and `message`:

```json
{
  "code": "CUSTOMER_HAS_ORDERS",
  "message": "Customer cannot be deleted because existing orders reference this customer. Deactivate the customer instead."
}
```

The message is fixed and safe; do not include database diagnostics. An unreferenced active or inactive customer is physically deleted with 204/no body. An absent customer retains CUSTOMER_NOT_FOUND/404 and the existing message `The customer was not found.` PATCH {"active":false} remains separate; DELETE never implicitly deactivates.

**Current implementation:** CUSTOMER_HAS_ORDERS is a specific application failure mapped to the exact 409 envelope above. Only a positive Order-owned reference query after locking the Customer produces this business failure; arbitrary integrity failures retain technical handling. Existing validation precedence, concurrent-modification and missing-resource responses remain unchanged. Phase 2 verification and human acceptance are tracked in the [Phase 2 review record](../04-agentic-coding/change-requests/CH-001-customer-delete-semantics/03-review.md).