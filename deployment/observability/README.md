# Observability Deployment Material

## Status

Deployment-oriented observability root for FA-MSA-001.

S12 adds policy material only. It does not add Prometheus, Grafana,
OpenTelemetry collectors, log shipping, external network services or live
credentials to the default local runtime.

## Material

- `service-diagnostics-policy.yaml` defines allowed diagnostic fields,
  forbidden sensitive values and the rule that operational logs remain
  diagnostics rather than verified forensic evidence.

Future runtime descriptors must reference this policy and verify their own
configuration before claiming Docker Compose, Swarm or Kubernetes observability
readiness.
