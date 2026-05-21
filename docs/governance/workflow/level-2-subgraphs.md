# Level 2 Governance Subgraphs

Each subgraph is small enough to review independently. The diagrams describe
governance flow only; they do not authorize product implementation or invent
repository behavior outside the referenced process documents.

Flowchart integrity for these subgraphs is audited through
`.agents/skills/flowchart-integrity-auditor/SKILL.md`. The audit checks
decision labels, STOP paths, fallback paths, terminal nodes, accidental
self-loops, forbidden `workflow execute` to `workflow create` jumps and Level 1
/ Level 2 consistency.

## S1 Skills And Agents

```mermaid
flowchart TD
  S1_CMD["skills update"]
  S1_INTAKE["Skill / agent intake"]
  S1_INTEGRITY["Integrity review"]
  S1_LINKAGE["Linkage and owner review"]
  S1_CONFLICT["Conflict and duplicate review"]
  S1_ORG["Organigramm review"]
  S1_REGISTRY["Skill registry review"]
  S1_AGENTS["AGENTS impact review"]
  S1_DOC["S1_DOC: Local skills-agents documentation"]
  S1_GUARD["S1_PUSH_ELIGIBILITY_GUARD"]
  S1_READY["Ready for allowed publication path"]
  S1_STOP["STOP: skills-agents blocker"]
  RA["ROOT_ARCHITECT: Escalation"]

  S1_CMD --> S1_INTAKE --> S1_INTEGRITY --> S1_LINKAGE --> S1_CONFLICT
  S1_CONFLICT --> S1_ORG --> S1_REGISTRY --> S1_AGENTS --> S1_DOC --> S1_GUARD
  S1_GUARD -->|"eligible"| S1_READY
  S1_GUARD -->|"not eligible"| S1_STOP
  S1_INTEGRITY -->|"failure"| S1_STOP
  S1_LINKAGE -->|"unowned capability"| S1_STOP
  S1_CONFLICT -->|"conflict"| S1_STOP
  S1_STOP --> RA
```

## S2 Workflow Create

```mermaid
flowchart TD
  S2_CMD["workflow create"]
  S2_REQ["Requirement intake"]
  S2_THREE["Three Amigos requirement gate"]
  S2_LOOP["Clarification loop"]
  S2_RETRY{"Retry <= 3?"}
  S2_BRANCH["Dedicated workflow branch"]
  S2_AUTHOR["Create or update workflow artifacts"]
  S2_DOC["S2_DOC: Local workflow-create documentation"]
  S2_ARC["arc42 and ADR impact check"]
  S2_RELEASE["Release checked workflow for execute"]
  S2_STOP["STOP: workflow-create blocker"]
  RA["ROOT_ARCHITECT: Escalation"]

  S2_CMD --> S2_REQ --> S2_THREE
  S2_THREE -->|"ready"| S2_BRANCH --> S2_AUTHOR --> S2_DOC --> S2_ARC --> S2_RELEASE
  S2_THREE -->|"requires refinement"| S2_LOOP --> S2_RETRY
  S2_RETRY -->|"yes"| S2_REQ
  S2_RETRY -->|"no"| S2_STOP
  S2_BRANCH -->|"branch or worktree unsafe"| S2_STOP
  S2_ARC -->|"governance conflict"| S2_STOP
  S2_STOP --> RA
```

## S3 Workflow Execute

