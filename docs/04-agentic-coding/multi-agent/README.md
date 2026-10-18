# Multi-Agent Change Request Framework

**Status:** Setup verification recorded in [setup-verification.md](setup-verification.md); human acceptance Pending. No Change Request executed by setup.

The main Codex session is the Orchestrator and single coordination authority. It selects context, assigns file ownership, tracks dependencies, approvals, findings, verification and a durable correction counter, consolidates actual agent results, and stops for human review. This is a repository workflow enforced through instructions and review, not an external orchestration service or a machine-enforced state engine.

Start with [AGENTS.md](../../../AGENTS.md) and the [existing AI workflow](../ai-development-workflow.md). Use the [invocation template](change-request-template.md) in a fresh chat for each independent request. This setup neither authorizes nor starts Product Images. CH-002 remains reserved for that future feature; the accidentally numbered Sonar records are preserved as [framework setup evidence](sonar-setup-evidence/README.md).

The [mandatory Change Request convention](../change-requests/README.md) governs classification, sequential identifiers, the exact four-file directory and record responsibilities. The Orchestrator maintains these records throughout execution using the [ownership mapping](workflow.md#standard-record-ownership); framework setup creates no Change Request.

## Guide

- [Workflow and role responsibilities](workflow.md)
- [Handoff and durable records](handoff-contract.md)
- [Quality gates](quality-gates.md)
- [Approval policy](approval-policy.md)
- [Context and runtime operation](context-management.md)
- [Final report template](final-report-template.md)
- [Setup evidence and limitations](setup-verification.md)

## Runtime configuration

[Project configuration](../../../.codex/config.toml) enables multi-agent tools and caps concurrently open delegated threads at three. Six role definitions do not mean six simultaneous agents. The Orchestrator remains the parent session; no extra orchestrator file is needed. Models and reasoning effort are deliberately not pinned.

| Role | Configuration | Responsibility |
| --- | --- | --- |
| Requirements | [requirements.toml](../../../.codex/agents/requirements.toml) | Business rules, acceptance criteria and edge cases; scoped target requirements |
| Architecture | [architect.toml](../../../.codex/agents/architect.toml) | Read-only impact and decision analysis; no ADR acceptance |
| Implementation | [implementer.toml](../../../.codex/agents/implementer.toml) | Approved production changes and selected corrections |
| Testing | [tester.toml](../../../.codex/agents/tester.toml) | Acceptance-derived tests, focused checks and regression evidence |
| Review | [reviewer.toml](../../../.codex/agents/reviewer.toml) | Independent read-only defect, contract, security and coverage review |
| Documentation | [documenter.toml](../../../.codex/agents/documenter.toml) | Assigned authoritative Markdown, OpenAPI workflow and consistency |

Official sources checked on 2026-10-18: [subagents](https://learn.chatgpt.com/docs/agent-configuration/subagents), [configuration reference](https://learn.chatgpt.com/docs/config-file/config-reference), [configuration layers](https://learn.chatgpt.com/docs/config-file/config-basic). These document project configuration and custom agents. Actual client/tool support must still be checked at intake; see the setup evidence.
