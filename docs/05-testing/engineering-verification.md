# Automated Local Engineering Verification

**Authority:** Human-approved CH-003 Increment 0 follow-up, 2026-10-21. [ADR-0007](../03-decisions/ADR-0007-hexagonal-architecture-migration.md) accepts Option B. This workflow does not authorize production migration or human acceptance.

## Existing gate inventory

| Mechanism | Actual configuration and scope |
| --- | --- |
| Java / Boot | Java 21, Spring Boot parent 4.1.1; unchanged POM |
| Spotless | 3.10.4, Palantir 2.102.0/PALANTIR; check at validate; production/test Java only; no auto-apply in verification |
| JUnit | Boot-managed Jupiter/Platform 6.0.3; the request's JUnit 5 label does not change the accepted JUnit 6 baseline |
| Mockito | Boot-managed core and junit-jupiter 5.23.0; supported local execution needs JVM agent attachment; no plugin/config change |
| ArchUnit | Existing 1.5.1 test dependency; baseline checks with exact migrated adapter-to-contract allowances, domain/rule self-tests, populated feature slice suites and Increment 7 architecture-wide enforcement; see [architecture tests](hexagonal-architecture-tests.md) |
| Surefire / Failsafe | Boot-managed 3.5.6; fast *Test at test, PostgreSQL *IT through integration-test/verify |
| Testcontainers / PostgreSQL | Boot-managed Testcontainers 2.0.5; configured isolated postgres:17.11-bookworm; Docker required, never the persistent development DB |
| JaCoCo | 0.8.15 prepare-agent and verify-phase report; XML/HTML at target/site/jacoco; no check goal or numeric minimum |
| PIT | Existing mutation-order profile, pitest-maven 1.30.1, pitest-junit5-plugin 1.2.3 (artifact name is not a request to downgrade JUnit), DEFAULTS, one thread, HTML/XML; failWhenNoMutations=true, skipFailingTests=false; no score threshold |
| Maven profiles | mutation-order only; no new Maven profile/plugin or dependency |
| CI | Existing .github/workflows/build.yml runs Java 21 clean verify and SonarScanner 5.8.0.7211 on configured push/PR events; qualitygate.wait=false; unchanged and not triggered here |
| SonarQube Cloud | Existing local scanner/findings targets remain manual entry points; no invocation, credential access, upload, remote evaluation or setting changes in this automation |
| Native hooks | Existing PreToolUse reminder and SubagentStop handoff checker; script tests separate from runtime trust/delivery |
| Git hook | Prepared .githooks/pre-commit, inactive; existing focused ArchUnit test lifecycle includes Spotless; no full regression/coverage/PIT/Cloud pipeline |

Versions above were confirmed from the current POM, existing stack record and a Java 21 filtered dependency tree on 2026-10-21; actual runs belong in the CH-003 records. No numeric quality threshold is invented. If a future POM supplies checks/thresholds, existing Maven execution must preserve them and the inventory must be updated.

## Commands

Set JAVA_HOME to an existing Java 21 JDK. From repository root:

```sh
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home make verify-fast
# Before human acceptance of an individual CH-003 increment (no PIT):
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./mvnw -B -ntp -DfailIfNoTests=true clean verify
# Final migration acceptance only, after all increments:
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home make verify-full SCOPE=all
```

For another installed JDK path, change only JAVA_HOME. The small [wrapper](../../tools/verify.py) uses Python 3 standard library and the existing Maven Wrapper/Makefile. Its process lock uses POSIX flock (macOS/Linux). Evidence defaults to a unique directory under the OS temp directory; set VERIFICATION_EVIDENCE_ROOT to another directory outside the checkout. Evidence is temporary unless deliberately archived; durable summaries must survive loss of logs.

### Console summary

Both commands finish with a compact stdout summary: execution PASS/FAIL, elapsed seconds, executed/passed/failed/errored/skipped test counts, architecture checks, Spotless, evidence directory and available report/log paths. Full verification also shows PostgreSQL integration counts, JaCoCo counters and percentages, PIT outcomes and the pending mutation-quality disposition. Failed tests include their names and a bounded error message; command failures include concise log diagnostics.

The display reads current test XML, Maven logs and collected coverage/mutation evidence. Missing, stale or invalid test reports and undispositioned skips cannot produce console PASS. On Maven failure it can read fresh reports directly from target; these paths remain temporary. The presentation preserves existing JSON, evidence collection and process exit codes, including nonzero failures. If the existing exit policy returns zero despite skips or incomplete display evidence, the console explicitly reports FAIL and explains the retained exit zero. Execution PASS does not grant mutation-quality approval, independent review or human acceptance.

Run the wrapper regression tests with `python3 -B tools/test_verify.py`; fixtures exercise success, failures, missing/stale evidence, skips and unchanged machine-report/exit behavior without launching Maven.

### Fast

make verify-fast runs one ./mvnw -B -ntp -DfailIfNoTests=true clean test. This covers formatting verification at validate, the complete fast JUnit/Mockito/MVC suite and existing ArchUnit checks. clean prevents deleted-class leftovers affecting package imports. It does not run Failsafe ITs, PIT, scanner goals or the verify-phase coverage report. No automatic formatting or staging occurs; formatting fixes must be explicitly limited to intended files.

Run fast verification automatically after a completed implementation increment or meaningful correction, not after each edit. Focused tests during development remain useful; once relevant inputs are stable reuse their evidence appropriately.

