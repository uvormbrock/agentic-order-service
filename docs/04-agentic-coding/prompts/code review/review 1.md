# Increment 4 — Independent Deep Code Review and Test Effectiveness Audit

## Role

Act as a skeptical senior Java architect, code reviewer, and test-quality engineer.

The Agentic Order Service Increment 4 implementation is complete. Maven verification and SonarQube quality analysis have passed.

Your task is to independently determine whether the implementation is correct and whether the automated tests genuinely detect defects.

**This is a strictly read-only audit. Do not modify any repository files, tests, documentation, configuration, or SonarQube issue statuses. Do not commit or push.**

Do not assume correctness merely because you implemented the code or because all tests pass.

## 1. Establish the Baseline

Read:

- AGENTS.md
    
- The Current Implementation Prompt — Increment 4
    
- Accepted REST API design
    
- API error catalog
    
- Product/Customer domain model
    
- ADR-0006
    
- Testing strategy
    

Inspect the actual production code and all existing relevant tests, including Increment 1–3 regression tests affected by Increment 4.

Record the exact Git commit and current working-tree state.

Use actual source files as evidence. Do not infer behavior from class names or documentation alone.

## 2. Complete Production Code Review

Review all production code added or modified by Increment 4.

Verify:

- Exactly ten supported Product/Customer business endpoints.
    
- No forbidden additional business operations.
    
- Correct controller/application/domain/persistence boundaries.
    
- Correct dependency direction.
    
- No JPA entity exposure through REST.
    
- Correct immutable result materialization.
    
- Valid transaction boundaries and proxy invocation.
    
- No success response before successful commit.
    
- Correct uniqueness-conflict translation.
    
- Correct optimistic-lock exception handling.
    
- Correct 500 versus 503 classification.
    
- No unintended state mutation on failed requests.
    
- No automatic write retries.
    
- Correct idempotent deactivation semantics.
    
- Correct Flyway V2 mapping and schema compatibility.
    
- Correct HTTP error rendering, including servlet dispatch.
    
- Preservation of Increment 3 domain invariants.
    

Identify correctness risks, concurrency problems, accidental complexity, hidden assumptions, or maintainability defects.

Do not recommend cosmetic refactoring without a concrete benefit.

## 3. Review Every Relevant Test

Inspect the actual test bodies and assertions, not merely test names or coverage reports.

For each test, determine:

1. Which concrete requirement it verifies.
    
2. What input and initial state it establishes.
    
3. What production behavior it exercises.
    
4. Which observable output or committed state it asserts.
    
5. Whether the assertion would fail if the behavior were wrong.
    
6. Whether mocks, fixtures, or helper methods could mask a defect.
    
7. Whether the test is deterministic and independently repeatable.
    
8. Whether it contributes unique coverage or duplicates another test.
    

Pay particular attention to shared test helpers such as ApiTestSupport, PostgreSqlApiTestSupport and SQL inspectors.

Classify weak tests explicitly:

- Assertion-free or indirectly asserted without adequate verification.
    
- Trivial assertions.
    
- Assertions that merely repeat implementation logic.
    
- Tests that only check HTTP status but not response content or state.
    
- Mocks configured to produce the expected result without testing the relevant logic.
    
- Exception tests that do not verify the correct failure category.
    
- Tests relying on execution order, shared mutable state, or test-container contamination.
    
- Tests that could pass despite a missing or incorrect database mutation.
    
- Tests that never actually exercise the intended branch.
    

Do not count the mere execution of a line as proof of test effectiveness.

## 4. Requirement-to-Test Traceability

Build a traceability matrix covering all accepted Increment 4 requirements.

At minimum include:

### REST contract

- All ten endpoints.
    
- Success HTTP statuses.
    
- Response fields and generated identifiers.
    
- Location and Allow headers.
    
- Listing active and inactive resources.
    
- Empty lists.
    
- Missing and malformed resource IDs.
    
- Deferred Customer DELETE behavior.
    

### Strict JSON and validation

- Absent versus whitespace-only body.
    
- Null, array, and empty-object roots.
    
- Duplicate JSON keys, including nested objects.
    
- Trailing JSON values and garbage.
    
- Unknown versus recognized forbidden fields.
    
- Wrong JSON scalar types.
    
- Missing and explicit null required values.
    
- Fixed error-category precedence.
    
- Deterministic same-category field precedence.
    
- JSON property-order permutations.
    
- Uppercase/lowercase canonical UUID parsing.
    
- Content-Type and Accept restrictions.
    
- UTF-8 enforcement.
    
- Numeric-token precision and exponent notation.
    
- Name/email Unicode, normalization, grammar, and boundaries.
    
- Exact safe code/message error envelopes.
    

### Persistence and transactions

- Generated UUID identity.
    
- Database uniqueness across active/inactive customers.
    
- Updating the existing owner's normalized email.
    
- Commit-time uniqueness exceptions.
    
- Product/Customer @Version behavior.
    
- V1-to-V2 migration with existing records.
    
- Fresh V1+V2 migration.
    
- Preserved database constraints and Flyway history.
    
- Rollback after confirmed failures.
    
- No unsupported rollback guarantee after ambiguous completion.
    
- No database access during REST serialization.
    

### Concurrency

For Product and Customer independently:

- PUT versus PUT.
    
- PUT versus PATCH.
    
