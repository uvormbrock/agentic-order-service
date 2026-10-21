# Increment 8 final migration acceptance report

**Date:** 2026-10-21, Europe/Berlin. **Recommendation:** BLOCKED pending required human-controlled Cloud evidence and historical debt disposition; targeted INC8-R1/R2 resolved in the authorized follow-up below. **Human Acceptance: Pending.**

## Initial final-verification assessment — historical evidence

The following original assessment predates the authorized test-only follow-up. Its commands, source fingerprints, counts and findings retain that original provenance; current resolution is recorded in the targeted follow-up section.

## Authority and source identity

Explicit human final-verification authorization (attachment `ca4c32d7-2641-491c-8e9a-6856552035c8/Eingefügter Text.txt`) covers verification, cumulative independent architecture review, PIT and documentation only. No remediation was performed. Target: [ADR-0007 Option B](../../../03-decisions/ADR-0007-hexagonal-architecture-migration.md), retaining [ADR-0004](../../../03-decisions/ADR-0004-persistence-mapping.md). Historical Increment 0–7 evidence and correction counters remain unchanged.

Branch `develop`; pre-migration HEAD `3d53fdf7a74d6cd96b1a658bb37c47613b347b89`. Staged diff empty. Complete baseline: `/tmp/CH-003-increment8/{status.txt,tracked.txt,staged.txt,intake-hashes.json}`. The 399-file intake includes all existing tracked/untracked regular-file contents, with the skill symlink separately inventoried. Final build fingerprint: 232 inputs in `verification/agentic-verify-full-y9c03p7g/source-sha256.json`, manifest SHA-256 `bf6a1ab26adaa604886aa42ee6a862ed151dc541f3d063ac87adf12bb9b5a4f7`; tester and reviewer independently matched all inputs to checkout.

The [complete cumulative inventory](increment-8-file-inventory.md) classifies all 169 intake paths: 166 CH-003, two pre-existing skill/discovery paths, one ambiguous empty untracked `rookie@MacBook` file. Preserve that file; human ownership clarification remains outstanding and does not itself demonstrate a source regression. Deleted profiles and moved implementation/test files have documented lineage; no unexplained deletion identified. No POM, CI, domain mappings, migrations or OpenAPI snapshot diff. Bounded private-key/AWS/GitHub-token signature scan found no matching changed file; this does not guarantee absence of every secret form.

## Migration-wide architecture assessment

The system is a feature-oriented modular monolith with pragmatic ports and adapters. HTTP controllers delegate to application-owned input capabilities; immutable commands/results keep HTTP mapping and error translation at the boundary. Product reads, writes/deactivation, Customer operations, Product images, Order reads and placement consume application-owned output contracts. Persistence, image processing/storage, cross-feature bridges and transaction infrastructure implement them. Spring component scanning/configuration and constructor injection supply composition.

| Area | Final dependency assessment |
| --- | --- |
| Product reads/writes | Input operations delegate to read/write use cases and narrow persistence capabilities. Legacy facade public/direct/static callers retain supported delegates. Validation, inactive lookup, identity timing, dirty checking and locking are preserved. |
| Customer | Application ports isolate read/write and Order-reference access; provider facts join caller transactions. Normalization, uniqueness and reference restrictions remain application/domain/database responsibilities. Commit observation retains the approved afterCommit exception. |
| Images | Entity-free metadata snapshots; ImageIO and filesystem code remain in storage adapters; transaction template isolated behind image transaction capability. Stage complete generations before metadata transaction. Clean old generations only after successful completion. Preserve candidates after **any** transaction failure, including confirmed rollback, as the approved existing behavior; uncertain outcomes are never treated as safe cleanup. Quiescent offline orphan reconciliation remains limited. |
| Order reads | Immutable historical snapshots preserve stored names/prices/discounts/totals. Customer enrichment uses current provider facts. Fetch plans/query counts and result materialization occur within read transaction. |
| Placement | Customer lock precedes canonical Product locks; attempt-local facts and aggregate construction/save/flush/materialization share the outer attempt. Completion observation precedes lock timeout setup. Success follows transaction completion; only confirmed rollback with exact eligible provenance may retry, with unbound checks, fresh reads and existing budgets/backoff. Unknown outcomes never replay. |
| Domain/shared | Customer/Product/Order/OrderItem retain responsibilities, Money/EmailAddress rules and direct mappings. Approved JPA metadata does not permit EntityManager/repository operations in domain. Existing HTTP/logging utilities stay at their technical boundary. |
| Cross-feature/composition | Consumer output capabilities bridge to provider input contracts; no foreign repository/entity graph exposure. No constructor cycles detected. Constructor graph cannot certify every factory-created Spring dependency. |

