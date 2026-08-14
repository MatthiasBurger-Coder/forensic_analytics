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

Must not change:

- product implementation
- services
- contracts
- Docker/runtime
- build logic

## Role Entry-Point Classification

Project roles are discovered under `.agents/roles` and classified by their
actual entry point. Seventeen roles use flat `.md` documents and two roles use
directory-style `SKILL.md` files: `senior-requirement-engineer` and
`senior-workflow-architect`. The reconciled project-role count is 19. Callable
`.codex/agents/*.toml` definitions remain a separate inventory category.

## Process Governance Entries

| Entry | Strand | Owner | Responsibility |
|---|---|---|---|
| Senior System Architect | all strands | Senior System Architect | architecture and governance authority |
| Senior Requirement Engineer | `workflow create`, `workflow execute` | Senior Requirement Engineer | requirement integrity, EPIC consistency, traceability, scope control and requirement-drift escalation |
| `DOCROOT` | all strands | Senior Documentation Engineer | global docs consistency for process docs, role model, organigramm, arc42 structure, governance rules, workflow conventions and hard boundaries |
| `S1_DOC` | `skills-agents` | Senior Documentation Engineer | update concrete skills, agents, roles, prompts, routing, organigramm, skill registry and process docs |
| `S2_DOC` | `workflow create` | Senior Documentation Engineer | update concrete requirement-gate, workflow authoring, workflow handoff and arc42-impact docs |
| `S3_DOC` | `workflow execute` | Senior Documentation Engineer / Workflow Executor | update concrete slice execution, quality gate, rollback, commit result and execution report docs |
| Skill / Agent Creator | `skills-agents` | Skill Registry Maintainer | create or update skills, roles, prompts and agent definitions |
| Skill Integrity Reviewer | `skills-agents` | Skill Registry Conflict Auditor | check dead references, ownership and duplicate authority |
| Skill Registry Maintainer | `skills-agents` | Senior System Architect | maintain this registry |
| Persistent Skill Registry Matrix | `skills-agents`, `workflow execute` | Skill Registry Conflict Auditor / Senior Documentation Engineer | maintain and validate `docs/skill-audit/skill-registry.md` and `docs/skill-audit/skill-registry.json` as hash-invalidated cache evidence |
| Organigramm Maintainer | `skills-agents` | Senior Documentation Engineer | maintain role hierarchy diagrams |
| AGENTS.md Maintainer | `skills-agents` | Senior System Architect | keep root agent governance authoritative |
| Process Governance Maintainer | all strands | Senior Documentation Engineer | keep process documents synchronized |
| `S1_PUSH_ELIGIBILITY_GUARD` | `skills-agents` | Git Commit Reviewer / Git Commit Operator | block `push auto` outside skill, agent, process-governance and governance-only workflow documentation scope |
| `PUB_PR_MERGE_GUARD` | publication mode | Git Commit Reviewer / Git Commit Operator | decide whether a PR may merge, stay open, be blocked or be rejected |
| docs/workflow/workflow.md Maintainer | `workflow create` | Senior Workflow Architect | maintain active workflow specification |
| arc42 Architecture Documentation Maintainer | `workflow create` | arc42 Architecture Governance | keep architecture docs checked or updated |
| S3D Execution Orchestrator | `workflow execute` | Senior Execution Orchestrator / `s3d-execution-orchestrator` | build dependency graph, run topological sort and enforce file, contract, module and architecture-boundary locks |
| Testing Documentation Maintainer | `workflow execute` | Senior Tester | keep test strategy and quality-gate evidence |
| Execution Report Maintainer | `workflow execute` | Workflow Executor | record workflow version, slice ID, responsible agent, changed files, quality gates, commit SHA, rollback reference, documentation update status, push result and blockers |
| Commit Traceability Maintainer | `workflow execute` | Senior Documentation Engineer / Workflow Executor | keep `CP_RECORD` fields, one-slice-one-commit evidence and workflow history synchronized |
| Execution Profile Router | `workflow create`, `workflow execute` | Senior System Architect / Workflow Executor | classify requests as `FAST_PATH`, `NORMAL_PATH` or `FULL_PATH` before specialist routing without weakening mandatory gates |

