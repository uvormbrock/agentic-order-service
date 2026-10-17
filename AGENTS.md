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
- Comply with documented package dependency directions and feature-owned public contracts; do not access foreign feature persistence internals.
- Keep controllers separate from persistence: delegate to application operations rather than repositories.
- Protect aggregate invariants through controlled domain operations; do not bypass aggregate ownership or expose mutable internals.
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
- Do not silently resolve Open decisions or treat Proposed architecture choices as Accepted, including subdecisions within Accepted baseline ADRs.
- Obtain explicit human approval for any Open or Proposed decision that blocks the implementation increment before implementing the affected work. Scope approval does not implicitly resolve an unspecified blocking decision.

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

For relevant Java/Spring implementation planning, also read the [Java and Spring guidelines](docs/04-agentic-coding/java-spring-guidelines.md); for JPA mappings, PostgreSQL schemas, and migrations, read the [persistence guidelines](docs/04-agentic-coding/persistence-guidelines.md). Both specialized guidelines are **Accepted implementation guidance** subordinate to the PRD, Accepted ADRs, architecture, and approved coding/workflow rules. Their acceptance does not approve a new implementation increment, introduce dependencies, authorize schema changes or migrations, or resolve Open/Proposed decisions (including the ADR-0002 protocol). Obtain explicit approval for blocking decisions and the relevant implementation scope before coding.

Accepted technology and architecture decisions: Java 21, Spring Boot 4.1.1, one Maven module, feature-oriented modular monolith, direct JPA aggregates, immutable value objects, Hibernate/Spring Data JPA, PostgreSQL, Flyway, and application-owned transaction boundaries. JUnit 6 is Accepted and replaces the earlier JUnit 5 selection; use Spring Boot 4.1.1-managed Jupiter and Platform versions without overrides to retain JUnit 5. Explicitly Proposed/Open subdecisions in ADR-0004 and ADR-0005 remain unresolved.

Selected technical direction for transaction management and locking: TransactionTemplate, READ COMMITTED, and deterministic pessimistic locking. This selection does not accept the complete protocol or its unresolved configuration. ADR-0002 remains Proposed and implementation-unverified; its detailed locking/retry implementation and blocking Open decisions require explicit approval before the affected implementation. Accepted transaction-boundary conventions remain governed by ADR-0003 and the transaction documentation.

Do not equate a recorded proposal, documentation approval, passing tests, or package sketch with approval to implement or accept an ADR. New implementation increments require explicit scope approval. Proposed subdecisions remain proposals even inside an Accepted baseline ADR.

Keep business rules authoritative in the PRD and decisions authoritative in their uniquely numbered ADRs. Preserve ADR history; do not reuse identifiers or maintain duplicate specifications. Use relative Markdown links and English Mermaid labels. No separate Obsidian copy is maintained.

Before requesting human acceptance, check document references and statuses and report applicable quality-gate evidence and limitations with the actual diff. No autonomous migrations, dependency additions, architecture changes, or Git commits are authorized by this documentation baseline.

## Logging and Observability

- Consider significant events, failures and retry decisions in every implementation plan; follow the accepted logging design. Do not require a logger in every class or emit noisy entry/exit or duplicate exception logs. Prefer meaningful structured events. The [Increment 6 playbook](docs/04-agentic-coding/prompts/07-application-logging-observability.md) records Human Approved L1–L6 and separate implementation authorization; follow its final lifecycle qualifications and current execution limitations. Implementation acceptance remains separate.
- Use the existing Spring Boot SLF4J/Logback infrastructure and parameterized logging. No new logging framework without an explicit architecture decision; no application `System.out.println`, `System.err.println` or stack-trace printing.
- TRACE is normally disabled; DEBUG supports troubleshooting; INFO records significant completed events; WARN records recoverable abnormal conditions and approved scheduled transient retries; ERROR records unexpected technical failures or operations that cannot complete. Levels reflect actual outcomes; ordinary validation 400s and business 404s are not automatically ERROR.
- Preserve transaction/locking/retry behavior. Never report committed Order success after save/flush alone: require successful transaction completion. Distinguish confirmed rollback, scheduled retry, exhaustion and unknown/potentially committed outcome. Never label uncertainty as confirmed failure/rollback or replay because of logging. Logging failures must not compromise business outcomes or mask original failures.
- Allowlist safe, minimal metadata. Never log secrets, credentials/tokens/keys, Authorization/Cookie headers, complete request/response bodies, customer email or unnecessary PII, confidential SQL parameters, arbitrary sensitive paths or unreviewed exception contents. No generic request-body logging; minimize identifiers and correlation data.
- HTTP correlation requires an explicit documented strategy. Increment 6 approves one server-generated UUID, response header X-Request-ID, owned MDC key requestId and explicit REQUEST/ASYNC/ERROR and request-worker scoping; inbound IDs are ignored. Follow the playbook for status provenance and lifecycle limits. Remove owned context at completion and on exception paths to prevent reused-thread leakage; do not assume executor propagation.
- Test relevant levels, safe fields, correlation creation/cleanup, post-commit success, retry/unknown-outcome accuracy and diagnostic deduplication where practical. Exact prose is not a project-wide API without approval. Keep logging proportionate and preserve privacy/transaction guarantees; do not silently resolve Open observability decisions.

## Documentation Structure

The numbered `docs/00-overview/` through `docs/07-implementation/` structure is authoritative. Inspect it before creating or relocating documentation. Do not create parallel `docs/adr/`, `docs/architecture/`, `docs/standards/`, or `docs/implementation/` hierarchies.
