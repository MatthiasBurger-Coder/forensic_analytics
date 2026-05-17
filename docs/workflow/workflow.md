# Microservices Ecosystem Conversion Workflow

## Status

Planned active workflow.

This workflow converts the user-supplied Microservices Ecosystem Conversion
draft into the active repository workflow under `docs/workflow`.

The workflow governs future migration work. It does not itself move production
code, create service implementations, change contracts, alter persistence,
publish deployment descriptors, commit or push.

## Verified Baseline

- Repository root: `/mnt/d/Projects/forensic_analytics`
- Windows path: `D:/Projects/forensic_analytics`
- Active branch: `architecture/microservices-ecosystem-conversion-20260516`
- Branch type: `architecture`, selected because the workflow plans an
  architecture-changing microservice conversion.
- Authoritative agent rules: `AGENTS.md`
- Authoritative quality rules: `QUALITY.md`
- Workflow authoring rules: `.agents/skills/workflow-authoring/SKILL.md`
- Workflow execution rules: `.codex/workflow/workflow-execution-rules.md`
- Project routing rules: `.agents/orchestrator/routing-rules.md`
- Project swarm rules: `.agents/orchestrator/swarm-orchestrator.md`
- Current state baseline: `docs/workflow/current-state-baseline.md`

## Requirement Source And Gate Decision

The user supplied a `workflow create` draft for converting the active
`forensic_analytics` project into a microservices ecosystem on 2026-05-16.

Three Amigos decision:

```text
READY_FOR_WORKFLOW
```

This decision approves workflow authoring only. Service extraction remains
subject to slice-specific service-boundary, contract, data-ownership,
runtime-readiness, rollback and quality-gate reviews.

## Target Outcome

After `workflow execute` completes this workflow:

- The platform has documented and implemented service boundaries for gateway,
  ingestion, repository analysis, AST analysis, Joern analysis, BTM generation,
  analysis storage, graph/replay, report generation and frontend access.
- Each service is independently buildable, startable, testable, configurable,
  health-checkable, containerized and deployable.
- No service shares Java implementation, domain, DTO, repository, service,
  fixture, utility or internal error-model modules with another service.
- Cross-service communication uses only REST/OpenAPI, gRPC/protobuf or approved
  event contracts.
- `contracts/` contains interface contracts only.
- Docker Compose, Docker Swarm and Kubernetes material exists for the service
  ecosystem.
- Documentation, tests and quality-gate evidence describe the new runtime
  model.
- Commit and push happen only in the final slice after required quality gates
  and commit-readiness review pass.

## Non-Goals

- Do not perform a big-bang migration.
- Do not move existing production code before current-state and service-boundary
  slices prove the target ownership.
- Do not introduce `forensic-common`, `shared-core`, `common-utils` or any
  equivalent shared runtime code module.
- Do not share Java domain models, DTOs, events, repositories, services,
  utility classes, test fixtures or internal error models across services.
- Do not treat current Gradle modules as deployed microservices without runtime
  evidence.
- Do not directly access another service's database.
- Do not silently invent missing graph labels, table names, Gradle tasks or API
  routes. The user clarification from 2026-05-16 permits plausible provisional
  inter-service communication contracts when final details are not yet defined,
  provided the contracts are logically consistent, documented as provisional and
  kept out of shared Java implementation modules.
- Do not claim Docker Swarm or Kubernetes readiness before manifests and
  validation evidence exist.

## Architecture Constraints

- Every service owns its own hexagonal architecture:
  - `domain`
  - `application`
  - `adapter/inbound`
  - `adapter/outbound`
  - `infrastructure`
- Domain and application code remain independent from frameworks, generated
  transport types, storage clients, runtime infrastructure and provider SDKs.
- Adapters and infrastructure depend inward. Domain and application do not
  depend outward.
- Contracts are external interface descriptions, not Java implementation
  sharing.
- Graph, vector and report views are projections or derived outputs, not hidden
  sources of truth.
- LLM output remains generated analysis or hypothesis, never verified evidence.

