# Resilient React UI MVP Workplan

This workplan replaces the previous workspace and gRPC integration plan. The current target is the first resilient UI MVP for Forensic Analytics:

```text
Browser
  -> forensic-ui Docker container
  -> HTTP/REST UI-facing backend API
  -> application use cases
  -> existing repository analysis and ingestion workflows
```

The React UI must communicate with the backend only through HTTP/REST in this slice. It must not use direct gRPC, gRPC-Web, WebSocket, SSE or browser-to-gRPC communication.

## Implementation Status

This workplan has been executed for the MVP slice. The current implementation adds:

- `forensic-analytics-rest`, a UI-facing Java inbound REST adapter based on JDK `HttpServer` and Gson.
- REST endpoints under `/api/repository-analyses` and `/api/workspaces`.
- Shared bootstrap wiring so gRPC and REST use the same in-memory analysis-session repository.
- `forensic-ui`, a standalone React, TypeScript and Vite application that is not a Gradle subproject.
- `forensic-ui/Dockerfile`, `forensic-ui/nginx.conf` and `forensic-ui/.dockerignore`.

The UI-visible repository action registers and prepares a repository analysis session. It does not claim that the full repository analysis pipeline has completed.

## Verified Starting Point

Read-only inspection found:

- Before this slice, no `forensic-ui` directory, React, Vite, TypeScript, nginx UI runtime or frontend package scripts existed.
- No root Docker Compose file exists. Existing non-UI Docker material is Joern-specific under `docker/joern`.
- Before this slice, no Spring Boot application, REST controller, REST endpoint or HTTP server implementation existed.
- The current executable backend is `forensic-analytics-bootstrap`, which starts a gRPC ingestion server.
- The verified unary gRPC `AnalyzeRepository` path delegates to `RepositoryAnalysisIngestionUseCase`.
- A separate repository analysis pipeline exists through `RunRepositoryAnalysisUseCase`.
- Workspace and analysis-session persistence currently use in-memory adapters.
- `QUALITY.md` defines the Gradle minimum and full local quality gates.
- `.agents/skills/resilience-engineering/SKILL.md` exists and must be applied by the implementation slices.

These facts are recorded in [00-verified-baseline.md](00-verified-baseline.md). If implementation discovers different facts, stop and update this workplan before proceeding.

## Workplan Files

1. [00-verified-baseline.md](00-verified-baseline.md) - verified repository state and stop points.
2. [01-architecture-target.md](01-architecture-target.md) - target architecture and non-goals.
3. [02-subagents-and-parallelization.md](02-subagents-and-parallelization.md) - dependency graph, ownership map and parallel execution plan.
4. [03-implementation-slices.md](03-implementation-slices.md) - ordered implementation slices with owners, write scopes and done criteria.
5. [04-rest-api-contract.md](04-rest-api-contract.md) - proposed UI-facing REST contract and verification rules.
6. [05-frontend-architecture.md](05-frontend-architecture.md) - React/Vite structure, routing, state and UI rules.
7. [06-resilience-requirements.md](06-resilience-requirements.md) - frontend, backend and Docker resilience requirements.
8. [07-docker-and-local-start.md](07-docker-and-local-start.md) - Docker, nginx and local run plan.
9. [08-verification-and-quality-gates.md](08-verification-and-quality-gates.md) - frontend and backend verification commands.
10. [09-commit-and-push-plan.md](09-commit-and-push-plan.md) - commit and push workflow.

## Execution Rule

Work as much in parallel as the dependency graph allows, but keep write ownership disjoint. Shared contracts must be stabilized before multiple workers edit dependent code. No worker may invent REST framework names, endpoint DTOs, status mappings, Gradle tasks, package names or frontend scripts that were not created and verified in an earlier slice.
