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
