# Example Requirement Validation

## Requirement

```text
Implement a new gRPC ingestion service that receives analysis events from the plugin and forwards them to an Analysis Store.
```

## Gate Decision

`REQUIRES_REFINEMENT`

The requirement successfully passed through the governance chain, but it is not ready for workflow authoring because required contract, ownership, security and acceptance details are missing.

## Repository Evidence

Verified existing artifacts:

- `forensic-analytics-ingestion-grpc/src/main/proto/forensic_ingestion.proto`
- `docs/README.md`
- `forensic-analytics-application/src/main/java/de/burger/forensics/analytics/application/analysis/port/AnalysisStorePort.java`
- `forensic-analytics-persistence/src/main/java/de/burger/forensics/analytics/persistence/InMemoryAnalysisStore.java`

Current gRPC contract already exposes `ForensicIngestionService` with:

- `AnalyzeRepository`
- `StartAnalysisSession`
- `UploadAnalysisData`
- `CompleteAnalysisSession`
- `AbortAnalysisSession`

`UploadAnalysisData` streams `AnalysisDataEnvelope` messages and supports `AnalysisPayloadKind.RUNTIME_TRACE`. The example requirement says "new gRPC ingestion service" and "analysis events", but it does not state whether this should extend the existing service, add a new RPC, add a new protobuf message, or create a separate service boundary.

## Three Amigos Findings

### Requirement Analyst

- Business goal is understandable at a high level: receive plugin events and store them for analysis.
- Technical goal is incomplete: "analysis events" are not defined.
- Non-goals are missing.
- Acceptance criteria are missing.
- Consumer and producer expectations are incomplete.

### Architecture Validator

- Existing gRPC ingestion adapter exists and must not be bypassed without a contract decision.
- Analysis Store port exists, but ownership for new event data is not defined.
- It is unclear whether runtime trace events, source facts, semantic artifacts or another event type are intended.
- No new shared Java DTO module is allowed.
- Contract-first review is required before implementation.

### Quality Validator

- Testability is not yet defined.
- Required contract tests, mapper tests, validation tests and store tests cannot be planned until event schema and ownership are defined.
- `QUALITY.md` remains the authority for minimum and full gates.

### Dependency / Deadlock Validator

- Contract-First API Steward must run before implementation.
- Data Ownership & Persistence Steward must define owner and write path.
- Security & Threat Modeling must review upload and sensitive trace data risks.
- Observability & Runtime Diagnostics must define correlation and trace context.
- Quality Gate Orchestrator must define tests after contract and data ownership decisions.

## Required Skills

- Three Amigos Requirement Gatekeeper
- Skill Registry & Conflict Auditor
- Contract-First API Steward
- Data Ownership & Persistence Steward
- Security & Threat Modeling
- Observability & Runtime Diagnostics
- Quality Gate Orchestrator
- Workflow Executor

## Blockers

| Blocker | Class | Owner | Required resolution |
| --- | --- | --- | --- |
| Event contract is undefined. | `REQUIRES_INPUT` | Contract-First API Steward | Define whether to extend `AnalysisDataEnvelope`, add an RPC, or create a new service. |
| Data ownership is unclear. | `REQUIRES_DECISION` | Data Ownership & Persistence Steward | Define owner service/module, write path, read path and retention for analysis events. |
| Security review is missing. | `REQUIRES_DECISION` | Security & Threat Modeling | Define upload risks, sensitive data handling, authentication and authorization expectations. |
| Trace context is incomplete. | `REQUIRES_INPUT` | Observability & Runtime Diagnostics | Define required `correlationId`, `traceId`, `spanId`, `analysisRunId` and runtime identifiers. |
| Acceptance criteria are missing. | `REQUIRES_INPUT` | Requirement owner | Provide testable acceptance criteria and non-goals. |

## Provisional Slice Outline

This outline is not executable until blockers are resolved.

1. Define gRPC/protobuf event contract.
2. Define data ownership and store behavior.
3. Define security and observability requirements.
4. Implement adapter and mapping changes.
5. Add contract, mapper, validation and persistence tests.
6. Run required quality gates.

## Result

The governance chain produced a blocker report without deadlock. The next safe action is requirement refinement, not implementation.
