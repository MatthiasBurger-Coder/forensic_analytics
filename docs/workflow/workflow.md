# Workflow: FA-MSA-001 Microservice Decomposition

## Workflow Version

| Field | Value |
|---|---|
| Workflow version | `fa-msa-001-microservice-decomposition-20260521-v1` |
| Requirement ID | `FA-MSA-001` |
| Workflow branch | `architecture/workflow-microservice-decomposition-20260521` |
| Creation status | Created by `workflow create`; execution requires `workflow execute`. |
| Process strand | `workflow create` now; later `workflow execute` for slices. |
| Execution profile | `FULL_PATH` |

## Executive Summary

FA-MSA-001 asks for the current `forensic-analytics-*` modular monolith to be
dissolved into independently buildable, startable and containerized services.
The target forbids central shared Java modules such as
`forensic-analytics-domain`, `forensic-analytics-application`,
`forensic-analytics-persistence`, `forensic-analytics-logging`,
`forensic-analytics-bootstrap` and `forensic-analytics-boot-app`.

This workflow is intentionally staged. It does not allow a big-bang move or
deletion. Each service boundary must be contract-first, data-owner-first,
caller-proof and quality-gated before legacy code is removed.

## Requirement Clarification Decision

| Field | Decision |
|---|---|
| Original request | Create a workflow for fully dissolving the monolithic `forensic-analytics` structure into real services. |
| Interpreted intent | Produce an executable migration workflow that removes central shared Java modules only after service-local ownership, explicit contracts, runtime readiness, tests and caller-free evidence exist. |
| Change type | Microservice migration, Gradle restructuring, contract governance, data ownership, deployment readiness, CLI decoupling, testbed decoupling and architecture documentation synchronization. |
| Affected process strand | `workflow create` now; later `workflow execute`. |
| Affected architecture area | Service boundaries, shared-code removal, build graph, REST/gRPC contracts, persistence ownership, logging/observability, runtime bootstrap, Docker/Compose and architecture tests. |
| Explicit requirements | Create target roots under `services/`; remove central monolith modules; keep services independent; communicate only through REST, gRPC, messaging or file contracts; add per-service Dockerfiles and architecture tests; keep CLI as API client; keep testbed non-production. |
| Implicit requirements | Reconcile FA-MSA-001 service names with existing ADR/arc42 names; avoid big-bang deletion; preserve rollback until parity exists; preserve forensic evidence semantics; do not create shared Java DTO, domain, utility, repository or fixture modules. |
| Assumptions | FA-MSA-001 intentionally supersedes the previously documented service naming direction. Slice 01 must update or supersede the affected ADR/arc42 documents before production code migration. |
| Non-goals | No production source move during workflow creation; no module removal without caller-free evidence; no shared Java compatibility bridge; no generated code shared as a Gradle dependency; no Swarm or Kubernetes readiness claim before manifests and commands exist. |
| Risks | Existing ADR-0017 and arc42 use different service names; current monolith modules have active callers; existing services use partial target names; data ownership for `analysis-orchestrator-service` and `query-report-api-service` must be clarified before persistence work. |
| Open questions | Whether optional later services such as `btm-generation-service`, `graph-replay-service` and `incident-analysis-service` are included in final acceptance. This is non-blocking because FA-MSA-001 marks them optional. |
| Blocking questions | None for workflow creation. Product migration slices stop until S01 resolves naming/ADR drift and S04 resolves data ownership. |
| Confidence | 90 percent. |
| Decision | `READY_FOR_WORKFLOW`. |

## Verified Baseline

Read-only verification before workflow authoring found:

- Repository root: `/mnt/d/Projects/forensic_analytics`.
- WSL repository access is available from the Windows-hosted worktree.
- Workflow branch exists and is active:
  `architecture/workflow-microservice-decomposition-20260521`.
- Working tree before workflow regeneration was clean.
- Quality authority is `QUALITY.md`.
- Minimum quality command:
  `./gradlew test --dependency-verification strict --console=plain --stacktrace`.
- Full local quality gate:
  `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`.
- Root `checkPackageCoverage` exists in `build.gradle.kts`.
- `settings.gradle.kts` still registers the central monolith modules listed in
  FA-MSA-001 AC-1.
- Current service slices are registered under names such as
  `services:forensic-ingestion-service`,
  `services:repository-analysis-service`,
  `services:java-ast-analysis-service`,
  `services:joern-cpg-analysis-service`,
  `services:analysis-store-service`,
  `services:forensic-gateway-service` and
  `services:btm-generation-service`.
- Existing ADR-0017 and arc42 documents define a target service landscape that
  differs from FA-MSA-001 names.
