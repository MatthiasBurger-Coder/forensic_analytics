# Codex Team Instructions

This directory defines a reusable Codex development-team structure for a repository.

The repository root `AGENTS.md`, when present, is the authoritative source for project-specific engineering rules, safety rules, architecture boundaries, stop conditions, documentation language, and final reporting. `QUALITY.md`, when present, is the authoritative source for verification commands. If this file conflicts with a root project document, the root document wins and the conflict must be reported before continuing.

## Directory Map

- `workflow/workflow-execution-rules.md` defines the reusable team execution workflow.
- `subagents/` describes the durable role hierarchy and responsibilities.
- `skills/<skill-name>/SKILL.md` contains reusable Codex-team skill entrypoints.
- `agents/` may contain project-scoped custom subagent TOML definitions used by Codex runtimes that support callable subagents.
- `.agents/`, when present, may contain project-specific role and skill extensions.

## Project Extension Model

Keep `.codex` portable.

Reusable `.codex` files must:

- avoid project names,
- avoid project-specific package names,
- avoid project-specific build tasks unless they are discovered from repository files,
- avoid hard dependencies on `.agents/skills/<project-name>-*`,
- prefer root `AGENTS.md`, `QUALITY.md`, and discovered local role or skill files for project-specific rules.

Project-specific rules belong in root `AGENTS.md`, `QUALITY.md`, `.agents/`, or project documentation.

## Portable Copy Set

To reuse this team model in another project, copy these paths:

- `.codex/AGENTS.md`
- `.codex/workflow/`
- `.codex/subagents/`
- `.codex/skills/`

Do not copy `.codex/agents/` as part of the portable template unless those TOML files have been generalized for the target project. That directory is allowed to contain runtime-specific or project-specific callable-agent definitions.

## Mandatory Subagent Workflow

All non-trivial work must be routed through the subagent workflow before implementation.

The Agent Workflow Orchestrator coordinates:

- workflow discovery and reading,
- slice detection,
- subagent or role assignment,
- architecture-rule enforcement,
- quality-gate enforcement,
- stop-rule enforcement,
- result aggregation.

Direct implementation without subagent or role review is forbidden for non-trivial work.

Callable subagents should be used when the active request or workflow command authorizes delegated execution. If callable subagents are unavailable, apply the matching file under `subagents/` or a discovered project role file as an explicit local review checklist and report that no callable subagent was used.

## Mandatory Command

When the user writes exactly:

```text
workflow execute
```

Codex must:

1. Locate the active workflow using the project-required checked workflow path.
2. Verify any project-required architecture-documentation check before implementation.
3. Read all slices.
4. Assign subagents or role reviews.
5. Execute slice by slice.
6. Run tests and quality checks after each slice.
7. Review `git diff` and `git diff --check`.
8. Continue only when the slice is clean or a documented blocker is explicitly allowed by the workflow.

If a project-specific workflow-executor skill exists, use it after reading this reusable workflow.

## Team Hierarchy

```text
Agent Workflow Orchestrator
|
+-- Workflow Executor Skill
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

## Default Microservice Guardrails

For service-split work, apply these guardrails unless the root project rules define a stricter or incompatible policy:

- No shared implementation modules between independently deployable services.
- No shared domain models between services.
- No shared event implementation classes between services.
- No direct class dependencies between services.
- Communication must use explicit external contracts such as REST/OpenAPI, gRPC/protobuf, or message contracts.
- Every service must be independently runnable, testable, containerized when containers are in scope, and deployable through the project's documented deployment targets.

These guardrails do not authorize speculative service extraction. Service extraction requires a dedicated verified slice or explicit user request.
