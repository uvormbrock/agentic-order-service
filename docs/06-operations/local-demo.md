# Local Demo Environment

## Quick Start

Run these commands from the repository root. You need:

- **Java 21**, with `JAVA_HOME` set to that JDK.
- A running **local Docker Engine with Compose v2**.
- **GNU Make, Python 3, Git and curl**; Maven is provided by the Wrapper.
- Free ports **15432** (PostgreSQL) and **18080** (application).

macOS is verified; see [platform setup](local-demo-technical.md#requirements) for Java configuration and Linux/WSL2 limitations.

### Terminal 1 — Start Database and Application

```sh
make demo-setup
make demo-run
```

Wait for Spring Boot's `Started` message. `demo-run` packages the application and runs it in the **foreground**; keep this terminal open. This startup command does not run the test suite.

### Terminal 2 — Load Sample Data

After the application has started:

```sh
make demo-seed
```

This creates **5 customers, 8 products, 4 orders and 8 order items** through the REST APIs. It also demonstrates discounts and historical product-price snapshots. See the [dataset](../../demo/data/README.md) for the scenarios. The seed expects referenced Customer DELETE/409, followed by explicit PATCH deactivation. The [approved DELETE change](api-documentation.md#customer-delete-contract-transition--2026-10-18) is implemented in Phase 2; verification evidence and pending human acceptance are recorded in the API guide.

Seeding requires an empty demo database. A second run refuses writes rather than duplicating records or overwriting changes. Do not run concurrent seeds or automatically retry a partial seed.

The demo is separate from the persistent development database. Its data is **temporary and lost when PostgreSQL stops**; do not store valuable data here. Root `.env` and development volumes are not used.

## Try the REST API

Base URL: **http://localhost:18080**.

```sh
curl http://localhost:18080/customers
curl http://localhost:18080/products
curl http://localhost:18080/orders
```

Successful reads return **200** with an `X-Request-ID` header. If `localhost` resolves to IPv6 on your machine, use `127.0.0.1`; the application binds to IPv4 loopback.

The first seeded order has subtotal **EUR 100.00**, discount **EUR 10.00** and total **EUR 90.00**. Its original product snapshot remains unchanged after repricing.

For order creation, updates, validation errors and the endpoint table, use the [additional API examples](local-demo-technical.md#rest-examples). Those write examples intentionally change the baseline record counts.

### Interactive Swagger UI

After `make demo-run` reports successful startup, open [Swagger UI](http://localhost:18080/swagger-ui/index.html). The runner activates the demo-only documentation profile. Use real IDs from list responses; **interactive write requests change demo data**. See [API documentation](api-documentation.md) for the generated specification and drift checks.

## View Data in DBeaver

| Setting | Value |
| --- | --- |
| Database type | PostgreSQL |
| Host | 127.0.0.1 |
| Port | 15432 |
| Database | agentic_order_demo |
| Username | demo |
| Password | demo_only_local |

In DBeaver Community, choose **New Database Connection → PostgreSQL**, enter these settings and select **Test Connection**. Allow the PostgreSQL driver download if prompted, then save.

Navigate to **Schemas → public → Tables**, open `customers`, `products`, `orders` or `order_items`, and choose **View Data → All Rows**. The tables appear after application startup; sample rows appear after `demo-seed`.

These intentionally insecure credentials are **only for the isolated, loopback-bound, disposable demo**. Never use them in production or shared environments.

An SQL editor on this connection can run:

```sql
SELECT id, name, price, active FROM products ORDER BY name;
SELECT id, subtotal, discount, total FROM orders ORDER BY subtotal;
```

## Available Demo Commands

| Command | Purpose |
| --- | --- |
| `make demo-setup` | Validate and start the owned demo PostgreSQL container without recreating it. |
| `make demo-run` | Package and run Spring Boot in the foreground. |
| `make demo-seed` | Create and verify the sample dataset in an empty database. |
| `make demo-status` | Optional diagnostic: show container, JVM and public-table status. |
| `make demo-db` | Open interactive psql on the demo database; exit with `\q`. |
| `make demo-down` | Stop the owned application/container and discard temporary demo data. |

## Stop the Demo

Press **Ctrl-C** in Terminal 1, then run:

```sh
make demo-down
```

You can also run `demo-down` from another terminal; it verifies ownership before stopping the recorded JVM. **All demo data is discarded.** Persistent development data is untouched.

To deliberately start fresh, repeat `demo-setup`, `demo-run` and `demo-seed` after shutdown. There is no automatic reset or Docker volume deletion.

## Troubleshooting

- **Java/Docker unavailable:** check `JAVA_HOME` selects Java 21 and start your local Docker Engine.
- **Connection refused:** wait for startup; use application port **18080** and database port **15432**, both on loopback.
- **Existing legacy password:** follow the [explicit legacy transition](local-demo-technical.md#explicit-transition-from-legacy-random-credentials); it preserves the old configuration and warns before data loss.
- **Seed refused or partially completed:** existing data is preserved; inspect it before choosing an explicit fresh-demo cycle.
- **Port, ownership or stale-PID error:** do not overwrite credentials or remove a live process record to bypass the guard. See [detailed troubleshooting](local-demo-technical.md#troubleshooting); avoid `mvn clean` while the demo is running.

## Further Technical Details

The [technical reference](local-demo-technical.md) covers Compose ownership, credential handling, migration safety, temporary storage, psql, advanced API examples and historical verification evidence. Existing development-database migration/recovery gates remain unchanged.

The [quality report](../05-testing/quality-report.md) records separate application-quality snapshots. This documentation change did not run the application or verification tools, and historical results do not verify a new source revision.
