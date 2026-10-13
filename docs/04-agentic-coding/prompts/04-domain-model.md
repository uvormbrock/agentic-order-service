# Domain Model & Persistence – Increment 3

**Status:** Planning Reviewed – Implementation Pending Human Approval

Historical planning is executed; Variant A for D1–D10 is explicitly Accepted. Increment 3 implementation/tests have not started. Concrete SQL review and runtime evidence remain Pending; deferred production/list/concurrency matters remain unresolved.

## Objective

Prepare one coherent Domain Model & Persistence increment applying Accepted D1–D10, with one complete implementation prompt awaiting final execution approval.

## Context and Approved Scope

Increments 1 and 2 are accepted and committed. This task authorizes documentation updates recording Accepted D1–D10 and aligning their affected references. It authorizes no implementation, migration execution, database/container operation, dependency change or Git commit. ADR-0002 and unrelated decisions remain unchanged; preserve unrelated work.

Follow [AGENTS.md](../../../AGENTS.md), the [PRD](../../01-requirements/product-requirements.md), [domain model](../../02-architecture/domain-model.md), [ADR-0004](../../03-decisions/ADR-0004-persistence-mapping.md), [ADR-0005](../../03-decisions/ADR-0005-technology-stack.md), [coding guidelines](../coding-guidelines.md), [Java/Spring guidelines](../java-spring-guidelines.md), [persistence guidelines](../persistence-guidelines.md), [open items](../../00-overview/open-items.md), [implementation plan](../../07-implementation/implementation-plan.md), and [testing strategy](../../05-testing/testing-strategy.md). The [playbook index](00-index.md) uses the same title/status.

Increment 3 remains one development increment. Internal sequencing and human review gates do not create separate increments or phase-specific prompt files. Accepted requirements and direct aggregate mapping remain unchanged; ADR-0002 stays Proposed and implementation-unverified.

## Implementation Prompt – Increment 3 (Pending Human Approval)

This is the single implementation prompt. It has not been executed or authorized by this documentation task. After human approval and execution, preserve its text verbatim and append actual execution evidence below.

```text
# Task: Implement Increment 3 – Domain Model & Persistence

Execute only after explicit human approval of this implementation prompt. D1–D10 are already Accepted; handle only genuinely unresolved requirements through the stop conditions in section 2. This is one development increment with internal sequencing; do not create independent sub-increments or additional phase-specific prompt-playbook files.

1. Read and establish authority

Read AGENTS.md, docs/01-requirements/product-requirements.md, docs/02-architecture/system-architecture.md, domain-model.md, order-item-snapshots.md, technology-stack.md, transaction-locking.md, all five ADRs under docs/03-decisions/, docs/04-agentic-coding/coding-guidelines.md, java-spring-guidelines.md, persistence-guidelines.md, docs/05-testing/testing-strategy.md, docs/07-implementation/implementation-plan.md, docs/00-overview/open-items.md, and this Increment 3 record/index.

Inspect current Git status, source/configuration and existing tests. Preserve unrelated work, the immutable historical planning prompt, and prior verification evidence. Follow the numbered documentation structure and English conventions. Apply the latest explicit approved PRD/domain decisions; report stale summaries without silently rewriting ADR history.

Retain Java 21, Spring Boot 4.1.1, Maven Wrapper 3.9.9, JUnit 6 with Boot-managed Jupiter/Platform, PostgreSQL 17.11, and the official image defined in config/postgres.properties. Reuse existing Spring Data JPA/Hibernate/JDBC/Flyway and Testcontainers/Failsafe infrastructure; no new dependency or version override is approved.

2. Confirm Accepted decisions and execution authorization

D1–D10 Variant A has been explicitly Accepted. Confirm the current PRD/domain model, ADR-0004 mapping selections and ADR-0005 local/test migration policy; do not seek repeat approval for UUID generation, item identity, autoApply converters, owned relationships/cascades, single-order fetch strategy, unconstrained NUMERIC, name/email conventions, selected schema/constraints/indexes, reference equality or local/test Flyway policy.

This prompt still requires separate explicit human execution approval. Review concrete SQL columns/check/index declarations against those Accepted policies and the PRD before recording them as final. Do not invent extra business restrictions. If the documents conflict or a genuine missing requirement, unselected setting (such as a required collation change), unsupported mapping behavior or additional dependency is needed, stop the affected work and request explicit approval with evidence. Accepted choices are not reopened merely because implementation details need verification. Production migration execution/privileges and list/pagination fetching are deferred and outside this increment; do not infer their settings or permissions. ADR-0002 remains Proposed and unverified.

3. Implement approved domain behavior

Use feature-owned customer/product/order domain packages and shared/domain for Money, within com.example.agenticorderservice. Preserve direct aggregate mapping and ID-only cross-aggregate references. Customer, Product and Order are roots; OrderItem belongs only to Order, with no independent CRUD/repository. Keep domain independent of API/application/persistence components except accepted JPA metadata. No database calls from domain behavior.

Money: immutable BigDecimal amount/currency value, EUR-only. Reject null/invalid inputs and source scale greater than two, including 10.000. Normalize accepted values to scale two without rounding. Use exact arithmetic without float/double or an arbitrary universal maximum. Equality/hash treat 1, 1.0 and 1.00 identically. Reject incompatible currencies; permit negative intermediate arithmetic unless an accepted field invariant forbids it. Product owns its 0.00–999,999.99 price range. Order owns discount rounding.

EmailAddress: immutable normalized value. Strip with String.strip(), lowercase with Locale.ROOT, and count at most 254 normalized Unicode code points. Require exactly one @. Local part allows ASCII letters/digits/dots and punctuation !#$%&'*+-/=?^_`{|}~; no leading/trailing/consecutive dots. Domain requires at least two nonempty ASCII dot-separated labels of letters/digits with optional internal hyphens, never leading/trailing hyphens. Reject quoted/internationalized email content, including non-ASCII content before case conversion. Do not perform DNS lookups or introduce extra local-part/label length limits. Preserve dots/plus-tags literally; normalization deterministic/idempotent and equality/hash on normalized value. Case-insensitive uniqueness includes inactive customers; no lookup inside the value object.

