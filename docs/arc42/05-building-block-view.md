# 5. Building Block View

## 5.1 Level 1 - System Overview

```text
Forensics Platform
├── Spring Boot Server Boundary
├── Application Core
├── Canonical Analysis Model
├── gRPC Ingestion Adapter
├── Analysis Import
├── Rule Planning
├── Runtime Event Processing
├── Incident Management
├── Replay Engine
├── Graph Projection
├── Vector Context Builder
├── LLM Diagnosis
├── Observability Boundary
├── Cross-cutting Logging Module
├── Repair Orchestration
└── Adapters
    ├── Gradle Plugin Request Adapter
    ├── Maven Plugin Request Adapter
    ├── Joern Adapter
    ├── Server-side Byteman/BTM Adapter
    ├── Runtime Collector Adapter
    ├── Relational Store Adapter
    ├── Graph DB Adapter
    ├── Vector DB Adapter
    └── LLM Provider Adapter
```

## 5.2 Level 2 - Core Building Blocks

| Building Block | Responsibility |
|---|---|
| Spring Boot Server Boundary | Owns Spring Boot startup, profile configuration and outer adapter lifecycle wiring for verified adapters |
| Canonical Analysis Model | Owns stable IDs and normalized facts |
| gRPC Ingestion Adapter | Receives plugin-triggered server analysis requests and maps transport DTOs to application commands |
| Static Fact Import | Imports server-side AST, build and dependency facts |
| Joern Semantic Import | Runs and maps server-side Joern semantic facts |
| Rule Planner | Plans instrumentation rules based on facts and policies |
| Byteman Generator | Generates server-side BTM files with stable rule IDs |
| Runtime Event Processor | Validates, redacts and stores runtime events |
| Incident Service | Creates and groups exception-based incidents |
| Replay Engine | Reconstructs timelines and call trees |
| Graph Projection Service | Builds graph projections from canonical facts |
| Vector Context Builder | Builds semantic context for retrieval and LLM use |
| LLM Diagnosis Service | Creates evidence-based root-cause analysis |
| Observability Boundary | Provides adapter-level correlation scopes and sanitized operation logging without becoming evidence storage |
| Cross-cutting Logging Module | Provides injectable logger wrappers and Boot-scoped method interception for sanitized operational diagnostics |
| Repair Orchestrator | Prepares future gated repair flows |

## 5.3 Hexagonal Architecture Mapping

| Layer | Examples |
|---|---|
| Domain | IDs, analysis model, incident model, replay model, rule plan |
| Application | Import use cases, replay use cases, diagnosis use cases |
| Ports | Fact import port, event store port, graph port, LLM port, rule generation port |
| Infrastructure | Adapter-facing observability, correlation support and outer server bootstrap configuration |
| Adapters | gRPC ingestion, REST API, CLI, Gradle/Maven request and runtime-binding adapters, server-side Joern, server-side Byteman/BTM, relational DB, graph DB, vector DB, LLM provider |

## 5.4 Important Boundary

Gradle and Maven plugins must not become the central platform. They trigger server-side analysis with repository, branch, commit, build and execution context. When debugging requires instrumentation, they may receive server-generated BTM files and bind them through the runtime agent. Parser execution, Joern execution, BTM generation, normalization, persistence, replay and graph projection stay in Analytics.

## 5.5 gRPC Ingestion Boundary

`forensic-analytics-ingestion-grpc` is an inbound adapter. It may depend on generated Protobuf/gRPC classes and the application layer. It must not depend on persistence adapters, Joern, replay, LLM providers or plugin internals.

The adapter maps:

```text
Proto DTO
  -> Application Command
    -> Application Use Case
```

## 5.6 Observability Boundary

`forensic-analytics-observability` is an infrastructure module for operational diagnostics. Adapter, engine, ingestion-request, persistence and bootstrap code may use it to create sanitized operation logs and correlation scopes where a request or command boundary exists.

The observability module must not depend on domain, application, persistence, REST, gRPC, generated protobuf classes, Spring AOP, AspectJ, SLF4J or concrete logging providers. Domain and application code must not depend on observability.

## 5.7 Spring Boot Server Boundary

ADR-0006 accepts Spring Boot as the outer server boundary. The implemented module is `forensic-analytics-boot-app`, which owns the Spring Boot application entrypoint, typed configuration and Spring bean wiring for verified adapters.

