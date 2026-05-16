# Workflow

## Phase 1 - Load Quality Authority

Read `QUALITY.md` and identify:

- minimum quality command;
- full local quality gate;
- domain-specific quality expectations;
- optional diagnostic commands;
- special gates for plugins, build logic, Docker or CI.

## Phase 2 - Classify Slice

Classify changed files:

- documentation only
- skill or role governance
- source code
- tests
- build logic
- dependency verification
- CI/CD
- security-sensitive configuration

## Phase 3 - Select Checks

Choose:

- narrow targeted checks first;
- `git diff --check` for documentation and Markdown slices;
- minimum Gradle gate before commit-ready claims;
- full local gate before commit or push when required by workflow or broad changes;
- specialized checks when affected files require them.

## Phase 4 - Execute Or Record Exception

For each required check, record:

- exact command;
- result;
- failure summary;
- owner;
- whether failure is related to current changes;
- next action.

Required checks that cannot run need an exception report and remain blocking for commit unless governance explicitly accepts a non-commit state.

## Phase 5 - Decide

Return:

- `PASS` when required checks are clean;
- `BLOCKED` when required checks fail or cannot be verified;
- `NOT_COMMIT_READY` when checks are deferred by documented execution scope;
- `NON_BLOCKING_NOTE` only for optional, unavailable or not-applicable checks.