| Governance Fast Path | `skills-agents`, `workflow create`, `workflow execute` | Execution Profile Router / Skill Registry Conflict Auditor | classify governance-only changes as `GOVERNANCE_FAST_PATH` when product, runtime, contract, quality, branch and publication impact are ruled out |
| Local Blocker Resolution | all strands | Active strand owner / Root Architect escalation | attempt bounded local blocker resolution before escalation without switching process strands |
| Quality Impact Classifier | `workflow execute` | Senior Tester / Quality Gate Orchestrator | classify changed files as `DOC_ONLY`, `GOVERNANCE_METADATA`, `PRODUCT_BUILD_AFFECTING` or `UNKNOWN` before quality command selection |
| Machine-Readable Slice Metadata | `workflow create`, `workflow execute` | Senior Workflow Architect / Workflow Executor / S3D | require concrete YAML slice metadata for dependency graph, lock and quality-gate validation |
| Persistent Registry Reuse | all strands | Skill Registry Conflict Auditor | allow registry-cache reuse only when hashes match and no governing files changed |
| Branch Strategy Matrix | all strands | Release Branch Governance / Git Branch Strategy | centralize `skills update`, `workflow create`, `workflow execute`, ad-hoc implementation and commit-preparation branch naming |
| Flowchart Integrity Auditor | `workflow create`, `workflow execute`, governance docs | Senior Documentation Engineer / `flowchart-integrity-auditor` | audit Level 1 and Level 2 governance diagrams for STOP paths, terminals, self-loops, fallback paths and forbidden backward jumps |
| Workflow Executor Resolution | `workflow execute` | Workflow Executor / Skill Registry Conflict Auditor | treat `.agents/skills/workflow-executor/SKILL.md` as active Forensic Analytics executor and `.codex/skills/workflow-executor/SKILL.md` as reusable base |
| Process Performance Profiler | `workflow execute` | Senior Performance Engineer / Workflow Executor | record process-performance diagnostics under `docs/workflow/metrics/**` without replacing gates or reviews |
| Source Code Responsibility | `workflow create`, `workflow execute` | Senior Java Backend / Senior System Architect | enforce one-class-one-responsibility, one-method-one-responsibility, one-variable-one-meaning, declarative-before-imperative design, Strategy Pattern use for varying behavior, small-file review triggers and IF-less source-code rework guidance alongside technical owner roles |

## Governance Flowchart V2 Capability Linkage

| Capability | Owner or source | Status |
|---|---|---|
| Root Architect Escalation | Senior System Architect via Root Architect decision path | MAPPED_WITH_GAP: no dedicated `.agents/roles/root-architect.md` exists |
| Typed Error Routing | Workflow Executor, Quality Gate Orchestrator, routing rules | VERIFIED |
| Execution Orchestration | Senior Execution Orchestrator, S3D, Senior Swarm Orchestrator coordination | VERIFIED |
| Conflict Locking | Senior Execution Orchestrator, S3D, Workflow Executor, Senior Swarm Orchestrator coordination | VERIFIED |
| Rollback Governance | Release and Branch Governance, Git Commit Preparation, Senior DevOps | VERIFIED |
| Documentation Governance | Senior Documentation Engineer, Documentation Sync, `DOCROOT` | VERIFIED |
| Requirement Governance | Senior Requirement Engineer, Requirement Engineering, Three Amigos Requirement Gatekeeper | VERIFIED |
| Quality Gate Classification | Senior Tester, Quality Gate Orchestrator, Quality Gate skill, `skills/quality-impact-classifier/SKILL.md` | VERIFIED |
| Execution Profile Routing | `skills/execution-profile-router/SKILL.md`, routing rules, Swarm Orchestrator, `GOVERNANCE_FAST_PATH` | VERIFIED |
| Flowchart Integrity Audit | `skills/flowchart-integrity-auditor/SKILL.md`, Senior Documentation Engineer, Senior System Architect escalation | VERIFIED |
| Persistent Skill Registry Matrix | `docs/skill-audit/skill-registry.md`, `docs/skill-audit/skill-registry.json`, `skill-registry-conflict-auditor` | VERIFIED |
| Branch Strategy Matrix | `docs/process/branch-governance.md`, `AGENTS.md`, `git-branch-strategy`, `release-branch-governance`, `git-commit-preparation` | VERIFIED |
| Workflow Executor Resolution | `.agents/skills/workflow-executor/SKILL.md`, `.codex/skills/workflow-executor/SKILL.md`, `.codex/AGENTS.md`, `.codex/workflow/workflow-execution-rules.md`, `docs/process/workflow-execute.md` | VERIFIED |
| Process Performance Profiling | `skills/process-performance-profiler/SKILL.md`, `docs/workflow/metrics/README.md`, Workflow Executor | VERIFIED |
| Source Code Responsibility | `skills/source-code-responsibility/SKILL.md`, routing rules, Swarm Orchestrator | VERIFIED |

Detailed evidence is recorded in
[`../skill-audit/governance-flowchart-v2-linkage.md`](../skill-audit/governance-flowchart-v2-linkage.md).
