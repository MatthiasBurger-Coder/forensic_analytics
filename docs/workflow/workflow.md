# Workflow: Microservices BTM Pipeline

## Workflow Version

| Field | Value |
|---|---|
| Workflow version | `microservices-btm-pipeline-20260517-v5` |
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

## Workflow Governance Update v5

`microservices-btm-pipeline-20260517-v5` inserts a new prerequisite Slice 13
before the end-to-end repository-to-BTM orchestration slice. The new slice is
the Source-Fact Artifact Contract And Artifact IO Hardening slice. It was added
after the v4 Slice 13 read-only precheck found that Analysis Store would
otherwise need to infer the private Java AST artifact JSON document shape and
that Java AST and BTM artifact filesystem writes still needed no-follow
symlink hardening before a default end-to-end path can be called secure.

The completed Slice 00 through Slice 12 checkpoints from v1 through v4 remain
valid historical execution checkpoints. Downstream v5 slices are renumbered by
one: the former end-to-end orchestration slice becomes Slice 14, and the final
quality gate becomes Slice 20.

New v5 decisions:

- The Java AST source-fact artifact media type
  `application/vnd.forensic-analytics.java-ast-source-facts.v1+json` must have
  an explicit external JSON schema or equivalent contract documentation before
  Analysis Store parses source-fact bytes for target planning.
- Analysis Store must parse Java AST source-fact bytes only in a service-local
  adapter boundary and map them into Analysis Store-owned fact models. It must
  not import Java AST implementation classes or domain records.
- The existing `GetSourceFactArtifactBytes` owner RPC remains the byte
  retrieval contract; the v5 prerequisite must not add duplicate inline
  source-fact fields to the gRPC response unless a later contract review
  explicitly changes the bounded-byte design.
- Java AST and BTM artifact filesystem adapters must reject symlinked
  directory or file segments and use no-follow checks before artifact
  read/write verification.
- The default deterministic path must keep using fakes, in-process gRPC or
  local fixtures only. No default test may require external Git network access,
  Docker, Jenkins, Artifactory, credentials or host workspace mounts.

## Workflow Governance Update v4

`microservices-btm-pipeline-20260517-v4` inserts a new prerequisite Slice 12
before the end-to-end repository-to-BTM orchestration slice. The new slice is
the Source-Fact Byte Retrieval And Java AST Handoff Contract. It was added
after the v3 Slice 12 execution precheck found that the previous end-to-end
slice still required guessing how Analysis Store retrieves Java AST
source-fact bytes, how Repository Analysis exposes the Java AST handoff through
owner APIs and how a deterministic local E2E fixture path is proven without
external services.

The completed Slice 00 through Slice 11 checkpoints from v1, v2 and v3 remain
valid historical execution checkpoints. Downstream v4 slices are renumbered by
one: the former end-to-end orchestration slice becomes Slice 13, and the final
quality gate becomes Slice 19.

New v4 decisions:

- Java AST remains the byte owner for produced source-fact artifacts until an
  explicit handoff or object-store contract transfers custody.
- `ArtifactByteAccess.retrieval_contract` must name a real, verified owner API
  before Analysis Store can consume source-fact bytes for target planning.
- Repository Analysis must expose Java AST handoff completion through the
  documented gRPC handoff contract authority; Analysis Store must not import
  Repository Analysis implementation classes.
- The deterministic local E2E fixture path must use existing Gradle `test`
  tasks, generated service-local protobuf code, in-process gRPC, fakes or
  local fixtures only.
- No default test may require external Git network access, Docker, Jenkins,
  Artifactory, credentials or host workspace mounts.
- Missing build-output or Joern inputs remain explicit incomplete diagnostics
  until owner APIs provide available and complete package descriptors.

## Workflow Governance Update v3

`microservices-btm-pipeline-20260517-v3` inserts a new prerequisite Slice 11
before the end-to-end repository-to-BTM orchestration slice. Slice 11 is the
Repository-to-BTM orchestration contract and artifact-readiness bridge required
after the read-only Slice 11 execution review found that the previous
end-to-end slice would otherwise require Gateway worker orchestration,
cross-service implementation imports or unverified contract assumptions.

