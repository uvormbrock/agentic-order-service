# Mutation Testing Runbook

**Current default:** automated PIT mutation testing through the root `Makefile`.
**Documentation status:** Updated – Pending Human Review

This reference describes the existing Makefile and POM as inspected on 2026-10-16. It changes no tooling, thresholds, CI or execution authorization. The [strategy](mutation-testing.md) defines interpretation and review; the [verified experiment report](mutation-results/mutation-testing-experiments.md) records the two controlled demonstrations. Ordinary test responsibilities remain in the [testing strategy](testing-strategy.md#running-tests).

## Current Automated Workflow

### Commands and Production Selectors

Set `JAVA_HOME` to an existing Java 21 installation. From the repository root, use:

```sh
make mutation-test SCOPE=order
make mutation-test SCOPE=product
make mutation-test SCOPE=customer
make mutation-test SCOPE=shared
make mutation-test SCOPE=all
```

`make mutation-test` defaults to `SCOPE=all`; bare `make` or `make help` displays help. Example with command-local Java selection:

```sh
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
  make mutation-test SCOPE=shared
```

| SCOPE | PIT production selector (`targetClasses`) |
| --- | --- |
| order | `com.example.agenticorderservice.order.domain.*` |
| product | `com.example.agenticorderservice.product.*` |
| customer | `com.example.agenticorderservice.customer.*` |
| shared | `com.example.agenticorderservice.shared.*` |
| all | `com.example.agenticorderservice.*` |

Scopes are class filters in one Maven module, not separate Maven modules. Order currently targets only its domain package. No speculative controller, service, repository, DTO or configuration exclusions are applied within the selected filters.

### Test Discovery and Persistence Limitation

Every scope uses the same complete project-wide fast-test selection:

- Surefire baseline: `com/example/agenticorderservice/**/*Test.java`.
- PIT eligible tests: `com.example.agenticorderservice.*Test`.

The first is Surefire's path-style selector; the second is PIT's fully qualified class-name filter. They include nested feature packages. Fast tests include Spring/MockMvc tests as well as plain domain/application unit tests; “fast” does not mean every test is a pure unit test.

Cross-feature assertions can live outside the feature being mutated. For example, Product API tests also exercise Customer behavior. Restricting Customer tests to Customer packages previously lost detecting tests. Broad test eligibility avoids hardcoded cross-feature lists while production scope remains focused. PIT uses coverage information to select tests for mutants and may stop when a test detects one; eligibility does not mean every selected class or test runs for every mutant. Inspect verbose `pit.log`, XML `killingTest` and execution counts.

PostgreSQL/Testcontainers `*IT` integration tests are excluded from both selectors. The Makefile runs `test` and PIT's explicit goal, not Failsafe `verify`. It does not require a running Compose database for these selected tests. A dedicated persistence mutation strategy remains future work: uncovered JPA converters or servlet error-dispatch paths must not be treated automatically as missing unit assertions. Ordinary PostgreSQL verification remains a separate `./mvnw verify` responsibility.

### Execution Sequence and Failure Behavior

1. Resolve the repository root from the Makefile location and validate SCOPE. An unknown scope prints an error and exits the recipe with code 2.
2. Require `JAVA_HOME`, invoke its `bin/java`, and verify `java.specification.version = 21`. The inherited `JAVA_HOME` is used by the existing Maven Wrapper; no system-wide configuration changes occur. Make, Python 3 (standard library), Git, `shasum`, `mktemp` and ordinary shell tools must be available.
3. Create/resolve the external evidence root and reject a root within the current project directory. Allocate a unique `PIT-<scope>-<UTC timestamp>-XXXXXX` directory with `mktemp`; never reuse a historical run directory. A final trap prints scope, result and evidence location once this setup succeeds.
4. Snapshot Makefile bytes/checksum, POM checksum, Git revision/branch/status/diffs, Java settings and selectors before execution.
5. Run Surefire with the project-wide baseline selector, `-Djacoco.skip=true`, `-DfailIfNoTests=true` and a unique `-Dsurefire.reportNameSuffix=<run ID>`. Save the command, combined output, Maven exit code and duration. Copy only this run's suffixed XML/text reports, preserving existing results.
6. Parse the copied `TEST-*.xml` using Python. Require at least one testsuite, valid nonnegative counters, skipped not exceeding tests, at least one executed test (`tests - skipped > 0`) and zero failures/errors. Record actual suite names/counts and validation exit code. Skipped tests are reported; the validator does not require zero skips. Missing/malformed XML or invalid counters prevent PIT. A parse error is retained in the validation log and may prevent summary JSON creation.
7. Stop before PIT if Maven baseline or XML validation fails. Otherwise run the explicit PIT goal with the selected production filter, common fast-test filter, `-Dverbose=true`, `-Djacoco.skip=true`, and this run's external `reportsDirectory`.
8. Save PIT command, combined output, Maven exit code and duration. A nonzero PIT exit fails the recipe. On success, require nonempty `pit-reports/mutations.xml` and `pit-reports/index.html`, print available statistics/discovery guidance and report `EXECUTED - PENDING HUMAN REVIEW`.

The recipe preserves underlying Maven/validator exit codes in evidence and exits nonzero on failure; GNU Make's own exit code need not equal Maven's code. The final report-file checks do not parse mutation XML or establish a complete/nonempty mutation population; review the XML separately, alongside PIT's configured empty-run guard. Survivors and timeouts do not automatically fail the run because no mutation-score threshold is configured.

The Makefile records Git state but does not enforce branch `develop`, a clean tree, unchanged HEAD or source restoration. Perform approved pre/post-execution safety checks yourself. Automated PIT does not require manual source edits or worktree creation; use an explicitly approved isolated worktree when the experiment requires temporary source/test changes. There is no worktree creation/removal or Git cleanup in this target.

The current Makefile prints a conservative message that Spring/Mockito compatibility beyond Order is unverified. This is a literal implementation warning, not a claim that no broader historical runs exist. Consult the specific run's discovery/execution evidence; neither this message nor successful execution proves universal compatibility.

### Evidence Layout

Default root: the sibling `../agentic-order-service-mutation-evidence`, resolved relative to the Makefile's project root. Override it without a personal hardcoded path:

```sh
make mutation-test SCOPE=order MUTATION_EVIDENCE_ROOT=/absolute/external/evidence
```

Keep the chosen root outside every relevant checkout. The implemented guard checks the current project directory, not all other registered worktrees.

| Evidence | Files in each new run directory |
| --- | --- |
| Recipe and build identity | Exact `Makefile` copy (also when untracked), `Makefile.sha256`, `pom.sha256` |
| Git state | `revision.txt`, `branch.txt`, `git-status.txt`, `working-tree.diff`, `staged.diff` |
| Environment and selection | `environment.txt`, `selection.txt` |
| Baseline execution | `baseline-command.txt`, `baseline.log`, `baseline-exit-code.txt`, `baseline-duration-seconds.txt` |
| Baseline validation | `surefire-reports/`, `baseline-validation.log`, `baseline-validation-exit-code.txt`, and `baseline-summary.json` when parsing completes |
| PIT execution | `pit-command.txt`, `pit.log`, `pit-exit-code.txt`, `pit-duration-seconds.txt` |
| Mutation reports | `pit-reports/mutations.xml`, `pit-reports/index.html` and generated HTML assets |

Failures can leave partial evidence; an early setup failure can precede directory/trap creation. The target does not copy every untracked source file, the entire POM, dependencies or a repository snapshot. Git diffs exclude untracked file contents; the explicit Makefile copy closes that particular gap only. Review source/test differences and preserve any additional approved reproducibility evidence separately. Logs and Git diffs may contain local information: review them before sharing. External persistence is not backup; independent backup ownership/retention/verification remains separate.

### Actual PIT Maven Configuration

| Setting | Current POM value |
| --- | --- |
| Explicit profile | `mutation-order` (name retained for all scopes) |
| Maven plugin | `org.pitest:pitest-maven:1.30.1` |
| Plugin dependency | `org.pitest:pitest-junit5-plugin:1.2.3` |
| Mutators | `DEFAULTS` |
| Threads | 1 |
| Reports | HTML and XML |
| Profile production/test defaults | Both `com.example.agenticorderservice.*` |
| Selection overrides | `-DtargetClasses=...`, `-DtargetTests=...` via POM properties; Makefile supplies the filters above |
| Surefire import | `parseSurefireConfig=false`, `parseSurefireArgLine=false`; PIT selection is explicit |
| Empty mutation guard | `failWhenNoMutations=true` |
| Failing test handling | `skipFailingTests=false` |
| Score threshold / lifecycle binding | None configured |

Despite its artifact name, the JUnit adapter's current combination with Java 21 and the project's JUnit 6.0.3 has executed successfully in the recorded Order experiments. This is scoped execution evidence, not universal Spring/integration-test support. No JUnit downgrade is configured. Ordinary Surefire, Failsafe, JaCoCo and CI behavior is unchanged; these Makefile commands explicitly skip JaCoCo for mutation runs. No custom PIT timeout policy is selected here.

### Interpretation and Review

- **KILLED:** a test detects the mutant; inspect the detecting test and behavior.
- **SURVIVED:** covered mutant not detected by the selected tests; assess requirements, assertions and equivalence before proposing changes.
- **NO_COVERAGE:** no selected test executes the mutation location; investigate missing execution or scope limitations separately.
- **TIMED_OUT:** mutant execution exceeded PIT's limit. PIT may mark it detected, but this is not an assertion-based kill. Investigate possible nontermination, instrumentation or environmental effects without silently changing timeouts.

Report the exact denominator and all statuses. PIT detection percentage uses mutants marked `detected=true` divided by generated mutants; strict assertion-based percentage is `KILLED / generated`. For the historical Shared report with 205 generated, 149 KILLED and one detected TIMED_OUT, these are `150/205 = 73.17%` and `149/205 = 72.68%`. That arithmetic example is historical, not a new execution or release threshold.

Use this review sequence within explicitly authorized scope:

1. Run the automated baseline/PIT workflow and preserve its initial evidence.
2. Inspect XML and HTML, logs and actual test discovery.
3. Identify meaningful survivors and execution gaps; separate equivalent candidates, framework/persistence limitations and timeouts.
4. Compare stable identities: class, method, JVM descriptor, line, mutator, instruction index and description. Check added/removed identities and scope changes; line/index identity can change with source/compiler changes.
5. Propose minimal improvements grounded in existing requirements, preferably extending existing parameterizations. Do not invent behavior or artificial persistence-ID assertions to increase a score.
6. Obtain human review and explicit approval before changing code/tests.
7. Rerun the approved scope, compare exact outcomes and detecting tests, and report regressions or remaining limitations.
8. Preserve earlier evidence, document new results and wait for human acceptance; commits require separate authorization.

Mutation coverage is diagnostic evidence, not a number to maximize through artificial assertions. The [controlled experiments](mutation-results/mutation-testing-experiments.md) show why green JUnit results alone can conceal a lost boundary safeguard and why a surviving mutant can coexist with Maven success.

## Historical Manual Workflow — Not the Default

Sections A–I below retain the earlier MUT-001 setup/migration instructions, manual experiment procedure and evidence templates for historical context and separately authorized manual investigations. Their old paths, branch observations, proposed statuses and command sequences are not prerequisites for `make mutation-test`. Do not execute them merely to start automated PIT. In particular, old main-worktree migration instructions describe the observed 2026-10-15 state, not the current repository.

## A. Prerequisites and Execution Context

Obtain explicit authorization for the exact experiment, worktree setup or migration, test selection and evidence handling before running any command that changes state. Test improvements, genuine production fixes, report commits, worktree removal and branch deletion need their own authorization. This documentation task runs none of them.

The registered Obsidian vault is `docs/` in the main repository directory, with configuration in `docs/.obsidian/`; the repository-root `.obsidian/` is only a placeholder. Worktrees share Git history and refs but have separate checked-out files, indexes and build outputs. Do not open the experiment worktree as a second authoritative vault.

```text
Development/Projekte/
├── agentic-order-service/                 # Main worktree: develop
│   ├── src/
│   └── docs/05-testing/
│       ├── mutation-testing.md
│       ├── mutation-testing-runbook.md
│       └── mutation-results/
│           ├── mutation-testing-experiments.md
│           └── MUT-NNN.md                 # Only after actual outcomes exist
├── agentic-order-service-mutation/        # Experiment worktree
│   └── src/
└── agentic-order-service-mutation-evidence/ # Ordinary directory, not Git
    └── MUT-NNN/                           # Raw evidence for one experiment
```

The main worktree owns accepted code/tests, documentation and reports on `develop`. Temporary production mutations and their test runs belong only in the experiment worktree. Raw evidence belongs outside **both** worktrees; do not create `.mutation-artifacts/` in either. No `.gitignore` change is needed for this sibling directory. Persistence alone is not backup: owner, independent backup destination, retention and verification remain explicit human-approval requirements.

**Observed state on 2026-10-15:** only the main directory is registered as a worktree, currently on `experiment/mutation-testing` at `5a2e1052f618360b733b0671e1d8b3b3bd57a031`; `develop` points to that commit too. The preceding storage revision is uncommitted: runbook/strategy edits and a new results index are present. The target experiment worktree and external evidence root do not exist. These are observations, not a completed migration.

**Context: main directory, read-only inspection.** Start with the verified workstation path below; on another machine substitute and verify its actual main repository path before deriving siblings. Keep these shell variables in the same shell for later blocks. All Git commands name their worktree explicitly; Maven blocks enter the experiment worktree in a subshell. Keep the variables/approved file array in the same shell session. Run guarded subshell blocks as standalone commands, never as an `if` condition or in an `&&`/`||` chain that can suppress shell errexit. `exit 1` inside them stops the block, not the interactive shell. Diagnostic commands are labeled; state changes require their stated prerequisites and separate authorization.

```sh
mutation_main=/Users/rookie/Documents/Development/Projekte/agentic-order-service
mutation_main=$(git -C "$mutation_main" rev-parse --show-toplevel)
mutation_parent=$(dirname "$mutation_main")
mutation_worktree="$mutation_parent/agentic-order-service-mutation"
mutation_evidence_root="$mutation_parent/agentic-order-service-mutation-evidence"
mutation_java_home=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
git -C "$mutation_main" status --short --untracked-files=all
git -C "$mutation_main" branch --show-current
git -C "$mutation_main" rev-parse HEAD
git -C "$mutation_main" branch -vv
git -C "$mutation_main" worktree list --porcelain
git -C "$mutation_main" config --get core.hooksPath
git -C "$mutation_main" rev-parse --git-path hooks
```

An unset `core.hooksPath` normally produces no value/nonzero exit; also inspect the reported default hook directory. Confirm Java 21 and Maven Wrapper 3.9.9 in the experiment environment during Section C. Docker is required only for selected PostgreSQL/Testcontainers tests, which use disposable databases, never local Compose credentials or persistent development data.

Inspect hooks and external automation before any authorized commit. A local commit does not inherently trigger GitHub Actions/SonarQube; do not push intentional faults or invoke the Sonar scanner for temporary mutants. No existing quality gate is relaxed.

## B. Git Worktree Setup

### One-Time Documentation Handoff Before Worktree Migration

**Preferred route:** make an explicitly authorized documentation-only checkpoint on the existing experiment branch, export that **single verified commit** as a patch outside the worktrees, then separately authorize/check/apply/commit it on `develop`. No whole-branch merge, arbitrary cherry-pick or history rewrite is involved. Neither checkpoint nor transfer is authorized by this runbook.

#### Inspect and Agree the Exact Scope

**Context: original main directory, read-only inspection; retain variables from A.** Inspect the whole tree/index first, then the proposed four-file scope. A Git diff omits untracked content, so read the replacement experiment report in full. If actual state differs, agree the scope again before staging anything.

```sh
mutation_doc_files=(
    docs/05-testing/mutation-testing-runbook.md
    docs/05-testing/mutation-testing.md
    docs/05-testing/testing-strategy.md
    docs/05-testing/mutation-results/mutation-testing-experiments.md
)
git -C "$mutation_main" status --short --untracked-files=all
git -C "$mutation_main" branch --show-current
git -C "$mutation_main" rev-parse HEAD
git -C "$mutation_main" branch -vv
git -C "$mutation_main" worktree list --porcelain
git -C "$mutation_main" diff
git -C "$mutation_main" diff --cached
git -C "$mutation_main" ls-files --others --exclude-standard
cat "$mutation_main/docs/05-testing/mutation-results/mutation-testing-experiments.md"
```

Verify all changed/new file contents; production/tests/POM/configuration/CI/ADRs and any other unrelated files must not enter the handoff. The earlier index was new at the historical handoff; it has since been replaced by the verified experiment report. Inspect the actual untracked list rather than relying on that historical observation. Unexpected changes or existing unrelated staging are a **stop/review point**: preserve them with their owner through a separately approved workflow, never automatically stash/discard them.

| Actual state after inspection | Safe choice |
| --- | --- |
| Approved docs are uncommitted on the experiment checkout | Obtain acceptance plus explicit checkpoint-commit authorization; use the checkpoint steps below. |
| A documentation-only commit already exists there | Skip staging/creating another commit; select and verify its full hash below. Process multiple accepted commits individually in reviewed order. |
| The intended changes already exist on develop | Compare actual content/history and record that no transfer is needed; do not force an empty commit or reapply older text over newer work. |
| Mixed source/docs commits, conflicts, partial overlap or unrelated work | Stop and propose a separately reviewed file-scoped patch/content resolution; do not cherry-pick the mixed commit or guess what to overwrite. |

#### Optional Checkpoint of Uncommitted Documentation

**Context: original main directory on `experiment/mutation-testing`. State-changing staging requires explicit authorization.** Prerequisites: human-reviewed exact contents/scope, no unexpected work, and an empty index. This Python standard-library preflight rejects any changed/untracked path outside the four-file scope; it changes no files. The guard and staging are one subshell so failure prevents `git add`.

```sh
(
    set -e
    [ -n "$mutation_main" ] || exit 1
    [ "$(git -C "$mutation_main" branch --show-current)" = experiment/mutation-testing ] || exit 1
    git -C "$mutation_main" diff --cached --quiet || exit 1
    python3 - "$mutation_main" "${mutation_doc_files[@]}" <<'PYSCOPE' || exit 1
import subprocess
import sys
root, *allowed = sys.argv[1:]
paths = set()
for args in (['diff', '--no-renames', '--name-only', '-z'], ['ls-files', '--others', '--exclude-standard', '-z']):
    output = subprocess.check_output(['git', '-C', root, *args])
    paths.update(p.decode() for p in output.split(b'\0') if p)
if not paths or paths - set(allowed):
    raise SystemExit('ERROR: Empty or unexpected changed-file scope; preserve work and review.')
PYSCOPE
    git -C "$mutation_main" add -- "${mutation_doc_files[@]}" || exit 1
    git -C "$mutation_main" diff --cached --check || exit 1
    git -C "$mutation_main" diff --cached --stat
    git -C "$mutation_main" diff --cached
)
```

Review the **actual staged diff** and obtain separate explicit authorization for the checkpoint commit. If anything unexpected appeared, stop; do not automatically unstage/restore files. Only with that authorization, an unchanged reviewed index and no unrelated work:

```sh
(
    set -e
    [ "$(git -C "$mutation_main" branch --show-current)" = experiment/mutation-testing ] || exit 1
    git -C "$mutation_main" diff --quiet || exit 1
    git -C "$mutation_main" diff --cached --check || exit 1
    python3 - "$mutation_main" "${mutation_doc_files[@]}" <<'PYINDEX' || exit 1
import subprocess
import sys
root, *allowed = sys.argv[1:]
output = subprocess.check_output(['git', '-C', root, 'diff', '--cached', '--no-renames', '--name-only', '-z'])
paths = {p.decode() for p in output.split(b'\0') if p}
if not paths or paths - set(allowed):
    raise SystemExit('ERROR: Staged scope changed; obtain review before committing.')
PYINDEX
    git -C "$mutation_main" commit -m "docs: define mutation worktree and evidence workflow" || exit 1
)
```

A checkpoint on the experiment branch does **not** appear automatically on `develop`. It changes the experiment tip, not MUT-001's original baseline receipt. Inspect the resulting commit even if hooks run; never proceed on an unexpected commit/scope.

#### Verify One Documentation Commit and Export Its Patch

**Context: shared repository history inspected from the main directory.** Enter the human-selected full documentation commit (new checkpoint or an already existing one), inspect its complete patch, and verify that it has exactly one parent and changes only the approved files. Mixed/merge/empty commits require a different reviewed plan.

```sh
printf '%s\n' 'Enter the full human-selected documentation-only commit:'
read -r mutation_doc_commit
git -C "$mutation_main" show --format=fuller --stat "$mutation_doc_commit"
git -C "$mutation_main" show --format=fuller --binary "$mutation_doc_commit"
```

**Context: external handoff artifacts; export only after human acceptance of that commit and explicit export authorization.** This guard checks the entire commit, not a filtered view hiding other files. The unique handoff directory preserves the selected hash/patch without overwriting an earlier attempt; reruns require inspection and a separately approved new location.

```sh
mutation_handoff_dir="$mutation_evidence_root/documentation-handoff/$mutation_doc_commit"
mutation_handoff_patch="$mutation_handoff_dir/documentation.patch"
(
    set -e
    set -C
    [ -n "$mutation_main" ] && [ -n "$mutation_doc_commit" ] || exit 1
    python3 - "$mutation_main" "$mutation_doc_commit" "${mutation_doc_files[@]}" <<'PYCOMMIT' || exit 1
import subprocess
import sys
root, commit, *allowed = sys.argv[1:]
run = lambda *args: subprocess.check_output(['git', '-C', root, *args])
resolved = run('rev-parse', '--verify', commit + '^{commit}').decode().strip()
parents = run('rev-list', '--parents', '-n', '1', resolved).split()
paths = {p.decode() for p in run('diff-tree', '--no-renames', '--no-commit-id', '--name-only', '-r', '-z', resolved).split(b'\0') if p}
if resolved != commit or len(parents) != 2 or not paths or paths - set(allowed):
    raise SystemExit('ERROR: Expected a full single-parent documentation-only commit; review scope.')
PYCOMMIT
    [ ! -L "$mutation_evidence_root" ] || exit 1
    [ ! -L "$mutation_evidence_root/documentation-handoff" ] || exit 1
    if [ -e "$mutation_handoff_dir" ] || [ -L "$mutation_handoff_dir" ]; then
        printf '%s\n' 'ERROR: Handoff destination exists; do not overwrite it.' >&2
        exit 1
    fi
    mkdir -p "$mutation_evidence_root/documentation-handoff" || exit 1
    if git -C "$mutation_evidence_root" rev-parse --git-dir >/dev/null 2>&1; then
        printf '%s\n' 'ERROR: Evidence root must be outside Git repositories/worktrees.' >&2
        exit 1
    fi
    mkdir "$mutation_handoff_dir" || exit 1
    git -C "$mutation_main" diff --binary "$mutation_doc_commit^" "$mutation_doc_commit" > "$mutation_handoff_patch" || exit 1
    printf '%s\n' "$mutation_doc_commit" > "$mutation_handoff_dir/source-commit.txt"
)
```

Inspect and retain the exported patch under the same evidence/backup policy as other raw artifacts. Do not overwrite an existing patch or use export success as authorization to apply it.

#### Transfer to Develop Without Merging the Experiment Branch

**Context: main directory, future separately authorized transfer.** First require a completely clean source checkout, including untracked files, before switching. If already on develop, no switch is needed. No unknown branch is automatically switched.

```sh
(
    set -e
    mutation_handoff_status=$(git -C "$mutation_main" status --porcelain=v1 --untracked-files=all) || exit 1
    [ -z "$mutation_handoff_status" ] || exit 1
    mutation_handoff_branch=$(git -C "$mutation_main" branch --show-current) || exit 1
    case "$mutation_handoff_branch" in
        experiment/mutation-testing) git -C "$mutation_main" switch develop || exit 1 ;;
        develop) ;;
        *) printf '%s\n' 'ERROR: Unexpected checkout branch; stop for review.' >&2; exit 1 ;;
    esac
    git -C "$mutation_main" rev-parse HEAD
    git -C "$mutation_main" diff --name-status HEAD "$mutation_doc_commit" -- "${mutation_doc_files[@]}"
    git -C "$mutation_main" diff HEAD "$mutation_doc_commit" -- "${mutation_doc_files[@]}"
)
```

The comparisons are **diagnostic**: a newer develop tip or different documentation is not automatically wrong. Review whether the changes are needed, already present, or overlap newer edits. If already present, record the relevant source/develop commits and skip application/duplicate commit. If partially present or conflicting, stop for a human-reviewed content-resolution plan; do not overwrite whole files from the old commit. Never substitute the new develop tip for the historical experiment baseline.

Only after explicit approval of the actual develop target and required patch, apply it atomically to a clean index/worktree. Recheck that the saved patch is byte-for-byte the exported change from the previously scope-verified commit; a changed patch must not be applied. `--check` must pass first; a failed prerequisite exits the subshell before any application:

```sh
(
    set -e
    [ "$(git -C "$mutation_main" branch --show-current)" = develop ] || exit 1
    mutation_handoff_status=$(git -C "$mutation_main" status --porcelain=v1 --untracked-files=all) || exit 1
    [ -z "$mutation_handoff_status" ] || exit 1
    [ -f "$mutation_handoff_patch" ] && [ ! -L "$mutation_handoff_patch" ] || exit 1
    python3 - "$mutation_main" "$mutation_doc_commit" "$mutation_handoff_patch" <<'PYPATCH' || exit 1
from pathlib import Path
import subprocess
import sys
root, commit, patch = sys.argv[1:]
expected = subprocess.check_output(['git', '-C', root, 'diff', '--binary', commit + '^', commit])
if Path(patch).read_bytes() != expected:
    raise SystemExit('ERROR: Exported patch differs from the reviewed commit; stop.')
PYPATCH
    git -C "$mutation_main" apply --check --index "$mutation_handoff_patch" || exit 1
    git -C "$mutation_main" apply --index "$mutation_handoff_patch" || exit 1
    git -C "$mutation_main" diff --cached --check || exit 1
    git -C "$mutation_main" diff --cached --stat
    git -C "$mutation_main" diff --cached
)
```

Review the staged destination diff, including the newly added index, its Markdown links and exact approved scope. Any application failure is a stop/review point; do not retry with force or discard unrelated work. Obtain human acceptance plus explicit destination-commit authorization for the unchanged reviewed index before the next block:

```sh
(
    set -e
    [ "$(git -C "$mutation_main" branch --show-current)" = develop ] || exit 1
    git -C "$mutation_main" diff --quiet || exit 1
    git -C "$mutation_main" diff --cached --check || exit 1
    python3 - "$mutation_main" "${mutation_doc_files[@]}" <<'PYINDEX' || exit 1
import subprocess
import sys
root, *allowed = sys.argv[1:]
output = subprocess.check_output(['git', '-C', root, 'diff', '--cached', '--no-renames', '--name-only', '-z'])
paths = {p.decode() for p in output.split(b'\0') if p}
if not paths or paths - set(allowed):
    raise SystemExit('ERROR: Staged scope changed; obtain review before committing.')
PYINDEX
    git -C "$mutation_main" commit -m "docs: publish mutation worktree and evidence runbook" || exit 1
    git -C "$mutation_main" show --stat --oneline HEAD
    mutation_handoff_status=$(git -C "$mutation_main" status --porcelain=v1 --untracked-files=all) || exit 1
    [ -z "$mutation_handoff_status" ] || exit 1
    git -C "$mutation_main" branch -vv
    git -C "$mutation_main" worktree list --porcelain
)
```

Revalidate complete documentation/link consistency on develop and record both documentation commit hashes and the preserved patch. For the exact current no-conflict transfer, the approved four-file content should match the checkpoint; if newer develop text was retained through a separately reviewed resolution, verify that approved result instead. Reconfirm clean status and actual branch tips before the existing-branch worktree migration below. No step edits MUT-001's `baseline-commit.txt`, and documentation commits do not invalidate that historical receipt. No handoff or commit is executed by this documentation task.

### One-Time Migration for Existing MUT-001

**Context: main directory.** Complete the [documentation handoff](#one-time-documentation-handoff-before-worktree-migration) first if changes are still pending. Main will normally already be on develop afterwards. Inspect actual status/worktree assignments and tips again; do not assume the historical branch positions still apply.

The historical MUT-001 baseline is `5a2e1052f618360b733b0671e1d8b3b3bd57a031`, preserved in its existing `baseline-commit.txt`. It is evidence of that experiment, not a permanent expected branch tip. Read the recorded baseline and dynamically inspect the actual tips and uncommitted changes before deciding how to migrate.

**Context: main directory, read-only inspection before human confirmation.** These assignments capture observations, not approval or a new baseline:

```sh
mutation_branch=experiment/mutation-testing
mutation_legacy_dir=/tmp/agentic-mutation.edjGwr/MUT-001
mutation_historical_baseline=$(cat "$mutation_legacy_dir/baseline-commit.txt")
mutation_observed_experiment_tip=$(git -C "$mutation_main" rev-parse --verify "$mutation_branch^{commit}")
mutation_observed_develop_tip=$(git -C "$mutation_main" rev-parse --verify 'develop^{commit}')
printf 'Historical MUT-001 baseline: %s\nExperiment tip: %s\nDevelop tip: %s\n' "$mutation_historical_baseline" "$mutation_observed_experiment_tip" "$mutation_observed_develop_tip"
git -C "$mutation_main" status --short --untracked-files=all
git -C "$mutation_main" worktree list --porcelain
git -C "$mutation_main" log --left-right --oneline "$mutation_historical_baseline...$mutation_observed_experiment_tip"
git -C "$mutation_main" diff --name-status "$mutation_historical_baseline" "$mutation_observed_experiment_tip"
git -C "$mutation_main" log --stat "$mutation_historical_baseline..$mutation_observed_experiment_tip"
git -C "$mutation_main" diff "$mutation_historical_baseline" "$mutation_observed_experiment_tip" -- src pom.xml .github .gitignore
git -C "$mutation_main" log --left-right --oneline "$mutation_observed_experiment_tip...$mutation_observed_develop_tip"
git -C "$mutation_main" diff --name-status "$mutation_observed_experiment_tip" "$mutation_observed_develop_tip"
```

Explain the inspected changes to the human. Distinguish documentation-only commits from production, test, configuration or mixed changes; a name-only diff or empty net diff does not replace commit review. An advanced branch is not automatically invalid. Obtain confirmation of the actual experiment tip and whether to continue the existing experiment on its accepted source/test baseline, preserve separately authorized documentation evolution, or start a new experiment at a newly accepted commit. If source/tests have changed, review how to reproduce the original baseline or authorize a new experiment; do not silently label newer code as the historical baseline. Do not reset, rebase, cherry-pick, merge or otherwise rewrite history automatically.

**Context: main directory, future explicitly authorized migration only.** Run after human confirmation and after all existing work is preserved and the index/worktree are clean. Enter the exact experiment tip just reviewed. This confirmation freezes the approved migration snapshot; if that branch changes again before creation, stop and repeat inspection/review rather than declaring all advances invalid:

```sh
printf '%s\n' 'Enter the human-confirmed current experiment tip from the inspection above:'
read -r mutation_confirmed_experiment_tip
(
    set -e
    [ -n "$mutation_historical_baseline" ]
    [ "$mutation_confirmed_experiment_tip" = "$mutation_observed_experiment_tip" ]
    mutation_pending=$(git -C "$mutation_main" status --porcelain=v1 --untracked-files=all)
    if [ -n "$mutation_pending" ]; then
        printf '%s\n' 'ERROR: Preserve and review uncommitted work before switching.' >&2
        exit 1
    fi
    mutation_main_branch=$(git -C "$mutation_main" branch --show-current) || exit 1
    [ "$(git -C "$mutation_main" rev-parse "$mutation_branch")" = "$mutation_confirmed_experiment_tip" ] || exit 1
    case "$mutation_main_branch" in
        "$mutation_branch") git -C "$mutation_main" switch develop || exit 1 ;;
        develop) ;;
        *) printf '%s\n' 'ERROR: Unexpected main checkout branch.' >&2; exit 1 ;;
    esac
    [ "$(git -C "$mutation_main" branch --show-current)" = develop ]
    git -C "$mutation_main" rev-parse HEAD
    if [ -e "$mutation_worktree" ] || [ -L "$mutation_worktree" ]; then
        printf '%s\n' 'ERROR: Proposed experiment-worktree path already exists.' >&2
        exit 1
    fi
    git -C "$mutation_main" worktree list --porcelain
    git -C "$mutation_main" worktree add "$mutation_worktree" "$mutation_branch"
    [ "$(git -C "$mutation_worktree" branch --show-current)" = "$mutation_branch" ]
    [ "$(git -C "$mutation_worktree" rev-parse HEAD)" = "$mutation_confirmed_experiment_tip" ]
    mutation_checked_status=$(git -C "$mutation_worktree" status --porcelain=v1 --untracked-files=all) || exit 1
    [ -z "$mutation_checked_status" ] || exit 1
    mutation_checked_status=$(git -C "$mutation_main" status --porcelain=v1 --untracked-files=all) || exit 1
    [ -z "$mutation_checked_status" ] || exit 1
    git -C "$mutation_main" worktree list --porcelain
)
```

A newer `develop` tip does not fail this migration merely for differing from MUT-001. Inspect any newly observed difference and obtain confirmation before experiment continuation; never merge/rebase it into the ongoing experiment automatically. For a confirmed continuation of MUT-001, preserve its baseline from the receipt and record the approved pre-mutation/test revision separately:

```sh
mutation_baseline="$mutation_historical_baseline"
mutation_restoration_revision="$mutation_confirmed_experiment_tip"
mutation_test_revision="$mutation_confirmed_experiment_tip"
```

These variables describe the human-confirmed continuation only; do not execute this initialization if the decision was to start a new experiment. The restoration revision is the approved pre-mutation checkpoint, not a refreshed develop tip. Review and record any source/test differences from the historical baseline before testing; do not claim a historical-baseline reproduction when the tested code differs.

This uses the **existing** branch: from the main directory its equivalent command is `git worktree add ../agentic-order-service-mutation experiment/mutation-testing`, without `-b`. Git normally refuses to check out a branch already checked out in another worktree; do not bypass that check. Stop on any failed assumption or command. If switching succeeds but worktree creation fails, inspect the actual state and preserve it; do not automatically switch back or delete paths. Review any main/experiment commit differences before execution, including separately authorized documentation commits; source/test/config differences need their own approval, not an automatic rejection or history rewrite.

### Standard Future Setup

The main worktree must already be clean on `develop`. Explicitly select the accepted full commit and approve a unique arrangement. The example below creates a **new** MUT-002 worktree/branch, not the already existing MUT-001 branch; replace the illustrative ID only with the next approved unused ID.

**Context: main directory; future worktree creation only after authorization.** Enter the accepted commit when prompted, not an automatically selected latest revision:

```sh
mutation_experiment_id=MUT-002
mutation_branch="experiment/mutation-$mutation_experiment_id"
mutation_worktree="$mutation_parent/agentic-order-service-mutation-$mutation_experiment_id"
printf '%s\n' 'Enter the explicitly accepted full baseline commit:'
read -r mutation_approved_start_commit
(
    set -e
    [ "$(git -C "$mutation_main" branch --show-current)" = develop ]
    mutation_checked_status=$(git -C "$mutation_main" status --porcelain=v1 --untracked-files=all) || exit 1
    [ -z "$mutation_checked_status" ] || exit 1
    [ "$(git -C "$mutation_main" rev-parse --verify "$mutation_approved_start_commit^{commit}")" = "$mutation_approved_start_commit" ]
    git -C "$mutation_main" check-ref-format --branch "$mutation_branch"
    if git -C "$mutation_main" show-ref --verify --quiet "refs/heads/$mutation_branch"; then
        printf '%s\n' 'ERROR: Branch exists; do not recreate or overwrite it.' >&2
        exit 1
    fi
    if [ -e "$mutation_worktree" ] || [ -L "$mutation_worktree" ]; then
        printf '%s\n' 'ERROR: Worktree destination exists; stop for review.' >&2
        exit 1
    fi
    git -C "$mutation_main" worktree list --porcelain
    git -C "$mutation_main" worktree add -b "$mutation_branch" "$mutation_worktree" "$mutation_approved_start_commit"
    [ "$(git -C "$mutation_worktree" rev-parse HEAD)" = "$mutation_approved_start_commit" ]
    [ "$(git -C "$mutation_worktree" branch --show-current)" = "$mutation_branch" ]
    mutation_checked_status=$(git -C "$mutation_worktree" status --porcelain=v1 --untracked-files=all) || exit 1
    [ -z "$mutation_checked_status" ] || exit 1
    [ "$(git -C "$mutation_main" branch --show-current)" = develop ]
)
```

After successful setup and confirmation of the explicitly approved commit, **capture a new experiment's baseline once** from the actual experiment worktree. Do not run this block to refresh an ongoing experiment:

```sh
mutation_candidate_baseline=$(git -C "$mutation_worktree" rev-parse HEAD)
if [ -n "$mutation_approved_start_commit" ] && [ "$mutation_candidate_baseline" = "$mutation_approved_start_commit" ]; then
    mutation_baseline="$mutation_candidate_baseline"
    mutation_restoration_revision="$mutation_baseline"
    mutation_test_revision="$mutation_baseline"
else
    printf '%s\n' 'STOP: Actual tip differs from the approved start commit; inspect and obtain human confirmation.' >&2
fi
```

Do not proceed after the STOP condition, even if older shell variables remain defined. An advanced tip is a review event, not permission to refresh an ongoing experiment or rewrite history.

Sequential approved experiments may share the dedicated worktree/branch. Verify previous restoration, a clean tree and reviewed history, inspect differences from current develop, and obtain approval for the actual commit. Set `mutation_approved_start_commit` to that confirmed hash before one-time capture under a **new** evidence ID/directory. Capturing HEAD is not acceptance; do not reset/switch an ongoing experiment to another baseline or recreate its branch. If source/tests differ from the newly approved baseline, stop and agree another arrangement.

No remote synchronization is assumed. If needed, separately authorize fetch, inspect divergence and accepted commits, and agree any fast-forward of main `develop` before setup. Never force synchronization or silently accept unreviewed remote work.

## C. Establish the Green Baseline

### Baseline and Revision Meanings

| Value | Meaning and lifetime |
| --- | --- |
| Historical MUT-001 baseline | Original receipt/hash for the initialized experiment; immutable historical evidence, not the expected current tip of either branch. |
| `mutation_baseline` | Explicitly approved experiment baseline, captured/recorded once per experiment. MUT-001 continuation retains its original receipt; each new experiment independently captures its own approved commit. |
| `mutation_restoration_revision` / `mutation_test_revision` | Human-approved pre-mutation checkpoint and unchanged-test revision for this run; recorded separately where authorized branch evolution differs from historical evidence. |
| Current experiment branch tip | Dynamically inspected observation; may advance through separately approved permanent changes. Record/review differences without silently refreshing baseline variables. |
| Current develop tip | Independent dynamically inspected observation; legitimate main development does not change an ongoing experiment's baseline or require the branches to stay equal. |

Freeze baseline/test/restoration values in the experiment record. In a new shell, reload their recorded receipts rather than deriving them again from current HEAD. New develop commits require no refresh. If they motivate testing newer code, inspect differences and obtain a human decision to continue the existing scope or start a new ID at a separately approved baseline; never automatically merge/rebase develop into an ongoing experiment.

### Persistent Reports and Technical Evidence

| Layer | Verified location | Ownership |
| --- | --- | --- |
| Readable reports | Main worktree `docs/05-testing/mutation-results/MUT-NNN.md` | Main `develop` Obsidian vault; create only after actual outcomes, review and separately authorize report commits. |
| Raw artifacts | Sibling `agentic-order-service-mutation-evidence/MUT-NNN/` | Ordinary persistent filesystem directory outside both worktrees; not a Git repository/worktree and not covered by Git backup. |

The [verified experiment report](mutation-results/mutation-testing-experiments.md) replaces the obsolete results index and preserves its navigation and evidence-retention guidance. Raw evidence uses absolute verified paths and a backup locator in those reports, not broken GitHub links or assumptions that Obsidian opens files outside its vault. No `.mutation-artifacts/` folder or `.gitignore` rule is part of this design.

Assign a unique approved experiment ID. The fresh-setup example below retains the unused ID assigned in future setup B (illustrative MUT-002); **existing MUT-001 uses the copy-migration subsection instead**. Keep shell variables from A/B; `mutation_baseline` is the frozen accepted baseline, `mutation_test_revision` identifies the unchanged tests, and `mutation_restoration_revision` identifies the approved pre-mutation checkpoint. `experiment-revision.txt` and `main-revision.txt` record the actual observed tips at setup, independently of those frozen values. Later accepted commits do not replace an existing experiment's receipts.

**Context: inspecting both worktrees and creating external artifacts only; environment command runs in the experiment worktree.** The main path was verified in A; the evidence root is derived from its parent, not from the caller's current directory. Approve the storage setup and backup owner/destination/retention before running:

```sh
# Retain the approved unused ID from B; for sequential reuse, assign a new approved ID first.
mutation_report_relative="docs/05-testing/mutation-results/$mutation_experiment_id.md"
mutation_report="$mutation_main/$mutation_report_relative"
mutation_evidence_root="$mutation_parent/agentic-order-service-mutation-evidence"
mutation_evidence_dir="$mutation_evidence_root/$mutation_experiment_id"
(
    set -e
    set -C
    [ -n "$mutation_experiment_id" ]
    [ "$(git -C "$mutation_main" branch --show-current)" = develop ]
    [ "$(git -C "$mutation_worktree" branch --show-current)" = "$mutation_branch" ]
    mutation_main_status=$(git -C "$mutation_main" status --porcelain=v1 --untracked-files=all)
    mutation_initial_status=$(git -C "$mutation_worktree" status --porcelain=v1 --untracked-files=all)
    if [ -n "$mutation_main_status" ] || [ -n "$mutation_initial_status" ]; then
        printf '%s\n' 'ERROR: Both worktrees must have reviewed clean baselines.' >&2
        exit 1
    fi
    [ -n "$mutation_baseline" ]
    [ -n "$mutation_restoration_revision" ]
    [ -n "$mutation_test_revision" ]
    git -C "$mutation_worktree" diff --exit-code "$mutation_restoration_revision" -- src pom.xml .github .gitignore
    git -C "$mutation_worktree" diff --exit-code "$mutation_test_revision" -- src/test
    case "$mutation_evidence_root/" in
        "$mutation_main/"*|"$mutation_worktree/"*) printf '%s\n' 'ERROR: Evidence must be outside both worktrees.' >&2; exit 1 ;;
    esac
    if [ -L "$mutation_evidence_root" ] || [ -e "$mutation_evidence_dir" ] || [ -L "$mutation_evidence_dir" ] || [ -e "$mutation_report" ] || [ -L "$mutation_report" ]; then
        printf '%s\n' 'ERROR: Refusing symlink root or existing evidence/report destination.' >&2
        exit 1
    fi
    mkdir -p "$mutation_evidence_root"
    if git -C "$mutation_evidence_root" rev-parse --git-dir >/dev/null 2>&1; then
        printf '%s\n' 'ERROR: Evidence root must not be a Git repository or worktree.' >&2
        exit 1
    fi
    mkdir "$mutation_evidence_dir"
    printf '%s\n' "$mutation_baseline" > "$mutation_evidence_dir/baseline-commit.txt"
    printf '%s\n' "$mutation_restoration_revision" > "$mutation_evidence_dir/restoration-revision.txt"
    printf '%s\n' "$mutation_test_revision" > "$mutation_evidence_dir/test-revision.txt"
    git -C "$mutation_worktree" rev-parse HEAD > "$mutation_evidence_dir/experiment-revision.txt"
    git -C "$mutation_worktree" branch --show-current > "$mutation_evidence_dir/branch.txt"
    git -C "$mutation_main" rev-parse HEAD > "$mutation_evidence_dir/main-revision.txt"
    printf '%s' "$mutation_initial_status" > "$mutation_evidence_dir/baseline-status.txt"
    printf '%s' "$mutation_main_status" > "$mutation_evidence_dir/main-status.txt"
    printf '%s\n' "$mutation_report" > "$mutation_evidence_dir/report-location.txt"
    cd "$mutation_worktree"
    env JAVA_HOME="$mutation_java_home" ./mvnw -v > "$mutation_evidence_dir/environment.txt" 2>&1
    printf 'Artifacts: %s\nFuture report: %s\n' "$mutation_evidence_dir" "$mutation_report"
)
```

Stop on failure; defined variables are not execution approval. `mkdir` exclusively creates each experiment directory; `set -C` prevents shell redirection from clobbering existing files in this block. Inspect and preserve partial setup rather than rerunning into it. The external root may host several unique IDs, but never reuse an experiment directory or ID. Use fresh attempt-specific log/exit/report names for authorized retries and keep original-suite/improved-suite evidence separate.

Illustrative layout after future actual execution, not existing results:

```text
agentic-order-service/docs/05-testing/mutation-results/MUT-NNN.md
agentic-order-service-mutation-evidence/MUT-NNN/
├── baseline-commit.txt
├── restoration-revision.txt
├── test-revision.txt
├── experiment-revision.txt
├── branch.txt
├── main-revision.txt
├── baseline-status.txt
├── environment.txt
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
└── restored-production-index.diff
```

A sibling local directory is not an archive or verified backup. Agree a backup owner, independent durable destination and retention policy. Copy the entire raw experiment directory there, verify its inventory and SHA-256 file contents against the original, and record destination/date/verification/retention in the report. Preserve reviewed assertion excerpts in the versioned report. Mark archival Pending until verified; successful migration to the sibling directory is not backup verification. No backup service/destination is selected here.

### Existing MUT-001 — Copy Migration, Pending Human Confirmation

The original setup is `/tmp/agentic-mutation.edjGwr/MUT-001`; read-only inspection on 2026-10-15 found `baseline-commit.txt`, `baseline-status.txt`, `branch.txt`, `environment.txt` and `experiment.md`. Baseline/branch match `5a2e1052f618360b733b0671e1d8b3b3bd57a031` / `experiment/mutation-testing`. The human reports Java 21/Maven 3.9.9 checked, no mutation performed and no confirmed test result. `experiment.md` is historical setup evidence, not a completed vault report. No result report is created now.

Complete the separately authorized one-time worktree migration first. Preserve the original baseline independently of any later documentation commits; record new worktree location/revision and the human-approved `restoration-revision.txt` / `test-revision.txt` receipts in new metadata without overwriting copied files. Do not rerun fresh setup for MUT-001.

**Context: external source/destination inspection, from any directory after A.** These commands inspect only; they do not activate a new artifact path:

```sh
mutation_experiment_id=MUT-001
mutation_legacy_dir=/tmp/agentic-mutation.edjGwr/MUT-001
mutation_destination="$mutation_evidence_root/$mutation_experiment_id"
mutation_report_relative="docs/05-testing/mutation-results/$mutation_experiment_id.md"
mutation_report="$mutation_main/$mutation_report_relative"
# Reload only the immutable original receipt; do not substitute a current branch tip.
mutation_baseline=$(cat "$mutation_legacy_dir/baseline-commit.txt")
find "$mutation_legacy_dir" -print
ls -ld "$mutation_legacy_dir"
if [ -e "$mutation_destination" ] || [ -L "$mutation_destination" ]; then
    printf '%s\n' 'STOP: Destination exists; never overwrite or merge evidence.'
fi
```

Review types/permissions/timestamps and confirm both absolute paths without exposing secrets. **Context: external evidence directories only, separately authorized copy.** The block refuses a symlink source/root or an existing destination, preserves metadata as supported by platform `cp`, and never moves/deletes the original:

```sh
(
    set -e
    if [ ! -d "$mutation_legacy_dir" ] || [ -L "$mutation_legacy_dir" ] || [ -L "$mutation_evidence_root" ]; then
        printf '%s\n' 'ERROR: Source/root must be the inspected real directories.' >&2
        exit 1
    fi
    if [ -e "$mutation_destination" ] || [ -L "$mutation_destination" ]; then
        printf '%s\n' 'ERROR: Refusing an existing migration destination.' >&2
        exit 1
    fi
    case "$mutation_evidence_root/" in
        "$mutation_main/"*|"$mutation_worktree/"*) printf '%s\n' 'ERROR: Evidence must be outside both worktrees.' >&2; exit 1 ;;
    esac
    mkdir -p "$mutation_evidence_root"
    if git -C "$mutation_evidence_root" rev-parse --git-dir >/dev/null 2>&1; then
        printf '%s\n' 'ERROR: Evidence root must not be Git-managed.' >&2
        exit 1
    fi
    mkdir "$mutation_destination"
    cp -pPR "$mutation_legacy_dir/." "$mutation_destination/"
)
```

Stop on any failure, preserving both locations; never retry into a partial destination. **Context: read-only comparison of external directories.** Confirm Python 3 availability first. This standard-library command compares complete entry inventories, SHA-256 file contents, link targets, permissions, ownership and modification times; it is not executed by this documentation task:

```sh
python3 - "$mutation_legacy_dir" "$mutation_destination" <<'PYVERIFY'
import hashlib
import stat
import sys
from pathlib import Path


def inventory(root):
    result = {}
    for entry in [root, *sorted(root.rglob('*'))]:
        metadata = entry.lstat()
        if entry.is_symlink():
            content = ('symlink', str(entry.readlink()))
        elif entry.is_file():
            content = ('file', hashlib.sha256(entry.read_bytes()).hexdigest())
        elif entry.is_dir():
            content = ('directory', None)
        else:
            raise SystemExit('Unsupported entry type; stop for human review.')
        result[str(entry.relative_to(root))] = (
            stat.S_IMODE(metadata.st_mode), metadata.st_uid, metadata.st_gid,
            metadata.st_mtime_ns, content)
    return result


source, destination = map(Path, sys.argv[1:])
if inventory(source) != inventory(destination):
    raise SystemExit('ERROR: Copy inventory, content or metadata differs; preserve both and stop.')
print('Copy content, link targets, permissions, ownership and modification times match.')
PYVERIFY
```


Inspect ACLs/extended attributes separately where present; hashes do not prove every metadata property. A mismatch blocks continuation; preserve both copies for review. Save comparison output as a new migration-verification artifact, and record source/destination and worktree metadata in new files without replacing historical `experiment.md`, `environment.txt` or `baseline-commit.txt`. Read-only inspection/copying may affect filesystem access times.

**Context: shell variables only, after successful copy verification and explicit human confirmation.** Activate the external location for all following commands:

```sh
mutation_evidence_dir="$mutation_destination"
printf 'Artifacts: %s\nMain-vault report: %s\n' "$mutation_evidence_dir" "$mutation_report"
```

No mutation or test success is inferred from migrated setup. Continue only with the separately approved baseline/test scope. Do not delete the temporary source as part of migration or automatic cleanup, even after comparison. Verify backup independently under the human-approved policy.

### Baseline Test Commands

**Context: experiment worktree only; logs/reports go to its external experiment directory.** Every Maven block below enters that worktree in a subshell, so the caller's shell and main checkout stay unchanged. The existing tests remain unchanged until original-suite results are preserved and reviewed.

```sh
(
    set -e
    set -C
    for mutation_output in baseline-order.log baseline-order.exit baseline-order-surefire; do
        if [ -e "$mutation_evidence_dir/$mutation_output" ] || [ -L "$mutation_evidence_dir/$mutation_output" ]; then
            printf '%s\n' 'ERROR: Prior evidence exists; use a new authorized attempt name.' >&2
            exit 1
        fi
    done
    cd "$mutation_worktree" || exit 1
    if env JAVA_HOME="$mutation_java_home" ./mvnw -B -ntp -Dtest=OrderTest test > "$mutation_evidence_dir/baseline-order.log" 2>&1; then
        mutation_exit=0
    else
        mutation_exit=$?
    fi
    printf '%s\n' "$mutation_exit" > "$mutation_evidence_dir/baseline-order.exit"
    cp -R target/surefire-reports "$mutation_evidence_dir/baseline-order-surefire"
    [ "$mutation_exit" -eq 0 ] || exit "$mutation_exit"
)
cat "$mutation_evidence_dir/baseline-order.log"
```

The baseline/restored blocks preserve logs and exit receipts, then return the captured nonzero Maven status on failure. Stop on that failure; do not run the next step. Proceed only after inspecting exit code/log/report and confirming selected tests actually passed without failures/errors/skips. A narrower selector is `-Dtest=OrderTest#calculatesPrdThresholdAndHalfUpExamples`; use the same selection for baseline/mutant comparisons. A baseline error is not a mutant kill.

**Context: experiment worktree, complete Docker-free Surefire baseline.**

```sh
(
    set -e
    set -C
    for mutation_output in baseline-fast.log baseline-fast.exit baseline-fast-surefire; do
        if [ -e "$mutation_evidence_dir/$mutation_output" ] || [ -L "$mutation_evidence_dir/$mutation_output" ]; then
            printf '%s\n' 'ERROR: Prior evidence exists; use a new authorized attempt name.' >&2
            exit 1
        fi
    done
    cd "$mutation_worktree" || exit 1
    if env JAVA_HOME="$mutation_java_home" ./mvnw -B -ntp test > "$mutation_evidence_dir/baseline-fast.log" 2>&1; then
        mutation_exit=0
    else
        mutation_exit=$?
    fi
    printf '%s\n' "$mutation_exit" > "$mutation_evidence_dir/baseline-fast.exit"
    cp -R target/surefire-reports "$mutation_evidence_dir/baseline-fast-surefire"
    [ "$mutation_exit" -eq 0 ] || exit "$mutation_exit"
)
```

**Context: experiment worktree, only if PostgreSQL integration tests are approved.** Docker unavailability blocks this gate; do not silently skip it. `verify` includes Surefire plus configured Failsafe `*IT` tests; capture both report groups:

```sh
(
    set -e
    set -C
    for mutation_output in baseline-verify.log baseline-verify.exit baseline-verify-surefire baseline-verify-failsafe; do
        if [ -e "$mutation_evidence_dir/$mutation_output" ] || [ -L "$mutation_evidence_dir/$mutation_output" ]; then
            printf '%s\n' 'ERROR: Prior evidence exists; use a new authorized attempt name.' >&2
            exit 1
        fi
    done
    cd "$mutation_worktree" || exit 1
    docker info || exit 1
    if env JAVA_HOME="$mutation_java_home" ./mvnw -B -ntp verify > "$mutation_evidence_dir/baseline-verify.log" 2>&1; then
        mutation_exit=0
    else
        mutation_exit=$?
    fi
    printf '%s\n' "$mutation_exit" > "$mutation_evidence_dir/baseline-verify.exit"
    cp -R target/surefire-reports "$mutation_evidence_dir/baseline-verify-surefire"
    cp -R target/failsafe-reports "$mutation_evidence_dir/baseline-verify-failsafe"
    [ "$mutation_exit" -eq 0 ] || exit "$mutation_exit"
)
```

Never rerun with an existing artifact filename/report destination: authorized additional runs need distinct attempt suffixes. Capture reports before later Maven runs overwrite the experiment worktree's `target/`. No Compose or persistent database migration is needed.

## D. Perform One Manual Mutation

**Proposed example only:** [Order constructor](../../src/main/java/com/example/agenticorderservice/order/domain/Order.java) and [existing threshold test](../../src/test/java/com/example/agenticorderservice/order/domain/OrderTest.java), method `calculatesPrdThresholdAndHalfUpExamples`. The [PRD](../01-requirements/product-requirements.md) requires a 10% discount when subtotal is at least EUR 100.00.

**Context: experiment worktree, explicit paths.** Inspect its current expression and fixture before editing; stop if they differ from this reviewed example:

```sh
mutation_source=src/main/java/com/example/agenticorderservice/order/domain/Order.java
rg -n -F 'discount = subtotal.compareTo(Money.of("100")) >= 0' "$mutation_worktree/$mutation_source"
rg -n -A 7 'void calculatesPrdThresholdAndHalfUpExamples' "$mutation_worktree/src/test/java/com/example/agenticorderservice/order/domain/OrderTest.java"
git -C "$mutation_worktree" diff --exit-code "$mutation_test_revision" -- src/test
```

In the editor, open `$mutation_worktree/$mutation_source` and change only this comparison. Never edit the main worktree source:

```diff
-        discount = subtotal.compareTo(Money.of("100")) >= 0
+        discount = subtotal.compareTo(Money.of("100")) > 0
```

Original: EUR 100.00 receives EUR 10.00 discount and totals EUR 90.00. Mutant: the same subtotal receives zero discount and totals EUR 100.00. The existing test includes `{"100", "10", "90"}` and should fail on its discount assertion. This is an expected outcome inferred from source, not an observed kill.

**Context: experiment worktree; external patch file.** Inspect and preserve the exact single-change patch:

```sh
(
    set -e
    set -C
    if [ -e "$mutation_evidence_dir/mutation.patch" ] || [ -L "$mutation_evidence_dir/mutation.patch" ]; then
        printf '%s\n' 'ERROR: Mutation patch already exists; preserve it and review the attempt.' >&2
        exit 1
    fi
    git -C "$mutation_worktree" status --short
    git -C "$mutation_worktree" diff -- "$mutation_source"
    git -C "$mutation_worktree" diff --cached
    git -C "$mutation_worktree" diff -- "$mutation_source" > "$mutation_evidence_dir/mutation.patch"
    git -C "$mutation_worktree" diff --exit-code "$mutation_test_revision" -- src/test
)
```

Do not stage or commit the temporary mutation. Additional candidates, requiring their own approval and separate experiment IDs, include HALF_UP discount rounding, duplicate-product quantity consolidation, and omitting one Product/Customer editable-field update. Never combine several faults into the same experiment.

## E. Execute and Interpret Tests

**Context: experiment worktree only, logs/reports in the external evidence directory. Expected-failure collection, not a green-test guard.** Run the same selected existing tests against the mutant, saving the initial result before proposing changes. This block can complete successfully while Maven fails as expected; its shell status means evidence collection completed, not that tests passed or the mutant was killed. Use the saved Maven exit code and actual assertion to classify the result:

```sh
(
    set -e
    set -C
    for mutation_output in mutant-original-suite.log mutant-original-suite.exit mutant-original-suite-surefire; do
        if [ -e "$mutation_evidence_dir/$mutation_output" ] || [ -L "$mutation_evidence_dir/$mutation_output" ]; then
            printf '%s\n' 'ERROR: Prior evidence exists; use a new authorized attempt name.' >&2
            exit 1
        fi
    done
    cd "$mutation_worktree" || exit 1
    if env JAVA_HOME="$mutation_java_home" ./mvnw -B -ntp -Dtest=OrderTest test > "$mutation_evidence_dir/mutant-original-suite.log" 2>&1; then
        mutation_exit=0
    else
        mutation_exit=$?
    fi
    printf '%s\n' "$mutation_exit" > "$mutation_evidence_dir/mutant-original-suite.exit"
    cat "$mutation_evidence_dir/mutant-original-suite.log"
    cp -R target/surefire-reports "$mutation_evidence_dir/mutant-original-suite-surefire"
    git -C "$mutation_worktree" diff --exit-code "$mutation_test_revision" -- src/test
)
```

Use a new filename or attempt subdirectory within the same experiment directory for every authorized attempt; do not overwrite the first outcome with a later improved-suite result. Approved broader runs use the Section C experiment-worktree blocks (`test` or Docker-backed `verify`) with the same command-local Java 21 environment and new attempt-specific logs/exit codes. Do not use direct Failsafe goals as a substitute for the configured lifecycle.

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

**Context: experiment worktree only.** Review all modified/staged/untracked files before restoring anything:

```sh
git -C "$mutation_worktree" status --short --untracked-files=all
git -C "$mutation_worktree" diff --stat
git -C "$mutation_worktree" diff -- "$mutation_source"
git -C "$mutation_worktree" diff --cached
```

**Context: experiment worktree restoration and external evidence.** Only when **all** changes in the selected source file are experiment-owned and its original content is the recorded approved pre-mutation checkpoint, restore that one file:

```sh
(
    set -e
    set -C
    [ -n "$mutation_restoration_revision" ] || exit 1
    for mutation_output in restored-production.diff restored-production-index.diff; do
        if [ -e "$mutation_evidence_dir/$mutation_output" ] || [ -L "$mutation_evidence_dir/$mutation_output" ]; then
            printf '%s\n' 'ERROR: Prior restoration evidence exists; review before another attempt.' >&2
            exit 1
        fi
    done
    git -C "$mutation_worktree" restore --source="$mutation_restoration_revision" --worktree -- "$mutation_source" || exit 1
    git -C "$mutation_worktree" diff --exit-code "$mutation_restoration_revision" -- "$mutation_source"
    git -C "$mutation_worktree" diff --cached --exit-code "$mutation_restoration_revision" -- "$mutation_source"
    git -C "$mutation_worktree" diff --exit-code "$mutation_restoration_revision" -- src/main
    git -C "$mutation_worktree" diff --cached --exit-code "$mutation_restoration_revision" -- src/main
    git -C "$mutation_worktree" diff "$mutation_restoration_revision" -- src/main > "$mutation_evidence_dir/restored-production.diff"
    git -C "$mutation_worktree" diff --cached "$mutation_restoration_revision" -- src/main > "$mutation_evidence_dir/restored-production-index.diff"
)
```

These checks compare experiment production content with its recorded pre-mutation checkpoint, never current develop or an assumed historical branch tip. They do not require either branch HEAD to equal that checkpoint. Keep the original baseline receipt immutable; an independently approved permanent change to the same source must be preserved and reviewed separately, not erased by whole-file restoration or hidden by changing the baseline. Worktree restoration does not clear staged changes: if any unexpected staged mutation exists, stop and inspect it rather than treating the worktree check as sufficient. If unrelated changes share the file, reverse only the experiment-owned lines manually, review the precise diff, and verify against the saved patch; do not restore the whole file. Never use a broad reset or cleanup to remove unrelated work.

**Context: experiment worktree only; external verification artifacts.** Rerun the selected tests against restored code and preserve the exit code and reports:

```sh
(
    set -e
    set -C
    for mutation_output in restored-order.log restored-order.exit restored-order-surefire; do
        if [ -e "$mutation_evidence_dir/$mutation_output" ] || [ -L "$mutation_evidence_dir/$mutation_output" ]; then
            printf '%s\n' 'ERROR: Prior evidence exists; use a new authorized attempt name.' >&2
            exit 1
        fi
    done
    cd "$mutation_worktree" || exit 1
    if env JAVA_HOME="$mutation_java_home" ./mvnw -B -ntp -Dtest=OrderTest test > "$mutation_evidence_dir/restored-order.log" 2>&1; then
        mutation_exit=0
    else
        mutation_exit=$?
    fi
    printf '%s\n' "$mutation_exit" > "$mutation_evidence_dir/restored-order.exit"
    cp -R target/surefire-reports "$mutation_evidence_dir/restored-order-surefire"
    git -C "$mutation_worktree" diff --check
    git -C "$mutation_worktree" diff --cached --check
    [ "$mutation_exit" -eq 0 ] || exit "$mutation_exit"
)
```

Inspect the restored log and require actual selected-test success. Repeat the applicable experiment-worktree full `test` and, if selected, Docker-backed `verify` gates from Section C with new `restored-*` log filenames. Do not overwrite baseline evidence or claim unexecuted verification passed.

## G. Investigate Survivors

Preserve the unchanged-suite result first. Determine whether the mutation changes observable approved behavior and whether the selected tests reach it; present the result and evidence for human review. Only for a reviewed genuine detection gap propose a targeted regression test and obtain separate authorization to change tests.

If authorized, retain the original result, reapply only the approved mutant in the experiment worktree if needed, and record the new regression test failing for the intended reason. Restore original production code again and record the improved suite passing. Label results **original suite** and **improved suite**; never rewrite an original survivor as an original-suite kill.

A genuine production defect follows its own authorized correction workflow. Test-improvement approval does not authorize changing accepted production behavior. Experimental mutations remain temporary and may never be committed as permanent implementation changes or merged into `develop`.

## H. Obsidian Reports and Evidence Template

Reports belong at `$mutation_main/docs/05-testing/mutation-results/MUT-NNN.md`, on **main `develop`**, not in the experiment worktree's mirrored docs. Worktrees share history, not checked-out files: a report committed only on the experiment branch does not automatically appear on `develop` or in the main vault.

After actual outcomes exist, create the readable report with an editor at `mutation_report`, using the template below. Refuse an existing report path for a new experiment; append only reviewed corrections to an existing report rather than overwriting history. Use exact original-suite outcomes, relevant assertions and separately labeled improved-suite evidence; do not fabricate a MUT-001 report from setup files. Add a relative navigation link from the **main** verified experiment report only after a new report exists.

**Context: main `develop`, read-only review of report changes.** Verify location/branch and inspect the report plus existing results navigation; the report path was derived from `mutation_main` in C:

```sh
git -C "$mutation_main" branch --show-current
git -C "$mutation_main" status --short --untracked-files=all
git -C "$mutation_main" diff -- "$mutation_report_relative" docs/05-testing/mutation-results/mutation-testing-experiments.md
cat "$mutation_report"
```

For a newly untracked report, `git diff` alone omits its content: read the complete file as above and include it in the human-reviewed diff. Obtain human acceptance and **separate explicit commit authorization**, then stage/commit only the approved report/index scope in the main worktree. Inspect any existing staging first; never include production changes or unrelated work. This task authorizes none of those future writes/commits. Record the verified report commit when available, without inventing a hash or requiring a report to contain its own commit hash. Do not merge the experiment branch wholesale to publish evidence.

Record the raw artifacts' verified absolute external location plus a portable sibling-path description and backup locator as code text. They are not GitHub files or vault notes; do not create broken Markdown links. Backup remains Pending until a complete verified copy exists under the approved policy.

```markdown
# Experiment <MUT-NNN>

- Date/time/timezone: <actual execution date>
- Authorization and scope: <human approval reference>
- Main worktree location/branch: <verified absolute path; develop>
- Experiment worktree location/branch: <verified absolute path; branch>
- Historical MUT-001 baseline, if applicable: <original receipt/hash>
- Approved experiment baseline: <full hash captured once; approval reference>
- Pre-mutation restoration checkpoint: <full hash; approved deviations from historical baseline>
- Unchanged-test revision: <full hash captured once>
- Experiment worktree starting revision: <full hash>
- Current experiment tip at review: <observed hash and timestamp>
- Current develop tip at review: <independent observed hash and timestamp>
- Branch-difference decision: <continue existing baseline or authorize new experiment; human reference>
- Authorized later commits: <hashes/scope, or none>
- Report location: <main docs/05-testing/mutation-results/MUT-NNN.md>
- Report commit, once available: <verified commit reference or Pending>
- Raw evidence location: <verified external absolute path; sibling-relative locator>
- Environment: <Java/Maven; Docker/PostgreSQL only if used>
- Requirement: <PRD rule/acceptance criterion>
- Source file/method: <experiment-worktree path and method>
- Original expression: <exact source>
- Mutated expression: <exact source>
- Existing unchanged test selection/revision: <classes/methods and commit>
- Passing original-code baseline: <command, exit code, reports>
- Expected behavioral impact/failing test: <hypothesis>
- Original-suite mutant result: <command, exit code, actual assertions or survival>
- Outcome/classification: <killed/survived/uncovered/equivalent candidate/inconclusive>
- Analysis/interpretation or separate original-code defect: <evidence>
- Human-review decision: <actual decision or Pending>
- Separately approved test improvement: <authorization and diff, or none>
- Improved-suite mutant result, if authorized: <distinct assertion evidence>
- Restoration evidence: <exact diff checks and saved mutation patch>
- Final restored-code verification: <commands/results/reports/limitations>
- Attempt-specific evidence: <separate log/exit/report names per authorized run>
- Migration verification: <source/destination/inventory/hash result, if applicable>
- Backup verification/retention: <owner/durable destination/date/hash verification/retention; Pending until verified>
- Remaining decisions: <explicit deferrals>
```

No extra prompt entry is created here. Follow the [playbook](../04-agentic-coding/prompts/00-index.md) if a future prompt record is agreed; readable experiment reports remain in `mutation-results/`, with navigation from the verified experiment report.

## I. Complete an Experiment Without Switching Main

Main should already remain on `develop`. Do not use `git switch develop` as normal experiment completion. Inspect each worktree independently and retain external evidence regardless of worktree lifecycle.

Before completion, require: recorded exact mutation/patch; every temporary production mutation removed; production code matching its recorded approved pre-mutation checkpoint; selected tests rerun successfully against restored code; no unexpected staged/unstaged/untracked experiment changes; no intentionally faulty production commits; human-reviewed results preserved on main `develop`; raw artifacts backed up and verified under the approved policy. Any unavailable gate remains Pending, never Passed.

**Context: independent read-only checks of both worktrees and experiment history.**

```sh
git -C "$mutation_main" branch --show-current
git -C "$mutation_main" status --short --untracked-files=all
git -C "$mutation_worktree" branch --show-current
mutation_current_experiment_tip=$(git -C "$mutation_worktree" rev-parse HEAD)
mutation_current_develop_tip=$(git -C "$mutation_main" rev-parse develop)
printf 'Experiment tip: %s\nDevelop tip: %s\n' "$mutation_current_experiment_tip" "$mutation_current_develop_tip"
git -C "$mutation_main" log --left-right --oneline "$mutation_current_experiment_tip...$mutation_current_develop_tip"
git -C "$mutation_main" diff --name-status "$mutation_current_experiment_tip" "$mutation_current_develop_tip"
git -C "$mutation_worktree" status --short --untracked-files=all
git -C "$mutation_worktree" diff --exit-code "$mutation_restoration_revision" -- src/main
git -C "$mutation_worktree" diff --cached --exit-code "$mutation_restoration_revision" -- src/main
git -C "$mutation_worktree" log --oneline "$mutation_baseline"..HEAD
git -C "$mutation_worktree" log -p "$mutation_baseline"..HEAD -- src/main
git -C "$mutation_worktree" diff --stat "$mutation_baseline"
git -C "$mutation_main" log -1 --format='%H %s' -- "$mutation_report_relative"
git -C "$mutation_main" worktree list --porcelain
```

Review every post-baseline commit, not just the final diff: reverting a committed intentional fault does not make committing it acceptable. Separately approved test/documentation commits may legitimately change the branch tip; record/review them rather than assuming final HEAD must equal the original baseline. A newer develop tip is not a restoration failure: the checks concern the experiment checkpoint only. Inspect branch differences and record the human decision to continue the existing experiment or separately authorize a new experiment on newer accepted code. Unexpected experiment production differences need separate review/correction authorization; never rewrite baseline receipts or automatically merge/rebase to make the branches match.

**Context: final guards for both worktrees, no branch switching or cleanup.** Run only after restoration, successful applicable verification, report acceptance/publication and backup verification. A nonempty status blocks completion; preserve untracked files and approved work through their own review/commit workflow, never stash/discard/reset automatically:

```sh
(
    set -e
    [ "$(git -C "$mutation_main" branch --show-current)" = develop ]
    [ "$(git -C "$mutation_worktree" branch --show-current)" = "$mutation_branch" ]
    [ -n "$mutation_baseline" ]
    [ -n "$mutation_restoration_revision" ]
    git -C "$mutation_worktree" diff --exit-code "$mutation_restoration_revision" -- src/main
    git -C "$mutation_worktree" diff --cached --exit-code "$mutation_restoration_revision" -- src/main
    mutation_experiment_status=$(git -C "$mutation_worktree" status --porcelain=v1 --untracked-files=all)
    mutation_main_status=$(git -C "$mutation_main" status --porcelain=v1 --untracked-files=all)
    if [ -n "$mutation_experiment_status" ] || [ -n "$mutation_main_status" ]; then
        printf '%s\n' 'ERROR: Review/preserve remaining changes before completion.' >&2
        exit 1
    fi
    printf '%s\n' 'Git restoration/clean-state checks passed; confirm the separate test, report and backup evidence.'
)
```

The subshell exits without terminating the interactive shell. Git cleanliness does not prove tests, report acceptance or backup: independently verify those records. Reports require their own main-worktree commit authorization. Test improvements stay in the experiment worktree until their independently authorized review/commit/merge workflow; do not discard them just to pass a guard or merge intentional faults to obtain them.

The experiment worktree may remain available for subsequent approved experiments. Removal is optional and separately authorized; first verify preserved reports/raw backup and absence of uncommitted changes. **Context: main worktree, only after explicit worktree-removal authorization.**

```sh
git -C "$mutation_main" worktree remove "$mutation_worktree"
git -C "$mutation_main" worktree list --porcelain
```

Use normal non-forced removal only; if Git refuses, stop and inspect rather than overriding it. Removal does not delete the branch or external evidence; do not delete the experiment branch merely because one experiment finished. Branch deletion needs separate authorization, evidence retention and history review.

Do not push mutants, open a pull request containing intentional faults, or intentionally run CI/SonarQube for temporary mutant code. The current [GitHub workflow](../../.github/workflows/build.yml) runs `clean verify` and the Sonar Maven scanner on pushes to `main`/`develop` and pull-request opened/synchronize/reopened events. A local commit alone causes none of those GitHub events; inspect hooks/external automation too. Legitimate separately approved report/test commits are permitted; pushing them and any resulting CI require the normal authorized workflow. No worktree, evidence migration, test run, code change or commit is executed by this document update.
