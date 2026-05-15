# Architecture Target

## Target Runtime Shape

```text
Browser
  -> forensic-ui container
  -> nginx static runtime
  -> HTTP/REST API base URL
  -> UI-facing backend REST adapter
  -> application use cases
  -> domain model and outbound adapters
```

The React app is a standalone frontend under:

```text
forensic-ui/
```

It is not a Gradle Java subproject in this slice unless the build logic is explicitly changed and verified.

## Backend Boundary

Create REST endpoints as UI-facing inbound adapters. Controllers may depend inward on application use cases and DTO mappers. They must not contain checkout, analysis, persistence, graph, replay, LLM or reporting business logic.

The REST adapter may delegate to verified application services such as:

- `RepositoryAnalysisIngestionUseCase`
- `RunRepositoryAnalysisUseCase`
- `WorkspaceManagementUseCase`
- any new UI query use case added by this workplan

The REST adapter must not delegate to gRPC service classes. gRPC remains a separate inbound adapter.

## Frontend Boundary

The frontend follows a hexagonal structure:

```text
forensic-ui/src/
  app/
  domain/
    workspace/
    repository-analysis/
    analysis-job/
  application/
    ports/
    usecases/
  adapters/
    api/
    ui/
  pages/
  widgets/
  shared/
  layouts/
```

Rules:

- Domain models must not depend on React.
- Pages and visual components must not call `fetch` directly.
- API calls live in `adapters/api`.
- UI components consume application use cases, hooks or view models.
- Backend DTOs must be mapped into frontend domain/view models before visual rendering.
- Diagnostics must be sanitized and rendered as data, not injected as HTML.

## UI Screens

The MVP must implement:

- Dashboard
- Workspace List
- Create Repository Analysis
- Analysis Job Detail
- Diagnostics View
- Backend Unavailable View

Navigation must include:

- Dashboard
- Workspaces
- Repository Analysis
- Analysis Jobs
- Diagnostics
- Settings placeholder

The operator UI must always answer:

- What is running?
- Which repository and commit are being analyzed?
- What failed?
- What can be retried?
- Which evidence or diagnostics exist?

## Explicit Non-Goals

Do not implement in this workplan:

- Joern UI
- AST graph UI
- Replay UI
- BTM editor
- LLM findings dashboard
- WebSocket
- SSE
- gRPC-Web
- direct browser-to-gRPC communication
- real-time log streaming
- authentication
- authorization
- production Kubernetes deployment

Future live logging may use WebSocket or SSE after a separate workplan. Internal service-to-service communication may use gRPC later, but the browser-facing UI boundary for this slice is HTTP/REST only.