Customer/Product: required fields, names normalized with String.strip() to 1–200 Unicode code points preserving case/umlauts/accents/valid Unicode, active creation, controlled valid updates and irreversible deactivation. Updating details never reactivates. Preserve identity and referenced records. Reference-aware customer deletion is later application work; do not implement deletion orchestration here.

Order: at least one item, customer ID and product ID references, positive integer quantities, duplicates consolidated by product ID with overflow-safe summation and maximum 10,000 after consolidation. Capture trusted original product names/unit prices and immutable quantity/lineTotal. Compute lineTotal = unitPrice × quantity; subtotal = sum(lineTotal); discount = 10% of subtotal when subtotal >= EUR 100.00, rounded HALF_UP to two places, otherwise zero; total = subtotal – discount. Store original amounts; reads never recalculate with current catalog prices or future rules. No arbitrary order-total/item-count limit. No mutable item collection, independent item mutation, or Order update/delete operation. Keep pricing inside domain behavior without a speculative service/interface.

Existing/active customer/product eligibility requires later application coordination; do not claim that domain construction or FKs alone implement it. A minimal immutable carrier of trusted product facts may be used only within reviewed aggregate construction; it is not an HTTP request accepting client prices.

4. Implement only approved persistence design

Map Customer/Product/Order and the owned OrderItem directly with JPA field access and protected no-argument constructors. Use @GeneratedValue(strategy = GenerationType.UUID) with Java UUID/PostgreSQL uuid and null IDs before persistence. No custom UUID generator, database UUID extension, BaseEntity or duplicate persistence model. Retain Java reference equality; do not override entity equals/hashCode or generate entity-wide toString. Compare persisted UUIDs explicitly; logging/object methods must not traverse lazy state or access the database.

Use Order.items @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY), orphanRemoval=false; OrderItem.order @ManyToOne(fetch = FetchType.LAZY, optional = false) owns the non-null order_items.order_id FK. Keep both sides consistent through Order operations. No CascadeType.REMOVE. Customer/Product references stay scalar UUID values with restrictive FKs, not managed cross-aggregate associations.

Create customers, products, orders and order_items in src/main/resources/db/migration/V1__create_order_domain_schema.sql under Accepted D8/D10 after this prompt receives execution authorization. Review concrete SQL against the PRD before recording final declarations. Preserve original product name/unitPrice/lineTotal and Order subtotal/discount/total, scalar customer/product references, approved non-null/FK/unique/check constraints, and justified indexes. Defend full-customer email uniqueness including inactive rows. No cascade deletion of historical orders from Customer/Product and no independent item repository.

Implement persistence-package MoneyConverter implements AttributeConverter<Money, BigDecimal> and EmailAddressConverter implements AttributeConverter<EmailAddress, String>, both @Converter(autoApply = true). Values remain immutable and JPA-annotation-free; no domain-to-persistence dependency. Verify converter discovery and exact hydration.

Use unconstrained PostgreSQL NUMERIC without fixed precision/scale for every monetary column. Named CHECK constraints reject NaN and positive/negative Infinity, scale greater than two and negative stored values where required by business invariants; product price stays 0.00–999,999.99. Do not silently substitute NUMERIC(19,2) or impose an artificial Order-total cap. Verify exact BigDecimal/Hibernate compatibility, EUR 9,999,999,900.00 lines and larger multi-product totals.

Use UUID PKs, required NOT NULLs, restrictive FKs without deletion cascades, named name/quantity/price/monetary checks and unique (order_id, product_id). Store normalized email with a unique index on lower(email) over all customers, including inactive ones; no partial/active-only uniqueness or citext. Keep enforcement outside EmailAddress and verify Java/PostgreSQL normalization/case/collation consistency. Add orders.customer_id and order_items.product_id indexes; avoid duplicate/speculative indexes. Do not encode cross-row totals/active eligibility/Order Placement coordination as unapproved schema features.

