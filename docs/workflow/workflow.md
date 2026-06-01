# Workflow: Repository Workspace Branch Selection And Refresh

## Executive Summary

This workflow plans repository workspace branch discovery, selection and refresh
behavior across the repository-source backend, public contracts and React UI.
The target is a workspace view where all verified remote branches are displayed
in a `Branches` combobox, the selected branch is persisted as
`workspace_branch`, and branch refresh is an explicit operator action.

The workflow does not implement product code. It defines executable slices for
`workflow execute`.

## Verified Baseline

- Active branch: `feature/workflow-workspace-branch-selection-20260601`
- Workflow version: `2026-06-01`
- Process strand: `workflow create`
- Execution profile: `FULL_PATH`
- Repository root: `/mnt/d/Projects/forensic_analytics`
- Owner service: `repository-source-service`
- Existing frontend module: `forensic-ui`
- Existing repository workspace contract: `contracts/grpc/repository-analysis.proto`
- Existing public REST contract: `contracts/openapi/gateway-api.yaml`
- Existing persistence decision: ADR-0024 PostgreSQL for repository-source workspace metadata
- Quality source: `QUALITY.md`

## Interpreted Intent

The request is interpreted as:

- Read all remote repository branches during workspace metadata resolution.
- Show those branches in the GUI combobox named `Branches`.
- Persist the selected branch as the active `workspace_branch`.
- Treat the selected branch as the branch used by subsequent analysis.
- Do not update branch content from the remote merely because a branch was
  selected.
- A selection may only update metadata/status, for example a red
  not-up-to-date indicator; it must not fetch, checkout or replace local branch
  content.
- Mark selected branches that are not up to date with a red status indicator.
- Refresh a branch only through an explicit `Update Branch` action.
- Before refresh, verify that the remote branch still exists.
- If the remote branch no longer exists, warn the operator before destructive
  cleanup of referenced branch data.
- Add a visible TBD note for the future stale-analysis-data requirement.
- Treat workspace deletion as logical deletion first so accidental deletion can
  be recovered through a future trash/final-delete workflow.

## Requirement Clarification Gate

Decision: `PROCEED_WITH_ACCEPTED_ASSUMPTIONS`

Confidence: 86 percent.

Accepted assumptions:

- `repository-source-service` remains the owner of workspace branch metadata.
- The public UI should use existing workspace API routes rather than direct Git
  or database access.
- The active analysis branch is represented by the selected persisted
  `workspace_branch`.
- A red not-up-to-date indicator can be represented by an explicit backend
  branch status and frontend status styling.
- Destructive branch cleanup after a missing remote branch requires an explicit
  confirmation API and is a separate slice from safe detection.
- Workspace trash/final-delete requires contract and persistence semantics and
  must not be hidden inside the existing cleanup endpoint.

Non-blocking open questions for `workflow execute`:

- Exact wording of the destructive confirmation warning.
- Whether final delete is operator-only or available to all UI users.
- Which analysis stores must be cleaned once stale-analysis tracking exists.

## Scope

- Repository-source metadata resolution for remote branch lists.
- gRPC and OpenAPI contract updates for branch lists and refresh/delete
  semantics.
- Repository-source application behavior for branch selection, status and
  explicit refresh.
- Repository-source persistence changes only when required by status, trash or
  confirmation state.
- React UI combobox, selected-branch state, branch list and actions.
- Tests for backend, contracts, frontend mapping and UI behavior.
- Documentation of deferred stale-analysis-data handling.

## Non-Goals

- No automatic analysis execution.
- No guessing runtime execution facts from branch selection.
- No direct UI access to Git remotes, PostgreSQL or repository-source private
  tables.
- No hidden fallback branch if the selected branch cannot be verified.
- No deletion of analysis artifacts before the owning analysis store contract is
  verified.
- No automatic remote update during branch selection.
- No `docker compose down -v` or destructive local volume reset.

## Target Picture

```text
forensic-ui
  -> public workspace API
     -> repository-source-service owner API
        -> Git metadata adapter: ls remote branches
        -> repository workspace application
        -> workspace_branch metadata
        -> explicit branch refresh
        -> logical workspace delete / trash state
```

Remote branch names are data only. They must never be used directly as local
paths or evidence of runtime execution.

