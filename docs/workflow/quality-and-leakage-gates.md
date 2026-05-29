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
| S08 | targeted repository-source test, Compose `config` checks, minimum gate, full local gate, `git diff --check` |

## Leakage Gates

Execution must stop if any slice:

- exposes PostgreSQL credentials in committed files, public responses, logs or
  diagnostics;
- exposes private repository checkout paths;
- exposes H2 file paths as public contract data;
- logs raw Git stdout, stderr, repository credentials or local source content;
- allows a non-owner service to mount `repository-source-workspaces` or query
  repository-source PostgreSQL tables directly;
- stores checkout bytes or source package bytes in PostgreSQL;
- represents database connectivity failure as successful workspace state.

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
