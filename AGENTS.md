# Agent Instructions — Agentic Order Service

## Project and Engineering Baseline
- Java 21, Spring Boot 4.1.1, Maven, PostgreSQL; one-module, feature-oriented modular monolith.
- Preserve documented feature dependencies and public application contracts. Controllers delegate to application operations, never directly to repositories or another feature's persistence internals.
- Protect aggregate invariants and transaction boundaries; prefer simple, maintainable, narrowly scoped changes. Avoid unnecessary abstractions.
- No new dependencies, architecture changes, or database migrations without explicit approval. Never modify unrelated files.
- Accepted baseline includes direct JPA aggregates, immutable value objects, Hibernate/Spring Data JPA, Flyway, application-owned transactions, and JUnit 6 using Boot-managed versions.

## Authorization and Review Workflow
1. Inspect the task, repository state, and **only the authoritative sources relevant to its scope**. Identify blocking Open/Proposed decisions before implementation.
2. Treat a specific, explicit human request as approval **only for its stated scope**. If scope or a blocking decision is unresolved, propose a concise plan and obtain approval before editing. Do not seek duplicate approval for already authorized work.
3. Implement only the approved increment, preserving unrelated changes and established contracts. Do not silently broaden scope.
4. Run proportional verification: focused checks first, full regression and external gates when required by scope or acceptance criteria. Unrun/unavailable checks are **Pending**, never **Passed**.
5. Self-review and report the actual diff (staged, unstaged, untracked), test evidence, limitations, and outstanding decisions. Retain verbose logs in files; summarize relevant failures rather than pasting full output.
6. Human acceptance is required to close an implementation increment; agent self-review is not independent human review. Never stage, commit, push, merge, or deploy without separate explicit authorization for that action.

