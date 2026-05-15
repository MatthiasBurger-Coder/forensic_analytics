# WildFly Hardening Result

This records the opt-in WildFly hardening run for the workspace and Git checkout path.

## Execution

- Executed at: `2026-05-15T10:38:48+02:00`
- Command: `FORENSIC_ANALYTICS_WILDFLY_HARDENING=true FORENSIC_ANALYTICS_WILDFLY_BRANCH=main FORENSIC_ANALYTICS_WILDFLY_TIMEOUT_SECONDS=1800 ./gradlew :forensic-analytics-testbed:test --tests de.burger.forensics.analytics.testbed.WildFlyRepositoryHardeningTest --rerun-tasks --dependency-verification strict --console=plain --stacktrace`
- Result: passed

## Metrics

- Repository URL: `https://github.com/wildfly/wildfly.git`
- Requested branch: `main`
- Requested commit: `UNSPECIFIED`
- Timeout: `1800` seconds
- Minimum free workspace disk: `5368709120` bytes
- Workspace preparation: `40` ms
- Clone: `63041` ms
- Branch checkout: `217` ms
- Commit resolution: `2` ms
- Remote URL resolution: `2` ms
- Source-root detection: `293` ms
- Workspace measurement: `291` ms
- Resolved remote URL: `https://github.com/wildfly/wildfly.git`
- Resolved commit: `b5a9850d01ed5bbdfa3db587a8342990771c175d`
- Detected source roots: `118`
- File count: `14118`
- Workspace size: `414142084` bytes
- Cleanup status: `CLEANED`
- Cleanup duration: `636` ms

## Evidence Boundary

The run validated clone, checkout, resolved commit detection, source-root detection and cleanup only. It did not execute parsers, Joern, BTM generation, replay, LLM calls or report generation. These metrics are operational hardening evidence, not analysis findings.
