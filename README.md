# Agentic Order Service

This repository starts with project documentation. Source code (`src/`) and Maven configuration (`pom.xml`) will follow later.

## Documentation

The repository `docs/` directory is the single source of truth. Open it directly in Obsidian; Codex reads the same Markdown files. Source implementation has not started.

- [Open items](docs/00-overview/open-items.md)
- [Product requirements](docs/01-requirements/product-requirements.md)
- [System architecture and ADR status matrix](docs/02-architecture/system-architecture.md)
- [Technology stack reference](docs/02-architecture/technology-stack.md)
- [Domain model](docs/02-architecture/domain-model.md)
- [Transaction and locking conventions](docs/02-architecture/transaction-locking.md)
- [REST API design](docs/02-architecture/rest-api-design.md)
- [Historical snapshots](docs/02-architecture/order-item-snapshots.md)
- [Coding guidelines](docs/04-agentic-coding/coding-guidelines.md)
- [Testing strategy](docs/05-testing/testing-strategy.md)
- [AI development workflow](docs/04-agentic-coding/ai-development-workflow.md)
- [Implementation plan](docs/07-implementation/implementation-plan.md)
- Deferred evidence areas: [Agentic Coding](docs/04-agentic-coding), [Testing](docs/05-testing), [Operations](docs/06-operations).

Java 21 and Spring Boot 4.1.1 are accepted. ADR-0002 remains Proposed and implementation-unverified; implementation needs scoped approval. JUnit 6 is Accepted with Spring Boot 4.1.1-managed Jupiter and Platform versions; ADR-0005 records the replacement of the earlier JUnit 5 selection.

The project folder can be opened as an Obsidian vault.
