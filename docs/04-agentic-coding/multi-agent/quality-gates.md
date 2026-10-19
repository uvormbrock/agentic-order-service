# Quality Gates

Use the [testing strategy](../../05-testing/testing-strategy.md), [quality report](../../05-testing/quality-report.md), [Maven build](../../../pom.xml), [mutation runbook](../../05-testing/mutation-testing-runbook.md#current-automated-workflow), [Makefile](../../../Makefile) and [existing CI](../../../.github/workflows/build.yml). Historical evidence does not verify a future source state. No new tools, dependencies or thresholds are introduced by this workflow.

## Scheduling and responsibility

For every authorized Change Request, the Orchestrator evaluates every gate below at planning and final reporting, recording applicability and reasons. Testing executes assigned checks and collects evidence; Review independently assesses results, coverage gaps, surviving mutants and findings; Documentation synchronizes contracts and evidence. The Orchestrator schedules stable inputs, consolidates results and enforces the [correction limit](workflow.md#correction-limit). Preserve the [four standard records](../change-requests/README.md), single-writer ownership and all [approval rules](approval-policy.md).

Run focused tests during implementation. Once implementation stabilizes, run full regression once and reuse its Surefire, Failsafe, formatting and JaCoCo evidence for that source state. Archive reports before a clean build or another run overwrites them. Schedule scoped mutation testing separately; its existing fast-test baseline remains required. Do not duplicate expensive regression or mutation runs without changed inputs, failures or an unresolved verification concern. After production changes in correction cycles, rerun full regression and revalidate every affected gate, including coverage, mutation, OpenAPI and remote analysis as applicable. Record why any earlier evidence remains valid; do not silently carry stale results forward. Missing access leaves required checks Pending.

## Gate inventory

| Gate | Execution or evaluation method and required evidence |
| --- | --- |
| Unit tests — Maven Surefire | Focused `./mvnw -B -ntp test -Dtest=<selected-tests>` during implementation; inspect `target/surefire-reports/` XML for actual tests, failures, errors and skips. Empty selection or successful startup is not test evidence. |
| Integration — Maven Failsafe / PostgreSQL Testcontainers | Use the existing `integration-test` / `verify` bindings in full regression; select focused integration tests with supported `-Dit.test=<selected-IT>` when needed. Inspect `target/failsafe-reports/`. Require disposable real PostgreSQL and reachable Docker; never reset the development database. |
| MVC and API contracts | Select affected MVC/HTTP/error tests and inspect assertions for binding, validation, exact status/envelope and response behavior. Record test identities and reports; unit tests alone do not prove HTTP contracts. |
| Concurrency and transactions | Where consistency, aggregates, locks or transaction boundaries are affected, require relevant PostgreSQL race, commit/rollback and state-preservation tests. Explain Not Applicable for unaffected scope; context smoke tests or mocked repositories do not establish locking correctness. |
| Formatting — Spotless / Palantir Java Format | `./mvnw -B -ntp spotless:check`, or the existing `validate` binding included in regression. Record actual result; do not modify formatter configuration. |
| Full Maven regression | `./mvnw -B -ntp clean verify` with existing Java 21 conventions after stabilization. Inspect both report sets, exit status and skipped tests, rather than relying only on BUILD SUCCESS. |
| Code coverage — JaCoCo | Reuse the matching regression's `target/site/jacoco/jacoco.xml` and HTML report. Report actual covered/missed line, branch and instruction counts and percentages, including affected packages/classes and meaningful uncovered paths. The current POM binds report generation to verify and defines no JaCoCo check goal or numeric minimum. Inspect current configuration for existing thresholds each time; report any applicable remote coverage condition separately. Never invent a threshold or treat report generation/high coverage as proof of assertion quality. |
| Mutation testing — PIT | For production behavior changes, execute or explicitly evaluate applicability using the policy below and existing `make mutation-test SCOPE=...` targets. Record selected classes/tests, baseline, actual outcomes, scores, survivor review and evidence. Never report quality Passed solely because Make/Maven exited zero. |
| Static analysis — SonarQube Cloud | Execute only with existing authorized access/credentials and project settings. Obtain the actual remote analysis and matching Quality Gate, conditions and relevant findings for the analyzed source state. Scanner upload success alone is insufficient. Pending if required access/analysis/gate evidence is unavailable. |
| OpenAPI contracts and snapshots | Evaluate affected `OpenApiDocumentationTest` and `OpenApiRuntimeIT`, runtime JSON/YAML equivalence, snapshot, references, annotations/examples and error codes. Use the established runtime generation workflow when updates are authorized; a hand-edited snapshot is not runtime verification. |
| Documentation consistency and Markdown links | Compare actual implementation, requirements, API/errors/examples, operations, status markers and all four Change Request records. Verify relative Markdown file targets and local heading anchors with existing methods; report method, checked scope and unresolved links. Preserve historical evidence and ADR statuses. |
| Git diff hygiene | Inspect staged, unstaged and untracked content against intake baseline; run `git diff --check` and separately check new-file whitespace/content. Include complete source-state identity without staging files. |
| Independent code review | A separate reviewer assesses actual diff, contracts, tests, coverage gaps, surviving mutants, static/security findings, transaction correctness and documentation. Record severity, evidence and resolution/re-evaluation. Implementer self-review cannot satisfy this gate. Never automatically suppress or accept security findings. |

## Mutation scope and interpretation

Use the narrowest meaningful existing scope, always explicitly: `make mutation-test SCOPE=order`, `product`, `customer` or `shared`. Never invoke the target without SCOPE because its default is `all`; do not automatically run `SCOPE=all`. Consult the runbook selectors: `order` covers only `order.domain.*`, while the other named scopes cover their feature packages. If changed Order application/API code or cross-feature behavior requires broader coverage, explain the selector gap and deliberately justify an all-scope run within authorization, or record the uncovered behavior and limitation. Do not claim a narrow run covers excluded code.

Every scope uses project-wide fast `*Test` discovery; PostgreSQL `*IT` tests are excluded. Mutation testing does not replace integration/concurrency verification. For non-behavioral changes, explain why PIT is Not Applicable. Applicable but unrun or blocked mutation verification is Pending, not Not Applicable merely because it is expensive or unavailable.

Inspect actual `mutations.xml` and HTML, not only report existence. Report generated counts and all outcome categories, including KILLED, SURVIVED, NO_COVERAGE and TIMED_OUT; distinguish detected/generated (which can include timeouts), strict KILLED/generated and test strength with their actual denominators. Explain empty/invalid populations and other execution errors. Review surviving and uncovered mutants against requirements/assertions, with identity, meaningful gap or evidence-backed equivalence candidate, resolution or explicit deferral. Timeouts and infrastructure errors are not assertion-based kills. The existing profile defines no mutation-score acceptance threshold. Numeric score alone neither passes nor fails quality; unresolved meaningful gaps remain findings requiring disposition and independent review. Never invent an acceptance threshold or silently accept survivors.

The Makefile archives baseline/command/selection/source-state and PIT reports under a unique external evidence directory described in the runbook. Preserve that path and a durable summary; external persistence is not verified backup. PIT skips JaCoCo, so mutation output is not fresh coverage evidence.

## SonarQube provenance and authorization

CI uses scanner `5.8.0.7211` with `sonar.qualitygate.wait=false`; scanner completion does not establish a passing Quality Gate. The established local entry point is `make sonar-local`; see [local operation and authentication](../../06-operations/local-development.md#local-sonarqube-cloud-quality-gate). It runs full `clean verify` together with analysis of the attached main/develop working tree and waits up to 300 seconds for the remote gate, without requiring publication. Schedule it once per stable source state after stabilization and focused local verification; use its verification reports as the full-regression evidence instead of immediately repeating a separate clean verify. A prior regression may be reused only for its matching inputs; this target intentionally always verifies before scanning. For an authorized run, inspect/wait for the matching remote analysis through existing supported access, or use the supported scanner wait option. The Testing Agent returns a structured handoff separating Maven verification, scanner upload/processing and actual remote Quality Gate outcomes, with exit status and dashboard URL if available. Record these in `03-review.md`. Review independently checks intended branch, relevant source state, genuine gate result, outstanding SonarQube findings and security risks. Record project/branch, analysis identity, execution date, source identity (including uncommitted content), actual remote gate status/conditions and finding disposition. Reuse matching valid build/coverage evidence rather than rebuilding unnecessarily. Do not expose secrets, create credentials, install a server, change thresholds, publish Git changes or dispatch remote workflows without separate authorization.

## Evidence and status contract

Every gate must appear in `03-review.md` and the final report with **Passed / Failed / Pending / Not Applicable**, actual command or verification method, execution date (with timezone), relevant source state (branch/base revision plus staged/unstaged/untracked diff identity), result, evidence location, limitations and open findings. Include exit code and counts where applicable. `02-implementation.md` retains implementation/test details; link evidence to avoid conflicting copies. Durable summaries must remain understandable if temporary logs disappear.

Passed requires actual quality evaluation for the relevant inputs, not merely a successful tool invocation. Failed records an observed failed check or unmet applicable criterion. Pending covers required unrun, unavailable, inconclusive or unevaluated results. Not Applicable requires a scope-based reason. Do not infer success from absent reports, previous revisions, missing findings or a tool's zero exit. Human acceptance remains Pending until explicitly granted.

For documentation/configuration-only framework maintenance, evaluate application execution gates as Not Applicable with reasons. Validate changed TOML syntax, Markdown links and diff hygiene; do not execute application gates or delegation smoke tests unless specifically authorized. Syntax validation does not prove runtime role loading or an autonomous end-to-end lifecycle.

## Actual findings and remediation

`make sonar-findings` independently retrieves current-branch findings using existing authentication and explicit project/branch scoping, without Maven or scanner submission. Maven/scanner execution is Not Applicable for retrieval-only mode, with a recorded reason. It establishes no scanner-task/analysis identity; the analysis-specific Quality Gate remains Pending. Human-approved standalone exit policy (2026-10-18): exit 0 means required issue retrieval completed; incomplete or failed issue retrieval returns 3. Hotspot completeness, Quality Gate, provenance and independent review remain separate; exit 0 is not a complete security review. Failed retrieval requires diagnosis, never an automatic full analysis rerun. Apply the [bounded retrieval-recovery policy](workflow.md#findings-retrieval-failure-and-workflow-completion): at most one authorized targeted retry per Change Request, only for an established ordinary transient failure.

`make sonar-local` also collects issues and legacy hotspots after a successfully processed analysis, even if its Quality Gate failed. See [collection, attribution and API limits](../../06-operations/local-development.md#automated-findings-and-controlled-remediation). The private external report keeps actual Quality Gate status, issue retrieval, hotspot retrieval and independent issue-review completion separate. A passed gate never establishes zero issues or completed review. Incomplete/denied retrieval is Pending with unknown counts, not zero; non-main legacy hotspot branch provenance remains Pending under the documented API limitation. For `sonar-local`, mandatory hotspot incompleteness remains nonzero: `develop` cannot complete successfully unless it is the confirmed remote main branch, even with a passed gate and complete issues. Record this blocker explicitly; do not weaken the rule or infer zero hotspots.

Testing returns actual open issue counts by type/severity and software-quality impact, finding IDs/rules/files/lines/messages/statuses, branch/source and analysis provenance, new-code and repository-change candidates, optional earlier-baseline membership and evidence limitations. Review independently assigns in-scope defects, worthwhile maintainability corrections, security priorities, potential false positives for human decision and pre-existing/out-of-scope findings. New-code membership and changed-line overlap are evidence, not automatic Change Request ownership.

The Orchestrator routes actionable authorized findings to Implementation under the existing three-correction limit; obtain human authorization for material architecture, security or scope decisions. Never automatically dismiss, suppress, resolve or accept findings in the Cloud service. After production corrections rerun affected tests and analysis/retrieval before claiming a final passed gate for the revised state. Record each iteration's corrected finding IDs, remaining findings/reasons, hotspots awaiting human review, final URL/branch and separate retrieval/review statuses in `03-review.md`. Keep reports external and copy only safe minimal summaries. Reuse valid unchanged-state evidence without unnecessary remote scans.

Approved limited provenance policy (2026-10-18): preserve fresh scanner evidence, matching CE task/project/branch identity and an analysis-specific Quality Gate. Report current-branch issue retrieval separately from `issueSnapshotProvenance: Pending` and `supersessionVerification: Pending`; these two limitations alone are nonfatal. They never establish an immutable historical snapshot or absence of findings. Required gate, identity, issue-retrieval and existing hotspot failures remain nonzero for `sonar-local`. Standalone retrieval requires issue completeness only under the human-approved policy above. See [operational contract](../../06-operations/local-development.md#automated-findings-and-controlled-remediation).

## Retrieval failures do not abort orchestration

Collector and Make exit codes and all mandatory `sonar-local` requirements remain
unchanged. A nonzero retrieval exit is evidence to assess, not a command to abort
independent workflow phases. Separate Maven verification, scanner execution/processing,
actual analysis-specific Quality Gate, issue retrieval, hotspot retrieval and review.
Do not use the overall Make exit as the sole gate verdict. Independently verified
matching gate evidence remains Passed despite collector failure; confirmed gate failure
remains Failed, and an unverified gate remains Pending.

For unavailable/incomplete issue retrieval record Pending; for a confirmed failed
operation record Failed, based on actual evidence. Record known root cause or Unknown,
safe diagnostics/evidence path and actual findings count Unknown. Partial results may
be retained as explicitly incomplete candidates, never a complete count or zero-issues
claim. Hotspot completeness remains Pending without authoritative evidence. Never
suppress, dismiss or automatically accept security findings.

Follow the bounded retry rule linked above; retrieval failure alone consumes no
implementation correction iteration and does not reset the maximum of three.
Continue independent authorized gates, review and documentation checks, then produce
`03-review.md` and the final human-review report with every separate status, remaining
risks and incomplete verification. **Completed — Human Review Required** may coexist
with Failed or Pending external checks; it is an execution status, not a green quality
verdict or authorization to commit, push or deploy. Human Acceptance remains Pending.
Mandatory safety restrictions stop affected operations, not independent safe work.
