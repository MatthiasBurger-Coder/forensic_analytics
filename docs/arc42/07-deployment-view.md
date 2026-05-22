# 7. Deployment View

## 7.1 MVP Deployment View

```text
Developer Machine / CI Environment
├── Target Java Project
├── Gradle or Maven Plugin
├── Server-generated BTM Files
├── Runtime Application with Agent-bound BTM instrumentation
└── Forensics Platform
    ├── gRPC Ingestion Server
    ├── Workspace and Git Checkout
    ├── Server-side Parser / Joern / BTM Generation
    ├── Canonical Store
    ├── Event Store
    ├── Simple Graph Projection
    └── LLM Diagnosis Adapter
```

## 7.2 Later Deployment View

```text
Forensics Platform Environment
├── Forensics API
├── Forensics UI
├── gRPC Ingestion Server
├── Relational Store
├── Graph DB
├── Vector DB
├── Event Store
├── Runtime Collector
├── LLM Adapter
└── Repair Orchestrator
```

## 7.3 Deployment Constraints

- The MVP may start as a local or CI-attached analysis platform.
- Runtime event ingestion may initially use JSONL files.
- Parser, Joern and BTM generation run on the Forensics Platform side.
- The plugin may bind server-generated BTM files to the runtime agent for debugging.
- HTTP collector support can be introduced later.
- Multi-tenant production deployment is out of MVP scope.

## 7.4 gRPC Ingestion Configuration

The bootstrap module can start the gRPC ingestion server. The default port is `9090`.

```properties
forensics.analytics.ingestion.grpc.enabled=true
forensics.analytics.ingestion.grpc.port=9090
```

Environment variable equivalents:

```text
FORENSICS_ANALYTICS_INGESTION_GRPC_ENABLED=true
FORENSICS_ANALYTICS_INGESTION_GRPC_PORT=9090
```

## 7.5 Spring Boot Deployment Direction

ADR-0006 accepts `forensic-analytics-boot-app` as the outer server boundary. The Boot app owns Spring Boot startup, typed configuration, profiles and adapter lifecycle wiring for verified inbound adapters.

A minimal Boot startup does not require a database, Joern container, graph database, vector database or live LLM provider. The existing bootstrap module remains available while parity is phased in.

Boot configuration is provided through `application.properties` and profile-specific `.properties` files. The `docker` and `prod` profiles disable gRPC and REST by default; operators must explicitly enable the inbound adapter they intend to expose.

The Boot app can be packaged with:

```bash
./gradlew :forensic-analytics-boot-app:bootJar --dependency-verification strict --console=plain --stacktrace
```

The Docker baseline lives under `docker/boot-app/`. It copies only the generated Boot jar, defines `/var/lib/forensic-analytics/workspaces` as the workspace volume and does not define an Actuator healthcheck because no accepted health endpoint exists yet.

## 7.6 Microservice Deployment Boundaries

ADR-0017 defines the FA-MSA-001 target service landscape. Microservice
extraction must keep every productive service independently deployable. Each
service must own its bootstrap, Dockerfile, health checks, configuration, tests
and service-local domain model before production readiness is claimed.

The FA-MSA-001 target service roots are:

```text
services/repository-source-service
services/ingestion-service
services/java-parser-analysis-service
services/joern-analysis-service
services/analysis-orchestrator-service
services/query-report-api-service
services/cli-client
services/observability-stack
services/testbed
```

`cli-client`, `observability-stack` and `testbed` are special boundaries:

- `cli-client` is a public API client, not a productive backend service. S11
  creates and registers it as an independently buildable command-line
  application; it does not own health endpoints, Docker runtime readiness or
  service deployment readiness unless a later operator slice adds and verifies
  them.
- `observability-stack` is deployment/configuration material, not a shared
  Java runtime module.
- `testbed` is non-production integration and system-test infrastructure.

Shared Java implementation modules between services are forbidden.
Service-to-service integration is limited to REST/OpenAPI, gRPC/protobuf,
approved message contracts or documented file contracts. Deployment targets
must cover local startup, Docker and later Docker Swarm or Kubernetes only
when service-owned deployment material and validation commands exist.

ADR-0018 keeps contract artifacts separate from deployment readiness. A
contract file may exist before the service, container, healthcheck, manifest or
broker topology exists. Deployment documentation must not claim readiness from
contract presence alone.

Joern Docker input must be a Joern-owned materialized workspace volume. A
Repository Source private workspace volume must not be mounted into the Joern
container.