Use minimal feature-internal root repositories and avoid unnecessary exposed methods where practical. Do not add another repository layer solely to hide inherited Spring Data methods. Inherited technical persistence capabilities do not authorize application business operations: no independent OrderItem repository/CRUD or Order update/delete application operations. Explain the selected repository contracts, inherited capabilities and trade-offs in the implementation report. Use lazy Order items and an explicit single-order JOIN FETCH or equivalent reviewed fetch plan, such as findByIdWithItems(UUID id); no blanket EAGER. Defer list/pagination-specific plans. Materialize immutable test/read values within appropriate transaction boundaries; never serialize or expose managed entities. Do not add application retrieval operations or REST DTOs merely to demonstrate persistence.

Keep OSIV false, Hibernate ddl-auto=validate, SQL initialization disabled, and Flyway disabled by default/production. D10 permits V1 in existing project-specific local development and isolated PostgreSQL/Testcontainers databases after implementation execution approval; preserve existing data/volumes. Flyway owns DDL; applied scripts are immutable and later changes require additional versioned migrations. Verify history/checksums and repeated startup, with clean disabled. No production migration mechanism, automatic baseline/repair/clean, or database reinitialization. Preserve local credentials/volumes; never docker compose down -v.

5. Verify continuously within this one increment

Use Boot-managed JUnit 6 pure tests for Money, EmailAddress, Customer/Product lifecycle, Order consolidation, quantities, pricing and snapshot behavior. Cover null/invalid inputs, approved boundary limits, excess scale including trailing zeros, signed intermediate arithmetic, scale-independent equality/hashes and deterministic normalization. Cover EUR 99.99, 100.00 and 100.05; the latter gives discount 10.01 and total 90.04. Include maximum line and larger multi-product totals. Do not claim int-only methods prove fractional JSON rejection.

Use the existing isolated PostgreSQL/Testcontainers setup and central image file; no local .env or running Compose database dependency. Require Docker and report unavailable infrastructure as Blocked/failed, never a passing skipped gate. Verify migrations/history/checksums and Hibernate schema validation, converter/aggregate round trips after flush/clear, IDs/lifecycle, negative named FK/unique/check cases including inactive-customer email duplicates, numeric scale/capacity, no unintended deletion cascade, and original snapshots after product changes. Use disposable transactions for negative tests; do not reuse failed persistence contexts.

Verify approved lazy/fetch behavior and query counts with multiple orders/items. Capture immutable data inside the transaction and verify its use after context closure requires no SQL. Actual production DTO serialization remains Pending until later application/API implementation. Validate package dependencies and report manual review separately from automated enforcement; no new enforcement library.

Mandatory technical verification obligations (all Pending until actually executed):

- NUMERIC/Hibernate: inspect the actual migrated monetary column declarations to establish unconstrained PostgreSQL NUMERIC without fixed precision/scale. Prove Hibernate 7 mapping through MoneyConverter, autoApply discovery, successful ddl-auto=validate and exact BigDecimal/Money round trips after flush(), clear() and reload. Cover EUR 9,999,999,900.00 for a maximum line and EUR 19,999,999,800.00 for two distinct maximum-value lines; expected amounts must be independently calculated from PRD rules. Verify real PostgreSQL rejection of excess scale (including 10.000), NaN, positive/negative Infinity and invalid negative/range values under the relevant named checks. Do not substitute fixed precision, disable schema validation, round rejected inputs or add a monetary cap to obtain passing tests. If actual provider/database behavior reveals an incompatibility, report the evidence and obtain approval before changing the Accepted mapping strategy.
- Order/items: demonstrate domain ownership by Order and relational FK ownership by OrderItem.order, with both association ends synchronized. Persist only the root through the production persistence path; CascadeType.PERSIST must persist every child without separate item saves. Assert non-null order_items.order_id values, then flush(), clear() and reload to verify complete item membership, parent references and original values. Verify no unintended deletion cascade; retain the Accepted lazy bidirectional mapping, PERSIST-only cascade, orphanRemoval=false and mandatory ManyToOne. No independent OrderItem persistence path or application-level item repository is permitted.
- Independent test expectations: make each scenario traceable to PRD acceptance criteria/business rules and Accepted decisions, identifying expected values independently of generated implementation helpers. Include product price 0.00/999,999.99 versus -0.01/1,000,000.00/excess scale, consolidated quantity 10,000 versus 10,001, and the existing discount threshold/HALF_UP examples. Use direct database negative cases to prove constraints rather than only Java guards. Verify snapshot preservation after product changes, converter round trips, aggregate reloads, large calculations, and relevant lazy-loading/query counts after clearing first-level state.
- Failed persistence: after a controlled failure during aggregate persistence, establish confirmed rollback and inspect committed database state through a fresh transaction/context. Assert no root or owned-item rows from the failed attempt and preservation of existing rows; an expected exception alone is insufficient. Use test-harness transaction boundaries only, without implementing Order Placement coordination or the ADR-0002 protocol. Do not reuse a failed persistence context or claim rollback from an uncertain outcome.

