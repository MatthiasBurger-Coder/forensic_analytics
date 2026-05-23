# CLI Public API Contract

## Status

CLI consumer contract for public repository-to-BTM submission.

`gateway-submit` is the explicit compatibility command for this transitional
contract. The local-path `forensic-analytics-cli analyze` and `ingest-request`
commands remain legacy in-process adapters and are not silently routed to the
public API. Under FA-MSA-001 the target CLI is `cli-client` and the public API
authority is `query-report-api-service`; Gateway naming is retained only as
current command, option and file compatibility evidence.

S16 records `analyze` and `ingest-request` as deprecated target behavior for
`cli-client`. They remain legacy in-process adapters only in predecessor
modules until caller-free removal is proven; target CLI implementations must
reject those commands instead of routing local paths or engine-request files to
the public API.

## Producer And Consumer

| Field | Value |
|---|---|
| Consumer | `cli-client` target; current predecessor implementation is `forensic-analytics-cli` |
| Producer | `query-report-api-service` target; current predecessor implementation evidence is `forensic-gateway-service` |
| Protocol | HTTP JSON through `contracts/openapi/gateway-api.yaml` |
| S09 operation | `startRepositoryToBtmAnalysis` |
| Future operation | `getRepositoryAnalysis`; out of S09 until this contract defines a concrete command and option mapping |
| Contract version | `gateway-cli-v1` |

## Current Compatibility Decision

The current CLI `analyze` command is an in-process legacy adapter. It accepts
local paths or file URIs and does not require branch, commit, correlation ID or
idempotency key inputs.

After S16, local `analyze` and `ingest-request` are not target CLI behavior.
They are retained only as predecessor rollback evidence until S19 proves
caller-free removal or a later workflow creates a new explicit target owner and
contract.

Public repository-to-BTM submission requires:

- an HTTPS repository URL without user information;
- explicit branch or commit input;
- `X-Correlation-Id`;
- `Idempotency-Key`;
- `StartRepositoryAnalysisRequest`;
- redacted `RepositoryToBtmSubmission` and `RepositoryToBtmStatus` responses.

The CLI validates repository URL syntax, HTTPS and user-info restrictions before
submission. Local/private host and network-range policy remains owned by
`query-report-api-service`, which returns a public `VALIDATION_ERROR` envelope
without exposing private workspace or checkout details.

Therefore, the S09 target-client path keeps the compatibility command name
`gateway-submit`, the `--gateway` option name and `gateway.v1` schema-version
examples. The implementation must not silently route the existing local-path
`analyze` or `ingest-request` commands to the public API. Later FA-MSA-001
client work may rename or supersede these compatibility names only with a
contract compatibility decision and tests.

## Planned CLI Request Mapping

The `gateway-submit` command must collect these values before sending the
request:

| CLI concept | Public API field or header |
|---|---|
| `--repo-url` | `StartRepositoryAnalysisRequest.repositoryUrl` |
| `--branch` | `StartRepositoryAnalysisRequest.branch` |
| `--commit` | `StartRepositoryAnalysisRequest.commit` |
| `--requested-outputs` | `StartRepositoryAnalysisRequest.requestedOutputs[]` |
| `--schema-version` | `StartRepositoryAnalysisRequest.schemaVersion` |
| `--request-id` | `StartRepositoryAnalysisRequest.requestId` |
| `--provider` | `StartRepositoryAnalysisRequest.provider` |
| `--build-tool` | `StartRepositoryAnalysisRequest.buildContext.buildTool` |
| `--build-id` | `StartRepositoryAnalysisRequest.buildContext.buildId` |
| `--root-project` | `StartRepositoryAnalysisRequest.buildContext.rootProjectName` |
| `--declared-modules` | `StartRepositoryAnalysisRequest.buildContext.declaredModules[]` |
| fixed client value `{}` | `StartRepositoryAnalysisRequest.buildContext.attributes` |
| `--timeout-seconds` | `StartRepositoryAnalysisRequest.workspacePolicy.timeoutSeconds` |
| `--max-workspace-bytes` | `StartRepositoryAnalysisRequest.workspacePolicy.maxWorkspaceBytes` |
| `--allow-shallow-clone` | `StartRepositoryAnalysisRequest.workspacePolicy.allowShallowClone` |
| fixed client value `false` | `StartRepositoryAnalysisRequest.workspacePolicy.ephemeral` |
| fixed client value `false` | `StartRepositoryAnalysisRequest.workspacePolicy.allowPartialClone` |
| fixed client value `false` | `StartRepositoryAnalysisRequest.workspacePolicy.allowSparseCheckout` |
| `--correlation-id` | `X-Correlation-Id` |
| `--idempotency-key` | `Idempotency-Key` |

