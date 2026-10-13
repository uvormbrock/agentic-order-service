# PostgreSQL Foundation – Increment 2

**Status:** Reviewed – Accepted

## Objective

Establish the approved PostgreSQL foundation without domain mappings or business behavior.

## Context

Follow [AGENTS.md](../../../AGENTS.md), [architecture](../../02-architecture/system-architecture.md), [technology reference](../../02-architecture/technology-stack.md), and [testing strategy](../../05-testing/testing-strategy.md). ADR-0002 remains Proposed and unverified.

## Approved Scope

PostgreSQL 17.11 Compose development database, Boot-managed JPA/JDBC/Flyway, disabled OSIV, separate Docker-free smoke and real PostgreSQL integration tests through Failsafe. Metadata initialization is authorized only in development/test databases. No application migrations, entities, repositories, transaction coordinator, or business logic.

## Prompt

The specification was supplied across two user messages; the interrupted “Maintain separate” sentence is joined to its continuation below. This complete prompt is immutable; execution sections may be updated.

```text
# Task: Implement Increment 2 – PostgreSQL Foundation

The proposed Increment 2 architecture and implementation scope have been reviewed.

I approve implementation with the following decisions and adjustments.

## 1. Approved Technology Decisions

- Java 21
- Spring Boot 4.1.1
- Maven Wrapper 3.9.9
- PostgreSQL 17.11
- Official Docker image: postgres:17.11-bookworm
- Spring Data JPA / Hibernate
- PostgreSQL JDBC driver
- Flyway with PostgreSQL support
- JUnit 6
- Testcontainers
- Maven Failsafe for integration tests

Use Spring Boot-managed dependency versions where applicable.

Do not introduce additional frameworks or libraries without approval.

## 2. PostgreSQL Configuration

Create a Docker Compose configuration for local development.

Requirements:

- Use the approved PostgreSQL image.
- Bind the published database port to 127.0.0.1.
- Support configurable host port allocation.
- Use a named persistent volume.
- Mount the data volume at /var/lib/postgresql/data.
- Configure a PostgreSQL health check.
- Obtain credentials from environment variables.
- Provide .env.example without real credentials.
- Ensure local .env files are ignored by Git.

Do not remove unrelated containers or volumes.

Never execute docker compose down -v.

## 3. Spring Data JPA and Hibernate

Add the approved dependencies.

Use Spring Boot auto-configuration.

Configure:

- spring.jpa.open-in-view=false
- spring.jpa.hibernate.ddl-auto=validate
- SQL initialization disabled

Do not create domain entities, JPA repositories, or business logic.

Do not implement TransactionTemplate or the locking protocol.

ADR-0002 remains Proposed and implementation-unverified.

## 4. Flyway

Flyway is approved for local development and integration testing.

Enable Flyway only in the local and integration-test profiles.

Explicitly authorize Flyway metadata initialization, including flyway_schema_history, in the project-specific development and disposable integration-test databases.

No domain migration scripts or application tables may be created in this increment.

Keep production migration execution disabled and its strategy Open.

Verify that startup with zero application migrations works with the selected Flyway version.

## 5. Testing

Configure Maven Failsafe to execute integration tests during verify.

Use JUnit 6 and Testcontainers with the approved PostgreSQL image.

Maintain separate testing responsibilities:

- A fast smoke test that does not require Docker.
- PostgreSQL integration tests that require Docker and verify the real database connection.

Preserve the existing smoke-test assertions where appropriate.

If database auto-configuration must be excluded for the fast smoke test, isolate that exclusion to the test configuration and document its reduced verification scope.

Integration tests must verify:

1. PostgreSQL container startup.
2. JDBC connectivity using SELECT 1.
3. Actual PostgreSQL server version.
4. EntityManagerFactory initialization.
5. Open EntityManager in View disabled.
6. Flyway integration and expected metadata.
7. No application migrations or domain tables.
8. Isolation from the local Docker Compose database.

Docker-unavailable integration tests must fail or report the environment as blocked, not silently pass.

## 6. Documentation

Before implementation, create:

`docs/04-agentic-coding/prompts/03-postgresql-foundation.md`

Follow the established prompt-playbook format.

Record this complete approved prompt before executing implementation.

During implementation, update Result, Verification Evidence, Problems Encountered, Lessons Learned, and Interview Talking Points.

Preserve the original prompt as an immutable historical record.

Update the playbook index and relevant architecture/technology documentation.

Do not silently change any Accepted ADR or promote ADR-0002.

## 7. Verification

Use Java 21 and the Maven Wrapper.

Execute:

- Maven dependency resolution
- Maven verify, including Failsafe integration tests
- Docker Compose configuration validation
- Local PostgreSQL startup and health check
- Spring Boot startup against local PostgreSQL
- Flyway metadata verification
- Git whitespace and documentation-link checks

Document actual results separately from expected results.

Stop only containers started for this verification and preserve the development database volume.

## 8. Completion

Report:

- Files created and modified
- Resolved dependency versions
- Test results
- PostgreSQL and Flyway verification evidence
- Docker Compose configuration
- Remaining risks and Open decisions
- Complete Git diff

Do not create a Git commit.

Do not begin Increment 3.

Mark the prompt record as Executed – Pending Human Review.

Wait for explicit human acceptance.
```

