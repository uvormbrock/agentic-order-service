# Requirements and Documentation

Status: Analysis in progress. Target requirements below are explicitly authorized; runtime implementation is Pending.

## Original request provenance

Verbatim attachment supplied by the human in this chat on 2026-10-18.

```text
Execute the Multi-Agent Change Request workflow in
docs/04-agentic-coding/multi-agent/README.md.

Use the main session as Orchestrator; delegate meaningful specialist work
and independent review to actual sub-agents. Wait for dependent handoffs.

# Change Request: CH-002 — Product Images

Extend the existing product feature to support an optional original image
and one automatically generated thumbnail per product.

The backend must implement the complete image lifecycle independently of
any future administration UI.

## Authorized scope

### 1. Business behavior

- A product may exist without an image.
- Each product may have at most one original image and one thumbnail.
- Uploading an original image automatically generates its thumbnail.
- Replacing an image replaces the corresponding thumbnail.
- Deleting an image removes its association and managed image files.
- Existing products, product APIs and business rules must remain compatible.
- Preserve existing inactive-product behavior unless a specific approved
  requirement states otherwise.
- Do not implement an administration UI in this Change Request.

### 2. Image storage

- Store actual image bytes in a configurable filesystem storage location.
- Do not store image BLOBs in PostgreSQL.
- Store relative, application-managed storage keys and image metadata
  in PostgreSQL, never machine-specific absolute paths.
- Keep the uploaded original image at its original resolution, without
  resizing or recompression, subject to approved validation limits.
- Generate one aspect-ratio-preserving thumbnail automatically.
- Separate version-controlled demo image sources from mutable runtime storage.
- Use a local development storage location that is Git-ignored.
- Support an externally configurable persistent storage directory for deployment.
- Do not assume that local container storage is durable or shared.
- Define safe file naming, path traversal prevention, cleanup and recovery
  behavior for partial filesystem/database failures.
- Keep the storage implementation simple. Do not implement S3, a CDN or
  other remote storage services.

### 3. Persistence

- Design a dedicated product image metadata table with a reference to
  the existing product.
- Enforce at most one image association per product at the database level.
- Store the original and thumbnail storage keys and necessary metadata.
- Preserve all existing product records without requiring image data.
- Ensure replacement, deletion and failure recovery do not leave
  inconsistent database references or unmanaged files.

### 4. REST API

Implement and document REST endpoints for:

- Uploading or replacing a product image using multipart/form-data.
- Retrieving the original image bytes.
- Retrieving the generated thumbnail bytes.
- Deleting a product image.

Follow existing REST, error-handling and OpenAPI conventions.

Handle at least:
- Missing product.
- Product without image.
- Invalid or unsupported image content.
- Oversized upload.
- Corrupt or undecodable image.
- Filesystem write/read failures.
- Replacement and deletion failure cases.

Evaluate the existing authorization model. Do not invent an administrator
identity system or silently expose a new production write capability.
Report any security decision requiring human approval.

### 5. Demo image assets and seeding

The repository must contain real, decodable sample image files under:

demo/assets/products/

Associate them with these existing demo products:

- Demo Mouse: mouse.jpg
- Demo Notebook: notebook.jpg — this is a laptop computer, NOT a paper notebook.
- Demo Monitor: monitor.jpg
- Demo Sticker: sticker.jpg
- Demo Desk: desk.jpg
- Demo Pen: pen.jpg
- Demo Keyboard v2: keyboard.jpg
- Demo Legacy Item: legacy-item.jpg

Use the actual product identities from the existing demo data.

The demo source images must be distinct from the application's runtime
image storage. Update the existing demo/seed scripts, including Python
scripts where applicable, to load the real image files through the approved
application mechanism.

Seeding must be repeatable and must not create duplicate image associations.

If any required realistic image files are not present in the repository,
report the missing assets explicitly. Do not substitute unrelated imagery,
fabricate successful asset generation or introduce an unapproved image API.

The inactive Demo Legacy Item must retain its existing inactive status.
Include an independent test scenario for a product without an image.

### 6. Testing and verification

The Testing Agent must independently verify:

- Optional images and compatibility with existing product data.
- Successful upload, original retrieval and thumbnail retrieval.
- Actual image bytes and decodability, not only response metadata.
- Correct thumbnail generation, dimensions and aspect ratio.
- Original resolution and byte preservation.
- Replacement and deletion.
- Database uniqueness and foreign-key behavior.
- Missing products and missing images.
- Unsupported, oversized and corrupt image uploads.
- Filesystem/database consistency and recovery behavior.
- Repeatable demo-data seeding.
- Applicable concurrency and security boundaries.

Run the established focused tests and applicable full quality gates,
including JaCoCo, scoped PIT, Spotless and SonarQube as defined by the
project's existing quality policy.

Do not weaken assertions or fabricate verification evidence.

If SonarQube findings retrieval fails, preserve Failed/Pending statuses,
return evidence to the Orchestrator and continue other authorized work.
Do not automatically repeat a full scanner run because retrieval failed.
Do not count a retrieval failure alone as an implementation correction
iteration.

### 7. Documentation

Documentation is mandatory, not optional.

Synchronize affected authoritative documentation for:

- Business requirements and acceptance criteria.
- Product image data model and database references.
- Filesystem storage architecture and configuration.
- Original-image and thumbnail lifecycle.
- Upload, retrieval, replacement and deletion REST contracts.
- Validation rules, error codes and security limitations.
- Demo asset directory and reproducible seeding instructions.
- Development storage versus persistent deployment storage.
- Backup, restore, cleanup and relevant operational limitations.
- Test results, Quality Gate evidence and outstanding risks.

The Documentation Agent must verify the final documentation against the
actual implementation, not merely the intended design.

## Architecture and authorization boundaries

The following architecture direction is approved for this Change Request:

- Optional image association.
- One original plus one generated thumbnail per product.
- PostgreSQL for image references and metadata, not image BLOBs.
- Configurable filesystem storage for image content.
- A dedicated image metadata table.
- No administration UI.
- No remote object storage.

An additive Flyway migration implementing this approved dedicated image
table is specifically authorized when it preserves existing data and
backward compatibility. Do not modify or remove existing production
data or constraints without separate approval.

No new third-party dependencies are preapproved. If thumbnail processing
requires one, identify the exact dependency, version, purpose and
alternatives, and obtain explicit human approval before adding it.

The Architect must resolve implementation details within the approved
direction and identify any genuinely blocking decision before dependent
changes. The Orchestrator must not treat an unapproved material
architecture or security decision as implicitly authorized.

## Acceptance criteria

1. Existing products continue working without images.
2. Each product supports zero or one original image.
3. Uploading a valid image persists its original bytes and creates one
   valid thumbnail automatically.
4. Original and thumbnail can be retrieved separately.
5. Replacing an image updates both managed image variants consistently.
6. Deleting an image removes the association and follows documented
   file-cleanup semantics.
7. PostgreSQL contains image references/metadata, never image BLOBs.
8. Runtime storage is configurable and independent of Git-managed demo assets.
9. All eight existing demo products can be seeded with their matching
   real images when the named files are present.
10. An imageless product remains valid.
11. Failure paths, security boundaries and compatibility are tested.
12. REST/OpenAPI, operations and architectural documentation match reality.
13. All applicable quality gates are reported with real evidence and
    explicit Failed/Pending statuses where necessary.
14. The final independent review and human-acceptance report are complete.

## Branch

develop

## Change Request records

Apply the mandatory convention in
docs/04-agentic-coding/change-requests/README.md.

Inspect the register and directories; use the next unused sequential CH-NNN
and a concise lowercase hyphen-separated slug. CH-002 is reserved for
Product Images; use CH-002-product-images if available and consistent with
the register.

Initially create exactly:
- README.md
- 01-documentation.md
- 02-implementation.md
- 03-review.md

Preserve those filenames; add evidence files only when genuinely necessary.

Maintain all four records throughout the lifecycle using workflow.md's role
contributions and one assigned writer per file.

Initialize human acceptance as Pending; record only actual verification
and explicit human decisions.

Do not alter historical CH-001 records or use an alternative structure.

## Execution authorization

I authorize requirements analysis, scoped target documentation,
implementation, the specifically approved additive migration, tests,
independent review, documentation synchronization and available verification
for this defined scope under AGENTS.md.

Use no more than three correction iterations after initial implementation;
preserve the counter across agents and restarts.

Stop dependent work for any missing mandatory approval, unresolved blocking
ADR decision, migration/dependency/architecture change outside the specific
authorization, scope expansion, destructive action or external publication.

Do not stage, commit, push, merge, create a pull request or deploy.
Do not dispatch remote workflows without separate authorization.

Stop with the final report and human acceptance Pending.
```

