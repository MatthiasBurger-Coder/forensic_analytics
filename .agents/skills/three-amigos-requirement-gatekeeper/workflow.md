# Workflow

## Phase 1 - Requirement Intake

Input:

- user requirement
- related EPIC or product goal when available
- architecture constraints from `AGENTS.md`, arc42 and ADRs
- quality constraints from `QUALITY.md`

Actions:

1. Normalize the request with `templates/requirement-template.md`.
2. Separate confirmed requirements from assumptions.
3. Mark missing fields as open questions, not inferred facts.
4. Identify the EPIC source by the precedence defined in `SKILL.md`.
5. If an active workflow exists, classify the request as extending, replacing, conflicting with or unrelated to that workflow.

Output:

- normalized requirement document
- assumptions and missing information
- requirement classification
- active workflow relationship, when relevant

## Phase 2 - Requirement Clarification Loop

Record:

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
- Decision: `READY_FOR_WORKFLOW`, `PROCEED_WITH_ACCEPTED_ASSUMPTIONS` or
  `REQUIRES_REFINEMENT`

If blocking questions remain open:

1. Do not authorize a final checked `docs/workflow/workflow.md`.
2. Do not release the workflow for `workflow execute`.
3. Ask focused clarification questions.
4. Return `REQUIRES_REFINEMENT`.

Non-blocking uncertainty may become an accepted assumption only when it does
not affect architecture boundaries, testability, data ownership, service
boundaries, APIs, contracts, runtime behavior or scope.

Confidence decisions:

- Confidence >= 90%: `READY_FOR_WORKFLOW` when no blocking questions remain.
- Confidence 70-89%: `PROCEED_WITH_ACCEPTED_ASSUMPTIONS` only when the
  assumptions are non-blocking and documented.
- Confidence < 70%: `REQUIRES_REFINEMENT`.

## Phase 3 - Three Amigos Review

Participants:

- Senior Requirement Engineer
- Senior System Architect
- Senior Java Backend Developer
- Senior React Frontend Developer
- Senior Tester

Optional additional lenses:

- Requirement Analyst
- Architecture Validator
- Quality Validator

Validate:

- goal, scope, non-goals, acceptance criteria, assumptions and open questions
- architecture boundaries, arc42, service boundaries, plugin-vs-analytics boundary and risks
- backend impact, ports, adapters, domain, JUnit 6 testability, Spring and microservice consequences
- frontend impact, UX flows, React components, state, API adapters and build/test consequences
- testability, regression, quality gates, acceptance criteria and slice acceptance
- evidence semantics
- operational impact
- rollback expectation
- whether implementation still matches the EPIC

Output:

- Senior Requirement Engineer findings
- Senior System Architect findings
- Senior Java Backend Developer findings
- Senior React Frontend Developer findings
- Senior Tester findings
- EPIC drift findings

## Phase 4 - Dependency Analysis

Check:

- service dependencies
- API ownership
- database ownership
- file ownership
- workflow cycles
- orchestration conflicts
- subagent or role handoff dependencies

Generate:

- dependency graph or ordered dependency summary
- deadlock risks
- parallelization blockers

## Phase 5 - Skill And Subagent Validation

Verify:

- required skills exist under `.agents/skills` or `.codex/skills`
- required roles exist under `.agents/roles`
- callable subagents exist under `.codex/agents` when delegated execution is requested
- no selected skill conflicts with `AGENTS.md` or `QUALITY.md`
- no missing role or skill would force an implementation agent to guess ownership

If callable subagents are unavailable, route to matching role files as explicit review checklists and report the limitation.

## Phase 6 - Slice Planning

Generate only a gate-level slice outline:

- slice boundaries
- execution order
- dependencies
- parallelization groups
- rollback points
- quality gates
- owner roles or subagents

When the decision is `REQUIRES_REFINEMENT`, slice outlines are provisional only and must name the missing decision, contract or ownership point that prevents execution.

Do not create or update checked `docs/workflow/workflow.md` here. Route that
work to `workflow-authoring` after a `READY_FOR_WORKFLOW` decision.

## Inspection Depth Rule

For broad cross-cutting requests, inspect enough repository evidence to verify every named or implied owner, contract, source of truth and quality command that affects the readiness decision. At minimum inspect:

- the governing EPIC or report its absence
- active workflow state when present
- root `AGENTS.md`
- root `QUALITY.md`
- directly affected architecture docs or ADRs
- directly named source, schema, fixture, API or build files
- routing, role and skill definitions needed for ownership

Stop with `REQUIRES_REFINEMENT` when the readiness decision depends on artifacts that cannot be inspected or found.

## Phase 7 - Workflow Approval

Decision:

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

Use `PROCEED_WITH_ACCEPTED_ASSUMPTIONS` only when all remaining uncertainty is
documented as accepted, non-blocking assumptions. Use `REQUIRES_REFINEMENT`
when continuing would require guessing requirements, APIs, ownership, quality
commands, architecture decisions, service boundaries, runtime facts or evidence
semantics, or when any blocking question remains open.