The completed Slice 00 through Slice 10 checkpoints from v2 remain valid
historical execution checkpoints. Downstream v3 slices are renumbered by one:
the former end-to-end orchestration slice becomes Slice 12, and the final
quality gate becomes Slice 18.

New v3 decisions:

- Gateway remains a public facade. It must not sequence Java AST, Joern, BTM
  Generation or Analysis Store worker internals directly.
- Analysis Store is the preferred owner for repository-to-BTM orchestration and
  worker-dispatch state unless the new Slice 11 records a stricter owner
  decision with architecture review.
- Gateway public API diagnostics must be allow-listed or redacted before any
  downstream message is returned to external clients.
- Public Gateway contracts must not present Repository Analysis private
  workspace identifiers or paths as the active repository-to-BTM API model.
- Java AST source-fact artifacts must carry valid `ArtifactByteAccess` before
  Analysis Store can accept them as input for target planning.
- Repository Analysis package descriptors may be unavailable or incomplete in
  deterministic local tests; Joern must then be represented as explicit
  incomplete diagnostics instead of being called with invalid package
  descriptors.
- The deterministic local end-to-end test path must use fakes, in-process
  gRPC or local fixtures and must not require external network services,
  Jenkins, Artifactory, Docker or credentials by default.

## Workflow Governance Update v2

`microservices-btm-pipeline-20260517-v2` inserts a new Slice 07 before the
previous Joern handoff slice. The new slice defines the repository source
snapshot, complete build-output package, artifact-byte access and Joern-owned
materialization contract required to continue without leaking Repository
Analysis private workspace identifiers or using shared filesystem coupling.

The completed Slice 00 through Slice 06 checkpoints from v1 remain valid
historical execution checkpoints. Downstream v2 slices are renumbered by one:
the former Joern Worker Handoff becomes Slice 08, and the final quality gate
becomes Slice 17.

New v2 decisions:

- Repository Analysis resolves branch or revision input to a concrete commit
  SHA before creating the analysis snapshot.
- The analysis snapshot includes a source package descriptor and an expected
  complete build-output package descriptor.
- Build-output package resolution is ordered:
  1. use a versioned Artifactory or Artifact Store artifact when one is
     configured and its manifest and checksums match;
  2. otherwise, use an optional Jenkins pipeline for the pinned source snapshot
     when configured;
  3. otherwise, use a future `build-artifact-worker-service` fallback that
     auto-detects the build system and produces the complete build-output
     package in a sandbox.
- Jenkins and Artifactory are optional external producers. They are not
  mandatory local quality gates and they do not become canonical evidence
  authorities.
- Analysis Store owns accepted metadata and must preserve `ArtifactByteAccess`;
  artifact byte custody remains with the producing service unless a later
  explicit handoff contract transfers custody.
- Joern materializes validated source/build package bytes into a Joern-owned
  workspace and mounts only that Joern-owned workspace into the Joern Docker
  runtime.

Execution release is conditional on committing this regenerated workflow
package so `workflow execute` can pass its clean-worktree preflight and read the
stable workflow version from `docs/workflow/workflow.history.md`.

## Three Amigos Findings

| Perspective | Finding |
|---|---|
| Senior Requirement Engineer | The request matches EPIC v0.2: producers trigger server-side analysis, Analytics owns canonical semantics, instrumentation planning and generated rule artifacts. |
| Senior System Architect | The accepted target landscape is already documented in ADR-0017. The workflow must preserve no shared Java implementation modules and must retire monolith modules only after service parity evidence. |
| Senior Java Backend Developer | Seven service slices already exist. The missing behavior is orchestration, build-output package ownership, artifact-byte ownership, BTM file delivery, and migration of remaining monolith implementation. |
| Senior React Frontend Developer | Frontend work is downstream of Gateway contract stabilization. The UI must call Gateway/public APIs only and must not call worker services directly. |
| Senior Tester | This is high-risk migration work. Every slice needs targeted service tests, contract tests, architecture tests and the applicable `QUALITY.md` gate before checkpoint commit or push. |

Question: Does the implementation still match the EPIC?

Current answer: partially. EPIC v0.2 and ADR-0001 require plugins to trigger
server-side analysis and receive server-generated BTM files when runtime
debugging requires instrumentation. The repository already has service slices
for Gateway submission, repository analysis and BTM generation, but the
end-to-end HTTP Gateway to gRPC BTM artifact path is not implemented yet.

