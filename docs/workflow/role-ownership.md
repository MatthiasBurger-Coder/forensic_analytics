# Role Ownership

## Mandatory Roles

| Role | Workflow Responsibility |
|---|---|
| Senior Requirement Engineer | Requirement scope, assumptions, non-goals, acceptance criteria. |
| Senior System Architect | Service-boundary, target-vs-transitional, arc42, ADR and evidence-integrity review. |
| Senior Java Backend Developer | Java service runtime, Dockerfile, Gradle task and health endpoint verification. |
| Senior React Frontend Developer | `forensic-ui`, public API routing, GUI smoke workflow. |
| Senior Tester | Slice test strategy, quality gates, Compose validation and final verification. |

## Specialist Roles

| Role | Applies To |
|---|---|
| Senior DevOps Engineer | Docker Compose, Dockerfiles, network, ports, volumes, health checks, startup and cleanup commands. |
| Senior Workflow Architect | Slice metadata, dependency graph, workflow consistency and handoff. |
| Senior Documentation Engineer | Deployment runbook, README updates, execution report, arc42 wording. |
| Senior Joern CPG Specialist | Joern image, workspace, artifact and semantic-analysis container risks. |
| Source Analysis Reviewer | JavaParser and Java AST source-fact semantics. |
| Replay Graph LLM Reviewer | Graph replay and report-generation planned roots and evidence boundaries. |
| Senior Analysis Storage Architect | Analysis Store transitional persistence and artifact metadata ownership. |
| Ingestion Handoff Reviewer | Ingestion and forensic-ingestion handoff semantics. |

## Slice Ownership

| Slice | Primary Owner | Secondary Reviewers |
|---|---|---|
| S01 | Senior DevOps Engineer | Senior System Architect, Senior Java Backend Developer, Senior Tester |
| S02 | Senior DevOps Engineer | Senior Java Backend Developer, Senior System Architect, Senior Tester |
| S03 | Senior DevOps Engineer | Senior Java Backend Developer, Senior Tester |
| S04 | Senior DevOps Engineer | Senior Java Backend Developer, Source Analysis Reviewer, Senior Tester |
| S05 | Senior DevOps Engineer | Senior Joern CPG Specialist, Senior Java Backend Developer, Senior Tester |
| S06 | Senior DevOps Engineer | Senior Java Backend Developer, Senior System Architect, Senior Tester |
| S07 | Senior DevOps Engineer | Senior Java Backend Developer, Senior React Frontend Developer, Senior Tester |
| S08 | Senior DevOps Engineer | Senior Java Backend Developer, Senior Tester |
| S09 | Senior DevOps Engineer | Observability Runtime Diagnostics, Senior System Architect, Senior Tester |
| S10 | Senior Tester | Senior DevOps Engineer, Senior System Architect |
| S11 | Senior DevOps Engineer | Senior Java Backend Developer, Ingestion Handoff Reviewer, Senior Tester |
| S12 | Senior DevOps Engineer | Senior Java Backend Developer, Senior System Architect, Senior Tester |
| S13 | Senior DevOps Engineer | Senior Java Backend Developer, Senior Analysis Storage Architect, Senior Tester |
| S14 | Senior DevOps Engineer | Senior Java Backend Developer, Source Analysis Reviewer, Senior Tester |
| S15 | Senior DevOps Engineer | Senior Java Backend Developer, Source Analysis Reviewer, Senior Tester |
| S16 | Senior DevOps Engineer | Senior Joern CPG Specialist, Senior Java Backend Developer, Senior Tester |
| S17 | Senior DevOps Engineer | Senior Java Backend Developer, Senior Tester |
| S18 | Senior System Architect | Replay Graph LLM Reviewer, Senior DevOps Engineer, Senior Tester |
| S19 | Senior System Architect | Replay Graph LLM Reviewer, Senior DevOps Engineer, Senior Tester |
| S20 | Senior React Frontend Developer | Senior DevOps Engineer, Senior Tester |
| S21 | Senior Documentation Engineer | Senior DevOps Engineer, Senior Tester, Senior System Architect |
| S22 | Senior Tester | Senior DevOps Engineer, Senior System Architect, Senior Documentation Engineer |

## Subagent Rule

The user explicitly requested subagents. `workflow execute` should use callable
subagents where available. If a callable subagent is unavailable for a required
role, execution must apply the matching `.agents/roles` file as a local review
checklist and report the limitation.
