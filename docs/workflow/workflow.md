# Workflow: Microservices BTM Pipeline

## Workflow Version

| Field | Value |
|---|---|
| Workflow version | `microservices-btm-pipeline-20260517-v1` |
| Workflow branch | `feature/workflow-microservices-btm-pipeline-20260517` |
| Creation status | Created by `workflow create`; execution requires a clean committed workflow package. |

## Executive Summary

This workflow converts the Forensic Analytics runtime toward the accepted
target microservices landscape and delivers the first end-to-end BTM pipeline:
a plugin or external client submits a Git repository through the public Gateway
HTTP API, the platform prepares a service-owned workspace from an external Git
repository, worker services produce accepted analysis artifacts, and the
server returns generated BTM files through a gRPC contract.

The workflow is intentionally slice-based. It must not perform a big-bang
module migration. Existing `forensic-analytics-*` modules remain the current
modular-monolith baseline until a replacement service path has verified
contract, runtime, test, healthcheck and rollback evidence. Only after that
evidence exists may obsolete modules be removed from `settings.gradle.kts`.

## Requirement Clarification Decision

| Field | Decision |
|---|---|
| Original request | Convert the project to microservices so collaboration happens only through services, distribute existing implementation modules into service-owned business boundaries, create BTM rules, create workspaces from external Git repositories, accept a plugin-submitted Git repository over HTTP and return completed BTM files over gRPC. |
| Interpreted intent | Create an executable workflow for completing the microservice migration, distributing existing implementation into service-owned boundaries, and implementing the Git repository to BTM artifact pipeline through HTTP plus gRPC. |
| Change type | Product architecture, backend, contracts, migration, DevOps and frontend integration workflow. |
| Affected process strand | `workflow create` now; later `workflow execute` for implementation slices. |
| Affected architecture area | Microservice autonomy, Gateway API, gRPC contracts, repository workspaces, static and semantic analysis workers, Analysis Store ownership, BTM generation, deployment and frontend/API adapters. |
| EPIC source | `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.2.md`. |
| Active branch | `feature/workflow-microservices-btm-pipeline-20260517`. |
| Confidence | 92 percent. |
| Decision | `READY_FOR_WORKFLOW`. |

No blocking requirement question remains for workflow creation. Contract and
runtime details that would otherwise require guessing are assigned to early
contract-first and architecture slices before implementation.

Execution release is conditional on committing this regenerated workflow
package so `workflow execute` can pass its clean-worktree preflight and read the
stable workflow version from `docs/workflow/workflow.history.md`.

## Three Amigos Findings

| Perspective | Finding |
|---|---|
| Senior Requirement Engineer | The request matches EPIC v0.2: producers trigger server-side analysis, Analytics owns canonical semantics, instrumentation planning and generated rule artifacts. |
| Senior System Architect | The accepted target landscape is already documented in ADR-0017. The workflow must preserve no shared Java implementation modules and must retire monolith modules only after service parity evidence. |
| Senior Java Backend Developer | Six service slices already exist. The missing behavior is orchestration, Gateway HTTP, artifact-byte ownership, BTM file delivery, and migration of remaining monolith implementation. |
| Senior React Frontend Developer | Frontend work is downstream of Gateway contract stabilization. The UI must call Gateway/public APIs only and must not call worker services directly. |
| Senior Tester | This is high-risk migration work. Every slice needs targeted service tests, contract tests, architecture tests and the applicable `QUALITY.md` gate before checkpoint commit or push. |

Question: Does the implementation still match the EPIC?

Current answer: partially. EPIC v0.2 and ADR-0001 require plugins to trigger
server-side analysis and receive server-generated BTM files when runtime
debugging requires instrumentation. The repository already has service slices
for repository analysis and BTM generation, but the end-to-end HTTP Gateway to
gRPC BTM artifact path is not implemented yet.

## Verified Baseline

Read-only verification before workflow authoring found:

