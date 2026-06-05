# Documentation Root Classification - 2026-06-05

## Purpose

This document classifies the repository documentation and artifact roots that
remain outside the arc42 chapter tree after the ADR Baseline Consolidation
workflow.

It does not move files, delete files, rewrite history, change contracts,
change deployment descriptors or create runtime behavior. It defines ownership
and authority so later workflows know whether a directory is an authoritative
architecture output, a source input, a process-governance root, a contract
artifact root, a deployment artifact root or historical evidence.

## Classification Terms

| Term | Meaning |
|---|---|
| Authoritative architecture output | Current architecture documentation produced under `docs/arc42/**`. |
| Source input | A requirement, ADR, contract, deployment, test or historical source read by architecture documentation. |
| Process-governance root | Documentation that governs agent, workflow, process or audit behavior rather than product architecture. |
| Contract artifact root | Files that define API, event, gRPC, OpenAPI or schema contracts. |
| Deployment artifact root | Files that define or document deployment, local runtime or observability descriptors. |
| Historical evidence | Existing repository documentation retained for traceability and compatibility input. |
| Pointer candidate | A directory that may later receive a short pointer to an arc42 authority, but only through a separate approved slice. |

## Root Classification

| Path | Classification | Current authority | Handling rule |
|---|---|---|---|
| `docs/arc42/` | Authoritative architecture output | Authoritative for architecture documentation, ADR consolidation outputs, requirement alignment, conflict analysis and final reports created by the ADR Baseline Consolidation workflow. | Keep architecture outputs here. Do not move contract or deployment artifacts here. |
| `docs/adr/` | Historical evidence and source input | Historical ADR source for ADR-0001 through ADR-0024. Mirrored byte-for-byte into `docs/arc42/09-architecture-decisions/adr/` during S05. | Do not rewrite or delete without a dedicated ADR/governance slice. New authoritative ADR output belongs under arc42. |
| `docs/agents/` | Process-governance root | Documents agent governance, organigramm and skill registry views. | Keep here unless a `skills-agents` governance workflow changes process documentation. |
| `docs/contracts/` | Source input and contract-governance documentation | Holds contract test planning documentation. It is not the contract artifact source of truth by itself. | Keep as documentation companion to `contracts/`. Architecture may summarize contract governance under arc42. |
| `contracts/` | Contract artifact root | Holds OpenAPI, gRPC/protobuf, event and schema contract artifacts. | Do not move into arc42. Contract changes require contract governance and compatibility review. |
| `docs/deployment/` | Source input and deployment documentation | Holds deployment documentation such as Docker Compose notes. | Keep as deployment documentation companion to `deployment/`. Architecture may summarize deployment constraints under arc42. |
| `deployment/` | Deployment artifact root | Holds Docker Compose, Swarm, Kubernetes and observability descriptors. | Do not move into arc42. Deployment changes require DevOps/runtime review and explicit verification. |
| `docs/epics/` | Requirement source input | Holds EPIC v0.2 as current requirement baseline and v0.1 as historical baseline. | Keep EPIC sources here. Requirement alignment outputs may be produced under arc42. |
| `docs/governance/` | Process-governance root | Holds governance docs and workflow diagrams. | Keep here. Architecture may reference governance decisions but must not absorb process diagrams by default. |
| `docs/process/` | Process-governance root | Holds branch, push, workflow-create, workflow-execute and skills-update process rules. | Keep here. Process rule changes belong to process/skills governance, not product architecture slices. |
| `docs/skill-audit/` | Process-governance root and audit evidence | Holds skill inventory, conflict audit and manual review records. | Keep here. Updates belong to `skills-agents` or governance workflows. |
| `docs/testing/` | Test documentation and source input | Holds testing and hardening documentation. | Keep here. Architecture may reference test constraints under arc42 but must not duplicate test guidance. |
| `docs/workflow/` | Workflow-control root | Holds the active executor entrypoint and execution reports. | Keep workflow-control artifacts here. Do not treat this as architecture output. |
| `docs/workflows/` | Workflow-control archive/root | Holds named workflow control files. | Keep workflow control here. Architecture outputs from workflows belong under arc42 when the workflow says so. |
| `docs/architecture/` | Historical architecture source input | Contains pre-consolidation architecture material and service-boundary source evidence. | Treat as historical/source input until a dedicated approved slice moves, mirrors or replaces content under arc42. |

## Directory-Specific Notes

### ADRs

`docs/adr/` remains historical source input. The arc42 ADR chapter under
`docs/arc42/09-architecture-decisions/adr/` is the authoritative architecture
output location for the ADR Baseline Consolidation workflow.

New numbered ADR output should be created under arc42 unless a future
repository governance decision explicitly changes this rule. Existing ADR
numbers must not be renumbered.

### Agents, Governance, Process And Skill Audit

`docs/agents/`, `docs/governance/`, `docs/process/` and `docs/skill-audit/`
are process-governance roots. They are intentionally outside arc42 because
they govern repository and agent operation rather than product architecture
structure.

Architecture documentation may reference the governance model, but process
rules must remain in their process-governance roots unless a dedicated
governance workflow changes that ownership.

### Contracts

The root `contracts/` directory is the contract artifact root. It contains
gRPC/protobuf, OpenAPI, event and schema artifacts. `docs/contracts/` is a
documentation companion for contract test planning.

arc42 may describe contract-first boundaries and architecture consequences, but
must not become the storage location for contract artifacts.

### Deployment

The root `deployment/` directory is the deployment artifact root. It contains
Docker Compose, Swarm, Kubernetes and observability descriptors. `docs/deployment/`
is deployment documentation.

arc42 may summarize deployment constraints and readiness limitations, but must
not claim runtime, Swarm or Kubernetes readiness unless repository evidence and
quality commands prove it.

### EPICs

`docs/epics/` holds requirement sources. EPIC v0.2 is the current requirement
baseline and v0.1 is historical. Requirement alignment outputs may be created
under `docs/arc42/01-introduction-and-goals/requirements/`, but EPIC source
files remain in `docs/epics/`.

### Testing

`docs/testing/` holds test and hardening documentation. It is source input for
quality and architecture documentation, not an arc42 replacement target.

## Move And Pointer Rules

- Do not move directories automatically based on naming alone.
- Do not delete historical source inputs without an approved slice and
  rollback notes.
- Do not create pointer stubs that duplicate architecture content.
- Do not place contract artifacts or deployment descriptors under arc42.
- Do not treat process-governance roots as product architecture outputs.
- Use arc42 for architecture summaries, traceability, requirements alignment,
  ADR consolidation, conflict analysis and final architecture reports.

## Current Decision

The listed directories remain in place. Their authority is classified by this
document so future workflow slices can decide whether to update arc42, update a
process-governance root, change a contract artifact, change a deployment
artifact, or read a historical source input.
