# Workflow: Remote Branch Metadata Listing And Persistence

## Executive Summary

This workflow enables the GUI to list remote repository branches resolved by the repository metadata endpoint and to persist the selected branch through the repository-source workspace metadata owner.

The concrete defect hypothesis from the request is narrow: `git branch -a` in the Forensic Analytics checkout only shows branches of this repository and is not the relevant command for a remote target repository such as `https://github.com/wildfly/wildfly.git`. The relevant product path is `POST /api/workspace-metadata`, which must call repository metadata resolution for the submitted repository URL and return `repositoryBranches` to the UI. If `repositoryBranches` is missing in the UI while the Git metadata adapter returns them, the remaining risk is runtime/service version, gateway forwarding, REST mapping, frontend mapper, or UI state.

This workflow does not implement code. It defines executable slices for `workflow execute`.

## Verified Baseline

- Active branch: `feature/workflow-remote-branches-gui-persistence-20260602`
- Workflow version: `2026-06-02`
- Process strand: `workflow create`
- Execution profile: `FULL_PATH`
- Repository root: `/mnt/d/Projects/forensic_analytics`
- Host execution requirement: WSL with `./gradlew`
- Owner service for repository metadata and persisted workspace branch state: `repository-source-service`
- Public gateway/API service: `query-report-api-service`
- Frontend module: `forensic-ui`
- Existing OpenAPI endpoint: `POST /api/workspace-metadata`
- Existing gRPC method: `PreviewRepositoryWorkspaceMetadata`
- Existing response field: `repositoryBranches` / `repository_branches`
- Existing persistence decision: ADR-0024 PostgreSQL for repository-source workspace metadata
- Quality source: `QUALITY.md`

## Requirement Clarification Gate

Decision: `READY_FOR_WORKFLOW`

Confidence: 92 percent.

Original request:

- Create a workflow so the GUI can list remote branches and store them in the database.
- Use `/api/workspace-metadata` with `https://github.com/wildfly/wildfly.git` as the relevant runtime path.
- Treat missing UI display as a data-path or runtime-version problem, not as a local `git branch -a` command problem.

Interpreted intent:

- Ensure the metadata preview endpoint resolves remote branches from the submitted remote repository URL.
- Ensure `repositoryBranches` survives the repository-source gRPC response, query-report gateway REST response, frontend API mapper, and React UI state.
- Ensure the selected branch is persisted as repository-source-owned workspace branch metadata when a workspace is created or selected.
- Add regression tests that can prove a multi-branch repository such as WildFly would be represented without depending on live GitHub in unit tests.

Explicit requirements:

- Remote branch discovery must use remote metadata resolution for the submitted repository URL, not local workspace branch enumeration.
- `/api/workspace-metadata` must expose `repositoryBranches` in the response.
- The GUI must show the remote branches returned by `repositoryBranches`.
- The selected branch must be stored through repository-source persistence.
- Runtime/gateway/UI version mismatches must be diagnosable.

Accepted assumptions:

- The database persistence target is repository-source workspace metadata governed by ADR-0024.
- The selected branch persistence is tied to workspace creation or workspace branch state, not to the preview-only metadata call by itself.
- Branch names are data values only; they are not filesystem paths and not evidence of runtime execution.
- Tests must use deterministic fixtures instead of calling live GitHub.

Non-goals:

- No direct UI access to Git commands, PostgreSQL, H2, or repository-source private tables.
- No live GitHub dependency in unit tests or required quality gates.
- No broad service migration or new shared Java DTO module.
- No automatic checkout, fetch, or analysis run merely because the user previews metadata.
- No fabricated branch count. If WildFly currently has a different branch count than the GitHub UI, diagnostics must show what the metadata path returned.

Open questions for workflow execution:

- Whether an optional manual smoke test against `https://github.com/wildfly/wildfly.git` is allowed in the developer environment.
- Whether existing running containers need a rebuild/restart or image tag change to remove a stale service-version problem.

## Five-Role Three Amigos Review

Senior Requirement Engineer:

- Requirement is traceable to the user request and existing workspace metadata capability.
- EPIC v0.2 supports repository context as provenance, but exact REST/gRPC fields are governed by contracts and workflow slices.
- No blocking requirement question remains because the requested endpoint, response field, UI behavior and persistence owner are verifiable.

Senior System Architect:

