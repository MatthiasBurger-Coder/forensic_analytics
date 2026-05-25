# Three Amigos Decision Record

## Decision

`PROCEED_WITH_ACCEPTED_ASSUMPTIONS`

Confidence: 74 percent after requirement-review dissent.

## Original Request

The user requested `workflow create with subagents` and described that it
should be possible to select current branches after a workspace from the list
has been selected. The provided screenshot showed the current `Selected branch`
UI as a text input containing `main`.

## Normalized Requirement

Create a workflow for changing the Workspaces list so branch selection is an
explicit operator choice after workspace selection. The selector must use
verified public branch records for the selected repository checkout workspace.

## Requirement Classification

- Functional requirement: select a branch after selecting a workspace.
- UX requirement: replace static or free-form branch presentation with a clear
  selector.
- Architecture constraint: branch options must come from public REST DTOs and
  not browser Git, query-report filesystem access or inferred local state.
- Quality requirement: regression tests must prove branch ID mapping, no-branch
  behavior and no invented options.
- Assumption: current branches means existing public `branches[]` branch
  records for a workspace.
- Open question: remote Git branch discovery may be a future feature but is not
  part of this workflow.

## Five-Role Review

### Senior Requirement Engineer

The business goal is clear enough for workflow creation under a documented
assumption: after a workspace is selected, branch choices come from that
workspace's current public branch records. Non-goals exclude remote branch
discovery and branch creation. EPIC alignment remains FA-MVP-0001 repository
checkout workspace behavior.

The requirement subagent recommended `REQUIRES_REFINEMENT` without explicit
answers to the current-branch, persistence, action-target and row-model
questions. This workflow records those concerns and converts them into the S01
semantic gate. Product implementation must stop if the UI-only `branches[]`
assumption is not valid.

### Senior System Architect

The architecture risk is the word "current". If it means public workspace
branch records, this is a frontend state and UX change. If it means remote Git
branches, there is no verified public branch-discovery contract and execution
must stop. Selected branch is UI intent, not forensic evidence. Repository
branch names remain data values only.

### Senior Java Backend Developer

Backend implementation is not needed for selecting among existing
`branches[]`. Existing OpenAPI and gRPC contracts expose branch names and opaque
branch IDs for list and refresh. Backend code must remain untouched unless S01
finds the contract is insufficient, in which case this workflow stops and a
contract-first workflow is required.

### Senior React Frontend Developer

The verified target is `forensic-ui/src/pages/workspaces/WorkspaceListPage.tsx`.
It currently flattens one row per branch and displays selected branch as static
text. The implementation should render one workspace row with an accessible
branch selector, keep selection keyed by `workspaceId`, refresh by
`workspaceBranchId` and show branch details next to the selector.

### Senior Tester

Tests must focus on `WorkspaceListPage.test.tsx`, with mapper/API regression
coverage only if needed. Required checks are the targeted Vitest command,
frontend build, repository minimum Gradle test command and full quality gate
before commit readiness. No mutation, lint, Playwright or Cypress command was
verified.

## Dependency And Deadlock Validation

The dependency chain is linear: S01 semantic and contract guard, S02 UI state,
S03 regression coverage, S04 quality and docs closure. Parallel execution is
not planned because the UI and test slices share frontend files.

## Accepted Assumptions

- `current branches` means public `workspace.branches[]` records returned by
  `GET /workspaces`.
- A selected branch is UI state until a backend refresh or checkout result
  reports branch status, commit and snapshot state.
- Refresh uses `WorkspaceBranch.workspaceBranchId`.
- The UI may default to the previous selected branch if still present,
  otherwise the first available branch record.
- Workspaces without branches remain visible and show branch selection as
  unavailable.

## Non-Goals

- No remote Git branch enumeration.
- No branch checkout creation from the list.
- No new OpenAPI or gRPC route.
- No persistence of selected branch UI state.
- No platform workspace administration model.
- No analysis pipeline, JavaParser, Joern, BTM, replay, graph, report, vector,
  LLM, plugin, Docker, CI or deployment changes.

## Open Questions

- Should a future workflow expose remote branch discovery for repositories?
- Should selecting a not-yet-created branch later create and check out a new
  workspace branch?

These questions are non-blocking because they are out of scope for this
workflow and covered by stop conditions.

## Blocking Questions

None for workflow creation under the accepted assumptions. Workflow execution
is blocked at S01 until these assumptions are re-verified against the user
request and repository state, and must stop if they are invalid.

## Final Decision

`PROCEED_WITH_ACCEPTED_ASSUMPTIONS`
