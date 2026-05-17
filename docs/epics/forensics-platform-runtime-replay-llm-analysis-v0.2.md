# EPIC: Forensics Platform - Exception-Centered Runtime Replay and LLM-Assisted Failure Analysis

**Status:** Draft
**Version:** 0.2
**Date:** 2026-05-17
**Role:** Senior Requirement Engineer
**Project Context:** Forensic Analytics Platform
**Supersedes:** `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.1.md`

## Source Note

EPIC v0.2 refines the archived v0.1 requirement baseline after comparing the
current `forensics_tracing` producer description with the Analytics
architecture documents and gRPC contract. EPIC v0.1 remains historical. This
version defines producer-neutral platform requirements and does not import
producer implementation details into Analytics core behavior.

## Executive Summary

The Forensic Analytics Platform is an independent analysis platform for Java
systems. It combines static source facts, semantic analysis facts, runtime
observations, exception context, replay timelines, graph projections, reports
and LLM evidence packages.

The platform must support two equivalent input paths:

- server-side repository analysis,
- producer-supplied artifact package ingestion.

Both paths normalize into the same Analytics-owned canonical analysis model.
Downstream replay, graph, reporting and LLM evidence packaging must not depend
on whether evidence came from a server-side scanner, uploaded artifact package,
build-tool producer, runtime import or semantic analysis adapter.

## Non-Negotiable Platform Boundary

Analytics owns:

- canonical analysis semantics,
- normalization of static, semantic and runtime facts,
- evidence provenance and completeness modeling,
- persistence boundaries for canonical evidence and projections,
- correlation between source facts, semantic facts, rule artifacts and runtime
  observations,
- exception-centered incident creation,
- replay timeline construction,
- graph projection rules,
- report context preparation,
- LLM evidence package construction.

Producers own:

- local build-tool configuration,
- local project and module context capture,
- transport session initiation,
- producer-side artifact packaging,
- runtime binding when a server-generated instrumentation plan requires it.

Producer metadata is provenance only. It must not define Analytics domain
semantics, canonical storage shape, graph labels, replay truth or LLM evidence
truth.

## Analysis Input Contract

Analytics accepts analysis input through producer-neutral request concepts:

- Analysis Request,
- Artifact Package,
- Static Fact Payload,
- Semantic Fact Payload,
- Runtime Event Payload,
- Instrumentation Plan,
- Rule-Set Artifact,
- Diagnostic Report Payload.

These are requirement-level concepts. This EPIC does not change REST, gRPC,
protobuf or event contracts. Exact field names, retry policy, deadline policy,
idempotency behavior, reserved-field policy and compatibility behavior require
a dedicated contract workflow or ADR.

## Artifact Package Ingestion

Analytics must support producer-supplied artifact packages as a first-class
input path.

An artifact package represents analysis-relevant material collected or packaged
outside Analytics core. Analytics must validate the package before import and
then normalize accepted payloads into the canonical analysis model.

Artifact package ingestion must support:

- package identity,
- producer provenance,
- schema version,
- payload list,
- payload kind,
- content type,
- source metadata,
- checksum or integrity metadata,
- optional runtime evidence,
- completeness or rejection diagnostics.

Partial or invalid packages must not be silently repaired. Missing facts,
ambiguous facts and rejected payloads must be represented explicitly.

## Artifact Integrity And Provenance

Every imported package or payload must preserve enough provenance for later
review:

- producer identity,
- analysis run identity when available,
- repository or project identity when available,
- module identity when available,
- schema version,
- payload kind,
- source metadata,
- checksum or integrity status,
- import status,
- diagnostics for rejected or incomplete input.

Checksum and manifest verification are required at requirement level. The exact
manifest format and checksum contract are deferred to a future contract-governed
slice.

## Static Analysis Fact Model

Analytics must normalize static source facts into a producer-neutral model.

Static facts include:

- source files,
- packages,
- classes,
- methods and constructors,
- method signatures,
- source locations,
- entry and exit candidates,
- returns,
- throws,
- branches,
- switch and case structures,
- call-site candidates,
- module and source-root ownership.

Static relationships are candidates unless proven by runtime evidence or a
documented derivation. Static reachability must never be presented as actual
execution.

## Semantic Analysis Fact Model

Analytics must support semantic analysis facts behind replaceable adapters.

Semantic facts include:

- semantic analysis run identity,
- call graph nodes and edges,
- static call relations,
- control-flow relations,
- data-flow paths,
- semantic slices,
- semantic anchors,
- mapping confidence,
- ambiguity markers.

Semantic facts must preserve unresolved, ambiguous or unsupported references
instead of converting them into false certainty.

## Runtime Observation And Event Model

Analytics must normalize runtime observations without fabricating missing
runtime values.

Runtime observations include:

- timestamp,
- event type,
- thread identity,
- correlation identifier,
- trace identifier when available,
- span identifier when available,
- parent span identifier when available,
- rule identifier or observation point identifier,
- class, method, branch or call-site key when available,
- details object,
- redaction status,
- exception metadata,
- error metadata.

Runtime values are sensitive by default. Missing parameters, return values,
branch decisions or stack frames must remain missing unless they were actually
observed or explicitly derived by a documented rule.

## Instrumentation Planning Model

Analytics owns instrumentation planning. A plan identifies observation points
and expected event families without binding Analytics core to a concrete
producer implementation.

Instrumentation plans may describe:

- selected classes,
- selected methods,
- selected branches,
- selected exception points,
- selected call sites,
- rule identifiers,
- rule-set version,
- sampling profile,
- redaction policy,
- expected event families,
- runtime overhead expectation.

Concrete runtime binders, helper classes and build-tool tasks are adapters or
producer implementation details.

## Rule-Set Artifact Model

Analytics may generate rule-set artifacts from instrumentation plans through
replaceable adapters. Rule-set artifacts must remain traceable to the plan and
to the source facts that justified them.

Rule-set artifacts must preserve:

- rule-set identity,
- rule identifiers,
- instrumentation plan identity,
- generation timestamp or generation order when available,
- source fact references,
- expected runtime event family,
- redaction policy reference,
- compatibility or limitation diagnostics.

## Correlation And Replay Responsibilities

Analytics owns correlation between:

- static facts,
- semantic facts,
- rule-set artifacts,
- runtime observations,
- exception incidents,
- replay timeline steps,
- graph projection nodes and edges,
- report sections,
- LLM evidence package entries.

Exception-centered replay must start from explicit exception or correlation
context. Replay output must separate observed runtime facts, derived analysis
facts, static context, missing evidence and hypotheses.

## Reporting And LLM Evidence Packaging

Analytics reports must distinguish:

- confirmed evidence,
- derived analysis,
- unresolved gaps,
- hypotheses,
- suggested fixes,
- verification status.

LLM integration is an analysis assistant, not an evidence source. LLM evidence
packages must be built from explicit evidence and must label generated output
as hypothesis, explanation, recommendation or generated text.

LLM output must never overwrite evidence or become a verified fact without
human or test-backed confirmation.

## Explicitly Excluded From Analytics Core

The following are excluded from Analytics core requirements:

- build-tool task names,
- Maven goal names,
- plugin extension names,
- producer helper class names,
- producer Java package names,
- producer default ports,
- producer transport defaults,
- producer deadlines and retry defaults,
- local output paths,
- local target paths,
- cleanup policies,
- cache behavior,
- producer-local H2 or file schemas as canonical Analytics schemas,
- quickstart project identifiers,
- sample repository URLs,
- local machine setup values,
- concrete database, graph database, vector database or LLM provider choices.

These items may appear in producer documentation, adapter documentation,
examples or future contract workflows, but they must not define Analytics
domain behavior.

## Planned-Vs-Implemented Discipline

This EPIC is a requirement target. It does not claim that every capability is
implemented.

Documentation and future workflows must label behavior as planned, target,
conceptual or implemented based on verified source, tests, runtime behavior or
accepted architecture documentation.

The platform must not claim runtime replay, graph/replay service readiness,
durable persistence, report generation, UI coverage or live LLM provider
integration as implemented unless that claim is verified from repository source
and tests.

## Open Decisions

| ID | Open Decision | Notes |
|---|---|---|
| OD-001 | Initial relational database | Not selected in EPIC v0.2. |
| OD-002 | Initial graph database | Graph store remains a replaceable projection decision. |
| OD-003 | Initial vector database | Vector store remains a replaceable projection decision. |
| OD-004 | Runtime ingestion mode | Collector mode requires a later workflow. |
| OD-005 | Runtime value storage policy | Redaction, retention and audit policy need dedicated design. |
| OD-006 | Initial LLM provider | Provider must remain replaceable. |
| OD-007 | Manifest and checksum contract | Exact format and schema require contract governance. |
| OD-008 | Contract compatibility for repository analysis RPCs | Requires contract workflow or ADR. |
| OD-009 | Retry, deadline, cancellation and idempotency policy | Requires contract and resilience governance. |
| OD-010 | Multi-repository and multi-service trace model | Needs later design. |

## Risks

| Risk | Description | Mitigation |
|---|---|---|
| Producer leakage | Producer implementation details could be mistaken for platform semantics. | Explicit exclusions, leakage audit and contract governance. |
| False execution certainty | Static facts could be presented as runtime execution. | Evidence categories, replay rules and acceptance criteria. |
| Incomplete packages | Missing or invalid payloads could be silently normalized. | Explicit rejection, completeness diagnostics and provenance. |
| Sensitive runtime data | Runtime values, stack traces or source payloads may contain secrets or personal data. | Sensitive-by-default handling, redaction, audit and retention rules. |
| Contract drift | Producer-local proto or adapter behavior could drift from Analytics contracts. | Contract-first workflows, compatibility review and contract tests. |
| LLM hallucination | Generated explanations may invent causes, files or fixes. | Evidence-only packaging and generated-output labeling. |

## Acceptance Criteria

1. Analytics owns the canonical analysis model.
2. Analytics owns normalization of static, semantic and runtime facts.
3. Analytics owns correlation between source facts, semantic facts, rule sets
   and runtime events.
4. Analytics owns replay construction.
5. Analytics owns graph projection rules.
6. Analytics owns LLM evidence package construction.
7. Producer packages are verified before import.
8. Manifest and checksum verification are required for artifact package
   ingestion.
9. Producer metadata is stored as provenance only.
10. Plugin-specific classes, tasks, goals and helper names are excluded from
    Analytics core.
11. Semantic facts are imported through a semantic analysis port.
12. Rule generation is described through instrumentation plans and rule-set
    adapters.
13. Runtime values are sensitive by default.
14. Missing or ambiguous evidence is represented explicitly instead of invented.