- `docs/architecture/current-coupling-map.md`,
  `docs/architecture/service-migration-map.md`,
  `docs/architecture/monolith-runtime-isolation.md` and
  `docs/architecture/monolith-caller-retirement-plan.md` record active callers
  and explicitly block unproven monolith module removal.
- Existing contract files live under `contracts/grpc`, `contracts/openapi`,
  `contracts/events` and `contracts/cli`.
- Existing deployment material includes
  `deployment/docker-compose/repository-to-btm.local.yml`; Docker Swarm and
  Kubernetes roots contain documentation only.

## Target Picture

Mandatory FA-MSA-001 target:

```text
services/
  repository-source-service/
  ingestion-service/
  java-parser-analysis-service/
  joern-analysis-service/
  analysis-orchestrator-service/
  query-report-api-service/
  cli-client/
  observability-stack/
  testbed/
```

Each productive service owns service-local `domain`, `application`, `adapter`
and `bootstrap` code, its own build entry point, tests, Dockerfile and
documented start path. Contracts may be central only as external interface
contracts: `.proto`, OpenAPI, JSON Schema, event contracts or versioned
contract documentation. They must not become shared Java implementation
modules.

## Scope

In scope:

- ADR and arc42 reconciliation for FA-MSA-001 names and responsibilities.
- Current caller and dependency inventory.
- Contract-first REST, gRPC, messaging or file handoff design.
- Per-service data ownership and persistence split.
- Gradle service project registration and old module deregistration when safe.
- Service-local extraction for repository source, ingestion, JavaParser, Joern,
  orchestration, query/report API, CLI, observability and testbed.
- Service-local Dockerfiles and verified build/start documentation.
- Architecture tests that block shared implementation dependencies.
- Final removal of central shared modules only after caller-free proof and
  quality gates.

Out of scope:

- Creating shared Java domain, application, DTO, repository, fixture, logging or
  error-model modules for the services.
- Keeping old service names as compatibility aliases unless a later explicit
  ADR requests and tests a temporary migration bridge.
- Treating existing partial service slices as production-ready without runtime
  readiness evidence.
- Running external Docker, Swarm, Kubernetes, Joern, LLM, Jenkins or Artifactory
  infrastructure in the default unit test gate.
- Push, PR creation, PR merge, branch cleanup or `push auto`.

## Architecture Constraints

- Current modular-monolith modules are not microservices.
- Services must not depend on each other as Gradle projects.
- Services must not depend on central monolith modules after their migration
  slice is complete.
- Service communication must use REST/OpenAPI, gRPC/protobuf, messaging or
  documented file contracts.
- Domain and application packages must remain framework-free.
- Bootstraps may use Spring Boot only inside service-local bootstrap packages,
  as allowed by ADR-0019.
- `analysis-orchestrator-service` coordinates workflows only. It must not
  absorb repository checkout, JavaParser analysis, Joern execution, reporting or
  canonical persistence as hidden business logic.
- `query-report-api-service` may aggregate query/report output through owner
  APIs. It must not run analysis, checkout repositories, execute Joern or read
  private service databases.
- Observability must be service-local configuration or deployment material, not
  a shared Java logging module.
- Testbed may start and test services, but no production service may depend on
  testbed code.

## Role And Subagent Assignment

Callable subagents were not used during this `workflow create` turn because the
active user request did not explicitly ask for delegated or parallel agent
execution. Mandatory reviews are represented as local role-review checklists in
`docs/workflow/three-amigos-decision-record.md`.

During `workflow execute`, use callable subagents only when explicitly
authorized by the current runtime and user request. Otherwise use matching role
files as local review checklists and report the limitation.

Required role routing:

- Senior Requirement Engineer for requirement drift and EPIC traceability.
- Senior System Architect for ADR/arc42, service boundaries and retirement
  safety.
- Microservice Senior Expert for service autonomy and no shared Java modules.
- Senior Java Backend Developer for Java service implementation and tests.
- Senior gRPC/Proto Specialist and Contract-First API Steward for contracts.
- Data Ownership and Persistence Steward for one-writer data ownership.
- Senior DevOps Engineer for Gradle, Docker, Compose and deployment evidence.
- Senior Security/Sandbox Engineer for repository isolation and leakage risks.
- Senior Tester for regression strategy, ArchUnit, coverage and quality gates.
- Senior React Frontend Developer for N/A impact unless public APIs affect
  frontend code.
- Senior Documentation Engineer for workflow, ADR, arc42 and README alignment.

## Test Strategy

Default verification follows `QUALITY.md`:

1. Run the narrowest targeted test for the changed slice.
2. Run affected service or module tests.
3. Run the repository minimum command when production Java, tests, Gradle,
   contracts or runtime wiring changes:
   `./gradlew test --dependency-verification strict --console=plain --stacktrace`.
