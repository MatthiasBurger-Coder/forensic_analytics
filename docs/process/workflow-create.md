# Workflow Create Command

`workflow create` activates the workflow creation process strand.

This strand sharpens requirements, validates architecture impact, creates or updates `docs/workflow/workflow.md`, checks or updates arc42 documentation and releases the result for `workflow execute`.

`workflow create` must not implement backend code, frontend code, Docker/runtime code, analytics implementation code, contracts implementation or database implementation.

The local documentation node for this strand is `S2_DOC`. `S2_DOC` updates
concrete workflow-create artifacts such as the requirement gate result,
`docs/workflow/workflow.md`, workflow handoff and checked arc42 impact.
`DOCROOT` separately checks global documentation consistency and is not a
workflow-create editing step.

## Required Flow

```mermaid
flowchart TD
  Start["workflow create started"]
  Intake["Requirement Intake"]
  Clarify["Requirement Clarification Loop"]
  Blocking["Blocking Questions?"]
  Retry{"Clarification attempts <= 3?"}
  Ask["Ask focused clarification questions"]
  Incorporate["Incorporate answers"]
  Escalate["STOP: Root Architect escalation"]
  Gate["Three Amigos Requirement Gate"]
  Profile["Execution Profile Routing"]
  Branch["Branch Governance / Branch Verification"]
  Req["Senior Requirement Engineer review"]
  Arch["Senior System Architect review"]
  Java["Senior Java Backend Developer impact review"]
  React["Senior React Frontend Developer impact review"]
  Tester["Senior Tester review"]
  Workflow["Create or sharpen docs/workflow/workflow.md"]
  WorkflowCheck["workflow.md Validation"]
  Arc42["Update or check arc42"]
  Arc42Check["arc42 Validation"]
  Docs["S2_DOC: Documentation Governance review"]
  Final["Final workflow-create gate"]
  Approved["Approved for workflow execute"]
  Stop["STOP and return to gate"]

  Start --> Intake --> Clarify --> Blocking
  Blocking -->|yes| Retry
  Retry -->|yes| Ask --> Incorporate --> Clarify
  Retry -->|no| Escalate
  Blocking -->|no| Gate --> Profile --> Branch --> Req --> Arch --> Java --> React --> Tester --> Workflow --> WorkflowCheck --> Arc42 --> Arc42Check --> Docs --> Final --> Approved
  Gate --> Stop
  WorkflowCheck --> Stop
  Arc42Check --> Stop
  Docs --> Stop
```

## Requirement Clarification Loop

Automatic clarification attempts are capped at:

```text
maxRetries = 3
```

After three unresolved attempts, `workflow create` must STOP and escalate to the Root Architect. Validation correction loops use the same cap and must not silently continue.

The loop must record:

- Original Request
- Interpreted Intent
- Change Type
- Affected Process Strand
- Affected Architecture Area
- Explicit Requirements
- Implicit Requirements
- Assumptions
- Non-Goals
- Risks
- Open Questions
- Blocking Questions
- Confidence Level
- Decision:
  - `READY_FOR_WORKFLOW`
  - `PROCEED_WITH_ACCEPTED_ASSUMPTIONS`
  - `REQUIRES_REFINEMENT`

## Confidence Rule

Confidence greater than or equal to 90 percent means `READY_FOR_WORKFLOW` when no blocking questions remain.

Confidence from 70 to 89 percent means `PROCEED_WITH_ACCEPTED_ASSUMPTIONS` only when every assumption is non-blocking and documented.

Confidence below 70 percent means `REQUIRES_REFINEMENT`.

## Blocking Questions

When blocking questions remain:

- do not create a final `docs/workflow/workflow.md`
- do not release the request for `workflow execute`
- ask focused clarification questions
- return `REQUIRES_REFINEMENT`

Non-blocking uncertainty may be documented as an assumption only when it does not affect architecture boundaries, testability, data ownership, service boundaries, APIs, contracts, runtime behavior or scope.

## Execution Profile Routing

After the Three Amigos gate and before specialist role depth is selected,
classify the request through
`.agents/skills/execution-profile-router/SKILL.md`.

Profiles decide review depth:

- `FAST_PATH`: documentation-only changes with no behavior, branch, quality,
  routing, ownership or process-authority impact.
- `NORMAL_PATH`: isolated changes with verified owner, disjoint locks and no
  architecture, contract, persistence, runtime, deployment or quality-policy
  impact.
- `FULL_PATH`: workflow governance, skills, roles, routing, branch rules,
  quality rules, architecture-sensitive work or unclear impact.

The profile may reduce unaffected roles to N/A impact checks. It must not
remove Five-Role Three Amigos participation, branch-first workflow creation,
arc42 validation, Documentation Governance, STOP rules or required quality
gates.

## Five Mandatory Three Amigos Roles

`workflow create` must use these five roles:

- Senior Requirement Engineer: target, scope, non-goals, acceptance criteria, assumptions and open questions
- Senior System Architect: architecture boundaries, arc42, service boundaries, plugin-vs-analytics boundary and risks
- Senior Java Backend Developer: backend impact, ports, adapters, domain, JUnit 6 testability and Spring or microservice consequences
- Senior React Frontend Developer: frontend impact, UX flows, React components, state, API adapters and build or test consequences
- Senior Tester: testability, regression, quality gates, acceptance criteria and slice acceptance

Classic labels such as Requirement Analyst, Architecture Validator and Quality Validator may be additional perspectives. They do not replace the five mandatory roles.

## End Artifacts

`workflow create` is complete only when both artifacts have been checked:

1. complete checked `docs/workflow/workflow.md`
2. checked or updated `docs/arc42/**` documentation

`docs/workflow/workflow.md` must contain:

- Executive Summary
- Target Picture
- Scope
- Non-Goals
- Architecture Boundaries
- Backend Assessment
- Frontend Assessment
- Test Strategy
- Slice Structure
- Subagent Assignment
- Quality Gates
- Definition of Done
- Handoff to workflow execute
- arc42 Check Status

`workflow create` should also create `docs/workflow/context-pack.md` and
`docs/workflow/context-pack.json` when the workflow needs repeated role,
routing or quality decisions. The context pack is a secondary navigation aid
with hash provenance. It must not replace `AGENTS.md`, `QUALITY.md`, ADRs,
arc42, routing rules, workflow files or skill files.

## Machine-Readable Slice Metadata

Every executable workflow slice must include a fenced `yaml` metadata block
with concrete fields for slice ID, profile, owner, reviewers, affected files,
affected modules, affected contracts, dependencies, parallel group, file locks,
contract locks, architecture locks, quality gates, documentation duties and
stop conditions.

Dependencies must be concrete slice IDs. Ranges and prose-only dependencies are
not executable.