Approved exceptions are explicit: retained JPA metadata; Spring application stereotypes/transaction annotations and exact Customer commit observation; supported facade/provider/static validation compatibility calls; same-feature ProductWriteStore/Product, CustomerWriteStore/Customer and OrderWriteStore/Order aggregate signatures. These are bounded ADR-0007 adaptations, not defects or general allowances. No generic repository/framework abstraction or new dependency was introduced. Detailed fresh independent review is consolidated below after evidence completion.

## ArchUnit enforcement

All migrated scopes contain real production classes and are covered by slice and architecture-wide tests. Whole-production guards enforce application dependencies, port signatures and implementation relationships, controller delegation, adapter restrictions, approved aggregate exceptions, provider boundaries and constructor cycles. Negative fixtures reject infrastructure bypass, HTTP/nested/generic entity leakage and cycles; empty selections fail. Legacy rules remain active with exact adapter exceptions. No inactive migrated placeholder or blanket feature exclusion was identified.

| Suite | Executed checks |
| --- | ---: |
| ArchitectureComplianceTest | 10 |
| HexagonalArchitectureRulesTest | 29 |
| ProductGetArchitectureTest | 8 |
| ProductWriteArchitectureTest | 6 |
| CustomerArchitectureTest | 6 |
| ProductImageArchitectureTest | 5 |
| OrderReadArchitectureTest | 7 |
| OrderPlacementArchitectureTest | 6 |
| HexagonalConsolidationArchitectureTest | 11 |
| **Total** | **88** |

RulesTest comprises 25 fixture checks (six valid, 13 invalid, six empty scopes) and four real-domain checks; 59 other executions are production-facing architecture checks. The wrapper console labels only ArchitectureComplianceTest's ten checks; the total above comes from all nine XML suites. ArchUnit cannot establish runtime commit/locking/lazy-load/HTTP behavior or every factory-created dependency; behavioral/PostgreSQL/context tests supply separate evidence. Current documentation count wording corrected; historical evidence preserved.

## Regression, coverage and API evidence

Successful final Maven phase: Java 21, environment-only headless/explicit existing Mockito agent as in prior documented runs; `make verify-full SCOPE=all`, serialized. Wrapper evidence root `/tmp/CH-003-increment8/verification/agentic-verify-full-y9c03p7g/`; host console `/tmp/CH-003-increment8/full-host-console.log`. Maven `clean verify` exit 0, finished **17:26:34 CEST**, duration 1:33.

| Check | Actual result |
| --- | --- |
| Surefire | Passed: 777 executions / 56 suites, zero failures/errors/skips |
| Failsafe | Passed: 172 executions / 25 suites, zero failures/errors/skips; **160 PostgreSQL-backed +12 servlet lifecycle**, not 172 database-only checks |
| ArchUnit | Passed: 88 executions, included in 777 |
| Spotless | Passed at validate; no formatter apply/configuration change |
| OpenAPI | Passed: 13 fast checks (7 disabled +6 documentation) and one PostgreSQL-backed runtime check; runtime JSON/YAML/snapshot equality and Swagger configuration; no snapshot edit |
| JaCoCo lines | 1889 covered /1936 total; 47 missed; **97.57%** |
| JaCoCo branches | 942 covered /1021 total; 79 missed; **92.26%** |
| JaCoCo instructions | 8719 covered /8899 total; 180 missed; **97.98%** |