- PATCH versus PUT.
    
- PATCH versus PATCH.
    
- Stale update after committed update.
    
- Stale update after committed deactivation.
    
- Sequential repeated PATCH without SQL UPDATE or version increment.
    
- Verified winning transaction state.
    
- Verified rejected transaction state.
    
- Proper independent transactions/persistence contexts.
    

For every requirement classify evidence as:

- Strong: a meaningful test would detect an incorrect implementation.
    
- Partial: some behavior is checked, but a significant defect could survive.
    
- Missing: no relevant test exists.
    
- Unverified: the test's effectiveness cannot be established from the evidence.
    

Include exact test names and source-line references.

## 5. Concurrency Test Credibility Audit

Examine the concurrency tests in depth.

Determine whether they actually produce controlled overlapping operations.

Verify:

- Independent transactions and entity managers.
    
- Real PostgreSQL concurrency, not mocked conflicts.
    
- Explicit synchronization using latches/barriers where needed.
    
- No arbitrary sleep-based synchronization.
    
- Bounded waits with meaningful failure conditions.
    
- Deterministic or explicitly justified outcomes.
    
- Correct optimistic-lock version predicates.
    
- Fresh reads after transaction completion.
    
- No test-wide rollback hiding commit failures.
    

An optimistic-lock test must prove that stale concurrent writes are rejected when applicable, not merely that @Version exists.

Distinguish a true simultaneous-write scenario from a sequential stale-entity test.

Report any tests whose names or descriptions promise stronger concurrency evidence than they actually provide.

## 6. Adversarial Test Effectiveness Analysis

For the ten highest-risk behaviors, identify one realistic implementation defect that could be introduced.

Examples:

- Accepting an exponent-form JSON price.
    
- Accepting duplicated JSON properties.
    
- Reversing error precedence.
    
- Ignoring the normalized-email uniqueness constraint.
    
- Returning success before transaction completion.
    
- Failing to detect a stale version.
    
- Performing an SQL UPDATE on repeated deactivation.
    
- Exposing a version field in an API response.
    
- Returning 503 for an ambiguous commit outcome.
    
- Failing to preserve inactive status during PUT.
    

For each proposed mutation:

1. Identify the exact production method and location.
    
2. Describe the smallest defect-producing change.
    
3. Identify the existing test expected to fail.
    
4. Explain which assertion should fail.
    
5. State whether the current test genuinely protects that behavior.
    

**Do not apply mutations in this phase.**

Produce a ranked mutation-testing plan for subsequent human-approved execution.

Do not claim a mutation is detected without actually running it.

## 7. Review Coverage Quality

Inspect available JaCoCo XML and HTML reports.

Distinguish:

- Overall coverage.
    
- New-code coverage.
    
- Line coverage.
    
- Branch coverage.
    
- Coverage of critical validation/error/transaction code.
    

Identify uncovered or partially covered high-risk branches.

Do not recommend achieving arbitrary coverage percentages merely for the metric.

Prioritize uncovered branches that represent meaningful business or technical risks.

## 8. Findings and Severity

Classify findings as:

- Critical — serious correctness, data integrity, or security defect.
    
- High — important functional defect or materially ineffective test.
    
- Medium — meaningful testing gap or maintainability risk.
    
- Low — limited impact improvement.
    

For every finding provide:

- Unique identifier.
    
- Severity.
    
- Exact file and line.
    
- Expected behavior from authoritative documentation.
    
- Actual behavior or test limitation.
    
- Evidence from inspected source.
    
- Concrete failure scenario.
    
- Smallest recommended correction.
    
- Required regression test.
    

Clearly distinguish confirmed defects from plausible risks and unverified hypotheses.

Do not invent findings to fill a quota.

Do not report speculative problems as established facts.

## 9. Audit Independence and Restrictions

This audit must challenge the existing implementation rather than defend it.

Do not:

- Modify production code or tests.
    
- Modify documentation.
    
- Modify pom.xml, CI, SonarQube, or JaCoCo.
    
- Execute migrations on persistent databases.
    
- Change ADR statuses.
    
- Add dependencies.
    
- Commit, push, or merge.
    
- Mark issues as accepted or false positive.
    
- Begin implementing fixes.
    

Read-only inspection and non-mutating verification commands are permitted.

If a verification command requires writing to a persistent development database or changing repository files, do not run it.

## 10. Required Report

Produce:

1. Executive assessment of Increment 4.
    
2. Production-code review findings.
    
3. Requirement-to-test traceability matrix.
    
4. Weak or ineffective tests with exact evidence.
    
5. Missing edge-case and negative tests.
    
6. Concurrency-test credibility assessment.
    
7. Coverage and high-risk uncovered branches.
    
8. Ranked mutation-testing plan.
    
9. Severity-ranked remediation recommendations.
    
10. Residual risks and audit limitations.
    

Explicitly report:

- Number of production classes reviewed.
    
- Number of relevant test classes reviewed.
    
- Number of test methods inspected.
    
- Number of accepted requirements mapped.
    
- Counts of Strong, Partial, Missing and Unverified evidence.
    
- Counts of findings by severity.
    

Do not equate the number of green tests with correctness.

Finish with:

**Increment 4 — Independent Code and Test Effectiveness Audit Complete; Pending Human Review**

Wait for human review and separate authorization before making corrections.