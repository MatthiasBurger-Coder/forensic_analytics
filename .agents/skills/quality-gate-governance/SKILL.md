---
name: quality-gate-governance
description: Use for quality-gate selection, command reporting, dependency verification, coverage, and optional external checks.
---

# Quality Gates

## Purpose

Use the repository quality contract without replacing or weakening it.

## Authoritative Commands

`QUALITY.md` is the source of truth. The default minimum command is:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

The full local gate is:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Practices

- Do not claim a command passed unless it was executed.
- Report exact failing tasks and suspected relation to the current change.
- Skip optional external checks only when credentials or services are unavailable, and report the skip.
- Apply `.agents/skills/resilience-engineering/SKILL.md` for quality-gate timeout, retry, health-check, diagnostics, cleanup and degraded-verification decisions.
