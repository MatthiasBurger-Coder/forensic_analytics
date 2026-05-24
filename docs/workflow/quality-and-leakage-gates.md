# Quality And Leakage Gates: FA-MVP-0001

## Authority

`QUALITY.md` is the authoritative quality contract.

Minimum repository command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Targeted Gates

Repository Source:

```bash
./gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace
```

Query Report API:

```bash
./gradlew :services:query-report-api-service:test --dependency-verification strict --console=plain --stacktrace
```

Frontend:

```bash
cd forensic-ui
npm ci
npm run test
npm run build
```

Docker model:

```bash
docker compose -f deployment/docker-compose/repository-to-btm.local.yml config
```

H2 dependency metadata repair, only when strict dependency verification reports
missing H2 metadata:

```bash
./gradlew --write-verification-metadata sha256 :services:repository-source-service:test
./gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace
```

## Leakage Gates

Public REST, gRPC and UI tests must verify that none of the following leak into
public responses:

- absolute filesystem paths;
- `/var/lib/forensic-analytics/repository-workspaces`;
- `/var/lib/forensic-analytics/repository-source-data`;
- `repository-workspaces`;
- raw stdout;
- raw stderr;
- credentials;
- tokens;
- passwords;
- authorization headers;
- private network details;
- local repository paths.

## Evidence Integrity Gates

Tests must verify:

- `workspaceTitle` is derived from repository name and is read-only.
- `workspaceTitle` is never used as a path or security key.
- `WorkspaceId` and `WorkspaceBranchId` are opaque.
- branch names are stored as data and never used directly as directories.
- same idempotency key plus same fingerprint returns the same result.
- same idempotency key plus different fingerprint returns a controlled conflict
  without mutation.
- branch refresh returns `UP_TO_DATE` when the commit is unchanged.
- branch refresh returns `UPDATED` and creates a new source snapshot reference
  when the commit changes.
- missing or unresolved repository facts are represented as diagnostics, not
  fabricated evidence.

## Optional External Checks

Docker image build and runtime checks are valuable but external:

```bash
./gradlew --no-daemon :services:repository-source-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/repository-source-service/Dockerfile --build-arg SERVICE_JAR=services/repository-source-service/build/libs/repository-source-service-0.1.0-SNAPSHOT.jar -t forensic-analytics/repository-source-service:local .
docker compose -f deployment/docker-compose/repository-to-btm.local.yml up -d
```

These checks may require Docker and network access. Do not claim they passed
unless executed. Report skipped external checks explicitly.
