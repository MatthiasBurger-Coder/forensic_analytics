# Workflow: FA-MVP-0001-EXT-01 Workspaces Management View

## Workflow Version

| Field | Value |
|---|---|
| Workflow version | `fa-mvp-0001-workspaces-management-extension-20260525-v1` |
| Requirement ID | `FA-MVP-0001-EXT-01` |
| Title | Workspaces navigation, list, branch refresh and safe delete workflow |
| Workflow branch | `feature/workflow-workspaces-management-20260525` |
| Creation status | Created by `workflow create`; implementation requires `workflow execute`. |
| Process strand | `workflow execute`; workflow creation completed under `workflow create`. |
| Execution profile | `FULL_PATH` |
| Repository-source owner | `repository-source-service` |
| Public API owner | `query-report-api-service` |
| Frontend owner | `forensic-ui` |

## Executive Summary

This workflow extends the existing repository checkout workspace foundation with
a first operator-facing Workspaces management view. The operator should see a
primary `Workspaces` navigation entry, open a list of existing repository
checkout workspaces, inspect `Workspace_ID`, workspace label and selected
branch, create a new workspace, refresh the checked-out branch, and delete a
workspace from the active list.

The workflow uses the existing FA-MVP-0001 repository checkout workspace
meaning. It does not introduce the broader platform workspace administration
model, a new `workspace-service`, or any analysis execution. Repository-source
remains the owner of checkout workspace state and H2 persistence.
Query-report-api remains the sanitized public REST facade. The browser calls
only public REST.

Delete is planned as a safe cleanup lifecycle: private checkout files are
cleaned through repository-source-owned ports, persisted metadata/provenance is
retained and the public workspace state becomes `CLEANED`. Hard deletion of H2
workspace records is out of scope. The default public list hides cleaned
workspaces so the UI behavior matches operator expectation without losing
forensic provenance.

## Target Picture

```text
Operator
  -> forensic-ui /workspaces list
  -> query-report-api-service public REST
  -> repository-source owner API
  -> repository-source application use cases
  -> repository-source H2 workspace and branch records
  -> repository-source workspace filesystem cleanup/refresh ports
```

Target UI routes:

```text
/workspaces      -> Workspaces list
/workspaces/new  -> Create repository workspace
```

Target list row semantics:

```text
Workspace row = repository checkout workspace branch view

Workspace_ID     = existing opaque workspaceId
Workspace        = workspaceTitle, with repository key as fallback only if the
                   public DTO does not provide a title
Selected Branch  = branch.repositoryBranch from the public branches[] data
Actions          = Add workspace, update/fetch selected branch, delete workspace
```

When a workspace has multiple branches, the UI renders one row per branch. A
workspace without branches remains visible as a workspace row with selected
branch marked unavailable and branch-refresh disabled.

## Verified Baseline

Read-only workflow creation verification found:

- Repository root: `/mnt/d/Projects/forensic_analytics`.
- WSL is available and repository commands must use the WSL-mounted worktree.
- Dedicated workflow branch is active:
  `feature/workflow-workspaces-management-20260525`.
- Local branch ref is verified:
  `refs/heads/feature/workflow-workspaces-management-20260525`.
- Working tree was clean before workflow regeneration.
- The previous `docs/workflow/**` files targeted
  `feature/workflow-repository-workspace-checkout-h2-persistence-20260524`
  and have been regenerated for this workflow.
- Quality authority is `QUALITY.md`.
- Minimum quality command:
  `./gradlew test --dependency-verification strict --console=plain --stacktrace`.
- Full local quality gate:
  `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`.
- `forensic-ui/package.json` verifies frontend scripts `npm run test` and
  `npm run build`; `forensic-ui/package-lock.json` is present for `npm ci`.
- `forensic-ui/src/app/App.tsx` currently routes `/workspaces` to
  `CreateWorkspacePage`.
- `forensic-ui/src/pages/workspaces/WorkspaceListPage.tsx` already exists but
  is not the active `/workspaces` route.
