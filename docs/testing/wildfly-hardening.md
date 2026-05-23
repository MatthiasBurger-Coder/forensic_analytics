# WildFly Repository Hardening

## Status

WildFly hardening is an opt-in external scenario for checkout, workspace,
source-root detection, metrics and cleanup behavior. It is not part of the
default quality gate.

The scenario is implemented by:

```text
forensic-analytics-testbed/src/test/java/de/burger/forensics/analytics/testbed/WildFlyRepositoryHardeningTest.java
services/testbed/src/test/java/de/burger/forensics/analytics/services/testbed/WildFlyRepositoryHardeningTest.java
```

## Default Verification

The default targeted command must pass without running the external checkout:

```bash
./gradlew :forensic-analytics-testbed:test --tests '*WildFlyRepositoryHardeningTest' --dependency-verification strict --console=plain --stacktrace
./gradlew :services:testbed:test --tests '*WildFlyRepositoryHardeningTest' --dependency-verification strict --console=plain --stacktrace
```

When `FORENSIC_ANALYTICS_WILDFLY_HARDENING` is not set to `true`, JUnit skips
the hardening scenario through an assumption. That skip is the expected default
result.

## Required External Inputs

Set these variables only when intentionally running the external WildFly
checkout:

```bash
export FORENSIC_ANALYTICS_WILDFLY_HARDENING=true
export FORENSIC_ANALYTICS_WILDFLY_BRANCH=<wildfly-branch>
# or:
export FORENSIC_ANALYTICS_WILDFLY_COMMIT=<wildfly-commit>
```

At least one of `FORENSIC_ANALYTICS_WILDFLY_BRANCH` or
`FORENSIC_ANALYTICS_WILDFLY_COMMIT` is required. Do not run against an
implicit default branch because that would make the hardening evidence drift
over time.

Optional resource controls:

```bash
export FORENSIC_ANALYTICS_WILDFLY_TIMEOUT_SECONDS=1200
export FORENSIC_ANALYTICS_WILDFLY_MIN_FREE_BYTES=5368709120
export FORENSIC_ANALYTICS_WILDFLY_REPORT_DIR=build/reports/wildfly-hardening
```

## External Run Command

```bash
./gradlew :forensic-analytics-testbed:test --tests '*WildFlyRepositoryHardeningTest' --dependency-verification strict --console=plain --stacktrace
./gradlew :services:testbed:test --tests '*WildFlyRepositoryHardeningTest' --dependency-verification strict --console=plain --stacktrace
```

## Evidence Written

The hardening report is written to:

```text
build/reports/wildfly-hardening/wildfly-hardening-metrics.txt
```

unless `FORENSIC_ANALYTICS_WILDFLY_REPORT_DIR` is set.

The report records:

- repository URL;
- requested branch;
- requested commit;
- timeout seconds;
- minimum free bytes;
- workspace path;
- clone duration;
- checkout duration when branch or commit checkout is used;
- resolved remote URL;
- resolved commit;
- detected source-root count;
- file count;
- workspace size;
- cleanup duration;
- cleanup status;
- failure type and message when an operation fails.

## Safety Rules

- Do not execute WildFly build scripts as part of this scenario.
- Do not infer a branch or commit when none is provided.
- Do not treat a skipped external run as hardening success.
- Do not lower disk, timeout or cleanup safeguards to force a local run.
- Do not commit generated hardening reports.
- If cleanup cannot prove that the temporary workspace was removed, report the
  run as failed.

## Expected Skip Reasons

Record optional external hardening as `SKIPPED` when:

- network access to GitHub is unavailable;
- no WildFly branch or commit was provided;
- the workspace file store has less free space than
  `FORENSIC_ANALYTICS_WILDFLY_MIN_FREE_BYTES`;
- the local timeout budget is too small for a large repository checkout;
- the operator intentionally runs only the default local quality gate.