## Verified Baseline

Read-only verification before workflow authoring found:

- Repository root: verified with `git rev-parse --show-toplevel`
- Branch created and verified: `feature/workflow-microservices-btm-pipeline-20260517`
- Working tree before workflow regeneration: clean
- Quality contract: `QUALITY.md`
- Java baseline: Java 25 through Gradle wrapper
- Full quality gate: `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`
- Current build shape: one Gradle multi-project build in `settings.gradle.kts`
- Existing service slices: `forensic-gateway-service`, `forensic-ingestion-service`, `repository-analysis-service`, `analysis-store-service`, `java-ast-analysis-service`, `joern-cpg-analysis-service`, `btm-generation-service`
- Planned service roots: `build-artifact-worker-service`, `graph-replay-service`, `report-generation-service`
- Existing contract roots: `contracts/grpc`, `contracts/openapi`, `contracts/events`
- Current frontend: `forensic-ui`

Verified service evidence:

- `repository-analysis-service` owns clean HTTPS repository checkout,
  workspace preparation, source snapshot IDs and gRPC port `9092`.
- `btm-generation-service` owns deterministic BTM generation, produces LF-only
  `.btm` files and exposes gRPC port `9095`.
- `analysis-store-service` owns the current service-local analysis job and
  artifact metadata subset.
- `forensic-gateway-service` owns the public HTTP shell and repository-analysis
  submission facade for external Git repository requests.
- Service projects do not declare `project(...)` dependencies on other Java
  modules.

Verified gaps:

- No implemented `build-artifact-worker-service` exists.
- No verified repository source package, complete build-output package or
  Joern materialization contract exists yet.
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
  build-artifact-worker-service/
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
  -> Build Artifact Worker Service or verified external artifact producer
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
- BTM Generation owns generated BTM bytes until an explicit byte-handoff,
  object-store ownership or delivery contract transfers byte custody.
  Analysis Store registration transfers accepted artifact metadata only.
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
Slice 14 proves the Gateway/public API path. The existing `forensic-ui` API
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
- repository-to-BTM orchestration owner API between Gateway and Analysis Store
  or another explicitly approved orchestration owner;
- public Gateway diagnostic allow-listing or redaction before downstream
  diagnostics cross the external API boundary;
- artifact metadata and byte ownership between BTM Generation and Analysis
  Store;
- Java AST source-fact artifact byte-access metadata, owner retrieval API and
  Repository Analysis handoff completion before Analysis Store consumption;
- repository source package, complete build-output package and Joern
  materialization contracts before Joern handoff implementation;
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
| Complete build-output packages | `build-artifact-worker-service` when introduced; optional Artifactory/Jenkins producers may provide bytes but do not own Analytics metadata |
| Java static source-fact worker output and source-fact artifact bytes | `java-ast-analysis-service` until accepted or transferred through an explicit byte-handoff contract |
| Joern semantic artifacts | `joern-cpg-analysis-service` until accepted |
| Canonical jobs, accepted artifact metadata and normalized facts | `analysis-store-service` |
| Generated BTM bytes until explicit byte handoff or delivery through an approved owner API | `btm-generation-service` |
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
- Source-package and build-output package materialization must verify manifests,
  checksums, byte limits and entry paths before extraction or Docker mounting.
- Jenkins and Artifactory integrations are optional and skipped by default in
  local quality gates; local tests must use fakes or deterministic fixtures.
