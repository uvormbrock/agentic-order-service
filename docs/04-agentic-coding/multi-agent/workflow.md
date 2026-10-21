# Orchestration Workflow

Follow [approval policy](approval-policy.md), [handoff contract](handoff-contract.md) and [quality gates](quality-gates.md). The main Codex session is the Orchestrator. It alone advances phases, delegates assignments, receives structured handoffs, enforces approval boundaries and the three-correction limit, manages nonfatal external-tool recovery, consolidates Quality Gate results and owns the final human-review report. Specialists return evidence and unresolved findings; they do not control sequencing or delegate. No role may publish Git changes without separate explicit human authorization. A defined lifecycle authorization can cover implementation and verification; it never implies ADR acceptance or final human acceptance.

| Phase | Owner and required outcome |
| --- | --- |
| A — Intake | Orchestrator records branch/base commit, complete staged/unstaged/untracked baseline and explicit scope. Classify the task using the [mandatory Change Request convention](../change-requests/README.md); only for an authorized Change Request, inspect the register and directories, reserve the next unused sequential CH-NNN and create exactly its four standard Markdown files with Pending statuses. The Orchestrator identifies authoritative rules, acceptance criteria, edge cases and blocking ambiguity. |
| B — Architecture and plan | The read-only reviewer assesses only relevant accepted ADRs and Open/Proposed subdecisions; identifies aggregates, transactions, persistence, APIs, concurrency, privacy/security and compatibility risks. Orchestrator assigns implementation/test/documentation files, prerequisites and gates. Resolve missing mandatory approval before dependent edits. |
| C — Target contract | The Orchestrator updates assigned approved target requirements, acceptance criteria, API/error contracts. Label target versus current runtime behavior; preserve historical evidence. Writes are sequential where files overlap. |
| D — Implementation | Implementation agent changes only assigned files under the approved contract. Preserve unrelated changes. Orchestrator checks the returned diff and handoff before advancing. |
| E — Testing | Testing agent derives/extends unit, MVC/contract, PostgreSQL integration and relevant concurrency tests. Evaluate every gate in the quality-gate inventory; run focused checks during implementation, full regression after stabilization, and mutation verification only when applicable under the scheduling policy (never during individual CH-003 increments). After completed increments or meaningful corrections run make verify-fast; for CH-003 Increment 1 and later, the Orchestrator must run complete Maven clean verify with PostgreSQL/JaCoCo and independent review before acceptance, without PIT. Reserve make verify-full SCOPE=all and PIT for final migration acceptance under the [CH-003 policy](quality-gates.md#ch-003-migration-verification-policy). Other authorized production-refactoring requests retain the generic full/PIT policy. Do not add redundant full builds. Collect measured JaCoCo results and authorized matching remote SonarQube Quality Gate evidence; retain dates, source states, counts, exits and locations. Cloud analysis/findings/gate evaluation is human-controlled; no agent invokes make sonar-local, make sonar-findings or scanners without a separate explicit human request. Keep required remote evaluation Pending until supplied. Failed retrieval never triggers automatic analysis or aborts independent phases; apply the bounded retrieval-recovery policy below. Return actual findings to Review and the report with separate gate, issue/hotspot retrieval, historical snapshot provenance and issue-review statuses; incomplete retrieval is Failed or Pending according to evidence, with actual findings count Unknown. |
| F — Independent review | Fresh reviewer context inspects the actual diff, contracts, test evidence, architecture, transactional/concurrency correctness, security/privacy, errors and docs, including measured coverage gaps, mutation outcomes/survivors and actual remote Quality Gate evidence. Distinguish execution success from quality evaluation. Independently classify automated SonarQube findings as genuine defects, scoped maintainability improvements, priority security concerns, potential false positives requiring human review or pre-existing/out-of-scope issues; never equate Sonar new code with Change Request ownership. Return severity, file/line, evidence and proposed correction. The implementer cannot substitute its self-review for this review. |
| G — Bounded correction | Orchestrator verifies actionable findings, increments the durable counter before correction, assigns only authorized actionable findings to Implementation without expanding scope, then reruns focused and affected gates and asks for re-evaluation. After production corrections rerun required local gates; revised-state remote evaluation remains human-controlled/Pending unless separately explicitly requested. Material architecture, security or scope decisions require human authorization. Record corrected IDs and remaining findings/reasons; never change Cloud finding statuses automatically. See stop rules below. |
| H — Final consistency | The Orchestrator compares implementation/requirements, public API/OpenAPI, examples, error codes, README/operations, links and status markers. Update superseded current descriptions while preserving historical runs and decision status. This phase is mandatory even if docs were updated earlier. |
| I — Final gates | Orchestrator consolidates applicable available checks and unresolved findings. Every inventory gate must have status, command/method, execution date, source state, result/evidence location and limitations/findings. Required unavailable gates remain Pending; never infer a green result from an earlier revision. Reuse valid evidence and avoid duplicate expensive runs; after production corrections revalidate all affected gates under the scheduling policy. |
| J — Human review | Produce the final report, complete diff/status and acceptance checklist. Stop with human acceptance Pending; no automatic Git publication, PR or deployment. |

Requirements and architecture investigation may overlap only when neither depends on the other's result. Implementation waits for approved contracts and blocking decisions. Testing and documentation may run independently only with disjoint file ownership and stable inputs; verification consuming changing files must wait for writers. A delegated role is recorded as executed only after actual invocation and returned evidence. Explain skipped roles for trivial/inapplicable work; do not manufacture executions.

## Findings retrieval failure and workflow completion

Only after a separate explicit human request execute any agent-assisted SonarQube checks; general increment or full-lifecycle approval does not authorize them. Record
Maven verification, unit/integration tests, Spotless, JaCoCo, applicable mutation testing,
SonarScanner execution/processing, actual Quality Gate, issue retrieval, hotspot retrieval
and independent review separately in the Change Request `03-review.md`. The overall
Make exit alone does not establish the Quality Gate result.

Technical SonarQube failures, including scanner execution, gate verification and findings retrieval, do not automatically abort the Change Request lifecycle. Stop affected operations when mandatory security or approval restrictions apply; preserve actual failed/unverified dimensions and return a structured handoff to the Orchestrator. Continue independent authorized phases. A confirmed failed Quality Gate still requires review or authorized correction.

For a technical findings-retrieval failure, record Issue Retrieval as Failed for a confirmed failed operation or Pending for
unavailable/incomplete evidence, the known root cause (or Unknown), safe diagnostics
and external evidence location, and actual findings count **Unknown**, never zero.
Do not automatically rerun Maven/SonarScanner because collection failed. Permit at
most one targeted `make sonar-findings` retry per Change Request, only when an ordinary
transient failure is established and the retry is authorized; record authorization,
reason, attempt and result. Authentication/permission, unsupported API or provenance
limitations alone do not establish a transient failure. If the conditions are absent
or the retry fails, retain the limitation and proceed with independent authorized work.
Retrieval failure or its retry alone consumes no implementation correction iteration.

Complete remaining authorized development, independent tests/gates, code review and
mandatory documentation consistency checks. Review the available actual findings and
diff; unavailable findings review remains Pending without preventing independent code
review. Preserve a confirmed failed Quality Gate as Failed, an unverified gate as
Pending, and an independently verified matching passed gate as Passed even if collection
fails. Hotspot completeness stays Pending without authoritative evidence. Never promote
statuses, suppress findings or automatically accept security risks to finish execution.

Always produce phase J's final report and update `03-review.md`, including when external
checks remain Failed or Pending. The workflow may reach **Completed — Human Review
Required** after all remaining authorized phases and available checks are completed;
this denotes completed execution, not passed gates or permission to publish. Human
Acceptance remains Pending. A mandatory safety restriction stops the affected operation;
continue independent safe authorized work and report the restriction where permitted.

## Correction limit

The initial implementation is iteration 0. A correction iteration is one selected batch of verified findings, narrowly assigned fixes, focused tests, affected gates and re-evaluation. Maximum: **three correction iterations per explicitly authorized implementation increment after its initial implementation**. Record the increment identifier and 1/3, 2/3 or 3/3 before starting each batch in the Change Request review record. Agent retries, new agents, new chats and restarts do not reset that increment’s counter. An explicitly authorized new increment starts at 0/3 only after its scope and historical counters are recorded. Preserve previous counters and findings; never use a new increment to conceal or relabel repeated unresolved defects. Exhaustion requires escalation to the human before further correction.

Stop early when all required available gates pass and actionable findings are resolved. Pending required gates still block claiming a fully verified result. Every correction changing production behavior requires complete regression again before any green final claim. After iteration 3 of an increment, stop even if findings remain, record failures/Pending gates and request human direction; do not launch a fourth correction under another label. Preserve narrower request-specific limits. A permission blocker pauses dependent work without consuming an unstarted correction.

## Standard record ownership

The [Change Request convention](../change-requests/README.md) defines the mandatory directory, filenames and complete file responsibilities. The Orchestrator creates and maintains that directory, assigns one writer per file and coordinates sequential updates where ownership overlaps. It ensures all four records remain complete and synchronized at every lifecycle transition and on continuation; a final report does not replace them.

| Role | Record contribution |
| --- | --- |
| Orchestrator — requirements | Requirements analysis, acceptance criteria and target/current behavior in `01-documentation.md`. |
| Reviewer — architecture | Read-only architectural findings, decision references and blockers; no decision acceptance. |
| Implementation | Plan, modified components/files, scoped decisions and implementation/test evidence in `02-implementation.md`. |
| Testing | Actual test and verification results for `02-implementation.md` and final gate evidence in `03-review.md`, including limitations and unrun checks. |
| Review | Independent findings, severity, resolution status and re-evaluation for `03-review.md`. |
| Orchestrator — documentation | Consistency across all four records and authoritative documents, preserving history and decision status; reviewer independently checks consistency. |
| Orchestrator | README scope/status/links, ownership, ledger, correction history (maximum three), consolidated gates/risks and human acceptance status. |

Contributions may arrive through handoffs for the assigned writer to incorporate; every agent need not directly edit the records. Read-only architecture and review roles remain read-only. Record human acceptance as Pending until an explicit human decision supplies reviewer, date and revision. Never invent verification or approvals.

## Three-role team — Increment 0, 2026-10-21

The main-session Orchestrator now owns requirements analysis, target documentation and final consistency. The reviewer adds read-only architecture assessment to independent review. Implementation and Testing retain disjoint production/test ownership. These duties replace separate requirements, architect and documenter assignments; the phases above and all approval/gate obligations remain mandatory. Historical setup evidence retains the role names actually exercised then.

Keep the configured cap of three spawned threads, excluding the primary. Normally use no more than two concurrent specialists: implementation and test development on disjoint stable contracts. Run reviewer after writers stabilize; never parallelize Maven builds against the shared target directory. Configure no model/effort overrides. No specialist delegates.

For CH-003 Increment 0 the Orchestrator is the sole writer of all four standard records and all configuration/documentation/hook files; independent reviewers return read-only handoffs. On later increments explicitly assign each file before work; contributions to a record may be returned to its sole writer. Maximum three correction iterations is durable across agent/session restarts.

## Automated local gates and reserved human actions

[Engineering verification](../../05-testing/engineering-verification.md) defines repeatable fast/full commands and evidence. For CH-003, Orchestrator invokes focused/fast checks and complete Maven verify plus independent review per increment, with PIT-enabled full verification only at final migration acceptance under the [policy](quality-gates.md#ch-003-migration-verification-policy). Other production-refactoring requests retain their applicable full gates; the tester validates behavior and reviewer independently assesses architecture/code and matching results. No concurrent builds against shared target; the wrapper lock covers wrapper entry points and the Orchestrator serializes direct Maven/mutation/pre-commit invocations. Failures require in-scope correction or explicit blocked/Pending status; never recommend acceptance with an unresolved failed gate. Scope/architecture approvals and human acceptance are separate.

No agent may stage, commit, push, merge, rebase, reset, deploy, change branch protection/repository settings or invoke/modify Cloud analysis/gates without a separate explicit human request. Native/Git hooks never trigger full verification, PIT, PostgreSQL or Cloud gates. They also never grant approval.


### Human-approved correction-policy amendment — 2026-10-21

The human explicitly approved per-increment budgets before Increment 3 implementation. Historical CH-003 Increment 2 cumulative counter remains 3/3 as originally recorded; it is not retrospectively recalculated. Increment 3 starts at 0/3 after consistent workflow and Change Request updates. This amendment does not accept earlier defects or permit carrying unresolved corrections into a new increment under another name.