4. Run the full local quality gate before old module removal and final closure:
   `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`.
5. Always run `git diff --check`.

Microservice slices must add or update tests for service-local architecture,
forbidden shared dependencies, contract mapping and validation, deterministic
evidence handling, explicit missing-data representation and service startup
where runtime behavior changes.

## Resilience And Security Requirements

- Long-running analysis jobs expose explicit state, timeout and retry semantics.
- Repository checkout and Joern execution have bounded resource, timeout,
  cleanup and diagnostics behavior.
- Missing AST, Joern, runtime, graph or report evidence remains explicit.
- Rollback preserves the previous in-process path until replacement service
  parity or explicit deprecation exists.
- Optional external checks report `SKIPPED` with reason, never success.
- Diagnostics do not leak secrets, credentials, private workspace paths, raw
  source content, raw stderr or raw runtime values.

## Ordered Slices

### Slice 00 - Execution Preflight And Context Freeze

```yaml
slice_id: S00
profile: FULL_PATH
owner: senior-execution-orchestrator
secondary_reviewers:
  - senior-requirement-engineer
  - senior-system-architect
  - senior-tester
affected_files:
  - docs/workflow/execution-report.md
affected_modules: []
affected_contracts: []
dependencies: []
parallel_group: G00
file_locks:
  - docs/workflow/**
contract_locks: []
architecture_locks:
  - workflow-execute-preflight
quality_gates:
  targeted:
    - git diff --check
  required:
    - git status --short
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - active branch is not architecture/workflow-microservice-decomposition-20260521
  - context pack hash drift is not reviewed
  - working tree has unrelated changes
```

Purpose: verify branch, context-pack hashes, workflow files, quality commands
and current module registrations before execution.

### Slice 01 - ADR And arc42 Target Landscape Reconciliation

```yaml
slice_id: S01
profile: FULL_PATH
owner: senior-system-architect
secondary_reviewers:
  - senior-requirement-engineer
  - senior-documentation-engineer
  - microservice-senior-expert
affected_files:
  - docs/adr/**
  - docs/arc42/**
  - docs/architecture/**
  - services/README.md
affected_modules: []
affected_contracts: []
dependencies:
  - S00
parallel_group: G01
file_locks:
  - docs/adr/**
  - docs/arc42/**
  - docs/architecture/**
  - services/README.md
contract_locks: []
architecture_locks:
  - target-service-landscape
  - no-shared-java-modules
quality_gates:
  targeted:
    - git diff --check
  required: []
documentation:
  arc42: update target service landscape and mark older names historical or superseded
  adr: add or update architecture decision for FA-MSA-001 target names
stop_conditions:
  - FA-MSA-001 names cannot be reconciled with accepted ADRs
  - current service slices are described as production-ready without evidence
  - optional services are made mandatory without a requirement update
```

Purpose: align architecture source-of-truth documents before code or Gradle
migration begins.

### Slice 02 - Caller And Coupling Inventory Gate

```yaml
slice_id: S02
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - senior-system-architect
  - microservice-senior-expert
  - senior-tester
affected_files:
  - docs/architecture/current-coupling-map.md
  - docs/architecture/service-migration-map.md
  - docs/architecture/monolith-caller-retirement-plan.md
  - docs/workflow/execution-report.md
affected_modules:
  - forensic-analytics-*
  - services:*
affected_contracts: []
dependencies:
  - S01
parallel_group: G02
file_locks:
  - docs/architecture/current-coupling-map.md
  - docs/architecture/service-migration-map.md
  - docs/architecture/monolith-caller-retirement-plan.md
contract_locks: []
architecture_locks:
  - legacy-caller-inventory
quality_gates:
  targeted:
    - rg -n "project\\(\":forensic-analytics-" -g "build.gradle.kts"
    - git diff --check
  required: []
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - legacy module removal is proposed without caller-free evidence
  - inventory commands are incomplete or cannot be reproduced
  - testbed caller coverage would be deleted before replacement tests exist
```

Purpose: freeze exact callers, dependencies and current coupling before any
module move or removal.

### Slice 03 - Contract-First Communication Baseline

