# Workflow: FA-DEPLOY-0001 Docker Compose Service Deployment

## Workflow Version

| Field | Value |
|---|---|
| Workflow version | `fa-deploy-0001-docker-compose-services-20260528-v1` |
| Requirement ID | `FA-DEPLOY-0001` |
| Title | Create local Docker Compose deployment descriptors for service roots |
| Workflow branch | `feature/workflow-docker-compose-deployment-20260528` |
| Creation status | Created by `workflow create`; implementation requires `workflow execute`. |
| Process strand | `workflow create` completed; `workflow execute` pending. |
| Execution profile | `FULL_PATH` |
| Deployment network | `forensic_analytics` |
| Root stack owner | `forensic_analytics` repository deployment boundary |
| Operator goal | Deploy locally, open the GUI, and find runtime or integration errors. |

## Executive Summary

This workflow creates an executable plan for local Docker Compose deployment
coverage across the service roots named by the user. Each named root receives
at least one implementation slice. The workflow also adds a dedicated
deployment-description slice so operators can build, start, smoke-check, use
the GUI, and tear down the local stack without treating unverified runtime
behavior as production readiness.

The requested `forensic_analytics` item is verified as the repository/worktree
name and the required Docker network name, not as a Gradle subproject. It is
therefore handled by the root stack and network slice.

The current repository contains a mixture of:

- runnable Java application service roots with Dockerfiles;
- a runnable CLI application without a Dockerfile;
- non-production or deployment-support roots without service runtime;
- planned service roots without implementation yet;
- the separate `forensic-ui` frontend, which is required for the user's GUI
  goal but was not named as a Gradle module.

The workflow must preserve those distinctions. A later implementation slice may
not fabricate service runtime, health endpoints, persistence stores, graph
relationships, replay data, reports, or observability telemetry simply to make
a Compose model look complete.

## Target Picture

```text
Browser
  -> forensic-ui container
  -> query-report-api-service public REST API
  -> analysis-orchestrator-service
  -> repository-source-service
  -> ingestion-service
  -> java-parser-analysis-service
  -> joern-analysis-service

Transitional repository-to-BTM path, when explicitly enabled:
  forensic-gateway-service
  -> analysis-store-service
  -> repository-analysis-service
  -> java-ast-analysis-service
  -> joern-cpg-analysis-service
  -> btm-generation-service

All Compose services and fragments:
  -> Docker network named forensic_analytics
```

The root deployment must provide:

- one Compose fragment per named root where a runnable or explicitly
  documented deployable boundary exists;
- a root stack or runbook that composes those fragments;
- stable host-port assignments that avoid collisions between target and
  transitional services;
- service-owned volumes only where ownership is verified;
- health checks only where an endpoint or Dockerfile health check is verified;
- optional profiles for CLI, testbed, observability, and planned roots that
  are not always-on backend services;
- GUI access through `forensic-ui` and public API access through
  `query-report-api-service`.

## Verified Baseline

Read-only workflow creation verification found:

- Repository root: `/mnt/d/Projects/forensic_analytics`.
- Active branch after branch-first creation:
  `feature/workflow-docker-compose-deployment-20260528`.
- The branch exists as `refs/heads/feature/workflow-docker-compose-deployment-20260528`.
- Working tree was clean before branch creation.
- Quality authority is `QUALITY.md`.
- Minimum quality command:
  `./gradlew test --dependency-verification strict --console=plain --stacktrace`.
- Full local quality gate:
  `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`.
- Registered Gradle subprojects include all named service roots except
  `forensic_analytics`, which is the repository/deployment network name.
- Existing local Compose evidence:
  `deployment/docker-compose/repository-to-btm.local.yml`.
- Root `.dockerignore` currently excludes `**/build/**` and re-includes only
  selected service jars. Several target-service Dockerfiles copy boot jars from
  the root build context, so image-build readiness requires an explicit
  build-context slice before any target-service image-build claim.
- Existing frontend root for the GUI goal:
  `forensic-ui`.
- `forensic-ui/nginx.conf` currently returns JSON `502 BACKEND_UNAVAILABLE`
  for `/api`. GUI deployment therefore requires an explicit same-origin
  reverse-proxy or equivalent verified API-routing slice.
- Existing Dockerfiles were verified for:
  `analysis-orchestrator-service`, `analysis-store-service`,
  `btm-generation-service`, `forensic-gateway-service`,
  `forensic-ingestion-service`, `ingestion-service`,
  `java-ast-analysis-service`, `java-parser-analysis-service`,
  `joern-analysis-service`, `joern-cpg-analysis-service`,
  `query-report-api-service`, `repository-analysis-service`,
  `repository-source-service`, and `forensic-ui`.
- No Dockerfile was found for:
  `cli-client`, `graph-replay-service`, `observability-stack`,
  `report-generation-service`, and `testbed`.
- `graph-replay-service` and `report-generation-service` are documented as
  planned roots with no implementation yet.
- `observability-stack` is deployment-oriented policy material, not a shared
  Java runtime module.
- `testbed` is non-production integration and system-test infrastructure.
- ADR-0017 marks several requested names as transitional current-state
  evidence, not target-service aliases.
- `docs/arc42/07-deployment-view.md` states that Docker Compose, Swarm, and
  Kubernetes readiness must not be claimed without service-owned descriptors
  and validation commands.

## Requirement Clarification Decision