- Fallback build execution must run in a sandboxed worker service with explicit
  build-system detection, quotas, network policy and secret isolation before it
  can be called runtime-ready.
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
| 07 | Repository snapshot and build artifact worker contract | Senior System Architect / Senior gRPC Proto Specialist | 03, 05, 06 |
| 08 | Joern CPG analysis handoff and artifact registration | Senior Joern CPG Specialist | 07 |
| 09 | Instrumentation target planning from accepted facts | Senior System Architect / Senior Java Backend | 06, 08 |
| 10 | BTM generation gRPC file delivery and artifact metadata registration | Senior gRPC Proto Specialist / Senior Java Backend | 03, 09 |
| 11 | Repository-to-BTM orchestration contract and artifact-readiness bridge | Senior System Architect / Contract-First API Steward | 02, 03, 05, 06, 07, 08, 09, 10 |
| 12 | Source-fact byte retrieval and Java AST handoff contract | Contract-First API Steward / Senior Java Backend | 11 |
| 13 | Source-fact artifact contract and artifact IO hardening | Contract-First API Steward / Security Sandbox Engineer | 12 |
| 14 | End-to-end repository-to-BTM orchestration | Senior Java Backend / Senior Swarm Orchestrator | 13 |
| 15 | Runtime readiness and local service landscape | Senior DevOps | 14 |
| 16 | Graph replay and report-generation service roots or explicit deferral | Senior System Architect | 14 |
| 17 | Frontend and CLI Gateway integration | Senior React Frontend / Senior Java Backend | 14 |
| 18 | Retire or isolate replaced monolith runtime paths | Senior System Architect / Senior Java Backend | 14, 15, 16, 17 |
| 19 | Remove obsolete shared implementation modules from Gradle registration | Senior System Architect / Senior DevOps | 18 |
| 20 | Full quality gate, evidence review and migration acceptance | Senior Tester / Quality Gate Orchestrator | 19 |

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
| 07 | `contracts/grpc/**`, `contracts/events/**`, `docs/architecture/**`, `docs/arc42/**`, Analysis Store/Repository Analysis/Joern contract tests and service mappings when required | Protobuf generation, contract tests, byte-access roundtrip tests, repository/analysis-store/Joern tests, security leakage checks | Record source package, build-output package, optional producer order, byte custody and Joern materialization boundaries |
| 08 | Joern handoff service files after Slice 07 contract precondition | Joern tests, workspace/artifact transfer tests, timeout/unavailable tests | Document Joern artifact transfer boundaries |
| 09 | Instrumentation target owner service and tests | Target-planning tests and evidence-integrity tests | Record target owner and non-evidence rule |
| 10 | BTM delivery contracts and service files | BTM generation tests, gRPC delivery tests, artifact determinism tests | Document BTM byte owner and delivery path |
| 11 | `contracts/**`, Gateway OpenAPI, Analysis Store orchestration contract/docs, Java AST byte-access bridge, architecture docs and focused readiness tests | Protobuf/OpenAPI contract checks, affected service tests for contract mappings, security leakage checks, deterministic local readiness test command | Record orchestration owner, public Gateway facade cleanup, byte-access bridge and explicit incomplete Joern behavior |
| 12 | `contracts/grpc/**`, Java AST, Repository Analysis and Analysis Store service-local clients/adapters/tests, deterministic fixture docs | Protobuf generation for affected services, service-local tests, fixture/readiness tests, leakage checks | Record Java AST byte owner API, Repository Analysis handoff contract and deterministic local E2E fixture command |
| 13 | Java AST artifact contract docs/schema, Java AST artifact adapter/tests, BTM artifact adapter/tests, Analysis Store parser planning docs/tests when added | Contract/schema checks, Java AST and BTM artifact filesystem tests, security leakage checks | Record source-fact artifact payload contract and no-follow artifact IO hardening |
| 14 | Gateway facade, Analysis Store job orchestration, worker clients/adapters | Deterministic end-to-end repository-to-BTM test command added and executed by this slice | Update runtime view and execution report |
| 15 | `deployment/**`, service config, Docker material | Service `bootJar`, healthcheck, Docker config checks that exist | Update deployment view |
| 16 | Graph/replay and report docs or service roots | Gate depends on deferral or implementation decision | Update arc42 runtime/deployment notes |
| 17 | `forensic-ui/**`, `frontend/**`, CLI files if migrated | `cd forensic-ui && npm ci && npm test && npm run build`; stop if `frontend/frontend-web-app` lacks package tooling | Update frontend README/API notes |
| 18 | Replaced `forensic-analytics-*` paths | Parity tests, caller-verification searches, rollback documentation | Document deprecation or isolation |
| 19 | `settings.gradle.kts`, obsolete modules | Full affected Gradle tests, dependency verification, architecture tests | Update current-state and migration map |
| 20 | Whole repository | Full `QUALITY.md` gate, leakage gates, diff checks | Final acceptance report and arc42/ADR sync |

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

