# Three Amigos Decision Record

## Decision

`PROCEED_WITH_ACCEPTED_ASSUMPTIONS`

Confidence: 84 percent.

## Original Request

The operator console should gain a `Workspaces` entry. The view should list
already-created workspaces with `Workspace_ID`, workspace and selected branch.
From that view, the operator can add a workspace, update/fetch the branch and
delete a workspace.

## Normalized Requirement

Create a workflow for extending FA-MVP-0001 repository checkout workspaces with
a management list and safe lifecycle actions. The implementation must be
contract-first and must preserve repository-source data ownership and forensic
provenance.

## Accepted Assumptions

- `Workspaces` means repository checkout workspaces, not platform workspace
  membership or project administration.
- `Workspace_ID` maps to existing public `workspaceId`.
- `workspace` displays public `workspaceTitle`; repository key is a fallback
  only when the public DTO lacks a title.
- `Selected branch` displays `branch.repositoryBranch` from the public
  `branches[]` DTO.
- The list renders one row per workspace branch.
- Delete means cleanup and mark `CLEANED` while retaining persisted metadata.
- The default list hides cleaned workspaces.
- Add workspace reuses the existing create workspace flow at `/workspaces/new`.
- Update/fetch branch reuses existing branch refresh behavior when the branch
  ID is present.

## Non-Goals

- No broader platform workspace lifecycle.
- No new `workspace-service`.
- No hard deletion of repository-source H2 records.
- No JavaParser, Joern, BTM, replay, graph, report, vector, LLM, plugin or
  deployment expansion.
- No browser Git, browser gRPC or direct internal service access.

## Role Findings

### Senior Requirement Engineer

The requested feature extends FA-MVP-0001, whose current scope is repository
checkout workspace behavior. The main requirement risks are delete semantics,
multi-branch display semantics and route drift. These are addressed through the
accepted assumptions and contract-first S01.

### Senior System Architect

Repository-source must remain the owner of checkout workspace state, H2 schema
and private directories. Query-report-api-service remains a public facade only.
The UI must call public REST only. Delete must be cleanup/mark-cleaned behavior
unless a future requirement authorizes hard deletion with data ownership review.

### Senior Java Backend Developer

Verified backend support exists for metadata preview, create/get, checkout
wait and branch refresh. Verified list/delete support does not exist. S01-S03
therefore introduce contracts before backend implementation and facade routing.

### Senior React Frontend Developer

`WorkspaceListPage` exists but is not routed at `/workspaces`.
`listWorkspaces` is a stub returning `[]`. `/workspaces/new` already maps to
the create page. Frontend implementation must wait for real list/delete
contracts before wiring the requested actions.

### Senior Tester

Quality commands are verified from `QUALITY.md`, Gradle build files and
`forensic-ui/package.json`. No mutation, lint, Playwright or Cypress command is
verified. Regression coverage must include backend contracts, no-leak behavior,
frontend route changes, list rendering and async action states.

## Dependency Summary

S01 contracts block all implementation. S02 repository-source state behavior
blocks S03 public facade. S03 blocks S04 frontend adapter. S04 blocks S05 list
UI actions. S06 closes quality and documentation.

## Open Questions

- Whether pagination/filtering is required beyond the first MVP list.
- Whether cleaned workspaces need an explicit "show cleaned" mode later.

These are non-blocking for this workflow because the default list is
deterministic and hides cleaned records.

## Stop Conditions

Stop if the user rejects an accepted assumption, if contracts cannot represent
safe cleanup semantics, or if implementation requires guessing an existing
symbol, task, route, field or evidence behavior.
