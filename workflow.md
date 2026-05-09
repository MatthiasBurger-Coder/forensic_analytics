# Workflow.md – arc42-Dokumentation für Forensics Platform aufbauen

**Status:** Draft
**Ziel:** Aufbau einer vollständigen arc42-Dokumentation im Unterordner `docs/` für die aktuelle EPIC-Version der **Forensics Platform – Exception-zentriertes Runtime Replay und LLM-gestützte Fehleranalyse**.
**Ausgangsversion:** EPIC Version `0.1`, Datum `2026-05-03`
**Projektkontext:** Forensics Tracing / Forensics Platform
**Rolle für Codex:** Senior Software Architect, Senior Requirement Engineer, Technical Documentation Engineer

---

## 1. Ziel dieses Workflows

Dieser Workflow beschreibt Schritt für Schritt, wie im Repository ein neuer Dokumentationsbereich unter `docs/` aufgebaut wird.

Innerhalb dieses Ordners soll eine arc42-konforme Architekturdokumentation für die Forensics Platform entstehen. Die Dokumentation muss die aktuelle EPIC-Version `0.1` fachlich und technisch abbilden.

Die Dokumentation soll nicht nur eine Kopie des EPICs sein, sondern dessen Inhalte in eine belastbare Architekturstruktur nach arc42 überführen.

---

## 2. Grundregeln für Codex

1. Keine bestehenden produktiven Dateien ändern, sofern es nicht ausdrücklich notwendig ist.
2. Den Ordner `docs/` erstellen, falls er noch nicht existiert.
3. Innerhalb von `docs/` eine klare arc42-Dokumentationsstruktur erzeugen.
4. Die Dokumentation muss Markdown-basiert sein.
5. Die Inhalte müssen aus dem EPIC `Forensics Platform – Exception-zentriertes Runtime Replay und LLM-gestützte Fehleranalyse` Version `0.1` abgeleitet werden.
6. Keine nicht belegten Produktentscheidungen treffen.
7. Offene Architekturentscheidungen ausdrücklich als offen markieren.
8. Technische Leitplanken aus dem EPIC übernehmen:

    * Java/JDK 17
    * Gradle 9.1
    * hexagonale Architektur
    * Gradle- und Maven-Plugins als Adapter
    * zentrale Forensics-Applikation als Analyseplattform
    * Plugins sind Producer, nicht die Plattform
    * Graph DB und Vector DB sind Projektionen
    * Runtime-Daten sind sensibel
9. Architekturgrenzen und Unsicherheiten sichtbar dokumentieren.
10. Keine automatische Auswahl finaler Datenbankprodukte erzwingen, wenn diese im EPIC als offen markiert sind.
11. Falls bestehende Dokumentation vorhanden ist, diese nicht überschreiben, sondern vorher prüfen und bestehende Inhalte integrieren oder sauber erweitern.

---

## 3. Erwartete Zielstruktur

Erstelle folgende Struktur:

```text
docs/
├── README.md
├── arc42/
│   ├── README.md
│   ├── 01-introduction-and-goals.md
│   ├── 02-architecture-constraints.md
│   ├── 03-system-scope-and-context.md
│   ├── 04-solution-strategy.md
│   ├── 05-building-block-view.md
│   ├── 06-runtime-view.md
│   ├── 07-deployment-view.md
│   ├── 08-crosscutting-concepts.md
│   ├── 09-architecture-decisions.md
│   ├── 10-quality-requirements.md
│   ├── 11-risks-and-technical-debt.md
│   └── 12-glossary.md
└── epics/
    └── forensics-platform-runtime-replay-llm-analysis-v0.1.md
```

Optional, falls es sinnvoll ist:

```text
docs/
└── adr/
    ├── README.md
    ├── ADR-0001-plugins-are-producers.md
    ├── ADR-0002-canonical-analysis-model.md
    ├── ADR-0003-runtime-events-are-sensitive.md
    └── ADR-0004-graph-and-vector-db-as-projections.md
```

Die ADR-Dateien nur erstellen, wenn keine bestehende ADR-Struktur vorhanden ist und wenn die Entscheidungen aus dem EPIC klar genug ableitbar sind.

---

## 4. Schritt 1 – Repository prüfen

Führe zunächst eine Bestandsaufnahme durch.

Prüfe:

```bash
git status --short
find . -maxdepth 3 -type d | sort
find . -maxdepth 4 -iname "*arc42*" -o -iname "*adr*" -o -iname "README.md"
```