- `forensic-ui/src/layouts/AppShell.tsx` currently labels the `/workspaces`
  navigation item `Create workspace`.
- `forensic-ui/src/application/ports/workspacePort.ts` already has
  `listWorkspaces`, but no delete command.
- `forensic-ui/src/adapters/api/apiClient.ts` currently implements
  `listWorkspaces` as an empty-list stub.
- `contracts/openapi/gateway-api.yaml` verifies public workspace metadata,
  create, get, checkout-result and branch-refresh routes, but not
  `GET /workspaces` or `DELETE /workspaces/{workspaceId}`.
- `contracts/grpc/repository-analysis.proto` verifies owner API cleanup,
  metadata preview, create, get and refresh operations, but no list operation.
- Existing cleanup requires `analysis_run_id`, so it is not a verified delete
  action for repository checkout workspace administration.
- `RepositoryWorkspaceRepository` supports `save`, `findById`,
  `findByRepositoryKey` and `findBranch`; it has no list or delete method.
- `RepositoryWorkspaceApplicationService` supports workspace metadata preview,
  create/reuse, get, checkout and branch refresh; no verified list/delete
  workflow exists.
- `H2RepositorySourcePersistenceAdapter` persists workspace and
  workspace-branch state and owns repository-source H2 access.

## Requirement Clarification Decision

| Field | Decision |
|---|---|
| Original request | Add `Workspaces` entry and view that lists existing workspaces with Workspace_ID, workspace and selected branch, with add, update/fetch branch and delete actions. |
| Interpreted intent | Extend the repository checkout workspace MVP with a management list and safe lifecycle actions. |
| Change type | Product feature extension with REST/gRPC contract, persistence lifecycle, frontend routing and quality impact. |
| Affected process strand | `workflow create` now; later `workflow execute`. |
| Affected architecture area | Repository-source ownership, query-report public facade, OpenAPI/gRPC contracts, React routing and list UX, H2-backed persistence state. |
| Explicit requirements | Primary Workspaces navigation, list existing workspaces, show Workspace_ID, workspace and selected branch, add workspace, update/fetch branch, delete workspace. |
| Accepted assumptions | Workspaces are repository checkout workspaces; `Workspace_ID` is existing `workspaceId`; workspace label is `workspaceTitle`; selected branch is each public branch row's `repositoryBranch`; list renders one row per branch; delete means cleanup and mark `CLEANED` while retaining metadata; default list hides cleaned workspaces. |
| Non-goals | No platform workspace membership/project administration, no new `workspace-service`, no JavaParser, Joern, BTM, replay, reports, graph, vector, LLM, plugin or deployment expansion. |
| Risks | Delete could be mistaken for hard evidence removal; selected branch could be misread as a top-level workspace field; unpaged lists may not scale; UI route changes may break existing tests; public DTOs must not leak private data. |
| Open questions | Pagination/filtering and showing cleaned workspaces are deferred MVP questions. |
| Blocking questions | None for workflow creation after the conservative assumptions above. Workflow execution must stop if the user rejects these assumptions or source/contracts contradict them. |
| Confidence | 84 percent. |
| Decision | `PROCEED_WITH_ACCEPTED_ASSUMPTIONS`. |

## Scope

In scope:

- Add a contract-first public workspace list route.
- Add a contract-first public workspace delete/cleanup route.
- Add repository-source owner API support for deterministic list and safe
  cleanup/mark-cleaned semantics.
- Extend repository-source workspace repository ports and H2/memory adapters
  without exposing H2 or filesystem paths outside repository-source.
- Extend query-report-api facade ports, service and HTTP handler for list and
  delete through owner APIs only.
- Replace the frontend empty-list stub with real public REST list integration.
- Add frontend delete command support only after the public delete contract is
  verified.
- Route `/workspaces` to the list view and keep `/workspaces/new` for create.
- Render a scan-friendly Workspaces list with `Workspace ID`, `Workspace`,
  `Selected branch`, status and actions.