| Field | Decision |
|---|---|
| Original request | `workflow create with subagents: erstelle pro service ein docker-compose file ... jedes modul bekommt mindesten einen Slice ... Deployment beschreibung ... docker-netzwerk forensic_analytics ... deployen um mit der gui zu interargieren` |
| Interpreted intent | Create an executable workflow for per-service local Docker Compose descriptors, a deployment description, a shared Docker network named `forensic_analytics`, and GUI-oriented local smoke verification. |
| Change type | Deployment and workflow planning with later Docker Compose, documentation, and runtime-smoke implementation slices. |
| Affected process strand | `workflow create` now; later `workflow execute`. |
| Affected architecture area | Local deployment, Docker Compose, service runtime readiness, GUI-to-public-API integration, documentation, quality gates. |
| Explicit requirements | One slice per listed module/root; per-service Compose file; deployment description; network `forensic_analytics`; deploy locally to interact with GUI and find errors. |
| Implicit requirements | Preserve service ownership; avoid host-port collisions; keep private volumes private; do not invent runtime for planned roots; include frontend deployment even though `forensic-ui` was not in the service list. |
| Accepted assumptions | `forensic_analytics` means repository stack and Docker network, not a Gradle service module. `forensic-ui` is the GUI target needed for manual interaction. Compose descriptors are local deployment evidence, not production readiness. |
| Non-goals | No Kubernetes, Docker Swarm, external database, broker, Graph DB, Vector DB, live LLM provider, production secrets, service extraction, shared Java module, or fabricated placeholder runtime. |
| Risks | Some roots are planned or non-production only; several internal service ports overlap; Joern image builds may require external image pulls; UI API base URL is compile-time Vite configuration; full-stack startup may reveal existing service integration gaps. |
| Open questions | Exact operator port preferences are not specified. The workflow uses deterministic proposed host ports and requires implementation to document any change. |
| Blocking questions | None for workflow creation. Later slices must stop when a root has no verified runnable artifact and a Compose service would require inventing runtime. |
| Confidence | 86 percent. |
| Decision | `PROCEED_WITH_ACCEPTED_ASSUMPTIONS`. |

## Scope

In scope:

- Create local Docker Compose fragment files for every listed root where a
  deployable descriptor can be verified.
- Create a root stack/network Compose entry point for `forensic_analytics`.
- Fix or verify the Docker build context so every service Dockerfile can copy
  its service-owned boot jar before image-build readiness is claimed.
- Add or update deployment documentation for local build, config validation,
  startup, health checks, GUI use, logs, and cleanup.
- Add Dockerfiles only for roots with a verified application entry point or an
  explicitly approved tool/test role.
- Keep planned roots explicitly marked as non-runnable when no implementation
  exists.
- Include `forensic-ui` deployment integration because the user goal requires
  GUI interaction.
- Run Compose model validation and relevant Gradle checks per slice.

Out of scope:

- Implementing missing business behavior for `graph-replay-service` or
  `report-generation-service`.
- Promoting `testbed`, `observability-stack`, or `cli-client` into productive
  backend services.
- Changing REST, gRPC, Protobuf, event, database, graph, replay, report, or
  LLM contracts unless a later slice verifies the exact contract impact first.
- Moving service ownership or sharing Java implementation modules.
- Claiming production, Docker Swarm, or Kubernetes readiness.

## Architecture Constraints

- All Compose services must attach to a Docker network named
  `forensic_analytics`.
- The root stack must not overwrite
  `deployment/docker-compose/repository-to-btm.local.yml`; it may reuse verified
  values or document migration from that descriptor.
- Target services and transitional services must remain explicitly labeled.
- Services may communicate only through verified REST/OpenAPI, gRPC/protobuf,
  approved events, or documented file/artifact contracts.
- Private owner volumes must be mounted only by the owner service.
- `repository-source-service` owns repository checkout workspaces and H2
  repository-source data.
- JavaParser, Joern, BTM, report, graph, and replay artifacts must retain
  producer ownership and must not become shared canonical evidence.
- Operational logs and observability data are diagnostics, not verified
  forensic evidence.
- The UI must use public API routes only. It must not call internal worker
  services directly.
- Browser-to-API connectivity must use same-origin proxying or another
  verified browser-safe path. It must not depend on unverified CORS behavior.
- Planned roots without executable runtime must remain documented as
  not deployable until a later implementation slice creates verified runtime
  evidence.

## Backend Assessment

Backend implementation is expected for Docker build and runtime configuration
only. Existing application services with Dockerfiles can receive Compose
fragments and validation. Services without Dockerfiles need verified runtime
entry points before adding container images:

- `cli-client` has an application main class and can be containerized as a
  tool profile if the slice adds a service-owned Dockerfile and tests it.
- `graph-replay-service` and `report-generation-service` currently use the
  Gradle `base` plugin and have no implementation. They must not be represented
  as running services until implementation exists.
- `observability-stack` is deployment/policy material. It may add verified
  observability deployment configuration, but not a shared Java runtime module.
- `testbed` is non-production infrastructure. It may consume the Compose stack
  for tests, but productive services must not depend on testbed code.

## Frontend Assessment

The GUI goal routes through `forensic-ui`. The workflow must include a UI slice
that verifies:

- `forensic-ui/Dockerfile` and `forensic-ui/nginx.conf`;
- `VITE_API_BASE_URL` handling for the selected public API host;
- an nginx `/api` reverse proxy to `query-report-api-service` over the Docker
  network, or an explicitly verified alternative that works from a browser;
- browser access through a stable local host port;
- API requests go to `query-report-api-service` public routes, not internal
  gRPC worker endpoints.

## Test Strategy

Targeted checks run before broader gates:

```bash
./gradlew :<module>:test --dependency-verification strict --console=plain --stacktrace
./gradlew :<module>:bootJar --dependency-verification strict --console=plain --stacktrace
docker compose -f <compose-file> config
git diff --check
```

For `cli-client`:

```bash
./gradlew :cli-client:test --dependency-verification strict --console=plain --stacktrace
./gradlew :cli-client:build --dependency-verification strict --console=plain --stacktrace
```

For the frontend:

```bash
cd forensic-ui
npm ci
npm run test
npm run build
docker build -t forensic-analytics/forensic-ui:local ./forensic-ui
```

Minimum repository gate:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate before commit readiness:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Compose runtime smoke checks are required only after the relevant images build
successfully and Docker is available. The result must state whether runtime
startup was executed, skipped, or blocked.

## Ordered Slices

### Slice 01 - Root Compose Network, Build Context, And Stack Entry

