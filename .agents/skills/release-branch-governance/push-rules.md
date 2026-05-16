# Push Rules

## Push Readiness

Push requires:

- committed changes;
- target branch known;
- required quality gates clean;
- no unresolved required failures;
- PR or release expectations known;
- no secrets or sensitive data in diff.

## Rules

- Do not push when required gates failed.
- Do not push when branch or remote target is unclear.
- Do not push unrelated local changes.
- Create PRs only when workflow or user request allows it.

## STOP Rules

Stop when:

- push target is unknown;
- required gate evidence is missing;
- secret or sensitive-data risk is unresolved;
- branch contains unrelated scope;
- workflow does not allow push.