Ziel:

* Prüfen, ob `docs/` bereits existiert.
* Prüfen, ob bereits Architektur- oder ADR-Dokumentation vorhanden ist.
* Prüfen, ob bestehende Dokumentation integriert werden muss.
* Keine bestehenden Dokumente ohne Prüfung überschreiben.

Wenn bestehende relevante Dokumente gefunden werden:

1. Inhalt prüfen.
2. Relevanz bewerten.
3. Neue Struktur daran anschließen.
4. Keine Duplikate erzeugen.
5. Änderungen nachvollziehbar dokumentieren.

---

## 5. Schritt 2 – docs-Ordner anlegen

Falls `docs/` noch nicht existiert:

```bash
mkdir -p docs
mkdir -p docs/arc42
mkdir -p docs/epics
```

Falls `docs/` bereits existiert:

```bash
mkdir -p docs/arc42
mkdir -p docs/epics
```

Danach prüfen:

```bash
find docs -maxdepth 3 -type d | sort
```

---

## 6. Schritt 3 – EPIC-Version archivieren

Erstelle die Datei:

```text
docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.1.md
```

Diese Datei enthält die aktuelle EPIC-Version `0.1` als fachliche Quelle für die Architektur.

Die Datei muss beginnen mit:

```markdown
# EPIC: Forensics Platform – Exception-zentriertes Runtime Replay und LLM-gestützte Fehleranalyse

**Status:** Draft  
**Version:** 0.1  
**Datum:** 2026-05-03  
**Rolle:** Senior Requirement Engineer  
**Projektkontext:** Forensics Tracing / Forensics Platform  
**Technische Leitplanken:** Gradle 9.1, Java/JDK 17, hexagonale Architektur, Plugins als Adapter, zentrale Forensics-Applikation als Analyseplattform
```

Danach den vollständigen Inhalt des EPICs übernehmen.

Wichtig:

* Inhalt nicht fachlich verfälschen.
* Keine Aussagen entfernen.
* Tippfehler dürfen nur korrigiert werden, wenn dadurch die Bedeutung nicht verändert wird.
* Die Datei dient als versionierte Ausgangsbasis.

---

## 7. Schritt 4 – docs/README.md erstellen

Erstelle oder aktualisiere:

```text
docs/README.md
```

Inhalt:

````markdown
# Forensics Platform Documentation

This directory contains the architecture and product documentation for the Forensics Platform.

## Documentation Structure

- `arc42/` – Architecture documentation based on the arc42 template
- `epics/` – Versioned product and requirement epics
- `adr/` – Architecture Decision Records, if present

## Current Architecture Baseline

The current architecture baseline is derived from:

- EPIC: Forensics Platform – Exception-zentriertes Runtime Replay und LLM-gestützte Fehleranalyse
- Version: 0.1
- Date: 2026-05-03

## Core Vision

The Forensics Platform combines static code analysis, semantic graph analysis, runtime tracing, exception replay and LLM-supported diagnosis into a controlled analysis and repair flow.

The long-term product vision is:

```text
Observe -> Replay -> Understand -> Fix -> Test -> Verify -> Deploy
````

````

---

## 8. Schritt 5 – arc42/README.md erstellen

Erstelle:

```text
docs/arc42/README.md
````

Inhalt:

```markdown
# arc42 Architecture Documentation – Forensics Platform

This directory contains the arc42-based architecture documentation for the Forensics Platform.

## Source Baseline

This documentation is based on the EPIC:

- Name: Forensics Platform – Exception-zentriertes Runtime Replay und LLM-gestützte Fehleranalyse
- Version: 0.1
- Date: 2026-05-03
- Status: Draft

## Sections

1. Introduction and Goals
2. Architecture Constraints
3. System Scope and Context
4. Solution Strategy
5. Building Block View
6. Runtime View
7. Deployment View
8. Crosscutting Concepts
9. Architecture Decisions
10. Quality Requirements
11. Risks and Technical Debt
12. Glossary

## Documentation Principle

The EPIC remains the product and requirement baseline. The arc42 documentation transforms this baseline into an architectural structure.
```

---

## 9. Schritt 6 – arc42 Kapitel erstellen

Erstelle alle folgenden Dateien. Jede Datei muss fachlich aus dem EPIC abgeleitet werden.

---

