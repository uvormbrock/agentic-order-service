# Product Requirements: Agentic Order Service

**Customer DELETE contract update — 2026-10-18:** The DELETE requirements below are the **Approved target contract**, not implemented behavior. The **Current implementation** still returns 204 and deactivates referenced customers (or leaves them inactive). **Pending Phase 2 implementation and verification:** apply the [handoff checklist](../02-architecture/rest-api-design.md#phase-2-implementation-acceptance-checklist) under separate authorization. Earlier increment execution and acceptance records describe the previous contract and remain historical evidence.

## Purpose

The Agentic Order Service is a small order management application. It supports managing products and customers and creating, storing, and retrieving orders. Prices and discounts are calculated exclusively on the server.

## Functional Requirements

| ID     | Requirement                                                                                                                                                                                                                 |
| ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| FR-001 | Create, retrieve individually, update, list, and deactivate products. Each product has an ID, name, gross price in EUR, and active status.                                                                                  |
| FR-002 | Create, retrieve individually, update, list, and delete customers. Each customer has an ID, name, email address, and active status. Physically delete customers only when no orders reference them; otherwise reject DELETE with 409. Deactivation is a separate PATCH operation. |
| FR-003 | Create and store an order for an existing active customer with one or more line items referencing eligible products and specifying quantities. Consolidate duplicate product references.                                    |
| FR-004 | Capture original product names and calculate and preserve product prices, line item amounts, subtotal, discount, and payable total on the server.                                                                           |
| FR-005 | Retrieve stored orders individually and list them. An individually retrieved order includes its customer reference, line items, original product names and prices, and calculated amounts.                                  |
| FR-006 | Expose the MVP operations through the REST contract below. Lists include active and inactive records.                                                                                                                       |

## Business Rules

| ID | Rule |
| --- | --- |
| BR-001 | Product prices must be between EUR 0.00 and EUR 999,999.99 inclusive. All prices are gross amounts in EUR. |
| BR-002 | Deactivated products cannot be included in new orders. Products referenced by existing orders must not be physically deleted. |
| BR-003 | DELETE physically removes an active or inactive customer only if no orders reference that customer. Otherwise reject with 409 CUSTOMER_HAS_ORDERS and preserve all customer and order state. DELETE never deactivates; PATCH explicitly deactivates. Deactivated customers cannot place new orders. |
| BR-004 | At order placement, the customer must exist and be active, and the order must contain at least one line item. Subsequent customer deactivation does not invalidate historical orders. |
| BR-005 | Quantities must be positive integers. Duplicate references to the same product within an order are consolidated into one line item by summing their quantities. The consolidated quantity must not exceed 10,000 per product per order; reject quantities outside these limits. |
| BR-006 | Server-side product prices exclusively determine order amounts. Caller-supplied prices and calculated amounts must not override them. |
| BR-007 | Each line item amount equals its product price multiplied by its quantity. The subtotal equals the sum of line item amounts. |
| BR-008 | A subtotal of at least EUR 100.00 qualifies for a 10% discount on the subtotal. Below that threshold, the discount is zero. |
| BR-009 | Monetary calculations use two decimal places. Discounts are rounded to two decimal places using HALF_UP. The payable total equals the subtotal minus the rounded discount. |
| BR-010 | Stored orders preserve their original product names, product prices, and calculated amounts. Customer details reflect the current customer record when an order is retrieved. Deactivation does not prevent order retrieval. |
| BR-011 | Product and customer names are required, normalized with `String.strip()`, and must contain 1–200 Unicode code points after normalization. Preserve case, umlauts, accents, and valid Unicode characters; do not lowercase names. Customer emails are required, normalized deterministically with `String.strip()` and `Locale.ROOT` lowercase before storage, and limited to 254 Unicode code points after normalization. Emails must follow the approved ASCII policy below and be unique case-insensitively across active and inactive customers. Do not apply provider-specific normalization, including Gmail dot or plus-tag removal. Reject values outside these limits. |
| BR-012 | New products and customers are active by default. Deactivation is irreversible within the MVP; updating details does not reactivate a record. |
| BR-013 | Product prices supplied with more than two decimal places are rejected rather than rounded. No separate tax rates or tax breakdowns are recorded or returned in the MVP. |

### Approved Email Validation Policy

For BR-011, remove leading/trailing whitespace with `String.strip()` and lowercase using `Locale.ROOT`. Normalization is deterministic and idempotent. Reject null, empty, or invalid email input. The normalized address has at most **254 Unicode code points** and exactly one separating `@`.

- The local part contains ASCII letters, digits, dots, and the unquoted punctuation characters ``!#$%&'*+-/=?^_`{|}~``. Dots cannot be leading, trailing, or consecutive. Plus-addressing is supported; dots and plus-tags remain literal data.
- The domain contains at least two nonempty ASCII labels separated by dots. Labels contain ASCII letters/digits and optional internal hyphens; leading/trailing hyphens are rejected.
- Reject quoted local parts and internationalized email addresses. Non-ASCII email content is not accepted through lowercase conversion; surrounding whitespace follows `String.strip()`. Do not perform DNS lookups or apply provider-specific transformations.
- Do not add independent local-part/domain-label length limits or other restrictions beyond this approved policy. Case-insensitive uniqueness includes inactive customers.

This approval resolves email grammar, email counting, and email normalization. Names use String.strip() and Unicode code-point counting under the accepted name policy below.

### Approved Name Validation Policy

For BR-011, remove leading/trailing whitespace with `String.strip()` and require 1–200 Unicode code points. Preserve case, umlauts, accents, and valid Unicode characters. Do not lowercase names or impose an ASCII-only rule. Apply this policy on customer/product creation and update.

## REST Contract

Requests and responses use JSON. IDs identify records and are read-only in request bodies. Creation assigns an ID. Monetary values are expressed in EUR; order responses identify the currency as `EUR`.

### Endpoints

| Method | Path | Request / behavior | Success response |
| --- | --- | --- | --- |
| POST | `/products` | Required `name`, `price`; initially active. | 201, product; `Location: /products/{id}` |
| GET | `/products` | List all products, including inactive products. | 200, array of products |
| GET | `/products/{id}` | Retrieve a product, including an inactive product. | 200, product |
| PUT | `/products/{id}` | Required `name`, `price`; replace editable details and retain active status. | 200, updated product |
| PATCH | `/products/{id}` | Body `{"active": false}`; deactivate without physical deletion. | 200, inactive product |
| POST | `/customers` | Required `name`, `email`; initially active. | 201, customer; `Location: /customers/{id}` |
| GET | `/customers` | List all customers, including inactive customers. | 200, array of customers |
| GET | `/customers/{id}` | Retrieve a customer, including an inactive customer. | 200, customer |
| PUT | `/customers/{id}` | Required `name`, `email`; replace editable details and retain active status. | 200, updated customer |
| PATCH | `/customers/{id}` | Body `{"active": false}`; deactivate while retaining the record. | 200, inactive customer |
| DELETE | `/customers/{id}` | Physically delete an active or inactive customer if unreferenced; otherwise reject with 409 CUSTOMER_HAS_ORDERS without changes. | 204, no body |
| POST | `/orders` | Required `customerId`, nonempty `items` array of `productId` and `quantity`; calculate and store the complete order. | 201, order; `Location: /orders/{id}` |
| GET | `/orders` | List stored orders. | 200, array of orders |
| GET | `/orders/{id}` | Retrieve a stored order. | 200, order |

Repeated deactivation of an existing inactive record succeeds without changing its state. Empty lists return `[]`. Invalid order creation stores no order. Product deletion and order updates or deletion are not exposed.

### Customer DELETE — Approved Target Contract (2026-10-18)

| Customer state | Referencing orders | DELETE result | HTTP |
| --- | --- | --- | --- |
| Active | None | Physically remove customer | 204, no body |
| Inactive | None | Physically remove customer | 204, no body |
| Active | One or more | Reject; customer and orders completely unchanged | 409, CUSTOMER_HAS_ORDERS |
| Inactive | One or more | Reject; customer and orders completely unchanged | 409, CUSTOMER_HAS_ORDERS |
| Not found | N/A | Existing CUSTOMER_NOT_FOUND error | 404 |

Only referencing orders determine deletability; active status does not. A 204 means physical removal, never implicit deactivation. The existing PATCH body `{"active":false}` remains the explicit, irreversible deactivation operation; no reactivation is introduced. The [error catalog](../02-architecture/api-error-codes.md#approved-customer-delete-target--2026-10-18) owns the exact 409 message and shared error envelope.

**Concurrency acceptance requirement:** racing DELETE with first-order creation must never leave an order referencing a deleted customer. If physical deletion wins, placement must observe the absent customer and return the existing 404. If placement commits first, DELETE must reject with 409 and leave the customer (including active status) and all orders unchanged. Rejected deletion must not partially modify either feature. This specifies externally observable outcomes, not a newly selected enforcement technique; Phase 2 must review and verify the implementation against the existing transaction boundaries.

### Response Fields

| Resource | Fields |
| --- | --- |
| Product | `id`, `name`, `price`, `active` |
| Customer | `id`, `name`, `email`, `active` |
| Order | `id`, `customerId`, `customer` (current customer details), `items`, `currency`, `subtotal`, `discount`, `total` |
| Order line item | `productId`, `productName` (original name at placement), `quantity`, `unitPrice` (original price), `lineTotal` (original amount) |

Individual and list responses use the same resource fields. Order creation accepts product references and quantities, not prices or calculated amounts. Read-only fields and unsupported request fields are rejected; clients cannot assign IDs, reactivate records, or override calculated prices.

### Error Responses

Errors return a JSON body containing `code` and `message`, describing the failed validation or operation.

| Status | Condition |
| --- | --- |
| 400 | Malformed request, missing required fields, names outside 1–200 Unicode code points after String.strip(), invalid email syntax under BR-011 or email longer than 254 Unicode code points after normalization, product price outside EUR 0.00–999,999.99 or with more than two decimal places, nonpositive or fractional quantities or consolidated quantity above 10,000 per product per order, empty order, or unsupported request fields or activation requests. |
| 404 | Requested resource or customer/product referenced by a new order does not exist. |
| 409 | Customer email already belongs to another stored customer, an order references an inactive customer or product, or Customer DELETE encounters referencing orders (CUSTOMER_HAS_ORDERS). |
| 405 | Unsupported resource operation, including product deletion and order updates or deletion. |

## Acceptance Criteria

| ID | Related requirements / rules | Testable criteria |
| --- | --- | --- |
| AC-001 | FR-001, BR-001 | A product can be created with its required fields, retrieved by ID, listed, and updated. Prices of EUR 0.00 and EUR 999,999.99 are accepted; EUR -0.01 and EUR 1,000,000.00 return 400. |
| AC-002 | FR-001, BR-002 | Deactivating a product changes its active status. A new order referencing it is rejected. A product referenced by an existing order remains stored. |
| AC-003 | FR-002 | A customer created with a name and email address receives an ID and can be retrieved by ID, listed, and updated. An active or inactive customer with no referencing orders can be physically deleted with 204/no body and is no longer retrievable. |
| AC-004 | FR-002, BR-003 | Deleting an active or inactive customer referenced by an existing order returns 409 CUSTOMER_HAS_ORDERS; every customer field, including active status, and all existing orders remain unchanged. PATCH {"active":false} is the separate deactivation operation; a deactivated customer cannot place a new order. |
| AC-005 | FR-003, BR-002–BR-005 | An order for an existing active customer with active products and positive integer quantities within the consolidated limit is accepted. An unknown or deactivated customer, an unknown or deactivated product, an empty order, or a zero, negative, or fractional quantity causes rejection. |
| AC-006 | FR-003, BR-005 | Two line items referencing the same product with quantities 2 and 3 result in one stored line item with quantity 5. Quantities 6,000 and 4,000 consolidate to 10,000 and are accepted; 6,000 and 4,001 return 400 and create no order. A single quantity of 10,001 also returns 400. |
| AC-007 | FR-004, BR-006–BR-007 | For an accepted request containing a product reference and quantity 2, a server-side product price of EUR 25.00 produces a line item amount of EUR 50.00. Requests supplying prices or totals are rejected as specified in AC-014. |
| AC-008 | FR-004, BR-008–BR-009 | The calculation examples below produce the stated discount and payable total, including the threshold and HALF_UP rounding cases. |
| AC-009 | FR-004–FR-005, BR-010 | After an order is stored, changing a product price does not change its original product prices or calculated amounts when retrieved. Product or customer changes and deactivation do not prevent retrieval of the stored order. |
| AC-010 | FR-005 | A stored order can be retrieved individually with its line items and calculated amounts and appears in the order list. |
| AC-011 | FR-001–FR-002, BR-011–BR-013 | On product/customer creation and update, names normalized with String.strip() to 1 or 200 Unicode code points are accepted; missing, empty after stripping, or 201-code-point names return 400. Case, umlauts, accents and valid Unicode characters are preserved; a supplementary character counts as one code point. Missing or syntactically invalid emails, normalized emails longer than 254 Unicode code points, and product prices of EUR 1.001 or EUR 10.000 return 400; otherwise valid normalized emails of 254 Unicode code points are accepted. ` Alice@Example.com ` is stored as `alice@example.com`; submitting `ALICE@example.com` for another customer returns 409, including when the existing customer is inactive. Dot and plus-address variants are not collapsed by provider-specific rules. The email-policy examples below are part of this criterion. |
| AC-012 | FR-006, BR-012 | Creation returns 201 with an assigned ID and Location header; new products and customers are active. Deactivation returns 200 with `active: false`; repeating it succeeds. A request to reactivate returns 400. Updating details retains inactive status. |
| AC-013 | FR-006 | Lists return 200 and include inactive records; an empty list returns `[]`. An unknown resource ID or order reference returns 404. Successful physical customer deletion returns 204 with no body; referenced customers return 409 CUSTOMER_HAS_ORDERS and remain retrievable with their original active status. Missing customers return CUSTOMER_NOT_FOUND/404. |
| AC-014 | FR-003, FR-006 | An empty order or invalid quantity returns 400; an inactive customer or product returns 409. Rejected requests create no order. Supplying `unitPrice`, `subtotal`, `discount`, or `total` in an order request returns 400. |
| AC-015 | BR-010, BR-013 | Renaming a product leaves the original product name unchanged in retrieved orders. Updated customer details are reflected; original prices and amounts remain unchanged. Responses contain no separate tax breakdown. |
| AC-016 | FR-006 | Product DELETE and order PUT, PATCH, or DELETE return 405. Each documented endpoint returns the stated success status and response fields; errors contain `code` and `message`. |
| AC-017 | FR-003–FR-004 | The order and all items are saved atomically. Validation rejection or confirmed transaction rollback leaves no order or items from that attempt. An uncertain commit outcome is not proof of rollback. |
| AC-018 | FR-003–FR-004, BR-010 | Concurrent product changes and customer deactivation/deletion follow the accepted ordering policy in ADR-0001. Product names, prices, and eligibility are captured consistently; no dangling references or partial orders result. |

Atomicity and concurrency behavior are specified in [ADR-0001](../03-decisions/ADR-0001-order-placement-concurrency.md); the detailed enforcement strategy remains Proposed in [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md). See [transaction conventions](../02-architecture/transaction-locking.md) for adopted application boundaries.

### Email Policy Examples (AC-011)

| Input / scenario | Expected behavior |
| --- | --- |
| Spaces or tabs around `Alice+Orders@Example.COM` | Store `alice+orders@example.com`; repeating normalization leaves it unchanged. |
| `a.b@example.com`, `ab@example.com`, `a+tag@example.com` | Valid, distinct normalized addresses; no dot or plus-tag removal. |
| `o'hara+tag@example-domain.com` | Valid local punctuation and internal domain hyphen. |
| `.alice@example.com`, `alice.@example.com`, `a..b@example.com` | Reject with 400. |
| `alice@@example.com`, `alice@example`, `alice@ex..com` | Reject with 400: invalid separator or domain labels. |
| `alice@-example.com`, `alice@example-.com` | Reject with 400. |
| `"alice"@example.com`, `álîce@example.com`, `alice@exämple.com` | Reject with 400: quoted/internationalized email content. |
| Local part of 242 ASCII `a` characters followed by `@example.com` | 254 normalized code points; accept with otherwise valid input. A 243-character local part yields 255 and is rejected with 400. |
| Normalized address already owned by an active or inactive customer | Reject another customer's create/update with 409, regardless of input case. |

### Calculation Examples

| Subtotal | Discount | Payable total |
| --- | --- | --- |
| EUR 90.00 | EUR 0.00 | EUR 90.00 |
| EUR 99.99 | EUR 0.00 | EUR 99.99 |
| EUR 100.00 | EUR 10.00 | EUR 90.00 |
| EUR 100.05 | EUR 10.01 | EUR 90.04 |
| EUR 150.00 | EUR 15.00 | EUR 135.00 |

## MVP Scope

The MVP includes:

- Creating, retrieving, updating, listing, and deactivating products.
- Creating, retrieving, updating, listing, explicitly deactivating customers via PATCH, and physically deleting only unreferenced customers via DELETE.
- Creating and storing orders with validated customers, products, and quantities, including duplicate consolidation.
- Server-side calculation of gross EUR amounts and discounts, preserving original product names, order prices, and amounts.
- Retrieving individual orders and listing orders.
- The REST endpoints, validation, response fields, and error behavior specified above.

Updating and deleting orders, physical product deletion, reactivation, and separate tax breakdowns are outside the MVP scope. This document specifies externally observable behavior, not internal architecture or implementation.

## Remaining Business Clarifications

Name/email validation is resolved by BR-011 and the policies above. Any additional overall order-size/total restriction remains undecided and requires approval; no new limit is introduced.