The current `services/analysis-store-service` and other `services/forensic-*`
or analysis-worker roots are transitional implementation evidence. They do not
prove that the FA-MSA-001 target service roots are independently deployable.
Docker Swarm and Kubernetes deployment descriptors are still future slice
material.

Slice S05 adds `services/repository-source-service` as target-service
deployment evidence. The service owns:

- `services/repository-source-service/build.gradle.kts`;
- `services/repository-source-service/Dockerfile`;
- `services/repository-source-service/src/main/resources/application.properties`;
- `services/repository-source-service/src/main/resources/application-docker.properties`;
- a service-local health HTTP endpoint on port `8083`;
- a service-local gRPC endpoint on port `9092`;
- the Docker profile workspace root
  `/var/lib/forensic-analytics/repository-workspaces`.

The service can be packaged independently with:

```bash
./gradlew :services:repository-source-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

It can be started locally with:

```bash
./gradlew :services:repository-source-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

The service Dockerfile is service-owned, but S05 does not add Docker Compose,
Docker Swarm or Kubernetes deployment descriptors for the target landscape.
Those readiness claims require later repository tooling and validation
commands.

Slice S06 adds `services/ingestion-service` as target-service deployment
evidence. The service owns:

- `services/ingestion-service/build.gradle.kts`;
- `services/ingestion-service/Dockerfile`;
- `services/ingestion-service/src/main/resources/application.properties`;
- `services/ingestion-service/src/main/resources/application-docker.properties`;
- a service-local health HTTP endpoint on port `8081`;
- a service-local gRPC endpoint on port `9090`.

The service can be packaged independently with:

```bash
./gradlew :services:ingestion-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

It can be started locally with:

```bash
./gradlew :services:ingestion-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

The service Dockerfile is service-owned, but S06 does not add Docker Compose,
Docker Swarm or Kubernetes deployment descriptors for the target landscape.
Those readiness claims require later repository tooling and validation
commands.

Slice S07 adds `services/java-parser-analysis-service` as target-service
deployment evidence. The service owns:

- `services/java-parser-analysis-service/build.gradle.kts`;
- `services/java-parser-analysis-service/Dockerfile`;
- `services/java-parser-analysis-service/src/main/resources/application.properties`;
- `services/java-parser-analysis-service/src/main/resources/application-docker.properties`;
- a service-local health HTTP endpoint on port `8085`;
- a service-local gRPC endpoint on port `9094`.

The service can be packaged independently with:

```bash
./gradlew :services:java-parser-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

It can be started locally with:

```bash
./gradlew :services:java-parser-analysis-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

The service Dockerfile is service-owned, but S07 does not add Docker Compose,
Docker Swarm or Kubernetes deployment descriptors for the target landscape.
Those readiness claims require later repository tooling and validation
commands.

Slice S06 adds `services/joern-analysis-service` as target-service deployment
evidence. The service owns:

- `services/joern-analysis-service/build.gradle.kts`;
- `services/joern-analysis-service/Dockerfile`;
- `services/joern-analysis-service/src/main/resources/application.properties`;
- `services/joern-analysis-service/src/main/resources/application-docker.properties`;
- a service-local health HTTP endpoint on port `8087`;
- a service-local gRPC endpoint on port `9096`.

The service can be packaged independently with:

```bash
./gradlew :services:joern-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

The local operator start command is:

```bash
./gradlew :services:joern-analysis-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

S06 records this as the service-local start command, but the S06 verification
run did not execute `bootRun` or a live health probe. Runtime smoke evidence
must be recorded separately before claiming a verified running Joern Analysis
Service instance.

The service Dockerfile is service-owned and based on the digest-pinned Joern
runtime plus a copied Java 25 runtime. S06 keeps Docker image build and Joern
runtime smoke testing as optional external checks because they may pull the
Joern base image or create local container state. The root `.dockerignore`
explicitly allows the service boot jar into the Docker build context; S06 does
not add target-service Docker Compose, Docker Swarm or Kubernetes deployment
descriptors.

Slice S07 keeps `services/analysis-orchestrator-service` as target-service
deployment evidence. The service owns:

- `services/analysis-orchestrator-service/build.gradle.kts`;
- `services/analysis-orchestrator-service/Dockerfile`;
- `services/analysis-orchestrator-service/src/main/resources/application.properties`;
- `services/analysis-orchestrator-service/src/main/resources/application-docker.properties`;
- a service-local health HTTP endpoint on port `8089`;
- a service-local gRPC endpoint on port `9098`.

The service can be packaged independently with:

```bash
./gradlew :services:analysis-orchestrator-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

The local operator start command is:

```bash
./gradlew :services:analysis-orchestrator-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