### 9.1 Datei: `01-introduction-and-goals.md`

Ziel:

* Executive Summary aus dem EPIC übernehmen und architekturgerecht formulieren.
* Fachliche Ziele beschreiben.
* Stakeholder und Qualitätsziele aufnehmen.

Mindestinhalt:

```markdown
# 1. Introduction and Goals

## 1.1 Requirements Overview

The Forensics Platform is an independent analysis platform for Java systems. It combines static analysis, semantic graph analysis, runtime tracing, exception replay and LLM-supported root-cause analysis.

The platform shall answer not only where an error happened, but how it emerged, which runtime values triggered it, which code path was actually executed and how the defect can be tested and fixed safely.

## 1.2 Business Goals

- Reconstruct runtime failures from exceptions or correlation IDs.
- Connect runtime events with static source-code context.
- Provide evidence-based LLM root-cause analysis.
- Prepare safe and reviewable fix plans.
- Support future controlled repair automation.

## 1.3 Key Capabilities

- Static Fact Ingestion
- Joern Semantic Ingestion
- Rule Planning and Byteman Generation
- Runtime Event Collection
- Exception Detection and Incident Creation
- Replay Engine
- Graph-Based UI Context
- LLM Incident Analysis
- Fix Planning
- Automated Repair Preparation

## 1.4 Stakeholders

| Stakeholder | Interest |
|---|---|
| Developer | Understand failures faster and reproduce them reliably |
| Lead Developer | Assess root cause, fix scope and regression risk |
| Platform Operator | Run and operate the Forensics Platform safely |
| Security Responsible | Ensure runtime data is protected and redacted |
| Reviewer | Review evidence-based fix proposals |
| Build/Tooling Engineer | Integrate Gradle, Maven, Byteman and Joern adapters |

## 1.5 Quality Goals

| Goal | Description |
|---|---|
| Traceability | Every runtime event must be traceable to source-code and rule context |
| Evidence-based diagnosis | LLM output must refer to available evidence |
| Security | Runtime values are sensitive by default |
| Scalability | Large multi-module Java systems must be analyzable incrementally |
| Extensibility | Storage engines, LLM providers and tool adapters must be replaceable |
```

---

### 9.2 Datei: `02-architecture-constraints.md`

Ziel:

* Technische, organisatorische und fachliche Constraints dokumentieren.

Mindestinhalt:

```markdown
# 2. Architecture Constraints

## 2.1 Technical Constraints

| Constraint | Description |
|---|---|
| Java/JDK 17 | The platform baseline is Java 17. |
| Gradle 9.1 | Gradle plugin integration must be compatible with Gradle 9.1. |
| Maven support | Maven must be supported as a separate plugin adapter. |
| Hexagonal Architecture | Core domain logic must be independent from frameworks and external tools. |
| Plugins as adapters | Gradle and Maven plugins are producers of facts, not the central platform. |
| Joern as adapter | Joern integration must be encapsulated behind a port. |
| Byteman integration | Byteman rules are generated from the analysis model and runtime planning. |

## 2.2 Product Constraints

- The MVP does not include autonomous code changes.
- The MVP does not include automatic pull request creation.
- The MVP does not include production deployment automation.
- The first platform version focuses on JSONL runtime event import.
- Graph DB and Vector DB products are not finally selected.

## 2.3 Security Constraints

- Runtime values must be treated as sensitive by default.
- Redaction must happen before persistence into graph or vector projections.
- Secrets must not be indexed in a Vector DB.
- Runtime data access must be auditable.

## 2.4 Architectural Guardrails

- Canonical model first.
- Graph DB and Vector DB are projections, not the source of truth.
- Ambiguous mappings must be reported, not silently accepted.
- LLM output must be evidence-based.
- Automated repair must be gated by tests, quality gates and human review.
```

---

### 9.3 Datei: `03-system-scope-and-context.md`

Ziel:

* Systemkontext der Forensics Platform dokumentieren.
* Externe Systeme und Schnittstellen beschreiben.

Mindestinhalt:

````markdown
# 3. System Scope and Context

## 3.1 Business Context

The Forensics Platform receives static facts, semantic facts and runtime events from different producers. It normalizes them into a canonical analysis model and provides replay, graph context and LLM-supported diagnosis.