Do not weaken assertions, remove meaningful tests, copy implementation calculations as the test oracle, or change Accepted business rules merely to obtain a green build. Report four separate evidence categories: passing domain unit tests (identify fast foundation/smoke results separately); passing PostgreSQL/Testcontainers tests; manually reviewed architecture/repository boundaries; and later-increment or unavailable verification still Pending. Manual review cannot replace integration evidence, and foundation tests cannot pass unimplemented domain/concurrency gates.

Update existing foundation assertions that currently require zero entities/migrations or no domain tables to match the approved schema. Preserve JDBC SELECT 1, actual server/image, disabled OSIV/SQL initialization, independent test database and fast Docker-free smoke responsibilities. Domain-schema expectations must be inspected without removing meaningful infrastructure checks.

Run from the repository root with command-local Java 21:
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp test
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp verify
Inspect dependency alignment; no new versions are authorized. Run git diff --check and git diff --cached --check, plus whitespace checks for new files, Markdown links, secret and scope checks.

6. Restrictions, reporting and human acceptance

Do not implement REST controllers/endpoints, Order Placement application orchestration, cross-feature business services, TransactionTemplate coordination/ambient guard, pessimistic locking, retries or the ADR-0002 transaction protocol. ADR-0002 stays Proposed and implementation-unverified; concurrency/deletion races remain Pending. No unapproved dependencies, production migration execution or autonomous Git commit.

Update Result/Verification Evidence/Problems Encountered/Lessons Learned/Interview Talking Points in this same playbook entry; preserve the original planning prompt and this implementation prompt verbatim once approved/executed. Record exact files, actual resolved versions/commands/results and failures separately from expectations. Update authoritative documents only within approved implementation/documentation scope; do not silently change Accepted decisions or ADR statuses.

