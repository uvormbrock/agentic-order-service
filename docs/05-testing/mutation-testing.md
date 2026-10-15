# Mutation Testing Strategy

**Status:** Mutation Testing – Planned, Pending Human Approval

**Execution:** Not performed. This document proposes future investigation; it approves no tooling, dependency, build change, branch creation or experiment.

For future authorized manual execution, use the [practical mutation-testing runbook](mutation-testing-runbook.md) for repository-specific commands, safe restoration and the evidence template. Its operational instructions do not authorize experiments or change this strategy.

## Purpose and Authority

Mutation testing investigates whether tests detect deliberately introduced behavioral faults. Green tests establish that the current code passes the selected assertions; code coverage shows which code was exercised. Neither establishes that a meaningful defect would fail a test. SonarQube static analysis examines code without establishing the fault-detection effectiveness of the test suite.

This is particularly useful for reviewing superficial or AI-generated tests that execute code but assert weak outcomes, repeat implementation calculations, or omit changed fields. Mutation testing complements human review and existing verification; it does not replace requirements, PostgreSQL integration tests, or architectural review.

Follow [AGENTS.md](../../AGENTS.md), the [testing strategy](testing-strategy.md) and [AI development workflow](../04-agentic-coding/ai-development-workflow.md). The accepted [Increment 3 record](../04-agentic-coding/prompts/04-domain-model.md#final-human-review-and-acceptance) and [Increment 4 record](../04-agentic-coding/prompts/05-product-customer-api.md#final-human-acceptance--2026-10-15) remain unchanged. Their passing tests, coverage and clean SonarQube result are not mutation-testing evidence. Business expectations come from the [PRD](../01-requirements/product-requirements.md), not from a mutant's output.

## Concepts and Interpretation

| Concept | Meaning |
| --- | --- |
| Mutation operator | A controlled transformation, such as changing a boundary comparison, arithmetic operation, return value or method call. |
| Killed mutant | An executable mutation detected by a relevant failing test; verify that the failure reflects the intended behavioral change. |
| Survived mutant | An executable mutation for which selected tests pass. It may expose a missing assertion, unsuitable test selection, or equivalent behavior; investigate before classifying it. |
| Equivalent mutant | A mutation with no observable behavioral difference in the supported context. It needs a documented rationale and human review, not just an assertion that testing is difficult. |
| Uncovered mutant | No selected test exercises the mutated code; report separately from covered-but-undetected behavior. |
| Inconclusive experiment | Compilation, setup, infrastructure failure or timeout prevents a reliable conclusion. Do not count it as demonstrated fault detection. |

A mutation score commonly compares killed mutants with the counted mutant population. Every report must state its exact formula, selected classes/tests/operators, denominator, exclusions and treatment of equivalent, uncovered, timed-out and invalid mutants. Compare scores only across comparable scopes and policies. No score or minimum target has been approved.

A high score can still miss ungenerated faults, missing requirements, integration failures or weakly selected code. A survivor is not automatically a production defect; a kill caused by broken infrastructure is not evidence of a useful assertion. Automatic equivalence classification and exhaustive fault coverage are not assumed.

## Proposed Project Scope — Pending Human Approval

Start with a small, reviewed selection of implemented behavior; do not introduce deferred features to create mutation targets. The following are candidate experiments and expected oracles, not executed results or guaranteed test kills.

| Target | Candidate behavioral change | Expected detection focus / existing test area |
| --- | --- | --- |
| Money calculations and rounding | Replace exact arithmetic or HALF_UP discount rounding | Independent literal amounts and rounding boundaries in MoneyTest and OrderTest. |
| Order thresholds and totals | Change subtotal >= EUR 100.00 to >, omit discount, or change total subtraction | OrderTest threshold cases, including EUR 99.99/100.00/100.05, and exact subtotal/discount/total assertions. |
| Order-owned item consolidation | Drop a duplicate product quantity or bypass the consolidated limit | OrderTest asserts one consolidated item, exact summed quantity, positive integers, overflow safety and the 10,000 limit. No standalone OrderItem repository is introduced. |
| Customer/Product invariants | Omit an editable-field update, bypass validation, or alter deactivation | CustomerTest/ProductTest and application tests independently check every field, unchanged identity/activity and atomic rejection. |
| Email normalization/validation | Omit stripping/lowercasing or weaken approved syntax/length checks | EmailAddressTest and customer application/API tests check literal normalized values and invalid boundaries; real uniqueness remains a PostgreSQL concern. |
| Application-service behavior | Ignore a requested price/email or return an incorrect materialized result | ProductApplicationServiceTest/CustomerApplicationServiceTest check independent inputs, entity effects and results. Mocked repositories cannot prove commit behavior. |
| API validation/failure handling | Accept forbidden/coerced input, change precedence, or map a known failure to the wrong code/status | StrictJsonRequestDecoderTest, API tests and ApiErrorContractTest use independent expected envelopes. Real servlet dispatch and commit/outage paths require the appropriate integration tests. |

Order domain calculations already exist; Order REST/placement, Customer DELETE coordination, pessimistic locking/retries and the full ADR-0002 protocol remain deferred. Mutation experiments must not implement or claim verification of them.

## Proposed Manual Experiment Procedure — Pending Human Approval

1. Obtain explicit approval for the target, exact mutation, expected failing test(s) and verification scope. Begin only on an isolated Git feature branch after separate branch/execution authorization; never mutate accepted production code on `develop`.
2. Establish and record a passing baseline against the original implementation: commit, working-tree state, selected unchanged tests and their results. Preserve unrelated changes, baseline tests and source history.
3. Introduce one controlled behavioral change at a time. Record the original expression, mutated expression and requirement that should be violated. Use independent expected values rather than reproducing the implementation's calculation.
4. **Baseline test effectiveness first:** execute each approved mutation against the existing, unchanged test suite before proposing any test improvement. Preserve the original result, including commands, environment, failing test names/assertions or surviving behavior.
5. Investigate and classify the original result. A baseline failure, invalid mutation or infrastructure error is inconclusive, not a kill; distinguish covered survivors, uncovered code and justified equivalent mutants. A genuine defect in the original implementation is a separate finding, not merely a test-detection gap.
6. Only after identifying and obtaining human review of a genuine detection gap, propose additional or strengthened regression tests. Do not weaken existing assertions or alter the recorded original-suite outcome.
7. If test improvements are separately authorized, demonstrate that the approved regression test fails against the mutant and passes against the restored original implementation. Record this outcome separately: detection after test hardening must never be retroactively reported as detection by the original suite.
8. Restore only experiment-owned production-code changes and inspect the diff to prove no experimental mutation remains. Do not discard unrelated work. Rerun applicable verification against the restored implementation, retaining traceable experiment evidence and separately approved test improvements.
9. Present the actual diff and original-suite/post-hardening evidence for independent human review. Investigation results, test-improvement acceptance, production-code correction acceptance and commit/merge authorization are separate decisions.

Experimental production-code mutations are temporary investigation artifacts: never commit them as production fixes or permanent implementation changes, and never merge them into `develop`. Remove all experimental mutations before any proposed merge. If an experiment reveals a genuine existing production defect rather than a test weakness, stop and request separate human authorization before modifying the accepted implementation. Such a correction requires an independently authorized workflow; approval of regression-test improvements does not authorize a production-code fix.

No manual experiment has been executed by this documentation task. Do not treat earlier source inspection, suspected test gaps or hypothetical kills as experimental evidence.

## Proposed Automation Investigation — Pending Human Approval

PIT is a **candidate**, not an approved plugin or dependency. No PIT version, JUnit adapter, configuration or command is selected here. Before proposing installation, investigate official compatibility information and demonstrate the exact candidate combination in an authorized isolated experiment:

- Existing Java 21, Maven Wrapper 3.9.9, Spring Boot 4.1.1 and its managed JUnit 6 Jupiter/Platform versions; do not downgrade JUnit or override accepted dependencies to obtain compatibility.
- JUnit 6 discovery/execution support, any required adapter and its version, parameterized tests, Spring contexts, Mockito instrumentation and the existing JaCoCo agent/configuration.
- Maven lifecycle interaction: Surefire runs `*Test`, Failsafe runs PostgreSQL `*IT` during `verify`. A mutation runner's discovery must be verified separately; it does not automatically inherit or reproduce both test responsibilities.
- Begin with proposed Docker-free domain/value-object targets and selected application/API tests. Full-context, real HTTP and PostgreSQL tests need separate feasibility, isolation, timeout and runtime review; unit-only mutations cannot establish database constraints, migrations, commit semantics or concurrency correctness.
- Measure baseline duration, mutation count and execution cost before expanding. Propose selective class/test/operator runs and bounded resources; exclusions and incremental results must be visible rather than presented as whole-project coverage.
- Preserve ordinary test/verify, JaCoCo and SonarQube behavior. Keep initial execution outside CI unless CI integration is separately approved. Tool-generated status labels require interpretation; a timeout is not automatically accepted as an assertion kill.

If compatibility is unsupported or unclear, report the blocker and alternatives for approval. Do not silently install tooling, add libraries, skip tests or treat a proposed tool as Accepted.

## Evidence and Proposed Acceptance Criteria

Use one record per manual mutation or a traceable automated report entry. At minimum capture:

| Evidence | Required content |
| --- | --- |
| Identity and baseline | Experiment ID, commit, branch, source location, environment, tooling/version if used, and passing baseline. |
| Behavioral hypothesis | Original/mutated behavior, operator and authoritative requirement; expected failing test and assertion. |
| Original-suite execution | Exact commands and unchanged test revision/selection, observed detecting test/assertion or surviving behavior, logs/report location and infrastructure limitations; preserve this initial outcome. |
| Disposition | Killed by the original suite, survived with a genuine detection gap, justified equivalent, uncovered or inconclusive; supporting reasoning and human review. Track any genuine defect in the original production implementation separately. |
| Separately authorized test improvement | Gap review and test-change authorization, exact regression-test diff, mutant failure and restored-original-code passing result; keep detection after hardening distinct from original-suite effectiveness. |
| Restoration and correction boundary | Proof that experimental mutations are removed and applicable final verification passes; persistent evidence remains traceable. Any genuine production defect needs separate correction authorization, review and acceptance, never promotion of an intentional mutation. |

Before accepting a future campaign, require traceable results for every executed mutation, explicit disposition of survivors and exclusions, honest score accounting if a score is reported, and successful applicable verification after restoration. Equivalent-mutant claims require review; genuine gaps require a proposed test correction or explicit deferral. Inconclusive/unavailable checks remain Pending. Do not turn these proposed criteria into a new mandatory release threshold without approval.

Store agreed future prompts/results in the existing [playbook](../04-agentic-coding/prompts/00-index.md) only when their contents and scope are approved. Do not create speculative prompt entries. Human acceptance of investigation evidence does not automatically accept implementation changes, ADRs or a merge.

## Deferred Decisions and Prerequisites

| Decision / prerequisite | Status |
| --- | --- |
| Manual experiment scope, branch creation and execution | Proposed; Pending Human Approval |
| Exact automated tooling, plugin/adapter versions and dependency changes | PIT candidate; Pending Human Approval and compatibility verification |
| Target classes, operators, test selection and PostgreSQL/HTTP inclusion | Proposed scope above; Pending Human Approval |
| Runtime budget, timeout policy and resource limits | Pending Human Approval |
| CI integration and reporting/artifact retention | Pending Human Approval |
| Mutation-score formula, exclusions and target, if any | Pending Human Approval; no arbitrary minimum selected |
| Actual experiments, results and test-improvement acceptance | Pending; no execution or mutation score claimed |

Existing Accepted decisions and quality gates remain unchanged. ADR-0002 remains Proposed and implementation-unverified; ADR-0006 remains Proposed as a complete record. This strategy neither reopens accepted behavior nor authorizes the next implementation increment.