- Repository root: verified with `git rev-parse --show-toplevel`
- Branch created and verified: `feature/workflow-microservices-btm-pipeline-20260517`
- Working tree before workflow regeneration: clean
- Quality contract: `QUALITY.md`
- Java baseline: Java 25 through Gradle wrapper
- Full quality gate: `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`
- Current build shape: one Gradle multi-project build in `settings.gradle.kts`
- Existing service slices: `forensic-ingestion-service`, `repository-analysis-service`, `analysis-store-service`, `java-ast-analysis-service`, `joern-cpg-analysis-service`, `btm-generation-service`
- Planned service roots: `forensic-gateway-service`, `graph-replay-service`, `report-generation-service`
- Existing contract roots: `contracts/grpc`, `contracts/openapi`, `contracts/events`
- Current frontend: `forensic-ui`

Verified service evidence:

- `repository-analysis-service` owns clean HTTPS repository checkout,
  workspace preparation, source snapshot IDs and gRPC port `9092`.
- `btm-generation-service` owns deterministic BTM generation, produces LF-only
  `.btm` files and exposes gRPC port `9095`.
- `analysis-store-service` owns the current service-local analysis job and
  artifact metadata subset.
- Service projects do not declare `project(...)` dependencies on other Java
  modules.

Verified gaps:

- No implemented `forensic-gateway-service` exists.
- No public HTTP Gateway endpoint exists for the new plugin repository request
  path as an implemented service.
- The current `btm-generation.proto` returns generated artifact references,
  not BTM file bytes.
- The worker chain from repository checkout to AST facts, Joern artifacts,
  instrumentation targets and BTM files is not implemented as an end-to-end
  service workflow.
- Graph replay and report generation remain planned service roots.
- Docker Compose, Docker Swarm and Kubernetes material for the full service
  landscape is not verified.
- Existing `forensic-analytics-*` modules still implement the current
  modular-monolith runtime and cannot be removed until service parity exists.

## Target Picture

The target repository shape for executable runtime behavior is:

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
```

`contracts/` remains contract-only. It may contain OpenAPI, `.proto` and event
documents, but it must not become a shared Java runtime module, DTO module,
domain module, mapper module, test fixture module or generated-code dependency
between services.

The target BTM pipeline is:

```text
Plugin / external client
  -> Gateway HTTP API
  -> Repository Analysis Service
  -> Java AST Analysis Service
  -> Joern CPG Analysis Service
  -> Analysis Store Service
  -> BTM Generation Service
  -> Gateway public gRPC BTM delivery facade
  -> Plugin receives generated BTM files
