# Agent Organigramm

This document maps repository governance roles to the three process strands.

Root `AGENTS.md` remains authoritative. `QUALITY.md` remains authoritative for quality gates.

## Overall Governance

```text
Senior System Architect
|-- skills-agents
|-- workflow create
|-- workflow execute
`-- Documentation Governance inside all active strands
```

```mermaid
flowchart TD
  Architect["Senior System Architect"]
  Skills["Strand 1: skills-agents"]
  Create["Strand 2: workflow create"]
  Execute["Strand 3: workflow execute"]
  Docs["Documentation Governance"]
  Registry["Skill Registry Maintainer"]
  Org["Organigramm Maintainer"]
  Process["Process Governance Maintainer"]

  Architect --> Skills
  Architect --> Create
  Architect --> Execute
  Architect --> Docs
  Skills --> Registry
  Skills --> Org
  Skills --> Process
  Create --> Docs
  Execute --> Docs
```

## Strand 1: skills-agents

```mermaid
flowchart TD
  Start["skills update"]
  Intake["Skill / Agent Intake"]
  Integrity["Skill Integrity Reviewer"]
  Registry["Skill Registry Maintainer"]
  Org["Organigramm Maintainer"]
  Agents["AGENTS.md Maintainer"]
  Process["Process Governance Maintainer"]
  Guard["Push Auto Guard"]
  Ready["Ready for optional push auto"]
  Stop["STOP and report"]

  Start --> Intake --> Integrity --> Registry --> Org --> Agents --> Process --> Guard --> Ready
  Integrity --> Stop
  Registry --> Stop
  Org --> Stop
  Agents --> Stop
  Process --> Stop
  Guard --> Stop
```

## Strand 2: workflow create

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

## Strand 3: workflow execute

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
  Docs["Documentation Strand"]
  Gate["Slice Quality Gate"]
  QG_STOP["QG_STOP: Stop execution"]
  CP_RECORD["CP_RECORD: Record slice result"]
  CP_COMMIT["CP_COMMIT: Commit exact slice"]
  CP_PUSH["CP_PUSH: Push or prepare publication"]
  CP_FINAL["CP_FINAL"]
  CP_ROLLBACK["CP_ROLLBACK: Rollback / Revert Decision"]
  CMD_PUSH["CMD_PUSH"]
  RELEASE["RELEASE"]
  Q11["Q11: Async Execution Report"]
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
  CP_PUSH -->|success| CP_FINAL
  CP_PUSH -->|failed| CP_ROLLBACK
  CP_FINAL --> CMD_PUSH
  CP_FINAL --> RELEASE
  CP_FINAL --> Q11
  CP_ROLLBACK --> RootArchitect
  Gate -->|failed| Router
  Router --> ArchFailure --> Retry
  Router --> BuildFailure --> Retry
  Router --> TestFailure --> Retry
  Router --> DocFailure --> Retry
  Router --> LockFailure --> Retry
  Router --> UnknownFailure --> QG_STOP
  Retry -->|yes| Fix --> Gate
  Retry -->|no| QG_STOP
  QG_STOP --> CP_ROLLBACK
```

Unrecovered quality-gate failures reach `QG_STOP` and then `CP_ROLLBACK`.
`CP_FINAL` continues only to explicit `CMD_PUSH`, `RELEASE` or non-blocking
`Q11` reporting paths.

## Publication Modes

```mermaid
flowchart TD
  Checkpoint["Slice checkpoint push"]
  Push["push"]
  Auto["push auto"]
  Execute["workflow execute"]
  PUB_PUSH["PUB_PUSH"]
  PUB_DONE["PUB_DONE"]
  PUB_PR_RESULT["PUB_PR_RESULT: PR open - no auto merge"]
  PUB_PUSH_FAILED["PUB_PUSH_FAILED"]
  PUB_REJECTED["PUB_REJECTED"]
  PUB_MERGE["PUB_MERGE"]
  CP_ROLLBACK["CP_ROLLBACK"]
  RA["Root Architect Escalation"]
  Skills["skills-agents"]
  Guard["Guarded PR lifecycle"]

  Execute --> Checkpoint --> PUB_PUSH
  Push --> PUB_PUSH
  Skills --> Auto --> Guard --> PUB_PUSH
  PUB_PUSH -->|checkpoint success| PUB_DONE
  PUB_PUSH -->|normal PR without auto merge| PUB_PR_RESULT
  PUB_PUSH -->|push rejected| PUB_PUSH_FAILED
  PUB_PUSH -->|governance, branch, scope or guard rejected| PUB_REJECTED
  PUB_PUSH -->|auto merge allowed| PUB_MERGE --> PUB_DONE
  PUB_PUSH_FAILED --> CP_ROLLBACK
  PUB_PUSH_FAILED -->|no rollback point| RA
  PUB_REJECTED -->|requires governance decision| RA
```

Slice checkpoint push is not `push auto`.
`push` is not `push auto`.
`skills update` is not `push auto`.

`PUB_PR_RESULT` is the normal `push` terminal for an open PR without automatic
merge. `PUB_PUSH_FAILED` routes to rollback or escalation, and `PUB_REJECTED`
stops publication when governance, branch, scope or guard rules block it.
