# Implementation

Status: Completed — Human Review Required. Two correction batches completed; no publication.

## Authorization and intake

Defined lifecycle and additive image metadata migration authorized by the [original request](01-documentation.md). No new dependencies or material security/architecture decisions implicitly approved.

Branch: develop. Intake HEAD: `a257c6b1090b2771cbb2e8fb906bf17b47316717`.
Pre-existing status before records: `?? demo/assets/` (eight generated PNGs and their README). No staged or tracked modifications at intake.

## Plan

1. Requirements and architecture handoffs; identify blockers.
2. Approved target documentation and contracts.
3. Scoped implementation, migration and demo seeding.
4. Independent tests and available gates, independent review.
5. At most three correction batches; documentation consistency and final human review.

## Ownership

Orchestrator writes all four Change Request records; other file ownership assigned before edits. Specialists currently read-only. Implementation and additive V3 file now authored; no persistent production/development schema migration executed.

## Verification

Pending. No build, scan or application runtime invoked during intake.

## Implemented components and ownership

- `/root/implementation`: product image processor/storage/domain/repository/service/
  controller/content/errors; safe shared API transport/error integration; V3; image
  configuration and ignored runtime root. Correction1 extends OpenApiConfiguration.
- `/root/testing`: independent new image unit/MVC/PostgreSQL/HTTP/migration tests;
  narrowly updated existing API error oracle and schema/V2 migration fixtures; runtime
  OpenAPI snapshot/assertions. Scoped formatter on changed Java only.
- `/root`: demo JPEG derivation, seed_images HTTP multipart helper and repeatable
  recorded-ID preflight; six offline Python tests; asset README; all four CH records.
- `/root/documentation`: seven affected authoritative Markdown documents.

Storage root defaults to ./var/product-images,independent of Git-managed demo sources;
external configuration supports persistent deployments. Writes default false,demo true.
New endpoints implement multipart PUT204, original GET,thumbnail GET and idempotent
DELETE204 for an existing product. Multipart/image negotiation is limited to image
handlers; existing product JSON unchanged. No POM dependency or CI modification.

## Actual verification so far

- Parent: six offline demo-seeding tests Passed,exit0; log
  `/private/tmp/ch002-demo-tests.log`. Preflight/repeated IDs/inactive legacy and actual
  multipart payload tested against mocks; not proof of running persistent demo.
- Testing: five focused PostgreSQL lifecycle tests Passed,exit0;
  `/tmp/CH-002-testing/focused-it-1.log`.
- Testing:167 focused fast tests Passed,exit0;
  `/tmp/CH-002-testing/focused-unit-7.log`. Earlier failed runs retained in testing log
  directory; causes were missing new catalog entries and Spring7 test-builder API.
- Scoped changed-Java formatting Passed; `/tmp/CH-002-testing/format.log`.
- Full regression,coverage,PIT,Sonar and final runtime snapshot checks Pending.

No staging,publication,remote workflow dispatch or destructive existing database action.

## Correction2 final local verification

Test-only batch2 adds mutation-driven assertions without changing any production file.
Final local clean verify exit0:618 fast tests and155 integration tests,zero failures,
errors or skips;Spotless and generated runtime OpenAPI checks Passed. Six independent
offline demo tests also Passed. Actual helper seeding runs twice over real HTTP with
all eight JPEG originals decoded/compared and a ninth independent imageless product.
Matching full reports and JaCoCo retained under
`/tmp/CH-002-testing/correction2-regression/` before PIT baselines overwrite target/.

Overall JaCoCo covered/missed:instructions7907/178 (97.798%),lines1698/48 (97.251%),
branches910/74 (92.480%). Product PIT final execution Passed:208 generated,
185 KILLED,21 SURVIVED,2 NO_COVERAGE,zero timeouts/errors. Compared to initial run,
28 survivors and2 uncovered mutants now killed;no regressions. All43 metadata-domain
mutants killed. Remaining outcomes are independently reviewed,not automatically
accepted as equivalent. Shared-scope PIT is being evaluated separately because new
shared API boundary behavior lies outside Product selector. Configuration lies outside
both named mutation scopes.