## Result

Executed on 2026-10-13 using the existing Java 21.0.6 installation and Maven Wrapper 3.9.9. Increment 2 and its reviewed enhancements are explicitly accepted; final human review is recorded below. No commit was created; Increment 3 has not started.

### Files Created

- `compose.yml`: official PostgreSQL 17.11 image, localhost-bound configurable port, required environment credentials, health check, and persistent named volume mounted at `/var/lib/postgresql/data`.
- `.env.example`: blank user/password placeholders, example database name/port, and guidance that Spring does not load `.env` automatically. Existing Git ignore rules already exclude `.env` and `.env.*` while allowing `.env.example`.
- `src/main/resources/application-local.yml`: environment-based JDBC settings and enabled Flyway.
- `src/test/resources/application-integration-test.yml`: enabled Flyway for isolated integration tests.
- `src/test/java/com/example/agenticorderservice/PostgreSqlTestConfiguration.java`: Spring-managed PostgreSQL container, `@ServiceConnection`, separate database/credentials, random host port, reuse disabled.
- `src/test/java/com/example/agenticorderservice/PostgreSqlFoundationIT.java`: three PostgreSQL foundation integration tests.
- This immutable prompt/execution record, created before implementation.

### Files Modified

- `pom.xml`: approved JPA/JDBC/Flyway/Testcontainers dependencies and Boot-managed Failsafe execution.
- `src/main/resources/application.yml`: OSIV false, Hibernate validate, SQL initialization never, Flyway disabled by default.
- `src/test/java/com/example/agenticorderservice/AgenticOrderServiceApplicationTest.java`: database exclusions isolated to the smoke-test annotation; original MVC/Validation assertions preserved.
- Playbook index, system architecture, technology reference, ADR-0005 (explicit scoped approval/history), testing strategy, implementation plan, and open-items index.

No domain source, repositories, controllers, migration files, domain tables, transaction coordinator, retries, or locking implementation were added. ADR-0002 remains Proposed and implementation-unverified.

## Verification Evidence

Expected results were defined before implementation: a Docker-free smoke test, real isolated PostgreSQL/JPA/Flyway startup, only history metadata, local Compose startup, and cleanup preserving the development volume. The following are actual results, not expectations.

### Maven and Test Results

Commands used command-local Java 21:

```sh
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp dependency:resolve dependency:tree
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp verify
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home DOCKER_HOST=unix:///tmp/agentic-order-service-no-docker.sock ./mvnw -B -ntp test
```

- Dependency resolution/tree: **BUILD SUCCESS**, no explicit dependency-version overrides.
- `verify`: **BUILD SUCCESS**; Surefire ran one smoke test and Failsafe ran three integration tests, with zero failures/errors/skips. Test compilation uses release 21.
- Docker-free `test` with an unavailable Docker endpoint: **BUILD SUCCESS**, one smoke test. It checks web-context/MVC/Validation wiring only, not database readiness.
- Full integration context: PostgreSQL container running, JDBC `SELECT 1` returns 1, actual server version begins with 17.11, EntityManagerFactory open with zero mapped entities, Hibernate validate enabled, SQL initialization disabled, OSIV false and no OSIV interceptor/filter beans.
- Flyway: zero discovered migrations; successful validation; only `public.flyway_schema_history`, with zero rows. No domain tables or migration files exist.
- Integration JDBC URL used random port 54001 and database `agentic_order_service_it`; it was supplied by the test container, independent of local Compose on port 15432/database `agentic_order_service`. No skip-without-Docker option exists; container startup failure fails the integration context. A Docker outage was not induced during the integration run.
- Spring closed JPA and the connection pool before stopping the container. After the test JVM exited, Docker showed only the development Compose container; the test PostgreSQL and Ryuk containers were removed.

