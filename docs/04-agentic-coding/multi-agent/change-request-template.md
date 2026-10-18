# Change Request Invocation

Copy into a fresh trusted-project chat and replace the placeholders with concrete scope. This template is not authorization until a human submits it for an actual defined request.

```text
Execute the Multi-Agent Change Request workflow in
 docs/04-agentic-coding/multi-agent/README.md.
Use the main session as Orchestrator; delegate meaningful specialist work
and independent review to actual sub-agents. Wait for dependent handoffs.

Change Request: <description and intended outcome>
Authorized scope: <features, behavior, interfaces and exclusions>
Acceptance criteria: <observable requirements, or authorize derivation
within the defined scope; stop for genuinely blocking ambiguity>
Branch: <explicit branch>
Apply the mandatory convention in
 docs/04-agentic-coding/change-requests/README.md.
Inspect the register and directories; use the next unused sequential CH-NNN
and a concise lowercase hyphen-separated slug. Initially create exactly
README.md, 01-documentation.md, 02-implementation.md and 03-review.md.
Preserve those filenames; add evidence files only when genuinely necessary.
Maintain all four records throughout the lifecycle using workflow.md's role
contributions and one assigned writer per file. Initialize human acceptance
as Pending; record only actual verification and explicit human decisions.
Do not alter historical CH-001 records or use an alternative structure.
Specifically approved exceptional work: <none, or exact already-approved
architecture decision/dependency/migration and human authorization reference>

I authorize requirements analysis, scoped target documentation,
implementation, tests, independent review, documentation synchronization
and available verification for this defined scope under AGENTS.md.
Use no more than three correction iterations after initial implementation;
preserve the counter across agents and restarts.
Stop dependent work for any missing mandatory approval, unresolved blocking
ADR decision, migration/dependency/architecture change outside the specific
authorization, scope expansion, destructive action or external publication.
Do not stage, commit, push, merge, create a pull request or deploy.
Do not dispatch remote workflows without separate authorization.
Stop with the final report and human acceptance Pending.
```

The Orchestrator assigns the next unused CH-NNN using the [existing convention](../change-requests/README.md), records the original invocation and explicit authorization, and derives a bounded plan. A short business description never silently approves every architectural or schema consequence. See [approval policy](approval-policy.md). Do not execute the product-image example from the setup request or CH-002 as part of setup.

This template applies only to tasks explicitly initiated or classified as Change Requests against the existing application; new application features may qualify. Independent projects, research, questions, standalone documentation discussions and tasks not authorized as Change Requests do not automatically use it. Clarify genuinely ambiguous classification when it would materially change the workflow.
