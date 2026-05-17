# Agent And Skill Organigramm

This organigramm documents the three governed process strands for Codex work in
Forensic Analytics.

The Senior System Architect owns the top-level governance boundary. Documentation
Governance is mandatory in every strand, but it executes inside the active
strand and does not create a fourth strand.

## Overall Governance

```mermaid
flowchart TD
  SSA["Senior System Architect"]
  DG["Documentation Governance"]
  SA["skills-agents"]
  WC["workflow create"]
  WE["workflow execute"]

  SSA --> SA
  SSA --> WC
  SSA --> WE
  DG --> SA
  DG --> WC
  DG --> WE
```

## Strand 1: skills-agents

```mermaid
flowchart TD
  SA["skills-agents"]
  Creator["Skill / Agent Creator"]
  Integrity["Skill Integrity Reviewer"]
  Registry["Skill Registry Maintainer"]
  Org["Organigramm Maintainer"]
  Agents["AGENTS.md Maintainer"]
  Process["Process Governance Maintainer"]
  Push["Push Auto Guard"]
  Docs["Documentation Governance"]
  Approval["Approved skills-agents change"]

  SA --> Creator --> Integrity --> Registry --> Org --> Agents --> Process --> Docs --> Approval
  Approval --> Push
```

Required checks:

- Integrity review.
- Linkage and owner review.
- Duplicate, contradiction and dead-reference review.
- Organigramm review.
- Skill registry review.
- Documentation review.
- `push auto` guard when publication is requested.

## Strand 2: workflow create

```mermaid
flowchart TD
  WC["workflow create"]
  Branch["Branch Governance"]
  Gate["Three Amigos Requirement Gate"]
  Req["Senior Requirement Engineer"]
  Arch["Senior System Architect"]
  Back["Senior Java Backend Developer"]
  Front["Senior React Frontend Developer"]
  Test["Senior Tester"]
  WF["docs/workflow/workflow.md Maintainer"]
  Arc["arc42 Architecture Documentation Maintainer"]
  Docs["Documentation Governance"]
  Ready["Release for workflow execute"]

  WC --> Branch --> Gate
  Gate --> Req --> Arch --> Back --> Front --> Test --> WF --> Arc --> Docs --> Ready
```

Required end artifacts:

- Checked `docs/workflow/workflow.md`.
- Checked or updated arc42 documentation.

## Strand 3: workflow execute

```mermaid
flowchart TD
  WE["workflow execute"]
  Executor["workflow-executor"]
  Orchestrator["Agent Swarm Orchestrator"]
  Backend["Backend Strand"]
  Frontend["Frontend Strand"]
  Runtime["Docker / Runtime Strand"]
  Docs["Documentation Strand"]
  SliceGate["Slice Quality Gate"]
  FinalGate["Final Workflow Execute Gate"]

  WE --> Executor --> Orchestrator
  Orchestrator --> Backend --> SliceGate
  Orchestrator --> Frontend --> SliceGate
  Orchestrator --> Runtime --> SliceGate
  Orchestrator --> Docs --> SliceGate
  SliceGate --> FinalGate
```

Backend strand roles:

- Senior Java Backend Developer
- Microservice Senior Expert
- `architecture-hexagonal`
- `spring-core` when Spring wiring is affected
- `testing-junit6`
- Senior DevOps with `devops-docker` when container readiness is affected

Frontend strand roles:

- Senior React Frontend Developer
- Senior UX Designer
- Senior DevOps with `devops-docker` when container readiness is affected

Documentation strand roles:

- Senior Documentation Engineer
- docs/workflow/workflow.md Maintainer
- arc42 Architecture Documentation Maintainer
- Testing Documentation Maintainer
- Execution Report Maintainer

## Boundary Rule

A role may participate in more than one strand only as a strand-scoped role.
It must use the active strand's inputs, outputs, allowed files and quality gate.
It must not move unfinished work or changed files between strands.