## Target Service Landscape

| Service | Responsibility | Initial Source Evidence | Primary Protocols |
|---|---|---|---|
| `forensic-gateway-service` | UI, CLI and external client entry point; orchestration facade without analysis logic | `forensic-analytics-rest`, `forensic-analytics-cli`, Boot wiring | REST inbound, REST/gRPC outbound |
| `forensic-ingestion-service` | Receive plugin, scanner and runtime data over gRPC; validate ingestion packages | `forensic-analytics-ingestion-grpc`, `forensic_ingestion.proto`, ingestion use cases | gRPC inbound, REST/gRPC/event outbound |
| `repository-analysis-service` | Repository checkout, branch resolution and workspace preparation | `forensic-analytics-adapter-repository-source`, ingestion workspace services | REST/gRPC inbound, gRPC/event outbound |
| `java-ast-analysis-service` | JavaParser source scanning and stable source identifiers | `forensic-analytics-adapter-javaparser` | gRPC/event inbound and outbound |
| `joern-cpg-analysis-service` | Joern CPG/CFG/DFG analysis and semantic artifact mapping | `forensic-analytics-adapter-joern-docker`, `docker/joern/**` | gRPC/event inbound and outbound |
| `btm-generation-service` | Generate versioned BTM rule artifacts from delivered analysis facts | `RuleGenerationPort`, `.btm` tests and arc42 BTM decisions | REST/gRPC inbound, artifact outbound |
| `analysis-store-service` | Own analysis job lifecycle, artifact metadata and later normalized facts, incidents and correlations | `forensic-analytics-persistence`, analysis stores and ports | gRPC inbound for Slice 05 job lifecycle; later database outbound |
| `graph-replay-service` | Build graph/runtime overlays and exception-centered replay | arc42 graph/replay concepts, semantic graph model | REST/gRPC inbound, graph DB outbound |
| `report-generation-service` | Produce reports, incident context packages and LLM-ready packages | arc42 reporting/LLM concepts, artifact/report storage areas | REST/gRPC inbound, storage outbound |
| `frontend-web-app` | React frontend communicating through Gateway or public APIs only | `forensic-ui` | REST through Gateway |

## Target Repository Shape

The planned target structure is:

```text
services/
  forensic-gateway-service/
  forensic-ingestion-service/
  repository-analysis-service/
  java-ast-analysis-service/
  joern-cpg-analysis-service/
  btm-generation-service/
  analysis-store-service/
  graph-replay-service/
  report-generation-service/
frontend/
  frontend-web-app/
contracts/
  grpc/
  openapi/
  events/
deployment/
  docker-compose/
  docker-swarm/
  kubernetes/
docs/
  architecture/
  workflow/
```

`contracts/` may contain `.proto`, OpenAPI YAML/JSON, event schema documents and
contract documentation only. It must not contain Java service code, shared
utility classes, shared domain models, shared mappers, shared exceptions or
shared Spring configuration.

## Slice Execution Protocol

Each slice follows this sequence:

1. Read the slice goal.
2. Verify the active workflow branch.
3. Identify affected services and files.
4. Run the Three Amigos or migration safety gate when required.
5. Run Senior System Architect boundary review.
6. Route implementation to the listed owner and reviews.
7. Apply only the smallest verified change.
8. Run targeted tests and applicable quality checks.
9. Inspect `git diff` and `git diff --check`.
10. Document the result before continuing.

Stop if any expected module, class, method, package, contract, task, endpoint,
event field, graph label, table, deployment file or quality command cannot be
verified exactly, unless the user has explicitly authorized provisional
contract definition for that slice and the resulting communication remains
documented, logical and reviewable.

## Slice 00 - Repository And Current-State Analysis

Purpose: record the current project state before architecture changes.

Owner: Senior System Architect.

Reviews: Microservice Senior Expert, Senior Java Backend Developer, Senior
DevOps Engineer, Senior Tester.

Allowed write scope:

