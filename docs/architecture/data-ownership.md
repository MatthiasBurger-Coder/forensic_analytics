# Data Ownership

## Status

FA-MSA-001 Slice 04 data-ownership and persistence split baseline with S11
legacy persistence ownership clarification and S18 public API ownership exit
evidence.

FA-MVP-0001 adds a narrower repository checkout workspace concept. In this
document, "repository checkout workspace" means repository-source checkout
state only. It is separate from the broader platform workspace lifecycle,
membership, project, asset, audit and retention concepts that remain explicit
ownership gaps below.

This document assigns target ownership for FA-MSA-001 persistent data areas.
ADR-0022 and S05 retire the former `forensic-analytics-persistence` source
tree, but they do not by themselves assign durable service storage, schemas,
tables, migrations, topics, buckets, object prefixes, graph labels, vector
stores or database clients.

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
| Repository checkout workspaces | `repository-source-service` owns checkout workspace aggregate state, repository identity, branch state, PostgreSQL metadata schema, workspace directories and durable idempotency for repository-source operations | Source snapshot IDs, artifact references, sanitized diagnostics and owner APIs | ADR-0024 owner clear |
| Source snapshots | `repository-source-service` owns repository checkout workspace state, source package bytes, source snapshot descriptors, source snapshot references and accepted source metadata | AST, Joern, orchestrator and query/report consumers receive source snapshot IDs, artifact references and diagnostics through owner APIs or file contracts | S04 owner assigned |
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
| Platform workspace lifecycle state | Historical workspace/application predecessor path; no mandatory FA-MSA-001 target service owns this state yet. This is not FA-MVP-0001 repository checkout workspace state. | No active legacy API remains after S05; later requirements must create an owner or explicitly deprecate the feature | S11 ownership gap remains explicit |
| Platform workspace membership state | Historical workspace/application predecessor path; no mandatory FA-MSA-001 target service owns this state yet. This is not FA-MVP-0001 repository checkout workspace state. | No active legacy API remains after S05; later requirements must create an owner or explicitly deprecate the feature | S11 ownership gap remains explicit |
| Project lifecycle state | Historical workspace/application predecessor path; no mandatory FA-MSA-001 target service owns this state yet | No active legacy API remains after S05; later requirements must create an owner or explicitly deprecate the feature | S11 ownership gap remains explicit |
| Project membership state | Historical workspace/application predecessor path; no mandatory FA-MSA-001 target service owns this state yet | No active legacy API remains after S05; later requirements must create an owner or explicitly deprecate the feature | S11 ownership gap remains explicit |
| Platform workspace and project assets | Historical asset/application predecessor path; generated report package ownership remains with `query-report-api-service` only for mandatory query/report scope | Public report package APIs after owner-specific report contracts exist; workspace/project asset owner remains undecided | S11 ownership gap remains explicit for workspace/project assets; report package owner assigned |
| Platform workspace audit events | Historical audit/application predecessor path; no mandatory FA-MSA-001 target service owns audit-grade workspace events yet | Later requirements must create an audit owner or explicitly deprecate the feature | S11 ownership gap remains explicit; durable audit ordering not claimed |
| Retention policy metadata | Historical workspace/application predecessor path; no mandatory FA-MSA-001 target service owns this state yet | Later requirements must create a workspace administration owner or explicitly deprecate the feature | S11 ownership gap remains explicit |
| Project storage path resolution | Historical storage adapter predecessor path for workspace/project storage; producer services own their own artifact/object paths | Target services expose owner-issued artifact references or owner APIs, never private paths | S11 ownership gap remains explicit for legacy storage resolver behavior |

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

The former `forensic-analytics-persistence` module was a monolith persistence
adapter. It is retired source-tree evidence after ADR-0022 and S05; its
deletion does not prove durable target persistence or assign ownership for
deferred workspace/project behaviors.

S11 clarified that `forensic-analytics-persistence` contained retained
workspace/project administration, membership, asset, audit, retention and
legacy project-storage behavior that is not part of the mandatory FA-MSA-001
repository-to-BTM target acceptance path. Those areas remain explicit ownership
gaps after S05. They must not be silently moved to
`analysis-orchestrator-service`, `ingestion-service`,
`query-report-api-service` or another target service without a later
requirement, owner decision, contract impact review, tests and rollback or
deprecation notes.

S11 also clarifies that current in-memory target-service stores prove
service-local replacement direction only. They do not prove durable production
persistence, event sourcing, audit-grade ordering, event outbox behavior,
broker readiness, schema/table names or final caller-free module retirement.

S18 adds target-service public API and CLI client contract-test ownership for
the repository-to-BTM submission/status path. This closes only the executable
public API contract-test ownership gap for that target path. It does not assign
legacy workspace/project administration, membership, asset, audit, retention
or project-storage persistence to a target service.

ADR-0024 moves repository-source workspace metadata from the Docker-local H2
MVP adapter to service-owned PostgreSQL. The selected PostgreSQL scope is
limited to repository checkout workspace, branch, repository preparation and
idempotency records. It is service-local one-writer storage, not shared
cross-service persistence, not canonical analytics persistence and not a
production relational database selection for the broader platform.

Repository checkout bytes and source package bytes remain outside PostgreSQL in
repository-source-owned storage. Existing H2 files remain historical MVP
evidence until the H2 retirement slice removes active runtime fallback or
records an explicit migration policy.

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
