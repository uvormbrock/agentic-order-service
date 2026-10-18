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

## Local SonarQube Cloud Quality Gate

With Java 21 and Docker available, run from the local working tree before staging, committing or pushing:

```sh
make sonar-local
```

This command performs `./mvnw -B -ntp clean verify` followed in the same Maven invocation by scanner `5.8.0.7211`, project `uvormbrock_agentic-order-service` and the existing POM organization `uvormbrock`. It explicitly supplies the actual attached Git branch, waits for the remote Quality Gate (`sonar.qualitygate.wait=true`, timeout 300 seconds), and uploads source/analysis data to SonarQube Cloud. Run only when that external analysis is authorized. Integration tests use disposable PostgreSQL Testcontainers; the command does not start the persistent development database. No staging, commit, push or GitHub Actions change is required.

Supported local branch analyses are `develop` and `main`, matching CI's configured branch names. Other branches, detached HEAD and recognized PR contexts are rejected before credential retrieval. The target never creates a PR analysis; do not inject PR/scanner overrides through Maven settings or environment. Branch analysis availability also depends on the existing Cloud project permissions/subscription. Keep analyzed inputs stable while the command runs.

Authentication uses a nonempty inherited `SONAR_TOKEN` first. Otherwise, on macOS it reads the existing Keychain generic password for account `$USER` (current user fallback if unset) and service `agentic-order-service-sonar`. It never creates/replaces an item. The token is held only in the recipe shell and passed through the Maven and findings-collector process environments, never as an argument or repository file; Git/branch checks run with `SONAR_TOKEN` removed from their environment. Do not use shell tracing, Maven debug/environment dumps or include credentials in evidence. No user profile or global environment is modified.

Both targets reuse this authentication. Missing credentials fail before Maven or API retrieval starts. On non-macOS systems securely supply `SONAR_TOKEN` through the invoking environment. On macOS, missing/empty items, denied access or required approval produce a safe error; resolve the existing item's access through normal macOS controls. A system Keychain prompt may require human interaction; an agent must report it as a blocker rather than bypassing controls. The Codex App may not inherit terminal variables, so its Keychain permissions matter independently.

The recipe propagates Maven's nonzero exit to Make (Make itself normally exits 2 for a failed recipe and displays the recipe exit code). Inspect the output and reports to distinguish Maven verification failure, scanner/upload/processing failure, timeout and an actual failed remote Quality Gate. Scanner startup or upload is not a passed Quality Gate. A missing/unavailable or timed-out result remains Pending; a confirmed failed gate is Failed. Passed requires the actual matching remote gate result and review of relevant findings. A successful Quality Gate does not grant human acceptance.

For future authorized Change Requests, the Testing Agent returns a structured [handoff](../04-agentic-coding/multi-agent/handoff-contract.md) with separate Maven verification, scanner/upload/processing and actual gate outcomes. Record execution date/timezone, analyzed branch, base revision plus staged/unstaged/untracked source identity, exit status, actual gate/analysis identity, dashboard URL if available, evidence locations and outstanding findings/limitations in `03-review.md`. Review independently checks provenance, findings and security risks. Keep safe logs outside the repository and archive reports before another clean run; never copy credentials into evidence. Follow the [gate scheduling policy](../04-agentic-coding/multi-agent/quality-gates.md) and [approval policy](../04-agentic-coding/multi-agent/approval-policy.md).

### Independent current-branch findings

```sh
make sonar-findings
```

This read-only operation uses existing authentication and the same explicit project and supported attached branch checks as `sonar-local`. It retrieves current branch issues and security hotspots without Maven, scanner submission, a scanner task file or a fabricated analysis ID. Maven/scanner execution is Not Applicable with a recorded retrieval-only reason. No analysis identity is reported and the analysis-specific Quality Gate remains Pending. Local HEAD identifies the retrieval workspace, not the remotely analyzed sources. Historical snapshot provenance and supersession verification remain Pending. The standalone collector returns 0 when required issue retrieval is complete, even if hotspot completeness remains Pending; incomplete or failed issue retrieval returns 3. Exit 0 confirms issue retrieval only, not a complete security review. Snapshot/supersession Pending alone is nonfatal and standalone retrieval does not evaluate a gate. Make normally returns 2 for a failed recipe. Failed retrieval never triggers an automatic full analysis rerun; resolve its cause and retry this target deliberately.

### Automated findings and controlled remediation

After Maven returns, `make sonar-local` runs the read-only standard-library [findings collector](../../tools/sonar_findings.py). A failed Quality Gate still allows retrieval when the scanner task was successfully processed. A failed build without a fresh scanner task leaves retrieval Pending. The original Maven failure exit takes priority; if Maven succeeded, incomplete retrieval returns recipe exit 3, and a confirmed failed remote gate returns 1. Make normally returns 2 for any failed recipe. A retrieval error never becomes a zero-issues claim or erases a separately confirmed gate result.

