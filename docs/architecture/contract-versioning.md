# Contract Versioning

## Status

Contract versioning rules for the microservices ecosystem conversion workflow.

The active workflow assigns Gateway HTTP and public gRPC BTM delivery contract
work to Slice 02, artifact-byte and instrumentation-target ownership contracts
to Slice 03, and source-package, complete build-output package and Joern
materialization contracts to Slice 07. Slice 11 owns the repository-to-BTM
orchestration owner API refinement, Gateway public diagnostic model and
artifact-readiness bridge. Slice 12 owns Java AST source-fact byte retrieval,
Repository Analysis to Java AST handoff closure and deterministic local fixture
readiness before end-to-end orchestration can resume. Existing
planned contracts remain design artifacts unless their operation explicitly says
`current-verified`. The user approved logical initial contracts for
not-yet-implemented service communication on 2026-05-16. That approval allows
planned endpoint, RPC and event design, but it does not turn planned contracts
into implemented runtime evidence.

## Contract Authorities

| Contract | Authority | Status |
|---|---|---|
| `contracts/grpc/forensic-ingestion.proto` | gRPC ingestion compatibility contract | Current v1 shape extracted from implementation evidence |
| `contracts/grpc/analysis-job.proto` | Worker handoff, analysis-job state, instrumentation target planning and repository-to-BTM orchestration owner API | Current verified for Analysis Store service-local gRPC implementation |
| `contracts/grpc/repository-analysis.proto` | Repository checkout and source-snapshot preparation contract | Planned initial contract |
| planned `contracts/grpc/build-artifact-worker.proto` or approved owner API | Complete build-output package production contract | Planned by workflow v2 and retained by v3/v4; no file exists until a slice verifies the name and service boundary |
| repository-to-BTM orchestration owner API | Worker-dispatch/job-graph owner contract between Gateway and Analysis Store | Implemented by workflow v3 Slice 11 in `contracts/grpc/analysis-job.proto`; end-to-end orchestration may only use this owner API and readiness bridge |
| `contracts/grpc/java-ast-analysis.proto` | Java AST source-fact worker and source-fact byte retrieval contract | Planned initial contract; workflow v4 Slice 12 must verify the owner retrieval API before end-to-end orchestration consumes source-fact bytes |
| `contracts/grpc/joern-cpg-analysis.proto` | Joern CPG/CFG/DFG semantic artifact worker contract | Planned initial contract |
| `contracts/grpc/btm-generation.proto` | BTM generation, generated artifact metadata and public BTM artifact delivery contract | Planned initial contract |
| `contracts/openapi/gateway-api.yaml` | Public Gateway REST contract | Mixed current verified and planned initial operations; repository-to-BTM submission is planned initial |
| `contracts/events/analysis-events.md` | Analysis event contract | Planned initial contract |

Contracts in `contracts/` are interface descriptions only. They must not contain
or generate shared Java implementation modules, shared DTO modules, shared
domain models, shared mappers, shared exceptions, shared test fixtures, shared
Spring configuration or shared runtime libraries.

## Compatibility Rules

### Protobuf

- Keep existing package names, service names, RPC names, field names and field
  numbers for active v1 contracts.
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
- Planned operations must remain marked as planned until implemented and tested.

### Events

- Events use semantic schema versions.
- Minor versions may add optional payload fields.
- Required fields, event names, routing keys and enum semantics require a major
  version.
- Consumers must deduplicate by `eventId`.
- Events are at-least-once notifications, not owner database read models.

## Error And Status Models

REST Gateway errors use the OpenAPI `ErrorEnvelope`.

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
- Default synchronous Gateway reads should complete within 30 seconds unless a
  later service slice records a tighter timeout.
- Long-running work must be represented as jobs, reports or replay requests
  with explicit status instead of blocking Gateway calls indefinitely.

## Evidence Boundaries

Contracts must preserve evidence category boundaries:

- static source facts;
- semantic artifacts;
- runtime traces;
- exception facts;
- graph and replay projections;
- reports;
- LLM-generated hypotheses or text.

Contracts must not present missing data as observed evidence. Unknown or partial
state must be represented as `UNKNOWN`, `INCOMPLETE`, diagnostics, gaps or
unavailable state.

## Implementation Rules For Later Slices

- Service implementations must map transport messages into service-owned domain
  models.
- Domain and application code must not depend on generated transport classes.
- Gateway is a facade and must not own analysis business logic.
- Analysis Store owns canonical analysis state and normalized facts.
- Graph/replay and reports are projections or generated artifacts, not primary
  evidence stores.
- Contract changes require contract-governance review before implementation.