### Resolved Dependency Versions

| Component | Actual resolved version |
| --- | --- |
| Spring Boot starters/test integration | 4.1.1 |
| Spring Framework | 7.0.9 |
| Spring Data JPA | 4.1.1 |
| Hibernate ORM | 7.4.5.Final |
| Jakarta Persistence API | 3.2.0 |
| HikariCP | 7.0.2 |
| PostgreSQL JDBC | 42.7.13 |
| Flyway core/PostgreSQL support | 12.4.0 |
| Testcontainers modules | 2.0.5 |
| JUnit Jupiter/Platform | 6.0.3 |
| Surefire/Failsafe | 3.5.6 |

### Local PostgreSQL Verification

Verification supplied generated temporary local-only credentials via a private file outside the repository (`/tmp/agentic-order-service-increment2.env`, mode 0600); no credentials were printed or committed. The file is retained for access to the preserved verification volume. These credentials initialize the volume once; changing environment values later does not change existing database credentials.

```sh
docker compose --env-file /tmp/agentic-order-service-increment2.env config --quiet
docker compose --env-file /tmp/agentic-order-service-increment2.env up -d --wait postgres
docker compose --env-file /tmp/agentic-order-service-increment2.env exec -T postgres sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Actual: configuration valid, PostgreSQL healthy, readiness reports accepting connections. The published port was `127.0.0.1:15432`, demonstrating configurable host allocation. The named volume is `agentic-order-service_postgres-data`.

The wrapper-built JAR was started with Java 21, exported verification environment, `--spring.profiles.active=local --server.port=0`. Actual: application started successfully on random HTTP port 54060, connected to `jdbc:postgresql://127.0.0.1:15432/agentic_order_service`, and initialized JPA. Flyway validated zero migrations and created only its history table.

A read-only `psql` query returned:

```text
SELECT 1: 1
server_version: 17.11 (Debian 17.11-1.pgdg12+2)
public tables: flyway_schema_history
history row count: 0
```

Observed official image digest: `postgres@sha256:3645570cccdfa447589da9f57dd740faa29b30938e861289a5574b6ca6b03826`. This is execution evidence, not a new digest-pinning decision; the approved tag remains in configuration.

Only the application process launched for verification was terminated, gracefully closing JPA/pool. Then:

```sh
docker compose --env-file /tmp/agentic-order-service-increment2.env down
docker volume inspect agentic-order-service_postgres-data --format '{{.Name}}'
```

Actual: verification container/network removed, development volume still present, no running containers left. No volume deletion or unrelated container manipulation occurred.

### Documentation and Git Checks

Standard `git diff --check` and `git diff --cached --check`, plus whitespace checks for new files, pass. Local Markdown file links resolve; five ADR identifiers remain unique and statuses retain their Accepted/Proposed/Open distinctions. The recorded prompt was checked for unchanged content. The complete review diff includes tracked and new files; no files were staged or committed by this increment.

### Local Development Usage

Copy `.env.example` to ignored `.env`, fill in local credentials, and choose an available host port. Compose reads this file; Spring requires the same values exported in its process environment. Use the local profile explicitly. Do not point local/test Flyway at an existing unrelated database. A persistent volume retains initialized credentials and metadata; production migration execution remains disabled/Open.

## Problems Encountered

- Docker socket access and Maven cache/network access required sandbox permission. Authorized retries succeeded; system Java/Maven configuration was unchanged.
- The first PostgreSQL image pull took about 2 minutes 22 seconds; subsequent runs can use the cache. Testcontainers also pulled its standard Ryuk cleanup image, version 0.14.0.
- Flyway logs “No migrations found”; this is expected in this increment and was not suppressed. Actual zero-migration startup and metadata initialization succeeded with Flyway 12.4.0.
- Existing Mockito dynamic-agent warnings persist; tests passed and no unrelated agent configuration was added.

## Lessons Learned

- Keeping database exclusions in one smoke-test annotation preserves a fast test without weakening the full PostgreSQL integration context.
- Boot 4 uses dedicated JPA/Flyway modules and Testcontainers 2 coordinates; inspecting the resolved graph and actual auto-configuration classes avoids copying obsolete package names.
- Flyway creates history metadata even when zero application migrations exist; that change required and received explicit authorization.
- Spring-managed container lifecycle keeps database teardown after JPA/pool teardown, while Failsafe distinguishes real-database evidence from the smoke test.


## Remaining Risks and Open Decisions