## Analysis and decisions

Requirements and architecture handoffs Pending. Prepared PNG sources exist; requested JPEG filenames require evaluation. No dependency or new production authorization mechanism approved.

## Documentation synchronization

Pending implementation/decision resolution. Orchestrator owns this record; specialist contributions arrive as handoffs.

## Requirements and architecture handoffs — 2026-10-18

Actual specialists `/root/requirements` and `/root/architect` independently inspected
PRD, feature code, accepted ADR baselines, transaction rules and demo assets.
Existing product JSON remains exactly id/name/price/active; image APIs are separate.
Inactive products support image lifecycle without reactivation. Historical example
UUIDs in asset README are not reproducible demo IDs; seed.json is authoritative.

### Explicit subsequent human approvals

The human selected both async approval options in this chat:

1. “Approve default-disabled image writes with explicit trusted-deployment opt-in”.
   Upload/delete are disabled by default, enabled for the isolated demo; deployment
   enablement requires an external trusted access-control boundary, no new identity system.
2. “Approve JPEG conversion and preserve PNG originals”. All eight required JPEGs
   are now derived from matching PNG sources via macOS sips, quality90,1254×1254.

The only identified blocking security decision is resolved by that express approval.
No third-party image dependency is needed. Scoped validation details: JPEG/PNG,
10MiB,40million pixels,16384 maximum edge;320×320 PNG thumbnail,aspect preserved,
no upscale; original upload bytes are retained exactly. These are implemented limits,
not blanket approval of unrelated architecture or proposed ADR details.

