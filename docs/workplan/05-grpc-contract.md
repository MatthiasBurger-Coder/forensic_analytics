# gRPC Contract

The planned repository-ingestion contract lets a producer plugin ask the Analytics platform to prepare a server-side repository workspace and register an analysis session.

The current repository already contains `forensic-analytics-ingestion-grpc/src/main/proto/forensic_ingestion.proto` with an `AnalyzeRepository` RPC and the model names listed below. Future implementation work must verify that file before changing any field, field number, package, Java package or service name.

## Service Shape

```proto
service ForensicIngestionService {
  rpc AnalyzeRepository(AnalyzeRepositoryRequest)
      returns (AnalyzeRepositoryResponse);
}
```

Other ingestion RPCs may continue to exist. This workplan focuses on repository preparation and analysis session creation.

## Required Request Models

### AnalyzeRepositoryRequest

Carries:

- `RepositoryReference repository`
- `BranchReference branch`
- `CommitReference commit`
- `WorkspacePolicy workspace_policy`
- `BuildContext build_context`
- `string request_id`
- `string schema_version`

Validation:

- repository remote URL is required
- branch may be optional only when policy allows the server to use the remote default branch
- commit may be optional, but if present and marked required it must be checked out exactly
- request ID and schema version must be preserved for traceability

### RepositoryReference

Carries:

- remote URL
- provider, if known
- attributes for explicit producer metadata

The attributes map must not carry secrets. Unknown provider values remain unknown instead of being normalized to a guessed provider.

### BranchReference

Carries:

- branch name
- whether the branch is required

If the branch is required and missing on the remote, checkout fails explicitly.

### CommitReference

Carries:

- commit hash
- whether the commit is required

If the commit is required and cannot be resolved after clone or fetch, checkout fails explicitly.

### WorkspacePolicy

Carries:

- ephemeral flag
- shallow clone allowance
- partial clone allowance
- sparse checkout allowance
- timeout in seconds
- maximum workspace bytes

Policy values guide the server. They do not authorize the plugin to create workspaces or perform analysis itself.

### BuildContext

Carries:

- build tool
- build ID
- root project name
- declared modules
- explicit attributes

Build context is producer metadata. It can help correlate a request with a build, but it is not proof that any source path was parsed or executed.

## Required Response Models

### AnalyzeRepositoryResponse

Carries:

- `AnalysisSessionId analysis_session_id`
- `WorkspaceId workspace_id`
- `CheckoutResult checkout_result`
- message

The response represents repository preparation, not completed analysis.

### AnalysisSessionId

Stable identifier for the server-side analysis session. It must be generated or persisted by Analytics, not by the plugin.

### WorkspaceId

Stable identifier for the server-side workspace. The plugin may display it or use it for follow-up calls, but it must not assume the filesystem path.

### CheckoutResult

Carries:

- resolved remote URL
- requested branch
- requested commit
- resolved commit
- detected source roots
- checkout status
- diagnostics

Diagnostics must preserve checkout uncertainty. For example, no detected source roots is a valid explicit outcome and must not be converted into a parser failure.

## Error Handling

Transport errors describe gRPC-level failures. Domain or application errors are mapped into explicit response status and diagnostics where the contract supports that. The server must preserve the original cause internally for logs and tests without exposing secrets.

## Compatibility Rules

- Do not reuse Protobuf field numbers for different meanings.
- Do not rename public messages without a dedicated compatibility plan.
- Do not add fallback aliases unless backward compatibility is explicitly requested and tested.
- Keep Protobuf DTOs out of domain and application service APIs.
