# Forensics Platform Documentation

This directory contains the architecture and product documentation for the Forensics Platform.

## Documentation Structure

- [arc42/](arc42/) - Architecture documentation based on the arc42 template
- [epics/](epics/) - Versioned product and requirement epics
- [adr/](adr/) - Architecture Decision Records, if present
- [governance/](governance/) - Reusable engineering governance flow for EPIC, arc42, workplans, skills and roles
- [workplan/](workplan/) - Current governance workplan and execution slices for the engineering governance system

## Modules

The current platform direction is server-bound repository analysis. Plugins trigger analysis on the Forensic Analytics server; Analytics prepares workspaces, checks out repositories, runs parser/Joern/BTM capabilities server-side and returns the artifacts needed by clients.

The current implementation baseline contains the technical modules for ingestion, server-side repository analysis preparation and local bootstrap/test scenarios:

- `forensic-analytics-domain` - domain model for ingestion sessions, artifact metadata, source facts and semantic Joern graph facts
- `forensic-analytics-application` - application use case contracts for ingestion and server-side repository analysis orchestration
- `forensic-analytics-engine` - engine facade for repository analysis use cases
- `forensic-analytics-logging` - cross-cutting logging module for injectable logger wrappers and Boot-runtime method interception
- `forensic-analytics-observability` - framework-neutral operational correlation and sanitized diagnostic logging support
- `forensic-analytics-adapter-repository-source` - outbound adapter for server-side workspace repository source acquisition
- `forensic-analytics-adapter-javaparser` - outbound adapter for Java source scanning with JavaParser
- `forensic-analytics-adapter-joern-docker` - outbound adapter for Docker-based Joern analysis and semantic artifact parsing
- `forensic-analytics-cli` - inbound command line adapter for local bootstrap and engine request ingestion
- `forensic-analytics-testbed` - deterministic local integration scenarios
- `forensic-analytics-persistence` - persistence adapter boundary, currently with an in-memory implementation for local bootstrap and tests
- `forensic-analytics-ingestion-grpc` - inbound gRPC adapter for plugin-triggered server-side analysis requests
- `forensic-analytics-ingestion-request` - engine request importer for plugin-produced analysis request manifests
- `forensic-analytics-rest` - UI-facing inbound HTTP/REST adapter for the React MVP
- `forensic-analytics-bootstrap` - executable bootstrap wiring for gRPC and REST servers
- `forensic-analytics-boot-app` - Spring Boot outer server boundary for typed configuration and lifecycle wiring
- `forensic-ui` - standalone React, TypeScript and Vite operator UI; it is not a Gradle subproject

### gRPC Ingestion

`forensic-analytics-ingestion-grpc` exposes `ForensicIngestionService` as an inbound adapter. It receives plugin-triggered analysis requests and repository/build context, validates transport-level fields, maps Protobuf DTOs to application commands and delegates to `ForensicIngestionUseCase`.

The service methods are:

- `AnalyzeRepository`
- `StartAnalysisSession`
- `UploadAnalysisData`
- `CompleteAnalysisSession`
- `AbortAnalysisSession`

Runtime configuration for the bootstrap module:

```properties
forensics.analytics.ingestion.grpc.enabled=true
forensics.analytics.ingestion.grpc.port=9090
```

Environment variable equivalents:

```text
FORENSICS_ANALYTICS_INGESTION_GRPC_ENABLED=true
FORENSICS_ANALYTICS_INGESTION_GRPC_PORT=9090
```

The final plugin payload schema is intentionally not part of this module. Parser execution, Joern execution, BTM generation, replay, LLM context construction and direct database logic are outside the gRPC adapter boundary.

### REST UI API

`forensic-analytics-rest` exposes UI-facing HTTP endpoints under `/api`. The adapter uses JDK `HttpServer` and Gson, delegates to application use cases and does not reuse gRPC transport classes as browser DTOs.

Implemented endpoints:

- `POST /api/repository-analyses`
- `GET /api/repository-analyses`
- `GET /api/repository-analyses/{analysisRunId}`
- `GET /api/workspaces`
- `GET /api/workspaces/{workspaceId}`

`POST /api/repository-analyses` registers and prepares a repository analysis session through `RepositoryAnalysisIngestionUseCase#analyze`. It returns backend status `REGISTERED` and workflow `REPOSITORY_SESSION_REGISTRATION`; it does not claim completion of the full repository analysis pipeline.

