# Workflow: FA-MVP-0001 Repository Workspace Checkout MVP

## Workflow Version

| Field | Value |
|---|---|
| Workflow version | `fa-mvp-0001-repository-workspace-checkout-h2-persistence-20260524-v1` |
| Requirement ID | `FA-MVP-0001` |
| Title | Repository Workspace Checkout MVP with H2 Persistence and Docker Volumes |
| Workflow branch | `feature/workflow-repository-workspace-checkout-h2-persistence-20260524` |
| Creation status | Created by `workflow create`; implementation requires `workflow execute`. |
| Process strand | `workflow create` now; later `workflow execute` for slices. |
| Execution profile | `FULL_PATH` |
| Primary owner | `repository-source-service` |
| Public API owner | `query-report-api-service` |
| Frontend owner | `forensic-ui` |

## Executive Summary

FA-MVP-0001 creates the first product-grade repository checkout workspace
foundation. The operator can enter a Git HTTPS repository URL in the GUI,
preview verified repository metadata, save or reuse a repository-level
workspace, create or reuse a branch-level workspace branch, checkout the
selected branch into a service-owned Docker volume, persist the resulting state
in service-local H2 storage and manually refresh a branch later.

This workflow is deliberately foundation-only. It does not run JavaParser,
Joern, BTM generation, replay, report generation, LLM context generation or any
repository build commands. Repository source checkout, branch state, source
snapshot references, private workspace paths and H2 data remain owned by
`repository-source-service`. `query-report-api-service` exposes only sanitized
public REST DTOs and must call the repository-source owner API. The browser
must never call Git remotes, internal services or gRPC directly.

Existing read-only verification found a partial baseline: `repository-source-service`
already owns `PrepareRepository`, `GetRepositoryPreparation` and
`CleanupRepositoryWorkspace` through `RepositorySourceGrpcEndpoint`; it uses
`GitRepositoryCheckoutAdapter`, `FileSystemRepositoryWorkspaceAdapter` and
`SourceRootDetector`, but currently wires `InMemoryRepositoryPreparationRepository`.
There is no verified `Workspace`, `WorkspaceBranch`, `RepositoryIdentity`, H2
dependency, durable idempotency repository, metadata preview route, branch
refresh use case, public `/workspaces` REST contract or UI workspace creation
flow yet.

## Target Picture

```text
Operator
  -> forensic-ui Create Workspace flow
  -> query-report-api-service public REST
  -> repository-source-service owner API
  -> repository-source-service domain/application use cases
  -> service-owned H2 file database
  -> service-owned repository workspace volume
```

Target repository hierarchy:

```text
Workspace
  owns one RepositoryIdentity
  contains many WorkspaceBranch records

WorkspaceBranch
  owns one checked-out branch state
  references current SourceSnapshot
  records requested and resolved commits
```

Allowed private path pattern:

```text
/var/lib/forensic-analytics/repository-workspaces/<workspaceId>/branches/<workspaceBranchId>/checkout
```

Public responses expose opaque IDs, repository metadata, branch names,
sanitized diagnostics, source roots and source snapshot IDs only. Public
responses must not expose local filesystem paths, raw Git output, credentials,
tokens, private network details or service-owned H2 paths.

## Verified Baseline

Read-only workflow creation verification found:

- Repository root: `/mnt/d/Projects/forensic_analytics`.
- WSL is available and repository commands must use the WSL-mounted worktree.
- Dedicated workflow branch is active:
  `feature/workflow-repository-workspace-checkout-h2-persistence-20260524`.
- The branch is based on `origin/main` and tracks `origin/main`.
- Working tree was clean before workflow regeneration.
- The previous `docs/workflow/**` files described `FA-MSA-001-LMR`; they are
  regenerated for this new workflow branch by the workflow-authoring rule.
- Quality authority is `QUALITY.md`.
- Minimum quality command:
  `./gradlew test --dependency-verification strict --console=plain --stacktrace`.
- Full local quality gate:
  `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`.
- `gradle/libs.versions.toml` does not contain H2.
- `gradle/verification-metadata.xml` does not contain `com.h2database:h2`.
- `services/repository-source-service/build.gradle.kts` has no H2 dependency.
- `RepositorySourceServiceConfiguration` currently wires
  `InMemoryRepositoryPreparationRepository`.
- `RepositorySourceDomain` contains `AnalysisRunId`, `SourceSnapshotId`,
  `WorkspaceId`, `RepositoryReference`, `RevisionSelector`, `WorkspacePolicy`,
  `CheckoutResult`, `SourceSnapshot` and `RepositoryPreparation`; it does not
  contain `Workspace`, `WorkspaceBranch`, `RepositoryIdentity` or `RepositoryKey`.
- `RepositorySourceApplicationService` currently exposes `prepare`, `get` and
  `cleanup`.
- Current repository-source ports are `RepositoryPreparationRepository`,
  `RepositoryWorkspacePort`, `RepositoryCheckoutPort` and `PreparedWorkspace`.
