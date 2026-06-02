# Quality And Leakage Gates

## Required Quality Source

`QUALITY.md` is authoritative.

## Minimum Command

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

## Full Local Gate

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Leakage Rules

- Do not expose local workspace paths, JDBC URLs, raw Git output, credentials, tokens, private DNS data or PostgreSQL table names in public DTOs or UI diagnostics.
- Branch names are public data returned from remote refs, but they must not be interpreted as paths.
- UI and query-report must not read repository-source persistence directly.
- Live WildFly smoke checks are optional diagnostics, not mandatory quality gates.