- Reuse the existing branch refresh route for update/fetch when a branch ID is
  present.
- Add no-leak tests for list/delete public responses and UI rendering.
- Synchronize arc42 and workflow documentation for planned behavior only.

Out of scope:

- Hard-deleting repository-source H2 workspace or branch records.
- Introducing a platform workspace service or membership/project lifecycle.
- Running analysis, JavaParser, Joern, BTM generation, replay, reporting,
  graph projection, vector indexing or LLM context generation.
- Browser Git access, browser gRPC, direct internal service calls, local fake
  workspace storage, direct H2 reads or direct filesystem reads from the UI or
  query-report-api-service.
- Changing production database decisions, Docker topology, Swarm or Kubernetes
  readiness.
- Weakening ArchUnit, dependency verification, coverage or redaction rules.

## Architecture Constraints

- `repository-source-service` owns repository checkout workspace records,
  branch records, source snapshot references, H2 schema and private checkout
  directories.
- `query-report-api-service` owns public REST validation, public DTO mapping,
  correlation/idempotency headers, error redaction and owner API calls.
- `forensic-ui` owns UI state and calls public REST only.
- Public list and delete responses may expose only opaque workspace IDs,
  branch IDs, workspace titles, repository keys or names, branch names, public
  statuses, source snapshot IDs, relative source roots and sanitized
  diagnostics.
- H2 paths, private checkout paths, raw Git output, stdout/stderr, credentials,
  tokens and internal service details must not cross the public API boundary.
- Domain and application code must remain independent from HTTP, gRPC, H2,
  JDBC, filesystem and React concerns.
- Delete must be explicit cleanup/mark-cleaned behavior. A future hard-delete
  policy requires a separate requirement, data ownership review and migration
  or rollback strategy.
- List ordering must be deterministic for stable inputs.
- Branch refresh remains evidence-based: it updates a branch only from observed
  Git remote state and must not fabricate commits, source roots or snapshots.

## Backend Assessment

Backend impact is high. The current backend has verified create/get/refresh
workspace behavior but no list or delete workflow. Execution must introduce
contracts first, then repository-source list and cleanup behavior, then the
query-report facade. Every exact method, field, route and enum must be verified
before implementation. Existing cleanup that requires `analysis_run_id` must
not be reused as workspace deletion without a new verified contract.

## Frontend Assessment

Frontend impact is moderate to high. The React/Vite app already contains
workspace domain models, list hook and list page skeleton, but the active route
and API adapter do not expose the requested experience. Execution must activate
the list route, keep create at `/workspaces/new`, update the sidebar label to
`Workspaces`, wire real list/delete API calls, and add accessible async states
for refresh and delete. UI rows must use public API branch data rather than
derive a selected branch locally.

## Test Strategy

Targeted checks must run before wider gates:

```bash
cd /mnt/d/Projects/forensic_analytics
./gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:query-report-api-service:test --dependency-verification strict --console=plain --stacktrace
cd /mnt/d/Projects/forensic_analytics/forensic-ui
npm ci
npm run test
npm run build
```

Repository minimum gate:

```bash
cd /mnt/d/Projects/forensic_analytics
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
cd /mnt/d/Projects/forensic_analytics
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

No mutation-testing, frontend lint, Playwright or Cypress command was verified
for this workflow.

## Resilience Requirements

- List, refresh and delete requests must carry correlation IDs.
- Mutating public requests must keep idempotency semantics.
- Delete must reject or explicitly handle in-progress checkout/refresh states.
- Refresh and delete actions in the UI must disable duplicate submissions while
  in flight.
- Backend failures must return sanitized, retryable/non-retryable error
  envelopes without leaking private details.
- Repeated list calls must be deterministic for stable stored state.

## Ordered Slices

### Slice 01: Contract And Semantics Closure

Purpose: Define and verify public REST and owner API contracts for workspace
list and safe delete/cleanup before implementation.

Prerequisites: Active branch
`feature/workflow-workspaces-management-20260525`; regenerated workflow read in
full; accepted assumptions still valid.

```yaml
slice_id: S01
profile: FULL_PATH
owner: senior_system_architect
secondary_reviewers:
  - senior_requirement_engineer
  - senior_java_backend
  - senior_tester
