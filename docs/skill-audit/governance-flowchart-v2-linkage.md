# Governance Flowchart V2 Linkage Audit

## Purpose

This audit records the GOV-03 linkage check for Governance Flowchart V2
capabilities. It verifies which roles, skills and process documents carry the
required governance behavior.

No product implementation, build logic, contracts, runtime behavior or
analytics behavior is changed by this audit.

## Inspected Sources

- `.agents/orchestrator/routing-rules.md`
- `.agents/orchestrator/swarm-orchestrator.md`
- `.agents/roles/**`
- `.agents/skills/**`
- `docs/agents/skill-registry.md`
- `docs/process/**`
- `docs/governance/workflow/**`
- `docs/workflow/**`
- `docs/skill-audit/**`

## Capability Matrix

| Capability | Verified owner or source | Status | Notes |
|---|---|---|---|
| Root Architect Escalation | `.agents/roles/root-architect.md`, `.agents/roles/senior-system-architect.md`, `.agents/orchestrator/routing-rules.md`, `docs/process/workflow-execute.md` | VERIFIED | Root Architect escalation has a dedicated role and explicit collaboration with the Senior System Architect. |
| Typed Error Routing | `.agents/orchestrator/routing-rules.md`, `.agents/skills/workflow-executor/SKILL.md`, `.agents/skills/quality-gate-orchestrator/SKILL.md`, `docs/process/workflow-execute.md` | VERIFIED | All required error types are mapped: `ARCH_VIOLATION`, `BUILD_FAILURE`, `TEST_FAILURE`, `DOC_GOVERNANCE_FAILURE`, `LOCK_CONFLICT`, `UNKNOWN_FAILURE`. |
| Execution Orchestration | `.agents/roles/senior-swarm-orchestrator.md`, `.agents/orchestrator/swarm-orchestrator.md`, `.agents/skills/workflow-executor/SKILL.md`, `.agents/skills/agent-swarm-coordination-specialist/SKILL.md` | VERIFIED | S3D owns dependency graph extraction, topological ordering and execution grouping. |
| Conflict Locking | `.agents/orchestrator/swarm-orchestrator.md`, `.agents/skills/workflow-executor/SKILL.md`, `.agents/skills/agent-swarm-coordination-specialist/SKILL.md`, `docs/workflow/slice-dependency-map.md` | VERIFIED | File, contract, module and architecture-boundary locks are checked before parallel write-capable work. |
| Rollback Governance | `.agents/skills/release-branch-governance/SKILL.md`, `.agents/skills/git-commit-preparation/SKILL.md`, `docs/process/workflow-execute.md`, `docs/process/branch-governance.md` | VERIFIED | `CP_ROLLBACK` is documented as a decision node and is not a blind reset command. |
| Documentation Governance | `.agents/roles/senior-documentation-engineer.md`, `.agents/skills/documentation-sync/SKILL.md`, `docs/governance/README.md`, `docs/process/README.md`, `docs/agents/skill-registry.md` | VERIFIED | `DOCROOT` is global; `S1_DOC`, `S2_DOC` and `S3_DOC` update local strand artifacts. |
| Quality Gate Classification | `.agents/roles/senior-tester.md`, `.agents/skills/quality-gate-orchestrator/SKILL.md`, `.agents/skills/quality-gate/SKILL.md`, `.agents/orchestrator/routing-rules.md` | VERIFIED | Quality failures are classified through the Typed Error Router and D8 blocks commit, push and release readiness when required gates fail. |
| Flowchart Integrity Audit | `.agents/skills/flowchart-integrity-auditor/SKILL.md`, `docs/governance/workflow/README.md`, `docs/governance/workflow/level-1-overview.md`, `docs/governance/workflow/level-2-subgraphs.md`, `.agents/roles/senior-documentation-engineer.md`, `.agents/roles/senior-system-architect.md` | VERIFIED | Dedicated audit skill exists and owns the integrity decision; documentation and architecture owners remain explicit. |

## Conflict Classification

No blocking skill or role contradiction was found.

The Root Architect role is now a dedicated escalation artifact. The Flowchart
Integrity Audit remains owned by the existing
`.agents/skills/flowchart-integrity-auditor/SKILL.md`; no replacement skill or
duplicate owner was introduced.

No existing skill or role was changed to resolve a contradiction because no
contradiction required a new authority artifact.

## Continuation Decision

`CONTINUE`

Workflow execution may continue because every required capability has a
verified owner and the dedicated Root Architect escalation artifact exists.
