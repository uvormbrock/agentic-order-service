# Agentic Coding Prompt Playbook

## Purpose

Maintain agreed prompts and their execution evidence in Git alongside the project documentation. Obsidian and Codex read the same repository files. This playbook is an execution record, not an architecture specification or approval to perform a task.

## Naming and Structure

Use sequential two-digit prefixes and a descriptive lowercase filename: `NN-task-description.md`. `00-index.md` is navigation; task prompts begin at `01`. Keep established filenames stable for traceability. Create a prompt file only after its actual text is agreed; do not generate speculative future entries.

Each prompt document contains Objective, Context, Prompt, Result, and Lessons Learned. Preserve the agreed prompt verbatim, preferably inside a fenced text block. Record later revisions explicitly rather than silently rewriting executed history.

## Status and Evidence

- **Planned:** Prompt has not been executed. Do not fabricate results or lessons learned.
- **Executed:** Record the actual execution, outcome, evidence, and limitations; execution does not imply human acceptance or completion.
- Record human acceptance separately where required by the project workflow. Failed or incomplete execution must be described honestly.

Results and lessons remain pending until supported by actual execution. Interview preparation is outside the maintained playbook structure; references inside immutable historical prompts remain historical instructions.

## Current Entry

| Sequence | Prompt                                                                         | Status                              |
| -------- | ------------------------------------------------------------------------------ | ----------------------------------- |
| 01       | [Environment and bootstrap readiness assessment](01-environment-assessment.md) | Executed                            |
| 02       | [Technical bootstrap – Increment 1](02-technical-bootstrap.md)                 | **Reviewed – Accepted** |
| 03 | [PostgreSQL foundation – Increment 2](03-postgresql-foundation.md) | **Reviewed – Accepted** |
| 04 | [Domain Model & Persistence – Increment 3](04-domain-model.md) | **Reviewed – Accepted** |
| 05 | [Product & Customer Application Services and REST API – Increment 4](05-product-customer-api.md) | **Reviewed – Accepted** |
| 06 | [Order Placement & Concurrency – Increment 5](06-order-placement-concurrency.md) | **Reviewed – Accepted — 2026-10-16** |

## Authority and Workflow

Prompts do not change Accepted decisions, resolve Open questions, promote Proposed ADRs, or authorize code/migrations/commits. Requirements and uniquely numbered ADRs retain authority. Follow [AGENTS.md](../../../AGENTS.md), the [AI workflow](../ai-development-workflow.md), and the [system architecture](../../02-architecture/system-architecture.md). Storing a prompt and executing it are separate actions; observe the user's authorized scope.

Increment 4 has one executable contract: [Current Implementation Prompt](05-product-customer-api.md#current-implementation-prompt--increment-4), explicitly authorized and executed; [final human acceptance](05-product-customer-api.md#final-human-acceptance--2026-10-15) records **Reviewed – Accepted** on 2026-10-15. Historical prompts/sketches are non-executable; persistent local migration has its own target/recovery safety gate, separate from disposable Testcontainers verification.

Increment 5 G1–G6 are [Human Accepted](06-order-placement-concurrency.md#final-human-architecture-decisions--2026-10-16), including four endpoints and Customer DELETE; original proposals remain historical. The [implementation prompt](06-order-placement-concurrency.md#current-implementation-prompt--increment-5) was explicitly authorized and executed; its pre-authorization wording is preserved as history. See the appended execution evidence. The [final human acceptance](06-order-placement-concurrency.md#final-human-acceptance--2026-10-16) records Reviewed – Accepted on 2026-10-16. Complete ADR-0002/0006 remain Proposed; remote Sonar/CI and operational gates remain separate.