```yaml
slice_id: S03
profile: FULL_PATH
owner: contract-first-api-steward
secondary_reviewers:
  - senior-grpc-proto-specialist
  - senior-java-backend
  - senior-tester
affected_files:
  - contracts/**
  - docs/architecture/service-communication-matrix.md
  - docs/architecture/contract-versioning.md
affected_modules: []
affected_contracts:
  - contracts/grpc/**
  - contracts/openapi/**
  - contracts/events/**
dependencies:
  - S01
  - S02
parallel_group: G03
file_locks:
  - contracts/**
  - docs/architecture/service-communication-matrix.md
  - docs/architecture/contract-versioning.md
contract_locks:
  - repository-source-service
  - ingestion-service
  - java-parser-analysis-service
  - joern-analysis-service
  - analysis-orchestrator-service
  - query-report-api-service
architecture_locks:
  - contract-first-service-communication
quality_gates:
  targeted:
    - git diff --check
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - service communication lacks an explicit contract
  - generated Java transport types become shared Gradle dependencies
  - request, response, error, retry or idempotency semantics would need guessing
```

Purpose: define or reconcile external contracts before service code depends on
cross-service communication.

### Slice 04 - Data Ownership And Persistence Split

```yaml
slice_id: S04
profile: FULL_PATH
owner: data-ownership-persistence-steward
secondary_reviewers:
  - senior-analysis-storage-architect
  - senior-system-architect
  - senior-security-sandbox-engineer
  - senior-tester
affected_files:
  - docs/architecture/contract-versioning.md
  - docs/architecture/data-ownership.md
  - docs/architecture/service-boundaries.md
  - docs/architecture/service-communication-matrix.md
  - docs/architecture/service-migration-map.md
  - docs/architecture/target-microservices-architecture.md
  - docs/workflow/execution-report.md
  - services/analysis-store-service/README.md
  - services/btm-generation-service/README.md
  - services/forensic-gateway-service/README.md
  - services/forensic-ingestion-service/README.md
  - services/graph-replay-service/README.md
  - services/java-ast-analysis-service/README.md
  - services/joern-cpg-analysis-service/README.md
  - services/report-generation-service/README.md
affected_modules:
  - forensic-analytics-persistence
  - services:*
affected_contracts:
  - contracts/**
dependencies:
  - S01
  - S02
  - S03
parallel_group: G03
file_locks:
  - docs/architecture/contract-versioning.md
  - docs/architecture/data-ownership.md
  - docs/architecture/service-boundaries.md
  - docs/architecture/service-communication-matrix.md
  - docs/architecture/service-migration-map.md
  - docs/architecture/target-microservices-architecture.md
  - docs/workflow/execution-report.md
  - services/analysis-store-service/README.md
  - services/btm-generation-service/README.md
  - services/forensic-gateway-service/README.md
  - services/forensic-ingestion-service/README.md
  - services/graph-replay-service/README.md
  - services/java-ast-analysis-service/README.md
  - services/joern-cpg-analysis-service/README.md
  - services/report-generation-service/README.md
contract_locks:
  - persistence-ownership
architecture_locks:
  - one-writer-per-data-type
quality_gates:
  targeted:
    - git diff --check
  required: []
documentation:
  arc42: update if persistence ownership changes architecture view
  adr: add ADR if long-lived storage ownership changes
stop_conditions:
  - more than one service writes the same canonical data
  - direct cross-service database or table access is planned
  - table, topic, bucket, schema or graph label names would need guessing
```

Purpose: make persistence ownership explicit before removing
`forensic-analytics-persistence` or rehoming canonical state. S04 includes
documentation consistency updates needed to remove stale S04 owner placeholders
from contract governance notes and current/predecessor service READMEs.

### Slice 05 - Repository Source Service Extraction

```yaml
slice_id: S05
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - microservice-senior-expert
  - senior-security-sandbox-engineer
  - senior-devops
  - senior-tester
affected_files:
  - services/repository-source-service/**
  - forensic-analytics-adapter-repository-source/**
  - settings.gradle.kts
  - contracts/**
  - docs/arc42/**
  - docs/architecture/**
  - docs/workflow/execution-report.md
affected_modules:
  - services:repository-source-service
  - forensic-analytics-adapter-repository-source
affected_contracts:
  - repository-source-service contracts
dependencies:
  - S03
  - S04
parallel_group: G04
file_locks:
  - services/repository-source-service/**
  - forensic-analytics-adapter-repository-source/**
  - settings.gradle.kts
  - docs/arc42/**
  - docs/architecture/**
  - docs/workflow/execution-report.md
contract_locks:
  - repository-source-service
architecture_locks:
  - repository-workspace-ownership
  - no-private-workspace-crossing
quality_gates:
  targeted:
    - ./gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace
    - git diff --check
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: update repository-source service runtime/build view
  adr: checked
stop_conditions:
  - repository workspace paths cross service boundaries
  - Git checkout executes repository code, hooks or build scripts without approval
  - service depends on monolith domain/application/persistence/logging modules after extraction
```

Purpose: move repository access, branch resolution, checkout/fetch and source
snapshot preparation into `services/repository-source-service`.

