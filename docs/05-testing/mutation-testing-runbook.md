# Manual Mutation Testing Runbook

**Status:** Reviewed – Accepted

**Execution:** No experiment has been executed. Commands below are instructions for a future separately authorized investigation, not recorded results or permission to run them.

Use the [mutation-testing strategy](mutation-testing.md) for methodology and outcome definitions, the [testing strategy](testing-strategy.md#running-tests) for ordinary Maven execution, and the [AI workflow](../04-agentic-coding/ai-development-workflow.md) for approval gates. This runbook selects no automated tool, plugin, score threshold or CI change.

## A. Prerequisites

- Obtain human authorization for the exact mutation, test selection, branch creation and evidence location. Later test improvements and production fixes require separate authorization.
- Start from the committed, human-accepted baseline on `develop`, with a clean index and working tree, including untracked files. The current documentation changes must first be reviewed and handled through the normal commit workflow; do not discard them to satisfy this prerequisite.
- Run from the repository root. Use Java 21 and the executable Maven Wrapper 3.9.9. The example JDK path below exists on the reviewed macOS workstation; confirm it on the execution machine.
- Docker is needed only when selecting PostgreSQL/Testcontainers integration tests. Those tests use disposable databases, not local Compose credentials or the persistent development database. Docker unavailability is a blocker, not a passing result.
- Do not invoke the Sonar Maven goal or push temporary mutants. Local commits do not inherently trigger GitHub Actions or SonarQube; external automation and hooks may differ, so inspect them before authorized commits.

Inspect the environment without changing system Java configuration:

```sh
pwd
git status --short --untracked-files=all
git branch --show-current
git config --get core.hooksPath
git rev-parse --git-path hooks
mutation_java_home=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
env JAVA_HOME="$mutation_java_home" ./mvnw -v
```

An unset `core.hooksPath` normally produces no value and a nonzero exit status; inspect the default hooks directory reported by Git as well. Maven version output must report Maven 3.9.9 and Java 21. Do not proceed if the working tree is dirty, the accepted baseline is uncertain, or the required environment differs. No Maven/test command in this runbook was executed while writing it.

## B. Create an Isolated Experiment Branch

After confirming the clean tree and execution authorization:

```sh
git status --porcelain=v1 --untracked-files=all
git switch develop
git log -1 --format='%H %s'
git remote -v
git branch -vv
```

The status command must produce no output. Confirm that the displayed `develop` commit is the intended accepted baseline. The configured remote is currently `origin`; do not assume it must be synchronized. If synchronization is explicitly authorized and desired, first fetch and inspect divergence:

```sh
git fetch origin
git log --oneline --left-right develop...origin/develop
```

Only if the inspected remote tip is approved and fast-forwarding is appropriate:

```sh
git merge --ff-only origin/develop
```

Stop if histories diverge or the remote contains unreviewed work; do not reset or manufacture a merge. Reconfirm the baseline after any update. Then create the specifically requested experiment branch:

```sh
git switch -c experiment/mutation-testing
git branch --show-current
mutation_baseline=$(git rev-parse HEAD)
git show --no-patch --format=fuller "$mutation_baseline"
git status --porcelain=v1 --untracked-files=all
```

Expect `experiment/mutation-testing` and an empty status. If that branch already exists, stop and inspect its purpose/history; do not overwrite or automatically reuse it. Keep the shell variables for the following commands in the same shell session.

## C. Establish the Green Baseline

Assign a unique experiment ID before starting; use the next unused ID in the agreed investigation record. `MUT-001` below is illustrative, not an executed experiment. Create a dedicated evidence directory outside the repository for that ID, never reuse it for another mutation, and retain its printed location. The command substitution runs in a subshell: its `exit 1` stops only directory creation, not your interactive shell. The conditional prevents recording into a failed or reused directory.

```sh
mutation_experiment_id=MUT-001
if mutation_evidence_dir=$(
    mutation_evidence_root=$(mktemp -d /tmp/agentic-mutation.XXXXXX) || exit 1
    mutation_new_dir="$mutation_evidence_root/$mutation_experiment_id"
    mkdir "$mutation_new_dir" || exit 1
    printf '%s\n' "$mutation_new_dir"
); then
    printf '%s\n' "$mutation_evidence_dir"
    printf '# Experiment %s\n\nEvidence directory: %s\n' "$mutation_experiment_id" "$mutation_evidence_dir" > "$mutation_evidence_dir/experiment.md"
    git rev-parse HEAD > "$mutation_evidence_dir/baseline-commit.txt"
    git branch --show-current > "$mutation_evidence_dir/branch.txt"
    git status --porcelain=v1 --untracked-files=all > "$mutation_evidence_dir/baseline-status.txt"
    env JAVA_HOME="$mutation_java_home" ./mvnw -v > "$mutation_evidence_dir/environment.txt" 2>&1
else
    unset mutation_evidence_dir
    printf '%s\n' 'ERROR: Evidence directory creation failed. Stop before running an experiment.' >&2
fi
```

Stop on any creation or recording failure. Keep `mutation_experiment_id`, `mutation_evidence_dir` and the earlier baseline/Java variables in the same shell session; do not rerun the setup for an existing experiment. `mktemp` creates a fresh root and `mkdir` without `-p` rejects a pre-existing experiment directory rather than reusing it.

Illustrative contents under the generated temporary root, after a future authorized experiment:

```text
MUT-001/
├── baseline-commit.txt
├── baseline-order.log
├── baseline-order.exit
├── mutation.patch
├── mutant-original-suite.log
├── mutant-original-suite.exit
├── mutant-original-suite-surefire/
├── restored-order.log
├── restored-order.exit
├── restored-order-surefire/
├── restored-production.diff
├── restored-production-index.diff
└── experiment.md
```

Preserve exit codes, relevant test reports, the mutation patch and restoration evidence. Never overwrite original unchanged-suite results with post-hardening results. The filenames below describe the first attempt only: for each separately authorized retry or additional run, use new names such as `mutant-original-suite-attempt-02.log`, its matching `.exit` and a distinct report directory. Keep improved-suite results separately named and linked to the same experiment ID.

`/tmp` is not durable storage. Transfer reviewed evidence to an approved durable location before temporary files are removed or become unavailable, and record that destination in `experiment.md`. Do not create repository evidence folders or commit generated logs/reports without separate approval.

Run the existing Order tests before touching source or tests. The conditional preserves an expected nonzero test exit without depending on shell pipeline behavior:

```sh
if env JAVA_HOME="$mutation_java_home" ./mvnw -B -ntp -Dtest=OrderTest test > "$mutation_evidence_dir/baseline-order.log" 2>&1; then
    mutation_exit=0
else
    mutation_exit=$?
fi
printf '%s\n' "$mutation_exit" > "$mutation_evidence_dir/baseline-order.exit"
cat "$mutation_evidence_dir/baseline-order.log"
```

Proceed only if the exit code is zero and the report shows the intended tests actually executed without failures/errors/skips. Stop on an unexpected baseline failure; do not count it as mutant detection. A narrower valid method selector is `-Dtest=OrderTest#calculatesPrdThresholdAndHalfUpExamples`; use the same selection for baseline and mutant comparisons.

For the complete Docker-free Surefire suite, record a separate baseline:

```sh
if env JAVA_HOME="$mutation_java_home" ./mvnw -B -ntp test > "$mutation_evidence_dir/baseline-fast.log" 2>&1; then
    mutation_exit=0
else
    mutation_exit=$?
fi
printf '%s\n' "$mutation_exit" > "$mutation_evidence_dir/baseline-fast.exit"
```

If the approved scope includes PostgreSQL integration tests, check Docker and record `verify`, which includes both Surefire and Failsafe:

```sh
docker info
if env JAVA_HOME="$mutation_java_home" ./mvnw -B -ntp verify > "$mutation_evidence_dir/baseline-verify.log" 2>&1; then
    mutation_exit=0
else
    mutation_exit=$?
fi
printf '%s\n' "$mutation_exit" > "$mutation_evidence_dir/baseline-verify.exit"
```

Stop if Docker is unavailable; do not skip integration tests and call the gate passed. Failsafe is configured for `integration-test` and `verify`; Surefire selects `*Test`, Failsafe selects `*IT` under the existing conventions. Reports are in `target/surefire-reports/` and `target/failsafe-reports/`. Capture relevant reports after each run because subsequent runs can overwrite them. Do not start Compose or run a persistent migration for this experiment.

The existing suite is the object being evaluated. Keep every selected test unchanged until the original-suite result is preserved and reviewed; improving a test first would measure a different baseline.

## D. Perform One Manual Mutation

**Proposed example only:** [Order constructor](../../src/main/java/com/example/agenticorderservice/order/domain/Order.java) and [existing threshold test](../../src/test/java/com/example/agenticorderservice/order/domain/OrderTest.java), method `calculatesPrdThresholdAndHalfUpExamples`. The [PRD](../01-requirements/product-requirements.md) requires a 10% discount when subtotal is at least EUR 100.00.

Inspect the current expression and fixture before editing; stop if they differ from this reviewed example:

```sh
mutation_source=src/main/java/com/example/agenticorderservice/order/domain/Order.java
rg -n -F 'discount = subtotal.compareTo(Money.of("100")) >= 0' "$mutation_source"
rg -n -A 7 'void calculatesPrdThresholdAndHalfUpExamples' src/test/java/com/example/agenticorderservice/order/domain/OrderTest.java
git diff --exit-code "$mutation_baseline" -- src/test
```

In the editor, change only this comparison:

```diff
-        discount = subtotal.compareTo(Money.of("100")) >= 0
+        discount = subtotal.compareTo(Money.of("100")) > 0
```

Original: EUR 100.00 receives EUR 10.00 discount and totals EUR 90.00. Mutant: the same subtotal receives zero discount and totals EUR 100.00. The existing test includes `{"100", "10", "90"}` and should fail on its discount assertion. This is an expected outcome inferred from source, not an observed kill.

Inspect and preserve the exact single-change patch:

```sh
git status --short
git diff -- "$mutation_source"
git diff --cached
git diff -- "$mutation_source" > "$mutation_evidence_dir/mutation.patch"
git diff --exit-code "$mutation_baseline" -- src/test
```

Do not stage or commit the temporary mutation. Additional candidates, requiring their own approval and separate experiment IDs, include HALF_UP discount rounding, duplicate-product quantity consolidation, and omitting one Product/Customer editable-field update. Never combine several faults into the same experiment.

## E. Execute and Interpret Tests

Run the same selected existing tests against the mutant, saving the initial result before proposing changes:

```sh
if env JAVA_HOME="$mutation_java_home" ./mvnw -B -ntp -Dtest=OrderTest test > "$mutation_evidence_dir/mutant-original-suite.log" 2>&1; then
    mutation_exit=0
else
    mutation_exit=$?
fi
printf '%s\n' "$mutation_exit" > "$mutation_evidence_dir/mutant-original-suite.exit"
cat "$mutation_evidence_dir/mutant-original-suite.log"
cp -R target/surefire-reports "$mutation_evidence_dir/mutant-original-suite-surefire"
git diff --exit-code "$mutation_baseline" -- src/test
```

Use a new evidence directory/filename for every attempt; do not overwrite the first outcome with a later improved-suite result. An approved broader fast run uses `./mvnw -B -ntp test`; an approved database run uses `./mvnw -B -ntp verify`, both with the same command-local Java 21 environment and separate logs/exit codes. Do not use direct Failsafe goals as a substitute for the configured lifecycle.

| Observed result | Interpretation and next action |
| --- | --- |
| Relevant original test fails at the intended behavioral assertion | Killed by the existing suite; preserve the exact assertion and restore the source. |
| Selected tests pass despite changed approved behavior | Survived; investigate selection and assertions before claiming a test gap. |
| Selected tests never exercise the changed code | Uncovered; do not report covered-but-undetected behavior. Establish reachability using source/available evidence, not just a green run. |
| No observable difference for supported inputs | Equivalent candidate; document the reasoning for human review. Difficulty writing a test is not equivalence. |
| Compilation, setup, Docker, timeout or unrelated test failure | Inconclusive; preserve the error and resolve/report the blocker. Nonzero Maven exit alone does not prove a kill. |
| Original accepted implementation itself violates approved behavior | Genuine production-defect finding; stop and request separate correction authorization. Do not disguise a permanent fix as a mutation. |

For the worked example, expect the EUR 100 boundary assertion to detect the mutation. Inspect the actual test and failure: do not infer causality from a generic build failure. No detection or mutation score is claimed by this runbook.

## F. Restore the Original Implementation Safely

Review all modified/staged/untracked files before restoring anything:

```sh
git status --short --untracked-files=all
git diff --stat
git diff -- "$mutation_source"
git diff --cached
```

Only when **all** changes in the selected source file are experiment-owned and its original content is the recorded baseline, restore that one file:

```sh
git restore --source="$mutation_baseline" --worktree -- "$mutation_source"
git diff --exit-code "$mutation_baseline" -- "$mutation_source"
git diff --cached --exit-code "$mutation_baseline" -- "$mutation_source"
git diff --exit-code "$mutation_baseline" -- src/main
git diff --cached --exit-code "$mutation_baseline" -- src/main
git diff "$mutation_baseline" -- src/main > "$mutation_evidence_dir/restored-production.diff"
git diff --cached "$mutation_baseline" -- src/main > "$mutation_evidence_dir/restored-production-index.diff"
```

These checks must return zero for unchanged production content. Worktree restoration does not clear staged changes: if any unexpected staged mutation exists, stop and inspect it rather than treating the worktree check as sufficient. If unrelated changes share the file, reverse only the experiment-owned lines manually, review the precise diff, and verify against the saved patch; do not restore the whole file. Never use a broad reset or cleanup to remove unrelated work.

Rerun the selected tests against restored code and preserve the exit code and reports:

```sh
if env JAVA_HOME="$mutation_java_home" ./mvnw -B -ntp -Dtest=OrderTest test > "$mutation_evidence_dir/restored-order.log" 2>&1; then
    mutation_exit=0
else
    mutation_exit=$?
fi
printf '%s\n' "$mutation_exit" > "$mutation_evidence_dir/restored-order.exit"
cp -R target/surefire-reports "$mutation_evidence_dir/restored-order-surefire"
git diff --check
git diff --cached --check
```

Inspect the restored log and require actual selected-test success. Repeat the applicable full `test` and, if selected, Docker-backed `verify` gates from Section C with new `restored-*` log filenames. Do not overwrite baseline evidence or claim unexecuted verification passed.

## G. Investigate Survivors

Preserve the unchanged-suite result first. Determine whether the mutation changes observable approved behavior and whether the selected tests reach it; present the result and evidence for human review. Only for a reviewed genuine detection gap propose a targeted regression test and obtain separate authorization to change tests.

If authorized, retain the original result, reapply only the approved mutant if needed, and record the new regression test failing for the intended reason. Restore original production code again and record the improved suite passing. Label results **original suite** and **improved suite**; never rewrite an original survivor as an original-suite kill.

A genuine production defect follows its own authorized correction workflow. Test-improvement approval does not authorize changing accepted production behavior. Experimental mutations remain temporary and may never be committed as permanent implementation changes or merged into `develop`.

## H. Evidence Template

Copy this template into the agreed investigation record. Replace placeholders with actual evidence after execution; do not invent results. The [playbook](../04-agentic-coding/prompts/00-index.md) requires agreed contents before a new prompt entry is created.

```markdown
# Experiment <ID>

- Date/time/timezone: <actual execution date>
- Unique experiment ID and dedicated evidence directory: <MUT-NNN and full path>
- Authorization and scope: <human approval reference>
- Baseline commit: <full hash>
- Branch: <actual branch>
- Environment: <Java/Maven; Docker/PostgreSQL only if used>
- Requirement: <PRD rule/acceptance criterion>
- Source file and method: <path and method>
- Original expression: <exact source>
- Mutated expression: <exact source>
- Existing unchanged test selection/revision: <classes/methods and commit>
- Passing original-code baseline: <command, exit code, reports>
- Expected behavioral impact/failing test: <hypothesis>
- Original-suite mutant result: <command, exit code, actual assertions or survival>
- Outcome/classification: <killed/survived/uncovered/equivalent candidate/inconclusive>
- Equivalence/gap reasoning or separate original-code defect: <evidence>
- Human-review decision: <actual decision or Pending>
- Separately approved test improvement: <authorization and diff, or none>
- Improved-suite mutant result, if authorized: <separate assertion evidence>
- Restoration evidence: <exact diff checks and saved mutation patch>
- Final restored-code verification: <commands, results, reports, limitations>
- Attempt-specific evidence: <separate log/exit/report names for each authorized run>
- Evidence location/retention: <approved durable destination and transfer confirmation>
- Remaining decisions: <explicit deferrals>
```

## I. Finish the Investigation

After restoration and applicable verification, inspect the complete diff and ensure no experimental production change is staged, unstaged or committed:

```sh
git status --short --untracked-files=all
git diff --exit-code "$mutation_baseline" -- src/main
git diff --cached --exit-code "$mutation_baseline" -- src/main
git log --oneline "$mutation_baseline"..HEAD
git diff --stat "$mutation_baseline"
```

Before returning to `develop`, confirm that all approved experiments are completed, every temporary production-code mutation is removed, and all relevant verification checks completed successfully. Inspect and appropriately preserve untracked files and any approved work; never automatically stash, discard, reset or overwrite changes to satisfy the check.

For the standard investigation without approved permanent changes, the following block enforces the expected experiment branch, unchanged recorded baseline, and a clean index/working tree including untracked files. Run it only after the completion and verification prerequisites above are met. The parentheses create a subshell: `exit 1` aborts this block without terminating the interactive shell, and `git switch` is reached only if every guard succeeds.

```sh
(
    if [ "$(git branch --show-current)" != 'experiment/mutation-testing' ]; then
        printf '%s\n' 'ERROR: Not on the expected experiment branch.' >&2
        exit 1
    fi
    if [ -z "$mutation_baseline" ] || [ "$(git rev-parse HEAD)" != "$mutation_baseline" ]; then
        printf '%s\n' 'ERROR: Baseline changed. Review separately approved permanent changes before proceeding.' >&2
        exit 1
    fi
    mutation_finish_status=$(git status --porcelain=v1 --untracked-files=all) || exit 1
    if [ -n "$mutation_finish_status" ]; then
        printf '%s\n' 'ERROR: Working tree is not clean. Review and preserve changes before switching branches.' >&2
        exit 1
    fi
    git switch develop || exit 1
    git branch --show-current
    git status --short --untracked-files=all
)
```

If separately approved test improvements exist, preserve them on the experiment branch and complete their independently authorized human review/acceptance and commit workflow before returning to `develop`. Legitimate future commits of those improvements are permitted; their approved branch tip intentionally differs from the original baseline, so the standard block will refuse to switch. Use that separately authorized workflow to verify the approved tip and a clean index/working tree, inspect untracked files, and return safely. Verify no intentional faults remain before any proposed merge. Do not discard approved work or bypass a failed guard merely to satisfy baseline equality.

Delete the local branch only after its contents/evidence are no longer needed and deletion is explicitly approved:

```sh
git branch -d experiment/mutation-testing
```

Run this only after safely returning to `develop`. If Git refuses because commits are unmerged, stop and review; do not force deletion.

Do not push experimental mutations, open a pull request containing intentional faults, or intentionally trigger CI/SonarQube for temporary mutant code. The current [GitHub workflow](../../.github/workflows/build.yml) triggers on pushes to `main`/`develop` and pull-request opened/synchronize/reopened events, running `clean verify` and the Sonar Maven scanner. A local commit alone triggers none of those GitHub events; other remote automation and local hooks still require inspection. This document neither runs nor changes CI.
