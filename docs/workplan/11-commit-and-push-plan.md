# Commit And Push Plan

## Branch Rule

If execution reaches commit readiness while on `main`, create a dedicated branch first.

Use the repository branch prefix:

```text
codex/
```

Execution result:

```text
codex/engineering-governance-system
```

The branch was created from `main` before staging, committing or pushing.

## Pre-Stage Review

Before staging:

```bash
git status --short --branch
git diff -- docs/workplan .agents/skills .agents/roles .agents/orchestrator .codex/agents
git diff --check
```

Review that changed files are limited to the approved governance scope.

## Line Ending Check

Because this repository is accessed from a Windows host through WSL, verify that Git status is not polluted by line-ending-only changes before staging or committing.

If broad unexpected line-ending changes appear, stop and report.

## Commit Message Format

The commit message must contain:

```text
Why:
What:
How:
Verification:
Impact:
Limitations:
```

## Push Rule

Push automatically only when repository rules and explicit user instructions allow it.

If push is requested, follow the repository commit-preparation workflow and create a pull request when required by that workflow.

Execution result: staging, commit and push were not performed. `.agents/skills/forensic-orchestration-branch-strategy/SKILL.md` requires an explicit commit or push instruction, so Slice 16 stops after branch creation and verified working-tree review.

## Final Report

The final report must include:

- files changed
- verification commands executed
- exact command results
- unresolved conflicts
- limitations
- whether commit and push were performed
