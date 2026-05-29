# Deployment Description Draft

## Purpose

This workflow-local deployment description defines the target operator flow for
the later implementation slices. It is not runtime evidence until
`workflow execute` creates the files and records command results.

## Target File Layout

```text
deployment/docker-compose/
  forensic-analytics.local.yml
  README.md
  services/
    analysis-orchestrator-service.compose.yml
    analysis-store-service.compose.yml
    btm-generation-service.compose.yml
    cli-client.compose.yml
    forensic-gateway-service.compose.yml
    forensic-ingestion-service.compose.yml
    forensic-ui.compose.yml
    graph-replay-service.compose.yml
    ingestion-service.compose.yml
    java-ast-analysis-service.compose.yml
    java-parser-analysis-service.compose.yml
    joern-analysis-service.compose.yml
    joern-cpg-analysis-service.compose.yml
    observability-stack.compose.yml
    query-report-api-service.compose.yml
    report-generation-service.compose.yml
    repository-analysis-service.compose.yml
    repository-source-service.compose.yml
    testbed.compose.yml

docs/deployment/
  forensic-analytics-docker-compose.md
```

## Network

All Compose descriptors must use:

```yaml
networks:
  forensic_analytics:
    name: forensic_analytics
```

Service entries must attach to that network. The root stack or the first
service descriptor may create the network. The name must stay stable across
Compose project names.

If service descriptors are run independently, use an explicit external shared
network strategy:

```bash
docker network inspect forensic_analytics >/dev/null 2>&1 || docker network create forensic_analytics
```

The runbook must state whether the root stack creates the network or expects
the external network to exist.

## GUI API Routing

The GUI must reach `query-report-api-service` through a verified browser-safe
path. Preferred local deployment behavior is:

```text
browser -> forensic-ui nginx -> /api proxy -> query-report-api-service:8080
```

The existing `forensic-ui/nginx.conf` returns `502 BACKEND_UNAVAILABLE` for
`/api`, so a later implementation slice must replace that behavior or provide a
verified alternative before claiming GUI deployment success.

## Operator Flow

1. Build service artifacts with the Gradle tasks named in the runbook.
2. Build frontend assets with `npm ci`, `npm run test`, and `npm run build`.
3. Verify `.dockerignore` includes every service boot jar needed by
   root-context Dockerfiles.
4. Validate each service Compose fragment with `docker compose ... config`.
5. Validate the combined root stack.
6. Build images.
7. Start the root stack.
8. Check service health through host ports.
9. Open the GUI.
10. Verify same-origin `/api/health` through the GUI origin.
11. Exercise a public workflow through the GUI.
12. Collect logs and command results.
13. Tear down the stack.

## Runtime Evidence Rules

The deployment runbook must distinguish:

- Compose model validation;
- image build evidence;
- container startup evidence;
- health probe evidence;
- GUI/browser manual evidence;
- discovered runtime defects;
- skipped checks and reasons.

Do not convert skipped or failed runtime checks into readiness claims.
