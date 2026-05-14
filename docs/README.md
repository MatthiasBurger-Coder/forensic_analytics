# Forensics Platform Documentation

This directory contains the architecture and product documentation for the Forensics Platform.

## Documentation Structure

- [arc42/](arc42/) - Architecture documentation based on the arc42 template
- [epics/](epics/) - Versioned product and requirement epics
- [adr/](adr/) - Architecture Decision Records, if present
- [workflows/](workflows/) - Developer workflows for local analysis infrastructure and future adapter integration targets

## Modules

The current implementation baseline contains the technical modules for ingestion and local repository analysis:

- `forensic-analytics-domain` - domain model for ingestion sessions, artifact metadata, source facts and semantic Joern graph facts
- `forensic-analytics-application` - application use case contracts for ingestion and local repository analysis orchestration
- `forensic-analytics-engine` - engine facade for repository analysis use cases
- `forensic-analytics-adapter-repository-source` - outbound adapter for local repository source acquisition
- `forensic-analytics-adapter-javaparser` - outbound adapter for Java source scanning with JavaParser
- `forensic-analytics-adapter-joern-docker` - outbound adapter for Docker-based Joern analysis and semantic artifact parsing
- `forensic-analytics-cli` - inbound command line adapter for local analysis and engine request ingestion
- `forensic-analytics-testbed` - deterministic local integration scenarios
- `forensic-analytics-persistence` - persistence adapter boundary, currently with an in-memory implementation for local bootstrap and tests
- `forensic-analytics-ingestion-grpc` - inbound gRPC adapter for plugin analysis uploads
- `forensic-analytics-ingestion-request` - engine request importer for plugin-produced payload manifests
- `forensic-analytics-bootstrap` - executable bootstrap wiring for the gRPC ingestion server

### gRPC Ingestion

`forensic-analytics-ingestion-grpc` exposes `ForensicIngestionService` as an inbound adapter. It receives plugin scan data, validates transport-level fields, maps Protobuf DTOs to application commands and delegates to `ForensicIngestionUseCase`.

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

The final plugin payload schema is intentionally not part of this module. Joern, replay, LLM context construction and direct database logic are outside the gRPC adapter boundary.

### Local Repository Analysis

The local analysis path resolves repository source roots, scans Java source facts with the JavaParser adapter and can enrich the application result with Docker-based Joern artifacts. Joern analysis currently preserves call graph nodes and edges, methods, static call relations, control-flow relations, data-flow paths and semantic anchors as explicit semantic graph facts in the application result.

## Current Architecture Baseline

The current architecture baseline is derived from:

- EPIC: Forensics Platform – Exception-zentriertes Runtime Replay und LLM-gestützte Fehleranalyse
- Version: 0.1
- Date: 2026-05-03

## Core Vision

The Forensics Platform combines static code analysis, semantic graph analysis, runtime tracing, exception replay and LLM-supported diagnosis into a controlled analysis and repair flow.

The long-term product vision is:

```text
Observe -> Replay -> Understand -> Fix -> Test -> Verify -> Deploy
```