- `repository-source-service` remains the only owner and writer of repository workspace metadata.
- `query-report-api-service` remains a public facade and must not read repository-source tables directly.
- `forensic-ui` consumes sanitized public DTOs only.
- ADR-0024 covers PostgreSQL ownership for repository-source workspace metadata.

Senior Java Backend Developer:

- Backend execution must verify `GitRepositoryMetadataAdapter.resolveBranches`, `RepositoryWorkspaceApplicationService.previewRepositoryWorkspaceMetadata`, gRPC endpoint mapping, query-report gRPC client mapping and HTTP handler serialization.
- Any missing field or mapping mismatch is a STOP condition, not a reason to invent a parallel endpoint.

Senior React Frontend Developer:

- Frontend execution must verify `WorkspaceMetadata.repositoryBranches`, API DTO mapper behavior, create-workspace branch chooser, and user-visible diagnostics when no branches are returned.
- UI must not infer remote branches from local workspace branches.

Senior Tester:

- Required tests include deterministic fake metadata with many branches, contract serialization checks, API mapper checks and React UI assertions.
- Optional manual smoke test can be documented separately and must not be required for CI.

Dependency / Deadlock Validator:

- Slice dependencies are acyclic.
- Contract/backend slices precede UI integration.
- Persistence validation depends on repository-source branch metadata behavior.
- File locks are disjoint where parallel review is possible, but implementation should run one slice at a time under `workflow execute`.

## Target Picture

```text
forensic-ui Create Workspace page
  -> POST /api/workspace-metadata { repositoryUrl: "https://github.com/wildfly/wildfly.git" }
     -> query-report-api-service HTTP handler
        -> query-report repository-source gRPC client
           -> repository-source-service PreviewRepositoryWorkspaceMetadata
              -> GitRepositoryMetadataAdapter: git ls-remote --heads <submitted remote URL>
              -> RepositoryWorkspaceMetadataPreview.repositoryBranches
     <- WorkspaceMetadataResponse.repositoryBranches
  -> branch selector displays remote branches
  -> create workspace with selectedBranch
     -> repository-source-service persists repository workspace branch metadata
```

Remote branch names are data only. They must never be used directly as local paths or evidence of runtime execution.

## Architecture Constraints

- Domain and application code remain independent from Git, REST, gRPC, PostgreSQL and React implementation details.
- Git remote metadata resolution stays in an outbound adapter.
- Public REST and gRPC contracts remain sanitized and deterministic.
- Repository branch names must never be used directly as local paths.
- Repository-source remains the only writer for workspace metadata and branch records.
- Query-report and UI must not access repository-source persistence directly.
- Planned behavior must not be described as implemented until `workflow execute` completes the relevant slice.

## Backend Assessment

Verified backend symbols and files:

- `repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/git/GitRepositoryMetadataAdapter.java`
- `repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/application/RepositoryWorkspaceApplicationService.java`
- `repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/application/port/RepositoryMetadataResolution.java`
- `repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/in/grpc/RepositorySourceGrpcEndpoint.java`
- `query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/adapter/out/grpc/RepositorySourceWorkspaceGrpcClient.java`
- `query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/adapter/in/http/QueryReportApiHttpHandler.java`
- `contracts/grpc/repository-analysis.proto`
- `contracts/openapi/gateway-api.yaml`

## Frontend Assessment

Verified frontend symbols and files:

- `forensic-ui/src/domain/workspace.ts`
- `forensic-ui/src/adapters/api/dtos.ts`
- `forensic-ui/src/adapters/api/mappers.ts`
- `forensic-ui/src/adapters/api/apiClient.ts`
- `forensic-ui/src/pages/workspaces/CreateWorkspacePage.tsx`
- Existing tests under `forensic-ui/src/**.test.tsx` and `forensic-ui/src/**.test.ts`

## Test Strategy

Run narrow tests first, then the full quality gate when implementation changes are complete.

Targeted backend candidates:

```bash
./gradlew :repository-source-service:test --tests "*GitRepositoryMetadataAdapterTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :repository-source-service:test --tests "*RepositorySourceGrpcEndpointTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :query-report-api-service:test --tests "*RepositorySourceWorkspaceGrpcClientTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :query-report-api-service:test --tests "*QueryReportApiHttpAdapterTest" --dependency-verification strict --console=plain --stacktrace
```

Targeted frontend candidates:

```bash
cd forensic-ui && npm test -- --run src/adapters/api/mappers.test.ts src/adapters/api/apiClient.test.ts src/pages/workspaces/CreateWorkspacePage.test.tsx
```

Minimum repository command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Resilience And Diagnostics Requirements

- Metadata lookup timeouts must remain bounded by the existing metadata policy.
- Missing or empty remote branch lists must be represented as diagnostics; the UI must not silently replace them with local branches.
- Gateway and UI tests must prove `repositoryBranches` is not dropped.
- Runtime-version diagnosis must include service image/build freshness checks in documentation or execution report when manual smoke testing is performed.
- Public diagnostics must not leak local paths, raw credentials, private DNS results, JDBC URLs or raw Git output.

## Ordered Slices

### Slice 01 - Metadata Contract And Owner Path Verification

Purpose: Prove the existing contracts and repository-source owner path carry remote branch lists end to end inside backend service boundaries.

```yaml
slice_id: S01
profile: FULL_PATH
owner: Senior Java Backend Developer
secondary_reviewers:
  - Senior System Architect
  - Senior Tester
affected_files:
  - contracts/grpc/repository-analysis.proto
  - contracts/openapi/gateway-api.yaml
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/git/GitRepositoryMetadataAdapter.java
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/application/RepositoryWorkspaceApplicationService.java
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/in/grpc/RepositorySourceGrpcEndpoint.java
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/git/GitRepositoryMetadataAdapterTest.java
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/adapter/in/grpc/RepositorySourceGrpcEndpointTest.java
affected_modules:
  - repository-source-service
affected_contracts:
  - contracts/grpc/repository-analysis.proto
  - contracts/openapi/gateway-api.yaml
dependencies: []
parallel_group: G1
file_locks:
  - contracts/grpc/repository-analysis.proto
  - contracts/openapi/gateway-api.yaml
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/**
contract_locks:
  - PreviewRepositoryWorkspaceMetadataResponse.repository_branches
  - WorkspaceMetadataResponse.repositoryBranches
architecture_locks:
  - repository-source-service owns repository workspace metadata
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --tests "*GitRepositoryMetadataAdapterTest" --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :repository-source-service:test --tests "*RepositorySourceGrpcEndpointTest" --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: ADR-0024 applies
stop_conditions:
  - PreviewRepositoryWorkspaceMetadataResponse.repository_branches cannot be verified
  - repository-source-service is not the owner of workspace metadata
  - branch list requires live GitHub for automated tests
```

Done criteria:

- Tests prove a deterministic fake remote with many branch names is returned as `repositoryBranches`.
- Contract tests prove the field is present and sanitized.
- No local `git branch -a` behavior is used as remote metadata evidence.

### Slice 02 - Gateway Forwarding And Public REST Serialization

Purpose: Ensure `query-report-api-service` forwards and serializes `repositoryBranches` without dropping or renaming the field.

```yaml
slice_id: S02
profile: FULL_PATH
owner: Senior Java Backend Developer
secondary_reviewers:
  - Contract-First API Steward
  - Senior Tester
affected_files:
  - query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/domain/QueryReportApiWorkspace.java
  - query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/adapter/out/grpc/RepositorySourceWorkspaceGrpcClient.java
  - query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/adapter/in/http/QueryReportApiHttpHandler.java
  - query-report-api-service/src/test/java/de/burger/forensics/analytics/services/queryreportapi/adapter/out/grpc/RepositorySourceWorkspaceGrpcClientTest.java
  - query-report-api-service/src/test/java/de/burger/forensics/analytics/services/queryreportapi/adapter/in/http/QueryReportApiHttpAdapterTest.java
affected_modules:
  - query-report-api-service
affected_contracts:
  - POST /api/workspace-metadata
dependencies:
  - S01
parallel_group: G2
file_locks:
  - query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/**
  - query-report-api-service/src/test/java/de/burger/forensics/analytics/services/queryreportapi/**
contract_locks:
  - POST /api/workspace-metadata WorkspaceMetadataResponse.repositoryBranches
architecture_locks:
  - query-report-api-service remains public facade only
quality_gates:
  targeted:
    - ./gradlew :query-report-api-service:test --tests "*RepositorySourceWorkspaceGrpcClientTest" --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :query-report-api-service:test --tests "*QueryReportApiHttpAdapterTest" --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: ADR-0024 applies
stop_conditions:
  - Gateway cannot verify repositoryBranches in response body
  - Gateway reads repository-source persistence directly
  - REST field name differs from OpenAPI without contract update
```

