# Requirement Alignment - 2026-06-04

## Purpose

This document aligns the current requirement baseline with the consolidated
architecture baseline recorded in
`docs/arc42/09-architecture-decisions/adr/ADR-0025-consolidated-architecture-baseline-without-migration.md`.

It is a traceability document. It does not create new requirements, close open
decisions, claim implementation status, or change ADR history.

## Verified Requirement Sources

- `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.2.md`
- `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.1.md`
- `docs/arc42/01-introduction-and-goals.md`
- `docs/arc42/10-quality-requirements.md`
- `docs/arc42/11-risks-and-technical-debt.md`
- `docs/arc42/09-architecture-decisions/adr/ADR-0025-consolidated-architecture-baseline-without-migration.md`

EPIC v0.2 is the current requirement baseline. EPIC v0.1 is historical because
v0.2 explicitly supersedes it.

## Alignment Result

The EPIC v0.2 requirement target aligns with ADR-0025 when these constraints
are kept explicit:

- Analytics owns canonical analysis semantics, normalization, evidence
  provenance, correlation, replay, graph projection rules, report context and
  LLM evidence package construction.
- Producers own local build context capture, artifact packaging, transport
  initiation and runtime binding when server-generated instrumentation plans
  require it.
- Producer metadata is provenance only. It must not define Analytics domain
  semantics, canonical storage shape, graph labels, replay truth or LLM
  evidence truth.
- Static and semantic facts remain candidates or documented derivations unless
  runtime evidence proves execution.
- Runtime values are sensitive by default, and missing or ambiguous evidence
  must stay explicit.
- LLM output is generated analysis, not verified evidence.
- Target architecture and planned capabilities must not be documented as
  implemented behavior without verified repository source, tests, runtime
  evidence or accepted architecture documentation.

## Requirement-To-ADR Traceability

| Requirement area | Verified requirement source | ADR-0025 alignment |
|---|---|---|
| Producer/platform boundary | EPIC v0.2 Non-Negotiable Platform Boundary; Analysis Input Contract | Plugins and producers are adapters. Server-side Analytics owns parser execution, Joern execution, BTM generation, normalization, persistence, replay, graph projection and LLM analysis. |
| Canonical evidence model | EPIC v0.2 Executive Summary; Acceptance Criteria 1 and 2 | The canonical analysis model is the source for normalized analysis facts. |
| Static and semantic facts | EPIC v0.2 Static Analysis Fact Model; Semantic Analysis Fact Model | Static reachability and semantic relationships must not be presented as actual runtime execution without evidence. |
| Runtime observations | EPIC v0.2 Runtime Observation And Event Model; Acceptance Criteria 13 and 14 | Runtime values are sensitive by default. Missing runtime values remain missing unless observed or explicitly derived by a documented rule. |
| Instrumentation planning | EPIC v0.2 Instrumentation Planning Model; Rule-Set Artifact Model | Analytics owns instrumentation planning and server-side rule-set generation through replaceable adapters. |
| Replay and correlation | EPIC v0.2 Correlation And Replay Responsibilities; Acceptance Criteria 3 and 4 | Replay starts from explicit exception or correlation context and separates observed runtime facts, derived analysis facts, static context, missing evidence and hypotheses. |
| Graph and vector projections | EPIC v0.2 Executive Summary; Open Decisions OD-002 and OD-003 | Graph and vector stores are projections derived from the canonical model; their concrete technologies remain open. |
| LLM evidence packages | EPIC v0.2 Reporting And LLM Evidence Packaging; Acceptance Criteria 6 | LLM output is an assistant-generated hypothesis, explanation or recommendation, not evidence. |
| Service autonomy | ADR-0025; EPIC v0.2 Planned-Vs-Implemented Discipline | The target service landscape is architecture direction only until build, start, configuration, healthcheck, container and deployment evidence exists. |
| Persistence ownership | EPIC v0.2 Open Decisions OD-001; ADR-0025 | PostgreSQL is selected only for repository-source workspace metadata. Broader Analytics persistence remains open. |
| Quality and process gates | `docs/arc42/10-quality-requirements.md`; ADR-0025 | Workflow slices require explicit quality evidence, and missing facts must not be invented to pass gates. |

## Open Decisions Kept Open

The following EPIC v0.2 open decisions remain open after this alignment:

- OD-001 Initial relational database, except the bounded repository-source
  workspace metadata decision recorded by ADR-0024 and consolidated by
  ADR-0025.
- OD-002 Initial graph database.
- OD-003 Initial vector database.
- OD-004 Runtime ingestion mode.
- OD-005 Runtime value storage policy.
- OD-006 Initial LLM provider.
- OD-007 Manifest and checksum contract.
- OD-008 Contract compatibility for repository analysis RPCs.
- OD-009 Retry, deadline, cancellation and idempotency policy.
- OD-010 Multi-repository and multi-service trace model.

None of these open decisions is closed by S06.

## Explicit Non-Requirements For This Alignment

This alignment does not require or claim:

- concrete REST, gRPC, protobuf or event field names;
- build-tool task names, Maven goal names or plugin extension names;
- producer helper class names or producer package names;
- concrete database, graph database, vector database or LLM provider choices;
- runtime replay, graph service, report generation, UI coverage or live LLM
  provider integration as implemented behavior;
- production deployment automation, Swarm readiness or Kubernetes readiness.

## Residual Requirement Risks

| Risk | Required handling |
|---|---|
| Producer leakage into Analytics semantics | Keep producer-specific names and paths out of Analytics core requirements unless a future contract workflow verifies them. |
| Static facts presented as execution | Keep static and semantic facts separate from runtime observations and replay evidence. |
| Missing package data silently repaired | Represent missing, ambiguous and rejected payloads explicitly. |
| Sensitive runtime data exposure | Preserve sensitive-by-default runtime handling and redaction/audit requirements. |
| Open persistence decisions over-closed | Keep repository-source PostgreSQL bounded and broader Analytics persistence open. |
| Target service landscape treated as implemented readiness | Require repository evidence and quality commands before readiness claims. |

## S06 Decision

S06 confirms that the current requirement baseline can reference ADR-0025 as
the consolidated architecture baseline, provided the open decisions and
planned-versus-implemented distinction above remain visible.
