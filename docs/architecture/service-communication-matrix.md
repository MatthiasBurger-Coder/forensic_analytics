# Service Communication Matrix

## Status

Slice 01 communication baseline updated by Slice 03 contracts, Slice 05
Analysis Store implementation, Slice 07 Java AST implementation and Slice 08
Joern CPG implementation.

This document defines planned communication ownership and sequencing. It does
not define database schemas. Slice 03 introduced initial logical API
contracts. Slice 05 implements only the `AnalysisJobService` job and artifact
metadata subset for `analysis-store-service`; Slice 08 implements the
provisional `joern-cpg-analysis-service` gRPC worker boundary.

## Contract Locations

| Contract Area | Planned Location |
|---|---|
| gRPC ingestion | `contracts/grpc/forensic-ingestion.proto` |
| analysis job and worker handoff | `contracts/grpc/analysis-job.proto` |
| repository checkout and source snapshot preparation | `contracts/grpc/repository-analysis.proto` |
| Joern CPG/CFG/DFG semantic artifact analysis | `contracts/grpc/joern-cpg-analysis.proto` |
| Gateway API | `contracts/openapi/gateway-api.yaml` |
| analysis events | `contracts/events/analysis-events.md` |

These files must remain interface contracts and documentation only. They must
not contain Java implementation code or service-shared runtime classes.

## Matrix