```text
Developer / Lead Developer
        |
        v
Forensics UI / API
        |
        v
Forensics Platform
        |
        +--> Gradle Plugin
        +--> Maven Plugin
        +--> Joern Adapter
        +--> Byteman Runtime Collector
        +--> Graph DB Projection
        +--> Vector DB Projection
        +--> Event Store
        +--> LLM Provider
````

## 3.2 Technical Context

| External System     |        Direction | Purpose                                                                 |
| ------------------- | ---------------: | ----------------------------------------------------------------------- |
| Gradle Plugin       |          inbound | Provides build context, source roots, AST facts and rule bindings       |
| Maven Plugin        |          inbound | Provides Maven build context and facts                                  |
| Joern               | inbound/outbound | Provides semantic code analysis, data-flow and control-flow information |
| Runtime Application |          inbound | Emits Byteman-generated runtime events                                  |
| Byteman Agent       | outbound/inbound | Executes generated instrumentation rules                                |
| Relational Store    |         outbound | Stores canonical model and transactional state                          |
| Graph DB            |         outbound | Stores graph projection for navigation and incident context             |
| Vector DB           |         outbound | Stores semantic projections for similarity and LLM context retrieval    |
| Event Store         |         outbound | Stores runtime event timelines and replay data                          |
| LLM Provider        |         outbound | Performs root-cause analysis and fix planning                           |

## 3.3 Main Data Flow

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

````

---

### 9.4 Datei: `04-solution-strategy.md`

Ziel:

- Lösungsstrategie aus dem EPIC formulieren.

Mindestinhalt:

```markdown
# 4. Solution Strategy

## 4.1 Strategy Overview

The Forensics Platform separates data production from central analysis. Build plugins and tool adapters produce facts. The central platform normalizes, persists, correlates and analyzes them.

## 4.2 Core Strategy

1. Use Gradle and Maven plugins only as fact producers.
2. Normalize all inputs into a canonical analysis model.
3. Use stable IDs for classes, methods, callsites, branches, rules and runtime events.
4. Generate Byteman rules from an explicit instrumentation plan.
5. Collect runtime events with stable `ruleId` and `methodKey` references.
6. Build incidents from exception events.
7. Reconstruct replay timelines by correlation ID, trace ID, thread ID and sequence.
8. Build graph and vector projections from the canonical model.
9. Provide curated evidence packages to the LLM.
10. Keep repair automation gated by tests, quality gates and review.

## 4.3 MVP Strategy

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

## 4.4 Non-MVP Scope

The following items are explicitly postponed:

- Automated patch generation
- Automated pull request creation
- Automated staging or production deployment
- Full Vector DB integration
- Production-ready multi-tenant architecture
- Complete graph UI with all layers
````

---

### 9.5 Datei: `05-building-block-view.md`

Ziel:

* Bausteinsicht der Plattform dokumentieren.
* Hexagonale Architektur sichtbar machen.

Mindestinhalt:

````markdown
# 5. Building Block View

## 5.1 Level 1 – System Overview

```text
Forensics Platform
├── Application Core
├── Canonical Analysis Model
├── Analysis Import
├── Rule Planning
├── Runtime Event Processing
├── Incident Management
├── Replay Engine
├── Graph Projection
├── Vector Context Builder
├── LLM Diagnosis
├── Repair Orchestration
└── Adapters
    ├── Gradle Plugin Adapter
    ├── Maven Plugin Adapter
    ├── Joern Adapter
    ├── Byteman Rule Adapter
    ├── Runtime Collector Adapter
    ├── Relational Store Adapter
    ├── Graph DB Adapter
    ├── Vector DB Adapter
    └── LLM Provider Adapter
````

## 5.2 Level 2 – Core Building Blocks

| Building Block           | Responsibility                                          |
| ------------------------ | ------------------------------------------------------- |
| Canonical Analysis Model | Owns stable IDs and normalized facts                    |
| Static Fact Import       | Imports AST, build and dependency facts                 |
| Joern Semantic Import    | Imports and maps Joern semantic facts                   |
| Rule Planner             | Plans instrumentation rules based on facts and policies |
| Byteman Generator        | Generates Byteman rules with stable rule IDs            |
| Runtime Event Processor  | Validates, redacts and stores runtime events            |
| Incident Service         | Creates and groups exception-based incidents            |
| Replay Engine            | Reconstructs timelines and call trees                   |
| Graph Projection Service | Builds graph projections from canonical facts           |
| Vector Context Builder   | Builds semantic context for retrieval and LLM use       |
| LLM Diagnosis Service    | Creates evidence-based root-cause analysis              |
| Repair Orchestrator      | Prepares future gated repair flows                      |

## 5.3 Hexagonal Architecture Mapping

| Layer       | Examples                                                                        |
| ----------- | ------------------------------------------------------------------------------- |
| Domain      | IDs, analysis model, incident model, replay model, rule plan                    |
| Application | Import use cases, replay use cases, diagnosis use cases                         |
| Ports       | Fact import port, event store port, graph port, LLM port, rule generation port  |
| Adapters    | Gradle, Maven, Joern, Byteman, relational DB, graph DB, vector DB, LLM provider |

## 5.4 Important Boundary

Gradle and Maven plugins must not become the central platform. They provide raw facts, build context, source roots, classpath information and integration points only.

````

---

### 9.6 Datei: `06-runtime-view.md`

Ziel:

- Runtime-Szenarien dokumentieren.

Mindestinhalt:

```markdown
# 6. Runtime View