All78 src/main file hashes match actual scanner source.json. Comparison evidence
`/tmp/CH-002-testing/production-source-comparison.json`. Exact final test-tree remote
analysis remains Pending:the recorded Passed remote gate belongs to its genuine
pre-batch2 source identity,not a fabricated new identity. No second Cloud upload.

## Final execution and source validity

All six actual specialist roles returned handoffs. Production completed by Implementer;
Testing independently added69 fast image cases and10 image integration cases plus
necessary compatibility fixtures. Final local clean verify at2026-10-18 17:38:23 CEST,
exit0;618+155 tests. Parent6 offline demo tests exit0. Logs and archived XML preserve
earlier failure/superseding-run history. Actual commands and full gate evaluation are
in [review](03-review.md). Shared PIT completed399mutants:301killed,74survived,
18uncovered,6timedout;timeouts are not assertion kills. Residual quality disposition
remains Pending;no numeric threshold invented and no third correction batch started.

Source/test manifest hash `34d6252ea372a318fa6e7f9a54d72b1a894fb4bc745a99d354e2f59389f106b5`.
Current production matches the single successful Cloud analysis;test/audit-only changes
are explicitly later. No persistent development/production database altered; migrations
and repeatable helper verified only with disposable PostgreSQL/Testcontainers.

## V-007 continuation — assigned test extension

Explicit human authorization resumes the existing Testing assignment. Only
ProductImageLifecycleIT.java is extended: two parameter cases perform real JPEG/PNG
uploads and replacements, assert every metadata getter after a fresh committed-database
reload, and independently compare all ten JDBC columns. Filesystem generation changes,
fixture bytes, decoded images and actual file lengths supply independent expectations.
No production/migration/API/dependency change and no new implementation correction batch;
counter **2/3** preserved. The initially Pending focused integration, relevant unit/API and full clean verify
results are superseded by the executed evidence below and [review](03-review.md).

## V-007 executed verification — 2026-10-18

Only ProductImageLifecycleIT.java changed in the code/test input set. The parameterized
roundtrip test starts at line 121: two cases cover JPEG→PNG and PNG→JPEG. Each first
upload and replacement is committed before a fresh EntityManager transaction clears
and reloads metadata; all ten getters match independent expectations. JDBC then
compares all ten columns. Filesystem generation deltas establish keys independently;
fixture bytes/media/dimensions and actual PNG file length/pixels establish metadata.
One row, unchanged product identity/inactive status, new generations and retired-file
removal are verified. Existing assertions are preserved.

Focused command (Java 21):

```text
./mvnw -B -ntp test failsafe:integration-test failsafe:verify '-Dtest=ProductImage*Test,ProductApiTest,ApiErrorContractTest,OpenApiDocumentationTest' -Dit.test=ProductImageLifecycleIT
```

Focused exit 0 at 20:37:54 CEST: 127 fast tests + 8 lifecycle cases, no failures,
errors or skips. Final `./mvnw -B -ntp clean verify` exit 0 at 20:39:43 CEST:
618 Surefire + 157 Failsafe, no failures/errors/skips. Spotless, runtime OpenAPI
and existing concurrency checks Passed. Matching XML and JaCoCo are archived at
`/tmp/CH-002-V007-20261018/final-regression/`; commands/logs and checksums are in
`/tmp/CH-002-V007-20261018/artifact-manifest.json`. Earlier runs are retained.

Coverage: lines 1698 covered/48 missed (97.251%), branches 910/74 (92.480%),
instructions 7907/178 (97.798%). Prior scoped PIT reused: all production, fast-test,
resource and build inputs are identical; only an excluded *IT changed. No new score,
kill or resolution is inferred. Prior actual Sonar gate is reused for its recorded
source, with exact final-tree analysis and hotspot completeness still Pending.

Independent Reviewer /root/review verified assertions, actual reports and reuse
comparison, with no actionable finding; V-007 is Passed. Code/test manifest hash:
`1e9928703210b64a48b2f08f477343141414cabfb5bead4947ca0d255d1d2a13`.
Verified test SHA256: `1d8a73dd54f40e6ab7373eb85bba931a517b5cf62c7a17af43adbf8d595a1324`.
No production/migration/API/dependency changes; correction counter remains **2/3**.
