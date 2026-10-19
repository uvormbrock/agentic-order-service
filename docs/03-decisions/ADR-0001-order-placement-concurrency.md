# ADR-0001: Consistent Ordering of Order Placement and Related Changes

**Scoped contract precedence — 2026-10-18:** The [approved Customer DELETE target](../01-requirements/product-requirements.md#customer-delete--approved-target-contract-2026-10-18) supersedes the earlier referenced-customer deactivation outcome below for future DELETE behavior only. Placement-first requires DELETE/409 with unchanged customer/order state; delete-first must not leave dangling references. Existing text and execution evidence preserve the historical contract; Phase 2 now implements referenced DELETE/409 without mutation; see the [Phase 2 review record](../04-agentic-coding/change-requests/CH-001-customer-delete-semantics/03-review.md) for verification and pending human acceptance; no new enforcement technique or change to this ADR's status is implied.

**Status:** Accepted business consistency policy; technical enforcement pending
**Related:** [Product Requirements](../01-requirements/product-requirements.md) · [Domain Model](../02-architecture/domain-model.md) · [Order Item Snapshots](../02-architecture/order-item-snapshots.md)

## Context

Order creation reads customer eligibility and product eligibility/names/prices, calculates amounts, and stores references. Concurrent name or price updates, deactivation, or customer deletion can otherwise invalidate those checks before the order is saved. A product snapshot alone does not prevent this race.

## Decision

Successful operations affecting the same customer or products must have an outcome equivalent to a consistent serial ordering. Non-overlapping operations retain their actual order; overlapping operations may take either valid order. Request arrival time does not reserve a price or establish priority.

- If order placement takes effect first, it uses the previously valid active records, product names, and prices. Later price changes or deactivation do not alter its monetary facts.
- If a product update takes effect first, placement captures the updated name and price. If product/customer deactivation takes effect first, placement is rejected with 409 and stores nothing.
- If physical customer deletion takes effect first, placement fails with 404 and stores nothing.
- If placement takes effect before customer deletion, deletion must observe the new reference and deactivate the customer instead of physically deleting it.
- Checks and the accepted product-name and price capture must remain valid through atomic order persistence. One operation cannot validate an old active status and then accept a price from an incompatible newer state.
- For multiple products, all accepted names, prices, and eligibility checks must belong to a state permitted by the same consistent ordering, rather than incompatible independent observations.

Overlap alone is not a reason to return a business conflict. Subsequent retrieval displays original product names, prices, and amounts with current customer details, as required by PRD BR-010.

## Consequences

The application/persistence boundary coordinates consistency across the customer, referenced products, and the order. Aggregate-local validation is necessary but insufficient.

All relevant writes must participate in the policy, including product updates/deactivation, customer deactivation/deletion, and order placement. Prevent dangling references and partial orders. Product physical deletion remains outside the MVP.

This policy may require waiting, validation retries, or transaction aborts under contention. It does not select locking, transaction isolation, or a retry implementation. Automatic replay of an order after an uncertain commit is not authorized by this decision because it could create duplicates.

## Verification Scenarios

| Overlapping operations | Valid outcome |
| --- | --- |
| Product rename during placement | Order captures the name before or after the rename according to the same ordering used for price and eligibility. |
| Price changes from EUR 25.00 to EUR 30.00 during placement | Order uses EUR 25.00 if placement wins, or EUR 30.00 if the update wins; all derived amounts use the accepted price. |
| Product or customer deactivation versus placement | Placement succeeds before deactivation, or fails with 409 after it; no order is saved on failure. |
| Unreferenced customer deletion versus first order | Delete first: 404 for placement. Placement first: saved order and customer deactivated by deletion; no dangling reference. |
| Updates to several ordered products | Captured prices correspond to one valid ordering of successful operations; no incompatible mixture. |
| Confirmed rollback during placement | No order or items from that attempt remain. |
| Commit succeeds but confirmation is lost | The complete order may already exist; the caller cannot infer rollback from the communication failure. |

## Rollback and Uncertain Commit Outcomes

A confirmed rollback means the attempt persisted no order or items. A timeout or lost connection around commit can leave the outcome unknown: the complete transaction may have committed or rolled back. Atomicity rules out partial persistence, not uncertainty about which complete outcome occurred.

Do not report an uncertain outcome as a confirmed rollback or automatically replay the request. Outcome reconciliation and client-facing handling require a separate technical decision; this ADR introduces no idempotency feature or retry strategy.

## Remaining Technical Decisions

Before implementation, select and document the transaction/locking strategy, resource acquisition order where applicable, and enforcement of referential integrity. Define bounded retries, timeout behavior, and API responses for exhausted transient failures without conflating these with the PRD's business-validation conflicts.

## Decision History and Alternatives

- Original record: `docs/03-decisions/0001-order-placement-concurrency.md`, retained in Git history. The accepted policy is unchanged.
- 2026-10-13: Relocated to the authoritative ADR directory. Application and technical choices are tracked separately in ADR-0002 through ADR-0005.
- Rationale: order placement and related mutations must have explainable outcomes without invalid references or inconsistent historical facts.
- Alternatives: accepting unprotected stale reads violates eligibility/snapshot invariants; rejecting every overlap creates business conflicts not required by the accepted policy.

Verification criteria remain the scenarios above. No implementation or test evidence is claimed by this relocation.
