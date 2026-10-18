# Local Demo — Technical Reference

Use the [developer quick start](local-demo.md) for normal startup, API reads and DBeaver access. This companion preserves implementation details, additional examples, legacy-transition instructions and historical verification evidence. It does not authorize teardown, credential changes or a new verification run.

## Purpose and Isolation

Run the existing REST application on the host with a disposable PostgreSQL database in Docker. This workflow demonstrates accepted application behavior; it does not configure a production deployment or change application code, contracts or Flyway migrations.

The demo uses [its own Compose file](../../demo/compose.yml), project `agentic-order-service-demo`, database `agentic_order_demo` and database user `demo`. PostgreSQL binds to `127.0.0.1:15432`; the application binds to `127.0.0.1:18080`. Its PostgreSQL data directory is a **tmpfs mount**, with no named volume or host data mount. Stopping the container discards the database. Do not store valuable data here.

The root development Compose project, `.env` and persistent volumes are not used. Existing persistent-development migration/recovery gates remain unchanged. Only the isolated demo database is migrated automatically.

[config/postgres.properties](../../config/postgres.properties) remains the single image source. The helper removes inherited Spring/Compose variable overrides from child environments, pins the demo connection explicitly and checks container ownership, image, ports, storage and database identity. A different checkout's container or conflicting port causes refusal rather than replacement.

## Requirements

- Java 21; set `JAVA_HOME` to the installed JDK and add its `bin` directory to `PATH`.
- Docker Engine with Docker Compose v2; Docker Desktop is suitable on macOS.
- GNU Make, Python 3, Git and curl. Maven 3.9.9 is provided by the existing Wrapper.
- Free loopback ports 15432 and 18080 and sufficient Docker memory for PostgreSQL tmpfs storage.

On macOS, `/usr/libexec/java_home -v 21` can locate an installed JDK:

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
```

On Linux, set `JAVA_HOME` to your JDK 21 installation and ensure the Docker daemon is reachable. The helper uses POSIX process signaling and `ps`; native Windows execution is not supported. Use a Linux environment such as WSL2 with Docker integration instead. Linux/WSL2 behavior remains unverified until run there.

## Fresh-Clone Workflow

From the repository root:

```sh
make demo-setup
make demo-run
```

`demo-setup` checks Java/Docker, exclusively creates `demo/.env.demo` with the fixed demo-only password and restrictive permissions, validates Compose and starts PostgreSQL with a health check. It never reads or replaces root development credentials. The demo environment file remains Git-ignored because it also records checkout ownership. Existing credentials are never overwritten. See the explicit transition below if an older demo used a random password.

`demo-run` verifies isolation and port availability, packages with `./mvnw -B -ntp -DskipTests package` and starts the executable JAR in the foreground. This packages the application and checks formatting; it is **not** a test-suite verification. No `local` profile is activated. Explicit demo settings enable Flyway validation/migration, prohibit automatic baseline/clean and retain Hibernate schema validation. Existing V1/V2 create the schema on this fresh database.

Wait for the Spring Boot `Started` message. In a second terminal:

```sh
# Optional diagnostic check:
make demo-status
make demo-seed
```

There is no readiness Actuator endpoint. `demo-status` reports container/application state and public tables; successful API requests establish HTTP readiness. If startup is still in progress, wait before seeding. The application records its owned JVM PID under `target/local-demo/`; a stale PID record requires inspection, not automatic reuse.

## Dataset and Repeatability

The [dataset description](../../demo/data/README.md) and [scenario file](../../demo/data/scenarios.json) define five customers, eight products, four multi-item orders and eight order items. All records are created through APIs; the server supplies UUIDs and calculates amounts. The helper then reprices/renames a product and verifies that its historical order snapshot stays unchanged.

The logical dataset is deterministic; UUIDs vary per fresh database. Generated IDs and original creation responses are saved in `target/local-demo/seed.json`. These responses describe creation time: fetch current state after subsequent deactivations/repricing. No production migration contains seed data.

A second seed run **fails before writes** when any resource list is nonempty. It does not upsert, overwrite edits or replay uncertain writes. A failed/partial seed also requires inspection and an explicit fresh-demo cycle; do not simply retry it. Avoid concurrent seed invocations or API writes while seeding.

## REST Examples

Base URL: `http://127.0.0.1:18080`. After seeding, read IDs in a POSIX shell with Python 3:

