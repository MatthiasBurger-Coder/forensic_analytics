# 6. Runtime View

## 6.1 Server-Side Analysis Request Flow

```text
Build Tool
  -> Gradle/Maven Plugin
  -> gRPC Analysis Request
  -> Forensics Server
  -> Workspace Checkout
  -> Server-side Static Analysis
  -> Server-side Joern Analysis
  -> Canonical Analysis Model
```

## 6.2 Rule Generation Flow

```text
Canonical Analysis Model
  -> Rule Planner
  -> Instrumentation Plan
  -> Server-side Byteman/BTM Generator
  -> Versioned BTM Files
  -> Plugin Runtime Binding
  -> Runtime Session With Agent
```

## 6.3 Runtime Event Flow

```text
Runtime Application
  -> Byteman Agent
  -> Server-generated BTM File
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

## 6.7 Operational Logging Flow

```text
REST / gRPC / CLI / Bootstrap boundary
  -> Correlation scope
  -> Sanitized operation event
  -> JDK System.Logger
```

Operational logs record request, command and server lifecycle categories. They do not contain raw runtime payloads, source content, method arguments, method return values, LLM prompts or raw exception messages.

Operational correlation IDs help connect adapter logs for diagnostics. They are not canonical evidence and must not be used as proof of runtime execution.

## 6.8 Target Microservice Runtime Flow

The target runtime flow for service-split work is planned by ADR-0017. It is
not implemented yet:

```text
Frontend / CLI / external client
  -> Gateway
  -> Ingestion or analysis job APIs
  -> Repository Analysis
  -> Java AST Analysis
  -> Joern CPG Analysis
  -> Analysis Store
  -> Graph Replay
  -> Report Generation
  -> Gateway
  -> Frontend / client
```

Plugin, scanner and runtime evidence enters through the ingestion service.
Canonical evidence and one-writer analysis state belong to the analysis store.
Graph, replay, reports and LLM packages are projections or generated artifacts
that must remain traceable to owner evidence APIs.

ADR-0018 allows the initial runtime communication contracts to describe planned
Gateway, worker, replay, report and event flows before the services exist.
Those flows remain contract design until a later slice implements and verifies
the corresponding runtime behavior.

Slice 05 verifies a narrow Analysis Store runtime path:

```text
worker or future Gateway
  -> AnalysisJobService gRPC
  -> analysis-store-service application service
  -> service-local analysis job repository
```

This path supports job submission, leasing, progress, completion, failure,
listing and artifact metadata registration. It does not yet ingest normalized
fact bodies, runtime trace facts, incidents or correlation indexes.

## 6.9 Agent Governance Runtime Flows

These are repository governance flows, not product runtime flows.

### skills update Flow

```mermaid
flowchart TD
  Start["skills update"]
  Intake["Skill / Agent Intake"]
  Integrity["Skill Integrity Reviewer"]
  Registry["Skill Registry Maintainer"]
  Org["Organigramm Maintainer"]
  Agents["AGENTS.md Maintainer"]
  Process["Process Governance Maintainer"]
  Guard["S1_PUSH_ELIGIBILITY_GUARD"]
  Ready["Ready for optional push auto"]

  Start --> Intake --> Integrity --> Registry --> Org --> Agents --> Process --> Guard --> Ready
```

### workflow create Flow

```mermaid
flowchart TD
  Intake["Requirement Intake"]
  Clarify["Requirement Clarification Loop"]
  Blocking["Blocking Questions?"]
  Retry{"Clarification attempts <= 3?"}
  Ask["Ask focused clarification questions"]
  Incorporate["Incorporate answers"]
  Escalate["STOP: Root Architect escalation"]
  Gate["Three Amigos Requirement Gate"]
  Branch["Branch Governance"]
  Req["Senior Requirement Engineer"]
  Architect["Senior System Architect"]
  Java["Senior Java Backend Developer"]
  React["Senior React Frontend Developer"]
  Tester["Senior Tester"]
  Workflow["workflow.md Maintainer"]
  Arc42["arc42 Architecture Documentation Maintainer"]
  WorkflowCheck["workflow.md Validation"]
  Arc42Check["arc42 Validation"]
  Docs["Documentation Governance"]
  Final["Final Gate"]
  Release["Release for workflow execute"]

  Intake --> Clarify --> Blocking
  Blocking -->|yes| Retry
  Retry -->|yes| Ask --> Incorporate --> Clarify
  Retry -->|no| Escalate
  Blocking -->|no| Gate --> Branch --> Req --> Architect --> Java --> React --> Tester --> Workflow --> WorkflowCheck --> Arc42 --> Arc42Check --> Docs --> Final --> Release
```

### workflow execute Flow

```mermaid
flowchart TD
  Status["S3_STATUS: Check working tree"]
  Branch["S3_BRANCH: Check execution branch"]
  Scope["S3_SCOPE: Check workflow scope"]
  Classify["S3_CLASSIFY: Classify slice"]
  BE_Q["BE_Q: Backend slice"]
  FE_Q["FE_Q: Frontend slice"]
  RT_Q["RT_Q: Runtime slice"]
  DOC_Q["DOC_Q: Documentation / governance slice"]
  Unclassified["S3_UNCLASSIFIED: Stop and escalate"]
  RootArchitect["Root Architect decision"]
  StopStatus["STOP: Dirty working tree - report only"]
  StopBranch["STOP: Wrong branch - report only"]
  StopScope["STOP: Scope conflict - escalate"]
  Executor["workflow-executor"]
  Swarm["Agent Swarm Orchestrator"]
  Backend["Backend Strand"]
  Frontend["Frontend Strand"]
  Runtime["Docker / Runtime Strand"]
  Docs["Documentation Strand"]
  Gate["Slice Quality Gate"]
  Router["Typed Error Router"]
  ArchFailure["ARCH_VIOLATION"]
  BuildFailure["BUILD_FAILURE"]
  TestFailure["TEST_FAILURE"]
  DocFailure["DOC_GOVERNANCE_FAILURE"]
  LockFailure["LOCK_CONFLICT"]
  UnknownFailure["UNKNOWN_FAILURE"]
  Retry{"Retry <= 3?"}
  Fix["Targeted Fix Slice"]
  Escalate["Root Architect Escalation"]
  Commit["Slice Checkpoint Commit"]
  Push["Push Workflow Branch"]
  Final["Final Workflow Execute Gate"]

  Status -->|clean| Branch
  Status -->|dirty working tree| StopStatus
  Branch -->|valid workflow branch| Scope
  Branch -->|wrong branch| StopBranch
  Scope -->|scope valid| Classify
  Scope -->|scope conflict| StopScope
  Classify -->|backend| BE_Q --> Executor
  Classify -->|frontend| FE_Q --> Executor
  Classify -->|runtime / devops / contracts| RT_Q --> Executor
  Classify -->|documentation / governance / metadata declared by workflow| DOC_Q --> Executor
  Classify -->|none of the above| Unclassified --> RootArchitect
  Executor --> Swarm
  Swarm --> Backend
  Swarm --> Frontend
  Swarm --> Runtime
  Swarm --> Docs
  Backend --> Gate
  Frontend --> Gate
  Runtime --> Gate
  Docs --> Gate
  Gate -->|passed| Commit --> Push --> Final
  Gate -->|failed| Router
  Router --> ArchFailure --> Retry
  Router --> BuildFailure --> Retry
  Router --> TestFailure --> Retry
  Router --> DocFailure --> Retry
  Router --> LockFailure --> Retry
  Router --> UnknownFailure --> Escalate
  Retry -->|yes| Fix --> Gate
  Retry -->|no| Escalate
```
