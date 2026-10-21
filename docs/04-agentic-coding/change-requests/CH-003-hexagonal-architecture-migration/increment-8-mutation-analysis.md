# Increment 8 mutation-quality assessment

**Date:** 2026-10-21 Europe/Berlin. **Current quality disposition:** INC8-R1/R2 resolved by authorized targeted test hardening below; overall acceptance BLOCKED pending required Cloud gate and historical debt disposition. The initial full-run assessment below is historical; production unchanged. **Human Acceptance Pending.**

## Initial full-run assessment — historical evidence

The following results and findings describe the original 1,050-mutant run. Current R1/R2 resolution and the separate 68-mutant verification are recorded in the targeted follow-up section.

## Execution, population and counting

Java 21 `make verify-full SCOPE=all` completed exit0 in408.9s. Its unchanged `make mutation-test SCOPE=all` preflight executed777 fast tests (zero failures/errors/skips), validated XML and then ran PIT1.30.1/DEFAULTS/one thread with the existing junit plugin. PIT exit0, finished17:31:48CEST,4:57. No assertion/selection/configuration/threshold changes. PostgreSQL *IT are outside existing PIT selection and were separately verified by full Failsafe.

| Status | Historical baseline | Final |
| --- | ---: | ---: |
| KILLED | 746 | 800 |
| SURVIVED | 212 | 213 |
| NO_COVERAGE | 37 | 31 |
| TIMED_OUT | 6 | 6 |
| Total | 1001 | 1050 |

All other final status counts zero. Strict assertion kills/generated:800/1050=76.19%. Detection including timeouts:806/1050=76.76%. Strict kills/covered mutants:800/1019=78.51%. Baseline strict746/1001=74.53%, detection752/1001=75.12%. Timeouts are not assertion-based kills. No configured numeric threshold; changed aggregate score proves neither improvement nor regression.

Mutated class population grows65→82:22new class names,5retired names. Changes include new read/write/bridge/transaction adapters and extracted attempt/image carriers; moved guard/retry classifier/image processor account for retired names. Interfaces with no executable mutatable bytecode are not missing mutants. This is a changed code/mutant population, not49 directly comparable extra requirements.

Tester comparison uses explicit class/package/attempt descriptor lineage and extracted-method semantics, then groups class/method/JVM descriptor/operator/description. Locations and index/block metadata support exact identity when stable; line numbers are not assumed stable across extraction. Duplicate identities are compared as status multisets, not arbitrarily paired. **944 baseline/final mutants trace to matching groups:939 retain status, five unique changes.** The155duplicate groups retain identical status multisets; individual identity within a repeated group is inconclusive. Remaining57old mutants (54K/3NC) retire and106new mutants (105K/1S) appear. This population decomposition reconciles1001→1050; a method extraction is not automatically a quality change.

Five traceable status changes: three NO_COVERAGE→KILLED, Customer.getId SURVIVED→KILLED, Product facade validatePrice KILLED→SURVIVED. Exact identities and former killing test are preserved in tester comparison JSON. Reviewer independently traced all250final nonkills:248retain historical same-status lineage and two are the actionable new/worsened gaps below. No newly uncovered final mutation identified. The six fewer uncovered mutants reflect three newly killed traceable mutants and three retired uncovered sites; retirement alone proves no coverage improvement.

## Risk-ranked findings and disposition

