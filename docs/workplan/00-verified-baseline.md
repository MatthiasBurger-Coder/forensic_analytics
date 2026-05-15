# Verified Baseline

This file records the repository facts verified before creating this workplan.
The implementation slice has now added REST, frontend and Docker UI files; the baseline below remains the pre-implementation reference used to avoid guessing.

## Current Slice Additions

The executed slice deliberately added:

- `forensic-analytics-rest` as a Java UI-facing inbound adapter.
- JDK `HttpServer` REST runtime wiring in `forensic-analytics-bootstrap`.
- Gson JSON mapping through `gradle/libs.versions.toml`.
- `forensic-ui` as a standalone React, TypeScript and Vite application.
- `forensic-ui/Dockerfile`, `forensic-ui/nginx.conf` and `forensic-ui/.dockerignore`.
- Frontend generated-output ignores in `.gitignore`.

The frontend remains outside `settings.gradle.kts`. Only the Java REST adapter was added as a Gradle subproject.

## Build And Modules

The repository is a Gradle multi-project build with Java 25. `settings.gradle.kts` includes:

- `forensic-analytics-domain`
- `forensic-analytics-application`
- `forensic-analytics-engine`
- `forensic-analytics-adapter-repository-source`
- `forensic-analytics-adapter-javaparser`
- `forensic-analytics-adapter-joern-docker`
- `forensic-analytics-cli`
- `forensic-analytics-testbed`
- `forensic-analytics-persistence`
- `forensic-analytics-ingestion-grpc`
- `forensic-analytics-ingestion-request`
- `forensic-analytics-bootstrap`

`build.gradle.kts` applies Java and JaCoCo to subprojects and registers `checkPackageCoverage`. A standalone frontend must not be added to `settings.gradle.kts` unless the Gradle build logic is deliberately changed to avoid applying Java and JaCoCo to the Vite app.

`QUALITY.md` defines:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

as the minimum backend quality command, and:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

as the full local backend quality gate.

No `.github/workflows` directory was found during inspection.

## Current Backend

The current executable server is gRPC-only:

- `forensic-analytics-bootstrap/src/main/java/de/burger/forensics/analytics/bootstrap/ForensicAnalyticsServerApplication.java`
- `forensic-analytics-bootstrap/src/main/java/de/burger/forensics/analytics/bootstrap/GrpcIngestionServerFactory.java`
- `forensic-analytics-bootstrap/src/main/java/de/burger/forensics/analytics/bootstrap/GrpcIngestionServerSettings.java`

No Spring Boot application, REST controller, `@RestController`, `@RequestMapping`, JAX-RS resource, `HttpServer`, WebSocket or SSE implementation was found.

The implementation must therefore create a new UI-facing HTTP/REST inbound adapter deliberately. It must not pretend that Spring Boot or REST infrastructure already exists.

## Existing Repository Analysis Paths

The gRPC contract exists in:

```text
forensic-analytics-ingestion-grpc/src/main/proto/forensic_ingestion.proto
```

`ForensicIngestionService` includes unary `AnalyzeRepository` and session lifecycle RPCs. The UI must not call this service directly in this slice.

The verified application use case for gRPC `AnalyzeRepository` is:

```text
RepositoryAnalysisIngestionUseCase#analyze(AnalyzeRepositoryCommand)
DefaultRepositoryAnalysisIngestionUseCase
AnalyzeRepositoryResult
```

That flow prepares a workspace, checks out the repository, registers an `AnalysisSession`, and returns an analysis run ID, workspace ID and checkout result.

A separate analysis pipeline exists:

```text
RunRepositoryAnalysisUseCase#run(RunRepositoryAnalysisCommand)
DefaultRunRepositoryAnalysisUseCase
RepositoryAnalysisEngine
```

That pipeline resolves repository source, scans source facts, runs semantic analysis, generates rules and stores results. Before the REST controller claims to "start analysis", the implementation must verify which workflow should be triggered for the UI MVP and document the behavior. If the MVP starts only repository ingestion/session registration, the UI must label it honestly and must not claim full analysis completion.

## Persistence And Query Gaps

Current analysis-session persistence is port-based and in-memory:

```text
AnalysisSessionRepository
InMemoryAnalysisSessionRepository
IngestionSessionRepository
InMemoryIngestionSessionRepository
```

`AnalysisSessionRepository` was initially verified with `save` and `findById`. This slice added `findAll` and `RepositoryAnalysisQueryUseCase` so list/detail REST endpoints can read through application contracts instead of persistence internals. Workspace management use cases exist, including `WorkspaceManagementUseCase#list`, but prepared repository-analysis workspaces remain a separate concern and are exposed as repository-analysis workspace views with unknown lifecycle fields kept explicit.

If UI list/detail endpoints need additional query ports or repository methods, add the smallest application-level query contract and tests. Do not expose persistence internals through REST controllers.

## Frontend And Docker

Before implementation no frontend was found:

- no `forensic-ui/`
- no root `package.json`
- no `vite.config.*`
- no `tsconfig*.json`
- no `.tsx` files
- no frontend lockfile
- no nginx UI config
- no UI Dockerfile

Docker files are currently Joern-only under `docker/joern`. No root `docker-compose.yml` was found.

`.gitignore` now includes frontend generated output such as `node_modules/`, `dist/`, `.vite/`, `.vitest/`, coverage and TypeScript build-info files under `forensic-ui`.

## Required Stop Points

Stop and report before implementation if:

- the chosen REST framework cannot be verified or justified from the task and repository constraints;
- an implementation assumes Spring Boot exists before adding it deliberately;
- a REST endpoint maps `RunRepositoryAnalysisUseCase` and `RepositoryAnalysisIngestionUseCase` incorrectly or without tests;
- UI status names are silently mapped from domain status names without an explicit REST contract;
- a worker tries to add WebSocket, SSE, gRPC-Web or direct browser gRPC;
- a worker tries to add `forensic-ui` as a Gradle subproject without adapting the root Gradle build intentionally;
- the resilience skill is missing or cannot be read.