- `services/repository-source-service/src/main/resources/application-docker.properties`
  uses `/var/lib/forensic-analytics/repository-workspaces`.
- `services/repository-source-service/Dockerfile` creates only
  `/var/lib/forensic-analytics/repository-workspaces`.
- `deployment/docker-compose/repository-to-btm.local.yml` is transitional and
  currently includes `repository-analysis-service`, not `repository-source-service`.
- `query-report-api-service` currently exposes `/api/health`, `/api/status`,
  `POST /api/repository-analyses` and
  `GET /api/repository-analyses/{analysisRunId}`.
- `contracts/openapi/gateway-api.yaml` is the transitional public REST contract
  under `query-report-api-service` authority.
- `GatewayOpenApiContractTest` currently asserts that `/workspaces`,
  `workspaceId`, `workspaceName`, `WorkspaceList` and `resolvedRemoteUrl` are
  absent from the public contract.
- `forensic-ui` is a React/Vite app with verified scripts `npm run test` and
  `npm run build`.
- `forensic-ui` currently routes `/workspaces` to the repository-analysis create
  flow and has placeholder workspace models/list behavior.

## Requirement Clarification Decision

| Field | Decision |
|---|---|
| Original request | Create a workflow for FA-MVP-0001 Repository Workspace Checkout MVP with H2 persistence, Docker volumes, public REST and UI flow. |
| Interpreted intent | Create an executable, slice-based workflow for a repository-source-owned checkout workspace MVP. |
| Change type | Feature foundation with persistence, public REST contract, frontend, Docker-local runtime and quality-gate impact. |
| Affected process strand | `workflow create` now; later `workflow execute`. |
| Affected architecture area | Repository source ownership, service-local persistence, public facade, OpenAPI/gRPC contracts, frontend state, Docker-local deployment. |
| Explicit requirements | Reuse `repository-source-service`; add repository-level `Workspace`, branch-level `WorkspaceBranch`, repository metadata, H2 file persistence, idempotency, branch refresh, Docker volumes, public REST endpoints and GUI flow. |
| Implicit requirements | Contract-first public API changes, service-private data ownership, deterministic source snapshot references, no path leakage, strict dependency verification and restart-persistence tests. |
| Accepted assumptions | `Workspace` in FA-MVP-0001 means repository checkout workspace state owned by `repository-source-service`, not broader organization/project membership workspace lifecycle. H2 is MVP-local service persistence, not the canonical analytics store. |
| Non-goals | No new `workspace-service`; no JavaParser, Joern, BTM, replay, reports, LLM, PostgreSQL, Neo4j, Kafka, RabbitMQ, Swarm or Kubernetes implementation. |
| Risks | Public `/workspaces` currently conflicts with OpenAPI tests; H2 and durable idempotency are new; default-branch resolution conflicts with current revision contract if branch/commit are both absent; frontend must not infer repository metadata. |
| Open questions | Exact repository-source owner API shape must be settled in the contract-first slice. Whether the transitional Compose descriptor is expanded or a new local descriptor is created is owned by the DevOps slice. |
| Blocking questions | None for workflow creation after recording accepted assumptions. Workflow execution must stop in the affected slice if a contract, table, field, task, route or ownership decision cannot be verified. |
| Confidence | 88 percent. |
| Decision | `PROCEED_WITH_ACCEPTED_ASSUMPTIONS`. |

## Scope

In scope:

- Extend `repository-source-service` with service-owned `Workspace`,
  `WorkspaceBranch`, `RepositoryIdentity`, `RepositoryKey`,
  `WorkspaceTitle`, branch status and repository metadata concepts.
- Add repository metadata resolution for clean HTTPS remotes.
- Add default-branch resolution with explicit fallback diagnostics.
- Add idempotent create/reuse workspace and branch use cases.
- Add idempotent branch refresh.
- Add service-local H2 persistence and schema initialization.
- Preserve in-memory repositories for tests or configured fallback.
- Add public REST workspace endpoints through `query-report-api-service`.
- Update OpenAPI and contract tests before public endpoint implementation.
- Add a repository-source owner API client in `query-report-api-service`.
- Add `forensic-ui` Create Workspace flow after API contracts are verified.
- Add Docker-local H2 data volume and preserve repository workspace volume.
- Update deployment and arc42 documentation only for verified local-Docker
  behavior.
- Add deterministic tests for persistence, idempotency, branch refresh, path
  redaction, public DTOs and UI behavior.

Out of scope:

- Creating a new `workspace-service`.
- Running repository build commands.
- Executing JavaParser, Joern, BTM generation, report generation, replay,
  graph projection or LLM context generation.
- Introducing PostgreSQL, Neo4j, vector databases, Kafka, RabbitMQ, Swarm or
  Kubernetes readiness.
- Sharing Java implementation, domain, DTO, utility, fixture, persistence or
  error-model modules between services.
