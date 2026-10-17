# Quality Report — Agentic Order Service

This report separates historical execution results from the stable project overview. It was compiled from existing evidence on 2026-10-17; no tests, builds, PIT or SonarQube analysis were executed for this documentation update.

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
