# Microservices Ecosystem Conversion Execution Prompt

Use this prompt when the user writes:

```text
workflow execute
```

## Required Opening Checks

1. Verify the repository root:

```bash
git rev-parse --show-toplevel
```

2. Verify the active branch:

```bash
git branch --show-current
```

Expected branch:

```text
architecture/microservices-ecosystem-conversion-20260516
```

3. Inspect the working tree:

```bash
git status --short
```

4. Read the full workflow:

```text
docs/workflow/workflow.md
docs/workflow/three-amigos-decision-record.md
docs/workflow/current-state-baseline.md
docs/workflow/slice-dependency-map.md
docs/workflow/agent-handoff-matrix.md
docs/workflow/quality-gate-plan.md
```

## Execution Rules

- Execute one slice at a time.
- Route each slice through the owner and required reviews in
  `agent-handoff-matrix.md`.
- Use callable subagents only when the active request explicitly authorizes
  delegated or parallel agent work. Otherwise, use role files and skills as
  local review checklists.
- Stop if a required module, class, method, Gradle task, contract field,
  Dockerfile, deployment file or test command cannot be verified exactly.
- Do not introduce shared Java implementation modules between services.
- Do not claim service independence without build, start, test, healthcheck,
  configuration, container and deployment evidence.
- Do not commit or push before Slice 20 and required quality evidence.

## First Slice

Start with Slice 00 in `workflow.md`.
