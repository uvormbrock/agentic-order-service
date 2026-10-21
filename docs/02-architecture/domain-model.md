# Domain Model — Agentic Order Service

**Status:** Accepted aggregate and Increment 3 persistence design; scoped implementation Reviewed – Accepted
**Scope:** Current conceptual domain model, including CH-002 Product Image metadata; Increment 3/4 provenance retained
**Related:** [Product Requirements](../01-requirements/product-requirements.md) · [Order Item Snapshots](order-item-snapshots.md)

## 1. Overview

The Agentic Order Service models product catalog management, customer management, and order placement. The accepted Increment 3 aggregate baseline contains three aggregate roots (`Customer`, `Product`, and `Order`), one child entity (`OrderItem`), and two value objects (`Money` and `EmailAddress`). CH-002 extends the current model with the directly JPA-mapped `ProductImage` metadata entity in the Product feature. It references Product by scalar UUID and is not a separate aggregate root; Product has no image property or collection. Internal Product/Customer version metadata from Increment 4 remains outside this conceptual diagram, as documented below.

The model deliberately separates the **current catalog state** from the **historical facts captured when an order is placed**. It is a conceptual domain model. Direct JPA mapping is accepted in [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md); D1–D10 mapping/storage selections are Accepted there; concrete SQL was reviewed against the PRD and scoped runtime verification passed as recorded in the [Increment 3 execution record](../04-agentic-coding/prompts/04-domain-model.md#implementation-results--executed-pending-human-review). Increment 3 is Reviewed – Accepted.

## 2. Domain Model Diagram

```mermaid
classDiagram
    direction TB

    namespace CustomerAggregate {
        class Customer {
            <<Aggregate Root>>
            +UUID id
            +String name
            +EmailAddress email
            +boolean active
        }
    }

    namespace ProductAggregate {
        class Product {
            <<Aggregate Root>>
            +UUID id
            +String name
            +Money price
            +boolean active
        }
    }

    namespace ProductFeatureMetadata {
        class ProductImage {
            <<Entity>>
            +UUID productId
            +String originalKey
            +String thumbnailKey
            +String mediaType
            +long originalSize
            +long thumbnailSize
            +int width
            +int height
            +int thumbnailWidth
            +int thumbnailHeight
        }
    }

    namespace OrderAggregate {
        class Order {
            <<Aggregate Root>>
            +UUID id
            +UUID customerId
            +List~OrderItem~ items
            +Money subtotal
            +Money discount
            +Money total
        }

        class OrderItem {
            <<Entity>>
            +UUID id
            +UUID productId
            +String productName
            +int quantity
            +Money unitPrice
            +Money lineTotal
        }
    }

    namespace ValueObjects {
        class Money {
            <<Value Object>>
            +BigDecimal amount
            +Currency currency
        }

        class EmailAddress {
            <<Value Object>>
            +String value
        }
    }

    Customer "1" <-- "0..*" Order : customerId
    Order "1" *-- "1..*" OrderItem : contains
    Product "1" <-- "0..*" OrderItem : productId
    Product "1" <-- "0..1" ProductImage : productId

    Customer --> EmailAddress : uses
    Product --> Money : uses
    Order --> Money : uses
    OrderItem --> Money : uses
```

**Reading the diagram:** The filled diamond indicates that `OrderItem` is part of the `Order` aggregate. The arrows labeled `customerId` and `productId` indicate **references by identity**, not mandatory in-memory object relationships or JPA mappings. `ProductImage` belongs to the Product feature, outside the diagram's aggregate namespaces: each metadata entity references exactly one Product, while a Product may have zero or one metadata entity. Its scalar `productId` is also its persistence identity. This is not a Product image field, JPA `@OneToOne`, aggregate composition or a new aggregate root. The diagram lists ProductImage's persisted Java attributes; their creation, validation and SQL constraints are detailed in [Product Image Metadata](#product-image-metadata--ch-002). The `OrderItem` multiplicity represents the intended invariant for a created order; a transient construction process may have different intermediate states.

## 3. Aggregate Boundaries

### 3.1 Customer aggregate

**Aggregate root:** `Customer`

- Represents a customer with a stable identity, name, email address, and active status. Customer names use String.strip() and 1–200 Unicode code points, preserving case and valid Unicode (PRD BR-011).
- A new order may only be placed for an existing, active customer.
- An inactive customer can remain referenced by historical orders.
- **Approved target — 2026-10-18 (PRD BR-003):** physically delete unreferenced active or inactive customers; reject referenced DELETE with 409 CUSTOMER_HAS_ORDERS and no state changes. PATCH remains explicit irreversible deactivation. Phase 2 implements this behavior; see the [review record](../04-agentic-coding/change-requests/CH-001-customer-delete-semantics/03-review.md) for actual verification and pending human acceptance.

### 3.2 Product aggregate

**Aggregate root:** `Product`

- Represents the **current** catalog product, including name, price, and active status. Product names use String.strip() and 1–200 Unicode code points, preserving case and valid Unicode; gross EUR prices range from EUR 0.00 to EUR 999,999.99 with at most two decimal places (PRD BR-001, BR-011, BR-013).
- Only active products are eligible for new orders.
- Later name or price changes must not change original order product names, prices, or amounts (PRD BR-010).
- Product deactivation preserves the catalog identity for existing references.

### 3.3 Order aggregate

**Aggregate root:** `Order`
**Child entity:** `OrderItem`

- At placement, every order references an existing active customer by `customerId`. Later customer deactivation does not invalidate that order.
- A placed order contains at least one item.
- Each item has a positive integer quantity; after duplicate consolidation, the quantity must not exceed 10,000 per product per order (PRD BR-005).
- Duplicate references to the same product within one order are consolidated by summing their quantities (PRD BR-005).
- The order governs its own totals and discount invariants.
- Changes to order items must be controlled through the `Order` aggregate root, not through an independent `OrderItem` repository API.
- The MVP supports order creation and retrieval, not updates or deletion of placed orders.

**Cross-aggregate checks:** Ensuring that a customer and products exist and are active requires reading other aggregates before placing an order. The application layer can coordinate these checks and the order-creation transaction; the `Order` aggregate protects its own internal invariants.

## 4. Value Objects

### Money

**Accepted Java semantics for Increment 3:** Immutable `BigDecimal` amount/currency value, EUR-only in the MVP. Reject null/invalid inputs and source amounts with more than two decimal places, including trailing zeros such as `10.000`. Normalize accepted values to scale two without rounding; `1`, `1.0`, and `1.00` have equal monetary values and matching hashes. Use exact decimal arithmetic, never `float`/`double`, and reject incompatible-currency operations.

Do not impose a universal maximum amount or prohibit negative intermediate arithmetic without a specific accepted invariant. Product enforces the PRD price range. Order owns the 10% discount beginning at EUR 100.00 and its HALF_UP rounding; rounded discount results are constructed as Money. See PRD BR-001/BR-008/BR-009/BR-013.

Java semantics and SQL enforcement are distinct: the subsequent D1–D10 approval selects an auto-applied BigDecimal converter and unconstrained PostgreSQL NUMERIC with finite, scale ≤2 and invariant-specific non-negative checks. No fixed order-total maximum is selected. [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md) owns those Accepted mapping decisions; scoped runtime evidence is recorded in the [Increment 3 execution record](../04-agentic-coding/prompts/04-domain-model.md#implementation-results--executed-pending-human-review).

### EmailAddress

**Accepted validation semantics:** Immutable normalized value using `String.strip()` and `Locale.ROOT` lowercase, at most 254 Unicode code points after normalization, deterministic/idempotent normalization, and equality/hashing on the normalized value. The [PRD email policy](../01-requirements/product-requirements.md#approved-email-validation-policy) owns the exact ASCII grammar and examples: one `@`, unquoted local-part punctuation/dots/plus-addressing, at least two nonempty domain labels with internal hyphens, and rejection of quoted/internationalized forms. No DNS lookup or provider-specific transformation is permitted.

Case-insensitive uniqueness spans active and inactive customers. It is coordinated/enforced outside EmailAddress; this value object performs no database lookup. D3/D7 now select an auto-applied String converter, normalized email storage and an all-customer unique lower(email) index, without citext. Verify Java/PostgreSQL case/collation consistency. Customer/product names separately use String.strip() and 1–200 code points, preserving case and Unicode.

## 5. Historical Order Item Snapshots

At order placement, the system captures product data and calculated values in each `OrderItem` under ADR-0001:

| Field | Meaning | Historical behavior |
| --- | --- | --- |
| `productId` | Identity of the catalog product | Retained as a reference |
| `productName` | Product name accepted at placement | Remains unchanged |
| `lineTotal` | Calculated line item amount at placement | Remains unchanged |
| `unitPrice` | Unit price at placement | Remains unchanged |
| `quantity` | Ordered quantity | Remains unchanged for placed orders |

**Example:** A product named *MacBook Pro M4* costs EUR 1,999.00 when ordered. The resulting item stores the original name, price, and calculated line amount. If the catalog entry is renamed to *MacBook Pro M4 – Sale* and repriced to EUR 1,699.00, the order still displays *MacBook Pro M4* at the original EUR 1,999.00 unit price. Customer details likewise come from the current customer record.

The snapshot is **ordinary persisted data**, not an automatic versioning feature or a copy of the entire `Product` entity. See [Order Item Snapshots](order-item-snapshots.md) for additional rationale.

## 6. Pricing Invariants

For the approved MVP requirements:

1. `lineTotal = unitPrice × quantity`.
2. `subtotal = sum(lineTotal)` for all order items.
3. If `subtotal >= EUR 100.00`, `discount = HALF_UP(10% × subtotal, 2)`; otherwise the discount is zero.
4. `total = subtotal − discount`.
5. The discounted total and historical pricing must remain reproducible when an order is retrieved.

For example, EUR 100.05 yields a rounded discount of EUR 10.01 and a payable total of EUR 90.04; EUR 99.99 receives no discount.

The domain model shows `subtotal`, `discount`, and `total` as observable domain values. Their original results must be preserved. Reads must not apply current pricing rules to historical orders. D6/D8 select explicit original monetary columns using unconstrained NUMERIC; persisted reads preserve original results even after rules change. Compatibility and exact round trips remain unverified.

## 7. Aggregate vs. Persistence Model

- Aggregate boundaries express where business invariants must be protected.
- They do **not** automatically prescribe Java packages, JPA relationships, or SQL foreign keys.
- `Order` can reference `Customer` by `customerId`; `OrderItem` can reference `Product` by `productId` while retaining original product names, prices, and monetary results. Only customer details are resolved from current records for responses.
- Customer, Product, Order and OrderItem use JPA-generated UUIDs, PostgreSQL uuid, and null IDs before persistence. Entities retain reference equality; compare persisted business identity by UUID explicitly.
- Order.items is lazy one-to-many mapped by OrderItem.order, cascade PERSIST only, orphan removal false. The lazy, mandatory child association owns the non-null order_id FK. Maintain both sides through Order; cross-aggregate references stay scalar UUIDs with restrictive FKs and no deletion cascades.
- Direct field-access mappings, protected hydration constructors and persistence-package autoApply converters are Accepted. Value objects remain free of JPA annotations; domain has no persistence-package dependency. The original four-table Increment 3 baseline and required constraints/indexes are selected in [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md). CH-002 adds the fifth table, `product_images`, through V3; this extension does not change the historical Increment 3 acceptance or ADR status.
- Single-order reads use an explicit item fetch plan and materialize within transactions with OSIV disabled. List/pagination fetch details remain deferred. Application transaction ownership follows [ADR-0003](../03-decisions/ADR-0003-application-architecture.md); ADR-0002's detailed locking protocol remains Proposed.

## 8. Responsibilities and Open Design Questions

| Responsibility | Owner |
| --- | --- |
| Request shape and required fields; reject caller prices; HTTP error mapping | REST boundary, following the PRD contract |
| Customer/product eligibility and consistent order placement | Application operation coordinating aggregates and persistence |
| Consolidation, quantities, pricing, and internal order invariants | Order domain behavior |
| Atomic storage, referential integrity, cross-customer email uniqueness enforcement | Persistence boundary |
| Historical product names/prices/amounts plus current customer details | Order response assembly |

Concurrency is documented separately in [Order Placement Concurrency](../03-decisions/ADR-0001-order-placement-concurrency.md). Its business policy is accepted. [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) describes the Proposed technical transaction and locking strategy; it is not implementation-verified.

Remaining design questions:

1. Concrete SQL column/check declarations must be reviewed against the Accepted schema/PRD; prove converter hydration, unconstrained NUMERIC capacity and Java/PostgreSQL normalization/collation consistency. No runtime evidence exists yet.
2. List/pagination-specific fetching, production migration execution/privileges and the detailed ADR-0002 protocol remain Open/Proposed for later work.
3. A line can reach EUR 9,999,999,900.00 and multi-product totals can exceed it. No artificial fixed maximum is selected for Order totals; any additional overall order-size restriction requires approval.

UUID identity in the diagram is now Accepted, including OrderItem's private persistence identity and unique (order_id, product_id); it does not introduce an item API field.

## 9. Suggested Acceptance Tests

- Reject an order without items.
- Reject an item with zero, negative, or fractional quantity.
- Reject orders for nonexistent or inactive customers.
- Reject orders containing nonexistent or inactive products.
- Calculate the expected discount for subtotals below, at, and above EUR 100.00.
- Consolidate duplicate product references according to the agreed rule.
- Preserve original product names, unit prices, and monetary results after catalog changes; display current customer details.
- Reject caller-supplied prices with 400; reject inactive references with 409, as specified in the PRD.
- Verify the EUR 100.05 HALF_UP example and unchanged historical amounts after pricing-rule changes.
- Retrieve a placed order with the same item values and monetary results that were recorded at placement.

## 10. Status and Next Step

Aggregate boundaries, snapshots, names/value objects and D1–D10 persistence selections are Accepted. Increment 3 implementation, concrete DDL review and scoped compatibility tests executed successfully; Increment 3 is Reviewed – Accepted. See the [Increment 3 execution record](../04-agentic-coding/prompts/04-domain-model.md#implementation-results--executed-pending-human-review). Deferred list fetching and production/locking questions remain unresolved. Consult [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md) and the [implementation plan](../07-implementation/implementation-plan.md) before implementation; no code is authorized by this document.

## Increment 4 Product/Customer Version Direction

Human Accepted @Version for Product and Customer, with confirmed optimistic conflicts mapped outside the domain to CONCURRENT_MODIFICATION/409. [ADR-0006](../03-decisions/ADR-0006-product-customer-optimistic-locking.md) is Proposed as a complete record; the exact boxed Long mapping, BIGINT/NOT NULL/existing-row 0/no final default policy and sequential PATCH no-update/no-increment semantics are now Human Accepted there. Concrete four-statement V2 SQL is reviewed; scoped implementation and runtime verification passed; human acceptance is recorded in [final Increment 4 acceptance](../04-agentic-coding/prompts/05-product-customer-api.md#final-human-acceptance--2026-10-15). Existing UUID identity, reference equality, domain operations, converters, monetary D6 and all D1–D10/Increment 3 acceptance remain unchanged. Version is internal persistence metadata; no API field, Order/OrderItem version or ETag/If-Match selected. It does not establish future Order read/lock consistency. Original diagrams describe the accepted Increment 3 model; the later Product/Customer version mapping is implemented and human accepted in Increment 4.

## Product Image Metadata — CH-002

`com.example.agenticorderservice.product.domain.ProductImage` is a directly mapped JPA metadata entity with field access, not a value object. Its `UUID productId` is the `@Id`; there is no separate generated image ID or mapped Product object association. Product itself has no image field or collection. The optional `product_images` row is owned by the Product feature. `product_id` is both its primary key and a foreign key to `products(id)`, enforcing at most one association and preventing references to nonexistent products. The foreign key uses ON DELETE NO ACTION; original/thumbnail keys are separately unique, with checks for managed-key shape, supported media, positive bounded sizes and dimension limits. Existing products require no row. The additive V3 migration preserves existing records; production/persistent-database migration execution still requires its existing safety gate.

Metadata comprises `original_key`, `thumbnail_key`, original `media_type`, original/thumbnail byte sizes, original width/height and thumbnail width/height. No image BLOB or absolute machine path belongs in PostgreSQL. Originals are JPEG or PNG; thumbnails are PNG. Keys identify the same generated immutable UUID directory with `original` and `thumbnail` children. Product response fields, order snapshots, prices and activity are unaffected.

Product-owned application operations serialize association changes with the existing product-row lock; controllers do not access repositories directly. Image metadata is independent of the Product aggregate's price/name/activity state. See [storage and failure recovery](system-architecture.md#product-image-storage-and-lifecycle--ch-002) and [CH-002 verification](../04-agentic-coding/change-requests/CH-002-product-images/03-review.md). This scoped direction does not accept unresolved ADR-0002 or ADR-0006 details.

### Product Image Metadata — Creation, Validation and Persistence

[`ProductImageController.upload()`](../../src/main/java/com/example/agenticorderservice/product/api/ProductImageController.java)
reads the multipart `file` and passes the existing path `id`, bounded bytes and declared
content type to
[`ProductImageApplicationService.upload()`](../../src/main/java/com/example/agenticorderservice/product/application/ProductImageApplicationService.java).
The service checks write enablement, invokes
[`ProductImageProcessor.process()`](../../src/main/java/com/example/agenticorderservice/product/storage/ProductImageProcessor.java),
then stages both files with
[`ProductImageStorage.stage()`](../../src/main/java/com/example/agenticorderservice/product/storage/ProductImageStorage.java).
Inside its application-owned transaction it locks/verifies Product, loads any existing
metadata through the application-owned `ProductImageMetadataStore`. The JPA
`ProductImageMetadataAdapter` maps immutable snapshots to the unchanged entity and
uses `ProductImageRepository` inside the same application-owned transaction.
Transaction completion precedes success; file cleanup and uncertain-outcome handling
follow the [storage recovery policy](system-architecture.md#product-image-storage-and-lifecycle--ch-002).

| Persisted Java attribute | Source passed by the application service |
| --- | --- |
| `UUID productId` | Existing REST path `id`, passed as the service's `productId`; the locked Product must exist. |
| `String originalKey` | `stage()` returns the generated storage UUID plus `/original`. |
| `String thumbnailKey` | `stage()` returns the same storage UUID plus `/thumbnail`. |
| `String mediaType` | `process()` maps the detected ImageReader format to canonical `image/jpeg` or `image/png` and compares it with the declared type. Describes the original only. |
| `long originalSize` | `processed.original().length`, measured from the original upload bytes. |
| `long thumbnailSize` | `processed.thumbnail().length`, measured from the generated PNG bytes. |
| `int width` | `ImageReader.getWidth(0)` for the uploaded image. |
| `int height` | `ImageReader.getHeight(0)` for the uploaded image. |
| `int thumbnailWidth` | `max(1, round(width * scale))`, where `scale = min(1.0, 320.0 / max(width, height))`. |
| `int thumbnailHeight` | `max(1, round(height * scale))`, using the same scale. |

`productId` remains the catalog identity and metadata primary key. The separately
generated `UUID.randomUUID().toString()` in `stage()` identifies an immutable filesystem
generation, not another entity ID. Replacement creates a fresh generation while
retaining `productId`. The processor clones and preserves the original bytes without
resizing or recompression; it decodes them for validation and draws a separate
aspect-ratio-preserving thumbnail, without upscaling, encoded as PNG.

The service constructs validated metadata and selects an immutable snapshot. For a
first image, the JPA metadata adapter calls the public
[`ProductImage(...)` constructor](../../src/main/java/com/example/agenticorderservice/product/domain/ProductImage.java)
with all ten values. The constructor requires a non-null `productId` and delegates the
other nine attributes to `replace()`. Replacement calls `ProductImage.replace()` on
the existing managed entity with those nine values; it validates before assignment.
The adapter saves that same managed instance; snapshot reads copy scalar getters
without reconstructing or revalidating persisted metadata. No factory
method, individual setters or Product object association is used.

Validation is layered: the REST boundary bounds file reads; the processor validates
nonempty JPEG/PNG bytes, declared/detected format agreement, at most 10 MiB, original
edges at most 16,384 and at most 40 million pixels before decoding. Entity validation
checks supported media, sizes, dimensions, thumbnail bounds of 1–320 without upscaling,
and paired generation keys. The
[V3 migration](../../src/main/resources/db/migration/V3__create_product_images.sql)
adds NOT NULL, PK/FK, unique-key and CHECK enforcement. These validators are not
identical: Java requires positive `thumbnailSize` but SQL also caps it at 10 MiB;
Java accepts a looser 36-character hex/hyphen key prefix than SQL's UUID-shaped
pattern. Normal generated keys and bounded PNG thumbnails satisfy these stricter
constraints; arbitrary directly constructed metadata is not guaranteed to do so.
See the [CH-002 validation and round-trip verification items](../04-agentic-coding/change-requests/CH-002-product-images/03-review.md#v-006--java-versus-sql-metadata-validation).

Hibernate reads the entity through its protected no-argument constructor and
`@Access(AccessType.FIELD)` mappings, populating fields directly. Hydration does not
call the public validating constructor or `replace()`. PostgreSQL holds the ten
metadata attributes in `product_images`; image bytes remain in the configured
filesystem under their relative managed keys. Functional image retrieval and decoded
thumbnail tests do not by themselves verify every persisted metadata column. The
executed [V-007 round-trip test](../04-agentic-coding/change-requests/CH-002-product-images/03-review.md#v-007--execution-and-independent-review)
now separately compares all ten attributes after fresh JPA reload and direct SQL,
for JPEG/PNG initial uploads and replacements; execution and independent review Passed.
