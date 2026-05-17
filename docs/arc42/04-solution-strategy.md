# 4. Solution Strategy

## 4.1 Strategy Overview

The Forensics Platform separates producer-side triggers from central server-side analysis. Build plugins provide repository, branch, commit, build and runtime-launch context. The central platform checks out repositories, runs analysis, normalizes evidence, persists facts, correlates runtime data and produces generated artifacts such as BTM files.

## 4.2 Core Strategy

1. Use Gradle and Maven plugins only as request producers and runtime-binding adapters.
2. Normalize all inputs into a canonical analysis model.
3. Use stable IDs for classes, methods, callsites, branches, rules and runtime events.
4. Generate Byteman/BTM files server-side from an explicit instrumentation plan.
5. Let the plugin bind server-generated BTM files through the runtime agent when debugging requires instrumentation.
6. Collect runtime events with stable `ruleId` and `methodKey` references.
7. Build incidents from exception events.
8. Reconstruct replay timelines by correlation ID, trace ID, thread ID and sequence.
9. Build graph and vector projections from the canonical model.
10. Provide curated evidence packages to the LLM.
11. Keep repair automation gated by tests, quality gates and review.

## 4.3 MVP Strategy

The MVP focuses on read-only analysis:

- Static fact import
- Canonical model persistence
- Joern result import or attachment
- Server-side Byteman/BTM generation with stable rule IDs
- JSONL runtime event import
- Exception incident creation
- CorrelationID-based replay
- Simple graph projection
- LLM root-cause explanation without code modification

## 4.4 Non-MVP Scope

The following items are explicitly postponed:

- Automated patch generation
- Automated pull request creation
- Automated staging or production deployment
- Full Vector DB integration
- Production-ready multi-tenant architecture
- Complete graph UI with all layers

## 4.5 Repository Governance Strategy

Repository agent governance follows three process strands:

1. `skills-agents`
2. `workflow create`
3. `workflow execute`

`skills update` is the explicit entrypoint for the `skills-agents` strand. It updates skills, agents, roles, prompts, Codex agent definitions, routing rules, organigramm, skill registry and process documentation. It does not implement product behavior.

`workflow create` sharpens requirements through the Requirement Clarification Loop and the five-role Three Amigos Requirement Gate. It ends with checked `docs/workflow/workflow.md`, checked or updated arc42 documentation, Documentation Governance and explicit release for `workflow execute`.

`workflow execute` executes the checked workflow slice by slice. Each successful slice must run its quality gate, create a slice-scoped checkpoint commit and push the current workflow branch to `origin`.

Publication modes stay separate: slice checkpoint push, `push` and `push auto` are different processes. `push auto` belongs only to `skills-agents`.

## 4.6 Agent-Governed Engineering Strategy

The repository uses agent-governed engineering to prevent uncontrolled changes, mixed responsibilities and architecture drift.

The strategy separates:

- `skills update` for maintaining the virtual development team,
- `workflow create` for requirement clarification and architecture-aware workflow planning,
- `workflow execute` for controlled slice implementation.

This separation ensures that requirements are clarified before implementation, workflow artifacts are checked before execution and every implementation slice is validated, documented, committed and pushed as a recoverable checkpoint.

The Senior System Architect owns the top-level governance boundary. Documentation Governance is mandatory in every strand but does not create a separate fourth strand.
