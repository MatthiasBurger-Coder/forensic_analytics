# Agent Governance

## Purpose

This documentation describes the agent and skill governance model for forensic_analytics.

The model is part of architecture governance because repository agents can create, change, validate, commit and publish architecture-sensitive artifacts. Agent work must therefore stay traceable to explicit commands, process strands, role ownership, quality gates and publication rules.

Root `AGENTS.md` remains authoritative for mandatory agent behavior. `QUALITY.md` remains authoritative for verification commands and quality-gate expectations.

## Process Strands

There are exactly three process strands:

1. `skills-agents`
2. `workflow create`
3. `workflow execute`

These strands must not be mixed.

Documentation Governance is not a fourth strand. Documentation Governance runs inside the active strand and applies that strand's file scope, quality gate and publication rules.

## Command Mapping

| Command or mode | Process strand | Meaning |
|---|---|---|
| `skills update` | `skills-agents` | Activates skill, agent, role, prompt, routing-rule, organigramm, skill-registry and process-documentation maintenance. |
| `workflow create` | `workflow create` | Activates requirement clarification, workflow authoring, workflow validation and arc42 synchronization. |
| `workflow execute` | `workflow execute` | Activates checked slice execution, quality gates, documentation synchronization and slice checkpoint push. |
| `push` | publication mode | Runs the normal branch push and pull-request process without automatic merge. |
| `push auto` | `skills-agents` only | Runs the guarded skills-agents PR lifecycle after guard checks pass. |
| Slice checkpoint push | `workflow execute` only | Commits and pushes a successfully completed workflow slice after the slice quality gate passes. |

Slice checkpoint push is not `push auto`.
`push` is not `push auto`.
`skills update` is not `push auto`.

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

`skills-agents` maintains the virtual development team and its governance artifacts:

- skills
- agents
- roles
- prompts
- Codex agent definitions
- routing rules
- organigramm
- skill registry
- process documentation
- governance-limited arc42 or ADR notes

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

`push auto` belongs only to this strand. It must never publish backend, frontend, Docker/runtime, contracts, persistence, analysis-engine, Joern, JavaParser, BTM generator or product implementation changes.

## Strand 2: workflow create

`workflow create` turns a user request into a clarified, checked and executable workflow. It owns requirement clarification, branch governance before workflow artifact mutation, role review, workflow validation and arc42 synchronization.

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

The clarification loop is capped at `maxRetries = 3`. After retry exhaustion, `workflow create` stops and escalates to the Root Architect instead of continuing automatically.

The workflow create end state is:

1. no blocking requirement questions remain,
2. the workflow is checked,
3. arc42 is checked or updated,
4. Documentation Governance passes,
5. the result is explicitly released for `workflow execute`.

## Strand 3: workflow execute

`workflow execute` runs only checked workflow slices. It separates backend, frontend, runtime and documentation work, routes each slice to the required roles or subagents, runs quality gates and creates recoverable checkpoints.

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

Slice checkpoint push belongs to `workflow execute`. It commits only the completed slice and pushes the current workflow branch to `origin`; it does not create or merge a PR, run branch cleanup or run `push auto`.

Quality-gate and validation failures in `workflow execute` use the Typed Error
Router before retry or escalation. Retry attempts stay inside the active S3
execution scope, are capped at `maxRetries = 3` and must not jump back to
`workflow create`.

## Publication Modes

```mermaid
flowchart TD
  Checkpoint["Slice checkpoint push"]
  Push["push"]
  Auto["push auto"]
  Execute["workflow execute"]
  Pr["PR without automatic merge"]
  Skills["skills-agents"]
  Guard["Guarded PR lifecycle"]

  Execute --> Checkpoint
  Push --> Pr
  Skills --> Auto --> Guard
```

Publication modes are deliberately separate:

1. Slice checkpoint push belongs to `workflow execute`.
2. `push` is the normal branch push and pull-request process.
3. `push auto` belongs only to `skills-agents`.

This separation prevents a documentation, workflow or implementation slice from accidentally gaining guarded auto-merge authority.

## Architecture-Governance Role

The agent governance model is architecture governance because it protects:

- requirement clarification before workflow authoring,
- architecture-aware role routing,
- arc42 synchronization,
- ADR coverage for governance decisions,
- quality-gate selection,
- evidence-first documentation,
- controlled commit and push behavior,
- separation between planned behavior and implemented behavior.

The Senior System Architect owns the top-level governance boundary. Shared roles such as Documentation Governance, Skill Registry Maintainer, Organigramm Maintainer, Process Governance Maintainer and Push Auto Guard run inside the active strand rather than creating a fourth strand.