### Slice 07 - Repository Snapshot And Build Artifact Worker Contract

Purpose: define and implement the contract path that pins repository input to a
deterministic source snapshot, declares the complete build-output package, and
allows Joern to materialize a service-owned workspace without receiving
Repository Analysis private workspace IDs.

Allowed write scope:

- `contracts/grpc/**`
- `contracts/events/**`
- `docs/architecture/**`
- `docs/arc42/**`
- `services/repository-analysis-service/**` for verified source/build package
  contract mapping
- `services/analysis-store-service/**` for `ArtifactByteAccess` preservation
  and validation
- `services/joern-cpg-analysis-service/**` for verified byte-access and
  materialization contract mapping
- `settings.gradle.kts` and `services/build-artifact-worker-service/**` only
  if this slice explicitly introduces the service shell and its contract
- focused tests

Required decisions:

- Branch and revision input are resolved to a concrete commit SHA before the
  analysis snapshot is created. A moving branch update creates a new source
  snapshot, not an in-place workspace update.
- The source snapshot declares source-package metadata and the expected
  complete build-output package metadata.
- Build-output package resolution order is Artifactory or Artifact Store first,
  optional Jenkins second, and `build-artifact-worker-service` fallback third.
- Jenkins and Artifactory are optional producers only. They are not local test
  dependencies and are not canonical evidence authorities.
- `build-artifact-worker-service`, when introduced, owns produced build-output
  bytes, manifests, checksums and retrieval references. Analysis Store owns
  accepted metadata only.
- Joern consumes validated source/build package bytes or owner API references,
  materializes them into a Joern-owned workspace ID, and mounts only that
  Joern-owned workspace into the Joern Docker runtime.

Stop if:

- any Repository Analysis private `workspace_id`, workspace path, `file:` URI,
  private object prefix or shared mount crosses into Joern;
- `ArtifactByteAccess` is missing, omitted or dropped by Analysis Store, Joern
  or a producer/consumer mapper;
- Artifactory or Jenkins credentials are required but not verified through a
  secret-governed path;
- checksum or manifest mismatch is treated as a fallback trigger instead of a
  terminal integrity failure;
- fallback build execution would require unbounded network access,
  user-supplied shell commands, host secret mounts, Docker socket access or
  privileged containers;
- build-system detection is ambiguous and the result would be guessed;
- archive extraction would allow absolute paths, Windows drives, UNC paths,
  traversal, symlinks, hardlinks, device files, sockets, FIFOs, duplicate
  normalized paths, excessive depth or quota overruns;
- diagnostics expose local paths, command arguments, URLs with coordinates,
  source content, secrets or raw stderr.

### Slice 08 - Joern Worker Handoff

Purpose: connect source snapshot output to Joern semantic analysis and register
semantic artifacts with explicit incompleteness when Joern is unavailable or
mapping is partial.

Allowed write scope:

- `services/joern-cpg-analysis-service/**`
- `services/repository-analysis-service/**`
- `services/analysis-store-service/**`
- `docker/joern/**` only when verified as service-contained runtime material
- tests

Contract precondition: Slice 07 must have defined and verified repository
source-package/build-output package transfer, artifact-byte access preservation
and Joern-local materialization. Passing Repository Analysis private workspace
IDs into Joern is forbidden.

Stop if Joern artifacts become shared filesystem coupling.

### Slice 09 - Instrumentation Target Planning

Purpose: create deterministic instrumentation targets from accepted static and
semantic facts through an explicitly owned service path.

Allowed write scope:

- service selected by Slice 03 ownership decision
- affected contracts and tests
- architecture docs if the owner decision changes

Stop if target selection would infer runtime execution or fabricate missing
facts.

### Slice 10 - BTM gRPC File Delivery

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

### Slice 11 - Repository-To-BTM Orchestration Contract And Artifact-Readiness Bridge

Purpose: close the contract, security and artifact-readiness gaps found during
the read-only end-to-end orchestration review before implementation resumes.
This slice defines the owner API for repository-to-BTM orchestration, keeps
Gateway facade-only, removes or clearly reclassifies public Gateway workspace
exposure, preserves Java AST artifact byte access and defines deterministic
local substitutes for unavailable Joern/build-artifact inputs.