Present implementation summary, complete staged/unstaged/new-file Git diff, test and database evidence, boundary review, remaining Open decisions/risks and Pending gates. Mark execution Executed – Pending Human Review; if blocked, record the blocker honestly. Agent self-review does not replace human review. Wait for explicit human acceptance before Increment 3 is complete; do not start the next increment or commit without separate explicit authorization.
```

## Design Decisions and Approval Prerequisites

The human reviewer explicitly accepted Variant A for every D1–D10 selection. Acceptance is not runtime verification or authorization to execute this prompt. Do not ask for repeat approval of selected policies; concrete SQL still needs review against those policies and genuinely new decisions require approval.

| Decision | Current status | Accepted selection | Remaining boundary |
| --- | --- | --- | --- |
| D1 — Root IDs/generation | Accepted | Java UUID/PostgreSQL uuid; JPA GenerationType.UUID for Customer/Product/Order; IDs null before persistence; no custom generator/extension/BaseEntity. | Lifecycle/newness verification Pending. |
| D2 — OrderItem identity | Accepted | JPA-generated UUID, unique (order_id, product_id); exclusively Order-owned. | No independent repository/CRUD/API identity; verification Pending. |
| D3 — Direct mapping/converters | Accepted | Field access/protected constructors/controlled mutations; JPA-free values; persistence-package Money→BigDecimal and EmailAddress→String converters with autoApply=true. | No domain-to-persistence dependency; discovery/hydration evidence Pending. |
| D4 — Relationships/cascades | Accepted | Lazy bidirectional Order/items; child-owned mandatory FK; cascade PERSIST only; orphanRemoval=false; scalar cross-aggregate UUIDs/restrictive FKs. | No REMOVE/deletion cascades; both-end/SQL evidence Pending. |
| D5 — Fetch plans | Accepted for Increment 3 | Lazy items; explicit single-order JOIN FETCH or equivalent; transaction materialization; OSIV off. | Query/context tests Pending; list/pagination fetching deferred. |
| D6 — Money/storage | Accepted | Existing Money semantics plus unconstrained NUMERIC; named finite/scale ≤2/invariant-specific non-negative checks; product range, no artificial total cap. | Concrete SQL review and exact PostgreSQL/Hibernate evidence Pending. |
| D7 — Email/names/SQL | Accepted | Names: String.strip(), 1–200 code points, preserve case/Unicode. Existing ASCII email policy; normalized storage, all-customer unique lower(email), no citext/partial uniqueness. | Java/PostgreSQL case/collation evidence Pending; no value-object lookup. |
| D8 — Schema/constraints/indexes | Accepted | Four tables, UUID PKs/NOT NULL/restrictive FKs/named approved checks/uniqueness; indexes on orders.customer_id and order_items.product_id; original snapshots/amounts. | Review concrete SQL without new restrictions; cross-row eligibility/totals remain application/domain responsibilities. |
| D9 — Entity equality/hash | Accepted | Reference identity; no entity equals/hashCode overrides or generated entity-wide toString; compare UUIDs explicitly; value equality for values. | Lifecycle/proxy/object-method evidence Pending. |
| D10 — Flyway policy/execution | Accepted for local/test | V1__create_order_domain_schema.sql; immutable applied scripts/new versions, Hibernate validate; no automatic baseline/repair/clean; preserve local data/volumes. | Prompt execution approval Pending; production execution/privileges Open. |

### Acceptance Boundaries and Material Risks

- **D1/D2:** Sequence-backed bigint and a composite child key were considered but generated UUIDs are now selected. Assigned UUIDs need a reviewed insertion/newness strategy to avoid unintended Spring Data merge behavior; do not introduce domain Persistable coupling. Generated IDs do not prove commit. No item repository/API identity or guaranteed item ordering is implied.
- **D3:** Persistence-package converters referenced from domain @Convert annotations create a forbidden dependency. Explicit orm.xml conversion is an alternative to autoApply; embeddables change value-object mapping. The Accepted autoApply selection explicitly replaces ADR-0004's earlier Proposed explicit-conversion preference; the ADR preserves that history. Verify discovery, EUR reconstruction, invalid/null handling, hydration and round trips; global conversion affects every attribute of the value type.
- **D4/D5:** A unidirectional join-column mapping is an alternative but can add FK updates; avoid an unnecessary join table. Verify both relationship ends, insert/rollback SQL, no deletion cascade, explicit fetching and query counts. Blanket EAGER loading is not an N+1 solution; collection-fetch pagination must be reviewed when actually needed.
- **D6:** The [accepted Money semantics](../../02-architecture/domain-model.md#4-value-objects) include null/invalid-input rejection, excess source scale including trailing zeros, normalization without rounding, scale-independent equality/hash, exact EUR arithmetic, incompatible-currency rejection, no universal cap, and permitted negative intermediates. Product owns its limits; Order owns threshold/discount/HALF_UP behavior. The earlier Java-only approval selected no SQL/converter; subsequent D3/D6 now accept autoApply conversion and unconstrained NUMERIC with reviewed checks. A line can reach EUR 9,999,999,900.00; multiple products can exceed it. Fixed NUMERIC(8,2)/(12,2)/(19,2) alternatives do not establish an approved overall order bound; constrained numerics can coerce excess scale before checks. Verify physical capacity, special values and Hibernate validation with the actual provider rather than implicit defaults.
- **D7:** The [PRD policy/examples](../../01-requirements/product-requirements.md#approved-email-validation-policy) own the exact Accepted ASCII grammar, String.strip()/Locale.ROOT normalization, 254 code-point limit, literal plus/dots, rejection of quoted/internationalized content, and uniqueness across active/inactive customers. No lookup belongs in EmailAddress. Name code-point counting/String.strip() is now Accepted; UTF-16 and grapheme counting were alternatives, not selected. Java/PostgreSQL whitespace/lowercase functions are not assumed equivalent; review SQL normalization/collation together. Active-only uniqueness violates Accepted requirements; citext is excluded.
- **D8:** Selected indexes are orders.customer_id and order_items.product_id; the order/product unique index already supports order-leading access. Avoid speculative/redundant indexes. FKs alone cannot enforce active eligibility, prohibit every product deletion, prove at least one item, or sum child totals. Domain construction and later atomic application orchestration cover those gaps. Named constraints support future safe SQLSTATE/error classification, not automatic mapping of every failure to a business conflict.
- **D9/D10:** Reference equality means separate objects/proxies for one row need not compare equal; ID-based proxy-safe equality is an alternative requiring evidence. Mutable fields/generated IDs must not destabilize hashes or trigger SQL through equality/logging. Flyway metadata initialization is not V1; D10 accepts local/test V1 policy, while actual execution still needs implementation authorization. Editing applied migrations, Hibernate-generated schema, automatic repair/clean and production execution are not approved.

Primary technical references used during the historical review: [Jakarta Persistence](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html), [Spring Data persistence](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html), [PostgreSQL 17 numeric types](https://www.postgresql.org/docs/17/datatype-numeric.html), and [Hibernate mapping guide](https://docs.hibernate.org/orm/7.4/userguide/html_single/). The live Hibernate guide described a later patch than the recorded 7.4.5.Final runtime; those references are design support, not runtime evidence or upgrade approval.

## Planning Results and Verification Evidence

### Completed Planning and Earlier Evidence

The read-only planning review ran on 2026-10-13 and progressed from Planned to Executed – Pending Human Review. It inspected all authoritative sources listed in the historical prompt, plus REST design, the index and Increment 2 evidence. It inspected pom.xml, wrapper properties, Compose/image configuration, the Boot entry point, base/local/test configuration and the four foundation test/configuration classes. The empty incorrectly named 04-domain-model.md.md was verified and removed. No implementation or database change occurred.

The later human review accepted D6 Java Money and D7 email validation. The documentation consolidation recorded them in the PRD/domain model/open-items index and replaced the current subdivision with one Increment 3 roadmap; it did not accept persistence proposals.

[Increment 2](03-postgresql-foundation.md) records **13 passing foundation tests**, not Increment 3 evidence: one smoke, nine image-configuration and three PostgreSQL integration tests. Recorded versions were Java 21, Boot 4.1.1, Maven 3.9.9, Hibernate 7.4.5.Final, JDBC 42.7.13, Flyway 12.4.0, Testcontainers 2.0.5 and JUnit Jupiter/Platform 6.0.3. No build/dependency/database verification was repeated during planning or this documentation task.

Current foundation configuration disables OSIV/SQL initialization, uses Hibernate validate and enables Flyway only locally/integration-test. Its zero-entity/migration and metadata-only assertions must evolve with the approved domain schema while preserving meaningful infrastructure checks. Testcontainers stays independent of local .env and Compose.

Historical documentation checks passed: original prompt integrity, 14 relative links, Markdown/whitespace, five unchanged ADR statuses and original two-file scope. Subsequent consolidation checks passed: one implementation prompt, no current subdivision labels, 236 internal targets/anchors, preserved requirement IDs/REST fields/calculation examples/diagram, whitespace and its authorized five-file scope. These are documentation evidence, not passing domain/persistence tests.

### Internal Sequence and Planned Files

Within this one increment, sequence values, aggregate behavior, approved schema, direct mappings, and persistence verification. Write relevant tests alongside each step. Confirm recorded decisions and obtain execution authorization; report genuinely missing requirements before affected work. No independent sub-increments or extra playbook entries.

All paths below are the prepared implementation file scope, not generated files. Java paths share base `src/main/java/com/example/agenticorderservice/`; test paths share `src/test/java/com/example/agenticorderservice/`.

| Planned Java package | Classes |
| --- | --- |
| shared/domain | Money.java |
| customer/domain | EmailAddress.java, Customer.java |
| product/domain | Product.java |
| order/domain | Order.java, OrderItem.java, OrderLineInput.java |
| shared/persistence | MoneyConverter.java |
| customer/persistence | EmailAddressConverter.java, CustomerRepository.java |
| product/persistence | ProductRepository.java |
| order/persistence | OrderRepository.java |

OrderLineInput is a minimal immutable carrier of trusted product facts for domain construction, not a REST request accepting caller prices. Its signature remains an implementation review detail; it adds no new business rule. Repositories stay feature-internal/minimal. Inherited Spring Data capabilities are technical, not permission for Order update/delete application operations; do not add layers solely to hide them. No independent OrderItem repository/CRUD/persistence path is permitted.

Planned test files: shared/domain/MoneyTest.java; customer/domain/EmailAddressTest.java and CustomerTest.java; product/domain/ProductTest.java; order/domain/OrderTest.java; persistence/DomainSchemaIT.java; customer/persistence/CustomerPersistenceIT.java; product/persistence/ProductPersistenceIT.java; order/persistence/OrderPersistenceIT.java. Expand those tests instead of adding redundant classes. JDBC fixtures may create foreign-feature rows without production access to foreign persistence packages.

Accepted migration path (not generated): `src/main/resources/db/migration/V1__create_order_domain_schema.sql`. Modify the same Customer/Product/Order/OrderItem classes for approved direct metadata/hydration and update existing PostgreSqlFoundationIT entity/migration/table expectations. D10 accepts local/test domain DDL policy; actual file generation/execution awaits implementation authorization.

The latest documentation task records D1–D10 in the PRD, domain model, ADR-0004/0005 and affected references, preserving decision history. Subsequent execution evidence belongs in this record and authoritative documents only within the approved scope. Keep prompt/execution/review evidence in this record; ADR-0002 receives no change.

### Verification Required During Implementation — Pending

- Pure JUnit 6 tests: Money source scale/equality/arithmetic, approved email normalization/grammar/boundaries, Customer/Product lifecycle and the Accepted code-point name limits, Order consolidation/overflow-safe quantity limits and pricing. Include EUR 99.99/100.00/100.05 and maximum line/larger order totals.
- PostgreSQL/Testcontainers: fresh/repeat migration history/checksums, Hibernate validation of actual entities, converters/aggregate round trips after flush/clear, inactive-inclusive email uniqueness, named FK/unique/check negative cases, special/excess-scale numeric values, IDs/lifecycle and no unintended cascades. Use disposable transactions and discard failed contexts; checksum failures must not trigger repair.
- Snapshot/fetch evidence: preserve original names/prices/amounts after catalog changes, load original results without current-rule recalculation, verify query counts over multiple orders/items, and capture immutable values inside a transaction for query-free use after closure.
- Boundary evidence: inspect imports/cycles and absence of API/application/persistence dependencies from domain except JPA metadata. No foreign-feature persistence access or managed entity exposure. Manual review is not automated enforcement; tooling remains Open.
- Application eligibility/current-customer response assembly, actual REST DTO serialization, fractional JSON rejection, transaction coordination, deletion races, locking/retries and uncertain-commit behavior remain later Pending gates. No persistence test substitutes for that evidence.

The implementation prompt contains the command-local Java 21 Maven test/verify and Git checks. Docker-unavailable integration execution is Blocked/failed, never Passed; no H2 substitute, dependency addition, production migration mechanism, local-volume deletion or database recreation is authorized.

### Accepted Design Update and Remaining Open Matters

The human reviewer explicitly selected Variant A for every D1–D10. ADR-0004 records the accepted mapping/storage/schema/equality choices and explicit replacement of the former converter preference; ADR-0005 records D10 local/test migration policy. No implementation or runtime verification occurred. Older Open statements in historical prompts/ADR proposal sections are preserved as history, not current gates.

Concrete SQL declarations/check expressions must be reviewed against the PRD/Accepted design before final recording. Case/collation and unconstrained NUMERIC compatibility require evidence; if a required setting/requirement is genuinely unselected, stop and request approval rather than infer it. List/pagination fetch details, production migration execution/privileges, deployment/security/enforcement tooling and detailed concurrency settings remain deferred/Open. No extra order-size/total restriction is introduced; ADR-0002 stays unchanged and unverified.

### Documentation Verification of D1–D10 Recording

Passed: D1–D10 table/authoritative decisions/prompt alignment; one implementation prompt with no obsolete repeat-approval checklist and retained genuine-gap stop conditions; verbatim historical planning prompt; 250 internal Markdown links/anchors and table structure; preserved requirement IDs, endpoints/response fields, calculation examples and unrelated rules; five unique ADR identifiers with ADR-0001/0002/0003 unchanged. Standard unstaged/staged whitespace and separate untracked-entry checks passed. No implementation, migration, dependency or runtime tests were executed. The already consistent index is unchanged; an unrelated Obsidian Git setting change is preserved outside this task's edits.

## Implementation Results (Pending)

- No Increment 3 Java classes have been generated.
- No Increment 3 domain or persistence tests have been executed.
- No domain Flyway migration has been created or run.
- Variant A for all D1–D10 is explicitly Accepted; implementation and runtime verification are not accepted by that decision.
- ADR-0002 remains Proposed and implementation-unverified.
- Java implementation requires separate explicit human execution authorization; ask only about genuinely unresolved requirements, not already Accepted D1–D10. Earlier foundation-test results do not pass Increment 3 gates.

## Lessons Learned

- Direct JPA mapping still needs package-boundary review: a converter annotation can introduce a forbidden dependency even when the converter itself is correctly placed in persistence.
- Two decimal places do not define storage capacity. An individual maximum line amount cannot establish a maximum order total.
- Passing zero-entity foundation tests proves infrastructure readiness, not domain mapping correctness; assertions must evolve with separately approved schema changes.
- Keep PostgreSQL verification alongside schema/mapping changes within the increment, and preserve approval boundaries for business grammar, schema, and concurrency.

## Interview Talking Points

- Explain the difference between aggregate ownership and the child-owned relational foreign key.
- Compare provider-generated UUIDs, composite child identity, and assigned-ID newness detection without introducing a generic entity framework.
- Discuss preserving immutable historical prices/amounts while reading current customer information.
- Explain why converter placement, numeric coercion, and Unicode normalization need explicit decisions and real provider/database tests.
- Demonstrate documentation-first approval, immutable prompts, narrow increments, actual diffs, and evidence that distinguishes verified infrastructure from Pending domain/concurrency work.

## Historical Planning Prompt – Executed

The following original, executed planning prompt is preserved verbatim as history; its earlier subdivision does not define the current implementation roadmap.

```text
# Task: Document and Plan Increment 3A – Domain Model & Persistence Design Review

