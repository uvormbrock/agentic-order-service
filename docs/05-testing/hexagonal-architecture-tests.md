# Hexagonal Architecture Tests

[ADR-0007](../03-decisions/ADR-0007-hexagonal-architecture-migration.md) and the [migration boundaries](../02-architecture/hexagonal-migration.md) define the target. Option B deliberately retains JPA metadata on domain aggregates. The installed skill does not supersede that decision.

## Initial enforcement and per-increment activation history

[ArchitectureComplianceTest](../../src/test/java/com/example/agenticorderservice/architecture/ArchitectureComplianceTest.java) continues to check the real production classes with ten executed checks. Increment 1 precisely replaces the persistence restriction for ProductReadAdapter, permitting its exact output interface and immutable ProductResult. Increment 2 adds the exact ProductWriteAdapter/ProductWriteStore contract guard; Increment 3 adds exact Customer read/write/reference adapter exceptions constrained by CustomerArchitectureTest; all other persistence classes retain the legacy restriction. Four additional production-domain checks in HexagonalArchitectureRulesTest narrow the JPA exception to annotations and enum metadata: EntityManager and other JPA runtime types are forbidden.

[HexagonalArchitectureRules](../../src/test/java/com/example/agenticorderservice/architecture/HexagonalArchitectureRules.java) supplies reusable checks for explicitly migrated scopes:

- Domain dependencies: own domain, shared domain, Java and JPA annotation/enum metadata only; no JPA runtime access.
- Application dependencies: no concrete persistence, HTTP, storage, SQL, filesystem or ImageIO infrastructure.
- Input/output port packages: interfaces rather than concrete implementations.
- Port dependencies: no managed entities, HTTP, Spring Data or other listed infrastructure types, including generic signature dependencies. Exact slice checks enforce the explicitly human-approved ProductWriteStore/Product and CustomerWriteStore/Customer same-feature output exceptions; other input/output rules remain.
- HTTP adapters: no direct aggregate, persistence or storage access.
- Outbound adapters: no dependency on the explicitly identified migrated use-case implementation; application data contracts remain allowed.

[HexagonalArchitectureRulesTest](../../src/test/java/com/example/agenticorderservice/architecture/HexagonalArchitectureRulesTest.java) tests the checks against isolated compiled Java fixtures outside the production namespace. Six valid scenarios preserve the JPA exception and inward dependencies; thirteen deliberately invalid scenarios prove detection with class names; six empty-scope scenarios prove that missing packages cannot silently pass. These 25 fixture checks validate rule behavior; four further checks run against the real Customer, Product, Order and shared domain classes. They do **not** certify that the current application has migrated.

## Per-slice activation convention

Product GET-by-ID is the first registered production slice under the explicit Increment 1 authorization. ProductGetArchitectureTest imports its six-class manifest, checks implementation relationships, applies the relevant rules and guards public port data signatures plus controller GET delegation. Product writes are activated separately below; collection reads and other features remain legacy scopes. A migration must add a production-facing test for its reviewed class manifest, importing production classes with `DO_NOT_INCLUDE_TESTS`, and call the applicable rule factories against that populated scope. Keep each rule's default empty-selection failure. Verify the manifest includes the input port, output port, use-case implementation and both adapters; assert each expected class is imported. Do not merely scan for whatever port packages happen to exist.

Apply checks only to the explicitly migrated classes while retaining the legacy checks on unmigrated code. Supply the concrete use-case class to `outboundAdaptersDoNotAccessUseCase`. Add a precise inbound adapter-to-input-port rule once the slice's actual port, implementation and DTO contracts are reviewed. Replace the legacy persistence-to-application prohibition for migrated adapters with output-contract enforcement as approved in ADR-0007; do not delete it globally or add blanket exceptions.

The factories are an initial rule set, not a complete architecture specification. Port implementation/wiring, cross-feature bridges and transitive DTO/entity leakage need slice-specific checks once the actual contracts exist. The existing feature-boundary tests remain active. Do not impose a blanket feature-cycle prohibition: accepted cross-feature contracts can reference each other. ArchUnit cannot prove transaction propagation, commit outcomes, lazy-loading behavior or REST equivalence; retain behavioral, wiring and PostgreSQL regression tests.

