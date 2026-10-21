# Documentation and Decisions

**Date:** 2026-10-21, Europe/Berlin
**Authorization:** Increment 8 final verification/documentation authorized; original Increment 0 attachment and later approvals retained below as history.
**Human Acceptance:** Pending.
**Current decision:** Option B is explicitly Accepted by the subsequent human approval below; initial option-pending statements and the original request are historical.

## Initial Increment 0 — historical evidence

## Acceptance criteria and current/target distinction

1. Native project team is Orchestrator plus implementer/tester/reviewer, preserving cap three, file ownership, independent review, handoff and three correction cycles.
2. Former role duties are reassigned before retirement; historical evidence is retained.
3. ADR-0007 compares A/B, recommends one and stays Proposed. ADR-0004 and current layered architecture remain authoritative.
4. Target describes ports/adapters, feature boundaries, transaction propagation/completion, mapping risks, incremental migration and executable verification; no production implementation follows.
5. Inventory differentiates native lifecycle hooks, Git hooks, Maven checks and CI gates. Only supported event/configuration schemas are used; script smoke tests and actual runtime triggering are reported separately.
6. Git pre-commit is prepared but not activated; no user-global hooks, automatic staging/formatting, full integration/PIT/Sonar checks on lifecycle events or commits.
7. No application Java/tests, POM, schema, REST/OpenAPI, CI or pre-existing untracked skill/link content changes.

## Proposed architecture decision

Recommend B (ports/adapters with selected accepted JPA metadata) because current requirements favor a small single-database modular monolith and tested identity/version/transaction behavior. A offers stronger ORM independence but introduces duplicate models and mapping/rehydration risks without a current replacement requirement. This narrows the earlier strict recommendation; it is not an accepted decision. The strict skill target is explicitly adapted under B, never silently redefined as framework-independent. The human must select an option and authorize a bounded production increment separately.

## Documentation changes

New ADR-0007 and hexagonal-migration target document; additive proposed-scope notes in system architecture, persistence guidance, testing strategy and implementation plan. Current accepted ADRs and business/API contracts remain unchanged. Workflow/readme/runtime documentation now assign requirements/documentation to the Orchestrator and architecture assessment to the read-only reviewer. Existing handoff fields remain, with explicit Restrictions and approval status added. Hook validation reads that template rather than a second field specification.

## Original approved request — verbatim

# APPROVED — Increment 0: Hexagonal Architecture, Agent Team Engineering and Hooks

Work in the existing `agentic-order-service` Spring Boot repository.

## Authorization

**Human approval is granted for Increment 0 only: Architecture Decisions, Agent Team Setup, and Quality-Gate/Hook Configuration.**

This approval does NOT authorize production-code refactoring or migration.

Use the previously completed architecture and agent-team assessment as the baseline.

Read the installed hexagonal architecture skill at:

`.codex/skills/hexagonal-architecture/SKILL.md`

Follow the project's existing approval and change-request procedures.

## 1. Architecture Decision and Documentation

Prepare the transition from the current feature-oriented layered modular monolith to a hexagonal architecture while preserving the existing modular-monolith deployment model.

Document:

- Target architecture and package boundaries
- Input ports and output ports
- Inbound and outbound adapters
- Domain and application responsibilities
- Dependency directions
- Cross-feature communication
- Transaction ownership and propagation
- Persistence mapping requirements
- Incremental migration strategy
- ArchUnit enforcement strategy

### Important: JPA Domain Separation

The existing ADR-0004 explicitly allows direct JPA mapping of domain aggregates.

Do not silently override that accepted decision.

Compare two options:

A. Strict hexagonal architecture with framework-independent domain models and separate JPA persistence entities.

B. Pragmatic hexagonal architecture with ports and adapters while retaining selected JPA mappings in domain aggregates.

Explain the advantages, costs, risks and implications of each option.

Recommend one option based on the actual project requirements.

Keep the new migration ADR in Proposed status until this specific architectural decision receives explicit human approval.

Do not start production migration based on an unresolved ADR.

## 2. Native Codex Agent Team Engineering

The proposed team structure is approved for configuration work:

- Main-session Orchestrator
- implementer
- tester
- reviewer

Consolidate the responsibilities of the existing requirements, architect and documenter roles into the Orchestrator and reviewer as proposed.

Update the native Codex agent configurations accordingly.

Requirements:

- Keep responsibilities narrow and non-overlapping.
- Preserve the existing handoff contract.
- Preserve the maximum of three correction cycles.
- Keep the existing concurrency limit of three.
- Assign clear file ownership.
- Require independent review.
- Maintain human approval gates.
- Never allow agents to commit, push, merge or deploy automatically.

Before retiring existing agent configurations, verify that their responsibilities and relevant workflow references have been reassigned.

Validate native Codex agent discovery and configuration compatibility.

Do not invent configuration keys.

## 3. Hooks — Inventory and Configuration

Hooks are part of this approved Increment 0.

First, inspect the repository and determine whether any hooks are already configured.

Inspect:

- `.codex/`
- `.git/hooks/`
- Existing Git hook managers, if any
- `pom.xml`
- CI workflows
- Existing Maven quality gates
- Existing ArchUnit tests
- Existing Spotless configuration

Distinguish between:

1. Native Codex lifecycle hooks
2. Git hooks
3. Maven quality checks
4. CI quality gates

Do not describe existing Maven commands as hooks unless they are actually configured to run through a hook mechanism.

### Proposed Hook A — Architecture Rules Reminder

Evaluate a native Codex `PreToolUse` hook that reminds the agent to consult the relevant architecture rules before modifying Java production files.

The reminder should reference:

- `AGENTS.md`
- The approved architecture documentation
- The installed hexagonal architecture skill

It must not inject the entire architecture documentation into every tool call.

If the installed Codex version supports this lifecycle hook, implement a minimal, validated configuration.

If unsupported, document the limitation and use the existing AGENTS.md workflow without inventing a substitute hook mechanism.

### Proposed Hook B — Agent Handoff Validation

Evaluate a native Codex `SubagentStop` or equivalent supported lifecycle hook.

Its purpose is to verify that specialist-agent handoffs contain the required information:

- Assigned task and scope
- Files changed or inspected
- Actual verification commands and results
- Findings and unresolved issues
- Restrictions and approval status

Use existing handoff contracts as the source of truth.

Do not duplicate the handoff specification.

Do not automatically run long Maven builds at every agent stop.

Do not allow a hook to silently approve a change or modify production code.

Implement only if supported and reliably testable.

### Proposed Hook C — Git Pre-Commit Quality Checks

Evaluate a lightweight Git pre-commit hook for Java changes.

Candidate checks:

```bash
./mvnw -B -ntp spotless:check
./mvnw -B -ntp -Dtest=ArchitectureComplianceTest test
```

Adapt the commands to the existing build setup and Java 21 configuration.

Important:

- Reuse the existing ArchUnit test infrastructure.
- Do not change architecture rules merely to make tests pass.
- Keep execution time reasonable.
- Avoid redundant execution.
- Do not automatically format unrelated files.
- Do not automatically stage files.
- Do not install or activate Git hooks globally.
- If activation affects the developer's local Git behavior, explain the impact and obtain approval before activation.

The hook implementation itself may be prepared and validated during Increment 0.

### Proposed Hook D — Full Verification

Do NOT automatically run the entire PostgreSQL integration suite, PIT mutation tests or SonarQube analysis through every commit or agent lifecycle event.

Keep comprehensive verification in the existing explicit Maven and CI quality gates.

Avoid adding a redundant pre-push hook unless there is a demonstrable benefit.

## 4. Hooks and Skills: Separation of Responsibilities

Use the following model:

- AGENTS.md defines mandatory project instructions.
- The hexagonal architecture skill provides reusable architecture guidance.
- Specialist agents own bounded engineering responsibilities.
- Hooks trigger lightweight checks or reminders.
- ArchUnit enforces structural dependency rules.
- JUnit and integration tests verify behavior.
- CI executes the agreed automated quality gates.
- The human approves architectural decisions and migration increments.

Hooks must not replace tests, skills or architecture documentation.

Keep AGENTS.md concise and refer to authoritative documentation instead of duplicating it.

## 5. Scope of Increment 0

Permitted changes:

- Agent configuration files
- Agent workflow and handoff documentation
- Architecture documentation
- Proposed ADR for the migration
- Relevant quality-gate documentation
- Minimal hook scripts and configurations
- Necessary hook-validation tests or smoke checks
- Minimal AGENTS.md updates

Not permitted:

- Production Java code modifications
- Domain-to-JPA mapping changes
- Database migrations
- REST or OpenAPI changes
- Business-logic changes
- Large-scale package renaming
- Premature replacement of existing ArchUnit rules
- Unrelated dependency upgrades
- New framework adoption without approval

Preserve all pre-existing uncommitted and untracked files.

Inspect Git status and the existing diff before writing.

Do not overwrite unrelated work.

## 6. Verification

Verify:

1. Native Codex agent configuration loads correctly.
2. Retired roles have no unresolved references.
3. Agent workflow and handoff documentation remain consistent.
4. Architecture documentation and ADR status are correct.
5. Hook configuration uses supported APIs.
6. Hook scripts pass targeted positive and negative smoke tests.
7. No unexpected repository files were modified.
8. `git diff --check` passes.

Do not claim that hooks are active or working without actually validating their triggering behavior.

If native hook functionality cannot be exercised in the current environment, report it as unverified.

Do not run the full application regression suite unless required by an actual change.

## 7. Deliverables

Provide a concise implementation report with:

1. Final agent-team structure.
2. Roles retained, modified or retired.
3. Documentation and ADR changes.
4. Comparison of strict versus pragmatic hexagonal architecture.
5. Recommended unresolved architectural decisions.
6. Existing hooks discovered.
7. Hooks created or prepared.
8. Hook events, commands and responsibilities.
9. Hook smoke-test results.
10. Existing Maven/CI checks reused.
11. Exact changed files.
12. Any necessary manual hook activation steps.
13. Git status.
14. Next recommended migration increment.

Do not begin Increment 1.

Do not stage, commit, push, merge or deploy.

Stop for human review.

**Human Acceptance: Pending for the resulting configuration and all subsequent production migration work.**

## Follow-up authorization and current acceptance criteria — 2026-10-21

Explicit human decision selects Option B, preserves ADR-0004 and approves a local fast/full engineering workflow while retaining personal Git/Cloud control. Accept ADR-0007, keep mappings/REST/production unchanged, document actual gate inventory (JUnit 6, not a downgrade to 5), run config/hook smoke checks, validate fast and full including PostgreSQL/PIT where available, report actual coverage/mutation outcomes and blockers. Preserve existing thresholds (none configured locally for numeric JaCoCo/PIT scores), no automatic formatting, no hook activation, no Sonar actions/CI changes or Git mutation. Independent review remains required; the human alone accepts the result. Increment 1 remains unapproved.

### Original follow-up request — verbatim

# APPROVED — Architecture Option B and Automated Engineering Quality Gates

Work in the existing `agentic-order-service` repository.

Use the approved Increment 0 implementation and review as the baseline.

## Human Decisions

The following decisions are explicitly approved:

1. **Architecture Option B: Pragmatic Hexagonal Architecture.**
2. Keep the existing feature-oriented modular monolith.
3. Introduce input ports, output ports and adapters incrementally.
4. Preserve existing domain/JPA mappings unless a separate change is justified and approved.
5. Preserve existing transactional, concurrency, persistence and REST behavior.
6. Retain the approved native Codex agent team:
   - Main-session Orchestrator
   - implementer
   - tester
   - reviewer

The user wants extensive automated engineering verification but retains personal control of Git commits, pushes, merges, deployments and SonarQube Cloud quality-gate evaluation.

## 1. Finalize Architecture Decision

Update ADR-0007 to reflect the human-approved Option B.

Document:

- The chosen pragmatic hexagonal architecture.
- Input and output port conventions.
- Dependency directions.
- Existing JPA/domain coupling as an explicitly accepted constraint.
- Relationship to the still-valid ADR-0004.
- Cross-feature access rules.
- Transaction and persistence invariants.
- Incremental migration boundaries.
- ArchUnit enforcement strategy.

Change ADR-0007 status to Accepted, provided there are no unresolved contradictions.

Do not supersede ADR-0004.

Keep the documentation concise and internally consistent.

## 2. Existing Quality-Gate Inventory

Before modifying any build or automation configuration, inspect the existing project.

Identify the actual configuration, commands and execution scope for:

- Spotless
- JUnit 5
- Mockito
- ArchUnit
- Maven Surefire
- Maven Failsafe
- Testcontainers / PostgreSQL
- JaCoCo
- PIT mutation testing
- Maven profiles
- Existing coverage and mutation thresholds
- Existing CI workflows
- SonarQube Cloud integration
- Native Codex lifecycle hooks
- Prepared Git pre-commit hooks

Reuse existing mechanisms.

Do not introduce duplicate plugins, competing configurations or invented thresholds.

Do not remove or weaken any existing quality requirement.

## 3. Define Fast Verification

Establish a repeatable, documented command or workflow for fast local verification.

It should cover:

- Code formatting
- Unit tests
- Existing ArchUnit tests
- Relevant fast regression tests

Use the existing Maven configuration.

Where automatic formatting is enabled, limit formatting changes to the intended scope whenever practical.

Never reformat unrelated source files.

Fast verification should run automatically after a completed implementation increment or meaningful correction.

Do not execute the complete verification pipeline after every individual file edit.

## 4. Define Full Verification

Establish a repeatable, documented full-verification workflow.

It should cover:

- Code formatting verification
- Complete unit-test suite
- ArchUnit compliance
- Integration tests using PostgreSQL/Testcontainers
- JaCoCo coverage generation and existing configured checks
- PIT mutation testing
- Existing relevant Maven quality gates
- Independent reviewer assessment

The Orchestrator must invoke full verification automatically before presenting a completed production-refactoring increment for human acceptance.

A verification failure must be reported and corrected before acceptance is recommended.

Do not silently skip expensive tests.

If PIT or integration tests cannot run due to environment limitations, explicitly report the blocker and keep verification status Pending.

Preserve existing thresholds. Do not invent new coverage or mutation thresholds.

If the repository already provides suitable Maven goals or profiles, reuse them.

If a wrapper script or Makefile target would materially simplify execution, propose or implement the smallest maintainable solution.

Avoid redundant Maven invocations and conflicting concurrent builds.

## 5. Hooks

Inspect and preserve the native hooks prepared during Increment 0:

- PreToolUse architecture reminder
- SubagentStop handoff validation

Validate their configuration and smoke tests.

If native hook trust or activation requires explicit developer action, report the exact action required.

Do not bypass trust controls or silently enable broader permissions.

Review the prepared Git pre-commit hook.

Keep pre-commit checks lightweight.

Do not include full PostgreSQL integration tests, JaCoCo full verification, PIT, or SonarQube analysis in the pre-commit hook.

Do not automatically activate Git hooks through core.hooksPath during this task.

The user will explicitly decide whether to activate them.

## 6. Automation Boundaries

The following actions are strictly forbidden without a separate explicit human request:

- git add
- git commit
- git push
- git merge
- git rebase
- git reset
- deployment
- triggering or modifying SonarQube Cloud analysis or remote quality gates
- changing branch protection or GitHub repository settings

Automated testing, formatting, local analysis and evidence collection are allowed within the approved scope.

Do not confuse automated verification with automated approval.

The user decides when to commit and push.

## 7. Update Agent Responsibilities

Update agent instructions and workflow documentation so that:

- implementer produces a bounded production-code change.
- tester verifies behavior and tests.
- reviewer performs an independent architectural and code review.
- Orchestrator coordinates the workflow, runs the required final quality gates and consolidates evidence.

No agent may declare human acceptance on behalf of the user.

Avoid duplicating the detailed quality-gate instructions across every agent profile.

Keep AGENTS.md concise and reference the authoritative workflow documentation.

## 8. Scope Restrictions

This task finalizes Increment 0 configuration and quality automation only.

Do not start Increment 1.

Do not modify production Java code.

Do not migrate persistence mappings.

Do not modify database schemas or REST contracts.

Do not add new business features.

Preserve pre-existing uncommitted and untracked work.

Do not introduce a new orchestration framework.

## 9. Verification and Report

Run all relevant configuration and hook smoke tests.

Validate the agreed fast-verification workflow.

Run the full-verification workflow if the current environment supports it, including PIT and PostgreSQL integration tests.