Allowed write scope:

- `contracts/openapi/**`
- `contracts/grpc/**`
- `contracts/events/**` when orchestration events are selected
- `services/forensic-gateway-service/**` for public facade redaction,
  Gateway-to-owner client contracts and contract tests
- `services/analysis-store-service/**` for orchestration owner API, worker
  dispatch contract mappings, accepted metadata and tests
- `services/java-ast-analysis-service/**` for source-fact artifact
  `ArtifactByteAccess` preservation and tests
- `services/repository-analysis-service/**` only when deterministic local
  source/build package descriptor readiness or owner handoff metadata is
  implemented
- `docs/architecture/**`
- `docs/arc42/**`
- `docs/workflow/**`
- focused contract, architecture, security and readiness tests

Done criteria:

- Orchestration ownership is explicitly documented and contract-first. Analysis
  Store owns worker-dispatch/job-graph state unless this slice records another
  reviewed owner.
- Gateway public API no longer presents Repository Analysis private workspace
  identifiers or paths as the active repository-to-BTM model.
- Gateway diagnostics returned to public clients are allow-listed or redacted
  and tests prove that local paths, workspace IDs, tokens, raw command output
  and `file:` references do not leak.
- Java AST source-fact artifact output includes valid `ArtifactByteAccess`, and
  Analysis Store registration tests accept it.
- Repository Analysis either provides deterministic local `AVAILABLE` package
  descriptors for the test path or returns explicit unavailable/incomplete
  diagnostics that cause Joern to be skipped safely.
- Joern materialization consumes only `AVAILABLE`/`COMPLETE` package
  descriptors through owner APIs or deterministic fixtures; invalid package
  descriptors produce explicit incomplete diagnostics.
- BTM Generation receives only accepted artifact metadata and target-selection
  snapshots; BTM bytes remain owned by BTM Generation until delivery.
- The slice adds and executes an exact deterministic local readiness test
  command that requires no external network services, Jenkins, Artifactory,
  Docker or credentials by default.

Stop if the orchestration owner cannot be verified, if Gateway would sequence
worker business logic, if a public contract would expose private workspace
identifiers, if Java AST byte access cannot be represented without changing the
contract, or if the default readiness test requires external services.

### Slice 12 - Source-Fact Byte Retrieval And Java AST Handoff Contract

Purpose: define and verify the source-fact byte retrieval and Java AST handoff
preconditions that the blocked end-to-end orchestration review could not safely
guess. This slice closes the Java AST owner API for produced source-fact bytes,
the Repository Analysis to Java AST handoff signal and the deterministic local
repository-to-BTM fixture contract before the end-to-end orchestration slice
resumes.

Allowed write scope:

- `contracts/grpc/analysis-job.proto`
- `contracts/grpc/repository-analysis.proto`
- `contracts/grpc/java-ast-analysis.proto`
- affected service-local `build.gradle.kts` protobuf includes
- `services/repository-analysis-service/**`
- `services/java-ast-analysis-service/**`
- `services/analysis-store-service/**`
- `docs/workflow/fixtures/repository-to-btm-v1.md` when documenting the fixture
  shape
- service-local `src/test/resources/repository-to-btm/v1/**` files for the
  services that consume the fixtures
- `contracts/grpc/README.md`
- `docs/architecture/**`
- `docs/arc42/**`
- `docs/workflow/**`
- service-local tests

Done criteria:

- `ArtifactByteAccess.retrieval_contract` names a real, versioned and verified
  Java AST owner API for source-fact artifact bytes, or the slice records an
  explicit unavailable state that blocks downstream orchestration safely.
- Java AST source-fact bytes are retrievable from the owner service with
  checksum and size validation.
- Analysis Store consumes source-fact bytes only through the verified Java AST
  owner API using service-local generated client stubs; it does not import Java
  AST or Repository Analysis implementation classes and does not read private
  workspace paths.
- Repository Analysis exposes Java AST handoff completion through an approved
  gRPC service contract. If that contract is missing or insufficient, the slice
  stops for contract refinement instead of using an alternate path.
- Source-fact artifact metadata preserves byte access, checksum, size, schema
  version, producer identity and completeness from Repository Analysis through
  Java AST and Analysis Store acceptance.
