# Push Auto Guard

`push auto` is restricted to the `skills-agents` process strand.

It must not act as a global publication command for backend, frontend,
Docker/runtime or analytics implementation work.

## Publication Modes

The repository separates three publication modes:

| Mode | Strand | Action | PR / merge / cleanup |
|---|---|---|---|
| Slice checkpoint push | `workflow execute` | Commit the completed slice and push the current workflow branch to `origin` after the slice quality gate passes | No PR, no merge, no branch cleanup |
| `push` | Explicit user publication request | Normal branch push and PR process | PR may be created, no automatic merge |
| `push auto` | `skills-agents` only | Guarded PR lifecycle for approved skill/agent governance changes | PR merge and cleanup only after guard checks pass |

Slice checkpoint push is not `push auto`.

Checkpoint push may run during `workflow execute` only when:

- the slice is complete;
- slice quality gates passed;
- the diff contains only files from the current slice;
- the commit happens on the workflow branch;
- the push target is only `origin/<workflow-branch>`.

## Required Inputs

Before `push auto` can proceed, the git-commit-preparation workflow must have
verified:

1. The active branch is not `main`, `master`, `develop` or another shared
   branch.
2. `git status --short --branch` is clear and understandable.
3. The full diff is inspected.
4. Every changed file is classified as part of `skills-agents`.
5. Skill integrity, registry, organigramm and documentation checks passed.
6. Required verification from `QUALITY.md` passed or is documented as not
   applicable for documentation-only governance changes.

## Allowed File Families

`push auto` may consider only changes in these file families, and only when the
task is explicitly a `skills-agents` task:

```text
AGENTS.md
.agents/**
.codex/agents/**
.codex/skills/**
.codex/subagents/**
.codex/workflow/**
docs/agents/**
docs/process/**
docs/governance/**
docs/skill-audit/**
docs/arc42/**
docs/adr/**
```

`docs/arc42/**` and `docs/adr/**` are allowed only when they describe
skill/agent/process governance consequences. They must not claim product
runtime behavior that was not implemented.

## Blocked File Families

`push auto` must stop when the diff includes product implementation or runtime
files such as:

```text
src/**
*/src/**
forensic-ui/**
docker/**
docker-compose*.yml
build.gradle*
settings.gradle*
gradle/**
proto/**
contracts/**
```

Build, Gradle, contract or product-source changes require normal commit or
workflow execution governance. They are outside the `skills-agents` `push auto`
guard.

## Stop Message

When a blocked file appears, report:

```text
STOP: push auto is limited to the skills-agents strand.
Reason: <blocked file path> is outside the allowed skills-agents file set.
No push, merge or cleanup was performed.
```

## Required Output

Before publication, `push auto` must list the exact files it intends to stage,
commit, push and merge. The list must be traceable to the skills/agents task and
the registry, organigramm and documentation checks.
