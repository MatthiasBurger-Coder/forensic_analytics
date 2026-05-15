# Frontend Architecture Plan

## Stack

Create a standalone frontend in:

```text
forensic-ui/
```

Use:

- React
- TypeScript
- Vite
- npm scripts
- nginx runtime image for Docker

The package manager must be chosen and locked by the implementation. Once selected, commit the lockfile and document the command used.

## Source Layout

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
    dashboard/
    workspaces/
    repository-analysis/
    analysis-jobs/
    diagnostics/
    backend-unavailable/
    settings/
  widgets/
  shared/
  layouts/
```

## Domain Models

Create frontend domain models for:

- `Workspace`
- `RepositoryAnalysis`
- `AnalysisJob`
- `DiagnosticMessage`

Domain models must represent uncertainty explicitly:

- absent branch
- absent commit
- unresolved commit
- unavailable timestamps
- incomplete diagnostics
- stale status
- backend unavailable

## Application Layer

Create ports/use cases for:

- start repository analysis;
- list repository analyses;
- get analysis job detail;
- list workspaces;
- get workspace detail;
- collect current diagnostics.

Pages and widgets consume application hooks or use cases. They must not call `fetch` or parse backend DTOs directly.

## API Adapter

`adapters/api` owns:

- base URL configuration through `VITE_API_BASE_URL`;
- default local value `/api`;
- DTO parsing and mapping;
- timeout and cancellation;
- bounded retries for idempotent GET requests;
- no automatic retry for repository-analysis POST;
- error envelope mapping;
- manual retry helpers.

## UI Design

Use a technical operator-style design:

- dark theme;
- sidebar navigation;
- top status bar;
- status badges;
- dense technical tables;
- diagnostics panels;
- progress/status cards;
- concise empty states.

Do not use marketing-page structure. The first screen is the operational dashboard.

## Required States

Every data-loading screen must handle:

- initial loading;
- empty data;
- successful data;
- validation error;
- backend unavailable;
- stale data after backend loss;
- manual retry;
- route-level error fallback.

The app root must include a global ErrorBoundary. Route layouts must include route-level ErrorBoundaries.

## Diagnostics Rendering

Diagnostics are potentially sensitive. Render diagnostics as escaped text data only. Do not render raw HTML. Do not expose stack traces, tokens, private local paths or full source snippets in normal user-facing panels.