```

Every step must preserve provenance, correlation IDs, schema versions,
completeness and diagnostics. Static and semantic facts are not runtime
execution evidence. BTM files are generated instrumentation, not observed
runtime facts.

## Scope

Allowed write scope during later `workflow execute` slices:

- `docs/workflow/**`
- `docs/architecture/**`
- `docs/arc42/**`
- `docs/adr/**`
- `contracts/**`
- `services/**`
- `frontend/**`
- `forensic-ui/**` when the frontend is migrated or adapted to Gateway APIs
- `forensic-analytics-*` modules only when the slice explicitly migrates,
  isolates or retires verified behavior
- `settings.gradle.kts` and module `build.gradle.kts` files only when the
  slice changes verified service/module registration
- `deployment/**` when service runtime material is added
- focused tests for every changed slice

Read-only comparison scope:

- `AGENTS.md`
- `QUALITY.md`
- `docs/epics/**`
- `docs/architecture/**`
- `docs/arc42/**`
- `docs/adr/**`
- existing services, contracts, frontend and Gradle files

## Non-Goals

This workflow must not:

- implement all services in one slice;
- remove legacy modules before service parity evidence exists;
- introduce shared Java implementation modules between services;
- create DTO, domain, mapper, repository, fixture or error-model modules shared
  by services;
- let Gateway own AST, Joern, BTM, graph, replay, report or persistence
  business logic;
- let worker services read another service's private database, private
  filesystem paths or generated classes;
- expose repository workspace paths outside `repository-analysis-service`;
- let BTM generation scan repositories directly;
- treat static analysis, Joern output, generated rules or LLM output as
  observed runtime evidence;
- select a production database, graph database, vector database, LLM provider
  or broker without a dedicated slice and ADR review;
- claim Docker Swarm or Kubernetes readiness before manifests and verification
  commands exist.

## Architecture Boundaries

- Dependency direction remains adapter/infrastructure to application to domain.
- Service domain and application code must not import generated transport
  classes, Spring, gRPC, persistence clients, JavaParser, Joern, Docker APIs or
  other service implementation packages.
- Service-to-service communication is limited to REST/OpenAPI,
  gRPC/protobuf or approved message contracts.
- Generated Protobuf and OpenAPI code must be service-local build output.
- Analysis Store is the owner of canonical analysis state and accepted artifact
  metadata.
- Repository Analysis owns workspaces and source snapshot preparation.
- Java AST and Joern services own worker outputs until accepted by Analysis
  Store.
- BTM Generation owns generated BTM bytes until they are registered or
  delivered through an owner-approved artifact path.
- Frontend owns UI state only and must use Gateway/public APIs.
- Graph/replay and report services are projections or generated artifact
  owners, not primary evidence stores.

## Backend Assessment

Backend migration is high risk because current modules still share domain,
application, observability and persistence code. Each implementation slice must
either create service-owned behavior or retire an already-replaced monolith path.

Backend execution must preserve:

- Java 25;
- JUnit 6;
- service-local generated transport code;
- service-local domain models;
- explicit request validation;
- deterministic artifact generation;
- evidence completeness and diagnostics;
- ArchUnit checks for service boundaries.

## Frontend Assessment

Frontend implementation is not first in the dependency chain. It waits until
Slice 10 proves the Gateway/public API path. The existing `forensic-ui` API
adapter currently targets `/api`; later slices may either adapt it to the new
Gateway contract or migrate it into `frontend/frontend-web-app` after package
tooling exists in that target root.

Frontend views must distinguish accepted evidence, generated artifacts,
diagnostics, derived analysis, unresolved gaps, missing evidence, hypotheses
and suggested fixes. The UI must not call repository-analysis, AST, Joern, BTM,
analysis-store, graph or report worker services directly.

## Contract Strategy

Early slices must close the verified contract gaps before implementation:

- HTTP Gateway request for plugin/external Git repository submission;
- job/status model for repository-to-BTM generation;
- Gateway public gRPC delivery facade for completed BTM file bytes or chunks;
- owner API between Gateway and BTM Generation for byte retrieval without
  Gateway owning generated artifacts;
- artifact metadata and byte ownership between BTM Generation and Analysis
  Store;
- error/status model, idempotency, retries, deadlines and cancellation;
- required `Idempotency-Key` behavior for every Gateway mutation;
- compatibility of existing `forensic-ingestion.proto` and
  `btm-generation.proto` field numbers;
- contract tests for every implemented public operation.

No slice may implement a public route, RPC or event by guessing fields from
similarly named Java classes.

## Data Ownership

| Data area | Owner |
|---|---|
| Public request facade, status facade and public BTM delivery facade | `forensic-gateway-service` |
| Raw producer upload sessions | `forensic-ingestion-service` |
| Repository workspaces and source snapshots | `repository-analysis-service` |
| Java static source-fact worker output | `java-ast-analysis-service` until accepted |
| Joern semantic artifacts | `joern-cpg-analysis-service` until accepted |
| Canonical jobs, accepted artifact metadata and normalized facts | `analysis-store-service` |
| Generated BTM bytes before registration or delivery | `btm-generation-service` |
| Graph and replay projections | `graph-replay-service` |
| Reports and LLM-ready packages | `report-generation-service` |
| Browser state | `frontend-web-app` |

Direct cross-service database access, shared private filesystem paths and
shared Java persistence entities are forbidden.

Gateway is not the worker orchestrator. Job lifecycle, worker dispatch and
accepted artifact metadata belong to `analysis-store-service` unless Slice 01
records a stricter owner decision. Gateway may coordinate public request and
status views only through documented owner APIs.

## Resilience, Security And Evidence Requirements

- External Git repositories must be clean HTTPS URLs without userinfo, query
  secrets, fragments, local/private hosts, `file:` URLs, SSH or SCP-style
  remotes.
- Repository checkout must disable hooks, credentials, file protocol and
  repository-supplied execution.
- Workspace policies must keep timeout and byte quota bounds explicit.
- Gateway mutations must require idempotency keys.
- gRPC mutations must require request IDs, idempotency keys and correlation IDs.
- Long-running work must be represented as jobs, not as unbounded synchronous
  HTTP calls.
- BTM artifact transfer must support bounded payload or streaming behavior with
  explicit size limits.
- Missing analysis facts, missing Joern artifacts, partial BTM generation or
  incomplete runtime evidence must remain visible as incomplete, unknown,
  rejected or unavailable.
- Runtime values are sensitive by default. Logs and diagnostics must not expose
  secrets, raw source content, raw runtime values, local paths or LLM prompts.

## Test Strategy

Documentation-only workflow creation must run:

```bash
git status --short --branch
git diff --stat
git diff --name-status
git diff --check
```

Each implementation slice must run the narrowest meaningful checks first.
Expected slice gates include:

```bash
./gradlew :services:<service-name>:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:<service-name>:jacocoTestReport :services:<service-name>:jacocoTestCoverageVerification --dependency-verification strict --console=plain --stacktrace
./gradlew :services:<service-name>:bootJar --dependency-verification strict --console=plain --stacktrace
```

When shared contracts, Gradle registration or service interaction changes, run
the affected service tests and contract tests together. If no executable
contract-test command exists for a changed contract, the slice must add one or
stop before implementation. Before commit or push readiness, run the full local
gate from `QUALITY.md` when feasible:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Docker, Swarm and Kubernetes commands may be documented or run only after the
corresponding files exist and are verified in the slice.

Every successful `workflow execute` slice must create a CP_RECORD entry, stage
only current-slice files, run `git diff --cached --check`, create a
slice-scoped checkpoint commit and push the workflow branch to `origin` before
the next slice starts. This workflow does not use a final optional catch-all
commit slice.

## Slice Structure

| Slice | Purpose | Owner | Dependencies |
|---|---|---|---|
| 00 | Execution preflight, branch verification and baseline refresh | Senior Workflow Architect | none |
| 01 | Contract gap and service-boundary freeze for the BTM pipeline | Senior System Architect / Contract-First API Steward | 00 |
| 02 | Gateway HTTP and gRPC BTM delivery contracts | Senior gRPC Proto Specialist / Senior Java Backend | 01 |
| 03 | Analysis Store artifact-byte and instrumentation-target ownership contract | Senior Analysis Storage Architect | 02 |
| 04 | Gateway service bootstrap and public HTTP shell | Senior Java Backend / Senior DevOps | 02 |
| 05 | Gateway to Repository Analysis integration for external Git repos | Senior Git Workspace Specialist / Senior Java Backend | 04 |
| 06 | Source snapshot handoff to Java AST Analysis | Senior Java Backend | 05 |
| 07 | Joern CPG analysis handoff and artifact registration | Senior Joern CPG Specialist | 05 |
| 08 | Instrumentation target planning from accepted facts | Senior System Architect / Senior Java Backend | 06, 07 |
| 09 | BTM generation gRPC file delivery and artifact metadata registration | Senior gRPC Proto Specialist / Senior Java Backend | 03, 08 |
| 10 | End-to-end repository-to-BTM orchestration | Senior Java Backend / Senior Swarm Orchestrator | 04, 05, 06, 07, 08, 09 |
| 11 | Runtime readiness and local service landscape | Senior DevOps | 10 |
| 12 | Graph replay and report-generation service roots or explicit deferral | Senior System Architect | 10 |
| 13 | Frontend and CLI Gateway integration | Senior React Frontend / Senior Java Backend | 10 |
| 14 | Retire or isolate replaced monolith runtime paths | Senior System Architect / Senior Java Backend | 10, 11, 12, 13 |
| 15 | Remove obsolete shared implementation modules from Gradle registration | Senior System Architect / Senior DevOps | 14 |
| 16 | Full quality gate, evidence review and migration acceptance | Senior Tester / Quality Gate Orchestrator | 15 |

## Slice Execution Matrix

| Slice | Affected files/modules/contracts | Required quality gate | Documentation duty |
|---|---|---|---|
| 00 | `docs/workflow/**` execution report | Git status, diff stat, diff name-status, diff check | Record CP_RECORD baseline and workflow version |
| 01 | `docs/workflow/**`, `docs/architecture/**`, `docs/arc42/**`, `docs/adr/**` | Git diff checks and architecture review | Update service-boundary and ownership docs if decisions change |
| 02 | `contracts/openapi/**`, `contracts/grpc/**`, `contracts/events/**`, contract tests | Protobuf generation for changed `.proto`; Gateway OpenAPI contract tests or stop until added | Update contract-versioning and contract-test plan |
| 03 | `contracts/**`, `docs/architecture/data-ownership.md`, Analysis Store/BTM contract tests | Affected contract tests, Analysis Store/BTM service tests where code changes | Update data ownership and communication matrix |
| 04 | `services/forensic-gateway-service/**`, Gradle registration | Gateway service tests, ArchUnit boundary tests, `bootJar` | Add Gateway README and runtime notes |
| 05 | Gateway and Repository Analysis integration files | Gateway tests, Repository Analysis tests, safe Git workspace tests | Update workspace security notes if behavior changes |
| 06 | Java AST handoff service files after contract precondition | Java AST tests, Repository Analysis source-transfer tests, contract tests | Document source transfer ownership |
| 07 | Joern handoff service files after contract precondition | Joern tests, workspace/artifact transfer tests, timeout/unavailable tests | Document Joern artifact transfer boundaries |
| 08 | Instrumentation target owner service and tests | Target-planning tests and evidence-integrity tests | Record target owner and non-evidence rule |
| 09 | BTM delivery contracts and service files | BTM generation tests, gRPC delivery tests, artifact determinism tests | Document BTM byte owner and delivery path |
| 10 | Gateway facade, Analysis Store job orchestration, worker clients/adapters | Deterministic end-to-end repository-to-BTM test command added and executed by this slice | Update runtime view and execution report |
| 11 | `deployment/**`, service config, Docker material | Service `bootJar`, healthcheck, Docker config checks that exist | Update deployment view |
| 12 | Graph/replay and report docs or service roots | Gate depends on deferral or implementation decision | Update arc42 runtime/deployment notes |
| 13 | `forensic-ui/**`, `frontend/**`, CLI files if migrated | `cd forensic-ui && npm ci && npm test && npm run build`; stop if `frontend/frontend-web-app` lacks package tooling | Update frontend README/API notes |
| 14 | Replaced `forensic-analytics-*` paths | Parity tests, caller-verification searches, rollback documentation | Document deprecation or isolation |
| 15 | `settings.gradle.kts`, obsolete modules | Full affected Gradle tests, dependency verification, architecture tests | Update current-state and migration map |
| 16 | Whole repository | Full `QUALITY.md` gate, leakage gates, diff checks | Final acceptance report and arc42/ADR sync |

## Slice Details

### Slice 00 - Execution Preflight

Purpose: prove that `workflow execute` is running this workflow on
`feature/workflow-microservices-btm-pipeline-20260517` with a clean or
understood worktree.

Allowed write scope: `docs/workflow/**` execution evidence only.

Verification:

```bash
git rev-parse --show-toplevel
git branch --show-current
git status --short --branch
git diff --stat
git diff --name-status
```

Stop if the branch is not the workflow branch, if unrelated changes exist or
if `QUALITY.md` cannot be read. During workflow creation handoff, dirty
`docs/workflow/**` changes are understood only until they are committed as the
workflow-create package. `workflow execute` must start from a clean status.

### Slice 01 - Contract Gap And Service Boundary Freeze

Purpose: freeze the exact service owners, communication paths and non-goals for
the BTM pipeline before any production code is changed.

Allowed write scope:

- `docs/workflow/**`
- `docs/architecture/**`
- `docs/arc42/**`
- `docs/adr/**` if a new decision is required

Done criteria:

- Gateway, Repository Analysis, Java AST, Joern, Analysis Store and BTM
  Generation responsibilities are confirmed.
- Remaining monolith modules are mapped to target service owners.
- All implementation blockers are converted into later contract or migration
  slices.

### Slice 02 - Gateway HTTP And gRPC BTM Delivery Contracts

Purpose: define public HTTP submission and gRPC BTM file delivery before
implementation.

Allowed write scope:

- `contracts/openapi/**`
- `contracts/grpc/**`
- `contracts/events/**`
- contract tests or fixtures when the repository pattern exists
- documentation explaining compatibility and field numbering

Required decisions:

- HTTP operation for plugin/external repository submission.
- Job/status response for asynchronous BTM generation.
- gRPC method for completed BTM file transfer, including chunking or size
  limits.
- Error envelope, gRPC statuses, idempotency, timeout and cancellation behavior.
- Required `Idempotency-Key` semantics for every Gateway mutation, replacing
  optional mutation idempotency in the current OpenAPI component.

Stop if field numbers, response semantics or consumers are unclear.
Stop if no executable OpenAPI or protobuf contract verification exists for the
changed contract and the slice does not add one before implementation.

### Slice 03 - Artifact And Instrumentation Target Ownership Contract

Purpose: define how accepted fact artifacts become bounded instrumentation
targets and how BTM artifact bytes are registered or retrieved.

Allowed write scope:

- `contracts/grpc/**`
- `contracts/events/**`
- `docs/architecture/data-ownership.md`
- `docs/architecture/service-communication-matrix.md`
- focused contract tests

Stop if artifact byte ownership, canonical fact ownership or target-selection
ownership is unclear.

### Slice 04 - Gateway Service Bootstrap

Purpose: create the independent `forensic-gateway-service` runtime shell with
service-owned configuration, tests, health check and HTTP adapter boundary.

Allowed write scope:

- `services/forensic-gateway-service/**`
- `settings.gradle.kts`
- root/module Gradle files required for the new service
- service-local tests

Forbidden:

- worker logic in Gateway;
- direct Java dependencies on worker services;
- direct database access.

### Slice 05 - External Git Repository Workspace Flow

Purpose: implement Gateway HTTP request mapping to
`repository-analysis-service` so an external Git repository can create a
service-owned source snapshot.

Allowed write scope:

- `services/forensic-gateway-service/**`
- `services/repository-analysis-service/**` only for verified contract or
  validation gaps
- contract tests and integration tests

Stop if the flow exposes workspace paths, accepts unsafe remotes or requires
repository-supplied code execution.

### Slice 06 - Java AST Worker Handoff

Purpose: connect source snapshot output to Java AST analysis through bounded
source files or artifact references without sharing workspace internals.

Allowed write scope:

- `services/java-ast-analysis-service/**`
- `services/repository-analysis-service/**`
- `services/analysis-store-service/**`
- tests

Contract precondition: Repository to Java AST source transfer must already be
defined by Slice 02 or Slice 03. If a contract gap is discovered, stop and route
back to a contract-governance slice before production code changes.

Stop if static facts are treated as runtime execution or unresolved symbols are
silently dropped.

### Slice 07 - Joern Worker Handoff

Purpose: connect source snapshot output to Joern semantic analysis and register
semantic artifacts with explicit incompleteness when Joern is unavailable or
mapping is partial.

Allowed write scope:

- `services/joern-cpg-analysis-service/**`
- `services/repository-analysis-service/**`
- `services/analysis-store-service/**`
- `docker/joern/**` only when verified as service-contained runtime material
- tests

Contract precondition: Repository to Joern transfer must use artifact transfer,
source package transfer or another explicit owner API. Passing Repository
Analysis private workspace IDs into Joern is forbidden.

Stop if Joern artifacts become shared filesystem coupling.

### Slice 08 - Instrumentation Target Planning

Purpose: create deterministic instrumentation targets from accepted static and
semantic facts through an explicitly owned service path.

Allowed write scope:

- service selected by Slice 03 ownership decision
- affected contracts and tests
- architecture docs if the owner decision changes

Stop if target selection would infer runtime execution or fabricate missing
facts.

### Slice 09 - BTM gRPC File Delivery

Purpose: extend or implement BTM delivery so completed `.btm` bytes and
manifest bytes can be received over gRPC with bounded transfer semantics.

Allowed write scope:

- `contracts/grpc/**`
- `services/btm-generation-service/**`
- `services/analysis-store-service/**` when artifact metadata registration is
  changed
- focused gRPC and artifact determinism tests

Stop if BTM bytes cannot be traced to rule IDs, source fact references,
semantic artifact references and generation policy.

### Slice 10 - End-To-End Repository To BTM Orchestration

Purpose: connect the public Gateway facade, Analysis Store job lifecycle,
Repository Analysis, Java AST, Joern and BTM Generation into a tested local
end-to-end flow through owner APIs.

Allowed write scope:

- `services/forensic-gateway-service/**`
- `services/analysis-store-service/**` when job orchestration or worker dispatch
  is implemented there
- affected service-local clients/adapters
- integration tests

Done criteria:

- HTTP request accepts a clean HTTPS repository URL.
- Repository workspace and source snapshot are created.
- Static and semantic worker outputs are accepted or explicit incomplete
  diagnostics are returned.
- BTM files are generated deterministically.
- gRPC BTM file delivery returns the completed files or explicit unavailable
  state.
- Gateway remains a facade and does not own worker business logic.
- The slice adds and executes an exact deterministic local end-to-end test
  command. External network services are forbidden in the default test path;
  any optional external-service test must be separately named and skipped by
  default unless credentials and environment are documented.

### Slice 11 - Runtime Readiness And Local Service Landscape

Purpose: add verified local runtime material for the implemented service path.

Allowed write scope:

- `deployment/**`
- service Dockerfiles and READMEs
- service-local configuration
- DevOps docs and tests

Stop if Docker, Swarm or Kubernetes readiness is claimed without files and
commands.

### Slice 12 - Graph Replay And Report Service Decision

Purpose: decide whether graph-replay and report-generation services are
required for the BTM pipeline acceptance or remain explicitly deferred.

Allowed write scope:

- `docs/architecture/**`
- `docs/arc42/**`
- `services/graph-replay-service/**` and `services/report-generation-service/**`
  only if implementation is approved in this slice

Stop if projections become source of truth.

### Slice 13 - Frontend And CLI Gateway Integration

Purpose: route UI and CLI behavior through the Gateway/public API after Slice
10 proves the Gateway/public API path.

Allowed write scope:

- `forensic-ui/**`
- `frontend/**`
- `forensic-analytics-cli/**` only if CLI migration remains in repo scope
- tests and README updates

Verification:

```bash
cd forensic-ui && npm ci && npm test && npm run build
```

Stop if frontend or CLI calls internal worker services directly. Stop before
migrating into `frontend/frontend-web-app` unless that root has package tooling
and verified test/build commands.

Done criteria:

- Gateway requests include required idempotency and correlation metadata.
- UI states distinguish confirmed evidence, derived analysis, generated BTM
  artifacts, diagnostics, unresolved gaps, hypotheses and suggested fixes.
- Tests cover missing idempotency/correlation behavior in the frontend API
  adapter when the Gateway contract requires those headers.

### Slice 14 - Retire Or Isolate Replaced Monolith Runtime Paths

Purpose: disable, isolate or retire old in-process paths only after replacement
service evidence exists.

Allowed write scope:

- affected `forensic-analytics-*` modules
- tests proving parity or explicit deprecation
- documentation explaining rollback

Stop if removing a module would break a still-used behavior.

### Slice 15 - Remove Obsolete Shared Implementation Modules

Purpose: remove Gradle registrations and source roots for implementation
modules that have verified service-owned replacements.

Allowed write scope:

- `settings.gradle.kts`
- obsolete module directories only when proven unused
- dependency and architecture tests

Stop if any caller cannot be verified.

### Slice 16 - Full Quality Gate And Migration Acceptance

Purpose: run final repository validation and verify that the target path is
accurately documented.

Verification:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
git status --short --branch
git diff --stat
git diff --name-status
git diff --check
```

Optional external checks are reported as skipped unless credentials and
commands are documented.

## Parallelization

Default execution is serial until Slice 03 stabilizes contracts. After that:

- Gateway shell, BTM delivery tests and Analysis Store artifact metadata may be
  parallel only when contracts are frozen and write scopes are disjoint.
- Frontend work waits for Gateway API implementation.
- Deployment work waits for service bootJar and healthcheck evidence.
- Module removal waits for end-to-end parity evidence.

## Checkpoint And CP_RECORD Rules

Every successful implementation slice must perform the repository checkpoint
process from `docs/process/workflow-execute.md`.

Before `CP_COMMIT`, record a CP_RECORD entry in `docs/workflow/execution-report.md`
with:

```text
workflowVersion
sliceId
sliceTitle
responsibleAgent
changedFiles
qualityGateCommands
qualityGateResult
commitHash=pending
rollbackReference
arc42Updated
adrUpdated
```

After `CP_COMMIT` and `CP_PUSH`, update the CP_RECORD with the actual commit
hash and push result. If push fails, stop and route to `CP_ROLLBACK`; do not
force-push, run `push auto`, merge a PR or continue to the next slice.

## Subagent Assignment

`workflow execute` should route each slice through the configured subagent
workflow or matching role review:

- Workflow orchestration: Senior Workflow Architect / Workflow Executor
- Service boundaries: Senior System Architect / Microservice Senior Expert
- Contracts: Contract-First API Steward / Senior gRPC Proto Specialist
- Repository workspaces: Senior Git Workspace Specialist / Security Sandbox
  Engineer
- Java backend implementation: Senior Java Backend Developer
- Joern: Senior Joern CPG Specialist
- Persistence and artifacts: Senior Analysis Storage Architect
- Frontend: Senior React Frontend Developer / Senior UX Designer
- Runtime and deployment: Senior DevOps / Microservice Runtime Readiness Expert
- Testing and quality: Senior Tester / Quality Gate Orchestrator

Callable subagents may be used during `workflow execute` only when the runtime
and current user instruction authorize delegation. Otherwise use the matching
role files as explicit review checklists.

## Stop Conditions

Stop workflow execution when:

- the active branch is not `feature/workflow-microservices-btm-pipeline-20260517`;
- a required file, contract, Gradle task, class, method or field cannot be
  verified exactly;
- a slice would introduce shared Java implementation between services;
- a service would import another service's implementation classes;
- Gateway would own worker logic or read private service databases;
- repository workspace paths would cross service boundaries;
- BTM generation would scan repositories directly;
- static or semantic facts would be reported as runtime execution;
- generated BTM files would be untraceable to accepted facts and policy;
- quality commands cannot be verified from `QUALITY.md`;
- Docker, Swarm or Kubernetes readiness would be claimed without verified
  files and commands;
- continuing would require guessing ownership, API fields, schema fields,
  event names, deployment commands or rollback behavior.

## Definition Of Done

The workflow is done only when:

- public HTTP submission of an external Git repository is implemented through
  Gateway;
- service-owned repository workspace preparation is verified;
- static and semantic worker outputs are accepted or explicitly incomplete;
- deterministic BTM files are generated from accepted analysis facts or
  explicit target input;
- completed BTM files are retrievable through the approved gRPC contract;
- old modular-monolith implementation modules are removed or retained only as
  documented non-runtime compatibility paths with tests;
- no service depends on another service's Java implementation;
- frontend and CLI use Gateway/public APIs only where migrated;
- service runtime readiness evidence exists for implemented services;
- `QUALITY.md` gates have passed or blockers are reported with exact failures;
- arc42, ADRs, architecture docs and workflow docs match the implemented state.

## Handoff To Workflow Execute

This workflow is ready for release to `workflow execute` after the
workflow-create package is committed.

`workflow execute` must start at Slice 00, execute one slice at a time and
run a slice-scoped checkpoint commit and push after every successful slice.
No direct implementation may start before the relevant service-boundary,
contract, data ownership and quality review for that slice has completed.
Because `workflow execute` requires a clean preflight, do not run it on the
dirty workflow-create worktree; first commit the regenerated workflow package.

## arc42 Check Status

arc42 was checked during workflow creation:

- `docs/arc42/03-system-scope-and-context.md`
- `docs/arc42/04-solution-strategy.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/06-runtime-view.md`
- `docs/arc42/07-deployment-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/arc42/09-architecture-decisions.md`

Current arc42 already records plugin producer boundaries, server-side BTM
generation, target microservice runtime flow and governance process rules.
No arc42 production-claim correction is required during workflow creation.
Stale slice-number wording in arc42 and supporting architecture documents was
aligned where it described current or future migration sequencing.
Later implementation slices must update arc42 when verified behavior changes.
