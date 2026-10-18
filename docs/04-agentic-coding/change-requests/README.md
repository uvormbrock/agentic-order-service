# Change Requests

Track maintenance and feature changes separately from the established [implementation prompt playbook](../prompts/00-index.md). Numbered prompts 00–07 retain their filenames, content and historical evidence.

## Convention and lifecycle

Use `CH-NNN-short-description`: a unique, sequential three-digit ID and a lowercase, hyphen-separated description. Never reuse an ID or renumber an existing request. Each directory contains:

- `README.md`: motivation, approved scope, authority links and current status.
- `01-documentation.md`: Phase 1 prompt provenance and documentation outcome.
- `02-implementation.md`: Phase 2 execution prompt and authorization status.
- `03-review.md`: implementation, verification and human review evidence.

The lightweight lifecycle is Proposed → business scope approved → Phase 1 documentation recorded → Phase 2 authorized → implemented and verified → human reviewed and accepted. Record blockers or revision requests where they occur; do not advance a phase without evidence. Documentation approval, implementation authorization and final human acceptance are separate events. Unrun checks remain Pending. Preserve original prompts verbatim when available and label later revisions; record missing history explicitly.

Follow [AGENTS.md](../../../AGENTS.md). PRD and accepted architecture decisions retain authority; a change record neither duplicates the specification nor accepts Proposed ADRs. Record actual evidence and limitations without rewriting earlier acceptance records. Git publication actions require separate explicit authorization.

## Register

| Request | Current status |
| --- | --- |
| [CH-001 — Customer DELETE Semantics](CH-001-customer-delete-semantics/README.md) | Business target approved; Phase 2 explicitly authorized and implemented; verification in the review record; human acceptance Pending |
