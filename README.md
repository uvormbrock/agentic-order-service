# Agentic Order Service

A Java 21 / Spring Boot REST backend demonstrating **agentic software engineering with human-controlled architecture, implementation and quality assurance**.

The application manages customers, products and orders, with a focus on transactional consistency, PostgreSQL concurrency control, bounded retries and observability. It follows a feature-oriented modular monolith architecture.

Developed with OpenAI Codex under explicit human review and acceptance controls, the project serves as a practical engineering demonstration rather than a commercial e-commerce platform. Production deployment readiness has not been established.

## Key Engineering Features

- **Customer, Product and Order APIs:** create, query and update customers/products, deactivate products, delete or deactivate customers according to order references, and place/retrieve orders.
- **Strict HTTP contracts:** dedicated request/response DTOs, nested JSON validation, rejected caller-supplied prices and deterministic error codes with safe response messages.
- **Domain invariants:** immutable monetary values, consolidated order quantities, historical product snapshots and a 10% discount at a subtotal of **EUR 100.00 or above**.
- **Transactional consistency:** `TransactionTemplate`, READ COMMITTED, ambient-transaction rejection, Customer-first pessimistic locking and deterministically ordered Product locks. Product/Customer writes retain optimistic `@Version` checks.
- **Conservative retries:** at most three complete attempts; only approved transient failures after confirmed rollback and resource release. Uncertain commit outcomes are never automatically replayed.
- **Persistence:** directly mapped JPA aggregates, PostgreSQL constraints and versioned Flyway migrations; Open EntityManager in View is disabled.
- **Observability:** SLF4J application events, generated `X-Request-ID` correlation, scoped MDC handling and synchronous/asynchronous Servlet lifecycle support. Increment 6's remaining acceptance limits are explicit below.

Controllers delegate to application operations. Features expose narrow application contracts instead of accessing another feature's persistence internals; response data is materialized inside transactions.

## Technology Stack

Versions below come from the current build and image configuration. Components marked **Boot-managed** follow Spring Boot dependency management rather than independent version pins.

| Category | Technology | Version / integration |
| --- | --- | --- |
| Language | Java | 21 |
| Backend | Spring Boot | 4.1.1 |
| HTTP | Spring MVC; Jakarta Bean Validation | Boot-managed |
| Servlet runtime | Embedded Apache Tomcat | 11.0.26; explicit security override |
| Database | PostgreSQL | 17.11; official `postgres:17.11-bookworm` |
| Persistence | Spring Data JPA; Hibernate ORM; HikariCP | Boot-managed |
| Schema evolution | Flyway with PostgreSQL support | Boot-managed; V1/V2 migrations |
| JDBC | PostgreSQL JDBC driver | 42.7.14; explicit security override |
| JSON | Jackson | BOM 3.1.7; explicit security override |
| Logging | SLF4J; Logback; MDC | SLF4J Boot-managed; Logback 1.6.5 override |
| Build | Maven Wrapper | Maven 3.9.9; wrapper scripts 3.3.4 |
| Test framework | JUnit 6; Mockito; AssertJ; Spring Boot Test | Boot-managed |
| Test execution | Maven Surefire; Maven Failsafe | Boot-managed; `*Test` / `*IT` separation |
| Database tests | Testcontainers PostgreSQL; Docker Engine | Boot-managed Testcontainers; Docker required |
| Coverage | JaCoCo | 0.8.15; report generated during `verify` |
| Mutation testing | PIT Maven; PIT JUnit platform adapter | 1.30.1; `pitest-junit5-plugin` 1.2.3 |
| Formatting | Spotless; Palantir Java Format | 3.10.4; 2.102.0 |
| External analysis | SonarQube Cloud; Maven scanner | Scanner 5.8.0.7211 in GitHub Actions |

The PIT adapter's artifact name does not change the project's JUnit 6 runtime. PIT is opt-in; SonarQube analysis is a separate scanner invocation. Neither runs as part of ordinary `./mvnw verify`.

## Quality Assurance

JUnit 6, Mockito, AssertJ and Spring Boot Test support domain, application and MVC verification. Maven Surefire runs the fast tests; Failsafe runs integration tests, including isolated PostgreSQL databases managed by Testcontainers. JaCoCo measures execution coverage, while separate PIT mutation runs assess whether selected tests detect behavioral changes.

The standard verification command is:

```sh
./mvnw -B -ntp clean verify
```