## 6.1 Static Analysis Flow

```text
Build Tool
  -> Gradle/Maven Plugin
  -> Static Fact Export
  -> Forensics Import API
  -> Canonical Analysis Model
  -> Graph Projection
````

## 6.2 Rule Generation Flow

```text
Canonical Analysis Model
  -> Rule Planner
  -> Instrumentation Plan
  -> Byteman Rule Generator
  -> Versioned Rule Set
  -> Runtime Session
```

## 6.3 Runtime Event Flow

```text
Runtime Application
  -> Byteman Agent
  -> Generated Rule
  -> Runtime Event
  -> JSONL / Collector
  -> Runtime Event Importer
  -> Redaction
  -> Event Store
```

## 6.4 Exception Replay Flow

```text
Exception Event
  -> Incident Creation
  -> CorrelationID Event Lookup
  -> Timeline Reconstruction
  -> Call Tree Reconstruction
  -> Source-Code Mapping
  -> Graph Context Loading
  -> Replay View
```

## 6.5 LLM Diagnosis Flow

```text
Incident
  -> Replay Timeline
  -> Source Slices
  -> Graph Context
  -> Joern Findings
  -> Redacted Runtime Values
  -> Incident Context Package
  -> LLM Diagnosis
  -> Root-Cause Explanation
  -> Fix Plan
```

## 6.6 Missing Event Handling

The Replay Engine must explicitly show uncertainty if events are missing, incomplete or ambiguous. It must not pretend that a reconstructed path is complete when the evidence is incomplete.

````

---

### 9.7 Datei: `07-deployment-view.md`

Ziel:

- Deployment-Sicht für MVP und spätere Ausbaustufen dokumentieren.

Mindestinhalt:

```markdown
# 7. Deployment View

## 7.1 MVP Deployment View

```text
Developer Machine / CI Environment
├── Target Java Project
├── Gradle or Maven Plugin
├── Generated Byteman Rules
├── Runtime Application with Byteman Agent
└── Forensics Platform
    ├── Import API
    ├── Canonical Store
    ├── Event Store
    ├── Simple Graph Projection
    └── LLM Diagnosis Adapter
````

## 7.2 Later Deployment View

```text
Forensics Platform Environment
├── Forensics API
├── Forensics UI
├── Relational Store
├── Graph DB
├── Vector DB
├── Event Store
├── Runtime Collector
├── LLM Adapter
└── Repair Orchestrator
```

## 7.3 Deployment Constraints

* The MVP may start as a local or CI-attached analysis platform.
* Runtime event ingestion may initially use JSONL files.
* HTTP collector support can be introduced later.
* Multi-tenant production deployment is out of MVP scope.

````

---

### 9.8 Datei: `08-crosscutting-concepts.md`

Ziel:

- Querschnittliche Architekturkonzepte dokumentieren.

Mindestinhalt:

```markdown
# 8. Crosscutting Concepts

## 8.1 Canonical IDs

The platform uses stable IDs to correlate static facts, semantic facts, Byteman rules and runtime events.

Important IDs:

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

## 8.2 Runtime Data Sensitivity

Runtime data must be treated as sensitive by default.

Supported mechanisms:

- Allowlisting
- Redaction
- Hashing
- Masking
- Length limits
- Sampling
- Retention
- Encryption
- Auditing

## 8.3 Evidence-Based LLM Usage