- The versioned synthetic fixture shape is documented in
  `docs/workflow/fixtures/repository-to-btm-v1.md`, and concrete fixture files
  exist only as service-local `src/test/resources/repository-to-btm/v1/**`
  resources for the services that consume them. The fixtures cover Gateway
  submission, Analysis Store orchestration, Repository Analysis source snapshot,
  Java AST source-fact artifact metadata, Joern unavailable or available
  descriptors, target selection, BTM generation request, BTM manifest and
  delivery status.
- Service-local tests consume those fixtures through existing Gradle `test`
  tasks. The slice does not invent a new `integrationTest`, `e2e` or
  `contractTest` task unless the repository verifies and adds that task.
- The deterministic local readiness path uses fakes, in-process gRPC or local
  fixtures only; it does not require external Git network access, Docker,
  Jenkins, Artifactory, credentials or host workspace mounts by default.
- Missing build-output or Joern inputs remain explicit incomplete diagnostics
  instead of being silently converted to complete facts.

Required verification:

```bash
./gradlew :services:repository-analysis-service:generateProto :services:java-ast-analysis-service:generateProto :services:analysis-store-service:generateProto --dependency-verification strict --console=plain --stacktrace
./gradlew :services:repository-analysis-service:test :services:java-ast-analysis-service:test :services:analysis-store-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew test --dependency-verification strict --console=plain --stacktrace
git diff --check
```

Stop if the retrieval RPC name, fields, status model, chunking or byte limits
are unclear; if `ArtifactByteAccess` remains string-only metadata without a
verified owner API; if Gateway would sequence workers; if Analysis Store would
need direct filesystem access; if fixture data leaks local paths, source
content, credentials, private repository coordinates or host workspace details;
or if the default readiness command requires external services.

### Slice 13 - Source-Fact Artifact Contract And Artifact IO Hardening

Purpose: formalize the Java AST source-fact artifact payload contract and
harden Java AST and BTM artifact filesystem access before the end-to-end
repository-to-BTM orchestration slice consumes artifact bytes.

Allowed write scope:

- `contracts/grpc/java-ast-source-facts-v1.schema.json` or an equivalent
  precise contract document for
  `application/vnd.forensic-analytics.java-ast-source-facts.v1+json`
- `contracts/grpc/README.md`
- `docs/workflow/fixtures/repository-to-btm-v1.md`
- `services/java-ast-analysis-service/**`
- `services/analysis-store-service/**` for service-local parser or mapper work
  and tests only when needed by the contract
- `services/btm-generation-service/**`
- service-local `src/test/resources/repository-to-btm/v1/**` fixtures
- `docs/architecture/**`
- `docs/arc42/**`
- `docs/workflow/**`

Done criteria:

- The Java AST source-fact artifact media type has an explicit v1 JSON payload
  contract with required identity, summary, source-fact and diagnostic fields.
- Java AST writer tests prove that produced source-fact JSON is deterministic
  and contract-compliant.
- Analysis Store parses Java AST source-fact bytes only through a service-local
  adapter boundary and maps them to Analysis Store-owned accepted static facts,
  or the slice records a reviewed deferral that keeps Slice 14 blocked.
- Invalid schema fields, identity mismatches, unsafe source paths, unsupported
  fact types and unsupported evidence kinds fail closed with explicit
  diagnostics instead of invented facts.
- Java AST artifact reads/writes reject symlinked directories and files before
  byte access.
- BTM artifact writes and identical-existing-file verification reject
  symlinked directories and files before byte access.
- The existing `GetSourceFactArtifactBytes` owner RPC remains unchanged unless
  a contract review explicitly approves a protobuf change.
- Default tests require no external Git network, Docker, Jenkins, Artifactory,
  credentials or host workspace mounts.

Required verification:

```bash
./gradlew :services:java-ast-analysis-service:test --tests "*FileSystemAstResultArtifactWriterTest" :services:analysis-store-service:test :services:btm-generation-service:test --tests "*FileSystemBtmArtifactWriterTest" --dependency-verification strict --console=plain --stacktrace
./gradlew test --dependency-verification strict --console=plain --stacktrace
git diff --check
```

