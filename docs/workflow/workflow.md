# Workflow: FA-MVP-0001 PostgreSQL Repository Workspace Checkout Alignment

## Executive Summary

This workflow corrects FA-MVP-0001 workflow input after the H2 runtime blocker.
The authoritative runtime persistence target is PostgreSQL under ADR-0024.
ADR-0023 is retained only for deterministic repository-source adapter tests and
direct fixtures. H2 must not be used as runtime storage, Docker persistence,
startup fallback, readiness fallback or cross-service persistence.

This workflow does not implement product code. It creates the corrected
workflow documentation, acceptance criteria, service-boundary checks,
persistence constraints and quality gates needed before any future
`workflow execute` step.

## Verified Baseline

- Active branch:
  `feature/workflow-repository-workspace-checkout-20260604`
- Workflow version: `2026-06-04`
- Process strand: `workflow create`
- Execution profile: `FULL_PATH`
- Repository root: `/mnt/d/Projects/forensic_analytics`
- Host execution requirement: WSL path with Linux-style commands and
  `./gradlew`
- Runtime persistence decision: ADR-0024 PostgreSQL for
  `repository-source-service` workspace metadata
- H2 decision: ADR-0023 accepted for tests only, superseded for runtime by
  ADR-0024
- Owner service for repository checkout workspace metadata:
  `repository-source-service`
- Public facade service: `query-report-api-service`
- Frontend module: `forensic-ui`
- Current Docker-local repository-source descriptor mounts only
  `repository-source-workspaces` and configures PostgreSQL through
  `forensic-postgres`
- Quality source: `QUALITY.md`

Verified source-of-truth files:

- `AGENTS.md`
- `QUALITY.md`
- `.codex/AGENTS.md`
- `.codex/workflow/workflow-execution-rules.md`
- `.agents/orchestrator/routing-rules.md`
- `.agents/orchestrator/swarm-orchestrator.md`
- `.agents/skills/workflow-authoring/SKILL.md`
- `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md`
- `docs/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md`
- `docs/adr/ADR-0024-postgres-for-repository-source-workspace-metadata.md`
- `docs/architecture/data-ownership.md`
- `docs/architecture/service-boundaries.md`
- `repository-source-service/README.md`
- `query-report-api-service/README.md`
- `repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceServiceApplicationTest.java`

## Requirement Clarification Gate

Decision: `READY_FOR_WORKFLOW`

Confidence: 94 percent.

Original blocker:

- The pasted FA-MVP-0001 requirement still required H2 runtime and Docker
  persistence.
- Current ADRs, README files and tests require PostgreSQL for runtime metadata
  persistence and reject H2 as runtime storage.

User clarification:

- PostgreSQL under ADR-0024 is authoritative for this workflow.
- ADRs must not be reopened, superseded or weakened.
- H2 runtime persistence, H2 Docker persistence and H2 fallback behavior are
  outdated for FA-MVP-0001.
- H2 remains allowed only for deterministic repository-source adapter tests and
  direct fixtures.

Interpreted intent:

- Regenerate workflow artifacts so FA-MVP-0001 aligns with ADR-0024 and the
  revised ADR-0023.
- Keep repository-source as the only owner and writer for checkout workspace,
  branch, preparation and idempotency metadata.
- Keep query-report and UI as consumers of owner APIs and sanitized public DTOs.
- Preserve visibility of PostgreSQL startup/readiness failures.
- Do not implement product code during workflow creation.

Explicit requirements:

- FA-MVP-0001 no longer requires H2 runtime or Docker persistence.
- Runtime repository-source metadata persistence uses PostgreSQL.
- H2 is allowed only for deterministic adapter tests and direct fixtures.
- Docker-local deployments do not mount H2 files as active runtime storage.
- No service except `repository-source-service` may access repository-source
  persistence directly.
- Other services may consume only owner APIs and sanitized public DTOs.
- PostgreSQL startup or readiness failure is visible and must not be hidden by
  fallback storage.
