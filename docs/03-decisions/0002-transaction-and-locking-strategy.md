# ADR-0002: Transaction and Locking Strategy

**Status:** Proposed

## 1. Context

The MVP is a Java 21 / Spring Boot modular monolith using Spring Data JPA and PostgreSQL. Customer, Product, and Order are independent aggregates; Order owns its items.

[ADR-0001](0001-order-placement-concurrency.md) defines the accepted business consistency policy. This ADR proposes its technical enforcement without changing that policy or the [Product Requirements](../01-requirements/product-requirements.md). Related designs: [Domain Model](../02-architecture/domain-model.md), [Order Item Snapshots](../02-architecture/order-item-snapshots.md), and [REST API Design](../02-architecture/rest-api-design.md).

The strategy is not implementation-verified. Deterministic PostgreSQL/Testcontainers integration tests must verify the multi-product locking protocol and customer deletion race handling before implementation is considered correct.

## 2. Requirements and Constraints

- Successful conflicting operations must behave as a consistent serial ordering, preserving the actual order of non-overlapping operations.
- Capture original product names, prices, and eligibility consistently; preserve original quantities and calculated amounts. Customer details remain current on retrieval.
- Only existing active customers and products are eligible for new orders. Preserve existing 400/404/409 business-error semantics.
- Prevent physical deletion of referenced customers; deactivate them instead. Product physical deletion is outside the MVP.
- Save the order and every item atomically, preserving references and historical facts.
- Retry only complete attempts known not to have committed. Never automatically replay after an uncertain commit outcome.
- No idempotency-key feature, new domain patterns, or application code is introduced by this ADR.

## 3. Alternatives Considered

| Approach | Correctness and complexity | Contention, performance, and testing |
| --- | --- | --- |
| Optimistic version fields | Version checks on an entity update do not automatically validate customer/product reads when inserting an order. Correct cross-aggregate validation requires additional coordination, especially for deletion and multiple products. | Avoids long-held read locks, but conflicts require complete retries. Tests must prove that all relevant referenced versions are checked. More moving parts than needed here. |
| READ COMMITTED with pessimistic locks | Explicitly protects the required existing rows through commit, provided every relevant operation follows the protocol below. Most directly explainable for this MVP. | Shared customers/products serialize overlapping operations. Deadlocks and timeouts remain possible; blocking behavior and generated SQL must be tested. |
| PostgreSQL SERIALIZABLE | Protects against serialization anomalies when relevant transactions participate consistently, but does not solve stale JPA state or uncertain commit outcomes. | Requires complete retries on serialization failures; behavior depends on read/write dependencies. Defensible alternative, but less explicit for this demonstration. |

Propose READ COMMITTED with pessimistic write locks. This is a protocol-specific guarantee for the listed MVP operations, not a claim that READ COMMITTED makes arbitrary transactions serializable.

## 4. Proposed Transaction Strategy

Each attempt uses one short, read-write PostgreSQL READ COMMITTED transaction and a fresh transaction-scoped persistence context. Acquire exclusive row locks with Spring/JPA pessimistic-write semantics and verify the emitted SQL provides PostgreSQL `FOR UPDATE` or equivalent conflicts with updates and deletion.

Retain all locks through flush and commit or full rollback. Do not release them early through partial savepoint rollback. Do not perform external calls, user interaction, or response serialization inside the locked transaction. No partial commits or separate transactions for individual items are permitted.

Restrictive foreign keys must protect orders-to-customers, items-to-orders, and items-to-products, without cascading customer/product deletion into orders or items. Explicit locks protect status, name, and price; foreign keys alone do not. A uniqueness constraint on normalized customer email must enforce concurrent email uniqueness separately from this locking protocol.

An order response may be returned as successful only after commit completes. A successful flush is not proof of commit.

## 5. Lock Acquisition Protocol

### Order placement

1. Validate request structure and positive quantities; consolidate duplicate product references and enforce the per-product limit before acquiring locks. Use safe arithmetic for consolidation.
2. Begin the attempt transaction and lock the customer by ID, without filtering by active status. A missing row produces 404.
3. Sort distinct product IDs using one deterministic comparator shared by all multi-product operations. Lock each product individually by ID in that order, without filtering by active status. A missing row produces 404.
4. After all locks are acquired, validate customer and product eligibility; inactive references produce 409 and full rollback.
5. Capture product names and unit prices from the protected records. Calculate line totals, subtotal, rounded discount, and total using the approved rules.
6. Persist the complete order and items, then commit while holding the locks.

