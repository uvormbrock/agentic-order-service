# ADR-0004: Persistence Mapping

**Status:** Accepted baseline; detailed converter choices Proposed; schema decisions Open
**Recorded:** 2026-10-13
**Implementation:** Not started

## Context and Accepted Decision

Map Customer, Product, and Order aggregate roots directly with JPA/Hibernate; map OrderItem as an Order-owned child, without independent CRUD/repository lifecycle. Use immutable Money and EmailAddress value objects and separate REST DTOs. Preserve IDs between aggregates rather than mutable cross-aggregate object graphs. Domain mutation stays controlled; persistence hydration does not make invalid states acceptable.

Historical productName, quantity, unitPrice, lineTotal, subtotal, discount, and total remain original. Customer details remain current. See the [domain model](../02-architecture/domain-model.md) and [PRD](../01-requirements/product-requirements.md).

## Proposed Mapping Conventions

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

## Open Decisions and History

OrderItem persistence identity, concrete constraints/JPA mapping, and original monetary storage representation remain Open. One line can reach EUR 9,999,999,900.00, and several distinct products can exceed it; numeric precision/scale and overall order-size/total limits are not selected. Business calculation scale is two decimal places; database declaration remains a persistence decision.

Direct mapping and immutable values were explicitly adopted by the user. The earlier separate-model recommendation is superseded by this baseline, not erased from history. Converter details came from a later unapproved skeleton proposal and remain Proposed. See [implementation gates](../07-implementation/implementation-plan.md).
