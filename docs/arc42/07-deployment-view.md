# 7. Deployment View

## 7.1 Long-Term Platform Deployment Reference

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

This view is the broader platform deployment direction from the EPIC baseline.
It is not the verified FA-MVP-0001 runtime.

### 7.1.1 Current FA-MVP-0001 Docker-Local View

```text
forensic-ui
  -> query-report-api-service
    -> repository-source-service
       -> forensic-postgres / repository_source schema
       -> repository-source-workspaces volume
```

FA-MVP-0001 verifies Docker-local configuration for the repository-source
workspace checkout foundation. `repository-source-service` owns the checkout
volume and PostgreSQL metadata schema. No other service mounts or reads those
private paths or tables directly. H2 is not active Docker-local runtime
persistence.

This current MVP view does not claim JavaParser, Joern, BTM generation, replay,
report, LLM, shared analytics database, Docker Swarm or Kubernetes readiness.

### 7.1.2 Historical H2 Test Fixture Boundary

```text
repository-source-service tests
  -> direct H2 adapter fixture
```

ADR-0023 is accepted for tests only and superseded for runtime by ADR-0024. H2
must not be used as runtime storage, Docker persistence, startup fallback or
readiness fallback. Existing H2 files are historical MVP data; preserving them
requires an explicit migration slice with verified inputs, acceptance criteria,
rollback strategy and quality gates.

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

The predecessor bootstrap module documented gRPC ingestion startup before
FA-MSA-001 service extraction. It is historical pre-retirement evidence, not a
current executable runtime unit. The verified current service-local ingestion
runtime is owned by `ingestion-service` and
`forensic-ingestion-service`; both document gRPC on port `9090` with
service-owned health endpoints.

```properties
forensics.ingestion.service.grpc.port=9090
```

Predecessor environment variable names are retained only as historical
configuration evidence:

```text
FORENSICS_ANALYTICS_INGESTION_GRPC_ENABLED=true
FORENSICS_ANALYTICS_INGESTION_GRPC_PORT=9090
```

## 7.5 Spring Boot Deployment Direction

ADR-0006 documented the predecessor Boot application as the outer server
boundary before FA-MSA-001 service extraction. The final legacy source-tree
retirement workflow treats that predecessor source tree as historical
pre-retirement evidence, not as a current executable deployment unit.

Spring Boot startup, typed configuration, profiles and adapter lifecycle wiring
are service-local responsibilities in the current top-level service projects project model.
Operators must use verified service-local Gradle paths when building or
starting a Spring service. Source-tree deletion alone does not prove Docker,
Swarm, Kubernetes or production health-check readiness.

The retired Boot-app Docker baseline is no longer a runnable deployment target.
Target services may claim container readiness only when the service owns a
Dockerfile or deployment descriptor and the corresponding verification command
has passed.

## 7.6 Microservice Deployment Boundaries

ADR-0017 defines the FA-MSA-001 target service landscape. Microservice
extraction must keep every productive service independently deployable. Each
service must own its bootstrap, Dockerfile, health checks, configuration, tests
and service-local domain model before production readiness is claimed.

The FA-MSA-001 target service roots are:

```text
repository-source-service
ingestion-service
java-parser-analysis-service
joern-analysis-service
analysis-orchestrator-service
query-report-api-service
cli-client
observability-stack
testbed
```

`cli-client`, `observability-stack` and `testbed` are special boundaries:

- `cli-client` is a public API client, not a productive backend service. S09
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

The current `analysis-store-service` and other `forensic-*`
or analysis-worker roots are transitional implementation evidence. They do not
prove that the FA-MSA-001 target service roots are independently deployable.
Docker Swarm and Kubernetes deployment descriptors are still future slice
material.

Slice S05 adds `repository-source-service` as target-service
deployment evidence. The service owns:

- `repository-source-service/build.gradle.kts`;
- `repository-source-service/Dockerfile`;
- `repository-source-service/src/main/resources/application.properties`;
- `repository-source-service/src/main/resources/application-docker.properties`;
- a service-local health HTTP endpoint on port `8083`;
- a service-local gRPC endpoint on port `9092`;
- the Docker profile workspace root
  `/var/lib/forensic-analytics/repository-workspaces`;
- service-owned PostgreSQL metadata persistence for repository checkout
  workspace, branch, repository preparation and idempotency records;
- historical H2 adapter tests and direct fixtures only.

The service can be packaged independently with:

```bash
./gradlew :repository-source-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

It can be started locally with:

```bash
./gradlew :repository-source-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

Current Docker-local Compose evidence for the repository-source workspace
checkout MVP mounts `repository-source-workspaces` only into
`repository-source-service` at
`/var/lib/forensic-analytics/repository-workspaces` and configures PostgreSQL
through `forensic-postgres`. Other services do not mount repository-source
private volumes and must use owner APIs instead of reading checkout paths,
PostgreSQL tables or historical H2 files directly.

