# 11 - Quality Gates

## Authoritative Source

`QUALITY.md` is authoritative for verification commands.

## Minimum Verification

The repository minimum command is:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

## Full Local Quality Gate

The full local quality gate is:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Additional Checks

When plugin metadata, Gradle task inputs, task outputs or plugin implementation classes change, also verify whether this task exists and is applicable:

```bash
./gradlew validatePlugins --dependency-verification strict --no-daemon --console=plain --stacktrace
```

For general build-health diagnostics:

```bash
./gradlew clean check --dependency-verification strict --console=plain --stacktrace
```

If a task does not exist, do not invent a substitute. Document the missing task and choose the closest repository-documented verification command.

## Documentation-Only Change Policy

For documentation-only and agent-instruction-only changes, run the minimum quality command when practical. If it is skipped because the change does not affect executable code or because the command is too costly for the current environment, document the reason and do not claim it passed.

## Current Verification Record

Verified during this documentation and agent-configuration update:

- Markdown structure: passed by file-existence and required-section inspection for `docs/workplan/` and `docs/skill-audit/`.
- Internal links: passed for the relative links in `docs/skill-audit/README.md` and `docs/skill-audit/conflicts-resolved.md`.
- Parser exclusion check: passed. The workplan keeps parser, AST, Joern execution, BTM generation, replay, graph and UI work out of the current implementation phase.
- WildFly scope check: passed. WildFly is documented only as Git/workspace hardening after the mini repository test.
- gRPC/plugin-to-Analytics scope check: passed. The plugin remains the producer and Analytics remains the consumer/platform.
- Gradle verification: passed with `./gradlew test --dependency-verification strict --console=plain --stacktrace`.
- Diagnostic build verification: passed with `./gradlew clean check --dependency-verification strict --console=plain --stacktrace`.

The diagnostic build emitted Java runtime warnings from gRPC Netty shaded and Protobuf dependencies about `sun.misc.Unsafe` and native-access usage. The build still completed successfully and no repository dependency or source change was made for those warnings.