LLM analysis must be based on curated evidence packages. The LLM must not invent missing facts. If evidence is insufficient, the diagnosis must state the limitation.

## 8.4 Graph and Vector Projections

Graph DB and Vector DB are projections from the canonical analysis model. They are optimized views, not the source of truth.

## 8.5 Ambiguity Handling

Ambiguous mappings between JavaParser, Joern, Byteman rules and runtime events must be marked with confidence levels. Unclear mappings must not be silently accepted.

## 8.6 Replay Uncertainty

The replay must explicitly show missing, incomplete or uncertain event chains.
````

---

### 9.9 Datei: `09-architecture-decisions.md`

Ziel:

* Bekannte Architekturentscheidungen und offene Entscheidungen dokumentieren.

Mindestinhalt:

```markdown
# 9. Architecture Decisions

## 9.1 Accepted Decisions

| ID | Decision | Status | Rationale |
|---|---|---|---|
| AD-001 | Plugins are producers, not the platform | Accepted | Keeps build integration separate from central analysis |
| AD-002 | Use a canonical analysis model | Accepted | Enables correlation across JavaParser, Joern, Byteman and runtime events |
| AD-003 | Graph DB and Vector DB are projections | Accepted | Prevents storage-specific models from becoming the source of truth |
| AD-004 | Runtime data is sensitive by default | Accepted | Protects secrets, personal data and business-critical values |
| AD-005 | LLM diagnosis must be evidence-based | Accepted | Reduces hallucination risk and improves reviewability |
| AD-006 | Automated repair is gated | Accepted | Prevents unsafe autonomous changes |

## 9.2 Open Decisions

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
```

---

### 9.10 Datei: `10-quality-requirements.md`

Ziel:

* Qualitätsanforderungen dokumentieren.

Mindestinhalt:

````markdown
# 10. Quality Requirements

## 10.1 Quality Tree

```text
Quality
├── Performance
│   ├── Incremental static imports
│   ├── Fast incident graph queries
│   └── Low runtime overhead
├── Scalability
│   ├── Multi-module support
│   ├── Large legacy codebase support
│   └── Runtime event partitioning
├── Security
│   ├── Runtime redaction
│   ├── Secret protection
│   ├── Auditability
│   └── Retention control
├── Traceability
│   ├── Rule to source mapping
│   ├── Event to rule mapping
│   ├── Patch to incident mapping
│   └── Diagnosis to evidence mapping
└── Extensibility
    ├── Replaceable storage adapters
    ├── Replaceable LLM provider
    ├── Replaceable graph projection
    └── Replaceable vector projection
````

## 10.2 Quality Scenarios

| Scenario                                 | Expected Result                                                         |
| ---------------------------------------- | ----------------------------------------------------------------------- |
| Import changed Java files only           | The platform detects changed files by hash and avoids full reprocessing |
| Load incident by CorrelationID           | Replay timeline is generated in interactive time                        |
| Runtime event contains secret-like value | Value is redacted before persistence                                    |
| Joern mapping is ambiguous               | Mapping is marked as ambiguous and not silently linked                  |
| LLM lacks evidence                       | Diagnosis reports insufficient evidence                                 |
| Tests fail after generated fix           | No PR is created                                                        |

````

---

### 9.11 Datei: `11-risks-and-technical-debt.md`

Ziel:

- Risiken aus dem EPIC übernehmen.

Mindestinhalt:

```markdown
# 11. Risks and Technical Debt

| Risk | Description | Mitigation |
|---|---|---|
| Runtime overhead | Too many Byteman rules can slow down the target application | Rule planner, sampling, profiles, selective instrumentation |
| Sensitive trace data | Parameters may contain personal data or secrets | Redaction, allowlisting, hashing, retention |
| Wrong graph correlation | JavaParser and Joern data may be mapped incorrectly | Confidence levels, validation, ambiguity reporting |
| LLM hallucination | LLM may suggest wrong causes or fixes | Evidence-only prompting, tests, review gates |
| Large graphs | UI may become overloaded for large codebases | Layering, filtering, slicing, lazy loading |
| Unsafe automatic fixes | Patches may have unexpected side effects | Regression tests, risk classifier, human review |
| Toolchain complexity | Joern, Byteman, Graph DB, Vector DB and LLM increase complexity | Hexagonal ports, modular adapters, MVP slicing |

## 11.1 Technical Debt Candidates