```mermaid
flowchart TD
  S3_CMD["workflow execute"]
  S3_STATUS["S3_STATUS: Check working tree"]
  S3_BRANCH["S3_BRANCH: Check execution branch"]
  S3_SCOPE["S3_SCOPE: Check workflow scope"]
  S3_CLASSIFY["S3_CLASSIFY: Classify slice"]
  S3D["S3D: Execution Orchestrator"]
  BE_Q["BE_Q: Backend slice"]
  FE_Q["FE_Q: Frontend slice"]
  RT_Q["RT_Q: Runtime slice"]
  DOC_Q["DOC_Q: Documentation / governance slice"]
  BE_EXEC["BE: Backend execution subgraph"]
  FE_EXEC["FE: Frontend execution subgraph"]
  RT_EXEC["RT: Runtime / DevOps / Docker / gRPC subgraph"]
  DOC_EXEC["DOC: Documentation governance subgraph"]
  S3_UNCLASSIFIED["S3_UNCLASSIFIED"]
  S3_STOP_STATUS["STOP: Dirty working tree - report only"]
  S3_STOP_BRANCH["STOP: Wrong branch - report only"]
  S3_STOP_SCOPE["STOP: Scope conflict - escalate"]
  S3D_STOP["STOP: Dependency or metadata blocker"]
  ROUTER["Typed Error Router"]
  RA["ROOT_ARCHITECT: Escalation"]

  S3_CMD --> S3_STATUS
  S3_STATUS -->|"clean"| S3_BRANCH
  S3_STATUS -->|"dirty working tree"| S3_STOP_STATUS
  S3_BRANCH -->|"valid workflow branch"| S3_SCOPE
  S3_BRANCH -->|"wrong branch"| S3_STOP_BRANCH
  S3_SCOPE -->|"scope valid"| S3_CLASSIFY
  S3_SCOPE -->|"scope conflict"| S3_STOP_SCOPE
  S3_CLASSIFY -->|"backend"| BE_Q
  S3_CLASSIFY -->|"frontend"| FE_Q
  S3_CLASSIFY -->|"runtime / devops / contracts"| RT_Q
  S3_CLASSIFY -->|"documentation / governance / metadata declared by workflow"| DOC_Q
  S3_CLASSIFY -->|"none of the above"| S3_UNCLASSIFIED
  BE_Q --> S3D
  FE_Q --> S3D
  RT_Q --> S3D
  DOC_Q --> S3D
  S3D -->|"backend"| BE_EXEC
  S3D -->|"frontend"| FE_EXEC
  S3D -->|"runtime / devops / contracts"| RT_EXEC
  S3D -->|"documentation / governance / metadata"| DOC_EXEC
  S3D -->|"LOCK_CONFLICT"| ROUTER
  S3D -->|"cycle, missing metadata or unknown dependency"| S3D_STOP
  S3_UNCLASSIFIED --> RA
  S3_STOP_STATUS --> RA
  S3_STOP_BRANCH --> RA
  S3_STOP_SCOPE --> RA
  S3D_STOP --> RA
```

## BE Backend Execution

```mermaid
flowchart TD
  BE_START["BE: Backend slice"]
  BE_REVIEW["Senior Java Backend and architecture review"]
  BE_IMPL["Backend implementation worker if in scope"]
  BE_TEST["Targeted backend tests"]
  D8["D8: Blocking quality gate"]
  ROUTER["Typed Error Router"]
  CP_RECORD["CP_RECORD"]

  BE_START --> BE_REVIEW
  BE_REVIEW -->|"approved"| BE_IMPL --> BE_TEST --> D8
  BE_REVIEW -->|"architecture concern"| ROUTER
  BE_TEST -->|"failure"| ROUTER
  D8 -->|"passed"| CP_RECORD
  D8 -->|"failed"| ROUTER
```

## FE Frontend Execution

```mermaid
flowchart TD
  FE_START["FE: Frontend slice"]
  FE_REVIEW["Senior React Frontend and UX review"]
  FE_IMPL["Frontend implementation worker if in scope"]
  FE_TEST["Targeted frontend tests"]
  D8["D8: Blocking quality gate"]
  ROUTER["Typed Error Router"]
  CP_RECORD["CP_RECORD"]

  FE_START --> FE_REVIEW
  FE_REVIEW -->|"approved"| FE_IMPL --> FE_TEST --> D8
  FE_REVIEW -->|"UX or boundary concern"| ROUTER
  FE_TEST -->|"failure"| ROUTER
  D8 -->|"passed"| CP_RECORD
  D8 -->|"failed"| ROUTER
```

## RT Runtime DevOps Docker GRPC

```mermaid
flowchart TD
  RT_START["RT: Runtime / DevOps / Docker / gRPC slice"]
  RT_REVIEW["DevOps, contract and runtime review"]
  RT_IMPL["Runtime or contract worker if in scope"]
  RT_TEST["Targeted runtime, contract or build checks"]
  D8["D8: Blocking quality gate"]
  ROUTER["Typed Error Router"]
  CP_RECORD["CP_RECORD"]

  RT_START --> RT_REVIEW
  RT_REVIEW -->|"approved"| RT_IMPL --> RT_TEST --> D8
  RT_REVIEW -->|"contract or deployment concern"| ROUTER
  RT_TEST -->|"failure"| ROUTER
  D8 -->|"passed"| CP_RECORD
  D8 -->|"failed"| ROUTER
```