### Slice 06 - Ingestion Service Extraction

```yaml
slice_id: S06
profile: FULL_PATH
owner: senior-grpc-proto-specialist
secondary_reviewers:
  - senior-java-backend
  - contract-first-api-steward
  - microservice-senior-expert
  - senior-tester
affected_files:
  - services/ingestion-service/**
  - forensic-analytics-ingestion-grpc/**
  - forensic-analytics-ingestion-request/**
  - settings.gradle.kts
  - contracts/grpc/**
  - docs/arc42/**
  - docs/architecture/**
  - docs/workflow/execution-report.md
affected_modules:
  - services:ingestion-service
  - forensic-analytics-ingestion-grpc
  - forensic-analytics-ingestion-request
affected_contracts:
  - ingestion-service gRPC contracts
dependencies:
  - S03
  - S04
parallel_group: G04
file_locks:
  - services/ingestion-service/**
  - forensic-analytics-ingestion-grpc/**
  - forensic-analytics-ingestion-request/**
  - settings.gradle.kts
  - contracts/grpc/**
  - docs/arc42/**
  - docs/architecture/**
  - docs/workflow/execution-report.md
contract_locks:
  - ingestion-service
architecture_locks:
  - raw-ingestion-ownership
quality_gates:
  targeted:
    - ./gradlew :services:ingestion-service:test --dependency-verification strict --console=plain --stacktrace
    - git diff --check
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: update ingestion service boundary
  adr: checked
stop_conditions:
  - generated protobuf Java becomes a shared DTO module
  - ingestion writes canonical analysis facts directly without approved owner
  - missing runtime or request fields are silently invented
```

Purpose: move gRPC intake, request validation and ingestion request handling
into `services/ingestion-service`.

### Slice 07 - JavaParser Analysis Service Extraction

```yaml
slice_id: S07
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - source-analysis-pipeline
  - microservice-senior-expert
  - senior-tester
affected_files:
  - services/java-parser-analysis-service/**
  - forensic-analytics-adapter-javaparser/**
  - settings.gradle.kts
  - contracts/grpc/**
  - contracts/events/**
  - docs/arc42/**
  - docs/architecture/**
  - docs/workflow/execution-report.md
  - services/README.md
affected_modules:
  - services:java-parser-analysis-service
  - forensic-analytics-adapter-javaparser
affected_contracts:
  - java-parser-analysis-service contracts
dependencies:
  - S03
  - S04
parallel_group: G05
file_locks:
  - services/java-parser-analysis-service/**
  - forensic-analytics-adapter-javaparser/**
  - settings.gradle.kts
  - contracts/grpc/**
  - contracts/events/**
  - docs/arc42/**
  - docs/architecture/**
  - docs/workflow/execution-report.md
  - services/README.md
contract_locks:
  - java-parser-analysis-service
architecture_locks:
  - static-source-facts
  - unresolved-symbol-diagnostics
quality_gates:
  targeted:
    - ./gradlew :services:java-parser-analysis-service:test --dependency-verification strict --console=plain --stacktrace
    - git diff --check
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: update JavaParser service boundary
  adr: checked
stop_conditions:
  - static reachability is treated as runtime execution
  - unresolved symbols are dropped silently
  - JavaParser APIs leak into domain, application or service-neutral contracts
```

Purpose: move JavaParser AST scanning and static source-fact extraction into
`services/java-parser-analysis-service`.

`services/java-ast-analysis-service` remains predecessor and rollback
evidence during S07 unless a later scope update explicitly adds it as writable
scope. S07 must create and register the target service and must not substitute
`:services:java-ast-analysis-service:test` for the required
`:services:java-parser-analysis-service:test` quality gate.

### Slice 08 - Joern Analysis Service Extraction

