# Service Communication Matrix

## Status

FA-MSA-001 Slice 03 contract-first communication baseline.

This document records target communication ownership. It does not define
database schemas and does not claim that the target services or endpoints are
implemented. Existing contract files are current evidence and may need to be
renamed, split or superseded by later contract slices.

## Contract Locations

| Contract Area | Current or Planned Location And Authority |
|---|---|
| ingestion intake | `contracts/grpc/forensic-ingestion.proto`, authority `ingestion-service` |
| repository source and source snapshot preparation | `contracts/grpc/repository-analysis.proto`, predecessor filename with authority `repository-source-service` |
| JavaParser analysis and source facts | `contracts/grpc/java-ast-analysis.proto` and `contracts/grpc/java-ast-source-facts-v1.schema.json`, predecessor filenames with authority `java-parser-analysis-service` |
| Joern semantic analysis | `contracts/grpc/joern-cpg-analysis.proto`, predecessor filename with authority `joern-analysis-service` |
| orchestration and job status | `contracts/grpc/analysis-job.proto` plus `contracts/events/analysis-events.md`, authority `analysis-orchestrator-service`; S04 still confirms canonical state ownership |
| public query and report API | `contracts/openapi/gateway-api.yaml`, transitional filename with authority `query-report-api-service` |
| CLI public API behavior | `contracts/cli/gateway-cli-contract.md`, transitional filename with authority `cli-client` |
| analysis events | `contracts/events/analysis-events.md`, authority per event producer/consumer table |

Contracts are interface descriptions only. They must not contain Java
implementation code or service-shared runtime classes.

## Target Matrix

| Producer | Consumer | Protocol | Contract Authority | Purpose | Notes |
|---|---|---|---|---|---|
| UI, CLI or external client | `query-report-api-service` | REST/OpenAPI | `contracts/openapi/gateway-api.yaml` transitional public OpenAPI file | Submit analysis requests, query status and retrieve reports | Public responses must redact private paths, secrets and internal diagnostics |
| `query-report-api-service` | `analysis-orchestrator-service` | REST/gRPC/event | `contracts/grpc/analysis-job.proto` and `contracts/events/analysis-events.md` | Start or observe analysis workflows | API service remains facade-only |
| producer, scanner or runtime collector | `ingestion-service` | gRPC/REST/message | `contracts/grpc/forensic-ingestion.proto` and `contracts/events/analysis-events.md` | Upload analysis or runtime data | Preserve provenance, schema version and correlation |
| `ingestion-service` | `analysis-orchestrator-service` or S04-approved owner | gRPC/event/file | `contracts/grpc/forensic-ingestion.proto`, `contracts/events/analysis-events.md` or a later explicit handoff file contract | Handoff accepted or rejected intake | Canonical data ownership is resolved by S04 |
| `analysis-orchestrator-service` | `repository-source-service` | gRPC/REST/file | `contracts/grpc/repository-analysis.proto` transitional repository-source contract | Prepare repository source snapshots | Workspaces remain private to repository source service |
| `analysis-orchestrator-service` | `java-parser-analysis-service` | gRPC/file | `contracts/grpc/java-ast-analysis.proto` and `contracts/grpc/java-ast-source-facts-v1.schema.json` | Request AST/source-fact analysis from source snapshots | Static facts are not runtime execution evidence |
| `analysis-orchestrator-service` | `joern-analysis-service` | gRPC/file | `contracts/grpc/joern-cpg-analysis.proto` | Request CPG/CFG/DFG semantic analysis | Joern receives explicit materialization or artifact references only |
| `java-parser-analysis-service` | S04-approved canonical fact owner | gRPC/event/file | `contracts/grpc/java-ast-analysis.proto`, `contracts/grpc/java-ast-source-facts-v1.schema.json` and `contracts/events/analysis-events.md` | Publish source-fact metadata and diagnostics | Generated transport classes stay service-local |
| `joern-analysis-service` | S04-approved canonical fact owner | gRPC/event/file | `contracts/grpc/joern-cpg-analysis.proto` and `contracts/events/analysis-events.md` | Publish semantic artifact metadata and diagnostics | Incomplete mappings remain explicit |
| S04-approved report owner | `query-report-api-service` | REST/gRPC/file | `contracts/openapi/gateway-api.yaml` and `contracts/events/analysis-events.md` report events | Provide report data to clients | Reports separate evidence, gaps, derived facts and hypotheses |
| `cli-client` | `query-report-api-service` | REST/OpenAPI | `contracts/cli/gateway-cli-contract.md` and `contracts/openapi/gateway-api.yaml` transitional public API files | Start jobs, read status and retrieve reports | CLI has no business logic or service implementation dependency |
| `observability-stack` | productive services | deployment/configuration | observability configuration | Logging, metrics, tracing and dashboards | Not a shared Java library |
| `testbed` | productive services | Compose, REST, gRPC or test contracts | test environment docs | Integration and end-to-end tests | No production service may depend on testbed code |

## Transitional Contract Evidence

Existing contracts and transitional service slices may remain during migration,
but later slices must make their target ownership explicit. Current names such
as `forensic-gateway-service`, `repository-analysis-service`,
`java-ast-analysis-service`, `joern-cpg-analysis-service` and
`analysis-store-service` are current evidence only, not FA-MSA-001 aliases.

## Error, Retry And Idempotency Rules

Contract slices must define concrete error and status models. Until then:

- public API errors must not leak private payloads, stack traces, secrets or
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

The repository contains partial service-local contract tests for transitional
services, but no dedicated service-level contract-test Gradle task that covers
every FA-MSA-001 contract family. Later contract slices must add or name an
executable contract-test command before claiming contract readiness.