- Initial event import may be JSONL-only.
- First graph projection may be simplified.
- Vector DB integration may be postponed.
- UI may start as minimal read-only analysis view.
- Joern mapping may initially support only selected node types.
````

---

### 9.12 Datei: `12-glossary.md`

Ziel:

* Einheitliche Begriffe definieren.

Mindestinhalt:

```markdown
# 12. Glossary

| Term | Definition |
|---|---|
| Forensics Platform | Central analysis platform for static facts, runtime events, replay and LLM diagnosis |
| Static Fact | Persisted information derived from source code, build context or AST analysis |
| Runtime Event | Event emitted by the instrumented runtime application |
| RuleID | Stable identifier of a generated Byteman rule |
| MethodKey | Stable identifier for a method across analysis and runtime data |
| CorrelationID | Identifier connecting runtime events belonging to the same business or technical flow |
| TraceID | Identifier for a distributed or local execution trace |
| SpanID | Identifier for a specific execution span |
| Incident | Error-centered analysis unit usually created from an exception event |
| Replay | Reconstructed timeline and call tree for an incident |
| Incident Context Package | Curated evidence package for LLM diagnosis |
| Joern | External semantic code analysis tool for CPG, data-flow and control-flow analysis |
| Graph Projection | Graph representation derived from the canonical model |
| Vector Projection | Semantic vector representation derived from selected canonical facts |
| Redaction | Removal or masking of sensitive runtime values |
| Repair Orchestrator | Future component for gated fix, test and PR preparation |
```

---

## 10. Schritt 7 – Optional ADR-Struktur erzeugen

Wenn noch keine ADR-Struktur vorhanden ist, erstelle optional:

```bash
mkdir -p docs/adr
```

### Datei: `docs/adr/README.md`

```markdown
# Architecture Decision Records

This directory contains architecture decisions for the Forensics Platform.

The decisions are derived from the EPIC baseline and refined during implementation.
```

### ADR-0001: Plugins are producers

```markdown
# ADR-0001: Plugins are producers, not the platform

## Status

Accepted

## Context

The Forensics Platform uses Gradle and Maven integrations. These plugins can collect build context, source roots, dependencies and raw analysis facts.

## Decision

Gradle and Maven plugins are fact producers and integration adapters. The central Forensics Platform owns normalization, persistence, correlation, replay, graph building, LLM analysis and repair orchestration.

## Consequences

- Plugins remain smaller and focused.
- The central application owns the canonical model.
- Maven and Gradle can evolve independently as adapters.
```

### ADR-0002: Canonical analysis model

```markdown
# ADR-0002: Use a canonical analysis model

## Status

Accepted

## Context

The platform must correlate JavaParser facts, Joern results, Byteman rules and runtime events.

## Decision

All external inputs are normalized into a canonical analysis model with stable identifiers.

## Consequences

- Correlation becomes explicit.
- Storage projections remain replaceable.
- Ambiguous mappings can be represented safely.
```

### ADR-0003: Runtime events are sensitive

```markdown
# ADR-0003: Runtime events are sensitive by default

## Status

Accepted

## Context

Runtime events may contain secrets, personal data or business-critical values.

## Decision

Runtime values must be treated as sensitive by default. Redaction, masking, hashing, allowlisting and retention rules must be applied before unsafe persistence or indexing.

## Consequences

- Security is part of the core model.
- Event ingestion must validate and redact data.
- Vector indexing must never receive secrets.
```

### ADR-0004: Graph and Vector DB are projections

```markdown
# ADR-0004: Graph DB and Vector DB are projections

## Status

Accepted

## Context

The platform needs graph navigation and semantic search, but storage technologies are not finally selected.

## Decision

Graph DB and Vector DB are projections derived from the canonical model. They are not the primary source of truth.

## Consequences

- Storage technology remains replaceable.
- Projection rebuilds are possible.
- The domain model remains independent from database-specific structures.
```

---

## 11. Schritt 8 – Konsistenzprüfung

Nach Erstellung aller Dateien prüfen:

```bash
find docs -type f | sort
```

Erwartete Mindestdateien:

```text
docs/README.md
docs/arc42/README.md
docs/arc42/01-introduction-and-goals.md
docs/arc42/02-architecture-constraints.md
docs/arc42/03-system-scope-and-context.md
docs/arc42/04-solution-strategy.md
docs/arc42/05-building-block-view.md
docs/arc42/06-runtime-view.md
docs/arc42/07-deployment-view.md
docs/arc42/08-crosscutting-concepts.md
docs/arc42/09-architecture-decisions.md
docs/arc42/10-quality-requirements.md
docs/arc42/11-risks-and-technical-debt.md
docs/arc42/12-glossary.md
docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.1.md
```