Purpose: create the root `forensic_analytics` deployment boundary, network
declaration, file naming convention, root stack assembly strategy, common
environment conventions, and Docker build-context guard for service boot jars.

```yaml
slice_id: S01_ROOT_FORENSIC_ANALYTICS_STACK
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_system_architect
  - senior_tester
affected_files:
  - .dockerignore
  - deployment/docker-compose/forensic-analytics.local.yml
  - deployment/docker-compose/README.md
  - deployment/README.md
affected_modules: []
affected_contracts: []
dependencies: []
parallel_group: P0
file_locks:
  - .dockerignore
  - deployment/docker-compose/forensic-analytics.local.yml
  - deployment/docker-compose/README.md
  - deployment/README.md
contract_locks: []
architecture_locks:
  - docker-network-forensic_analytics
  - local-deployment-boundary
quality_gates:
  targeted:
    - ./gradlew --no-daemon --max-workers=1 :repository-source-service:bootJar :ingestion-service:bootJar :java-parser-analysis-service:bootJar :joern-analysis-service:bootJar :analysis-orchestrator-service:bootJar :query-report-api-service:bootJar --dependency-verification strict --console=plain --stacktrace
    - docker compose -f deployment/docker-compose/forensic-analytics.local.yml config
    - git diff --check
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Docker Compose network naming cannot be represented as forensic_analytics.
  - Root stack would overwrite existing repository-to-btm Compose evidence.
  - A target-service Dockerfile cannot copy its verified boot jar from the build context.
```

Done criteria:

- The root Compose entry defines or references a network named
  `forensic_analytics`.
- The network strategy is explicit: either a root-created network or an
  external shared network with documented `docker network create` and cleanup
  behavior.
- `.dockerignore` re-includes every boot jar needed by service Dockerfiles that
  use the repository root as build context.
- The deployment README documents how service fragments are combined.
- `deployment/README.md` no longer contradicts the verified Compose scope.
- Existing transitional Compose evidence remains intact.

### Slice 02 - Repository Source Service Compose

Purpose: add local Compose deployment for the repository source owner service
and preserve private workspace/data volume ownership.

```yaml
slice_id: S02_REPOSITORY_SOURCE_SERVICE_COMPOSE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_java_backend
  - senior_system_architect
  - senior_tester
affected_files:
  - deployment/docker-compose/services/repository-source-service.compose.yml
affected_modules:
  - repository-source-service
affected_contracts:
  - contracts/grpc/repository-analysis.proto
dependencies:
  - S01_ROOT_FORENSIC_ANALYTICS_STACK
parallel_group: P1
file_locks:
  - deployment/docker-compose/services/repository-source-service.compose.yml
contract_locks:
  - repository-source-owner-api
architecture_locks:
  - repository-source-private-workspace-volume
  - repository-source-h2-data-volume
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :repository-source-service:bootJar --dependency-verification strict --console=plain --stacktrace
    - docker compose -f deployment/docker-compose/services/repository-source-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md
stop_conditions:
  - Compose mounts repository-source private volumes into another service.
  - Health or port values cannot be verified from service configuration.
```

Done criteria:

- Compose uses the existing service-owned Dockerfile.
- Private workspace and H2 volumes stay owner-only.
- Health and port mappings are documented with host-port collision avoidance.

### Slice 03 - Ingestion Service Compose

Purpose: add local Compose deployment for the target ingestion service.

```yaml
slice_id: S03_INGESTION_SERVICE_COMPOSE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_java_backend
  - senior_tester
affected_files:
  - deployment/docker-compose/services/ingestion-service.compose.yml
affected_modules:
  - ingestion-service
affected_contracts:
  - contracts/grpc/forensic-ingestion.proto
dependencies:
  - S01_ROOT_FORENSIC_ANALYTICS_STACK
parallel_group: P1
file_locks:
  - deployment/docker-compose/services/ingestion-service.compose.yml
contract_locks:
  - ingestion-service-grpc
architecture_locks:
  - raw-ingestion-owner
quality_gates:
  targeted:
    - ./gradlew :ingestion-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :ingestion-service:bootJar --dependency-verification strict --console=plain --stacktrace
    - docker compose -f deployment/docker-compose/services/ingestion-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Compose would collide with forensic-ingestion host ports without explicit mapping.
  - Runtime trace payload ownership becomes ambiguous.
```

Done criteria:

- Compose attaches the service to `forensic_analytics`.
- gRPC and health ports are exposed with unique host mappings.
- Runtime intake evidence semantics remain owned by `ingestion-service`.

### Slice 04 - Java Parser Analysis Service Compose

Purpose: add local Compose deployment for the target JavaParser analysis
service and its artifact volume.

```yaml
slice_id: S04_JAVA_PARSER_ANALYSIS_SERVICE_COMPOSE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_java_backend
  - source_analysis_reviewer
  - senior_tester
affected_files:
  - deployment/docker-compose/services/java-parser-analysis-service.compose.yml
affected_modules:
  - java-parser-analysis-service
affected_contracts:
  - contracts/grpc/java-ast-analysis.proto
  - contracts/grpc/java-ast-source-facts-v1.schema.json
dependencies:
  - S01_ROOT_FORENSIC_ANALYTICS_STACK
parallel_group: P1
file_locks:
  - deployment/docker-compose/services/java-parser-analysis-service.compose.yml
contract_locks:
  - java-parser-source-facts
architecture_locks:
  - static-source-fact-owner
quality_gates:
  targeted:
    - ./gradlew :java-parser-analysis-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :java-parser-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
    - docker compose -f deployment/docker-compose/services/java-parser-analysis-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Static source facts are represented as runtime execution evidence.
  - Artifact volume ownership is shared with another service.
```

Done criteria:

- Compose models the service-owned artifact path only.
- Static analysis output remains separate from runtime trace evidence.

### Slice 05 - Joern Analysis Service Compose

Purpose: add local Compose deployment for the target Joern analysis service
without mounting repository-source private workspaces into the Joern container.

