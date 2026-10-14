# Transaction and Locking Conventions

**Status:** Accepted application conventions; detailed protocol Proposed and implementation-unverified

## Authority

[ADR-0001](../03-decisions/ADR-0001-order-placement-concurrency.md) owns the accepted business policy. [ADR-0002](../03-decisions/ADR-0002-transaction-locking.md) preserves the detailed proposal. Adoption of READ COMMITTED and pessimistic locking does not promote the complete ADR or establish correctness of actual JPA SQL.

## Decision Scope

Accepted: application ownership, TransactionTemplate, ambient rejection, disabled OSIV, fresh attempt contexts, complete materialization, atomicity, READ COMMITTED/deterministic pessimistic locking baseline, and no uncertain-outcome replay.

Proposed: the full ADR-0002 protocol, precise propagation setup, lock-query paths, ordering comparator, retry eligibility/count and technical HTTP responses. Open: concrete timeouts/budgets/delays, error classifiers, the locking comparator, and reconciliation. UUID identities are now Accepted in ADR-0004; that does not select the comparator or accept the protocol. Wording in a Proposed section describes the candidate implementation, not additional acceptance. Verification remains Pending.

## Accepted Boundaries

- The application use case owns a transaction spanning all participating database operations. No repository operation or module helper independently commits part of an order.
- The order retry coordinator explicitly rejects an active ambient transaction before entering any attempt. It must neither join it nor silently suspend it. An explicit runtime guard also protects internal/self-invoked paths.
- Use TransactionTemplate for each complete order attempt; do not silently suspend an ambient transaction. Concrete propagation configuration remains Proposed, as described below.
- Open EntityManager in View is disabled (`spring.jpa.open-in-view=false`). Use transaction-scoped persistence contexts and fresh authoritative reads for each attempt; never reuse failed-attempt entities or an extended request context.
- Fully construct immutable Order DTOs and materialize all required items, monetary facts, and current customer details inside the relevant transaction. Read operations use read-only application transactions. Creation returns its materialized result only after successful commit; HTTP serialization afterward performs no lazy reads.

## Completion and Retry Boundary

The coordinator surrounds TransactionTemplate execution, including commit/rollback, rather than retrying its callback or individual queries. Callback return and flush do not establish success. Commit exceptions must reach the coordinator. A new attempt begins only after the previous transaction ended, its context was released, and noncommit is known. Let invalidating failures escape; explicitly handle rollback semantics for checked failures. Do not continue a rollback-only attempt or use a self-invoked transactional annotation as the boundary.

## Proposed Retry Configuration and Failure Handling

REQUIRED propagation is a proposed configuration relying on explicit nonambient entry; this document does not approve its concrete wiring.

ADR-0002 proposes at most two additional attempts, finite waits, and bounded delays/budget. Proposed retry candidates are `40P01` deadlocks, `40001` serialization failures, and a confirmed lock-timeout path such as `55P03`, only after full confirmed rollback. Do not retry all cancellations (`57014`), integrity errors, or connection failures indiscriminately. Duplicate normalized-email violations are business conflicts, not generic retry candidates. Precise exception classification and delays remain open.

Confirmed rollback means no order/items from the attempt. Lost commit confirmation means a complete order may exist. Never replay automatically or claim rollback for an unknown outcome. ADR-0002 proposes 503 on transient exhaustion and 500 for unexpected/unknown outcomes where a response is possible; a failed HTTP connection can prevent any response. Error taxonomy and reconciliation remain open.

## Proposed Locking Protocol

1. Validate and consolidate product quantities before locking.
2. Begin an attempt and lock Customer by ID without an active-status filter.
3. Lock distinct Products individually in a deterministic shared ID order. UUID identities are Accepted under ADR-0004; the shared ordering comparator remains Open. Bulk ORDER BY is not acquisition proof.
4. Validate fresh protected eligibility and capture names/prices after all locks are held.
5. Calculate and atomically persist original order/item facts; hold locks through completion.

Within the Proposed protocol, product name/price updates and deactivation lock their product first. Customer updates/deactivation/deletion lock their customer. Any operation touching both uses customer before products; multi-product writes use the shared ordering. Prevent pre-lock dirty mutations, autoflush surprises, stale caches, reverse-order paths, and unreviewed bypass operations.

The Proposed deletion protocol locks the Customer and then checks Order references in a separate subsequent READ COMMITTED statement. If placement committed first, deletion must observe the reference and deactivate; if deletion committed first, placement finds no customer. Restrictive foreign keys backstop integrity, but do not protect active status or prices. Normalized-email uniqueness requires its own database constraint.

## Verification Gate

Deterministic PostgreSQL/Testcontainers tests must prove both operation orders, multi-product consistency, the deletion race, atomic rollback, actual generated locking SQL, complete transaction retries, and unknown-outcome nonreplay. Use independent connections and barriers/latches with bounded blocking observation. No implementation or concurrency behavior is verified by these documents. See the [testing strategy](../05-testing/testing-strategy.md).

## Increment 4 Product/Customer Transaction Policy

Human Accepted: application writes @Transactional, reads @Transactional(readOnly=true); externally invoked proxy boundaries must complete before successful HTTP response. Controllers own no transaction; domain objects do not use Spring transaction APIs. Materialize immutable application results within transactions and serialize/map afterward without lazy persistence access. API translation occurs outside transaction completion, including known uniqueness/version failures raised at flush/commit. Proxy/self-invocation/interceptor/cause-chain behavior requires technical verification.

Product/Customer @Version direction is Human Accepted; [ADR-0006](../03-decisions/ADR-0006-product-customer-optimistic-locking.md) remains Proposed as a complete record, with finalized mapping/schema/no-op choices Accepted and concrete SQL/implementation/runtime verification Pending. Confirmed optimistic conflicts use CONCURRENT_MODIFICATION/409 without automatic write retries; known normalized-email conflicts retain DUPLICATE_EMAIL/409. Unexpected technical failure→500; only clearly classified temporary dependency outage→503, as the [catalog](api-error-codes.md) defines. Confirmed rollback and uncertain completion remain distinct; 500/503 does not establish rollback and transport failure may prevent a response.

This scoped policy adds Product/Customer version checking; it does not implement/replace the Order TransactionTemplate, ambient guard, pessimistic protocol, retries, timeout/exhaustion classifiers or customer-delete race in ADR-0002. Earlier Proposed technical-response statements apply to that deferred Order work, not a reopening of Accepted Increment 4 catalog decisions. Optimistic versions alone cannot validate all Order placement reads. Full protocol and production gates remain Pending/Open.

Sequential repeated Product/Customer deactivation is Accepted as 200 with unchanged inactive representation and no SQL UPDATE/version increment. Concurrent overlapping writes may conflict according to observed versions and actual writes; do not retry automatically. The exact declaration/migration policy and seven PostgreSQL scenarios are owned by [ADR-0006](../03-decisions/ADR-0006-product-customer-optimistic-locking.md#human-approved-idempotent-deactivation), not duplicated here.
