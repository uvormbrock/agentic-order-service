# Agent Handoff Contract

The Orchestrator sends this compact structure with the assignment and receives it completed. Use repository-relative paths, section/symbol references and concise evidence, not full conversation histories.

```text
Change Request ID: CH-NNN (SETUP for framework verification only)
Agent role / actual runtime agent ID:
Task objective:
Authorized scope: files, allowed actions, exclusions, authorization reference
Relevant file paths and authoritative source references:
Dependencies and prerequisites: required handoffs, approved decisions, stable revision
Completed work: edits/findings, or none at assignment
Evidence and verification status: Passed / Failed / Pending / Not Applicable
  actual command/method, execution date/timezone, exit code, counts, source-state/diff identity,
  result/evidence path, limitations and open findings; or reason unrun/inapplicable
Open findings: ID, severity, file/line, evidence, suggested corrective action
Required next action: owner and outcome
Blocking decisions: explicit human decision required, or none
```

The Orchestrator maintains a task ledger in `03-review.md`: actual agent ID, role, task, assigned files, prerequisites, status, returned handoff and next owner. Track Pending/Running/Returned/Blocked tasks explicitly; reconcile all running agents before final review. Only the Orchestrator delegates; specialists do not create nested agents. Communicate follow-ups and wait for dependent results through tools actually exposed by the client. Do not treat a sent assignment as completed work.

## Durable versus temporary evidence

Use the [mandatory Change Request convention](../change-requests/README.md) and [record ownership mapping](workflow.md#standard-record-ownership). The Orchestrator creates exactly `README.md`, `01-documentation.md`, `02-implementation.md` and `03-review.md` initially in `docs/04-agentic-coding/change-requests/CH-NNN-short-descriptive-slug/`, then maintains their required content throughout the lifecycle. Assign one writer per file; incorporate specialist handoffs without conflicting concurrent edits. Additional evidence files require genuine need and never replace these records. Update the register only for an actual authorized Change Request; setup reserves no CH identifier. Preserve historical CH-001 records unchanged.

Store verbose runtime logs under `/tmp/CH-NNN-<run>/` with unique run IDs. These are temporary and may disappear. Commit-ready Markdown summaries must retain commands, timestamps, exits, counts, relevant failures and revision/diff identity so they remain auditable without temporary logs. Durable evidence artifacts, when necessary and safe, belong within the same request directory and must be explicitly listed; no secrets, bodies or PII. Preparing records never authorizes staging or committing them. Preserve earlier failures and label superseding runs; do not rewrite history. On continuation, recover state/counter from these records before delegating.
