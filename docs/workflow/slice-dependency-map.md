# Slice Dependency Map

## Overview

The workflow is intentionally linear because branch-selection state, rendering
and regression coverage share the same frontend area.

```text
S01 Branch Semantics And Contract Guard
  -> S02 Workspace Branch Selector UI
  -> S03 Frontend Regression Coverage
  -> S04 Quality And Documentation Closure
```

## Slice Table

| Slice | Owner | Depends On | Parallel Group | Write Scope |
|---|---|---|---|---|
| S01 | Senior Requirement Engineer | None | G01 | Read-only verification plus execution-report notes |
| S02 | Senior React Frontend Developer | S01 | G02 | `WorkspaceListPage.tsx`, `styles.css` |
| S03 | Senior Tester | S02 | G03 | `WorkspaceListPage.test.tsx`, optional API/mapper tests |
| S04 | Senior Tester | S03 | G04 | workflow execution report and arc42 check status |

## Locks

| Lock | Owner | Reason |
|---|---|---|
| `public-workspace-branches-read-only` | S01/S03 | Branch options must come only from public DTO records. |
| `frontend-public-rest-only` | S02 | The browser must not call Git, gRPC or internal services. |
| `no-private-path-leakage` | S03 | Tests must prevent local paths, raw Git output and secrets from rendering. |
| `documentation-matches-implemented-scope` | S04 | Docs must not claim remote branch discovery or persisted selected branch state. |

## Parallelization Decision

No slices are parallelized. The feature is small, and splitting UI and tests
across concurrent writers would create avoidable overlap in the Workspaces page
and fixture setup.

## Stop Paths

Execution stops before implementation if S01 finds that:

- current branches means remote Git branch discovery;
- public DTOs do not contain both branch names and branch IDs;
- a new REST/gRPC method is required;
- branch options would have to be inferred from default branch or local state.
