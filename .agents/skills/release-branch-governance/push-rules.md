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
- Treat slice checkpoint push as a `workflow execute` branch checkpoint only:
  commit the completed slice, push only to `origin/<workflow-branch>`, do not
  create or merge a PR, do not clean up branches and do not force-push.
- Treat `push auto` as restricted to the `skills-agents` strand. Stop when the
  diff includes backend, frontend, Docker/runtime or analytics implementation
  files.

## STOP Rules

Stop when:

- push target is unknown;
- required gate evidence is missing;
- secret or sensitive-data risk is unresolved;
- branch contains unrelated scope;
- workflow does not allow push;
- slice checkpoint push includes files from another slice or targets anything
  other than `origin/<workflow-branch>`;
- `push auto` is requested for changes outside the `skills-agents` strand.