- Allowing browser-to-Git or browser-to-gRPC calls.
- Exposing private filesystem paths, H2 paths, raw stdout/stderr, credentials
  or tokens.
- Making `workspaceTitle` editable or using it as a path, authorization key or
  security decision.

## Architecture Constraints

- `repository-source-service` owns checkout, repository metadata, source root
  detection, source snapshot references, workspace directories, H2 repository
  tables and durable idempotency for repository-source operations.
- `query-report-api-service` owns public REST validation, public DTO mapping,
  error redaction, idempotency header enforcement and owner API calls only.
- `query-report-api-service` must not read repository-source H2 files, private
  workspace directories or Git command output.
- `forensic-ui` owns UI state and calls only public REST endpoints.
- Service integration must remain contract-first through OpenAPI and
  gRPC/protobuf or another documented owner API contract.
- H2/JDBC classes must remain in repository-source outbound adapters and
  bootstrap wiring. Domain and application code must not depend on JDBC, H2,
  SQL result sets or Spring persistence APIs.
- `WorkspaceBranch.repositoryBranch` is data, not a directory name.
- Branch refresh must preserve previous snapshot evidence and create a new
  source snapshot reference when the resolved commit changes.
- Static source relationships, runtime traces, replay, reports and LLM outputs
  are outside this workflow.
- Diagnostics must represent missing evidence as unresolved, incomplete,
  unknown or not available instead of fabricating repository facts.

## Backend Assessment

Backend impact is high. The workflow adds new repository-source domain models,
ports, persistence, idempotency storage, branch refresh and metadata resolution.
It also adds public API routing in `query-report-api-service`. Existing
`PrepareRepository` logic must be reused where possible, but the current
fresh-random-workspace model does not yet represent a repository-level
workspace with multiple branches. Execution must verify each exact class,
method, proto field, route and table before modifying implementation.

## Frontend Assessment

Frontend impact is high because the requested Create Workspace flow is not
implemented today. The frontend already has an app shell, API adapter boundary,
ports, hooks and repository-analysis creation page. It must not present
repository metadata derived from the URL as confirmed evidence. Metadata,
workspace title, branch choices, save result and refresh result must come from
verified public API responses.

## Test Strategy

Each implementation slice must be regression-first:

1. Add or identify a deterministic failing test for the behavior.
2. Implement the smallest verified change.
3. Run the narrowest targeted test first.
4. Run the affected service or frontend test.
5. Run contract, architecture or Docker checks required by the slice.
6. Run `git diff --check`.
7. Run the minimum or full quality gate when the slice requires it.

Default commands from `QUALITY.md`:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Frontend commands:

```bash
cd forensic-ui
npm ci
npm run test
npm run build
```

Docker runtime checks are external and must be reported as skipped when Docker,
base-image access or network access is unavailable. `validatePlugins` is not
required unless Gradle plugin metadata, task inputs, task outputs or plugin
implementation classes change.

## Resilience And Security Requirements

- Only clean HTTPS repository URLs are allowed.
- Reject userinfo, query strings, fragments, `file:` URLs, local paths, SSH,
  SCP-style remotes, localhost and private IP ranges.
- Do not checkout submodules.
- Do not execute repository code or build commands.
- Enforce bounded timeout and workspace byte quota.
- Keep idempotency durable for H2-backed operations.
- Same idempotency key plus same fingerprint returns the same result.
- Same idempotency key plus different fingerprint returns a controlled
  conflict without state mutation.
- Partial checkout failures must not leave public success state.
- Cleanup must not escape the configured workspace root.
- Restart recovery must not infer missing state; unresolved or orphaned state
  must be surfaced as diagnostics.
- Correlation IDs must be propagated across public REST and owner API calls.

## Ordered Slices

### Slice 00 - Workflow Execution Preflight And Context Freeze

```yaml
slice_id: S00
profile: FULL_PATH
owner: senior-workflow-architect
secondary_reviewers:
  - senior-requirement-engineer
  - senior-system-architect
  - senior-tester
affected_files:
  - docs/workflow/**
affected_modules: []
affected_contracts: []
dependencies: []
parallel_group: G00
file_locks:
  - docs/workflow/**
contract_locks: []
architecture_locks:
  - workflow-create
  - workflow-execute-preflight
quality_gates:
  targeted:
    - 'git status --short --branch'
    - 'python3 -m json.tool docs/workflow/context-pack.json >/dev/null'
    - 'git diff --check'
  required:
    - 'git status --short'
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - active branch is not feature/workflow-repository-workspace-checkout-h2-persistence-20260524
  - working tree has unrelated or unclear changes
  - context pack hashes drift without review
```

Purpose: freeze the workflow branch, active requirement, governing document
hashes, role map, quality commands and accepted assumptions before any
implementation slice modifies production files.

### Slice 01 - Requirement Terminology And Data Ownership Gate

