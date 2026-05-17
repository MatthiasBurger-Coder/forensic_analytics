# Kubernetes

## Status

Planned Kubernetes root.

No Kubernetes manifests exist yet. Kubernetes readiness must not be claimed
before service container, configuration, readiness and liveness evidence exists
for the participating services.

Verified service-local Docker evidence currently exists for
`forensic-ingestion-service`, `analysis-store-service` and
`repository-analysis-service`. This does not claim Kubernetes runtime readiness.