- Existing H2 files are historical MVP data and require an explicit migration
  slice if preservation is needed.
- The workflow references ADR-0023, ADR-0024, data ownership documentation,
  service-boundary documentation and relevant README/test expectations.
- `git status --short` must remain clean except intended workflow and
  documentation artifacts.

Accepted assumptions:

- The branch created for the stopped workflow remains the active workflow branch.
- The current ADR-0023 update on this branch is the accepted clarification of
  H2 scope.
- Existing implementation evidence may be cited as baseline only after exact
  file paths are verified. It is not changed by this workflow-create run.
- If preservation of existing H2 files is required, that is a separate
  migration workflow or slice, not part of this workflow.

Non-goals:

- No product source, test, build, OpenAPI, gRPC, Dockerfile or frontend code
  changes in `workflow create`.
- No new `workspace-service`.
- No H2 runtime fallback, Docker fallback or readiness fallback.
- No direct query-report, UI, CLI, worker or analysis-service access to
  repository-source PostgreSQL tables, H2 files or checkout directories.
- No JavaParser, Joern, BTM, replay, report, LLM, Neo4j, vector database,
  Kafka/RabbitMQ, Swarm or Kubernetes implementation.
- No migration of historical H2 files unless a later explicit migration slice
  is approved.

Open questions:

- None blocking for workflow creation.

## Five-Role Three Amigos Review

Senior Requirement Engineer:

- Requirement drift is resolved by treating ADR-0024 as authoritative and
  revised ADR-0023 as test-only H2 scope.
- FA-MVP-0001 remains repository checkout workspace scope, not broader platform
  workspace administration.
- Acceptance criteria are testable and trace to ADRs, README files, service
  boundaries and quality gates.

Senior System Architect:

- `repository-source-service` remains the only owner and writer for
  repository checkout workspace metadata.
- PostgreSQL is service-owned repository-source metadata storage only, not
  shared canonical analytics persistence.
- H2 is historical/test fixture scope only.
- Query-report and UI remain facade/client layers and must not read private
  storage.

Senior Java Backend Developer:

- Backend verification must prove runtime persistence selection is PostgreSQL
  or memory only where explicitly test-profile scoped.
- H2 adapter tests remain allowed when they instantiate the H2 adapter directly.
- PostgreSQL readiness failure must surface as startup failure or storage
  readiness `DOWN`.

Senior React Frontend Developer:

- Frontend consumes only public workspace DTOs from `query-report-api-service`.
- UI must never infer, display or depend on repository-source private database
  names, JDBC URLs, H2 paths, checkout paths, raw Git output or credentials.

Senior Tester:

- Tests must cover H2 rejection as runtime, PostgreSQL selection, storage
  readiness `DOWN`, public DTO redaction and deterministic H2 adapter fixture
  behavior.
- Docker-local checks must prove active runtime storage is PostgreSQL plus the
  private checkout workspace volume, not H2 files.

Dependency / Deadlock Validator:

- Slice dependencies are acyclic.
- Documentation alignment precedes verification slices.
- Backend persistence verification precedes API/UI and Docker closure checks.
- No slice writes product code. Any product mismatch stops and requires a new
  workflow or an explicitly approved implementation slice.

## Target Picture

```text
forensic-ui
  -> query-report-api-service public REST DTOs
    -> repository-source-service owner API
       -> repository_source PostgreSQL schema
       -> repository-source-workspaces private checkout volume
```

H2 exists only as direct adapter test and fixture storage:

```text
repository-source-service tests
  -> H2RepositorySourcePersistenceAdapter fixture
```

Runtime and Docker-local behavior must not use that H2 fixture path.

## Architecture Constraints

- Domain and application code remain independent from JDBC, PostgreSQL,
  Liquibase, H2, REST, gRPC and React implementation details.
- PostgreSQL tables are private repository-source persistence.
- Repository checkout bytes and source package bytes remain outside PostgreSQL
  in repository-source-owned storage.