affected_files:
  - contracts/openapi/gateway-api.yaml
  - contracts/grpc/repository-analysis.proto
  - contracts/openapi/README.md
  - contracts/grpc/README.md
  - services/query-report-api-service/src/test/java/de/burger/forensics/analytics/services/queryreportapi/adapter/in/http/GatewayOpenApiContractTest.java
  - services/repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/adapter/in/grpc/RepositorySourceContractTest.java
affected_modules:
  - contracts
  - services:query-report-api-service
  - services:repository-source-service
affected_contracts:
  - contracts/openapi/gateway-api.yaml
  - contracts/grpc/repository-analysis.proto
dependencies: []
parallel_group: G1
file_locks:
  - contracts/openapi/gateway-api.yaml
  - contracts/grpc/repository-analysis.proto
contract_locks:
  - public-workspaces-rest
  - repository-source-owner-api
architecture_locks:
  - repository-source-owns-workspace-state
  - query-report-public-facade-only
quality_gates:
  targeted:
    - ./gradlew :services:query-report-api-service:test --tests "*GatewayOpenApiContractTest" --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :services:repository-source-service:test --tests "*RepositorySourceContractTest" --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/workflow/arc42-check-status.md
  adr: docs/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md
stop_conditions:
  - Delete semantics cannot be expressed as cleanup plus retained metadata.
  - Selected branch cannot be represented from public branch data.
  - Contract changes would expose private paths or storage details.
  - Field numbering or OpenAPI compatibility cannot be verified.
```

Done criteria:

- OpenAPI documents `GET /workspaces` and the safe delete route.
- Owner API contract supports list and safe cleanup/delete semantics.
- Contract tests prove route shape, field stability and no hard-delete claim.
- Documentation states default list behavior for cleaned workspaces.

### Slice 02: Repository-Source List And Cleanup Lifecycle

Purpose: Add deterministic repository-source list behavior and safe workspace
cleanup lifecycle in application, memory adapter and H2 adapter.

Prerequisites: Slice S01 completed and contracts verified.

```yaml
slice_id: S02
profile: FULL_PATH
owner: senior_java_backend
secondary_reviewers:
  - senior_system_architect
  - senior_tester
  - senior_security_sandbox_engineer
affected_files:
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/application/RepositoryWorkspaceApplicationService.java
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/application/port/RepositoryWorkspaceRepository.java
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/memory/InMemoryRepositoryWorkspaceRepository.java
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out/h2/H2RepositorySourcePersistenceAdapter.java
  - services/repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/application/RepositorySourceApplicationServiceTest.java
  - services/repository-source-service/src/test/java/de/burger/forensics/analytics/services/repositorysource/application/RepositorySourceH2PersistenceApplicationTest.java
affected_modules:
  - services:repository-source-service
affected_contracts:
  - repository-source-owner-api
dependencies:
  - S01
parallel_group: G2
file_locks:
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/application
  - services/repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/adapter/out
contract_locks:
  - repository-source-owner-api
architecture_locks:
  - repository-source-owns-h2
  - domain-application-no-h2-dependency
quality_gates:
  targeted:
    - ./gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/workflow/arc42-check-status.md
  adr: docs/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md
stop_conditions:
  - Implementation would hard-delete H2 records.
  - List ordering is nondeterministic.
  - Cleanup would hide inconsistent or incomplete evidence instead of representing it.
  - Query-report or UI ownership leaks into repository-source domain/application.
