# Mutation Experiment Results

**Status:** Mutation Testing Worktree & Evidence Storage – Pending Human Review

## Purpose and Navigation

This index locates actual experiment reports inside the main `develop` worktree's registered `docs/` Obsidian vault. Open `05-testing/mutation-results/00-index.md` from Obsidian's file explorer, search or the mutation-testing navigation. GitHub reads the same Markdown files; no second vault or duplicate report copy is maintained.

- [Testing strategy](../testing-strategy.md): existing quality gates and the distinction between tests, coverage, static analysis and mutation evidence.
- [Mutation-testing strategy](../mutation-testing.md): methodology, proposed scope, interpretation and approval boundaries.
- [Operational runbook](../mutation-testing-runbook.md): authoritative commands, safe restoration, evidence template and the proposed persistent storage/migration procedure.

## Reports

No completed experiment reports exist. MUT-001 has setup evidence only: no mutation has been performed and no test result is confirmed. This index deliberately contains no placeholder report links or fabricated outcomes. Create `MUT-NNN.md` only after an experiment has actual outcomes, then add a relative link to the existing report and label its execution/review state honestly. A completed run and a human-accepted result are distinct states; interrupted or inconclusive execution must not be presented as Passed.

Each report records its unique ID/date, authorization, original accepted baseline and experiment branch, requirement, exact original/mutated expressions, unchanged test selection, baseline/mutant outcomes and relevant assertions, classification and analysis, restoration/final verification, human-review decision, raw-evidence paths, separately approved test improvements and remaining decisions. Follow the runbook template rather than duplicating the procedure here. Preserve original-suite outcomes separately from improved-suite results.

## Two Evidence Layers

| Layer | Location | Discoverability and protection |
| --- | --- | --- |
| Readable analysis | Main `agentic-order-service/docs/05-testing/mutation-results/MUT-NNN.md` on develop | Normal Obsidian/GitHub navigation. Eligible for Git versioning only after human review and explicit commit authorization; never commit fabricated or unreviewed findings. |
| Technical artifacts | Sibling `agentic-order-service-mutation-evidence/MUT-NNN/` | Persistent local files outside both main and experiment worktrees; not a Git repository or worktree. Logs, exit codes, environment/test reports, mutation patches, attempts and restoration evidence stay traceable through report paths. No artifacts are created by this index. |

Raw evidence belongs in the external sibling directory, not `.mutation-artifacts/` in either worktree. No `.gitignore` change is required. Git does not version or back up that ordinary filesystem directory, and local persistence does not protect against disk loss or cleanup. The human must approve an independent durable backup destination, owner and retention policy; copy complete artifacts, verify inventory/SHA-256 contents and record backup verification in each report. Archival remains Pending until that evidence exists; migration verification is separate from backup verification.

Reports use ordinary relative Markdown links to existing documents/reports. For raw artifacts outside the vault, record the filesystem path and verified backup locator as code text; do not create broken links to unversioned external files or assume Obsidian can open files outside its vault. The operational source for exact paths and backup responsibilities remains the runbook. Reports created only on the experiment branch do not appear automatically on develop; publish reviewed report/index changes directly through the separately authorized main-worktree documentation workflow, not a wholesale experiment-branch merge.

## Review and Execution Boundaries

This worktree/storage revision does not authorize setup, execution, evidence migration or commits. Main is intended to remain on develop; temporary mutations/test execution belong in the separate experiment worktree. The observed one-time migration is still required because the main directory is currently on the existing experiment branch with uncommitted documentation changes. MUT-001's existing temporary setup remains untouched; use the runbook's copy-with-verification procedure only after explicit approval. Preserve its original baseline and earlier files, even if later documentation commits change the approved branch tip.

Human authorization is required before each scoped mutation experiment. Existing tests stay unchanged during initial detection; test improvements and genuine production fixes have independent approval/review paths. Remove all intentional faults before any proposed merge. Committing a readable report, changing an ADR, running a new experiment and merging test improvements are separate decisions. No automated tooling, new score threshold, execution success or security guarantee is implied by this index.
