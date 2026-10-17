# Verified Mutation Testing Experiments

**Execution status:** Executed – Pending Human Review

**Experiment date:** 2026-10-16

**Source revision:** `f1b79a44edf3bae91c3803092df69cfcbb29444e` (`develop`)

## Purpose and Navigation

These two explicitly authorized, completed experiments show complementary evidence: an existing JUnit assertion detects a production defect, and PIT exposes a lost assertion safeguard while JUnit remains green. Execution evidence is distinct from human acceptance of this report.

- [Testing strategy](../testing-strategy.md): applicable quality gates.
- [Mutation-testing strategy](../mutation-testing.md): interpretation and limitations.
- [Operational runbook](../mutation-testing-runbook.md): approval, isolation, restoration and evidence handling.
- [Product requirements](../../01-requirements/product-requirements.md): BR-008 and AC-008 define the discount boundary.

This report replaces the former `00-index.md` in this directory. It preserves navigation and evidence-storage guidance below. The earlier MUT-001 setup is separate historical evidence; neither experiment is a continuation or renaming of MUT-001. No historical evidence was modified.

## Mutation and Outcome Definitions

A production mutant is an intentional behavioral change used to challenge tests. PIT generates mutants in its test execution environment; a manual experiment can instead temporarily edit a source file in an isolated worktree.

| Outcome | Meaning |
| --- | --- |
| KILLED | A selected test detects the mutant. Inspect the failure or detecting test to distinguish meaningful detection from an unrelated problem. |
| SURVIVED | Tests exercise the mutant but do not detect its changed behavior. Investigate the assertions, supported behavior and possible equivalence. |
| NO_COVERAGE | Selected tests do not exercise the mutation location. This is not evidence that assertions accepted the altered behavior. |

A surviving mutant does not necessarily fail Maven: this project's PIT profile has no mutation-score threshold. Successful execution and an acceptable mutation result are separate questions. Compilation errors, infrastructure failures and timeouts are not assertion-based detection evidence.

## Experiment 1 — Intentional Production Defect

**Evidence ID:** `MANUAL-ORDER-THRESHOLD-20261016T073525Z-fr9wecqk`

BR-008 requires a 10% discount at EUR 100.00 and above. Only the comparison in `Order.java`, line 68, was temporarily changed:

```diff
-        discount = subtotal.compareTo(Money.of("100")) >= 0
+        discount = subtotal.compareTo(Money.of("100")) > 0
```

The unchanged `OrderTest.calculatesPrdThresholdAndHalfUpExamples()` expects EUR 10.00 discount at exactly EUR 100.00. The mutant instead returns EUR 0.00.

| Phase | Tests | Failures | Errors | Skipped | Maven exit |
| --- | ---: | ---: | ---: | ---: | ---: |
| Original | 1 | 0 | 0 | 0 | 0 |
| Mutated | 1 | 1 | 0 | 0 | 1 |
| Restored | 1 | 0 | 0 | 0 | 0 |

Actual assertion failure at `OrderTest.java:30`:

```text
expected: Money[amount=10.00, currency=EUR]
 but was: Money[amount=0.00, currency=EUR]
```

This experiment manually altered production source and used ordinary JUnit, not a new PIT run, to detect the defect. The corresponding boundary mutant was also historically KILLED by PIT in `PIT-ORDER-VALIDATION-20261015T120256Z-388154d9/pit-reports/mutations.xml`, with this same test recorded as the killer.

Verified raw evidence directory:

```text
/Users/rookie/Documents/Development/Projekte/agentic-order-service-mutation-evidence/MANUAL-ORDER-THRESHOLD-20261016T073525Z-fr9wecqk
```

Each of `baseline/`, `mutated/` and `restored/` contains `command.json`, `maven.log`, `result.json` and copied `surefire-reports/`. The root contains `mutation.patch`, `Order.original.java`, `experiment.json`, `restoration-check.json` and `final-safety.json`. The restored source SHA-256 matches the original:

```text
12bc16e8b719116daf5b204579b5e84ef342b6445675a5a714a569cc4a4ab24f
```

## Experiment 2 — Test Greenwashing

**Evidence ID:** `GREENWASH-ORDER-20261016T074124Z-t_ano87l`

Production source remained unchanged. The only temporary test edit removed `{"100", "10", "90"}` from `OrderTest.calculatesPrdThresholdAndHalfUpExamples()`:

```diff
-        String[][] examples = {{"99.99", "0", "99.99"}, {"100", "10", "90"}, {"100.05", "10.01", "90.04"}, {"150", "15", "135"}};
+        String[][] examples = {{"99.99", "0", "99.99"}, {"100.05", "10.01", "90.04"}, {"150", "15", "135"}};
```

The assertions and EUR 99.99, 100.05 and 150.00 cases remained unchanged. Removing this example removes both discount and total checks at the exact threshold; removing only the discount assertion would leave the total assertion able to detect the defect.

### Exact Mutation Identity

| Field | Value |
| --- | --- |
| Class | `com.example.agenticorderservice.order.domain.Order` |
| Method | `<init>` |
| JVM descriptor | `(Ljava/util/UUID;Ljava/util/List;)V` |
| Source line | 68 |
| Mutator | `org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator` |
| Instruction index | 185 |
| Description | `changed conditional boundary` |