Increment 1 (Technical Bootstrap) and Increment 2 (PostgreSQL Foundation) have been completed, reviewed, and committed.

We are now preparing Increment 3A – Domain Model & Persistence Design Review.

This task explicitly authorizes documentation and read-only planning. It does not authorize domain implementation, persistence mappings, Flyway migrations, or database changes.

## 1. Create the Prompt Playbook Entry

Create:

`docs/04-agentic-coding/prompts/04-domain-model.md`

Follow the established prompt-playbook format used by the previous increments.

Set the initial status to:

**Planned – Pending Human Approval**

Include:

- Status
- Objective
- Context
- Approved Scope
- Prompt
- Result
- Lessons Learned
- Interview Talking Points

Record this complete prompt verbatim as an immutable historical record before performing the design analysis.

Update:

`docs/04-agentic-coding/prompts/00-index.md`

If an empty `04-domain-model.md.md` file exists, verify that it contains no meaningful content before removing the incorrectly named duplicate. Do not delete files containing user work.

Do not create duplicate specifications.

## 2. Read Authoritative Documentation

Read:

- AGENTS.md
- docs/01-requirements/product-requirements.md
- docs/02-architecture/system-architecture.md
- docs/02-architecture/domain-model.md
- docs/02-architecture/order-item-snapshots.md
- docs/02-architecture/technology-stack.md
- docs/02-architecture/transaction-locking.md
- docs/03-decisions/ADR-0001-order-placement-concurrency.md
- docs/03-decisions/ADR-0002-transaction-locking.md
- docs/03-decisions/ADR-0003-application-architecture.md
- docs/03-decisions/ADR-0004-persistence-mapping.md
- docs/03-decisions/ADR-0005-technology-stack.md
- docs/04-agentic-coding/coding-guidelines.md
- docs/04-agentic-coding/java-spring-guidelines.md
- docs/04-agentic-coding/persistence-guidelines.md
- docs/05-testing/testing-strategy.md
- docs/07-implementation/implementation-plan.md
- docs/00-overview/open-items.md

