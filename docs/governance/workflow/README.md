# Governance Workflow Diagrams

This directory contains the two-level diagram package for Governance Flowchart
V2. The diagrams are visual projections of the authoritative rules in
`AGENTS.md`, `QUALITY.md`, `docs/process/**`, `docs/workflow/**`,
`docs/agents/**`, `docs/arc42/**` and the `docs/adr/README.md`
compatibility pointer.
The governing architecture decision is
[`ADR-0021: Governance Flowchart V2`](../../arc42/09-architecture-decisions/adr/ADR-0021-governance-flowchart-v2.md).

## Diagram Levels

- [Level 1 Overview](level-1-overview.md) shows the global governance flow:
  ROOT, commands, S1, S2, S3, hard boundaries, publication modes and global
  governance nodes.
- [Level 2 Subgraphs](level-2-subgraphs.md) shows the separately reviewable
  S1, S2, S3, BE, FE, RT, QG, CP, PUB and DOC subgraphs.

## Source Cross References

| Diagram area | Source documents |
|---|---|
| ROOT and hard boundaries | `AGENTS.md`, `docs/process/README.md`, `docs/governance/README.md` |
| S1 skills-agents | `docs/process/skills-update.md`, `docs/process/skill-agent-creation.md`, `docs/process/push-auto.md`, `docs/agents/agent-governance.md` |
| S2 workflow create | `docs/process/workflow-create.md`, `docs/process/three-amigos-requirement-gate.md`, `docs/workflow/workflow.md` |
| S3 workflow execute | `docs/process/workflow-execute.md`, `.agents/skills/workflow-executor/SKILL.md`, `.agents/orchestrator/swarm-orchestrator.md` |
| BE, FE and RT execution branches | `docs/process/workflow-execute.md`, `docs/arc42/05-building-block-view.md`, `docs/arc42/06-runtime-view.md` |
| QG quality gates | `QUALITY.md`, `docs/process/workflow-execute.md`, `.agents/skills/quality-gate-orchestrator/SKILL.md` |
| CP checkpoint and rollback | `docs/process/workflow-execute.md`, `docs/process/branch-governance.md`, `docs/workflow/workflow.history.md` |
| PUB publication modes | `docs/process/branch-governance.md`, `docs/process/push-auto.md` |
| DOC documentation governance | `docs/governance/README.md`, `docs/process/README.md`, `.agents/skills/documentation-sync/SKILL.md` |

## Diagram Review Rules

Flowchart integrity audits route through
`.agents/skills/flowchart-integrity-auditor/SKILL.md`. Senior Documentation
Engineer owns the documentation updates that follow the audit, and Senior
System Architect owns architecture-governance escalation for blocking findings.

Each Level 2 subgraph must be reviewed for:

- dead nodes
- missing `no` paths
- unbounded loops
- missing STOP paths
- circular references
- missing terminals
- wrong backward jumps
- missing escalation paths

The review must also confirm that `workflow execute` never calls
`workflow create` automatically and that publication failures route through a
controlled terminal, `CP_ROLLBACK` or Root Architect escalation.

## Required Terminal Or Escalation Nodes

These nodes must remain explicit in active diagrams or their owning source
documents:

- `S3_STOP_STATUS`
- `S3_STOP_BRANCH`
- `S3_STOP_SCOPE`
- `S3_UNCLASSIFIED`
- `QG_STOP`
- `CP_ROLLBACK`
- `PUB_DONE`
- `PUB_PR_RESULT`
- `PUB_PUSH_FAILED`
- `PUB_REJECTED`
- `ROOT_ARCHITECT`

## Validation Notes

The diagrams must not introduce product behavior, runtime behavior, service
contracts, Gradle tasks or implementation assumptions. If a diagram and a
source document disagree, the owning source document must be corrected or the
diagram must be updated before the workflow can pass documentation governance.