```yaml
slice_id: S01
profile: FULL_PATH
owner: senior-requirement-engineer
secondary_reviewers:
  - senior-system-architect
  - data-ownership-persistence-steward
  - senior-analysis-storage-architect
  - microservice-senior-expert
  - senior-tester
affected_files:
  - docs/workflow/three-amigos-decision-record.md
  - docs/architecture/data-ownership.md
  - docs/architecture/service-boundaries.md
  - docs/arc42/05-building-block-view.md
  - docs/arc42/08-crosscutting-concepts.md
affected_modules: []
affected_contracts: []
dependencies:
  - S00
parallel_group: G01
file_locks:
  - docs/workflow/**
  - docs/architecture/**
  - docs/arc42/**
contract_locks: []
architecture_locks:
  - repository-source-ownership
  - no-workspace-service
quality_gates:
  targeted:
    - 'git diff --check'
  required: []
documentation:
  arc42: update-if-ownership-text-drifts
  adr: check-ADR-0009-ADR-0013-ADR-0017
stop_conditions:
  - Workspace is reinterpreted as platform membership or organization workspace
  - H2 is treated as shared or canonical analytics persistence
  - a new workspace-service is introduced
  - service ownership remains ambiguous
```

Purpose: record that FA-MVP-0001 `Workspace` is a repository-source checkout
aggregate only, not broader platform workspace administration. Confirm
repository-source data ownership before implementation.

### Slice 02 - Contract-First Workspace API And Owner API

```yaml
slice_id: S02
profile: FULL_PATH
owner: contract-governance-expert
secondary_reviewers:
  - senior-grpc-proto-specialist
  - senior-java-backend
  - senior-react-frontend
  - senior-tester
  - security-reviewer
affected_files:
  - contracts/openapi/gateway-api.yaml
  - contracts/openapi/README.md
  - contracts/grpc/repository-analysis.proto
  - services/query-report-api-service/src/test/java/de/burger/forensics/analytics/services/queryreportapi/adapter/in/http/GatewayOpenApiContractTest.java
  - services/repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/adapter/in/grpc/RepositorySourceContractTest.java
  - docs/contracts/contract-test-plan.md
affected_modules:
  - services:query-report-api-service
  - services:repository-source-service
affected_contracts:
  - contracts/openapi/gateway-api.yaml
  - contracts/grpc/repository-analysis.proto
dependencies:
  - S01
parallel_group: G02
file_locks:
  - contracts/openapi/**
  - contracts/grpc/**
  - services/query-report-api-service/src/test/**
  - services/repository-source-service/src/test/**
contract_locks:
  - public-workspace-rest
  - repository-source-owner-api
architecture_locks:
  - contract-first-service-communication
quality_gates:
  targeted:
    - './gradlew :services:query-report-api-service:test --tests "*GatewayOpenApiContractTest" --dependency-verification strict --console=plain --stacktrace'
    - './gradlew :services:repository-source-service:test --tests "*RepositorySourceContractTest" --dependency-verification strict --console=plain --stacktrace'
    - 'git diff --check'
  required:
    - './gradlew :services:query-report-api-service:test --dependency-verification strict --console=plain --stacktrace'
    - './gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: check-service-communication
  adr: check-ADR-0010-ADR-0018
stop_conditions:
  - public REST routes are implemented before contract approval
  - workspace DTOs expose filesystem paths or raw Git output
  - owner API shape cannot be verified
  - current OpenAPI no-workspace assertions are removed without replacement coverage
```

Purpose: define the public REST routes, DTOs, error envelope, correlation and
idempotency requirements, plus the internal repository-source owner API needed
by `query-report-api-service`. This slice must settle whether new gRPC methods
are added to `repository-analysis.proto` or another verified owner API is used.

### Slice 03 - Repository Source Workspace Domain And In-Memory Use Cases

```yaml
slice_id: S03
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - senior-system-architect
  - senior-git-workspace-specialist
  - senior-tester
  - security-sandbox-specialist
affected_files:
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/domain/**
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/application/**
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/application/port/**
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/memory/**
  - services/repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/**
affected_modules:
  - services:repository-source-service
affected_contracts:
  - repository-source-domain
dependencies:
  - S02
parallel_group: G03
file_locks:
  - services/repository-source-service/src/main/**
  - services/repository-source-service/src/test/**
contract_locks:
  - repository-source-owner-api
architecture_locks:
  - repository-source-hexagonal-boundary
quality_gates:
  targeted:
    - './gradlew :services:repository-source-service:test --tests "*RepositorySourceDomainTest" --tests "*RepositorySourceApplicationServiceTest" --dependency-verification strict --console=plain --stacktrace'
    - 'git diff --check'
  required:
    - './gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update-if-domain-model-changes
  adr: check
stop_conditions:
  - WorkspaceId is derived from workspaceTitle or branch name
  - branch names are used directly as directories
  - default branch resolution requires guessing missing branch/commit contract behavior
  - domain/application code depends on JDBC, H2, filesystem or Git adapter classes
```

