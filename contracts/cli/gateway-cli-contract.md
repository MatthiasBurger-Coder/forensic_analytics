# CLI To Gateway Contract

## Status

Planned CLI consumer contract for Gateway repository-to-BTM submission.

This file is a contract document only. It does not claim that `forensic-analytics-cli` already calls `forensic-gateway-service`.

## Producer And Consumer

| Field | Value |
|---|---|
| Consumer | `forensic-analytics-cli` |
| Producer | `forensic-gateway-service` |
| Protocol | HTTP JSON through `contracts/openapi/gateway-api.yaml` |
| Gateway operations | `startRepositoryToBtmAnalysis`, `getRepositoryAnalysis` |
| Contract version | `gateway-cli-v1` |

## Current Compatibility Decision

The current CLI `analyze` command is an in-process legacy adapter. It accepts
local paths or file URIs and does not require branch, commit, correlation ID or
idempotency key inputs.

Gateway repository-to-BTM submission requires:

- an HTTPS repository URL without user information;
- explicit branch or commit input;
- `X-Correlation-Id`;
- `Idempotency-Key`;
- `StartRepositoryAnalysisRequest`;
- redacted `RepositoryToBtmSubmission` and `RepositoryToBtmStatus` responses.

Therefore, a later implementation slice must add an explicit Gateway mode or Gateway command before CLI traffic can move to Gateway. The implementation slice must not silently route the existing local-path `analyze` command to Gateway.

## Planned CLI Request Mapping

The Gateway CLI mode must collect these values before sending the request:

| CLI concept | Gateway field or header |
|---|---|
| repository URL | `StartRepositoryAnalysisRequest.repositoryUrl` |
| branch | `StartRepositoryAnalysisRequest.branch` |
| commit | `StartRepositoryAnalysisRequest.commit` |
| requested output | `StartRepositoryAnalysisRequest.requestedOutputs[]` |
| schema version | `StartRepositoryAnalysisRequest.schemaVersion` |
| request ID | `StartRepositoryAnalysisRequest.requestId` |
| build tool | `StartRepositoryAnalysisRequest.buildContext.buildTool` |
| build ID | `StartRepositoryAnalysisRequest.buildContext.buildId` |
| root project name | `StartRepositoryAnalysisRequest.buildContext.rootProjectName` |
| declared modules | `StartRepositoryAnalysisRequest.buildContext.declaredModules[]` |
| workspace policy | `StartRepositoryAnalysisRequest.workspacePolicy` |
| correlation ID | `X-Correlation-Id` |
| idempotency key | `Idempotency-Key` |

At least one of branch or commit is required. If both are provided, Gateway
must preserve both in the request and downstream services must resolve the
actual commit before analysis.

## Planned CLI Response Mapping

For `202 Accepted`, CLI output may include only public Gateway fields:

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
- Gateway implementation package names.

## Error Mapping

The CLI must map Gateway `ErrorEnvelope` values without changing their meaning:

| Gateway code | CLI behavior |
|---|---|
| `VALIDATION_ERROR` | Exit non-zero and print a redacted validation failure. |
| `CONFLICT` | Exit non-zero and state that the idempotency key conflicts with another request. |
| `BACKEND_UNAVAILABLE` | Exit non-zero and report a retryable Gateway backend failure when `retryable=true`. |
| `TIMEOUT` | Exit non-zero and report timeout without retrying unless a later retry policy is explicitly approved. |
| `NOT_FOUND` | Exit non-zero and report missing public analysis status. |
| `UNEXPECTED_ERROR` | Exit non-zero and report an unexpected redacted Gateway failure. |

## Retry, Timeout And Idempotency

- CLI Gateway submission must send an idempotency key for every mutation.
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

- `GatewayOpenApiContractTest` verifies Gateway route, schema, idempotency and
  redaction contract markers.
- `ForensicAnalyticsCliTest` verifies that this CLI contract exists and
  preserves the current compatibility decision before implementation.

## Implementation Stop Conditions

Stop a later implementation slice when:

- a CLI option cannot be mapped to a Gateway field or header;
- Gateway OpenAPI lacks a required request, response or error field;
- local-path analysis would be silently routed to Gateway;
- public CLI output would expose private workspace or checkout data;
- implementation requires shared Java DTOs or Gateway implementation classes;
- retry or polling behavior is needed but not explicitly approved.