- Other services access repository-source state only through owner APIs,
  public facade APIs, explicit contracts or owner-issued artifact references.
- Public DTOs may expose opaque workspace IDs, branch IDs, source snapshot IDs,
  relative source roots and sanitized diagnostics only.
- Public DTOs must not expose JDBC URLs, table names, H2 paths, checkout paths,
  raw stdout, raw stderr, credentials, tokens or private network details.
- Missing or unreachable PostgreSQL must not be masked by in-memory, H2 or
  file-based fallback storage.
- Historical H2 files are not active runtime state. Preserving them requires
  an explicit migration slice with verified input files, acceptance criteria,
  rollback strategy and quality gates.

## Backend Assessment

Verified backend areas:

- `repository-source-service/src/main/resources/application.properties`
- `repository-source-service/src/main/resources/application-docker.properties`
- `repository-source-service/src/main/resources/application-test.properties`
- `repository-source-service/src/main/resources/db/changelog/repository-source-workspace.postgresql.yaml`
- `repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceServiceProperties.java`
- `repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceServiceConfiguration.java`
- `repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceStorageReadiness.java`
- `repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/HealthHttpServerLifecycle.java`
- `repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/postgres`
- `repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/h2`
- `repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/RepositorySourceServiceApplicationTest.java`
- `repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/application/RepositorySourcePostgresPersistenceApplicationTest.java`
- `repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/application/RepositorySourceH2PersistenceApplicationTest.java`

Backend STOP conditions:

- H2 is selected as runtime or Docker persistence.
- Missing PostgreSQL is converted to an in-memory, H2 or file fallback.
- H2 tests assert runtime fallback, Docker startup behavior or production
  readiness behavior.
- JDBC, Liquibase, PostgreSQL or H2 types leak into domain or application
  boundaries.

## Public API And Frontend Assessment

Verified public API and UI areas:

- `query-report-api-service/README.md`
- `query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/application/QueryReportApiWorkspaceService.java`
- `query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/application/port/RepositoryWorkspaceOwnerPort.java`
- `query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/adapter/out/grpc`
- `query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/adapter/in/http`
- `forensic-ui/README.md`
- `forensic-ui/src/domain/workspace.ts`
- `forensic-ui/src/adapters/api/dtos.ts`
- `forensic-ui/src/adapters/api/mappers.ts`
- `forensic-ui/src/adapters/api/apiClient.ts`
- `forensic-ui/src/pages/workspaces/CreateWorkspacePage.tsx`

Public API and frontend STOP conditions:

- A public DTO exposes repository-source PostgreSQL table names, JDBC URLs,
  H2 paths, checkout paths, raw Git output, credentials or tokens.
- `query-report-api-service` reads repository-source PostgreSQL tables, H2
  files or private checkout directories directly.
- The UI calls repository-source-service, Git remotes, gRPC, WebSocket,
  SSE or gRPC-Web directly for this flow.

## Deployment Assessment

Verified deployment areas:

- `repository-source-service/Dockerfile`
- `deployment/docker-compose/services/repository-source-service.compose.yml`
- `deployment/docker-compose/repository-to-btm.local.yml`
- `repository-source-service/README.md`

Current Docker-local repository-source evidence:

- `repository-source-service` is configured with
  `--forensics.repository-source.service.persistence.type=postgres`.
- PostgreSQL connection settings are provided through
  `FORENSICS_REPOSITORY_SOURCE_POSTGRES_*`.
- The active service mount is
  `repository-source-workspaces:/var/lib/forensic-analytics/repository-workspaces`.
- No active H2 data volume is mounted into `repository-source-service` in the
  verified Compose descriptors.

Deployment STOP conditions:

- Docker-local config mounts repository-source H2 files as active runtime
  persistence.
