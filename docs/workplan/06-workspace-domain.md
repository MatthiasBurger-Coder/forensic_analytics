# 06 - Workspace Domain

## Purpose

Workspace domain concepts describe server-side preparation of repository input before parser or analyzer execution.

## Required Concepts

### WorkspaceId

Stable identifier for a server-managed workspace.

### WorkspacePath

Server-side path value. Concrete path operations belong in filesystem adapters, not domain policy.

### WorkspaceStatus

Minimum statuses:

- `REQUESTED`
- `CREATING`
- `READY`
- `FAILED`
- `CLEANED`

### WorkspacePolicy

Requested behavior for ephemeral workspaces, clone optimization, timeouts, disk limits and cleanup.

### WorkspaceLease

Represents ownership of a workspace during preparation or analysis. A lease records owner, start time, optional expiry and current lifecycle state.

### WorkspaceCleanupPolicy

Defines when and how workspace content is removed. Cleanup must be explicit and auditable.

### PreparedWorkspace

Application result that contains `WorkspaceId`, status, path reference, lease and diagnostics.

### RepositoryCheckout

Records repository URL, requested branch, requested commit, resolved commit, checkout mode, diagnostics and source-root metadata.

### SourceRoot

Metadata-only source root found after checkout. Detecting source roots in this phase must not parse source files or run builds.

## Lifecycle

```text
REQUESTED
  -> CREATING
    -> READY
    -> FAILED
READY
  -> CLEANED
FAILED
  -> CLEANED
```

## Evidence Rules

- Requested repository, branch and commit are preserved.
- Resolved commit is recorded after checkout.
- Missing branch, missing commit or unresolved source roots remain explicit.
- Workspace cleanup does not erase analysis-session metadata.
- Workspace path traversal is rejected by the adapter before filesystem writes.
