# AI Development Workflow

**Status:** Approved process; implementation requires separate scoped approval

## Read and Establish Authority

Start with root [AGENTS.md](../../AGENTS.md), then [system architecture](../02-architecture/system-architecture.md), the [PRD](../01-requirements/product-requirements.md), relevant ADRs, [coding guidelines](coding-guidelines.md), and [testing strategy](../05-testing/testing-strategy.md). Check current working-tree changes before editing. Docs in this repository are authoritative and Git-versioned; Obsidian uses those same files.

## Controlled Increments

1. Read requirements/decisions and identify Accepted, Proposed, Open, and implementation evidence separately.
2. Present a small concrete plan, files affected, decisions required, and verification criteria.
3. Obtain explicit human approval of the implementation plan before coding unless the exact scope is already approved. Documentation approval alone does not authorize implementation.
4. Implement only that increment; preserve unrelated edits and decision history. Do not add speculative frameworks or abstractions.
5. Run applicable automated verification and self-review the diff. Report future or unavailable gates as Pending, never Passed; include failures and limitations.
6. Update authoritative documents first for explicitly approved decision changes, then indexes. Do not create duplicate specifications or reuse ADR numbers.
7. Present the actual Git diff, including staged, unstaged, and new-file changes, and verification results for human review.
8. Wait for explicit human acceptance before considering the implementation increment complete. Agent self-review does not replace human review.
9. Create a Git commit only with explicit authorization; acceptance of the increment does not itself authorize a commit.

## Decision and Approval Rules

Accepted requirements and architecture govern behavior; change them only with explicit human approval and an authoritative decision record preserving history. Proposed details are not silently promoted. Ask about genuinely unresolved business or architecture decisions; do not repeatedly reopen accepted choices without a concrete incompatibility. ADR acceptance is a human decision, separate from test verification. Keep ADR-0002 Proposed until explicit approval.

No autonomous database migration, source generation, dependency addition, or architecture change follows from this documentation task. Implementation stages are listed in the [plan](../07-implementation/implementation-plan.md). JUnit 6 replaces the earlier JUnit 5 selection under ADR-0005. Its Boot 4.1.1-managed Jupiter/Platform versions are required; actual test-build verification remains Pending.

## Review Record

For each increment report changed files, why, gates run/results, remaining risks, and the Git diff. Link artifacts rather than copying whole documents into chat. Preserve the original ADR history when relocating files; the numbered documentation directories remain the single authority; do not recreate parallel hierarchies.

## Version-Controlled Prompts

The [prompt playbook](prompts/00-index.md) records agreed prompts and real execution evidence. Prompt storage does not authorize execution or replace requirements/ADRs. Keep unexecuted prompts Planned and do not fabricate results or lessons learned.
