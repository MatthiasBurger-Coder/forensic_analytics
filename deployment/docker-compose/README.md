# Local Docker Compose

## Status

`forensic-analytics.local.yml` is the root local Compose entry point for the
Forensic Analytics deployment workflow. It defines the shared Docker network
boundary named `forensic_analytics` and is intentionally empty until service
fragments are added by the workflow slices.

The root stack uses an external Docker network so independently validated
service fragments can join the same local boundary without recreating or
renaming it:

```bash
docker network create forensic_analytics
docker compose -f deployment/docker-compose/forensic-analytics.local.yml config
```

After service fragments exist, combine the selected fragment files before the
root file. Docker Compose resolves relative build contexts from the first
Compose file, and the service fragments use paths relative to
`deployment/docker-compose/services/`:

```bash
docker compose \
  -f deployment/docker-compose/services/<service>.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  config
```

The root entry point does not claim that any service image has been built,
started, or health-checked. Build, startup, and runtime evidence must be
recorded by the slice that executes those commands.

## Service Fragments

The deployment workflow adds one fragment per registered root under
`deployment/docker-compose/services/`.

Runnable backend and UI fragments:

- `analysis-orchestrator-service.compose.yml`
- `analysis-store-service.compose.yml`
- `btm-generation-service.compose.yml`
- `forensic-gateway-service.compose.yml`
- `forensic-ingestion-service.compose.yml`
- `forensic-ui.compose.yml`
- `ingestion-service.compose.yml`
- `java-ast-analysis-service.compose.yml`
- `java-parser-analysis-service.compose.yml`
- `joern-analysis-service.compose.yml`
- `joern-cpg-analysis-service.compose.yml`
- `query-report-api-service.compose.yml`
- `repository-analysis-service.compose.yml`
- `repository-source-service.compose.yml`

Tool, diagnostics, testbed and planned-root fragments:

- `cli-client.compose.yml` uses profile `tools`.
- `observability-stack.compose.yml` uses profile `diagnostics`.
- `testbed.compose.yml` uses profile `testbed`.
- `graph-replay-service.compose.yml` uses profile `planned`.
- `report-generation-service.compose.yml` uses profile `planned`.

The GUI fragment publishes `http://127.0.0.1:18000/` and proxies same-origin
`/api` requests to `query-report-api-service:8080` inside the
`forensic_analytics` Docker network. The query-report API is published on
`http://127.0.0.1:18080/api`.

Validate a selected runnable service with the root network file last:

```bash
docker compose \
  -f deployment/docker-compose/services/query-report-api-service.compose.yml \
  -f deployment/docker-compose/services/forensic-ui.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  config
```

Activate optional profiles explicitly:

```bash
docker compose --profile tools \
  -f deployment/docker-compose/services/cli-client.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  config

docker compose --profile diagnostics \
  -f deployment/docker-compose/services/observability-stack.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  config

docker compose --profile testbed \
  -f deployment/docker-compose/services/testbed.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  config

docker compose --profile planned \
  -f deployment/docker-compose/services/graph-replay-service.compose.yml \
  -f deployment/docker-compose/services/report-generation-service.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  config
```

`graph-replay-service` and `report-generation-service` remain planned roots.
Their fragments are deployment markers only and do not define a runnable
service implementation, Dockerfile, health endpoint or evidence-producing
runtime.

`repository-to-btm.local.yml` remains the verified transitional local
descriptor for the repository-to-BTM service path and the FA-MVP-0001
repository-source owner service. It defines:

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

`testbed` may use this descriptor as local test environment evidence.
The descriptor remains a transitional repository-to-BTM Compose landscape and
does not become a production deployment claim for the FA-MSA-001 target
services or the FA-MVP-0001 repository workspace flow. Runtime startup,
image-build and health-check evidence must be recorded separately when those
commands are executed.

## Local Verification

Build service jars first:

```bash
./gradlew --no-daemon --max-workers=1 :forensic-gateway-service:bootJar :analysis-store-service:bootJar :repository-analysis-service:bootJar :repository-source-service:bootJar :java-ast-analysis-service:bootJar :joern-cpg-analysis-service:bootJar :btm-generation-service:bootJar --dependency-verification strict --console=plain --stacktrace
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