JaCoCo XML/HTML archived at `jacoco/`; POM defines report generation with no numeric check goal/minimum. Full coverage does not imply assertion quality. Meaningful remaining gaps include two coordinator lines/four branches, retry FailureGraph two lines, image processor one line/four branches and image storage two lines/six branches; shared HTTP error/lifecycle gaps also remain. Mutation disposition and real integration evidence must be considered separately.

Critical behavioral evidence includes Product/Customer validation/normalization/uniqueness/deletion/optimistic version tests and transaction-participation ITs; image binary/metadata/staging/failure-retention tests; OrderReadQueryIT stored monetary facts, query counts, read-only participation and serialization; placement/concurrency tests for actual committed state, lock order, rollback/fresh contexts/facts, SQLSTATE-specific classification, retry budget, uncertain commit/no replay and commit-before-success. Test execution proves the covered scenarios, not every possible failure schedule.

Initial sandbox attempt is retained as **Failed**: 777 fast executed, 15 context errors caused by local socket bind `Operation not permitted`, no assertion failure; integration/JaCoCo/PIT not reached. Evidence `verification/agentic-verify-full-4iczggp_` and `full-console.log`. Retry changed execution permissions only; reports were archived first. No production/test correction, threshold/selection change or correction iteration was consumed.

## PIT execution and quality disposition

Full wrapper exit **0**, duration **408.9 seconds**; PIT exit **0**, finished **17:31:48 CEST**, Maven PIT duration **4:57**. The required mutation preflight separately passed all 777 fast tests with zero failures/errors/skips (baseline and XML validation exit 0); it is not an additional independent suite. Final **1050 =800 KILLED +213 SURVIVED +31 NO_COVERAGE +6 TIMED_OUT**, all other statuses zero. Detailed population comparison and independent quality disposition follow. Baseline retained XML: `/tmp/ch003-quality-followup/agentic-verify-full-vk4eczlx/mutation/PIT-all-20261021T120702Z-Ka0J6S/pit-reports/mutations.xml`. Historical 1001 =746 KILLED +212 SURVIVED +37 NO_COVERAGE +6 TIMED_OUT. Final report belongs under `verification/agentic-verify-full-y9c03p7g/mutation/PIT-all-20261021T152634Z-k2BfBG/`. Strict assertion kill score is **800/1050 =76.19%**; detection including timeouts is **806/1050 =76.76%**; strict covered-population kill rate is **800/(1050−31) =78.51%**. Historical strict score is **746/1001 =74.53%**, detection **752/1001 =75.12%**. Scores use different populations and do not prove quality improvement. No score threshold is configured or invented; timeouts remain distinct from assertion kills. PostgreSQL *IT are excluded from existing PIT selection and remain separately verified above.

## Hooks, agents and operational boundaries

Native config/three role TOMLs and hooks JSON parse successfully. Tester and reviewer are actual native role invocations in this session; implementer was unnecessary and not invoked. Model/effort overrides were not set. Current definitions inherit approvals/sandbox and reserve orchestration/documentation to main.

`python3 -B tools/test_verify.py`: Passed 24 tests. `python3 -B .codex/hooks/test_hooks.py`: Passed 21 tests. Direct `.githooks/pre-commit`: exit 0, Maven skipped because no Java files staged. Logs under `/tmp/CH-003-increment8/`. `core.hooksPath` remains unset (read-only lookup exit 1). No Git hook activation. Native hook definitions have historical discovery/untrusted evidence; current syntax and smoke tests do not establish actual lifecycle delivery, which remains **Pending**. Human controls future trust approval. No trust settings changed.

SonarQube Cloud scanner, findings/hotspot retrieval and matching remote gate remain **Pending — human-controlled**, with findings count **Unknown**. No Cloud/CI/remote action or credential access occurred. Existing external gates are not waived by local success.

## Independent review, remaining decisions and recommendation

Fresh `/root/reviewer` and independent `/root/tester` completed their assessments and independently verified matching execution/source/mutation evidence. No BLOCKER/HIGH production finding or critical safety regression was demonstrated. Architecture complies with ADR-0007 Option B; mutation quality is not unconditionally green.

