# Forensics Platform Documentation

This directory contains the architecture and product documentation for the Forensics Platform.

## Documentation Structure

- [arc42/](arc42/) - Architecture documentation based on the arc42 template
- [epics/](epics/) - Versioned product and requirement epics
- [adr/](adr/) - Architecture Decision Records, if present
- [workplan/](workplan/) - Planned workspace, gRPC and server-side analysis workflows and execution slices

## Modules

The current platform direction is server-bound repository analysis. Plugins trigger analysis on the Forensic Analytics server; Analytics prepares workspaces, checks out repositories, runs parser/Joern/BTM capabilities server-side and returns the artifacts needed by clients.

The current implementation baseline contains the technical modules for ingestion, server-side repository analysis preparation and local bootstrap/test scenarios:

- `forensic-analytics-domain` - domain model for ingestion sessions, artifact metadata, source facts and semantic Joern graph facts
- `forensic-analytics-application` - application use case contracts for ingestion and server-side repository analysis orchestration
- `forensic-analytics-engine` - engine facade for repository analysis use cases
- `forensic-analytics-adapter-repository-source` - outbound adapter for server-side workspace repository source acquisition
- `forensic-analytics-adapter-javaparser` - outbound adapter for Java source scanning with JavaParser
- `forensic-analytics-adapter-joern-docker` - outbound adapter for Docker-based Joern analysis and semantic artifact parsing
- `forensic-analytics-cli` - inbound command line adapter for local bootstrap and engine request ingestion
- `forensic-analytics-testbed` - deterministic local integration scenarios
- `forensic-analytics-persistence` - persistence adapter boundary, currently with an in-memory implementation for local bootstrap and tests
- `forensic-analytics-ingestion-grpc` - inbound gRPC adapter for plugin-triggered server-side analysis requests
- `forensic-analytics-ingestion-request` - engine request importer for plugin-produced analysis request manifests
- `forensic-analytics-bootstrap` - executable bootstrap wiring for the gRPC ingestion server

### gRPC Ingestion

`forensic-analytics-ingestion-grpc` exposes `ForensicIngestionService` as an inbound adapter. It receives plugin-triggered analysis requests and repository/build context, validates transport-level fields, maps Protobuf DTOs to application commands and delegates to `ForensicIngestionUseCase`.

The service methods are:

- `StartAnalysisSession`
- `UploadAnalysisData`
- `CompleteAnalysisSession`
- `AbortAnalysisSession`

Runtime configuration for the bootstrap module:

```properties
forensics.analytics.ingestion.grpc.enabled=true
forensics.analytics.ingestion.grpc.port=9090
```

Environment variable equivalents:

```text
FORENSICS_ANALYTICS_INGESTION_GRPC_ENABLED=true
FORENSICS_ANALYTICS_INGESTION_GRPC_PORT=9090
```

The final plugin payload schema is intentionally not part of this module. Parser execution, Joern execution, BTM generation, replay, LLM context construction and direct database logic are outside the gRPC adapter boundary.

### Server-Side Repository Analysis

Repository analysis is server-bound. Analytics resolves repository source roots from server-side workspaces, scans Java source facts with the JavaParser adapter and can enrich the application result with Docker-based Joern artifacts. Joern analysis currently preserves call graph nodes and edges, methods, static call relations, control-flow relations, data-flow paths and semantic anchors as explicit semantic graph facts in the application result.

When runtime debugging requires instrumentation, Analytics owns BTM generation from the server-side analysis and instrumentation plan. The plugin may receive server-generated BTM files and bind them to the target implementation through the runtime agent so runtime information can be collected during debugging. The plugin does not generate BTM files and does not become the analysis platform.

## Current Architecture Baseline

The current architecture baseline is derived from:

- EPIC: Forensics Platform - Exception-centered Runtime Replay and LLM-assisted Error Analysis
- Version: 0.1
- Date: 2026-05-03

## Core Vision

The Forensics Platform combines static code analysis, semantic graph analysis, runtime tracing, exception replay and LLM-supported diagnosis into a controlled analysis and repair flow.

The long-term product vision is:

```text
Observe -> Replay -> Understand -> Fix -> Test -> Verify -> Deploy
```
