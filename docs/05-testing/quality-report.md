# Quality Report — Agentic Order Service

This report separates historical execution results from the stable project overview. The original sections were compiled from existing evidence on 2026-10-17; later dated entries retain their own provenance. No tests, builds, PIT or SonarQube calls were executed for the 2026-10-21 architecture documentation update.

## SonarQube Screenshot and ArchUnit Count — 2026-10-22

The developer supplied a SonarQube Cloud summary screenshot and saved a cropped status view as [sonarqube_2026_10_22.png](../../assets/screenshots/sonarqube_2026_10_22.png). The asset was inspected and embedded unchanged. The full supplied summary identifies branch **main**, source revision **41f9abdd** and a private project. The capture date is 2026-10-22; the relative “15 hours ago” analysis label is not treated as an exact analysis timestamp or analysis ID. No authenticated Cloud query, new scan or badge-token retrieval was performed.

Visible evidence: **Quality Gate Passed** for “Sonar way for Agentic AI” and **AI Code Assurance passed**. The selected **New Code** tab shows **0 new issues, 0 accepted issues, 0 security hotspots, 0 dependency risks, 0.0% duplication and 95.72% coverage**. The screenshot also notes a changed quality profile; this task made no quality-profile or gate change. These are the captured new-code metrics, not independently verified overall-code counts. The developer's earlier report of no remaining findings is retained separately; the unselected Overall Code tab is not inferred to be empty from this image. Cloud new-code coverage and local JaCoCo line/branch coverage have different scopes and are not interchangeable.

Read-only parsing of current `target/surefire-reports/TEST-*.xml` confirmed the developer's **88 ArchUnit test executions across nine architecture suites**: ArchitectureComplianceTest 10; CustomerArchitectureTest 6; HexagonalArchitectureRulesTest 29; HexagonalConsolidationArchitectureTest 11; OrderPlacementArchitectureTest 6; OrderReadArchitectureTest 7; ProductGetArchitectureTest 8; ProductImageArchitectureTest 5; ProductWriteArchitectureTest 6. This total includes rule-fixture checks and parameterized executions; it is not 88 distinct architectural rules or an additional population on top of the fast-suite count. No tests were rerun for this documentation update.

The README now names the implemented **pragmatic hexagonal architecture (ADR-0007 Option B)** explicitly in At a Glance. Modular monolith describes deployment/feature organization; hexagonal describes internal dependencies. Both apply to the current system. The approved aggregate JPA mappings remain deliberate exceptions under ADR-0004/0007.

## Current Architecture and Quality Status — 2026-10-21

The human confirmed that CH-003's pragmatic hexagonal migration is complete under Accepted ADR-0007 Option B, with ADR-0004 and the approved JPA mappings retained. The [system architecture](../02-architecture/system-architecture.md#implemented-ports-and-adapters) describes the final class paths; the [architecture test guide](hexagonal-architecture-tests.md) describes current enforcement.

**Cloud provenance:** In the documentation request on 2026-10-21, the human reported that the latest human-run SonarQube Cloud analysis passed its Quality Gate with no remaining findings. This is a human-reported result, not a Cloud query or scanner execution by this documentation task. No analysis ID, branch/revision attribution or dashboard evidence was supplied with that report; none is invented here. Earlier Pending/Failed/Unknown Cloud entries below and in CH-003 records remain historical snapshots, superseded for this reported latest status only.

**Local verification:** The [CH-003 final assessment](../04-agentic-coding/change-requests/CH-003-hexagonal-architecture-migration/increment-8-acceptance-report.md) retains complete regression/JaCoCo/full PIT results and the subsequent targeted test-hardening evidence. The targeted R1/R2 mutation gaps were killed in the recorded narrow follow-up; that result does not replace the historical full-run population. Historical surviving/uncovered mutants and timeouts retain their individual provenance and review debt. A passing Cloud gate does not establish their resolution or risk acceptance.

`make verify-fast` runs all fast tests/ArchUnit and Spotless. Complete non-PIT `clean verify` adds integration tests and JaCoCo reporting. `make verify-full SCOPE=all` additionally invokes the existing PIT workflow; process success is not a mutation-score threshold or acceptance of survivors. See [engineering verification](engineering-verification.md) for exact scope. Native lifecycle delivery remains unverified and the prepared Git hook remains inactive under the [recorded hook inventory](../04-agentic-coding/multi-agent/hooks.md); no activation is inferred from passing tests.

