# Quality And Leakage Gates

## Required Quality Commands

Targeted repository-source check:

```bash
./gradlew :services:repository-source-service:test --tests '*RepositorySourceServiceApplicationTest' --dependency-verification strict --console=plain --stacktrace
```

Targeted query-report check:

```bash
./gradlew :services:query-report-api-service:test --tests '*QueryReportApiServiceApplicationTest' --dependency-verification strict --console=plain --stacktrace
```

Minimum repository gate:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local gate before commit readiness:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Live Proof Gate

The live proof must use only public query-report REST endpoints. It must record:

- query-report health response;
- workspace create response;
- workspace checkout-result response;
- workspace list response;
- workspace ID;
- branch status;
- resolved commit;
- repository-source workspace root;
- UI URL.

## Leakage Gate

The execution report must not expose:

- credentials or tokens;
- raw Git stdout/stderr containing sensitive data;
- private H2 JDBC URLs except sanitized service-owned configuration summaries;
- local checkout file listings beyond the root locality evidence;
- source contents from WildFly.

## Failure Reporting

If a command fails, report:

- command executed;
- failure summary;
- failing task or endpoint;
- whether the failure appears caused by the current change;
- remaining blocker.
