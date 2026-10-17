# Increment 5 — Order Placement & Concurrency

## Status

**Reviewed – Accepted**

**Recorded:** 2026-10-16 (Europe/Berlin). **Implementation:** Explicitly authorized by the human execution request and executed. **Verification:** Preserved execution evidence supports final human acceptance on 2026-10-16; see the final acceptance record. Historical Pending statements below describe earlier stages.

The [final G1–G6 record](#final-human-architecture-decisions--2026-10-16) supersedes the earlier proposals only for the explicitly accepted choices. All six architecture gates are resolved; the scoped implementation was explicitly authorized. Current execution evidence is appended below; historical Pending statements remain audit records. Complete ADR-0002 and ADR-0006 remain Proposed. Use the [current approved scope and matrices](#current-approved-scope-and-matrix-update) and [single current implementation prompt](#current-implementation-prompt--increment-5), not the historical draft below.

## Historical Planning Record — Before G1–G6 Acceptance

The following original objectives, proposals, gates, draft instructions and planning result are retained as audit history. Their Proposed/Pending wording is not the current decision state. The final decision record and current matrices appended below take precedence; no historical proposal authorizes implementation.

## Objective

Prepare the remaining Order creation/retrieval API and cross-aggregate consistency work against PRD FR-003–FR-006 and Accepted ADR-0001. Preserve the accepted Product/Customer API, domain/persistence model and quality infrastructure. Make the Customer DELETE scope decision explicit before coding.

## Authoritative References

- [Agent instructions](../../../AGENTS.md), [PRD](../../01-requirements/product-requirements.md).
- [System architecture](../../02-architecture/system-architecture.md), [domain model](../../02-architecture/domain-model.md), [REST design](../../02-architecture/rest-api-design.md), [API error catalog](../../02-architecture/api-error-codes.md), [transaction conventions](../../02-architecture/transaction-locking.md), [technology reference](../../02-architecture/technology-stack.md).
- [Accepted ADR-0001](../../03-decisions/ADR-0001-order-placement-concurrency.md), [Proposed ADR-0002](../../03-decisions/ADR-0002-transaction-locking.md), [Accepted ADR-0003](../../03-decisions/ADR-0003-application-architecture.md), [ADR-0004](../../03-decisions/ADR-0004-persistence-mapping.md), [ADR-0005](../../03-decisions/ADR-0005-technology-stack.md), [Proposed complete ADR-0006](../../03-decisions/ADR-0006-product-customer-optimistic-locking.md).
- [Coding](../coding-guidelines.md), [Java/Spring](../java-spring-guidelines.md), [persistence](../persistence-guidelines.md), [AI workflow](../ai-development-workflow.md).
- [Testing strategy](../../05-testing/testing-strategy.md), [mutation strategy](../../05-testing/mutation-testing.md), [automated PIT runbook](../../05-testing/mutation-testing-runbook.md#current-automated-workflow), [verified experiments](../../05-testing/mutation-results/mutation-testing-experiments.md).
- [Implementation plan](../../07-implementation/implementation-plan.md), [open items](../../00-overview/open-items.md), [accepted Increment 3](04-domain-model.md#final-human-review-and-acceptance), [accepted Increment 4](05-product-customer-api.md#final-human-acceptance--2026-10-15).

The PRD owns business rules, ADRs own decisions, and this playbook records the proposal and later execution evidence. It does not duplicate or replace their authority.

## Current Implementation Baseline

Read-only inspection: main checkout `develop`, HEAD `81f96e262fdf0a67c96ef7861856e95b9a185d55`, initially clean. Increment 4 is **Reviewed – Accepted** on 2026-10-15; its recorded implementation revision `8e66d8c1cc454c5300a1622ca3adc4c3d6d75f6e` is an ancestor of this HEAD. Its 309-fast/94-PostgreSQL and zero-findings Sonar evidence is historical acceptance evidence, not a new build/analysis. Later test/Makefile improvements are committed in `f1b79a44edf3bae91c3803092df69cfcbb29444e`. Capture an explicitly approved implementation baseline again before coding; do not freeze future work to this planning revision.

| Existing implementation | Reuse / gap |
| --- | --- |
| `order/domain/Order`, `OrderItem`, `OrderLineInput` | UUID root/owned-item mappings, consolidation, checked quantity sum, 10,000 limit, snapshots, 10% threshold/HALF_UP and original totals already implemented; do not rewrite pricing in services. |
| `shared/domain/Money`, converters; Customer EmailAddress | Exact EUR arithmetic and accepted normalization; preserve semantics and unconstrained NUMERIC. |
| `order/persistence/OrderRepository` | `save`, `findById`, `findByIdWithItems` explicit fetch; no list/reference-existence operation or Order use-case transaction yet. |
| Product/Customer application services and repositories | Ten REST operations, transactional writes/read-only reads and immutable result records exist; current mutations use ordinary `findById`, not explicit pessimistic reads. |
| Product/Customer entities | Internal boxed Long `@Version`, controlled irreversible deactivation; preserve confirmed conflict/409 and sequential no-op semantics. |
| Shared API and `configuration/ApiWebConfiguration` | Exact safe envelopes, typed exception classification, strict parser/converter, missing-body advice and header/path interceptor exist. Decoder supports four Product/Customer DTOs; interceptor matches only Product/Customer API packages. Order is not automatically covered. |
| Flyway V1/V2 and PostgreSQL infrastructure | Four domain tables, restrictive FKs, original numeric columns, unique order/product item pair, customer-reference index and version columns exist. `PostgreSqlTestConfiguration` uses the central approved image and isolated service connections. |
| Order implementation absent | No Order application service/controller/request/response DTO, provider-owned locking contract, ambient guard or retry coordinator. No customer physical-delete application operation. |

V1 and V2 already provide the apparent storage needs. Recommend **no new migration** unless an approved concrete gap is demonstrated; preserve applied SQL/checksums. This is a proposal, not permission to apply migrations or start a persistent local database.

## Proposed Scope and Explicit Exclusions

**Proposed core:** three PRD Order endpoints; transport and direct-application validation; consistent eligibility/name/price capture; atomic aggregate persistence; original monetary retrieval with current customer details; explicit transaction/ambient/attempt boundaries; deterministic lock coordination and reviewed bounded retries; compatible existing Product/Customer mutation paths; all three test levels below.

**Separately gated addition:** Customer DELETE/delete-or-deactivate. Recommend completing it alongside this protocol, but it is not included without gate G6 approval. If deferred, retain interim 405 and report the deletion race/MVP completeness obligation as deferred; do not claim full ADR-0001 enforcement.

**Excluded:** Product DELETE; Order PUT/PATCH/DELETE; reactivation; ETag/If-Match/client versions; idempotency keys or automatic uncertain-outcome reconciliation; generic retry/contract frameworks; new dependencies, Maven/Makefile/CI changes; pagination API or invented total/item-count limits; production deployment/migrations; persistent database migration without its independent recovery gate.

## PRD Traceability

| Requirement / acceptance criteria | Increment 5 obligation |
| --- | --- |
| FR-003, BR-002–005, AC-002/005/006/014 | Existing active resources; nonempty items; positive integral quantities; duplicate consolidation and 10,000 maximum; no rows on rejection. |
| FR-004, BR-006–009/013, AC-007/008/014/017 | Server prices only; exact line/subtotal/discount/total; threshold EUR 100.00 and HALF_UP; atomic commit. |
| FR-005/006, BR-010, AC-009/010/015/016 | Creation/get/list include original items/amounts, EUR currency and current customer; inactive records do not prevent retrieval. |
| AC-013/014/016 | 400 validation, 404 absent references/order, 409 inactive references, 405 unsupported operations; safe code/message. Exact new Order codes need G3 approval. |
| ADR-0001, AC-017/018 | Serially explainable conflicting operations, stable multi-product snapshot, complete rollback, no replay of uncertain outcome. |
| FR-002, BR-003, AC-003/004/013/018 | Conditional Customer DELETE/204 and first-order race; G6 decides increment inclusion, not whether the MVP requirement exists. |

## Proposed Order API Contract

Endpoint paths, success statuses and resource fields below are **Accepted PRD requirements**. Class names, new error-code names and Order-specific tie-breakers are proposals.

| Method / path | Request DTO (proposed name) | Response / success | Transaction | Required tests / trace |
| --- | --- | --- | --- | --- |
| POST `/orders` | `CreateOrderRequest(customerId, items)`; each `OrderItemRequest(productId, quantity)` | 201, complete `OrderResponse`; `Location: /orders/{id}` | Nontransactional coordinator; one committed read-write attempt before success | U1–U4, H1–H5, P1–P8; FR-003/004/006, AC-005–009/014/017/018 |
| GET `/orders/{id}` | Canonical UUID path; no request body DTO | 200, complete `OrderResponse`; 404 absent order | Application-owned read-only transaction | U5, H1/H4, P2/P9; FR-005/006, AC-009/010/013/015/016 |
| GET `/orders` | None | 200, array with same resource fields; `[]` when empty | Application-owned read-only transaction with explicit bulk materialization | U5, H1, P9; FR-005/006, AC-009/010/015/016 |

`OrderResponse`: exactly PRD `id`, `customerId`, `customer`, `items`, `currency`, `subtotal`, `discount`, `total`. Nested customer uses current `id`, `name`, `email`, `active`; item uses `productId`, original `productName`, `quantity`, original `unitPrice`, original `lineTotal`. No item persistence ID, version or tax breakdown. Use immutable transport-neutral application result records, then API mapping; never expose managed entities. Read historical stored amounts without recalculation. These fields apply equally to creation and lists; creation's customer data is current at its protected transaction read, not a promise of remaining unchanged after commit.

Reject unknown fields recursively and recognized server-owned fields such as root/item IDs, names/prices/amounts/currency/version rather than ignoring them. Check complete syntax, duplicate keys at every depth and trailing content before selecting field errors. Required/null/wrong-shape items and fields, malformed UUID strings and invalid/consolidated quantities must yield 400 with no application lookup before transport validation. Recheck quantities/consolidation for direct application calls before locks. Do not expose trusted `OrderLineInput` as an HTTP DTO.

Preserve Accepted header/path-before-body and six-category validation precedence, UUID case/shape and safe JSON behavior. **Proposed Order tie-breaker:** root customerId then items; item fields productId then quantity, array indices ascending; unknown/forbidden paths lexicographically. **Proposed integer-token policy:** require JSON integer tokens, reject strings, booleans, fractions, `1.0` and exponent notation; parse without overflow/coercion and normalize UUIDs before duplicate consolidation. PRD requires positive integers but does not explicitly settle integral decimal/exponent spelling or same-category multi-reference priority; approve these extensions under G3 before implementation.

| Failure | Existing authority / proposed Order extension |
| --- | --- |
| Malformed syntax, unknown/forbidden/type/presence errors | Reuse accepted catalog envelopes/statuses; approve Order trigger extensions, full recognized-field sets and nested precedence under G3. |
| Bad body/path UUID | Recommend existing `INVALID_RESOURCE_ID`/400 for canonical syntax; body-field applicability is Proposed. |
| Empty items, invalid/consolidated quantity | PRD 400 Accepted; recommend `INVALID_ORDER_ITEMS` / `INVALID_ORDER_QUANTITY` with fixed safe messages, exact wording Pending G3. |
| Customer/Product absent | PRD 404 Accepted; recommend extending `CUSTOMER_NOT_FOUND` / `PRODUCT_NOT_FOUND` unchanged safe templates to placement. |
| Customer/Product inactive | PRD 409 Accepted; recommend `CUSTOMER_INACTIVE` / `PRODUCT_INACTIVE`, exact safe messages Pending G3. |
| Stored Order absent | PRD 404 Accepted; recommend `ORDER_NOT_FOUND`, exact safe message Pending G3. |
| Unsupported Order mutations | PRD 405; reuse `METHOD_NOT_ALLOWED`, accurate Allow with supported GET/HEAD/OPTIONS where applicable. |
| Retry exhaustion / unknown completion | ADR-0002 proposes 503/500; approve exact Order technical envelope/unknown-outcome wording under G4. No blind retry instruction. |

The existing Product/Customer catalog remains authoritative and unchanged by this proposal. This document does not silently extend accepted codes or turn proposed messages into executable API constants. Approval must record exact messages before implementation.

## Transaction and Concurrency Architecture

### Accepted Conventions and Proposed Components

| Responsibility | Minimal proposed owner / dependency |
| --- | --- |
| Request/response binding | `order.api.OrderController`, explicit DTOs; calls Order application operations only. |
| Pure prevalidation | Small order application command/preparation operation; immutable customer ID and consolidated ID/quantity list; no database access. Retain domain invariant checks. |
| Retry and ambient guard | `order.application.OrderPlacementCoordinator`; constructor-injected TransactionTemplate and narrow collaborators; no enclosing transactional annotation. |
| Attempt orchestration | Ordinary internal operation/lambda under the template: locked customer, locked products, domain construction, root save and result materialization. No proxy required for its transaction boundary. |
| Cross-feature access | Concrete provider-owned `customer.application.CustomerOrderAccess` and `product.application.ProductOrderAccess` exposing immutable locked facts, never entities/repositories; propose mandatory participation in the existing transaction and runtime read-write checks. |
| Persistence | Own feature repository performs its ID-specific pessimistic read; Order owns save/fetch/reference queries. No foreign persistence imports. |
| Retrieval | `order.application.OrderQueryService` with read-only transactions; existing root/items fetch plus Customer-owned current-detail lookup/batch lookup. |
| Composition | Constructor injection; `configuration/OrderTransactionConfiguration` wires template and proposed timing settings; no extra framework or circular constructor graph. |

Order references Customer/Product access components, not broad CRUD services. Optional Customer deletion references an independent Order-reference query component, not OrderPlacementCoordinator. Access components never call back into orchestration, start independent transactions or recursively invoke application services. The bean graph must remain acyclic even when narrow feature contracts point in both feature directions.

### Proposed Placement Sequence

1. Reject an active ambient Spring transaction before any coordinator work, database calls or template execution; repeat the runtime entry guard for internal invocation. Do not join or suspend it. Reject an unexpectedly bound EntityManager/resource context as well; OSIV remains false.
2. Validate/consolidate immutable command input with overflow-safe arithmetic before locking, without trusted client prices/names.
3. Start one read-write READ COMMITTED `TransactionTemplate` attempt. Proposed propagation REQUIRED is safe only with the explicit nonambient guard. No REQUIRES_NEW suspension or self-invoked `@Transactional` boundary.
4. Acquire a pessimistic-write Customer row by UUID without filtering active state, then each distinct Product individually in one shared order. **Proposed comparator:** lexicographic canonical lowercase UUID text. It is independent of request order and PostgreSQL UUID ordering; every multi-product writer must use the same comparator. Do not rely on bulk ORDER BY for acquisition order.
5. Missing rows produce 404. After acquiring all required rows, validate fresh active state and copy product names/prices. Decide mixed missing/inactive reference precedence under G3; recommendation is missing-resource lookup before eligibility and sorted-ID product priority. Do not preload authoritative entities or assume a later lock refreshes them.
6. Construct Order using trusted immutable facts; reuse domain pricing, snapshots and item ownership. Save root/items in the same attempt, fully materialize immutable result including current customer fields, then return from the callback.
7. Return success only after template execution completes commit. Callback return or flush is not commit evidence. Retain locks until full completion; no external calls or HTTP serialization in the locked transaction.

Customer locking protects eligibility from deactivation and coordinates the first-order/deletion reference decision; it intentionally serializes simultaneous placements for the same customer. Product locks protect active state, name and price until order commit. At the last product acquisition, all protected facts coexist; subsequent mutations cannot alter that captured state. FKs prevent dangling references but cannot protect activity/prices. Domain operations protect quantity/calculation invariants; the database separately protects required fields, numeric bounds, unique item pairs and references. READ COMMITTED alone protects none of the complete cross-row protocol.

### Compatible Mutations and Remaining Races

**Proposed — G2:** existing Product update/deactivate obtains its Product lock before authoritative read/mutation; Customer update/deactivate obtains its Customer lock first. Preserve `@Version`, validation-before-lookup, immutable transaction results, confirmed optimistic-conflict handling and no-op inactive PATCH. Optimistic conflicts can still occur on stale externally held entities; do not require every overlapping application request to conflict after locking. Existing tests may need reviewed coordination changes to avoid a barrier requiring both contenders to finish a read that is now deliberately blocked; never weaken their accepted behavioral assertions.

Keep pre-lock dirty state/autoflush and cached entity reads out of these operations. All Order insert paths participate in Customer-first/Product-order protocol. Raw SQL, future batch updates, triggers and alternate callers must not bypass it. Single-resource writes lock only that resource. Any operation needing both types uses Customer before Products. Existing create/email-uniqueness paths retain constraints; unique-index/FK/other lock cycles can still deadlock. Deterministic ordering reduces relevant cycles, not every database deadlock. Lock waits, pool starvation, transaction context leaks and test-only reversed lock paths need verification.

Retrieval uses original items plus current customer fields, including inactive customers. **Proposed — G5:** an unpaginated list fetches roots/items explicitly and batches distinct current customers through a provider-owned immutable lookup, avoiding per-order/per-item queries. No cross-feature entity graph. Review query counts and empty-list behavior; do not add pagination or promise a globally simultaneous customer snapshot across READ COMMITTED statements. If such a read guarantee is needed, return it for separate decision.

## ADR-0002 Decision Analysis

ADR-0001 business outcomes and ADR-0003 transaction ownership, ambient rejection, OSIV=false and materialization are Accepted. TransactionTemplate, READ COMMITTED and deterministic pessimistic locking are selected baseline direction. The complete protocol remains **Proposed and implementation-unverified**; no concurrency correctness is claimed here.

| Topic | Recommendation (Proposed where new) | Alternatives / consequences / verification |
| --- | --- | --- |
| Propagation, participant wiring, fresh state | REQUIRED template after explicit no-ambient/no-bound-resource guard; provider helpers participate mandatorily; fresh EntityManager per attempt | REQUIRES_NEW suspension hides invalid callers; whole-method annotation/self-invocation does not provide retry completion boundary. Verify proxy and direct coordinator entry fail early, rollback/context cleanup and distinct persistence contexts. |
| Lock path/comparator | Customer first, individual Products by canonical UUID text; fresh unfiltered locked reads | Optimistic versions alone do not protect insert-time multi-resource reads; SERIALIZABLE requires a separately approved protocol. Verify SQL, actual blocking and incompatible multi-product observations. |
| Completion before retry | Observe template completion including commit/rollback; capture per-attempt transaction synchronization outcome as diagnostic evidence; permit retry only after template exits with confirmed full rollback and resource cleanup | `afterCompletion` alone, callback return, flush or broad exception type is insufficient; UNKNOWN or unavailable completion proof must not retry. Verify actual committed/rolled-back state and context release before next attempt. |
| Retry classification | Structured cause/SQLSTATE plus confirmed noncommit: 40P01 and 40001; 55P03 only for a positively identified configured lock-timeout path | No blanket 57014 cancellation, integrity/23505, optimistic conflict or connection-failure retry. Business errors stop. Failure to classify safely means no replay. Verify classifier negatives and real deadlock/timeout, not just mocks. |
| Bounds | At most two extra attempts (three total), proposed 2-second local lock_timeout, 25/50 ms delays and 10-second monotonic retry-scheduling budget | All numbers are recommendations requiring G4 approval; configure locally per transaction, never database-wide. Scheduling budget alone is not a hard HTTP/commit deadline: pool/statement/network/commit timeout interactions remain a blocking configuration review. Do not abandon an uncertain commit and retry. |
| Exhaustion / uncertain commit | Proposed safe 503 on confirmed-rollback transient exhaustion; safe 500 on unknown completion when transport permits, no retry/reconciliation/idempotency feature | Existing Product/Customer policies stay intact. Define exact codes/messages separately under G3/G4. Inject lost confirmation after an actual commit, verify exactly one complete Order and no second attempt; broken transport may give no response. |

Checked failures that invalidate an attempt must leave it via an approved rollback path (for example conversion to a typed runtime failure retaining the cause); do not catch and continue a rollback-only context or retry a statement/savepoint. An exception after a successful commit is not rollback proof. Never retain failed-attempt entities/results, retry under an existing transaction, or sleep/back off while holding locks. No generic retry dependency is proposed.

## Customer DELETE Dependency Assessment

PRD FR-002/BR-003 requires DELETE `/customers/{id}`: unreferenced → physical delete; referenced → deactivate; both 204 without a body; absent customer → 404 under AC-013. Repeated delete after physical removal is therefore not promised 204. Repeated deletion of an existing inactive referenced customer remains 204/no reactivation.

Recommend including this operation as an explicitly approved G6 sub-scope because the same Customer lock protects the first-order race. The historical plan deferred it to Order coordination, not to an already numbered later increment. Planning it here does not authorize replacing the accepted interim 405.

Proposed Customer-owned transaction: lock Customer first, then execute a separate subsequent Order-owned reference-existence query at READ COMMITTED. If placement commits first, the post-wait statement must see the reference and deactivate; if delete commits first, placement sees no Customer/404. Never trust a pre-lock check or a reference check embedded in the original locking statement. Restrictive FK is a backstop. Preserve no-op/version policy on already inactive rows. No foreign repository import, cascade delete, Flyway clean or local-volume reset.

If deferred, the core increment can implement placement plus mutation coordination, but Customer deletion/AC-003/004 and its real race tests remain unfinished. Human review must explicitly accept that scope limitation; no full MVP or complete protocol claim.

## Proposed Implementation Files

Names below are reviewable proposals under existing `src/main/java/com/example/agenticorderservice/` and matching `src/test/java/`; do not create placeholder classes. Prefer a few concrete components, not one interface per operation.

| Files | Proposed change |
| --- | --- |
| `order/application/OrderPlacementCoordinator.java`, `PlaceOrderCommand.java`, `OrderResult.java`, `OrderQueryService.java` | Validated commands/results, explicit attempt orchestration and reads. Keep attempt helper internal unless separation demonstrably simplifies tests. |
| `order/application/AmbientTransactionGuard.java`, typed Order validation/eligibility/not-found/technical failures as needed | Narrow runtime entry guard and HTTP-independent signals; exact exception count/names follow approved catalog. |
| `configuration/OrderTransactionConfiguration.java` | Template/isolation and reviewed timing configuration; no custom DataSource or framework. |
| `customer/application/CustomerOrderAccess.java`, `product/application/ProductOrderAccess.java` | Narrow immutable current/locked fact access, mandatory transaction participation; provider-owned repository use only. |
| `order/persistence/OrderRepository.java`, existing Product/Customer repositories | Own-feature lock/fetch operations; explicit Order list fetch; no new independent OrderItem repository. |
| Existing Product/Customer application services | Compatible pre-read locks for affected writes, retaining accepted versions and endpoint behavior. |
| `order/api/OrderController.java`, `CreateOrderRequest.java`, `OrderItemRequest.java`, `OrderResponse.java` | Three endpoints and nested immutable DTOs; nested response carriers may be nested records, not gratuitous files. |
| Shared `StrictJsonRequestDecoder`, `ApiRequestInterceptor`, `ApiError`, `ApiExceptionHandler`; `ApiWebConfiguration` only if required | Extend only supported Order DTO/handler paths and approved error taxonomy/precedence; preserve Product/Customer behavior. Existing converter/missing-body advice delegates supports to decoder and may need no edit. |
| `application.yml` / profile configuration only if approved settings require it | OSIV/validate/no SQL initialization retained; finite timing scopes selected under G4. Never real credentials or .env changes. |
| Order application/query/coordinator `*Test`; `order/api/OrderApiTest.java`; existing shared API contract/decoder tests | Unit and actual MVC matrix; preserve earlier regression assertions. |
| `order/api/OrderApiIT.java`, `order/application/OrderPlacementConcurrencyIT.java`, `OrderPlacementFailureIT.java`; existing `order/persistence/OrderPersistenceIT.java` | Real API, generated SQL, persistence/atomicity/concurrency/fault evidence using existing PostgreSQL infrastructure. |
| Existing Product/Customer concurrency tests and fast smoke/MVC supports as actually affected | Register/mock new collaborators only where required; preserve all earlier tests and accepted outcomes. |
| Conditional G6: Customer service/controller/repository; `order/application/OrderReferenceQuery.java`; Customer deletion unit/MVC/PostgreSQL tests | DELETE endpoint, own-feature delete and independent reference query, both first-order race schedules. |
| Existing docs and this playbook | Approved decisions first, execution evidence later; no ADR renumbering/status promotion. |

No POM, Makefile, CI, source migration or database operation is proposed in this planning task. Existing V1/V2 apply only under later authorized disposable verification. Any schema/dependency change discovered later requires a separate reviewed scope amendment and persistent-target safety checks where applicable.

## Complete Test Matrix

Every runtime row below is **Pending**. Tests must assert business outcomes, exact response/envelope where applicable, final committed database state and absence of unwanted calls/writes. Existing passed counts do not satisfy these new gates.

| ID / level | Required scenarios and evidence |
| --- | --- |
| U1 Unit | Direct application missing/null customer/items/item/product/quantity; empty items; zero/negative/over-limit/overflow sums; duplicate UUID normalization; reject before provider/repository access. |
| U2 Unit | Missing vs inactive resources; complete protected fact collection; correct customer/product calls and deterministic distinct lock order; no domain creation/save on rejection; approved mixed-failure priority. |
| U3 Unit/domain | Reuse exact independent PRD 99.99/100.00/100.05/150 cases; zero price, maximum line/larger subtotal, duplicate 2+3 and 6000+4000/4001; original name/price snapshots. Extend only real orchestration gaps, not duplicate pricing implementation. |
| U4 Unit/infrastructure | Ambient and bound-context guards, complete attempt count, retry eligibility/negatives, delay/budget exhaustion, checked-failure propagation, no reuse of failed results, no retry for unknown/postcommit outcome. Mocks prove orchestration only. |
| U5 Unit | Creation/get/list immutable result completeness, empty list, absent order, customer current inactive details, mapping no item ID/version/tax. |
| H1 MVC | POST 201/Location and exact complete shape; individual/list 200 with identical resource fields; [] empty; unsupported Order PUT/PATCH/DELETE 405/Allow; no accidental extra endpoint. |
| H2 MVC | Recursive unknown/recognized forbidden fields at root/items, client prices/totals/IDs, nested duplicates/trailing/syntax/null-root/array-root/absent-body distinctions, invalid UTF-8; no application calls on invalid transport. |
| H3 MVC | Items wrong type/null/empty/null element, missing/null/wrong scalar IDs/quantities, fraction/string/boolean/exponent/integral-decimal per approved policy, large values/overflow and consolidated 10000/10001. |
| H4 MVC | Canonical path/body UUID case/shape, syntactically valid absent vs malformed, header/media/path/body precedence, deterministic multi-error/tie ordering; exact codes/messages under G3; preserve incompatible Accept safe JSON. |
| H5 MVC | Customer/Product missing 404, inactive 409, Order missing 404; expected 500/503/optimistic errors mapped outside transaction, no blind replay advice. Actual transaction timing requires P tests. |
| P1 PostgreSQL | Real POST persists root/all owned items with generated IDs, restrictive references and exact stored amounts; duplicates one item; Hibernate validation and unchanged V1/V2 history/checksums. |
| P2 PostgreSQL | Rename/reprice/deactivate Product and change/deactivate Customer after placement; GET/list preserve original facts and show current customer, including inactive; response serialization after context closure causes no SQL/lazy access. |
| P3 PostgreSQL | Inject failure after root/item flush and prove complete rollback/no orphans with fresh independent transactions; business rejection leaves no rows. Commit succeeds before HTTP 201, not merely flush/callback return. |
| P4 PostgreSQL | Same/different-customer simultaneous placements sharing Products; deterministic reversed input order; no false overlap business rejection, no duplicate item rows or incompatible snapshots; independent connections/contexts. |
| P5 PostgreSQL | Product rename/reprice/deactivation vs placement and Customer update/deactivation vs placement, each winner order; observe real waits/locks and final state, preserve irreversible activity and accepted optimistic/no-op tests. |
| P6 PostgreSQL | Multi-product placement against a test-only controlled atomic multi-product writer using the same order, forced intermediate blocking, all facts coexist in a valid serial order; inspect actual JPA locking SQL and isolation, no bulk ORDER BY assumption. |
| P7 PostgreSQL | Controlled deadlock (opposite order only in test fixture), local lock timeout and classifier-negative cancellations/integrity errors; real full rollback before bounded fresh-context retry; success once or approved 503 exhaustion. Do not depend on deadlock victim selection. |
| P8 PostgreSQL/transport | Lost confirmation after real database commit: complete order may exist, no second attempt and safe approved unknown-outcome response where possible. Distinguish confirmed rollback and HTTP disconnect; no guaranteed response on broken transport. |
| P9 PostgreSQL | Explicit single/list fetch plans; batch current-customer reads; query counts avoid per-order/per-item N+1, no unexpected cross-feature managed entities/cache state; no invented global snapshot guarantee. |
| P10 Conditional G6 | Unreferenced delete/204/absent404, referenced deactivate/204/no body/retrievable history, repeated inactive no-op; delete vs first order both schedules, post-wait reference statement visibility, no dangling refs. If excluded, mark Deferred, not Passed. |
| A Architecture | Controller→application only, no foreign persistence imports, no API/application dependencies from domain, no managed entities in REST, no recursive service/circular constructor dependencies. Tool choice stays Open; do not add ArchUnit. |

Use the existing `PostgreSqlTestConfiguration`, `@ServiceConnection`, central `config/postgres.properties`, integration-test profile, Failsafe `*IT`, actual random-port support and isolated fixtures. Never use a persistent Compose database for races or fault injection. Synchronize holder/contender stages with barriers/latches, observe bounded PostgreSQL waits, then commit/rollback deliberately. Timeouts bound test hangs; arbitrary sleeps and a common enclosing rollback transaction are not proof. Join workers and clean only test-owned resources; no global schema resets or Flyway clean/repair/baseline.

## Verification and Evidence Requirements

After separate approval, record baseline branch/HEAD/status, exact authorized scope and configuration. Sequence: approve G1–G6 → unit-safe command/contracts → complete transaction/lock and compatible mutation paths → REST/read integration → PostgreSQL concurrency/failure matrix → full regression and review. No successful unprotected placement path should be treated as a completed intermediate feature.

Proposed later commands (not executed now): command-local Java 21 `./mvnw -B -ntp test`, then Docker-backed `./mvnw -B -ntp clean verify`; inspect Surefire/Failsafe counts, failures/errors/skips, generated SQL and JaCoCo HTML/XML. Preserve existing dependencies, wrapper, CI, Sonar and all tests; use actual analysis evidence when separately authorized, not an assumed clean result.

Current PIT `SCOPE=order` targets only `order.domain.*`; it will not mutate new `order.application` or `order.api` code. Use the unchanged Makefile's `SCOPE=all` for new application/API mutation evidence with broad fast tests. Individual `product/customer/shared` scopes remain useful for affected code. A future Order-selector expansion needs separate Makefile approval. PostgreSQL *IT is excluded; PIT cannot replace P1–P10. No numeric mutation target or artificial assertion is selected.

The latest recorded all-scope run is `PIT-all-20261015T180615Z-W3xosD` at `8c780fd581639f6fb5f14ba24ed5519f1d42f8bd` plus recorded Product test edits: 314 green tests, 346 mutants (274 KILLED, 53 SURVIVED, 18 NO_COVERAGE, one TIMED_OUT). It predates Customer/Shared hardening. Latest Shared evidence includes current reviewed test changes (321 green tests) but is not a new all-scope baseline. Recommend a separately authorized final `make mutation-test SCOPE=all` before measuring Increment 5 deltas; this evidence refresh is not an architecture approval gate or authorization to run now.

Preserve unique external evidence directories, commands, environment, frozen source/test revision/checksums, logs/exit codes, reports, mutation identities/transitions and unknown-outcome fault controls. Distinguish PIT detected/generated from strict KILLED/generated. Report all unexecuted/blocked/deferred gates honestly. Finish implementation with complete staged/unstaged/new-file diff, actual test/coverage/SQL evidence, restored test infrastructure and explicit human review; no commit without authorization.

## Open Questions and Human Decision Gates

| Gate | Explicit decision required before affected implementation |
| --- | --- |
| G1 Protocol and boundary | Approve the scoped ADR-0002 enforcement proposal, propagation/participant checks, completion/rollback proof and fresh-context design. Keep complete ADR status Proposed unless separately explicitly accepted. |
| G2 Participating writes | Approve pre-read pessimistic paths for Product/Customer update/deactivate while preserving accepted @Version/API behavior and reviewed concurrency assertions. |
| G3 Order API | Approve precise Order codes/messages/recognized forbidden fields, integer token spelling (including 1.0/exponents), nested required/type/tie priority and mixed resource-error precedence. PRD statuses/endpoints stay Accepted. |
| G4 Failure/timing | Approve actual max attempts, local lock timeout, backoff/budget, pool/statement/network/commit interaction, exact safe retry SQLSTATE/proven-rollback classifier and unknown/exhaustion messages; no blind replay. |
| G5 Retrieval | Approve immutable current-customer batch contract and unpaginated list fetch/query evidence strategy; no pagination or new global consistency promise. |
| G6 Customer DELETE | Explicitly include coordinated DELETE/204 and P10 now, or defer it with interim 405 and incomplete MVP/full-protocol claims. Recommendation: include after review. |

Additional limits: no unselected dependency/schema change may be inferred; persistent local migration/recovery and production execution remain separate. Architecture review, implementation authorization, runtime verification, human acceptance and Git authorization are distinct gates.

## Draft Future Implementation Instructions — Not Executable

The following draft is for review only; replace/finalize it after G1–G6 decisions are recorded, preserving this planning history. Do not execute it based on documentation approval.

```text
After explicit human implementation authorization, implement only the finalized Increment 5
scope in this playbook at the newly approved source revision. Read AGENTS.md and the
linked authoritative requirements, architecture, ADRs and guidelines first.

Preserve Increment 3 and Increment 4 acceptance, D1–D10, Product/Customer @Version,
strict API contracts, V1/V2, dependencies, Makefile, CI and database data.
Implement the three PRD Order endpoints, provider-owned narrow contracts, approved
transaction/locking/retry design, current-customer retrieval and compatible mutation paths.
Implement Customer DELETE only if G6 explicitly includes it.

Do not choose unresolved G1–G6 details yourself. Stop for a genuine conflict or unsupported
requirement. No automatic replay after uncertain commit, foreign persistence imports,
managed entities in REST, new framework or autonomous persistent migration is authorized.

Run the approved unit, actual MVC and PostgreSQL matrices and applicable existing gates.
Capture actual outcomes, SQL, rollback/commit evidence, coverage, mutation limitations and
complete Git diff. Keep future/unavailable gates Pending, not Passed. Preserve all evidence.
Mark execution Executed – Pending Human Review and wait for explicit human acceptance.
Do not commit, push, merge, deploy or begin another increment.
```

## Result

Planning documentation prepared; no implementation, migration, build, tests, PIT, Sonar analysis or Docker execution performed. G1–G6 remain Pending Human Architecture Review. Increment 4 acceptance is confirmed from its authoritative record and preserved history, not re-reviewed here.

## Lessons Learned

No new implementation lessons are claimed. Planning identified that existing optimistic versions do not protect Order eligibility reads, strict input support is explicitly DTO/feature-scoped, and the Order PIT selector currently excludes future Order application/API classes.

## Final Human Architecture Decisions — 2026-10-16

The human explicitly approved G1–G6 below. This decision record preserves the supplied approval text; the added status labels identify each accepted gate. Architecture approval grants no implementation authorization. The PRD, [transaction conventions](../../02-architecture/transaction-locking.md) and [central API catalog](../../02-architecture/api-error-codes.md) remain authoritative for their respective rules.

### G1 — Transaction Architecture

**Status: Human Accepted.**

Approved:

- Order-owned `OrderPlacementCoordinator`.
- Explicit `TransactionTemplate`.
- PostgreSQL `READ COMMITTED`.
- One independent, read-write transaction per placement attempt.
- Reject unexpected ambient transactions before processing.
- Require fresh persistence state for each attempt.
- Do not suspend an existing transaction using `REQUIRES_NEW`.
- Observe transaction completion, including commit, before returning success.
- Do not treat callback return or flush as successful commit.
- Preserve existing application transaction conventions.

The exact ambient-resource guard and transaction participation mechanisms require runtime verification.

### G2 — Locking and Participating Mutations

**Status: Human Accepted.**

Approved:

- Acquire the Customer lock first.
- Acquire each distinct Product lock individually.
- Sort Product identifiers using canonical lowercase UUID text.
- Use pessimistic row locks for authoritative eligibility and price reads.
- Product PUT/PATCH and Customer PUT/PATCH participate in compatible locking.
- Preserve Product/Customer `@Version` mappings.
- Preserve confirmed optimistic-conflict behavior.
- Preserve irreversible deactivation.
- Preserve repeated inactive PATCH as a no-op without UPDATE/version increment.
- Do not preload stale entity state before authoritative locked reads.
- Verify actual PostgreSQL blocking, lock order and committed outcomes.

Deterministic ordering reduces deadlocks but does not guarantee their absence.

### G3 — Order REST Contract

**Status: Human Accepted.**

Approved business endpoints:

- `POST /orders` — 201, complete Order response, Location header.
- `GET /orders` — 200, complete Order array.
- `GET /orders/{id}` — 200, complete Order, 404 when absent.

Preserve the PRD response contract:

- Original product names and prices as stored snapshots.
- Current Customer information.
- Original stored monetary results.
- No JPA entities or internal item IDs/versions in responses.

Reuse existing strict JSON behavior and safe error envelope.

Extend parsing to nested Order items:

- Reject unknown and recognized forbidden properties recursively.
- Reject duplicate JSON keys, trailing data and malformed roots.
- Preserve header/path/body precedence.
- Validate canonical UUIDs.
- Apply deterministic field ordering: customerId before items, then productId before quantity, item indices ascending.
- Use lexicographic ordering for unknown/forbidden property paths.
- Require quantity to be a JSON integer token.
- Reject `1.0`, exponent notation, strings and booleans as quantities.
- Validate positive quantities and the existing consolidated maximum of 10,000.
- Apply Customer-first, then sorted-Product priority for multiple missing/inactive resource findings.

Approved new Order error codes and fixed messages:

| Code | HTTP | Message |
|---|---|---|
| INVALID_ORDER_ITEMS | 400 | The order must contain at least one item. |
| INVALID_ORDER_QUANTITY | 400 | Order item quantities must be positive integers and the consolidated quantity must not exceed 10,000. |
| CUSTOMER_INACTIVE | 409 | The customer is inactive. |
| PRODUCT_INACTIVE | 409 | The product is inactive. |
| ORDER_NOT_FOUND | 404 | The order was not found. |

Reuse existing safe messages for:

- INVALID_RESOURCE_ID
- CUSTOMER_NOT_FOUND
- PRODUCT_NOT_FOUND
- MALFORMED_JSON
- UNKNOWN_FIELD
- UNSUPPORTED_FIELD
- INVALID_FIELD_TYPE
- REQUIRED_FIELD_MISSING
- METHOD_NOT_ALLOWED
- Existing technical 500/503 codes

Do not invent additional fields or error codes.

Document recognized forbidden Order properties and structural validation behavior consistently with existing strict input conventions.

### G4 — Retry, Failure and Timing Strategy

**Status: Human Accepted.**

Approved initial configuration:

- Maximum three placement attempts, including the first.
- Local per-transaction PostgreSQL lock timeout: 2 seconds.
- Retry delays: 25 ms and 50 ms.
- Monotonic retry-scheduling budget: 10 seconds.
- These are initial project settings, not universal production recommendations.

Retry is permitted only when:

1. The failed transaction has definitively completed with full rollback.
2. Resources and attempt-local persistence state are released.
3. The failure is classified as an approved transient condition.
4. Attempt and scheduling limits have not been exceeded.

Approved classifications:

- SQLSTATE `40P01`: deadlock.
- SQLSTATE `40001`: serialization failure.
- SQLSTATE `55P03`: only when positively attributable to the configured lock-timeout mechanism.

No automatic retry for:

- Business validation failures.
- Missing or inactive resources.
- Optimistic-lock conflicts.
- Unique-constraint failures.
- Generic SQL exceptions.
- Unknown transaction outcomes.
- Failures after a potentially successful commit.

Confirmed rollback with transient retry exhaustion: safe HTTP 503.

Unknown commit result: safe HTTP 500 where a response remains possible, without retry or a false rollback claim.

The existing fixed safe 500/503 error catalog should be reused unless the current accepted contract explicitly requires a separate Order-specific message.

Explicitly document that the 10-second scheduling budget is not a hard end-to-end HTTP deadline. Pool, statement, network and commit timeout interactions require implementation review and runtime verification.

### G5 — Order Retrieval

**Status: Human Accepted.**

Approved:

- Dedicated `OrderQueryService`.
- Application-owned read-only transactions.
- Explicit loading of Orders with their persisted items.
- Original stored product names, prices and amounts remain unchanged in responses.
- Current Customer details are obtained through a narrow Customer-owned access contract.
- Batch distinct Customer IDs for list retrieval to avoid N+1 queries.
- Build immutable application results inside transactions.
- Do not expose managed entities or trigger lazy queries during HTTP serialization.
- No new pagination, sorting guarantees or global multi-query snapshot guarantee.

Use real PostgreSQL evidence for query behavior and absence of N+1 loading.

### G6 — Customer DELETE

**Status: Human Accepted.**

Approved inclusion in Increment 5.

Implement:

`DELETE /customers/{id}`

Accepted behavior:

- Existing unreferenced Customer: physical deletion, 204, no body.
- Existing referenced Customer: deactivation, 204, no body.
- Already inactive referenced Customer: 204, no reactivation.
- Absent Customer: 404 using the existing Customer error contract.

The former Increment 4 interim 405 OPERATION_NOT_AVAILABLE behavior must be replaced only during the subsequently authorized Increment 5 implementation.

Approved concurrency approach:

- Acquire the Customer lock first.
- After lock acquisition, execute a separate Order-reference existence query.
- Use READ COMMITTED.
- Do not perform check-then-delete without the shared locking protocol.
- Preserve restrictive foreign keys.
- Use a narrow Order-owned reference query contract rather than importing Order repositories into Customer.
- Cover both winner orders of Customer DELETE versus first Order placement with deterministic PostgreSQL integration tests.

Do not silently modify Product DELETE or introduce other deletion behavior.


## Current Approved Scope and Matrix Update

**Architecture Decisions Accepted — Implementation Pending Human Authorization.** G1–G6 are Resolved (Human Accepted), not implementation-complete. The original planning and test matrices above remain history; the following changes define their current interpretation.

| Business endpoint | Approved response | Current implementation state |
| --- | --- | --- |
| POST `/orders` | 201, complete Order response and Location | Not implemented |
| GET `/orders` | 200, complete Order array / [] | Not implemented |
| GET `/orders/{id}` | 200, complete Order or existing safe 404 using ORDER_NOT_FOUND | Not implemented |
| DELETE `/customers/{id}` | 204/no body for existing Customer: physical delete when unreferenced, otherwise deactivate; absent → CUSTOMER_NOT_FOUND/404 | Included in Increment 5; interim 405 remains implemented until separately authorized replacement |

The other ten accepted Product/Customer endpoints remain intact; PUT/PATCH lock acquisition becomes compatible with placement under G2, without removing @Version or changing irreversible/no-op deactivation. No Product DELETE or Order mutation endpoints are added.

Current ownership: OrderPlacementCoordinator owns explicit independent read-write READ COMMITTED attempts; OrderQueryService owns read-only retrieval; provider-owned Customer/Product access returns immutable facts without foreign persistence imports. Customer DELETE owns its transaction and calls an independent Order-owned reference contract after locking Customer in a separate subsequent statement. The constructor/call graph must remain acyclic.

Customer-first then sorted-Product priority applies across missing and inactive resource findings; do not implement the historical suggestion that every missing Product outranks an inactive Customer. Preserve consistent protected reads and deterministic Product ordering. Exact ambient-resource guard, transaction participation/completion observation, fresh state and timeout interactions need implementation review and runtime verification. This is not a reopened G1–G6 policy decision or proof of correctness.

| Historical matrix / files | Current approved interpretation |
| --- | --- |
| Original Order endpoint/DTO/result rows; U1–U5, H1–H5, P1–P9, A | Mandatory within accepted G1–G5. Replace proposed errors/tie/token/resource priority with final G3 and the authoritative catalog; exact fixed 500/503 templates are reused. |
| Conditional Customer DELETE files, OrderReferenceQuery and P10 | Now mandatory under G6: Customer controller/service/repository and own-feature delete, independent Order reference query; unit and actual MVC/HTTP coverage for 204/no body and 404, restrictive FK/current order retrieval, already inactive no-op, both first-order race schedules. Remove no accepted Product/Customer regression assertion. |
| Proposed retry bounds/classifiers, U4/P7/P8 | Use accepted three total attempts, transaction-local lock_timeout 2s, delays 25/50 ms, monotonic scheduling budget 10s, confirmed rollback/resource release and approved SQLSTATE paths. Verify unknown/postcommit outcome never replays and no budget is treated as a hard HTTP deadline. |
| Single/list retrieval, U5/P2/P9 | Mandatory OrderQueryService/read-only materialization, explicit item loading and Customer-owned distinct-ID batch access; original amounts/current Customer, no serialization SQL or N+1. No pagination/sorting/global-snapshot guarantee. |
| Shared API/configuration/test-support rows | Extend only Order DTO/handler support and accepted exact error envelopes. Test Customer DELETE replacing interim 405 only when implemented, accurate Allow on unsupported methods, nested precedence and rejection before use-case invocation. |
| Domain/storage/dependency/quality boundaries | Reuse accepted Order/Money/owned items, V1/V2 and constraints; no new schema/dependency/Makefile/CI change selected. Existing application/API tests, PostgreSQL *IT, JaCoCo/PIT/Sonar responsibilities remain. |

No row has been executed by this decision-record update. Runtime gates remain Pending, including actual SQL/blocking, serially valid multi-product facts, commit/rollback evidence, query counts, body/error pipeline, first-order delete races, retry limits and lost-confirmation fault injection. Production/persistent-local migration gates remain separate. All four endpoint scopes require explicit implementation authorization before code.

## Current Verified Project-Wide PIT Baseline

This replaces the old all-scope baseline as the current reference; the earlier run in the historical planning record is preserved as history.

```text
/Users/rookie/Documents/Development/Projekte/agentic-order-service-mutation-evidence/PIT-all-20261016T075327Z-qMGnCc
```

Recorded revision `81f96e262fdf0a67c96ef7861856e95b9a185d55`, branch develop, clean recorded working tree. Baseline: 321 fast-test executions, zero failures/errors/skips. Baseline Maven, XML validation and PIT exit codes are all 0; both Maven logs report BUILD SUCCESS. Selectors cover project production packages with project-wide fast *Test eligibility.

| Generated | KILLED (XML) | SURVIVED | NO_COVERAGE | TIMED_OUT | detected=true |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 346 | 288 | 40 | 17 | 1 | 289 |

PIT's console reports 289 detections (rounded 84%), 88% test strength and mutated-class-only line coverage 430/455 (rounded 95%). Exact XML detected/generated is 289/346 = 83.53%; strict KILLED/generated is 288/346 = 83.24%. The timeout is detected but is not an assertion-based kill. Test strength is a different covered-mutant metric, not KILLED/generated or a whole-application guarantee. These are inspected existing results, not newly executed tests or a new score threshold.

PostgreSQL *IT mutation verification remains excluded. No final all-scope refresh is currently missing for this source revision; new implementation changes need a new approved baseline/comparison. The unchanged Makefile order scope still mutates domain only; use all for future Order application/API mutation evidence, with meaningful survivor review rather than artificial assertions.

## Historical Revised Future Implementation Prompt — Non-executable

**Historical only:** superseded by the [single current implementation prompt](#current-implementation-prompt--increment-5). The original quoted instructions below remain unchanged.

This supersedes the original draft only as a future instruction. Human architecture acceptance is recorded; no human coding authorization has been granted. The actual ambient/participation/completion mechanisms and pool/statement/network/commit interactions must be reviewed and verified, without claiming that accepted scheduling bounds impose a hard response deadline.

```text
Do not execute this prompt until the human explicitly authorizes Increment 5 implementation.

After that authorization, confirm the approved source revision and Git state, read AGENTS.md,
PRD, accepted G1–G6, authoritative architecture/catalog/ADRs and coding/testing guidance.
Preserve Increment 3/4 acceptance and immutable history; ADR-0002 and ADR-0006 remain Proposed.

Implement exactly four business endpoints: POST /orders, GET /orders, GET /orders/{id},
DELETE /customers/{id}, with the accepted response fields/statuses and five new fixed
Order error templates. Reuse all applicable existing error templates; no new error codes.
Extend strict nested JSON, canonical UUIDs and integer tokens with accepted deterministic
precedence. Customer-first then sorted-Product priority covers missing/inactive findings.

Use OrderPlacementCoordinator, explicit independent read-write READ COMMITTED attempts,
ambient rejection without suspension, fresh persistence state, Customer-first individual
Product locks ordered by canonical lowercase UUID text, and compatible Product/Customer
PUT/PATCH locking. Preserve @Version, confirmed conflicts, irreversible activity and no-op PATCH.
Return success only after commit. Never retry an uncertain or potentially committed outcome.
Retry only confirmed full rollback plus released resources plus approved transient SQLSTATE,
within three total attempts, local 2s lock timeout, 25/50ms delays and 10s scheduling budget.
Implement approved current-customer batch retrieval and immutable read-only materialization.
Customer DELETE locks first and then queries references separately, delete or deactivate/204.

Reuse existing domain calculations, mappings, V1/V2 and PostgreSQL infrastructure; add no
unapproved schema, framework, dependency, Makefile or CI change. No autonomous persistent
or production migration, credential/volume deletion or broad cleanup is authorized.
Run the complete U/H/P/A matrix, including mandatory DELETE unit/MVC and P10 race tests,
applicable existing regression/coverage gates and separately approved PIT verification.
Capture actual SQL/blocking/rollback/commit/query evidence, exact counts, reports and full diff.
Report future/unavailable gates Pending; mark execution Executed – Pending Human Review.
Do not commit, push, merge or deploy. Wait for explicit human implementation acceptance.
```

## Decision Recording Result — 2026-10-16

G1–G6 architecture gates are **Human Accepted**. Four-endpoint scope, technical direction, accepted fixed errors and current test/file matrix are recorded. Original planning proposals, original draft prompt and earlier result are preserved above. Implementation authorization, all Increment 5 runtime verification and human implementation acceptance remain Pending. No code, tests, migration, build, PIT execution, Sonar analysis or database/container operation occurred in this documentation task.

## Historical User-Supplied Implementation Draft — Non-executable

**Status:** Historical user-supplied draft, preserved verbatim — superseded and not authorized for execution. Use only the [single current implementation prompt](#current-implementation-prompt--increment-5) after explicit authorization.

The accepted G1–G6 decisions and PRD remain authoritative. This quoted draft does not supersede them: its discount threshold `> EUR 100.00` conflicts with the accepted `>= EUR 100.00`, and its mutation kill-rate threshold of 83% is not an accepted quality gate. Insertion alone approves neither change nor implementation. Existing accepted timing and failure requirements remain in force even where this draft abbreviates them.

```markdown
## Executable Implementation Prompt

### Role and Objective
You are acting as a senior Java software architect executing **Increment 5 — Order Placement & Concurrency** in the `agentic-order-service` repository.
Architecture Decisions G1–G6 have been explicitly **Human Accepted** on 2026-10-16. Complete ADR-0002 and ADR-0006 remain Proposed.
Your task is to implement the approved application code, API endpoints, transaction coordination, locking protocol, and mandatory tests.
Follow AGENTS.md, all applicable architectural guidelines, and the exact decisions recorded in this playbook.

---

### 1. Read and Review Authoritative Guidance
Before writing any code, inspect and adhere to:
- `AGENTS.md`
- `docs/01-requirements/product-requirements.md` (FR-002, FR-003, FR-004, FR-005, FR-006)
- `docs/02-architecture/system-architecture.md`
- `docs/02-architecture/domain-model.md`
- `docs/02-architecture/rest-api-design.md`
- `docs/02-architecture/api-error-codes.md`
- `docs/02-architecture/transaction-locking.md`
- `docs/03-decisions/ADR-0001` (Accepted), `ADR-0002` (Proposed), `ADR-0003` (Accepted), `ADR-0006` (Proposed)
- `docs/04-agentic-coding/coding-guidelines.md`
- `docs/04-agentic-coding/java-spring-guidelines.md`
- `docs/04-agentic-coding/persistence-guidelines.md`
- `docs/05-testing/testing-strategy.md`

Do not reopen accepted G1–G6 decisions or introduce unapproved dependencies, database schema changes, or pom.xml modifications.

---

### 2. Approved Scope & Endpoint Matrix
Implement exactly these four REST operations:

| Method | Endpoint | Success Status | Behavior / Key Obligations |
| :--- | :--- | :--- | :--- |
| **POST** | `/orders` | `201 Created` | Create complete Order aggregate; `Location: /orders/{id}`; transactional locking. |
| **GET** | `/orders/{id}` | `200 OK` | Retrieve canonical Order by ID; `404 Not Found` if absent. |
| **GET** | `/orders` | `200 OK` | List all Orders; empty array `[]` when no orders exist; batch-fetch current customer details. |
| **DELETE** | `/customers/{id}` | `204 No Content` | Delete unreferenced Customer or deactivate referenced Customer; `404 Not Found` if absent (Gate G6). |

#### Explicit Exclusions:
- `PUT`, `PATCH`, or `DELETE` on `/orders` (must return `405 Method Not Allowed` with valid `Allow` header).
- Product deletion or reactivation of inactive records.
- Idempotency keys or automatic reconciliation frameworks.
- New dependencies, Flyway migrations, or Maven profile edits.

---

### 3. Architecture & Transaction Protocol Rules

#### A. Pre-Lock Validation & Input Normalization
- Perform complete syntax, JSON structure, duplicate key checking, and UUID format validation at the API/DTO layer before entering application transactions.
- Consolidate duplicate product items and validate quantity limits (max 10,000) using overflow-safe arithmetic prior to locking.
- Never accept client-supplied product names, prices, subtotals, or tax breakdown fields.

#### B. Transaction & Locking Protocol (Gate G1 & G2)
1. **Ambient Transaction Guard:** Reject execution if an active ambient transaction or bound resource context is present (`AmbientTransactionGuard`).
2. **Single Transaction Attempt:** Execute under a single `TransactionTemplate` attempt (propagation `REQUIRED`).
3. **Pessimistic Acquisition Order:**
   - First: Acquire pessimistic-write lock on the target `Customer` row via `CustomerOrderAccess`.
   - Second: Acquire pessimistic-write locks on distinct `Product` rows via `ProductOrderAccess` in **ascending lexicographical order of canonical UUID strings**.
4. **State Verification:** After acquiring all locks, verify active status for Customer and all Products. Inactive state triggers `409 Conflict`. Missing entity triggers `404 Not Found`.
5. **Aggregate Commit:** Construct `Order` using trusted immutable snapshots (exact EUR calculations, 10% discount threshold > EUR 100.00 with `HALF_UP`), persist root and items, and materialize immutable result record before transaction commit.

#### C. Bounded Bounded Retry Strategy (Gate G4)
- Allow at most two retries (3 total attempts) exclusively for transient database failures:
  - Deadlock (`SQLSTATE 40P01`)
  - Serialization failure (`SQLSTATE 40001`)
  - Identified lock timeout (`SQLSTATE 55P03`)
- Require full rollback and resource context cleanup before initiating a retry.
- **Never retry** on business validation errors or uncertain/unconfirmed commit outcomes. Return `500 Internal Server Error` or `503 Service Unavailable` with safe error envelopes.

#### D. Customer Deletion Race Protocol (Gate G6)
- `DELETE /customers/{id}` acquires Customer lock first, then executes an independent `OrderReferenceQuery` at `READ COMMITTED`.
- If referenced by any order: mark `active = false` and return `204 No Content`.
- If unreferenced: physically delete row and return `204 No Content`.
- If customer absent: return `404 Not Found`.

#### E. Query Performance (Gate G5)
- `GET /orders` must materialize current customer details using provider-owned batch lookups to prevent N+1 queries.
- Preserve original historical line-item snapshots (`productName`, `unitPrice`, `lineTotal`).

---

### 4. Mandatory Three-Level Test Matrix

#### Level 1 — Unit Tests (`*Test.java`)
- `U1`: Direct command validation: null/empty items, negative/zero/overflow quantities, duplicate UUID consolidation.
- `U2`: Missing vs. inactive resource checks, lock acquisition order, mixed error precedence.
- `U3`: PRD pricing calculations (EUR 99.99, 100.00, 100.05, 150.00), original snapshot preservation.
- `U4`: Guard checks, transaction attempt counting, retry eligibility filters, failure propagation.
- `U5`: Result record mapping, absence of internal entity/version/tax leaks.

#### Level 2 — HTTP/MVC Tests (`*ApiTest.java`)
- `H1`: `POST /orders` 201 Created & Location header; `GET /orders` array/empty JSON; 405 Allow headers for unsupported methods.
- `H2`: Strict JSON parsing: rejection of unknown fields, read-only client fields, invalid types, invalid UTF-8.
- `H3`: Scalar field errors, invalid/consolidated quantity bounds (10,000 / 10,001).
- `H4`: Canonical UUID path/body validation, precedence rules, exact error envelope mappings.
- `H5`: Missing/inactive 404/409 responses, mapping of 500/503 tech errors without stack traces.

#### Level 3 — PostgreSQL Integration Tests (`*IT.java`)
- `P1`: Real database persistence of root/items, foreign key constraints, exact numeric column storage.
- `P2`: Post-placement Product/Customer edits; verification that historical order snapshots remain intact.
- `P3`: Simulated transaction rollback: verify complete absence of orphan rows upon failure.
- `P4`: Concurrent order placements sharing products with opposite item request order (verify deterministic lock order).
- `P5`: Concurrent placement vs. Customer/Product update/deactivation races.
- `P6`: Controlled multi-product writer race using `PostgreSqlTestConfiguration`.
- `P7`: Controlled deadlock and lock timeout retries; verification of fresh context on retry.
- `P8`: Lost confirmation simulation after commit (no duplicate order creation).
- `P9`: Execution plan and N+1 query count verification for `GET /orders`.
- `P10`: Customer DELETE vs. first-order placement race schedules (both execution order wins).

---

### 5. Execution Rules & Verification Requirements
1. Implement code strictly according to the file matrix (`OrderPlacementCoordinator`, `OrderQueryService`, `OrderController`, `CustomerOrderAccess`, `ProductOrderAccess`, `OrderReferenceQuery`, etc.).
2. Execute full build, Unit, MVC, and `*IT` integration test suites.
3. Execute PIT mutation testing via `make mutation-test SCOPE=all` or Maven PIT plugin. Ensure strict mutation kill rate remains ≥83%.
4. Run SonarQube quality checks to ensure zero code smells, security hotspots, or coverage regressions.
5. Do not create Git commits or push code until human review is requested.

```

Execution note — 2026-10-16: the human explicitly authorized this instruction through the “Execute Increment 5 — Order Placement & Concurrency” request. The instruction below, including its original pre-authorization status, is preserved verbatim. The current execution/review status is recorded at the top and in the appended execution record.

## Current Implementation Prompt — Increment 5

**Status: Pending Human Implementation Authorization**

**Authority:** This is the single authoritative future implementation instruction. The original planning draft, historical revised prompt and historical user-supplied quoted draft are non-executable audit records. G1–G6 are Human Accepted; complete ADR-0002 and ADR-0006 remain Proposed. This section is ready for human review, not permission to code, run tools or migrate databases.

### Role, Preconditions and Authoritative Guidance

After explicit human implementation authorization, act as the implementation engineer for Increment 5 in the authoritative `agentic-order-service` repository on `develop`. Inspect branch, HEAD and the complete staged/unstaged/untracked state; confirm the newly approved execution revision and preserve unrelated work. Read AGENTS.md, the PRD, system/domain/REST/transaction/technology documents, ADR-0001 through ADR-0006, coding/Java-Spring/persistence guidelines, testing strategy, current implementation plan/open items, the immutable Increment 3/4 acceptance records and the [final G1–G6 approval](#final-human-architecture-decisions--2026-10-16).

Accepted G1–G6 and the [central API catalog](../../02-architecture/api-error-codes.md#human-accepted-increment-5-order-extensions--2026-10-16) take precedence over historical sketches. Do not reopen approved policies. Exact guard, participation, completion and timeout interactions require implementation review and runtime verification within these policies; report a genuine conflict or unsupported requirement for approval rather than silently changing behavior.

### Four Business Endpoints and Response Contract

| Endpoint | Required success / failure |
| --- | --- |
| POST /orders | 201; Location: /orders/{id}; complete committed Order response |
| GET /orders | 200; array of complete Orders, [] when empty |
| GET /orders/{id} | 200 complete Order; ORDER_NOT_FOUND/404 for absent canonical ID |
| DELETE /customers/{id} | 204/no body for an existing Customer: delete unreferenced, deactivate referenced, preserve already inactive referenced state; CUSTOMER_NOT_FOUND/404 if absent |

Keep the existing ten Product/Customer operations and their accepted fields/statuses. Replace interim Customer DELETE OPERATION_NOT_AVAILABLE/405 only as part of the authorized implementation. Product DELETE and Order PUT/PATCH/DELETE remain unsupported with METHOD_NOT_ALLOWED/405 and accurate Allow. Do not introduce other business endpoints.

Order responses contain exactly id, customerId, customer, items, currency, subtotal, discount and total. Current customer fields are id/name/email/active; item fields are productId/productName/quantity/unitPrice/lineTotal. Use original stored product names/prices/amounts and EUR currency, never current product prices or recalculated historical amounts. No managed JPA entities, private item IDs, versions or tax fields escape. Materialize immutable transport-independent application results inside transactions and map to API DTOs without later persistence access.

### Domain Reuse and Complete Nested Validation

Reuse existing Order, OrderItem, trusted OrderLineInput, Money, converters, aggregate ownership and calculation behavior. The accepted rule is **subtotal >= EUR 100.00**: a 10% discount applies at exactly EUR 100.00; round the discount to two decimals with HALF_UP and subtract it from subtotal. Preserve the original domain implementation and PRD boundary assertions; do not duplicate pricing in controllers/services.

Creation accepts root customerId and nonempty items, each containing only productId and quantity. Never bind untrusted JSON to managed entities or expose OrderLineInput as a request DTO. Before application lookup, apply routing/method/Content-Type/Accept → canonical path UUID → complete body validation → application resource checks. Preserve safe JSON even for incompatible Accept; application/json UTF-8 media rules and UUID upper/lower/mixed-case acceptance remain unchanged.

Parse the entire document with valid UTF-8, object root, no duplicate keys at any depth and no nonwhitespace trailing data. Then select exactly one error in order: syntax/root; unknown/forbidden properties; invalid types; missing/null required fields; business values; missing/inactive resources. Inspect recursively before selecting later-category errors. Field order is customerId before items, then productId before quantity, item indices ascending; unknown/forbidden paths sort lexicographically.

Recognized forbidden root fields: id, version, customer, currency, subtotal, discount, total, unitPrice. Recognized forbidden item fields: id, version, productName, unitPrice, lineTotal, subtotal, discount, total, currency. These use UNSUPPORTED_FIELD; other unrecognized properties use UNKNOWN_FIELD. Reject caller pricing/amounts rather than ignoring them.

Require canonical 36-character UUID text with standard hyphens, normalize before duplicate consolidation, and reject malformed path/body references with INVALID_RESOURCE_ID. Required null/missing fields or null required item use REQUIRED_FIELD_MISSING; wrong field/item shapes use INVALID_FIELD_TYPE; empty items uses INVALID_ORDER_ITEMS after earlier categories. Quantity requires a JSON integer token: reject fractions, 1.0, exponent notation, strings and booleans with INVALID_FIELD_TYPE. Nonpositive/out-of-range integer values or invalid/overflowing sums use INVALID_ORDER_QUANTITY. Consolidate duplicate products using overflow-safe arithmetic before locking and enforce the existing 10,000 per-product limit, including direct application callers. Introduce no new total/order-size restriction.

### Exact Accepted Error Templates

Use exactly code/message JSON with these existing or accepted new templates where applicable; authoritative triggers remain in the central catalog. Preserve unrelated Product/Customer codes/messages unchanged. Do not add any other code, diagnostic response field, parser/SQL detail or blind replay advice.

| Code | HTTP | Exact fixed message |
| --- | ---: | --- |
| MALFORMED_JSON | 400 | The request body must be a valid JSON object. |
| UNKNOWN_FIELD | 400 | The request contains an unknown field. |
| UNSUPPORTED_FIELD | 400 | The request contains a field or activation value that this operation does not permit. |
| INVALID_FIELD_TYPE | 400 | A request field has an invalid JSON type. |
| REQUIRED_FIELD_MISSING | 400 | A required request field is missing or null. |
| INVALID_RESOURCE_ID | 400 | The resource identifier must use canonical UUID text representation. |
| PRODUCT_NOT_FOUND | 404 | The product was not found. |
| CUSTOMER_NOT_FOUND | 404 | The customer was not found. |
| ENDPOINT_NOT_FOUND | 404 | The requested endpoint was not found. |
| METHOD_NOT_ALLOWED | 405 | This HTTP method is not supported for this resource. |
| NOT_ACCEPTABLE | 406 | The requested response representation is not supported. |
| CONCURRENT_MODIFICATION | 409 | The resource was modified concurrently. Please reload and retry. |
| UNSUPPORTED_MEDIA_TYPE | 415 | The request media type is not supported. |
| INTERNAL_SERVER_ERROR | 500 | An unexpected server error occurred. |
| SERVICE_UNAVAILABLE | 503 | A required service is temporarily unavailable. |
| INVALID_ORDER_ITEMS | 400 | The order must contain at least one item. |
| INVALID_ORDER_QUANTITY | 400 | Order item quantities must be positive integers and the consolidated quantity must not exceed 10,000. |
| CUSTOMER_INACTIVE | 409 | The customer is inactive. |
| PRODUCT_INACTIVE | 409 | The product is inactive. |
| ORDER_NOT_FOUND | 404 | The order was not found. |

### G1/G2 — Placement Transactions and Participating Writes

Use Order-owned OrderPlacementCoordinator with explicit TransactionTemplate, one independent read-write PostgreSQL READ COMMITTED transaction per attempt. Reject unexpected ambient transactions before processing or database access, including internal invocation paths; never silently join or suspend an existing transaction with REQUIRES_NEW. Verify the exact ambient/bound-resource guard and participation mechanism. Provider helpers share the attempt transaction and must not independently commit.

Validate/consolidate immutable command input before acquiring locks. Lock Customer first, then each distinct Product individually in ascending canonical lowercase UUID-text order. Use authoritative pessimistic row reads without active-state filters or stale preloads. Never assume a bulk ORDER BY guarantees lock acquisition order. Read current eligibility/name/price under the protected state and retain locks through complete transaction completion. Resource-error priority is Customer-first then sorted Products across missing/inactive findings: a later missing Product must not override an inactive Customer. Capture a consistent protected state for all accepted facts; preserve ADR-0001's serially explainable outcomes and no false conflict merely because operations overlap.

Product PUT/PATCH and Customer PUT/PATCH acquire compatible locks before authoritative reads/mutation. Preserve @Version mappings, confirmed optimistic-conflict behavior, irreversible deactivation and sequential repeated inactive PATCH with no UPDATE/version increment. Avoid pre-lock dirty mutations/autoflush and cached entity state. Single-resource writes lock only that resource; any operation touching Customer and Products follows the shared order. Ordering reduces deadlocks but does not prove their absence.

Construct/save the complete Order aggregate and owned items atomically, fully materialize its immutable result inside the attempt, and return HTTP success only after TransactionTemplate completes commit. Callback return and flush are not successful commit evidence. No external calls, user interaction, response serialization or partial savepoint commit inside the locked attempt. Fresh persistence state is mandatory on every attempt; never reuse failed entities/results or continue a rollback-only transaction. Domain/application failures contain no HTTP types; central API translation observes completion outside the application transaction.

Use narrow concrete provider-owned CustomerOrderAccess/ProductOrderAccess contracts and immutable facts, not broad CRUD chaining or foreign repositories/entities. Customer deletion uses an independent OrderReferenceQuery; neither provider helpers nor reference queries call back into placement/deletion orchestration. Keep constructor and call graphs acyclic. Use existing constructor injection and composition-root wiring, not a new contracts/retry framework.

### G4 — Retry, Failure and Timing

Initial accepted settings are **three total attempts maximum**, **two-second transaction-local PostgreSQL lock timeout**, **25 ms / 50 ms retry backoff**, and a **ten-second monotonic retry-scheduling budget**. Set database timeout locally per transaction, not database-wide. These are project settings, not universal production recommendations. The scheduling budget is not a hard end-to-end HTTP deadline; review and verify pool, statement, network and commit timeout interactions.

Retry the whole attempt only if all four conditions hold: definitive completed full rollback; resources and attempt-local persistence state released; approved transient cause; remaining attempt and scheduling allowance. Observe template completion including commit/rollback; broad exception type, flush, callback return or a synchronization notification alone is insufficient. Verify cleanup before beginning the next transaction and back off outside locks/transactions. Checked failures invalidating an attempt must trigger a reviewed complete rollback path with cause preserved.

Approved transient classifications are structured SQLSTATE 40P01 (deadlock), 40001 (serialization failure), and 55P03 only positively attributable to the configured lock-timeout mechanism. Do not retry business validation, missing/inactive resources, optimistic conflicts, uniqueness violations, generic SQL/cancellation/connection failures, unknown transaction outcomes or failures after potentially successful commit. Never infer retry safety from rollback alone without approved transient classification.

Confirmed full-rollback transient exhaustion returns the existing SERVICE_UNAVAILABLE/503 template. Unknown commit result returns existing INTERNAL_SERVER_ERROR/500 where transport remains possible, with no retry and no false rollback claim; a broken connection may prevent any response. Do not implement reconciliation or idempotency features. Distinguish these outcomes with actual fault-injection evidence, including loss of confirmation after a real successful commit.

### G5/G6 — Queries and Customer DELETE

Implement dedicated OrderQueryService with application-owned read-only transactions. Explicitly load persisted Order items and obtain current Customer information through a narrow Customer-owned contract. Batch distinct Customer IDs for list retrieval; build complete immutable results within the transaction and prove absence of N+1 and serialization-time queries on real PostgreSQL. No new pagination, sorting guarantee or global multi-query snapshot guarantee.

Customer-owned DELETE transaction uses READ COMMITTED, locks Customer first, then executes a separate subsequent Order-reference existence query through an Order-owned contract. Do not check before the lock or combine the authoritative reference check into the original locking statement. Delete only unreferenced Customers; deactivate referenced ones and preserve already inactive referenced state without reactivation or unnecessary version increment. Preserve restrictive foreign keys and original Order retrieval. If deletion wins before first placement, placement observes absent Customer/404; if placement commits first, deletion's post-wait query observes the reference and deactivates. Both schedules require deterministic PostgreSQL evidence. No Product deletion, global reset or cascading deletion is introduced.

### Complete Mandatory U/H/P/A Matrix

Every row must receive actual evidence after coding authorization; all are Pending now. G6/P10 is included, not conditional. Extend existing parameterizations where useful; preserve earlier behavioral assertions and adapt only harness coordination that conflicts with legitimate blocking, without weakening accepted outcomes.

| ID / level | Required scenarios and evidence |
| --- | --- |
| U1 Unit | Direct application missing/null customer/items/item/product/quantity; empty items; zero/negative/over-limit/overflow sums; duplicate UUID normalization; reject before provider/repository access. |
| U2 Unit | Missing vs inactive resources; complete protected fact collection; correct customer/product calls and deterministic distinct lock order; no domain creation/save on rejection; approved mixed-failure priority. |
| U3 Unit/domain | Reuse exact independent PRD 99.99/100.00/100.05/150 cases; zero price, maximum line/larger subtotal, duplicate 2+3 and 6000+4000/4001; original name/price snapshots. Extend only real orchestration gaps, not duplicate pricing implementation. |
| U4 Unit/infrastructure | Ambient and bound-context guards, complete attempt count, retry eligibility/negatives, delay/budget exhaustion, checked-failure propagation, no reuse of failed results, no retry for unknown/postcommit outcome. Mocks prove orchestration only. |
| U5 Unit | Creation/get/list immutable result completeness, empty list, absent order, customer current inactive details, mapping no item ID/version/tax. |
| H1 MVC | POST 201/Location and exact complete shape; individual/list 200 with identical resource fields; [] empty; unsupported Order PUT/PATCH/DELETE 405/Allow; no accidental extra endpoint. |
| H2 MVC | Recursive unknown/recognized forbidden fields at root/items, client prices/totals/IDs, nested duplicates/trailing/syntax/null-root/array-root/absent-body distinctions, invalid UTF-8; no application calls on invalid transport. |
| H3 MVC | Items wrong type/null/empty/null element, missing/null/wrong scalar IDs/quantities, fraction/string/boolean/exponent/integral-decimal rejection under accepted integer-token policy, large values/overflow and consolidated 10000/10001. |
| H4 MVC | Canonical path/body UUID case/shape, syntactically valid absent vs malformed, header/media/path/body precedence, deterministic multi-error/tie ordering; exact codes/messages under G3; preserve incompatible Accept safe JSON. |
| H5 MVC | Customer/Product missing 404, inactive 409, Order missing 404; expected 500/503/optimistic errors mapped outside transaction, no blind replay advice. Actual transaction timing requires P tests. |
| P1 PostgreSQL | Real POST persists root/all owned items with generated IDs, restrictive references and exact stored amounts; duplicates one item; Hibernate validation and unchanged V1/V2 history/checksums. |
| P2 PostgreSQL | Rename/reprice/deactivate Product and change/deactivate Customer after placement; GET/list preserve original facts and show current customer, including inactive; response serialization after context closure causes no SQL/lazy access. |
| P3 PostgreSQL | Inject failure after root/item flush and prove complete rollback/no orphans with fresh independent transactions; business rejection leaves no rows. Commit succeeds before HTTP 201, not merely flush/callback return. |
| P4 PostgreSQL | Same/different-customer simultaneous placements sharing Products; deterministic reversed input order; no false overlap business rejection, no duplicate item rows or incompatible snapshots; independent connections/contexts. |
| P5 PostgreSQL | Product rename/reprice/deactivation vs placement and Customer update/deactivation vs placement, each winner order; observe real waits/locks and final state, preserve irreversible activity and accepted optimistic/no-op tests. |
| P6 PostgreSQL | Multi-product placement against a test-only controlled atomic multi-product writer using the same order, forced intermediate blocking, all facts coexist in a valid serial order; inspect actual JPA locking SQL and isolation, no bulk ORDER BY assumption. |
| P7 PostgreSQL | Controlled deadlock (opposite order only in test fixture), local lock timeout and classifier-negative cancellations/integrity errors; real full rollback before bounded fresh-context retry; success once or SERVICE_UNAVAILABLE/503 exhaustion. Do not depend on deadlock victim selection. |
| P8 PostgreSQL/transport | Lost confirmation after real database commit: complete order may exist, no second attempt and safe approved unknown-outcome response where possible. Distinguish confirmed rollback and HTTP disconnect; no guaranteed response on broken transport. |
| P9 PostgreSQL | Explicit single/list fetch plans; batch current-customer reads; query counts avoid per-order/per-item N+1, no unexpected cross-feature managed entities/cache state; no invented global snapshot guarantee. |
| P10 Mandatory G6 | Unreferenced delete/204/absent404, referenced deactivate/204/no body/retrievable history, repeated inactive no-op; delete vs first order both schedules, post-wait reference statement visibility, no dangling refs. Included and mandatory; cover this through unit, actual MVC/HTTP and PostgreSQL tests. |
| A Architecture | Controller→application only, no foreign persistence imports, no API/application dependencies from domain, no managed entities in REST, no recursive service/circular constructor dependencies. Tool choice stays Open; do not add ArchUnit. |


Additionally require Customer DELETE unit and actual MVC/HTTP coverage for unreferenced/referenced/already-inactive/absent outcomes, exact 204/no body or404, and immutable historical Order access. Reuse PostgreSqlTestConfiguration/@ServiceConnection, central approved postgres image, integration-test profile and random-port test support. Use independent transactions/connections/contexts, barriers/latches and bounded PostgreSQL wait observations; no arbitrary sleeps or shared rollback transaction as concurrency proof. Join workers and clean only test-owned fixtures; no persistent Compose database, Flyway clean/repair/baseline, global schema reset or volume deletion.

### Approved Files, Verification and Evidence

Use the existing file matrix as the reviewed responsibility map: Order application coordinator/commands/results/query/ambient guard; configuration wiring; Customer/Product provider-owned access and own repositories; participating application writes; Order controller/request/response DTOs; shared decoder/interceptor/error extension points; Customer service/controller/delete operation and independent OrderReferenceQuery; matching unit/MVC/PostgreSQL tests. G6 rows and all relevant smoke/MVC wiring adaptations are mandatory where actually needed. Do not create speculative placeholder classes or one interface per class.

Preserve Java21, Spring Boot4.1.1, Maven Wrapper, JUnit6, Mockito, PostgreSQL/Testcontainers, Surefire/Failsafe, JaCoCo, PIT, SonarQube and GitHub Actions. No unapproved schema/Flyway migration, dependency, POM, Makefile, Maven profile, CI or Sonar configuration change is authorized. Preserve V1/V2 and existing data; persistent-local migration/recovery and production execution remain separate safety/authorization gates.

Once authorized, run applicable ordinary Maven fast tests and complete clean verify with command-local Java21 and Docker for existing *IT responsibilities; record commands, Java/Maven identity, actual suite counts/failures/errors/skips, SQL/isolation/lock/rollback/commit/query evidence and JaCoCo reports. Preserve existing quality gates and collect actual Sonar/CI results only within approved execution scope; do not claim a new analysis or guarantee from old evidence. Report unavailable/future gates Pending, never Passed.

Run the approved applicable PIT workflow against the current baseline described above. The unchanged order scope targets domain only; use make mutation-test SCOPE=all for new Order application/API coverage, with project-wide fast tests. Preserve exact XML outcomes and reports; match class, method, JVM descriptor, source line, mutator, instruction index and description against the baseline. Explain added/removed identities and changed test selection. Review newly surviving mutants for behavioral regressions before proposing changes. Never weaken assertions or manufacture tests solely to improve the score. Keep TIMED_OUT separate from assertion-based KILLED and report both detected/generated and strict KILLED/generated when appropriate. PostgreSQL *IT remains excluded from PIT; mutation analysis does not replace integration/concurrency verification. **No numeric mutation-score threshold is authorized.**

Preserve all historical evidence in unique external directories. Present the actual complete Git diff, staged/unstaged/untracked status, verification outcomes and limitations for human review. Mark Increment5 execution Executed – Pending Human Review until explicit human implementation acceptance. Do not stage/commit/push/merge/deploy or begin another increment without the required explicit authorization; human acceptance alone is not commit or push authorization.

## Implementation Execution Evidence — 2026-10-16

**Status: Executed – Pending Human Review.** The human explicitly authorized the complete current Increment 5 prompt and G1–G6 through the execution request. Human implementation acceptance has not been granted. The original prompts, approved decisions and earlier evidence above remain unchanged; their pre-execution Pending wording records the state at that time.

### Execution Baseline and Preserved Boundaries

- Repository branch: `develop`; starting and final implementation HEAD: `81f96e262fdf0a67c96ef7861856e95b9a185d55`. This is the parent revision plus uncommitted implementation, not a claim that this implementation is committed.
- Starting state contained the existing Increment 5 planning/decision documentation, new playbook and unrelated historical Increment 3/4 editorial changes. These were preserved. No branch/worktree operation, staging, commit, push, merge or deployment was performed.
- Reused existing Order/OrderItem/Money calculations and mappings, V1/V2, Java 21, Maven Wrapper 3.9.9, Spring Boot 4.1.1, JUnit 6.0.3, dependencies, Makefile, JaCoCo, PIT and CI. No migration or application-table/schema change was introduced.
- All database tests used the centrally configured PostgreSQL 17.11 Testcontainers image. Fault triggers/functions/sequences were uniquely named, test-owned disposable fixtures and removed in `finally`; they are not migrations. No persistent Compose database or volume was used, reset or deleted.
- G1–G6 remain Human Accepted; complete ADR-0002/0006 remain Proposed. Execution evidence does not silently accept those ADRs or authorize production use.

### Implemented Files and Responsibilities

| Area | Actual additions / changes |
| --- | --- |
| Composition and attempts | `configuration/OrderTransactionConfiguration`; `order/application/AmbientTransactionGuard`, `OrderPlacementCoordinator`, `OrderRetryPolicy`: explicit READ COMMITTED template, ambient/bound-state rejection, complete transaction observation, configured local lock timeout and bounded fresh attempts. |
| Feature-owned contracts | `customer/application/CustomerOrderAccess`, `product/application/ProductOrderAccess`, `order/application/OrderReferenceQuery`; own repositories provide individual pessimistic reads, batch current Customers, reference existence, explicit item fetch/list and flush. No foreign persistence access. |
| Existing writes and DELETE | Product/Customer application updates/deactivation acquire compatible locks; Customer DELETE locks first then queries references separately, deletes only unreferenced Customers and otherwise deactivates/no-ops. Existing versions and irreversible activity are preserved. |
| Order results and transport | `PlaceOrderCommand`, `OrderFailure`, `OrderResult`, `OrderQueryService`; `order/api/CreateOrderRequest`, `OrderItemRequest`, `OrderResponse`, `OrderController`; three Order endpoints and the Customer DELETE controller method. Immutable complete results are built inside transactions. |
| Strict shared API | `StrictJsonRequestDecoder`, `ApiRequestInterceptor`, `ApiError`, `ApiExceptionHandler`; nested Order validation, five exact error templates and removal of the superseded Customer DELETE fallback. Existing Product/Customer contracts retained. |
| Tests and wiring | New Order application/MVC/PostgreSQL tests and Customer deletion unit tests; adapted smoke/MVC mock wiring, repository expectations, literal error catalog and Customer DELETE assertions. Existing optimistic completion fixture adapted as described below. |
| Documentation | This execution record, playbook index and directly affected current-status references; the final complete diff includes the preserved pre-existing documentation edits separately from source implementation. |

The exact new/modified file inventory and every added-file diff are preserved in the external evidence directory, not inferred solely from tracked `git diff --stat`.

### Actual Maven and Coverage Results

Command-local Java environment:

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
  ./mvnw -B -ntp clean verify
```

Final command exit: **0**, `BUILD SUCCESS`, 44.557 seconds. Surefire: **429 tests, 0 failures, 0 errors, 0 skipped**. Failsafe: **119 tests, 0 failures, 0 errors, 0 skipped**. Total: **548 test executions**; counts include parameterized invocations and are not counts of Java methods. Final Surefire/Failsafe XML/text reports and the complete build log are preserved externally.

JaCoCo from that clean build: lines **667/677 (98.52%)**; branches **477/519 (91.91%)**; instructions **4125/4210 (97.98%)**; methods **217/219**; classes **59/59**. The full HTML/XML report and execution data are preserved. Coverage is execution evidence, not proof of correctness or a new threshold.

### U/H/P/A Matrix — Actual Evidence

| Matrix | Actual checks / test owner |
| --- | --- |
| U1–U3 | `PlaceOrderCommandTest`, `OrderPlacementCoordinatorTest` and preserved `OrderTest`: direct required/empty/quantity/consolidation boundaries, immutable input, Customer-first/canonical distinct Product calls, resource rejection without persistence, exact EUR 100 orchestration; original PRD arithmetic, maximum/zero/snapshot assertions preserved. |
| U4 | `OrderPlacementCoordinatorTest`: independent starts/confirmed rollback, 25/50ms delays, three attempts, positive-origin monotonic clock and exact scheduling boundary, second budget check after backoff, missing/inactive/SQL negatives, bound/synchronized context rejection, cleanup-leak rejection, real interrupted backoff with interrupt preserved, unknown commit nonreplay. SQLException causes remain available for classification. |
| U5 | `OrderQueryServiceTest`, `OrderApiTest`: explicit get/list, distinct current-Customer batching, empty/missing behavior, inactive current data, immutable historical items/results and exact transport fields. |
| H1–H5 | `OrderApiTest`, existing shared error tests and updated Customer API tests: exact success/Location/body contracts, all recognized forbidden root/item fields, recursive parse/type/presence/value precedence, UTF-8/UUID/integer token and inclusive/consolidated quantity boundaries, safe fixed error envelopes and unsupported methods with GET/HEAD/OPTIONS Allow. Invalid requests never invoke placement. |
| P1/P2/P9 | `OrderApiIT` plus preserved foundation/persistence tests: HTTP 201 followed by independent committed root/item reads, duplicate consolidation, original stored facts after repricing/renaming/deactivation, current inactive Customer, unchanged Flyway history, complete serialized GET/list with two Hibernate statements each. Serialization adds no lazy SQL. |
| P3 | `OrderPlacementConcurrencyIT.databaseFlushFaultsRequireCompleteRollbackAndApprovedClassification`: fault on the second owned-item INSERT after root/first-item INSERT; no partial rows/orphans, fresh attempts and one final root/two items for retryable faults; no rows for negative classifications. Business rejection also leaves no order. HTTP 201 follows the coordinator's completed template and independently visible committed data. |
| P4/P5 | `OrderPlacementConcurrencyIT`: same/different Customer placements with shared Products and reversed input; eight Product/Customer update/deactivate winner schedules; latches plus observed `pg_stat_activity` lock waits, canonical per-row calls, exact committed historical facts and retained versions. |
| P6 | Atomic test-only multi-product writer versus placement in both winner orders: forced intermediate block, both original or both new facts, correct totals, no incompatible mix. Captured Hibernate SQL uses individual `where id=? for no key update of ...` reads; no bulk ordering assumption. |
| P7 | Real opposing-order fixture deadlock: exactly one commit/one SQLSTATE 40P01, no victim assumption. Separate controlled server faults 40P01/40001 prove full partial-graph rollback before three fresh EntityManagers and eventual single success. Actual local 2s lock timeout exhausts three fresh attempts without rows. Server-raised 23514/57014 and 55P03 outside the configured lock path stop after one attempt. Unit tests additionally reject connection/unknown/optimistic categories. |
| P8 | `actualCommitWithLostConfirmationNeverReplays`: real transaction manager commits, wrapper then loses confirmation; exactly one Order/item exists, one commit attempt and typed TECHNICAL outcome. MVC verifies its exact safe 500 envelope separately. This controlled injection is not a network-disconnect or guaranteed-response test. |
| P10/G6 | Customer deletion unit, MVC/real HTTP tests and both deterministic first-order races: unreferenced delete/204/404, referenced deactivation, inactive no-op/version preservation, historical Order retained; post-wait reference query sees the committed first order. |
| A | Source import inspection/manual contract review found no controller-to-repository, foreign feature persistence or domain-to-project-outer-layer dependencies; immutable REST shapes verified. Successful full Spring contexts prove current constructor wiring has no circular dependency. No architecture enforcement dependency added; concrete tooling remains Open. |

The real concurrency suite has **23 parameterized/test executions**. PostgreSQL assertions confirm READ COMMITTED and transaction-local `lock_timeout=2s`; failed attempts expose three distinct EntityManagers. All threads are joined and synchronization has bounded deadlines. These tests do not establish a universal ten-second HTTP deadline, production pool/network timeout behavior or absolute deadlock freedom.

### Existing Optimistic Test Harness

Production writes now serialize through pessimistic reads. The old `OptimisticLockingIT` rendezvous required two stale service reads before either commit, which conflicts with legitimate blocking. Its armed test-only repository advisor now substitutes an ordinary read only for that fixture's resource, preserving all existing optimistic completion/409/no-replay assertions. Unarmed paths and the new concurrency suite use actual production locking. This stale-read fixture tests fallback optimistic handling; it is not evidence that production pessimistic writes overlap.

### PIT Evidence

Executed with command-local Java 21:

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
  make mutation-test SCOPE=all
```

The unchanged Makefile runs/validates the complete fast baseline and PIT DEFAULTS with one thread, preserving HTML/XML, logs, exit codes, checksums, selectors and Git state in unique external directories. Final baseline **429/0/0/0**, baseline XML validation exit **0**, PIT exit **0**. PostgreSQL `*IT` remains excluded; eligible tests are coverage-selected, not all executed for every mutant. No numeric mutation-score threshold is applied.

| Run | Generated | KILLED | SURVIVED | NO_COVERAGE | TIMED_OUT | Detected/generated | Strict KILLED/generated |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Pre-change baseline evidence `PIT-all-20261016T075327Z-qMGnCc` | 346 | 288 | 40 | 17 | 1 | 83.53% | 83.24% |
| Initial implementation `PIT-all-20261016T085219Z-xAEZE5` | 483 | 389 | 64 | 29 | 1 | 80.75% | 80.54% |
| Final implementation `PIT-all-20261016T085748Z-5CPr2f` | 483 | 403 | 53 | 26 | 1 | **83.64%** | **83.44%** |

Final PIT Maven duration: **1 minute 47 seconds**. Detected is **404/483**, including the one timeout; assertion-based killed is **403/483**. A successful run does not require all mutants killed. The timeout is `ApiExceptionHandler.classify`, line 48, conditional-removal index 52: removing a traversal guard can prevent termination. It is recorded separately, never represented as an assertion kill.

Baseline-to-final exact tuple matching (class/method/JVM descriptor/line/mutator/index/description) finds **222 shared identities, 261 added and 124 removed**. Production additions and shifted lines/bytecode account for the changed inventory; these are not 261 demonstrated new behaviors or 124 lost assertions. Two mechanically matched transitions require interpretation:

- `Order.getId`, line 75, null-return index 5: NO_COVERAGE → SURVIVED. New application mapping executes this getter with transient mock aggregates that have no persistence ID. Generated-ID semantics are verified in PostgreSQL tests, excluded from PIT; no artificial ID unit test was added.
- `StrictJsonRequestDecoder.supports`, line 24, conditional-removal index 14: KILLED → SURVIVED is an index collision after adding the Order clause. Previously it referred to Customer PATCH; that semantic clause now has index 17 and remains KILLED by the same `acceptsTrailingJsonWhitespaceAndInactivePatch` test. This is not evidence of a behavioral regression.

Between the two implementation PIT runs all **483 identities match**, none are added/removed, and **14 improve to KILLED** (11 formerly SURVIVED, three formerly NO_COVERAGE); none regress. Exact XML identities and detecting tests are in `pit-final-comparison.json`:

| Improved behavior / mutant | Detecting existing/new test |
| --- | --- |
| Backoff sleep and interrupt restoration (two) | `interruptedRealBackoffPreservesInterruptAndNeverBeginsAnotherAttempt` |
| Local lock-timeout statement and explicit flush (two) | `consolidatesLocksCanonicalOrderAndCalculatesAcceptedThreshold` |
| Post-rollback resource guard | `leakedRollbackResourceStopsRetryBeforeBackoff` |
| Exact retry-budget boundary | `schedulingBudgetPreventsAnotherAttempt` |
| Elapsed-time subtraction at both checks (two) | `exhaustionReturnsUnavailableAndNeverSaves`, using a positive clock origin |
| Order get returning a complete result | `batchesDistinctCurrentCustomersAndReturnsImmutableHistoricalFacts` |
| Inclusive 10,000 command/decoder boundaries and consolidated limit (three) | `acceptsInclusiveQuantityBoundariesThroughStrictMvc` |
| Accurate HEAD Allow | `precedenceMediaAndUnsupportedMethodsRemainSafe` |
| Strict Product-ID scalar shape | `rejectsEveryWrongProductIdShapeBeforeApplication` |

Remaining new survivors were reviewed without automatic exclusions: the loop boundary is masked by the explicit third-attempt exhaustion guard; command/decoder quantity checks have overlapping defenses, so removing one can still yield the same required HTTP rejection. `OrderResult`'s missing-current-Customer guard is protected by restrictive database references and the coordinated delete protocol. Defensive retry cause-chain guards have residual fast-test gaps for combined/nested exception shapes; the implementation remains fail-closed for unknown categories, with actual SQL negative tests passing. Provider/query/configuration mutations remain uncovered by the mocked fast suite and are exercised through the separate PostgreSQL suite; this is a persistence mutation limitation, not proof those mutants were killed. Existing framework/transport survivors remain review items. No new production behavioral regression was identified, and no test was added solely to raise the score.

### Evidence Locations

- Build, reports, coverage, safety checks, full patch and file inventory: `agentic-order-service-mutation-evidence/INCREMENT5-20261016T085046Z-3oiwc8s6` (sibling of the repository).
- Initial PIT: sibling `agentic-order-service-mutation-evidence/PIT-all-20261016T085219Z-xAEZE5`.
- Final PIT: sibling `agentic-order-service-mutation-evidence/PIT-all-20261016T085748Z-5CPr2f`.
- `complete-working-tree.diff` includes staged/unstaged/new-file content and preserved pre-existing documentation. `implementation-source.diff` isolates source/test changes for review. Neither patch stages changes.
- Original current instruction SHA-256 before execution-record edits: `eadf21667e288b09af73438cffb48cd104c6e725e1938322ea47b9a19b7f2d0e`; instruction content is unchanged. All earlier experiment directories remain untouched.

### Problems Encountered

- Initial restricted-environment Mockito attachment/context failures were diagnostic failures, not passing verification. Verification used the approved Java 21 environment with the required process/Docker access; intermediate failed logs are retained.
- Mockito stubbing through a `MANDATORY` transactional provider proxy initially invoked transaction advice outside a transaction. The concurrency fixture now stubs the underlying spy while production calls retain transaction advice; all final tests pass.
- Initial PIT inspection exposed a missing HEAD Allow assertion after Customer DELETE replaced interim 405, and meaningful retry/validation assertion gaps. Targeted tests now cover HEAD/OPTIONS, explicit local timeout/flush, positive monotonic clock origin, exact budget boundary, interrupt preservation, cleanup leaks and strict quantity/ID boundaries. No production rule was changed to improve a score.
- Adding Order decoder support shifts bytecode indices; a tuple match can refer to a different conditional even with the same line/index. Source/semantic review is required in addition to mechanical matching.

### Lessons Learned

- Preserve optimistic fallback tests with explicit stale-state fixtures when a new pessimistic protocol correctly prevents their original rendezvous.
- Prove rollback using partially inserted aggregate graphs and real PostgreSQL failures; a mock rollback callback is insufficient.
- Separate successful database commit, lost confirmation and HTTP rendering; uncertain outcomes cannot safely be replayed.
- Use a nonzero monotonic clock origin when testing elapsed budgets. PIT can reveal assertions that accidentally depend on a zero clock.
- Broad fast-test eligibility preserves cross-feature detection; persistence/concurrency tests remain a separate mandatory gate.

### Review Status and Remaining Gates

**Executed – Pending Human Review.** Human acceptance, a new SonarQube analysis/remote CI result for this uncommitted change, persistent-local recovery/migration authorization, production operational validation and complete ADR acceptance remain separate. No new Sonar result is claimed. PostgreSQL integration mutation coverage remains future work; surviving/timeout mutants require contextual review, not artificial tests or automatic exclusions. No next increment, commit or push is authorized.

### Final Documentation and Git Checks

`git diff --check` passed. New Git-visible files passed individual `git diff --no-index --check` checks. Relative Markdown targets (434) and heading anchors (142) resolved. The original current instruction content is unchanged. Source import review found no forbidden project-layer/foreign persistence imports; successful Spring contexts verified current construction wiring. The secret check found no recognized token/private-key patterns in added content; `.env` remains ignored and untracked, and no credentials were read or printed. This is a scoped heuristic/manual check, not a security guarantee.

Branch and HEAD remain `develop` / `81f96e262fdf0a67c96ef7861856e95b9a185d55`; the index is empty. Build inputs, wrapper, migrations and CI were preserved. The complete working-tree patch includes the existing documentation edits, including the unrelated Increment 3/4 editorial changes, which were not discarded or reinterpreted as implementation work. No commit, stage, push or branch/worktree operation occurred.


## Targeted Independent Review — 2026-10-16

**Status: Increment 5 — Executed, Pending Human Review.** The human requested targeted investigation/correction of timeout attribution, ambiguous SQL exception graphs and nested REST response DTO ownership. G1–G6, four endpoints, safe error templates, calculations, lock order, transaction boundaries and retry limits remain unchanged. This appended record supersedes the earlier *current* verification totals for the reviewed changes, while preserving every original prompt and preceding execution result as history.

### Finding 1 — Confirmed Timeout-Attribution Defect

`OrderPlacementCoordinator.place` previously set a boolean for every RuntimeException from a Customer/Product provider. That established only the call phase; it did not prove that a nested 55P03 represented the configured `lock_timeout`. PostgreSQL can also report 55P03 for NOWAIT or user-raised errors.

Correction: capture the actual provider exception; the timeout classification is eligible only when the exception leaving the completed template is that same original lock-call failure. `OrderRetryPolicy.postgresLockTimeout` additionally requires the actual PostgreSQL driver's structured 55P03 diagnostic with routine `ProcessInterrupts`. Merely containing a SQLException with that state or an English timeout message is insufficient. The successful preceding `SET LOCAL lock_timeout='2s'`, known locking call, server diagnostic, definitive full rollback and resource-cleanup guard are complementary evidence; none alone permits replay. Wrapped causes are accepted only when their full graph is consistent and no unsafe category is present.

The existing JDBC dependency is runtime-scoped. A small reflective lookup reads its public `getServerErrorMessage/getRoutine` diagnostics without changing the POM, adding a dependency or parsing localized messages. Absent/inaccessible diagnostics and unrecognized driver types fail closed. The routine is PostgreSQL implementation metadata, not a cross-version guarantee: keep the real timeout/NOWAIT/RAISE regression checks when changing the pinned server/driver. See [PostgreSQL 17 timeout handling](https://raw.githubusercontent.com/postgres/postgres/REL_17_STABLE/src/backend/tcop/postgres.c) and [JDBC structured error API](https://jdbc.postgresql.org/documentation/publicapi/org/postgresql/util/ServerErrorMessage.html).

Actual PostgreSQL checks: real Customer and Product lock waits still exhaust three fresh attempts under the configured 2s timeout; NOWAIT is rejected as retry evidence; a server-raised 55P03 *inside* ProductOrderAccess causes one attempt and no Order. The earlier server-raised 55P03 at item flush remains a negative case. Synthetic positive timeout fixtures now supply structured server diagnostics, preserving the original attempt/rollback assertions rather than weakening them.

### Finding 2 — Confirmed Exception-Graph Classification Defect

`OrderRetryPolicy.transientFailure` previously retained an approved candidate even when another SQLException had an unrecognized state. A mixed 40P01/HY000 or 40P01/23514 chain could therefore authorize a retry. Cause/nextException cycles terminated but were not themselves treated as ambiguous evidence.

Correction: every reachable SQLException must carry the same positively approved state. Missing, empty, unknown, connection, integrity and cancellation states reject the entire graph; differing approved states also reject it conservatively. Typed connection/integrity/generic timeout failures cannot be overridden by a contradictory approved state. Business/resource/optimistic errors, transaction-system/establishment failures and suppressed cleanup failures are nonretryable. An iterative identity-based DFS distinguishes active-path cycles (reject) from legitimate shared completed nodes (accept when otherwise consistent). It neither uses SQLException's potentially cyclic iterator nor recurses through the Java call stack.

`OrderRetryPolicyTest` covers both state orders, wrapped causes, nextException chains, consistent shared nodes, mixed approved states, null/empty/unknown states, unsafe exception types, hidden transaction application failures, suppressed cleanup failure and cause/nextException cycles. Coordinator tests prove unattributed provider 55P03 stops after one rollback with no persistence or backoff. Rollback confirmation, unknown-commit nonreplay and all accepted retry settings are preserved.

### Finding 3 — Optional DTO Refinement; No Current Boundary Defect

`OrderResponse.from` uses Customer-owned immutable `CustomerResult` and immutable `OrderResult.Item`. Both contain only the approved response fields and immutable values; no entity, persistence ID/version or lazy state is exposed. The dependency is API → application, never application → API, and feature collaboration uses public contracts. Existing exact root/customer/item field-set assertions protect the accepted JSON contract.

Dedicated nested API records could decouple future transport evolution, but would currently duplicate field-for-field mappings without correcting observed behavior or a forbidden dependency. No DTO/class change was made. Reconsider this small optional refinement if transport and application result shapes diverge; this review does not approve such a divergence.

### Changed Files

- `order/application/OrderPlacementCoordinator.java`: original lock exception identity rather than phase-only boolean.
- `order/application/OrderRetryPolicy.java`: positive timeout attribution, consistent-state whitelist and conservative graph traversal.
- `order/application/OrderPlacementCoordinatorTest.java`: attributed positive fixture, strengthened plain-55P03 expectation and no-replay negative case.
- New `order/application/OrderRetryPolicyTest.java`: exception-graph and driver-diagnostic cases.
- `order/application/OrderPlacementConcurrencyIT.java`: retain existing assertions, run configured timeout against both resource types, add real NOWAIT and provider-raised 55P03 negatives.
- This playbook: append findings/rationale/evidence; original prompts and previous results untouched. Other existing working-tree files, including `OrderResponse`, were preserved.

### Actual Verification — Targeted Review

Focused command, Java 21:

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
  ./mvnw -B -ntp -Dtest=OrderRetryPolicyTest,OrderPlacementCoordinatorTest \
  -Dit.test=OrderPlacementConcurrencyIT verify
```

Focused execution passed **63 unit** and **26 PostgreSQL integration** invocations, exit 0. Additional contradictory-type tests were then included in the full regression run:

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
  ./mvnw -B -ntp clean verify
```

Full result: **BUILD SUCCESS**, exit **0**, **52.921 seconds**; Surefire **474**, Failsafe **122**, all **0 failures/errors/skips** (**596 test executions**). The final full suite includes 44 policy tests, 26 coordinator tests and 26 concurrency-test invocations. JaCoCo: **684/696 lines (98.28%)**, **523/561 branches (93.23%)**, **4275/4362 instructions (98.01%)**. Intermediate diagnostic failures (an unavailable test enum constant and an uninitialized mocked Throwable) were corrected in test fixtures and retained in evidence; neither was reported as passing verification.

### PIT Comparison — Targeted Review

Focused PIT used the unchanged `mutation-order` profile, DEFAULTS/one thread, project-wide fast-test eligibility and command-line production filters for exactly `OrderPlacementCoordinator` and `OrderRetryPolicy`. XML confirmed precisely those two classes. Exit 0; **78 generated = 74 KILLED, 3 SURVIVED, 1 NO_COVERAGE, 0 TIMED_OUT**; strict killed and detected percentages both **94.87%**. This is a focused result, not a project-wide score.

The preceding full-run subset had 49 mutants (44 KILLED/5 SURVIVED). Exact identity matching found 15 shared, 63 added, 34 removed and no status transitions among shared identities; rewritten graph logic and shifted bytecode prevent treating the inventory difference as a score-only improvement. Remaining focused outcomes: the coordinator loop boundary is masked by explicit third-attempt exhaustion; skipping completed-node deduplication repeats work on a shared DAG without changing the tested decision; the driver-type guard overlaps the reflective missing-method fallback for ordinary SQLExceptions; the diagnostic-reflection failure fallback has no fast execution coverage with the installed supported API. These are recorded limitations, not automatic exclusions or reasons to manufacture tests.

#### Refreshed Project-Wide PIT Evidence

Executed `make mutation-test SCOPE=all` with command-local Java 21. Evidence: repository sibling `agentic-order-service-mutation-evidence/PIT-all-20261016T091902Z-qLzvm4`. Baseline **474/0/0/0**, baseline XML validation exit 0 and PIT exit 0; PIT Maven duration **1 minute 53 seconds**. XML: **512 generated = 433 KILLED, 51 SURVIVED, 27 NO_COVERAGE, 1 TIMED_OUT**. Detected **434/512 (84.77%)** includes the timeout; strict killed **433/512 (84.57%)** does not. This is the refreshed current full-run evidence, not an imposed score gate.

Compared with pre-review `PIT-all-20261016T085748Z-5CPr2f`, **449 exact identities match**, **63 are added** and **34 removed**; **no matched identity changes status**. The changed graph implementation/bytecode accounts for the new inventory. The 78-mutant focused subset and its four residual outcomes above explain the scoped limitations; unchanged-class outcomes did not regress. The existing full-run timeout remains separate from killed. Exact XML comparisons and reports are preserved. PostgreSQL `*IT` remains excluded; no numeric mutation threshold or automatic exclusions were introduced.

Git whitespace checks passed for tracked and new files; 434 relative Markdown targets and 142 heading anchors resolved. The previous playbook content is byte-for-byte preserved. Scope hashing confirms only the five listed existing files and one new test file changed during this targeted review. The index is empty and all unrelated working-tree changes remain intact.

### Evidence and Review Boundaries

Evidence root (repository sibling): `agentic-order-service-mutation-evidence/INCREMENT5-REVIEW-20261016T091354Z-ftnte4uc`. Contains original file snapshots, focused/full Maven logs and exit evidence, Surefire/Failsafe XML, JaCoCo, focused PIT XML/HTML, exact comparisons, review-only diff, complete working-tree diff and final Git status. Previous evidence was not overwritten.

No persistent database operation, migration, dependency/POM/Makefile/CI change, branch switch, stage, commit, push, merge or deployment occurred. Branch remains `develop` at `81f96e262fdf0a67c96ef7861856e95b9a185d55`. Complete ADR-0002 and ADR-0006 remain Proposed; human implementation acceptance and new remote Sonar/CI evidence remain separate. **Increment 5 — Executed, Pending Human Review.**


## Final Human Acceptance — 2026-10-16

**Reviewed – Accepted.** The human explicitly accepted Increment 5 — Order Placement & Concurrency on 2026-10-16, including implementation, approved G1–G6 decisions, mandatory U/H/P/A responsibilities, PostgreSQL concurrency verification and the targeted independent-review corrections. Earlier Pending statements remain historical execution/review records, superseded for current implementation acceptance by this section.

Accepted operations: **POST /orders**, **GET /orders**, **GET /orders/{id}**, **DELETE /customers/{id}**. Existing Product/Customer operations are preserved. The discount applies at **subtotal >= EUR 100.00**. Original domain calculations/mappings, Flyway V1/V2, dependencies and build infrastructure are preserved.

The two confirmed review findings were resolved: positive PostgreSQL configured-lock-timeout attribution and conservative SQL exception-graph retry classification. Nested REST DTO separation was reviewed and intentionally deferred as optional, not a known functional defect. Customer-first canonical individual Product locks, completed READ COMMITTED attempts, full rollback/resource release, safe bounded retries and no uncertain-commit replay are within the reviewed scope.

### Accepted Verification Evidence — Previously Executed

These results are verified from preserved reports, not new executions during acceptance:

- Java 21 Maven `clean verify`: **BUILD SUCCESS**, exit 0; Surefire **474**, Failsafe **122 PostgreSQL tests**, **596 total executions**, zero failures/errors/skips.
- JaCoCo: **684/696 lines (98.28%)**, **523/561 branches (93.23%)**.
- Project-wide PIT `PIT-all-20261016T091902Z-qLzvm4`: **512 generated**, **433 KILLED**, **51 SURVIVED**, **27 NO_COVERAGE**, **1 TIMED_OUT**; **449 matching identities** versus the preceding implementation run, no status regressions. A timeout is not an assertion kill.
- Focused retry PIT: **78 generated**, **74 KILLED**, **3 SURVIVED**, **1 NO_COVERAGE**. PostgreSQL `*IT` remains excluded from PIT; no numeric mutation threshold is introduced.
- Source checksums match the verified implementation snapshots. Evidence is preserved in the sibling `agentic-order-service-mutation-evidence/INCREMENT5-REVIEW-20261016T091354Z-ftnte4uc` and referenced PIT directories. No tests, PIT, SonarQube or CI were rerun for this acceptance task.

### Delivery Authorization and Separate Gates

The human separately authorized one commit and a non-force push to the existing `develop` upstream, with subject `feat(order): implement atomic order placement and concurrency-safe customer deletion`. Delivery starts from `81f96e262fdf0a67c96ef7861856e95b9a185d55`; this acceptance record and implementation belong to that delivery commit. The resulting commit SHA and actual push outcome are reported after Git confirms them; this section does not predict success.

The human additionally requested removal of Interview Talking Points from maintained documentation, including the pre-existing Increment 3/4 editorial deletions. Lessons/evidence and immutable prompt text are preserved; this editorial cleanup does not broaden earlier increment acceptance.

**Complete ADR-0002 and ADR-0006 remain Proposed.** A new SonarQube analysis and remote CI evidence remain Pending unless separately verified. Persistent-local database recovery/migration, production operational validation, deployment and dedicated persistence mutation verification remain separate gates. No merge, rebase, branch switch, release, deployment or next increment is authorized.