This documentation update requires human acceptance. It records migration completion without changing ADR statuses, granting production readiness or closing historical mutation/deployment decisions.

## Latest Recorded Local Regression — 2026-10-21

The later SonarQube issue-cleanup task ran Java 21 `./mvnw -B -ntp clean verify`, finishing **2026-10-21T20:13:30+02:00** in 1:47 with exit 0. Its source was HEAD `46278837932b9b20940c70597dd696a47206a111` plus the ten test-only cleanup changes; it followed the CH-003 targeted hardening, rather than reusing the earlier 777-test migration run. No production/build inputs changed during cleanup verification.

| Check | Observed result in this full non-PIT run |
| --- | --- |
| Surefire | 779 executions, zero failures/errors/skips |
| Failsafe | 172 executions across 25 suites, zero failures/errors/skips; 160 PostgreSQL-backed and 12 HTTP lifecycle cases |
| Architecture | 88 executions, included in the 779 fast tests |
| OpenAPI fast tests | 13 executions, included in the 779; runtime compatibility also runs through Failsafe |
| JaCoCo lines | 1889/1936 covered = 97.57% |
| JaCoCo branches | 942/1021 covered = 92.26% |
| Formatting | Spotless lifecycle check passed |
| PIT / Cloud analysis | Not executed by this non-PIT verification |

A separate preceding `make verify-fast` passed 779 tests, and clean focused regression passed 62. Those are repeated/subset executions, not extra unique tests added to the full-run totals. The first focused sandbox/non-clean attempt failed on Mockito attachment and stale compiled test classes; clean verification with the required local permissions passed without additional source changes.

Evidence is retained temporarily under `/var/folders/53/qkl7wg6953lc164f335n3skr0000gn/T/sonar-local-7t3y4xk6/`: `full.log`, Surefire/Failsafe XML, `jacoco.xml`, `verification-summary.json`, reviewed cleanup diff and issue analysis. The separate fast archive is `/private/var/folders/53/qkl7wg6953lc164f335n3skr0000gn/T/agentic-verify-fast-vkoawc9_/`. These external artifacts are not durable repository storage; the counts and provenance above are the retained summary.

For the portfolio documentation audit on **2026-10-22**, the archived fast `source-sha256.json` was compared read-only with current HEAD `293360db483b28733c61c05a9f18c1a2fa75d4b8`: **232 build inputs matched, zero mismatches**. Full-run test XML and JaCoCo counters were re-read, without running Maven. This supports reuse of the recorded results for unchanged build inputs, not a new test execution or Cloud result. Earlier full PIT and narrow targeted PIT runs retain their own populations and outcomes in the CH-003 assessment.

## Public Badge Availability — 2026-10-22

Unauthenticated HTTP checks used only public repository/project identifiers and no credentials. GitHub's public repository API confirmed `uvormbrock/agentic-order-service` is public with default branch `main`. The README explicitly selects **develop push events**, matching the recent development evidence, using the [documented workflow badge format](https://docs.github.com/en/actions/how-tos/monitor-workflows/add-a-status-badge):

- `https://github.com/uvormbrock/agentic-order-service/actions/workflows/build.yml/badge.svg?branch=develop&event=push` returned HTTP 200, SVG, **SonarQube - passing**. This is the workflow's configured name and execution result; its `sonar.qualitygate.wait=false` means it is not an independent Quality Gate badge.
- `https://sonarcloud.io/api/project_badges/measure?project=uvormbrock_agentic-order-service&metric=alert_status` and `https://sonarcloud.io/api/project_badges/ai_code_assurance?project=uvormbrock_agentic-order-service` both returned HTTP 200 SVGs labelled **Project not found**, not valid status badges. Unauthenticated `components/show` returned 404. The project key is confirmed by the existing POM/workflow; public visibility and project-specific AI Code Assurance eligibility could not be established.

The two SonarQube badges are omitted. This access result neither contradicts the human-reported successful analysis nor proves project deletion or a particular visibility setting. Private-project badge URLs can contain access tokens; none was retrieved, created, published or embedded. No visibility, gate, AI-assurance, CI or remote repository setting was changed. Verbose token-free checks are retained in `/tmp/portfolio-readme/badge-checks.json`. No companion React/TypeScript repository URL was found in the existing repository documentation, so none is invented.

