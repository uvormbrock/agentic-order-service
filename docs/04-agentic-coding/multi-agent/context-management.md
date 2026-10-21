# Context and Runtime Operation

The Orchestrator uses targeted filename/symbol searches, reads AGENTS.md and only applicable authoritative sources, and sends compact handoffs. Fresh specialist contexts receive task-specific source paths, authorization, prerequisites and file ownership. Keep logs in temporary files and return counts/failures rather than dumps. Delegate meaningful independent analysis, implementation or review; keep trivial operations in the parent.

## Tools and configuration

The current desktop chat exposes `collaboration.spawn_agent`, `send_message`, `followup_task`, `wait_agent`, `list_agents` and `interrupt_agent`. Use their actual schemas; `wait_agent` signals updates and returned agent messages contain results. `send_message` communicates with running agents; `followup_task` can trigger an idle specialist. The orchestrator must wait for prerequisites and reconcile returned findings. Tools in another client may differ; inspect them instead of inventing names.

### Historical Sub-Agent Selection and Runtime Compatibility — 2026-10-18

The read-only smoke test on 2026-10-18 confirmed that the tested Codex runtime exposes native role selection through `spawn_agent.agent_type` and recognizes all six configured custom roles: `requirements`, `architect`, `implementer`, `tester`, `reviewer` and `documenter`.

The `requirements` and `reviewer` agents were successfully spawned, completed their assignments and returned actual handoffs. The remaining four roles have not yet been individually spawn-tested. A complete autonomous Change Request lifecycle, including correction loops, remains unverified.

**Agent selection rules:**

- Prefer native role selection when the current client's tool schema exposes the configured agent roles.
- If native role selection is unavailable, use `fork_turns="none"` where supported and explicitly supply the selected role's instructions from `.codex/agents/<role>.toml`, together with the relevant handoff contract and `AGENTS.md` path.
- Record fallback execution as *prompt-scoped role execution*, not as proof of automatic TOML role loading.
- Leave model and reasoning-effort settings unset to inherit the parent configuration.

Project TOML files define agent roles but do not automatically execute or orchestrate the complete Change Request workflow.

After configuration changes, start a fresh trusted-project Codex session and verify role discovery before relying on automatic role loading. Never modify global project trust automatically.

The [setup evidence](setup-verification.md) documents earlier runtime observations and outstanding verification checks. The successful smoke test supersedes the previously recorded custom-role selector limitation **for the tested runtime only**.

Agents share the checkout here. Worktrees are optional, not automatically created by delegation. Assign one writer per file and serialize overlapping edits. Use isolated worktrees only when justified and authorized under applicable instructions; explicitly consolidate/review their diffs. A worktree does not waive approvals.

Custom roles do not form independent security sandboxes. Parent permissions and managed constraints apply. Reviewer read-only instructions are behavioral constraints, not proof of filesystem isolation. Record actual configured versus observed behavior separately.

The current session permits workspace writes, restricts network access and protects `.codex`/`.git`; automatic approval review handles eligible escalations. User-global config reports `approval_policy=never`, `sandbox_mode=workspace-write` and trusted project; session controls take precedence. Authorized local work can proceed until a permission or mandatory decision blocks it. Fully unattended execution is not guaranteed: approvals, Docker/network access, external credentials, human decisions and final acceptance may require interaction. Do not use scheduling, external orchestrators or account changes as workarounds.

## Current Increment 0 configuration — 2026-10-21

Only implementer, tester and reviewer remain project roles. The Orchestrator owns requirements/documentation and the reviewer owns read-only architecture assessment. Earlier six-role spawn observations above are historical, not a claim that retired roles remain available. Do not invoke retired profiles. Native discovery is through standalone .codex/agents TOML files with name/description/developer_instructions. Keep agents.enabled=true and max_concurrent_threads_per_session=3; no user-global settings or trust changes.

Validate actual loading separately from TOML parsing and role execution separately from metadata discovery. Native hooks require reviewed trust; listing/configuration/script smoke tests do not prove event delivery. See [hooks](hooks.md) and the CH-003 review record for current measured outcomes. Never bypass hook trust or start an application migration as a configuration smoke test.

## Accepted Option B and automated local gate scheduling

The 2026-10-21 follow-up accepts Option B in ADR-0007, retains the native three-role team and updates its instructions. Earlier Proposed/option-pending observations describe the initial Increment 0 state. Production migration remains separately gated. Use [engineering verification](../../05-testing/engineering-verification.md) at stable milestones; final local gates belong to the Orchestrator, not every tool/agent stop. SonarQube analysis/evaluation remains human-controlled; do not invoke Cloud actions under general increment authorization. Native hook discovery/trust/delivery remain separate observations.
