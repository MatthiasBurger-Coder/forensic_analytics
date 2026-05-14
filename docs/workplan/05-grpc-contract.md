# 05 - gRPC Contract

## Contract Goal

The planned contract lets the plugin send repository context and lets Analytics create an analysis session, prepare a workspace, check out the repository and return deterministic identifiers and checkout metadata.

This is a planned contract. Existing `.proto` symbols must be verified before implementation.

## Required Models

### AnalyzeRepositoryRequest

Fields:

- `RepositoryReference repository`
- `BranchReference branch`
- `CommitReference commit`
- `WorkspacePolicy workspace_policy`
- `BuildContext build_context`
- `string request_id`
- `string schema_version`

The request is created by the plugin. It must not include AST facts, Joern output, BTM rules, replay data or graph data for this phase.

### RepositoryReference

Fields:

- `string remote_url`
- `string provider`
- `map<string, string> attributes`

### BranchReference

Fields:

- `string name`
- `bool required`

Branch may be empty only when an exact commit is supplied and the application contract allows detached checkout.

### CommitReference

Fields:

- `string hash`
- `bool required`

Commit pinning is preferred for deterministic analysis. When a branch is supplied without a commit, Analytics must resolve and return the actual commit checked out.

### WorkspacePolicy

Fields:

- `bool ephemeral`
- `bool allow_shallow_clone`
- `bool allow_partial_clone`
- `bool allow_sparse_checkout`
- `int64 timeout_seconds`
- `int64 max_workspace_bytes`

Policy values are requests, not guarantees. Analytics decides supported behavior and reports the actual checkout mode.

### BuildContext

Fields:

- `string build_tool`
- `string build_id`
- `string root_project_name`
- `repeated string declared_modules`
- `map<string, string> attributes`

The plugin supplies build context only. It does not run analysis.

### AnalyzeRepositoryResponse

Fields:

- `AnalysisSessionId analysis_session_id`
- `WorkspaceId workspace_id`
- `CheckoutResult checkout_result`
- `string message`

### AnalysisSessionId

Fields:

- `string value`

### WorkspaceId

Fields:

- `string value`

### CheckoutResult

Fields:

- `string resolved_remote_url`
- `string requested_branch`
- `string requested_commit`
- `string resolved_commit`
- `repeated string detected_source_roots`
- `string checkout_status`
- `repeated string diagnostics`

## RPC Shape

Initial implementation:

```text
rpc AnalyzeRepository(AnalyzeRepositoryRequest)
    returns (AnalyzeRepositoryResponse);
```

Future extensions:

- server streaming for job progress events,
- client streaming for chunked payload upload,
- bidirectional streaming for long-running interactive analysis sessions,
- compression for large metadata payloads,
- explicit deadlines and retry policy,
- idempotency keys for safe retries.

## Compatibility Rules

- Use schema versioning.
- Reserve removed field numbers.
- Keep new fields additive where possible.
- Validate required business fields in the adapter/application layer.
- Do not add hidden aliases or undocumented compatibility wrappers.