```sh
DEMO_CUSTOMER_ID=$(python3 -c 'import json; print(json.load(open("target/local-demo/seed.json"))["customers"][0]["id"])')
DEMO_PRODUCT_ID=$(python3 -c 'import json; print(json.load(open("target/local-demo/seed.json"))["products"][0]["id"])')
DEMO_ORDER_ID=$(python3 -c 'import json; print(json.load(open("target/local-demo/seed.json"))["orders"][0]["id"])')

curl -i http://127.0.0.1:18080/customers
curl -i http://127.0.0.1:18080/products
curl -i "http://127.0.0.1:18080/orders/$DEMO_ORDER_ID"
```

All three return **200** and an `X-Request-ID` header. The first seeded order returns EUR subtotal **100.00**, discount **10.00** and total **90.00**. Its keyboard item retains `productName: "Demo Keyboard"` and `unitPrice: 50.00`, while the current product is `Demo Keyboard v2` at 60.00. JSON numeric formatting may omit trailing zeros.

Create another order using only client-owned fields:

```sh
curl -i -X POST http://127.0.0.1:18080/orders \
  -H 'Content-Type: application/json' \
  -d "{\"customerId\":\"$DEMO_CUSTOMER_ID\",\"items\":[{\"productId\":\"$DEMO_PRODUCT_ID\",\"quantity\":2}]}"
```

Expected **201**, a `Location` header, server-generated `id`, current customer data, item snapshots and `currency: "EUR"`; subtotal **120.00**, discount **12.00**, total **108.00**. This intentional extra order changes the baseline record counts.

Create and update resource examples (these also change the dataset):

```sh
curl -i -X POST http://127.0.0.1:18080/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Demo Visitor","email":"visitor@example.invalid"}'

curl -i -X POST http://127.0.0.1:18080/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Demo Cable","price":12.50}'

curl -i -X PUT "http://127.0.0.1:18080/products/$DEMO_PRODUCT_ID" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Demo Keyboard v3","price":65.00}'
```

Creates return **201** with generated IDs; PUT returns **200**. For example, a created product has the shape `{"id":"<generated UUID>","name":"Demo Cable","price":12.50,"active":true}`. A repeated customer email returns **409**; do not rerun creates expecting idempotent behavior.

A deliberately invalid payload demonstrates strict JSON handling:

```sh
curl -i -X POST http://127.0.0.1:18080/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Rejected Demo Item","price":12.50,"unexpected":true}'
```

Expected **400** with `{"code":"UNKNOWN_FIELD","message":"The request contains an unknown field."}`. Errors use fixed safe templates; no database internals should appear.

| Resource | Implemented methods |
| --- | --- |
| `/customers` | POST, GET |
| `/customers/{id}` | GET, PUT, PATCH `{"active":false}`, DELETE |
| `/products` | POST, GET |
| `/products/{id}` | GET, PUT, PATCH `{"active":false}` |
| `/orders` | POST, GET |
| `/orders/{id}` | GET |

