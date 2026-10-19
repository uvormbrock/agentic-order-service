# Approval and Safety Policy

Root [AGENTS.md](../../../AGENTS.md) governs every role. Delegation cannot enlarge human authorization or runtime permissions. The Orchestrator records specific human authorization and its exclusions before edits. A user explicitly authorizing a defined Change Request's full lifecycle need not approve the same scoped work repeatedly; documentation approval alone is insufficient.

Stop dependent work and present the exact decision, affected files/behavior and proposed resolution when any of these lack explicit authorization:

- Architecture changes or blocking Open/Proposed ADR decisions, including unresolved subdecisions inside an otherwise Accepted baseline.
- New dependencies, migrations or schema changes. A future request may explicitly authorize a defined already-approved migration; record that authorization and preserve applicable safety requirements.
- Scope expansion, destructive actions or deletion of existing user data.
- External publication/deployment; Git staging, committing, pushing or merging; pull request creation under this workflow.

Never silently promote ADR status, suppress/accept security findings, invent credentials, or weaken tests. Maintain transaction integrity, commit/outcome provenance and safe logging as required by AGENTS.md. The architecture agent recommends decisions; only humans accept them. Available automated approval review grants tool access, not business/architecture acceptance.

Final Change Request acceptance always requires an explicit human decision recorded with reviewer/date/revision. Automated gates and independent agent review cannot grant it. Finish at Pending human review. No agent may bypass a blocker by asking another agent to execute the action.

Sandbox defaults are not an unattended-execution guarantee. Continue authorized permitted work; report approval/network/credential/tool failures as blockers or Pending gates. Never relax permissions, change global settings, install tooling or trigger remote workflows to avoid a blocker without explicit authorization. A framework setup does not authorize any future business change.

`make sonar-local` uploads analysis of the local working tree to SonarQube Cloud and requires Change Request authorization covering that external analysis. It needs no Git publication. Existing environment or macOS Keychain credentials may be used only for an authorized run; never expose them or bypass denied/interactive Keychain access. Tooling setup and mock validation do not authorize a real scan or credential retrieval. Keep missing access/results Pending and preserve final human acceptance.

## Read-only SonarQube findings

`make sonar-findings` uses existing authentication only when Cloud retrieval is authorized. It does not authorize analysis submission, credential creation, project-setting changes or issue/hotspot acceptance. Setup and mocked verification must not retrieve credentials or call Cloud APIs. Failed retrieval does not authorize an automatic `make sonar-local` rerun.