S07 records this as the service-local start command, but the S07 verification
run does not execute `bootRun` or a live health probe. Runtime smoke evidence
must be recorded separately before claiming a verified running Analysis
Orchestrator Service instance.

The service Dockerfile is service-owned, but S07 does not add target-service
Docker Compose, Docker Swarm or Kubernetes deployment descriptors and does not
claim Docker image build readiness from the Dockerfile alone. Those readiness
claims require later repository tooling, Docker build-context verification and
validation commands.

Slice S08 adds `services/query-report-api-service` as target-service
deployment evidence. The service owns:

- `services/query-report-api-service/build.gradle.kts`;
- `services/query-report-api-service/Dockerfile`;
- `services/query-report-api-service/src/main/resources/application.properties`;
- `services/query-report-api-service/src/main/resources/application-docker.properties`;
- a service-local health HTTP endpoint on port `8080`;
- a service-local public API HTTP endpoint on port `8080`.

The service can be packaged independently with:

```bash
./gradlew :services:query-report-api-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

The local operator start command is:

```bash
./gradlew :services:query-report-api-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

S08 records this as the service-local start command, but the S08 verification
run does not execute `bootRun` or a live health probe. Runtime smoke evidence
must be recorded separately before claiming a verified running Query Report API
Service instance.

The service Dockerfile is service-owned, but S08 does not add target-service
Docker Compose, Docker Swarm or Kubernetes deployment descriptors and does not
claim Docker image build readiness. Those readiness claims require later
repository tooling and validation commands.

Slice S13 adds `services/testbed` as the non-production integration and
system-test deployment boundary. The testbed owns:

- `services/testbed/build.gradle.kts`;
- `services/testbed/README.md`;
- deterministic repository E2E fixtures copied under
  `services/testbed/src/test/resources/repository-e2e/`;
- service-root test coverage under
  `services/testbed/src/test/java/de/burger/forensics/analytics/services/testbed`.

The service-local testbed gate is:

```bash
./gradlew :services:testbed:test --dependency-verification strict --console=plain --stacktrace
```

`services/testbed` is not a productive backend service. It does not add a
Dockerfile, Docker Compose service, Docker Swarm stack or Kubernetes manifest
in S13. The legacy `forensic-analytics-testbed` module remains active until a
later retirement slice proves parity, caller migration and rollback evidence.

Slice S14 does not remove deployment or runtime paths. It records
`NO_REMOVAL_SAFE` when caller scans still find active legacy module use. The
Boot app, Bootstrap runtime and retained `forensic-analytics-*` modules remain
legacy in-process and rollback deployment evidence until later retirement
slices migrate, deprecate or prove each path caller-free.

## 7.7 Local Repository-to-BTM Transitional Landscape

The repository currently contains a local Docker Compose descriptor for the
implemented transitional repository-to-BTM path:

```text
deployment/docker-compose/repository-to-btm.local.yml
```

The descriptor covers only the current transitional service path:

```text
forensic-gateway-service
analysis-store-service
repository-analysis-service
java-ast-analysis-service
joern-cpg-analysis-service
btm-generation-service
```

It uses service-owned Dockerfiles, Docker profile configuration, service-local
health checks and named volumes for repository workspaces plus generated Java
AST, Joern and BTM artifacts. Gateway is the only public HTTP facade in this
local landscape. Analysis Store remains the repository-to-BTM orchestration
owner and calls worker owner APIs over gRPC.

The local descriptor does not introduce external databases, Graph DB, Vector
DB, brokers, Jenkins, Artifactory or live credentials. Docker Swarm and
Kubernetes remain explicitly not ready until stack files or manifests,
readiness/liveness probes, resource policies and validation commands are added
by a later slice.

The local descriptor commands are documented in
`deployment/docker-compose/README.md`. S15 did not execute or record Compose
model validation, image-build, startup or health-check commands for this
descriptor.

The descriptor is current evidence only. It is not a readiness claim for the
FA-MSA-001 target landscape until the target services exist and are verified by
their own build, start, healthcheck, Docker and quality gates.

S15 closes the workflow by verifying the mandatory FA-MSA-001 target service
build tasks, service-owned Dockerfiles, Docker healthcheck definitions,
service-local configuration files and architecture tests. This closure remains
limited to service-local build and packaging evidence plus the repository
quality gate; it does not claim Docker image-build, Docker Compose startup,
Docker Swarm or Kubernetes runtime readiness for the target landscape.
The S15 full local quality gate passed with the repository `QUALITY.md`
command including `clean`, `test`, `jacocoTestReport`,
`jacocoTestCoverageVerification` and `checkPackageCoverage`.
