# Agentic Coding Prompt Playbook

## Purpose

Maintain agreed prompts and their execution evidence in Git alongside the project documentation. Obsidian and Codex read the same repository files. This playbook is an execution record, not an architecture specification or approval to perform a task.

## Naming and Structure

Use sequential two-digit prefixes and a descriptive lowercase filename: `NN-task-description.md`. `00-index.md` is navigation; task prompts begin at `01`. Keep established filenames stable for traceability. Create a prompt file only after its actual text is agreed; do not generate speculative future entries.

Each prompt document contains Objective, Context, Prompt, Result, Lessons Learned, and Interview Talking Points. Preserve the agreed prompt verbatim, preferably inside a fenced text block. Record later revisions explicitly rather than silently rewriting executed history.

## Status and Evidence

- **Planned:** Prompt has not been executed. Do not fabricate results or lessons learned.
- **Executed:** Record the actual execution, outcome, evidence, and limitations; execution does not imply human acceptance or completion.
- Record human acceptance separately where required by the project workflow. Failed or incomplete execution must be described honestly.

Interview talking points may describe intended methodology while Planned, but must not claim observed outcomes. Results and lessons remain pending until supported by actual execution.

## Current Entry

| Sequence | Prompt                                                                         | Status                              |
| -------- | ------------------------------------------------------------------------------ | ----------------------------------- |
| 01       | [Environment and bootstrap readiness assessment](01-environment-assessment.md) | Executed                            |
| 02       | [Technical bootstrap – Increment 1](02-technical-bootstrap.md)                 | **Reviewed – Accepted** |

| 03 | [PostgreSQL foundation – Increment 2](03-postgresql-foundation.md) | **Reviewed – Accepted** |

## Authority and Workflow

Prompts do not change Accepted decisions, resolve Open questions, promote Proposed ADRs, or authorize code/migrations/commits. Requirements and uniquely numbered ADRs retain authority. Follow [AGENTS.md](../../../AGENTS.md), the [AI workflow](../ai-development-workflow.md), and the [system architecture](../../02-architecture/system-architecture.md). Storing a prompt and executing it are separate actions; observe the user's authorized scope.
