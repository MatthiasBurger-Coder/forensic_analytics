# Failure Handling

## Required Failure Report

Every failed gate report must include:

- command executed
- failing task or test
- failure summary
- related changed files
- whether the failure appears related to current changes
- owner
- next action
- rerun command

## Failure Classification

- `CURRENT_CHANGE_FAILURE`: caused by the current slice.
- `PRE_EXISTING_FAILURE`: visible before or outside current slice.
- `ENVIRONMENT_FAILURE`: missing tool, lock, network or local setup issue.
- `UNKNOWN_FAILURE`: cause cannot be determined without more evidence.

## Rules

- Fix current-change failures inside the slice when safe.
- Do not hide pre-existing failures; document them and keep commit readiness blocked unless repository governance explicitly accepts the state.
- Do not downgrade required failures to warnings.
- Rerun the affected gate after fixes.
- Stop when fixing a failure would require unrelated refactoring or guessing.