### CH-003 increment acceptance without PIT

The [human-approved policy](../04-agentic-coding/multi-agent/quality-gates.md#ch-003-migration-verification-policy) applies from Increment 1 onward. Automatically run focused tests, relevant ArchUnit and Spotless checks plus make verify-fast during each increment. Before human acceptance, run the complete Java 21 `./mvnw -B -ntp -DfailIfNoTests=true clean verify` and independent architectural/code review. This existing lifecycle includes all unit/applicable architecture tests, PostgreSQL/Testcontainers ITs, Spotless and JaCoCo reporting/checks; PIT is a separately invoked plugin and is not started by this command.

Do not activate the mutation profile or invoke mutation goals/targets or PIT-enabled make verify-full during an individual increment, including corrections. Do not pass test/IT/coverage skip flags or narrow the acceptance suites. If Docker or PostgreSQL verification cannot run, report the blocker and keep that gate Pending. Archive Surefire/Failsafe/JaCoCo reports, log/exits and source identity externally before another clean build; preserve commands/counts/findings in CH-003 records. The direct Maven command uses Maven's normal console reporting, not the fast/full wrapper's JSON summary. No wrapper, Makefile or quality configuration is changed by this policy.

### Full — final CH-003 migration acceptance

make verify-full defaults to SCOPE=all; make verify-full SCOPE=all remains equivalent. The wrapper also defaults to all when invoked directly as python3 tools/verify.py full. The console summary prints the selected scope. Use order/product/customer/shared/all selectors from the [mutation runbook](mutation-testing-runbook.md#current-automated-workflow). For CH-003, invoke make verify-full SCOPE=all only after all implementation increments are completed. All existing scope selectors remain available for other explicitly authorized mutation workflows; order remains domain-only. This scheduling policy does not change selector semantics.

Full sequence, executed serially:

1. Java 21 validation and checkout-specific exclusive wrapper lock.
2. One ./mvnw -B -ntp -DfailIfNoTests=true clean verify: formatting, all fast tests/ArchUnit, PostgreSQL/Testcontainers ITs, existing Maven gates and JaCoCo report.
3. Archive fresh Surefire, Failsafe and JaCoCo reports before mutation execution; collect source hashes, revision/status, commands/exits and actual counters.
4. Invoke the existing make mutation-test SCOPE=<selected scope>. Its own required full fast-test baseline and XML validation are intentionally preserved, not bypassed after verify. This distinct JaCoCo-disabled PIT preflight is the only repeated baseline; do not add a separate manual test/verify invocation around the wrapper.
5. Read fresh mutation XML and record outcomes; the Orchestrator and independent reviewer evaluate survivors, uncovered paths, timeout/error outcomes, selector limitations, coverage/assertion gaps and all required gate statuses before a final report.

The wrapper does not spawn agents or grant acceptance. Independent reviewer assessment is a required workflow step outside the Make process, using native Codex tools. For CH-003 the Orchestrator invokes this PIT-enabled full workflow automatically at final migration acceptance, consolidating final coverage/regression/ArchUnit evidence and independent reviewer findings against the pre-migration mutation baseline. Individual increments and corrections use the no-PIT acceptance command above. Earlier Increment 0/1 full/PIT runs remain historical evidence; this policy does not erase or repeat them. Configuration/documentation-only maintenance uses proportional checks.

Maven failure prevents mutation and records it Pending/blocked; no expensive step is silently skipped. Missing reports/nonpositive populations are verification errors even when a command exits zero. Test skips are retained in summaries and require explicit disposition; a zero process exit is not proof of complete verification. PIT execution success does not approve surviving mutants or imply a score threshold. Failures block recommending acceptance; environmental blockers retain Pending verification and actual observed failures remain Failed. Fix only within authorized scope; no production/test changes to hide an existing finding.

The wrapper lock prevents concurrent verify-fast/full calls in one checkout, including make -j. Direct Maven, mutation-test, demo builds and pre-commit are not secretly locked by it: the Orchestrator must serialize every target-writing entry point and must never run one alongside a wrapper. Do not describe a wrapper-local lock as universal build isolation. Full and mutation commands are sequential, not parallel Make prerequisites.

## Cloud, hooks and human ownership

SonarQube Cloud analysis/gate evaluation remains human-controlled. Automated local verification never calls sonar-local, sonar-findings, scanner goals, remote workflows or credentials. A separate explicit human request is required for an agent-assisted Cloud action; a general increment/full-lifecycle approval is insufficient. Existing remote quality/security requirements are not weakened: their evaluation remains a separate human gate, Pending until actual matching evidence is supplied. Existing CI is unchanged; a later human push may trigger its existing scanner workflow, but agents do not push or dispatch it.

Native hook scripts remain lightweight. Use /hooks in a trusted session to review/trust only the exact project definitions when the developer chooses; do not bypass trust or claim delivery from smoke tests. The Git hook remains inactive and core.hooksPath must not be set by this task. See [hook activation guide](../04-agentic-coding/multi-agent/hooks.md).

No automatic Git add/commit/push/merge/rebase/reset, deployment, repository settings or branch-protection changes. No automation or reviewer may declare Human Acceptance. Continue to [quality gates](../04-agentic-coding/multi-agent/quality-gates.md) and the existing handoff/three-correction-cycle policy.
