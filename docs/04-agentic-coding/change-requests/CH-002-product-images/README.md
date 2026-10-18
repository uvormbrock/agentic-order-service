# CH-002 — Product Images

Execution status: **Completed — Human Review Required**. V-007 is resolved by executed PostgreSQL round-trip tests and independent review; remaining external and mutation verification limitations are retained.
Human Acceptance: **Pending**. Correction iterations: **2/3**.

## Objective and scope

Add optional filesystem-backed product originals and generated thumbnails, dedicated PostgreSQL metadata, REST lifecycle, reproducible demo seeding, tests and synchronized documentation. The attached human request authorizes this defined lifecycle and a backward-compatible additive migration. No new dependencies, remote storage, administration UI, Git publication or destructive data changes are approved.

## Authority and records

- [AGENTS.md](../../../../AGENTS.md)
- [Workflow](../../multi-agent/workflow.md)
- [Requirements and original request](01-documentation.md)
- [Implementation](02-implementation.md)
- [Review and gate evidence](03-review.md)

Blocking architecture/security decisions must be resolved before dependent edits. CH-001 remains unchanged.

## Outcome

Optional JPEG/PNG originals and generated thumbnails, four image REST operations,
V3 metadata constraints, filesystem recovery/configuration and repeatable demo image
seeding are implemented. Existing product contracts and inactive status preserved.
Final 618 fast + 157 integration tests Passed; six existing offline demo tests retain their earlier Passed evidence. See [review](03-review.md)
for exact coverage,mutation outcomes,Sonar source identity and remaining limitations.

Actual remote Quality Gate Passed for recorded analyzed source and0issues retrieved;
hotspots/snapshot/supersession and exact final-tree remote analysis remain Pending.
Mutation execution completed,but residual assertion gaps require human disposition.
Completion is not all-gates-passed and grants no Git publication/deployment approval.

## V-007 completion

The previously queued metadata round-trip extension is implemented and independently
reviewed. Both JPEG/PNG upload and replacement orders assert all ten fields after
fresh JPA reload and direct SQL comparison. Final clean verify exit 0 on 2026-10-18
20:39:43 CEST. No production change or new correction batch; counter remains **2/3**.
See [V-007 execution evidence](03-review.md#v-007--execution-and-independent-review).
