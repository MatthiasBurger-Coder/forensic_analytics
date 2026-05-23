# Data Ownership

## Status

FA-MSA-001 Slice 04 data-ownership and persistence split baseline with S11
legacy persistence ownership clarification.

This document assigns target ownership for FA-MSA-001 persistent data areas
before persistence is split or `forensic-analytics-persistence` is retired. It
does not create schemas, tables, migrations, topics, buckets, object prefixes,
graph labels, vector stores or database clients.

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
| Source snapshots | `repository-source-service` owns workspace state, source package bytes, source snapshot descriptors and accepted source metadata | AST, Joern, orchestrator and query/report consumers receive source snapshot IDs, artifact references and diagnostics through owner APIs or file contracts | S04 owner assigned |
| Raw ingestion payload intake | `ingestion-service` owns raw payload intake, intake diagnostics and raw runtime or analysis payload byte custody until an explicit handoff transfers custody | Handoff contracts, owner APIs or accepted/rejected intake events | S04 owner assigned |
| Upload session state | `ingestion-service` | Query/report or orchestrator reads through owner API after contracts exist | S04 owner assigned |
| JavaParser AST/source-fact worker output | `java-parser-analysis-service` owns canonical static Java source facts it produces, source-fact artifact bytes and producer-local artifact metadata | Orchestrator and query/report consumers read through service-owned retrieval APIs, artifact contracts or events | S04 owner assigned |
| Joern CPG/CFG/DFG artifacts | `joern-analysis-service` owns canonical semantic CPG/CFG/DFG facts it produces, semantic artifact bytes and producer-local artifact metadata | Orchestrator and query/report consumers read through service-owned retrieval APIs, artifact contracts or events | S04 owner assigned |
| Analysis job orchestration state | `analysis-orchestrator-service` owns job lifecycle, workflow status, worker leases, worker attempts, retries, timeout state, failure/dead-letter state, correlation references and job-to-artifact references | Query/report API and clients read through orchestrator APIs or events | S04 owner assigned |
| Canonical normalized analysis facts | No single shared canonical fact store. Ownership is service-local by evidence category: repository source metadata by `repository-source-service`; raw/runtime intake records by `ingestion-service`; static Java facts by `java-parser-analysis-service`; semantic graph facts by `joern-analysis-service`; orchestration facts by `analysis-orchestrator-service`; public report projections by `query-report-api-service` | Owner APIs, query interfaces, events or documented projections | S04 owner assigned by category |
| Artifact catalog metadata | The producer that owns artifact bytes owns producer-local catalog metadata. `analysis-orchestrator-service` owns only job-to-artifact references. `query-report-api-service` owns generated report or LLM-ready package metadata. | Owner APIs or scoped object access approved by the byte owner | S04 owner assigned |
| Incident records | Optional future `incident-analysis-service`; no mandatory FA-MSA-001 owner | Owner APIs after a later requirement approves the service | Deferred and blocked until a later requirement |
| Graph/replay projections | Optional future `graph-replay-service` projection owner | Query/report APIs or owner APIs after a later requirement approves the service | Deferred projection only |
| Reports and LLM-ready packages | `query-report-api-service` owns public read models, generated report package state, LLM-ready package state and public cache state. Canonical evidence remains owned by the producing services. | Public APIs and owner APIs for source evidence | S04 owner assigned for mandatory query/report scope; standalone report service remains optional later |
| LLM-generated output | `query-report-api-service` owns stored LLM-generated output only as generated analysis or hypotheses, never verified evidence. No live LLM-output persistence is approved until redaction, retention, access and indexing rules are documented and tested. | Public APIs with generated-output labeling and evidence-owner references | S04 owner assigned; persistence blocked until security rules exist |
| CLI state | `cli-client` | Local only | No forensic evidence ownership |
| Observability data | `observability-stack` for operational configuration and dashboards | Logs/metrics/traces through operational tools | Diagnostics only, not forensic evidence |
| Test data | `testbed` | Test-only | Non-production only |
| Workspace lifecycle state | Retained legacy workspace/application path; no mandatory FA-MSA-001 target service owns this state yet | Current legacy APIs only until a later requirement creates a workspace administration owner or explicitly deprecates the feature | S11 retention-only; blocked for retirement |
| Workspace membership state | Retained legacy workspace/application path; no mandatory FA-MSA-001 target service owns this state yet | Current legacy APIs only until a later requirement creates a workspace administration owner or explicitly deprecates the feature | S11 retention-only; blocked for retirement |
| Project lifecycle state | Retained legacy workspace/application path; no mandatory FA-MSA-001 target service owns this state yet | Current legacy APIs only until a later requirement creates a project administration owner or explicitly deprecates the feature | S11 retention-only; blocked for retirement |
| Project membership state | Retained legacy workspace/application path; no mandatory FA-MSA-001 target service owns this state yet | Current legacy APIs only until a later requirement creates a project administration owner or explicitly deprecates the feature | S11 retention-only; blocked for retirement |
| Workspace and project assets | Retained legacy asset/application path; generated report package ownership remains with `query-report-api-service` only for mandatory query/report scope | Current legacy asset APIs for workspace/project assets; public report package APIs after owner-specific report contracts exist | S11 retention-only for workspace/project assets; report package owner assigned |
| Workspace audit events | Retained legacy audit/application path; no mandatory FA-MSA-001 target service owns audit-grade workspace events yet | Current legacy audit APIs only until a later requirement creates an audit owner or explicitly deprecates the feature | S11 retention-only; durable audit ordering not claimed |
| Retention policy metadata | Retained legacy workspace/application path; no mandatory FA-MSA-001 target service owns this state yet | Current legacy retention APIs only until a later requirement creates a workspace administration owner or explicitly deprecates the feature | S11 retention-only; blocked for retirement |
| Project storage path resolution | Retained legacy storage adapter path for legacy workspace/project storage; producer services own their own artifact/object paths | Legacy storage resolver only for retained workspace/project paths; target services expose owner-issued artifact references or owner APIs, never private paths | S11 retention-only for legacy storage resolver |

