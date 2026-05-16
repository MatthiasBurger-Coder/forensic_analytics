# Quality Gate Plan

`QUALITY.md` is the authoritative quality contract.

## Required Commands

Minimum command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Documentation and workflow artifact checks:

```bash
git status --short --branch
git diff --name-status -- docs/workflow
git diff -- docs/workflow
git diff --check
```

## Scope-Based Minimums

| Workflow scope | Branch example | Minimum verification |
| --- | --- | --- |
| Feature/default | `feature/workflow-grpc-ingestion-20260516` | Tests, build and quality according to `QUALITY.md`. |
| Fix | `fix/workflow-branch-conflict-check-20260516` | Regression test or documented defect reproduction plus `QUALITY.md` gate. |
| Docs | `docs/workflow-git-branch-strategy-20260516` | Documentation diff review, `git diff --check`, and documented reason if Gradle gate is not run. |
| Architecture | `architecture/workflow-microservice-boundaries-20260516` | Architecture consistency review with `AGENTS.md`, skills and ADRs plus `QUALITY.md` gate. |

## Commit And Push Readiness

Before commit or push readiness is claimed:

1. Run the narrowest meaningful checks for the slice.
2. Run the full local quality gate from `QUALITY.md`, or document why it could
   not be executed and treat the result as not fully clean.
3. Inspect `git diff` and `git diff --check`.
4. Verify no broad line-ending-only changes are present.
5. Verify the branch push target explicitly.

`validatePlugins` is required only when Gradle plugin metadata, task
inputs/outputs or plugin implementation classes change.