If a command cannot run, report exactly why.

Provide:

1. Final ADR-0007 status.
2. Quality-gate inventory.
3. Exact fast-verification command.
4. Exact full-verification command.
5. PIT configuration, scope and result.
6. JaCoCo configuration and result.
7. Integration-test result.
8. Native hook activation status.
9. Git pre-commit activation status.
10. Exact changed files.
11. Git status.
12. Remaining blockers, if any.

Do not stage or commit anything.

Stop before production migration.

Human Acceptance: Pending.

## Increment 1 — Product GET-by-ID authorization and contract

2026-10-21, Europe/Berlin: the human supplied “IMPLEMENTATION APPROVED — Increment 1: Product GET-by-ID Hexagonal Migration” at `/Users/rookie/.codex/attachments/f516e88a-d8ca-4101-a323-0d9142dcadfd/Eingefügter Text.txt`. This explicitly approves one production slice with implementer/tester/reviewer, characterization, precise ArchUnit activation, fast/full PostgreSQL/JaCoCo/PIT verification and updates to CH-003 records. It supersedes only the previous absence of Increment 1 authorization. ADR statuses/mappings remain unchanged. No Increment 2, dependencies, schema, API changes, Cloud/settings/hooks or Git publication are authorized. Human Acceptance Pending.

Proposed minimal contract, awaiting the reviewer's returned handoff before edits:

- Existing ProductController.get(UUID) calls GetProductQuery.get(UUID):ProductResult; remaining controller operations keep their existing service.
- GetProductService implements GetProductQuery; read-only REQUIRED transaction; calls FindProductById.findById(UUID):Optional<ProductResult>, throws existing ProductNotFoundException when absent, includes inactive products.
- ProductReadAdapter implements FindProductById via unchanged ProductRepository.findById; constructs the existing ProductResult record inside the application transaction. No managed entity crosses either port.
- Preserve public ProductApplicationService.get as a compatibility delegate to GetProductQuery with the same transaction annotation. Other operations and ProductResult stay unchanged. This facade preserves existing direct callers without a second lookup algorithm.
- Freeze exact input/output types and constructor dependencies in the implementation handoff; no generic repository, duplicate JPA entity or speculative transaction abstraction.

Acceptance criteria: exact GET path/method/JSON/status/errors and inactive visibility; no repository/adapter dependencies in the extracted use case; input delegation at controller method scope; output adapter implements its port; port signatures contain no HTTP/provider/managed entity types; populated production imports and representative negative tests; precise adapter-to-contract replacement of the legacy persistence prohibition; real PostgreSQL transaction/materialization, runtime OpenAPI equality, fast/full gates and baseline-aware mutation evaluation. ArchUnit does not prove runtime semantics. Unrelated feature/image behavior remains protected by regression.

### Increment 1 contract freeze

Initial reviewer /root/inc1_reviewer independently confirmed the proposed signatures and file manifest without blocking defects. Orchestrator freezes the contract above on 2026-10-21 before production edits. Preserve the package-private ProductResult.from(Product) factory; the adapter maps through the canonical record constructor. The public legacy get delegates with read-only REQUIRED semantics retained. Port signature checks inspect the immutable data shape rather than all implementation dependencies of ProductResult. Implementation authorization already covers these minimal choices.

### Original Increment 1 authorization request (verbatim)

<details>
<summary>Human implementation request, 2026-10-21</summary>

# IMPLEMENTATION APPROVED — Increment 1: Product GET-by-ID Hexagonal Migration

Work in the existing `agentic-order-service` Spring Boot repository.

This is the first production-refactoring increment of CH-003.

## Human Authorization

The following is explicitly approved:

**Increment 1 — Migrate the existing Product GET-by-ID use case to the approved pragmatic hexagonal architecture.**

ADR-0007, Option B, is Accepted.

ADR-0004 remains valid. Existing domain aggregates and their JPA mappings must be preserved.

No subsequent increment is authorized.

## 1. Use the Approved Agent Team

Coordinate the task using the project's native Codex sub-agents:

- Orchestrator (main session)
- implementer
- tester
- reviewer

Read and follow:

- AGENTS.md
- ADR-0007
- ADR-0004
- Current system architecture documentation
- Installed hexagonal architecture skill
- Existing multi-agent workflow and handoff contracts
- Existing architecture-test documentation
- Existing verification workflows

Do not invent architecture rules that contradict approved ADRs.

Use the existing coordination, ownership and approval mechanisms.

## 2. Establish the Baseline

Before editing:

1. Inspect the current Git branch and working tree.
2. Identify uncommitted or untracked work.
3. Inspect the existing Product GET-by-ID implementation.
4. Identify the actual HTTP and OpenAPI contract.
5. Identify existing unit, integration and architecture tests.
6. Identify existing Product application services and persistence abstractions.
7. Identify the relevant ArchUnit rules prepared for migration.

Preserve unrelated changes.

Do not overwrite, stage or commit pre-existing work.

If the working tree contains overlapping changes that cannot be safely distinguished, stop and request guidance.

## 3. Define the Increment Contract

Freeze a minimal migration scope for Product GET-by-ID.

The target should establish these responsibilities:

### Inbound Adapter

The existing REST controller handles HTTP transport concerns and invokes an application input port.

### Input Port

Defines the application-facing Product GET-by-ID capability without exposing transport or persistence implementation types.

### Application Use Case

Owns the existing read-use-case behavior and coordinates access through an output port.

### Output Port

Defines the narrow persistence capability required by this use case.

### Outbound Adapter

Uses the existing Spring Data/JPA infrastructure to implement the output port.

### Domain

Retains the existing Product aggregate, business semantics and JPA mappings permitted by ADR-0007 Option B.

Do not introduce a second JPA entity representation.

Prefer reusing existing services and repositories where this provides clean boundaries without unnecessary indirection.

Do not create generic frameworks or speculative abstractions.

If the proposed design would introduce redundant pass-through layers, simplify it before implementation.

## 4. Preserve All Existing Behavior

The migration must preserve:

- Product GET-by-ID endpoint and HTTP method
- Existing response structure and JSON serialization
- Existing error behavior and HTTP status codes
- Missing or inactive product handling
- Existing security and validation behavior
- Transaction semantics
- Product identity and persistence behavior
- OpenAPI compatibility
- Existing test expectations

Do not change the database schema.

Do not change unrelated Product operations.

Do not modify Customer, Order or image-processing behavior unless an essential dependency makes that unavoidable; in that case, stop for approval.

## 5. Agent Execution

Use a contract-first, bounded implementation workflow.

### Reviewer — Initial Architecture Check

Before implementation, independently assess the proposed Product GET-by-ID port and adapter boundaries.

Identify architectural contradictions, unnecessary abstractions and risks.

### Tester — Characterization and Architecture Verification

Ensure the existing behavior is adequately characterized before implementation.

Add focused regression tests only where necessary.

Prepare or activate the relevant ArchUnit rules for the migrated Product GET-by-ID boundary.

Ensure architecture tests fail for representative prohibited dependencies and do not pass vacuously.

### Implementer — Production Refactoring

Perform only the approved Product GET-by-ID migration.

Respect the frozen boundaries and assigned file ownership.

Do not change the business contract.

Do not weaken tests to accommodate the refactoring.

### Reviewer — Independent Final Review

Inspect the final production diff and test evidence.

Check:

- Correct dependency directions
- Ports and adapters consistency
- ADR-0007 compliance
- Preservation of Product behavior
- Unnecessary abstraction or complexity
- JPA and transaction compatibility
- Activation of appropriate ArchUnit rules
- Scope containment

Report actionable findings independently.

Do not automatically accept the implementation.

## 6. Architecture Enforcement

The relevant architecture rules must become executable for the migrated scope.

At minimum, verify:

- The REST adapter depends on the input-port contract rather than a concrete use-case implementation.
- The application use case does not directly depend on Spring Data repository implementations.
- The output adapter implements the defined output-port contract.
- The migrated port contracts do not expose HTTP or persistence infrastructure types.
- The application does not depend on concrete outbound adapters.
- Permitted JPA annotations in domain aggregates remain permitted under Option B.
- Relevant rule scopes are non-empty and actually examine the migrated production classes.

Preserve all existing ArchUnit rules unless an approved architecture decision requires a precisely scoped change.

Do not prematurely activate strict rules across legacy features that have not been migrated.

