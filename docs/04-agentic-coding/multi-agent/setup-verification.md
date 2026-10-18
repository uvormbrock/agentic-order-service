# Framework Setup Verification — 2026-10-18

**Status:** Multi-Agent Framework — Setup Complete, Pending Human Review (supported configuration and prompt-scoped delegation; native role auto-loading and full lifecycle unverified). This record covers configuration/documentation setup only, not a business Change Request. No business Change Request was executed. Sonar setup records were later mistakenly labeled CH-002; the classification was corrected on 2026-10-18 and the original records moved to [preserved Sonar setup evidence](sonar-setup-evidence/README.md). CH-002 remains reserved for future Product Images.

## Baseline and authorized scope

Human request: “Setup — Multi-Agent Change Request Orchestration Framework”, supplied in this setup chat on 2026-10-18. It explicitly authorized repository configuration/docs, official documentation research and a harmless delegation smoke test on `develop`. It excluded production Java, business contracts, dependencies, migrations, application test/regression/PIT/Sonar execution, global settings, staging/commits/publication and PRs.

Baseline: branch `develop`, HEAD `2fa999a6713b9073aa8378c01051fab6bea3aada`; staged, unstaged and untracked status empty. Reviewed root AGENTS.md, the AI workflow, existing Change Request convention and CH-001 lifecycle/review evidence. CH-001 history remains unchanged. No blocking business/architecture decision is introduced by this setup; future blocking ADR details remain subject to human decision.

## Discovered capabilities

| Capability | Observed support / limitation |
| --- | --- |
| Installed runtime | `codex --version`: `codex-cli 0.151.0`; package metadata confirms aarch64-apple-darwin variant. Exact desktop bundle/build could not be established from available local metadata; the current desktop chat's actual tools were inspected. |
| Enablement | `codex features list`: `multi_agent stable true`; `multi_agent_v2 stable false`. Project config explicitly sets the documented `features.multi_agent=true`; no experimental feature enabled. |
| Project config | Official configuration layers support trusted `.codex/config.toml`. Installed app-server strict configuration read resolves project multi-agent true and concurrency 3 (see evidence below). |
| Custom roles | Official subagent documentation specifies standalone `.codex/agents/*.toml` with `name`, `description`, `developer_instructions`. Six files use that schema. No models, permission overrides or external services configured. Native automatic discovery/selection in this already-running desktop chat remains Pending. |
| Delegation | This chat exposes collaboration spawn/message/followup/wait/list/interrupt tools. Actual read-only child returned a compact handoff. Spawn has no custom-role selector; documented fallback supplies the selected TOML instructions in a fresh task prompt. |
| Context/config scope | Actual spawn supports fresh context. Models/effort left inherited. Parent sandbox applies; read-only role instructions do not establish separate security isolation. Current agents share checkout; separate worktrees are optional. |
| Capacity | Current tool context allows four active agents including parent. Configuration caps delegated threads at three; roles execute sequentially as dependencies require. No claim that six specialists run at once. |
| Unattended operation | Workspace writes are permitted but `.codex` is protected, network restricted and eligible escalations use automatic review. Global defaults were inspected by selected keys only: approval never, workspace-write, trusted project. No global settings changed. Permission errors, credentials, blocking decisions and human acceptance prevent a general unattended guarantee. |
| Activation | Fresh trusted-project session recommended for config discovery; already-running tool schemas are not rewritten by saving TOML. Native role loading and complete autonomous lifecycle remain unverified. No extra installation/account change is required by this setup. |