Spotless / Palantir Java Format checks formatting during the build. SonarQube Cloud provides separately invoked static analysis; it does not run in ordinary Maven verification. CI publishes analysis without waiting for the remote quality gate.

See the **[Quality Report](docs/05-testing/quality-report.md)** for dated test results, coverage, mutation outcomes, static-analysis evidence and limitations. Results apply to their recorded source snapshot and do not automatically verify later changes or certify production readiness.

## Getting Started

### Prerequisites

- JDK 21, with `JAVA_HOME` pointing to that installation and its `bin` directory on `PATH`.
- Docker Engine and Docker Compose v2 for the development database and PostgreSQL integration tests.
- Git; use the included Maven Wrapper instead of requiring system Maven.
- GNU Make, Python 3 and `shasum` for the optional mutation workflow.

Run commands from the repository root. Confirm the selected Java/Maven environment:

```sh
./mvnw -v
```

### Development Database

For a new local setup, create the ignored environment file without overwriting an existing one:

```sh
test -e .env || cp .env.example .env
```

Edit `.env` locally: set `POSTGRES_USER` and `POSTGRES_PASSWORD`, and review `POSTGRES_DB` and `POSTGRES_HOST_PORT`. Use shell-compatible assignments because the application startup example exports this file. Never commit credentials. For an existing database, preserve its existing credentials and volume.

The shared image reference is defined in [config/postgres.properties](config/postgres.properties). Compose uses it for variable substitution; Testcontainers loads it directly with Java `Properties`.

```sh
# Exported shell variables override Compose environment files.
# Remove an unintended image override before resolving configuration.
unset POSTGRES_IMAGE
docker compose --env-file .env --env-file config/postgres.properties config --quiet
docker compose --env-file .env --env-file config/postgres.properties up -d --wait
```

The database port binds to loopback. `config --quiet` validates without printing resolved credentials. Review any exported `POSTGRES_*` values as well: they take precedence over the files.