### Lifecycle decision

Filesystem and database are not one atomic transaction. Immutable UUID generations
are staged completely before application-owned metadata transactions. Existing Product
row pessimistic locks serialize first association, replacement,deletion and buffered
reads. One productId PK/FK association; V3 preserves existing data. Commit failures
conservatively retain candidates; confirmed commits allow best-effort retired-file
cleanup. Missing referenced bytes are storage failures,not image absence. Explicit
quiescent reconciliation uses committed metadata and requires all readers/writers
across instances stopped. No automatic destructive cleanup or public cleanup endpoint.

### Documentation scope

Documenter `/root/documentation` updated requirements,domain model,architecture,REST,
error catalog and development/demo operations against actual implementation. Existing
ADR statuses and CH-001 history remain unchanged. Final consistency recheck Pending
completion of correction1,tests and OpenAPI generation. Authoritative docs contain the
full contracts; this record preserves decision and execution provenance.

### Explicit Cloud execution confirmation

After automatic approval review incorrectly applied the historical setup-only no-Cloud
restriction to CH-002, the Orchestrator asked the human specifically about one current
CH-002 make sonar-local analysis upload using existing credentials. The human selected
“Explicitly authorize one CH-002 SonarQube Cloud analysis upload”. This is additional
explicit authorization for one actual execution, not for remote workflow dispatch,
credential creation,project changes or Git publication. The rejected request never ran.

## Final acceptance trace

Runtime implementation and authoritative documentation now agree; documentation agent
verified actual production/config/demo/OpenAPI contracts. No production changes followed
that check. Existing ADR statuses remain unchanged. Human Acceptance remains Pending.

| Criteria from original request | Actual evidence / status |
| --- | --- |
| 1,10 optional/imageless products | Unit+PostgreSQL+HTTP cases include independent ninth imageless product; Passed |
| 2,7 single metadata association,not BLOB | V3 PK/FK/unique/checks and V2→V3 preservation tests; Passed |
| 3,4 original+thumbnail lifecycle and bytes | JPEG/PNG decoded,dimensions/aspect/no upscale,original byte comparisons over actual HTTP; Passed |
| 5,6 replacement/deletion/recovery | Product locks,transaction rollback/uncertainty,cleanup failure,partial-stage fault injection and quiescent reconciliation tests; Passed with documented operator boundary |
| 8 storage separation/configuration | Configurable ignored runtime root;demo assets preserved; external durability/access policy documented; Passed within approved deployment assumptions |
| 9 all8demo images | Real JPEG sources present; actual Python helper seeds twice over isolated HTTP; inactive Legacy retained; Passed |
| 11 security/failure/compatibility | Disabled write guard,image parsing/path/limits,error contracts and existing regression; Passed tests with residual mutation assertion gaps Pending human disposition |
| 12 documentation matches runtime | Seven authoritative docs+runtime-generated OpenAPI snapshot+asset/seed instructions; Passed |
| 13 actual gate evidence | Recorded separately,including actual Passed remote gate and explicit Pending hotspot/snapshot/exact-final-tree limitations; report complete |
| 14 independent review/final human report | Reviewer returned completed read-only handoff; final report complete; Human Acceptance Pending |

Target PRD and REST details remain in their authoritative documents; this trace does
not promote all proposed ADR details or accept unresolved security/verification risks.

## V-007 verification continuation authorization and outcome

The human explicitly authorized completing the existing Testing assignment before
human acceptance, including focused and final regression checks and applicable gate
reuse. Scope remained test-only ProductImageLifecycleIT.java plus record synchronization;
no new production correction batch or Change Request. All ten metadata attributes
now have committed PostgreSQL reload/SQL assertions, including replacement. Actual
execution and independent review Passed; [V-007 evidence](03-review.md#v-007--execution-and-independent-review)
supersedes the earlier identified coverage gap. Existing architecture, API contracts
and ADR statuses are unchanged. Human Acceptance remains Pending.