**Current implementation:** successful GET/PUT/PATCH return 200; creates return 201; Customer DELETE returns 204/no body only for physical removal. Referenced DELETE returns 409 CUSTOMER_HAS_ORDERS without changes, followed by explicit PATCH where the seed intends deactivation. Missing IDs retain 404; inactive records cannot enter new orders, and reactivation is unsupported. See the [API guide](api-documentation.md#customer-delete-contract-transition--2026-10-18).

## Inspecting PostgreSQL

```sh
make demo-db
```

Inside psql:

```sql
\dt public.*
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT count(*) FROM customers;
SELECT count(*) FROM products;
SELECT count(*) FROM orders;
SELECT count(*) FROM order_items;
SELECT subtotal, discount, total FROM orders ORDER BY subtotal;
\q
```

Before additional curl writes, expected counts are **5 / 8 / 4 / 8**, with successful Flyway versions **1 and 2**. The user/database names identify only the disposable demo.

## DBeaver Reference

The [main guide's DBeaver section](local-demo.md#view-data-in-dbeaver) contains the fixed demo-only connection settings and GUI steps. An SQL editor on that connection can also run:

```sql
SELECT id, name, active FROM customers ORDER BY name;
SELECT id, name, price, active FROM products ORDER BY name;
SELECT id, subtotal, discount, total FROM orders ORDER BY subtotal;
```

## Explicit Transition from Legacy Random Credentials

An existing demo may still use its previously generated random password. The helper preserves that file and existing data. `demo-status`, `demo-db` and `demo-down` remain available with matching legacy settings, but `demo-setup`, `demo-run` and `demo-seed` refuse the legacy configuration rather than silently applying the new password. The documented fixed password does not authenticate an unchanged legacy database.

Only when you deliberately choose to discard that disposable demo, run these steps yourself from the repository root:

```sh
make demo-status
# This stops the owned JVM/container and permanently discards ALL tmpfs demo data.
make demo-down
# Preserve the old environment file; do not overwrite an existing archive.
test ! -e demo/.env.demo.before-fixed-credentials && \
  mv demo/.env.demo demo/.env.demo.before-fixed-credentials
make demo-setup
make demo-run
# In another terminal, after application startup:
make demo-seed
```

Proceed with `demo-setup` only if the archive operation succeeded. If the archive already exists, stop and choose a different unused archive filename rather than overwriting it. The archive remains Git-ignored; it may contain the old private random password, so keep it private. There is no automatic reset, password alteration or archival. If data needs preserving, export and review it separately before choosing teardown. Never use these steps on the root development database.

## Stop and Safe Fresh-Demo Cycle

Ctrl-C in the `demo-run` terminal stops its JVM. Then:

```sh
make demo-down
```

Alternatively, `demo-down` from a second terminal signals only a PID whose process command matches this checkout's demo owner marker, waits for the foreground runner to confirm shutdown and then stops the container. An unverified PID or failed shutdown prevents database teardown. No process is killed merely because it occupies a port.

**Explicitly choosing `demo-down` discards all demo data.** For a fresh dataset, deliberately run `demo-down`, `demo-setup`, `demo-run` and then `demo-seed` again. There is no automatic reset or volume deletion. Stopping/restarting PostgreSQL also loses tmpfs contents; rerun the application to initialize the schema before reseeding. Root development data remains untouched.

## Troubleshooting

- **Java mismatch:** set `JAVA_HOME` to Java 21. The helper rejects other versions before building.
- **Docker unavailable:** start Docker Desktop/Engine and check your context. No database fallback is attempted.
- **Port occupied:** stop your own conflicting service or choose another workstation; fixed demo ports intentionally have no silent fallback.
- **Ownership/storage failure:** inspect the named demo project. Do not relabel or force-adopt an existing container, mount or another checkout's credentials.
- **Environment file from another checkout:** do not overwrite it. Inspect ownership first; stop that checkout's demo using its own commands. A copied checkout needs its own newly created demo environment after review.
- **Stale application record:** inspect `target/local-demo/app.json` and its PID. If the process is definitively absent, remove only that stale record manually. Never delete it to bypass a live-process conflict; avoid `mvn clean` during a running demo.
- **Seed refused:** existing data is preserved. Inspect it or explicitly choose the fresh-demo cycle.
- **Partial seed / connection timeout:** do not replay uncertain writes. Inspect API/database state, then choose a fresh demo if appropriate.
- **Build or startup failure:** inspect foreground output; the demo does not alter production code, migrations or tests to compensate. The database stays isolated for inspection.
- **Memory pressure:** tmpfs consumes Docker memory. Stop the demo when finished; use only this small dataset.

## Verification Evidence

Verified on macOS with Java 21, Maven Wrapper 3.9.9 and a local Docker Engine on **2026-10-17 (+02:00)**. Source base: `fa457b7b15ae89311fb5e00df379ed5b80992ba7`, branch `develop`, plus the uncommitted demo files described here. These results are not verification of the committed base alone.

| Check | Actual result |
| --- | --- |
| `make help`; `make -n demo-setup` | Passed; existing mutation recipe unchanged byte-for-byte. |
| `make demo-setup` | Exit 0; approved central image, loopback port 15432, healthy PostgreSQL and tmpfs storage confirmed. Root `.env` not used. |
| `make demo-run` startup | Package/Spotless and startup succeeded; V1/V2 migrated and Hibernate validation passed; HTTP bound to 127.0.0.1:18080. Tests were skipped by the documented packaging command. |
| `make demo-status` | Exit 0; owned JVM identified and five approved public tables listed. |
| `make demo-seed` | Exit 0; 5 customers / 8 products / 4 orders / 8 items verified; discount and historical keyboard checks passed. |
| Repeat `make demo-seed` | Expected refusal: helper exit 1 / Make exit 2, before writes. Subsequent SQL confirmed the original counts. |
| `make demo-db` | Interactive psql passed: V1/V2 successful; counts 5/8/4/8; customers 3 active/2 inactive; products 7 active/1 inactive; all four monetary scenarios matched. |
| Documented curl examples | GET 200; Order/Customer/Product POST 201; Product PUT 200; duplicate email 409; unknown JSON field 400 with the exact catalog response. Every checked response contained X-Request-ID. Extra successful writes intentionally changed counts to 6/9/5/9. |
| Repeated setup with inherited image/datasource overrides | Exit 0; reused the owned container with the configured image. Inherited override values were not passed to Compose/application children. |
| Foreign-owner refusal | In-memory guard check rejected foreign metadata before any Docker lifecycle action; no real foreign container was changed. |
| Credentials and static checks | Demo environment Git-ignored, mode 0600; no password printed. Python/JSON syntax, documentation links/anchors/fences, whitespace and Git scope checks passed. |
| Follow-up start/seed/down | Fresh disposable cycle passed; final `demo-down` and foreground `demo-run` both exited 0. No demo container/network or JVM left running; the two original unrelated containers remained unchanged. |

**One focused correction was used:** the initial foreground runner treated the JVM's intentional SIGTERM status 143 as a failure, although shutdown and demo teardown completed. It now accepts normal SIGTERM termination. The single follow-up cycle verified corrected exit reporting with fresh startup, API seeding and clean teardown. Initial and follow-up application/build output is retained temporarily in `/tmp/agentic-local-demo-smoke.log` and `/tmp/agentic-local-demo-followup.log`; these are not durable evidence storage.

Not exercised: DBeaver GUI, Linux/WSL2, Docker outage/remote contexts, memory exhaustion, every ownership/port-conflict branch, and interactive Ctrl-C shutdown. Use a local Docker Engine; remote contexts are not qualified by this evidence. No full regression suite, PIT or SonarQube analysis was run. No persistent development database, existing volume, application source, test, migration, dependency or CI file was changed.

## Fixed Demo Credentials and DBeaver Follow-Up

The fixed-credential change preserves the earlier execution record above as historical evidence. On **2026-10-17**, focused verification established:

- Fresh environment creation writes the documented fixed password and checkout owner with mode 0600. Temporary-file checks verified that repeated setup does not overwrite it and legacy setup refuses before `up`; these checks used mocked Docker actions.
- Effective Compose configuration with a temporary fixed-credential environment resolved database `agentic_order_demo`, user `demo`, the documented password, `127.0.0.1:15432`, tmpfs and no persistent volume.
- A captured application-launch check confirmed the same password and pinned demo datasource URL/username without building or restarting the existing application.
- Mismatched container credentials were rejected by the ownership guard before lifecycle actions.
- A separate temporary PostgreSQL instance with no published host ports or persistent mounts accepted the fixed password over verified TCP and rejected a deliberately wrong password. Only that verification container was stopped/removed.
- The initial authentication probe did not establish TCP transport and also accepted a wrong password, so it was classified as inconclusive. The single focused correction selected an explicit bridge address and required a non-null TCP client address; the corrected probe passed both positive and negative authentication checks.
- The existing legacy demo remained healthy with its original container/JVM and counts **5 customers / 8 products / 4 orders / 8 items**. Its environment file and root `.env` were unchanged; no legacy credentials were displayed, replaced or reset. Real `make demo-status` passed, and dry-run expansion verified all six Make targets.
- Python syntax, documentation links/anchors/fences, whitespace and scope checks passed. No full regression suite, Maven build, PIT or SonarQube analysis was executed.

The DBeaver GUI itself remains unverified. No fixed-credential application was started on the occupied demo port: launch settings were checked without replacing the existing JVM. The running legacy demo intentionally still requires its original password until a human performs the explicit transition above. Temporary verification script: `/tmp/agentic-dbeaver-credential-verification.py`; this is not durable evidence storage.