At least one of branch or commit is required. If both are provided, the public
API must preserve both in the request and downstream services must resolve the
actual commit before analysis.

## Planned CLI Response Mapping

For `202 Accepted`, CLI output may include only public API fields:

- `analysisRunId`;
- `status`;
- `statusUrl`;
- `jobsUrl`;
- `btmDeliveryStatus`;
- `btmDeliveryService`;
- `correlationId`;
- public diagnostics.

Status reads are out of S09. A later status command must define the command
name, `analysisRunId` option and required `X-Correlation-Id` mapping before it
may call `getRepositoryAnalysis`. When approved, status output may include only
fields from `RepositoryToBtmStatus`.

CLI output must not include:

- workspace IDs or workspace paths;
- local checkout paths;
- raw Git stdout or stderr;
- internal service exception messages;
- credentials, tokens, authorization headers or secret-like values;
- generated Java DTO class names;
- public API implementation package names.

## Error Mapping

The CLI must map public API `ErrorEnvelope` values without changing their
meaning:

| Public API code | CLI behavior |
|---|---|
| `VALIDATION_ERROR` | Exit non-zero and print a redacted validation failure. |
| `CONFLICT` | Exit non-zero and state that the idempotency key conflicts with another request. |
| `BACKEND_UNAVAILABLE` | Exit non-zero and report a retryable public API backend failure when `retryable=true`. |
| `TIMEOUT` | Exit non-zero and report timeout without retrying unless a later retry policy is explicitly approved. |
| `NOT_FOUND` | Exit non-zero and report missing public analysis status. |
| `UNEXPECTED_ERROR` | Exit non-zero and report an unexpected redacted public API failure. |

## Retry, Timeout And Idempotency

- CLI public API submission must send an idempotency key for every mutation.
- Automatic retries are not part of this contract version.
- A future retry slice must preserve the same idempotency key for retried
  submission attempts.
- CLI status polling is not part of this contract version.
- CLI implementation must use an explicit timeout and report timeout as a
  redacted failure.

## Generated-Code Boundary

The CLI must not depend on `query-report-api-service` implementation classes,
predecessor Gateway implementation classes, service-local domain models,
service-local DTOs, service-local mappers, internal exceptions, generated
gRPC/protobuf transport classes or shared Java DTO modules. S09 uses HTTP JSON
against the public OpenAPI contract. Generated OpenAPI client code is not part
of S09; if introduced later, it must be generated service-locally inside
`services/cli-client` from `contracts/openapi/gateway-api.yaml`.

## Contract Tests

Required contract tests:

- `GatewayOpenApiContractTest` verifies the transitional public API route,
  schema, idempotency and
  redaction contract markers.
- `CliClient*Test` verifies that `services/cli-client` uses HTTP JSON public API
  access instead of in-process analysis, parser, Joern or persistence behavior.
- Predecessor `ForensicAnalyticsCliTest` continues to verify that
  `gateway-submit` uses a Gateway client instead of the in-process
  `RunRepositoryAnalysisUseCase`, preserves the current local `analyze`
  compatibility decision and keeps output public while the predecessor remains.

## Implementation Stop Conditions

Stop a later implementation slice when:

- a CLI option cannot be mapped to a public API field or header;
- public OpenAPI lacks a required request, response or error field;
- local-path analysis would be silently routed to Gateway;
- local-path ingestion request import would be silently routed to the public API;
- public CLI output would expose private workspace or checkout data;
- implementation requires shared Java DTOs or public API implementation classes;
- implementation imports `query-report-api-service` Java classes or generated
  gRPC/protobuf transport classes;
- implementation adds status reads without a concrete status command contract;
- retry or polling behavior is needed but not explicitly approved.
