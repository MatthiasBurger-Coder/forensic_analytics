# Forensics Platform Documentation

This directory contains the architecture and product documentation for the Forensics Platform.

## Documentation Structure

- [arc42/](arc42/) - Architecture documentation based on the arc42 template
- [epics/](epics/) - Versioned product and requirement epics
- [adr/](adr/) - Architecture Decision Records, if present

## Modules

The current implementation baseline contains the first technical modules for the platform:

- `forensic-analytics-domain` - domain model for ingestion sessions and payload metadata
- `forensic-analytics-application` - application use case contracts and session-oriented ingestion orchestration
- `forensic-analytics-persistence` - persistence adapter boundary, currently with an in-memory implementation for local bootstrap and tests
- `forensic-analytics-ingestion-grpc` - inbound gRPC adapter for plugin analysis uploads
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
