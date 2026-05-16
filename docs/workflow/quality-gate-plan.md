# Quality Gate Plan

`QUALITY.md` is the authoritative quality contract.

## Required Commands From QUALITY.md

Minimum command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Diagnostic command only:

```bash
./gradlew clean check --dependency-verification strict --console=plain --stacktrace
```

`clean check` is not the full gate unless `checkPackageCoverage` is explicitly
wired into `check`.

## Workflow Creation Checks

For this `workflow create` step, the changed files are workflow documents only.
The narrow checks are:

```bash
git status --short --branch
git diff --name-status -- docs/workflow
git diff -- docs/workflow
git diff --check
```

Gradle is not required to validate docs-only workflow creation, but the full
quality gate remains required before later commit/push readiness unless an
explicit documented exception is accepted.

## Execution Slice Checks

Future execution slices must run at least:

```bash
git diff --check
```

and any targeted checks implied by the changed files. If a slice changes
`QUALITY.md`, Gradle build logic, plugin metadata, task inputs/outputs or
production code, it must use the applicable `QUALITY.md` gate before claiming
readiness.

`validatePlugins` is required only when Gradle plugin metadata, task
inputs/outputs or plugin implementation classes are changed.

Do not document mutation testing or CI workflow checks unless the repository
contains verified tooling for them.

## Governance Manual Checks

When no automated checker is verified, execution must record manual review for:

- branch-first workflow creation;
- Three Amigos readiness before workflow authoring;
- role authority conflicts;
- Senior System Architect final architecture authority;
- executor and orchestrator boundary limits;
- microservice no-shared-code invariants;
- subagent handoff rules;
- traceability from requirement to quality evidence.

## Optional Checks

- Markdown linting: not verified during workflow creation.
- Link checking: not verified during workflow creation.
- SonarCloud: optional and credential-dependent.
- Docker, Docker Swarm and Kubernetes checks: not verified for this
  governance-only workflow creation.
- `validatePlugins`: required only if Gradle plugin metadata, task inputs,
  task outputs or plugin implementation classes change.