```

Done criteria:

- Repository-source can list non-cleaned workspaces deterministically.
- Cleanup marks state as `CLEANED`, retains metadata and uses
  repository-source-owned filesystem cleanup ports.
- H2 and in-memory adapters implement the same observable repository behavior.
- Tests cover list ordering, cleaned-workspace exclusion, cleanup idempotency,
  in-progress rejection or explicit handling, and persistence reload behavior.

### Slice 03: Query-Report Public Facade

Purpose: Expose sanitized public list and delete routes through query-report
API without reading repository-source storage directly.

Prerequisites: S01 and S02 completed.

```yaml
slice_id: S03
profile: FULL_PATH
owner: senior_java_backend
secondary_reviewers:
  - senior_system_architect
  - senior_tester
  - security_reviewer
affected_files:
  - services/query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/application/port/RepositoryWorkspaceOwnerPort.java
  - services/query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/application/QueryReportApiWorkspaceService.java
  - services/query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/domain/QueryReportApiWorkspace.java
  - services/query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi/adapter/in/http/QueryReportApiHttpHandler.java
  - services/query-report-api-service/src/test/java/de/burger/forensics/analytics/services/queryreportapi/adapter/in/http/QueryReportApiHttpAdapterTest.java
  - services/query-report-api-service/src/test/java/de/burger/forensics/analytics/services/queryreportapi/domain/QueryReportApiWorkspaceTest.java
affected_modules:
  - services:query-report-api-service
affected_contracts:
  - public-workspaces-rest
  - repository-source-owner-api
dependencies:
  - S01
  - S02
parallel_group: G3
file_locks:
  - services/query-report-api-service/src/main/java/de/burger/forensics/analytics/services/queryreportapi
  - services/query-report-api-service/src/test/java/de/burger/forensics/analytics/services/queryreportapi
contract_locks:
  - public-workspaces-rest
architecture_locks:
  - query-report-no-h2-or-filesystem-access
quality_gates:
  targeted:
    - ./gradlew :services:query-report-api-service:test --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/workflow/arc42-check-status.md
  adr: docs/adr/ADR-0010-contract-first-rest-and-grpc.md
stop_conditions:
  - Query-report would read H2 tables or workspace directories directly.
  - Public DTOs expose private paths, H2 paths, raw output, credentials or tokens.
  - Delete lacks idempotency or correlation handling.
  - Error mapping would turn unknown cleanup state into confirmed deletion.
```

Done criteria:

- `GET /api/workspaces` returns sanitized public workspace list data.
- Delete route delegates through owner API and returns safe status/diagnostics.
- Tests cover route validation, correlation/idempotency headers, redaction and
  backend failure mapping.

### Slice 04: Frontend Workspace API Adapter

Purpose: Replace placeholder workspace list behavior with public REST list
integration and add delete command support behind verified contracts.

Prerequisites: S03 completed and public REST DTO shape verified.

```yaml
slice_id: S04
profile: NORMAL_PATH
owner: senior_react_frontend
secondary_reviewers:
  - senior_tester
  - security_reviewer
affected_files:
  - forensic-ui/src/application/ports/workspacePort.ts
  - forensic-ui/src/adapters/api/apiClient.ts
  - forensic-ui/src/adapters/api/dtos.ts
  - forensic-ui/src/adapters/api/mappers.ts
  - forensic-ui/src/adapters/api/apiClient.test.ts
  - forensic-ui/src/adapters/api/mappers.test.ts
affected_modules:
  - forensic-ui
affected_contracts:
  - public-workspaces-rest
dependencies:
  - S03
parallel_group: G4
file_locks:
  - forensic-ui/src/application/ports/workspacePort.ts
  - forensic-ui/src/adapters/api
contract_locks:
  - public-workspaces-rest
architecture_locks:
  - frontend-public-rest-only
quality_gates:
  targeted:
    - cd forensic-ui && npm run test
    - cd forensic-ui && npm run build
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/workflow/arc42-check-status.md
  adr: []
stop_conditions:
  - Adapter would fabricate workspace rows from local placeholder state.
  - Adapter would infer selected branch outside public DTO data.
  - Delete route or DTO shape is not verified.
