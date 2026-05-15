# Documentation And Examples

## Documentation Flow

Governance documentation should follow this order:

1. Identify the requirement or drift.
2. Verify the current source of truth.
3. Update the EPIC when product or requirement intent changed.
4. Update arc42 when architecture, runtime, deployment or crosscutting behavior changed.
5. Review ADR references when a decision exists or is needed.
6. Regenerate or update `docs/workplan` according to workplan lifecycle rules.
7. Align skills and roles only when the reference is obvious and verified.
8. Record verification status.

## Workplan Lifecycle Documentation

Every workplan must state whether it is:

- planned
- in progress
- implemented
- blocked
- superseded

If superseded, the new workplan must regenerate `docs/workplan` instead of keeping old active slices.

## Requirement Lifecycle Documentation

Requirement documentation must include:

- requirement statement
- source
- classification
- affected architecture areas
- affected quality or resilience expectations
- assumptions
- unresolved conflicts
- traceability to workplan slices

## EPIC Synchronization

When implementation no longer matches the EPIC, do not silently update downstream artifacts only.

The Requirement Engineer must either update the EPIC or document why the EPIC update is blocked. Downstream arc42, ADR references, workplans, skills and roles must not hide that conflict.

## arc42 Synchronization

arc42 changes must be based on verified architecture impact.

Examples:

- service boundary changes belong in system scope, building block view and possibly runtime view
- communication changes belong in runtime view and crosscutting concepts
- deployment topology changes belong in deployment view
- resilience decisions belong in crosscutting concepts and quality requirements
- architecture decision references belong in the architecture decisions section

## Example: Plugin Responsibility Moved To Server

If a capability previously owned by a plugin becomes a server responsibility:

- update EPIC if responsibility ownership changed at product level
- update arc42 building block view and runtime view
- review ADRs about plugin producer boundaries
- regenerate or update the workplan to sequence server-side implementation
- update affected skills or roles only where ownership guidance is explicit
- document migration risks and stop conditions

## Example: UI Communication Strategy Changed

If UI communication changes from REST to another protocol:

- verify current REST assumptions in workplan and arc42
- update EPIC if user-facing interaction behavior changed
- update arc42 runtime view and deployment view
- review resilience expectations for streaming, retry, timeout and degraded states
- update quality-gate expectations for frontend and backend verification
- document why the previous communication strategy is superseded

## Example: New Resilience Requirement Introduced

If a new resilience requirement appears:

- classify it as a non-functional requirement
- update EPIC or record the required EPIC change
- update arc42 crosscutting concepts and quality requirements
- update affected runtime or deployment views
- update relevant workplan slices with verification steps
- update related skills only when the rule is reusable
- document unresolved operational assumptions
