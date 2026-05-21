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

The target runtime flow for service-split work is defined by ADR-0017 and is
partially implemented. The full evidence-review flow remains planned until
graph-replay and report-generation runtime paths are implemented and verified:

```text
Frontend / CLI / external client
  -> Gateway
  -> Analysis Store orchestration owner API
  -> Repository Analysis
  -> Source Snapshot And Build Artifact Resolution
  -> Java AST Analysis
  -> Java AST Source-Fact Byte Retrieval
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

Gateway remains a public facade in this flow. Repository-to-BTM worker
dispatch, retry and job-graph readiness state is owned by Analysis Store
through the Slice 11 owner API. Gateway must not sequence worker business logic
directly.

Slice 12 verifies the Java AST source-fact byte retrieval owner API, the
Repository Analysis to Java AST handoff signal and deterministic local
repository-to-BTM fixtures. The default readiness path uses fakes, in-process
gRPC and service-local fixtures rather than external Git network access,
Docker, Jenkins, Artifactory or credentials.

The repository-to-BTM delivery path must be verified as:

```text
Plugin / external client
  -> Gateway HTTP repository-to-BTM request
  -> Analysis Store orchestration owner API
  -> Repository Analysis source snapshot
  -> Java AST source-fact bytes through owner API
  -> optional Joern worker outputs
  -> Analysis Store accepted metadata and target selection
  -> BTM Generation
  -> Gateway public BTM delivery facade
  -> Plugin / external client receives completed BTM files or unavailable state
```

Slice 16 defers graph-replay and report-generation service implementation from
repository-to-BTM acceptance. The accepted BTM pipeline does not require replay
views, graph projections, reports, incident packages, LLM-ready packages or
live LLM output. Those services may be added only after a later slice defines
contracts, owner-query access, projection rebuild rules, storage ownership and
tests that keep projections and generated output separate from evidence.

Repository Analysis resolves branch input to a concrete commit SHA before
analysis handoff. The source snapshot may reference a complete build-output
package from a verified Artifact Store/Artifactory artifact, an optional
Jenkins pipeline for the pinned snapshot, or a future sandboxed
`build-artifact-worker-service` fallback. Joern consumes only validated
source/build package descriptors or materialized Joern-owned workspaces; it
must not receive Repository Analysis private workspace IDs.

ADR-0018 allows the initial runtime communication contracts to describe planned
Gateway, worker, replay, report and event flows before each runtime path exists.
Those flows remain contract design until a slice implements and verifies the
corresponding runtime behavior.

The current implementation verifies a narrow Analysis Store runtime path:

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
  Docs["S2_DOC: Documentation Governance"]
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
  S3D["S3D: Execution Orchestrator"]
  Executor["workflow-executor"]
  Swarm["Agent Swarm Orchestrator"]
  Backend["Backend Strand"]
  Frontend["Frontend Strand"]
  Runtime["Docker / Runtime Strand"]
  Docs["S3_DOC: Documentation path inside workflow execute"]
  Gate["D8: Blocking Slice Quality Gate"]
  QG_STOP["QG_STOP: Stop execution"]
  CP_RECORD["CP_RECORD: Slice traceability"]
  CP_COMMIT["CP_COMMIT: Commit exact slice"]
  CP_PUSH["CP_PUSH: Push workflow branch"]
  CP_FINAL["CP_FINAL"]
  CP_ROLLBACK["CP_ROLLBACK: Rollback / Revert Decision"]
  CMD_PUSH["CMD_PUSH"]
  RELEASE["RELEASE"]
  Q11["Q11: Async execution report"]
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

  Status -->|clean| Branch
  Status -->|dirty working tree| StopStatus
  Branch -->|valid workflow branch| Scope
  Branch -->|wrong branch| StopBranch
  Scope -->|scope valid| Classify
  Scope -->|scope conflict| StopScope
  Classify -->|backend| BE_Q --> S3D
  Classify -->|frontend| FE_Q --> S3D
  Classify -->|runtime / devops / contracts| RT_Q --> S3D
  Classify -->|documentation / governance / metadata declared by workflow| DOC_Q --> S3D
  Classify -->|none of the above| Unclassified --> RootArchitect
  S3D -->|dependency graph and locks valid| Executor
  S3D -->|lock conflict| Router
  S3D -->|cycle, missing metadata or unknown dependency| Escalate
  Executor --> Swarm
  Swarm --> Backend
  Swarm --> Frontend
  Swarm --> Runtime
  Swarm --> Docs
  Backend --> Gate
  Frontend --> Gate
  Runtime --> Gate
  Docs --> Gate
  Gate -->|passed| CP_RECORD --> CP_COMMIT --> CP_PUSH
  Gate -->|failed| Router
  Router --> ArchFailure --> Retry
  Router --> BuildFailure --> Retry
  Router --> TestFailure --> Retry
  Router --> DocFailure --> Retry
  Router --> LockFailure --> Retry
  Router --> UnknownFailure --> Escalate
  Retry -->|yes| Fix --> Gate
  Retry -->|no| QG_STOP --> CP_ROLLBACK
  CP_PUSH -->|success| CP_FINAL
  CP_PUSH -->|failed| CP_ROLLBACK
  CP_FINAL --> CMD_PUSH
  CP_FINAL --> RELEASE
  CP_FINAL --> Q11
  CP_ROLLBACK --> RootArchitect
```

`CP_FINAL` does not end workflow governance by itself. It hands off to normal
push, release preparation or the non-blocking Q11 execution report path.
`CP_ROLLBACK` is a decision node and must not be interpreted as blind history
rewriting.

Publication-mode details remain separate from workflow execution. Normal
publication outcomes are `PUB_DONE`, `PUB_PR_RESULT`, `PUB_PUSH_FAILED` and
`PUB_REJECTED`; failed publication routes to `CP_ROLLBACK` when a rollback
point exists or to Root Architect escalation when it does not.