Inspect the current project structure and existing source code.

Follow all Accepted decisions.

Do not silently resolve Open or Proposed choices.

## 3. Review the Domain Model

Analyze the existing requirements for:

- Customer
- Product
- Order
- OrderItem
- Money
- EmailAddress

Identify:

- Aggregate roots and ownership
- Entity identities and lifecycles
- Value objects
- Required fields and invariants
- State transitions
- Historical OrderItem snapshots
- Cross-aggregate references

Distinguish Accepted business requirements from Open or Proposed implementation decisions.

Do not redesign the existing domain model without explicit justification.

## 4. Persistence Design Review

Evaluate and propose concrete approaches for:

1. Entity identifier types and generation strategies.
2. OrderItem identity and lifecycle.
3. Direct JPA aggregate mapping.
4. Relationship ownership, cascades, and orphan removal.
5. Lazy loading and fetching strategies.
6. Money mapping and PostgreSQL precision/scale.
7. EmailAddress conversion, normalization, and uniqueness.
8. Database constraints and indexes.
9. Entity equality and hash-code behavior.
10. Flyway migration structure and versioning.

For every blocking Open or Proposed decision, provide:

- Existing documented constraints
- Recommended approach
- Relevant alternatives
- Technical rationale
- Trade-offs and risks
- Required verification

