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
  Ask["Ask focused clarification questions"]
  Incorporate["Incorporate answers"]
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
  Blocking -->|yes| Ask --> Incorporate --> Clarify
  Blocking -->|no| Gate --> Branch --> Req --> Architect --> Java --> React --> Tester --> Workflow --> WorkflowCheck --> Arc42 --> Arc42Check --> Docs --> Final --> Release
```

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
  Executor["workflow-executor"]
  Swarm["Agent Swarm Orchestrator"]
  Backend["Backend Strand"]
  Frontend["Frontend Strand"]
  Runtime["Docker / Runtime Strand"]
  Docs["Documentation Strand"]
  Gate["Slice Quality Gate"]
  Commit["Slice Checkpoint Commit"]
  Push["Push Workflow Branch"]
  Final["Final Workflow Execute Gate"]

  Executor --> Swarm
  Swarm --> Backend
  Swarm --> Frontend
  Swarm --> Runtime
  Swarm --> Docs
  Backend --> Gate
  Frontend --> Gate
  Runtime --> Gate
  Docs --> Gate
  Gate --> Commit --> Push --> Final
```

Slice checkpoint push belongs to `workflow execute`. It commits only the completed slice and pushes the current workflow branch to `origin`; it does not create or merge a PR, run branch cleanup or run `push auto`.

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