## Change Request Records
- For tasks explicitly initiated or classified as Change Requests against the existing application, the Multi-Agent Orchestrator must follow the [mandatory Change Request convention](docs/04-agentic-coding/change-requests/README.md) and [record ownership workflow](docs/04-agentic-coding/multi-agent/workflow.md#standard-record-ownership). Create exactly the four standard Markdown files initially, maintain them throughout execution, coordinate one writer per file, and preserve historical CH-001 records.
- Do not automatically classify independent projects, research, questions, standalone documentation discussions or unauthorized tasks as Change Requests. New application features may qualify. Clarify genuinely unclear classification only when it materially changes the workflow.

## Multi-Agent Orchestrator Control
- The team is the main-session Orchestrator plus implementer, tester and read-only reviewer. The Orchestrator owns requirements/documentation; reviewer also assesses architecture. The main Codex session is the Orchestrator; specialists do not sequence phases or delegate. The Orchestrator owns phase sequencing, assignments and structured handoffs, approval boundaries, nonfatal external-tool recovery, Quality Gate consolidation and the final human-review report. Follow the [workflow](docs/04-agentic-coding/multi-agent/workflow.md), [approval policy](docs/04-agentic-coding/multi-agent/approval-policy.md) and [handoff contract](docs/04-agentic-coding/multi-agent/handoff-contract.md).
- Enforce at most three implementation correction iterations per explicitly authorized increment; preserve historical counters and escalate on exhaustion; a retrieval failure alone consumes none. Technical SonarQube failures do not abort independent authorized development, verification, review or documentation. Preserve actual Failed/Pending statuses, Unknown findings counts when retrieval is incomplete and mandatory safety/approval restrictions. Apply the [bounded recovery policy](docs/04-agentic-coding/multi-agent/workflow.md#findings-retrieval-failure-and-workflow-completion); never automatically repeat a full scan because retrieval failed.
- Consolidate all remaining risks in the final report with Human Acceptance Pending. Completed — Human Review Required does not mean every gate passed. No specialist or Orchestrator may stage, commit, push, merge or deploy without separate explicit human authorization.

## Decision and Documentation Authority
- `docs/` is authoritative; business rules belong in the [PRD](docs/01-requirements/product-requirements.md), architectural decisions in uniquely numbered ADRs. Preserve statuses and history; do not treat proposals, passing tests, or scope approval as ADR acceptance.
- Accepted ADR-0001/0003 remain authoritative; ADR-0002 is **Proposed** (transaction/locking protocol not fully approved or verified). ADR-0004/0005 contain Accepted baselines with Open/Proposed subdecisions. Seek approval for blocking decisions.
- Selected transaction direction (TransactionTemplate, READ COMMITTED, deterministic pessimistic locks) does **not** approve ADR-0002's unresolved details. Preserve transaction integrity and never claim committed success before completion.
- Specialized [Java/Spring guidance](docs/04-agentic-coding/java-spring-guidelines.md) and [persistence guidance](docs/04-agentic-coding/persistence-guidelines.md) are Accepted but subordinate to PRD, accepted ADRs, architecture, and approved scope.
- Use relative Markdown links and the existing numbered `docs/00-overview/`–`docs/07-implementation/` structure; do not create parallel documentation hierarchies or a separate Obsidian copy.

## Task-Specific Context Routing
Read `AGENTS.md` and identify relevant files using targeted searches before loading lengthy documents. **The following are pointers, not a mandatory reading list.**
- Business behavior: [PRD](docs/01-requirements/product-requirements.md), [domain model](docs/02-architecture/domain-model.md), relevant feature code/tests.
- REST/OpenAPI: [REST design](docs/02-architecture/rest-api-design.md), [error catalog](docs/02-architecture/api-error-codes.md), relevant controllers/DTOs and API documentation.
- Transactions/JPA/migrations: [transaction conventions](docs/02-architecture/transaction-locking.md), [architecture](docs/02-architecture/system-architecture.md), relevant ADRs and persistence guidelines.
- Java changes: [coding guidelines](docs/04-agentic-coding/coding-guidelines.md), Java/Spring guidelines, relevant code/tests.
- Testing/quality: [testing strategy](docs/05-testing/testing-strategy.md), affected tests and current [quality report](docs/05-testing/quality-report.md) when reporting evidence.
- Dependencies: `pom.xml`, [technology stack](docs/02-architecture/technology-stack.md), relevant dependency tree.
- Workflow/decisions: [AI workflow](docs/04-agentic-coding/ai-development-workflow.md), [implementation plan](docs/07-implementation/implementation-plan.md), [open items](docs/00-overview/open-items.md) **only when relevant**.
- Logging/HTTP lifecycle: [observability playbook](docs/04-agentic-coding/prompts/07-application-logging-observability.md) and affected implementation/tests.

## Logging, Security and Correctness
- Use existing SLF4J/Logback, parameterized meaningful events; avoid noisy entry/exit or duplicate exception logs. Never add a logging framework without approval or print from application code to stdout/stderr.
- Respect recorded log levels, retry/outcome semantics, and status provenance. No reported Order success before transaction commit; distinguish confirmed rollback from uncertain outcome, and never replay an uncertain commit.
- Log only safe, minimal metadata. Never log secrets, authorization/cookie headers, request/response bodies, customer emails, other unnecessary PII, confidential SQL parameters, or unchecked exception contents.
- Preserve the approved server-generated `X-Request-ID`, scoped MDC `requestId`, async/ERROR lifecycle semantics, and cleanup on completion/error. Consult the observability playbook for detailed constraints and pending acceptance limits; do not infer executor propagation.

## Context and Output Economy
- Use a fresh session for each independent change request. A new task should not require prior chat history when source files and approved documents contain the needed facts.
- Search filenames/symbols first; inspect narrow ranges and only expand on demonstrated need. Avoid repository-wide dumps and repeatedly rereading unchanged documents.
- Keep plans, command output, and final reports brief but auditable. Save full logs/diffs as artifacts where needed; report precise paths, relevant failures, and actual verification status.
- Do not omit necessary safety checks or contractual context merely to reduce tokens.

## Language
All project Markdown, code, identifiers, comments, tests, diagrams, Git messages, plans, and agent reports use **English**, unless explicitly overridden. Preserve established technical terms and API identifiers.

## Scoped maintenance tasks:
Bug fixes, static-analysis findings, test corrections and documentation adjustments must not automatically invoke the Multi-Agent Change Request workflow. Use direct, single-agent execution unless the human explicitly requests delegation. Do not introduce new classes, abstractions, dependencies or architectural changes solely to resolve static-analysis findings without explicit approval. When the smallest viable fix exceeds the authorized scope, stop and request approval.

## Architecture migration and hooks
- [ADR-0007](docs/03-decisions/ADR-0007-hexagonal-architecture-migration.md) accepts Option B with retained domain/JPA mapping; ADR-0004 remains authoritative. Increment 0 authorizes configuration/documentation/quality automation only, not production migration. Consult the installed `.codex/skills/hexagonal-architecture/SKILL.md` for migration guidance, subordinate to accepted project decisions.
- Consult the [hook inventory and activation guide](docs/04-agentic-coding/multi-agent/hooks.md). Reminders and handoff checks do not approve edits or replace review. Git hook activation requires separate approval; no automatic staging or formatting.

## Automated local verification
- Follow [verification scheduling](docs/04-agentic-coding/multi-agent/quality-gates.md#ch-003-migration-verification-policy): for CH-003 Increment 1 and later increments, run focused tests/ArchUnit/Spotless and make verify-fast; before acceptance run complete Maven clean verify (including PostgreSQL/Testcontainers and JaCoCo) and independent review. No PIT or PIT-enabled make verify-full during individual increments; reserve make verify-full SCOPE=all and baseline comparison for final migration acceptance. Serialize builds; unavailable required checks stay Pending.
- SonarQube Cloud analysis and gate evaluation remain human-controlled. Do not run scanner/findings targets, change Cloud/CI settings, or stage/commit/push/merge/rebase/reset/deploy without a separate explicit human request. Local verification never grants human acceptance.