## Architecture Constraints

- Domain and application code remain independent from Git, PostgreSQL, REST,
  gRPC and React implementation details.
- Git operations stay in outbound adapters.
- Public DTOs expose sanitized branch names and opaque workspace IDs only.
- Branch selection must not fetch, checkout or mutate workspace bytes.
- Branch status checks may contact the remote for metadata only when the
  implementation can prove that no local checkout or workspace bytes are
  updated as a side effect.
- Branch refresh through `Update Branch` may fetch only after the target remote
  branch is verified.
- Missing remote branches must be represented explicitly; they must not be
  silently mapped to a similarly named branch.
- Workspace deletion must preserve recoverability until final-delete semantics
  are explicitly executed.

## Backend Assessment

Backend work is centered on `repository-source-service`.

Expected changes:

- Extend metadata resolution with a deterministic list of remote branches.
- Persist selected branch as a `RepositoryWorkspaceBranch`.
- Add or verify branch statuses for selected, stale, missing-remote and updated
  states.
- Keep branch selection separate from checkout/refresh.
- Add a remote-existence check before refresh.
- Add an explicit confirmation path before deleting branch references after a
  missing remote branch.
- Extend cleanup semantics to logical deletion/trash before final deletion.

## Frontend Assessment

Frontend work is centered on `forensic-ui`.

Expected changes:

- Map branch-list metadata from the public API.
- Render `Branches` as a combobox.
- Store the selected branch in local UI state until workspace save.
- Show persisted branches below the view in a list.
- Add `Update Branch` action for each persisted branch.
- Show a red status indicator for not-up-to-date or missing-remote states.
- Add the TBD note for future stale-analysis-data warnings.
- Add workspace trash/final-delete affordances only after backend contract
  support exists.

## Test Strategy

- Backend unit tests for metadata branch-list parsing and sanitization.
- Backend application tests for branch selection without remote update.
- Backend application tests for missing-remote refresh warning and confirmation
  behavior.
- Persistence tests for any new status or logical-delete fields.
- gRPC contract tests for field numbers and compatibility.
- REST/OpenAPI DTO mapping tests.
- React component tests for combobox, branch list, red status and action
  enablement.
- End-to-end or smoke checks only after targeted unit/component checks pass.

## Quality Gates

Minimum required command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Targeted frontend commands:

```bash
npm test -- --run src/pages/workspaces/CreateWorkspacePage.test.tsx src/pages/workspaces/WorkspaceListPage.test.tsx src/adapters/api/mappers.test.ts
npm run build
```

Targeted backend command:

```bash
./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate before publication when practical:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Ordered Slices

### Slice 01 - Branch Metadata Contract

Purpose: expose verified remote branch names through owner and public contracts.

```yaml
slice_id: S01_BRANCH_METADATA_CONTRACT
profile: FULL_PATH
owner: Senior Java Backend Developer
secondary_reviewers:
  - Contract-First API Steward
  - Senior Tester