- `docs/architecture/current-state.md`
- `docs/architecture/current-coupling-map.md`
- `docs/architecture/current-build-and-test-map.md`

Tasks:

- Document all Gradle modules and current responsibilities.
- Identify current repository analysis, JavaParser, Joern, BTM, persistence,
  graph/replay/report, gRPC, CLI, REST, Boot and frontend capabilities.
- Document current build, test, Docker and deployment material.
- Document current coupling and no service-independence claims.
- Record `QUALITY.md` requirements.

Done criteria:

- All verified modules are documented.
- Current capabilities are mapped to candidate target services.
- Critical couplings are visible.
- No architecture change has been made.

Verification:

```bash
git diff --check
```

For non-documentation changes, also run the minimum command from `QUALITY.md`.

## Slice 01 - Target Architecture And Service Boundaries

Purpose: define the final service decomposition and service ownership model.

Owner: Senior System Architect.

Reviews: Three Amigos Requirement Gatekeeper, Microservice Senior Expert, Data
Ownership And Persistence Steward, Senior Tester.

Allowed write scope:

- `docs/architecture/target-microservices-architecture.md`
- `docs/architecture/service-boundaries.md`
- `docs/architecture/service-communication-matrix.md`
- `docs/architecture/data-ownership.md`
- related arc42 and ADR updates when required by governance review

Tasks:

- Finalize service boundaries, ownership, data authority and non-scope.
- Define inbound and outbound protocols for each service.
- Create the communication matrix.
- Model analysis-job, plugin-ingestion, report and replay data flows.
- Define rollback or strangler strategy for behavior-changing extraction.
- Document forbidden coupling and shared-code rules.

Done criteria:

- Each service has a business responsibility and owned process or data.
- Each service has explicit allowed communication.
- No shared runtime-code module is planned.
- Data ownership and non-owner access paths are explicit.

Verification:

```bash
git diff --check
```

## Slice 02 - Prepare Independent-Service Monorepo Structure

Purpose: prepare repository structure for independently buildable services
without moving business logic blindly.

Owner: Senior DevOps Engineer.

Reviews: Senior System Architect, Senior Java Backend Developer, Senior Tester.

Allowed write scope:

- `services/**`
- `frontend/**`
- `contracts/**`
- `deployment/**`
- `docs/architecture/service-migration-map.md`
- `docs/architecture/monorepo-service-build-strategy.md`
- build files only after Gradle strategy review

Tasks:

- Create target directories.
- Keep `contracts/` contract-only.
- Document which current code is planned for which target service.
- Decide whether each service uses its own Gradle build or root-included
  projects.
- Do not move functional logic in this slice unless separately approved.

Done criteria:

- Target structure exists.
- Migration mapping is documented.
- No uncontrolled logic move occurred.
- `contracts/` contains no runtime Java code.

Verification:

```bash
git diff --check
```

Run repository Gradle checks if build files changed.

## Slice 03 - Introduce Contract-First Communication

Purpose: define service communication contracts before service implementation.

Owner: Senior gRPC/Proto Specialist.

Reviews: Contract Governance Expert, Senior System Architect, Microservice
Senior Expert, Senior Tester.

Allowed write scope:

- `contracts/grpc/forensic-ingestion.proto`
- `contracts/grpc/analysis-job.proto`
- `contracts/openapi/gateway-api.yaml`
- `contracts/events/analysis-events.md`
- `docs/architecture/contract-versioning.md`
- contract-test planning docs

Tasks:

- Define gRPC ingestion and analysis-job contracts.
- Define Gateway OpenAPI for workspaces, analysis jobs, results, reports,
  replay and health/status.
- Define event schemas when a broker is selected or planned.
- Document versioning, compatibility and breaking-change rules.
- Define contract-test strategy.

Done criteria:

- Planned service communication has explicit contracts.
- Contracts avoid implementation details.
- Error/status models and compatibility rules are documented.
- Generated-code boundaries are documented.

Verification:

```bash
git diff --check
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

## Slice 04 - Build `forensic-ingestion-service`

Purpose: create the first independent gRPC ingestion service.

Owner: Senior Java Backend Developer.

Reviews: Senior gRPC/Proto Specialist, Microservice Senior Expert, Senior
DevOps Engineer, Senior Tester.

Allowed write scope:

- `services/forensic-ingestion-service/**`
- service-local build files
- service-local tests
- service README and Dockerfile
- root build files only when required to register the service

Tasks:

- Create an independent Spring Boot service.
- Create service-local hexagonal layers.
- Implement the gRPC inbound adapter from verified contracts.
- Define service-internal ingestion domain models.
- Define outbound ports for store or broker handoff.
- Add healthcheck, tests, README and Dockerfile.

Done criteria:

- Service starts independently.
- gRPC endpoint is reachable in local verification.
- No dependency on internal classes of another service exists.
- Docker image can be built.

Verification:

- Use service-specific Gradle or service-local commands only after verifying the
  build path exists.
- Run the applicable `QUALITY.md` gate for production/build changes.

## Slice 05 - Build Initial `analysis-store-service`

Purpose: create the initial owned Analysis Store service boundary for analysis
job lifecycle state and artifact metadata registration.

Owner: Senior Java Backend Developer.

Reviews: Data Ownership And Persistence Steward, Senior System Architect,
Senior DevOps Engineer, Senior Tester.

Allowed write scope:

- `services/analysis-store-service/**`
- service README and Dockerfile
- related ownership documentation

Tasks:

- Create an independent Spring Boot service.
- Implement the `AnalysisJobService` job lifecycle subset from the provisional
  contract.
- Preserve request schema version, correlation ID, attributes, progress and
  artifact metadata in the service-owned model.
- Use service-local in-memory persistence as a non-durable first boundary.
- Document that durable database migrations, normalized facts, incidents and
  correlation indexes remain later slices.
- Document data ownership and one-writer rules for the implemented subset.
- Prevent direct database access from other services.

Done criteria:

- Analysis store builds, starts, exposes gRPC and health endpoints, and can be
  containerized independently.
- Job lifecycle and artifact metadata can be stored and queried through APIs.
- Retryable jobs can be leased again and concurrent workers cannot lease the
  same in-memory job twice.
- No shared entity class is introduced.
- Swarm/Kubernetes deployment evidence remains Slice 15 material.

Verification:

- Service-specific tests after service build registration.
- Applicable `QUALITY.md` gate.

## Slice 06 - Build `repository-analysis-service`

Purpose: move repository checkout, branch selection and workspace preparation
behind a service boundary.

Owner: Senior Java Backend Developer.

Reviews: Microservice Senior Expert, Senior DevOps Engineer, Senior Git
Workspace Specialist, Security Sandbox Specialist, Senior Tester.

Allowed write scope:

- `services/repository-analysis-service/**`
- `contracts/grpc/repository-analysis.proto`
- `settings.gradle.kts`
- root `build.gradle.kts` only for generated protobuf coverage exclusion
- service-local tests
- service README and Dockerfile
- contract adapters related to repository-analysis requests

Tasks:

- Encapsulate Git and workspace functionality inside the service.
- Define service-internal workspace models.
- Define and implement the provisional repository-analysis gRPC contract.
- Provide an API for analysis preparation.
- Prepare handoff to AST and Joern services through contracts.
- Document failure cases for missing repository, branch, checkout, workspace
  conflict and permissions.
- Enforce network-service Git security constraints: HTTPS-only repository URLs,
  no userinfo or credentials, no local paths, no SSH/SCP remotes, sanitized Git
  environment, no hooks, no submodules, no build/script/parser execution and
  redacted diagnostics.
- Keep mutable workspace paths private to the service. Public responses use
  opaque workspace IDs, source snapshot IDs, relative source roots, artifact
  references, completeness and diagnostics.

Done criteria:

- The service can independently prepare a repository-analysis job.
- Other services do not access its workspace internals.
- Failure cases are testable.
- Docker image can be built.
- Security Sandbox review approves the implemented URL, Git process, workspace
  confinement, cleanup and diagnostics behavior.

Verification: service-specific tests and applicable `QUALITY.md` gate.

## Slice 07 - Build `java-ast-analysis-service`

Purpose: extract JavaParser and AST analysis into an independent worker service.

Owner: Senior Java Backend Developer.

Reviews: Source Analysis Pipeline, Microservice Senior Expert, Senior Tester.

Allowed write scope:

- `services/java-ast-analysis-service/**`
- `contracts/grpc/java-ast-analysis.proto`
- `settings.gradle.kts`
- root `build.gradle.kts` only for generated protobuf coverage exclusion
- `.dockerignore` only for the service JAR build-context exception
- service-local tests
- service README and Dockerfile
- AST contract adapters

Scope clarification:

- Slice 07 is authorized to define a provisional `java-ast-analysis` gRPC
  v1 contract because the existing job contract only carries worker lifecycle
  and artifact metadata, not typed AST source facts or diagnostics.
- The provisional contract must remain logical and contract-first while final
  production schemas are still open. It must not use mutable workspace paths
  or shared Java DTO/domain classes.
- Current JavaParser behavior has no verified symbol solver. Slice 07 must
  report symbol resolution as an explicit limitation or diagnostic instead of
  claiming verified unresolved-symbol facts.

Tasks:

- Identify existing AST analysis behavior.
- Migrate or reimplement only verified AST responsibility.
- Create service-internal models and stable ID generation.
- Implement inbound analysis-job and outbound result contracts.
- Preserve unresolved symbol diagnostics.

Done criteria:

- Service runs independently.
- Java sources can be analyzed through the service boundary.
- Results are delivered through contracts.
- No shared domain class is used.

Verification: service-specific tests and applicable `QUALITY.md` gate.

## Slice 08 - Build `joern-cpg-analysis-service`

Purpose: isolate Joern analysis and CPG/CFG/DFG handling in its own runtime.

Owner: Senior Java Backend Developer.

Reviews: Senior Joern CPG Specialist, Senior DevOps Engineer, Microservice
Senior Expert, Senior Tester.

Allowed write scope:

- `services/joern-cpg-analysis-service/**`
- Joern service Dockerfile and runtime scripts
- service-local tests
- Joern contract adapters
- `contracts/grpc/joern-cpg-analysis.proto`
- `contracts/grpc/README.md`
- `settings.gradle.kts`
- root `build.gradle.kts` only for generated protobuf coverage exclusion
- `.dockerignore`
- workflow execution and quality documentation for Slice 08 evidence

Tasks:

- Capture Joern runtime requirements.
- Encapsulate Joern invocation inside the service.
- Map CPG/CFG/DFG artifacts into service-internal models.
- Prepare mapping to analysis IDs from contract data.
- Test unavailable Joern, oversized analysis, timeout, invalid workspace and
  incomplete mapping cases.

Done criteria:

- Service is independently startable.
- Joern dependencies are container-contained.
- No local developer Joern installation is required for service operation.
- Results are exchanged only through contracts.

Verification: service-specific tests, Docker build and applicable
`QUALITY.md` gate.

## Slice 09 - Build `btm-generation-service`

Purpose: move BTM rule generation into a service that generates artifacts from
delivered analysis facts.

Owner: Senior Java Backend Developer.

Reviews: Microservice Senior Expert, Senior Tester.

Allowed write scope:

- `services/btm-generation-service/**`
- service-local tests
- service README and Dockerfile
- BTM contract adapters, including `contracts/grpc/btm-generation.proto` and
  `contracts/grpc/README.md`
- service registration in `settings.gradle.kts`
- generated protobuf coverage exclusion in root `build.gradle.kts`
- `.dockerignore` service-jar allowlist entries

Tasks:

- Identify existing rule-generation behavior.
- Accept input only through contract data.
- Generate deterministic artifacts or API results.
- Test rule ID stability.
- Keep repository analysis out of this service.

Done criteria:

- Service generates BTM files from supplied facts.
- Service performs no repository scanning.
- Rules are reproducible.
- Docker image can be built.

Verification: service-specific tests and applicable `QUALITY.md` gate.

## Slice 10 - Build `graph-replay-service`

Purpose: isolate graph, runtime overlay and exception-centered replay.

Owner: Senior Java Backend Developer.

Reviews: Senior System Architect, Replay/Graph/LLM Reviewer, Data Ownership And
Persistence Steward, Microservice Senior Expert, Senior Tester.

Allowed write scope:

- `services/graph-replay-service/**`
- service-local tests
- service README and Dockerfile
- graph/replay contracts and documentation

Tasks:

- Identify graph and replay responsibilities from current code and docs.
- Model replay domain inside the service boundary.
- Access analysis-store data only through owner APIs.
- Encapsulate graph database access inside the service.
- Define exception-centered replay APIs.
- Test path reconstruction and missing-evidence representation.

Done criteria:

- Service runs independently.
- No direct analysis-store database coupling exists.
- Replay queries are available through API.
- Runtime overlay responsibility is documented.

Verification: service-specific tests and applicable `QUALITY.md` gate.

## Slice 11 - Build `report-generation-service`

Purpose: isolate reports and LLM context packages.

Owner: Senior Java Backend Developer.

Reviews: Senior UX Designer, Replay/Graph/LLM Reviewer, Microservice Senior
Expert, Senior Tester.

Allowed write scope:

- `services/report-generation-service/**`
- service-local tests
- service README and Dockerfile
- report and LLM package contracts

Tasks:

- Define report types and export formats.
- Define reproducible LLM context package structure.
- Access analysis-store and graph-replay services only through APIs.
- Preserve distinctions between confirmed evidence, derived analysis, gaps,
  hypotheses and generated text.

Done criteria:

- Reports are generated independently from Gateway.
- No direct foreign database coupling exists.
- LLM context packages are reproducible and labeled.
- Docker image can be built.

Verification: service-specific tests and applicable `QUALITY.md` gate.

## Slice 12 - Build `forensic-gateway-service`

Purpose: create the central API gateway for frontend, CLI and external access.

Owner: Senior Java Backend Developer.

Reviews: Senior System Architect, Senior UX Designer, Contract Governance
Expert, Senior Tester.

Allowed write scope:

- `services/forensic-gateway-service/**`
- service-local tests
- service README and Dockerfile
- Gateway OpenAPI adapters

Tasks:

- Implement REST API from the Gateway OpenAPI contract.
- Prepare analysis-job orchestration without analysis business logic.
- Provide UI-facing status and error models.
- Configure service discovery or local static service endpoints.

Done criteria:

- Gateway starts independently.
- Frontend can communicate through Gateway.
- Gateway contains no AST, Joern, BTM, store, report or replay domain logic.
- Gateway calls other services only through defined APIs.

Verification: service-specific tests and applicable `QUALITY.md` gate.

## Slice 13 - Decouple Frontend

Purpose: align the React frontend with Gateway/API communication only.

Owner: Senior React Frontend Developer.

Reviews: Senior UX Designer, Senior Tester.

Allowed write scope:

- `forensic-ui/**` or `frontend/frontend-web-app/**` after target root is
  verified
- frontend API clients and tests
- frontend documentation

Tasks:

- Identify direct backend or worker-service coupling.
- Introduce or update API client structure through Gateway.
- Show analysis-job status, long-running operation state and service errors.
- Prepare replay and report flows.
- Add frontend tests.

Done criteria:

- Frontend does not call internal worker services directly.
- Gateway is the central UI API boundary.
- Long-running analysis jobs and errors are visible.

Verification:

```bash
npm test
npm run build
```

Run inside the verified frontend root.

## Slice 14 - Local Docker Compose Landscape

Purpose: make the service ecosystem startable locally.

Owner: Senior DevOps Engineer.

Reviews: Senior System Architect, Microservice Runtime Readiness Expert, Senior
Tester.

Allowed write scope:

- `deployment/docker-compose/docker-compose.yml`
- `deployment/docker-compose/.env.example`
- `docs/deployment/local-microservices.md`
- service-local Dockerfiles only when owned by this slice

Tasks:

- Define networks, ports, healthchecks, databases and minimal dependencies.
- Ensure containers do not share runtime code volumes.
- Document local start and stop flow.

Done criteria:

- All services can be started locally as containers.
- Every service has a healthcheck.
- Local environment is documented.

Verification:

```bash
docker compose -f deployment/docker-compose/docker-compose.yml config
docker compose -f deployment/docker-compose/docker-compose.yml build
docker compose -f deployment/docker-compose/docker-compose.yml up -d
docker compose -f deployment/docker-compose/docker-compose.yml ps
docker compose -f deployment/docker-compose/docker-compose.yml down
```

## Slice 15 - Prepare Docker Swarm And Kubernetes

Purpose: prepare cluster deployment without blocking local development.

Owner: Senior DevOps Engineer.

Reviews: Senior System Architect, Microservice Runtime Readiness Expert, Senior
Tester.

Allowed write scope:

- `deployment/docker-swarm/stack.yml`
- `deployment/kubernetes/**`
- `docs/deployment/swarm.md`
- `docs/deployment/kubernetes.md`

Tasks:

- Prepare Swarm stack and Kubernetes manifests or Helm structure.
- Document ConfigMap and secret strategy.
- Define readiness and liveness probes.
- Prepare worker-service scalability and resource limits.

Done criteria:

- Services are modeled as independent deployments.
- Worker services are horizontally scalable.
- Gateway and ingestion exposure is explicit.
- Internal services remain internal.

Verification:

```bash
docker compose -f deployment/docker-compose/docker-compose.yml config
```

When available:

```bash
kubectl apply --dry-run=client -f deployment/kubernetes/
```

## Slice 16 - Integration Tests And Contract Tests

Purpose: prove that the services cooperate through contracts.

Owner: Senior Tester.

Reviews: Senior gRPC/Proto Specialist, Senior Java Backend Developer, Senior
DevOps Engineer.

Allowed write scope:

- contract tests
- integration tests
- test fixtures
- test documentation
- build files required for test tasks

Tasks:

- Add REST and gRPC contract tests.
- Define test data for the analysis flow.
- Automate at least one end-to-end analysis flow.
- Test failure, timeout and retry behavior.
- If `integrationTest` does not exist, define and document the chosen test
  structure before requiring the command.

Done criteria:

- Contract breaks are detected.
- At least one complete analysis flow is automated.
- Services remain individually testable.

Verification:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Use the full `QUALITY.md` gate when production or build changes require it.

## Slice 17 - Complete Monolith Path Migration

Purpose: remove obsolete direct couplings only after service replacements are
verified.

Owner: Senior System Architect.

Reviews: Senior Java Backend Developer, Senior Tester.

Allowed write scope:

- obsolete module dependencies
- obsolete packages and entrypoints
- migration documentation
- build cleanup files

Tasks:

- Verify old module dependencies.
- Remove obsolete direct method calls across target service boundaries.
- Archive or update outdated documentation.
- Remove dead code only when replacement evidence exists.
- Ensure no shared runtime code library remains.

Done criteria:

- No direct runtime-code coupling remains between services.
- Documentation describes the new target state.
- Tests pass.

Verification: applicable `QUALITY.md` gate and diff inspection.

## Slice 18 - Architecture Readiness Review

Purpose: validate the resulting ecosystem against microservice guardrails.

Owner: Senior System Architect.

Reviews: Microservice Senior Expert, Senior DevOps Engineer, Senior Tester.

Allowed write scope:

- `docs/architecture/microservices-readiness-review.md`
- `docs/architecture/service-independence-checklist.md`
- `docs/architecture/architecture-decision-records/adr-microservices-ecosystem.md`
- arc42 and ADR synchronization files

Tasks:

- Verify independent build, start, test, container and deployment evidence per
  service.
- Verify no shared Java implementation modules exist.
- Verify communication only through allowed contracts.
- Verify data ownership and service-private persistence.
- Verify tests and deployment documentation.
- Verify `AGENTS.md`, `.agents/skills` and `.codex` consistency.

Done criteria:

- All microservice rules are reviewed.
- Blocking deviations are fixed.
- Non-blocking deviations have follow-ups.

Verification: applicable `QUALITY.md` gate and diff inspection.

## Slice 19 - Finalize Documentation

Purpose: document the ecosystem for development and operations.

Owner: Senior Documentation Engineer.

Reviews: Senior System Architect, Senior DevOps Engineer, Senior UX Designer,
Senior Tester.

Allowed write scope:

- `README.md`
- `docs/architecture/overview.md`
- `docs/deployment/local-microservices.md`
- `docs/testing/microservices-testing.md`
- `docs/contracts/contract-guidelines.md`
- service READMEs and diagrams

Tasks:

- Update overview, local start, deployment, testing, contract versioning and
  troubleshooting docs.
- Ensure new developers can start the local service landscape.
- Ensure service boundaries and contract ownership are understandable.

Done criteria:

- Development, operation, testing and contract workflows are documented.
- Documentation does not claim unverified runtime evidence.

Verification:

```bash
git diff --check
```

Run repository quality gates when docs change build, code or tests.

## Slice 20 - Final Review, Commit And Push

Purpose: close the workflow with review, quality evidence, commit and push.

Owner: Senior Swarm Orchestrator.

Reviews: Senior System Architect, Senior Tester, Git Commit Reviewer.

Allowed write scope:

- final workflow execution notes
- commit preparation artifacts when required by repository governance

Tasks:

- Inspect full diff and changed-file ownership.
- Run final architecture and microservice rule checks.
- Run final tests and quality gate.
- Verify documentation consistency.
- Prepare the commit message.
- Commit and push only after quality and commit-readiness review pass.

Required final commands:

```bash
git status --short
./gradlew clean test check --dependency-verification strict --console=plain --stacktrace
```

Use the full `QUALITY.md` gate when required:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

When Docker artifacts are part of the final result:

```bash
docker compose -f deployment/docker-compose/docker-compose.yml config
docker compose -f deployment/docker-compose/docker-compose.yml build
```

Commit subject:

```text
architecture: convert forensic analytics to microservices ecosystem
```

Push branch:

```bash
git push -u origin architecture/microservices-ecosystem-conversion-20260516
```

Done criteria:

- Final diff is reviewed.
- Required quality gates pass or blockers are documented.
- Commit is created only from reviewed files.
- Branch is pushed only after commit-readiness approval.

## Global Stop Rules

Stop and report when:

- an expected module, class, method, task, package, build file, endpoint, schema
  field, graph label or deployment file cannot be verified exactly;
- a provisional contract or event field cannot be made logically consistent from
  the documented service relationship and user-approved communication model;
- a change would introduce shared Java runtime code between services;
- service boundaries or data ownership are unclear;
- a slice would directly access another service's database;
- a service cannot be independently built, started, tested or containerized;
- Dockerization is not possible for a required service;
- `QUALITY.md` requires different commands than the slice plan;
- continuing would require guessing.

## Definition Of Done

The workflow is complete when:

- a dedicated branch exists;
- service decomposition is documented;
- every planned service has its own structure;
- every service is independently buildable, startable, testable and
  containerized;
- no shared runtime-code modules exist;
- REST, gRPC and event contracts are present;
- Gateway, ingestion, workers, store, graph/replay and reports are separated;
- local Docker Compose exists;
- Swarm and Kubernetes structure is prepared;
- tests and quality checks pass;
- documentation is updated;
- commit is created;
- branch is pushed.

## Optional Follow-Up Workflows

After this workflow completes, create separate workflows for:

- service mesh observability;
- message broker eventing;
- microservice security;
- scalable worker execution;
- LLM incident context pipeline.
