# Review

Human acceptance: **Pending**. Initial implementation iteration 0; maximum three subsequent correction iterations. No correction batches needed.

## Task ledger and ownership

- Orchestrator: collector/tests, role instructions, docs, all four records and consolidated verification.
- Native reviewer /root/collector_review: independent read-only collector/test review; completed with no actionable defects. Independently reran 22 tests and diff hygiene successfully.
- Requirements/architecture/documentation: handled by Orchestrator for this bounded tooling increment; no application boundary or architecture changes.

## Verification — 2026-10-18, Europe/Berlin

- Collector: python3 -m unittest discover -s tools -p test_sonar_findings.py; exit 0, 22 passed.
- Mocked Make: /private/tmp/sonar-findings-setup/check_make.py; exit 0, 17 scenarios passed; dry-run recipe sh -n passed. Evidence: /private/tmp/sonar-findings-setup/make-results.json.
- Python AST/TOML syntax, Markdown local links and git diff --check: Passed.
- Maven, Docker, PIT, application coverage/OpenAPI: Not Applicable to tooling-only increment and explicitly excluded.
- Revised-source live Sonar analysis, actual gate and findings: Pending, not authorized or run. Historical evidence does not verify this source state.

## Exit and limitations

CLI: 1 for confirmed failed gate; 3 for required identity, unverifiable gate, incomplete issues or mandatory hotspot Pending; 0 only when all mandatory dimensions pass. Snapshot and supersession Pending alone do not alter exit. Original Maven failure retains priority in the unchanged recipe; Make normally returns 2 for failed recipes.

Current branch findings can change during pagination even with stable totals. Immutable snapshot and supersession remain Pending, nonfatal by explicit human approval. Non-main hotspots remain mandatory Pending and prevent overall success. No actual findings were retrieved or classified in this offline task; independent findings review remains Pending. No cloud suppression or acceptance performed.

## Workflow simplification verification — 2026-10-18

The follow-up setup request authorizes tooling/tests/documentation only. On intake,
`develop` already contained the separate Make targets, retrieval-only collector mode,
updated role/workflow instructions and the relocated four setup records as uncommitted
work. The former `CH-002-sonar-branch-provenance` directory was already absent;
its original contents could not be compared directly. Inspection covered all four
preserved records and repository Markdown references; no inbound links to the former
directory remain. Historical evidence above is retained unchanged. CH-001 is unchanged;
no Product Images directory or additional number reservation was created.

Self-review found the guard-test harness executed a string without its required
imports, causing NameError before its scenarios ran. It is now a normal test method
with explicit imports, retaining all 31 isolated scenarios and both shell syntax checks.
No independent agent review or human acceptance is claimed for this follow-up.

Actual offline verification:

- `python3 -m unittest discover -s tools -p 'test_sonar*.py'`: exit 0,
  27 tests passed (25 collector tests, one guard test covering 31 scenarios,
  one Make target test covering five scenarios). Both recipes passed `/bin/sh -n`
  inside the mocked guard suite. Log: `/private/tmp/sonar-workflow-tests.log`.
- Python AST syntax: all four tooling Python files passed.
- TOML parsing: all seven project configuration/role files passed.
- Markdown local-file links: 93 checked across multi-agent guides/evidence and
  affected root/workflow/register/operations documentation; no missing targets.
  This check validates file destinations, not heading anchors or external URLs.
- `git diff --check`: exit 0. Nothing staged; existing tracked and untracked setup
  work retained. No Java, application tests, POM or GitHub Actions changes.

The resulting operations remain distinct: sonar-local verifies/scans once and
collects against a strictly validated fresh task/analysis; sonar-findings retrieves
current branch issues/hotspots without Maven, submission or analysis identity.
Standalone gate evidence remains Pending. Collector exits are 1 for a confirmed
failed gate and 3 for mandatory incomplete evidence; standalone success requires
complete issues and hotspots. Maven failure retains priority; Make reports failed
recipes as 2. Snapshot provenance and supersession remain separately Pending and
nonfatal. Security findings remain visible regardless of gate success.

Live scan execution, remote Quality Gate, current remote findings, hotspot completeness
and independent findings review remain **Pending**, explicitly not executed here.
Mandatory non-main hotspot provenance still blocks successful completion on develop
unless it is confirmed as the remote main branch; no policy was weakened. Failed
retrieval permits deliberate retrieval retry, never an automatic analysis rerun.
Human review/acceptance remains **Pending**.

## Approved standalone exit-policy correction — 2026-10-18

Human approval explicitly supersedes the earlier standalone hotspot exit requirement
only. The collector now requires `issues` for standalone retrieval, while full analysis
still requires `quality_gate`, `issues` and `hotspots`. Confirmed failed gates retain
exit 1; incomplete required evidence retains exit 3. The Make recipes are unchanged.
Standalone exit 0 is completed issue retrieval, not complete security review. All
hotspot candidates, security issues, provenance limitations and independent-review
statuses remain reported; no Cloud finding disposition changes.

Updated focused tests cover standalone hotspot Pending success, issue API failure and
explicit Failed status, empty issues with unknown hotspot counts, and JSON/console
Pending evidence. Existing full-mode tests still cover passed gate with incomplete
hotspots (3) and confirmed failed gate (1). Operations, quality-gate policy and tester
instructions now distinguish standalone success from full-scan requirements.

Actual verification on develop:

- `python3 -m unittest discover -s tools -p 'test_sonar*.py'`: exit 0,
  29 tests passed (27 collector tests, 31 guard scenarios within one test,
  five mocked target scenarios within one test). Both recipe shell syntax checks
  passed within the guard suite. Log: `/private/tmp/sonar-standalone-policy-tests.log`.
- Python AST syntax: four files Passed. TOML parsing: seven files Passed.
- `git diff --check`: Passed.
- No Maven, scanner or authenticated Cloud requests executed. Live verification
  remains Pending. No Java, dependency, CI, project-setting or CH-001 changes.

Existing unrelated tracked modifications and untracked setup files were preserved;
nothing staged or published. This is agent self-review; human acceptance of the
implementation remains **Pending**. Historical full-scan hotspot blockers above
remain applicable to sonar-local; their earlier application to standalone exit codes
is superseded by this explicit policy approval.