Done criteria:

- HTTP adapter test proves JSON contains `"repositoryBranches"` with multiple branch values.
- gRPC client test proves repository-source `repository_branches` maps to public domain `repositoryBranches`.

### Slice 03 - UI Metadata Data Path And Branch Listing

Purpose: Ensure the GUI renders remote branches returned by `/api/workspace-metadata` and does not replace them with local workspace branches.

```yaml
slice_id: S03
profile: FULL_PATH
owner: Senior React Frontend Developer
secondary_reviewers:
  - Senior UX Designer
  - Senior Tester
affected_files:
  - forensic-ui/src/domain/workspace.ts
  - forensic-ui/src/adapters/api/dtos.ts
  - forensic-ui/src/adapters/api/mappers.ts
  - forensic-ui/src/adapters/api/apiClient.ts
  - forensic-ui/src/pages/workspaces/CreateWorkspacePage.tsx
  - forensic-ui/src/adapters/api/mappers.test.ts
  - forensic-ui/src/adapters/api/apiClient.test.ts
  - forensic-ui/src/pages/workspaces/CreateWorkspacePage.test.tsx
affected_modules:
  - forensic-ui
affected_contracts:
  - POST /api/workspace-metadata
dependencies:
  - S02
parallel_group: G3
file_locks:
  - forensic-ui/src/domain/workspace.ts
  - forensic-ui/src/adapters/api/**
  - forensic-ui/src/pages/workspaces/CreateWorkspacePage.tsx
  - forensic-ui/src/pages/workspaces/CreateWorkspacePage.test.tsx
contract_locks:
  - WorkspaceMetadata.repositoryBranches
architecture_locks:
  - frontend consumes public API only
quality_gates:
  targeted:
    - cd forensic-ui && npm test -- --run src/adapters/api/mappers.test.ts src/adapters/api/apiClient.test.ts src/pages/workspaces/CreateWorkspacePage.test.tsx
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: n/a
stop_conditions:
  - UI mapper drops repositoryBranches
  - UI infers branches from local workspace records during metadata preview
  - branch selector cannot be tested deterministically
```

Done criteria:

- UI tests prove multiple `repositoryBranches` from metadata appear in the branch selection control.
- Empty branch list shows explicit diagnostics or unavailable state instead of a fabricated branch.

### Slice 04 - Selected Branch Persistence Through Repository-Source Metadata

Purpose: Ensure selecting a remote branch results in persisted repository-source workspace branch metadata when the workspace is created or updated through the owner service.

```yaml
slice_id: S04
profile: FULL_PATH
owner: Senior Java Backend Developer
secondary_reviewers:
  - Senior Analysis Storage Architect
  - Senior Tester
affected_files:
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/application/RepositoryWorkspaceApplicationService.java
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/postgres/PostgresRepositorySourcePersistenceAdapter.java
  - repository-source-service/src/main/resources/db/changelog/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/application/RepositorySourcePostgresPersistenceApplicationTest.java
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/postgres/PostgresRepositorySourceLiquibaseTest.java
affected_modules:
  - repository-source-service
affected_contracts:
  - CreateRepositoryWorkspaceRequest.selected_branch
  - CreateWorkspaceRequest.selectedBranch
dependencies:
  - S01
parallel_group: G4
file_locks:
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/application/**
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/postgres/**
  - repository-source-service/src/main/resources/db/changelog/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/**
contract_locks:
  - repository workspace branch persistence
architecture_locks:
  - ADR-0024 repository-source-only write ownership
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --tests "*RepositorySourcePostgresPersistenceApplicationTest" --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :repository-source-service:test --tests "*PostgresRepositorySourceLiquibaseTest" --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: ADR-0024 applies
stop_conditions:
  - selected branch persistence field cannot be verified
  - persistence requires query-report-api-service or UI to write repository-source tables
  - schema change is needed but no Liquibase owner path is found
```

Done criteria:

- Persistence tests prove the selected branch is stored and loaded through repository-source persistence.
- No other service writes repository-source workspace tables.

### Slice 05 - Runtime Smoke Diagnostics And Documentation Closure

Purpose: Add operator-facing verification notes for runtime/service-version, gateway and UI data-path diagnosis without making live GitHub a mandatory quality gate.

