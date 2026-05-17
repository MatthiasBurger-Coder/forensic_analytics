# Governance Inventory

## Repository Baseline

| Item | Verified path |
|---|---|
| Repository root | `/mnt/d/Projects/forensic_analytics` |
| Active workflow branch | `architecture/workflow-governance-flowchart-v2-20260517` |
| Root agent rules | `AGENTS.md` |
| Quality contract | `QUALITY.md` |
| Active workflow folder | `docs/workflow/` |
| Process docs | `docs/process/` |
| Agent docs | `docs/agents/` |
| Governance docs | `docs/governance/` |
| arc42 docs | `docs/arc42/` |
| ADR docs | `docs/adr/` |
| Project roles | `.agents/roles/` |
| Project skills | `.agents/skills/` |
| Orchestrator docs | `.agents/orchestrator/` |
| Codex reusable workflow docs | `.codex/` |

## Existing Governance Capabilities

- Three process strands are documented: `skills-agents`, `workflow create`, `workflow execute`.
- `workflow create` branch-first behavior is documented.
- `workflow execute` slice checkpoint push is documented.
- `push auto` is restricted to `skills-agents`.
- Documentation Governance is already inside active strands, not a fourth strand.
- arc42 and ADR documentation already contain agent governance sections.

## Introduced V2 Labels

These labels are introduced by the Governance Flowchart V2 workflow and must be added or mapped by execution slices:

| Label | Meaning |
|---|---|
| S1 | Existing `skills-agents` strand |
| S2 | Existing `workflow create` strand |
| S3 | Existing `workflow execute` strand |
| S3D | Execution-orchestration node inside S3 |
| CP | Commit, checkpoint and rollback subgraph |
| PUB | Publication-mode subgraph |
| DOCROOT | Global documentation governance |
| `S1_DOC` | Local skills-agents documentation step |
| `S2_DOC` | Local workflow-create documentation step |
| `S3_DOC` | Local workflow-execute documentation step |

## Risks

| Risk | Mitigation in workflow |
|---|---|
| V2 labels are mistaken for new process strands | Explicit mapping to existing strands. |
| Automatic retry loops become unbounded | `maxRetries = 3` with Root Architect escalation. |
| S3 preflight checks silently continue on failure | Explicit STOP paths for status, branch and scope checks. |
| Quality failures route to generic retry | Typed Error Router with owner roles. |
| Parallel agents edit the same artifact | S3D conflict locks for files, contracts and architecture boundaries. |
| Rollback is interpreted as destructive reset | `CP_ROLLBACK` is a decision node with safe options and escalation. |
| Documentation nodes overlap | `DOCROOT` is separated from `S1_DOC`, `S2_DOC` and `S3_DOC`. |
| Workflow-create artifacts collide with `push auto` | Branches containing `docs/workflow/**` use normal `push` or workflow-execute slice checkpoint push, not `push auto`. |

## Open Governance Gaps For Slice 14

- Dedicated Root Architect role file is not present.
- Dedicated Flowchart Integrity Audit skill is not present.
- Typed Error Router is not yet represented by a dedicated skill or process document.
- Conflict Locking is present as orchestration intent, but not yet as a named governance node.

These gaps are documented and are not blockers for workflow creation because the user request defines the target semantics and Slice 14 owns the linkage decision.
