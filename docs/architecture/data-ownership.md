# Data Ownership

## Status

Slice 01 data-ownership baseline for the microservices ecosystem conversion
workflow.

This document assigns planned data owners and cross-service access paths. It
does not create schemas, tables, migrations, topics, buckets or database
clients.

## Ownership Rules

Every persistent data type must have:

- one owner service;
- one write path;
- explicit non-owner read paths;
- provenance and correlation preservation;
- completeness or missing-evidence representation when facts are partial;
- redaction and retention decisions before sensitive runtime data is stored or
  indexed.

Forbidden:

- direct cross-service database access;
- shared private tables;
- shared Java entity modules;
- shared repository modules;
- shared DTO/domain modules;
- graph, vector or report projections as primary evidence;
- treating operational logs as runtime execution evidence.

## Ownership Matrix

| Data Area | Owner / One Writer | Non-Owner Access Path | Notes |
|---|---|---|---|
| Raw ingestion payload intake | `forensic-ingestion-service` | Analysis Store reads through ingestion API or event handoff | Preserve schema version, provenance and correlation |
| Upload session state | `forensic-ingestion-service` | Gateway reads through ingestion or Analysis Store views after contracts exist | Transport lifecycle is separate from canonical analysis state |
| Canonical normalized analysis facts | `analysis-store-service` | Owner APIs, query interfaces or events | Planned owner; durable fact schemas are not implemented yet |
| Analysis sessions and jobs | `analysis-store-service` for canonical job lifecycle state; `forensic-ingestion-service` for upload-session lifecycle | Gateway and frontend read via Gateway and owner APIs | The current service-local gRPC implementation covers the analysis job lifecycle subset |
| Workspace/project metadata | `analysis-store-service` unless a later slice records a narrower owner | Gateway reads through owner APIs | Current code stores these in monolith in-memory repositories |
| Audit and retention metadata | `analysis-store-service` unless a later slice records a narrower owner | Gateway or admin APIs through owner | Runtime values remain sensitive |
| Incident records | `analysis-store-service` | Graph/replay/report query owner APIs | Planned owner; not implemented yet |
| Correlation indexes | `analysis-store-service` | Graph/replay/report query owner APIs | Planned owner; operational `CorrelationContext` logs are diagnostics, not evidence |
| Artifact catalog metadata | `analysis-store-service` | Owner APIs | The current service implementation registers path/reference, category, checksum, size, producer, schema version and completeness metadata |
| Raw evidence artifact bytes | Producing service until an explicit byte-handoff or object-store ownership contract transfers byte custody | Scoped owner APIs or signed object access after design | Analysis Store owns accepted artifact metadata only unless a later contract slice assigns byte custody; no shared filesystem or bucket-prefix coupling |
| Repository workspaces | `repository-analysis-service` | Immutable source snapshot or artifact references | Other services must not use workspace internals directly |
| Source snapshots | `repository-analysis-service` for workspace/source package; `analysis-store-service` for accepted snapshot metadata | AST and Joern receive references through contracts | Snapshot identity must be deterministic and pinned to a resolved commit SHA |
| Complete build-output packages | `build-artifact-worker-service` when introduced; otherwise the verified external artifact producer keeps byte custody | Analysis Store metadata with `ArtifactByteAccess`; Joern reads through owner API or validated artifact access | Artifactory and Jenkins are optional producers only; checksum or manifest mismatch is terminal |
| AST worker output and source-fact artifact bytes | `java-ast-analysis-service` until accepted or transferred through an explicit byte-handoff/object-store contract; `analysis-store-service` for canonical facts and accepted metadata | Analysis Store retrieves bytes through the verified Java AST owner API using service-local generated client stubs only; downstream reads accepted facts from Analysis Store | Unresolved symbols remain explicit; `ArtifactByteAccess.retrieval_contract` must name a real owner API before source-fact bytes are consumed |
| Joern CPG/CFG/DFG artifacts | `joern-cpg-analysis-service` for execution artifacts; `analysis-store-service` for accepted semantic facts and references | Graph/replay reads through owner APIs | Incomplete mappings remain explicit |
| Instrumentation target selection | `analysis-store-service` | BTM Generation receives bounded target snapshots through `contracts/grpc/btm-generation.proto` | Targets are derived from accepted facts and semantic artifacts; they are not runtime execution evidence |
| BTM rule artifacts | `btm-generation-service` owns generated bytes until an explicit byte-handoff or object-store ownership contract transfers byte custody; `analysis-store-service` owns accepted artifact metadata only | Gateway/report reads through Gateway/public APIs, Analysis Store metadata and BTM owner APIs after contracts exist | Rule IDs must be stable; metadata registration does not transfer byte ownership |
| Graph projections | `graph-replay-service` | Gateway/report APIs | Rebuildable projection, not source of truth |
| Replay projections | `graph-replay-service` | Gateway/report APIs | Missing evidence is represented explicitly |
| Reports | `report-generation-service` | Gateway/report APIs | Reports distinguish evidence, derived facts, gaps and hypotheses |
| LLM-ready packages | `report-generation-service` | Gateway/report APIs | Package construction must be reproducible |
| LLM-generated output | `report-generation-service` if live generation is later approved | Gateway/report APIs | Label as generated analysis or hypothesis, never evidence |
| Frontend state | `frontend-web-app` | Browser-local only | Frontend owns no forensic evidence |
| Gateway request, status and delivery facade state | `forensic-gateway-service` only for public facade state | Gateway APIs | Gateway must not own canonical facts, worker orchestration state or artifact bytes |
| Repository-to-BTM orchestration state | `analysis-store-service` unless Slice 11 records another reviewed owner | Gateway submits through the approved owner API; worker services receive explicit jobs or contracts | Gateway remains facade-only; orchestration state must preserve correlation, idempotency, completeness and retry provenance |