Earlier locked rows cannot change while later locks are acquired. At completion of lock acquisition, all captured records coexist in a protected state. A transaction that updates several relevant products must use the same ordering and retain its locks through commit; incompatible mixtures of before/after states must not be accepted.

### Participating operations

| Operation | Required protocol |
| --- | --- |
| Product price/name update or deactivation | Lock the product first, read its current state, then modify and commit. |
| Customer details update or deactivation | Lock the customer first, read its current state, then modify and commit. |
| Customer deletion | Lock the customer, then query order references in a separate subsequent statement; delete only if unreferenced, otherwise deactivate. |
| Operation touching customer and products | Customer first, then distinct products individually in the shared deterministic order. |

Customer deletion must not trust an existence check performed before locking or a check embedded in the original locking statement. After waiting for placement to commit, the subsequent READ COMMITTED statement must see the new order reference. If deletion wins first, a later customer lock finds no row and placement returns 404. If placement wins first, deletion observes its order and deactivates the customer. All order insertions must participate in this customer protocol; the foreign key is a final integrity safeguard.

### JPA and PostgreSQL safeguards

- Do not assume `ORDER BY` in a bulk locking query guarantees acquisition order. Use separate ID-specific locking statements; verify SQL and blocking behavior on PostgreSQL.
- Acquire the required write lock initially; avoid read-to-write upgrades. Single-product operations need not acquire unrelated customer locks.
- Do not preload entities and then assume locking refreshes their fields. Use fresh locked reads in a fresh persistence context, or an explicitly verified refresh-under-lock path if entities were loaded earlier. Do not use cached values for authoritative checks or snapshots.
- Avoid mutations or JPA autoflush before the intended locks are acquired. Inspect eager relationship loading, automatic flushes, and provider follow-on locking for unexpected reads, writes, or lock acquisition.
- Keep explicit locks ahead of order insertion and its foreign-key checks. Do not introduce reverse-order paths, cascading writes, or bulk updates that bypass the protocol.
- Consistent ordering reduces relevant deadlocks; it does not eliminate cycles caused by other constraints, triggers, or future operations. Referential constraints and normalized-email uniqueness remain necessary.

## 6. Retry and Error Handling

A nontransactional coordinator invokes each complete attempt through a real Spring transaction boundary. Do not rely on self-invocation of a transaction-annotated method. Each retry starts only after the previous transaction has ended, with a new persistence context and fresh reads. Do not accidentally join an enclosing transaction or retry a rollback-only transaction. Commit exceptions must reach the coordinator.

Allow at most two additional attempts (three total), subject to a bounded overall duration and short bounded delay between attempts. Configure finite lock waits. Exact timeout and delay values remain open. Do not retry individual repository statements or reuse entities/results from a failed attempt. Revalidation may legitimately yield a business rejection after a retry.

| Failure | Retry and response |
| --- | --- |
| Deadlock (`40P01`) | Retry the complete attempt only after confirmed rollback; on exhaustion return 503. |
| Serialization failure (`40001`) | Apply the same bounded rule if encountered; not normally expected from this READ COMMITTED protocol. |
| Lock timeout | Explicitly end/roll back the full attempt, then retry only when noncommit is confirmed. On exhaustion return 503. Do not classify every cancellation as a lock timeout. |
| Confirmed rollback from a nontransient failure | No automatic retry unless the cause is explicitly classified as safely transient. |
| Business rejection | No retry; preserve the PRD's 400/404/409 semantics. |
| Unexpected database error | No automatic retry; return 500 if a response is possible. A rollback alone does not make an error retryable. |
| Uncertain commit outcome | No automatic replay. The complete order may already exist. If a response is possible, return 500 with a safe message indicating unknown outcome and no claim of rollback; a broken connection may prevent any HTTP response. |

Inspect the root database failure and transaction completion rather than relying only on broad Spring exception categories. A JPA lock exception does not by itself prove full rollback or justify retry. The transaction boundary must roll back all failures that invalidate the attempt, including checked exceptions where applicable.

Return safe JSON `code` and `message` fields where a response is possible; do not expose SQL, internal IDs, or database exception text. Technical failures are not business conflicts. Do not attach a blind replay instruction to uncertain outcomes. Internal diagnostics should distinguish confirmed rollback from unknown commit; they do not provide an idempotency or reconciliation feature.

## 7. Failure and Concurrency Scenarios

