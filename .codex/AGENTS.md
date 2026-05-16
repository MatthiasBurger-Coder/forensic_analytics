# Codex Team Instructions

This directory defines the durable Codex development-team structure for the Forensic Analytics repository.

The repository root `AGENTS.md` is the authoritative source for engineering rules, forensic evidence integrity, architecture boundaries, stop conditions, documentation language, and final reporting. `QUALITY.md` is the authoritative source for verification commands. If this file conflicts with either root document, the root document wins and the conflict must be reported before continuing.

## Directory Map

- `workflow/workflow-execution-rules.md` defines the team execution workflow.
- `subagents/` describes the durable role hierarchy and responsibilities.
- `skills/<skill-name>/SKILL.md` contains Codex-team skill entrypoints that delegate to the authoritative repository skills under `.agents/skills`.
- `agents/` contains project-scoped custom subagent TOML definitions used by Codex runtimes that support callable subagents.

## Mandatory Subagent Workflow

All non-trivial work must be routed through the subagent workflow before implementation.

The Agent Workflow Orchestrator coordinates:

- workplan discovery and reading,
- slice detection,
- subagent or role assignment,
- architecture-rule enforcement,
- quality-gate enforcement,
- stop-rule enforcement,
- result aggregation.

Direct implementation without subagent or role review is forbidden for non-trivial work.

Callable subagents should be used when the active request or workplan command authorizes delegated execution. If callable subagents are unavailable, apply the matching file under `subagents/` or `.agents/roles/` as an explicit local review checklist and report that no callable subagent was used.

## Mandatory Command

When the user writes exactly:

```text
workplan execute
```

Codex must:

1. Locate the active workplan.
2. Read all slices.
3. Assign subagents or role reviews.
4. Execute slice by slice.
5. Run tests and quality checks after each slice.
6. Review `git diff` and `git diff --check`.
7. Continue only when the slice is clean or a documented blocker is explicitly allowed by the workplan.

The detailed command behavior is defined by root `AGENTS.md` and `.agents/skills/workplan-executor/SKILL.md`.

## Team Hierarchy

```text
Agent Workflow Orchestrator
|
+-- Workplan Executor Skill
|
+-- Senior System Architect
    |
    +-- Senior Java Backend Developer
    +-- Senior React Frontend Developer
    +-- Senior UX Designer
    +-- Senior DevOps Engineer
    +-- Senior Tester
    +-- Documentation Engineer
    +-- Microservice Senior Expert
```

## Mandatory Architecture Rules

For service-split work:

- No shared Java code modules between microservices.
- No shared domain models.
- No shared event classes.
- No direct class dependencies between services.
- Communication is allowed only through REST/OpenAPI, gRPC/protobuf, or RabbitMQ/message contracts.
- Every service must be independently runnable, testable, containerized, and deployable.
- Every service must have its own Docker image.
- Every service must be deployable to Docker, Docker Swarm, and Kubernetes when the slice targets service deployment.

These rules do not authorize speculative service extraction. Service extraction requires a dedicated verified workplan slice.