Stop if the artifact payload shape cannot be verified without guessing Java
AST private implementation records, if the fix would introduce shared Java DTO
or fixture modules, if the parser would read another service's private
filesystem, if no-follow symlink behavior cannot be tested on the local
filesystem, or if the default verification path requires external services.

### Slice 14 - End-To-End Repository To BTM Orchestration

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

### Slice 15 - Runtime Readiness And Local Service Landscape

Purpose: add verified local runtime material for the implemented service path.

Allowed write scope:

- `deployment/**`
- service Dockerfiles and READMEs
- service-local configuration
- DevOps docs and tests

Stop if Docker, Swarm or Kubernetes readiness is claimed without files and
commands.

### Slice 16 - Graph Replay And Report Service Decision

Purpose: decide whether graph-replay and report-generation services are
required for the BTM pipeline acceptance or remain explicitly deferred.

Allowed write scope:

- `docs/architecture/**`
- `docs/arc42/**`
- `services/graph-replay-service/**` and `services/report-generation-service/**`
  only if implementation is approved in this slice

Stop if projections become source of truth.

### Slice 17 - Frontend And CLI Gateway Integration

Purpose: route UI and CLI behavior through the Gateway/public API after Slice
14 proves the Gateway/public API path.

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

### Slice 18 - Retire Or Isolate Replaced Monolith Runtime Paths

Purpose: disable, isolate or retire old in-process paths only after replacement
service evidence exists.

Allowed write scope:

- affected `forensic-analytics-*` modules
- tests proving parity or explicit deprecation
- documentation explaining rollback

Stop if removing a module would break a still-used behavior.

### Slice 19 - Remove Obsolete Shared Implementation Modules

Purpose: remove Gradle registrations and source roots for implementation
modules that have verified service-owned replacements.

Allowed write scope:

- `settings.gradle.kts`
- obsolete module directories only when proven unused
- dependency and architecture tests

Stop if any caller cannot be verified.

### Slice 20 - Full Quality Gate And Migration Acceptance

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
- Joern handoff Slice 08 waits for Slice 07 because Joern must consume only
  source/build artifacts through verified owner APIs or materialized
  Joern-owned workspaces.
- Slice 12 waits for Slice 11 and proves source-fact byte retrieval, Java AST
  handoff closure and deterministic local fixture readiness before end-to-end
  orchestration starts.
- Slice 13 waits for Slice 12 and proves the source-fact artifact payload
  contract plus no-follow artifact IO hardening before end-to-end
  orchestration consumes produced bytes.
- Slice 14 waits for Slice 13 and proves the Gateway/public API path.
- Frontend work waits for Gateway API implementation from Slice 14.
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

This workflow is ready for release to `workflow execute` after the v5
workflow-governance package is committed and pushed.

`workflow execute` has already completed Slice 00 through Slice 12 across the
v1, v2, v3 and v4 workflow packages. After the v5 package is committed and
pushed, execution resumes at the new Slice 13, then continues one slice at a
time with a slice-scoped checkpoint commit and push after every successful
slice.
No direct implementation may start before the relevant service-boundary,
contract, data ownership and quality review for that slice has completed.
Because `workflow execute` requires a clean preflight, do not continue on a
dirty workflow-governance worktree; first commit and push the v5 workflow
package.

## arc42 Check Status

arc42 was checked during workflow creation and updated during the v2, v3, v4
and v5 workflow-governance updates:

- `docs/arc42/03-system-scope-and-context.md`
- `docs/arc42/04-solution-strategy.md`
- `docs/arc42/05-building-block-view.md`
- `docs/arc42/06-runtime-view.md`
- `docs/arc42/07-deployment-view.md`
- `docs/arc42/08-crosscutting-concepts.md`
- `docs/arc42/09-architecture-decisions.md`

Current arc42 records plugin producer boundaries, server-side BTM generation,
target microservice runtime flow, optional Artifact Store/Jenkins producer
boundaries, planned build-artifact worker ownership, Joern-owned materialized
workspaces, Gateway facade-only orchestration constraints, Java AST source-fact
byte retrieval constraints, the Java AST source-fact artifact payload contract
prerequisite, no-follow artifact IO hardening and governance process rules.
Later implementation slices must update arc42 when verified behavior changes.
