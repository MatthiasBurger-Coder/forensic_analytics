# Skill Conflict Matrix

## Purpose

This matrix records which governance skills review, support or block each other during workflow execution.

## Matrix

| Skill or role | Must run before | Can block | Consults | Primary conflict types |
| --- | --- | --- | --- | --- |
| Three Amigos Requirement Gatekeeper | Workflow authoring for new or changed requirements | Workflow authoring and execution | Requirement Engineering, Skill Registry & Conflict Auditor, Senior System Architect, Quality Gate Orchestrator | Requirement, architecture, quality, dependency |
| Skill Registry & Conflict Auditor | Workflow execution and new skill creation | Any workflow slice with skill or ownership conflict | Senior System Architect, Quality Gate Orchestrator, ADR Steward | Ownership, workflow, tooling, microservice boundary |
| Agent Handoff Protocol | Parallel work and owner transitions | Parallel or delegated execution | Senior Swarm Orchestrator, Workflow Executor, Workflow Conflict Resolution | Workflow, ownership, deadlock |
| Contract-First API Steward | REST, gRPC or messaging implementation | API or service communication changes | gRPC/Proto Specialist, Senior Java Backend, ADR Steward, Security & Threat Modeling | API, compatibility, microservice boundary |
| Data Ownership & Persistence Steward | Persistence and cross-service data flow changes | Persistence, projection or ownership changes | Analysis Storage Architect, Security & Threat Modeling, Observability & Runtime Diagnostics | Data ownership, security, architecture |
| Quality Gate Orchestrator | Commit readiness and required validation | Commit, push, slice continuation after failed required gates | Senior Tester, Senior DevOps, Senior System Architect | Quality, tooling |
| ADR Steward | Durable architecture or governance decisions | Architecture decision changes | Senior System Architect, arc42 governance, Skill Registry & Conflict Auditor | Architecture, workflow, ownership |
| Security & Threat Modeling | Security-sensitive APIs, containers, repositories, secrets or traces | Security-sensitive slices | Contract-First API Steward, Observability & Runtime Diagnostics, Data Ownership & Persistence Steward | Security, data ownership, tooling |
| Observability & Runtime Diagnostics | Service communication, workers, runtime events, diagnostics | Correlation, logging or runtime diagnostic changes | Security & Threat Modeling, Data Ownership & Persistence Steward, Senior DevOps | Observability, security, evidence integrity |
| Release & Branch Governance | Commit, push, PR or release actions | Commit, push and release readiness | Quality Gate Orchestrator, git governance skills, Workflow Conflict Resolution | Quality, release, branch ownership |

## Blocking Rules

- Missing owner is blocking.
- Failed required quality gate is blocking.
- Shared Java implementation module between microservices is blocking.
- Cross-service database access is blocking.
- Project-specific `.codex` governance without portability review is blocking.
- Breaking API change without ADR or consumer plan is blocking.

## Non-Blocking Notes

- Missing examples are non-blocking when required rules exist.
- Optional external checks may be non-blocking when `QUALITY.md` does not require them.
- Overlap between advisory and authoritative skills is non-blocking when authority is explicit.