```yaml
slice_id: S05_JOERN_ANALYSIS_SERVICE_COMPOSE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_joern_cpg_specialist
  - senior_java_backend
  - senior_tester
affected_files:
  - deployment/docker-compose/services/joern-analysis-service.compose.yml
affected_modules:
  - joern-analysis-service
affected_contracts:
  - contracts/grpc/joern-cpg-analysis.proto
dependencies:
  - S01_ROOT_FORENSIC_ANALYTICS_STACK
parallel_group: P1
file_locks:
  - deployment/docker-compose/services/joern-analysis-service.compose.yml
contract_locks:
  - joern-analysis-grpc
architecture_locks:
  - joern-owned-workspace-volume
  - semantic-artifact-owner
quality_gates:
  targeted:
    - ./gradlew :joern-analysis-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :joern-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
    - docker compose -f deployment/docker-compose/services/joern-analysis-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Repository-source private workspace volume is mounted into Joern.
  - Joern image pull or build cannot be verified and runtime readiness would be claimed anyway.
```

Done criteria:

- Joern runtime image use is documented as an optional external Docker check.
- Joern artifacts and workspaces are service-owned.

### Slice 06 - Analysis Orchestrator Service Compose

Purpose: add local Compose deployment for the target orchestration service and
document dependencies on owner APIs.

```yaml
slice_id: S06_ANALYSIS_ORCHESTRATOR_SERVICE_COMPOSE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_java_backend
  - senior_system_architect
  - senior_tester
affected_files:
  - deployment/docker-compose/services/analysis-orchestrator-service.compose.yml
affected_modules:
  - analysis-orchestrator-service
affected_contracts:
  - contracts/grpc/analysis-job.proto
  - contracts/events/analysis-events.md
dependencies:
  - S02_REPOSITORY_SOURCE_SERVICE_COMPOSE
  - S03_INGESTION_SERVICE_COMPOSE
  - S04_JAVA_PARSER_ANALYSIS_SERVICE_COMPOSE
  - S05_JOERN_ANALYSIS_SERVICE_COMPOSE
parallel_group: P2
file_locks:
  - deployment/docker-compose/services/analysis-orchestrator-service.compose.yml
contract_locks:
  - analysis-job-orchestration
architecture_locks:
  - orchestration-state-owner
quality_gates:
  targeted:
    - ./gradlew :analysis-orchestrator-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :analysis-orchestrator-service:bootJar --dependency-verification strict --console=plain --stacktrace
    - docker compose -f deployment/docker-compose/services/analysis-orchestrator-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Compose implies direct database or filesystem access to another service.
  - Orchestration facts become canonical evidence owned by a worker service.
```

Done criteria:

- The orchestrator depends on owner APIs, not private data paths.
- Startup order is health-based where verified.

### Slice 07 - Query Report API Service Compose

Purpose: add local Compose deployment for the public API facade used by the GUI
and CLI.

```yaml
slice_id: S07_QUERY_REPORT_API_SERVICE_COMPOSE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_java_backend
  - senior_react_frontend
  - senior_tester
affected_files:
  - deployment/docker-compose/services/query-report-api-service.compose.yml
affected_modules:
  - query-report-api-service
affected_contracts:
  - contracts/openapi/gateway-api.yaml
  - contracts/grpc/repository-analysis.proto
  - contracts/grpc/analysis-job.proto
dependencies:
  - S02_REPOSITORY_SOURCE_SERVICE_COMPOSE
  - S06_ANALYSIS_ORCHESTRATOR_SERVICE_COMPOSE
parallel_group: P3
file_locks:
  - deployment/docker-compose/services/query-report-api-service.compose.yml
contract_locks:
  - public-query-report-api
architecture_locks:
  - public-api-facade
quality_gates:
  targeted:
    - ./gradlew :query-report-api-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :query-report-api-service:bootJar --dependency-verification strict --console=plain --stacktrace
    - docker compose -f deployment/docker-compose/services/query-report-api-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Public API exposes private repository-source paths or internal diagnostics.
  - GUI would need to call internal gRPC worker endpoints directly.
```

Done criteria:

- The public API is exposed on a stable host port.
- Backend service references use Compose service names and verified ports.

### Slice 08 - CLI Client Compose Tool Profile

Purpose: add a deployment descriptor or documented tool profile for the public
CLI client without promoting it into an always-on backend service.

```yaml
slice_id: S08_CLI_CLIENT_COMPOSE_TOOL_PROFILE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_java_backend
  - senior_tester
affected_files:
  - .dockerignore
  - deployment/docker-compose/services/cli-client.compose.yml
  - cli-client/Dockerfile
affected_modules:
  - cli-client
affected_contracts:
  - contracts/cli/gateway-cli-contract.md
  - contracts/openapi/gateway-api.yaml
dependencies:
  - S07_QUERY_REPORT_API_SERVICE_COMPOSE
parallel_group: P4
file_locks:
  - .dockerignore
  - deployment/docker-compose/services/cli-client.compose.yml
  - cli-client/Dockerfile
contract_locks:
  - cli-public-api-client
architecture_locks:
  - cli-is-not-backend-service
quality_gates:
  targeted:
    - ./gradlew :cli-client:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :cli-client:installDist --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :cli-client:build --dependency-verification strict --console=plain --stacktrace
    - docker compose --profile tools -f deployment/docker-compose/services/cli-client.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - CLI container is modeled as an always-on backend service.
  - CLI bypasses query-report-api-service and calls private services directly.
```

Done criteria:

- CLI Compose usage is profile-based or one-shot.
- The CLI targets the public API only.

### Slice 09 - Observability Stack Compose Boundary

Purpose: add verified local observability deployment material without turning
observability into a shared Java runtime module or evidence source.

