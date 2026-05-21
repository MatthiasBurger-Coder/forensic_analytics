# Skill Registry

This registry maps process governance entries to owners, outputs and process strands.

Root `AGENTS.md` remains authoritative for mandatory behavior. `QUALITY.md` remains authoritative for verification.

## Process Strands

- `skills-agents`
- `workflow create`
- `workflow execute`

## Process Commands

| Command | Process strand | Owner | Output | Forbidden scope |
|---|---|---|---|---|
| `skills update` | `skills-agents` | Senior System Architect / Skill Registry Maintainer | approved skill/agent governance change | product implementation, services, contracts, Docker/runtime, build logic |
| `workflow create` | `workflow create` | Senior Workflow Architect / Senior Requirement Engineer | checked `docs/workflow/workflow.md` and checked or updated arc42 | backend, frontend, Docker/runtime or analytics implementation |
| `workflow execute` | `workflow execute` | Workflow Executor / Agent Swarm Orchestrator | executed checked workflow slices with quality gates and checkpoint pushes | work outside approved slice scope |

## skills update command

Process strand: `skills-agents`

Owner: Senior System Architect / Skill Registry Maintainer

Output: approved skill/agent governance change

May change:

- `AGENTS.md`
- `.agents/**`
- `.codex/**`
- `docs/README.md`
- `docs/agents/**`
- `docs/process/**`
- `docs/governance/**`
- governance-limited `docs/workflow/**`
- `docs/skill-audit/**`
- governance-limited `docs/arc42/**`
- governance-limited `docs/adr/**`

Must not change:

- product implementation
- services
- contracts
- Docker/runtime
- build logic

## Process Governance Entries

| Entry | Strand | Owner | Responsibility |
|---|---|---|---|
| Senior System Architect | all strands | Senior System Architect | architecture and governance authority |
| `DOCROOT` | all strands | Senior Documentation Engineer | global docs consistency for process docs, role model, organigramm, arc42 structure, governance rules, workflow conventions and hard boundaries |
| `S1_DOC` | `skills-agents` | Senior Documentation Engineer | update concrete skills, agents, roles, prompts, routing, organigramm, skill registry and process docs |
| `S2_DOC` | `workflow create` | Senior Documentation Engineer | update concrete requirement-gate, workflow authoring, workflow handoff and arc42-impact docs |
| `S3_DOC` | `workflow execute` | Senior Documentation Engineer / Workflow Executor | update concrete slice execution, quality gate, rollback, commit result and execution report docs |
| Skill / Agent Creator | `skills-agents` | Skill Registry Maintainer | create or update skills, roles, prompts and agent definitions |
| Skill Integrity Reviewer | `skills-agents` | Skill Registry Conflict Auditor | check dead references, ownership and duplicate authority |
| Skill Registry Maintainer | `skills-agents` | Senior System Architect | maintain this registry |
| Organigramm Maintainer | `skills-agents` | Senior Documentation Engineer | maintain role hierarchy diagrams |
| AGENTS.md Maintainer | `skills-agents` | Senior System Architect | keep root agent governance authoritative |
| Process Governance Maintainer | all strands | Senior Documentation Engineer | keep process documents synchronized |
| `S1_PUSH_ELIGIBILITY_GUARD` | `skills-agents` | Git Commit Reviewer / Git Commit Operator | block `push auto` outside skill, agent, process-governance and governance-only workflow documentation scope |
| `PUB_PR_MERGE_GUARD` | publication mode | Git Commit Reviewer / Git Commit Operator | decide whether a PR may merge, stay open, be blocked or be rejected |
| docs/workflow/workflow.md Maintainer | `workflow create` | Senior Workflow Architect | maintain active workflow specification |
| arc42 Architecture Documentation Maintainer | `workflow create` | arc42 Architecture Governance | keep architecture docs checked or updated |
| S3D Execution Orchestrator | `workflow execute` | Senior Swarm Orchestrator | build dependency graph, run topological sort and enforce file, contract, module and architecture-boundary locks |
| Testing Documentation Maintainer | `workflow execute` | Senior Tester | keep test strategy and quality-gate evidence |
| Execution Report Maintainer | `workflow execute` | Workflow Executor | record workflow version, slice ID, responsible agent, changed files, quality gates, commit SHA, rollback reference, documentation update status, push result and blockers |
| Commit Traceability Maintainer | `workflow execute` | Senior Documentation Engineer / Workflow Executor | keep `CP_RECORD` fields, one-slice-one-commit evidence and workflow history synchronized |
| Execution Profile Router | `workflow create`, `workflow execute` | Senior System Architect / Workflow Executor | classify requests as `FAST_PATH`, `NORMAL_PATH` or `FULL_PATH` before specialist routing without weakening mandatory gates |

## Governance Flowchart V2 Capability Linkage

| Capability | Owner or source | Status |
|---|---|---|
| Root Architect Escalation | Senior System Architect via Root Architect decision path | MAPPED_WITH_GAP: no dedicated `.agents/roles/root-architect.md` exists |
| Typed Error Routing | Workflow Executor, Quality Gate Orchestrator, routing rules | VERIFIED |
| Execution Orchestration | Senior Swarm Orchestrator, S3D, Agent Swarm Coordination Specialist | VERIFIED |
| Conflict Locking | Senior Swarm Orchestrator, S3D, Workflow Executor | VERIFIED |
| Rollback Governance | Release and Branch Governance, Git Commit Preparation, Senior DevOps | VERIFIED |
| Documentation Governance | Senior Documentation Engineer, Documentation Sync, `DOCROOT` | VERIFIED |
| Quality Gate Classification | Senior Tester, Quality Gate Orchestrator, Quality Gate skill | VERIFIED |
| Execution Profile Routing | `skills/execution-profile-router/SKILL.md`, routing rules, Swarm Orchestrator | VERIFIED |
| Flowchart Integrity Audit | Senior Documentation Engineer and Senior System Architect using `docs/governance/workflow/` rules | MAPPED_WITH_GAP: no dedicated flowchart-integrity skill exists |

Detailed evidence is recorded in
[`../skill-audit/governance-flowchart-v2-linkage.md`](../skill-audit/governance-flowchart-v2-linkage.md).