Human acceptance is recorded below. Image tags remain mutable; production PostgreSQL deployment, production migration execution/privileges, domain schema, converter choices, OrderItem identity, monetary precision/capacity, email syntax/counting, and retry/locking details remain unresolved where previously Open/Proposed. `verify` requires Docker and an initial image download. Empty-entity JPA validation does not verify domain mappings. Domain, migration-script, transaction-coordinator, and concurrency gates remain Pending. No Increment 3 work is authorized.

## Post-Review Enhancement – PostgreSQL Configuration

**Enhancement status:** Reviewed – Accepted. Final human acceptance is recorded below.

### Original Problem and Selected Approach

The development credentials lived in a temporary file outside the project, and the approved image was hardcoded separately in Compose and Testcontainers.

Copied the existing environment bytes to project-local `.env` with mode 0600, without generating or changing credentials. The original temporary file is untouched. Existing ignore rules exclude `.env`; `.env.example` remains a versioned template without real credentials. The database and named volume were not recreated.

Root `compose.yml`, `services.postgres.image`, is the single executable source of the image value. `PostgreSqlTestConfiguration` reads it using Spring's already available `YamlPropertiesFactoryBean`; a `DockerImageName` bean feeds the container and the integration assertions. This avoids another configuration file, duplicated versions, new libraries, or scripts. Tests retain a separate database/credentials and random port; reading the image does not reuse Compose database settings or lifecycle.

The loader requires a literal official `postgres:<major>.<patch>-bookworm` image. Missing Compose file fails YAML resource loading; a missing/invalid field throws an error identifying `compose.yml` and `services.postgres.image`. A nonexistent image fails container startup rather than skipping tests. Validation does not silently select a default image. The accepted current image remains `postgres:17.11-bookworm`.

### Exact Files Changed by This Enhancement

- Created ignored local `.env` by copying existing credentials; it is excluded from all review diffs and commit scope.
- `compose.yml`: added a comment identifying the authoritative image field; image/configuration values unchanged.
- `src/test/java/com/example/agenticorderservice/PostgreSqlTestConfiguration.java`: reads and validates the shared image value.
- `src/test/java/com/example/agenticorderservice/PostgreSqlFoundationIT.java`: compares the container image against shared configuration and actual server version against the configured tag, removing hardcoded image/version assertions.
- `docs/02-architecture/technology-stack.md`: documents shared image configuration and credential delivery.
- This prompt record: appended enhancement evidence. Original prompt and earlier results are unchanged.

The POM, dependencies, `.env.example`, application configuration, ADRs, domain logic, and business requirements were not changed by this enhancement. Earlier uncommitted Increment 2 changes remain preserved.

### Current Local Development Instructions

Compose automatically reads project-local `.env` from the repository root:

```sh
docker compose config --quiet
docker compose up -d --wait postgres
```

For a new checkout, copy `.env.example` to `.env` and set local-only values. For this existing development database, the values were preserved; do not replace them or delete its volume. Editing environment values does not change credentials already initialized in PostgreSQL.

Spring Boot requires the variables exported into its process. For a trusted, shell-compatible `.env` file, use a subshell to avoid changing the parent shell; do not enable shell tracing or print the values:

```sh
(
  set -a
  . ./.env
  set +a
  JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
)
```

