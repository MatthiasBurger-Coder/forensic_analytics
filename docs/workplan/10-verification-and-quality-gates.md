# Verification And Quality Gates

## Verification Strategy

This workplan creates documentation and agent governance artifacts. It must not execute runtime parsers, Joern, graph runtime, replay runtime or LLM runtime.

Use the narrowest meaningful verification after each slice, then the repository quality gate before commit readiness when feasible.

## Execution Results

The governance implementation was verified from WSL with:

```bash
git diff --check
```

Result: passed.

The new governance `SKILL.md` files were checked for required YAML frontmatter.

The documented minimum repository verification was executed:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Result: passed with `BUILD SUCCESSFUL`.

The full local quality gate was executed:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Result: passed with `BUILD SUCCESSFUL`.

The full local quality gate emitted Java runtime warnings from third-party gRPC Netty and Protobuf dependencies about restricted or deprecated `sun.misc.Unsafe` and native-access usage. The Gradle build completed successfully, and no current-change failure was observed.

## Documentation-Only Verification

For documentation-only slices, run:

```bash
git diff --check
```

Also review:

```bash
git status --short --branch
```

## Minimum Repository Verification

`QUALITY.md` defines the minimum command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Run this after creating or aligning skills and roles if execution reaches commit readiness.

## Full Local Quality Gate

`QUALITY.md` defines the full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Run this when feasible before commit readiness, especially if any non-documentation behavior, build logic, quality rules or repository-wide guidance changes affect verification expectations.

## Plugin Validation

No Gradle plugin metadata or plugin implementation class is planned for this governance work.

Do not run or document `validatePlugins` as required unless a later slice actually changes plugin metadata, Gradle task inputs, task outputs or plugin implementation classes.

## Failure Reporting

If a command fails, report:

- command executed
- failure summary
- failing task or test
- whether the failure was caused by the current change
- remaining blocker

Do not claim that a command passed unless it was actually executed.