The [official sources](README.md#runtime-configuration) establish supported syntax and locations; runtime observations establish only the checks actually performed. Config-file role registries were unnecessary because standalone role files are documented. A separate orchestrator agent is unnecessary because the parent session owns coordination. No external orchestration application was added.

## Runtime configuration check

Generated local protocol schemas using `codex app-server generate-json-schema --out /tmp/multi-agent-setup/schema`. The generated `ConfigReadParams` documents `cwd` and `includeLayers` and the request schema documents `config/read`.

Initial normal-home app-server attempt failed to initialize SQLite under protected `~/.codex`; it did not validate runtime loading. Repeated safely with a temporary `CODEX_HOME=/tmp/multi-agent-setup/codex-home`, containing only a trust entry for this repository, and `codex app-server --strict-config --stdio`. This preserves all user-global settings and avoids copying credentials. Sent only initialize/initialized and read-only `config/read` for repository cwd; no model turn, application task, database operation or business Change Request ran. Server initialized successfully; effective result contained:

```text
max_concurrent_threads_per_session: 3
features.multi_agent: true
```

This proves the installed runtime accepts/resolves the project config under a trusted isolated home. It does not prove that the desktop app loaded six standalone roles. Temporary scripts, protocol schemas and sanitized result are in `/tmp/multi-agent-setup/`; these are disposable local evidence, not committed archival logs.

## Actual multi-agent execution

| Actual task ID | Assigned work | Returned evidence |
| --- | --- | --- |
| `/root/setup_smoke` | Fresh read-only specialist read AGENTS.md and Change Request convention, no edits/tests/Git/database operations | Read command exit 0; returned SETUP handoff with scope, sources, prerequisites, findings, status, next action and blockers. Confirmed separate human acceptance and Git authorization; no blockers. |
| `/root/setup_smoke` follow-up | Independent read-only setup review of new roles/workflow/templates | Returned: no actionable policy/role contradictions; authorization, correction cap, handoff/report completeness and relative paths reviewed. This file was still being written; parent validated it afterward. |

These are prompt-scoped smoke/review assignments, not six native configured-role invocations. Correction iterations for a business Change Request: Not Applicable; no implementation cycle executed.

## Setup gate results

| Gate | Status | Evidence / limit |
| --- | --- | --- |
| Seven TOML files parse | Passed | Python 3.11 tomllib; seven files; exact documented fields/types and role names/paths checked |
| Supported keys / file locations | Passed | Official standalone schema and config reference compared with actual files; strict runtime config/read succeeded |
| Desktop compatibility | Passed | Actual delegation and return supported; prompt-scoped fallback documented. Native TOML role auto-loading Pending, exact desktop build unknown |
| Role/AGENTS alignment and authorization gates | Passed | Parent self-review plus actual independent read-only specialist; no contradictions found |
| Correction cap, handoff and final report completeness | Passed | Independent review confirms maximum three persists across agents/chats/restarts, required handoff/report fields and human-only acceptance |
| Markdown links / relative paths | Passed | Parent local validation of final files; 40 relative file links; fragment check recorded below |
| Read-only delegation smoke | Passed | Actual `/root/setup_smoke` returned its handoff |
| Git hygiene / complete diff review | Passed | git diff --check; new-file whitespace/newline checks; staged empty; complete intended file inventory reviewed |
| Native automatic role discovery and selection | Pending | Current spawn schema has no role selector; validate in a new trusted client session before relying on it |
| Complete autonomous Change Request lifecycle | Pending | Intentionally not executed; parsing/smoke cannot prove it |
| Application compilation, formatting, unit/MVC/integration/concurrency/full regression, OpenAPI regeneration, PIT and Sonar execution | Not Applicable | Configuration/docs-only scope; application checks explicitly excluded |
| Human acceptance | Pending | Only explicit human review can accept setup |

## Exact deliverables and diff scope

Added `.codex/config.toml` and `.codex/agents/{requirements,architect,implementer,tester,reviewer,documenter}.toml` (seven configuration files). Added this record and the eight requested Markdown files: README, workflow, handoff-contract, quality-gates, approval-policy, context-management, final-report-template and change-request-template. Modified only `docs/04-agentic-coding/ai-development-workflow.md` to link this framework. Total: 16 added files, one modified file, no removals. All setup files remain unstaged; no staging, commit, push, merge, PR or deployment occurred.

The linked guides contain the role responsibilities, phases A–J, durable handoff ledger, approval matrix, maximum-three correction policy, existing Maven/OpenAPI/Sonar integration design, context/file ownership rules, invocation prompt and final report template. Final human review must inspect the full new-file contents as well as the tracked diff.

## Final verification and review

Python 3.11 `tomllib` parsed all seven configuration files; validation asserted the exact supported field sets, types, six role names, file locations and concurrency value. A local script checked all 40 relative file links across the nine new Markdown files and the modified workflow, plus trailing whitespace/newline hygiene including untracked files. `git diff --check` passed; branch remains develop and staged diff is empty. Link fragments were separately checked against target Markdown headings. Validation scripts/results are retained temporarily under `/tmp/multi-agent-setup/`.

The independent reviewer returned no actionable findings in the eight workflow guides and six role definitions. The parent reviewed this evidence record afterward and reconciled the earlier Pending link-to-record check. Actual invocation/return is proven for one specialist with a follow-up; six native configured roles and runtime concurrency enforcement are not proven. Setup correction iterations: 0; no behavior implementation or correction cycle occurred.

Final diff: one tracked workflow file has four added lines; 16 new files contain configuration and guides/evidence only. No removals, staged changes or unrelated modifications. Full new-file inventory and sanitized diff artifacts are retained in `/tmp/multi-agent-setup/final-status.txt`, `file-inventory.txt` and `full.diff`; temporary paths are not durable evidence guarantees. The exact deliverable inventory above remains the durable summary.

Human review: inspect seven TOML files and nine new Markdown files plus the four-line workflow link; confirm instruction-based approval/correction controls and the limitations. Recommended next step after human review is a fresh trusted-project chat to check native role discovery before relying on native selection. No business request is started by that recommendation. Human acceptance remains **Pending**; no mandatory setup blocker remains for the documented supported fallback. End-to-end autonomous execution and native desktop role discovery remain **Pending**, not established by parsing or smoke review.