## QG Quality Gates

```mermaid
flowchart TD
  D8["D8: Required quality gate"]
  QG_PASS{"Quality gate passed?"}
  ROUTER["Typed Error Router"]
  RETRY{"Retry <= 3?"}
  FIX["Targeted fix slice"]
  QG_STOP["QG_STOP: Stop execution"]
  CP_RECORD["CP_RECORD"]
  CP_ROLLBACK["CP_ROLLBACK"]
  RA["ROOT_ARCHITECT: Escalation"]

  D8 --> QG_PASS
  QG_PASS -->|"yes"| CP_RECORD
  QG_PASS -->|"no"| ROUTER
  ROUTER --> RETRY
  RETRY -->|"yes"| FIX --> D8
  RETRY -->|"no"| QG_STOP
  QG_STOP --> CP_ROLLBACK --> RA
```

## CP Commit Checkpoint Rollback

```mermaid
flowchart TD
  CP_RECORD["CP_RECORD: Record slice result"]
  CP_COMMIT["CP_COMMIT: Commit exact slice"]
  CP_PUSH["CP_PUSH: Push checkpoint branch"]
  CP_FINAL["CP_FINAL"]
  CP_ROLLBACK["CP_ROLLBACK: Rollback / Revert Decision"]
  CMD_PUSH["CMD_PUSH"]
  RELEASE["RELEASE"]
  Q11["Q11: Async execution report"]
  RA["ROOT_ARCHITECT: Escalation"]

  CP_RECORD --> CP_COMMIT --> CP_PUSH
  CP_PUSH -->|"success"| CP_FINAL
  CP_PUSH -->|"failed"| CP_ROLLBACK
  CP_FINAL --> CMD_PUSH
  CP_FINAL --> RELEASE
  CP_FINAL --> Q11
  CP_ROLLBACK --> RA
```

## PUB Publication Modes

```mermaid
flowchart TD
  PUB_PUSH["PUB_PUSH: Publish branch or PR"]
  PUB_GUARD["PUB_PR_MERGE_GUARD"]
  PUB_MERGE["PUB_MERGE"]
  PUB_PR_RESULT["PUB_PR_RESULT: PR open - no auto merge"]
  PUB_PUSH_FAILED["PUB_PUSH_FAILED"]
  PUB_REJECTED["PUB_REJECTED"]
  PUB_DONE["PUB_DONE"]
  CP_ROLLBACK["CP_ROLLBACK"]
  RA["ROOT_ARCHITECT: Escalation"]

  PUB_PUSH --> PUB_GUARD
  PUB_GUARD -->|"auto merge allowed"| PUB_MERGE
  PUB_GUARD -->|"PR without automatic merge"| PUB_PR_RESULT
  PUB_GUARD -->|"rejected"| PUB_REJECTED
  PUB_PUSH -->|"push rejected"| PUB_PUSH_FAILED
  PUB_MERGE --> PUB_DONE
  PUB_PUSH_FAILED --> CP_ROLLBACK
  PUB_REJECTED --> RA
```

`PUB_PR_RESULT` is the terminal for an open PR without automatic merge.
`PUB_DONE` is reserved for a verified checkpoint publication completion,
automatic merge, or explicitly completed publication path.

## DOC Documentation Governance

```mermaid
flowchart TD
  DOCROOT["DOCROOT: Global documentation governance"]
  S1_DOC["S1_DOC: skills-agents artifacts"]
  S2_DOC["S2_DOC: workflow-create artifacts"]
  S3_DOC["S3_DOC: workflow-execute artifacts"]
  CONSISTENCY["Global consistency check"]
  DOC_FAIL["DOC_GOVERNANCE_FAILURE"]
  ROUTER["Typed Error Router"]
  DOC_DONE["Documentation consistent"]

  S1_DOC --> CONSISTENCY
  S2_DOC --> CONSISTENCY
  S3_DOC --> CONSISTENCY
  DOCROOT --> CONSISTENCY
  CONSISTENCY -->|"consistent"| DOC_DONE
  CONSISTENCY -->|"conflict"| DOC_FAIL --> ROUTER
```