The existing `forensic-analytics-bootstrap` module remains available while parity is phased in. Spring Boot adoption must not add Spring dependencies or annotations to `forensic-analytics-domain` or `forensic-analytics-application`.

ADR-0007 keeps the existing JDK REST adapter behind Boot lifecycle wiring. Spring MVC, WebFlux and Actuator endpoints are not part of the current Boot boundary.

ADR-0019 extends the Spring Boot boundary to service-local bootstrap packages
under `de.burger.forensics.analytics.services..bootstrap..`. Service domain and
application packages remain framework-free; service-local bootstrap code may
own the Spring Boot entrypoint, configuration and lifecycle wiring for an
independent service.

## 5.8 Cross-cutting Logging Module

ADR-0008 accepts `forensic-analytics-logging` as a cross-cutting infrastructure module. It provides `ForensicLoggerFactory` for explicit logger injection and optional Spring method interception in the Boot runtime.

The logging module may depend on `forensic-analytics-observability` for correlation context and on Spring AOP/Context for the accepted method-interception exception. It may publish Boot auto-configuration metadata. It must not depend on domain, application, adapters, persistence, REST, gRPC, generated Protobuf, AspectJ, SLF4J or concrete logging providers.

Automatic logging records operation name, phase, duration, correlation ID and exception category only. It must not log method arguments, return values, raw exception messages, stack frames, payloads, source content, credentials or LLM prompt content.

## 5.9 Target Microservices Ecosystem

ADR-0017 defines the active target service landscape for service-split work.
The landscape is partially implemented: ingestion, repository-analysis,
analysis-store, Java AST, Joern CPG and BTM generation have service slices.
Gateway, graph-replay, report-generation and frontend migration remain planned:

```text
frontend-web-app
  -> forensic-gateway-service
    -> forensic-ingestion-service
    -> repository-analysis-service
    -> analysis-store-service
    -> graph-replay-service
    -> report-generation-service

repository-analysis-service
  -> java-ast-analysis-service
  -> joern-cpg-analysis-service

analysis-store-service
  <- java-ast-analysis-service
  <- joern-cpg-analysis-service
  <- btm-generation-service

graph-replay-service
  -> analysis-store-service

report-generation-service
  -> analysis-store-service
  -> graph-replay-service
```

Every service must own its internal domain, application, adapters,
configuration, tests, health checks and Dockerfile before production readiness
is claimed. Service communication is limited to REST/OpenAPI, gRPC/protobuf or
approved event contracts. Shared Java implementation modules between
independently deployable services are forbidden.

ADR-0018 accepts initial logical contracts for target service communication.
Contracts marked as planned are design artifacts only; they do not prove that a
Gateway endpoint, gRPC method, event publisher or event consumer is implemented.
Generated transport classes must remain service-local implementation details.

The current `analysis-store-service` implementation provides the first
boundary for the `AnalysisJobService` job lifecycle and artifact metadata
subset. The service has its own domain, application, gRPC adapter, in-memory
repository, Spring Boot bootstrap, tests and Dockerfile. It does not depend on
monolith domain, application or persistence modules and does not yet implement
durable normalized facts, incident records, correlation indexes or database
migrations.

## 5.10 Agent Governance Building Blocks

ADR-0021 accepts Governance Flowchart V2 as the active repository governance
model. The canonical Governance Flowchart V2 diagrams are maintained in
`docs/governance/workflow/`. Agent governance explanations remain in
`docs/agents/agent-governance.md`, and the role organigramm remains in
`docs/agents/organigramm.md`. The arc42 building-block view embeds the
architecture-relevant governance overview and publication-mode separation
directly because they define ownership boundaries for repository changes.

```mermaid
flowchart TD
  Architect["Senior System Architect"]
  Skills["Strand 1: skills-agents"]
  Create["Strand 2: workflow create"]
  Execute["Strand 3: workflow execute"]
  Docs["DOCROOT: Global Documentation Governance"]
  Registry["Skill Registry Maintainer"]
  Org["Organigramm Maintainer"]
  Process["Process Governance Maintainer"]

  Architect --> Skills
  Architect --> Create
  Architect --> Execute
  Architect --> Docs
  Docs --> Skills
  Docs --> Create
  Docs --> Execute
  Skills --> Registry
  Skills --> Org
  Skills --> Process
  Create --> Docs
  Execute --> Docs
```

