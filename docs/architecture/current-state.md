# Current State

## Status

Slice 00 baseline for the microservices ecosystem conversion workflow.

This document records the verified repository state before service-boundary,
contract, deployment or production-code migration slices start. The current
repository is a Gradle modular monolith with a separate React frontend. The
current modules must not be described as independently deployable
microservices.

## Verification Sources

Verified from:

- `settings.gradle.kts`
- root and module `build.gradle.kts` files
- `QUALITY.md`
- `docs/workflow/workflow.md`
- `docs/workflow/current-state-baseline.md`
- `docs/architecture/microservice-governance.md`
- `docs/arc42/**`
- `docs/adr/**`
- production and test source trees under `forensic-analytics-*`
- `forensic-analytics-ingestion-grpc/src/main/proto/forensic_ingestion.proto`
- `docker/**`
- `forensic-ui/**`

Required Slice 00 role reviews were completed by Senior System Architect,
Microservice Senior Expert, Senior Java Backend Developer, Senior DevOps
Engineer and Senior Tester. The reviews reported no blocker for documenting
the current state.

## Repository Shape

| Area | Current Evidence | Current Status |
|---|---|---|
| Java build | `settings.gradle.kts`, root `build.gradle.kts` | One Gradle multi-project build |
| Frontend | `forensic-ui/package.json` | Separate Vite/React project, not a Gradle module |
| Boot runtime | `forensic-analytics-boot-app` | One Spring Boot application boundary for the current platform |
| Bootstrap runtime | `forensic-analytics-bootstrap` | Non-Spring server composition for gRPC and REST |
| gRPC contract | `forensic-analytics-ingestion-grpc/src/main/proto/forensic_ingestion.proto` | Proto is inside the current Java implementation module |
| REST API | `forensic-analytics-rest` | JDK HTTP server adapter |
| Persistence | `forensic-analytics-persistence` | In-memory stores and repositories |
| Docker | `docker/boot-app`, `docker/joern`, `forensic-ui/Dockerfile` | Existing container material, not a service ecosystem |
| Service roots | `services/` | Not present |
| External contracts root | `contracts/` | Not present |
| Deployment roots | `deployment/`, Kubernetes, Swarm, Helm roots | Not present |

## Current Gradle Modules

Verified modules from `settings.gradle.kts`:

| Module | Current Responsibility |
|---|---|
| `forensic-analytics-domain` | Forensic IDs and domain models for analysis, ingestion, repository, semantic graph, source, workspace, audit and artifacts |
| `forensic-analytics-application` | Use cases, commands, ports, query/result models and orchestration for analysis, ingestion, workspace, project, assets, audit, retention, canvas and security |
| `forensic-analytics-engine` | In-process repository analysis engine wrapper around the application use case |
| `forensic-analytics-logging` | Cross-cutting logging infrastructure and optional Spring logging integration |
| `forensic-analytics-observability` | Correlation context and sanitized operation logging support |
| `forensic-analytics-adapter-repository-source` | Git checkout, local repository source and workspace preparation adapters |
| `forensic-analytics-adapter-javaparser` | JavaParser source scanning adapter |
| `forensic-analytics-adapter-joern-docker` | Joern Docker command, artifact and semantic-analysis adapter |
| `forensic-analytics-cli` | CLI commands for analysis and ingestion-request import |
| `forensic-analytics-testbed` | In-process integration and architecture tests across current modules |
| `forensic-analytics-persistence` | In-memory implementations of application persistence ports and isolated storage path resolving |
| `forensic-analytics-ingestion-grpc` | gRPC inbound adapter, generated protobuf classes, mappers and validators |
| `forensic-analytics-ingestion-request` | JSON engine-ingestion request reader and importer |
| `forensic-analytics-rest` | JDK REST API server, HTTP handler and JSON DTO mapping |
| `forensic-analytics-bootstrap` | Manual gRPC and REST server assembly |
| `forensic-analytics-boot-app` | Spring Boot application, configuration and lifecycle wiring |

## Current Capability Map

