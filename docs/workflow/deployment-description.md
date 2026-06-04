# Deployment Description

## Corrected Repository-Source Runtime View

```text
forensic-ui
  -> query-report-api-service
    -> repository-source-service
       -> forensic-postgres / repository_source schema
       -> repository-source-workspaces volume
```

## Verified Docker-Local Baseline

The verified Docker-local repository-source descriptors configure:

- `--forensics.repository-source.service.persistence.type=postgres`
- `FORENSICS_REPOSITORY_SOURCE_POSTGRES_JDBC_URL`
- `FORENSICS_REPOSITORY_SOURCE_POSTGRES_USERNAME`
- `FORENSICS_REPOSITORY_SOURCE_POSTGRES_PASSWORD`
- `FORENSICS_REPOSITORY_SOURCE_POSTGRES_SCHEMA`
- `repository-source-workspaces:/var/lib/forensic-analytics/repository-workspaces`

No verified Docker-local descriptor mounts repository-source H2 files as active
runtime persistence.

## H2 Boundary

H2 is allowed only when deterministic tests or direct fixtures instantiate the
repository-source H2 adapter directly. H2 is not runtime storage, Docker
persistence, startup fallback, readiness fallback or cross-service persistence.

## Readiness Boundary

Missing or unreachable PostgreSQL must be visible as startup failure or storage
readiness `DOWN`. Readiness `DOWN` must not be masked by memory, H2 or
file-based fallback storage.

## Non-Claims

This workflow does not claim:

- Docker image startup success;
- repository-source health-check smoke success;
- full production Compose readiness;
- Docker Swarm readiness;
- Kubernetes readiness;
- migration of historical H2 files.