Purpose: introduce repository-source-owned `Workspace`, `WorkspaceBranch`,
`RepositoryIdentity`, `RepositoryKey`, `WorkspaceTitle`, branch status and
ports, with in-memory adapters or test doubles preserving existing tests.

### Slice 04 - Repository Metadata Resolution And Branch Checkout Refresh

```yaml
slice_id: S04
profile: FULL_PATH
owner: senior-git-workspace-specialist
secondary_reviewers:
  - senior-java-backend
  - security-sandbox-specialist
  - resilience-engineering
  - senior-tester
affected_files:
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/application/**
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/git/**
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/filesystem/**
  - services/repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/**
affected_modules:
  - services:repository-source-service
affected_contracts:
  - repository-source-owner-api
dependencies:
  - S03
parallel_group: G04
file_locks:
  - services/repository-source-service/src/main/**
  - services/repository-source-service/src/test/**
contract_locks:
  - repository-source-owner-api
architecture_locks:
  - repository-source-workspace-safety
quality_gates:
  targeted:
    - './gradlew :services:repository-source-service:test --tests "*RepositorySourceApplicationServiceTest" --tests "*RepositorySourceGrpcEndpointTest" --dependency-verification strict --console=plain --stacktrace'
    - 'git diff --check'
  required:
    - './gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update-runtime-view-if-refresh-flow-added
  adr: check
stop_conditions:
  - remote metadata lookup or refresh exposes raw stdout/stderr
  - implementation executes repository code, submodules or builds
  - branch refresh mutates previous source snapshot evidence destructively
  - cleanup can escape configured workspace root
  - unresolved default branch is silently converted to confirmed metadata
```

Purpose: add metadata preview, default branch resolution, create/reuse branch
checkout, source snapshot update and manual branch refresh behavior while
reusing existing checkout and source-root detection where verified.

### Slice 05 - H2 Dependency, Schema And Persistence Adapters

```yaml
slice_id: S05
profile: FULL_PATH
owner: senior-analysis-storage-architect
secondary_reviewers:
  - data-ownership-persistence-steward
  - senior-java-backend
  - senior-devops
  - security-reviewer
  - senior-tester
affected_files:
  - gradle/libs.versions.toml
  - gradle/verification-metadata.xml
  - services/repository-source-service/build.gradle.kts
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/h2/**
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/**
  - services/repository-source-service/src/main/resources/application.properties
  - services/repository-source-service/src/main/resources/application-docker.properties
  - services/repository-source-service/src/main/resources/application-test.properties
  - services/repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/**
affected_modules:
  - services:repository-source-service
affected_contracts:
  - repository-source-h2-schema
dependencies:
  - S04
parallel_group: G05
file_locks:
  - gradle/**
  - services/repository-source-service/**
contract_locks:
  - repository-source-h2-schema
architecture_locks:
  - service-local-persistence
quality_gates:
  targeted:
    - './gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace'
    - 'git diff --check'
  required:
    - './gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update-persistence-note-if-H2-added
  adr: check-ADR-0013
stop_conditions:
  - H2 dependency version or verification metadata cannot be verified
  - JDBC/H2 leaks into domain or application packages
  - schema creates shared cross-service tables
  - repository-source H2 files are read by another service
  - strict dependency verification cannot be restored
```

Purpose: add the H2 dependency, service-local schema initializer, configurable
adapter selection, H2-backed preparation/workspace/branch/idempotency
repositories and restart-persistence tests. Preserve in-memory adapters where
tests or fallback configuration require them.

### Slice 06 - Repository Source gRPC Endpoint And Error Mapping

```yaml
slice_id: S06
profile: FULL_PATH
owner: senior-grpc-proto-specialist
secondary_reviewers:
  - senior-java-backend
  - contract-governance-expert
  - senior-tester
  - observability-runtime-diagnostics
affected_files:
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/in/grpc/**
  - services/repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/adapter/in/grpc/**
  - contracts/grpc/repository-analysis.proto
affected_modules:
  - services:repository-source-service
affected_contracts:
  - contracts/grpc/repository-analysis.proto
dependencies:
  - S05
parallel_group: G06
file_locks:
  - services/repository-source-service/src/main/**
  - services/repository-source-service/src/test/**
  - contracts/grpc/**
contract_locks:
  - repository-source-owner-api
architecture_locks:
  - grpc-adapter-boundary
quality_gates:
  targeted:
    - './gradlew :services:repository-source-service:test --tests "*RepositorySourceGrpcEndpointTest" --tests "*RepositorySourceGrpcEndpointMappingTest" --tests "*RepositorySourceContractTest" --dependency-verification strict --console=plain --stacktrace'
    - 'git diff --check'
  required:
    - './gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: check-runtime-view
  adr: check-ADR-0010-ADR-0018
stop_conditions:
  - idempotency conflict is not mapped deterministically
  - validation, checkout and persistence errors are collapsed into raw internal errors
  - diagnostics leak private paths, raw stdout, raw stderr, credentials or tokens
  - generated protobuf code becomes a shared Java implementation dependency between services
```

