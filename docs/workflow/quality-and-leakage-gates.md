# Quality And Leakage Gates

## Documentation Gate

```bash
git diff --check
```

Additional documentation search before closure:

```bash
rg -n "Docker-local MVP H2|H2 data volume|H2 data volumes|H2 volumes|repository-source-data H2|service-local H2 file persistence|H2 is Docker-local" docs/architecture docs/arc42 repository-source-service/README.md query-report-api-service/README.md
```

Any match must be reviewed. It is acceptable only when the text explicitly
describes historical H2 test or fixture scope and forbids runtime, Docker and
fallback usage.

## Repository-Source Gates

```bash
./gradlew :repository-source-service:test --tests "*RepositorySourceServiceApplicationTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :repository-source-service:test --tests "*RepositorySourcePostgresPersistenceApplicationTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :repository-source-service:test --tests "*RepositorySourceH2PersistenceApplicationTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :repository-source-service:test --dependency-verification strict --console=plain --stacktrace
```

Required evidence:

- H2 is rejected as runtime persistence.
- PostgreSQL persistence is selected for runtime metadata.
- Storage readiness `DOWN` is visible and sanitized.
- H2 adapter tests instantiate the H2 adapter directly as deterministic
  fixtures only.

## Public API And UI Gates

```bash
./gradlew :query-report-api-service:test --tests "*QueryReportApiWorkspaceServiceTest" --dependency-verification strict --console=plain --stacktrace
./gradlew :query-report-api-service:test --tests "*QueryReportApiWorkspaceTest" --dependency-verification strict --console=plain --stacktrace
cd forensic-ui && npm run test -- src/adapters/api/mappers.test.ts src/adapters/api/apiClient.test.ts src/pages/workspaces/CreateWorkspacePage.test.tsx
cd forensic-ui && npm run build
```

Required evidence:

- Public DTOs do not expose JDBC URLs, table names, H2 paths, checkout paths,
  raw Git output, credentials or tokens.
- Query-report uses repository-source owner APIs only.
- UI uses query-report public REST only.

## Docker-Local Gates

```bash
docker compose -f deployment/docker-compose/repository-to-btm.local.yml config
docker compose -f deployment/docker-compose/services/repository-source-service.compose.yml config
```

Required evidence:

- Runtime persistence type is PostgreSQL.
- PostgreSQL settings are configured through repository-source-owned settings.
- Repository-source mounts its private checkout workspace volume only.
- H2 files are not mounted as active runtime persistence.

## Repository Gates

Minimum:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Leakage Stop Conditions

Stop if any public path exposes:

- PostgreSQL JDBC URL;
- PostgreSQL table or schema internals;
- H2 path or file name;
- private checkout path;
- raw stdout or stderr;
- credentials, tokens or private network details.