## 7. Automated Verification

Use the existing verification workflows.

During implementation, run focused tests and `make verify-fast` at appropriate milestones.

Before presenting the increment for human acceptance, run:

```bash
make verify-full
```

Use the repository's established Java 21 environment and verification conventions.

Full verification must include the existing:

- Unit tests
- ArchUnit tests
- PostgreSQL integration tests
- Spotless checks
- JaCoCo reporting
- PIT mutation testing

Compare PIT results against the documented pre-migration baseline.

Do not treat an unchanged historical mutation deficit as a newly introduced regression.

Investigate newly introduced or worsened mutation outcomes in the changed Product scope.

Do not invent quality thresholds.

If full verification cannot complete, explain the blocker and leave acceptance Pending.

Never report PASS for a check that did not execute.

Avoid running competing Maven builds against the same target directory.

## 8. Strict Scope and Git Restrictions

Do not:

- Start Increment 2
- Modify unrelated use cases
- Replace existing domain aggregates
- Introduce separate JPA entity models
- Change REST/OpenAPI contracts
- Change database schema
- Introduce new Maven dependencies without approval
- Modify CI or SonarQube settings
- Activate Git hooks without explicit approval
- Stage files
- Commit
- Push
- Merge
- Deploy

The user will perform Git commits and remote SonarQube verification manually.

## 9. Deliverables

Provide a concise final report containing:

1. Before-and-after architecture of Product GET-by-ID.
2. Input port and output port created or reused.
3. Inbound and outbound adapter changes.
4. Exact changed files.
5. Agent responsibilities and handoffs.
6. New or activated ArchUnit rules.
7. Focused regression-test results.
8. `make verify-fast` result.
9. `make verify-full` result.
10. JaCoCo and PIT results, including relevant baseline comparison.
11. Independent reviewer findings.
12. Git status.
13. Any unresolved risks or decisions.

Update the existing CH-003 implementation and review records according to the project's approved workflow.

Do not modify unrelated documentation.

Stop after Increment 1.

**Human Acceptance: Pending.**

</details>

### Increment 1 acceptance-criteria evidence

The approved six-file production migration is implemented without additional decisions. Populated ProductGetArchitectureTest verifies input delegation, output implementation, inward application dependencies and materialized neutral signatures; representative illegal dependencies and empty scopes fail as intended. Existing domain/JPA metadata remains accepted, with no second entity representation. Unit/MVC/PostgreSQL tests prove missing/inactive lookup, exact DTO facts, unchanged HTTP/error behavior, read-only transactions, ambient REQUIRED joining and detached snapshots. Runtime OpenAPI JSON/YAML/snapshot equality passed without regeneration. Schema, other operations and public get compatibility are preserved.

Final local gate details and mutation comparison are in 02/03. Existing 212 survivors and 37 uncovered mutants require historical disposition, not a new Product regression. One unchanged shared async mutant now times out; its cause/disposition remain Pending outside this increment. No implicit approval of those outcomes, Cloud results or Human Acceptance follows from successful execution. No further migration is authorized.

## Verification policy update — 2026-10-21

Explicit human instruction changes CH-003 scheduling from Increment 1 onward. This supersedes the earlier per-increment PIT/full-target requirement in the original authorization and historical records; preserve their wording/results as history. Architecture/test/coverage/mutation requirements remain unchanged; only PIT execution moves to final migration acceptance. No production change or new increment is authorized.

Individual increments use focused regression/ArchUnit/Spotless and make verify-fast during development, then complete Java 21 `./mvnw -B -ntp -DfailIfNoTests=true clean verify` plus independent architecture/code review before acceptance. No PIT/profile/goals/mutation target or PIT-enabled make verify-full during an increment. PostgreSQL/Testcontainers must execute; blocked checks remain Pending. Final migration acceptance uses make verify-full SCOPE=all, complete PIT, final JaCoCo/regression/ArchUnit/reviewer assessment and comparison with the established pre-migration baseline.

