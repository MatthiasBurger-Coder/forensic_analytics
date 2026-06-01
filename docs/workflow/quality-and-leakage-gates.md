# Quality and Leakage Gates

## Authoritative Commands

Minimum quality command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Diff check:

```bash
git diff --check
```

## Slice-Specific Checks

| Slice | Required Checks |
|---|---|
| S01 | `git diff --check` |
| S02 | `./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace`; minimum repository test gate |
| S03 | `./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace`; minimum repository test gate |
| S04 | `./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace`; minimum repository test gate |
| S05 | `./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace`; minimum repository test gate |
| S06 | Docker Compose `config` checks for PostgreSQL and repository-source descriptors; `git diff --check` |
| S07 | `./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace`; minimum repository test gate |
| S08 | `./gradlew :query-report-api-service:test --dependency-verification strict --console=plain --stacktrace`; `./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace`; minimum repository test gate |
| S09 | `cd forensic-ui && npm run test`; `cd forensic-ui && npm run build`; minimum repository test gate |
| S10 | targeted repository-source and query-report API tests, frontend test/build, Compose `config` checks, minimum gate, full local gate, `git diff --check` |

## S10 Quality Result

S10 resolved the PostgreSQL adapter package coverage blocker without lowering
coverage thresholds. The full local quality gate passed on 2026-05-31:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

The generated package coverage report records:

```text
de.burger.forensics.analytics.services.repositorysource.adapter.out.postgres line 93.11% branch 87.50%
```

## Leakage Gates

Execution must stop if any slice:

- exposes PostgreSQL credentials in committed files, public responses, logs or
  diagnostics;
- exposes private repository checkout paths;
- exposes H2 file paths as public contract data;
- exposes database credentials, JDBC URLs with credentials or secret values in
  UI state, browser storage, logs, diagnostics or public API responses;
- logs raw Git stdout, stderr, repository credentials or local source content;
- allows a non-owner service to mount `repository-source-workspaces` or query
  repository-source PostgreSQL tables directly;
- stores checkout bytes or source package bytes in PostgreSQL;
- represents database connectivity failure as successful workspace state.
- hides missing or unreachable PostgreSQL behind H2 fallback outside tests.

## Optional Runtime Evidence

Live Docker checks are useful but optional unless the executing slice records
Docker availability and elects to run them:

```bash
docker compose --env-file docker/postgres/.env -f docker/postgres/docker-compose.yml up -d
docker compose -f deployment/docker-compose/repository-to-btm.local.yml build repository-source-service
docker compose -f deployment/docker-compose/repository-to-btm.local.yml up -d repository-source-service
curl -fsS http://127.0.0.1:18087/health
```

Do not claim runtime health unless these commands, or a documented equivalent,
were actually executed.