Do not treat recommendations as Accepted decisions.

Do not reopen already Accepted business rules.

## 5. Propose the Increment 3 Implementation Breakdown

Evaluate this proposed sequence:

- 3A: Domain and persistence design review
- 3B: Value objects and domain behavior
- 3C: Flyway schema and JPA mappings
- 3D: PostgreSQL persistence verification

Recommend narrower implementation boundaries if justified.

Identify dependencies, approval requirements, and measurable acceptance criteria.

Do not include REST endpoint implementation, Order Placement transaction coordination, pessimistic locking, retries, or concurrency implementation without separate authorization.

ADR-0002 remains Proposed and implementation-unverified.

## 6. Testing Strategy

Propose appropriate verification for:

- Money arithmetic and monetary boundaries
- Email normalization and uniqueness
- Aggregate invariants
- OrderItem snapshot behavior
- Flyway migration execution and Hibernate schema validation
- Persistence round trips
- PostgreSQL constraints, including negative cases
- Lazy loading, fetching, and DTO materialization

Use JUnit 6 and PostgreSQL/Testcontainers for persistence evidence.

Keep transaction protocol and concurrency gates Pending until their implementation decisions are approved.

Distinguish proposed tests from existing passing evidence.

## 7. Proposed Implementation Files

List the exact Java classes, packages, SQL migration files, tests, and documentation changes expected for the subsequent approved implementation increments.

Do not create these implementation files.

Do not introduce new dependencies or modify the Maven configuration.

## 8. Update the Prompt Playbook After Planning

After completing the read-only analysis, update:

`docs/04-agentic-coding/prompts/04-domain-model.md`

Record:

- Actual documents and source files inspected
- Accepted decisions identified
- Open and Proposed decisions requiring approval
- Recommended design approaches
- Proposed implementation breakdown
- Proposed files
- Verification and acceptance criteria
- Risks and architectural trade-offs
- Lessons Learned
- Interview Talking Points

Preserve the original prompt verbatim.

The status must accurately distinguish completed planning from implementation authorization. Use:

**Executed – Pending Human Review**

once the planning analysis is completed.

Do not mark the implementation as Accepted or completed.

## 9. Restrictions

Do not:

- Implement Java domain classes
- Implement JPA entities or repositories
- Create Flyway migration scripts
- Modify database schemas or data
- Start or modify database containers
- Change existing dependencies
- Implement REST endpoints or application business operations
- Implement transaction coordination, pessimistic locking, or retries
- Change Accepted ADRs
- Promote Proposed or Open decisions without explicit approval
- Begin Increment 3B, 3C, or 3D
- Create a Git commit

Only the new prompt-playbook entry, its index, and removal of a verified empty incorrectly named duplicate may be changed.

## 10. Completion Report

Present:

1. Current project state assessment
2. Accepted domain and persistence decisions
3. Open/Proposed decisions requiring approval
4. Recommendations with rationale and alternatives
5. Proposed Increment 3 breakdown
6. Proposed implementation files
7. Verification and acceptance criteria
8. Remaining risks
9. Actual documentation Git diff

Wait for explicit human review and approval before any implementation work.
```
