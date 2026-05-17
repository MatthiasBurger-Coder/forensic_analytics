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
| Documentation Governance | all strands | Senior Documentation Engineer | docs consistency and source-of-truth alignment |
| Skill / Agent Creator | `skills-agents` | Skill Registry Maintainer | create or update skills, roles, prompts and agent definitions |
| Skill Integrity Reviewer | `skills-agents` | Skill Registry Conflict Auditor | check dead references, ownership and duplicate authority |
| Skill Registry Maintainer | `skills-agents` | Senior System Architect | maintain this registry |
| Organigramm Maintainer | `skills-agents` | Senior Documentation Engineer | maintain role hierarchy diagrams |
| AGENTS.md Maintainer | `skills-agents` | Senior System Architect | keep root agent governance authoritative |
| Process Governance Maintainer | all strands | Senior Documentation Engineer | keep process documents synchronized |
| Push Auto Guard | `skills-agents` | Git Commit Reviewer / Git Commit Operator | block `push auto` outside skills-agents |
| docs/workflow/workflow.md Maintainer | `workflow create` | Senior Workflow Architect | maintain active workflow specification |
| arc42 Architecture Documentation Maintainer | `workflow create` | arc42 Architecture Governance | keep architecture docs checked or updated |
| S3D Execution Orchestrator | `workflow execute` | Senior Swarm Orchestrator | build dependency graph, run topological sort and enforce file, contract, module and architecture-boundary locks |
| Testing Documentation Maintainer | `workflow execute` | Senior Tester | keep test strategy and quality-gate evidence |
| Execution Report Maintainer | `workflow execute` | Workflow Executor | record slice commit SHA, push result and blockers |
