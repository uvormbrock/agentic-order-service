# CH-001 — Review Record

**Overall status:** Implemented; focused regression correction and full Java 21 regression passed on 2026-10-18. F-001 is resolved. Fresh SonarQube verification and human acceptance remain Pending; CH-001 is not accepted or closed.

The initial Phase 2 evidence and stop condition below are preserved as historical records. The separately authorized [focused regression correction](#focused-regression-correction--2026-10-18) records the subsequent result and supersedes the earlier unresolved F-001/full-regression status.

## Authorization and implementation

The human explicitly authorized the approved Phase 2 scope on 2026-10-18 in the implementation chat, with a maximum of one focused correction attempt and no Git publication actions. Work remained on `develop`, base HEAD `5d2f189febee177146bc11da2b4372e8cb4ef20a`, with pre-existing documentation changes. No dependencies, migrations or architecture changes were introduced; ADR statuses and historical acceptance records are preserved.

Customer DELETE retains its Customer-owned READ COMMITTED transaction, pessimistic Customer lock and subsequent separate Order-owned reference query. A positive reference result raises `CustomerHasOrdersException` before any mutation or deletion callback registration. Only that failure maps to the exact CUSTOMER_HAS_ORDERS/409 envelope; unrelated integrity failures retain existing classification. Physical deletion logs CUSTOMER_DELETED only after confirmed commit. PATCH remains explicit irreversible deactivation.

## Acceptance checklist coverage

The [authoritative checklist](../../../02-architecture/rest-api-design.md#phase-2-implementation-acceptance-checklist) governs scope. Checked items record scoped evidence, not human acceptance.

| Checklist item | Evidence / status |
| --- | --- |
| 1. Explicit authorization and DELETE behavior | Implemented as authorized above; unreferenced active/inactive physically deleted, referenced active/inactive rejected |
| 2. Specific failure, exact envelope and retained error handling | CustomerHasOrdersException, ApiError.Code, ApiExceptionHandler; CustomerApiTest and independent ExpectedApiErrors oracle |
| 3. Lock/query ownership and atomic rejection | Existing CustomerRepository pessimistic read followed by mandatory OrderReferenceQuery retained; READ COMMITTED post-wait visibility proved by concurrency tests |
| 4. Five PRD cases, unchanged state, PATCH | CustomerDeletionTest, CustomerApiTest, CustomerApiIT and OrderApiIT passed; referenced rejection compares complete persisted customer rows including version and historical order responses before/after; existing PATCH/no-reactivation checks retained |
| 5. Both first-order schedules and rollback guarantees | All 27 OrderPlacementConcurrencyIT executions passed; delete-first yields existing customer-not-found, placement-first raises mapped conflict and retains active/version/order count; physical-delete rollback and commit-only logging checks retained |
| 6. Demo and dataset | Drew DELETE expects exact 409 envelope followed by PATCH; isolated Python seed simulation passed; no existing environment reset; full live demo seed not executed |
| 7. Java OpenAPI metadata | CustomerController DELETE description/204 updated; shared 409 includes CUSTOMER_HAS_ORDERS alongside CONCURRENT_MODIFICATION |
| 8. Runtime DELETE/schema/contracts | Exact examples, 204/no body, 404, strict ApiError, unchanged PATCH and six paths/14 operations checked by OpenApiDocumentationTest; all 5 methods passed in the full fast run |
| 9. Generated specification and UI | OpenApiRuntimeIT generated snapshot over HTTP from running disposable PostgreSQL application; JSON/YAML/snapshot equivalence and Swagger UI/config passed; focused reference/default-disabled checks passed |
| 10. Full regression and SonarQube | **Regression Passed:** latest rerun passed 537 fast + 144 integration executions with zero failures/errors/skips; see the focused correction below. **SonarQube Pending:** the combined item remains incomplete |

## Initial Phase 2 Verification Evidence (Historical)

Verbose evidence is retained under `/tmp/ch001-phase2/` (temporary local storage, not durable archival evidence). No historical run is substituted for current verification.

| Gate | Actual result | Evidence |
| --- | --- | --- |
| Initial focused run | Environment blocked: Mockito could not attach inside sandbox | `focused.log` |
| Supported focused rerun | Failed formatting check after logging-fixture simplification | `focused-supported.log` |
| Single focused correction | Applied existing Spotless formatter to the affected fixture; no further code correction after this | `format-correction.log` |
| Corrected focused Java 21 unit/MVC/OpenAPI | **Passed: 28 executions, zero failures/errors/skips** | `focused-corrected.log` |
| Focused disposable PostgreSQL | **Passed: 41 executions, zero failures/errors/skips** (CustomerApiIT 4, OrderApiIT 2, concurrency 27, async logging 7, runtime OpenAPI 1) | `focused-postgres.log` |
| Isolated demo helper sequencing | Passed mocked seed, exact 409 and subsequent PATCH, expected fixture counts; not a live seed run | `demo-check.log`, `check-demo.py` |
| Complete Java 21 `clean verify` | **Failed: 537 fast executions, 1 failure, 0 errors/skips**; 536 passed. Failsafe and later lifecycle gates not reached, hence Pending for the complete run | `full-verify.log`, `final-surefire-reports/` |
| Fresh SonarQube analysis / quality gate / findings review | **Pending:** documented Maven/CI workflow requires SONAR_TOKEN, unavailable in this process. No credential introduced, no push or CI dispatch; historical remote results do not cover this diff | Existing workflow and quality-report provenance inspected; environment presence checked without disclosing values |
| Diff hygiene | Passed `git diff --check`; staged diff empty | `final-status.txt`, `full.diff`, `baseline.diff` |

Commands used command-local Java 21 (`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`) and `./mvnw -B -ntp`:

- Focused: `test '-Dtest=CustomerDeletionTest,CustomerApiTest,OpenApiDisabledTest,OpenApiDocumentationTest#documentsOnlyImplementedFeatureOperationsAndActualSuccessCodes+customerDeleteDocumentsExactConflictAlongsideConcurrencyAndNotFound+schemasPreserveStrictJsonAndDocumentBusinessConstraintsWithoutAddingValidation+allLocalComponentReferencesResolveAndSharedErrorMatchesTheResponseContract'`.
- PostgreSQL/runtime generation: `failsafe:integration-test failsafe:verify '-Dit.test=OpenApiRuntimeIT,CustomerApiIT,OrderApiIT,OrderPlacementConcurrencyIT,AsyncTransactionLoggingIT' -Dch001.generateOpenApi=true` after test compilation. Snapshot was generated from HTTP, not hand-edited.
- Full regression: `clean verify` (failed as recorded above).

## Initial Phase 2 Findings and Stop Condition (Historical)

**F-001 — unresolved, blocks green regression:** `ApiErrorContractTest.catalogContainsExactlyTheTwentyFiveApprovedCodes` still asserts catalog size 25. The approved CUSTOMER_HAS_ORDERS addition makes 26. Its failure is at line 18: expected 25, actual 26. The other 26 parameterized exact status/envelope checks in that class passed. The test count assertion/name needs adaptation and regression must be rerun before acceptance. No second correction was made because the human's single correction allowance was already consumed by formatting.

No additional blocking architectural decision was identified for retaining the existing scoped protocol. Complete ADR-0002/0006 and operational decisions remain in their existing states. Fresh SonarQube findings are unknown. Browser visual inspection and a complete live demo seed remain Pending; HTTP UI/config and isolated seed checks above passed.

## Diff self-review

Reviewed the production/test diff, generated YAML, current documentation changes, new exception/runtime test and all untracked change-request records. The actual Git diff also contains the human's pre-existing changes; it must not be attributed entirely to Phase 2. Baseline diff/status are saved separately. Pre-existing AGENTS.md and PRD diffs remain byte-for-byte unchanged; historical prompt and acceptance records were not rewritten.

Task changes cover five existing production files plus the new exception; eight existing test files plus the new runtime integration test; demo/demo.py; generated openapi.yaml; current README/dataset, overview, architecture, scoped ADR precedence notes, testing/quality, operational and implementation tracking documentation. See `task-files.txt` for exact task-touched paths and `final-status.txt` for the full working tree. Existing change-request files remain untracked; the original Phase 1 provenance record is preserved. There are no staged changes. No commit, push, merge, deployment or pull request was created.

## Human review and acceptance

| Record                                              | Status                                               |
| --------------------------------------------------- | ---------------------------------------------------- |
| Implementation reviewer, date and reviewed revision | Passed                                               |
| F-001 resolution and complete regression            | Passed in the separately authorized correction below |
| Fresh SonarQube verification                        | Pending                                              |
| Findings resolved or explicitly accepted            | Pending                                              |
| Final human acceptance and closure                  | Pending                                              |

Agent self-review is not independent human review. Implementation and verification stopped at the authorized correction limit; closure requires explicit human acceptance.


## Focused Regression Correction — 2026-10-18

**Authorization:** The human explicitly authorized one narrowly scoped correction to the failing error-catalog test, preserving exact-set assertions and production behavior, followed by the previously failing test and complete Java 21 regression. This is separate from the initial Phase 2 correction allowance and does not authorize publication or close human acceptance.

**Catalog verification:** Compared both `ApiError.Code` and the independent `ExpectedApiErrors.CATALOG` with base HEAD `5d2f189febee177146bc11da2b4372e8cb4ef20a`. Each retains all 25 original codes and adds exactly `CUSTOMER_HAS_ORDERS`, with no removed codes. Their diffs contain only that approved addition, including the catalog's exact 409 message. No production correction was needed.

**Single correction:** In `src/test/java/com/example/agenticorderservice/shared/api/ApiErrorContractTest.java`, renamed `catalogContainsExactlyTheTwentyFiveApprovedCodes` to `catalogContainsExactlyTheTwentySixApprovedCodes` and changed only `hasSize(25)` to `hasSize(26)`. The existing `containsExactlyInAnyOrderElementsOf` assertion is unchanged, as are all parameterized exact status/message/envelope and response-writer checks. No coverage was removed or weakened.

| Verification | Actual result | Evidence |
| --- | --- | --- |
| Previously failing test class | **Passed: 27 executions, zero failures/errors/skips** | `/tmp/ch001-regression-correction/focused.log` |
| Complete Java 21 `clean verify` | **Passed: 537 Surefire + 144 Failsafe = 681 executions, zero failures/errors/skips**; Spotless lifecycle check and JaCoCo report generation completed | `/tmp/ch001-regression-correction/full-verify.log`, archived `surefire-reports/` and `failsafe-reports/` |
| Fresh SonarQube analysis/findings review | **Pending:** SONAR_TOKEN remains unavailable; no credentials introduced, CI dispatch or historical remote result substituted | `/tmp/ch001-regression-correction/sonar-availability.txt` (presence only) |
| Diff self-review | Passed `git diff --check`; only the test and this review record changed during this correction; existing tracked/untracked work preserved; staged diff empty | `/tmp/ch001-regression-correction/correction.diff`, `baseline.diff`, `final-status.txt` |

Commands from the repository root used command-local Java 21:

```sh
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp test -Dtest=ApiErrorContractTest
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp clean verify
```

The full run exited 0 at `2026-10-18T12:44:47+02:00`. Evidence belongs to the existing uncommitted Phase 2 working tree plus this test-only correction on `develop`, not to base HEAD alone. Logs/reports are temporary local evidence. The earlier failed run and original stop-condition text above remain historical evidence under `/tmp/ch001-phase2/`; they were not relabeled as passing. Other planning/quality records that mention that failure describe the earlier run; this follow-up is the current regression evidence.

**F-001 resolution:** Resolved and verified. No remaining regression failures were observed. Fresh SonarQube verification, any other previously Pending checks and final human review/acceptance remain Pending. No dependency, architecture, schema, migration or production behavior changed. Nothing was staged, committed, pushed, merged or deployed. Work stops for human review.


## Final Review Evidence Synchronization — 2026-10-18

Documentation-only synchronization on `develop`: the [quality report](../../../05-testing/quality-report.md#ch-001-phase-2--2026-10-18) now records both the historical failed run and the successful rerun. No regression was rerun, and no application logic, tests, dependencies or OpenAPI files were changed by this synchronization.

**Current status:** focused correction **Passed** (27 executions); complete Java 21 regression **Passed** (537 fast + 144 integration, zero failures/errors/skips); F-001 **Resolved**. Fresh SonarQube analysis/findings review, previously unrun live-demo/browser checks and final human acceptance remain **Pending**. A passing regression is not human acceptance; CH-001 remains open. Historical failures and original stop conditions above retain their original scope.

### Complete Untracked Review Inventory

Confirmed against `git ls-files --others --exclude-standard`; all seven untracked source/documentation files are included. Paths are relative to the repository root:

- `docs/04-agentic-coding/change-requests/CH-001-customer-delete-semantics/01-documentation.md`
- `docs/04-agentic-coding/change-requests/CH-001-customer-delete-semantics/02-implementation.md`
- `docs/04-agentic-coding/change-requests/CH-001-customer-delete-semantics/03-review.md`
- `docs/04-agentic-coding/change-requests/CH-001-customer-delete-semantics/README.md`
- `docs/04-agentic-coding/change-requests/README.md`
- `src/main/java/com/example/agenticorderservice/customer/application/CustomerHasOrdersException.java`
- `src/test/java/com/example/agenticorderservice/shared/api/OpenApiRuntimeIT.java`

The inventory includes the new application exception, runtime integration test, four CH-001 records and the change-request convention/register. These files remain untracked for human review; inclusion in this inventory does not stage them. Existing tracked changes and all other file contents were preserved. `git diff --check` passed, and no staged changes or Git publication actions were made. Full synchronization evidence and final status are retained in `/tmp/ch001-evidence-sync/` (temporary local storage). Stop for human review; no acceptance or closure is inferred.

## Demo Client Expected HTTP Error Correction — 2026-10-18

**Authorization and scope:** The human explicitly authorized correcting the demo HTTP client and recording actual evidence. Only `demo/demo.py` and this review record were modified on `develop`. Existing untracked `logs/` files were preserved. No backend, API contract, dependency, database configuration, OpenAPI or unrelated demo behavior changed.

**Cause:** `urllib.request.urlopen()` raises `urllib.error.HTTPError` for HTTP 409. The old helper unconditionally converted every HTTPError to a failure, so the explicit `expected=409` never reached status/header/body handling. The earlier mocked seed simulation replaced the request helper and therefore did not exercise this urllib behavior; its historical result remains scoped to seed sequencing.

**Correction:** Catch HTTPError only around `urlopen`. If its status differs from `expected`, close the response and retain the existing immediate RuntimeError without replay. Otherwise pass the HTTPError response through the shared response context, status check, required `X-Request-ID` validation and JSON parsing. Normal responses use the same path. Expected 204 still returns None without reading/parsing a body. Network errors and malformed JSON continue to propagate as failures. The exact CUSTOMER_HAS_ORDERS body validation and subsequent explicit PATCH deactivation in `seed()` are unchanged; all code outside `request()` was compared before/after and is identical.

| Verification | Actual result | Evidence |
| --- | --- | --- |
| Python syntax | Passed using `compile(source, filename, 'exec')` without writing bytecode | `demo/demo.py` |
| Mocked urllib response tests | **Passed: 12 tests** covering expected 409 JSON/closure, unexpected 409/no replay, unexpected 500, missing/empty request-ID on expected 409, malformed JSON on expected 409, 204/no read, missing request-ID on 204, unexpected normal status, normal JSON success, missing request-ID/malformed JSON on normal success, and network failure/no replay | `/tmp/ch001-demo-http/test_request.py`, `/tmp/ch001-demo-http/tests.log` |
| Diff self-review and hygiene | Passed `git diff --check`; only helper and this appended review entry changed; staged diff empty | `/tmp/ch001-demo-http/review.diff`, `/tmp/ch001-demo-http/final-status.txt` |
| Live demo seed and existing database | **Pending; deliberately not run** because the previous seed may have partially populated existing data. No reset or data changes performed | No live HTTP/database calls made |
| Maven / SonarQube | Not run, as explicitly instructed; no fresh verification claim | Earlier results retain their original scope |
| Human acceptance | **Pending** | Stop for human review |

No existing Python test mechanism was found in the repository. Verification used Python's standard-library `unittest` and mocks in a temporary external harness (`python3 -B /tmp/ch001-demo-http/test_request.py`), without adding dependencies or repository test files. All checks passed on the initial implementation; no correction retry was needed. Temporary evidence is not durable archival storage. No staging, commit, push, merge or deployment was performed.

## Customer DELETE HTTP 409 Logging Investigation — 2026-10-18

**Authorization and outcome:** The human authorized scoped investigation, reproduction and a minimal production correction only if a defect was confirmed. Investigation ran on `develop`. No production defect was reproduced, so no production code, logging configuration or observability contract was changed. Existing demo-client changes, earlier review entries and untracked `logs/` content were preserved.

### Verified cause and lifecycle trace

`RequestLoggingListener.completionLevel()` uses INFO for completed 2xx/3xx and DEBUG for completed 4xx. `application.yml` sets root/application levels to INFO; `application-demo.yml` does not override them. The human-approved L1/L3 policy explicitly selects default INFO with DEBUG for expected rejection (see the [observability decisions](../../prompts/07-application-logging-observability.md#final-human-architecture-decisions--2026-10-16)). Controlled INFO/DEBUG tests reproduced precisely this visibility difference: 200/204 events are visible at INFO; the 409 event becomes visible at DEBUG, retaining the correct status and correlation. A read-only aggregate check of existing `logs/demo.log` found INFO completion events for 200, 201, 204 and 304, consistent with this configuration. No raw request data or log contents were copied into this record.

The CustomerHasOrdersException mapping returns CUSTOMER_HAS_ORDERS through ApiExceptionHandler and the shared response writer, setting the actual response status to 409. The handled exception returns through the normal correlation filter path; status and route are observed in its finally block. Terminal emission belongs exclusively to the listener's requestDestroyed callback and is deduplicated by request-owned state. ApiExceptionHandler intentionally emits no technical diagnostic for expected 4xx; it is not the HTTP completion owner. No advice-bypass, fabricated-success status, missing listener lifecycle or ERROR/ASYNC defect was demonstrated. Logging in the advice would introduce the wrong completion owner and is not required.

**Manual-process limitation:** The original Swagger browser session, active JVM's runtime overrides, console filters and process identity were not independently inspected. INFO-level filtering is a verified explanation under the repository/demo defaults, not proof of every detail of that earlier session. If targeted DEBUG in the actual serving process still lacks the event, the response request ID, process/log destination and effective logger level require investigation. For visibility during troubleshooting, the relevant Spring property is `logging.level.com.example.agenticorderservice.shared.api.RequestLoggingListener=DEBUG` in the serving application's configuration (for a direct application launch, the corresponding `--logging.level.com.example.agenticorderservice.shared.api.RequestLoggingListener=DEBUG` argument). No such runtime change or application restart was performed here.

### Files changed and test coverage

- `src/test/java/com/example/agenticorderservice/shared/api/RequestLogStateTest.java`: six parameterized status/level cases (200/204/409 at DEBUG and INFO) verify exact method/route/status, level filtering, matching generated X-Request-ID/log MDC, cleanup after filter/listener, preservation of unrelated MDC, no early event and no duplicate after repeated requestDestroyed.
- `src/test/java/com/example/agenticorderservice/shared/api/CustomerDeleteRequestLoggingIT.java` (new): real HTTP and disposable PostgreSQL comparison of GET /customers → 200, physical DELETE → 204 and referenced DELETE → exact CUSTOMER_HAS_ORDERS/409. At DEBUG, each response has exactly one completion with the correct method, route, status, level and response/header correlation. The rejection has no correlated CUSTOMER_DELETED or technical-failure event. The complete persisted customer row (including active/version), order row, item rows and historical order response remain unchanged; the unreferenced customer is physically absent.
- This review record: appended investigation and actual results; historical evidence remains intact. Existing accepted observability documentation did not require changes because the contract is unchanged.

### Actual verification

Command-local Java 21 was used with Docker/JVM-attachment access for disposable tests only:

```sh
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp test failsafe:integration-test failsafe:verify -Dtest=RequestLogStateTest,HttpRequestLoggingTest -Dit.test=CustomerDeleteRequestLoggingIT,HttpRequestLifecycleIT,AsyncTransactionLoggingIT
```

| Gate | Actual result |
| --- | --- |
| Focused fast tests | **Passed: 40 executions** — RequestLogStateTest 25; HttpRequestLoggingTest 15 |
| Focused real-container/PostgreSQL tests | **Passed: 20 executions** — CustomerDeleteRequestLoggingIT 1; HttpRequestLifecycleIT 12; AsyncTransactionLoggingIT 7 |
| Status, correlation, exactly-once and cleanup | Passed for the new comparison/status-level cases; existing REQUEST/ERROR/ASYNC, status-provenance and worker/context restoration coverage retained and passed |
| Rejected DELETE business outcome | Passed: exact 409, no false success/technical event, unchanged customer/order/items/history |
| Full regression | Not rerun: no production changes; earlier full-regression evidence remains historical and is not asserted for these new test sources |
| Original manual Swagger session / live demo | **Pending**; no live request, reset or reseed of the existing demo database |
| SonarQube | No fresh analysis performed or claimed |
| Diff hygiene / human acceptance | `git diff --check` passed; human acceptance **Pending** |

The run exited 0 at `2026-10-18T13:46:42+02:00`, zero failures/errors/skips in all 60 executions. Spotless preparation and lifecycle checks passed. No failed-verification correction was needed. Verbose output, XML reports, the baseline/current diff and status are retained under `/tmp/ch001-http409-logging/` (temporary local evidence). No business/API behavior, dependencies or migrations changed. Nothing was staged, committed, pushed, merged or deployed. Stop for human review.

## Demo Logging Commands — 2026-10-18

**Authorization:** Explicitly approved Makefile/documentation increment plus separately approved minimal launcher propagation. Work remained on `develop`. Existing demo-client/test/review changes were preserved; no production Java, dependencies, schema or database changes were made.

**Changes:** `make demo-run-logged` defaults `LOG_LEVEL` to INFO, validates INFO/DEBUG before startup, creates `logs/`, and runs the existing Python startup once through Bash `pipefail` and append-mode `tee`, combining stdout/stderr with console output. The tee process ignores SIGINT so the launcher's existing Ctrl-C/JVM termination and state cleanup can finish and flush output. Pipeline failures remain nonzero (GNU Make reports its usual recipe-failure status). `make demo-logs` checks for the file and follows it with `tail -f`. `/logs/` is ignored. README links to the expanded demo guide with commands, levels, expected 409 filtering, event searches and request correlation.

**Configuration:** Only the dedicated `DEMO_HTTP_LOG_LEVEL` is read in `start()` and validated against INFO/DEBUG. When supplied, it becomes the exact Spring Boot CLI property `logging.level.com.example.agenticorderservice.shared.api.RequestLoggingListener`; no arbitrary environment forwarding or global DEBUG was added. The existing subprocess environment allowlist is unchanged. Without the dedicated override, `make demo-run` adds no logging property and retains its previous startup and logging behavior.

| Verification | Actual result |
| --- | --- |
| Make syntax/targets and input validation | Passed dry runs and isolated execution of the real Makefile; unsupported levels fail before creating logs or starting the launcher |
| Launcher propagation | Passed mocked startup for absent override, INFO, DEBUG and invalid/empty values; exact logger argument asserted, dedicated input not forwarded in child environment, existing new-session startup retained |
| Console/file and failure propagation | Passed isolated stdout/stderr markers, INFO/DEBUG arguments, append preservation and stub JVM exit 7 producing nonzero Make status with the launcher's diagnostic |
| Duplicate/startup/Ctrl-C lifecycle | Passed isolated real `start()` with build/database/port checks stubbed and a disposable subprocess replacing Java: existing state rejects a second launch, SIGINT terminates the child and removes its state record without orphaning it |
| Follow/missing file | Passed actual `make demo-logs` missing-file guidance and following a newly appended marker in the isolated log |
| HTTP 409 visibility | Passed existing `RequestLogStateTest#completionLevelsPreserveStatusCorrelationAndMdcCleanup`: six parameterized cases, zero failures/errors/skips; 409 completion visible at DEBUG and filtered at INFO, 200/204 visibility and correlation/MDC assertions retained |
| Existing local logs | Passed SHA-256 comparison of `logs/demo.log`; contents unchanged; `git check-ignore` confirms exclusion |
| Live demo/DB startup and real-terminal Ctrl-C | **Pending**; no live demo launch, restart, signal, HTTP request, reset or seed performed. Final sandbox `ps` access was unavailable and no listener/state record was visible at final inspection; continuous liveness of the earlier JVM is not claimed |
| Full Maven regression | Not run as requested; no production Java changes |
| Human review/acceptance | **Pending**; no acceptance or closure inferred |

Evidence: `/tmp/ch001-demo-logging/check.py`, `check.log`, `http-level-test.log`, `baseline.diff`, `implementation.diff`, `existing-log.sha256` and final diff/status (temporary local files). The focused Java command used Java 21 and `./mvnw -B -ntp test '-Dtest=RequestLogStateTest#completionLevelsPreserveStatusCorrelationAndMdcCleanup'`. Isolated checks did not call Docker, Maven packaging or the real database. All verification passed on the first implementation; no failed-check correction was used. An incidental Python bytecode cache created by the isolated harness was removed. No runtime logs were staged, and no staging, commit, push, merge or deployment occurred. Stop for human review.
