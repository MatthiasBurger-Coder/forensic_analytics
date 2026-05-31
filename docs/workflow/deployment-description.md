# Deployment Description

## Current Verified State

`repository-source-service` previously stored repository checkout workspace
metadata in an H2 file under its private data volume and stores checkout bytes
under the separate `repository-source-workspaces` Docker volume.

After the accepted 2026-05-31 clarification, H2 is not a runtime target. H2 may
remain only for tests and deterministic fixtures. Runtime and production
operation must use PostgreSQL and must report missing or unreachable PostgreSQL
instead of silently falling back.

`docker/postgres/docker-compose.yml` defines `forensic-postgres` with a
PostgreSQL database named through `POSTGRES_DB`.

## Target Local Runtime

```text
forensic-postgres
  -> PostgreSQL database forensic_analytics
     -> Liquibase-created repository_source schema

repository-source-service
  -> JDBC connection to forensic-postgres
  -> repository-source-workspaces volume for checkout bytes

forensic-ui Settings
  -> query-report-api-service public Settings API
  -> repository-source configuration handoff
  -> sanitized PostgreSQL validation/readiness diagnostics
```

## Required Deployment Changes

- Attach `forensic-postgres` and repository-source runtime to a shared local
  network or include both in the same Compose model.
- Configure repository-source Docker profile with PostgreSQL connection
  properties.
- Keep `repository-source-workspaces` mounted only into
  `repository-source-service`.
- Remove the active `repository-source-data` H2 runtime volume after the
  PostgreSQL cutover unless it is retained only as explicit migration input.
- Add contract-governed Settings API and React Settings UI flow for operator
  database configuration.
- Keep database credentials out of committed files, browser storage, logs and
  public responses.
- Document that `docker compose down -v` removes local PostgreSQL and checkout
  volumes.

## Not Claimed

This workflow does not claim Docker Swarm, Kubernetes, production secrets
management, database backup/restore, hot runtime reconfiguration without
restart or live runtime readiness until the corresponding commands are
executed and recorded.
