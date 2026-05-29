# Forensic Analytics Docker Compose Runbook

## Scope

This runbook describes the local Docker Compose deployment material generated
by the workflow on the `forensic_analytics` Docker network. It is local
operator evidence, not a production, Swarm or Kubernetes readiness claim.

Runnable services use service-owned Dockerfiles and prebuilt Gradle boot jars.
`cli-client`, `observability-stack`, `testbed`, `graph-replay-service` and
`report-generation-service` are profile-gated or marker descriptors and are
not productive backend services.

## Network

Create the shared network once:

```bash
docker network create forensic_analytics
```

If it already exists, Docker reports that the network is already present. The
workflow smoke test created this network locally on May 28, 2026.

## Build Inputs

Build the runnable service jars before Docker image builds:

```bash
./gradlew --no-daemon --max-workers=1 :repository-source-service:bootJar :ingestion-service:bootJar :java-parser-analysis-service:bootJar :joern-analysis-service:bootJar :analysis-orchestrator-service:bootJar :query-report-api-service:bootJar :forensic-ingestion-service:bootJar :forensic-gateway-service:bootJar :analysis-store-service:bootJar :repository-analysis-service:bootJar :java-ast-analysis-service:bootJar :joern-cpg-analysis-service:bootJar :btm-generation-service:bootJar --dependency-verification strict --console=plain --stacktrace
```

Build the CLI distribution before building the CLI image:

```bash
./gradlew :cli-client:installDist --dependency-verification strict --console=plain --stacktrace
```

Build and test the UI:

```bash
cd forensic-ui
npm ci
npm run test
npm run build
```

## Compose Config Validation

Validate the API and GUI path:

```bash
docker compose \
  -f deployment/docker-compose/services/query-report-api-service.compose.yml \
  -f deployment/docker-compose/services/forensic-ui.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  config
```

Validate optional profiles:

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

## GUI Smoke Path

The GUI is published at:

```text
http://127.0.0.1:18000/
```

The public API is published at:

```text
http://127.0.0.1:18080/api
```

Start the verified GUI smoke pair:

```bash
docker compose -p forensic-analytics-smoke \
  -f deployment/docker-compose/services/query-report-api-service.compose.yml \
  -f deployment/docker-compose/services/forensic-ui.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  up -d query-report-api-service forensic-ui
```

Check same-origin API routing through nginx:

```bash
curl -fsS http://127.0.0.1:18000/api/health
```

Executed result on May 28, 2026:

```json
{"status":"UP"}
```

Stop the smoke pair:

```bash
docker compose -p forensic-analytics-smoke \
  -f deployment/docker-compose/services/query-report-api-service.compose.yml \
  -f deployment/docker-compose/services/forensic-ui.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  down
```

## Full Local Startup

The full local model can be built and started by combining runnable fragments
before the root network file. This command validates the model; runtime startup
may still reveal service-level integration gaps and should be recorded as
runtime evidence only after it is executed.

The documented commands can be run through the local helper script:

```bash
bash deployment/docker-compose/setup.sh full
```

The helper refuses to stop a running Forensic Analytics Docker instance by
default. This protects local persistence state, including the PostgreSQL
development database and service-owned named volumes. When an intentional local
restart is needed, set `ALLOW_FORENSIC_ANALYTICS_RESTART=1`; the helper still
stops known local Compose projects without removing named volumes. This avoids
stale containers holding host ports such as `127.0.0.1:18080` during repeated
local deploys while preserving persisted state.

```bash
docker compose \
  -f deployment/docker-compose/services/repository-source-service.compose.yml \
  -f deployment/docker-compose/services/ingestion-service.compose.yml \
  -f deployment/docker-compose/services/java-parser-analysis-service.compose.yml \
  -f deployment/docker-compose/services/joern-analysis-service.compose.yml \
  -f deployment/docker-compose/services/analysis-orchestrator-service.compose.yml \
  -f deployment/docker-compose/services/query-report-api-service.compose.yml \
  -f deployment/docker-compose/services/forensic-ingestion-service.compose.yml \
  -f deployment/docker-compose/services/forensic-gateway-service.compose.yml \
  -f deployment/docker-compose/services/analysis-store-service.compose.yml \
  -f deployment/docker-compose/services/repository-analysis-service.compose.yml \
  -f deployment/docker-compose/services/java-ast-analysis-service.compose.yml \
  -f deployment/docker-compose/services/joern-cpg-analysis-service.compose.yml \
  -f deployment/docker-compose/services/btm-generation-service.compose.yml \
  -f deployment/docker-compose/services/forensic-ui.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  config
```

