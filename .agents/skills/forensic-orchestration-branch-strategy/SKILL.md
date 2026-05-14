---
name: forensic-orchestration-branch-strategy
description: Use for Forensic Analytics branch isolation, commit preparation boundaries, staged-file review, and line-ending checks.
---

# Branch Strategy

## Purpose

Keep slices isolated and reviewable.

## Practices

- Use focused `codex/` branches for agent-created implementation work unless the user requests another branch strategy.
- Keep commits scoped to the requested task.
- Do not commit or push unless the user explicitly asks for that step.
- Verify that broad line-ending changes are absent before staging or committing.
- Prefer non-interactive git commands.

## Verification

- Run `git status --short` before and after changes.
- Review staged files before commit preparation.
