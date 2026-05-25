# Local Docker Compose

## Status

Documented local Docker Compose descriptor for the repository-to-BTM service
path and the FA-MVP-0001 repository-source owner service.

`repository-to-btm.local.yml` defines the transitional local landscape:

- `forensic-gateway-service`
- `analysis-store-service`
- `repository-analysis-service`
- `repository-source-service`
- `java-ast-analysis-service`
- `joern-cpg-analysis-service`
- `btm-generation-service`

The file uses service-owned Dockerfiles and their Docker profile configuration.
It does not add external databases, brokers, Graph DB, Vector DB, Jenkins,
Artifactory or live credentials.

`repository-source-service` owns two named Docker volumes:

- `repository-source-workspaces` mounted at
  `/var/lib/forensic-analytics/repository-workspaces`
- `repository-source-data` mounted at
  `/var/lib/forensic-analytics/repository-source-data`

No other service in this descriptor mounts those repository-source private
volumes. The H2 state and checkout bytes survive container restart while the
named volumes exist. `docker compose down -v` removes them.

`services:testbed` may use this descriptor as local test environment evidence.
The descriptor remains a transitional repository-to-BTM Compose landscape and
does not become a production deployment claim for the FA-MSA-001 target
services or the FA-MVP-0001 repository workspace flow. Runtime startup,
image-build and health-check evidence must be recorded separately when those
commands are executed.

## Local Verification

Build service jars first:

```bash
./gradlew --no-daemon --max-workers=1 :services:forensic-gateway-service:bootJar :services:analysis-store-service:bootJar :services:repository-analysis-service:bootJar :services:repository-source-service:bootJar :services:java-ast-analysis-service:bootJar :services:joern-cpg-analysis-service:bootJar :services:btm-generation-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

Validate the Compose model:

```bash
docker compose -f deployment/docker-compose/repository-to-btm.local.yml config
```

Optional runtime check:

```bash
docker compose -f deployment/docker-compose/repository-to-btm.local.yml build
docker compose -f deployment/docker-compose/repository-to-btm.local.yml up -d
curl -fsS http://127.0.0.1:18080/api/health
curl -fsS http://127.0.0.1:18082/health
curl -fsS http://127.0.0.1:18083/health
curl -fsS http://127.0.0.1:18087/health
curl -fsS http://127.0.0.1:18084/health
curl -fsS http://127.0.0.1:18085/health
curl -fsS http://127.0.0.1:18086/health
docker compose -f deployment/docker-compose/repository-to-btm.local.yml down -v
```

The optional runtime check may require network access to pull base images,
including the digest-pinned Joern image.