**Existing database safety:** the `local` profile enables Flyway and applies pending migrations on startup. Before starting against an existing persistent database, satisfy the documented backup/recovery and target-verification gate. That persistent-local migration gate remains blocked in the execution record; disposable Testcontainers verification does not authorize it. See the [local configuration and migration guidance](docs/02-architecture/technology-stack.md#postgresql-configuration-source-and-local-credentials).

### Build and Start

With Docker available, build and verify the application:

```sh
./mvnw -B -ntp clean verify
```

Spring Boot does **not** load `.env` automatically. For a new development database, or an existing database whose migration gate has been approved, export the trusted local file before startup:

```sh
set -a
. ./.env
set +a
./mvnw -B -ntp spring-boot:run -Dspring-boot.run.profiles=local
```

The application runs on port 8080 and needs the configured PostgreSQL database. The `local` profile enables Flyway validation/migration; Hibernate validates the schema. Flyway is disabled in the default application configuration, and production migration execution remains a separate decision.

| Resource | Implemented operations |
| --- | --- |
| `/products` | POST, GET; GET/PUT/PATCH on `/products/{id}`; PATCH deactivates |
| `/customers` | POST, GET; GET/PUT/PATCH/DELETE on `/customers/{id}` |
| `/orders` | POST, GET; GET on `/orders/{id}` |

Customer DELETE physically deletes an unreferenced customer or deactivates a referenced one. See the [PRD](docs/01-requirements/product-requirements.md) and [API error catalog](docs/02-architecture/api-error-codes.md) for request fields, status codes and fixed error templates.

Stop Compose while retaining its named database volume:

```sh
docker compose --env-file .env --env-file config/postgres.properties down
```

## Running Tests and Quality Checks

### Automated Tests and Regression Verification

```sh
# Fast Surefire tests: domain, application, configuration and MVC; no Docker.
./mvnw -B -ntp test

# Complete regression: Surefire, Failsafe, formatting check and coverage report.
./mvnw -B -ntp clean verify

# Failsafe integration tests independently, after compiling test sources.
./mvnw -B -ntp test-compile failsafe:integration-test failsafe:verify

# Explicit formatting validation; apply formatting only when intentionally editing.
./mvnw -B -ntp spotless:check
./mvnw -B -ntp spotless:apply
```

Surefire runs `*Test`; Failsafe runs `*IT`, including PostgreSQL and embedded-Servlet lifecycle tests. PostgreSQL tests require Docker, use isolated Testcontainers databases and do not require local `.env`, a running Compose database or persistent development data. Context teardown stops the containers. Spotless also checks formatting in Maven's `validate` phase.

`verify` generates JaCoCo HTML at `target/site/jacoco/index.html` and XML at `target/site/jacoco/jacoco.xml`. To regenerate reports from existing execution data:

```sh
./mvnw -B -ntp jacoco:report
```

### Automated Mutation Testing

```sh
make mutation-test SCOPE=order
make mutation-test SCOPE=product
make mutation-test SCOPE=customer
make mutation-test SCOPE=shared
make mutation-test SCOPE=all
```

`SCOPE` defaults to `all` and restricts **production mutation targets**. Order targets `order.domain.*`; Product, Customer and Shared target their respective feature packages; All targets `com.example.agenticorderservice.*`.

Every scope selects the project-wide fast `*Test` suite for the baseline and PIT discovery. Cross-feature assertions remain eligible; PIT uses coverage to select tests for individual mutants. **PostgreSQL `*IT` tests are excluded**; persistence mutation testing remains future work.

The Makefile checks Java 21, runs a green baseline and verifies nonempty, failure-free Surefire XML before PIT. It activates `mutation-order` with `DEFAULTS`, one thread and HTML/XML reports. Each execution creates unique evidence under the sibling `agentic-order-service-mutation-evidence` directory, preserving logs, exit codes, selected test counts, Git state, the Makefile copy and checksums. Override the external root with `MUTATION_EVIDENCE_ROOT`.

Mutation testing is separate and potentially expensive. There is no configured numeric mutation-score threshold: Maven success can include survivors. Review meaningful behavioral gaps, retain timeouts separately and preserve prior evidence. See the [automated runbook](docs/05-testing/mutation-testing-runbook.md#current-automated-workflow) and [verified experiments](docs/05-testing/mutation-results/mutation-testing-experiments.md).

### SonarQube Analysis

The existing GitHub Actions workflow invokes the scanner after `clean verify`. For a separately authorized local analysis, configure `SONAR_TOKEN` securely in the environment and set `SONAR_PROJECT_KEY` to the existing project's key from that workflow:

```sh
./mvnw -B -ntp clean verify \
  org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar \
  -Dsonar.projectKey="$SONAR_PROJECT_KEY" \
  -Dsonar.qualitygate.wait=false
```

This publishes analysis to the external service. It does not wait for the remote quality-gate result, and a clean SonarQube result does not constitute mutation-testing evidence.

## Human-Controlled Agentic Development

Codex assists with scoped implementation, review, testing and documentation. Humans own architecture decisions, approval of implementation scope and final acceptance.

The workflow defines explicit acceptance criteria, bounded review/correction rounds, automated verification and traceable execution evidence. Unavailable gates remain Pending; a passing build does not grant acceptance. Freeze decisions constrain further work, and commit/push authorization remains separate.

See [AGENTS.md](AGENTS.md), the [AI development workflow](docs/04-agentic-coding/ai-development-workflow.md) and the [prompt playbook](docs/04-agentic-coding/prompts/00-index.md) for the actual approval history and execution records.

## Architecture and Further Documentation

- **Requirements and boundaries:** [PRD](docs/01-requirements/product-requirements.md), [system architecture](docs/02-architecture/system-architecture.md), [domain model](docs/02-architecture/domain-model.md).
- **Persistence and concurrency:** [transaction/locking conventions](docs/02-architecture/transaction-locking.md), [technology reference](docs/02-architecture/technology-stack.md), [REST design](docs/02-architecture/rest-api-design.md).
- **Engineering practice:** [Java/Spring guidelines](docs/04-agentic-coding/java-spring-guidelines.md), [persistence guidelines](docs/04-agentic-coding/persistence-guidelines.md), [testing strategy](docs/05-testing/testing-strategy.md), [mutation testing strategy](docs/05-testing/mutation-testing.md).
- **Progress and decisions:** [implementation plan](docs/07-implementation/implementation-plan.md), [open items](docs/00-overview/open-items.md), [increment execution records](docs/04-agentic-coding/prompts/00-index.md).

## Project Status

Customer and Product management, Order placement and concurrency handling have been implemented, verified and formally accepted.

Application observability is functionally stabilized and frozen, with selected acceptance evidence and operational requirements still pending.

The project is intended as an engineering demonstration. Production deployment readiness has not yet been established.

See the [Quality Report](docs/05-testing/quality-report.md), [architecture documentation](docs/02-architecture/system-architecture.md) and [open items](docs/00-overview/open-items.md) for detailed verification evidence, architectural decisions and outstanding work.
