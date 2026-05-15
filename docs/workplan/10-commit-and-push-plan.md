# Commit And Push Plan

## Scope

This commit includes the executed initial adapter-scoped observability slice and the synchronized workplan, ADR, and arc42 documentation.

The implemented scope covers the new `forensic-analytics-observability` module, adapter integrations, architecture guardrails, and tests. Follow-up implementation should be handled as a new task with a fresh read-only verification phase.

## Suggested Implementation Commit Slices

If follow-up work is executed later, prefer small commits aligned to slice boundaries:

1. `docs: plan logging system integration`
2. `build: add observability logging boundary`
3. `feat: add correlation context foundation`
4. `feat: add sanitized operation logger`
5. `feat: log REST adapter operations`
6. `feat: log gRPC ingestion operations`
7. `feat: log CLI and server lifecycle`
8. `test: enforce logging architecture boundaries`
9. `docs: document observability logging boundary`

Commit names are suggestions only. Final commit messages must be prepared from actual diffs.

## Pre-Commit Checks

Before staging implementation changes:

```bash
git status --short
```

On this Windows-hosted worktree, verify that status is not polluted by line-ending-only changes.

Run targeted tests first, then:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Before final integration commit, run:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Push Plan

No push is part of this workplan unless explicitly requested.

If a later push is requested, use the repository git commit preparation workflow, verify branch state, inspect diffs, run required quality gates, and create a pull request only after successful local validation.
