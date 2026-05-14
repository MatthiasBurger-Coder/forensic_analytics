# Architecture Target

The target architecture keeps Forensic Analytics hexagonal:

```text
Inbound adapters
  -> application services
    -> domain model and ports
      <- outbound adapters
```

The plugin remains outside the analysis platform. It is a producer and gRPC client only. The Analytics server owns workspace preparation, repository checkout, session persistence and all later analysis execution.

## Inbound Adapter

### gRPC Server

Module: `forensic-analytics-ingestion-grpc`

Responsibilities:

- expose the repository-ingestion RPC
- validate transport-level request completeness
- map Protobuf DTOs to application commands
- map application results to Protobuf responses
- never perform Git, filesystem, parser, persistence or analysis behavior directly

The existing `forensic_ingestion.proto` must be verified before implementation changes. If a field or service name differs from this workplan, the implementation slice must stop and report the mismatch before changing code.

## Application Services

Target service concepts:

- `AnalysisIngestionService`
- `WorkspacePreparationService`
- `RepositoryCheckoutService`

These names describe the intended responsibilities. During implementation, existing use cases and services must be inspected first. If an equivalent service already exists under another verified name, the slice must decide whether to extend the existing service or introduce the planned name with a documented reason.

### AnalysisIngestionService

Responsibilities:

- accept an `AnalyzeRepository` command from the gRPC adapter
- create or register an `AnalysisSession`
- coordinate workspace preparation
- register the analysis job context
- return session, workspace and checkout result

### WorkspacePreparationService

Responsibilities:

- allocate a `WorkspaceId`
- create a workspace path through a filesystem port
- apply `WorkspacePolicy`
- create a `WorkspaceLease`
- clean up failed or completed workspaces according to policy

### RepositoryCheckoutService

Responsibilities:

- call the Git port for clone, fetch and checkout
- resolve the effective commit
- detect source roots through an application port or repository-source adapter
- build a deterministic `CheckoutResult`
- report missing branch, missing commit or checkout failures explicitly

## Outbound Adapters

### Git Client

Target module: `forensic-analytics-adapter-repository-source`

Responsibilities:

- clone repositories
- fetch updates
- checkout branches and commits
- resolve the current commit
- detect the effective remote URL
- clean up repository working copies

The adapter may call the local Git executable or a verified library only after the dependency and operational impact are reviewed. Parser execution must not be introduced here.

### Filesystem Workspace

Target module: `forensic-analytics-adapter-repository-source` or a dedicated filesystem adapter if the existing boundary proves insufficient.

Responsibilities:

- create workspace directories
- enforce workspace path boundaries
- report disk and permission failures
- support deterministic cleanup
- avoid writing outside the configured workspace root

### Persistence / Analysis Session Store

Target module: `forensic-analytics-persistence`

Responsibilities:

- persist `AnalysisSession`
- persist job/workspace association
- persist checkout result metadata
- keep generated analysis output separate from verified repository evidence

## Domain Concepts

Required concepts for this platform step:

- `AnalysisSession`
- `Workspace`
- `RepositoryReference`
- `BranchReference`
- `CommitReference`
- `SourceRoot`
- `CheckoutResult`

Additional workspace concepts are detailed in [06-workspace-domain.md](06-workspace-domain.md).

## Boundary Rules

- Domain code depends only on domain-internal code and the Java standard library.
- Application code depends on domain and ports, not concrete adapters.
- gRPC DTOs stay in the gRPC adapter.
- Git and filesystem APIs stay in outbound adapters.
- Persistence APIs stay in persistence adapters.
- The plugin must not contain parser, Joern, BTM, replay or LLM logic.
- Static source roots are repository facts, not proof of runtime execution.
