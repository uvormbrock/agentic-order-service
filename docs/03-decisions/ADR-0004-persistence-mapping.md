# ADR-0004: Persistence Mapping

**Status:** Accepted Increment 3 mapping decisions; deferred details Open; Increment 3 implementation Reviewed – Accepted
**Recorded:** 2026-10-13
**Implementation:** Increment 3 Reviewed – Accepted; later application/concurrency gates Pending

## Context and Accepted Decision

Map Customer, Product, and Order aggregate roots directly with JPA/Hibernate; map OrderItem as an Order-owned child, without independent CRUD/repository lifecycle. Use immutable Money and EmailAddress value objects and separate REST DTOs. Preserve IDs between aggregates rather than mutable cross-aggregate object graphs. Domain mutation stays controlled; persistence hydration does not make invalid states acceptable.

Historical productName, quantity, unitPrice, lineTotal, subtotal, discount, and total remain original. Customer details remain current. See the [domain model](../02-architecture/domain-model.md) and [PRD](../01-requirements/product-requirements.md).

## Historical Proposed Mapping Conventions

The following proposal is preserved as recorded history. The Accepted Increment 3 selections below supersede its explicit-conversion preference and then-open identity/storage/name/email selections.

- Money: immutable amount/currency value; explicit JPA AttributeConverter to BigDecimal for the EUR-only MVP, reconstructing EUR and rejecting other currencies. No binary floating point and no scale-based inequality for equivalent values. Column precision/capacity and converter details are not selected by this proposal.
- EmailAddress: immutable normalized string value with an explicit converter to text. The accepted business policy trims, lowercases, limits to 254 characters after normalization, and enforces case-insensitive uniqueness across active/inactive customers. Do not use provider-specific dot/plus rules.
- Keep converters in persistence packages and value objects free of JPA annotations. Prefer explicit conversion rather than globally auto-applying conversions. Compare against embeddable mappings before accepting converter details.

Exact basic email syntax, Unicode counting, SQL normalization/collation enforcement, and concrete converter behavior remain open. Do not reopen the approved normalization/uniqueness semantics.

## Rationale and Alternatives

Direct mapping reduces duplicate models and snapshot mapping drift for this small MVP. Separate JPA/domain objects improve framework independence but add little concrete benefit at this scale. Embeddables are a valid alternative for Money if future currency requirements justify multiple columns; multi-currency behavior is not an approved feature. Exposing JPA entities as REST payloads is rejected because it leaks persistence state and lazy loading.

## Consequences

The domain intentionally carries JPA metadata and entity construction constraints. Use controlled setters/access and immutable value replacement; immutable value objects do not imply final JPA entities. Mapping and domain tests must protect invariants against hydration and dirty-checking behavior. Concrete annotations, cascades, fetch queries, indexes, and schema require review. Only Order owns item persistence; never cascade Customer/Product deletion into historical orders.

## Verification Criteria

PostgreSQL round trips for aggregates and both value objects; unchanged original snapshot fields after product edits; normalized email uniqueness under concurrent create/update; valid boundary values; complete DTO materialization with OSIV disabled. Verify numeric capacity and inspect generated SQL/cascades. No mapping evidence exists yet.

## Historical Open Decisions and History

This section records the pre-approval state; current selections and remaining boundaries follow below.

OrderItem persistence identity, concrete constraints/JPA mapping, and original monetary storage representation remain Open. One line can reach EUR 9,999,999,900.00, and several distinct products can exceed it; numeric precision/scale and overall order-size/total limits are not selected. Business calculation scale is two decimal places; database declaration remains a persistence decision.

Direct mapping and immutable values were explicitly adopted by the user. The earlier separate-model recommendation is superseded by this baseline, not erased from history. Converter details came from a later unapproved skeleton proposal and remain Proposed. See [implementation gates](../07-implementation/implementation-plan.md).

## Accepted Increment 3 Selections and Replacement History

