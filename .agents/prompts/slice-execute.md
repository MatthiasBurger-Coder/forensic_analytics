# Slice Execute Prompt

Use for executing one workflow slice at a time.

## Required Flow

1. Read the slice scope, prerequisites, dependencies and allowed write scope.
2. Verify required files, symbols, tasks, contracts and commands before editing.
3. Route to required subagents or role reviews.
4. Apply the smallest verified change.
5. Run targeted checks first.
6. Run required quality gates from `QUALITY.md` or the workflow.
7. Inspect `git diff` and `git diff --check`.
8. Record result, blockers and handoff state.

## Stop Conditions

Stop when:

- write scope is unclear;
- required role review is missing;
- exact repository artifact cannot be verified;
- tests or required gates fail;
- continuing would require guessing.
