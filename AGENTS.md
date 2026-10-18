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
