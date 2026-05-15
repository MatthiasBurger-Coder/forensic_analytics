# Requirement Engineering Lifecycle

## Purpose

The requirement-engineering skill makes requirement drift visible before architecture, implementation or workplans diverge from the EPIC.

## EPIC Lifecycle

The Requirement Engineer must:

- identify the current EPIC source before changing requirements
- verify whether implementation assumptions still match the EPIC
- classify new requirements before they are implemented
- keep assumptions and constraints explicit
- trace requirements into arc42, ADRs, workplans and quality checks
- document unresolved conflicts instead of silently choosing one artifact

## Requirement Classification

Classify requirements as:

- functional requirements
- non-functional requirements
- architecture constraints
- resilience requirements
- scalability requirements
- UX requirements
- observability requirements
- security and data-protection requirements
- quality-gate requirements
- assumptions
- open questions

## Continuous Drift Detection

For every meaningful architecture, runtime, adapter, UI, persistence, orchestration or deployment change, ask:

- Does the implementation still match the EPIC?
- Did architecture change?
- Did runtime behavior change?
- Did responsibilities move?
- Did service boundaries change?
- Did plugin versus server ownership change?
- Did new runtime assumptions appear?
- Did new orchestration assumptions appear?
- Did new persistence assumptions appear?
- Did new deployment assumptions appear?
- Did new UI assumptions appear?
- Did new resilience assumptions appear?
- Did scalability assumptions change?
- Did observability requirements change?

## Traceability

Each requirement-impacting change must be traceable to at least one of:

- requested task
- EPIC requirement
- architecture decision
- verified implementation behavior
- quality-gate rule
- documented project decision

If no trace can be established, stop and report.

## Drift Response

If drift is detected:

1. Update or propose an update to the EPIC.
2. Update or propose an update to arc42.
3. Review ADR references.
4. Regenerate or update the workplan according to the workplan lifecycle.
5. Review affected skills and roles.
6. Document unresolved conflicts.

Do not implement hidden compatibility behavior to conceal drift.