Quote shell-sensitive values appropriately in `.env`. The existing local profile consumes `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, and `POSTGRES_HOST_PORT`. No application dotenv loader was added. Stop the application when finished; `docker compose down` preserves the named volume. Never use volume-deleting teardown.

### Actual Verification Evidence

On 2026-10-13:

- `.env` is byte-identical to the existing temporary verification file, mode 0600, Git-ignored, and not tracked. Secret scanning of tracked/proposed files found none of the real local credential values. The temporary file remained unchanged.
- `docker compose config --quiet` passed with no `--env-file` argument. Safe inspection of its resolved configuration showed `postgres:17.11-bookworm`, host port 15432 bound to 127.0.0.1, and the original data mount.
- `docker compose up -d --wait postgres` succeeded using the existing volume. TCP `psql` authentication used the preserved password without printing it; `SELECT 1` returned 1, actual server version was `17.11 (Debian 17.11-1.pgdg12+2)`, and history row count remained zero.
- The wrapper-built application started with Java 21 and environment values loaded from `.env`, using the local profile. It connected to the existing database and initialized JPA; Flyway validated zero migrations without replacing metadata.
- `env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp verify dependency:tree -DoutputFile=/tmp/increment2-configuration-dependencies.txt`: **BUILD SUCCESS**; one smoke test and all three PostgreSQL integration tests passed, zero failures/errors/skips. The container image matched the image read from Compose, and actual PostgreSQL version matched its tag.
- POM bytes/dependency declarations were unchanged from the enhancement baseline; inspected graph retains Boot 4.1.1, Hibernate 7.4.5.Final, JDBC 42.7.13, Flyway 12.4.0, Testcontainers 2.0.5, and JUnit 6.0.3. No new dependencies or version overrides.
- Only the verification application and project Compose container/network were stopped. Testcontainers cleaned up its test containers. The named development volume retains its original name and creation time, `agentic-order-service_postgres-data`, `2026-10-13T10:05:23Z`; no volume deletion occurred.
- Standard staged/unstaged/new-file whitespace checks and local documentation links passed. The immutable prompt and earlier execution evidence match the pre-enhancement record. Neither staged files nor a commit were created.

### Lessons Learned and Remaining Risks

A literal Compose image field can be reused by Testcontainers without adding an extra settings format. Keeping credentials project-local and ignored simplifies Compose startup while explicit environment export preserves Spring's existing configuration model. Persistent database credentials must be preserved independently of container recreation.

Integration tests now require root `compose.yml` in the Maven project working directory; missing or invalid configuration fails startup clearly. Image tags remain mutable, and Docker is required for `verify`. Existing Mockito and expected zero-migration Flyway warnings persist. Missing/invalid-image failures were reviewed in the loader; the successful build exercised the accepted image, not deliberate malformed-file cases. Production migration and domain/concurrency decisions remain Open/Proposed; ADR-0002 is unchanged. No Increment 3 work or commit is authorized.

## Post-Review Enhancement – Central PostgreSQL Image Configuration

**Enhancement status:** Reviewed – Accepted.

### Approval, Rationale, and Current Configuration

The user explicitly approved replacing the Compose-based image lookup with a neutral properties file. This supersedes the earlier image-loading implementation and local command instructions recorded above; their execution evidence remains historical and unchanged. Integration tests should depend on the approved database image, not a local Compose service name or YAML structure.

Created [config/postgres.properties](../../../config/postgres.properties) with `POSTGRES_IMAGE=postgres:17.11-bookworm`, the single executable image source. Compose now uses `image: ${POSTGRES_IMAGE:?Load config/postgres.properties}` without a fallback. `PostgreSqlTestConfiguration` loads the properties using a standard Java UTF-8 reader and `Properties`, validates the image, and retains the existing Spring-managed container and `@ServiceConnection` wiring. No dependency or wrapper script was added.

The test loader ignores local `.env`, Compose services/YAML, and shell image overrides. Missing/unreadable files and malformed properties syntax produce `Cannot read PostgreSQL image configuration from <path>` with the original cause. Missing/empty/malformed image values produce `<path> must define POSTGRES_IMAGE as a literal official postgres:<major>.<patch>-bookworm image`. Validly formatted but unavailable tags still fail Docker image resolution. No default image is silently selected.

### Exact Changes in This Enhancement

New files:

- `config/postgres.properties`.
- `src/test/java/com/example/agenticorderservice/PostgreSqlImageConfigurationTest.java`: Docker-free missing-file/key, empty/malformed-image, and malformed-properties checks using temporary fixtures.

Modified files:

- `compose.yml`: required image substitution; credential, port, health, and volume settings unchanged.
- `src/test/java/com/example/agenticorderservice/PostgreSqlTestConfiguration.java`: replaces YAML parsing with standard properties loading.
- `docs/02-architecture/technology-stack.md`: current source/consumer contract, required local commands, and override detection.
- This execution record: append-only enhancement history and actual results.

The existing integration assertions already compare against the shared `DockerImageName` bean and derive the server version from its tag, so `PostgreSqlFoundationIT.java` required no additional change in this enhancement. POM, application logic/configuration, dependencies, ADR decisions, `.env` values, and `.env.example` were preserved.

### Current Local Development Commands

From the repository root, always supply both files for Compose configuration, startup, and teardown:

```sh
docker compose --env-file .env --env-file config/postgres.properties config --quiet
docker compose --env-file .env --env-file config/postgres.properties up -d --wait postgres
docker compose --env-file .env --env-file config/postgres.properties down
```

Later environment files override earlier file values; exported shell variables take precedence over environment-file values. Do not intentionally export `POSTGRES_IMAGE` for this project unless separately approved. Detect an exported value without exposing any credentials and inspect only the resolved image:

```sh
if printenv POSTGRES_IMAGE >/dev/null; then
  echo "POSTGRES_IMAGE is exported; unset it to use config/postgres.properties"
