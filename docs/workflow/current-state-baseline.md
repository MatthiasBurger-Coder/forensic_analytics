# Current State Baseline

## Repository And Branch

- Repository root: `/mnt/d/Projects/forensic_analytics`
- Windows path: `D:/Projects/forensic_analytics`
- Active workflow branch:
  `architecture/microservices-ecosystem-conversion-20260516`
- Root project name: `forensic-analytics`
- Build files: `settings.gradle.kts`, `build.gradle.kts`
- Java baseline: 25
- Gradle baseline from `QUALITY.md`: 9.4.0

## Current Gradle Modules

Verified from `settings.gradle.kts`:

- `forensic-analytics-domain`
- `forensic-analytics-application`
- `forensic-analytics-engine`
- `forensic-analytics-logging`
- `forensic-analytics-observability`
- `forensic-analytics-adapter-repository-source`
- `forensic-analytics-adapter-javaparser`
- `forensic-analytics-adapter-joern-docker`
- `forensic-analytics-cli`
- `forensic-analytics-testbed`
- `forensic-analytics-persistence`
- `forensic-analytics-ingestion-grpc`
- `forensic-analytics-ingestion-request`
- `forensic-analytics-rest`
- `forensic-analytics-bootstrap`
- `forensic-analytics-boot-app`

These are current modular-platform modules. They are not independently
deployable microservices unless a later slice proves independent build, start,
test, configuration, healthcheck, container and deployment evidence.

## Current Frontend

- Verified frontend root: `forensic-ui`
- Verified package file: `forensic-ui/package.json`
- Verified scripts:
  - `npm test`
  - `npm run build`

The user target names `frontend/frontend-web-app`. Any move from `forensic-ui`
to that target must be a verified frontend migration slice, not an assumption.

## Current Contracts

- Verified protobuf file:
  `forensic-analytics-ingestion-grpc/src/main/proto/forensic_ingestion.proto`
- Verified gRPC service:
  `ForensicIngestionService`
- Verified RPCs:
  - `AnalyzeRepository`
  - `StartAnalysisSession`
  - `UploadAnalysisData`
  - `CompleteAnalysisSession`
  - `AbortAnalysisSession`

The target `contracts/grpc/**` structure does not currently exist. It may only
contain contract files and contract documentation, never shared Java
implementation code.

## Current Docker And Deployment Material

- Verified Joern Dockerfile: `docker/joern/Dockerfile`
- Verified Joern compose file: `docker/joern/docker-compose.joern.yml`

No verified `services/**`, `deployment/docker-compose/**`,
`deployment/docker-swarm/**` or `deployment/kubernetes/**` target structure
exists at workflow creation time.

## Current Capability Map

| Capability | Current Evidence |
|---|---|
| Repository checkout and workspace preparation | `forensic-analytics-adapter-repository-source`, application ingestion ports and services |
| Java AST analysis | `forensic-analytics-adapter-javaparser` |
| Joern / CPG semantic analysis | `forensic-analytics-adapter-joern-docker`, `docker/joern/**` |
| BTM / rule generation | `RuleGenerationPort`, `RuleGenerationRequest`, `RuleGenerationResult`, tests using `.btm` artifacts |
| gRPC ingestion | `forensic-analytics-ingestion-grpc` and `forensic_ingestion.proto` |
| REST API | `forensic-analytics-rest`, `forensic-analytics-boot-app` wiring |
| In-memory persistence | `forensic-analytics-persistence` |
| Workspace, project, analysis and ingestion application logic | `forensic-analytics-application` |
| Domain models | `forensic-analytics-domain` |
| CLI | `forensic-analytics-cli` |
| Observability and logging | `forensic-analytics-observability`, `forensic-analytics-logging` |
| Graph, replay and LLM target concepts | Documented in arc42 and ADRs; only partial model evidence currently verified in domain/application code |

## Current Coupling Snapshot

Verified Gradle dependencies show a modular-monolith shape:

- Adapters depend on `forensic-analytics-application` and often
  `forensic-analytics-domain`.
- `forensic-analytics-boot-app` wires adapters, persistence, REST, gRPC,
  logging and observability into one application runtime.
- `forensic-analytics-ingestion-grpc` currently has an `api` dependency on
  `forensic-analytics-application`.
- `forensic-analytics-persistence` depends on application, domain and
  observability.

This coupling is expected for the current platform. It must be dismantled only
through contract-first service slices with tests and migration records.