```

Done criteria:

- `listWorkspaces` calls the verified public list route with correlation
  metadata.
- Delete command is exposed through `WorkspacePort` only if the public delete
  contract is verified.
- API tests prove list/delete paths, headers, mapping and safe diagnostics.

### Slice 05: Workspaces List UI And Actions

Purpose: Activate the Workspaces navigation/list experience and wire add,
refresh and delete actions.

Prerequisites: S04 completed.

```yaml
slice_id: S05
profile: NORMAL_PATH
owner: senior_react_frontend
secondary_reviewers:
  - senior_ux_designer
  - senior_tester
affected_files:
  - forensic-ui/src/app/App.tsx
  - forensic-ui/src/layouts/AppShell.tsx
  - forensic-ui/src/pages/workspaces/WorkspaceListPage.tsx
  - forensic-ui/src/pages/workspaces/CreateWorkspacePage.tsx
  - forensic-ui/src/app/App.test.tsx
  - forensic-ui/src/pages/workspaces/CreateWorkspacePage.test.tsx
  - forensic-ui/src/styles.css
affected_modules:
  - forensic-ui
affected_contracts:
  - public-workspaces-rest
dependencies:
  - S04
parallel_group: G5
file_locks:
  - forensic-ui/src/app
  - forensic-ui/src/layouts
  - forensic-ui/src/pages/workspaces
  - forensic-ui/src/styles.css
contract_locks:
  - public-workspaces-rest
architecture_locks:
  - frontend-state-separated-from-api-adapter
quality_gates:
  targeted:
    - cd forensic-ui && npm run test
    - cd forensic-ui && npm run build
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/workflow/arc42-check-status.md
  adr: []
stop_conditions:
  - UI uses a card-only summary that omits required list fields.
  - UI labels unverified branch data as selected.
  - Actions are not keyboard reachable or can double-submit.
  - Text overlaps, truncates required IDs without accessible labels, or hides statuses by color alone.
```

Done criteria:

- Sidebar shows `Workspaces`; `/workspaces` renders the list.
- `/workspaces/new` keeps the create flow.
- List rows show Workspace ID, workspace label, selected branch, status and
  actions.
- Add navigates to create; refresh uses existing branch refresh behavior;
  delete uses the verified public delete command.
- Loading, empty, stale, error, refresh-in-flight and delete-in-flight states
  are tested.

### Slice 06: Quality, Leakage And Documentation Closure

Purpose: Run final verification, inspect diffs, and synchronize workflow/arc42
documentation with the implemented result.

Prerequisites: S01 through S05 completed.

```yaml
slice_id: S06
profile: FULL_PATH
owner: senior_tester
secondary_reviewers:
  - senior_documentation_engineer
  - senior_system_architect
affected_files:
  - docs/workflow/execution-report.md
  - docs/workflow/arc42-check-status.md
  - docs/arc42/README.md
  - docs/arc42/06-runtime-view.md
  - docs/arc42/08-crosscutting-concepts.md
  - docs/arc42/10-quality-requirements.md
  - docs/arc42/11-risks-and-technical-debt.md
affected_modules:
  - docs
affected_contracts: []
dependencies:
  - S01
  - S02
  - S03
  - S04
  - S05
parallel_group: G6
file_locks:
  - docs/workflow
  - docs/arc42
contract_locks: []
architecture_locks:
  - documentation-matches-implemented-behavior
quality_gates:
  targeted:
    - cd forensic-ui && npm run test
    - cd forensic-ui && npm run build
    - ./gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace
    - ./gradlew :services:query-report-api-service:test --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: docs/workflow/arc42-check-status.md
  adr: []
stop_conditions:
  - Any quality gate fails and cannot be classified.
  - Diff contains unrelated changes.
  - Documentation describes planned behavior as implemented before verification.
  - Public list/delete responses leak private data.
