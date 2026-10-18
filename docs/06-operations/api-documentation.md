# Interactive API Documentation

## Start and Explore

Start the application with the [isolated local demo quick start](local-demo.md#quick-start): `make demo-setup`, then `make demo-run`, and `make demo-seed` in a second terminal after startup. The demo runner activates the `demo` profile.

- [Swagger UI](http://localhost:18080/swagger-ui/index.html)
- [Runtime OpenAPI JSON](http://localhost:18080/v3/api-docs)
- [Runtime OpenAPI YAML](http://localhost:18080/v3/api-docs.yaml)
- [Versioned OpenAPI YAML snapshot](../02-architecture/openapi.yaml)

If localhost resolves to IPv6, use `127.0.0.1`. Existing running applications must be restarted to load the new dependency/profile. Stop only the application with Ctrl-C and rerun `make demo-run` if you want to preserve the running demo database; `demo-down` discards its data. Existing legacy credentials still require the separate [explicit transition](local-demo-technical.md#explicit-transition-from-legacy-random-credentials).

Swagger UI groups the actual operations under **Customers**, **Products** and **Orders**. Use list/read operations to obtain real IDs, then replace the illustrative UUIDs in request examples before trying writes. **Try it out / Execute sends real HTTP requests**: creates, updates, deactivations and deletes change demo data. Seeding will subsequently refuse a nonempty database.

## Contract and Schema Authority

Runtime generation scans only the three feature API packages and `/customers`, `/products` and `/orders` paths. It documents 14 operations on six paths, excluding `/error`, documentation routes, test fixtures and persistence internals. The schema follows the actual request/response records; no Java or Jakarta validation behavior was added. The application has a validation starter, but its ordered custom JSON decoder and domain/application validation own these constraints.

The shared metadata derives exact error examples from the existing `ApiError.Code` messages. It includes required fields, strict JSON rules, canonical UUIDs, false-only deactivation, numeric limits, generated `Location` and `X-Request-ID` headers, current customer data and historical product/price snapshots. Unsupported methods/routing errors are described without inventing operations for them. Success examples are illustrative, not stored records or guarantees of generated IDs.

Some accepted requirements cannot be fully expressed by JSON Schema: names are counted after Unicode stripping; email normalization precedes validation; prices reject exponent spelling and more than two source decimal places; quantities require integer tokens; duplicate items have a consolidated quantity limit. Descriptions explain these rules instead of adding inaccurate raw-string limits or changing validation. The [PRD](../01-requirements/product-requirements.md), [REST design](../02-architecture/rest-api-design.md) and [error catalog](../02-architecture/api-error-codes.md) remain authoritative. Older planning statements in those records do not override implemented, accepted behavior.

## Customer DELETE Contract Transition — 2026-10-18

**Approved target contract:** DELETE physically removes an unreferenced active or inactive customer and returns 204/no body. Referencing orders require 409 CUSTOMER_HAS_ORDERS, preserving all customer/order state; absence retains CUSTOMER_NOT_FOUND/404. PATCH {"active":false} remains explicit deactivation. See the [authoritative five-case matrix](../01-requirements/product-requirements.md#customer-delete--approved-target-contract-2026-10-18) and [exact code/message](../02-architecture/api-error-codes.md#approved-customer-delete-target--2026-10-18).

**Current implementation:** referenced DELETE returns the exact CUSTOMER_HAS_ORDERS/409 envelope without changes. The shared OpenAPI metadata includes this example alongside CONCURRENT_MODIFICATION, retaining CUSTOMER_NOT_FOUND and the strict two-field ApiError schema. PATCH, demo-only exposure and six business paths/14 operations are preserved.

**Phase 2 evidence:** the [review record](../04-agentic-coding/change-requests/CH-001-customer-delete-semantics/03-review.md) records runtime HTTP generation, snapshot equivalence, reference checks, Swagger UI checks and any Pending gates. Historical Phase 1 and earlier execution records below retain their original scope. Human acceptance is pending.

## Exposure and Dependency Configuration

The single added direct dependency is `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1`. Its [official documentation](https://springdoc.org/) describes the Spring Boot 4 integration. Swagger metadata libraries and UI assets arrive transitively; no Maven lifecycle plugin or CI integration was added. The resolved Swagger integration uses Jackson 2 managed by Boot alongside the application's existing Jackson 3 override; existing versions were not changed.

`application.yml` disables API docs and Swagger UI. Only `application-demo.yml` enables them; `make demo-run` activates that profile while retaining its pinned loopback datasource and explicit fresh-demo Flyway flags. The `local` persistent-database profile was not changed. A non-demo MVC guard also denies the documentation paths and bundled Swagger WebJar aliases, including when the static resource handler could otherwise serve them.

These are **demo-only** endpoints. Do not activate the demo profile in production. Disabling documentation is not authentication or a production security design; the existing API exposure/deployment decisions remain separate. The dependency remains packaged even when documentation routes are disabled.

## Regenerate the Versioned Snapshot

The running application is the source of the YAML; do not hand-edit a competing specification. After changing documentation metadata, restart the demo application and run from the repository root:

```sh
openapi_tmp=$(mktemp)
curl --fail --silent --show-error \
  http://127.0.0.1:18080/v3/api-docs.yaml --output "$openapi_tmp" &&
  cp "$openapi_tmp" docs/02-architecture/openapi.yaml
```

Before publishing, run the focused `OpenApiDocumentationTest` and `OpenApiDisabledTest` checks, which validate every local component reference in JSON/YAML and the demo-only exposure boundary. Only copy after a successful download, compare the snapshot with the running application using the drift check below, inspect the resulting Git diff and retain the generated YAML as a reviewable snapshot. Key ordering is enabled and the server URL is relative (`/`), avoiding host/port-specific changes. Do not place credentials or real customer data in documentation examples.

## Lightweight Drift Check

With the current application running, compare generated output without overwriting the snapshot:

```sh
curl --fail --silent --show-error http://127.0.0.1:18080/v3/api-docs.yaml |
  diff -u docs/02-architecture/openapi.yaml -
```

No diff and exit 0 means an exact match for that running application. A difference or unavailable runtime fails the comparison. This is an explicit review command, not an automatic CI gate and not proof of compatibility with an independently modified application.

## Initial Integration Verification — Historical

**Historical outcome: incomplete.** The initial integration had an unresolved schema reference and did not publish a YAML snapshot. This outcome is superseded by the separately authorized schema-resolution verification below; the original execution evidence is retained here.

- Initial sandbox execution could not initialize Mockito's self-attachment; the focused tests were rerun outside the sandbox without a code change for that infrastructure restriction.
- The initial focused suite exposed a test-side YAML charset-decoding error. The single code correction changed response parsing to UTF-8 bytes. The corrected **10 focused executions passed**, including operation/schema metadata, JSON/YAML parsing and equivalence, UI/config availability and seven default-disabled documentation/asset routes.
- Live verification used a new PostgreSQL container with tmpfs and a dynamically allocated loopback port, plus an independent demo-profile JVM on a random loopback HTTP port. Swagger UI HTML/bundle, Swagger config and JSON/YAML endpoints returned 200. Discovery found six resource paths, 14 operations and the expected operation tags.
- An initial runtime probe incorrectly required an optional top-level tag array; the probe was adjusted to check the actual operation tags. This did not change application code or the required Customers/Products/Orders grouping.
- **Initial blocking result:** runtime reference validation found `#/components/schemas/ApiError` in error responses, but the referenced component was absent. At that point, the response was not a valid complete OpenAPI description. The existing focused checks did not validate every reference; their green result did not close that gap. Live create/strict-validation checks placed after reference validation were not reached.
- The initial allowed correction cycle was used; no second correction was attempted under that authorization. The proposed next scope was: register the reusable error schema in the final customizer and add explicit reference-resolution coverage, then validate the runtime output before snapshot generation. That proposal required separate human authorization, subsequently granted for the correction recorded below.
- Both verification JVM/container instances were removed normally; the original container inventory was preserved. The existing demo's bookkeeping was backed up before the authorized full regression clean and restored afterward.

The single authorized Java 21 Maven Wrapper `./mvnw -B -ntp clean verify` execution completed successfully on **2026-10-17T14:28:30+02:00**: **538 Surefire + 148 Failsafe = 686 executions**, zero failures/errors/skips. The new documentation tests are included in the Surefire count. Spotless's lifecycle check passed. These results belong to base revision `4a9e31f3cb99c01b0ef602c868337861c4aa403f` plus this uncommitted increment, not the base commit alone. That green regression result did not resolve the independently detected invalid OpenAPI reference or supply the missing snapshot. Evidence is temporarily retained in `agentic-openapi-evidence-gp9ehila` under the system temporary directory: focused/package logs and reports, runtime logs/command, container inventories and preserved demo bookkeeping. Temporary storage is not durable archival evidence. PIT and SonarQube were not run, and no prior increment or ADR acceptance status was changed.


## Schema Resolution Verification — 2026-10-17

**Status: schema-reference blocker resolved; integration pending human review.** This was a separately authorized focused correction, not a change to application error handling or the REST contract.

springdoc 3.1.1's `AbstractOpenApiResource.getOpenApi()` removes unreferenced component definitions before invoking `OpenApiCustomizer`. The original bean registered `ApiError` before its response references existed, so pruning removed the schema; the later customizer introduced references to that missing component. The correction registers the same shared schema in `actualApiContracts()` alongside the error-response references. It retains exactly the required string fields `code` and `message`, with no additional properties. No error DTO, controller behavior, status code, business rule, dependency or exposure setting changed in this correction.

The new reference-integrity regression reproduced the defect before the correction (one execution, one assertion failure naming `#/components/schemas/ApiError`). After correction, **11 focused executions passed**, with zero failures/errors/skips: four OpenAPI documentation tests and seven default-disabled route cases. The regression recursively resolves every local `#/components/...` reference in both generated JSON and YAML and checks the shared error schema explicitly.

Packaged-application verification used a fresh tmpfs-backed PostgreSQL container and an independent demo-profile JVM, both bound to random loopback ports. Swagger UI HTML/bundle, Swagger configuration and JSON/YAML endpoints returned HTTP 200. All **189 component references (13 unique targets)** resolved; six paths and 14 operations remained grouped under Customers, Products and Orders. Live reads and creates succeeded, including the EUR 100.00 subtotal / EUR 10.00 discount / EUR 90.00 total example; strict unknown-field and false-only deactivation checks still returned the expected errors. Only those verification resources were removed, and the original container inventory remained unchanged.

The [versioned YAML snapshot](../02-architecture/openapi.yaml) was downloaded from that running application without manual specification edits. Parsing with the existing Swagger YAML mapper confirmed equality with runtime JSON; all local component references resolved. A second runtime download matched the snapshot byte for byte. The snapshot uses relative server URL `/` and contains only the six implemented resource paths.

The single full Java 21 Maven Wrapper `./mvnw -B -ntp clean verify` run completed at **2026-10-17T14:37:54+02:00**, exit 0: **539 Surefire + 148 Failsafe = 687 executions**, zero failures/errors/skips. Spotless passed and JaCoCo generated its report. Existing Mockito dynamic-agent warnings and the previously recorded Tomcat async-fixture callback warning remain outside this scope; the 12 lifecycle executions passed. Existing demo bookkeeping was preserved across `clean` and restored byte for byte.

These results describe base revision `4a9e31f3cb99c01b0ef602c868337861c4aa403f` plus the uncommitted OpenAPI integration and this focused correction. Logs, XML reports, runtime output, schema-lifecycle source excerpt, before/after checksums and review diffs are temporarily retained in `openapi-schema-resolution-zzbogswx` under the system temporary directory. Temporary storage is not durable archival evidence. PIT and SonarQube were not run; no files were staged or committed, and no ADR or prior increment acceptance status changed.
