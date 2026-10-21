# Hexagonal Architecture Tests

The implemented [pragmatic hexagonal architecture](../02-architecture/system-architecture.md#implemented-ports-and-adapters) follows Accepted [ADR-0007 Option B](../03-decisions/ADR-0007-hexagonal-architecture-migration.md) and retains [ADR-0004](../03-decisions/ADR-0004-persistence-mapping.md). CH-003 is complete. ArchUnit enforces actual production classes, rather than treating the architecture as a future target.

## Current enforcement

| Suite | Enforced boundary |
| --- | --- |
| [ArchitectureComplianceTest](../../src/test/java/com/example/agenticorderservice/architecture/ArchitectureComplianceTest.java) | Production domain/feature rules and exact adapter-to-application contract allowances; ten executions in the recorded migration evidence |
| [HexagonalArchitectureRulesTest](../../src/test/java/com/example/agenticorderservice/architecture/HexagonalArchitectureRulesTest.java) | Shared rule behavior against valid, invalid and empty-scope fixtures, plus real domain checks; JPA annotations/enum metadata allowed, JPA runtime access forbidden |
| [ProductGetArchitectureTest](../../src/test/java/com/example/agenticorderservice/architecture/ProductGetArchitectureTest.java) and [ProductWriteArchitectureTest](../../src/test/java/com/example/agenticorderservice/architecture/ProductWriteArchitectureTest.java) | Populated Product slices, controller input delegation, application/adapter boundaries and exact Product write-aggregate exception |
| [CustomerArchitectureTest](../../src/test/java/com/example/agenticorderservice/architecture/CustomerArchitectureTest.java) | Operations/facts, read/write/reference adapters, controller calls and exact Customer write-aggregate exception |
| [ProductImageArchitectureTest](../../src/test/java/com/example/agenticorderservice/architecture/ProductImageArchitectureTest.java) | Input plus four output capabilities, infrastructure-neutral image contracts and technical adapters |
| [OrderReadArchitectureTest](../../src/test/java/com/example/agenticorderservice/architecture/OrderReadArchitectureTest.java) | Immutable stored snapshots, Customer input-capability bridge and read controller/adapter boundaries |
| [OrderPlacementArchitectureTest](../../src/test/java/com/example/agenticorderservice/architecture/OrderPlacementArchitectureTest.java) | Placement input/four outputs, provider bridges, transaction adapter and exact Order save/flush exception |
| [HexagonalConsolidationArchitectureTest](../../src/test/java/com/example/agenticorderservice/architecture/HexagonalConsolidationArchitectureTest.java) | Whole-production application/controller/adapter/provider boundaries, real port implementations, recursive signatures and constructor cycles |

The [shared rules](../../src/test/java/com/example/agenticorderservice/architecture/HexagonalArchitectureRules.java) reject concrete persistence, HTTP, storage, integration, transaction, SQL, filesystem and ImageIO dependencies from application orchestration. Production imports exclude test fixtures and require populated scopes, preventing empty-package success. Negative fixtures prove rejection of representative bypasses, nested entity/HTTP/infrastructure leaks, aggregate-save lookalikes and constructor cycles.

Exact exceptions retain domain JPA metadata, the three approved same-feature managed-write ports, supported static price/email validation calls and Customer commit observation. No input/read/cross-feature entity allowance or blanket feature exclusion follows. Provider capabilities may point in opposite feature directions; component constructor wiring must remain acyclic. Intentional Order/OrderItem associations are not constructor cycles.

ArchUnit checks compile-time structure. It cannot prove commit outcomes, rollback/retry safety, SQL lock order, lazy/query behavior, image cleanup or HTTP equivalence. Unit/application/MVC tests, real PostgreSQL/Testcontainers tests, Spring context checks and runtime OpenAPI comparisons remain necessary. Constructor graph checks complement Spring startup; they do not prove every bean-factory dependency.

## Running the checks

Select Java 21, then run:

```sh
# All architecture-package checks; clean removes stale compiled fixtures/classes.
./mvnw -B -ntp '-Dtest=com.example.agenticorderservice.architecture.*Test' -DfailIfNoTests=true clean test
# Complete fast suite, including all architecture tests and Spotless.
make verify-fast
```

The wrapper's console Architecture count refers specifically to ArchitectureComplianceTest; other architecture suites contribute to the overall fast count. The recorded final migration/hardening evidence contains 88 architecture executions. This documentation update did not rerun them. See [engineering verification](engineering-verification.md) for complete non-PIT and PIT-enabled commands and [quality status](quality-report.md#current-architecture-and-quality-status--2026-10-21) for evidence provenance.

## Initial enforcement and per-increment activation history

The following headings preserve earlier links. They summarize historical activation stages, not inactive or unmigrated production scope. Detailed authorization, manifests and dated results remain in the [CH-003 records](../04-agentic-coding/change-requests/CH-003-hexagonal-architecture-migration/README.md).

## Per-slice activation convention

Production manifests name actual ports, implementations and adapters, exclude tests and reject empty selections. Slice checks remain active alongside the consolidation suite. Future changes must preserve precise contract allowances and negative fixtures rather than relaxing rules globally.

## Increment 1 activation — Product GET-by-ID

Activated GetProductQuery/GetProductService/FindProductById/ProductReadAdapter and method-scoped controller GET checks. Other Product methods migrated in later increments.

## Increment 2 activation — Product writes

Activated ProductWriteOperations/ProductWriteService/ProductWriteStore/ProductWriteAdapter, including the approved same-feature Product aggregate signatures.

## Increment 3 activation — Customer operations and facts

Activated Customer operations/facts, managed-write exception and reference bridge checks. The later consolidation replaced the transitional concrete Order provider dependency with OrderReferences.

## Increment 4 — Product image capabilities

Activated ProductImageOperations, four output capabilities and processing/storage/metadata/transaction adapter checks, with infrastructure-neutral payload signatures.

## Increment 5 — Existing Order reads

Activated OrderQueries/OrderQueryService, immutable OrderReadStore snapshots and the consumer-to-provider Customer bridge.

## Increment 6 — Order placement

Activated placement input/output capabilities, exact OrderWriteStore save/flush signatures, provider bridges and transaction-adapter dependency checks. Behavioral tests retain completion registration, original failure identity and UNKNOWN/rollback/commit distinctions.

## Increment 7 — complete migrated scope enforcement

Completed Product list/locked-fact and Order reference paths and activated the consolidation suite across all migrated production boundaries. No migrated rule is intentionally inactive. Final regression, mutation assessment and later human-reported Cloud results are separate evidence, not proof supplied by architecture tests alone.