- Docker-local config masks PostgreSQL failure with H2, memory or file fallback.
- Another service mounts repository-source workspaces, PostgreSQL data, H2
  files or private repository-source paths.
- Docker Compose model validation is treated as proof of image startup,
  health-check smoke tests, Swarm readiness or Kubernetes readiness.

## Test Strategy

Run narrow checks first, then broader quality gates when product code or tests
change in a future workflow execution.

Targeted documentation checks:

```bash
git diff --check
rg -n "Docker-local MVP H2|H2 data volume|H2 data volumes|H2 volumes|repository-source-data H2|service-local H2 file persistence|H2 is Docker-local" docs/architecture docs/arc42 repository-source-service/README.md query-report-api-service/README.md
```

Targeted backend checks:

```bash
./gradlew :repository-source-service:test --tests "*RepositorySourceServiceApplicationTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :repository-source-service:test --tests "*RepositorySourcePostgresPersistenceApplicationTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :repository-source-service:test --tests "*RepositorySourceH2PersistenceApplicationTest" --dependency-verification strict --console=plain --stacktrace
```

Targeted public API checks:

```bash
./gradlew :query-report-api-service:test --tests "*QueryReportApiWorkspaceServiceTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :query-report-api-service:test --tests "*QueryReportApiWorkspaceTest" --dependency-verification strict --console=plain --stacktrace
```

Targeted frontend checks:

```bash
cd forensic-ui && npm run test -- src/adapters/api/mappers.test.ts src/adapters/api/apiClient.test.ts src/pages/workspaces/CreateWorkspacePage.test.tsx
cd forensic-ui && npm run build
```

Minimum repository quality command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Docker-local model checks when deployment documentation or Compose files change:

```bash
docker compose -f deployment/docker-compose/repository-to-btm.local.yml config
docker compose -f deployment/docker-compose/services/repository-source-service.compose.yml config
```

## Resilience And Readiness Requirements

- PostgreSQL startup failure must be visible as startup failure or storage
  readiness `DOWN`.
- Storage readiness `DOWN` must not be hidden by in-memory, H2 or file fallback.
- Public diagnostics must be sanitized before leaving repository-source or
  query-report boundaries.
- Repository metadata and checkout operations must remain idempotent where the
  existing public contracts require idempotency keys.
- Docker-local Compose checks do not prove container startup, health, Swarm or
  Kubernetes readiness unless those commands are executed and recorded.

## Ordered Slices

### Slice 01 - Requirement And ADR Alignment Documentation

Purpose: Correct FA-MVP-0001 workflow documentation so PostgreSQL is the only
runtime metadata persistence target and H2 is test/fixture scope only.

```yaml
slice_id: S01
profile: FULL_PATH
owner: Senior Requirement Engineer
secondary_reviewers:
  - Senior System Architect
  - Senior Documentation Engineer
  - Senior Tester
affected_files:
  - docs/workflow/**
  - docs/architecture/data-ownership.md
  - docs/architecture/service-boundaries.md
  - docs/arc42/**
affected_modules: []
affected_contracts: []
dependencies: []
parallel_group: G1
file_locks:
  - docs/workflow/**
  - docs/architecture/data-ownership.md
  - docs/architecture/service-boundaries.md
  - docs/arc42/**
contract_locks: []
architecture_locks:
  - ADR-0023 test-only H2 scope
  - ADR-0024 repository-source PostgreSQL metadata ownership
quality_gates:
  targeted:
    - git diff --check
  required:
    - git diff --check
documentation:
  arc42: checked/update stale H2 runtime wording
  adr: ADR-0023 and ADR-0024 referenced
stop_conditions:
  - workflow requires H2 runtime or Docker persistence
  - workflow weakens ADR-0023 or ADR-0024
  - service ownership becomes ambiguous
```

Done criteria:

- Workflow artifacts say PostgreSQL is the runtime and Docker persistence
  target for repository-source metadata.
- H2 is allowed only for deterministic adapter tests and direct fixtures.
- Acceptance criteria include the ten user-confirmed PostgreSQL/H2 boundary
  checks.

