# Persistent Skill Registry Matrix

This file is the human-readable companion to
`docs/skill-audit/skill-registry.json`.

The matrix is a cache for routing and conflict-audit reuse. It is not the
source of truth. The source of truth remains the repository files listed in the
registry, with root `AGENTS.md` and `QUALITY.md` taking precedence for mandatory
behavior and quality commands.

## Status

| Field | Value |
|---|---|
| Registry version | `skill-registry-v1` |
| Workflow version | `governance-performance-20260521-v1` |
| Last verified | `2026-05-21` |
| Reuse status | `CONDITIONAL_REUSE_ALLOWED` |
| Registry scope | Governance-routing-critical matrix with counts for full repositories of skills, roles and callable agent definitions |

## Inventory Counts

| Asset set | Count | Source path |
|---|---:|---|
| Project skills | 75 | `.agents/skills/*/SKILL.md` |
| Project roles | 17 | `.agents/roles/*.md` |
| Reusable Codex skills | 6 | `.codex/skills/*/SKILL.md` |
| Callable Codex agents | 34 | `.codex/agents/*.toml` |

The existing exhaustive narrative inventory remains in
`docs/skill-audit/skill-inventory.md`. This matrix records the cache and reuse
decision for the governance-critical routing set.

## Reuse Rule

Previous registry evidence may be reused for routing only when all conditions
are true:

- recorded hashes match the current repository files;
- no file under `.agents/**`, `.codex/**`, `AGENTS.md`, `QUALITY.md`,
  `docs/workflow/**`, `docs/skill-audit/**`, `docs/agents/**`,
  `docs/process/**` or `docs/governance/**` changed since the registry was
  verified;
- no unresolved owner, STOP-rule, quality authority or architecture authority
  conflict is recorded as ready;
- the active workflow does not touch skill, role, routing, quality, process,
  branch, flowchart or workflow-executor governance.

Manual review is required when any condition fails. The manual review must
reopen the authoritative files and refresh this matrix only after the conflict
decision is explicit.

## Governance-Critical Matrix

| Entry | Path | Owner | Scope | Cache rule |
|---|---|---|---|---|
| Root agent governance | `AGENTS.md` | Senior System Architect | mandatory repository behavior | hash must match |
| Quality authority | `QUALITY.md` | Senior Tester / Quality Gate Orchestrator | required quality commands | hash must match |
| Project workflow executor | `.agents/skills/workflow-executor/SKILL.md` | Workflow Executor | active Forensic Analytics execution protocol | hash must match |
| Reusable workflow executor | `.codex/skills/workflow-executor/SKILL.md` | Reusable Codex workflow base | portable base protocol | conflict status must be checked |
| Execution profile router | `.agents/skills/execution-profile-router/SKILL.md` | Senior System Architect / Workflow Executor | `FAST_PATH`, `NORMAL_PATH`, `FULL_PATH` routing | hash must match |
| Quality impact classifier | `.agents/skills/quality-impact-classifier/SKILL.md` | Senior Tester / Quality Gate Orchestrator | quality command impact decision | hash must match |
| S3D execution orchestrator | `.agents/skills/s3d-execution-orchestrator/SKILL.md` | Senior Execution Orchestrator | dependency graph, topological groups and locks | hash must match |
| Flowchart integrity auditor | `.agents/skills/flowchart-integrity-auditor/SKILL.md` | Senior Documentation Engineer / Senior System Architect | governance diagram integrity audit | hash must match |
| Process performance profiler | `.agents/skills/process-performance-profiler/SKILL.md` | Senior Performance Engineer / Workflow Executor | process diagnostics under `docs/workflow/metrics/**` | hash must match |
| Skill registry conflict auditor | `.agents/skills/skill-registry-conflict-auditor/SKILL.md` | Senior System Architect | ownership and compatibility review | hash must match |
| Routing rules | `.agents/orchestrator/routing-rules.md` | Agent Workflow Orchestrator | specialist and typed-error routing | hash must match |
| Swarm orchestrator | `.agents/orchestrator/swarm-orchestrator.md` | Senior Swarm Orchestrator | handoff and coordination rules | hash must match |
| Process skill registry | `docs/agents/skill-registry.md` | Skill Registry Maintainer | process-strand ownership map | hash must match |
| Skills update process | `docs/process/skills-update.md` | Senior Documentation Engineer | `skills-agents` flow | hash must match |
| Workflow execute process | `docs/process/workflow-execute.md` | Workflow Executor | slice execution flow | hash must match |
| Active workflow | `docs/workflow/workflow.md` | Senior Workflow Architect | executable slice scope | hash must match |

## Known Conflict Decisions

| Conflict | Status | Decision |
|---|---|---|
| Duplicate front-matter name `workflow-executor` in `.agents/skills/workflow-executor/SKILL.md` and `.codex/skills/workflow-executor/SKILL.md` | `RESOLVED_BY_S09` | Front-matter names remain unchanged. `.agents/skills/workflow-executor/SKILL.md` is the active Forensic Analytics executor and `.codex/skills/workflow-executor/SKILL.md` is the reusable base protocol. |

Any route that depends on executor identity must use the project-specific
executor during Forensic Analytics `workflow execute` and may read the `.codex`
executor only for reusable baseline context or conflict detection.

## Blocking Rules

The persistent matrix must never allow:

- hidden reuse after governing files changed;
- unresolved conflicts to be reported as ready;
- missing owners or missing STOP rules to be bypassed;
- the JSON registry to override repository files;
- `QUALITY.md` gates to be weakened by cache reuse.