```

Done criteria:

- Targeted frontend and backend checks pass.
- Full local quality gate passes or a documented blocker stops execution.
- `git diff --check` passes.
- Execution report records slice results, commands and residual risk.
- arc42 notes are synchronized with verified implementation only.

## Slice Dependency Graph

```text
S01 Contract And Semantics Closure
  -> S02 Repository-Source List And Cleanup Lifecycle
  -> S03 Query-Report Public Facade
  -> S04 Frontend Workspace API Adapter
  -> S05 Workspaces List UI And Actions
  -> S06 Quality, Leakage And Documentation Closure
```

## Parallelization Opportunities

The workflow is mostly sequential because list/delete contracts and delete
semantics affect every later slice. After S01, read-only UI preparation and
test planning may run in parallel, but write scopes must stay behind the
verified contracts. No production-code slice may bypass S01.

## Role Ownership Map

- Senior Requirement Engineer: requirement drift, accepted assumptions and
  EPIC alignment.
- Senior System Architect: contract-first sequencing, service ownership,
  evidence retention and architecture stop conditions.
- Senior Java Backend Developer: repository-source and query-report backend
  slices.
- Senior React Frontend Developer: frontend API adapter, routing and list UI.
- Senior UX Designer: list information hierarchy and accessible action states.
- Senior Tester: regression strategy, targeted gates, full quality gate and
  no-leak verification.
- Security reviewer or Senior Security Sandbox Engineer: remote/Git/path
  leakage and cleanup safety review.

## Documentation Synchronization Points

- S01 updates contract documentation if route/RPC names are introduced.
- S02 updates repository-source README only if operator-visible lifecycle
  semantics change.
- S03 updates query-report API README only if public routes change.
- S05 updates frontend README only if route usage changes.
- S06 updates arc42 sections only for behavior verified by executed slices.

## Stop Conditions

Stop workflow execution when any of these are true:

- The active branch is not `feature/workflow-workspaces-management-20260525`.
- `workspace` is reinterpreted as platform workspace administration.
- Delete semantics require hard-delete or evidence removal.
- Selected branch cannot be represented from public branch DTO data.
- Contract tests cannot verify new route/RPC names and field stability.
- Query-report-api-service or forensic-ui would read repository-source H2 files,
  private checkout paths, Git remotes, gRPC internals or raw output directly.
- Public responses or UI renderings expose H2 paths, private checkout paths,
  stdout/stderr, credentials, tokens or secrets.
- Any quality command fails and the failure cannot be attributed safely.
- Continuing would require guessing a method, field, route, task, contract,
  schema, graph label or evidence semantics.

## Uncertainty Escalation Rules

Automatic clarification or correction loops are capped at `maxRetries = 3`.
After the third unresolved attempt, stop and escalate to the Root Architect
path with the attempted loop, unresolved blocker, affected files and reason why
continuing would be unsafe.

## Commit And Push Plan

Workflow creation itself does not commit or push unless the user explicitly
requests it. During `workflow execute`, each completed slice may use the
repository's slice checkpoint push process only when the active workflow
execution rules allow it, after the slice quality gates pass and the diff is
inspected.

## Definition Of Done

- `docs/workflow/workflow.md` is regenerated on the dedicated workflow branch.
- Context pack files are regenerated.
- Slices include metadata, dependencies, locks, quality gates and stop
  conditions.
- arc42 check status records the architecture sections reviewed for the
  workflow.
- Callable subagents or role reviews are recorded.
- Workflow handoff is clear: implementation starts only after `workflow execute`.

## Handoff To Workflow Execute

This workflow is ready for `workflow execute` under the accepted assumptions
documented above. Before implementing S01, the executor must reread this file,
verify the active branch, run S3D dependency and lock checks, and stop if the
user contradicts the accepted assumptions.

## arc42 Check Status

See `docs/workflow/arc42-check-status.md`. The workflow creation check found
no need to update implemented behavior claims in arc42 at creation time. S06
must update arc42 after implementation verifies list/delete behavior.