### Slice 02 - Repository-Source Persistence Boundary Verification

Purpose: Verify repository-source runtime persistence, storage readiness and
test-only H2 boundaries without product code edits.

```yaml
slice_id: S02
profile: FULL_PATH
owner: Senior Java Backend Developer
secondary_reviewers:
  - Senior System Architect
  - Senior Tester
affected_files:
  - repository-source-service/src/main/resources/application.properties
  - repository-source-service/src/main/resources/application-docker.properties
  - repository-source-service/src/main/resources/application-test.properties
  - repository-source-service/src/main/resources/db/changelog/repository-source-workspace.postgresql.yaml
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/**
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/postgres/**
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/h2/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/bootstrap/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/application/RepositorySourcePostgresPersistenceApplicationTest.java
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/application/RepositorySourceH2PersistenceApplicationTest.java
affected_modules:
  - repository-source-service
affected_contracts: []
dependencies:
  - S01
parallel_group: G2
file_locks:
  - repository-source-service/src/main/resources/application*.properties
  - repository-source-service/src/main/resources/db/changelog/repository-source-workspace.postgresql.yaml
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/**
  - repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/**
contract_locks: []
architecture_locks:
  - repository-source-service owns repository_source PostgreSQL schema
  - H2 direct adapter fixtures only
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --tests "*RepositorySourceServiceApplicationTest" --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :repository-source-service:test --tests "*RepositorySourcePostgresPersistenceApplicationTest" --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :repository-source-service:test --tests "*RepositorySourceH2PersistenceApplicationTest" --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: persistence and readiness checked
  adr: ADR-0023 and ADR-0024 apply
stop_conditions:
  - H2 is accepted as runtime persistence type
  - PostgreSQL failure is hidden by fallback storage
  - H2 tests assert runtime or Docker fallback behavior
  - PostgreSQL table or JDBC details leak into domain/application code
```

Done criteria:

- Tests prove H2 is rejected as runtime persistence.
- Tests prove PostgreSQL selection and storage readiness behavior.
- H2 adapter tests remain direct fixture tests only.

### Slice 03 - Public API And Frontend Boundary Verification

Purpose: Verify query-report and UI consume only owner APIs and sanitized DTOs.

```yaml
slice_id: S03
profile: FULL_PATH
owner: Senior React Frontend Developer
secondary_reviewers:
  - Senior Java Backend Developer
  - Senior UX Designer
  - Senior Tester
affected_files:
  - query-report-api-service/README.md
  - query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/**
  - query-report-api-service/src/test/java/de/burger/forensics/analytics/services/queryreportapi/**
  - forensic-ui/README.md
  - forensic-ui/src/domain/workspace.ts
  - forensic-ui/src/adapters/api/**
  - forensic-ui/src/pages/workspaces/**
affected_modules:
  - query-report-api-service
  - forensic-ui
affected_contracts:
  - contracts/openapi/gateway-api.yaml
  - contracts/grpc/repository-analysis.proto
dependencies:
  - S02
parallel_group: G3
file_locks:
  - query-report-api-service/**
  - forensic-ui/**
contract_locks:
  - workspace public REST DTOs
  - repository-source owner gRPC workspace API
architecture_locks:
  - query-report facade only
  - UI consumes public REST only
quality_gates:
  targeted:
    - ./gradlew :query-report-api-service:test --tests "*QueryReportApiWorkspaceServiceTest" --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :query-report-api-service:test --tests "*QueryReportApiWorkspaceTest" --dependency-verification strict --console=plain --stacktrace
    - cd forensic-ui && npm run test -- src/adapters/api/mappers.test.ts src/adapters/api/apiClient.test.ts src/pages/workspaces/CreateWorkspacePage.test.tsx
  required:
    - ./gradlew :query-report-api-service:test --dependency-verification strict --console=plain --stacktrace
    - cd forensic-ui && npm run test
documentation:
  arc42: public facade and UI boundary checked
  adr: ADR-0010 and ADR-0024 apply
stop_conditions:
  - query-report reads repository-source private persistence directly
  - UI calls repository-source internals directly
  - public DTO exposes database, H2, filesystem, raw Git or credential details
```

