# Three Amigos Decision Record

## Decision

```text
READY_FOR_WORKFLOW
```

## Requirement Summary

Prepare the repository's agent, skill, prompt and governance landscape for later
controlled microservice migration. The workflow must not perform production
service extraction.

## Requirement View

- Scope: governance, skills, roles, prompts, quality documentation, workflow
  rules and architecture documentation for future microservice migration.
- Non-scope: production code migration, service directory creation, endpoint
  implementation, persistence splitting, container/deployment finalization and
  common Java module creation.
- Acceptance criteria: active workflow exists, slices are ordered, owners are
  named, quality gates are verified from `QUALITY.md`, stop conditions are
  explicit and later code changes require contract-first slices.

## Architecture View

- Service boundaries must be based on bounded contexts, not technical packages.
- The current Gradle layout is a modular monolith and must not be described as
  implemented microservices.
- Future service extraction requires no shared Java implementation modules,
  contract-first communication and runtime independence evidence.
- ADR-0006 currently limits Spring Boot to the boot app. Any later service with
  its own Spring Boot application requires a dedicated ADR and architecture-test
  slice.

## Quality View

- Workflow creation requires documentation diff review and `git diff --check`.
- Later execution must use `QUALITY.md` commands and strict dependency
  verification.
- Commit or push readiness cannot be claimed unless required gates actually run
  or the workflow records a blocker.

## Risk Level

```text
MEDIUM
```

Reason: this workflow changes governance and skill authority, not production
behavior. Risk increases during execution because multiple agent, skill and
documentation files may overlap.

## Stop Conditions

- Three Amigos readiness is skipped or becomes stale.
- Skill Registry ownership is unresolved.
- A slice introduces shared Java implementation between services.
- A slice claims service runtime independence without build, start, test,
  container, configuration and healthcheck evidence.
- A workflow references missing commands, skills, roles or contract files and
  would continue by guessing.

## Execution Refresh

Decision refreshed during `workflow execute` after Slice 00 branch verification:

```text
READY_FOR_WORKFLOW
```

Requirement Analyst finding:

- The workflow remains governance-only. The active execution request adds an
  explicit branch-created verification requirement for workflow execution
  guidance and does not request production service extraction.

Architecture Validator finding:

- The branch verification requirement is an orchestration safeguard and does
  not change product architecture, service boundaries, API contracts, storage,
  runtime evidence, graph, replay or LLM behavior.
- Future microservice work must still use bounded-context ownership,
  contract-first communication and no shared Java implementation modules.

Quality Validator finding:

- The added branch-created verification is testable with Git commands:
  `git branch --show-current`, `git show-ref --verify --quiet
  refs/heads/<workflow-branch>` and `git status --short --branch`.
- Slice execution quality remains governed by `QUALITY.md`; documentation-only
  slices require `git diff --check` plus the slice-specific diff inspection.

Dependency finding:

- Slice order remains acyclic. The persistent executor guidance update belongs
  to Slice 07 after the Skill Registry gate and skill-boundary slices establish
  ownership.