Increment 1 already executed the exact complete Maven phase on matching sources and has independent review; reuse that evidence, do not rerun for documentation alone. Its previous mutation results and INC1-R1 remain recorded for final migration assessment, not an automatically recurring increment gate. Human Acceptance remains Pending for each increment. Authoritative scheduling: [quality gates](../../multi-agent/quality-gates.md#ch-003-migration-verification-policy).

### Original verification-policy request (verbatim)

<details>
<summary>Human scheduling update, 2026-10-21</summary>

# Verification Policy Update — Hexagonal Architecture Migration

Apply this verification policy to Increment 1 and all subsequent implementation increments of CH-003.

## During Each Increment

Automatically execute:

- Focused unit and regression tests
- Relevant ArchUnit architecture tests
- Spotless verification
- `make verify-fast`

## Before Human Acceptance of Each Increment

Automatically execute:

- Complete unit-test suite
- All applicable ArchUnit tests
- PostgreSQL integration tests using Testcontainers
- JaCoCo coverage reporting and existing checks
- Independent architectural and code review

Use existing Maven goals or verification targets without changing test semantics.

**Do not run PIT mutation testing during individual implementation increments.**

Do not invoke `make verify-full` if it automatically starts PIT.

Do not silently skip PostgreSQL integration tests.

## Final Migration Acceptance

After all implementation increments are completed, execute:

- `make verify-full SCOPE=all`
- Complete PIT mutation testing
- Final JaCoCo and regression assessment
- Final ArchUnit compliance review
- Independent reviewer assessment

Compare mutation results against the established pre-migration baseline.

## Restrictions

Do not weaken existing tests, architecture rules, mutation configuration or coverage requirements.

Do not modify production code merely to accommodate this policy.

Do not automatically stage, commit, push or deploy.

Human Acceptance: Pending for each implementation increment.

</details>

## Increment 2 — Product write authorization and frozen contract

Explicit human request on 2026-10-21 approves Product create/update/delete migration only. Human clarification defines delete as existing PATCH deactivation; physical DELETE remains 405. Human additionally explicitly approved the narrow same-feature output-port exception below after reviewer identified the existing managed-entity prohibition. No later increment is authorized.

Bounded contract: `ProductWriteOperations` exposes `create(String, BigDecimal)`, `update(UUID, String, BigDecimal)` and `deactivate(UUID)`, returning immutable `ProductResult`. `ProductWriteService` implements these with existing application-owned REQUIRED write transactions. `ProductWriteStore` exposes `save(Product)` and `findLockedById(UUID)` returning `Optional<Product>`; this exact port may carry the existing Product aggregate only within Product and inside the application transaction. `ProductWriteAdapter` delegates unchanged repository operations. No entity allowance applies to input ports, GET or cross-feature contracts.

Preserve name/price validation and precedence, generated ID timing, result materialization, pessimistic lookup, optimistic versions, managed dirty checking without update saves, inactive updates and idempotent deactivation. Keep facade write compatibility delegates and public static price validator; list/get, Order helpers/images, mappings/schema and HTTP/OpenAPI remain unchanged. Acceptance requires focused characterization, populated/negative architecture checks, Spotless, make verify-fast, complete non-PIT Maven verify/PostgreSQL/JaCoCo/OpenAPI and independent review. PIT Deferred by human decision; Cloud and Git actions remain human-controlled. Human Acceptance Pending.


## Increment 3 — Authorization and proposed bounded contract

Human request: attachment `/Users/rookie/.codex/attachments/5e58d7a2-524e-420f-9fad-4c5759d7ff88/Eingefügter Text.txt`, 2026-10-21, Europe/Berlin. Explicitly authorizes existing Customer operations/facts/reference bridge, team execution, characterization, precise ArchUnit activation, focused/fast/complete non-PIT verification and CH-003 documentation. No Increment 4, Order/image migration, schema/API/dependency changes, Cloud actions or Git publication. Preserve existing unrelated uncommitted work.

Proposed contract, not yet an accepted managed-output decision:

- `CustomerOperations` input port: existing create(String,String), get(UUID), list(), update(UUID,String,String), deactivate(UUID), delete(UUID); immutable CustomerResult/list or void. Existing CustomerApplicationService implements it, preserving application-owned transactions and static normalizeEmail/validateEmail compatibility. Controller injects the input interface.
- `CustomerWriteStore` output port: `Customer save(Customer)`, `Optional<Customer> findLockedById(UUID)`, `void delete(Customer)`; CustomerWriteAdapter delegates to the unchanged CustomerRepository within the same transaction. These exact same-feature aggregate signatures require human approval because the accepted exception is Product-only.
- `CustomerReadStore`/CustomerReadAdapter return immutable CustomerResult facts for by-ID, list, batch-current and locked provider lookup. Existing CustomerResult mapping can be made accessible to the adapter without changing its data shape.
- `CustomerFacts` input contract exposes existing lock(UUID) and current(Collection<UUID>), implemented by CustomerOrderAccess with unchanged MANDATORY participation. Retain existing Order callers through that independent compatibility provider; do not route through deletion orchestration.
- `CustomerOrderReferences` output contract exposes references(UUID); a narrow bridge delegates to existing MANDATORY OrderReferenceQuery without changing Order persistence or orchestration.

Preserve all six actual endpoints (POST, collection/item GET, PUT, PATCH deactivation, physical DELETE), validation/error precedence, ASCII email policy, normalization/uniqueness across inactive records, generated UUID/@Version/JPA mapping, dirty checking, read-only reads, REQUIRED writes, READ COMMITTED deletion, lock-before-distinct-reference-query, after-commit logging and both placement/delete schedules. No API-to-domain normalization shortcut.

Decision INC3-A1: explicitly approve the three CustomerWriteStore aggregate signatures above, limited to same-feature application transactions; input/read/cross-feature ports remain immutable. ADR-0007 remains unchanged until a human decision. Decision INC3-A2: direct whether initial implementation may proceed with cumulative3/3 and zero correction capacity, stopping at the first required correction. Do not infer a reset or fourth batch from Increment3 authorization.


### Increment 3 human decisions resolved — 2026-10-21

Human explicitly approved the exact CustomerWriteStore aggregate exception and ADR-0007 update. Human rejected proceeding under the exhausted cumulative allowance and instead approved the governing per-increment policy amendment. Workflow now grants three correction cycles per explicitly authorized increment. Historical Increment2 remains 3/3 without reinterpretation; Increment3 starts at **0/3**, initial implementation iteration0. Escalate on exhaustion; no unresolved earlier defect may be concealed by increment boundaries. Earlier intake holds are historical and resolved by these decisions. Proceed with the already authorized Customer scope; non-PIT verification and Human Acceptance Pending remain binding.


### Increment 3 frozen implementation contract

Initial reviewer `/root/initial_architecture` re-evaluated the approved decisions and found no remaining blocker. CustomerApplicationService remains the transactional implementation bean, implements CustomerOperations, and preserves static validation/normalization. CustomerReadStore exposes immutable by-ID/all/current-batch/locked facts. CustomerWriteStore has only the approved three aggregate methods. CustomerOrderAccess implements CustomerFacts with existing MANDATORY participation; unchanged Order consumers retain this compatibility provider. CustomerOrderReferenceAdapter bridges CustomerOrderReferences to existing MANDATORY OrderReferenceQuery, explicitly transitional provider-contract debt for the later Order increment. No Order source migration is required. CustomerResult.from is public mapping support, not an entity-bearing input contract.

Assigned production files: existing CustomerController, CustomerApplicationService, CustomerOrderAccess, CustomerResult; new CustomerOperations, CustomerFacts, CustomerReadStore, CustomerWriteStore, CustomerOrderReferences, CustomerReadAdapter, CustomerWriteAdapter and CustomerOrderReferenceAdapter. Tester owns relevant tests/fixtures and exact architecture enforcement; main owns documentation and serialized verification.


### Increment 3 acceptance-criteria evidence

Approved Customer exception and per-increment policy recorded before implementation; historical correction records preserved. Actual Customer operations, provider/reference boundaries and exact36-file manifest match the frozen contract. Successful baseline160/focused207/fast708/complete708+167, Spotless/ArchUnit negative tests, PostgreSQL transaction/concurrency/optimistic behavior, unchanged runtime OpenAPI and JaCoCo results are recorded in02/03. No schema/domain mapping/API/Order/image production change. PIT Deferred by human decision, Cloud Pending human control; final human acceptance remains Pending.


## Increment 4 — Human-authorized contract and intake

2026-10-21 (Europe/Berlin). Request provenance: user attachment titled “IMPLEMENTATION APPROVED — Increment 4: Product Image Hexagonal Migration”. Scope: existing image endpoints and maintenance capability, processing, local binary storage, metadata persistence and transaction coordination. No subsequent increment, Order/Customer rework, schema/API/dependency/CI changes. All cumulative work must remain uncommitted.

Current endpoints: PUT multipart `/products/{id}/image` (204), GET original and GET `/thumbnail` (binary), DELETE association (204); quiescent reconciliation has no HTTP endpoint. Preserve all existing validation/error precedence, media negotiation, write opt-in, inactive reads, size/dimension limits, metadata association and filesystem safety.

Current orchestration directly uses ProductRepository, ProductImageRepository, ProductImageProcessor, ProductImageStorage and Spring TransactionTemplate. Stage writes both complete files before metadata mutation; there is no temporary-to-final rename or synchronization callback. READ COMMITTED transactions lock Product before metadata/read access. Only successful execute/commit permits obsolete-generation cleanup. All database failures retain staged and prior generations, even confirmed rollback; no automatic retries. Ambient transactions, synchronization and bound resources are rejected before processing/storage. Reconciliation requires caller-confirmed global quiescence.

Target: smallest coherent application input/output capabilities with existing JPA/ImageIO/filesystem/Spring adapters; preserve Option B domain mappings and application orchestration. Do not silently extend existing managed-entity output exceptions to ProductImage. Exact bounded metadata and transaction contract under independent assessment before production edits. Required acceptance: focused image/negative architecture tests, Spotless, make verify-fast, complete non-PIT clean verify including PostgreSQL/JaCoCo/OpenAPI and independent review. All fresh gates Pending.


### Increment 4 bounded contract — initial assessment complete

Independent reviewer found no blocking decision for entity-free image capabilities within the explicit increment authorization. ProductImage is a metadata entity, not a separate aggregate root. Use immutable metadata snapshots; do not add a managed-entity port exception or change ADR statuses. Application selects operations, creates metadata and owns ordering/transactions/cleanup. JPA adapter maps through unchanged domain constructor/replace and saves the same managed existing instance; no detached merge. Reads must not unnecessarily reconstruct/revalidate historical metadata. Test identity and all persisted fields.

Selected boundaries: ProductImageOperations input; processing and storage output capabilities with application-owned processed/staged values; entity-free image metadata capability including Product locking; narrow transaction execution/ambient guard capability. Exact names and manifest will follow implementation. READ COMMITTED, ambient rejection, commit-before-return, unchanged exceptions and conservative generation retention remain mandatory. No generic outcome boolean or new synchronization protocol. Reviewer initial assessment completed read-only; production correctness still Pending.


### Increment 4 documentation consistency and verified compatibility

Current architecture, domain metadata call sites and image architecture-test guidance were updated in place, alongside these four records. No PRD/REST/OpenAPI/schema/domain mapping decision changed. Final complete local verification Passed 727 fast + 167 PostgreSQL, 62 architecture and 13 OpenAPI executions; runtime API snapshot unchanged. JaCoCo and exact 36-path manifest in [implementation](02-implementation.md#increment-4-exact-changed-file-manifest). Fresh independent milestone review Pending; Human Acceptance Pending. Increment 4 correction1/3; PIT Deferred by human decision. Stop after Increment 4.


Increment 4 final milestone review returned Passed with no actionable findings. All four records/current documentation are consolidated, including exact scope, lifecycle invariants, matching full verification and retained limitations. **Completed — Human Review Required; Human Acceptance Pending.** This does not accept Cloud/PIT/historical outstanding decisions or authorize any later increment/Git action.


## Increment 5 — Authorization and actual read scope

2026-10-21 Europe/Berlin. User attachment “IMPLEMENTATION APPROVED — Increment 5: Order Read Operations” explicitly authorizes existing reads only. Actual endpoints: `GET /orders`, `GET /orders/{id}`; no filters, sorting or pagination contract. Current `OrderQueryService` has `get(UUID)` and `list()`, class-level `@Transactional(readOnly=true)` with default REQUIRED participation. Controller is its only production caller; tests mock/call it. No overlapping Order production edits at intake.

Current flow: controller → service → existing `findByIdWithItems`/`findAllWithItems` left-fetch-join queries → immutable OrderResult, enriched once with distinct current Customer IDs through existing MANDATORY CustomerOrderAccess. Missing ID fails before Customer access; empty list still invokes Customer.current(empty). No sort is added; preserve returned root/item order. Results must be materialized before transaction completion and serializable with OSIV disabled. Copy stored amounts/names/quantities; never invoke placement construction or pricing calculations on reads. OrderResult.from is also used by unchanged placement and must remain compatible.

Target: same service implements entity-free read input contract and consumes immutable-read output contract plus consumer-owned Customer facts capability backed by existing provider. Existing fetch queries/domain/mappings/schema/placement remain unchanged. Initial independent contract assessment and characterization Pending; no managed-entity output exception authorized or needed. Required local gates: focused unit/HTTP/PostgreSQL/ArchUnit, Spotless, make verify-fast, complete non-PIT clean verify/JaCoCo/OpenAPI and fresh independent review. Human Acceptance Pending.


### Increment 5 bounded implementation contract

Initial independent reviewer `/root/inc5_architecture` found no blocker for the following design; Orchestrator selected it within explicit implementation authorization. `OrderQueries` input: `OrderResult get(UUID)`, `List<OrderResult> list()`. `OrderReadStore` output: `Optional<OrderReadSnapshot> findById(UUID)`, `List<OrderReadSnapshot> findAll()`. Snapshot fields: id, customerId, immutable List<OrderResult.Item>, currency, subtotal, discount, total; copy stored values directly, no revalidation or recalculation. `OrderCustomerFacts` output: `Map<UUID, CustomerResult> current(Collection<UUID>)`. `order.integration.OrderReadCustomerAdapter` delegates to existing provider input `CustomerFacts.current`, retaining MANDATORY transaction participation. No Customer implementation changes.

Existing OrderQueryService implements input port with unchanged class-level readOnly/default REQUIRED semantics; persistence adapter invokes unchanged distinct left fetch joins and materializes snapshots in the caller transaction. Service retains current-customer batching and result assembly, including missing-customer exception/message, missing-order-before-provider precedence and empty-list provider call. OrderResult.from and all placement/retry/domain/repository code remain unchanged. No managed-entity output exception/new transaction protocol/dependency is required.

Review risks requiring evidence: multi-root/item loading without duplicate roots or N+1, detached immutable response serialization, direct copies of stored monetary facts differing from current pricing rules, and read-only/ambient transaction equivalence. No new sorting or query filters are introduced.


### Increment 5 compatibility evidence and current documentation

Existing architecture/migration/architecture-test guidance and these four records updated in place; no PRD/REST/OpenAPI/domain/JPA/schema decision changed. Baseline/characterization and final tests prove stored historical monetary facts, current inactive Customer enrichment, get/list two-query fetching, multiple roots/items, materialization/serialization after closure and readOnly/ambient transaction equivalence. Complete non-PIT verification740fast+170PostgreSQL,69architecture,13OpenAPI Passed. Exact23-path manifest/coverage/provenance in [implementation](02-implementation.md#increment-5-exact-changed-file-manifest). Final fresh handoff Pending; Human Acceptance Pending, correction1/3, PIT Deferred by human decision. Stop after Increment5.


Final independent milestone review Passed,no actionable finding. All four records/current documentation consolidated with exact scope, historical-data/fetch/transaction evidence and remaining debt. **Completed — Human Review Required; Human Acceptance Pending.** No ADR status/human acceptance or later implementation/Git authority inferred from passed tests.


## Increment 6 Phase A bounded migration contract

**Date:**2026-10-21 Europe/Berlin. **Status:**Proposed; Phase B blocked. Provenance: human attachment “IMPLEMENTATION APPROVED — Increment 6: Order Placement Hexagonal Migration”,conditionally authorized subject to mandatory safety gate. No later increment,Git mutation,PIT,Cloud,API/schema/domain/pricing change.

### Observed placement and public contract

`POST /orders` binds existing CreateOrderRequest,constructs PlaceOrderCommand,invokes OrderPlacementCoordinator and maps OrderResult after return. Success201 with Location and unchanged JSON; errors retain current catalog/envelope/precedence. Strict nested transport validation precedes application call. Existing GET/list ports remain untouched.

The coordinator first rejects actual ambient transaction,active synchronization or any bound resource,then validates nonnull command/consolidates quantities. PlaceOrderCommand uses canonical UUID-text ordering and duplicate consolidation/max10000 per product. The monotonic budget starts after consolidation. No lock or database mutation occurs before these steps.

Each attempt uses the existing qualified TransactionTemplate: REQUIRED,READ COMMITTED. Inside its callback it registers afterCompletion observation **first**,sets `SET LOCAL lock_timeout = '2s'` **second**,locks Customer before Products in consolidated canonical order,checks active states,creates OrderLineInput price/name snapshots,constructs Order in application,saves,flushes and materializes OrderResult before callback exit. Existing root generated UUID,item generated UUID,cascade PERSIST and original mappings remain. No separate transactions or provider-owned commits. Both providers remain MANDATORY.

Normal template return follows transaction completion/resource cleanup and precedes ORDER_COMMITTED. On exceptions,the exact callback status is retained,initially UNKNOWN. Anything other than confirmed ROLLED_BACK yields existing TECHNICAL/unknown diagnostic ownership with no replay,including commit-confirmation failure after a real commit. Confirmed rollback must pass the unbound-resource guard before classification or backoff. A lock failure is recorded as its exact RuntimeException object; retry lock provenance is `failure == lockCallFailure`,not “a lock failed”.

Classifier preserves its complete graph algorithm: approved consistent40P01/40001,or55P03 only from original lock call with exact PostgreSQL structured ProcessInterrupts timeout evidence. Unknown/mixed SQL states,cycles,suppressed failures,connection/integrity/optimistic/business/transaction exclusions fail closed. Flush55P03 is not a lock timeout; flush40P01/40001 may qualify after confirmed rollback. No exception-text parsing.

At most3attempts;25/50ms backoffs outside completed attempts;10s monotonic scheduling budget checked before and after backoff. Fresh attempt contexts/facts; no uncertain replay. Interruption preserves interrupt state and terminal diagnostic ownership. Existing event names,levels,safe fields,best-effort logger and commit-before-success placement remain unchanged. OrderRetryPolicy algorithm,pricing/discount/HALF_UP rules and OrderResult.from are not rewritten. Full ADR-0002 remains Proposed; preserving these existing scoped settings does not accept it.

### Proposed exact capability contracts — awaiting decision

```java
// Input, immutable command/result only.
interface OrderPlacementOperations {
    OrderResult place(PlaceOrderCommand command);
}
// Scoped managed-Order exception requested, NOT yet accepted.
interface OrderWriteStore {
    Order save(Order order);
    void flush();
}
interface OrderPlacementCustomerFacts {
    CustomerResult lock(UUID id);
}
interface OrderPlacementProductFacts {
    ProductResult lock(UUID id);
}
interface OrderPlacementAttempts {
    void checkUnbound();
    OrderResult execute(PlacementAttempt attempt, Supplier<OrderResult> work);
    boolean transientFailure(RuntimeException failure, boolean originalLockCallFailed);
}
// Product-owned input; existing provider implementation gains interface only.
interface ProductFacts {
    ProductResult lock(UUID id);
}
```

PlacementAttempt is application-owned attempt-local state: completion UNKNOWN/COMMITTED/ROLLED_BACK,initially UNKNOWN,and exact lockCallFailure identity. The transaction adapter updates completion from Spring afterCompletion without wrapping/replacing failures. Callback absence or unrecognized status stays UNKNOWN. Normal execute return retains current success semantics; completion status controls exceptional retry eligibility,not a new success condition. No generic success/failure boolean,retry loop inside adapter,new propagation,independent commit or asynchronous work. `execute` is narrowly typed to OrderResult and exposes no Spring transaction status.

One coherent transaction-attempt adapter owns existing template/JDBC/ambient guard/retry classifier mechanics. Classification is invoked by application only after confirmed rollback and unbound check,as before. Time/backoff use existing JDK LongSupplier/LongConsumer/TimeUnit; no new timing framework. Application retains business sequencing,aggregate construction,lock-origin tracking,retry budget/scheduling,result construction and logging. OrderWriteAdapter delegates same aggregate save/flush to unchanged repository. Customer bridge calls existing CustomerFacts.lock; Product bridge calls proposed ProductFacts.lock through existing MANDATORY provider proxy. No recursive feature calls or constructor cycles.

### Proposed affected production files — no edits performed

Repository prefix `src/main/java/com/example/agenticorderservice/`:

| Action | Exact path |
| --- | --- |
| Modify placement injection only | `order/api/OrderController.java` |
| Implement input,consume ports,preserve sequencing | `order/application/OrderPlacementCoordinator.java` |
| Add attempt-local state | `order/application/PlacementAttempt.java` |
| Add input | `order/application/port/in/OrderPlacementOperations.java` |
| Add exact scoped write capability | `order/application/port/out/OrderWriteStore.java` |
| Add Customer capability | `order/application/port/out/OrderPlacementCustomerFacts.java` |
| Add Product capability | `order/application/port/out/OrderPlacementProductFacts.java` |
| Add attempt/completion/classification capability | `order/application/port/out/OrderPlacementAttempts.java` |
| Add write adapter | `order/persistence/OrderWriteAdapter.java` |
| Add Customer bridge | `order/integration/OrderPlacementCustomerAdapter.java` |
| Add Product bridge | `order/integration/OrderPlacementProductAdapter.java` |
| Add transaction adapter | `order/transaction/SpringOrderPlacementAttempts.java` |
| Relocate unchanged guard mechanics | `order/application/AmbientTransactionGuard.java` → `order/transaction/AmbientTransactionGuard.java` |
| Relocate unchanged classifier algorithm | `order/application/OrderRetryPolicy.java` → `order/transaction/OrderRetryPolicy.java` |
| Add Product provider input | `product/application/port/in/ProductFacts.java` |
| Implement interface only,no behavior change | `product/application/ProductOrderAccess.java` |

This is18proposed paths,counting both ends of2relocations. Existing OrderTransactionConfiguration bean,Order/OrderItem,OrderRepository,OrderResult,commands/failures,all Order read components and Customer implementation stay unchanged. Proposed Product interface is limited to existing lock capability; do not migrate Product provider persistence or add another storage port.

### Characterization and architecture enforcement plan

Tester confirms broad existing coverage (matrix in03). Before extraction,add deterministic changed-facts-between-retry characterization: mutate committed provider facts after a completed rollback and prove later successful attempt uses fresh facts with no partial prior aggregate. After contract approval,add adapter tests for same aggregate/generated root+item identities,save/flush exception identity and attempt completion/provenance. Keep original unit,real PostgreSQL concurrency,unknown-commit,HTTP and async/safe logging assertions.

Expected test/support edits under `src/test/java/com/example/agenticorderservice/`: existing `order/application/OrderPlacementCoordinatorTest.java`,`order/application/OrderPlacementConcurrencyIT.java`,`order/application/AsyncTransactionLoggingIT.java`; relocate `order/application/OrderRetryPolicyTest.java` to `order/transaction/OrderRetryPolicyTest.java`; necessary mock wiring in `AgenticOrderServiceApplicationTest.java`,`shared/api/ApiTestSupport.java`; precise `architecture/ArchitectureComplianceTest.java` exception. New contract tests: `order/persistence/OrderWriteAdapterTest.java`,`order/transaction/SpringOrderPlacementAttemptsTest.java`,`order/integration/OrderPlacementCustomerAdapterTest.java`,`order/integration/OrderPlacementProductAdapterTest.java`,`architecture/OrderPlacementArchitectureTest.java`. Additional negative fixtures may be nested in architecture test unless package selection requires a named fixture; freeze exact manifest before writing.

Activate populated named-placement rules with positive implementation checks: controller uses both approved Order input ports; migrated coordinator has no JDBC/Spring Data/transaction/runtime provider dependency; ordinary ports/signatures/nested payloads remain infrastructure/entity-free; only exact OrderWriteStore.save(Order):Order gets the requested exception; adapters access exact contracts only,bridges use provider inputs,no foreign persistence; domain Option B metadata rule unchanged. Negative fixtures must reject entity leakage elsewhere,controller bypass,coordinator repository/transaction access,adapter-to-use-case coupling and provider bypass. Preserve all existing read/Customer/Product/image/legacy reference rules. No broad package exception.

### Phase A blocking decisions and safety gate

**INC6-A1 (blocking):** ADR-0007 expressly limits managed-entity output exceptions to ProductWriteStore and CustomerWriteStore. New OrderWriteStore.save(Order):Order requires explicit human approval and a narrowly scoped ADR-0007 addition. flush() delegates existing flush within that same attempt. It does not authorize entities in input/read/cross-feature contracts,new entities,mappings or schema.

No legitimate entity-free alternative preserves required application-owned Order construction and existing JPA identity/cascade save without moving calculation into persistence,introducing rehydration/direct SQL or hiding the entity in opaque callbacks/Object. Those alternatives cross approved constraints.

**INC6-A2 (contract decision):** Explicitly approve the attempt/completion/provenance contract above under ADR-0007's reserved specific-signature/completion decisions. It extracts existing mechanics only and does not accept full ADR-0002 or change runtime behavior.

Safety gate: existing behavior understood; existing coverage strong and bounded characterization possible; recommended ports can preserve semantics **conditional on these decisions**; reviewer has unresolved architecture authorization blocker; “no additional human architecture decision required” is **not satisfied**. Therefore **Phase B must not start**. No human acceptance inferred.


Independent reviewer re-assessed this exact recorded contract and bounded file proposal: no further semantic concern beyond INC6-A1/A2. It is ready for a concrete human decision; it remains Proposed. No Phase B source changes authorized by this assessment.


## Increment 6 Phase B human approval — 2026-10-21

Human explicitly approved both INC6-A1 exact OrderWriteStore.save(Order):Order/flush exception and INC6-A2 documented transaction-attempt/completion contract,including narrow ADR-0007 update. All Phase A safety decision blockers resolved; independent assessment found no additional concern. The approved bounded contract/file ownership remains unchanged. Historical blocked/Proposed entries above retain their then-current status; this decision supersedes only those two holds. Human Acceptance remains Pending.

Tester must add and pass deterministic fresh-facts-between-eligible-retries characterization on unchanged placement code before critical production extraction. Implementer holds all production edits until Orchestrator release. Then run focused/ArchUnit/Spotless,make verify-fast,complete non-PIT clean verify/PostgreSQL/JaCoCo/OpenAPI and fresh independent final review serially. Counter starts/stays **0/3**; no correction consumed in Phase A. No PIT/Cloud/Git mutation,unrelated change or Increment7.


### Increment 6 final compatibility evidence

Approved bounded contract implemented in18production paths,13test/support paths and8documentation paths. Exact39-path manifest and matching commands/source/coverage in02. Characterization before extraction proved fresh facts;post-extraction full verification Passed758fast+171PostgreSQL,75architecture,13fast+1runtime OpenAPI,Spotless and JaCoCo. No domain/schema/API/pricing/Order-read/configuration change. Three-state outcome,exact provenance,rollback-onlyretry,locks/resources/budgets/logging preserved;fresh final independent handoff Pending. Counter2/3,Human Acceptance Pending,PIT Deferred by human decision. Stop after Increment6.


Final fresh independent milestone review Returned Passed,no actionable finding or blocking decision. Approved Order-only managed-aggregate and exact attempt contracts implemented and verified without business/API/schema/mapping/read change. All four records/current architecture/test guidance consolidated. **Completed — Human Review Required; Human Acceptance Pending.** Counter2/3,PIT Deferred by human decision;no later increment or Git action.


## Increment 7 authorization and acceptance contract

Source: human attachment `Eingefügter Text.txt`, heading “IMPLEMENTATION APPROVED — Increment 7: Hexagonal Architecture Consolidation”, received 2026-10-21. Explicitly authorized final production consolidation, inventory before edits, independent initial/final review, targeted cleanup, complete applicable populated ArchUnit enforcement with negative fixtures, focused/fast/full non-PIT PostgreSQL/JaCoCo/OpenAPI verification. Preserve all business, API, schema, mapping, transaction completion/provenance/retry/locking and image lifecycle behavior. ADR-0007 Option B and ADR-0004 govern. Remove bridges only with complete caller evidence and reviewer agreement; retain supported public contracts. No new decisions, generic frameworks, dependencies, migrations, orphan reconciliation, CI/Cloud changes, PIT or Increment 8. Material scope/architecture decisions require human approval.

Phase A evidence and exact bounded cleanup contract Pending specialist handoffs. All implementation acceptance criteria and runtime checks Pending. Human Acceptance Pending.


## Increment 7 Phase A inventory and passed scope safety gate

Independent `/root/reviewer` confirmed INC7-A1–A4 (Medium architectural enforcement/consolidation findings, no demonstrated runtime defect); `/root/implementer` independently found the same paths. Tester mapped existing list/API, provider MANDATORY, locking/fresh retry facts, delete/reference schedules, image lifecycle, read query-count/materialization and placement completion regressions. No blocking new architectural decision. Scope safety gate **Passed** under the explicit Increment 7 authorization; implementation release follows this recorded contract.

| Existing dependency / callers | Minimal mandatory correction | Behavior impact / required evidence |
| --- | --- | --- |
| ProductApplicationService.list → ProductRepository; ProductController.list → concrete facade | Existing facade implements new `ListProductsQuery.list(): List<ProductResult>`; inject input into controller. New output `ProductCatalogReads.findAll(): List<ProductResult>` implemented by existing ProductReadAdapter | Same repository findAll, mapping, active/inactive inclusion, order, readOnly REQUIRED; list/API/adapter and PostgreSQL materialization checks |
| ProductOrderAccess.lock → ProductRepository | Same new output also exposes `lock(UUID): ProductResult`; existing ProductReadAdapter performs identical locked query/missing exception/mapping | Keep provider MANDATORY, same lock and transaction; missing/inactive facts, ambient rejection and placement concurrency/fresh retry checks |
| OrderReferenceQuery → OrderRepository; CustomerOrderReferenceAdapter → concrete provider | New `OrderReferences.references(UUID): boolean` input implemented by existing provider; new `OrderReferenceStore.references(UUID): boolean` output implemented by existing OrderReadAdapter; Customer bridge uses input | Same existsByCustomerId call, MANDATORY provider, distinct statement after Customer lock; delegation, participation, delete/placement schedules |
| Explicit slice manifests omit helpers; generic rule omits transaction/integration adapters; no wiring-cycle enforcement | Add populated architecture-wide enforcement and representative forbidden-dependency/signature/cycle fixtures; keep all existing slice checks and exact approved exceptions | Constructor/component dependency cycles prohibited; intentional Order/OrderItem JPA association and nested callbacks are not constructor wiring cycles |

Exact production ownership `/root/implementer`: modify `product/application/ProductApplicationService.java`, `product/application/ProductOrderAccess.java`, `product/api/ProductController.java`, `product/persistence/ProductReadAdapter.java`, `order/application/OrderReferenceQuery.java`, `order/persistence/OrderReadAdapter.java`, `customer/persistence/CustomerOrderReferenceAdapter.java`; add `product/application/port/in/ListProductsQuery.java`, `product/application/port/out/ProductCatalogReads.java`, `order/application/port/in/OrderReferences.java`, `order/application/port/out/OrderReferenceStore.java` (all under src/main/java/com/example/agenticorderservice). No other production files.

Retain facade and all public methods, Product/Customer provider classes and OrderReferenceQuery: supported direct/static/test and provider consumers remain. StrictJsonRequestDecoder's existing static price/email validation calls remain exact compatibility exceptions. Retain Customer afterCommit observation, Spring transaction annotations, domain JPA metadata and only the three expressly approved same-feature write aggregate exceptions. No bean/configuration/transaction relocation, interface merging, package uniformity rewrite or new entity allowance. Optional facade/validator removal and transaction redesign declined; unrelated orphan reconciliation and historical mutation/Cloud debt remain.

Tester exclusively owns the existing affected application/adapter/API/support tests and architecture suites, new focused provider tests, consolidated production architecture suite and necessary isolated negative fixtures. No production ownership. Orchestrator remains sole record/documentation writer and build sequencer. All builds wait for stable writers and explicit build lease. Counter **0/3**, fresh checks Pending.


### Increment 7 final acceptance evidence

The bounded four-capability correction and complete applicable architecture enforcement are implemented;all supported compatibility delegates/providers remain. Initial inventory/safety gate and fresh final independent review Passed,no remaining actionable finding or blocking decision. Focused303+41,verify-fast777,complete non-PIT777+172PostgreSQL,88architecture,13fast+1runtimeOpenAPI,Spotless and JaCoCo report Passed on222matchingJava source hashes. [Exact39pathmanifest and execution evidence](02-implementation.md#increment-7-exact-changed-file-manifest) and [finalreview/risks](03-review.md#increment-7-final-independent-handoff-and-execution-disposition) establish implementation evidence,not human acceptance. Correction2/3;earlierhistory preserved. No business/API/schema/mapping/transaction/retry/image lifecycle decision changed. Human Acceptance Pending;PIT Deferred,Cloud Pending humancontrol,findings Unknown. Stop after Increment7;no Increment8.


## Increment 8 acceptance contract — 2026-10-21

Authority: explicit human “FINAL VERIFICATION AUTHORIZED — Increment 8” attachment `ca4c32d7-2641-491c-8e9a-6856552035c8/Eingefügter Text.txt`. Verification and documentation only, preserving every accumulated tracked/untracked file. Review cumulative migration against unchanged pre-migration HEAD; distinguish unrelated and ambiguous files. Apply ADR-0007 Option B and retained ADR-0004, exact approved port exceptions and transaction/invariant contracts.

Required: serialized Java 21 `make verify-full SCOPE=all`, complete unit/ArchUnit/PostgreSQL/Spotless/JaCoCo/OpenAPI evidence, full PIT and population-aware mutation disposition against retained historical XML; wrapper/hook smoke checks, configuration/activation distinctions, fresh independent final review, complete classified inventory and final report. No remediation, assertions/rules/selection/threshold/configuration changes, Git or Cloud actions. Missing checks stay Pending; critical unresolved regression blocks recommendation. Human Acceptance Pending. Historical increment counters remain unchanged; Increment 8 has no authorized correction batch.

## Increment 8 final requirements and documentation consistency

All authorized final-verification phases executed; no production/test/rule/configuration remediation. Accepted OptionB and all scoped managed-aggregate/transaction exceptions remain unchanged. Full regression/PIT/source-matched independent review and classified cumulative inventory are documented in the [acceptance report](increment-8-acceptance-report.md). Current architecture-test counts and AcceptedADR label corrected; historical increment evidence/counters preserved.

Target architecture satisfied in reviewed scope; unconditional quality acceptance blocked by unresolved R1Medium/R2Low mutation disposition and required human-controlled Cloud gate. Existing PostgreSQL tests already cover Customer/Product lock-timeout retries; R1 is a fast-suite/PIT-selector limitation. Historical mutation debt is not silently accepted. No business/API/schema/decision change. Remaining human actions and bounded test-only follow-up proposal appear in final report; **Human Acceptance Pending**.


## Increment 8 targeted test-hardening follow-up — 2026-10-21

Authorization: human “Increment8 — Targeted Test Hardening Follow-up”. Investigate R1/R2 before edits; meaningful gaps may receive smallest deterministic regression tests. Both are non-equivalent: Customer-origin provenance affects eligible55P03retry; public Product validator must return validatedMoney. Preserve behavior/assertions,production/rules/mappings/API/schema/dependencies/configuration. Run focused/fast/relevantPG/ArchUnit/Spotless and exact-class PIT using supported profile overrides; never fullSCOPEall. Existing reviewer final assessment mandatory; noGit/Cloud/hooks.


### Targeted follow-up final contract consistency

Both authorized concerns proved meaningful and resolved with two bounded tests;production/business/API/schema/mappings/rules/decisions unchanged. Focused132fast+2PG,fullfast779,88architecture,Spotless andexact2classPIT68(63K/5S)Passed. Narrowtestselection isexplicitlyauthorized,not a changedprojectconfiguration. Earlierfull1050run/JaCoCometrics remainhistorical;no fabricatednewglobalresult. Originalfindings preserved;newtechnicalresolution/evidence appended tofinalreport/mutationanalysis. Existingreviewer finalassessment recordedin03;historicalsurvivors/Cloud/hook/humanacceptancelimits remain.
