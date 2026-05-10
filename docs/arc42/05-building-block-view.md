# 5. Building Block View

## 5.1 Level 1 - System Overview

```text
Forensics Platform
├── Application Core
├── Canonical Analysis Model
├── gRPC Ingestion Adapter
├── Analysis Import
├── Rule Planning
├── Runtime Event Processing
├── Incident Management
├── Replay Engine
├── Graph Projection
├── Vector Context Builder
├── LLM Diagnosis
├── Repair Orchestration
└── Adapters
    ├── Gradle Plugin Adapter
    ├── Maven Plugin Adapter
    ├── Joern Adapter
    ├── Byteman Rule Adapter
    ├── Runtime Collector Adapter
    ├── Relational Store Adapter
    ├── Graph DB Adapter
    ├── Vector DB Adapter
    └── LLM Provider Adapter
```

## 5.2 Level 2 - Core Building Blocks

| Building Block | Responsibility |
|---|---|
| Canonical Analysis Model | Owns stable IDs and normalized facts |
| gRPC Ingestion Adapter | Receives session-based plugin analysis uploads and maps transport DTOs to application commands |
| Static Fact Import | Imports AST, build and dependency facts |
| Joern Semantic Import | Imports and maps Joern semantic facts |
| Rule Planner | Plans instrumentation rules based on facts and policies |
| Byteman Generator | Generates Byteman rules with stable rule IDs |
| Runtime Event Processor | Validates, redacts and stores runtime events |
| Incident Service | Creates and groups exception-based incidents |
| Replay Engine | Reconstructs timelines and call trees |
| Graph Projection Service | Builds graph projections from canonical facts |
| Vector Context Builder | Builds semantic context for retrieval and LLM use |
| LLM Diagnosis Service | Creates evidence-based root-cause analysis |
| Repair Orchestrator | Prepares future gated repair flows |

## 5.3 Hexagonal Architecture Mapping

| Layer | Examples |
|---|---|
| Domain | IDs, analysis model, incident model, replay model, rule plan |
| Application | Import use cases, replay use cases, diagnosis use cases |
| Ports | Fact import port, event store port, graph port, LLM port, rule generation port |
| Adapters | gRPC ingestion, Gradle, Maven, Joern, Byteman, relational DB, graph DB, vector DB, LLM provider |

## 5.4 Important Boundary

Gradle and Maven plugins must not become the central platform. They provide raw facts, build context, source roots, classpath information and integration points only.

## 5.5 gRPC Ingestion Boundary

`forensic-analytics-ingestion-grpc` is an inbound adapter. It may depend on generated Protobuf/gRPC classes and the application layer. It must not depend on persistence adapters, Joern, replay, LLM providers or plugin internals.

The adapter maps:

```text
Proto DTO
  -> Application Command
    -> Application Use Case
```