| Building Block | Responsibility |
|---|---|
| Senior System Architect | Owns architecture and process-strand governance. |
| `DOCROOT` | Checks global consistency for process documentation, role model, organigramm, arc42 structure, governance rules, workflow conventions and hard boundaries. |
| `S1_DOC` | Updates concrete skills-agents documentation artifacts. |
| `S2_DOC` | Updates concrete workflow-create documentation artifacts. |
| `S3_DOC` | Updates concrete workflow-execute documentation artifacts. |
| Skill Registry Maintainer | Maintains the skills-agents registry and ownership map. |
| Organigramm Maintainer | Maintains agent role hierarchy and process-strand diagrams. |
| Process Governance Maintainer | Maintains command and publication-mode documentation. |
| `S1_PUSH_ELIGIBILITY_GUARD` | Blocks `push auto` outside `skills-agents` and blocks product implementation changes from `push auto`. |
| `PUB_PR_MERGE_GUARD` | Decides whether a PR may merge, stay open, be blocked or be rejected. |
| docs/workflow/workflow.md Maintainer | Maintains the checked active workflow produced by `workflow create`. |
| arc42 Architecture Documentation Maintainer | Checks or updates arc42 before workflow execute is released. |
| S3D Execution Orchestrator | Extracts slice metadata, builds the dependency graph, runs topological sort and enforces file, contract, module and architecture-boundary locks. |
| Typed Error Router | Classifies workflow-execute validation failures before retry, targeted fix or escalation. |
| `CP_ROLLBACK` | Decides between file revert, slice-commit revert, fix slice, branch discard, workflow recut or Root Architect escalation. |
| Testing Documentation Maintainer | Maintains workflow test strategy and quality-gate evidence. |
| Execution Report Maintainer | Records slice checkpoint commit SHA, push result and blockers during `workflow execute`. |
| Governance Flowchart V2 diagram package | Maintains the Level 1 overview and Level 2 subgraphs used for flowchart integrity review. |

### Senior System Architect

Top-level architecture and process governance authority.

### skills-agents Strand

Maintains skills, agents, roles, prompts, routing rules, organigramm, skill registry and process documentation.

Triggered by:

```text
skills update
```

### workflow create Strand

Turns a user request into a clarified, checked and executable workflow.

Produces:

1. checked `docs/workflow/workflow.md`
2. checked or updated arc42 documentation

### workflow execute Strand

Executes checked workflow slices through the agent workflow.

Responsibilities:

- load checked workflow
- load checked arc42 documentation
- check `S3_STATUS` working-tree safety before routing slices
- check `S3_BRANCH` execution branch safety before routing slices
- check `S3_SCOPE` workflow scope safety before classifying slices
- classify backend, frontend, runtime and documentation work
- run S3D dependency, topological-order and lock checks
- route explicitly declared governance, metadata and documentation-only slices through `S3_DOC` only when the checked workflow declares that scope
- stop and escalate unclassifiable slices through `S3_UNCLASSIFIED` instead of executing them automatically
- route to roles or subagents
- run Slice Quality Gates
- route quality-gate or validation failures through the Typed Error Router
- create a one-slice Slice Checkpoint Commit
- push workflow branch to origin
- update execution report and arc42 consistency

### Documentation Governance

Local documentation nodes update concrete artifacts in the active strand:
`S1_DOC` for skills-agents, `S2_DOC` for workflow create and `S3_DOC` for
workflow execute.

`DOCROOT` checks that workflow, process, agent, arc42 and ADR artifacts remain
synchronized with the global governance model. `DOCROOT` is not a fourth
process strand and does not replace local documentation nodes.

### Publication Modes

```mermaid
flowchart TD
  Checkpoint["Slice checkpoint push"]
  Push["push"]
  Auto["push auto"]
  Execute["workflow execute"]
  Pr["PR without automatic merge"]
  Skills["skills-agents"]
  Guard["PUB_PR_MERGE_GUARD"]

  Execute --> Checkpoint
  Push --> Pr
  Skills --> Auto --> Guard
```

Slice checkpoint push, `push` and `push auto` are separate publication
mechanisms. `push auto` is restricted to `skills-agents`; slice checkpoint push
belongs to `workflow execute`.