Done criteria:

- Public API and UI tests prove only sanitized public DTOs cross the boundary.
- No public API or UI code depends on repository-source private persistence.

### Slice 04 - Docker-Local PostgreSQL And Volume Boundary Verification

Purpose: Verify Docker-local descriptors keep PostgreSQL as active metadata
storage and do not mount H2 files as active runtime storage.

```yaml
slice_id: S04
profile: FULL_PATH
owner: Senior DevOps Engineer
secondary_reviewers:
  - Senior System Architect
  - Senior Security Sandbox Engineer
  - Senior Tester
affected_files:
  - repository-source-service/Dockerfile
  - deployment/docker-compose/services/repository-source-service.compose.yml
  - deployment/docker-compose/repository-to-btm.local.yml
  - repository-source-service/README.md
  - docs/arc42/07-deployment-view.md
affected_modules:
  - repository-source-service
affected_contracts: []
dependencies:
  - S02
parallel_group: G3
file_locks:
  - repository-source-service/Dockerfile
  - deployment/docker-compose/**
  - repository-source-service/README.md
  - docs/arc42/07-deployment-view.md
contract_locks: []
architecture_locks:
  - repository-source-workspaces private volume
  - repository-source PostgreSQL metadata schema
quality_gates:
  targeted:
    - docker compose -f deployment/docker-compose/repository-to-btm.local.yml config
    - docker compose -f deployment/docker-compose/services/repository-source-service.compose.yml config
  required:
    - git diff --check
documentation:
  arc42: deployment view checked
  adr: ADR-0024 applies
stop_conditions:
  - repository-source H2 data files are mounted as active runtime storage
  - another service mounts repository-source private workspaces or persistence
  - PostgreSQL failure is masked by fallback storage
  - Compose config is claimed as startup or health readiness without execution evidence
```

Done criteria:

- Compose config shows PostgreSQL settings and no active H2 data mount.
- Documentation distinguishes Compose model validation from runtime startup,
  health, Swarm and Kubernetes readiness.

### Slice 05 - Final Quality, Arc42 And Handoff Closure

Purpose: Close the workflow with checked workflow docs, checked/updated arc42,
diff inspection and handoff instructions for future `workflow execute`.

```yaml
slice_id: S05
profile: FULL_PATH
owner: Senior Tester
secondary_reviewers:
  - Senior Documentation Engineer
  - Senior System Architect
affected_files:
  - docs/workflow/**
  - docs/arc42/**
  - docs/architecture/**
affected_modules: []
affected_contracts: []
dependencies:
  - S01
  - S02
  - S03
  - S04
parallel_group: G4
file_locks:
  - docs/workflow/**
  - docs/arc42/**
  - docs/architecture/**
contract_locks: []
architecture_locks:
  - FA-MVP-0001 PostgreSQL runtime persistence
  - H2 test-only fixture scope
quality_gates:
  targeted:
    - git diff --check
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: checked/updated
  adr: ADR-0023 and ADR-0024 checked
stop_conditions:
  - workflow metadata is missing or invalid
  - arc42 contradicts ADR-0023 or ADR-0024
  - quality command cannot be verified from QUALITY.md
  - final git status includes unrelated changes
```

Done criteria:

- `docs/workflow/workflow.md` and context-pack artifacts are complete.
- arc42 and architecture documentation no longer describe H2 as active runtime
  or Docker persistence.
- Diff is documentation-only unless a future `workflow execute` explicitly
  approves product implementation.

## Slice Dependency Summary

```text
S01
  -> S02
      -> S03
      -> S04
          -> S05
```

