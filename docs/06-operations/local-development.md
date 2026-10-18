# Persistent Local Development Setup

This optional workflow is for developers who specifically need a persistent local PostgreSQL database. For first-time exploration, use the [isolated disposable demo](local-demo.md) instead.

This guide relocates the existing README procedure; it does not authorize a migration or change the recovery safety gate. The root Compose project and `.env` belong to this persistent setup, not to the demo. The application uses port 8080 here; the demo uses port 18080.

## Prerequisites

- JDK 21, with `JAVA_HOME` pointing to that installation and its `bin` directory on `PATH`.
- Docker Engine and Docker Compose v2 for the development database and PostgreSQL integration tests.
- Git; use the included Maven Wrapper instead of requiring system Maven.
- GNU Make, Python 3 and `shasum` for the optional mutation workflow.

Run commands from the repository root. Confirm the selected Java/Maven environment:

```sh
./mvnw -v
```

## Development Database

For a new local setup, create the ignored environment file without overwriting an existing one:

```sh
test -e .env || cp .env.example .env
```

Edit `.env` locally: set `POSTGRES_USER` and `POSTGRES_PASSWORD`, and review `POSTGRES_DB` and `POSTGRES_HOST_PORT`. Use shell-compatible assignments because the application startup example exports this file. Never commit credentials. For an existing database, preserve its existing credentials and volume.

The shared image reference is defined in [config/postgres.properties](../../config/postgres.properties). Compose uses it for variable substitution; Testcontainers loads it directly with Java `Properties`.

```sh
# Exported shell variables override Compose environment files.
# Remove an unintended image override before resolving configuration.
unset POSTGRES_IMAGE
docker compose --env-file .env --env-file config/postgres.properties config --quiet
docker compose --env-file .env --env-file config/postgres.properties up -d --wait
```

The database port binds to loopback. `config --quiet` validates without printing resolved credentials. Review any exported `POSTGRES_*` values as well: they take precedence over the files.

**Existing database safety:** the `local` profile enables Flyway and applies pending migrations on startup. Before starting against an existing persistent database, satisfy the documented backup/recovery and target-verification gate. That persistent-local migration gate remains blocked in the execution record; disposable Testcontainers verification does not authorize it. See the [local configuration and migration guidance](../02-architecture/technology-stack.md#postgresql-configuration-source-and-local-credentials).

## Build and Start

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

**Current implementation:** Customer DELETE physically removes only unreferenced customers (active or inactive); referenced DELETE returns 409 CUSTOMER_HAS_ORDERS without mutation. Use PATCH {"active":false} for explicit deactivation. See the [API guide](api-documentation.md#customer-delete-contract-transition--2026-10-18) and Phase 2 review evidence for verification and pending human acceptance.

Stop Compose while retaining its named database volume:

```sh
docker compose --env-file .env --env-file config/postgres.properties down
```

