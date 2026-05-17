# Manual Review Required

## Summary

No unresolved conflicts remain from the skill audit after the confirmed corrections for Java/Gradle/JUnit baseline, plugin-triggered server-side analysis, server-side JavaParser/Joern ownership and server-side BTM generation.

No unresolved governance conflicts remain from adding the engineering governance skills and roles. Existing flat role files remain in place, while the new reusable governance roles intentionally use role directories with `SKILL.md` files as requested by the governance workflow.

Manual review must keep the three process strands separate:

- `skills update` routes to `skills-agents`.
- `workflow create` routes to requirement clarification, workflow authoring and arc42 validation.
- `workflow execute` routes to slice execution, tests, quality gates and slice checkpoint push.

Manual review is required if `push auto` would include files outside the `skills-agents` governance scope.

## Missing Paths

The following requested inspection paths were absent:

- root `README.md`
- root `workflow.md`
- `docs/workflow/`

No substitute content was invented for these paths.
