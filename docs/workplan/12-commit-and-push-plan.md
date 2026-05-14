# 12 - Commit and Push Plan

## Planned Git Flow

```bash
git status
git diff
git add .agents .codex docs/skill-audit docs/workplan
git status
git commit -m "docs: consolidate skills and add workspace grpc workplan"
git push
```

## Branch Rule

Do not push directly to `main`. If work starts on `main`, create a non-main work branch first.

Current work should use a `codex/` branch unless the user requests another branch prefix.

## Staging Scope

Expected staged paths:

- `.agents`
- `.codex`
- `docs/skill-audit`
- `docs/workplan`

If files outside these areas change, inspect them, explain why they are in scope and document the reason in the commit body.

## Commit Message

Use:

```text
docs: consolidate skills and add workspace grpc workplan
```

The final commit body must include:

- why the skill audit and workplan were needed,
- what changed,
- verification commands and results,
- documentation-only impact,
- limitations or skipped checks.

## Push Failure Handling

If no remote is configured or push fails:

1. Record the exact failure.
2. Keep the local commit if it was created successfully.
3. Update this file with the blocker if possible.
4. Report the branch and commit hash.
