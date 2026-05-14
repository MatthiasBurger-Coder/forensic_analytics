# 3. System Scope and Context

## 3.1 Business Context

The Forensics Platform receives repository-analysis requests, server-side analysis artifacts and runtime events. It performs server-side static and semantic analysis, normalizes evidence into a canonical analysis model and provides replay, graph context and LLM-supported diagnosis.

```text
Developer / Lead Developer
        |
        v
Forensics UI / API
        |
        v
Forensics Platform
        |
        +--> Plugin gRPC Request
        +--> Gradle Plugin
        +--> Maven Plugin
        +--> Joern Adapter
        +--> Server-side BTM Generation
        +--> Runtime Collector
        +--> Graph DB Projection
        +--> Vector DB Projection
        +--> Event Store
        +--> LLM Provider
```

## 3.2 Technical Context

| External System | Direction | Purpose |
|---|---:|---|
| Plugin gRPC Client | inbound | Sends repository-analysis requests with repository, branch, commit, build and execution context |
| Gradle Plugin | inbound/outbound | Triggers server-side analysis and binds server-generated BTM files to the target runtime through the agent when debugging requires instrumentation |
| Maven Plugin | inbound/outbound | Triggers server-side analysis and binds server-generated BTM files to the target runtime through the agent when debugging requires instrumentation |
| Joern | outbound | Runs as a server-side Analytics adapter for semantic code analysis, data-flow and control-flow information |
| Runtime Application | inbound | Emits runtime events produced by server-generated BTM instrumentation |
| Byteman Agent | outbound/inbound | Executes server-generated BTM files bound by the plugin for debugging/runtime collection |
| Relational Store | outbound | Stores canonical model and transactional state |
| Graph DB | outbound | Stores graph projection for navigation and incident context |
| Vector DB | outbound | Stores semantic projections for similarity and LLM context retrieval |
| Event Store | outbound | Stores runtime event timelines and replay data |
| LLM Provider | outbound | Performs root-cause analysis and fix planning |

## 3.3 Main Data Flow

```text
Repository Request + Server-side Static Facts + Server-side Joern Facts + Runtime Events
        |
        v
Canonical Analysis Model
        |
        +--> Graph Projection
        +--> Vector Projection
        +--> Event Timeline
        |
        v
Incident Replay
        |
        v
Incident Context Package
        |
        v
LLM Root-Cause Analysis
```

## 3.4 gRPC Ingestion Context

```text
Plugin
  -> gRPC Client
    -> forensic-analytics-ingestion-grpc
      -> Analysis Ingestion Use Case
        -> Workspace / Analysis Session Ports
          -> Persistence Adapter
```

The gRPC ingestion module is an inbound adapter. It does not own persistence, Joern integration, replay logic, LLM logic, BTM generation or plugin internals.
