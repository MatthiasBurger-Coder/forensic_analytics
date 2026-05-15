# Subagents And Parallelization

The work should be split across subagents wherever the dependency graph allows. Shared contracts must be stabilized before dependent workers begin write-heavy implementation.

## Dependency Graph

```text
Slice 01 Verify baseline
  -> Slice 02 REST framework and API contract decision
    -> Slice 03 Backend REST adapter
    -> Slice 04 Frontend API contract models

Slice 01 Verify baseline
  -> Slice 05 Frontend scaffold
    -> Slice 06 Frontend domain/application/API client
      -> Slice 07 Screens
      -> Slice 08 Polling and stale state

Slice 05 Frontend scaffold
  -> Slice 09 Docker/nginx

Slice 03 Backend REST adapter
Slice 07 Screens
Slice 08 Polling
Slice 09 Docker/nginx
  -> Slice 10 Documentation
  -> Slice 11 Verification
  -> Slice 12 Commit and push
```

## Parallel Waves

### Wave 0 - Read-Only Verification

Run in parallel:

- Backend explorer: verify REST absence, gRPC ingestion, `AnalyzeRepository`, `RunRepositoryAnalysisUseCase`, persistence/query gaps.
- Frontend/Docker explorer: verify absence of frontend, Docker Compose, nginx and stale workplan files.
- Build/Quality explorer: verify Gradle modules, quality gates, resilience skill and CI state.

No writes are allowed in this wave.

### Wave 1 - Contract Stabilization

Mostly sequential, with parallel read-only support:

- Orchestrator owns the REST contract, status vocabulary, route map and write-scope boundaries.
- Backend contract reviewer verifies the selected REST framework/module and application use case mapping.
- Frontend contract reviewer verifies TypeScript domain models against the proposed REST DTOs.
- Quality reviewer identifies the narrowest backend and frontend commands.

Do not start backend/frontend write workers until the REST DTO names, route paths and status mapping rules are stable.

### Wave 2 - Disjoint Implementation

Run in parallel after Wave 1:

- Backend REST worker writes only backend REST adapter, bootstrap wiring and backend tests.
- Frontend foundation worker writes only `forensic-ui` scaffold, routing, layout, theme, shared components and API foundation.
- Docker worker writes only `forensic-ui/Dockerfile`, `forensic-ui/nginx.conf`, `forensic-ui/.dockerignore` and compose integration if a compose file exists or is deliberately created.
- Documentation worker writes only implementation docs and keeps this workplan aligned.
- Quality reviewer stays read-only until implementation workers produce testable changes.

### Wave 3 - Feature Screens

Run in parallel only after frontend foundation and API contracts are in place:

- Dashboard/overview worker owns `forensic-ui/src/pages/dashboard` and related widgets.
- Workspace worker owns `forensic-ui/src/pages/workspaces` and workspace widgets.
- Repository-analysis form worker owns `forensic-ui/src/pages/repository-analysis`.
- Analysis-job worker owns `forensic-ui/src/pages/analysis-jobs` and polling hooks.
- Diagnostics worker owns `forensic-ui/src/pages/diagnostics`, unavailable view and safe diagnostics widgets.

Workers may share common components only through contracts created by the frontend foundation worker. If a shared component needs to change, route it through the orchestrator.

### Wave 4 - Integration And Review

Run in parallel where possible:

- Backend reviewer checks controller thinness, application boundaries and REST tests.
- Frontend reviewer checks no direct `fetch` in pages/components, no DTO leakage, no secret rendering and no UI overlap.
- Resilience reviewer checks timeouts, retries, stale data, manual retry, duplicate submit prevention and terminal polling behavior.
- Docker reviewer checks nginx SPA fallback, API proxy behavior and container build.
- Quality reviewer runs or triages the required commands.

## Suggested Subagent Roles

| Role | Responsibility | Write Scope |
| --- | --- | --- |
| Swarm orchestrator | Maintains dependency graph, ownership and handoff artifacts. | Workplan and coordination notes only. |
| Backend REST worker | Adds REST inbound adapter and backend tests. | New REST module or verified bootstrap REST files; backend tests. |
| Backend query worker | Adds missing application query contracts for UI list/detail if verified necessary. | Application query contracts, persistence adapter updates, tests. |
| Frontend foundation worker | Creates Vite app, shell, routing, layout, API client foundation. | `forensic-ui/src/app`, `layouts`, `shared`, `adapters/api`, package config. |
| Frontend screen workers | Implement route-specific screens after shared contracts exist. | Route-specific `pages` and widgets only. |
| Docker worker | Dockerfile, nginx config, compose integration and ignore files. | `forensic-ui` Docker files and compose files. |
| Documentation worker | Updates docs for local start, API communication and non-goals. | `docs/` and workplan files. |
| Quality reviewer | Runs targeted and full gates, records command evidence. | Read-only until fixes are routed. |
| Security/resilience reviewer | Checks diagnostic redaction, retry safety, stale status and no secret leakage. | Review comments or routed fixes only. |

## Conflict Rules

- No two write workers may edit the same file unless the orchestrator serializes the work.
- Shared TypeScript types, API mappers, REST DTOs and package scripts are contract files and require orchestrator approval before parallel edits.
- Backend controller names, package names and framework classes must be verified from the chosen implementation before use.
- If a subagent finds a mismatch against this workplan, it must stop and report expected versus found files.