```yaml
slice_id: S09_OBSERVABILITY_STACK_COMPOSE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - observability_runtime_diagnostics
  - senior_system_architect
  - senior_tester
affected_files:
  - deployment/docker-compose/services/observability-stack.compose.yml
  - deployment/observability/service-diagnostics-policy.yaml
affected_modules:
  - observability-stack
affected_contracts: []
dependencies:
  - S01_ROOT_FORENSIC_ANALYTICS_STACK
parallel_group: P4
file_locks:
  - deployment/docker-compose/services/observability-stack.compose.yml
  - deployment/observability/service-diagnostics-policy.yaml
contract_locks: []
architecture_locks:
  - observability-is-diagnostics-only
quality_gates:
  targeted:
    - ./gradlew :observability-stack:test --dependency-verification strict --console=plain --stacktrace
    - docker compose --profile diagnostics -f deployment/docker-compose/services/observability-stack.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0005-adapter-logging-observability-boundary.md
stop_conditions:
  - Observability logs or traces are treated as verified forensic evidence.
  - A shared Java logging or telemetry module is introduced.
```

Done criteria:

- Observability remains optional and profile-gated unless verified otherwise.
- Diagnostic fields and redaction policy remain documented.

### Slice 10 - Testbed Compose Consumer

Purpose: add testbed Compose consumption or a non-production descriptor without
promoting testbed into a productive service.

```yaml
slice_id: S10_TESTBED_COMPOSE_CONSUMER
profile: FULL_PATH
owner: senior_tester
secondary_reviewers:
  - senior_devops
  - senior_system_architect
affected_files:
  - deployment/docker-compose/services/testbed.compose.yml
  - testbed/README.md
affected_modules:
  - testbed
affected_contracts: []
dependencies:
  - S01_ROOT_FORENSIC_ANALYTICS_STACK
parallel_group: P4
file_locks:
  - deployment/docker-compose/services/testbed.compose.yml
  - testbed/README.md
contract_locks: []
architecture_locks:
  - testbed-is-non-production
quality_gates:
  targeted:
    - ./gradlew :testbed:test --dependency-verification strict --console=plain --stacktrace
    - docker compose --profile testbed -f deployment/docker-compose/services/testbed.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Productive service depends on testbed code or fixtures.
  - Test-only data is represented as production evidence.
```

Done criteria:

- Testbed remains non-production.
- Compose use is documented as integration environment support.

### Slice 11 - Forensic Ingestion Service Compose

Purpose: add local Compose deployment for the transitional forensic ingestion
service while avoiding target-service aliasing.

```yaml
slice_id: S11_FORENSIC_INGESTION_SERVICE_COMPOSE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_java_backend
  - ingestion_handoff_reviewer
  - senior_tester
affected_files:
  - deployment/docker-compose/services/forensic-ingestion-service.compose.yml
affected_modules:
  - forensic-ingestion-service
affected_contracts:
  - contracts/grpc/forensic-ingestion.proto
dependencies:
  - S01_ROOT_FORENSIC_ANALYTICS_STACK
parallel_group: P5
file_locks:
  - deployment/docker-compose/services/forensic-ingestion-service.compose.yml
contract_locks:
  - forensic-ingestion-transitional-grpc
architecture_locks:
  - transitional-service-not-target-alias
quality_gates:
  targeted:
    - ./gradlew :forensic-ingestion-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :forensic-ingestion-service:bootJar --dependency-verification strict --console=plain --stacktrace
    - docker compose -f deployment/docker-compose/services/forensic-ingestion-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Descriptor hides the distinction between forensic-ingestion-service and ingestion-service.
  - Host ports collide with ingestion-service without explicit resolution.
```

Done criteria:

- The descriptor labels the service as transitional.
- Port collisions with `ingestion-service` are resolved.

### Slice 12 - Forensic Gateway Service Compose

Purpose: add local Compose deployment for the transitional gateway facade
without confusing it with the target `query-report-api-service`.

```yaml
slice_id: S12_FORENSIC_GATEWAY_SERVICE_COMPOSE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_java_backend
  - senior_system_architect
  - senior_tester
affected_files:
  - deployment/docker-compose/services/forensic-gateway-service.compose.yml
affected_modules:
  - forensic-gateway-service
affected_contracts:
  - contracts/openapi/gateway-api.yaml
dependencies:
  - S13_ANALYSIS_STORE_SERVICE_COMPOSE
parallel_group: P7
file_locks:
  - deployment/docker-compose/services/forensic-gateway-service.compose.yml
contract_locks:
  - transitional-gateway-api
architecture_locks:
  - transitional-facade-not-target-alias
quality_gates:
  targeted:
    - ./gradlew :forensic-gateway-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :forensic-gateway-service:bootJar --dependency-verification strict --console=plain --stacktrace
    - docker compose -f deployment/docker-compose/services/forensic-gateway-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Gateway is described as the target public API owner.
  - Public responses expose private worker or store internals.
```

Done criteria:

- The descriptor preserves gateway as transitional repository-to-BTM facade.
- Host-port conflicts with `query-report-api-service` are avoided.
- Standalone Compose validation is not broken by undefined dependencies.

### Slice 13 - Analysis Store Service Compose

Purpose: add local Compose deployment for transitional analysis store
orchestration and artifact metadata.

```yaml
slice_id: S13_ANALYSIS_STORE_SERVICE_COMPOSE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_java_backend
  - senior_analysis_storage_architect
  - senior_tester
affected_files:
  - deployment/docker-compose/services/analysis-store-service.compose.yml
affected_modules:
  - analysis-store-service
affected_contracts:
  - contracts/grpc/analysis-job.proto
dependencies:
  - S14_REPOSITORY_ANALYSIS_SERVICE_COMPOSE
  - S15_JAVA_AST_ANALYSIS_SERVICE_COMPOSE
  - S16_JOERN_CPG_ANALYSIS_SERVICE_COMPOSE
  - S17_BTM_GENERATION_SERVICE_COMPOSE
parallel_group: P6
file_locks:
  - deployment/docker-compose/services/analysis-store-service.compose.yml
contract_locks:
  - transitional-analysis-store
architecture_locks:
  - transitional-store-not-canonical-target-store
quality_gates:
  targeted:
    - ./gradlew :analysis-store-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :analysis-store-service:bootJar --dependency-verification strict --console=plain --stacktrace
    - docker compose -f deployment/docker-compose/services/analysis-store-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Analysis Store is described as canonical FA-MSA-001 persistence owner.
  - Another service writes its private store directly.
```