Purpose: expose verified repository-source owner API behavior through gRPC or
the approved owner API, including metadata, workspace create/get and branch
refresh, with controlled status/error mapping.

### Slice 07 - Query Report Public Workspace REST Facade

```yaml
slice_id: S07
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - contract-governance-expert
  - senior-grpc-proto-specialist
  - security-reviewer
  - senior-react-frontend
  - senior-tester
affected_files:
  - services/query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/**
  - services/query-report-api-service/src/test/java/de/burger/forensics/analytics/services/queryreportapi/**
  - contracts/openapi/gateway-api.yaml
affected_modules:
  - services:query-report-api-service
affected_contracts:
  - contracts/openapi/gateway-api.yaml
  - repository-source-owner-api
dependencies:
  - S06
parallel_group: G07
file_locks:
  - services/query-report-api-service/**
  - contracts/openapi/**
contract_locks:
  - public-workspace-rest
architecture_locks:
  - public-facade-no-private-db
quality_gates:
  targeted:
    - './gradlew :services:query-report-api-service:test --tests "*GatewayOpenApiContractTest" --tests "*QueryReportApiHttpAdapterTest" --dependency-verification strict --console=plain --stacktrace'
    - 'git diff --check'
  required:
    - './gradlew :services:query-report-api-service:test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update-public-api-runtime-if-needed
  adr: check
stop_conditions:
  - query-report-api-service reads repository-source H2 files or workspace directories
  - public DTOs expose private paths or raw Git output
  - mutation routes do not require Idempotency-Key
  - correlation IDs are dropped
  - public REST behavior diverges from OpenAPI
```

Purpose: implement `POST /api/workspace-metadata`, `POST /api/workspaces`,
`GET /api/workspaces/{workspaceId}` and branch refresh through the
repository-source owner API only.

### Slice 08 - Forensic UI Create Workspace Flow

```yaml
slice_id: S08
profile: FULL_PATH
owner: senior-react-frontend
secondary_reviewers:
  - senior-ux-designer
  - contract-governance-expert
  - security-reviewer
  - senior-tester
affected_files:
  - forensic-ui/src/**
  - forensic-ui/package.json
  - forensic-ui/package-lock.json
  - forensic-ui/README.md
affected_modules:
  - forensic-ui
affected_contracts:
  - public-workspace-rest
dependencies:
  - S07
parallel_group: G08
file_locks:
  - forensic-ui/**
contract_locks:
  - public-workspace-rest
architecture_locks:
  - frontend-public-api-only
quality_gates:
  targeted:
    - 'cd forensic-ui && npm run test'
    - 'cd forensic-ui && npm run build'
    - 'git diff --check'
  required:
    - 'cd forensic-ui && npm ci'
    - 'cd forensic-ui && npm run test'
    - 'cd forensic-ui && npm run build'
documentation:
  arc42: check-frontend-context
  adr: check
stop_conditions:
  - UI derives repository metadata locally and presents it as confirmed
  - browser calls Git remotes, gRPC or internal services directly
  - workspaceTitle is editable in MVP
  - Save operation retries POST with a new semantic operation key by accident
  - diagnostics display private paths, raw stdout, raw stderr, credentials or tokens
```

Purpose: add the Create Workspace page, metadata preview, read-only title,
branch selection, save/progress/status, sanitized diagnostics and manual branch
refresh UI using the verified public REST contract.

### Slice 09 - Docker Local Volumes And Runtime Configuration

```yaml
slice_id: S09
profile: FULL_PATH
owner: senior-devops
secondary_reviewers:
  - senior-analysis-storage-architect
  - senior-git-workspace-specialist
  - security-reviewer
  - microservice-runtime-readiness-expert
  - senior-tester
affected_files:
  - services/repository-source-service/Dockerfile
  - services/repository-source-service/src/main/resources/application-docker.properties
  - deployment/docker-compose/repository-to-btm.local.yml
  - deployment/docker-compose/README.md
  - services/repository-source-service/README.md
  - docs/arc42/07-deployment-view.md
affected_modules:
  - services:repository-source-service
affected_contracts: []
dependencies:
  - S05
  - S06
parallel_group: G09
file_locks:
  - services/repository-source-service/Dockerfile
  - services/repository-source-service/src/main/resources/**
  - deployment/docker-compose/**
  - services/repository-source-service/README.md
  - docs/arc42/07-deployment-view.md
contract_locks: []
architecture_locks:
  - service-owned-volumes
  - docker-local-only
quality_gates:
  targeted:
    - './gradlew --no-daemon :services:repository-source-service:bootJar --dependency-verification strict --console=plain --stacktrace'
    - 'docker compose -f deployment/docker-compose/repository-to-btm.local.yml config'
    - 'git diff --check'
  required:
    - './gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update-deployment-view
  adr: check
stop_conditions:
  - repository-source private volumes are mounted into other services
  - host ports collide with existing services
  - Compose change claims full FA-MSA runtime readiness without executed evidence
  - Dockerfile runs repository-source as root
  - H2 data path is not owned by repository-source user
```

