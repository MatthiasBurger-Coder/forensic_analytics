# Manual Review Required

## Summary

No unresolved conflicts remain from the skill audit after the confirmed corrections for Java/Gradle/JUnit baseline, plugin-triggered server-side analysis, server-side JavaParser/Joern ownership and server-side BTM generation.

No unresolved governance conflicts remain from adding the engineering governance skills and roles. Existing flat role files remain in place, while the new reusable governance roles intentionally use role directories with `SKILL.md` files as requested by the governance workflow.

Manual review must keep the three process strands separate:

- `skills update` routes to `skills-agents`.
- `workflow create` routes to requirement clarification, workflow authoring and arc42 validation.
- `workflow execute` routes to slice execution, tests, quality gates and slice checkpoint push.

Manual review is required if `push auto` would include files outside the `skills-agents` governance scope.

## Governance Flowchart V2 Linkage

No unresolved blocking governance conflicts remain after the Slice 14 linkage
audit.

One non-blocking dedicated-artifact gap remains documented:

- No dedicated `.agents/roles/root-architect.md` file exists. Root Architect
  escalation currently uses the documented Root Architect decision path with
  Senior System Architect ownership.
- Flowchart Integrity Audit is resolved by the dedicated
  `.agents/skills/flowchart-integrity-auditor/SKILL.md`, with Senior
  Documentation Engineer synchronization and Senior System Architect
  architecture escalation.

Create these dedicated artifacts only through a future `skills-agents` slice or
an explicit workflow that authorizes new role or skill creation.

## Historical Missing Paths

The following requested inspection paths were absent during the original skill
audit:

- root `README.md`
- root `workflow.md`

No substitute content was invented for these paths. `docs/workflow/` now exists
as the active Governance Flowchart V2 workflow documentation package and is
covered by `docs/skill-audit/governance-flowchart-v2-linkage.md`.
