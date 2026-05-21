# CLI Public API Contract

## Status

CLI consumer contract for public repository-to-BTM submission.

`forensic-analytics-cli gateway-submit` is the explicit Gateway command for this
transitional contract. The local-path `forensic-analytics-cli analyze` command
remains a legacy in-process adapter and is not silently routed to Gateway.
Under FA-MSA-001 the target CLI is `cli-client` and the public API authority is
`query-report-api-service`; Gateway naming is retained only as current command
and file compatibility evidence.

## Producer And Consumer

| Field | Value |
|---|---|
| Consumer | `cli-client` target; current predecessor implementation is `forensic-analytics-cli` |
| Producer | `query-report-api-service` target; current predecessor implementation evidence is `forensic-gateway-service` |
| Protocol | HTTP JSON through `contracts/openapi/gateway-api.yaml` |
| Gateway operations | `startRepositoryToBtmAnalysis`, `getRepositoryAnalysis` |
| Contract version | `gateway-cli-v1` |

## Current Compatibility Decision

The current CLI `analyze` command is an in-process legacy adapter. It accepts
local paths or file URIs and does not require branch, commit, correlation ID or
idempotency key inputs.

Public repository-to-BTM submission requires:

- an HTTPS repository URL without user information;
- explicit branch or commit input;
- `X-Correlation-Id`;
- `Idempotency-Key`;
- `StartRepositoryAnalysisRequest`;
- redacted `RepositoryToBtmSubmission` and `RepositoryToBtmStatus` responses.

Therefore, the implemented Gateway path is `gateway-submit`. The implementation
must not silently route the existing local-path `analyze` command to Gateway.
Later FA-MSA-001 client work may rename or supersede this command only with a
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
| `--timeout-seconds`, `--max-workspace-bytes`, `--allow-shallow-clone` | `StartRepositoryAnalysisRequest.workspacePolicy` |
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

For status reads, CLI output may include only fields from
`RepositoryToBtmStatus`.

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

The CLI must not depend on Gateway implementation classes, generated transport
classes, service-local domain models, service-local DTOs, mappers or internal
exceptions. The contract source of truth is `contracts/openapi/gateway-api.yaml`
and this file.

## Contract Tests

Required contract tests:

- `GatewayOpenApiContractTest` verifies the transitional public API route,
  schema, idempotency and
  redaction contract markers.
- `ForensicAnalyticsCliTest` verifies that `gateway-submit` uses a Gateway
  client instead of the in-process `RunRepositoryAnalysisUseCase`, preserves the
  current local `analyze` compatibility decision and keeps output public.

## Implementation Stop Conditions

Stop a later implementation slice when:

- a CLI option cannot be mapped to a public API field or header;
- public OpenAPI lacks a required request, response or error field;
- local-path analysis would be silently routed to Gateway;
- public CLI output would expose private workspace or checkout data;
- implementation requires shared Java DTOs or public API implementation classes;
- retry or polling behavior is needed but not explicitly approved.