## Companion Frontend and Default-Branch Badge — 2026-10-22

After the earlier portfolio check, the developer supplied `https://github.com/uvormbrock/agentic-order-service-ui`. Public GitHub access succeeded. Read-only inspection covered its README, package.json, npm lockfile presence, .env.example, vite.config.ts, application navigation, API client and feature source. This supersedes the earlier absence of a supplied frontend URL; it does not change historical CH-003 approval records.

The frontend documents Node.js 24 and npm, with `dev` invoking Vite. Its server-only `BACKEND_URL` defaults to `http://127.0.0.1:18080`; the development proxy forwards anchored Product/Customer/Order and image paths. Browser API calls use relative paths, avoiding a browser cross-origin API request in the documented local setup. The README uses documented Vite host/port/strictPort flags to select `http://127.0.0.1:5173` explicitly rather than assuming an unconfigured port. No CORS setting or frontend file was changed. Production hosting requires separate same-origin API routing.

Source confirms Admin Product/Customer/Order views and a Customer shopping mode with catalog, cart, checkout, receipt and My Orders. Modes are not authentication; My Orders filters the complete Order response in the browser. At this initial inspection, setup and walkthrough were verified against documentation/configuration/source only; the subsequent live verification is recorded below. No public hosted demo is claimed.

The public backend default branch was rechecked as **main**. Its token-free `build.yml/badge.svg?branch=main&event=push` returned HTTP 200 SVG labelled **SonarQube - passing**. The final README now explicitly displays main, replacing the earlier deliberate develop selection; neither workflow success nor the change of badge branch establishes a fresh Cloud gate result. The unavailable SonarQube badges remain omitted. Public metadata reports no license, and no root LICENSE file was found; selecting a license remains the owner's decision.

## Local Walkthrough Attempt — 2026-10-22

**Current result:** The interactive UI walkthrough passed after the developer restarted backend and Docker; see the successful retry below. Earlier Pending/Unknown entries retain their original execution-time meaning.

The README polish retains the developer-provided `assets/screenshots/products_ui.png`, showing the companion Product catalog; the file was inspected and left unchanged. It is a supplied UI illustration; the earlier failures and later successful live walkthrough have separate evidence below.

The live-check attempt found an existing PostgreSQL demo container reported healthy and an existing Java listener on `127.0.0.1:18080`. A local request to `GET /products` returned **HTTP 503 SERVICE_UNAVAILABLE**. The frontend listener used IPv6 loopback, and opening the documented `http://127.0.0.1:5173` failed with connection refused. The developer interrupted the attempt and directed reuse of the existing screenshot. No successful seed, frontend checkout, receipt or saved-order verification is claimed. **End-to-end walkthrough verification remains Pending**; no “last verified” date was added to the README. No application, database, frontend or network configuration was changed to bypass these conditions.

### Retry after frontend restart — 2026-10-22

The developer restarted Vite and supplied `http://127.0.0.1:5173/`. A fresh interactive browser visit succeeded and the Product catalog displayed eight demo products. Customer management was opened and a single synthetic customer creation was submitted with the name `README Walkthrough 2026-10-22` and an example.test address. Both customer listing and the write timed out; the UI explicitly reported an uncertain write outcome, retained the input and warned that cancellation does not guarantee rollback. The write was not replayed.

After closing the dialog, one read-only customer refresh also timed out, so customer creation could not be confirmed or disproved. A separate direct read-only request to backend `GET /customers` produced no HTTP response within eight seconds (curl exit 28); this demonstrates that the problem is not limited to the browser's development proxy. It does not identify the backend root cause or prove rollback. Screenshot evidence of the write timeout is retained temporarily at `/tmp/readme-final/customer-timeout-2026-10-22.png`.

**Result:** Frontend access and Product catalog verified; Customer creation outcome Unknown; checkout, receipt and saved-order verification Pending because the customer flow could not complete. No seed was rerun against the populated database. No completed-walkthrough date was added to the README, and no application, frontend or database configuration was changed. The supplied Product screenshot remains unchanged.

### Successful retry after backend and Docker restart — 2026-10-22

Earlier checks saw an IPv4 frontend connection failure, then customer reads and a write timing out even after Vite restarted; a direct backend read also timed out. After the developer restarted backend and Docker, the same live UI flow completed without those failures. This demonstrates recovery after the restart, not a diagnosed cause: no evidence establishes whether database connectivity, connection-pool state or another runtime condition caused the original timeouts. No code or configuration correction was needed for the successful retry.

