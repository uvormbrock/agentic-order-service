# ADR-0006: Product and Customer Optimistic Locking

**Status:** Proposed

**Direction:** Human Accepted — Product/Customer JPA @Version, recorded 2026-10-14

**Technical decisions:** Human Accepted — boxed Long mapping, BIGINT/NOT NULL/backfill/default policy, sequential no-op deactivation

**Implementation:** Not authorized; implementation/runtime verification Pending; complete ADR remains Proposed

## Context

Product/Customer PUT replaces editable details while retaining activity; PATCH irreversibly deactivates (PRD BR-012). Unversioned concurrent aggregate writes may silently overwrite newer details or restore a stale active value. The initial Increment 4 plan proposed column-scoped writes/DynamicUpdate as a narrow safeguard. The human instead explicitly selected optimistic conflict detection through @Version. This record captures that approval without promoting the complete ADR or approving code/migrations.

Authorities: [PRD](../01-requirements/product-requirements.md), [ADR-0004](ADR-0004-persistence-mapping.md), [approved Increment 4 decisions](../04-agentic-coding/prompts/05-product-customer-api.md#approved-architecture-and-api-decisions--2026-10-14), [transaction conventions](../02-architecture/transaction-locking.md), [API error catalog](../02-architecture/api-error-codes.md).

## Human-Approved Architecture Direction

- Introduce a JPA @Version field on Product and Customer aggregates to detect conflicting writes and prevent stale writes from undoing completed deactivation.
- Map confirmed optimistic-lock failures, including flush/commit failures, to CONCURRENT_MODIFICATION/409 outside the application transaction. Do not silently retry writes or convert unrelated failures into conflicts.
- Plan a new Flyway V2; do not edit V1. No V2 file, schema execution or implementation is authorized here.
- Preserve D1–D10 UUID generation, field access/protected constructors, controlled mutation, converters, reference equality, aggregate boundaries, constraints and Increment 3 acceptance/history.

This is additional version metadata, not a reopened UUID identity/equality choice. Order/OrderItem versions or API version fields are not selected. Entity versions remain internal; preserve existing response fields.

## Human-Approved JPA Mapping and Flyway V2

Selected for both Product and Customer:

```java
@Version
@Column(name = "version", nullable = false)
private Long version;
```

Use boxed Long, not primitive long. JPA/Hibernate exclusively initializes and maintains the version; no public setter, caller-supplied version or REST request/response version property. Preserve generated UUID identities, reference equality, controlled mutations and all D1–D10 choices. Prove entity-newness detection, version initialization/persistence/increment and stale-update detection with Spring Data JPA/Hibernate, rather than treating annotations as evidence.

Plan a new versioned migration; reviewed proposal filename `V2__add_product_customer_versions.sql` follows the existing naming convention. The accepted SQL policy for products/customers is BIGINT, final NOT NULL, existing rows backfilled with 0, and no final database default. A temporary default 0 may be used during migration but must be removed after backfill. New entity versions are initialized/maintained by Hibernate, not by a retained database default.

Preserve existing records, constraints, foreign keys, indexes and V1 checksum. Do not create/execute V2 or modify V1 in this task. The concrete four-statement SQL sequence is reviewed below; coding authorization and implementation verification remain Pending; the selected mapping, type, backfill and final default policy are no longer Open decisions. Production deployment, execution permissions and rollout coordination remain separate/Open.

Verify both existing V1 schemas with representative active/inactive records and fresh V1→V2 schemas, correct Flyway history/checksums, preservation of data/constraints, Hibernate validation and actual insert/update/version behavior. Repeat startup/validate/migrate verification must not reapply the versioned V2, change history or reset versions; this is not selection of a repeatable R__ migration.

## Human-Approved Idempotent Deactivation

PATCH {"active":false} on an active Product/Customer deactivates and returns 200. A sequential repeat on an already inactive record returns 200 with the unchanged inactive representation, no unnecessary SQL UPDATE and no artificial version increment. Do not force a version increment or issue unconditional update SQL for this no-op. Reactivation remains prohibited; no automatic retry.

Sequential repetition is distinct from concurrent overlap. Concurrent requests may conflict depending on their observed version and actual dirty-write behavior. A confirmed optimistic conflict returns CONCURRENT_MODIFICATION/409; do not require every overlapping request to conflict or convert every conflict to a successful no-op.

## Rationale and Alternatives

@Version checks the version observed by the transaction when writing and reports a stale concurrent mutation rather than silently losing another writer's state. It integrates with accepted direct JPA aggregates and application-owned transactions.

Rejected as the selected solution: unversioned whole-row writes can lose state; DynamicUpdate/column-scoped updates alone may avoid some activity overwrites but do not detect all conflicting detail updates. They are no longer the current Increment 4 recommendation. Explicit pessimistic mutation orchestration is deferred with the broader Order protocol; it is not introduced by this direction. ETag/If-Match could additionally detect stale HTTP clients, but requires separate API approval and is excluded now. No retry library or generic CRUD/version abstraction is needed.

## Consequences and Scope Limitations

A conflicting Product/Customer write may now return 409 and roll back. The API message asks the caller to reload/retry; the server does not replay automatically. Successful writes must preserve stable IDs, normalized fields and irreversible deactivation.

Optimistic locking detects overlapping persistence transactions observing an old entity version. A client that fetched an old representation may later issue PUT after another transaction committed; if the service loads the newest entity, JPA alone cannot detect that stale client intent. No ETag/If-Match or caller-supplied version is approved; do not promise protection against every stale-client overwrite.

Sequential repeated deactivation must produce no SQL UPDATE or version increment and return the unchanged inactive representation/200. Concurrent overlap follows the accepted conflict/no-op distinction above; its actual provider behavior requires PostgreSQL verification.

Version checks do not protect multi-product placement reads/snapshots or customer deletion races by themselves. [ADR-0001](ADR-0001-order-placement-concurrency.md) remains Accepted, [ADR-0002](ADR-0002-transaction-locking.md) remains Proposed/unverified and its future pessimistic protocol is not replaced. Future participating mutation paths must be reconciled with that protocol under separately approved work.

## Verification Criteria — Pending

All concurrency scenarios apply separately to Product and Customer. Use PostgreSQL/Testcontainers with independent transactions/persistence contexts, deterministic barriers/latches and bounded deadlines; no arbitrary sleeps. Inspect actual Hibernate version predicates, fresh committed database state and rollback after confirmed failures. Unit tests alone are insufficient.

| Scenario | Required evidence |
| --- | --- |
| PUT vs PUT | A stale dirty update is rejected; no silent lost update; winner preserved and loser fully rolled back. |
| PUT vs PATCH | A stale concurrent update cannot undo deactivation; confirm version conflict/state in the controlled completion order. |
| PATCH vs PUT | A later stale write cannot restore active state; check both permitted winner orders and final normalized details/activity. |
| PATCH vs PATCH | Actual observed version/dirty-write ordering determines conflict or no-op; no blanket all-conflict/all-success assertion. |
| Sequential repeated PATCH | Both calls return 200; second representation unchanged; SQL UPDATE count and version increment are zero for the repeat. |
| Stale entity after committed update | Real Hibernate optimistic conflict on a stale dirty write, including flush/commit translation to 409. |
| Stale entity after committed deactivation | No reactivation/stale overwrite; confirmed conflict leaves the committed inactive state intact. |

Also prove:

- Exact boxed Long mapping, initialization/persistence/update increments, UUID/newness behavior through existing Spring Data repositories and Hibernate schema validation.
- Existing representative V1 data→V2 and fresh V1+V2, BIGINT/NOT NULL, existing-row version 0, no final database default, unchanged V1 checksum/history, preserved data/constraints/indexes/FKs and Hibernate-owned insertion.
- Repeat startup/migration validation does not reapply V2, modify checksums/history, reset versions or alter records. Test version behavior after migration as well as schema shape.
- Actual API 200/409 and safe envelopes, external transaction proxy/completion, rollback and immutable response materialization; no automatic retry or assumed rollback for uncertain completion.
- Preserve all Increment 1–3 tests, D1–D10 and current quality gates. Full Order locking/retries are not verified by these tests; future/unavailable gates remain Pending.

## Remaining Decisions and History

2026-10-14: Human approved @Version direction and CONCURRENT_MODIFICATION/409, replacing the initial Increment 4 DynamicUpdate/column-scoped proposal. The original planning prompt/proposal remain historical in the playbook. The complete ADR remains Proposed pending explicit approval; implementation requires its own authorization.

Later 2026-10-14: Human finalized the exact boxed Long/@Column mapping, BIGINT/final NOT NULL/existing-row 0/no final default policy and no-update/no-increment sequential deactivation. These supersede the earlier candidate mapping/default questions; the initial proposal left these choices Open and considered a zero backfill/default. That earlier proposal is historical, not current policy.

Remaining: explicit implementation authorization and runtime verification of the reviewed V2 proposal; actual provider/newness/version/commit-cause behavior and deterministic race/no-op verification. Production deployment/execution permissions/rollout and Order Placement protocol remain Open/separate. The complete ADR is still Proposed pending explicit separate acceptance. No code, dependency, migration, database operation or commit is performed by this record.

## Reviewed V2 Implementation Proposal — 2026-10-14

The human confirmed the schema policy and requested the following reviewed proposal for V2__add_product_customer_versions.sql. It is not a created/executed migration; runtime verification remains Pending and the complete ADR remains Proposed.

```sql
ALTER TABLE products
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE customers
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE products
    ALTER COLUMN version DROP DEFAULT;

ALTER TABLE customers
    ALTER COLUMN version DROP DEFAULT;
```

The temporary constant default supplies 0 for existing rows; final defaults are removed and Hibernate owns new versions. Explicit coding authorization permits creating V2 and disposable Testcontainers verification. Persistent local execution additionally requires identified local target/history/schema evidence and an established usable recovery mechanism under the [current implementation prompt](../04-agentic-coding/prompts/05-product-customer-api.md#persistent-local-database-safety-gate). If recovery evidence is unavailable, report only local migration as Blocked — Human Action Required and continue other approved work; complete ADR acceptance remains separate from coding authorization. No migration operation is authorized by this documentation task; preserve local records/volume and V1. Production deployment/permissions/rollout remain excluded/Open. Existing direct SQL fixtures must use explicit columns/version values, and current history expectations must cover V1+V2 without weakening prior constraint/checksum tests. [Current implementation prompt](../04-agentic-coding/prompts/05-product-customer-api.md#current-implementation-prompt--increment-4) owns the gated execution sequence.