The S09 Compose descriptor publishes repository-source local health on
`127.0.0.1:18087` and gRPC on `127.0.0.1:19097` to avoid collisions with the
transitional `repository-analysis-service`. This is Docker-local MVP evidence
only. Docker Swarm, Kubernetes and full runtime-readiness claims still require
separate repository tooling and validation commands.

Slice S06 adds `ingestion-service` as target-service deployment
evidence. The service owns:

- `ingestion-service/build.gradle.kts`;
- `ingestion-service/Dockerfile`;
- `ingestion-service/src/main/resources/application.properties`;
- `ingestion-service/src/main/resources/application-docker.properties`;
- a service-local health HTTP endpoint on port `8081`;
- a service-local gRPC endpoint on port `9090`.

The service can be packaged independently with:

```bash
./gradlew :ingestion-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

It can be started locally with:

```bash
./gradlew :ingestion-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

The service Dockerfile is service-owned, but S06 does not add Docker Compose,
Docker Swarm or Kubernetes deployment descriptors for the target landscape.
Those readiness claims require later repository tooling and validation
commands.

Slice S07 adds `java-parser-analysis-service` as target-service
deployment evidence. The service owns:

- `java-parser-analysis-service/build.gradle.kts`;
- `java-parser-analysis-service/Dockerfile`;
- `java-parser-analysis-service/src/main/resources/application.properties`;
- `java-parser-analysis-service/src/main/resources/application-docker.properties`;
- a service-local health HTTP endpoint on port `8085`;
- a service-local gRPC endpoint on port `9094`.

The service can be packaged independently with:

```bash
./gradlew :java-parser-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

It can be started locally with:

```bash
./gradlew :java-parser-analysis-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

The service Dockerfile is service-owned, but S07 does not add Docker Compose,
Docker Swarm or Kubernetes deployment descriptors for the target landscape.
Those readiness claims require later repository tooling and validation
commands.

Slice S06 adds `joern-analysis-service` as target-service deployment
evidence. The service owns:

- `joern-analysis-service/build.gradle.kts`;
- `joern-analysis-service/Dockerfile`;
- `joern-analysis-service/src/main/resources/application.properties`;
- `joern-analysis-service/src/main/resources/application-docker.properties`;
- a service-local health HTTP endpoint on port `8087`;
- a service-local gRPC endpoint on port `9096`.

The service can be packaged independently with:

```bash
./gradlew :joern-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

The local operator start command is:

```bash
./gradlew :joern-analysis-service:bootRun --dependency-verification strict --console=plain --stacktrace
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

Slice S07 keeps `analysis-orchestrator-service` as target-service
deployment evidence. The service owns:

- `analysis-orchestrator-service/build.gradle.kts`;
- `analysis-orchestrator-service/Dockerfile`;
- `analysis-orchestrator-service/src/main/resources/application.properties`;
- `analysis-orchestrator-service/src/main/resources/application-docker.properties`;
- a service-local health HTTP endpoint on port `8089`;
- a service-local gRPC endpoint on port `9098`.

The service can be packaged independently with:

```bash
./gradlew :analysis-orchestrator-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

The local operator start command is:

```bash
./gradlew :analysis-orchestrator-service:bootRun --dependency-verification strict --console=plain --stacktrace
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

Slice S08 adds `query-report-api-service` as target-service
deployment evidence. The service owns:

- `query-report-api-service/build.gradle.kts`;
- `query-report-api-service/Dockerfile`;
- `query-report-api-service/src/main/resources/application.properties`;
- `query-report-api-service/src/main/resources/application-docker.properties`;
- a service-local health HTTP endpoint on port `8080`;
- a service-local public API HTTP endpoint on port `8080`.

The service can be packaged independently with:

```bash
./gradlew :query-report-api-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

The local operator start command is:

```bash
./gradlew :query-report-api-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

S08 records this as the service-local start command, but the S08 verification
run does not execute `bootRun` or a live health probe. Runtime smoke evidence
must be recorded separately before claiming a verified running Query Report API
Service instance.

The service Dockerfile is service-owned, but S08 does not add target-service
Docker Compose, Docker Swarm or Kubernetes deployment descriptors and does not
claim Docker image build readiness. Those readiness claims require later
repository tooling and validation commands.

Slice S13 verifies `testbed` as the non-production integration and
system-test deployment boundary. The testbed owns:

- `testbed/build.gradle.kts`;
- `testbed/README.md`;
- deterministic repository E2E fixtures copied under
  `testbed/src/test/resources/repository-e2e/`;
- service-root test coverage under
  `testbed/src/test/java/de/burger/forensics/analytics/testbed`.

The service-local testbed gate is:

```bash
./gradlew :testbed:test --dependency-verification strict --console=plain --stacktrace
```

`testbed` is not a productive backend service. It does not add a
Dockerfile, Docker Compose service, Docker Swarm stack or Kubernetes manifest
in S13. The local Compose descriptor is validated as model syntax only; S13
does not build images, start Compose or perform health probes. The predecessor
testbed source tree was retired by S05; remaining testbed evidence is
service-local under `testbed` plus historical documentation.

Earlier WildFly hardening work keeps the service-root scenario as
non-production deployment evidence that is skipped by default unless an
explicit WildFly branch or commit is provided. It does not promote
`testbed` into a runtime service and does not add a Dockerfile,
Compose service, Swarm stack, Kubernetes manifest or health probe.

Slice S14 did not remove deployment or runtime paths. That historical
`NO_REMOVAL_SAFE` result is superseded by S05 source-tree retirement. The
predecessor Boot and Bootstrap source trees are now deleted historical
deployment evidence, not active deployment units. S06 records the architecture
closure and S07 owns final release-readiness evidence.

## 7.7 Local Repository-to-BTM Transitional Landscape

The repository currently contains a local Docker Compose descriptor for the
implemented transitional repository-to-BTM path and the FA-MVP-0001
repository-source owner service:

```text
deployment/docker-compose/repository-to-btm.local.yml
```

The descriptor covers the current transitional local landscape:

```text
forensic-gateway-service
analysis-store-service
repository-analysis-service
repository-source-service
java-ast-analysis-service
joern-cpg-analysis-service
btm-generation-service
```

It uses service-owned Dockerfiles, Docker profile configuration, service-local
health checks, a private repository-source checkout volume and PostgreSQL
metadata persistence plus generated Java AST, Joern and BTM artifacts. Gateway remains the public
HTTP facade for the transitional repository-to-BTM path. Analysis Store remains
the repository-to-BTM orchestration owner and calls worker owner APIs over
gRPC. Repository-source owns its private workspace volume and PostgreSQL
metadata schema in the current MVP and exposes that state only through owner
APIs. H2 is not active runtime or Docker persistence.

The local descriptor does not introduce external databases, Graph DB, Vector
DB, brokers, Jenkins, Artifactory or live credentials. Docker Swarm and
Kubernetes remain explicitly not ready until stack files or manifests,
readiness/liveness probes, resource policies and validation commands are added
by a later slice.

The local descriptor commands are documented in
`deployment/docker-compose/README.md`. FA-MVP-0001 S09 executed and recorded
Compose model validation for this descriptor. Image-build, startup and
health-check commands remain optional runtime evidence and must be recorded
separately when executed.

The descriptor is current evidence only. It is not a readiness claim for the
FA-MSA-001 target landscape until the target services exist and are verified by
their own build, start, healthcheck, Docker and quality gates.

The S07 final release-readiness slice must close the active workflow by
verifying the mandatory FA-MSA-001 target service build tasks, service-owned
Dockerfiles, Docker healthcheck definitions, service-local configuration files
and architecture tests. That closure remains
limited to service-local build and packaging evidence plus the repository
quality gate; it must not claim Docker image-build, Docker Compose startup,
Docker Swarm or Kubernetes runtime readiness for the target landscape unless
those commands are explicitly executed and recorded.

## 7.8 Local Docker Compose Fragment Landscape

The Docker Compose deployment workflow adds service-specific local fragments
under `deployment/docker-compose/services/` and a root network descriptor at
`deployment/docker-compose/forensic-analytics.local.yml`. The root descriptor
uses the external Docker network `forensic_analytics`.

Runnable service fragments exist for repository-source, ingestion, JavaParser,
Joern analysis, analysis orchestration, query/report API, forensic ingestion,
forensic gateway, analysis store, repository analysis, Java AST, Joern CPG,
BTM generation and the React UI. Each runnable Java service fragment uses its
service-owned Dockerfile and does not share Java implementation modules with
other services.

`cli-client`, `observability-stack` and `testbed` are profile-gated local
support descriptors. They are not productive backend services. Graph Replay
and Report Generation remain planned roots; their Compose fragments are
profile-gated markers only and do not claim runnable service implementations,
health endpoints, graph data, replay data or report output.

The React UI fragment publishes `http://127.0.0.1:18000/` and uses same-origin
`/api` calls. nginx proxies those calls to `query-report-api-service:8080`
inside the Docker network. The workflow executed a local smoke check for
`http://127.0.0.1:18000/api/health` and received `{"status":"UP"}` on
May 28, 2026.

The runbook for build, config validation, startup, logs, GUI smoke and cleanup
is `docs/deployment/forensic-analytics-docker-compose.md`. Full-stack startup
and health checks for every container remain separate runtime evidence and are
not claimed unless those commands are executed and recorded.
