# Three Amigos Decision Record

## Decision

```text
READY_FOR_WORKFLOW
```

## Requirement Summary

Correct the repository's agent, skill, prompt and governance landscape so future
workflow creation and execution are branch-safe, Three-Amigos-led,
architecture-governed, quality-gated and traceable.

## Requirement View

- Scope: root governance, workflow rules, skills, roles, prompts, quality
  documentation, architecture documentation, ADR references, governance docs,
  workplan docs and README references where verified.
- Non-scope: production code changes, service extraction, endpoint
  implementation, persistence splitting, runtime evidence changes, graph
  changes, replay behavior, LLM behavior and deployment finalization.
- Acceptance criteria: active workflow exists, slices are ordered, owners are
  named, quality gates are verified from `QUALITY.md`, stop conditions are
  explicit, branch-first and Three Amigos rules are hard requirements and
  microservice invariants are documented as non-optional.
- EPIC traceability: no EPIC was named. This is a non-blocking traceability gap
  for workflow creation because the request concerns governance rather than a
  runtime product capability.

## Architecture View

- Senior System Architect owns final architecture decisions and may block
  architecture-sensitive workflows.
- Workflow / Workplan Executor orchestrates slices and stop rules but does not
  override architecture.
- Agent Swarm Orchestrator coordinates subagents and handoffs but does not
  override architecture, Three Amigos, quality gates or microservice rules.
- Microservice invariants remain hard: no shared Java implementation modules,
  no shared domain or DTO classes, no shared event or test-fixture
  implementation classes and communication only through REST/OpenAPI,
  gRPC/protobuf or approved event contracts.
- The current repository contains `.agents/prompts/*.md`; `.codex/prompts/**`
  is not present and `.codex/AGENTS.md` states that `.codex` should remain
  portable.

## Quality View

- Workflow creation requires documentation diff review and `git diff --check`.
- Later execution must use `QUALITY.md` commands and strict dependency
  verification.
- Commit or push readiness cannot be claimed unless required gates actually run
  or the workflow records a blocker.
- Governance-only slices may use documented manual checks when no automated
  Markdown, link or role-conflict checker is verified.

## Dependency And Deadlock View

- Slice dependencies are acyclic and sequential by default.
- Shared files such as `AGENTS.md`, `QUALITY.md`, `.agents/prompts/**`,
  `.agents/skills/**` and `docs/governance/**` require explicit handoff.
- Read-only reviews may run in parallel.
- Write-capable parallel work is allowed only when file ownership is disjoint
  and the Senior Workflow Architect records the handoff.

## Risk Level

```text
MEDIUM
```

Reason: this workflow changes governance and skill authority, not production
behavior. Risk increases during execution because multiple agent, skill, prompt
and documentation files overlap.

## Stop Conditions

- The active workflow branch is missing, inactive or unverifiable.
- Three Amigos readiness is skipped or becomes stale.
- Skill Registry ownership is unresolved.
- A role claims authority above its verified governance boundary.
- A slice introduces shared Java implementation between services.
- A workflow references missing commands, skills, roles or contract files and
  would continue by guessing.
- A quality gate fails and cannot be safely corrected.
