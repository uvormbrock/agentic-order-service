# Requirements and Authority

Approved target: validate fresh scanner task ID, REPORT type, successful processing, project, branch/type and analysis ID; verify gate by analysis ID. Retrieve complete current-branch issues. Snapshot provenance and supersession remain explicitly Pending and alone nonfatal. Non-main hotspot completeness remains Pending and mandatory for exit policy.

Original prompt provenance: human attachment, 2026-10-18. Original request preserved verbatim below. No architecture decision changes or implicit security acceptance.

## Original request

# SonarQube Collector — Approved Provenance Policy and Implementation

Work on `develop` and follow `AGENTS.md`.

## Human Policy Decision — Approved

I explicitly approve the following limited provenance policy for the existing SonarQube Cloud findings collector.

SonarQube Cloud does not provide a documented immutable, analysis-specific issue snapshot for the short-lived `develop` branch through the investigated APIs.

Therefore, the workflow may distinguish:

1. Verified scanner execution and Compute Engine task.
2. Verified analysis ID.
3. Analysis-specific Quality Gate result.
4. Complete retrieval of current branch issues.
5. Unverified historical issue snapshot provenance.
6. Unverified supersession state.
7. Independently evaluated security-hotspot completeness.

**Approval:** Missing immutable issue-snapshot provenance or unverified supersession alone must not cause an otherwise successful collector execution to fail.

These limitations must remain explicitly reported as `Pending`.

This approval does not authorize weakening other quality gates, suppressing findings, accepting security risks or changing the existing security-hotspot exit policy.

## Implementation

Apply the smallest correct changes to the existing collector, its tests, Makefile integration and directly relevant workflow documentation.

### Analysis Identity

- Use fresh scanner task evidence from `report-task.txt`.
- Resolve the actual Compute Engine task.
- Validate task identity, project and branch using supported metadata.
- Resolve the corresponding analysis ID.
- Fail closed if mandatory identity validation is missing or inconsistent.
- Do not use the incompatible short-lived-branch query through `/api/project_analyses/search`.

### Quality Gate

- Verify the actual Quality Gate using the analysis ID.
- Distinguish scanner completion from Quality Gate success.
- Never report `Passed` without server evidence.

### Issues

- Retrieve issues explicitly scoped to the correct project and branch.
- Preserve pagination and complete-result checks.
- Report actual findings and counts.
- Treat retrieval failure or incomplete pagination as failure.
- Do not treat successful retrieval as proof of an immutable historical analysis snapshot.
- Report `issueSnapshotProvenance: Pending` where the API cannot establish it.
- Report `supersessionVerification: Pending` where latest-analysis identity cannot be reliably established.

### Security Hotspots

- Preserve independent hotspot retrieval and completeness reporting.
- Preserve the existing non-main-branch limitation.
- Preserve the existing mandatory hotspot exit policy.
- Do not silently convert hotspot Pending or Failed into Passed.

### Diagnostics

For API failures, include sanitized:

- Processing stage.
- Endpoint path.
- HTTP status.
- Server error category or minimally necessary sanitized message.

Never expose tokens, Authorization headers or sensitive raw responses.

## Exit Policy

Return nonzero when:

- Required scanner-task or analysis identity verification fails.
- The actual required Quality Gate fails or cannot be verified.
- Required issue retrieval fails or returns incomplete results.
- Another existing mandatory quality gate fails.
- The existing security-hotspot policy requires failure.

Do not return nonzero **solely** because immutable issue-snapshot provenance or supersession verification is unavailable, provided all other mandatory checks have succeeded.

Do not change the meaning of `Passed`, `Failed`, `Pending` or `Not Applicable`.

A successful command exit does not mean all reported dimensions are Passed.

## Multi-Agent Integration

Ensure the Testing Agent returns the separate verification dimensions to the Orchestrator.

Ensure the Review Agent:

- Evaluates actual retrieved findings.
- Distinguishes new-code findings from existing baseline issues where supported.
- Identifies actionable issues within the authorized Change Request.
- Does not treat missing snapshot provenance as evidence that no issues exist.
- Does not automatically suppress, dismiss or accept issues.

The Orchestrator must preserve the maximum of three correction iterations and include all unresolved limitations in the final human-review report.

## Verification

Add or update focused offline tests covering:

- Short-lived `develop` branch.
- Scanner-task and analysis-ID validation.
- Analysis-specific Quality Gate.
- Branch-scoped issue retrieval and pagination.
- Missing or inconsistent analysis identity.
- HTTP errors and sanitized diagnostics.
- Snapshot-provenance Pending.
- Supersession Pending.
- Existing hotspot behavior and exit semantics.
- Correct overall exit-code propagation.

Run applicable offline collector tests, mocked Make scenarios, syntax validation, documentation-link checks and `git diff --check`.

Do not run Maven, Docker, PIT or a real SonarQube scan during this implementation task.

Do not retrieve real credentials or make live authenticated Cloud API calls without separate authorization.

## Restrictions

- No Java application changes.
- No application-test or Maven dependency changes.
- No GitHub Actions changes.
- No SonarQube Cloud project-setting changes.
- No historical CH-001 modifications.
- Preserve unrelated working-tree changes.
- No staging, commits, pushes, merges or deployments.

## Final Report

Report:

1. Exact policy implemented.
2. Files modified.
3. Actual offline verification results.
4. Remaining provenance and hotspot limitations.
5. Final exit-code semantics.
6. Git status.

Stop for human review.
## Acceptance evidence

Offline tests cover SHORT/LONG types, identity failure, analysis-ID gate, branch-scoped pagination, safe HTTP failures, Pending limitations and mandatory exits. Human acceptance Pending.
