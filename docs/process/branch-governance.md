# Branch Governance

Branch governance keeps workflow and skills-agent changes isolated from shared branches.

Root `AGENTS.md` remains authoritative for exact command handling. This document summarizes the branch and publication boundaries used by the process strands.

## Branch Rules

- Do not modify workflow or skills-agent artifacts on `main`, `master`, `develop` or another shared branch.
- Use a dedicated branch for workflow or governance changes.
- Verify the active branch before staging, committing or pushing.
- Stop when local changes are unclear.
- Do not force-push.
- Do not push directly to `main`.

## Process Strand Branch Expectations

`skills-agents`:

- may prepare `push auto` only after explicit user request
- must stay inside skills, agents, prompts, routing, process and governance documentation
- must not include product implementation files

`workflow create`:

- creates or sharpens workflow planning artifacts
- does not implement product changes
- ends with checked `docs/workflow/workflow.md` and checked or updated arc42 documentation

`workflow execute`:

- executes approved workflow slices
- runs the slice quality gate
- creates a slice-scoped checkpoint commit after each successful slice
- pushes the current workflow branch to `origin`

## Slice Checkpoint Push

A slice checkpoint push belongs only to `workflow execute`.

It must:

1. run the slice quality gate
2. inspect the slice diff
3. stage only files changed by the current slice
4. run `git diff --cached --check`
5. create a slice-scoped checkpoint commit
6. push the current workflow branch to `origin`
7. record the commit SHA and push result in the execution report

It must not:

- create or merge a PR
- clean up branches
- run `push auto`
- force-push
- push to `main`
