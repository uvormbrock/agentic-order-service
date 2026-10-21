# Multi-Agent Engineering Workflow

**Status:** Three-role configuration prepared under CH-003 Increment 0; resulting configuration Human Acceptance Pending. ADR-0007 Option B is subsequently human Accepted; production migration is not authorized.

The main session is the Orchestrator: sole sequencing/delegation authority, requirements analyst, documentation writer, file-ownership coordinator and final gate/report owner. This is an instruction-and-review workflow, not an autonomous state engine. Use [AGENTS.md](../../../AGENTS.md), [workflow](workflow.md), [handoff](handoff-contract.md), [approval policy](approval-policy.md), [quality gates](quality-gates.md), [runtime operation](context-management.md), [hooks](hooks.md) and the [invocation template](change-request-template.md).

## Native roles

| Role | Configuration | Ownership |
| --- | --- | --- |
| Implementer | [implementer.toml](../../../.codex/agents/implementer.toml) | Approved production/configuration files and bounded corrections |
| Tester | [tester.toml](../../../.codex/agents/tester.toml) | Assigned tests/fixtures, independent verification and evidence |
| Reviewer | [reviewer.toml](../../../.codex/agents/reviewer.toml) | Read-only architecture assessment and independent quality review |

Requirements/documentation responsibilities moved to the Orchestrator; architectural assessment moved to the reviewer. The three former project-role files are retired. Historical [setup evidence](setup-verification.md) records the previous six-role setup and is not rewritten as current execution evidence.

[Project configuration](../../../.codex/config.toml) keeps three concurrently open spawned threads (primary excluded). Standalone native role files declare name, description and developer_instructions; models/effort inherit the parent. Prefer two specialists at a time and serialize builds. Check discovery in the actual runtime; configuration alone is not execution proof. Independent review is required; no agent grants human acceptance or Git publication approval.

Use the [Change Request convention](../change-requests/README.md) and exactly its four initial records for authorized requests. CH-001/CH-002 are existing requests; this update neither closes nor accepts them. [CH-003](../change-requests/CH-003-hexagonal-architecture-migration/README.md) covers the approved Increment 0 only.

Native configuration references checked 2026-10-21: [subagents](https://learn.chatgpt.com/docs/agent-configuration/subagents) and [hooks](https://learn.chatgpt.com/docs/hooks). Local CLI/runtime evidence and limitations belong to CH-003, not to those documentation pages.

## Accepted Option B and local quality automation

The human explicitly accepted Option B on 2026-10-21, retaining ADR-0004 mappings and the three-role team. Use [engineering verification](../../05-testing/engineering-verification.md); Orchestrator invokes final full local gates, tester verifies behavior, reviewer independently assesses architecture/code. Cloud gate evaluation and Git publication remain human-controlled, not automatic tasks. The current configuration result and future production increments still require human acceptance.
