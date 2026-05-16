# Slice Execute Prompt

Use for executing one workflow slice at a time.

## Required Flow

1. Read the slice scope, prerequisites, dependencies and allowed write scope.
2. Verify the active workflow branch and local branch ref before editing:

```bash
git branch --show-current
git show-ref --verify --quiet refs/heads/<workflow-branch>
```

3. Verify required files, symbols, tasks, contracts and commands before editing.
4. Route to required subagents or role reviews.
5. Apply the smallest verified change.
6. Run targeted checks first.
7. Run required quality gates from `QUALITY.md` or the workflow.
8. Inspect `git diff` and `git diff --check`.
9. Record result, blockers and handoff state.

## Stop Conditions

Stop when:

- write scope is unclear;
- active workflow branch is missing, inactive, or has no local ref;
- required role review is missing;
- exact repository artifact cannot be verified;
- tests or required gates fail;
- continuing would require guessing.
