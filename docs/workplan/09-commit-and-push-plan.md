# Commit And Push Plan

Use this workflow only after implementation and verification are complete.

## Branch

If the current branch is `main`, create a task branch using the repository branch prefix:

```text
codex/resilient-react-ui-mvp
```

## Pre-Commit Review

Inspect:

```bash
git status --short
git diff --stat
git diff
git diff --check
```

Verify:

- old gRPC/WildFly workplan files are gone;
- new `docs/workplan` files are English and current;
- frontend generated files are not staged;
- `node_modules`, `dist`, coverage and logs are ignored;
- backend and frontend changes match the requested UI MVP;
- no WebSocket, SSE, gRPC-Web or direct browser gRPC code was added;
- no secrets or raw sensitive diagnostics are committed.

## Commit Message

Use:

```text
Why:
What:
How:
Verification:
Impact:
Limitations:
```

The `Verification` section must list the exact commands that were run and their results.

## Push

Push only when requested or when repository workflow explicitly allows it.

If pushing:

- ensure the branch is not `main`;
- push the current branch;
- create a pull request if the current workflow requires it;
- include known limitations and skipped checks in the PR body.
