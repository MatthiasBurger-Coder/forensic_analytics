# Local Docker Compose

## Status

Documented local Docker Compose descriptor for the repository-to-BTM service
path.

`repository-to-btm.local.yml` defines the six-service local landscape that is
needed after Slice 14:

- `forensic-gateway-service`
- `analysis-store-service`
- `repository-analysis-service`
- `java-ast-analysis-service`
- `joern-cpg-analysis-service`
- `btm-generation-service`

The file uses service-owned Dockerfiles and their Docker profile configuration.
It does not add external databases, brokers, Graph DB, Vector DB, Jenkins,
Artifactory or live credentials.

`services:testbed` may use this descriptor as local test environment evidence.
The descriptor remains a transitional repository-to-BTM Compose landscape and
does not become a production deployment claim for the FA-MSA-001 target
services. S15 did not execute or record Compose model validation, image-build,
startup or health-check commands for this descriptor.

## Local Verification

Build service jars first:

```bash
./gradlew --no-daemon --max-workers=1 :services:forensic-gateway-service:bootJar :services:analysis-store-service:bootJar :services:repository-analysis-service:bootJar :services:java-ast-analysis-service:bootJar :services:joern-cpg-analysis-service:bootJar :services:btm-generation-service:bootJar --dependency-verification strict --console=plain --stacktrace
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
curl -fsS http://127.0.0.1:18084/health
curl -fsS http://127.0.0.1:18085/health
curl -fsS http://127.0.0.1:18086/health
docker compose -f deployment/docker-compose/repository-to-btm.local.yml down -v
```

The optional runtime check may require network access to pull base images,
including the digest-pinned Joern image.
