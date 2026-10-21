# CH-003 — Hexagonal Architecture Migration

## Current documentation status — 2026-10-21

The human confirmed migration completion and reported that the latest human-run SonarQube Cloud analysis passed its Quality Gate with no remaining findings. See the [implemented architecture](../../../02-architecture/system-architecture.md#implemented-ports-and-adapters) and [quality status/provenance](../../../05-testing/quality-report.md#current-architecture-and-quality-status--2026-10-21). No Cloud query or new verification was performed for this documentation update. Historical mutation debt and hook activation limits retain their recorded disposition; this statement does not accept surviving mutants or complete ADR-0002.

## Historical final-increment disposition

The following statuses and increment entries retain their execution-time meaning. Earlier BLOCKED/Pending Cloud recommendations are historical, not the current human-reported Cloud result. Human acceptance of this documentation update remains Pending.

**Current increment:** 8 — Final verification and migration-wide acceptance assessment.
**Authorization:** Explicit human final verification, cumulative independent review, full mutation testing and documentation request; no production remediation.
**Status:** Targeted test hardening verified: INC8-R1/R2 technically resolved; focused 132 fast + two PostgreSQL, complete fast 779, 88 architecture checks, Spotless and narrow PIT (68 = 63 killed/five survived) passed. Independent review completed. **Recommendation BLOCKED** pending required human-controlled Cloud evidence and historical debt disposition. Initial full-verification evidence below remains historical.
**Human Acceptance:** Pending. Cloud evaluation remains human-controlled; no Git mutation or hook activation. Preserve cumulative unstaged/uncommitted work.

## Historical Increment 0 objective and motivation

Prepare a maintainable ports-and-adapters transition and a small native Codex team without changing the running application. The initial comparison is preserved; the human subsequently chose the pragmatic retained-mapping Option B. Prepare lightweight reminders/checks while retaining explicit comprehensive quality gates.

## Historical Increment 0 scope and exclusions

Allowed: Proposed ADR and target documentation, three-role configuration, workflow/ownership updates, minimal native lifecycle handlers, inactive Git hook and focused validation. No production Java, application tests/rules, POM/dependencies, API/OpenAPI, migrations, business behavior, global config, hook trust, Git hook activation or Git publication. No Increment 1.

Pre-existing untracked .agents/ and .codex/skills/ are preserved. Baseline develop / 3d53fdf7a74d6cd96b1a658bb37c47613b347b89; no staged/unstaged tracked changes at intake. The existing skill remains guidance, not architectural approval.

## Records and authority

- [Documentation, criteria and original authorization](01-documentation.md)
- [Implementation, exact files and verification](02-implementation.md)
- [Review, gates, task ledger and remaining decisions](03-review.md)
- [Accepted migration ADR](../../../03-decisions/ADR-0007-hexagonal-architecture-migration.md)
- [Current architecture](../../../02-architecture/system-architecture.md), [proposal](../../../02-architecture/hexagonal-migration.md), [workflow](../../multi-agent/workflow.md) and [hooks](../../multi-agent/hooks.md)

Only explicit human review may accept the result. Neither the configuration, native hooks, tests nor independent agent review approves migration or publication.

## Historical Increment 0 follow-up — Option B and local automated gates

The human explicitly accepted Option B and retained the team; ADR-0007 is Accepted without superseding ADR-0004. Only Increment 0 configuration/docs/local quality automation is authorized. [Engineering verification](../../../05-testing/engineering-verification.md) defines make verify-fast and make verify-full SCOPE=<explicit scope>. No production code/tests/POM/mapping/API/schema/CI or remote gate configuration changes. No native/Git activation or Git publication.

Fast validation passed 640 executions; full Maven passed 640 fast +157 disposable PostgreSQL executions with zero failures/errors/skips. Full SCOPE=all PIT executed: 1001 mutants =746 KILLED/212 SURVIVED/37 NO_COVERAGE/6 TIMED_OUT. No threshold is configured; representative independent review is complete but exhaustive outcome disposition remains Pending. Full-regression authorization in this follow-up supersedes only the earlier setup-only unrun status; it does not rewrite historical results. SonarQube remains human-controlled and unevaluated here. Actual outcomes and final blockers belong to 02/03 records.

## Increment 1 scope

Migrate only GET /products/{id} to an application input port, narrow immutable-result output port and existing-JPA adapter. Preserve active/inactive lookup, missing 404, DTO/OpenAPI/error contracts, read-only REQUIRED transactions, JPA mappings and all other operations. Retain the existing public ProductApplicationService.get as a compatibility delegate. Sources at intake are recorded in 02; no overlapping production edits were present. Implementation/test owners are recorded in 03. All four records remain owned by the Orchestrator.

## Increment 1 verification summary

GET-by-ID now follows input port → read use case → output port → JPA adapter → unchanged repository/aggregate. Existing immutable ProductResult, HTTP/OpenAPI contracts, inactive visibility and read-only/ambient transaction behavior are preserved. The legacy application get remains a compatibility delegate; no subsequent operation is migrated.

Focused84 and corrected smoke1 passed; make verify-fast passed 683; make verify-full (default all) passed 683 fast +161 PostgreSQL tests, zero failures/errors/skips, Spotless and runtime OpenAPI equality. JaCoCo lines 97.31%, branches 92.11%, instructions 97.82%; both new classes fully line-covered. PIT 1003 outcomes:747 killed,212 survived,37 uncovered,7 timed out. Changed Product classes25/25 killed; Product historical non-killed identities unchanged. One unchanged shared async mutant changed killed→timeout and remains Pending cause/disposition. Global historical mutation disposition and Cloud evaluation remain Pending. See [implementation details](02-implementation.md#increment-1-final-local-verification-and-mutation-comparison) and [independent review/gates](03-review.md#increment-1-final-quality-gates-and-risks).

All 21 task files are listed in the implementation record. Unrelated/pre-existing work is preserved; no files staged or committed. Stop after Increment 1. Human Acceptance Pending.

## Current verification policy — effective 2026-10-21

The human [policy update](01-documentation.md#verification-policy-update--2026-10-21) applies to Increment 1 and every subsequent implementation increment. Run focused unit/regression/ArchUnit/Spotless and make verify-fast during increments. Before each human acceptance, run complete Maven clean verify with PostgreSQL/Testcontainers, JaCoCo and independent architecture/code review. No PIT or PIT-enabled make verify-full during individual increments, including corrections. After all increments, run make verify-full SCOPE=all with complete PIT, final coverage/regression/architecture/reviewer assessment and baseline comparison.

Increment 1's matching 683 fast +161 PostgreSQL/coverage/review evidence remains valid; the earlier PIT run and its findings remain historical. The new policy supersedes per-increment PIT scheduling in earlier prompts/records without changing their results, accepting findings or granting Human Acceptance. See the [authoritative gate policy](../../multi-agent/quality-gates.md#ch-003-migration-verification-policy). No additional implementation increment is authorized.

## Increment 2 current execution

Product create/update/PATCH deactivation is authorized; physical DELETE remains unsupported. Exact same-feature ProductWriteStore aggregate output exception explicitly approved by human. See the Increment 2 sections in the four existing records. Initial architecture review completed; characterization/implementation and all final gates Pending. Earlier Increment 1 statements above are historical. PIT Deferred by human decision. Human Acceptance Pending.

## Increment 2 final verification summary

Create/update/PATCH deactivation now use ProductWriteOperations → ProductWriteService → ProductWriteStore → ProductWriteAdapter. Exact Product aggregate output exception explicitly approved; existing facade delegates/list/GET and Order/images remain.26 task files are listed in02.

Corrected make verify-fast Passed698; complete non-PIT clean verify Passed698fast+165PostgreSQL,zero failures/errors/skips; architecture50 and unchanged OpenAPI Passed. JaCoCo97.33%lines/92.11%branches/97.84%instructions, new service/adapter fully line-covered. Initial full test-fixture failures retained and corrected at cumulative3/3; final matching independent review Passed in03. PIT Deferred by human decision. Cloud and Human Acceptance Pending; no files staged/committed, no later increment started.

Final reconciliation: all three specialists Returned; local required gates Passed, correction counter3/3, no further increment started. Human Acceptance Pending.


## Increment 3 — Authorized intake; blocking decisions pending

2026-10-21, Europe/Berlin: the human explicitly authorized Customer migration only, with the native implementer/tester/reviewer team. This supersedes earlier statements that no Increment 3 is authorized. Initial architecture and baseline assessments completed; production implementation has not started. Human Acceptance Pending.

Two decisions block dependent implementation: the exact Customer-only managed-aggregate output exception (ADR-0007 currently permits ProductWriteStore only), and direction on proceeding with the exhausted cumulative correction counter 3/3. No counter reset or fourth correction is authorized. See the appended contract, implementation and review sections. PIT Deferred by human decision; all fresh implementation gates Pending.


### Increment 3 human decisions resolved — 2026-10-21

Human explicitly approved the exact CustomerWriteStore aggregate exception and ADR-0007 update. Human rejected proceeding under the exhausted cumulative allowance and instead approved the governing per-increment policy amendment. Workflow now grants three correction cycles per explicitly authorized increment. Historical Increment2 remains 3/3 without reinterpretation; Increment3 starts at **0/3**, initial implementation iteration0. Escalate on exhaustion; no unresolved earlier defect may be concealed by increment boundaries. Earlier intake holds are historical and resolved by these decisions. Proceed with the already authorized Customer scope; non-PIT verification and Human Acceptance Pending remain binding.


## Increment 3 execution summary

Customer REST now uses CustomerOperations, implemented by the existing application service through CustomerReadStore/CustomerWriteStore and JPA adapters. CustomerFacts and CustomerOrderReferences isolate provider/reference boundaries while preserving unchanged Order callers and MANDATORY participation. Static email normalization, all endpoints/errors, UUID/version/JPA mappings, deletion locks/reference ordering and transaction completion remain unchanged.

Local verification Passed: baseline160,focused207,fast708,complete708fast+167PostgreSQL;zero failures/errors/skips in successful final runs. Spotless,56 applicable architecture executions/negative cases, runtime OpenAPI compatibility and JaCoCo report Passed (97.41%lines,92.21%branches,97.87%instructions). Exact36-file manifest/evidence in02. Increment3 correction0/3; historical Increment2 3/3 preserved under approved policy amendment. Final independent review and human acceptance disposition in03. PIT Deferred by human decision; Cloud Pending human control/counts Unknown. Stop after Increment3; no publication.


## Increment 4 — Authorized intake, 2026-10-21

The human explicitly authorized Product image processing/storage/application migration only, with the native implementer, tester and read-only reviewer. Preserve accumulated uncommitted Increments 0–3; no Git staging, commits, history/branch changes or hook activation. Stop after Increment 4. PIT Deferred by human decision; SonarQube remains human-controlled. Human Acceptance Pending.

Initial lifecycle/architecture assessment and characterization are in progress. Production edits await a bounded contract consistent with ADR-0007. Increment 4 correction counter **0/3**; historical counters remain unchanged. Main-session Orchestrator owns all four records; implementer owns assigned image production files, tester owns image/architecture tests, reviewer remains read-only.

Increment 4 current correction counter: **1/3** (test interaction expectations); verification in progress.


## Increment 4 final summary

Image REST now uses ProductImageOperations → existing application orchestration → processing/storage/metadata/transaction ports → existing ImageIO/filesystem/JPA/Spring implementations. Entity-free metadata snapshots preserve domain mappings and managed identity. Conservative generation retention, locks, commit-before-cleanup, security, limits and HTTP/OpenAPI behavior are unchanged.

Passed: focused216, image PostgreSQL12, make verify-fast727, complete non-PIT727fast+167PostgreSQL,62architecture,13OpenAPI,Spotless and JaCoCo reporting (97.47%lines/92.22%branches/97.91%instructions). Independent fresh milestone review Passed with no actionable findings; correction **1/3**. [Exact36-path manifest and evidence](02-implementation.md#increment-4-exact-changed-file-manifest), [final gates/risks/ledger](03-review.md#increment-4-final-independent-review-and-disposition).

All accumulated work remains uncommitted and unstaged on unchanged develop/HEAD. No Increment5. PIT Deferred by human decision; Cloud Pending human control/findings Unknown. **Human Acceptance Pending.**


## Increment 5 authorized intake

The human explicitly authorized existing Order read migration only. Preserve Order/OrderItem mappings, historical stored names/prices/quantities/discounts/totals, fetch joins/query counts, read-only transaction participation, current Customer enrichment and HTTP/OpenAPI behavior. No placement/retry/reference-query migration, cross-feature behavior change or later increment. Team roles and scope are recorded in [review](03-review.md). All Git mutation and PIT/Cloud restrictions remain. Increment 5 starts **0/3**; earlier Increment 4 1/3 and other counters are unchanged.


## Increment 5 final summary

GET/list now follow OrderQueries → existing OrderQueryService → OrderReadStore → OrderReadAdapter → unchanged fetch-join repository. OrderCustomerFacts/OrderReadCustomerAdapter bridge to existing CustomerFacts; no Customer behavior change. Stored immutable snapshots preserve all amounts/names/quantities and current Customer enrichment; readOnly REQUIRED transactions,fetching/query counts and closed-context serialization verified. Placement/retry/reference lookup,OrderResult.from,domain/JPA/schema/API unchanged.

Passed: baseline123; pre-migration126fast+10PostgreSQL; corrected focused159fast+11PostgreSQL; make verify-fast740; complete non-PIT740fast+170PostgreSQL;69architecture;13OpenAPI;Spotless;JaCoCo97.52%lines/92.24%branches/97.94%instructions. Fresh independent milestone review Passed,no actionable findings; correction **1/3**. [Exact23-path manifest/evidence](02-implementation.md#increment-5-exact-changed-file-manifest), [final disposition/debt](03-review.md#increment-5-final-independent-review-and-disposition).

All accumulated changes remain unstaged/uncommitted on unchanged develop/HEAD. No Increment6. PIT Deferred by human decision; Cloud Pending human control/findings Unknown. **Human Acceptance Pending.**


## Increment 6 Phase A — safety gate blocked

Independent reviewer and implementer both identified missing Order-only managed-entity output authorization. ADR-0007's Product/Customer exceptions expressly do not extend to Order. Proposed exact exception: `OrderWriteStore.save(Order): Order`, with `flush(): void`, solely inside the owning placement transaction. Retain application construction,generated IDs,cascade PERSIST and same aggregate; no entities through input/read/cross-feature ports. Exact attempt completion/provenance contract is also presented for explicit decision.

[Phase A contract and proposed files](01-documentation.md#increment-6-phase-a-bounded-migration-contract), [execution limitations](02-implementation.md#increment-6-phase-a-only), [coverage/safety gate](03-review.md#increment-6-phase-a-safety-gate). No implementation starts until this decision is resolved. Fresh tests/fast/full/coverage/OpenAPI/final code review remain Pending; no tests executed in Phase A. Counter0/3; Human Acceptance Pending.

Final independent review of the exact Phase A contract Returned,no additional concern beyond INC6-A1/A2. Contract is ready for human decision; Phase B remains blocked.


## Increment 6 final summary

Human approved PhaseA A1/A2 and narrow ADR-0007 update;pre-extraction fresh-facts characterization Passed. Placement now uses OrderPlacementOperations → existing coordinator → OrderWriteStore/OrderPlacementCustomerFacts/OrderPlacementProductFacts/OrderPlacementAttempts → existing JPA/provider/Spring infrastructure. Three-state completion,original failure identity,rollback-onlyeligible retry,locks,timeouts,budgets,commit-before-success and atomic aggregate persistence preserved. Guard/classifier relocate unchanged;Product provider adds input interface only.

Passed:characterization80fast+1PG;focused360fast+46PG;make verify-fast758;complete non-PIT758fast+171PG;75architecture;13fast+1runtime OpenAPI;Spotless;JaCoCo97.56%lines/92.26%branches/97.97%instructions. Fresh independent review Passed,no actionable findings. Counter **2/3**,both test-only correction batches. [Exact39-path manifest/evidence](02-implementation.md#increment-6-exact-changed-file-manifest),[final disposition/debt](03-review.md#increment-6-final-independent-review-and-execution-disposition).

All cumulative changes unstaged/uncommitted on unchanged develop/HEAD,outside-scope intake hashes preserved. No Increment7. PIT Deferred by human decision;Cloud Pending human control/counts Unknown. **Human Acceptance Pending.**


## Increment 7 authorized intake — 2026-10-21

The human explicitly authorized architecture-wide consolidation of Increments 1–6 and final applicable architecture enforcement. This supersedes earlier “no Increment 7” restrictions only. Phase A inventory and independent assessment are running; production edits await the scope safety gate. Counter **0/3**. Human Acceptance **Pending**. Stop after Increment 7; no Increment 8, PIT, Cloud actions or Git mutation. Preserve the entire cumulative uncommitted diff.


## Increment 7 final summary

Closed Product list/locked-facts and Order reference repository coupling through four immutable capability interfaces implemented by existing services/adapters. Controllers and Customer reference bridge use inputs;all supported compatibility methods/providers retained. Complete populated application/port/controller/adapter/provider/constructor-cycle checks and negative fixtures activated. No business,API/schema,mapping,query,transaction/retry/image lifecycle or configuration change.

Passed:focused303fast+41PostgreSQL;make verify-fast777;complete non-PIT777fast+172PostgreSQL;88architecture;13fast+1runtimeOpenAPI;Spotless;JaCoCo97.57%lines/92.26%branches/97.98%instructions. Correction2/3;failed history retained. [Exact39pathmanifest and evidence](02-implementation.md#increment-7-exact-changed-file-manifest),[finalgates/risks](03-review.md#increment-7-consolidated-final-gates-and-remaining-risks). Fresh final independent review Passed,no remaining actionable finding;all specialists Returned.

Cumulative Git work preserved unstaged/uncommitted on unchanged develop/HEAD;169expandedstatuspaths including prior increments. PIT Deferred;Cloud Pending human control/findings Unknown. **Human Acceptance Pending. Stop after Increment7;no Increment8.**


## Increment 8 authorized final verification — 2026-10-21

The human explicitly authorized cumulative final verification, independent architecture review, full mutation testing and final acceptance documentation in attachment `ca4c32d7-2641-491c-8e9a-6856552035c8/Eingefügter Text.txt`. This supersedes the historical Increment 7 stop restriction only for verification/documentation. No production/test/configuration edits or remediation are authorized. Main-session Orchestrator owns all record/report writes; native tester and fresh reviewer are read-only. Human Acceptance Pending.

Intake: `develop`, HEAD `3d53fdf7a74d6cd96b1a658bb37c47613b347b89`, empty index; 399 existing file hashes captured in `/tmp/CH-003-increment8/intake-hashes.json`. Complete expanded status and tracked/staged diffs preserved beside it. No Git mutation, Cloud action or hook activation. Final verification is Running; recommendation Pending.

## Increment 8 final disposition

Full Java21 `make verify-full SCOPE=all` exit0;777fast+172integration (160PostgreSQL/12servlet),88architecture,OpenAPI and Spotless Passed. JaCoCo97.57%lines/92.26%branches/97.98%instructions. PIT1050=800K/213S/31NC/6TO;76.19%strictkill;different population from historical1001. Fresh tester/reviewer Returned: no demonstrated production defect or critical regression; R1Medium fast Customer-provenance gap and R2Low compatibility-return mutation regression remain open. Existing PostgreSQL Customer retry evidence is preserved. Cloud Pending human control/findings Unknown. **Recommendation BLOCKED; Human Acceptance Pending.**

Durable evidence: [final acceptance report](increment-8-acceptance-report.md), [complete inventory](increment-8-file-inventory.md), [mutation assessment](increment-8-mutation-analysis.md), [250-entry mutation ledger](increment-8-mutation-ledger.json). These four additional artifacts meet explicitly requested cumulative inventory/mutation audit needs; the four standard records remain authoritative. No remediation, production/test/config change, Git mutation, Cloud action or hook activation.


## Increment 8 targeted test-hardening follow-up — 2026-10-21

Scope: explicit human Increment8 targeted test-hardening follow-up. Main owns two existing test files and documentation; existing independent reviewer owns read-only assessment. No new production-refactoring increment. Verification Running; Human Acceptance Pending.

### Targeted hardening final summary

Bothgaps meaningful/non-equivalent;newCustomerparametercase andProductMoneyreturnassertion killboth exactmutants. Focused132+2PG,verifyfast779,architecture88,SpotlessPassed;narrow2classPIT68=63K/5S,remaining5historicalsurvivorsunchanged. Existingreviewer nofollow-upfinding. Onlytwoexistingtestfilesanddirectlyrelevantrecords/reportsupdated;no production/config/Git/Cloud/hooks. Currentfollow-upreadyhumanreview;overallrecommendationBLOCKEDforCloud/historicaldebt. HumanAcceptancePending.