| Capability | Verified Current Evidence | Migration Note |
|---|---|---|
| Repository checkout and workspace preparation | `forensic-analytics-adapter-repository-source`, application ingestion ports and services | Candidate ownership for repository-analysis service, but currently in-process |
| Java AST analysis | `forensic-analytics-adapter-javaparser` and `JavaParserSourceScanner` | Candidate ownership for Java AST analysis service |
| Joern / CPG semantic analysis | `forensic-analytics-adapter-joern-docker`, `docker/joern/**` | Joern runtime is optional Docker infrastructure, not an independent service |
| BTM / rule generation | `RuleGenerationPort`, `RuleGenerationRequest`, `RuleGenerationResult`, `.btm` tests and arc42 notes | No standalone generator module is currently verified |
| gRPC ingestion | `forensic-analytics-ingestion-grpc`, `forensic_ingestion.proto`, `ForensicIngestionGrpcService` | Current proto and adapter live inside one Java implementation module |
| REST API | `forensic-analytics-rest`, Boot REST lifecycle wiring | Current REST adapter is in-process, not a gateway service |
| Persistence | `forensic-analytics-persistence` | In-memory implementation, not an owned durable store service |
| Boot runtime | `forensic-analytics-boot-app`, `docker/boot-app/**` | One platform runtime; no accepted health endpoint exists yet |
| Manual server runtime | `forensic-analytics-bootstrap` | Alternative combined gRPC/REST process |
| CLI | `forensic-analytics-cli` | In-process command adapter |
| Frontend | `forensic-ui` | Existing Vite/React frontend outside planned `frontend/frontend-web-app` root |
| Observability and logging | `forensic-analytics-observability`, `forensic-analytics-logging` | Shared Java modules in current monolith; future services must not share them as runtime code |
| Graph, replay, report and LLM target concepts | arc42 docs, ADRs and partial domain/application model evidence | Not verified as standalone runtime capabilities or services |

## Current Runtime Composition

`forensic-analytics-boot-app` is the primary Spring Boot boundary. It wires
application use cases, repository-source adapters, in-memory persistence, gRPC,
REST, logging, observability and Joern settings into one application process.

`forensic-analytics-bootstrap` is a second combined runtime path. It manually
assembles gRPC and REST servers with the same in-memory repositories and
application use cases.

Neither runtime provides evidence for independently deployable services. The
current Docker Boot app image packages the Boot jar, exposes REST and gRPC
ports, and documents that no healthcheck is present because no accepted
Actuator endpoint exists.

## Current Frontend

The verified frontend root is `forensic-ui`.

Verified scripts from `forensic-ui/package.json`:

- `npm run dev`
- `npm run build`
- `npm run preview`
- `npm test`
- `npm run test:watch`

`forensic-ui/Dockerfile` builds the frontend with `npm ci` and
`npm run build`, then serves the built assets with nginx. The planned
`frontend/frontend-web-app` root exists only as a Slice 02 placeholder. It has
no frontend implementation, package metadata, build configuration or tests.

## Current Deployment Material

Verified existing deployment-related material:

- `docker/boot-app/Dockerfile`
- `docker/boot-app/README.md`
- `docker/joern/Dockerfile`
- `docker/joern/docker-compose.joern.yml`
- `docker/joern/scripts/**`
- `forensic-ui/Dockerfile`

Missing target material:

- `services/**`
- `contracts/**`
- `deployment/docker-compose/**`
- `deployment/docker-swarm/**`
- `deployment/kubernetes/**`
- Helm or chart roots
- CI workflow files under `.github/workflows`

Docker Compose currently exists for Joern tooling only. There is no verified
local microservices compose landscape, Swarm stack, Kubernetes manifest set or
service-by-service container readiness evidence.

## Microservice Readiness Baseline

The current repository has limited platform-level runtime evidence:

- one Spring Boot app entry point;
- one Boot container baseline;
- one source-owned gRPC proto;
- one React frontend package and container baseline;
- Joern Docker tooling.

The current repository does not have verified per-service:

- Spring Boot applications;
- service-local domain models;
- service-local ports and adapters;
- service-local tests;
- service-local Dockerfiles and READMEs;
- health checks;
- OpenAPI contracts;
- event contracts;
- service-private databases;
- Docker Compose service landscape;
- Docker Swarm or Kubernetes deployment manifests.

Future slices must treat current service names as planned targets until each
service has build, start, test, configuration, healthcheck, container and
deployment evidence.
