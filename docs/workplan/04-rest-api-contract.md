# REST API Contract Plan

The REST API is UI-facing. It exists to serve the React MVP and must delegate to application use cases. It must not replace gRPC ingestion and must not reuse gRPC transport classes as the browser contract.

## Proposed Endpoints

```text
POST /api/repository-analyses
GET  /api/repository-analyses
GET  /api/repository-analyses/{analysisRunId}
GET  /api/workspaces
GET  /api/workspaces/{workspaceId}
```

These endpoint paths are proposed by the task. Implementation must verify the selected REST framework and package names before creating controller classes.

## Repository Analysis Start

Request body:

```json
{
  "repositoryUrl": "https://example.invalid/project.git",
  "branch": "main",
  "commit": "",
  "workspaceName": "Optional workspace name"
}
```

Rules:

- `repositoryUrl` is required.
- `branch`, `commit` and `workspaceName` are optional.
- Empty optional fields must map to explicit absent values, not blank domain values.
- The backend must generate or accept a request ID according to a verified idempotency decision.
- Do not automatically retry this endpoint from the UI unless an idempotency mechanism exists.

Response body should include the UI-visible result from verified application evidence:

```json
{
  "analysisRunId": "analysis-run-id",
  "workspaceId": "workspace-id",
  "repositoryUrl": "https://example.invalid/project.git",
  "branch": "main",
  "commit": null,
  "resolvedCommit": "resolved-commit",
  "checkoutStatus": "CHECKED_OUT",
  "status": "REGISTERED",
  "sourceRoots": ["src/main/java"],
  "diagnostics": []
}
```

Status values in this example are not final. They must be verified and mapped explicitly during implementation.

## Repository Analyses List

`GET /api/repository-analyses` supports the Workspace List and Dashboard. The response should be deterministic, preferably ordered by created or started time descending when that evidence exists. If timestamps are not available in current domain models, the endpoint must either expose `null` or add a verified application-level timestamp model. Do not invent timestamps.

Fields needed by the UI:

- workspace ID
- repository URL
- branch
- commit
- resolved commit
- status
- created at or started at if available
- last diagnostics

## Repository Analysis Detail

`GET /api/repository-analyses/{analysisRunId}` supports Analysis Job Detail and status polling.

Fields needed by the UI:

- analysis run ID
- workspace ID
- checkout status
- resolved commit
- source roots
- diagnostics
- current status
- stale marker provided by frontend state, not by backend unless the backend can prove staleness

## Workspaces

`GET /api/workspaces` and `GET /api/workspaces/{workspaceId}` must use verified workspace application contracts. If prepared repository-analysis workspaces are not represented by `WorkspaceManagementUseCase`, add a thin application query endpoint or document a placeholder adapter. Do not merge unrelated workspace concepts silently.

## Errors

Use a stable sanitized error envelope:

```json
{
  "code": "BACKEND_UNAVAILABLE",
  "message": "Backend is unavailable",
  "retryable": true,
  "correlationId": "optional-correlation-id",
  "diagnostics": []
}
```

Rules:

- Do not expose stack traces, credentials, tokens, local absolute paths or raw repository contents.
- Validation failures are not retryable.
- Backend unavailable, timeout and transient dependency failures may be retryable for idempotent GET requests.
- Preserve correlation/request IDs when available.

## Status Mapping

The task requires UI terminal states:

```text
SUCCESS
FAILED
CANCELED
CLEANED
```

Current verified backend states include values such as `REGISTERED`, `COMPLETED`, `FAILED`, `DEAD_LETTERED`, `ACCEPTED`, `DISPATCHABLE`, `RUNNING` and `RETRYABLE` in different domain areas.

Implementation must choose one of these approaches:

1. expose backend domain states exactly and let the frontend map them through a tested adapter; or
2. define a REST-specific UI status vocabulary and map backend states to it in tested controller/mapper code.

Do not silently treat `COMPLETED` as `SUCCESS`, `DEAD_LETTERED` as `FAILED`, or `REGISTERED` as `RUNNING` without explicit tests and documentation.
