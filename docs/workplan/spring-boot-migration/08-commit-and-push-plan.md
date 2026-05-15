# Commit And Push Plan

This file is a suggested commit structure for later implementation. It does not stage, commit, push or create a pull request by itself.

## Suggested Commit Boundaries

```text
docs: add Spring Boot migration workplan
docs: accept Spring Boot server boundary ADR
build: add Spring Boot version aliases
feat: add Spring Boot application module
feat: wire existing application use cases through Boot configuration
feat: start gRPC ingestion from Spring Boot
docs: document Spring Boot profiles and startup
test: add Spring Boot smoke and architecture tests
build: add Spring Boot container packaging
```

## Commit Readiness Checks

Before each commit:

```bash
git status --short
git diff --check
```

Run the narrowest meaningful verification command for the slice.

Before final implementation commit:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Staging Rules

- Stage only files belonging to the completed slice.
- Inspect `git diff --cached` before committing.
- Do not stage generated build output.
- Do not stage local workspace data, runtime traces, secrets or container volumes.
- Do not stage dependency verification metadata before reviewing that it only contains approved artifacts.

## PR Notes

A future PR description should include:

- target Spring Boot version and dependency-minimization decisions
- Spring-free domain/application proof
- gRPC startup behavior
- profile defaults
- quality commands executed
- skipped optional checks and why
- remaining follow-up slices
