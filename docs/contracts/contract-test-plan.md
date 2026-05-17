# Contract Test Plan

## Status

Slice 03 initial contract-test plan.

No service-level contract test task exists yet. This document defines the test
expectations that later service slices must implement before claiming runtime
contract readiness.

## Required Contract Test Areas

| Area | Contract | Required Tests |
|---|---|---|
| gRPC ingestion | `contracts/grpc/forensic-ingestion.proto` | Schema compatibility, current RPC shape, deprecated field 6 preservation, required-at-application validation, streaming upload deduplication |
| Analysis jobs | `contracts/grpc/analysis-job.proto` | Job state transitions, lease idempotency, retryable failure, dead-letter semantics, artifact reference completeness |
| Repository analysis | `contracts/grpc/repository-analysis.proto` | HTTPS-only URL validation, no credentials/userinfo/query secrets/fragments, branch-or-commit requirement, safe ref validation, source snapshot response without private workspace paths, cleanup by opaque workspace ID, safe-attributes policy, opaque or snapshot-relative artifact references |
| Gateway REST | `contracts/openapi/gateway-api.yaml` | Request/response schema validation, error envelope shape, correlation header, idempotency key behavior, planned-operation implementation status, repository URL and workspace policy alignment with repository-analysis gRPC |
| Events | `contracts/events/analysis-events.md` | Envelope completeness, event id deduplication, required payload fields, at-least-once consumer behavior, unknown-field tolerance |
| Generated code boundaries | all contracts | Generated classes stay inside service-local build output and do not become shared Java modules |

## Minimum Slice 03 Verification

Slice 03 is documentation and contract-only. Required verification:

```bash
git diff --check
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

The full local quality gate remains required before final commit readiness:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Future Automation Targets

Later implementation slices should add or verify:

- Protobuf descriptor generation or compatibility checks for `.proto` files.
- OpenAPI schema validation for `gateway-api.yaml`.
- REST adapter contract tests for current and planned operations as they become
  implemented.
- gRPC server and client contract tests with generated code kept service-local.
- Event producer and consumer tests using deterministic JSON fixtures.
- Tests proving that generated transport classes do not leak into service
  domain or application packages.

Repository-analysis service tests must also prove that checked-out repository
contents are untrusted input: hooks, submodules, builds, parser execution, Joern,
BTM generation and repository-supplied tools are not run during checkout or
source-root detection.

Repository-analysis public responses must reject absolute paths, `file:` URIs,
workspace roots and server-local paths in artifact references. Request and
response `safe_attributes` must reject or redact secrets, credentials, tokens,
local/private paths, raw repository content, environment values and unvalidated
echoed input.

Repository-analysis validation tests must cover:

- `request_id`, `idempotency_key`, `schema_version`, `correlation_id`,
  `analysis_run_id`, repository reference, revision and workspace policy are
  required for prepare requests;
- get requests require `request_id`, `correlation_id`, `analysis_run_id` and
  `source_snapshot_id`;
- cleanup requests require `request_id`, `idempotency_key`, `correlation_id`,
  `analysis_run_id` and opaque `workspace_id`;
- branch-only, commit-only and branch-plus-commit revision modes are accepted
  when safe; no-branch/no-commit, missing required refs and option-like refs are
  rejected;
- branch-plus-commit checkout verifies that the resolved commit matches the
  requested commit and is reachable from the requested branch;
- zero timeout or zero workspace quota is rejected for the networked service;
- partial clone and sparse checkout remain rejected until a later contract
  explicitly enables them;
- `CheckoutResult` reports shallow, partial and sparse policy outcomes;
- top-level `source_snapshot_id` equals
  `source_snapshot.source_snapshot_id`;
- source snapshot IDs are deterministic for the same sanitized repository URL,
  requested revision, resolved commit and manifest artifact checksum.

Gateway REST contract tests must reject repository URLs with query strings or
fragments, zero timeout or workspace quota values, and absolute, `file:` or
parent-traversing artifact references.

## Evidence Integrity Expectations

Contract tests must prove that:

- correlation IDs are preserved;
- missing evidence is represented as unknown, incomplete, unavailable or a gap;
- static facts are not treated as runtime execution;
- report sections distinguish confirmed evidence, derived analysis, gaps,
  hypotheses and generated text;
- LLM-generated text is never asserted as verified evidence.
