# Governance Conflict Review

## Review Result

The reconstructed governance separates three process strands:

- `skills-agents`
- `workflow create`
- `workflow execute`

No strand may borrow publication authority from another strand.

## Resolved Conflicts

| Conflict | Resolution |
|---|---|
| `skills update` could be confused with `workflow create` | `skills update` activates only `skills-agents` and does not create executable workflow slices. |
| `workflow create` could be treated as implementation authorization | `workflow create` is requirement, architecture, planning and documentation only. |
| `workflow execute` could be blocked by old no-commit language | `workflow execute` now requires slice checkpoint commits and pushes after successful slice quality gates. |
| Slice checkpoint push could be confused with `push auto` | Slice checkpoint push belongs to `workflow execute`; `push auto` belongs only to `skills-agents`. |
| `push auto` could publish implementation files | `push auto` is blocked for product implementation, services, contracts, Docker/runtime, build logic, frontend and analytics behavior. |

## Stop Conditions

Stop if:

- a process strand cannot be identified,
- a slice checkpoint push would include files outside the current slice,
- `push auto` is requested outside `skills-agents`,
- product implementation files would need to change,
- checked workflow.md and checked or updated arc42 are unavailable before `workflow execute`.