## Transitional Ownership Evidence

The current `analysis-store-service` implementation owns a transitional job
lifecycle and artifact metadata subset. It is current implementation evidence,
not the FA-MSA-001 canonical owner.

FA-MSA-001 target ownership splits that predecessor responsibility as follows:

- job lifecycle, worker leases, retry state, failure state, readiness and
  correlation references move to `analysis-orchestrator-service`;
- repository workspaces, source package bytes and source snapshot metadata move
  to `repository-source-service`;
- raw ingestion sessions, raw runtime payload intake and rejected-intake
  diagnostics move to `ingestion-service`;
- static Java source facts and source-fact artifacts move to
  `java-parser-analysis-service`;
- Joern CPG/CFG/DFG semantic facts and semantic artifacts move to
  `joern-analysis-service`;
- generated report packages, LLM-ready packages and public read models move to
  `query-report-api-service`;
- artifact catalog metadata stays producer-local, while the orchestrator keeps
  only job-to-artifact references.

The current `forensic-analytics-persistence` module is a monolith persistence
adapter. It remains current implementation evidence until later service slices
provide service-local persistence, replacement tests and caller-free proof.

S11 clarifies that `forensic-analytics-persistence` contains retained
workspace/project administration, membership, asset, audit, retention and
legacy project-storage behavior that is not part of the mandatory FA-MSA-001
repository-to-BTM target acceptance path. Those areas remain legacy
rollback/current-state evidence. They must not be silently moved to
`analysis-orchestrator-service`, `ingestion-service`,
`query-report-api-service` or another target service without a later
requirement, owner decision, contract impact review, tests and rollback or
deprecation notes.

S11 also clarifies that current in-memory target-service stores prove
service-local replacement direction only. They do not prove durable production
persistence, event sourcing, audit-grade ordering, event outbox behavior,
broker readiness, schema/table names or final caller-free module retirement.

## Artifact Byte Custody Rules

Artifact bytes stay with the producer service that created or accepted them
unless a later explicit contract transfers custody. A consumer may receive only
an owner-issued artifact reference, a bounded response from an owner API, or
scoped object access approved by the owner.

Every artifact handoff must preserve:

- producer service;
- artifact type and schema or media type;
- deterministic identifier or checksum when the contract defines one;
- correlation and source snapshot references where applicable;
- completeness or unavailable state;
- sensitivity classification when known;
- maximum size or chunking policy before large payload transfer;
- retention and cleanup owner;
- redaction obligations before public report or LLM package use.

The orchestrator may persist job-to-artifact references for coordination, but
it must not become the artifact byte owner, producer catalog owner or canonical
fact store.

## Store Types

| Store Type | Owner Rule |
|---|---|
| Relational or transactional store | Owned by the service that owns the canonical data or orchestration state |
| File/object store | Owned by the service responsible for the artifact bytes or exposed through owner-approved scoped object access |
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