Done criteria:

- The descriptor preserves transitional status and owner API boundaries.
- Standalone Compose validation is not broken by undefined dependencies;
  Dockerfile health checks remain available when fragments are combined.

### Slice 14 - Repository Analysis Service Compose

Purpose: add local Compose deployment for the transitional repository analysis
service.

```yaml
slice_id: S14_REPOSITORY_ANALYSIS_SERVICE_COMPOSE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_java_backend
  - source_analysis_reviewer
  - senior_tester
affected_files:
  - deployment/docker-compose/services/repository-analysis-service.compose.yml
affected_modules:
  - repository-analysis-service
affected_contracts:
  - contracts/grpc/repository-analysis.proto
dependencies:
  - S01_ROOT_FORENSIC_ANALYTICS_STACK
parallel_group: P5
file_locks:
  - deployment/docker-compose/services/repository-analysis-service.compose.yml
contract_locks:
  - transitional-repository-analysis
architecture_locks:
  - transitional-repository-analysis-not-repository-source
quality_gates:
  targeted:
    - ./gradlew :repository-analysis-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :repository-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
    - docker compose -f deployment/docker-compose/services/repository-analysis-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Descriptor claims target repository-source ownership.
  - Repository workspaces are shared with non-owner services.
```

Done criteria:

- Transitional and target repository services remain distinct.
- Private workspace ownership is explicit.

### Slice 15 - Java AST Analysis Service Compose

Purpose: add local Compose deployment for transitional Java AST analysis.

```yaml
slice_id: S15_JAVA_AST_ANALYSIS_SERVICE_COMPOSE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_java_backend
  - source_analysis_reviewer
  - senior_tester
affected_files:
  - deployment/docker-compose/services/java-ast-analysis-service.compose.yml
affected_modules:
  - java-ast-analysis-service
affected_contracts:
  - contracts/grpc/java-ast-analysis.proto
  - contracts/grpc/java-ast-source-facts-v1.schema.json
dependencies:
  - S01_ROOT_FORENSIC_ANALYTICS_STACK
parallel_group: P5
file_locks:
  - deployment/docker-compose/services/java-ast-analysis-service.compose.yml
contract_locks:
  - transitional-java-ast-analysis
architecture_locks:
  - static-analysis-not-runtime-evidence
quality_gates:
  targeted:
    - ./gradlew :java-ast-analysis-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :java-ast-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
    - docker compose -f deployment/docker-compose/services/java-ast-analysis-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Static AST facts are presented as observed runtime flow.
  - Artifact volume is shared as canonical storage.
```

Done criteria:

- Compose preserves transitional service name and static evidence semantics.
- Artifact ownership is documented.

### Slice 16 - Joern CPG Analysis Service Compose

Purpose: add local Compose deployment for transitional Joern CPG analysis.

```yaml
slice_id: S16_JOERN_CPG_ANALYSIS_SERVICE_COMPOSE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_joern_cpg_specialist
  - senior_java_backend
  - senior_tester
affected_files:
  - deployment/docker-compose/services/joern-cpg-analysis-service.compose.yml
affected_modules:
  - joern-cpg-analysis-service
affected_contracts:
  - contracts/grpc/joern-cpg-analysis.proto
dependencies:
  - S01_ROOT_FORENSIC_ANALYTICS_STACK
parallel_group: P5
file_locks:
  - deployment/docker-compose/services/joern-cpg-analysis-service.compose.yml
contract_locks:
  - transitional-joern-cpg-analysis
architecture_locks:
  - joern-cpg-owned-workspace-volume
quality_gates:
  targeted:
    - ./gradlew :joern-cpg-analysis-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :joern-cpg-analysis-service:bootJar --dependency-verification strict --console=plain --stacktrace
    - docker compose -f deployment/docker-compose/services/joern-cpg-analysis-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Repository-source private workspace volume is mounted into Joern CPG.
  - Semantic artifacts are treated as canonical source evidence.
```

Done criteria:

- Joern CPG artifacts remain service-owned.
- External Joern image pull requirements are documented.

### Slice 17 - BTM Generation Service Compose

Purpose: add local Compose deployment for BTM generation artifacts.

```yaml
slice_id: S17_BTM_GENERATION_SERVICE_COMPOSE
profile: FULL_PATH
owner: senior_devops
secondary_reviewers:
  - senior_java_backend
  - senior_tester
affected_files:
  - deployment/docker-compose/services/btm-generation-service.compose.yml
affected_modules:
  - btm-generation-service
affected_contracts:
  - contracts/grpc/btm-generation.proto
dependencies:
  - S01_ROOT_FORENSIC_ANALYTICS_STACK
parallel_group: P5
file_locks:
  - deployment/docker-compose/services/btm-generation-service.compose.yml
contract_locks:
  - btm-generation-grpc
architecture_locks:
  - btm-artifact-owner
quality_gates:
  targeted:
    - ./gradlew :btm-generation-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :btm-generation-service:bootJar --dependency-verification strict --console=plain --stacktrace
    - docker compose -f deployment/docker-compose/services/btm-generation-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Generated BTM artifacts are treated as verified runtime execution evidence.
  - BTM output path is mounted writable by multiple producers.
```

Done criteria:

- BTM artifact volume is service-owned.
- Compose descriptor uses the verified Dockerfile.

### Slice 18 - Graph Replay Service Deployment Readiness

Purpose: satisfy the module slice requirement for `graph-replay-service` while
preserving the verified fact that no service implementation exists yet.