| Scenario | Required outcome |
| --- | --- |
| Placement versus product price/name update | Placement first captures the old name/price; update first captures the new state. Later changes never rewrite the snapshot. |
| Placement versus product/customer deactivation | Placement first commits a valid order; deactivation first produces 409 and no order. |
| Placement versus physical customer deletion | Delete first produces 404 for placement; placement first forces deletion to deactivate instead. No dangling references. |
| Multiple products with overlapping updates | Snapshot corresponds to one valid ordering of committed operations; no incompatible combination of product states. |
| Failure while saving items | Confirmed rollback removes the entire attempt, including order and items. |
| Deadlock or lock timeout | End the failed attempt before any bounded retry. Successful retry creates only one order from that request. |
| Lost commit confirmation or HTTP connection | Database contains a complete order or none; never partial data. No automatic replay, no guaranteed HTTP response, and no inferred rollback. |

## 8. Verification Strategy

Use PostgreSQL through Testcontainers, matching the selected deployment major version. In-memory database tests cannot establish PostgreSQL locking correctness. No tests are implemented by this ADR.

Coordinate separate connections, persistence contexts, and transactions using barriers/latches at known stages. Confirm a contender is waiting using bounded PostgreSQL lock observations, then release the holder by commit or rollback. Use bounded test deadlines and clean up all workers. Do not rely on arbitrary sleeps, assume thread scheduling order, or wrap all workers in one test transaction.

Required integration scenarios:

1. Price update versus placement in both winning orders; verify original unit price and all amounts through HTTP retrieval and database records.
2. Product rename versus placement in both orders; verify the original captured name remains unchanged after later renames.
3. Product deactivation versus placement in both orders; verify success or 409 and absence of failed-attempt rows.
4. Customer deactivation versus placement in both orders, with the same eligibility guarantees.
5. Physical deletion versus a customer's first order in both orders. In particular, force deletion to wait on placement, then prove its separate reference query sees the newly committed order and deactivates the customer. Verify restrictive foreign keys and absence of dangling references.
6. Orders sharing several products, including reversed request item order and different customers. Force blocking at intermediate product locks; verify actual deterministic acquisition and serially valid snapshots. Include a controlled transaction changing multiple products, so a mixture that never existed as committed state would be detected. Verify the exact JPA/provider path, not just hand-written SQL.
7. Inject an item-persistence failure after order insertion/flush; confirm rollback leaves neither order nor items. Also verify a holder rollback lets a waiting operation proceed using the committed state.
8. Force a lock timeout with a controlled holder and, separately, a database deadlock using test-only opposing lock order. Verify full rollback, at most three attempts, fresh transactions, and 503 on exhaustion. Do not depend on which transaction PostgreSQL chooses as deadlock victim.
9. Inject lost confirmation after a real database commit at the JDBC/transaction boundary. Verify no second attempt and one complete order. Separately simulate loss of the HTTP connection: no response is required. Test a confirmed rollback separately so failure classification cannot conflate the two outcomes.

Assert HTTP statuses and error shape where transport remains available, order cardinality, immutable names/prices/amounts, eligibility, and referential integrity. A database-success test does not establish unknown-outcome handling without fault injection. The multi-product and deletion-race tests are mandatory implementation-verification gates, not assumed consequences of annotations.

## 9. Consequences and Trade-offs

The protocol is small and explainable and fits the existing aggregate boundaries. Conflicting placements serialize on a shared customer or product even when both only need to read it. Large orders retain more locks and can increase contention; keep transactions short and bounded rather than adding an unapproved item-count limit.

Correctness depends on every relevant write path participating, fresh authoritative reads, and actual provider-generated SQL. Future bulk operations, triggers, integrations, or aggregate changes require reassessment. The protocol does not establish serializability for unrelated arbitrary database queries.

No code, dependencies, migrations, idempotency keys, or retry framework is prescribed here. Proposed HTTP responses fill technical-error behavior left open by the existing REST design; business responses remain unchanged.

## 10. Open Technical Questions

- Selected PostgreSQL, Spring Boot, JPA provider, and driver versions; actual pessimistic-write SQL and cache/refresh behavior.
- Shared ID comparator, concrete locked-read queries, and constraint definitions in the persistence design.
- Lock timeout, overall transaction/request budget, and bounded retry delay values and their safe configuration scope.
- Exact technical error codes, root-cause classification, checked-exception rollback configuration, and commit-outcome fault-injection setup.
- Outcome reconciliation after uncertain commit remains undecided; no automatic replay or new reconciliation feature is authorized.

## 11. References

- [PostgreSQL transaction isolation](https://www.postgresql.org/docs/current/transaction-iso.html)
- [PostgreSQL explicit locking](https://www.postgresql.org/docs/current/explicit-locking.html)
- [PostgreSQL serialization failure handling](https://www.postgresql.org/docs/current/mvcc-serialization-failure-handling.html)
- [Spring transaction propagation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)
- [Spring transaction annotations](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- [Jakarta Persistence specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html)