```yaml
slice_id: S05
profile: FULL_PATH
owner: Senior DevOps Engineer
secondary_reviewers:
  - Senior Documentation Engineer
  - Senior Tester
affected_files:
  - docs/workflow/execution-report.md
  - query-report-api-service/README.md
  - repository-source-service/README.md
  - forensic-ui/README.md
  - deployment/docker-compose/README.md
affected_modules:
  - documentation
  - deployment/docker-compose
affected_contracts: []
dependencies:
  - S02
  - S03
  - S04
parallel_group: G5
file_locks:
  - docs/workflow/execution-report.md
  - query-report-api-service/README.md
  - repository-source-service/README.md
  - forensic-ui/README.md
  - deployment/docker-compose/README.md
contract_locks: []
architecture_locks:
  - no live external dependency in required quality gate
quality_gates:
  targeted:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked
  adr: ADR-0024 applies
stop_conditions:
  - documentation claims a live WildFly smoke test passed without execution evidence
  - documentation requires network access for normal quality gates
  - runtime diagnosis exposes secrets, private paths or raw infrastructure details
```

Done criteria:

- Documentation explains that `git branch -a` in the Forensic Analytics repo is not the remote target branch source.
- Optional smoke command documents `POST /api/workspace-metadata` with `https://github.com/wildfly/wildfly.git` and expected `repositoryBranches` presence.
- Runtime stale-version checks are documented as operational diagnostics.

## Slice Dependency Graph

```text
S01
├── S02
│   └── S03
├── S04
└── S05 depends on S02, S03 and S04
```

## Parallelization Opportunities

- S01 is the prerequisite for backend confidence.
- S02 and S04 can be reviewed independently after S01 because gateway forwarding and persistence have separate write scopes.
- S03 must wait for S02 contract/API mapping stability.
- S05 must wait for implemented behavior evidence from S02, S03 and S04.

## Role Ownership Map

| Area | Primary Owner | Reviewers |
|---|---|---|
| Requirement traceability | Senior Requirement Engineer | Senior Tester |
| Architecture and ownership | Senior System Architect | Senior Analysis Storage Architect |
| Repository-source backend | Senior Java Backend Developer | Senior Tester |
| Gateway/API forwarding | Senior Java Backend Developer | Contract-First API Steward |
| React UI | Senior React Frontend Developer | Senior UX Designer, Senior Tester |
| Persistence | Senior Java Backend Developer | Senior Analysis Storage Architect |
| Runtime diagnostics docs | Senior DevOps Engineer | Senior Documentation Engineer |

## Documentation Synchronization Points

- Update `docs/workflow/execution-report.md` during workflow execution with actual commands and outcomes.
- Update service READMEs only when implementation behavior changes.
- Update arc42 sections only if execution changes accepted architecture behavior, not merely because tests are added.
- ADR-0024 remains sufficient unless execution changes persistence ownership or database technology.

## Stop Conditions

Stop during `workflow execute` if:

- Any named file, method, field or endpoint cannot be found exactly.
- `repositoryBranches` is absent from a verified contract or DTO path.
- Implementation would require UI, query-report or another service to write repository-source tables.
- A live GitHub response is required to pass automated tests.
- Branch count is asserted from GitHub UI instead of from the actual metadata endpoint response.
- Runtime smoke evidence is claimed without running the command.

## Commit And Push Plan

- `workflow create` may commit workflow documentation only if explicitly requested later.
- `workflow execute` may commit per slice only when the active workflow allows it and the slice quality gate passes.
- No push or PR is authorized by this workflow creation request.

## Definition Of Done

- `docs/workflow/workflow.md` exists and describes executable slices.
- `docs/workflow/context-pack.md` and `docs/workflow/context-pack.json` record governing context.
- `docs/workflow/three-amigos-decision-record.md`, `role-ownership.md`, `slice-dependency-map.md`, `quality-and-leakage-gates.md`, `deployment-description.md`, `execution-report.md` and `arc42-check-status.md` exist.
- arc42 has been checked for owner, gateway, UI and persistence boundaries.
- Workflow is ready for explicit `workflow execute`.

## Handoff To Workflow Execute

Start with S01. Before implementation, re-read this workflow completely, verify branch `feature/workflow-remote-branches-gui-persistence-20260602`, verify `git status --short`, and route the slice through the configured subagent or role-review workflow.