```yaml
slice_id: S18_GRAPH_REPLAY_SERVICE_READINESS
profile: FULL_PATH
owner: senior_system_architect
secondary_reviewers:
  - replay_graph_llm_reviewer
  - senior_devops
  - senior_tester
affected_files:
  - graph-replay-service/README.md
  - deployment/docker-compose/services/graph-replay-service.compose.yml
affected_modules:
  - graph-replay-service
affected_contracts: []
dependencies:
  - S01_ROOT_FORENSIC_ANALYTICS_STACK
parallel_group: P4
file_locks:
  - graph-replay-service/README.md
  - deployment/docker-compose/services/graph-replay-service.compose.yml
contract_locks: []
architecture_locks:
  - graph-replay-is-planned-projection
quality_gates:
  targeted:
    - ./gradlew :graph-replay-service:tasks --dependency-verification strict --console=plain --stacktrace
    - docker compose --profile planned -f deployment/docker-compose/services/graph-replay-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - A runnable graph-replay container would require inventing service code.
  - Graph or replay projections are described as canonical evidence.
```

Done criteria:

- The slice either adds a verified disabled/profile-gated deployment marker or
  records a blocking non-runnable state.
- No fake graph, replay, or health data is created.

### Slice 19 - Report Generation Service Deployment Readiness

Purpose: satisfy the module slice requirement for `report-generation-service`
while preserving the verified fact that no service implementation exists yet.

```yaml
slice_id: S19_REPORT_GENERATION_SERVICE_READINESS
profile: FULL_PATH
owner: senior_system_architect
secondary_reviewers:
  - replay_graph_llm_reviewer
  - senior_devops
  - senior_tester
affected_files:
  - report-generation-service/README.md
  - deployment/docker-compose/services/report-generation-service.compose.yml
affected_modules:
  - report-generation-service
affected_contracts: []
dependencies:
  - S01_ROOT_FORENSIC_ANALYTICS_STACK
parallel_group: P4
file_locks:
  - report-generation-service/README.md
  - deployment/docker-compose/services/report-generation-service.compose.yml
contract_locks: []
architecture_locks:
  - report-generation-is-planned
quality_gates:
  targeted:
    - ./gradlew :report-generation-service:tasks --dependency-verification strict --console=plain --stacktrace
    - docker compose --profile planned -f deployment/docker-compose/services/report-generation-service.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - A runnable report-generation container would require inventing service code.
  - Reports present hypotheses or generated output as verified evidence.
```

Done criteria:

- The non-runnable or optional-later status remains explicit.
- No fake report service health endpoint is created.

### Slice 20 - Forensic UI GUI Deployment Integration

Purpose: add the frontend Compose integration required for local GUI
interaction.

```yaml
slice_id: S20_FORENSIC_UI_GUI_DEPLOYMENT
profile: FULL_PATH
owner: senior_react_frontend
secondary_reviewers:
  - senior_devops
  - senior_tester
affected_files:
  - deployment/docker-compose/services/forensic-ui.compose.yml
  - forensic-ui/Dockerfile
  - forensic-ui/nginx.conf
  - forensic-ui/README.md
affected_modules:
  - forensic-ui
affected_contracts:
  - contracts/openapi/gateway-api.yaml
dependencies:
  - S07_QUERY_REPORT_API_SERVICE_COMPOSE
parallel_group: P7
file_locks:
  - deployment/docker-compose/services/forensic-ui.compose.yml
  - forensic-ui/Dockerfile
  - forensic-ui/nginx.conf
  - forensic-ui/README.md
contract_locks:
  - public-query-report-api
architecture_locks:
  - ui-public-api-only
quality_gates:
  targeted:
    - cd forensic-ui && npm ci
    - cd forensic-ui && npm run test
    - cd forensic-ui && npm run build
    - docker compose -f deployment/docker-compose/services/forensic-ui.compose.yml config
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - UI must call internal worker services or private gRPC endpoints.
  - VITE_API_BASE_URL cannot be configured for the Compose public API route.
  - nginx `/api` remains the hardcoded 502 response while documentation claims GUI deployment works.
  - Browser connectivity relies on unverified CORS or preflight behavior.
```

Done criteria:

- GUI container serves the app on a documented local host port.
- Browser API calls target the public `query-report-api-service` route.
- `/api/health` succeeds from the same origin used by the GUI when runtime
  smoke checks are executed.

### Slice 21 - Deployment Description And Operator Runbook

Purpose: create the deployment description requested by the user, including
build, start, health, GUI, logs, error collection, and cleanup steps.

```yaml
slice_id: S21_DEPLOYMENT_DESCRIPTION_AND_RUNBOOK
profile: FULL_PATH
owner: senior_documentation_engineer
secondary_reviewers:
  - senior_devops
  - senior_tester
  - senior_system_architect
affected_files:
  - docs/deployment/forensic-analytics-docker-compose.md
  - deployment/docker-compose/README.md
  - docs/arc42/07-deployment-view.md
affected_modules:
  - forensic_analytics
  - forensic-ui
affected_contracts: []
dependencies:
  - S02_REPOSITORY_SOURCE_SERVICE_COMPOSE
  - S03_INGESTION_SERVICE_COMPOSE
  - S04_JAVA_PARSER_ANALYSIS_SERVICE_COMPOSE
  - S05_JOERN_ANALYSIS_SERVICE_COMPOSE
  - S06_ANALYSIS_ORCHESTRATOR_SERVICE_COMPOSE
  - S07_QUERY_REPORT_API_SERVICE_COMPOSE
  - S08_CLI_CLIENT_COMPOSE_TOOL_PROFILE
  - S09_OBSERVABILITY_STACK_COMPOSE
  - S10_TESTBED_COMPOSE_CONSUMER
  - S11_FORENSIC_INGESTION_SERVICE_COMPOSE
  - S12_FORENSIC_GATEWAY_SERVICE_COMPOSE
  - S13_ANALYSIS_STORE_SERVICE_COMPOSE
  - S14_REPOSITORY_ANALYSIS_SERVICE_COMPOSE
  - S15_JAVA_AST_ANALYSIS_SERVICE_COMPOSE
  - S16_JOERN_CPG_ANALYSIS_SERVICE_COMPOSE
  - S17_BTM_GENERATION_SERVICE_COMPOSE
  - S18_GRAPH_REPLAY_SERVICE_READINESS
  - S19_REPORT_GENERATION_SERVICE_READINESS
  - S20_FORENSIC_UI_GUI_DEPLOYMENT
parallel_group: P8
file_locks:
  - docs/deployment/forensic-analytics-docker-compose.md
  - deployment/docker-compose/README.md
  - docs/arc42/07-deployment-view.md
contract_locks: []
architecture_locks:
  - deployment-docs-do-not-claim-production-readiness
quality_gates:
  targeted:
    - docker compose -f deployment/docker-compose/forensic-analytics.local.yml config
    - git diff --check
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Documentation claims a runtime check passed when it was not executed.
  - Documentation hides non-runnable planned roots.
```