```yaml
slice_id: S08
profile: FULL_PATH
owner: senior-joern-cpg-specialist
secondary_reviewers:
  - senior-java-backend
  - microservice-senior-expert
  - senior-devops
  - senior-tester
affected_files:
  - services/joern-analysis-service/**
  - forensic-analytics-adapter-joern-docker/**
  - settings.gradle.kts
  - contracts/grpc/joern-cpg-analysis.proto
  - contracts/grpc/README.md
  - contracts/events/analysis-events.md
  - docker/joern/**
  - docs/arc42/**
  - docs/architecture/**
  - docs/workflow/execution-report.md
  - services/README.md
affected_modules:
  - services:joern-analysis-service
  - forensic-analytics-adapter-joern-docker
affected_contracts:
  - contracts/grpc/joern-cpg-analysis.proto
  - contracts/events/analysis-events.md
dependencies:
  - S03
  - S04
parallel_group: G05
file_locks:
  - services/joern-analysis-service/**
  - forensic-analytics-adapter-joern-docker/**
  - settings.gradle.kts
  - contracts/grpc/joern-cpg-analysis.proto
  - contracts/grpc/README.md
  - contracts/events/analysis-events.md
  - docker/joern/**
  - docs/arc42/**
  - docs/architecture/**
  - docs/workflow/execution-report.md
  - services/README.md
contract_locks:
  - joern-analysis-service
  - contracts/grpc/joern-cpg-analysis.proto
  - contracts/events/analysis-events.md
architecture_locks:
  - joern-runtime-isolation
  - joern-semantic-artifact-ownership
  - static-semantic-evidence-boundary
quality_gates:
  targeted:
    - ./gradlew :services:joern-analysis-service:test --dependency-verification strict --console=plain --stacktrace
    - git diff --check
    - docker compose -f docker/joern/docker-compose.joern.yml config when docker/joern/** changes; otherwise record SKIPPED
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: update Joern service boundary and Docker evidence
  adr: checked
stop_conditions:
  - Joern container receives another service's private workspace path
  - CPG/CFG/DFG facts are presented as runtime trace facts
  - Joern unavailability, timeout or incomplete mapping is hidden
```

Purpose: move Joern Docker control and semantic graph fact production into
`services/joern-analysis-service`.

`services/joern-cpg-analysis-service` remains predecessor and rollback
evidence during S08 unless a later scope update explicitly adds it as writable
scope. S08 must create and register the target service and must not substitute
`:services:joern-cpg-analysis-service:test` for the required
`:services:joern-analysis-service:test` quality gate.

### Slice 09 - Analysis Orchestrator Service Boundary

```yaml
slice_id: S09
profile: FULL_PATH
owner: senior-system-architect
secondary_reviewers:
  - senior-java-backend
  - data-ownership-persistence-steward
  - microservice-senior-expert
  - senior-tester
affected_files:
  - services/analysis-orchestrator-service/**
  - forensic-analytics-engine/**
  - forensic-analytics-application/**
  - settings.gradle.kts
  - contracts/**
affected_modules:
  - services:analysis-orchestrator-service
  - forensic-analytics-engine
  - forensic-analytics-application
affected_contracts:
  - analysis orchestration contracts
dependencies:
  - S03
  - S04
  - S05
  - S06
  - S07
  - S08
parallel_group: G06
file_locks:
  - services/analysis-orchestrator-service/**
  - forensic-analytics-engine/**
  - forensic-analytics-application/**
  - settings.gradle.kts
contract_locks:
  - analysis-orchestrator-service
architecture_locks:
  - orchestration-without-hidden-monolith
quality_gates:
  targeted:
    - ./gradlew :services:analysis-orchestrator-service:test --dependency-verification strict --console=plain --stacktrace
    - git diff --check
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: update orchestrator responsibility and non-scope
  adr: add ADR if orchestration ownership changes existing Analysis Store decision
stop_conditions:
  - orchestrator embeds repository checkout, AST scanning, Joern execution, report generation or persistence internals
  - orchestrator reads private databases or private workspaces
  - orchestration state ownership is unclear
```

Purpose: create `analysis-orchestrator-service` as coordinator without turning
it into a new monolith.

### Slice 10 - Query Report API Service Boundary

```yaml
slice_id: S10
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - contract-first-api-steward
  - senior-react-frontend
  - microservice-senior-expert
  - senior-tester
affected_files:
  - services/query-report-api-service/**
  - forensic-analytics-rest/**
  - contracts/openapi/**
  - settings.gradle.kts
affected_modules:
  - services:query-report-api-service
  - forensic-analytics-rest
affected_contracts:
  - query-report REST/OpenAPI contracts
dependencies:
  - S03
  - S04
  - S09
parallel_group: G06
file_locks:
  - services/query-report-api-service/**
  - forensic-analytics-rest/**
  - contracts/openapi/**
  - settings.gradle.kts
contract_locks:
  - query-report-api-service
architecture_locks:
  - public-api-facade
quality_gates:
  targeted:
    - ./gradlew :services:query-report-api-service:test --dependency-verification strict --console=plain --stacktrace
    - git diff --check
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: update query/report API boundary
  adr: checked
stop_conditions:
  - API service performs analysis execution, checkout, JavaParser or Joern processing
  - API service reads private service databases instead of owner APIs
  - public responses leak internals or unverified hypotheses as evidence
```

Purpose: move REST/query/report facade behavior into
`services/query-report-api-service`.

### Slice 11 - CLI Client Decoupling

