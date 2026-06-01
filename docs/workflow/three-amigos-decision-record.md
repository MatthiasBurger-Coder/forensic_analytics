# Three Amigos Decision Record

## Workflow

Repository Workspace Branch Selection And Refresh

## Decision

`PROCEED_WITH_ACCEPTED_ASSUMPTIONS`

## Normalized Requirement Summary

Add backend and GUI support for remote branch discovery, branch selection,
explicit branch refresh, missing-remote warning behavior, branch list display
and future stale-analysis indicators. Plan logical workspace deletion and
future final deletion as a separate lifecycle instead of silently deleting
all referenced data.

## Five-Role Findings

### Senior Requirement Engineer

- The core branch-selection requirement is testable.
- Destructive cleanup and trash/final-delete are larger lifecycle requirements
  and must be sliced separately.
- Stale-analysis-data warning is explicitly TBD and must not be presented as
  implemented evidence.

### Senior System Architect

- `repository-source-service` remains the owner of branch metadata.
- UI must use public APIs and must not access Git or PostgreSQL directly.
- Analysis result deletion requires owner-store verification and cannot be
  implemented speculatively.

### Senior Java Backend Developer

- Backend impact includes metadata resolution, branch status semantics,
  refresh checks, contracts and possibly persistence state.
- Branch selection must not fetch remote content.
- Missing remote branch must be represented explicitly.

### Senior React Frontend Developer

- Frontend impact includes workspace domain types, API mapping, Create
  Workspace and Workspace List views.
- The combobox must render API-provided branches and preserve selected values.
- The branch list must show action state and diagnostics without inventing
  analysis freshness.

### Senior Tester

- Regression tests are required for contracts, mapping, UI state, no-refresh
  branch selection, missing remote branch refresh and delete lifecycle.
- `QUALITY.md` minimum Gradle test command is required before publication.

## Architecture And Evidence Validation

- Branch selection is repository metadata, not runtime execution evidence.
- Remote branch existence is a Git metadata fact.
- Stale analysis state is not implemented evidence until a later owner store
  can compare analysis artifacts with branch snapshots.

## Open Questions

- Exact UI copy for destructive branch cleanup warning.
- Whether final delete is operator-only.
- Which downstream stores own analysis results that must be deleted later.

## Blockers

None for workflow creation.

## Final Decision

Proceed with accepted assumptions. `workflow execute` must stop before final
delete or analysis-result cleanup if owner contracts are still unverified.