Purpose: add `/var/lib/forensic-analytics/repository-source-data`, service
configuration, Dockerfile ownership and Docker Compose volume model. Runtime
build/up checks remain optional external evidence unless actually executed.

### Slice 10 - Security, Leakage, Idempotency And Restart Integration Gate

```yaml
slice_id: S10
profile: FULL_PATH
owner: senior-tester
secondary_reviewers:
  - security-sandbox-specialist
  - resilience-engineering
  - senior-system-architect
  - senior-java-backend
  - senior-react-frontend
  - senior-devops
affected_files:
  - services/repository-source-service/src/test/**
  - services/query-report-api-service/src/test/**
  - forensic-ui/src/**/*.test.*
  - docs/workflow/execution-report.md
affected_modules:
  - services:repository-source-service
  - services:query-report-api-service
  - forensic-ui
affected_contracts:
  - public-workspace-rest
  - repository-source-owner-api
dependencies:
  - S07
  - S08
  - S09
parallel_group: G10
file_locks:
  - services/repository-source-service/src/test/**
  - services/query-report-api-service/src/test/**
  - forensic-ui/src/**
  - docs/workflow/**
contract_locks:
  - public-workspace-rest
  - repository-source-owner-api
architecture_locks:
  - evidence-integrity
  - no-path-leakage
quality_gates:
  targeted:
    - './gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace'
    - './gradlew :services:query-report-api-service:test --dependency-verification strict --console=plain --stacktrace'
    - 'cd forensic-ui && npm run test'
    - 'git diff --check'
  required:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: check-quality-requirements
  adr: check
stop_conditions:
  - no deterministic H2 restart/reopen test exists
  - idempotency conflict mutates state
  - branch refresh cannot prove unchanged versus changed commits
  - private paths or raw Git output appear in public DTO or UI tests
  - test fixtures are presented as forensic evidence
```

Purpose: close the cross-service regression risks: durable idempotency, H2
restart persistence, branch refresh determinism, no duplicate workspace/branch
records, redaction and frontend behavior.

### Slice 11 - Documentation, arc42 And ADR Closure

```yaml
slice_id: S11
profile: FULL_PATH
owner: senior-documentation-engineer
secondary_reviewers:
  - senior-system-architect
  - adr-steward
  - documentation-sync
  - senior-devops
  - senior-tester
affected_files:
  - docs/architecture/**
  - docs/arc42/**
  - docs/adr/**
  - docs/contracts/**
  - services/repository-source-service/README.md
  - services/query-report-api-service/README.md
  - forensic-ui/README.md
  - docs/workflow/**
affected_modules: []
affected_contracts:
  - public-workspace-rest
  - repository-source-owner-api
dependencies:
  - S10
parallel_group: G11
file_locks:
  - docs/**
  - services/repository-source-service/README.md
  - services/query-report-api-service/README.md
  - forensic-ui/README.md
contract_locks: []
architecture_locks:
  - documentation-traceability
quality_gates:
  targeted:
    - 'python3 -m json.tool docs/workflow/context-pack.json >/dev/null'
    - 'git diff --check'
  required: []
documentation:
  arc42: update-required
  adr: create-or-update-if-H2-decision-needs-ADR
stop_conditions:
  - documentation claims parser, Joern, BTM, replay, report, LLM or production database readiness
  - Docker docs claim Swarm or Kubernetes readiness without verified manifests
  - architecture docs describe H2 as canonical analytics persistence
  - requirement traceability to FA-MVP-0001 is missing
```

Purpose: synchronize architecture and service documentation with actual
implemented behavior and record limitations honestly.

### Slice 12 - Final Quality Gate And Workflow Handoff

```yaml
slice_id: S12
profile: FULL_PATH
owner: quality-gate-orchestrator
secondary_reviewers:
  - senior-devops
  - senior-tester
  - senior-system-architect
  - git-commit-reviewer
affected_files:
  - docs/workflow/execution-report.md
affected_modules:
  - services:repository-source-service
  - services:query-report-api-service
  - forensic-ui
affected_contracts:
  - public-workspace-rest
  - repository-source-owner-api
dependencies:
  - S11
parallel_group: G12
file_locks:
  - docs/workflow/**
contract_locks:
  - public-workspace-rest
  - repository-source-owner-api
architecture_locks:
  - final-quality-gate
quality_gates:
  targeted:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
    - 'cd forensic-ui && npm run test'
    - 'cd forensic-ui && npm run build'
    - 'git diff --check'
  required:
    - './gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - minimum quality gate fails due to current slice
  - full local quality gate fails without documented pre-existing blocker
  - frontend test or build fails due to current slice
  - strict dependency verification remains broken
  - final diff contains unrelated changes
```

