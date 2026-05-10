# 3. System Scope and Context

## 3.1 Business Context

The Forensics Platform receives static facts, semantic facts and runtime events from different producers. It normalizes them into a canonical analysis model and provides replay, graph context and LLM-supported diagnosis.

```text
Developer / Lead Developer
        |
        v
Forensics UI / API
        |
        v
Forensics Platform
        |
        +--> Plugin gRPC Ingestion
        +--> Gradle Plugin
        +--> Maven Plugin
        +--> Joern Adapter
        +--> Byteman Runtime Collector
        +--> Graph DB Projection
        +--> Vector DB Projection
        +--> Event Store
        +--> LLM Provider
```

## 3.2 Technical Context

| External System | Direction | Purpose |
|---|---:|---|
| Plugin gRPC Client | inbound | Sends session-based plugin analysis uploads to the gRPC ingestion adapter |
| Gradle Plugin | inbound | Provides build context, source roots, AST facts and rule bindings |
| Maven Plugin | inbound | Provides Maven build context and facts |
| Joern | inbound/outbound | Provides semantic code analysis, data-flow and control-flow information |
| Runtime Application | inbound | Emits Byteman-generated runtime events |
| Byteman Agent | outbound/inbound | Executes generated instrumentation rules |
| Relational Store | outbound | Stores canonical model and transactional state |
| Graph DB | outbound | Stores graph projection for navigation and incident context |
| Vector DB | outbound | Stores semantic projections for similarity and LLM context retrieval |
| Event Store | outbound | Stores runtime event timelines and replay data |
| LLM Provider | outbound | Performs root-cause analysis and fix planning |

## 3.3 Main Data Flow

```text
Static Facts + Joern Facts + Runtime Events
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
      -> ForensicIngestionUseCase
        -> IngestionSessionRepository Port
          -> Persistence Adapter
```

The gRPC ingestion module is an inbound adapter. It does not own persistence, Joern integration, replay logic, LLM logic or the final plugin payload schema.