The human reviewer explicitly accepted Variant A for D1–D10. This accepts design choices, not implementation or runtime correctness; Increment 3 execution still requires explicit authorization. The [PRD](../01-requirements/product-requirements.md) owns business validation and [ADR-0005](ADR-0005-technology-stack.md#accepted-increment-3-localtest-migration-policy) owns migration execution policy.

- **D1/D2:** Customer/Product/Order and each owned OrderItem have Java UUID/PostgreSQL uuid IDs generated with `@GeneratedValue(strategy = GenerationType.UUID)`. New entity IDs are null until persistence assigns them. No custom generator, database UUID extension or BaseEntity. Unique `(order_id, product_id)`; no independent item repository/CRUD/API identity.
- **D3:** Direct field-access mappings, protected no-argument constructors and controlled mutation; no unrestricted setters. Immutable Money/EmailAddress remain JPA-annotation-free. MoneyConverter implements AttributeConverter<Money, BigDecimal>; EmailAddressConverter implements AttributeConverter<EmailAddress, String>. Both use `@Converter(autoApply = true)` in corresponding persistence packages. Domain never depends on those packages.
- **D4:** Order.items uses `@OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)`, orphanRemoval false. OrderItem.order uses `@ManyToOne(fetch = FetchType.LAZY, optional = false)` and owns the non-null order_items.order_id FK. Order maintains both ends. No CascadeType.REMOVE; customer/product references are UUID values with restrictive FKs, not managed cross-aggregate associations.
- **D5:** Lazy items and explicit single-order JOIN FETCH or equivalent reviewed fetch plan, such as findByIdWithItems(UUID). Materialize inside transactions; OSIV remains disabled. No blanket EAGER. List/pagination-specific plans remain deferred.
- **D6:** Unconstrained PostgreSQL NUMERIC for all monetary columns, without fixed precision/scale. Named checks reject NaN/positive or negative Infinity and scale greater than two rather than coercing it. Apply non-negative checks to persisted fields whose invariants require them, including product price 0.00–999,999.99; no universal ban on signed Money arithmetic or artificial Order-total cap. Store original amounts explicitly and verify exact BigDecimal round trips/Hibernate compatibility.
- **D7:** Customer/product names use String.strip(), 1–200 Unicode code points, preserving case/umlauts/accents/valid Unicode. Preserve approved ASCII EmailAddress semantics. Store normalized email with unique lower(email) index across all active/inactive customers; no partial uniqueness or citext. Uniqueness stays outside the value object. Verify Java/PostgreSQL normalization/case/collation consistency.
- **D8:** customers/products/orders/order_items, UUID PKs, required NOT NULLs, restrictive FKs without deletion cascades, named approved name/quantity/price/monetary checks and uniqueness. Index orders.customer_id and order_items.product_id; avoid redundant/speculative indexes. Store original item productName/unitPrice/lineTotal and Order subtotal/discount/total. Review concrete column/check declarations against the PRD before recording final SQL; no added business restrictions.
- **D9:** Customer/Product/Order/OrderItem keep reference identity: do not override equals/hashCode or generate entity-wide equals/hashCode/toString. Compare UUIDs explicitly; no lazy traversal or database access from object/logging methods. Values retain value equality/hash.
- **D10:** First domain migration V1__create_order_domain_schema.sql and local/test policy are Accepted in ADR-0005; production remains Open.

**Explicit replacement:** autoApply=true replaces the earlier Proposed preference for explicit converter assignment. Auto-application preserves domain dependency directions without converter imports or extra XML configuration. The historical proposal above remains intact; it is not the current instruction.

Rationale: generated UUIDs keep Spring Data insertion/newness simple; ordinary child FK ownership supports aggregate persistence; explicit lazy fetch plans expose query behavior; unconstrained NUMERIC avoids silently inventing a fixed total capacity. Considered alternatives include bigint/assigned IDs, composite child keys, explicit XML conversion/embeddables, unidirectional collections, fixed decimals and ID-based equality. They were not selected for this MVP. Consequences: global conversion must stay consistent for each value type; reference equality differs across instances; database storage has physical limits and constraints cannot prove active eligibility/cross-row totals.

## Remaining Review and Verification Boundaries

Concrete SQL declarations/constraint expressions and mapping compatibility require implementation review/evidence, not repeat approval of already selected D1–D10 policies. Do not infer a new collation setting, business limit or permission if a genuine gap appears; report it for approval. List/pagination plans, production privileges/execution and full ADR-0002 implementation remain outside these selections.

Verify generated IDs/newness/proxies, converter discovery/hydration, root/item round trips, both relationship ends, no deletion cascades, maximum line and larger totals, all-customer case-insensitive uniqueness, negative named constraints, migration/Hibernate validation, query counts and closed-context materialization on PostgreSQL/Testcontainers. Domain tests cover accepted Money/email/name invariants. Actual REST DTO and concurrency gates remain later Pending. No code, migration, database changes or executed Increment 3 tests are claimed.

## Increment 3 Execution Evidence — Pending Human Review

On 2026-10-13 the human explicitly authorized the finalized implementation prompt. The [Increment 3 execution record](../04-agentic-coding/prompts/04-domain-model.md#implementation-results--executed-pending-human-review) records generated mappings/repositories/V1, 25 passing fast tests and 14 passing PostgreSQL integration tests, including Hibernate validation, converter round trips, unconstrained NUMERIC capacity/negative checks, migration checksums/repeat execution and root-owned item persistence. Earlier statements of no implementation/evidence describe the recorded pre-execution state. No decision or historical proposal is replaced by these results; implementation acceptance remains Pending. ADR-0002 stays Proposed and implementation-unverified. Local development data/volumes were preserved; production, HTTP/application and concurrency verification remain outside this increment.

## Increment 3 Human Acceptance

On 2026-10-13 the human accepted the reviewed implementation and test hardening after the local diff and 120 fast/21 PostgreSQL reports were confirmed. See the [final acceptance record](../04-agentic-coding/prompts/04-domain-model.md#final-human-review-and-acceptance). Earlier Pending review statements remain historical execution evidence. Accepted decisions and their history are unchanged; ADR-0002 remains Proposed and implementation-unverified, and later application/concurrency and production gates remain deferred.