After the developer restarted backend and Docker, a fresh browser session at `http://127.0.0.1:5173/` completed the README UI scenarios against the live backend. Existing seeded data was reused; the seed was not rerun. The initial customer list contained five seeded customers and no prior walkthrough customer, so no uncertain earlier write was blindly replayed.

| Interactive scenario | Observed result |
| --- | --- |
| Product catalog and thumbnails | Eight seeded products displayed; existing thumbnails loaded. |
| Product creation | Created `README Walkthrough Product 2026-10-22` at EUR 12.34; success confirmation and the ninth active catalog entry were visible. |
| Customer creation | Created `README Walkthrough 2026-10-22` with an example.test email; success confirmation and the sixth active customer entry were visible. |
| Customer selection and cart | Selected that customer; added two Demo Mouse units at EUR 25.00 and one Demo Desk at EUR 100.00; estimate EUR 150.00. |
| Checkout and receipt | Confirmed once; UI displayed Order confirmed and authoritative subtotal EUR 150.00, discount EUR 15.00, total EUR 135.00, currency EUR. The cart was cleared. |
| My Orders | The new order appeared with matching stored item quantities, unit prices and totals. |
| Independently loaded Admin detail | Opened Admin → Orders → View order; detail showed the same customer, two historical item positions and all matching monetary values. The list contained five orders. |

Confirmed order ID: `659fe09e-3c9c-4332-8094-8b5dbd2223b2`; customer ID: `355bc2d0-c401-4a2a-b0ca-87139eeca2bf`. This is a manual interactive smoke test, not a fresh Maven/PIT/Cloud run or a new clean-install/seed verification. Image upload, deletion and every possible failure path were outside this walkthrough. The prior failures are not erased; their underlying cause was not independently diagnosed.

The README verification date applies to the application/frontend/seed state used for this walkthrough; changes to those inputs require re-verifying the affected flow before refreshing that date. Subsequent README wording/link edits do not represent a new live run.

The created product, customer and order remain in the disposable demo for inspection; no data was deleted. Temporary screenshots: `/tmp/readme-final/successful-order-2026-10-22.png` (My Orders with complete amounts) and `/tmp/readme-final/saved-order-details-2026-10-22.png` (independent detail dialog). The supplied repository Product screenshot remains unchanged. README now contains the dated successful UI-walkthrough note. No production/test/build/frontend configuration, Git publication or remote analysis was changed or executed.

## Verification Metadata

The regression and coverage evidence below comes from two distinct Java 21 / Maven Wrapper 3.9.9 executions on `develop`.

| Snapshot | Execution and source provenance | Preserved evidence |
| --- | --- | --- |
| SonarQube remediation verification | Finished **2026-10-17T11:20:41+02:00**. Base HEAD: `8b350c54b5532589cea60758c336f1a7cdafb260`, with uncommitted remediation changes to 13 Java files and the playbook. | Evidence set `increment6-sonar-remediation-awjy37wh`: final Maven log, archived Surefire/Failsafe XML, JaCoCo XML, coverage summary, complete diff and final Git status. |
| Subsequent fixture-restoration verification | Finished **2026-10-17T11:43:35+02:00**. The restoration summary records unchanged branch/HEAD and only `testing-strategy.md` dirty in final Git status. The repository context is commit `5de3c22a7779ef8cf384a1b28815a148ace4cad7`, which commits the remediation. | Evidence set `async-callback-revert-_18yok1o`: lifecycle and full-verification logs, archived test XML, restoration summary, reversed correction diff and final Git status. |