```yaml
slice_id: S11
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - contract-first-api-steward
  - senior-tester
  - senior-documentation-engineer
affected_files:
  - services/cli-client/**
  - forensic-analytics-cli/**
  - contracts/cli/**
  - settings.gradle.kts
affected_modules:
  - services:cli-client
  - forensic-analytics-cli
affected_contracts:
  - CLI public API contract
dependencies:
  - S03
  - S10
parallel_group: G07
file_locks:
  - services/cli-client/**
  - forensic-analytics-cli/**
  - contracts/cli/**
  - settings.gradle.kts
contract_locks:
  - cli-client
architecture_locks:
  - cli-no-business-logic
quality_gates:
  targeted:
    - ./gradlew :services:cli-client:test --dependency-verification strict --console=plain --stacktrace
    - git diff --check
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - CLI depends on service implementation classes or monolith domain/application modules after migration
  - CLI executes analysis, parser, Joern or persistence behavior directly
  - local legacy commands are removed without deprecation or parity tests
```

Purpose: move CLI behavior into `services/cli-client` as a public API client.

### Slice 12 - Observability Stack And Logging Decoupling

```yaml
slice_id: S12
profile: FULL_PATH
owner: senior-devops
secondary_reviewers:
  - observability-runtime-diagnostics
  - senior-system-architect
  - senior-security-sandbox-engineer
  - senior-tester
affected_files:
  - services/observability-stack/**
  - forensic-analytics-observability/**
  - forensic-analytics-logging/**
  - deployment/**
  - settings.gradle.kts
affected_modules:
  - services:observability-stack
  - forensic-analytics-observability
  - forensic-analytics-logging
affected_contracts: []
dependencies:
  - S01
  - S02
parallel_group: G07
file_locks:
  - services/observability-stack/**
  - forensic-analytics-observability/**
  - forensic-analytics-logging/**
  - deployment/**
  - settings.gradle.kts
contract_locks: []
architecture_locks:
  - service-local-observability
quality_gates:
  targeted:
    - git diff --check
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: update crosscutting observability concept
  adr: supersede or update ADR-0008 if shared logging module is removed
stop_conditions:
  - observability stack becomes a shared Java runtime library
  - correlation or trace context is logged as forensic evidence
  - secrets or private workspace paths can leak through diagnostics
```

Purpose: replace central logging/observability modules with service-local
configuration and deployment-oriented observability material.

### Slice 13 - Testbed Decoupling

```yaml
slice_id: S13
profile: FULL_PATH
owner: senior-tester
secondary_reviewers:
  - senior-devops
  - microservice-senior-expert
  - senior-java-backend
affected_files:
  - services/testbed/**
  - forensic-analytics-testbed/**
  - deployment/docker-compose/**
  - settings.gradle.kts
affected_modules:
  - services:testbed
  - forensic-analytics-testbed
affected_contracts: []
dependencies:
  - S05
  - S06
  - S07
  - S08
  - S09
  - S10
parallel_group: G08
file_locks:
  - services/testbed/**
  - forensic-analytics-testbed/**
  - deployment/docker-compose/**
  - settings.gradle.kts
contract_locks: []
architecture_locks:
  - testbed-no-production-dependency
quality_gates:
  targeted:
    - ./gradlew :services:testbed:test --dependency-verification strict --console=plain --stacktrace
    - git diff --check
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: update testbed and deployment test environment view
  adr: checked
stop_conditions:
  - production services depend on testbed source or fixtures
  - monolith regression coverage is removed before replacement service E2E exists
  - Docker/Compose commands are documented without verified files
```

Purpose: move system and integration test orchestration into a non-production
testbed root while preserving coverage.

### Slice 14 - Legacy Shared Module Retirement

```yaml
slice_id: S14
profile: FULL_PATH
owner: senior-system-architect
secondary_reviewers:
  - senior-java-backend
  - microservice-senior-expert
  - senior-tester
  - senior-devops
affected_files:
  - settings.gradle.kts
  - forensic-analytics-application/**
  - forensic-analytics-boot-app/**
  - forensic-analytics-bootstrap/**
  - forensic-analytics-domain/**
  - forensic-analytics-engine/**
  - forensic-analytics-logging/**
  - forensic-analytics-observability/**
  - forensic-analytics-persistence/**
  - forensic-analytics-rest/**
affected_modules:
  - forensic-analytics-application
  - forensic-analytics-boot-app
  - forensic-analytics-bootstrap
  - forensic-analytics-domain
  - forensic-analytics-engine
  - forensic-analytics-logging
  - forensic-analytics-observability
  - forensic-analytics-persistence
  - forensic-analytics-rest
affected_contracts: []
dependencies:
  - S05
  - S06
  - S07
  - S08
  - S09
  - S10
  - S11
  - S12
  - S13
parallel_group: G09
file_locks:
  - settings.gradle.kts
  - forensic-analytics-*/**
contract_locks: []
architecture_locks:
  - monolith-module-removal
  - no-shared-java-implementation
quality_gates:
  targeted:
    - rg -n "project\\(\":forensic-analytics-(domain|application|persistence|logging|bootstrap|boot-app|engine|rest|observability)\"" -g "build.gradle.kts"
    - git diff --check
  required:
    - ./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: update final building-block and deployment views
  adr: checked or superseded for retired modules
stop_conditions:
  - any old module still has production or test callers
  - removal would delete the only regression coverage for a behavior
  - rollback instructions are missing
  - shared Java module replacement is introduced
```