S03 and S04 may be reviewed in parallel after S02 because their write scopes are
disjoint except for documentation closure handled by S05.

## Role Ownership Map

| Area | Primary Owner | Secondary Review |
|---|---|---|
| Requirement correction | Senior Requirement Engineer | Senior System Architect, Senior Tester |
| Workflow structure | Senior Workflow Architect | Senior Documentation Engineer |
| Repository-source persistence | Senior Java Backend Developer | Senior System Architect, Senior Tester |
| Public REST and UI DTOs | Senior React Frontend Developer | Senior Java Backend Developer, Senior UX Designer |
| Docker-local deployment | Senior DevOps Engineer | Senior Security Sandbox Engineer, Senior Tester |
| Quality closure | Senior Tester | Senior Documentation Engineer |

Callable subagents were not used during workflow creation because the user did
not explicitly request delegated or parallel agent work. The role files were
applied as local review checklists.

## Acceptance Criteria

1. FA-MVP-0001 no longer requires H2 runtime or Docker persistence.
2. Runtime repository-source metadata persistence uses PostgreSQL.
3. H2 is allowed only for deterministic adapter tests and direct fixtures.
4. Docker-local deployments do not mount H2 files as active runtime storage.
5. No service except `repository-source-service` may access repository-source
   persistence directly.
6. Other services may consume only owner APIs and sanitized public DTOs.
7. PostgreSQL startup/readiness failure is visible and must not be hidden by
   fallback storage.
8. Existing H2 files are treated as historical MVP data and require an explicit
   migration slice if preservation is needed.
9. The workflow references ADR-0023, ADR-0024, data ownership documentation,
   service-boundary documentation and the relevant README/test expectations.
10. `git status --short` remains clean except intended workflow and
    documentation artifacts.

## Documentation Synchronization Points

- `docs/workflow/**` is regenerated for the corrected workflow.
- `docs/arc42/**` is checked and stale H2 runtime wording is corrected where
  required.
- `docs/architecture/data-ownership.md` is checked as already aligned with
  ADR-0024.
- `docs/architecture/service-boundaries.md` is checked and stale H2 runtime
  wording is corrected where required.
- ADR-0023 and ADR-0024 remain unchanged by this workflow-create run unless a
  separate architecture decision request is made.

## Commit And Push Plan

This `workflow create` turn may leave documentation changes unstaged for user
review. It must not commit, push, create a PR or run `push auto` unless the user
explicitly requests that publication action.

If later requested, commit preparation must stage only the intended workflow and
documentation artifacts and run `git diff --cached --check` before commit.

## Stop Conditions

Stop and report if:

- the active branch is not
  `feature/workflow-repository-workspace-checkout-20260604`;
- unrelated local changes appear;
- workflow artifacts require H2 runtime, Docker persistence or fallback;
- ADR-0023 or ADR-0024 would need to be weakened;
- product source changes are needed to satisfy documentation acceptance;
- a Gradle, npm or Docker command cannot be verified;
- a public DTO or documentation path would expose private storage details;
- preserving historical H2 files becomes a requirement without a migration
  slice.

## Definition Of Done

- Corrected workflow artifacts exist under `docs/workflow`.
- Workflow context pack records branch, process strand, execution profile,
  affected areas, forbidden areas, roles, quality commands and governing file
  hashes.
- PostgreSQL is documented as the runtime and Docker metadata persistence target
  for repository-source workspace metadata.
- H2 is documented as deterministic adapter test/direct fixture scope only.
- Service ownership and direct access constraints are explicit.
- Quality gates are traceable to `QUALITY.md`, Gradle modules and the frontend
  `package.json`.
- arc42 and architecture documentation have been checked or updated for the H2
  to PostgreSQL correction.

## Handoff To Workflow Execute

`workflow execute` may execute the slices above only after reading this complete
workflow and verifying slice metadata. Execution must not implement product
code unless the workflow is explicitly amended through `workflow create` or the
user approves a new implementation workflow.
