# Implementation Slices

Each slice must start with read-only verification of the files it will touch. Keep changes small and tied to the requested UI MVP.

## Slice 01 - Preflight And Baseline

Owner: orchestrator with read-only explorers.

Write scope: none.

Verify:

- `git status --short`
- `settings.gradle.kts`
- `build.gradle.kts`
- `QUALITY.md`
- `.agents/skills/resilience-engineering/SKILL.md`
- current `docs/workplan`
- backend bootstrap and ingestion classes
- Docker files

Done when:

- repository baseline and unknowns are recorded;
- stale workplan replacement scope is confirmed;
- no worker has started implementation.

## Slice 02 - REST Framework And Contract Decision

Owner: backend architect.

Write scope: workplan update if the decision changes.

Decision required:

- whether to introduce Spring Boot as the requested REST/BFF runtime;
- which module owns the REST adapter;
- whether to create a new inbound REST module or extend `forensic-analytics-bootstrap`;
- how the REST runtime coexists with the existing gRPC bootstrap;
- which application use case starts the user-visible repository analysis action.

Done when:

- REST framework and module ownership are explicit;
- dependency additions are justified by the task;
- version catalog/build impact is known;
- no REST controller relies on gRPC service classes;
- status mapping is documented.

Stop if no safe REST runtime choice can be made from verified repository evidence and the user task.

## Slice 03 - Backend REST Adapter

Owner: backend REST worker.

Write scope:

- new or verified backend REST adapter package/module;
- backend DTOs and mappers;
- backend REST tests;
- bootstrap wiring for the REST runtime.

Implement:

- `POST /api/repository-analyses`
- `GET /api/repository-analyses`
- `GET /api/repository-analyses/{analysisRunId}`
- `GET /api/workspaces`
- `GET /api/workspaces/{workspaceId}`
- health/readiness endpoint only if the selected framework requires it for Docker readiness.

Rules:

- Controllers delegate to application services or UI query use cases.
- Controllers perform transport validation and mapping only.
- No business logic, checkout logic, parser logic, persistence internals or gRPC service calls in controllers.
- Validation errors and dependency failures must map to explicit sanitized error responses.
- Do not automatically retry analysis-start commands.

Done when:

- endpoint tests cover success, validation failure, not found and sanitized diagnostics;
- REST DTOs preserve evidence boundaries;
- backend package boundaries remain valid.

## Slice 04 - Backend Query Support

Owner: backend query worker.

Write scope:

- application query contracts;
- persistence adapter query support;
- tests for list/detail behavior.

Implement only if verified necessary:

- list analysis sessions/runs for the Workspace List and Analysis Job Detail screens;
- get analysis session/run by ID;
- list workspaces and get workspace by ID through existing use cases or a small query use case.

Rules:

- Do not expose repository internals directly to controllers.
- Do not invent durable storage schemas.
- If persistence remains in-memory, document the limitation and keep UI copy honest.

Done when:

- list/detail endpoints have application-level contracts;
- tests prove deterministic ordering and missing-data behavior.

## Slice 05 - Create `forensic-ui` App

Owner: frontend foundation worker.

Write scope:

- `forensic-ui/`
- frontend package configuration and lockfile;
- frontend TypeScript, Vite and test/lint configuration;
- `.gitignore` entries for frontend generated output.

Create:

- React + TypeScript + Vite app;
- `VITE_API_BASE_URL` support with default `/api`;
- routing foundation;
- app shell;
- global error boundary;
- route-level error boundary;
- dark operator theme;
- sidebar and top status bar.

Rules:

- Keep `forensic-ui` standalone.
- Do not add it to Gradle settings unless build logic is explicitly adapted.
- Use stable scripts: `build`, `test` if configured, `lint` if configured.

Done when:

- `npm install` or `npm ci` path is documented;
- `npm run build` succeeds;
- the app can render the route shell.

## Slice 06 - Frontend Domain, Ports And API Client

Owner: frontend foundation worker.

Write scope:

- `forensic-ui/src/domain`
- `forensic-ui/src/application`
- `forensic-ui/src/adapters/api`

Create models for:

- `Workspace`
- `RepositoryAnalysis`
- `AnalysisJob`
- `DiagnosticMessage`

Create:

- application ports for repository analyses, workspaces and diagnostics;
- API adapter that maps backend DTOs to frontend domain models;
- timeout support using abort/cancellation;
- safe error mapping;
- bounded retry for idempotent GET requests;
- no automatic retry for POST analysis start;
- manual retry hooks.

Done when:

- pages/components do not import backend DTOs;
- unit tests cover timeout, retry/no-retry and error mapping;
- diagnostics are sanitized before display.

## Slice 07 - Implement Screens

Owner: frontend screen workers after Slice 06.

Write scope:

- route-specific `pages`
- route-specific `widgets`
- route-specific tests.

Implement:

- Dashboard
- Workspace List
- Create Repository Analysis
- Analysis Job Detail
- Diagnostics View
- Backend Unavailable View
- Settings placeholder navigation target

Rules:

- Empty states must be honest and must not fake successful analyses.
- Loading, error and unavailable states must be visible.
- Duplicate submit prevention is required for repository-analysis start.
- Forms must expose repository URL, optional branch, optional commit and optional workspace name.
- Technical tables and status badges must stay readable on supported viewport sizes.

Done when:

- all routes render;
- screen tests cover empty, loading, error and data states;
- Create Repository Analysis navigates or displays the returned analysis/session result.

## Slice 08 - Polling And Stale Status

Owner: analysis-job frontend worker.

Write scope:

- analysis-job hooks and widgets;
- shared polling utility if approved by frontend foundation owner.

Implement:

- bounded polling interval for job detail/status;
- stop polling on terminal states;
- manual refresh button;
- stale-data warning when backend becomes unavailable after prior data was loaded.

Terminal UI states required by the task:

- `SUCCESS`
- `FAILED`
- `CANCELED`
- `CLEANED`

Mapping from existing backend states such as `COMPLETED`, `FAILED`, `DEAD_LETTERED` or `REGISTERED` must be explicit in the REST contract and tested. Do not silently rename domain states in the UI.

Done when:

- polling stops for terminal states;
- backend unavailability preserves stale data with a warning;
- no retry loop survives route unmount.

## Slice 09 - Dockerize `forensic-ui`

Owner: Docker worker.

Write scope:

- `forensic-ui/Dockerfile`
- `forensic-ui/nginx.conf`
- `forensic-ui/.dockerignore`
- compose file only if a root compose file is deliberately created or a verified compose file exists.

Implement:

- multi-stage Node build;
- nginx runtime serving the static Vite build;
- SPA fallback;
- API proxy behavior if needed for `/api`;
- container port `80`, host mapping plan `3000:80`.

Rules:

- Do not assume a backend compose service name. If no compose file exists, document the suggested service instead of wiring to a fictional backend name.
- Container logs and nginx errors must not expose secrets.

Done when:

- Docker build succeeds or blocker is recorded;
- nginx config is deterministic and minimal;
- local run docs explain API base URL behavior.

## Slice 10 - Documentation

Owner: documentation worker.

Write scope:

- `docs/`
- `forensic-ui/README.md` if created.

Document:

- how to run the UI locally;
- how to configure `VITE_API_BASE_URL`;
- how the UI communicates with backend REST;
- why gRPC/WebSocket/SSE are excluded in this slice;
- future live logging via WebSocket/SSE;
- backend REST runtime and startup commands;
- known limitations, including in-memory persistence if still true.

Done when:

- docs match verified code and commands;
- no stale gRPC/WildFly workplan content remains in `docs/workplan`.

## Slice 11 - Quality Gate

Owner: quality reviewer.

Run the narrowest meaningful checks first, then broaden:

Frontend:

```bash
cd forensic-ui
npm install
npm run build
npm run test
npm run lint
```

Run `test` and `lint` only if configured.

Backend:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full backend gate when feasible:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Always run:

```bash
git diff --check
```

Done when:

- exact commands and results are recorded;
- any skipped command has a reason;
- failures are triaged as caused by current changes or pre-existing.

## Slice 12 - Commit And Push

Owner: git operator after quality review.

Rules:

- Create a branch if on `main`.
- Inspect `git status --short` and `git diff`.
- Stage only task-related files.
- Do not stage generated artifacts such as `node_modules`, frontend `dist`, coverage output or local logs.
- Verify WSL EOL status before staging.

Commit message format:

```text
Why:
What:
How:
Verification:
Impact:
Limitations:
```

Push only when repository rules allow it and the user requested it.