Build images:

```bash
docker compose \
  -f deployment/docker-compose/services/repository-source-service.compose.yml \
  -f deployment/docker-compose/services/ingestion-service.compose.yml \
  -f deployment/docker-compose/services/java-parser-analysis-service.compose.yml \
  -f deployment/docker-compose/services/joern-analysis-service.compose.yml \
  -f deployment/docker-compose/services/analysis-orchestrator-service.compose.yml \
  -f deployment/docker-compose/services/query-report-api-service.compose.yml \
  -f deployment/docker-compose/services/forensic-ingestion-service.compose.yml \
  -f deployment/docker-compose/services/forensic-gateway-service.compose.yml \
  -f deployment/docker-compose/services/analysis-store-service.compose.yml \
  -f deployment/docker-compose/services/repository-analysis-service.compose.yml \
  -f deployment/docker-compose/services/java-ast-analysis-service.compose.yml \
  -f deployment/docker-compose/services/joern-cpg-analysis-service.compose.yml \
  -f deployment/docker-compose/services/btm-generation-service.compose.yml \
  -f deployment/docker-compose/services/forensic-ui.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  build
```

Start:

```bash
docker compose -p forensic-analytics-local \
  -f deployment/docker-compose/services/repository-source-service.compose.yml \
  -f deployment/docker-compose/services/ingestion-service.compose.yml \
  -f deployment/docker-compose/services/java-parser-analysis-service.compose.yml \
  -f deployment/docker-compose/services/joern-analysis-service.compose.yml \
  -f deployment/docker-compose/services/analysis-orchestrator-service.compose.yml \
  -f deployment/docker-compose/services/query-report-api-service.compose.yml \
  -f deployment/docker-compose/services/forensic-ingestion-service.compose.yml \
  -f deployment/docker-compose/services/forensic-gateway-service.compose.yml \
  -f deployment/docker-compose/services/analysis-store-service.compose.yml \
  -f deployment/docker-compose/services/repository-analysis-service.compose.yml \
  -f deployment/docker-compose/services/java-ast-analysis-service.compose.yml \
  -f deployment/docker-compose/services/joern-cpg-analysis-service.compose.yml \
  -f deployment/docker-compose/services/btm-generation-service.compose.yml \
  -f deployment/docker-compose/services/forensic-ui.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  up -d
```

Health checks to execute after startup:

```bash
curl -fsS http://127.0.0.1:18080/api/health
curl -fsS http://127.0.0.1:18000/api/health
```

Service Dockerfiles define container health checks where verified. Individual
host health ports are listed in the compose fragments. Do not report a service
healthy unless the matching command has been executed.

## CLI Tool Profile

Run the CLI as a one-shot tool profile:

```bash
docker compose --profile tools \
  -f deployment/docker-compose/services/cli-client.compose.yml \
  -f deployment/docker-compose/forensic-analytics.local.yml \
  run --rm cli-client --help
```

To submit through the public API inside the Docker network, pass
`--gateway http://query-report-api-service:8080/api`. The CLI must not call
private gRPC worker services directly.

## Logs And Cleanup

Inspect logs:

```bash
docker compose -p forensic-analytics-local logs --tail=200
```

Stop containers:

```bash
docker compose -p forensic-analytics-local down
```

Remove named volumes for a clean local reset:

```bash
docker compose -p forensic-analytics-local down -v
```

Repository-source, Java AST, Joern, Joern CPG and BTM volumes contain local
workspace or generated artifact bytes. Treat them as local deployment state,
not verified forensic evidence by themselves.

## Verification Status

Executed in this workflow:

- Compose config validation for each service fragment.
- UI `npm ci`, `npm run test`, `npm run build`.
- UI Docker image build.
- nginx syntax check inside the built UI image.
- Query Report API plus UI smoke with `curl -fsS http://127.0.0.1:18000/api/health`.

Not executed as a full-stack runtime claim:

- Full local stack startup for every service.
- Health checks for every service container.
- Joern CPG image runtime smoke; it may require pulling the digest-pinned Joern
  base image.
- Browser-driven interaction beyond the same-origin `/api/health` smoke.
