# Data Ownership

## Status

FA-MSA-001 Slice 01 data-ownership baseline.

This document assigns target ownership candidates and identifies decisions that
must be completed in Slice 04 before persistence is split or
`forensic-analytics-persistence` is retired. It does not create schemas,
tables, migrations, topics, buckets or database clients.

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
- graph, report, vector or LLM projections as primary evidence;
- treating operational logs as runtime execution evidence.

## Target Ownership Matrix

| Data Area | Target Owner / One Writer | Non-Owner Access Path | Status |
|---|---|---|---|
| Repository workspaces | `repository-source-service` | Source snapshot IDs, artifact references and owner APIs | Target owner clear |
| Source snapshots | `repository-source-service` for workspace/source package; S04-approved canonical owner for accepted metadata | AST, Joern and orchestrator receive explicit references | Target owner partly clear; accepted metadata owner requires S04 |
| Raw ingestion payload intake | `ingestion-service` | Handoff contract or owner API | Target owner clear |
| Upload session state | `ingestion-service` | Query/report or orchestrator reads through owner API after contracts exist | Target owner clear |
| JavaParser AST/source-fact worker output | `java-parser-analysis-service` until accepted or transferred | S04-approved owner reads through service-owned retrieval API or artifact contract | Target owner clear for worker output |
| Joern CPG/CFG/DFG artifacts | `joern-analysis-service` until accepted or transferred | S04-approved owner reads through service-owned retrieval API or artifact contract | Target owner clear for worker output |
| Analysis job orchestration state | `analysis-orchestrator-service` if S04 confirms it | Query/report API reads through orchestrator API | Requires S04 confirmation |
| Canonical normalized analysis facts | S04 decision required | Owner APIs, query interfaces or events | Open until S04 |
| Artifact catalog metadata | S04 decision required | Owner APIs | Open until S04 |
| Incident records | Optional future `incident-analysis-service` or S04-approved owner | Owner APIs | Deferred unless made mandatory |
| Graph/replay projections | Optional future `graph-replay-service` or S04-approved projection owner | Query/report APIs | Deferred unless made mandatory |
| Reports and LLM-ready packages | `query-report-api-service` for public aggregation; standalone report owner optional later | Public APIs | Target owner partly clear; generated package ownership requires later decision |
| LLM-generated output | S04 or later report/LLM decision | Public APIs with generated-output labeling | Deferred |
| CLI state | `cli-client` | Local only | No forensic evidence ownership |
| Observability data | `observability-stack` for operational configuration and dashboards | Logs/metrics/traces through operational tools | Diagnostics only, not forensic evidence |
| Test data | `testbed` | Test-only | Non-production only |

## Transitional Ownership Evidence

The current `analysis-store-service` implementation owns a transitional job
lifecycle and artifact metadata subset. It is not automatically the
FA-MSA-001 canonical owner. S04 must decide whether orchestration state,
canonical facts, artifact catalog metadata or report/query state belong to
`analysis-orchestrator-service`, `query-report-api-service`, service-local
persistence, or a later explicit service.

The current `forensic-analytics-persistence` module is a monolith persistence
adapter. It remains current implementation evidence until S04 and later
service slices provide one-writer ownership, replacement tests and caller-free
proof.

## Store Types

| Store Type | Owner Rule |
|---|---|
| Relational or transactional store | Owned by the service that owns the canonical data |
| File/object store | Owned by the service responsible for the artifact bytes or exposed through scoped object access |
| Event store | Owned by the producer or broker governance decision recorded in the contract slice |
| Graph store | Projection owner only; never source of truth |
| Vector store | Projection only; owner depends on a later LLM/context decision |
| Operational logs | Diagnostics only; not canonical forensic evidence |

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