| Producer | Consumer | Protocol | Contract Authority | Purpose | Notes |
|---|---|---|---|---|---|
| `frontend-web-app` | `forensic-gateway-service` | REST/OpenAPI | `contracts/openapi/gateway-api.yaml` | UI reads workspaces, analysis jobs, replay and report views | Frontend must not call internal worker services directly |
| CLI or external client | `forensic-gateway-service` | REST/OpenAPI | `contracts/openapi/gateway-api.yaml` | Public analysis and reporting workflows | Gateway translates public errors without leaking private service details |
| plugin / scanner / runtime collector | `forensic-ingestion-service` | gRPC/protobuf | `contracts/grpc/forensic-ingestion.proto` | Upload repository, scanner and runtime evidence packages | Payload provenance, correlation and schema version must be preserved |
| `forensic-gateway-service` | `forensic-ingestion-service` | REST or gRPC | Slice 03 contract decision | Start or observe ingestion-oriented workflows | Gateway is facade only; ingestion owns intake validation |
| `forensic-gateway-service` | `repository-analysis-service` | gRPC | `contracts/grpc/repository-analysis.proto` | Prepare repository workspaces and source snapshots from public analysis requests | Repository URLs must follow the stricter repository-analysis service URL policy |
| `forensic-gateway-service` | `analysis-store-service` | gRPC for implemented job lifecycle; REST/gRPC for later query views | `contracts/grpc/analysis-job.proto` for job and artifact metadata operations | Read canonical analysis job and status views | Gateway must not read Analysis Store private DB tables |
| `forensic-gateway-service` | `graph-replay-service` | REST or gRPC | Slice 03 contract decision | Request replay and graph views | Replay responses must expose missing evidence explicitly |
| `forensic-gateway-service` | `report-generation-service` | REST or gRPC | Slice 03 contract decision | Request reports and incident packages | Reports must separate evidence, gaps, derived facts and hypotheses |
| `forensic-ingestion-service` | `analysis-store-service` | gRPC or event contract | `contracts/grpc/analysis-job.proto` or `contracts/events/analysis-events.md` | Register accepted raw intake and canonical ingestion state | Slice 05 supports artifact metadata registration, not durable normalized facts |
| `repository-analysis-service` | `analysis-store-service` | gRPC or event contract | `contracts/grpc/repository-analysis.proto`, `contracts/grpc/analysis-job.proto` or `contracts/events/analysis-events.md` | Register source snapshot metadata and checkout diagnostics | Repository service owns workspaces; Analysis Store owns accepted metadata |
| `repository-analysis-service` | `java-ast-analysis-service` | gRPC handoff with job metadata | `contracts/grpc/repository-analysis.proto`, `contracts/grpc/java-ast-analysis.proto`, `contracts/grpc/analysis-job.proto` | Deliver immutable source snapshot identifiers, relative Java roots and bounded source content for AST analysis | Do not share mutable workspace paths as hidden coupling |
| `repository-analysis-service` | `joern-cpg-analysis-service` | gRPC handoff with job metadata | `contracts/grpc/repository-analysis.proto`, `contracts/grpc/joern-cpg-analysis.proto`, `contracts/grpc/analysis-job.proto` | Deliver immutable source snapshot references, opaque workspace IDs and relative Java roots for Joern analysis | Joern runtime remains service-contained; no public absolute workspace paths |
| `java-ast-analysis-service` | `analysis-store-service` | gRPC or event contract | `contracts/grpc/java-ast-analysis.proto`, `contracts/grpc/analysis-job.proto` or `contracts/events/analysis-events.md` | Submit source-fact artifact references, counts and diagnostics | Slice 05 accepts job and artifact metadata only; static facts must not be treated as runtime execution |
| `joern-cpg-analysis-service` | `analysis-store-service` | gRPC or event contract | `contracts/grpc/joern-cpg-analysis.proto`, `contracts/grpc/analysis-job.proto` or `contracts/events/analysis-events.md` | Submit static semantic artifact references, provenance and mapping diagnostics | Slice 05 accepts job and artifact metadata only; incomplete mappings stay explicit and static semantic output is not runtime execution evidence |
| `analysis-store-service` | `btm-generation-service` | gRPC | `contracts/grpc/btm-generation.proto` and `contracts/grpc/analysis-job.proto` | Provide accepted fact artifact references, semantic artifact references and bounded instrumentation targets for rule generation | BTM service must not scan repositories directly or treat generated rules as runtime evidence |
| `btm-generation-service` | `analysis-store-service` | gRPC or event contract | `contracts/grpc/btm-generation.proto`, `contracts/grpc/analysis-job.proto` or `contracts/events/analysis-events.md` | Register generated rule artifacts, manifests and reproducibility metadata | Rule IDs must be stable and reproducible |
| `graph-replay-service` | `analysis-store-service` | REST or gRPC | Slice 03 contract decision | Read canonical facts, incidents and correlations | Graph/replay must not read private Analysis Store tables |
| `report-generation-service` | `analysis-store-service` | REST or gRPC | Slice 03 contract decision | Read evidence, findings, incidents and artifact metadata | Reports must preserve evidence provenance |
| `report-generation-service` | `graph-replay-service` | REST or gRPC | Slice 03 contract decision | Read replay and graph context for reports | Graph context remains projection output |
| `report-generation-service` | LLM provider | Provider API | Future provider decision | Optional generated analysis when approved | LLM output is generated analysis, not evidence |

## Error, Retry And Idempotency Rules

Slice 03 must define concrete error and status models. Until then:

- public Gateway errors must not leak private payloads, stack traces, secrets or
  source content;
- ingestion uploads require correlation and schema-version preservation;
- worker submissions require idempotency or duplicate-detection semantics;
- long-running jobs require explicit status and retry behavior;
- timeout behavior must be visible to callers;
- failed or partial evidence must be represented as incomplete, rejected,
  unknown or unavailable rather than silently repaired.

## Generated-Code Boundaries

Generated REST or gRPC code may be service-local implementation detail. It must
not become a shared Java DTO or shared Java domain module between services.

If a service generates code from a central contract, the generated output must
be local to that service build or copied through a service-local generation
step. No service may depend on another service's generated classes.

## Contract-Test Expectations

Later contract tests must verify:

- request and response semantics;
- compatibility rules;
- required and optional fields;
- error/status mapping;
- idempotency behavior;
- timeout and retry expectations;
- correlation and provenance preservation;
- generated-code boundary compliance.

The repository does not currently contain service-level contract tests.
