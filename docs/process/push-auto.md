# Push Auto Governance

`push auto` is restricted to the `skills-agents` process strand.

It may create, verify and merge a pull request only after the skills-agents guard passes. It must never publish backend, frontend, Docker/runtime, contracts, persistence, analysis engine, Joern, JavaParser, BTM generator or analytics implementation changes.

## Publication Separation

Slice checkpoint push is not `push auto`.

`push` is not `push auto`.

`skills update` is not `push auto`.

## Allowed Review Scope

`push auto` may consider only these areas:

- `AGENTS.md`
- `.agents/**`
- `.codex/agents/**`
- `.codex/skills/**`
- `.codex/subagents/**`
- `.codex/workflow/**`
- `docs/agents/**`
- `docs/process/**`
- `docs/governance/**`
- `docs/skill-audit/**`
- `docs/arc42/**`
- `docs/adr/**`

## Blocked Review Scope

`push auto` is blocked when any of these areas changed:

- `src/**`
- `*/src/**`
- `forensic-ui/**`
- `docker/**`
- `docker-compose*.yml`
- `build.gradle*`
- `settings.gradle*`
- `gradle/**`
- `proto/**`
- `contracts/**`
- `services/**`

## Guard Checks

Before `push auto`, confirm:

1. The active branch is not `main`, `master`, `develop` or another shared branch.
2. `AGENTS.md` and `QUALITY.md` were read.
3. The change belongs to `skills-agents`.
4. No blocked review-scope files changed.
5. `git diff --check` passes.
6. Required documentation references are consistent.
7. The PR target is `main`.
8. No push to `main`, force-push or GitHub auto-merge is used.

The PR may be merged only after mergeability and required checks are verified.
