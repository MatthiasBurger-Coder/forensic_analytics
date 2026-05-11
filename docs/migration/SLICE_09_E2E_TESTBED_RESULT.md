# Slice 09 E2E Testbed Result

Date: 2026-05-11

## Scope Executed

- Added `forensic-analytics-testbed` as a dedicated test-only module.
- Added a default no-Joern repository analysis scenario that exercises:
  - CLI argument parsing and summary output.
  - `RepositoryAnalysisEngine`.
  - `DefaultRunRepositoryAnalysisUseCase`.
  - `LocalRepositorySourceAdapter`.
  - Testbed scanner, semantic, rule-generation, and result-store ports.
- Added a Joern adapter-boundary scenario that runs the application and engine path through `JoernDockerSemanticAnalysisAdapter` with a deterministic testbed command runner.
- Added an opt-in pinned Joern Docker version smoke test guarded by environment variables.

## Boundary Decision

The default quality gate must not require Docker, Joern, WildFly, or any external service.

The testbed therefore has three levels:

- Default no-Joern E2E scenario, always run by Gradle `test`.
- Default Joern adapter-boundary scenario using a deterministic testbed runner, always run by Gradle `test`.
- External Joern Docker smoke scenario, tagged `docker` and skipped unless explicitly enabled.

## Joern Docker Smoke

The external smoke test is intentionally opt-in. To run it locally, set:

```text
FORENSIC_ANALYTICS_JOERN_DOCKER_SMOKE=true
FORENSIC_ANALYTICS_JOERN_IMAGE=<sha256-pinned Joern image reference>
FORENSIC_ANALYTICS_DOCKER_EXECUTABLE=docker
```

`FORENSIC_ANALYTICS_DOCKER_EXECUTABLE` is optional and defaults to `docker`.

The smoke test runs only the pinned container version command. Full Joern script execution remains outside the default gate until the Joern scripts and image digest are fixed as repository decisions.

## WildFly

No WildFly scenario was added to the normal unit gate.

WildFly remains a future smoke/resource scenario because it can exceed local gate time and memory boundaries. It should be added only behind an explicit opt-in profile or environment contract.

## Workplan Alignment

This slice follows Slice 09 from `MIGRATION_WORKPLAN.md`:

- Added the first no-Joern E2E scenario.
- Added a Joern-container-ready testbed boundary with an opt-in external Docker smoke.
- Kept external Docker/WildFly work outside the normal unit gate.

## Verification

Executed in `D:\Projects\forensic_analytics`:

```text
.\gradlew.bat :forensic-analytics-testbed:test --dependency-verification strict --console=plain --stacktrace
.\gradlew.bat test --dependency-verification strict --console=plain --stacktrace
.\gradlew.bat clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Result: passed.

Sonar:

- Skipped because `SONAR_TOKEN` was not set in the local environment.
