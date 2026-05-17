# Three Amigos Decision Record

## Slice

| Field | Value |
|---|---|
| workflowVersion | `forensics-tracing-analytics-epic-alignment-20260516` |
| sliceId | `03` |
| sliceTitle | Three Amigos Requirement Review |
| sourceFactMatrix | `docs/workflow/forensics-tracing-fact-matrix.md` |
| sourceGapAnalysis | `docs/workflow/analytics-epic-gap-analysis.md` |

## Review Result

Slice 03 read-only review passed. EPIC v0.2 does not exist yet, which is
expected before Slice 05.

Question: Does the implementation still match the EPIC?

Answer: partially. ADRs and arc42 already support server-owned Analytics
responsibilities, but EPIC v0.1 needs the producer-neutral v0.2 update described
by this workflow.

## Candidate Decisions

| Candidate EPIC change | Classification | Decision | Five-perspective finding |
|---|---|---|---|
| Analytics-owned normalization, persistence boundaries, replay, graph, reporting context and LLM evidence packaging. | Functional requirement, architecture constraint | ACCEPTED_FOR_EPIC | Requirement: core platform scope. Architecture: matches ADR-0001, ADR-0002 and arc42. Backend: no code change in this workflow. Frontend: no UI implementation claim. Quality: leakage search can verify wording. |
| Artifact-package ingestion as an Analytics input path equivalent to server-side analysis after normalization. | Functional requirement, architecture constraint | ACCEPTED_FOR_EPIC | Requirement: needed in EPIC v0.2. Architecture: producer-neutral. Backend: future adapter or use-case impact only. Frontend: no direct browser gRPC. Quality: testable through documentation now and later contract fixtures. |
| Manifest, checksum, provenance, completeness and rejection semantics at requirement level. | Non-functional requirement, resilience requirement, observability requirement, security requirement, quality-gate requirement | ACCEPTED_FOR_EPIC | Requirement: evidence integrity. Architecture: canonical model and provenance aligned. Backend: future validation ports. Frontend: later gap display only. Quality: supports deterministic rejection tests. |
| Static, semantic and runtime fact models with missing, partial or invalid facts explicit. | Functional requirement, quality-gate requirement, security requirement | ACCEPTED_FOR_EPIC | Requirement: core analysis input. Architecture: canonical model. Backend: future typed models. Frontend: evidence category display later. Quality: deterministic fixtures required. |
| Correlation across static facts, semantic artifacts, runtime traces, incidents, replay, graph, reports and LLM packages. | Functional requirement, observability requirement, quality-gate requirement | ACCEPTED_FOR_EPIC | Requirement: central platform behavior. Architecture: matches canonical identifiers. Backend: no current code change. Frontend: no implemented replay or UI claim. Quality: acceptance criteria can verify traceability. |
| Producer metadata as provenance only. | Architecture constraint, observability requirement, assumption | ACCEPTED_FOR_EPIC | Requirement: prevents plugin behavior from becoming core semantics. Architecture: matches plugin-as-producer boundary. Backend and frontend: no code change. Quality: leakage scans can verify. |
| Planned-vs-implemented wording discipline for replay, graph, report, LLM and UI capabilities. | Quality-gate requirement, UX requirement, assumption | ACCEPTED_FOR_EPIC | Requirement: avoids false capability claims. Architecture: supports evidence-first documentation. Backend and frontend: impact-only. Quality: marker and wording scans required. |
| Gradle or Maven task names, Mojo or task classes, helper classes, default ports, local paths, H2/cache behavior and quickstart coordinates. | Assumption | REJECTED_AS_PLUGIN_SPECIFIC | Requirement: not platform requirements. Architecture: producer-local. Backend and frontend: no adoption. Quality: leakage audit must reject these. |
| Exact database, graph database, vector database, LLM provider and runtime collector choices. | Architecture constraint, scalability requirement, open question | DEFERRED_AS_OPEN_DECISION | Requirement: not needed for EPIC v0.2 requirements. Architecture: already open in EPIC and arc42. Backend and frontend: future workflows. Quality: cannot test without a decision. |
| Producer `traceId` and `parentSpanId` mapping semantics. | Observability requirement, open question | DEFERRED_AS_OPEN_DECISION | Requirement: canonical identifiers remain valid, but producer helper evidence is not verified as Analytics truth. Architecture: contract review needed. Backend: no inference. Quality: no fabricated correlation facts. |
| Exact proto field changes, retry, deadline, idempotency policy and compatibility behavior. | Resilience requirement, architecture constraint, quality-gate requirement | MOVE_TO_ADR_OR_ARC42 | Requirement: too concrete for EPIC text. Architecture: contract-first ADR or arc42 governance. Backend: future contract slice. Frontend: no direct impact. Quality: requires contract tests. |
| Related docs and arc42 current-baseline updates after EPIC v0.2 exists. | Documentation governance, architecture constraint, UX requirement | MOVE_TO_ADR_OR_ARC42 | Requirement: not EPIC content itself. Architecture and docs sync are owned by later slices. Backend and frontend: no code change. Quality: documentation consistency check. |

## Decision

EPIC v0.2 can be drafted after Slice 04 completes. Slice 05 must still wait for
the contract and producer-boundary comparison because the workflow dependency
graph requires both Slice 03 and Slice 04 before EPIC drafting.

## Verification

```bash
rg -n "ACCEPTED_FOR_EPIC|REJECTED_AS_PLUGIN_SPECIFIC|DEFERRED_AS_OPEN_DECISION|MOVE_TO_ADR_OR_ARC42" docs/workflow/three-amigos-decision-record.md
```