Optional zusätzlich:

```text
docs/adr/README.md
docs/adr/ADR-0001-plugins-are-producers.md
docs/adr/ADR-0002-canonical-analysis-model.md
docs/adr/ADR-0003-runtime-events-are-sensitive.md
docs/adr/ADR-0004-graph-and-vector-db-as-projections.md
```

---

## 12. Schritt 9 – Markdown-Qualität prüfen

Prüfe die Dokumentation auf:

* gültige Markdown-Überschriften,
* geschlossene Codeblöcke,
* konsistente Dateinamen,
* keine defekten relativen Links,
* keine Platzhalter ohne Aussage,
* keine technischen Entscheidungen, die im EPIC nicht getroffen wurden,
* klare Kennzeichnung offener Entscheidungen.

Optional, falls ein Markdown-Linter vorhanden ist:

```bash
markdownlint docs || true
```

Wenn kein Linter vorhanden ist, keine neue Tool-Abhängigkeit erzwingen.

---

## 13. Schritt 10 – Git-Diff prüfen

Prüfe die Änderungen:

```bash
git status --short
git diff -- docs
```

Bewerte:

* Wurden nur Dokumentationsdateien geändert?
* Ist die arc42-Struktur vollständig?
* Ist die EPIC-Version `0.1` archiviert?
* Sind offene Entscheidungen korrekt markiert?
* Sind Architekturentscheidungen aus dem EPIC nachvollziehbar abgeleitet?

---

## 14. Definition of Done

Der Workflow ist abgeschlossen, wenn:

1. Der Ordner `docs/` existiert.
2. Der Ordner `docs/arc42/` existiert.
3. Alle 12 arc42-Kapitel als Markdown-Dateien vorhanden sind.
4. Das EPIC Version `0.1` unter `docs/epics/` versioniert abgelegt ist.
5. `docs/README.md` die Dokumentationsstruktur erklärt.
6. `docs/arc42/README.md` die arc42-Struktur erklärt.
7. Die arc42-Dokumentation die Forensics Platform aus dem EPIC abbildet.
8. Technische Leitplanken sichtbar dokumentiert sind.
9. MVP-Scope und Nicht-Ziele klar getrennt sind.
10. Offene Entscheidungen nicht als final entschieden dargestellt werden.
11. Runtime-Datensensibilität und Redaction als zentrale Konzepte dokumentiert sind.
12. Graph DB und Vector DB als Projektionen beschrieben sind.
13. Plugins eindeutig als Producer und Adapter beschrieben sind.
14. Die Dokumentation keine unkontrollierten Codeänderungen verursacht hat.
15. `git diff -- docs` nachvollziehbar und reviewfähig ist.

---

## 15. Erwartete Commit Message

Wenn alle Prüfungen erfolgreich sind, schlage folgende Commit Message vor:

```text
docs: add arc42 architecture baseline for forensics platform

Add docs structure for the Forensics Platform architecture baseline.
The new documentation stores the EPIC v0.1 source and derives an arc42-based
architecture documentation from it.

Included sections:
- introduction and goals
- architecture constraints
- system scope and context
- solution strategy
- building block view
- runtime view
- deployment view
- crosscutting concepts
- architecture decisions
- quality requirements
- risks and technical debt
- glossary

The documentation keeps open decisions explicit and preserves the architectural
principles from the EPIC: plugins as producers, canonical analysis model,
runtime data sensitivity, and graph/vector databases as projections.
```

---

## 16. Wichtige Qualitätsgrenze

Diese Aufgabe ist rein dokumentationsbezogen.

Nicht umsetzen:

* keine neuen Java-Klassen,
* keine Gradle-Konfiguration ändern,
* keine Maven-Konfiguration ändern,
* keine Datenbankabhängigkeiten einführen,
* keine Joern-Integration implementieren,
* keine Byteman-Regeln ändern,
* keine Runtime-Collector-Implementierung bauen,
* keine UI erzeugen.

Falls Codex während der Umsetzung erkennt, dass eine technische Entscheidung fehlt, muss diese als offene Entscheidung dokumentiert werden. Nicht stillschweigend entscheiden.
