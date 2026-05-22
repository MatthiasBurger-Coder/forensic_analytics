# Observability Stack

## Status

Slice S12 target boundary for deployment-oriented observability material.

`observability-stack` is not a productive backend service and not a shared Java
runtime module. It owns deployable and operator-facing material for logs,
metrics, tracing and dashboards as those artifacts are introduced.

## Scope

Owns:

- service diagnostics policy material;
- redaction and allowed-field rules for operational logs;
- deployment observability documentation;
- future OpenTelemetry, Prometheus or Grafana configuration when verified.

Non-scope:

- shared Java logging APIs;
- shared Java DTOs or generated classes;
- domain, application, parser, Joern, persistence or reporting behavior;
- storing operational logs as verified forensic evidence.

## Current S12 Material

- `deployment/observability/service-diagnostics-policy.yaml`

The policy records allowed diagnostic fields and forbidden values for future
deployment configuration. It does not start external telemetry services, expose
network ports, collect runtime traces or claim Docker Compose, Swarm or
Kubernetes readiness.

## Verification

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
git diff --check
```