Before verification, the collector creates a private external temporary directory (`sonar-local-*`, mode 0700) and records branch, HEAD, index/content hashes for tracked and untracked nonignored files, the previous scanner task hash and changed-line ranges. It stores no source bodies or raw diffs. Afterward it requires a fresh `target/sonar/report-task.txt`, the intended project/server/branch, successful compute-engine processing, stable local source identity, matching CE task ID/type/project/branch and a nonempty analysis ID. The gate is queried by that analysis ID. The long-lived-only `project_analyses/search` endpoint is not used. Issue search retrieves current branch state, not an immutable historical snapshot. `issueSnapshotProvenance` and `supersessionVerification` remain Pending: another analysis can replace branch findings during retrieval, and unchanged totals cannot exclude that race. Under the explicitly approved limited provenance policy, these two Pending dimensions alone do not cause failure; all existing gate, issue-completeness and hotspot requirements remain mandatory. The collector rejects redirects and uses a bearer header, bounded JSON reads and request timeouts. HTTP failures include a processing stage, allowlisted endpoint path, numeric status and fixed safe category; raw error bodies and query data are excluded. HTTP 400/401/403/404/429, network failures, malformed pages, duplicates, changing totals or the 10,000-result search ceiling remain Pending; retry deliberately after resolving the cause, never silently truncate results.

`findings.json` (mode 0600) contains only selected issue fields: identifier, legacy type/severity, software-quality impacts, rule, file/line, message, status/resolution and classification evidence. It excludes source snippets, flows, authors, assignees, comments and raw HTTP/error bodies. Messages can still be sensitive: keep raw reports outside the repository, review before sharing, and put only safe concise summaries/necessary identifiers in `03-review.md`. The token is never an argument or saved credential; unexpected token echoes are redacted before report serialization. Temporary evidence is not durable archival storage.

The collector retrieves all OPEN/CONFIRMED issues, with no quality-type filter, for project `uvormbrock_agentic-order-service` and the explicit branch. It paginates a second `sinceLeakPeriod=true` query for Sonar new-code membership. Counts include legacy type/severity and modern SECURITY, RELIABILITY and MAINTAINABILITY impacts; a finding can have multiple impacts, so impact counts need not equal issue counts. Accepted, false-positive and fixed issues are outside these open-finding counts; this tool changes no cloud statuses.

Repository candidates compare current files/lines with `SONAR_BASE_REF` (default `HEAD`). For an entire Change Request spanning commits, set that variable to its recorded intake commit; the resolved commit is retained in the evidence. An optional `SONAR_BASELINE_REPORT` path can point to an earlier complete same-project/branch collector report to establish known pre-existing issue identifiers. The current analysis cannot serve as its own baseline. Without that report, baseline membership remains unknown. Sonar's configured new-code period is not the Change Request boundary, a changed file does not prove causation, and an absent baseline ID does not prove a new defect. Every ownership classification remains Pending independent review.

Hotspots requiring review are queried separately with `status=TO_REVIEW`; they are not merged with vulnerability/security-issue counts. Current Cloud API metadata deprecates hotspot search/show in favor of security issues and does not publicly declare a branch parameter, although SonarSource's own migration client supplies it. The collector supplies the intended branch but conservatively leaves non-main hotspot retrieval Pending with null authoritative counts and explicitly unverified candidate details. Only a branch confirmed as the remote main branch can currently have complete hotspot retrieval through this legacy API. Endpoint removal, denied permissions or missing provenance also remain Pending. The mandatory incomplete-hotspot rule remains nonzero for `sonar-local`; standalone `sonar-findings` reports it separately without failing completed issue retrieval. On `develop`, unless it is the confirmed remote main branch, this prevents successful `sonar-local` completion even with complete issues and a passed Quality Gate. Document this blocker separately; the human-approved standalone exit policy does not change this full-scan requirement. Human verification or an approved supported branch-scoped replacement is required to close that gap; never infer zero hotspots from successful issue retrieval.

The Testing Agent executes analysis once at the stable milestone, can refresh findings independently and returns actual finding details to Review in a structured handoff containing the actual gate result, issue/hotspot retrieval statuses, counts by type/severity, software-quality impacts, analysis/dashboard URL, analyzed branch/source state, source baseline, relevant finding candidates and evidence paths. The Review Agent independently determines genuine defects, useful in-scope maintainability corrections, priority security issues, potential false positives requiring human review, and pre-existing out-of-scope issues. No automatic cloud dismissal, suppression, resolution, acceptance or hotspot transition is performed.

The Orchestrator sends only actionable authorized findings to Implementation within the existing maximum three correction iterations. Material architecture, security or scope decisions require human authorization before dependent correction. Re-run affected tests and `make sonar-local` after production corrections; a prior gate result cannot certify revised sources. Avoid duplicate scans of unchanged inputs. Record each iteration's corrected IDs, remaining IDs/reasons, hotspots requiring human review and unavailable/incomplete retrieval as Pending in `03-review.md`. A passed Quality Gate can coexist with open findings and Pending issue review; neither is human acceptance.

API references inspected without credentials on 2026-10-18: [official Cloud authentication and rate-limit guidance](https://docs.sonarsource.com/sonarqube-cloud/appendices/web-api), [live endpoint metadata](https://sonarcloud.io/api/webservices/list), [issues search](https://sonarcloud.io/web_api/api/issues/search), [hotspots search](https://sonarcloud.io/web_api/api/hotspots/search), and [SonarSource's branch-aware migration client](https://github.com/SonarSource/sonar-migration-tool/blob/main/go/internal/extract/tasks_projectdata.go). The current v1 `ce/task`, `qualitygates/project_status` and `project_branches/list` GET endpoints supply task/analysis identity, analysis-specific gate and main-branch provenance; none establishes immutable issue-snapshot provenance; `qualitygates/project_status` is also deprecated but remains listed in the live API. Endpoint changes require revalidation, not guessed success.

Offline tooling verification (no Maven, Keychain or Cloud access):

```sh
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -p 'test_sonar_findings.py' -v
```
