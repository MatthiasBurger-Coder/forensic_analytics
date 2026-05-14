# EPIC: Forensics Platform – Exception-zentriertes Runtime Replay und LLM-gestützte Fehleranalyse

**Status:** Draft
**Version:** 0.1
**Datum:** 2026-05-03
**Rolle:** Senior Requirement Engineer
**Projektkontext:** Forensics Tracing / Forensics Platform
**Technische Leitplanken:** Gradle 9.1, Java/JDK 17, hexagonale Architektur, Plugins als Adapter, zentrale Forensics-Applikation als Analyseplattform

## Source Note

This repository does not contain a separate EPIC source file beyond `workflow.md`. This archived baseline captures the EPIC content available from `workflow.md` and keeps product and architecture decisions explicit. Missing choices remain marked as open.

## Executive Summary

The Forensics Platform is an independent analysis platform for Java systems. It combines static code analysis, semantic graph analysis, runtime tracing, exception replay and LLM-supported root-cause analysis.

The platform shall answer not only where an error happened, but how it emerged, which runtime values triggered it, which code path was actually executed and how the defect can be tested and fixed safely.

The long-term product vision is:

```text
Observe -> Replay -> Understand -> Fix -> Test -> Verify -> Deploy
```

## Business Goals

- Reconstruct runtime failures from exceptions or correlation IDs.
- Connect runtime events with static source-code context.
- Provide evidence-based LLM root-cause analysis.
- Prepare safe and reviewable fix plans.
- Support future controlled repair automation.

## Key Capabilities

| Capability | Description |
|---|---|
| Static Fact Ingestion | Imports facts from source code, build metadata and AST analysis |
| Joern Semantic Ingestion | Attaches semantic analysis data such as data-flow and control-flow findings |
| Rule Planning and Byteman Generation | Plans runtime instrumentation and emits versioned Byteman rule sets |
| Runtime Event Collection | Collects runtime events emitted by instrumented Java applications |
| Exception Detection and Incident Creation | Turns exception-centered runtime data into incident records |
| Replay Engine | Reconstructs event timelines and call paths for incidents |
| Graph-Based UI Context | Provides graph context for navigation and incident understanding |
| LLM Incident Analysis | Builds evidence packages for root-cause analysis and fix planning |
| Fix Planning | Prepares reviewable fix plans based on evidence |
| Automated Repair Preparation | Prepares future gated repair flows, without autonomous MVP changes |

## Technical Guardrails

- The platform baseline is Java/JDK 17.
- Gradle plugin integration must be compatible with Gradle 9.1.
- Maven must be supported as a separate plugin adapter.
- The architecture follows a hexagonal style.
- Gradle and Maven plugins are fact producers and adapters, not the central platform.
- The central Forensics application owns normalization, persistence, correlation, replay, graph building, LLM analysis and future repair orchestration.
- Joern integration is an adapter behind a port.
- Byteman rules are generated from an explicit instrumentation plan.
- Graph DB and Vector DB are projections derived from the canonical analysis model.
- Runtime data is sensitive by default.

## MVP Scope

The MVP focuses on read-only analysis:

- Static fact import
- Canonical model persistence
- Joern result import or attachment
- Byteman rule generation with stable rule IDs
- JSONL runtime event import
- Exception incident creation
- CorrelationID-based replay
- Simple graph projection
- LLM root-cause explanation without code modification

## Non-MVP Scope

The following items are explicitly postponed:

- Autonomous code changes
- Automated patch generation
- Automated pull request creation
- Production deployment automation
- Full Vector DB integration
- Production-ready multi-tenant architecture
- Complete graph UI with all layers

## Conceptual Data Flow

```text
Static Facts + Joern Facts + Runtime Events
        |
        v
Canonical Analysis Model
        |
        +--> Graph Projection
        +--> Vector Projection
        +--> Event Timeline
        |
        v
Incident Replay
        |
        v
Incident Context Package
        |
        v
LLM Root-Cause Analysis
```

## Canonical IDs

The platform uses stable IDs to correlate static facts, semantic facts, Byteman rules and runtime events:

- `projectId`
- `moduleId`
- `sourceFileId`
- `classKey`
- `methodKey`
- `callsiteKey`
- `branchKey`
- `ruleId`
- `analysisRunId`
- `runtimeSessionId`
- `correlationId`
- `traceId`
- `spanId`
- `parentSpanId`
- `incidentId`

## Security and Runtime Data Sensitivity

Runtime values must be treated as sensitive by default. Redaction must happen before unsafe persistence into graph or vector projections. Secrets must not be indexed in a Vector DB. Runtime data access must be auditable.

Supported protection mechanisms include:

- Allowlisting
- Redaction
- Hashing
- Masking
- Length limits
- Sampling
- Retention
- Encryption
- Auditing

## Architecture Decisions

| ID | Decision | Status |
|---|---|---|
| AD-001 | Plugins are producers, not the platform | Accepted |
| AD-002 | Use a canonical analysis model | Accepted |
| AD-003 | Graph DB and Vector DB are projections | Accepted |
| AD-004 | Runtime data is sensitive by default | Accepted |
| AD-005 | LLM diagnosis must be evidence-based | Accepted |
| AD-006 | Automated repair is gated | Accepted |

## Open Decisions

| ID | Open Decision | Notes |
|---|---|---|
| OD-001 | Initial relational database | Not selected in EPIC v0.1 |
| OD-002 | Initial Graph DB | Not selected in EPIC v0.1 |
| OD-003 | Initial Vector DB | Not selected in EPIC v0.1 |
| OD-004 | Runtime ingestion mode | JSONL likely for MVP, HTTP collector later |
| OD-005 | Runtime value storage policy | Needs redaction rule model |
| OD-006 | Initial LLM provider | Must remain replaceable |
| OD-007 | Source-code loading and versioning in UI | Needs later design |
| OD-008 | Multi-repo and multi-service trace model | Needs later design |

## Risks

| Risk | Description | Mitigation |
|---|---|---|
| Runtime overhead | Too many Byteman rules can slow down the target application | Rule planner, sampling, profiles, selective instrumentation |
| Sensitive trace data | Parameters may contain personal data or secrets | Redaction, allowlisting, hashing, retention |
| Wrong graph correlation | JavaParser and Joern data may be mapped incorrectly | Confidence levels, validation, ambiguity reporting |
| LLM hallucination | LLM may suggest wrong causes or fixes | Evidence-only prompting, tests, review gates |
| Large graphs | UI may become overloaded for large codebases | Layering, filtering, slicing, lazy loading |
| Unsafe automatic fixes | Patches may have unexpected side effects | Regression tests, risk classifier, human review |
| Toolchain complexity | Joern, Byteman, Graph DB, Vector DB and LLM increase complexity | Hexagonal ports, modular adapters, MVP slicing |