Purpose: remove central shared modules from the Gradle build and source tree
only after replacement service ownership and caller-free evidence are proven.

### Slice 15 - Runtime Readiness, Architecture Tests And Closure

```yaml
slice_id: S15
profile: FULL_PATH
owner: senior-tester
secondary_reviewers:
  - senior-devops
  - microservice-runtime-readiness-expert
  - senior-system-architect
  - microservice-senior-expert
affected_files:
  - services/**
  - deployment/**
  - docs/architecture/**
  - docs/arc42/**
  - docs/workflow/execution-report.md
affected_modules:
  - services:repository-source-service
  - services:ingestion-service
  - services:java-parser-analysis-service
  - services:joern-analysis-service
  - services:analysis-orchestrator-service
  - services:query-report-api-service
  - services:cli-client
  - services:testbed
affected_contracts:
  - contracts/**
dependencies:
  - S14
parallel_group: G10
file_locks:
  - services/**
  - deployment/**
  - docs/architecture/**
  - docs/arc42/**
  - docs/workflow/execution-report.md
contract_locks:
  - all-service-contracts
architecture_locks:
  - final-microservice-readiness
quality_gates:
  targeted:
    - ./gradlew :services:repository-source-service:build --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :services:ingestion-service:build --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :services:java-parser-analysis-service:build --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :services:joern-analysis-service:build --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :services:analysis-orchestrator-service:build --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :services:query-report-api-service:build --dependency-verification strict --console=plain --stacktrace
    - git diff --check
  required:
    - ./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: final check required
  adr: final check required
stop_conditions:
  - any mandatory service cannot be built independently
  - any mandatory service lacks Dockerfile or documented start path
  - architecture tests do not guard domain/application/adapter boundaries
  - Docker Compose, Swarm or Kubernetes readiness is claimed without verified files and commands
```

Purpose: verify FA-MSA-001 acceptance criteria and close the workflow with
documented evidence.

## Dependency Graph

```text
S00
  -> S01
    -> S02
      -> S03
      -> S04
        -> S05
        -> S06
        -> S07
        -> S08
          -> S09
          -> S10
            -> S11
        -> S12
          -> S13
            -> S14
              -> S15
```

S05 through S08 have parallelization potential after S03 and S04, but execution
must remain one slice at a time unless the user explicitly authorizes parallel
subagent or worker execution with disjoint write scopes.

## Commit And Push Plan

No commit, push, PR creation, PR merge, branch cleanup or `push auto` occurs
during workflow creation.

During `workflow execute`, a slice may be committed only after required checks
pass, `git diff --check` passes, diff review confirms the slice write scope and
no stop condition remains open. Push requires explicit user approval. `push
auto` is forbidden for this product migration workflow.

## Definition Of Done

FA-MSA-001 is complete when:

1. The mandatory service roots exist under `services/`.
2. The old central monolith modules in AC-1 are no longer central Gradle
   subprojects.
3. Productive services do not depend on shared Java domain, application,
   persistence, logging, bootstrap, DTO, fixture, repository or error-model
   modules.
4. Services communicate only through REST, gRPC, messaging or documented file
   contracts.
5. Every productive service is independently buildable and startable.
6. Every productive service owns a Dockerfile.
7. Service-local architecture tests guard hexagonal boundaries and forbidden
   shared-module dependencies.
8. CLI is an API client with no business logic.
9. Testbed is non-production and no service depends on it.
10. Architecture, ADR and workflow execution documentation record final state
    and verification evidence.

## Handoff To Workflow Execute

Run `workflow execute` only from branch
`architecture/workflow-microservice-decomposition-20260521` with a clean working
tree. The first execution step must read this complete workflow, validate
`docs/workflow/context-pack.json`, and execute Slice 00 before touching product
files.

## arc42 Check Status

See `docs/workflow/arc42-check-status.md`. Workflow creation checked the arc42
baseline and found service-name drift against FA-MSA-001. Slice 01 must update
or supersede the affected architecture documents before production migration
slices run.