## Store Types

| Store Type | Owner Rule |
|---|---|
| Relational or transactional store | Owned by the service that owns the canonical data |
| File/object store | Owned by the service responsible for the artifact bytes or exposed through scoped object access |
| Event store | Owned by the producer or broker governance decision recorded in the contract slice |
| Graph store | Owned by `graph-replay-service` as a projection |
| Vector store | Projection only; owner depends on a later LLM/context decision |
| Operational logs | Diagnostics only; not canonical forensic evidence |

## Analysis Store Implementation Status

`analysis-store-service` now has an independent Spring Boot service boundary and
service-local `AnalysisJobService` gRPC adapter. In the target Slice 03
contract, it is also the owner of accepted bounded instrumentation target
selection metadata derived from accepted facts. Its current implemented write
authority is limited to:

- analysis run IDs, job IDs and source snapshot IDs;
- analysis job state, worker kind, attempts, lease owner and lease expiry;
- job diagnostics and failure metadata;
- input and output artifact metadata references;
- idempotent job lifecycle operations.

It does not yet implement durable database access, migrations, normalized static
facts, runtime facts, incident records, correlation indexes, graph labels or
private storage tables. Those remain later slices that require explicit
contracts, storage decisions and migration evidence.

## Build Artifact Worker Ownership Status

`build-artifact-worker-service` is an approved planned owner for complete
build-output package bytes only after a workflow slice creates its service
boundary, contract and runtime evidence. Until then, Artifactory, Artifact
Store or Jenkins references are optional external producer inputs and do not
write canonical Analysis Store state directly.

Analysis Store may register build-output package metadata, provenance,
completeness and `ArtifactByteAccess`, but it must not take byte custody unless
a later explicit byte-handoff contract assigns that ownership.

## Java AST Source-Fact Byte Ownership Status

`java-ast-analysis-service` remains the owner of produced source-fact artifact
bytes until a later explicit byte-handoff or object-store contract transfers
custody. Analysis Store may accept source-fact metadata and canonical normalized
facts, but it may retrieve source-fact bytes only through a verified Java AST
owner API using service-local generated client stubs. Direct reads of
Repository Analysis workspaces, Java AST private files, private object prefixes
or another service's implementation classes remain forbidden.

## Cross-Service Read Rules

Non-owner services may read only through:

- owner APIs;
- published events;
- documented projections;
- defined query interfaces;
- scoped artifact/object access approved by the owner.

Non-owner services must not read another service's private database tables,
private filesystem paths, private object prefixes or private generated classes.

## Evidence Integrity Rules

Data ownership must preserve:

- evidence source;
- origin system;
- correlation identifiers;
- ordering or timestamp when available;
- producer service;
- schema version;
- completeness or missing-evidence status;
- sensitivity classification when known;
- checksums or deterministic identifiers for artifacts when defined.

Static facts, runtime facts, replay-derived facts, graph projections,
generated reports and LLM hypotheses must remain distinguishable.

## Stop Conditions

Stop a later slice when:

- an owner or write path is unclear;
- more than one service writes the same canonical data;
- direct cross-service database access is proposed;
- a shared Java persistence, DTO, domain or repository module is proposed;
- table, event, topic, bucket, graph label or schema field names would need
  guessing;
- sensitive runtime data would be persisted without redaction and retention
  decisions;
- a projection would become the source of truth.
