# Governance Flowchart V2 Linkage Audit

## Purpose

This audit records the GOV-03 linkage check for Governance Flowchart V2
capabilities. It verifies which existing roles, skills and process documents
carry the required governance behavior and which dedicated artifact remains a
documented bootstrap gap.

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
| Root Architect Escalation | `.agents/roles/senior-system-architect.md`, `.agents/orchestrator/routing-rules.md`, `docs/process/workflow-execute.md` | MAPPED_WITH_BOOTSTRAP_OWNER | Root Architect escalation is represented by Root Architect decision nodes and Senior System Architect governance. No dedicated `.agents/roles/root-architect.md` file exists; the interim owner is explicit. |
| Typed Error Routing | `.agents/orchestrator/routing-rules.md`, `.agents/skills/workflow-executor/SKILL.md`, `.agents/skills/quality-gate-orchestrator/SKILL.md`, `docs/process/workflow-execute.md` | VERIFIED | All required error types are mapped: `ARCH_VIOLATION`, `BUILD_FAILURE`, `TEST_FAILURE`, `DOC_GOVERNANCE_FAILURE`, `LOCK_CONFLICT`, `UNKNOWN_FAILURE`. |
| Execution Orchestration | `.agents/roles/senior-swarm-orchestrator.md`, `.agents/orchestrator/swarm-orchestrator.md`, `.agents/skills/workflow-executor/SKILL.md`, `.agents/skills/agent-swarm-coordination-specialist/SKILL.md` | VERIFIED | S3D owns dependency graph extraction, topological ordering and execution grouping. |
| Conflict Locking | `.agents/orchestrator/swarm-orchestrator.md`, `.agents/skills/workflow-executor/SKILL.md`, `.agents/skills/agent-swarm-coordination-specialist/SKILL.md`, `docs/workflow/slice-dependency-map.md` | VERIFIED | File, contract, module and architecture-boundary locks are checked before parallel write-capable work. |
| Rollback Governance | `.agents/skills/release-branch-governance/SKILL.md`, `.agents/skills/git-commit-preparation/SKILL.md`, `docs/process/workflow-execute.md`, `docs/process/branch-governance.md` | VERIFIED | `CP_ROLLBACK` is documented as a decision node and is not a blind reset command. |
| Documentation Governance | `.agents/roles/senior-documentation-engineer.md`, `.agents/skills/documentation-sync/SKILL.md`, `docs/governance/README.md`, `docs/process/README.md`, `docs/agents/skill-registry.md` | VERIFIED | `DOCROOT` is global; `S1_DOC`, `S2_DOC` and `S3_DOC` update local strand artifacts. |
| Quality Gate Classification | `.agents/roles/senior-tester.md`, `.agents/skills/quality-gate-orchestrator/SKILL.md`, `.agents/skills/quality-gate/SKILL.md`, `.agents/orchestrator/routing-rules.md` | VERIFIED | Quality failures are classified through the Typed Error Router and D8 blocks commit, push and release readiness when required gates fail. |
| Flowchart Integrity Audit | `.agents/skills/flowchart-integrity-auditor/SKILL.md`, `docs/governance/workflow/README.md`, `docs/governance/workflow/level-1-overview.md`, `docs/governance/workflow/level-2-subgraphs.md`, `.agents/roles/senior-documentation-engineer.md`, `.agents/roles/senior-system-architect.md` | VERIFIED | Dedicated audit skill exists and owns the integrity decision; documentation and architecture owners remain explicit. |

## Conflict Classification

No blocking skill or role contradiction was found.

One dedicated-artifact gap remains non-blocking because the active workflow
documents an explicit bootstrap owner and follow-up path:

- Dedicated Root Architect role file: owner is Senior System Architect until a
  dedicated role is introduced by a future `skills-agents` slice.
- Flowchart Integrity Audit is resolved by the existing
  `.agents/skills/flowchart-integrity-auditor/SKILL.md`; no replacement skill
  or duplicate owner was introduced.

No existing skill or role was changed to resolve a contradiction because no
contradiction required a new authority artifact.

## Continuation Decision

`CONTINUE`

Workflow execution may continue because every required capability has a
verified owner or an explicit bootstrap owner, and every missing dedicated
artifact has a documented follow-up path.