Done criteria:

- The runbook names exact build, config, startup, health, GUI, log, and cleanup
  commands.
- Runtime checks are clearly marked as executed, skipped, or blocked.

### Slice 22 - Final Stack Verification And Handoff

Purpose: validate the combined stack model, inspect diffs, synchronize arc42
deployment notes, and prepare the workflow-execute checkpoint.

```yaml
slice_id: S22_FINAL_STACK_VERIFICATION_AND_HANDOFF
profile: FULL_PATH
owner: senior_tester
secondary_reviewers:
  - senior_devops
  - senior_system_architect
  - senior_documentation_engineer
affected_files:
  - docs/workflow/execution-report.md
  - docs/workflow/arc42-check-status.md
  - deployment/docker-compose/README.md
  - docs/deployment/forensic-analytics-docker-compose.md
affected_modules:
  - forensic_analytics
affected_contracts: []
dependencies:
  - S21_DEPLOYMENT_DESCRIPTION_AND_RUNBOOK
parallel_group: P9
file_locks:
  - docs/workflow/execution-report.md
  - docs/workflow/arc42-check-status.md
  - deployment/docker-compose/README.md
  - docs/deployment/forensic-analytics-docker-compose.md
contract_locks: []
architecture_locks:
  - final-quality-gate
quality_gates:
  targeted:
    - docker compose -f deployment/docker-compose/forensic-analytics.local.yml config
    - git diff --check
  required:
    - ./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/arc42/07-deployment-view.md
  adr: docs/adr/ADR-0017-target-microservices-service-landscape.md
stop_conditions:
  - Any earlier slice reports unresolved architecture, quality, or deployment blockers.
  - Full local quality gate fails because of current slice changes.
```

Done criteria:

- Combined Compose model validates.
- Required quality gates are recorded.
- Final report distinguishes verified runtime evidence from planned or skipped
  checks.

## Slice Dependency Graph

See `docs/workflow/slice-dependency-map.md` for the graph and parallel groups.

## Parallelization Opportunities

- S02 through S05 can start after S01 because their service files are
  disjoint.
- S11 and S14 through S17 can start after S01 because they write separate
  transitional service fragments.
- S08, S09, S10, S18, and S19 can run in parallel after S01 if their
  non-runtime or tool-profile status remains explicit.
- S12 waits for S13 because the transitional gateway must not start before
  the transitional analysis store descriptor exists.
- S20 waits for S07 because the GUI must target the public API.
- S21 waits for all service fragments so the runbook documents the actual
  generated files.
- S22 is final and must run last.

## Quality-Gate Expectations

Each implementation slice must run its targeted module checks and
`docker compose ... config` for the changed descriptor. The minimum repository
gate is required before merging slice results. The full local quality gate is
required before final commit readiness or publication.

Optional Docker image build and runtime checks may require external image
pulls, especially for Joern. If they are not executed, the execution report
must say so and must not claim runtime readiness.

## Documentation Synchronization Points

- Update `deployment/docker-compose/README.md` only in S01, S21, or S22 so
  shared Compose documentation changes remain serialized.
- Update `docs/deployment/forensic-analytics-docker-compose.md` in S21.
- Update `docs/arc42/07-deployment-view.md` only with verified deployment
  evidence and planned-vs-implemented wording.
- Preserve ADR-0017 target-vs-transitional service language.
- Preserve `docs/architecture/service-roots.md` distinctions for target,
  transitional, optional, non-production, and planned roots.

## Stop Conditions

Stop workflow execution if:

- a named root lacks a Dockerfile and no verified executable runtime exists;
- a Compose descriptor would need a guessed health endpoint, port, artifact
  path, environment variable, or command;
- `.dockerignore` prevents a service Dockerfile from copying a verified boot jar
  while the slice would claim image-build readiness;
- service-private volumes are mounted into non-owner services;
- public UI or API paths expose private service data;
- UI API routing depends on unverified CORS instead of a verified same-origin
  proxy or explicitly tested browser-safe route;
- target and transitional service names are collapsed into aliases;
- a runtime smoke check fails and the failure cannot be classified;
- Docker is unavailable for a slice that requires Docker runtime evidence;
- any quality command from `QUALITY.md` cannot be verified or fails because of
  current changes.

## Handoff To Workflow Execute

`workflow execute` must:

1. Read this complete workflow and all metadata blocks.
2. Execute slices in dependency order.
3. Use callable subagents or role reviews per `role-ownership.md`.
4. Run targeted verification after each slice.
5. Inspect `git diff` and `git diff --check` after each slice.
6. Preserve planned/non-runnable status where implementation is not verified.
7. Record exact Docker, Gradle, npm, curl, and browser smoke evidence in
   `docs/workflow/execution-report.md`.

## Definition Of Done

The workflow is done when:

- every listed root has an executed slice outcome;
- runnable roots have per-service Compose descriptors or documented blockers;
- the root stack uses Docker network `forensic_analytics`;
- the deployment runbook exists and is aligned with the generated files;
- the GUI route is documented and smoke-tested when Docker runtime is
  available;
- no planned root is misrepresented as implemented;
- quality gates from `QUALITY.md` are executed and recorded;
- arc42 deployment status is checked and updated only with verified evidence.
