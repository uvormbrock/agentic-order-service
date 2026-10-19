# CH-001 — Customer DELETE Semantics

**Recorded:** 2026-10-18  
**Status:** Business target approved in the authoritative documents; Phase 1 documentation recorded. Phase 2 explicitly authorized on 2026-10-18 and implemented; verification is recorded in [03-review.md](03-review.md). Human review and final acceptance are Pending.

## Motivation and scope

Separate physical deletion from explicit deactivation so that DELETE does not silently change activity when orders require the customer to remain stored. The approved change allows physical deletion of unreferenced customers and rejects referenced deletion with 409, preserving customer and order state. PATCH remains explicit deactivation.

The approved specification is the [PRD target and five-case matrix](../../../01-requirements/product-requirements.md#customer-delete--approved-target-contract-2026-10-18), [REST contract and Phase 2 acceptance checklist](../../../02-architecture/rest-api-design.md#phase-2-implementation-acceptance-checklist), and [API error catalog](../../../02-architecture/api-error-codes.md#approved-customer-delete-target--2026-10-18). These sources govern behavior and exact responses.

Phase 2 changes referenced DELETE to CUSTOMER_HAS_ORDERS/409 without mutation, retaining the Customer lock and subsequent Order-owned reference query. Unit/MVC/PostgreSQL tests, demo expectations and OpenAPI metadata are adapted; the review record distinguishes actual verification from Pending gates. Creating the original tracking record did not authorize implementation; the later explicit human request did.

No new dependencies, migrations or architecture decisions are approved. Preserve transaction ownership, existing contracts outside this change, ADR statuses and historical acceptance evidence. ADR-0002 and ADR-0006 remain Proposed; unresolved blocking decisions require human resolution before dependent implementation.

## Phase records

1. [Documentation and prompt provenance](01-documentation.md)
2. [Phase 2 implementation prompt and authorization](02-implementation.md)
3. [Review and evidence — human acceptance Pending](03-review.md)

See the [Change Request convention](../README.md). Closure requires explicit human acceptance recorded in the review file.
