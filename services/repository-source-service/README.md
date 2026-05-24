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
- Docker profile H2 data root:
  `/var/lib/forensic-analytics/repository-source-data`
- local H2 JDBC URL:
  `jdbc:h2:file:./build/repository-source-data/repository-source;AUTO_SERVER=FALSE;DB_CLOSE_DELAY=-1`
- Docker H2 JDBC URL:
  `jdbc:h2:file:/var/lib/forensic-analytics/repository-source-data/repository-source;AUTO_SERVER=FALSE;DB_CLOSE_DELAY=-1`

The service accepts clean HTTPS repository URLs only. Local paths, `file:`
URLs, SSH/SCP remotes, submodules, build execution and parser execution are
explicitly deprecated at this service boundary. The legacy repository-source
adapter remains registered only as predecessor regression evidence until the
workflow reaches the final removal gate.

Repository checkout runs in a service-owned workspace. Public responses expose
opaque workspace IDs, source snapshot IDs, relative source roots and artifact
references only. Git command output and filesystem paths are not returned in
public error descriptions.

Repository checkout workspace, branch and idempotency state is persisted in a
service-local H2 file adapter for the Docker-local MVP. H2 is not a production
analytics persistence decision and is not shared with other services.

## Local Runtime

Package and build this service:

```bash
./gradlew --no-daemon :services:repository-source-service:bootJar --dependency-verification strict --console=plain --stacktrace
docker build -f services/repository-source-service/Dockerfile --build-arg SERVICE_JAR=services/repository-source-service/build/libs/repository-source-service-0.1.0-SNAPSHOT.jar -t forensic-analytics/repository-source-service:local .
```

Run the service locally:

```bash
./gradlew --no-daemon :services:repository-source-service:bootRun --dependency-verification strict --console=plain --stacktrace
```

The default health endpoint listens on `127.0.0.1:8083` and the service gRPC
port listens on `127.0.0.1:9092`.

For local Docker Compose, `deployment/docker-compose/repository-to-btm.local.yml`
publishes this service on `127.0.0.1:18087` for health and
`127.0.0.1:19097` for gRPC. The descriptor mounts
`repository-source-workspaces` at
`/var/lib/forensic-analytics/repository-workspaces` and
`repository-source-data` at
`/var/lib/forensic-analytics/repository-source-data` only into
`repository-source-service`.

Docker named volumes preserve checkout and H2 state across container restart
while the volumes are retained. Removing volumes, for example with
`docker compose down -v`, removes that local MVP state. This service README
does not claim Docker Swarm or Kubernetes runtime readiness.
