# Quality And Leakage Gates

## Verified Commands

Minimum repository gate:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Targeted backend gates:

```bash
./gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:query-report-api-service:test --dependency-verification strict --console=plain --stacktrace
```

Frontend gates:

```bash
cd forensic-ui && npm ci
cd forensic-ui && npm run test
cd forensic-ui && npm run build
```

Diff gate:

```bash
git diff --check
```

## No-Leak Assertions

Public list, delete and refresh responses must not contain:

- H2 JDBC URLs or H2 file paths.
- Repository-source private workspace paths.
- Raw Git stdout or stderr.
- Credentials, tokens, passwords, secrets or authorization headers.
- Internal service hostnames or private network details.
- Absolute source roots or local machine paths.

## Evidence Integrity Assertions

- Delete means cleanup/mark-cleaned, not unreviewed hard deletion.
- Cleanup diagnostics must represent incomplete or failed cleanup explicitly.
- Branch refresh may report only observed remote/checkout facts.
- The UI must not infer selected branch when no public branch DTO exists.
- Cleaned workspaces are hidden from default list only as a view policy; stored
  metadata remains available to repository-source.

## Quality Failure Routing

- Architecture violation: Senior System Architect and architecture skills.
- Build failure: responsible owner plus Senior DevOps when Gradle or build
  configuration is involved.
- Test failure: Senior Tester and slice owner.
- Contract failure: Senior System Architect, Senior Java Backend Developer and
  contract governance.
- Unknown failure: Root Architect escalation path after classification attempt.
