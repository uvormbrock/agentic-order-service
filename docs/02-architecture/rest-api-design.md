# REST API Design — Agentic Order Service

**Status:** Draft — implementation pending  
**Scope:** Request validation, strict JSON handling, and API error responses  
**Related:** [Product Requirements](../01-requirements/product-requirements.md) · [Domain Model](domain-model.md)

## 1. Purpose

Capture the agreed REST request-validation requirements before implementing the Spring Boot API. This is an initial design note, **not** a complete API specification.

The Product Requirements Document (PRD) is authoritative for endpoint behavior and HTTP status codes. In particular, unsupported request fields and caller-supplied pricing fields must be **rejected with HTTP 400**, not silently ignored.

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
- Configure and verify Jackson's unknown-property handling explicitly; **do not rely on framework defaults**.
- The exact Spring Boot/Jackson configuration is **TBD** and must match the versions actually selected. Do not copy a Jackson 2 configuration into a Jackson 3 application without verifying compatibility.

## 4. Request Validation

Use Bean Validation and appropriate request parsing to enforce PRD rules, including:

- Required `customerId`, `productId`, and order items.
- At least one order item.
- Positive, integral quantities; maximum **10,000 per product after duplicate consolidation**.
- Required trimmed names of 1–200 characters.
- Email trimming, lowercase normalization, basic syntactic validation, maximum 254 characters, and case-insensitive uniqueness.
- Product prices between EUR 0.00 and EUR 999,999.99 inclusive, with no more than two decimal places.
- No activation through unsupported request fields.

Validation must cover both **request structure** and **business rules**; field-level annotations alone are not sufficient for cross-item rules such as consolidated quantity limits.

## 5. Error Handling

Provide consistent JSON errors with at least:

```json
{
  "code": "INVALID_REQUEST",
  "message": "The request contains an unsupported field."
}
```

The example error code is provisional; the PRD only requires `code` and `message`. Use the PRD's established semantics:

| HTTP status | Examples |
| --- | --- |
| `400` | Malformed JSON, unknown fields, caller-provided prices or totals, invalid quantities, missing required fields, invalid names/email/price |
| `404` | Referenced customer or product does not exist; requested resource not found |
| `409` | Inactive customer or product referenced by a new order; duplicate normalized customer email |
| `405` | Unsupported resource methods, as defined by the PRD |

Technical transaction failures and uncertain commit outcomes are **not** automatically mapped to business-validation conflicts; the accepted business policy is defined in [ADR-0001](../03-decisions/0001-order-placement-concurrency.md). [ADR-0002](../03-decisions/0002-transaction-and-locking-strategy.md) proposes technical locking, retries, and error handling; it remains Proposed and implementation-unverified, including handling where no HTTP response is possible.

## 6. Integration-Test Checklist

Tests should send actual HTTP requests through Spring MVC's JSON binding and validation layer (not merely invoke controller methods).

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
| Product/customer name has 1 or 200 characters after trimming | Accepted on create/update with otherwise valid input; stored name is trimmed. |
| Product/customer name is missing, empty after trimming, or has 201 characters | `400 Bad Request` |
| Email ` Alice@Example.com ` | Stored as `alice@example.com` on create/update. |
| Otherwise valid email has 254 characters after normalization | Accepted on create/update. |
| Email is missing, syntactically invalid, or has 255 characters after normalization | `400 Bad Request` |
| Another active or inactive customer owns `alice@example.com`; submit `ALICE@example.com` | `409 Conflict` on create/update. |
| Email differs only by provider-specific dots or plus addressing | Do not collapse distinct addresses using provider-specific rules. |
| Product price is EUR 0.00 or EUR 999,999.99 | Accepted on create/update. |
| Product price is EUR -0.01, EUR 1,000,000.00, or EUR 1.001 | `400 Bad Request` |
| Quantity is 1 or 10,000; duplicate quantities 6,000 and 4,000 | Accepted for eligible references; duplicates consolidate to one item with quantity 10,000. |
| Quantity is 10,001 or duplicate quantities are 6,000 and 4,001 | `400 Bad Request`; no order persisted. |

Also verify that every error response contains `code` and `message`.

## 7. Open Technical Decisions

- Final Spring Boot and Jackson versions and the concrete strict-deserialization configuration.
- DTO field types, Bean Validation annotations, and exception-to-HTTP mapping.
- Exact error `code` taxonomy and safe error-message details.
- Concrete integration-test setup; ADR-0002 proposes PostgreSQL/Testcontainers for deterministic concurrency verification.
- Technical implementation and verification of the Proposed ADR-0002 strategy under accepted ADR-0001, including timeouts, retry delays, and uncertain commit reconciliation.

## 8. Implementation Reminder

**Before declaring the REST API complete:** prove with HTTP-level integration tests that unknown fields—including nested fields and attempted price overrides—return **400**. A server that calculates prices correctly but silently accepts those invalid requests **does not meet the PRD**.