fi
docker compose --env-file .env --env-file config/postgres.properties config --images
```

Clear an unintended override with `unset POSTGRES_IMAGE`. Java reads the properties file directly, so an exported shell override could otherwise make Compose and Testcontainers disagree. Bare Compose commands do not automatically load the central properties file; they are not the documented startup contract. The required substitution avoids a duplicated default.

Spring Boot continues to consume the credentials/port from its environment and the `local` profile. For trusted shell-compatible `.env` assignments, export them only in an application subshell without tracing or printing values:

```sh
(
  set -a
  . ./.env
  set +a
  JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
)
```

No new credentials were generated and no database initialization was repeated. The existing ignored `.env` and named volume remain in place; the old temporary verification file is untouched.

### Actual Verification Results

Executed on 2026-10-13 with Java 21.0.6 and Maven Wrapper 3.9.9:

- Before the Maven build, Docker showed no running Compose database. `.env` was temporarily moved outside the project and restored in a `finally` block after verification; PostgreSQL connection/image variables were removed from the Maven process environment. This exercised tests without local credentials or a running local database.
- `./mvnw -B -ntp verify dependency:tree -DoutputFile=/tmp/increment2-central-image-dependencies.txt` under explicit Java 21: **BUILD SUCCESS**. Surefire ran 10 tests (one smoke plus nine configuration checks), and Failsafe ran all three PostgreSQL integration tests. Zero failures/errors/skips.
- Missing configuration file/key, empty and malformed image values, and malformed properties escapes all produced the tested explanatory errors. The valid central file produced the accepted image and actual PostgreSQL 17.11 in integration tests; JDBC/JPA/OSIV/Flyway assertions remained passing.
- The complete dependency-tree output is byte-identical to `/tmp/increment2-configuration-dependencies.txt` from the previous enhancement. POM bytes are unchanged; no versions, dependencies, or plugins were added.
- Compose validation with both environment files passed. No exported `POSTGRES_IMAGE` override was present. Safe resolved-config inspection matched `POSTGRES_IMAGE` in the central file exactly: `postgres:17.11-bookworm`.
- Local Compose startup with both files succeeded against the existing named volume. TCP authentication using existing credentials succeeded; `SELECT 1` returned 1, actual version was `17.11 (Debian 17.11-1.pgdg12+2)`, and Flyway history remained empty.
- Local application startup with exported `.env` values, Java 21, and the local profile succeeded. JPA initialized against the preserved database, and Flyway validated zero migrations without replacing metadata.
- Only verification-owned application/container/network resources were stopped. The development volume retained its original name and creation time (`agentic-order-service_postgres-data`, `2026-10-13T10:05:23Z`). Testcontainers cleaned up its isolated containers. No volume deletion or database recreation occurred.
- `.env` remains ignored, untracked, mode 0600, and byte-identical to the original temporary file. Secret checks found no real local credentials in tracked/proposed files. Standard staged/unstaged/new-file whitespace checks and local Markdown links passed. The original prompt and all earlier execution evidence are preserved unchanged.

### Lessons Learned and Remaining Risks

A small neutral properties file decouples image selection from the development Compose layout. The explicit two-file Compose command is the tradeoff; it is documented rather than hidden in a new script. Shell precedence still requires checking for unexpected image overrides, while Java intentionally uses only the central file.

Tests require `config/postgres.properties` relative to the Maven project working directory and Docker for real-database verification. Image tags remain mutable. Expected zero-migration Flyway warnings and existing Mockito warnings persist. Production migration/schema/concurrency decisions remain unresolved where previously Open or Proposed; ADR-0002 is unchanged. Final human acceptance is recorded below. No commit or Increment 3 work was performed.

## Final Human Review and Acceptance

On 2026-10-13, the user explicitly accepted Increment 2 – PostgreSQL Foundation, including the central PostgreSQL image configuration enhancement, after human review. The Java test configuration and test classes were reviewed; no blocking code corrections were required. The centralized image configuration, Compose setup, and documentation are consistent with the verified implementation.

The original prompt and all build/test, database, configuration, and cleanup evidence above are preserved. Accepted implementation does not resolve the remaining Open/Proposed schema, production migration, or concurrency decisions. ADR-0002 remains Proposed and implementation-unverified. The approved files are prepared for a scoped commit; commit authorization is still pending. No Increment 3 work has begun.
