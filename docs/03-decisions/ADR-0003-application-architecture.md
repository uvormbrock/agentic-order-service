# ADR-0003: Application Architecture

**Status:** Accepted baseline
**Recorded:** 2026-10-13
**Implementation:** Not started; no code authorization

## Context and Decision

A small order-management MVP must keep business invariants explainable and order persistence atomic. Adopt one Maven module and one Spring Boot deployment, organized by product/customer/order features. Use narrow REST, application, domain, and persistence responsibilities. The [system architecture](../02-architecture/system-architecture.md) owns the operational overview.

Application use cases own transactions. Order placement uses TransactionTemplate outside an ambient transaction; reject ambient entry explicitly. Disable Open EntityManager in View and fully materialize response data inside application transactions. Domain behavior owns pricing/consolidation; controllers and repositories do not.

Cross-module operations use narrow provider-owned application contracts and immutable data, not direct foreign repositories or managed entity graphs. Small concrete components are sufficient; interfaces are optional when they protect an actual boundary. Helpers join the use-case transaction without independent commits.

## Rationale

One deployment/database enables atomic cross-aggregate coordination. Feature packages expose responsibilities without build-module overhead. Direct aggregate mapping is separately accepted in ADR-0004; this is a pragmatic modular monolith, not a claim of framework-pure domain isolation.

## Alternatives

- Horizontal-only controller/service/repository packages obscure feature ownership.
- Microservices and asynchronous coordination add distributed consistency unrelated to this MVP.
- Separate Maven modules and a generic ports framework add unnecessary maintenance.
- Broad CRUD service chaining hides transaction-sensitive access and encourages cycles.

## Consequences

Cross-feature workflows deliberately share one transaction. Narrow dependencies must remain acyclic at execution time even where feature contracts reference each other. Domain/JPA coupling is accepted; HTTP coupling is not. No new framework, message broker, event sourcing, or CQRS infrastructure is required.

## Verification Criteria

Review dependencies and controller thinness; test domain behavior without a server. Prove module helpers share transactions, ambient entry fails, OSIV is disabled, and response serialization performs no database reads. Transaction correctness requires the separate ADR-0002 integration gates.

## Open Details and History

Base package, exact class names, and implementation increments remain proposals until scoped approval. Accepted by the user's architecture-baseline instruction and documentation approval; replaces no earlier business ADR. ADR-0002 remains Proposed. Links: [ADR-0004](ADR-0004-persistence-mapping.md), [ADR-0005](ADR-0005-technology-stack.md).