affected_files:
  - contracts/grpc/repository-analysis.proto
  - contracts/openapi/gateway-api.yaml
  - repository-source-service/src/main/java/**
  - repository-source-service/src/test/java/**
affected_modules:
  - repository-source-service
affected_contracts:
  - repository-analysis.proto
  - gateway-api.yaml
dependencies: []
parallel_group: P1
file_locks:
  - contracts/grpc/repository-analysis.proto
  - contracts/openapi/gateway-api.yaml
contract_locks:
  - repository workspace metadata response
architecture_locks:
  - repository-source owner API
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: check
  adr: not-required
stop_conditions:
  - remote branch contract cannot be added compatibly
  - branch names cannot be sanitized as data-only values
```

Done criteria:

- Metadata responses contain deterministic remote branch lists.
- Contract tests pin new field numbers.
- Missing branch-list metadata is represented as an empty list, not guessed.

### Slice 02 - Branch Selection And Status Semantics

Purpose: persist the selected branch as `workspace_branch` without remote
refresh and mark stale/not-up-to-date state explicitly.

```yaml
slice_id: S02_BRANCH_SELECTION_STATUS
profile: FULL_PATH
owner: Senior Java Backend Developer
secondary_reviewers:
  - Senior System Architect
  - Senior Tester
affected_files:
  - repository-source-service/src/main/java/**
  - repository-source-service/src/test/java/**
  - repository-source-service/src/main/resources/db/**
affected_modules:
  - repository-source-service
affected_contracts:
  - repository workspace branch status
dependencies:
  - S01_BRANCH_METADATA_CONTRACT
parallel_group: P2
file_locks:
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/**
  - repository-source-service/src/main/resources/db/**
contract_locks:
  - workspace_branch state semantics
architecture_locks:
  - hexagonal repository-source boundary
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: check
  adr: check-if-persistence-status-added
stop_conditions:
  - selecting a branch would fetch remote content
  - active analysis branch cannot be traced to workspace_branch
```

Done criteria:

- Selecting a branch creates or selects a `workspace_branch`.
- No remote fetch happens during selection.
- The active branch for later analysis is unambiguous.
- Not-up-to-date state is explicit and visible to the frontend.

### Slice 03 - Frontend Branch Combobox And Branch List

Purpose: show all branches in `Branches`, persist selection, render branch list
and add `Update Branch` actions plus TBD stale-analysis note.

```yaml
slice_id: S03_FRONTEND_BRANCH_UI
profile: FULL_PATH
owner: Senior React Frontend Developer
secondary_reviewers:
  - Senior UX Designer
  - Senior Tester
affected_files:
  - forensic-ui/src/domain/workspace.ts
  - forensic-ui/src/adapters/api/**
  - forensic-ui/src/pages/workspaces/**
  - forensic-ui/src/styles.css
affected_modules:
  - forensic-ui
affected_contracts:
  - gateway-api.yaml
dependencies:
  - S01_BRANCH_METADATA_CONTRACT
  - S02_BRANCH_SELECTION_STATUS
parallel_group: P3
file_locks:
  - forensic-ui/src/domain/workspace.ts
  - forensic-ui/src/adapters/api/**
  - forensic-ui/src/pages/workspaces/**
contract_locks:
  - workspace metadata REST DTO
architecture_locks:
  - frontend adapter boundary
quality_gates:
  targeted:
    - npm test -- --run src/pages/workspaces/CreateWorkspacePage.test.tsx src/pages/workspaces/WorkspaceListPage.test.tsx src/adapters/api/mappers.test.ts
    - npm run build
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: not-required
  adr: not-required
stop_conditions:
  - frontend derives branch names locally
  - UI text claims analysis data is stale before backend evidence exists
```

Done criteria:

- `Branches` combobox is populated from API metadata.
- Selected branch is passed unchanged to workspace creation.
- Branch list renders persisted branches below the workspace view.
- `Update Branch` action is available only when backend state permits it.
- TBD stale-analysis note is visible and clearly non-final behavior.

### Slice 04 - Explicit Branch Update And Missing Remote Warning

Purpose: make branch refresh explicit and protect missing-remote destructive
cleanup behind a warning and confirmation contract.

```yaml
slice_id: S04_BRANCH_UPDATE_WARNING
profile: FULL_PATH
owner: Senior Java Backend Developer
secondary_reviewers:
  - Contract-First API Steward
  - Senior React Frontend Developer
  - Senior Tester
affected_files:
  - contracts/grpc/repository-analysis.proto
  - contracts/openapi/gateway-api.yaml
  - repository-source-service/src/main/java/**
  - repository-source-service/src/test/java/**
  - forensic-ui/src/pages/workspaces/**
affected_modules:
  - repository-source-service
  - forensic-ui
affected_contracts:
  - branch refresh
  - destructive cleanup confirmation
dependencies:
  - S02_BRANCH_SELECTION_STATUS
  - S03_FRONTEND_BRANCH_UI
parallel_group: P4
file_locks:
  - contracts/grpc/repository-analysis.proto
  - contracts/openapi/gateway-api.yaml
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/**
  - forensic-ui/src/pages/workspaces/**
contract_locks:
  - branch refresh warning and confirmation
architecture_locks:
  - no fabricated evidence
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
    - npm test -- --run src/pages/workspaces/WorkspaceListPage.test.tsx
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: check
  adr: check-if-destructive-confirmation-contract-added
stop_conditions:
  - missing remote branch is silently ignored
  - referenced data is deleted without explicit confirmation
  - analysis results are deleted without verified owner contract
```

Done criteria:

- `Update Branch` is the only action that loads remote branch content.
- Refresh checks remote branch existence before fetching.
- Missing remote branch returns a warning state.
- Destructive cleanup requires explicit confirmation.
- Existing branch data is preserved when confirmation is absent.

### Slice 05 - Workspace Trash And Final Delete Planning

Purpose: introduce recoverable workspace deletion and separate final deletion
from the current cleanup behavior.

```yaml
slice_id: S05_WORKSPACE_TRASH_DELETE
profile: FULL_PATH
owner: Senior Java Backend Developer
secondary_reviewers:
  - Data Ownership & Persistence Steward
  - Senior React Frontend Developer
  - Senior Tester
affected_files:
  - contracts/grpc/repository-analysis.proto
  - contracts/openapi/gateway-api.yaml
  - repository-source-service/src/main/java/**
  - repository-source-service/src/main/resources/db/**
  - repository-source-service/src/test/java/**
  - forensic-ui/src/pages/workspaces/**
affected_modules:
  - repository-source-service
  - forensic-ui
affected_contracts:
  - workspace cleanup
  - workspace trash
  - final delete
dependencies:
  - S04_BRANCH_UPDATE_WARNING
parallel_group: P5
file_locks:
  - repository-source-service/src/main/resources/db/**
  - repository-source-service/src/main/java/de/burger/forensics/analytics/services/repositorysource/**
  - forensic-ui/src/pages/workspaces/**
contract_locks:
  - workspace deletion lifecycle
architecture_locks:
  - data ownership and analysis-result cleanup ownership
quality_gates:
  targeted:
    - ./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
    - npm test -- --run src/pages/workspaces/WorkspaceListPage.test.tsx
  required:
    - ./gradlew test --dependency-verification strict --console=plain --stacktrace
documentation:
  arc42: update
  adr: required-if-persistence-lifecycle-changes
stop_conditions:
  - analysis result ownership cannot be verified
  - final delete would remove data outside repository-source ownership
  - restore semantics are unclear
```

Done criteria:

- Workspace deletion first marks a recoverable deleted/trash state.
- Final delete is a separate explicit operation.
- Analysis-result cleanup is deferred until each owner store contract is
  verified.
- UI distinguishes delete, restore and final delete.

## Dependency Summary

```text
S01_BRANCH_METADATA_CONTRACT
  -> S02_BRANCH_SELECTION_STATUS
      -> S03_FRONTEND_BRANCH_UI
          -> S04_BRANCH_UPDATE_WARNING
              -> S05_WORKSPACE_TRASH_DELETE
```

No slice is safely parallelizable because the contract and state semantics are
shared across backend and frontend.

## Role Ownership

- Senior Requirement Engineer: requirement traceability and open-question
  control.
- Senior System Architect: hexagonal boundaries and service ownership.
- Senior Java Backend Developer: repository-source implementation.
- Senior React Frontend Developer: UI state and API adapter integration.
- Senior UX Designer: branch status and warning flow.
- Senior Tester: regression and quality gate strategy.
- Contract-First API Steward: gRPC/OpenAPI changes.
- Data Ownership & Persistence Steward: trash/final-delete and analysis-result
  ownership.

## Stop Conditions

- Required contract, class, field, table or status cannot be verified.
- Any implementation would infer runtime execution from branch selection.
- Any cleanup would delete analysis data without verified owner contract.
- Remote branch existence cannot be checked deterministically.
- UI would hide missing remote or stale status.
- Quality commands from `QUALITY.md` fail.

## Definition Of Done

- All five slices complete or intentionally deferred with documented blockers.
- Targeted backend and frontend tests pass.
- Repository minimum quality gate passes.
- Contracts and tests agree on field numbers and DTO shapes.
- Branch selection, refresh and delete semantics are documented.
- No direct database, Git or workspace-path leakage reaches the UI.

## Handoff To Workflow Execute

Run `workflow execute` only after reviewing this workflow and confirming the
accepted assumptions remain valid. `workflow execute` must execute slices in
dependency order and stop on any missing contract, unverified owner, or failed
quality gate.
