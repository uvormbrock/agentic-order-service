# Change Requests

Track maintenance and feature changes separately from the established [implementation prompt playbook](../prompts/00-index.md). Numbered prompts 00–07 retain their filenames, content and historical evidence.

## Convention and lifecycle

This mandatory convention applies to every future task explicitly initiated or classified as a Change Request against the existing application, including new features within it. It does not automatically apply to independent projects, general research, ordinary questions, standalone documentation discussions or tasks not authorized as Change Requests. If classification is genuinely unclear and would materially change the workflow, request clarification before creating records.

Use [CH-001](CH-001-customer-delete-semantics/README.md) as the structural reference. Preserve its historical records without modification or retrofitting these expanded responsibilities.

```text
docs/04-agentic-coding/change-requests/
└── CH-NNN-short-descriptive-slug/
    ├── README.md
    ├── 01-documentation.md
    ├── 02-implementation.md
    └── 03-review.md
```

Inspect both the register and existing directories; use the next available sequential CH-NNN identifier after the highest existing identifier. Never reuse an identifier or renumber an existing request. Honor the explicit CH-002 Product Images reservation below when allocating a future request. Use a concise, lowercase, hyphen-separated descriptive slug. Create exactly the four standard Markdown files initially, preserving their exact filenames and numbering. Do not create an alternative directory structure. Additional evidence files may be added within the same directory only when genuinely necessary; list their purpose and never substitute them for a standard file.

| File | Required responsibilities |
| --- | --- |
| `README.md` | Change Request identification and title; objective and business motivation; scope and exclusions; authority links; current execution status; links to the other three documents; human acceptance status. |
| `01-documentation.md` | Requirements analysis; acceptance criteria; affected contracts and documentation; target versus current behavior; architectural decisions or blockers; documentation changes and evidence. Preserve original prompt provenance and label revisions. |
| `02-implementation.md` | Implementation plan and scoped authorization; modified components and files; implementation decisions within approved scope; test implementation and execution evidence; verification results and technical limitations. |
| `03-review.md` | Independent review findings with severity and resolution status; Quality Gate results, including SonarQube where applicable; correction iteration history (maximum three after initial implementation, or a narrower authorized limit); final verification evidence; outstanding risks and blockers; final human acceptance, initially **Pending**. Include the orchestration task ledger. |

Initialize required sections with explicit Pending or not-yet-performed status, not invented results or approvals. Explain Not Applicable results. Keep records complete and synchronized throughout execution; preserve earlier evidence and link detailed results rather than duplicating conflicting copies.

The lightweight lifecycle is Proposed → business scope approved → Phase 1 documentation recorded → Phase 2 authorized → implemented and verified → human reviewed and accepted. Record blockers or revision requests where they occur; do not advance a phase without evidence. Documentation approval, implementation authorization and final human acceptance are separate events. Unrun checks remain Pending. Preserve original prompts verbatim when available and label later revisions; record missing history explicitly.

Follow [AGENTS.md](../../../AGENTS.md). PRD and accepted architecture decisions retain authority; a change record neither duplicates the specification nor accepts Proposed ADRs. Record actual evidence and limitations without rewriting earlier acceptance records. Git publication actions require separate explicit authorization.

## Register

| Request | Current status |
| --- | --- |
| [CH-001 — Customer DELETE Semantics](CH-001-customer-delete-semantics/README.md) | Business target approved; Phase 2 explicitly authorized and implemented; verification in the review record; human acceptance Pending |
| CH-002 — Product Images | Reserved for the future feature; no request directory created and no implementation authorized here |

SonarQube integration belongs to the Multi-Agent Framework setup, not the business Change Request sequence. Its mistakenly numbered records are preserved as [setup evidence](../multi-agent/sonar-setup-evidence/README.md). This classification correction releases CH-002 for Product Images and does not renumber CH-001 or reserve any other number.