Exactly one XML entry matched this identity in each phase. Compare the complete identity, including method descriptor, mutation description and index, rather than scores alone: totals can conceal different mutants or test selections. These identity fields are stable across these three runs, not guaranteed across production/compiler changes.

### Verified Results

| Phase | JUnit passed | Failures / errors / skips | JUnit exit | Target status | detected | numberOfTestsRun | PIT exit |
| --- | ---: | --- | ---: | --- | --- | ---: | ---: |
| Original | 45 | 0 / 0 / 0 | 0 | KILLED | true | 7 | 0 |
| Weakened | 45 | 0 / 0 / 0 | 0 | SURVIVED | false | 15 | 0 |
| Restored | 45 | 0 / 0 / 0 | 0 | KILLED | true | 7 | 0 |

Original and restored XML identify `OrderTest.calculatesPrdThresholdAndHalfUpExamples()` as the killing test; weakened XML has an empty `killingTest`. `numberOfTestsRun` is PIT's per-mutant execution count, not the number of JUnit baseline tests.

| Phase | Generated | KILLED | SURVIVED | NO_COVERAGE | TIMED_OUT |
| --- | ---: | ---: | ---: | ---: | ---: |
| Original | 12 | 11 | 0 | 1 | 0 |
| Weakened | 12 | 10 | 1 | 1 | 0 |
| Restored | 12 | 11 | 0 | 1 | 0 |

The target transition is **KILLED → SURVIVED → KILLED**. The weakened mutant was exercised (`numberOfTestsRun > 0`), so this is genuine survival, not NO_COVERAGE. The separate uncovered mutant is not evidence for this claim.

Verified raw evidence directory:

```text
/Users/rookie/Documents/Development/Projekte/agentic-order-service-mutation-evidence/GREENWASH-ORDER-20261016T074124Z-t_ano87l
```

Each phase directory (`01-original/`, `02-weakened/`, `03-restored/`) contains JUnit and PIT `command.json`, `maven.log` and `execution.json` files, copied Surefire reports and JUnit counts, PIT HTML/XML under `pit-reports/`, `target-mutation.xml` and `summary.json`. Root evidence includes `weakened-test.patch`, `OrderTest.original.java`, `checksums.json`, `initial-state.json` and `result.json`. Original and restored test SHA-256 values agree:

```text
545a58ed773c68522ceebbed6784a4cdfc0a6974a55b3be46ac3b986d2d8c1d1
```

### Reproduction Parameters

Both experiments used command-local Java 21 and the Maven Wrapper. Experiment 1 selected only `OrderTest#calculatesPrdThresholdAndHalfUpExamples`; experiment 2 selected the complete `OrderTest` class. Both used `-Djacoco.skip=true -DfailIfNoTests=true` for ordinary JUnit execution. Exact argument arrays are preserved in the phase `command.json` files.

Experiment 2 used `-Pmutation-order`, PIT Maven 1.30.1, the configured `DEFAULTS` mutators and one thread, with production target `com.example.agenticorderservice.order.domain.Order`, test target `com.example.agenticorderservice.order.domain.OrderTest`, and a separate external `reportsDirectory` for each phase. It did not run project-wide or PostgreSQL integration tests. Follow the runbook's approval gates before any reproduction; this report authorizes no execution.

## Interpretation for Agentic Engineering

Passing JUnit tests show agreement with the remaining assertions. They do not prove that important examples were retained. Removing one row from a loop did not change the 45 reported test executions. Similarly, high line coverage shows execution, not whether assertions distinguish correct from incorrect behavior. These runs skipped JaCoCo; no measured coverage percentage or unchanged-coverage claim is made.

In AI-assisted development, an agent can accidentally weaken tests while making a suite green. Reviewing the test diff and comparing a known mutant's outcome provides evidence that an important safeguard was lost. PIT exposes this particular weakening through a surviving mutant; it does not automatically identify intent, detect every form of test weakening or replace human review. No production defect or security guarantee is inferred from the experiment.

## Safety, Evidence Storage and Review Boundaries

Both experiments used newly created, isolated, detached temporary Git worktrees at the recorded source revision. The main `develop` checkout remained clean during execution. Original files and their checksums were restored, temporary worktrees were removed normally without force, historical evidence remained outside Git, and no experimental changes were committed. Experiment 2's `result.json` additionally records production checksums unchanged at all verification checkpoints and existing worktrees unchanged. These are completed-experiment safety findings; this documentation edit naturally makes the current main working tree dirty.

Readable reports belong in this directory in the main `docs/` Obsidian vault and are eligible for Git versioning after review and explicit commit authorization. There is no duplicate Obsidian copy. Raw artifacts remain in the external sibling evidence directory, not `.mutation-artifacts/` or the vault. External filesystem paths above are code text rather than broken GitHub/Obsidian links.

Local persistence is not a verified backup. Independent backup destination, owner, retention, inventory/checksum verification and backup locator remain Pending; no durable-backup guarantee is claimed. Preserve MUT-001's independent setup history and original baseline. New experiments must capture their own explicitly approved baseline rather than reuse this report's revision automatically.

This report records completed execution, not final human acceptance of these results, a new score threshold, CI changes, architecture decisions or authorization for further experiments. Report commits require separate explicit authorization.
