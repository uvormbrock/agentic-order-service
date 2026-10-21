# Hooks — Inventory, Responsibilities and Activation

**Scope:** CH-003 Increment 0, 2026-10-21. Prepared configuration is not human acceptance or migration authorization.

## Inventory before changes

- Project .codex contained config.toml, six native role files and the pre-existing hexagonal skill; no project lifecycle hook configuration.
- .git/hooks contained only Git's *.sample files. No active pre-commit/pre-push, configured core.hooksPath or repository hook manager was found. No global hook setting was changed.
- Existing Maven Spotless check runs in validate; Surefire runs *Test (including ArchUnit), Failsafe runs *IT in verify, and JaCoCo reports at verify. These are Maven checks, not hooks.
- .github/workflows/build.yml runs Java 21 clean verify plus SonarScanner on its configured triggers. Its qualitygate.wait=false does not establish a passed remote gate. It is a CI workflow, not a local hook.
- Existing ArchitectureComplianceTest supplies eight checks; neither the class nor pom.xml changed.
- Installed Codex CLI 0.151.0 reports hooks and multi_agent stable/enabled. Runtime listing additionally exposes pre-existing bundled browser/computer-use plugin hooks; these are outside this repository, were untrusted in the probe and were not edited or trusted.

## Prepared mechanisms

| Mechanism | Event / entry point | Responsibility | Validation / activation boundary |
| --- | --- | --- | --- |
| Native architecture reminder | PreToolUse, matcher .*; .codex/hooks/architecture_reminder.py | Return short context only when serialized tool input names src/main/java and a .java path; reference AGENTS.md, current architecture/ADRs and installed skill | Advisory; path heuristic may remind on reads and miss computed/opaque paths. Never a write guard or permission grant |
| Native handoff check | SubagentStop, matcher implementer/tester/reviewer; .codex/hooks/handoff_check.py | Read required labels from the existing handoff template and check nonempty fields | Shape only, no truth/approval check; request one completion pass, then warn on stop_hook_active to prevent loops |
| Prepared Git pre-commit | .githooks/pre-commit, inactive | For staged Java changes run one Java 21 clean test with only ArchitectureComplianceTest selected | Explicit approval required before local activation; no staging, stashing or automatic formatting |
| Comprehensive gates | Explicit Maven/CI commands | Full regression, disposable PostgreSQL, coverage, applicable mutation and authorized external analysis | Never automatically attached to commit, tool use or agent stop; no pre-push hook prepared |

Native definitions live in [.codex/hooks.json](../../../.codex/hooks.json), with five-second command timeouts. Commands resolve from git root, so starting Codex in a nested directory is supported. Python 3 and Git must be available. Errors/malformed input emit safe diagnostics without printing tool inputs/transcripts; they do not establish approval. No transcript is read or persisted by these scripts.

Hook B does not duplicate the [handoff contract](handoff-contract.md): it derives top-level labels from the authoritative template. Nonempty placeholder/false values can still pass; only independent review and Orchestrator evidence validation establish substantive completeness. If runtime hooks are skipped or unavailable, the same mandatory handoff workflow remains in force.

## Runtime evidence and limits

The installed native app-server initialize and config/read succeeded with strict configuration loading. Effective agents.enabled=true and max_concurrent_threads_per_session=3 were observed. hooks/list discovered the two project events with no errors or warnings; both are enabled definitions but trustStatus=untrusted. Enabled does not mean executable: no hook trust was granted, and actual PreToolUse/SubagentStop lifecycle delivery remains **Pending**. Do not bypass trust to claim a passing event test.

Run handler/command smoke checks without activation:

```sh
python3 -B .codex/hooks/test_hooks.py
.githooks/pre-commit
```

Smoke tests use synthetic event payloads and mocked Git/Maven/JDK collaborators; they do not stage fixture files or execute Git commits. Native command strings are also invoked directly from a nested cwd. This establishes handler behavior and path resolution, not lifecycle triggering. The real pre-commit invocation currently skips because no Java files are staged. A separate explicit Java 21 focused clean test validates the Maven check; see [CH-003 review evidence](../change-requests/CH-003-hexagonal-architecture-migration/03-review.md).

The Git hook refuses unstaged/untracked build inputs under src, pom.xml, .mvn and the wrappers. Otherwise Maven would test worktree code rather than the staged snapshot. It performs no automatic reconciliation. JAVA_HOME must select Java 21; on macOS an unset value uses /usr/libexec/java_home -v 21, then validates the major version. On other systems set JAVA_HOME explicitly. It propagates Maven failure unchanged. clean removes stale classes; test already invokes Spotless at validate, so no duplicate standalone formatting check. Dependencies may need initial download; expect seconds locally, with cold-cache/network variation. It does not run PostgreSQL ITs, PIT or SonarQube.

## Optional activation — not performed

Native hooks: in a trusted project session, inspect /hooks and review/trust the exact two definitions. Do not trust unrelated user/plugin hooks as part of this increment. Changes to definitions require renewed trust. After activation, exercise a safe path-bearing tool event and a specialist-stop event, check native hook-start/completion notifications and model-visible results, and record source hashes/status. Until then event triggering stays Pending. User/system policy can still disable hooks.

Git hook: only after separate human approval, inspect any existing local core.hooksPath first; do not overwrite a user's hook manager. From repository root the intended local activation command is:

```sh
git config --local core.hooksPath .githooks
```

This affects future commits in this checkout, can reject partial-staging/build-input mismatches and runs the two existing Java checks through one Maven lifecycle. It does not stage files or authorize commits. To undo a newly introduced setting, restore the exact previously recorded local value or remove only this increment's setting when no prior value existed. Do not change global configuration. Validate actual Git-triggered behavior in a separately authorized disposable fixture or future authorized commit; no commit is authorized here.

## Sources and authority

[Official native hook API/trust documentation](https://learn.chatgpt.com/docs/hooks) and [native agent configuration](https://learn.chatgpt.com/docs/agent-configuration/subagents) checked 2026-10-21. Installed schema/runtime discovery is recorded separately from documentation claims. [AGENTS.md](../../../AGENTS.md), accepted architecture decisions, human scope, quality gates and independent review remain authoritative; hooks cannot accept ADR-0007 or start Increment 1.

## Option B / local verification follow-up — 2026-10-21

ADR-0007 Option B is now human Accepted and the reminder text reflects that status; production migration still requires separate authorization. Hook definitions/events, trust and pre-commit behavior are unchanged. Fresh native discovery again returned no errors/warnings and both project definitions untrusted. The focused 21-case hook suite passes; event triggering remains Pending. The new [fast/full workflow](../../05-testing/engineering-verification.md) is explicitly invoked by the Orchestrator at stable milestones, never from lifecycle/commit hooks. No core.hooksPath or trust changes were made. Separate developer action remains /hooks review/trust for native definitions and explicit approval before any local Git hook activation.
