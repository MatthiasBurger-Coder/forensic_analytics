# 01 - Architecture Target

## Hexagonal Target

```text
Inbound Adapter: gRPC Server
        -> Application Service: AnalysisIngestionService
        -> Application Service: WorkspacePreparationService
        -> Application Service: RepositoryCheckoutService
        -> Domain: AnalysisSession, Workspace, RepositoryReference
        -> Outbound Ports
             -> Git Client Adapter
             -> Filesystem Workspace Adapter
             -> Persistence / Analysis Session Store Adapter
```

Domain and application stay framework-free. gRPC DTOs, Git commands, filesystem paths and persistence implementation details stay outside the core.

## Required Inbound Adapter

### gRPC Server

The gRPC server receives `AnalyzeRepositoryRequest`, validates transport-level fields and maps the request into application commands. It returns `AnalyzeRepositoryResponse` with an `AnalysisSessionId`, `WorkspaceId` and `CheckoutResult`.

The adapter must not execute parser, Joern, BTM, graph, replay or UI logic.

## Required Outbound Adapters

### Git Client

The Git adapter implements repository clone, fetch, branch checkout, commit checkout, commit resolution, remote URL detection and repository cleanup behind a port.

### Filesystem Workspace

The filesystem workspace adapter creates isolated workspace directories, applies cleanup policy, enforces workspace-root boundaries and reports disk or lock failures explicitly.

### Persistence / Analysis Session Store

The persistence adapter stores analysis sessions, workspace references, checkout result metadata and job registration state. It must preserve provenance and not collapse raw request data into ambiguous strings.

## Required Application Services

### AnalysisIngestionService

Owns the request-level workflow: validate application inputs, create the analysis session, request workspace preparation, request checkout and register the first job state.

### WorkspacePreparationService

Owns workspace lifecycle orchestration through ports. It selects workspace policy, creates or leases a workspace and returns a prepared workspace reference.

### RepositoryCheckoutService

Owns checkout orchestration through the Git port. It resolves the requested branch and commit state and returns a deterministic checkout result.

## Required Domain Concepts

- `AnalysisSession`
- `Workspace`
- `RepositoryReference`
- `BranchReference`
- `CommitReference`
- `SourceRoot`
- `CheckoutResult`

These are target concepts for upcoming slices. Existing repository classes with similar names must be verified before implementation. Do not infer current symbols from this plan.

## Dependency Direction

```text
gRPC / Git / filesystem / persistence adapters
        -> application services and ports
        -> domain model
```

The reverse direction is forbidden.