| ID / severity | Exact evidence | Impact / recommended bounded action | Acceptance effect |
| --- | --- | --- | --- |
| INC8-R1 / MEDIUM | OrderPlacementCoordinator.lockCustomer line110, VoidMethodCallMutator removal of PlacementAttempt.recordLockCallFailure SURVIVED | New extraction-specific mutatable call; former direct field assignment has no equivalent call-removal operator. Omitting provenance means Customer-origin55P03 lock timeout after confirmed rollback is not eligible for retry. Fast Product-origin coverage does not prove Customer origin. Existing OrderPlacementConcurrencyIT.localLockTimeoutExhaustsThreeFreshAttemptsWithoutRows (lines313–354) parameterizes customers/products and passes real PostgreSQL retry assertions; PIT excludes *IT. This is a fast-suite/PIT-selector gap, not absent overall behavioral coverage. Add bounded fast Customer-origin provenance/classification assertions only after approval. | Open, requires explicit human disposition/follow-up; no demonstrated production defect or unsafe replay. Meaningful safety-adjacent availability assertion gap. |
| INC8-R2 / LOW | ProductApplicationService.validatePrice(BigDecimal):Money line53, NullReturnValsMutator; baseline line62 KILLED by createsNormalizedActiveResource, final SURVIVED | Real writes moved to ProductWriteService; HTTP decoder line258 discards compatibility helper return. Retained public Money return now lacks assertion. Add a direct valid-result delegate assertion only after approval. | Genuine traceable test-quality regression, low risk; human disposition required. Current validation still executes correctly. |
| HIST-TX / MEDIUM review debt | Coordinator place boundary65, rejectionReason170/172, diagnostic math127/138; OrderRetryPolicy.FailureGraph visit49/postgresLockTimeout107/122 | Historical surviving/uncovered identities. Retry loop boundary candidate is low value because final-attempt guard throws; math sites affect diagnostic attempt metadata. Reflection-unavailable122 is uncovered. Do not blanket-label equivalent; retain targeted retry/error/logging assertions and real PostgreSQL schedules; deeper mutation disposition remains human review debt. | No new critical regression demonstrated; not automatically accepted debt. |
| HIST-IMAGE / MEDIUM review debt | ProductImageStorage ensureRoot123/125/126, read61/66,reconcile93; ProductImageProcessor process21/22/44/51/58/62/65 | Historical same-status path/size/decode/graphics survivors. Security-sensitive path checks need explicit review; rendering-hint/dispose mutations are low-value candidates, not proven equivalent. Existing fast and real image integration evidence remains complementary. | Retain for explicit human risk disposition; no new image-staging/cleanup regression identified. |
| HIST-VALIDATION / MEDIUM review debt | Customer EmailAddress constructor18/lambda13; converters11/16; CustomerOrderAccess lock24/current29 | Historical validation/provider uncovered paths, affected by fast-only mutation selector. Database converter/participation behavior separately exercised by PostgreSQL tests. | Historical selector/assertion limitation; no inferred equivalence or automatic acceptance. |
| HIST-API / MEDIUM review debt | Shared HTTP/OpenAPI/error/logging nonkills in ledger | Existing OpenAPI configuration dominates survivors; response/error/lifecycle checks remain separate. Raw accessor/annotation configuration mutants can be low-value, but no blanket equivalence claim. | Historical operational/assertion debt; Cloud gate remains independently Pending. |

Historical pricing/discount/aggregate validation, transaction-completion adapter and attempt-state mutations introduce no newly surviving/uncovered defect in this comparison. Full regression establishes covered commit/rollback/unknown/no-replay, locking/fresh facts, historical monetary values and image failure retention scenarios; PIT fast-only selection cannot establish real database safety. No unresolved critical production regression was demonstrated. This assessment does not silently accept historical survivors.

## Timeouts and technical outcomes

All six final timeouts have the same historical identities:

- ApiExceptionHandler.classify58 — conditional removal.
- RequestCorrelationContext.lambda$wrap$0:28 — Runnable.run removal.
- RequestCorrelationContext.wrap34 — null Callable return.
- CorrelatedTaskExecutor.execute16 — delegate execute removal.
- CorrelatedTaskExecutor.submit21 and submit26 — null Future returns.

These affect asynchronous scheduling/error paths and retain timeout status; no assertion-based kill is claimed. Historical Increment1's extra RequestCorrelationFilter async timeout returned KILLED in this final run; no root cause or stability guarantee follows. PIT completed with no RUN_ERROR/MEMORY_ERROR/other technical failure status. Thread-count warnings appear in shared embedded-server tests and remain technical stability debt; no rerun was performed to turn outcomes green.

## Complete evidence

[Complete non-killed identity ledger](increment-8-mutation-ledger.json) preserves all250entries with class, method descriptor, location, operator, description,index/block metadata and status. It is a disposition index, not250approved exceptions. Raw final XML/HTML, preflight and PIT logs/commands/exits are under `/tmp/CH-003-increment8/verification/agentic-verify-full-y9c03p7g/mutation/PIT-all-20261021T152634Z-k2BfBG/`.

Historical XML/HTML: `/tmp/ch003-quality-followup/agentic-verify-full-vk4eczlx/mutation/PIT-all-20261021T120702Z-Ka0J6S/pit-reports/`. Tester full comparison `/tmp/CH-003-increment8-tester/mutation-lineage-comparison.json` plus lineage limitations; independent reviewer `/tmp/CH-003-increment8-review/nonkill-trace.json`. Temporary evidence may disappear; durable counting/method/findings/ledger are retained here and in the four standard records.

## Human decision

Recommend a bounded test-only follow-up for Customer-origin lock provenance and the retained Product validator return, with affected verification and independent re-evaluation under separate human approval. No mass test additions, production refactor or automatic PIT rerun is authorized. Human must also dispose historical mutation debt and supply required matching Cloud evidence before an unconditional acceptance recommendation.

**Human Acceptance: Pending.**

## Final non-killed population by class

