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
9. When the slice quality gate passed, prepare the `CP_RECORD` with workflow
   version, slice ID, slice title, responsible agent, changed files,
   quality-gate commands, quality-gate result, rollback reference,
   `arc42Updated` and `adrUpdated`.
10. Stage only current-slice files.
11. Run `git diff --cached --check`.
12. Create the slice-scoped checkpoint commit.
13. Push the current workflow branch to `origin`.
14. Record commit SHA, push result, blockers and handoff state.

The slice checkpoint push is not `push auto`. It must not create or merge a PR, clean up branches, force-push or push to `main`.
Each checkpoint commit must contain exactly one slice.

## Stop Conditions

Stop when:

- write scope is unclear;
- active workflow branch is missing, inactive, or has no local ref;
- required role review is missing;
- exact repository artifact cannot be verified;
- tests or required gates fail;
- the staged diff contains files from another slice;
- the staged diff or commit message would combine multiple slices;
- the active workflow version cannot be recorded;
- the checkpoint push target is not `origin/<workflow-branch>`;
- continuing would require guessing.