The [remediation execution record](../04-agentic-coding/prompts/07-application-logging-observability.md#separately-authorized-sonarqube-remediation--2026-10-17) is the version-controlled evidence reference. The named evidence sets currently reside in temporary storage; they are not durable repository artifacts. The later restoration evidence is a separate execution and does not replace the original remediation record.

The later restoration set does not independently capture a full Git SHA in its summary. Its unchanged-HEAD statement, final status and fixture fingerprint support the repository-context attribution above, rather than providing a complete source manifest. Its recorded restored fixture SHA-256, `2f0f1e0625dbde58aa278a0926d75f57963052dfa1c473fbf13e6e6b7b34f6f2`, matches the fixture bytes at commit `5de3c22a7779ef8cf384a1b28815a148ace4cad7`.

The full-verification command recorded for these tasks was:

```sh
./mvnw -B -ntp clean verify
```

Java 21 was selected for execution. The Maven logs establish successful completion but do not independently preserve the launching shell command's complete environment prefix. No complete source/environment fingerprint was recorded. In particular, the remediation result cannot be reproduced from its base HEAD alone: the preserved uncommitted diff is part of that snapshot. The later commit is useful source context, not a replacement for the original dirty-state provenance.

## Automated Test Execution & Regression Verification

Read-only parsing of the archived Surefire and Failsafe XML independently confirmed the same totals in both executions:

| Test group | Successful executions | Failures | Errors | Skips |
| --- | --- | --- | --- | --- |
| Surefire fast tests | 528 | 0 | 0 | 0 |
| Failsafe integration tests | 148 | 0 | 0 | 0 |
| **Total** | **676** | **0** | **0** | **0** |

The Failsafe total comprises **136 PostgreSQL executions and 12 Servlet lifecycle executions**. These are execution counts, not counts of distinct test methods or independently certified requirements. Both full Maven logs report `BUILD SUCCESS`. The separate 12-test lifecycle run also passed after restoration; it overlaps the full-suite total and must not be added to it.

Surefire covers fast domain, application, configuration and MVC tests. Failsafe adds real PostgreSQL/Testcontainers and embedded-Servlet lifecycle verification. See the [testing strategy](testing-strategy.md) for responsibilities and execution boundaries.

## Coverage

The **remediation snapshot**, not a new documentation-task run, supplies the archived JaCoCo XML counters:

| Counter | Covered | Total | Coverage |
| --- | --- | --- | --- |
| Lines | 1,188 | 1,226 | **96.90%** |
| Branches | 658 | 709 | **92.81%** |

These values were checked directly against the archived XML. They supersede the older stabilization snapshot's coverage for reporting the remediation, without rewriting that older record. The later restoration evidence set preserves test reports but no separate archived JaCoCo report; its passing tests must not be described as an independently archived coverage measurement.

Coverage establishes execution, not the strength of assertions, completeness of fault-path verification or production readiness.

## Mutation Testing

The [historical PIT interpretation](../04-agentic-coding/prompts/07-application-logging-observability.md#historical-pit-and-current-sonarqube-status) records a separate project-wide run identified as `PIT-all-20261016T191406Z-YZl37K`.

| Outcome | Recorded count |
| --- | --- |
| Generated | 697 |
| KILLED | 566 |
| TIMED_OUT | 6 |
| SURVIVED | 93 |
| NO_COVERAGE | 32 |

The recorded PIT console reported **82% mutation detection** and **86% test strength**. Its 572 detections include six timeouts: 572/697 = **82.07%**. Strict KILLED/generated is 566/697 = **81.21%**. TIMED_OUT remains distinct from KILLED; no numeric mutation acceptance threshold is implied.

This run predates final fixture stabilization and the later remediation. It does **not** verify either later source snapshot. The run identifier contains a UTC timestamp for 2026-10-16; a separate execution completion time, exact Git SHA and dirty-state fingerprint are not established by the linked summary. The originally recorded external run directory was unavailable at its documented location during this review, so its XML was not re-parsed here; the counts above are attributed to the preserved execution record's earlier XML inspection.

The current automated workflow excludes PostgreSQL `*IT` tests. Meaningful survivors, missing coverage, framework limitations and equivalent-mutant candidates need review against requirements; a successful PIT execution can still contain survivors. See the [mutation strategy](mutation-testing.md), [automated runbook](mutation-testing-runbook.md#current-automated-workflow) and [verified experiments](mutation-results/mutation-testing-experiments.md).

## Static Analysis

The remediation record distinguishes the following evidence:

- **Fetched findings:** 31 OPEN/CONFIRMED issues on `develop` at the recorded base revision.
- **Local remediation:** all 31 addressed locally, with regression verification. No suppressions, weakened assertions, issue-status changes or quality-threshold changes were used.
- **Local snippet checks:** the 31 fetched findings were absent from checks of the changed Java snippets. Pathless snippet analysis still produced package/path artifacts and an unchanged baseline constructor warning; this was not a full branch analysis.
- **Published analysis:** none published by that remediation task. A subsequent complete branch reanalysis remains **Pending in the recorded evidence**.
- **Quality gate:** no current passed-gate or zero-outstanding-findings claim is supported by that record.

The [GitHub Actions workflow](../../.github/workflows/build.yml) invokes the scanner separately after full verification. It currently uses `sonar.qualitygate.wait=false`, so successful scanner execution does not by itself establish a passed remote quality gate. No remote analysis was queried during this documentation task.

## Known Limitations

- Increment 6 remains **Functionally Stabilized, Frozen — Full Acceptance Pending**. The [remaining acceptance matrix](../04-agentic-coding/prompts/07-application-logging-observability.md#current-acceptance-evidence-and-remaining-gaps) includes lifecycle fault paths, MDC failure injection, diagnostic ownership and post-commit response-failure evidence.
- The restored test fixture retains an existing generic `onError()` redispatch limitation. Both the later lifecycle log and full-verification log contain one Tomcat `onError() call failed for listener` warning. A green suite does not prove every callback completed cleanly; this is not evidence of a production defect. No further correction was attempted in this documentation task.
- Mockito attachment warnings remain technical follow-up work; successful tests do not resolve future runtime restrictions.
- Persistence mutation testing, persistent-local backup/recovery and migration authorization, deployment/security policy and logging operations retain separate gates. Complete ADR-0002/0006 remain Proposed.
- Temporary evidence may disappear. The missing original PIT directory and incomplete source fingerprints limit independent reproduction; this report does not invent missing provenance or replace raw artifacts.

The [human freeze decision](../04-agentic-coding/prompts/07-application-logging-observability.md#increment-6--human-freeze-decision--2026-10-16) and [open-items register](../00-overview/open-items.md) remain authoritative for acceptance and operational obligations.

## Interpretation

These results are historical snapshots tied to their recorded source state, selected tests, configuration and environment. Later code changes require fresh applicable verification. Do not combine test, coverage, mutation and static-analysis snapshots into a single implied execution.

A successful automated suite is not production-readiness certification, a clean static analysis is not mutation-testing evidence, and agent verification does not replace human acceptance. No acceptance status, architecture decision, existing execution record or quality threshold was changed by this report.


## Dependency Security Remediation — 2026-10-17

**Status: executed locally, pending human review; one JAXB risk remains unresolved.** This is a separate execution record, not a revision of the historical evidence above.

### Source and Branch Safety

The task began on clean `main` at `1f23b6f1e5e1392efcb7d1f5873e1deb4f176b73` after PR #1. Its file tree matched `develop` exactly. As explicitly authorized, execution switched to `develop` at `779bb0e0036922253cf2aa7b0ec3e647cca183f3`. Remediation consists of the uncommitted POM change plus the directly related documentation updates; results must not be attributed to that base commit alone. The verified POM SHA-256 is `43c762e6b280a1a2159665b5383197fba0ba94074ef36ec85d37c9a15ab19f62`. No source/test, CI, migration, application configuration or earlier override was changed. Main's commit remains unchanged; there was no commit, merge or push.

### Eight Reported Dependency Risks

A fresh read-only SonarQube MCP query of `main` returned these eight OPEN findings. They are dependency/SCA risks, distinct from ordinary source-code issues and security hotspots. Sonar's contextual severity is recorded separately from advisory CVSS scoring.

| Advisory | Artifact | Sonar severity | Before | Local candidate | Verified upstream fixed release |
| --- | --- | --- | --- | --- | --- |
| CVE-2026-19032 | com.fasterxml.jackson.core:jackson-databind | MEDIUM | 2.21.5 | 2.21.7 | 2.21.6 |
| CVE-2026-68497 | com.fasterxml.jackson.core:jackson-databind | MEDIUM | 2.21.5 | 2.21.7 | 2.21.6 |
| CVE-2026-83557 | com.fasterxml.jackson.core:jackson-databind | MEDIUM | 2.21.5 | 2.21.7 | 2.21.6 |
| CVE-2026-89407 | com.fasterxml.jackson.core:jackson-core | MEDIUM | 2.21.5 | 2.21.7 | 2.21.7 |
| CVE-2026-89425 | com.fasterxml.jackson.core:jackson-core | LOW | 2.21.5 | 2.21.7 | 2.21.7 |
| CVE-2026-91776 | com.fasterxml.jackson.core:jackson-databind | LOW | 2.21.5 | 2.21.7 | 2.21.7 |
| CVE-2026-91777 | com.fasterxml.jackson.core:jackson-databind | LOW | 2.21.5 | 2.21.7 | 2.21.7 |
| CVE-2026-92877 | org.glassfish.jaxb:jaxb-core | LOW | 4.0.9 | 4.0.9, unchanged | No published compatible fix verified |

The [Jackson 2.21.6](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.21.6) and [2.21.7](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.21.7) release notes independently identify the Jackson fixes. The application already overrides Jackson **3** through `jackson-bom.version=3.1.7`; that property does not update Jackson **2**. Swagger brings Jackson 2 transitively through springdoc 3.1.1 → swagger-core-jakarta 2.2.55. The minimal change is Boot's existing `jackson-2-bom.version=2.21.7` property, keeping the supported patch line and BOM alignment.

The JSON dependency-tree comparison before/after confirms exactly four selected changes: Jackson 2 Core, Databind, YAML and JSR-310 **2.21.5 → 2.21.7**. Shared annotations stay **2.21**; Jackson 3 Core/Databind remain **3.1.7**. No selected artifacts were added/removed, no artifact has multiple selected versions, and all other selected versions are identical. Verbose dependency output retains normal Maven omitted/managed entries; those are not duplicate packaged libraries.

Hibernate 7.4.5.Final brings JAXB Runtime → Core → TXW2, all at 4.0.9. The [JAXB hardening PR](https://github.com/eclipse-ee4j/jaxb-ri/pull/2013) is merged, but [release tracking](https://github.com/eclipse-ee4j/jaxb-ri/issues/2031) remains open. Independently retrieved Maven Central Core/Runtime metadata still identifies **4.0.9** as the latest stable release, with no 4.0.10 published. The JAXB finding is therefore still open. There is no exclusion, snapshot, replacement implementation, blanket parser-property workaround or risk acceptance. A source search found no direct application JAXB unmarshalling call; that does not establish non-exploitability or waive the transitive dependency risk.

### Local Verification

- Java 21 / Maven Wrapper 3.9.9; Spring Boot remains 4.1.1.
- Focused OpenAPI/default-disabled checks: **11 passed**, zero failures/errors/skips. Swagger UI/config, JSON/YAML equivalence, local-reference resolution and equality with the versioned YAML remain covered. No snapshot rewrite was required.
- The single full `./mvnw -B -ntp clean verify` run completed at **2026-10-17T20:37:56+02:00**, exit 0: **539 Surefire + 148 Failsafe = 687 executions**, zero failures/errors/skips. Spotless passed and a fresh JaCoCo XML report was generated for the subsequent scan. These checks include PostgreSQL/Flyway and the existing concurrency/Servlet integration tests on disposable test infrastructure.
- No correction cycle was required. PIT was not run. Persistent databases were not migrated.

### Fresh SonarQube Verification

A fresh Java 21 Maven scanner execution ran on `develop` with `sonar.branch.name=develop` and `sonar.qualitygate.wait=true`, using the configured credential through the environment without printing it. The command was `./mvnw -B -ntp org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar -Dsonar.host.url=https://sonarcloud.io -Dsonar.projectKey=uvormbrock_agentic-order-service -Dsonar.branch.name=develop -Dsonar.qualitygate.wait=true`. It imported the newly generated JaCoCo XML and uploaded fresh SCA dependency files from the verified Maven graph; no stale report was substituted.

Scanner exit was **0**, finishing at **2026-10-17T20:38:55+02:00**. SonarQube records analysis time **2026-10-17T18:38:24Z** and confirms **Quality Gate: PASSED / OK** on `develop`. A subsequent read-only dependency-risk query independently confirms all seven targeted Jackson 2 findings are now **FIXED**. CVE-2026-92877 for `jaxb-core:4.0.9` remains **OPEN**. These statuses were updated by the fresh analysis, not by manual issue transitions.

Sonar identifies `develop` as a SHORT branch targeting `main`; the remaining JAXB risk is reported as `newlyIntroduced=false`. A passed new-code gate is therefore not a claim of zero remaining dependency vulnerabilities. `main` was neither changed nor reanalyzed: it retains its failed 2026-10-17T18:17:59Z analysis until separately updated and analyzed. No passing-main claim is made.

The scanner's SCM revision is the unchanged base `779bb0e0036922253cf2aa7b0ec3e647cca183f3`; the scanned POM includes the verified uncommitted override identified by the SHA-256 above. Documentation evidence was finalized after scanning; Java sources, resolved dependency versions and the verified POM remained unchanged. The full suite supplied JaCoCo counts of **1,329/1,367 covered lines** and **725/782 covered branches** for that code snapshot. Scan success is not mutation-testing evidence, acceptance of the JAXB risk or a production security guarantee.

The external temporary evidence set `dependency-security-qotq8xq_` preserves original POM/source references, full before/after JSON dependency graphs and verbose output, JAXB Central metadata, focused/full Maven logs and reports, JaCoCo XML, scanner command/result and the final diff. Temporary evidence is not durable archival storage. The [technology inventory](../02-architecture/technology-stack.md#authorized-jackson-2-security-remediation--2026-10-17) records the selected override and unresolved JAXB update. No quality threshold, ADR status or human acceptance was changed; no manual issue-status transition was performed.

## CH-001 Phase 2 — 2026-10-18

**Initial execution (historical):** Current uncommitted Phase 2 evidence is recorded in the [review record](../04-agentic-coding/change-requests/CH-001-customer-delete-semantics/03-review.md). Focused Java checks passed 28 executions; focused disposable PostgreSQL checks passed 41, including both first-order schedules and runtime-generated OpenAPI. Full Java 21 `clean verify` failed: 537 fast executions, one failure in the obsolete 25-code catalog-size assertion after adding CUSTOMER_HAS_ORDERS (26 codes); complete-run Failsafe/later gates remain Pending. Execution stopped after the single allowed formatting correction; the failing assertion is unresolved. Fresh SonarQube analysis/findings review is Pending because the documented scanner credential is unavailable; no historical remote gate certifies this diff. Human acceptance remains Pending.


**Successful focused correction and full rerun:** The separately authorized [focused regression correction](../04-agentic-coding/change-requests/CH-001-customer-delete-semantics/03-review.md#focused-regression-correction--2026-10-18) resolved F-001 by changing the catalog-size test from 25 to 26 while preserving exact-set assertions. The focused test class passed 27 executions. The subsequent Java 21 `clean verify` exited 0 at **2026-10-18T12:44:47+02:00**: **537 fast + 144 integration = 681 executions**, zero failures, errors or skips; Spotless and JaCoCo report generation completed. Evidence belongs to the uncommitted CH-001 Phase 2 working tree plus that test correction on `develop`, base HEAD `5d2f189febee177146bc11da2b4372e8cb4ef20a`. Logs and archived reports are retained in `/tmp/ch001-regression-correction/`; the original failed run remains historical evidence in `/tmp/ch001-phase2/` and the review record. This synchronization did not rerun regression. Fresh SonarQube analysis/findings review and final human acceptance remain **Pending**; CH-001 is not accepted or closed.

## CH-001 OpenAPI SonarQube Follow-up — 2026-10-18

The human explicitly authorized remediation of the single open `develop` issue `AaFOq1gyBynKBKHRmaDM` (`java:S3776`) in `OpenApiConfiguration.errors`: cognitive complexity 17 exceeds the allowed 15. Starting from clean `develop` at `abb3a2b`, the request-body error selection was extracted into `addBodyErrors`, preserving all codes and insertion order. No API metadata, schema, generated YAML, dependency or database change was introduced.

The supported SonarQube snippet analyzer with the project's Java profile reproduced S3776 on the committed file and did not report it on the refactored file. Both analyses reported S1598 because the snippet tool supplies an empty filename; the actual source resides in its declared package path. This scoped analysis does not update the remote branch issue or quality gate; confirmation through fresh branch analysis remains **Pending**.

Java 21 `./mvnw -B -ntp test -Dtest=OpenApiDocumentationTest,OpenApiDisabledTest` passed **12 executions**, zero failures/errors/skips, including lifecycle formatting checks, unchanged snapshot/runtime JSON/YAML equivalence, local-reference resolution, strict error contracts and demo-only exposure. The first sandbox run failed because Mockito could not attach to the JVM; the supported rerun outside the sandbox passed. Full regression was not repeated for this focused method extraction; the earlier 681-execution result remains historical evidence. Logs, analyzer summary and the reviewed diff are retained under `/tmp/ch001-sonar-refactor/` (temporary storage). `git diff --check` passed. Human acceptance remains Pending; nothing was staged, committed, pushed or deployed.