| Class | Survived | Uncovered | Timed out |
| --- | ---: | ---: | ---: |
| `configuration.ApiWebConfiguration` | 1 | 0 | 0 |
| `configuration.ApiWebConfiguration$MissingBodyAdvice` | 1 | 0 | 0 |
| `configuration.DisabledOpenApiRoutes$1` | 1 | 0 | 0 |
| `configuration.JacksonConfiguration` | 1 | 0 | 0 |
| `configuration.OpenApiConfiguration` | 86 | 1 | 0 |
| `configuration.OrderTransactionConfiguration` | 0 | 3 | 0 |
| `configuration.RequestLoggingConfiguration` | 5 | 0 | 0 |
| `customer.application.CustomerOrderAccess` | 0 | 2 | 0 |
| `customer.domain.EmailAddress` | 2 | 0 | 0 |
| `customer.persistence.EmailAddressConverter` | 0 | 4 | 0 |
| `order.application.OrderFailure` | 1 | 1 | 0 |
| `order.application.OrderPlacementCoordinator` | 6 | 0 | 0 |
| `order.application.OrderResult` | 1 | 0 | 0 |
| `order.application.PlaceOrderCommand` | 1 | 0 | 0 |
| `order.domain.Order` | 1 | 0 | 0 |
| `order.domain.OrderItem` | 1 | 0 | 0 |
| `order.transaction.OrderRetryPolicy$FailureGraph` | 2 | 1 | 0 |
| `product.api.ProductImageController` | 4 | 1 | 0 |
| `product.application.ProcessedImage` | 3 | 0 | 0 |
| `product.application.ProductApplicationService` | 1 | 0 | 0 |
| `product.application.ProductImageContent` | 3 | 0 | 0 |
| `product.domain.Product` | 1 | 0 | 0 |
| `product.storage.ProductImageProcessor` | 8 | 0 | 0 |
| `product.storage.ProductImageStorage` | 8 | 0 | 0 |
| `shared.api.ApiErrorController` | 3 | 0 | 0 |
| `shared.api.ApiErrorResponseWriter` | 1 | 0 | 0 |
| `shared.api.ApiExceptionHandler` | 8 | 7 | 1 |
| `shared.api.ApiRequestInterceptor` | 19 | 1 | 0 |
| `shared.api.CorrelatedTaskExecutor` | 0 | 0 | 3 |
| `shared.api.HttpFailureDiagnostic` | 4 | 0 | 0 |
| `shared.api.RequestCorrelationContext` | 0 | 0 | 2 |
| `shared.api.RequestCorrelationContext$Scope` | 1 | 0 | 0 |
| `shared.api.RequestCorrelationFilter` | 1 | 4 | 0 |
| `shared.api.RequestCorrelationFilter$ObservedRequest` | 1 | 0 | 0 |
| `shared.api.RequestCorrelationFilter$Observer` | 0 | 1 | 0 |
| `shared.api.RequestLogState` | 8 | 1 | 0 |
| `shared.api.RequestLoggingListener` | 6 | 0 | 0 |
| `shared.api.StrictJsonRequestDecoder` | 14 | 0 | 0 |
| `shared.api.StrictRequestHttpMessageConverter` | 5 | 0 | 0 |
| `shared.domain.Money` | 4 | 0 | 0 |
| `shared.persistence.MoneyConverter` | 0 | 4 | 0 |


## Authorized targeted test-hardening follow-up — 2026-10-21

**Current disposition:** INC8-R1 and INC8-R2 were demonstrated, non-equivalent test gaps. Both are now **resolved by deterministic regression tests and exact targeted PIT kills**. No production code changed. The original full-run findings, 1,050-mutant results and 250-entry ledger remain historical evidence; they have not been rewritten as a fresh global result.

### Tests and mutation disposition

| Finding | Exact test change | Observed disposition |
| --- | --- | --- |
| INC8-R1, originally MEDIUM | `OrderPlacementCoordinatorTest.exhaustionReturnsUnavailableAndNeverSaves(boolean customerLock)` retains the original Product case and adds a Customer case. It preserves UNAVAILABLE, three rollbacks and no Order persistence assertions; additionally checks three begins, zero commits, 25/50 ms virtual backoff, three Customer lookups and no Product access on Customer failure. | Coordinator line 110, VoidMethodCallMutator, index 17/block 2: SURVIVED → KILLED by Customer parameter invocation 2. Losing provenance changes eligible 55P03 retry behavior; this mutation is not equivalent. Existing real Customer/Product lock-timeout tests also pass. |
| INC8-R2, originally LOW | New `ProductApplicationServiceTest.priceValidatorCompatibilityMethodReturnsValidatedMoney()` compares `validatePrice(new BigDecimal("12.3"))` with `Money.of("12.30")`. Record equality asserts a nonnull amount and EUR currency. | Facade line 53, NullReturnValsMutator, index 5/block 1: SURVIVED → KILLED by the new test. The retained public Money return is a contract; a null return is not equivalent. |

