---
name: three-amigos-requirement-gatekeeper
description: "Use before workflow authoring or execution to validate incoming requirements with a five-role Three Amigos gate: requirement completeness, architecture fit, backend impact, frontend impact, quality/testability, dependency cycles, deadlock risks, slice boundaries, required skills, subagent availability, and READY_FOR_WORKFLOW / PROCEED_WITH_ACCEPTED_ASSUMPTIONS / REQUIRES_REFINEMENT decisions."
---

# Skill: Three Amigos Requirement Gatekeeper

## Mission

Validate and refine incoming requirements before implementation work starts.

Prevent:

- architecture drift
- undefined ownership
- unclear acceptance criteria
- incomplete workflows
- invalid slice dependencies
- deadlocks caused by unstable contracts, cyclic workflows or shared file ownership

## Authority

The gatekeeper may:

- reject requirements
- stop workflows
- request refinements
- split oversized slices
- enforce architecture, quality and evidence-integrity rules
- require specialist role or subagent review before workflow authoring

## Boundaries

- Do not implement production code.
- Do not generate workflow files directly; route ready requirements to `workflow-authoring`.
- Do not execute workflows; route execution to `workflow-executor`.
- Do not override root `AGENTS.md`, `QUALITY.md`, ADRs or verified repository behavior.
- Do not treat planned behavior, LLM output or inferred relationships as verified evidence.

## Required Inputs

Inspect the relevant subset of:

- user requirement and acceptance criteria
- root `AGENTS.md`
- root `QUALITY.md`
- `docs/epics`
- `docs/arc42`
- `docs/adr`
- existing `docs/workflow`
- `.agents/orchestrator`
- `.agents/roles`
- `.agents/skills`
- `.codex/agents`
- affected source, tests, schemas, fixtures, examples, API contracts or build files when named by the requirement

Use EPIC source precedence:

1. the EPIC explicitly named by the user
2. the EPIC referenced by the active workflow
3. the EPIC that directly matches the requested domain area under `docs/epics`
4. no EPIC, reported as an open traceability gap

When an active workflow exists and the user asks for a new requirement or workflow, first determine whether the request extends, replaces or conflicts with the active workflow. Return `REQUIRES_REFINEMENT` if that relationship is unclear.

## Reference Files

Load these files only when needed:

- `workflow.md` for the full gate workflow.
- `decision-rules.md` for stop, refinement, architecture, dependency and parallelization rules.
- `anti-patterns.md` when reviewing risky requirement or slice shapes.
- `templates/requirement-template.md` when normalizing a requirement.
- `templates/slice-template.md` when drafting slice boundaries.
- `templates/acceptance-template.md` when acceptance criteria are missing or incomplete.
- `examples/forensic-analytics-example.md` for a compact example output.

## Three Amigos Review

Before the role review, run the Requirement Clarification Loop. Record:

- Original Request
- Interpreted Intent
- Change Type
- Affected Process Strand
- Affected Architecture Area
- Explicit Requirements
- Implicit Requirements
- Assumptions
- Non-Goals
- Risks
- Open Questions
- Blocking Questions
- Confidence Level
- Decision

Blocking questions must remain visible and must block final checked
`docs/workflow/workflow.md` creation and release for `workflow execute`.

Run the review with these mandatory roles:

- Senior Requirement Engineer: goal, scope, non-goals, acceptance criteria, assumptions and open questions.
- Senior System Architect: architecture boundaries, arc42, service boundaries, plugin-vs-analytics boundary and risks.
- Senior Java Backend Developer: backend impact, ports, adapters, domain, JUnit 6 testability, Spring and microservice consequences.
- Senior React Frontend Developer: frontend impact, UX flows, React components, state, API adapters and build/test consequences.
- Senior Tester: testability, regression, quality gates, acceptance criteria and slice acceptance.

Requirement Analyst, Architecture Validator and Quality Validator may be used
only as additional perspectives. They do not replace the five mandatory roles.

Add a Dependency / Deadlock Validator pass when the request contains multiple slices, services, subagents, shared files, shared APIs, orchestration steps, queues, workers or rollout dependencies.

Always ask:

```text
Does the implementation still match the EPIC?
```

For microservice migration requests, require a Three Amigos Decision Record with:

- scope
- non-scope
- acceptance criteria
- service boundary
- contract impact
- data ownership impact
- test impact
- risk level
- rollback or strangler strategy
- stop conditions