Purpose: run the repository quality gate, frontend gate and final diff review
before commit or slice checkpoint push eligibility is considered.

## Slice Dependency Graph

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
      S05 + S06 -> S09
S07 + S08 + S09 -> S10
S10 -> S11 -> S12
```

Parallelization is limited because the public contract, repository-source owner
API and H2 persistence shape are dependency anchors. After S07, frontend S08
and Docker S09 may proceed in parallel only if their write scopes stay
disjoint and contract DTOs are frozen.

## Role And Subagent Ownership Map

| Area | Owner | Required reviewers |
|---|---|---|
| Requirement gate | Senior Requirement Engineer | Senior System Architect, Senior Tester |
| Architecture and service boundaries | Senior System Architect | Microservice Senior Expert, Data Ownership |
| Public REST/OpenAPI | Contract Governance Expert | Senior Java Backend, Senior React Frontend, Senior Tester |
| gRPC/protobuf owner API | Senior gRPC/Proto Specialist | Contract Governance, Senior Java Backend |
| Repository-source domain/application | Senior Java Backend | Senior System Architect, Senior Git Workspace Specialist |
| H2 persistence | Senior Analysis Storage Architect | Data Ownership, Senior Java Backend, Security |
| Workspace filesystem and Git safety | Senior Git Workspace Specialist | Security Sandbox, Resilience |
| Query-report facade | Senior Java Backend | Contract Governance, Security, Frontend impact |
| Frontend flow | Senior React Frontend | Senior UX Designer, Contract Governance, Senior Tester |
| Docker and local runtime | Senior DevOps | Storage, Security, Runtime Readiness |
| Quality gates | Senior Tester / Quality Gate Orchestrator | DevOps, System Architect |
| Documentation and arc42 | Senior Documentation Engineer | ADR Steward, System Architect |

Callable subagents are authorized by the user request. During `workflow execute`,
subagents may be used for read-only review or bounded implementation slices
with disjoint write scopes. Each subagent must verify the active workflow
branch before modifying files and must not switch branches unless the workflow
explicitly authorizes it.

## Stop Conditions

Stop workflow execution if:

- The active branch is not
  `feature/workflow-repository-workspace-checkout-h2-persistence-20260524`.
- A required file, class, method, route, proto field, table, Gradle task or
  config key cannot be verified exactly.
- Source, contracts and docs disagree in a behavior-relevant way.
- Implementation would create a new `workspace-service`.
- Implementation would add JavaParser, Joern, BTM, replay, report, LLM,
  PostgreSQL, Neo4j, Kafka, RabbitMQ, Swarm or Kubernetes behavior.
- Public routes are implemented before OpenAPI and owner API contracts are
  approved.
- H2/JDBC leaks into domain or application packages.
- `query-report-api-service` reads repository-source private storage.
- UI calls internal services, Git remotes or gRPC directly.
- Any public DTO or UI state exposes private paths, raw stdout, raw stderr,
  credentials, tokens or private network details.
- Default branch or branch refresh behavior would require guessing missing
  repository facts.
- Strict dependency verification cannot be restored after adding H2.
- Required quality commands cannot be verified from repository files.

## Commit And Push Plan

Workflow creation itself does not commit or push unless explicitly requested.

During later `workflow execute`, slice checkpoint commits and pushes are allowed
only when the active workflow executor confirms the slice passed required
quality gates and the workflow branch remains active. Slice checkpoint push is
not `push auto`, does not create or merge a PR and must not run branch cleanup.

## Definition Of Done

FA-MVP-0001 is done when:

- `repository-source-service` owns and persists repository checkout workspaces.
- `Workspace` is a repository-level aggregate and `WorkspaceBranch` is a
  branch-level aggregate.
- `workspaceTitle` is derived from repository name, read-only and never used as
  a path or security identifier.
- Repository identity is normalized through `RepositoryKey`.
- H2 persistence is configurable and service-local.
- In-memory repositories remain available where tests or fallback require them.
- Workspace and branch creation are idempotent.
- Branch refresh is idempotent and records changed versus unchanged commit
  state.
- H2 state survives repository-source restart with the same Docker data volume.
- Public REST exposes metadata preview, create/reuse workspace, get workspace
  and branch refresh through sanitized DTOs.
- `forensic-ui` can preview metadata, save a workspace, show progress/status
  and refresh a branch through public REST only.
- Docker-local volume ownership is documented and verified.
- No local paths, raw Git output or secrets are exposed publicly.
- Tests cover H2 persistence, idempotency, branch refresh, public DTO redaction
  and frontend behavior.
- Required quality gates pass or any pre-existing blocker is documented with
  exact failing task and evidence.

## Handoff To Workflow Execute

`workflow execute` must read this full workflow, load `docs/workflow/context-pack.md`
and `docs/workflow/context-pack.json`, run S00 first, then execute slices in
dependency order. Direct ad-hoc implementation is not allowed before the
relevant role or subagent review for the slice.