| Finding | Evidence and impact | Required disposition / acceptance effect |
| --- | --- | --- |
| INC8-R1 MEDIUM | Coordinator lockCustomer line110: removing recordLockCallFailure survives. Fast PIT selection lacks Customer-origin provenance assertion. PostgreSQL concurrency test lines313–354 already verifies Customer/Product retry behavior. | Open fast-suite/PIT-selector gap; human-approved bounded test-only follow-up or explicit disposition. No proven runtime defect. |
| INC8-R2 LOW | ProductApplicationService.validatePrice line53 return-null changed KILLED→SURVIVED; supported Money return lacks direct assertion after extraction. | Open demonstrated compatibility test-quality regression; bounded direct-return assertion after approval or explicit human disposition. Current write/HTTP validation still executes. |
| HIST-MUTATION review debt | 248 final nonkills retain historical same-status lineage; six timeouts remain separate. | Historical survivors/uncovered paths/equivalence candidates are not automatically accepted. See detailed disposition. |
| DOC-COUNTS LOW, resolved | Architecture guide's ten legacy checks,13 invalid scenarios/25 fixture checks corrected against final XML; Accepted ADR label corrected. | Documentation only; no test/rule change. |
| Operational/decision debt | Cloud gate Pending/findings Unknown; native delivery Pending; quiescent-only image orphan reconciliation; full ADR-0002 and recorded deployment/security decisions retain status. | Cloud remains a required human-controlled gate. Hook activation is separately optional; no activation implied by acceptance. |

[Detailed mutation assessment](increment-8-mutation-analysis.md) records scores,65→82 mutated classes,944 traceable mutants/939 stable statuses/five changes,57 retired and106 new mutants, all six timeout identities and risk-ranked historical debt. [Complete250-entry identity/disposition ledger](increment-8-mutation-ledger.json) preserves operators/descriptors/locations and historical candidates, including repeated-identity caveats. No newly uncovered final mutant identified; no blanket equivalence/accepted-debt decision made.

**Final recommendation: BLOCKED.** Local full execution passed, and architecture review found no production violation. Unresolved mutation findings require human disposition; the required matching human-controlled Cloud gate remains Pending. This recommendation is not human acceptance. Propose only the bounded test-only follow-up for R1/R2; do not implement it without explicit approval. Human also decides historical mutation-risk disposition and ambiguous empty-file ownership. No new architectural decision is needed for the completed verification.

Prior specialist usage-limit interruption is retained as a technical interruption: both specialists resumed after the human's reset instruction, reused completed evidence and returned final independent handoffs. No build/PIT rerun or correction iteration was consumed. All specialists are reconciled in the review ledger.

Final Git/source preservation audit is recorded below. No stage/unstage/commit/push/merge/rebase/reset/restore/clean/stash/branch switch, hook activation or deployment was authorized or performed.

**Human Acceptance: Pending.**

## Increment 8 final preservation and documentation audit

Final branch `develop`, HEAD `3d53fdf7a74d6cd96b1a658bb37c47613b347b89`; staged diff **0bytes**. Expanded status **173paths=49modified+7deleted+117untracked**, including169preserved intake paths and four new authorized evidence artifacts. Increment8 changed exactly ten documentation/evidence paths; **393other intake files retain hashes**. All**232build inputs match**, zero mismatches. Production,tests,configuration,POM,CI,mappings,migrations and unrelated/ambiguous files preserved. The cumulative migration diff remains unstaged/uncommitted; only authorized documentation was updated.

`git diff --check` exit0; targeted relativeMarkdown audit checked76file/local references and32heading anchors with zero unresolved targets before this plain-text audit appendix. Exact inventory/delta/status/evidence: `/tmp/CH-003-increment8/{final-audit.json,final-status.txt,file-inventory.json,intake-hashes.json}`. Verbose logs/reports retained under the final wrapper archive; durable summaries and mutation ledger reside in this request directory. No further test execution is required for documentation-only consolidation on unchanged build inputs.

**Final recommendation: BLOCKED. Human Acceptance: Pending.**


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