Ask explicitly:

- What service problem is being solved?
- What inputs and outputs cross the service boundary?
- What data is owned by the target service?
- Which dependencies are allowed?
- Which communication mechanisms are allowed?
- Which tests prove the slice is safe?
- Which changes are forbidden in this slice?

Classify the requirement as one or more of:

- functional requirement
- non-functional requirement
- architecture constraint
- resilience requirement
- scalability requirement
- UX requirement
- observability requirement
- security or data-protection requirement
- quality-gate requirement
- assumption
- open question

## Related Skills

Use related skills instead of duplicating their full workflows:

- `requirement-engineering` for requirement classification, EPIC drift and traceability.
- `engineering-governance` for cross-document governance synchronization.
- `arc42-architecture-governance` for architecture documentation impact.
- `workflow-slice` for implementation slice planning after the gate.
- `workflow-authoring` only after a `READY_FOR_WORKFLOW` decision.
- `quality-gate-governance` for verified quality commands.
- `documentation-sync` for documentation alignment.
- `agent-swarm-coordination-specialist` when the user explicitly authorizes subagent or parallel work.

## Decision Contract

Return exactly one decision:

```text
READY_FOR_WORKFLOW
```

or

```text
PROCEED_WITH_ACCEPTED_ASSUMPTIONS
```

or

```text
REQUIRES_REFINEMENT
```

Use `READY_FOR_WORKFLOW` only when:

- the business goal and technical goal are explicit
- scope and non-goals are clear
- affected services, APIs, storage and deployment impact are identified or explicitly not affected
- microservice service boundary, contract impact, data ownership impact, test impact, risk level and stop conditions are explicit when service-split work is requested
- acceptance criteria are testable
- required skills and role or subagent ownership are known
- slice dependencies are acyclic
- parallelization groups have disjoint write scopes and stable contracts
- quality commands are verified from `QUALITY.md` or repository build files
- unresolved uncertainty is either absent or documented as a blocker-free assumption accepted by the user

Use `PROCEED_WITH_ACCEPTED_ASSUMPTIONS` only when confidence is 70-89%, every
assumption is documented, and no assumption affects architecture boundaries,
testability, data ownership, service boundaries, APIs, contracts, runtime
behavior or scope.

Use `REQUIRES_REFINEMENT` when any requirement, ownership, API, quality command, architecture impact, evidence semantic, dependency edge or acceptance criterion would require guessing.

Use the confidence rule:

- Confidence >= 90%: `READY_FOR_WORKFLOW` when no blocking questions remain.
- Confidence 70-89%: `PROCEED_WITH_ACCEPTED_ASSUMPTIONS` only when every assumption is non-blocking and documented.
- Confidence < 70%: `REQUIRES_REFINEMENT`.

Draft slices may be included with `REQUIRES_REFINEMENT` only when they are clearly labeled provisional and used to explain dependency or ownership questions. Do not present provisional slices as executable workflow slices.

Treat a user-accepted blocker-free assumption as ready only when the gate report records the assumption, the user acceptance source and the affected slices or decisions.

## STOP Rules

Stop with `REQUIRES_REFINEMENT` when:

- business goal is missing;
- non-goals are missing;
- affected services, APIs, storage, data ownership or deployment impact are unclear;
- microservice service boundary, contract impact, test impact, risk level or forbidden changes are unclear;
- acceptance criteria are missing or not testable;
- blocking questions remain open;
- API contracts or message semantics are unclear;
- rollback strategy is missing when the change affects deployable behavior or persisted state;
- required skills, roles or callable subagents cannot be verified;
- slice dependencies are cyclic or file ownership overlaps without handoff rules;
- quality commands cannot be verified from `QUALITY.md` or build files;
- continuing would require guessing architecture, runtime facts, evidence semantics, data ownership or implementation details.

## Expected Output

Produce a concise gate report containing:

- normalized requirement summary
- Three Amigos findings from all five mandatory roles
- Senior Requirement Engineer findings
- Senior System Architect findings
- Senior Java Backend Developer findings
- Senior React Frontend Developer findings
- Senior Tester findings
- architecture and evidence-integrity validation
- quality and verification validation
- dependency graph or dependency summary
- required skills and subagents or role reviews
- draft slice boundaries and parallelization groups, if ready
- open questions and blockers
- confidence level and accepted assumptions
- final decision