For this MVP, REST repository targets must be HTTPS URLs without user information.
`workspacePolicy.allowShallowClone` is supported for branch-head checkouts and uses a depth-1 single-branch clone when no commit is pinned.
Partial clone, sparse checkout, byte quotas, and ephemeral REST cleanup remain intentionally restricted until implemented end to end.

Runtime configuration:

```properties
forensics.analytics.rest.enabled=true
forensics.analytics.rest.host=127.0.0.1
forensics.analytics.rest.port=8080
```

Environment variable equivalents:

```text
FORENSICS_ANALYTICS_REST_ENABLED=true
FORENSICS_ANALYTICS_REST_HOST=127.0.0.1
FORENSICS_ANALYTICS_REST_PORT=8080
```

Run the local backend from WSL with:

```bash
./gradlew :forensic-analytics-bootstrap:run --dependency-verification strict --console=plain --stacktrace
```

The bootstrap currently uses in-memory repositories for local runs, so sessions and repository-analysis workspace views are not durable across process restarts.

### Spring Boot Server

`forensic-analytics-boot-app` is the Spring Boot outer server boundary accepted by ADR-0006. It wires existing application use cases and adapters from the outside; domain and application modules remain Spring-free.

The Boot app uses `application.properties` plus profile-specific `.properties` files for `local`, `test`, `docker` and `prod`. The default and `local` profiles bind REST and gRPC to `127.0.0.1`; `docker` and `prod` keep both inbound servers disabled until explicitly enabled. Workspace paths are validated under `forensics.analytics.workspace.root-path`.

Build the executable jar with:

```bash
./gradlew :forensic-analytics-boot-app:bootJar --dependency-verification strict --console=plain --stacktrace
```

Run it explicitly from the generated jar, for example with only gRPC enabled:

```bash
java -jar forensic-analytics-boot-app/build/libs/forensic-analytics-boot-app-0.1.0-SNAPSHOT.jar \
  --forensics.analytics.ingestion.grpc.enabled=true \
  --forensics.analytics.ingestion.grpc.host=127.0.0.1 \
  --forensics.analytics.rest.enabled=false
```

Joern Docker settings are Spring-configurable, but Boot does not wire full repository-analysis execution because production `RuleGenerationPort` and `RepositoryAnalysisResultStore` adapters are not yet verified. Joern execution remains optional and disabled by default.

### React UI

The `forensic-ui` app communicates with the backend only through HTTP/REST. Browser gRPC, gRPC-Web, WebSocket and SSE are intentionally excluded from this MVP slice.

Local frontend commands:

```bash
cd forensic-ui
npm ci
npm run dev
npm run test
npm run build
```

The default API base URL is `/api`. For local Vite development against the default backend port, run:

```bash
VITE_API_BASE_URL=http://127.0.0.1:8080/api npm run dev
```

The nginx container serves the built Vite assets with SPA fallback. It does not proxy `/api` because the repository has no root compose file or verified backend service name.

### Server-Side Repository Analysis

Repository analysis is server-bound. Analytics resolves repository source roots from server-side workspaces, scans Java source facts with the JavaParser adapter and can enrich the application result with Docker-based Joern artifacts. Joern analysis currently preserves call graph nodes and edges, methods, static call relations, control-flow relations, data-flow paths and semantic anchors as explicit semantic graph facts in the application result.

When runtime debugging requires instrumentation, Analytics owns BTM generation from the server-side analysis and instrumentation plan. The plugin may receive server-generated BTM files and bind them to the target implementation through the runtime agent so runtime information can be collected during debugging. The plugin does not generate BTM files and does not become the analysis platform.

## Current Architecture Baseline

The current architecture baseline is derived from:

- EPIC: Forensics Platform - Exception-centered Runtime Replay and LLM-assisted Error Analysis
- Version: 0.1
- Date: 2026-05-03

## Core Vision

The Forensics Platform combines static code analysis, semantic graph analysis, runtime tracing, exception replay and LLM-supported diagnosis into a controlled analysis and repair flow.

The long-term product vision is:

```text
Observe -> Replay -> Understand -> Fix -> Test -> Verify -> Deploy
```
