# Agent Instructions

## Project
Agentic Order Service

## Technology
- Java 21
- Spring Boot 4.1.1
- Maven
- PostgreSQL

## Engineering Principles
- Prefer simple and maintainable solutions.
- Follow established domain boundaries.
- Avoid unnecessary abstractions.
- Do not introduce new dependencies without approval.
- Keep changes small and reviewable.

## Agent Workflow
1. Analyze the task and read the authoritative documents.
2. Present the implementation plan and obtain explicit human approval before coding. Existing approval applies only to its exact scope.
3. Implement only the approved increment, preserving unrelated changes.
4. Run applicable automated verification; report future or unavailable gates as Pending, never Passed.
5. Self-review and present the actual Git diff, including staged, unstaged, and new files, with verification results for human review.
6. Obtain explicit human acceptance before considering the implementation increment complete. Agent self-review does not replace human review.
7. Create no Git commit without explicit authorization; acceptance alone is not commit authorization.

## Restrictions
- No autonomous database migrations.
- No architecture changes without approval.
- Do not modify unrelated files.

## Language Conventions
- All project documentation must be written in English.
- All Markdown files, including README files, architecture documentation, ADRs, and technical specifications, must be written in English.
- Source code, identifiers, comments, docstrings, and test names must be in English.
- Git commit messages must be written in English.
- Technical diagrams, including Mermaid diagrams, must use English labels.
- Agent-generated plans, implementation summaries, and review reports must be written in English.
- Shell commands and configuration examples must use English descriptions and comments.
- Do not translate established technical terms or API identifiers.
- These conventions apply to all future changes unless explicitly overridden.

## Authoritative Engineering Documents

Repository `docs/` is the single source of truth. Read these documents before proposing or implementing changes:

1. [Product requirements](docs/01-requirements/product-requirements.md) and relevant acceptance criteria.
2. [System architecture](docs/02-architecture/system-architecture.md), [technology stack reference](docs/02-architecture/technology-stack.md), [domain model](docs/02-architecture/domain-model.md), and [transaction conventions](docs/02-architecture/transaction-locking.md).
3. Relevant ADRs linked in system architecture, especially Accepted ADR-0001/0003, Proposed ADR-0002, and ADR-0004/0005 with Accepted baselines and explicitly Proposed/Open subdecisions.
4. [Coding guidelines](docs/04-agentic-coding/coding-guidelines.md), [testing strategy](docs/05-testing/testing-strategy.md), and [AI workflow](docs/04-agentic-coding/ai-development-workflow.md).
5. [Implementation plan](docs/07-implementation/implementation-plan.md), [open items](docs/00-overview/open-items.md), and [REST design](docs/02-architecture/rest-api-design.md).

Accepted baseline: Java 21, Spring Boot 4.1.1, one Maven module, feature-oriented modular monolith, direct JPA aggregates, immutable value objects, Hibernate/Spring Data JPA, PostgreSQL, Flyway, application-owned transactions, TransactionTemplate, and deterministic READ COMMITTED pessimistic locking. ADR-0002 remains Proposed and implementation-unverified. JUnit 6 is Accepted and replaces the earlier JUnit 5 selection. Use Spring Boot 4.1.1-managed Jupiter and Platform versions without overrides to retain JUnit 5.

Do not equate a recorded proposal, documentation approval, passing tests, or package sketch with approval to implement or accept an ADR. New implementation increments require explicit scope approval. Proposed subdecisions remain proposals even inside an Accepted baseline ADR.

Keep business rules authoritative in the PRD and decisions authoritative in their uniquely numbered ADRs. Preserve ADR history; do not reuse identifiers or maintain duplicate specifications. Use relative Markdown links and English Mermaid labels. No separate Obsidian copy is maintained.

Before requesting human acceptance, check document references and statuses and report applicable quality-gate evidence and limitations with the actual diff. No autonomous migrations, dependency additions, architecture changes, or Git commits are authorized by this documentation baseline.

## Documentation Structure

The numbered `docs/00-overview/` through `docs/07-implementation/` structure is authoritative. Inspect it before creating or relocating documentation. Do not create parallel `docs/adr/`, `docs/architecture/`, `docs/standards/`, or `docs/implementation/` hierarchies.
