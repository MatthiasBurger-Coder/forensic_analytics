# Contract Versioning

## Status

FA-MSA-001 Slice 04 contract versioning and data-ownership baseline for workflow
`fa-msa-001-microservice-decomposition-20260521-v1`.

This document aligns existing contract files with the FA-MSA-001 target service
names. It does not rename contract files, change protobuf field numbers, change
REST paths, change event names or claim that planned operations are implemented.
Filenames that still contain predecessor vocabulary, such as
`gateway-api.yaml` and `gateway-cli-contract.md`, are transitional contract
file names until a later compatibility slice renames or supersedes them with
tests.

## Contract Authorities

| Contract | FA-MSA-001 authority | Status |
|---|---|---|
| `contracts/grpc/forensic-ingestion.proto` | `ingestion-service` intake and session contract | Current v1 shape extracted from implementation evidence; repository checkout ownership moves to `repository-source-service`. |
| `contracts/grpc/analysis-job.proto` | `analysis-orchestrator-service` job orchestration and worker handoff contract | Current/predecessor implementation evidence exists; target ownership is job lifecycle, worker leases, retries, failures, dead-letter state, correlation references and job-to-artifact references. |
| `contracts/grpc/repository-analysis.proto` | `repository-source-service` repository checkout, workspace preparation and source-snapshot contract | Current predecessor filename and package remain until a later compatibility slice changes them. |
| `contracts/grpc/java-ast-analysis.proto` | `java-parser-analysis-service` source-fact analysis and source-fact artifact byte retrieval contract | Current predecessor filename remains until renamed or superseded. |
| `contracts/grpc/java-ast-source-facts-v1.schema.json` | `java-parser-analysis-service` source-fact artifact payload contract | Defines `application/vnd.forensic-analytics.java-ast-source-facts.v1+json`; consumers must map it into service-owned models. |
| `contracts/grpc/joern-cpg-analysis.proto` | `joern-analysis-service` CPG/CFG/DFG semantic artifact contract | Planned initial contract with predecessor filename until renamed or superseded. |
| `contracts/grpc/btm-generation.proto` | Optional later `btm-generation-service` contract | Not mandatory for FA-MSA-001 closure; generated artifact bytes and producer metadata stay with the producing service unless an explicit handoff contract transfers custody. |
| `contracts/openapi/gateway-api.yaml` | `query-report-api-service` public REST/OpenAPI contract | Transitional filename. Current verified operations remain documented; planned operations are not runtime evidence. |
| `contracts/cli/gateway-cli-contract.md` | `cli-client` public API consumption contract | Transitional filename and command vocabulary. CLI must remain a public API consumer, not a business-logic owner. |
| `contracts/events/analysis-events.md` | Event contract for FA-MSA-001 services | Planned initial message contract; no broker/runtime implementation is implied. |

Contracts in `contracts/` are interface descriptions only. They must not contain
or generate shared Java implementation modules, shared DTO modules, shared
domain models, shared mappers, shared exceptions, shared test fixtures, shared
Spring configuration or shared runtime libraries.

## Compatibility Rules

### Protobuf

- Keep existing package names, service names, RPC names, field names and field
  numbers for active v1 contracts unless a later slice explicitly approves a
  breaking contract change.
- Do not reuse a field number or enum number.
- If a field is removed in a future major version, reserve its number and name.
- `AnalysisDataEnvelope.payload_type = 6` remains deprecated in
  `forensic-ingestion.proto`; field number `6` and name `payload_type` must not
  be reused while the v1 contract exists.
- Additive optional fields are compatible when consumers can safely ignore
  them.
- Generated Protobuf and gRPC Java classes must be service-local build output.
  No service may depend on another service's generated classes.

### REST/OpenAPI

- Adding an optional response field is compatible.
- Adding a required request field is breaking.
- Removing a response field is breaking unless the field was explicitly marked
  experimental and consumers were versioned away from it.
- Public error envelopes must keep `code`, `message`, `retryable`,
  `correlationId` and `diagnostics`.
- Planned operations must remain marked as planned until implemented and
  tested.
- The transitional `gateway-api.yaml` filename does not make
  `forensic-gateway-service` the FA-MSA-001 authority; public query and report
  behavior belongs to `query-report-api-service`.

### Events

- Events use semantic schema versions.
- Minor versions may add optional payload fields.
- Required fields, event names, routing keys and enum semantics require a major
  version.
- Consumers must deduplicate by `eventId`.
- Events are at-least-once notifications, not owner database read models.
- Event producer and consumer names must use FA-MSA-001 service names. If data
  ownership is unresolved, the event flow remains deferred instead of using a
  placeholder owner.

## Error And Status Models

Public REST errors use the OpenAPI `ErrorEnvelope`.

gRPC methods use transport status for infrastructure-level failures and
`OperationStatus` messages for business-level acceptance, retry and diagnostic
state when a response can be returned.

Events represent failures with explicit failure or dead-letter events. Missing
or delayed events must be visible as incomplete, unknown or unavailable state.

## Retry, Deadline And Idempotency Rules

- REST mutation operations require `Idempotency-Key`.
- gRPC mutation requests include `idempotency_key`.
- Client-streaming ingestion retries are tied to stable `session_id` and
  `AnalysisPayloadDescriptor.payload_id`.
- Worker job leasing retries are tied to `worker_id`, `worker_kind` and
  `idempotency_key`.
- Consumers must treat duplicate events as duplicates when `eventId` repeats.
- Default synchronous public API reads should complete within 30 seconds unless
  a later service slice records a tighter timeout.
- Long-running work must be represented as jobs, reports or replay requests
  with explicit status instead of blocking public API calls indefinitely.

## Evidence Boundaries

Contracts must preserve evidence category boundaries:

- static source facts;
- semantic artifacts;
- runtime traces;
- exception facts;
- graph and replay projections;
- reports;
- LLM-generated hypotheses or text.

Contracts must not present missing data as observed evidence. Unknown or
partial state must be represented as `UNKNOWN`, `INCOMPLETE`, diagnostics, gaps
or unavailable state.

## Implementation Rules For Later Slices

- Service implementations must map transport messages into service-owned domain
  models.
- Domain and application code must not depend on generated transport classes.
- `query-report-api-service` is a public API facade. It owns public read
  models, generated report packages, LLM-ready packages and stored
  LLM-generated output only as labeled generated analysis or hypotheses. It
  must not own analysis execution, repository checkout, JavaParser processing,
  Joern processing or canonical evidence.
- `analysis-orchestrator-service` coordinates jobs and worker handoffs. It
  owns job lifecycle, worker leases, retries, failures, dead-letter state,
  correlation references and job-to-artifact references, but it must not become
  a hidden monolith, artifact byte owner, producer catalog owner or canonical
  fact store.
- Canonical analysis facts and artifact metadata ownership follow the S04
  service-local ownership matrix in `docs/architecture/data-ownership.md`.
- Graph and replay outputs are projections, and report outputs are generated
  artifacts. They are not primary evidence stores without a later approved
  requirement and ADR.
- Contract changes require contract-governance review before implementation.
