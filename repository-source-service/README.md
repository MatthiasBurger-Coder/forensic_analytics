# Repository Source Service

## Scope

This service owns repository checkout, revision resolution, workspace
preparation, source-root detection, source snapshot descriptors and source
package byte custody. Other services receive opaque workspace IDs, source
snapshot IDs and artifact references only; private service filesystem paths are
never part of the public contract.

## Runtime

- gRPC port: `9092`
- health port: `8083`
- Docker profile workspace root:
  `/var/lib/forensic-analytics/repository-workspaces`
- local PostgreSQL JDBC URL:
  `jdbc:postgresql://127.0.0.1:5432/forensic_analytics`
- Docker PostgreSQL JDBC URL:
  `jdbc:postgresql://forensic-postgres:5432/forensic_analytics`

The service accepts clean HTTPS repository URLs only. Local paths, `file:`
URLs, SSH/SCP remotes, submodules, build execution and parser execution are
explicitly deprecated at this service boundary. The legacy repository-source
adapter remains registered only as predecessor regression evidence until the
workflow reaches the final removal gate.

Repository checkout runs in a service-owned workspace. Public responses expose
opaque workspace IDs, source snapshot IDs, relative source roots and artifact
references only. Git command output and filesystem paths are not returned in
public error descriptions.

Repository checkout workspace, branch, preparation and idempotency state is
persisted in service-owned PostgreSQL through the `repository_source` schema.
PostgreSQL is the runtime and production path. Missing or unreachable
PostgreSQL is reported through startup failure or storage readiness `DOWN`; the
service does not fall back to H2.

H2 remains available only for deterministic tests and fixtures that instantiate
the H2 adapter directly. H2 is not a runtime or Docker fallback and is not
shared with other services.

## Local Runtime

Package and build this service:

```bash
./gradlew --no-daemon :repository-source-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f repository-source-service/Dockerfile --build-arg SERVICE_JAR=repository-source-service/build/libs/repository-source-service-0.1.0-SNAPSHOT.jar -t forensic-analytics/repository-source-service:local .
```

Run the service locally:

```bash
./gradlew --no-daemon :repository-source-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

The default health endpoint listens on `127.0.0.1:8083` and the service gRPC
port listens on `127.0.0.1:9092`.

For local Docker Compose, `deployment/docker-compose/repository-to-btm.local.yml`
publishes this service on `127.0.0.1:18087` for health and
`127.0.0.1:19097` for gRPC. The descriptor mounts
`repository-source-workspaces` at
`/var/lib/forensic-analytics/repository-workspaces` only into
`repository-source-service`.

Docker named volumes preserve checkout and PostgreSQL state across container
restart while the volumes are retained. Removing volumes, for example with
`docker compose down -v`, removes local PostgreSQL data and checkout bytes.
This service README does not claim Docker Swarm or Kubernetes runtime
readiness.
