# Three Amigos Requirement Gate

The Three Amigos Requirement Gate validates a request before workflow authoring or execution.

For this repository, the gate uses five mandatory role reviews:

- Senior Requirement Engineer
- Senior System Architect
- Senior Java Backend Developer
- Senior React Frontend Developer
- Senior Tester

## Review Responsibilities

Senior Requirement Engineer verifies goal, scope, non-goals, acceptance criteria, assumptions, open questions and requirement confidence.

Senior System Architect verifies architecture boundaries, arc42 impact, service boundaries, plugin-vs-analytics boundary, governance risks and whether planned behavior is not described as implemented behavior.

Senior Java Backend Developer verifies backend impact, ports, adapters, domain isolation, JUnit 6 testability, Spring wiring impact and microservice consequences.

Senior React Frontend Developer verifies frontend impact, UX flows, React components, state ownership, API adapter impact and build or test consequences.

Senior Tester verifies testability, regression approach, quality gates, acceptance criteria and slice acceptance.

## Decisions

The gate returns exactly one decision:

- `READY_FOR_WORKFLOW`
- `PROCEED_WITH_ACCEPTED_ASSUMPTIONS`
- `REQUIRES_REFINEMENT`

`READY_FOR_WORKFLOW` requires confidence of at least 90 percent and no blocking questions.

`PROCEED_WITH_ACCEPTED_ASSUMPTIONS` is allowed only for confidence from 70 to 89 percent when every assumption is documented, non-blocking and accepted.

`REQUIRES_REFINEMENT` is required when confidence is below 70 percent or any blocking question remains.

## Blocking Questions

Questions are blocking when they affect:

- architecture boundaries
- testability
- data ownership
- service boundaries
- APIs
- contracts
- runtime behavior
- scope

When blocking questions remain, the gate must ask focused clarification questions and must not release `workflow execute`.
