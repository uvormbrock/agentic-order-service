# ADR-0007: Hexagonal Architecture Migration

**Status:** Accepted — Option B, explicitly approved by the human on 2026-10-21.
**Recorded:** 2026-10-21
**Authorization at Increment 0:** Human-approved Option B and Increment 0 quality-automation follow-up, 2026-10-21. Architecture acceptance did not itself authorize Increment 1 or production migration.

**Implementation context — 2026-10-21:** The human has since confirmed CH-003 completion. See the [implemented architecture](../02-architecture/system-architecture.md#implemented-ports-and-adapters) for current classes and [migration history](../02-architecture/hexagonal-migration.md) for the subsequent increments. The initial context/enforcement text below records the original migration decision; later accepted scoped contracts follow it. Option B, ADR-0004 and all decision statuses remain unchanged; this documentation update does not accept historical mutation debt or unresolved complete-ADR details.

## Context and unchanged authority

At the initial Increment 0 assessment, the application was a one-module, feature-oriented layered modular monolith. [ADR-0003](ADR-0003-application-architecture.md) accepts application-owned orchestration and narrow feature contracts. [ADR-0004](ADR-0004-persistence-mapping.md) deliberately accepts directly mapped JPA aggregates. [ADR-0005](ADR-0005-technology-stack.md) owns the stack. Full ADR-0002 and ADR-0006 statuses are unchanged; scoped accepted transaction/version directions remain binding. See [transaction conventions](../02-architecture/transaction-locking.md).

The installed [hexagonal skill](../../.codex/skills/hexagonal-architecture/SKILL.md) recommends a framework-independent domain, inward dependencies and incremental port extraction. It supplies guidance, not authority to replace accepted decisions. Initial assessment favored strict separation; the Increment 0 comparison recommended the pragmatic option for the current requirements. The human subsequently explicitly accepted Option B. This records architectural acceptance only; no mapping or production code is changed.

## Options

| Dimension | A — Strict separation | B — Pragmatic ports and adapters |
| --- | --- | --- |
| Domain | Framework-independent; separate JPA entities and explicit mapping | Keep approved JPA metadata on selected aggregates; no repository, HTTP or I/O calls in domain |
| Benefits | Strong ORM isolation; independently evolvable persistence representation; pure-domain checks | Smaller reversible increments; retains tested dirty checking, generated identity and version behavior; less mapping duplication |
| Cost | Rehydration/state/version contracts, duplicate representations and substantial persistence-test adaptation | Explicit, permanent JPA exception; domain remains coupled to ORM metadata and hydration constraints |
| Main risks | Snapshot recalculation, lost version checks, incorrect merge/newness, association/cascade drift and lock behavior changes | Uncontrolled expansion of metadata exception into infrastructure calls; falsely claiming full framework independence |
| Schema/data | No schema change required by the design; prove identical mapping, constraints and data interpretation | Existing schema and mapping remain; ports isolate infrastructure operations |
| Existing decisions | Requires explicit supersession of ADR-0004 for each migrated scope | Retains ADR-0004; changes application dependency direction under a newly accepted migration contract |
| Skill implications | Matches the skill's framework-free domain target | Deliberate documented adaptation of that target; do not claim strict hexagonal conformance |

## Accepted decision — Option B

The current requirements specify one deployment/database, direct aggregates, PostgreSQL concurrency, historical snapshots and small maintainable changes. No requirement calls for replacing the ORM or independent domain deployment. Introduce application-owned ports and explicit adapters incrementally while retaining the narrowly accepted JPA metadata. This addresses concrete service-to-infrastructure coupling without the immediate mapping risk of A. Revisit A only when a demonstrated requirement justifies its additional contracts and tests.

Option A remains an unselected alternative. Any future change to A requires separate approval of identity assignment, domain rehydration, optimistic-version tokens, managed update strategy and snapshot hydration before splitting an aggregate. Preserve reference equality, generated UUID timing, lazy fetch plans, root-owned items and all existing SQL constraints; never introduce blind detached merges or recalculate stored totals. No schema migration is implied by either selection.

## Accepted target and conventions

Follow [target boundaries and increments](../02-architecture/hexagonal-migration.md). Keep Customer, Product and Order features, shared Money and one Maven/Spring deployment. Input ports describe capabilities with immutable transport-neutral data. Output ports belong to consuming application code and express persistence, cross-feature access, image processing/storage or transaction-completion capabilities. Concrete adapters implement them; only composition code selects implementations. Introduce no universal repository/port framework.

Existing api/persistence/storage names can remain semantic adapter packages. application.port.in/out are the selected convention for new capability interfaces; these packages did not yet exist at Increment 0 and this does not mandate renaming existing adapters. For the chosen B, domain may depend on Java, domain values and required jakarta.persistence metadata only. This accepted exception does not allow repository calls, SQL or HTTP coupling. ADR-0004 remains valid and is not superseded. Application orchestration must not depend on concrete Spring Data/JDBC/filesystem/ImageIO adapters.

Cross-feature calls use consumer output ports bridged to provider input contracts, returning immutable facts; no entity graphs or foreign repositories. Helpers join the outer transaction. Opposite feature references do not justify recursive calls, constructor cycles or independently committed helpers.

Transaction scope stays application-owned; Spring infrastructure supplies read/write decorators or a narrow attempt-execution port. Preserve ambient rejection, READ COMMITTED, deterministic locks, transaction-scoped contexts, complete materialization and return only after successful commit. Retry only after confirmed full rollback, outside attempts; preserve provenance and unknown-outcome behavior. The full details of ADR-0002 are not accepted by this proposal. Image staging/metadata/cleanup are not a distributed transaction: preserve staged generations on transaction failure and quiescent-only reconciliation.

## Enforcement and acceptance

ArchUnit remains executable verification of approved documents, not a decision maker. Existing rules stay unchanged in Increment 0. Later adapters may implement application output ports, so the current blanket persistence-to-application ban must become a precise contract-versus-implementation rule after approval. Do not exclude whole features to hide violations. Introduce target rules per named migrated slice with positive population checks; retain legacy checks for remaining code.

Human decisions still required: specific port signatures and transaction-completion contracts; implementation authorization for each increment; any dependency/schema exception; final acceptance. Architecture acceptance, skill guidance, green tests and hook results supply none of those approvals. Existing layered paths remain until explicitly migrated; do not claim all current classes already satisfy the target. Do not begin Increment 1 under this documentation/configuration authorization.

## Decision history

2026-10-21: ADR created Proposed under initial Increment 0. The strict and pragmatic alternatives above remain the recorded comparison. Later the same day, the human explicitly accepted Option B and the retained three-specialist team, authorized local automated verification configuration/validation, and reserved SonarQube Cloud analysis/gate evaluation and all Git publication actions for personal control. ADR-0004 is unchanged. Human Acceptance of the resulting implementation/configuration remains Pending; implementation migration remains separately gated.

## Increment 2 scoped output contract — Accepted 2026-10-21

For explicitly approved Product create/update/PATCH deactivation migration, the human approved one narrow exception to managed-entity-free port signatures: application-owned `ProductWriteStore.save(Product)` and `findLockedById(UUID): Optional<Product>` may carry the existing same-feature Product aggregate within the application transaction. This preserves application-owned aggregate mutation, managed dirty checking, generated identity and existing pessimistic/optimistic behavior under ADR-0004. It does not authorize entity exposure through input ports, HTTP, GET or cross-feature contracts, or any other output port. Exact scoped ArchUnit rules must enforce this exception; the general prohibition remains. No separate persistence entities, schema change or later increment is approved. Human Acceptance of implementation remains Pending.


## Increment 3 scoped output contract — Accepted 2026-10-21

The human explicitly approved `CustomerWriteStore.save(Customer)`, `findLockedById(UUID): Optional<Customer>` and `delete(Customer)` as a narrowly scoped same-feature managed-aggregate output exception. These operations execute within existing application transactions through a Customer persistence adapter, preserving application-owned mutation, managed dirty checking, locking, rollback, generated identity and optimistic versions under ADR-0004. No separate JPA entities are introduced. Customer input, read-result and cross-feature contracts remain immutable and entity-free. Exact scoped architecture rules enforce this exception; the earlier Product exception remains unchanged and neither exception extends to other ports. Customer and Order behavior remain unchanged. Human Acceptance Pending.


## Increment 6 scoped Order persistence and attempt contracts — Accepted 2026-10-21

Following the mandatory read-only Phase A safety gate,the human explicitly approved `OrderWriteStore.save(Order): Order` and `flush(): void` as an Order-only same-feature managed-aggregate output exception. Application constructs the existing Order/OrderItem aggregate; the persistence adapter delegates existing save/flush within the same placement transaction,retaining generated identities,cascade/managed semantics and unchanged ADR-0004 mappings. No independent transaction/commit,EntityManager or Spring Data exposure. Flush is not commit. Input/read/cross-feature contracts stay entity-free. Earlier Product/Customer exceptions remain unchanged; none grants a general entity-bearing port allowance. Exact Order signature/dependency architecture checks must enforce this exception.

The human also explicitly approved the [Increment 6 Phase A attempt contract](../04-agentic-coding/change-requests/CH-003-hexagonal-architecture-migration/01-documentation.md#increment-6-phase-a-bounded-migration-contract): application-owned attempt-local UNKNOWN/COMMITTED/ROLLED_BACK completion and exact lock-call failure identity; narrow synchronous `OrderPlacementAttempts` capability for unbound-entry checking,existing transaction execution and unchanged eligible-failure classification. The adapter uses existing qualified REQUIRED/READ COMMITTED TransactionTemplate,registers completion observation before SET LOCAL2s lock timeout,propagates original exceptions and returns only after successful completion/resource cleanup. No exception implies rollback; exceptional completion other than confirmed rollback is never replayed.

Application retains Customer-before-canonical-Product lock order,aggregate construction/save/flush/materialization inside the attempt,retry eligibility invocation only after rollback+unbound check,three-attempt/10s scheduling budget,25/50ms backoff outside attempts,fresh provider reads and safe commit-before-success logging. Existing retry graph classifier and error/HTTP semantics are preserved. No generic exception-based retry,asynchronous work,new propagation or additional commit. Approved Phase B authorization resolves only recorded INC6-A1/A2; full ADR-0002 is not accepted. No new schema,dependencies,pricing,mappings,Order reads or later increment. Human Acceptance of implementation remains Pending.