## Running the checks

With Java 21 selected:

```sh
./mvnw -B -ntp -Dtest=ArchitectureComplianceTest,HexagonalArchitectureRulesTest,ProductGetArchitectureTest -DfailIfNoTests=true test
make verify-fast
```

The new test class runs automatically in the existing fast suite. The console's Architecture count currently refers to the production-facing `ArchitectureComplianceTest`; the rule self-tests contribute to the overall fast-test count.

The [official ArchUnit guide](https://www.archunit.org/userguide/html/000_Index.html) documents dependency checks, empty-selection failures and onion architecture. Explicit dependency rules are used here because the project's accepted JPA exception and incremental migration require more precise scope than a global textbook onion rule.

## Increment 1 activation — Product GET-by-ID

The [CH-003 contract](../04-agentic-coding/change-requests/CH-003-hexagonal-architecture-migration/01-documentation.md#increment-1--product-get-by-id-authorization-and-contract) authorizes GetProductQuery, GetProductService, FindProductById and ProductReadAdapter, reusing ProductResult and the existing ProductController. The controller mixes migrated GET and legacy operations, so GET delegation is checked at method scope. It must call the input interface and must not also access the legacy service/repository/concrete use case or adapter. Adapter dependencies on the application layer are restricted to the exact output interface and result record in ArchitectureComplianceTest.

Public port signatures are inspected through generic arguments and record components to reject entity and HTTP/provider leakage. The existing ProductResult.from(Product) mapping helper is not part of the returned data shape and remains unchanged. Negative cases exercise legacy/repository/concrete bypasses and entity/HTTP signatures. The four domain checks retain the accepted JPA metadata exception. Product GET transaction ownership and DTO materialization are exercised on PostgreSQL by GetProductQueryIT and the unchanged ProductApiIT; runtime OpenAPI compatibility remains checked by OpenApiRuntimeIT.

## Increment 2 activation — Product writes

The human approved ProductWriteOperations (create/update/deactivate), ProductWriteService, ProductWriteStore and ProductWriteAdapter. ProductWriteStore alone may expose the existing Product aggregate under the [scoped ADR-0007 decision](../03-decisions/ADR-0007-hexagonal-architecture-migration.md#increment-2-scoped-output-contract--accepted-2026-10-21). Input ports remain immutable-result contracts; entity exposure through GET, HTTP and cross-feature contracts stays prohibited. The migrated controller methods must delegate through the write input interface; collection listing remains legacy. Exact adapter output-contract checks replace only the migrated adapter's legacy prohibition.

ProductWriteArchitectureTest must prove populated production scope, implementation relationships, method delegation, application isolation, exact aggregate exception and representative invalid dependencies. Existing GET and rule-fixture checks remain active. Behavioral suites preserve validation precedence, unsupported DELETE, inactive/idempotent writes, optimistic conflict translation, committed version/dirty checking and Order/image compatibility; PostgreSQL and unchanged OpenApiRuntimeIT are mandatory before acceptance.


## Increment 3 activation — Customer operations and facts

CustomerArchitectureTest imports a positive thirteen-class production manifest covering controller, existing application service/provider, five ports, three adapters, immutable result and unchanged Customer aggregate. It checks application/domain/HTTP isolation, implemented contracts and every actual controller operation's CustomerOperations call. Ordinary ports retain the general entity/infrastructure prohibition; only CustomerWriteStore permits the exact three approved aggregate signatures, with an exact dependency whitelist. The general rule intentionally rejects the full slice containing that approved exception, proving it has not been loosened globally.

Precise application dependency allowlists constrain CustomerReadAdapter (read contract, result and not-found failure), CustomerWriteAdapter (write contract) and CustomerOrderReferenceAdapter (consumer contract and transitional independent OrderReferenceQuery). Existing ArchitectureComplianceTest excludes only these exact migrated adapters from its legacy application ban. CustomerOrderAccess remains MANDATORY and implements CustomerFacts; existing Order consumers retain the compatibility class until their migration.

Negative cases prove rejection of concrete/repository controller bypass even with an input port present, application-to-repository access, an unapproved Customer entity port, and adapter-to-use-case coupling. Existing empty-scope rule self-tests, Option B metadata checks and Product enforcement stay active. CustomerAdaptersTest checks materialization/immutability/query delegation; CustomerTransactionParticipationIT verifies MANDATORY rejection including empty reads, shared persistence context and rollback of a flushed deletion. Existing HTTP precedence, uniqueness, optimistic conflicts, both delete/first-order schedules and runtime OpenAPI tests remain required. Actual counts and gate status belong to CH-003 records.


## Increment 4 — Product image capabilities

`ProductImageArchitectureTest` imports an explicit populated image production manifest: controller, application service, five ports, processing/storage/metadata/transaction adapters and application values/limits. It verifies all five implementations and the service's four output dependencies. Image ports remain entity-free, including nested generic/record signatures; no managed-entity exception is added.

The migrated scope activates existing application, port, API and adapter dependency rules, with additional image-specific AWT and Spring transaction isolation. All four image HTTP handlers must invoke `ProductImageOperations`, and controller dependencies are limited to that input port and neutral content/failure/limits. Representative forbidden controller bypass, repository/filesystem/ImageIO/AWT/storage/transaction access, managed entity or infrastructure signatures, and adapter-to-use-case dependencies must fail the same relevant checks. Legacy Product, Customer and Order rules remain active; only the exact image metadata adapter receives the necessary contract/value/not-found allowance under the persistence check.

Image regression tests preserve original validation, binary/HTTP behavior, complete file lifecycle, metadata round trips and locking. Additional cases characterize candidate retention on failed first upload, deletion uncertainty, staging failure before database access, managed metadata identity/all-field projection, transaction completion/guards and orchestration with neutral port doubles. Actual execution status and counts belong to [CH-003 evidence](../04-agentic-coding/change-requests/CH-003-hexagonal-architecture-migration/02-implementation.md); test existence alone does not establish Passed.


## Increment 5 — Existing Order reads

`OrderReadArchitectureTest` imports a populated ten-class production manifest: Order controller, query service, input/output contracts, snapshot/result types, persistence adapter and Customer bridge. It verifies three implemented ports, use-case dependencies on both output capabilities, and bridge dependency on existing provider input `CustomerFacts`. Existing class-level readOnly transactions remain application-owned. Strict read rules are not applied to placement/retry/reference components.

Both GET handlers must call `OrderQueries` without concrete query-service/repository/adapter bypasses; the existing placement dependency remains outside the read-method rule. Application checks reject persistence/integration and direct Customer-provider bypasses; persistence and integration adapters have exact application dependency allowlists, including an explicit foreign-persistence ban for the bridge. Port signatures and nested record/generic payloads reject entities, HTTP, SQL and other infrastructure types. Negative fixtures prove concrete-controller bypass even with an input-port call, direct provider access, adapter-to-use-case coupling, foreign persistence and nested entity/HTTP/infrastructure leaks.

The legacy persistence ban exempts only exact `OrderReadAdapter`, protected by its contract/snapshot/item allowlist; no blanket Order exclusion or managed-entity output exception is added. Existing Product/Customer/image/legacy Order and Option B domain rules remain active. Regression covers direct stored monetary copies, missing/empty reads, current inactive Customer batching, exact get/list query counts, multi-root/item fetches, readOnly and ambient read-write transaction participation, and JSON serialization after transaction closure. Actual evidence belongs to [CH-003](../04-agentic-coding/change-requests/CH-003-hexagonal-architecture-migration/02-implementation.md).


## Increment 6 — Order placement

`OrderPlacementArchitectureTest` imports a populated13-class placement manifest and verifies implementation of placement input,four outputs and ProductFacts provider input. Controller delegates placement through OrderPlacementOperations; existing OrderQueries reads remain. Coordinator dependency allowlist permits domain/neutral application contracts and rejects concrete persistence,integration,transaction,JDBC,Spring Data and transaction-runtime dependencies.

Ordinary port signatures/nested generic/record payloads are infrastructure/entity-free. Only exact OrderWriteStore.save(Order):Order is exempt under the human-approved ADR-0007 exception; reflection enforces exactly save plus zero-argument void flush. Lookalike save/input/nested entity leakage must fail. OrderWriteAdapter gets only its exact output contract; provider bridges get exact consuming/provider inputs and immutable facts; transaction adapter gets exact attempt/result contracts. Negative fixtures detect concrete-controller bypass,application infrastructure/provider access and adapter-to-use-case coupling. Existing persistence rule exempts only exact OrderWriteAdapter with a narrow contract allowlist; all earlier Product/Customer/image/Order-read/domain rules remain active.

Transaction adapter tests preserve registration-before-timeout,commit/cleanup-before-return,original callback/setup/commit failure identity,confirmed rollback,missing callback/begin failure and unrecognized completion UNKNOWN. Relocated retry classifier retains all existing SQL-graph cases. Existing coordinator/real PostgreSQL concurrency and async logging tests retain original rollback/unknown-outcome,lock ordering,timeout,retry budgets,atomicity and diagnostic assertions. Fresh-facts characterization passed before extraction and runs afterward,checking later committed provider facts/distinct contexts and one persisted aggregate. Actual counts/source identity and limitations belong to [CH-003](../04-agentic-coding/change-requests/CH-003-hexagonal-architecture-migration/02-implementation.md).


## Increment 7 — complete migrated scope enforcement

Increment 7 is explicitly authorized and closes the remaining Product list/locked-fact and Order reference repository paths. The new architecture-wide suite supplements every existing populated slice suite; their exact write-aggregate signature restrictions and forbidden-dependency fixtures remain active. It imports actual production packages with tests excluded and checks positive population of applications, ports, controllers and adapters. The generic application rule also names integration and transaction adapters, preventing concrete wiring dependencies from escaping through those packages.

Target checks include input-port controller delegation, application output boundaries, adapter-to-contract dependencies, provider-input cross-feature communication, infrastructure-neutral transitive port data signatures, exact three approved managed-write exceptions, and constructor wiring cycles with negative fixtures. Intentional aggregate associations and provider contracts in opposite feature directions do not justify a blanket class/feature-cycle prohibition. Existing static validation compatibility calls and Customer completion observation are explicitly retained behavior; no blanket feature exclusions.

[HexagonalConsolidationArchitectureTest](../../src/test/java/com/example/agenticorderservice/architecture/HexagonalConsolidationArchitectureTest.java) activates whole-production application/domain/API/port scopes; real input/output implementation coverage; all four controller input constructors and endpoint calls; persistence/storage/integration/transaction adapter boundaries; provider capability access; recursive generic/record/type-variable signatures with exact approved aggregate exceptions; the narrow existing Customer completion observation; exact shared static price/email validation compatibility; and interface-resolved component constructor-cycle detection. Negative fixtures exercise concrete controller/adapter/foreign provider access, transaction-runtime access, nested entities, HTTP signatures, aggregate-save lookalikes, generic arrays and cyclic constructors. Additional generic application-rule fixtures reject concrete integration/transaction dependencies.

The component constructor graph complements runtime context checks; it does not prove every possible Spring bean-factory dependency. All existing slice checks remain active. No migrated application/port/controller/adapter rule is intentionally left inactive; transaction annotations and the exact documented completion/validator/aggregate exceptions remain bounded exceptions rather than unexamined excluded features. Commands and actual execution results belong to [CH-003 Increment 7 evidence](../04-agentic-coding/change-requests/CH-003-hexagonal-architecture-migration/02-implementation.md). Architecture rules cannot prove transaction outcomes, retry safety, lazy query behavior, image cleanup or REST equivalence; focused/full PostgreSQL and runtime OpenAPI verification remain required. Human Acceptance Pending; PIT Deferred to Increment 8, which is not authorized here.
