# Workspace Domain

The workspace domain represents a server-side, evidence-preserving place where Analytics can prepare repository content before later analysis. It must not become a parser model and must not contain framework, gRPC, Git executable or persistence details.

## Required Concepts

### WorkspaceId

Stable value object that identifies a prepared workspace.

### WorkspacePath

Server-side path value. It is returned to internal services only when needed. The plugin should receive `WorkspaceId`, not a filesystem path.

### WorkspaceStatus

Minimum lifecycle states:

```text
REQUESTED
CREATING
READY
FAILED
CLEANED
```

`READY` means the workspace is prepared for later steps. It does not mean parsing or analysis completed.

### WorkspacePolicy

Domain policy derived from the request and server defaults. It includes ephemeral behavior, clone optimization allowances, timeout and size constraints.

### WorkspaceLease

Represents who or what owns the workspace for the current operation, when it was created and when it expires if expiration is configured.

### WorkspaceCleanupPolicy

Defines whether a workspace is deleted on completion, retained for review, retained on failure or cleaned by retention processing.

### PreparedWorkspace

Combines workspace ID, path, status, lease, policy and diagnostics after preparation.

### RepositoryCheckout

Represents the checkout request and result context for a workspace. It links repository reference, branch reference, commit reference and checkout result.

### SourceRoot

Represents a detected source root path inside the workspace. It is repository structure evidence, not parsed source evidence.

## Lifecycle

```text
REQUESTED -> CREATING -> READY
          -> FAILED
READY     -> CLEANED
FAILED    -> CLEANED
```

The lifecycle must be deterministic for the same inputs and failure conditions. Invalid transitions should fail fast in domain or application code.

## Invariants

- Workspace IDs are non-empty.
- Workspace paths are non-empty and must stay under the configured workspace root.
- A cleaned workspace cannot be used for checkout.
- A failed workspace preserves diagnostics.
- A ready workspace records the checkout result that made it ready.
- Cleanup failures are explicit and do not convert a failed checkout into a successful one.

## Tests

Domain tests should cover:

- value object validation
- lifecycle transitions
- cleanup policy mapping
- lease expiration handling
- deterministic diagnostics ordering
- source-root ordering

Application tests should cover:

- workspace creation through a fake filesystem port
- checkout through a fake Git port
- cleanup on failure
- no parser execution during preparation
