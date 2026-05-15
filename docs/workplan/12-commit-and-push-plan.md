# Commit And Push Plan

The final implementation must use a non-`main` work branch. The branch should use the repository default prefix:

```text
codex/
```

## Preconditions

Before staging:

- run commands from WSL on Windows hosts
- verify the worktree is not detached
- verify the current branch is not `main`
- verify no broad line-ending-only changes exist
- inspect every changed file
- classify changes outside `docs/workplan` before continuing

If files outside `docs/workplan` changed, document why they changed and whether they belong to the task. Implementation files are in scope only when they directly execute one of the verified slices in [02-slices.md](02-slices.md). Unrelated files must not be staged.

## Final Git Flow

For documentation-only workplan updates, run:

```bash
git status
git diff
git add docs/workplan
git status
git diff --cached
git commit -m "docs: add workspace grpc integration workplan"
git push
```

If the branch has no upstream, push with:

```bash
git push -u origin HEAD
```

## Review Before Commit

Before `git commit`, verify:

- staged files are only under `docs/workplan`
- deleted old workplan files are replaced by the new workplan
- no generated build output is staged
- no credentials or local paths with secrets are staged
- documentation remains in English
- the workplan states that parsers are later work
- WildFly is documented as a hardening test only

## Commit Message

Use:

```text
docs: add workspace grpc integration workplan
```

For implementation-slice execution, stage only the exact in-scope source, test and documentation files touched by the executed slice. The commit message must be prepared from the staged diff and the verification evidence, not reused from the documentation-only workplan commit.

The documentation-only commit covers documentation only. It must not include implementation code, generated build output or quality reports.

## Push Rules

- Do not push directly to `main`.
- Do not force-push.
- Push the current work branch.
- If remote push fails, report the branch name, command and failure summary.
- Pull request creation is a separate publication step unless the active workflow explicitly requests it.
