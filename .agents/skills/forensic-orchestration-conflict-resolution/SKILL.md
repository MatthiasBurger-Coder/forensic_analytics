---
name: forensic-orchestration-conflict-resolution
description: Use when Forensic Analytics work may overlap with local changes, user edits, generated files, or parallel agent ownership.
---

# Conflict Resolution

## Purpose

Resolve or report overlapping changes without losing user work.

## Practices

- Inspect git status before editing.
- Treat unexpected local changes as user-owned unless proven otherwise.
- Do not revert unrelated changes.
- Assign disjoint file ownership for parallel work.
- Stop and ask when a user-owned change makes the task impossible to complete safely.

## Verification

- Review diffs for touched files.
- Confirm no broad line-ending-only changes were introduced on Windows-hosted WSL worktrees.
