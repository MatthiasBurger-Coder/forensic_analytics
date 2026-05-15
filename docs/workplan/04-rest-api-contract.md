# REST API Contract Plan

The REST API is UI-facing. It exists to serve the React MVP and must delegate to application use cases. It must not replace gRPC ingestion and must not reuse gRPC transport classes as the browser contract.

## Implemented Runtime

The MVP uses `forensic-analytics-rest`, a UI-facing inbound adapter implemented with JDK `HttpServer` and Gson. It is deliberately not Spring Boot. Bootstrap owns wiring to persistence and gRPC; the REST module depends inward on application/domain contracts and must not depend on gRPC or persistence.

## Implemented Endpoints

```text
POST /api/repository-analyses
GET  /api/repository-analyses
GET  /api/repository-analyses/{analysisRunId}
GET  /api/workspaces
GET  /api/workspaces/{workspaceId}
```

These endpoint paths are implemented under `/api`.

## Repository Analysis Start

Request body:

```json
{
  "repositoryUrl": "https://example.invalid/project.git",
  "provider": "github",
  "branch": "main",
  "commit": null,
  "requestId": "request-1",
  "schemaVersion": "schema-v1",
  "buildContext": {
    "buildTool": "gradle",
    "buildId": "build-1",
    "rootProjectName": null,
    "declaredModules": [":app"],
    "attributes": {}
  },
  "workspacePolicy": {
    "ephemeral": false,
    "allowShallowClone": false,
    "allowPartialClone": false,
    "allowSparseCheckout": false,
    "timeoutSeconds": 60,
    "maxWorkspaceBytes": 0
  }
}
```

Rules:

- `repositoryUrl` is required and must be an HTTPS URL without user information.
- `requestId`, `schemaVersion`, `buildContext` and `workspacePolicy` are required because `AnalyzeRepositoryCommand` requires them.
- At least one of `branch` or `commit` is required.
- `provider`, `branch`, `commit`, `buildContext.rootProjectName`, `buildContext.declaredModules` entries and `buildContext.attributes` are mapped only when nonblank.
- `workspaceName` has no verified storage target in this slice. A nonblank value is rejected with `VALIDATION_ERROR` instead of being silently ignored.
- Workspace clone-mode options are accepted only as explicit unsupported-safe values: `ephemeral`, `allowShallowClone`, `allowPartialClone` and `allowSparseCheckout` must be `false`, and `maxWorkspaceBytes` must be `0`. Non-default values are rejected until checkout mode, quota enforcement and cleanup semantics are implemented and tested.
- Empty optional fields must map to explicit absent values, not blank domain values.
- The backend accepts the caller-provided `requestId`; it does not generate hidden request IDs for this command.
- Do not automatically retry this endpoint from the UI unless an idempotency mechanism exists.

Response body includes the UI-visible result from verified application evidence:

```json
{
  "analysisRunId": "analysis-run-id",
  "workspaceId": "workspace-id",
  "repositoryUrl": "https://example.invalid/project.git",
  "branch": "main",
  "commit": null,
  "resolvedRemoteUrl": "https://example.invalid/project.git",
  "resolvedCommit": "resolved-commit",
  "checkoutStatus": "CHECKED_OUT",
  "status": "REGISTERED",
  "workflow": "REPOSITORY_SESSION_REGISTRATION",
  "createdAt": null,
  "sourceRoots": ["src/main/java"],
  "diagnostics": []
}
```

`POST /api/repository-analyses` registers and prepares a repository analysis session through `RepositoryAnalysisIngestionUseCase#analyze`. It does not run or claim completion of the full `RunRepositoryAnalysisUseCase` pipeline.

## Repository Analyses List

`GET /api/repository-analyses` supports the Dashboard and analysis-job polling. Results are deterministic and ordered by analysis run ID because current analysis-session models do not expose timestamps. `createdAt` is `null` until a verified timestamp model exists.

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

`GET /api/workspaces` and `GET /api/workspaces/{workspaceId}` expose repository-analysis workspace views derived from analysis sessions. They are not lifecycle `WorkspaceManagementUseCase` workspaces. Unknown workspace name, status and timestamps are returned as `null`.

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

The frontend maps backend status values to UI lifecycle states in a tested adapter. Backend REST responses expose exact backend status values and the workflow name.

The task requires UI terminal states:

```text
SUCCESS
FAILED
CANCELED
CLEANED
```

Current verified backend states include values such as `REGISTERED`, `COMPLETED`, `FAILED`, `DEAD_LETTERED`, `ACCEPTED`, `DISPATCHABLE`, `RUNNING` and `RETRYABLE` in different domain areas.

Implemented frontend mapping:

- `COMPLETED` -> `SUCCESS`
- `FAILED` and `DEAD_LETTERED` -> `FAILED`
- `CLEANED` -> `CLEANED`
- `REGISTERED`, `ACCEPTED`, `DISPATCHABLE`, `RUNNING` and `RETRYABLE` stay nonterminal.
- `CANCELED` is mapped only if a backend response explicitly contains that status.

No backend state is silently renamed in the REST response.
