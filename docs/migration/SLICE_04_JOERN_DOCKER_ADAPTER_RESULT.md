# Slice 04 Joern Docker Adapter Result

Date: 2026-05-10

## Scope Executed

- Added `forensic-analytics-adapter-joern-docker`.
- Added a Joern Docker command builder for version, parse, callgraph export, controlflow export, and data-flow slice operations.
- Added a Joern Docker command runner abstraction and process runner.
- Added a semantic analysis adapter implementing the application `SemanticAnalysisPort`.
- Added filesystem artifact collection for Joern output artifacts.
- Added an empty `slices.json` fallback when Joern does not emit a slices artifact.
- Added unit tests for command construction, adapter orchestration, artifact collection, process execution, timeout handling, and model validation.

## Image Pinning Decision Handling

`MIGRATION_WORKPLAN.md` states that the Joern Docker image/version requires a decision.

This slice does not choose a Joern image. Instead:

- there is no hardcoded default image,
- `JoernDockerImage` requires an explicit image reference,
- the image reference must be pinned by a 64-character `@sha256:` digest,
- tests use synthetic digest-pinned fixture image strings only.

This keeps the adapter usable once a real Joern image digest is selected without silently choosing one in code.

## Scope Deliberately Not Executed

- No live Docker or Joern integration test was added to the default gate.
- No Git, repository source acquisition, JavaParser, H2, Gradle, Maven, CLI, gRPC, or Byteman adapter was implemented.
- No code was changed in `forensics_tracing`.
- No real Joern image digest was selected.
- No parsed Joern graph model was introduced; this slice returns collected artifact references only.

## Workplan Alignment

This slice follows Slice 04 from `MIGRATION_WORKPLAN.md`:

- It adds the Joern Docker adapter module.
- It adds a command builder and command runner.
- It adds adapter tests without requiring Docker in the default quality gate.
- It preserves the explicit open decision for the real pinned image.

## Verification

The first targeted adapter test run exposed two implementation/test issues:

- standard artifact fixture byte lengths were asserted incorrectly,
- Windows could keep timeout-test temp files locked during cleanup.

Both issues were fixed and the targeted command was rerun successfully:

```text
.\gradlew.bat :forensic-analytics-adapter-joern-docker:test --dependency-verification strict --console=plain --stacktrace
```

The minimum test gate passed:

```text
.\gradlew.bat test --dependency-verification strict --console=plain --stacktrace
```

The full quality gate passed:

```text
.\gradlew.bat clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```