Only these two existing test files changed. Existing assertions and behavior were preserved; no new class, dependency, production configuration, ArchUnit rule, mapping, API or schema change.

### Verification

| Check | Result |
| --- | --- |
| Focused unit and architecture tests | Passed 132 executions: 39 coordinator, five Product application and 88 architecture checks; zero failures/errors/skips. |
| Relevant PostgreSQL tests | Passed both Customer/Product parameter cases of `OrderPlacementConcurrencyIT.localLockTimeoutExhaustsThreeFreshAttemptsWithoutRows`; zero failures/errors/skips. |
| Focused Maven and Spotless | `verify` exit 0 at 19:42:56 CEST, 34.546 seconds; Spotless passed at validate. |
| Complete fast verification | `make verify-fast` exit 0, 26.1 seconds; 779 tests, including all 88 architecture checks; zero failures/errors/skips; Spotless passed. |
| Narrow PIT | Exit 0 at 19:44:04 CEST, 7.610 seconds; 68 mutants = 63 KILLED + five SURVIVED; no uncovered, timed-out or other outcomes. Strict kill rate 63/68 = 92.65%, for this scope only. |
| Independent review | Existing `/root/reviewer` independently matched the test delta, 232 current build inputs, test results and exact mutation identities; no new follow-up finding. |

Commands and environment are retained in `02-implementation.md`. Java 21 and the existing headless/Mockito-agent environment match previous successful runs. Builds were serialized; local Docker/socket permission was used for unchanged PostgreSQL/HTTP checks. Evidence root: `/tmp/CH-003-increment8-hardening/`. Focused reports and summary, `test-hardening.patch`, fast archive `fast/agentic-verify-fast-8u01rji3/`, PIT HTML/XML, commands/logs/exits and `mutation-comparison.json` remain available there.

The first focused attempt failed Spotless before tests because the new assertion needed line wrapping. Only that wrapping was corrected; the failed log/exit remain preserved. The copied focused archive also contains historical PIT-suffixed XML; `focused-summary.json` explicitly excludes those filenames, so stale/duplicate results are not counted.

### Scope, remaining risks and readiness

The existing `mutation-order` profile supports exact `targetClasses`/`targetTests` overrides. The Customer-lock provenance code belongs to **OrderPlacementCoordinator**, so the narrow run selected that class plus **ProductApplicationService**, using their two fast test classes. No package-wide run, full `SCOPE=all` run or plugin/configuration change occurred.

All 68 production mutation identities match the same two classes in the previous full run, including class, method descriptor, operator, line, index, block and description. The comparison is **61 killed/seven survived → 63 killed/five survived**, with exactly R1/R2 changing. The five remaining survivors are identical historical coordinator sites: `place` line 65, diagnostic math at 127/138 and `rejectionReason` at 170/172. They remain unaccepted review debt; no blanket equivalence or risk acceptance is inferred.

Broader historical mutations and timeouts were not re-executed. The narrow 68-mutant result is not a fresh global mutation score. Earlier full regression/JaCoCo/full PIT results retain their original source/test provenance; this follow-up provides current 779-fast, two PostgreSQL and scoped PIT evidence. No fresh full-regression or JaCoCo coverage claim is made.

**Readiness:** The bounded follow-up is ready for human review; both targeted gaps are technically resolved. The overall migration recommendation remains **BLOCKED** pending required human-controlled Cloud evidence and explicit historical mutation/debt disposition. Cloud findings remain Unknown; hook activation and human acceptance remain Pending. No Git mutation, Cloud action or hook activation occurred.

**Human Acceptance: Pending.**


### Targeted follow-up final Git and content audit

Final audit: exactly **ten existing files** changed since follow-up intake (two tests and eight documentation files; exact list in the inventory). No files added/deleted; **393 other intake files unchanged**, including every production/configuration input and unrelated cumulative change. All **232 current fast-build inputs match** their verification hashes. No source changes occurred after verification.

Branch `develop`, HEAD `3d53fdf7a74d6cd96b1a658bb37c47613b347b89` unchanged; staged diff **0 bytes**. Cumulative status remains **173 paths: 49 modified, seven previously deleted, 117 untracked**. Those seven deletions predate this follow-up; none was introduced here. No staging, unstaging, commit, push, merge, rebase, reset, restore, clean, stash, branch switch, Cloud action or hook activation.

`git diff --check` exit 0. Audit of changed Markdown checked **64 relative references and 30 local heading anchors**, with zero errors. Machine evidence `/tmp/CH-003-increment8-hardening/final-audit.json`; expanded status `/tmp/CH-003-increment8-hardening/final-status.txt`. No additional builds were needed after documentation-only consolidation.

**Both targeted findings resolved; bounded follow-up ready for human review. Overall migration recommendation: BLOCKED pending required Cloud evidence and historical debt disposition. Human Acceptance: Pending.**
