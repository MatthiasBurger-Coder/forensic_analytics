# Slice 06 CLI Entry Point Result

Date: 2026-05-10

## Scope Executed

- Added `forensic-analytics-cli`.
- Added an application entry point for `forensic-analytics analyze --repo --profile --output --joern-mode`.
- Added strict CLI parsing for local repository location, analysis profile, output directory, and Joern mode.
- Added CLI execution through the application `RunRepositoryAnalysisUseCase`.
- Added deterministic analysis run ID creation from the explicit CLI inputs.
- Added text summary output to stdout and `analysis-summary.txt` in the configured output directory.
- Added tests for parsing, help, usage errors, use-case invocation, result output, and missing standalone service wiring.

## Runtime Wiring

The CLI is an inbound adapter. It does not implement analysis itself.

Standalone `main` loads a `RunRepositoryAnalysisUseCase` through `ServiceLoader`. No provider is registered in this slice, because concrete source scanning, semantic analysis, and rule generation wiring remains outside the CLI boundary.

## Scope Deliberately Not Executed

- No JavaParser scanner was added.
- No Joern Docker runtime wiring was added.
- No Byteman rule generation adapter was added.
- No remote clone, branch, tag, or commit checkout behavior was added.
- No gRPC, persistence, bootstrap server, Gradle, Maven, or plugin adapter behavior was changed.
- No code was changed in `forensics_tracing`.

## Workplan Alignment

This slice follows Slice 06 from `MIGRATION_WORKPLAN.md`:

- It adds the `analyze --repo --profile --output --joern-mode` CLI surface.
- It keeps the CLI as a thin inbound adapter.
- It delegates analysis to the application use case and only formats the returned result.

## Verification

The first targeted CLI test run exposed an ambiguous constructor overload because `RunRepositoryAnalysisUseCase` is also a functional interface. The CLI factory wiring was separated from the public use-case constructor and the targeted test was rerun successfully.

The first full quality gate run exposed package branch coverage below the threshold for `de.burger.forensics.analytics.cli`. Additional parser and command tests were added and the full gate was rerun successfully.

Executed commands:

```text
.\gradlew.bat :forensic-analytics-cli:test --dependency-verification strict --console=plain --stacktrace
.\gradlew.bat :forensic-analytics-cli:run --args="--help" --dependency-verification strict --console=plain --stacktrace
.\gradlew.bat test --dependency-verification strict --console=plain --stacktrace
.\gradlew.bat clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Result:

- Targeted CLI tests passed.
- CLI help entry point executed successfully.
- The documented minimum quality command passed.
- The full local quality gate passed after adding coverage for the missed CLI branches.
- The JVM emitted a deprecation warning from `grpc-netty-shaded` using `sun.misc.Unsafe`; it did not fail the build.
