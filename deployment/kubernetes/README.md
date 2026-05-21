# Kubernetes

## Status

Planned Kubernetes root.

No Kubernetes manifests exist yet. Kubernetes readiness must not be claimed
before service container, configuration, readiness and liveness evidence exists
for the participating services.

Verified local Docker Compose evidence exists for the repository-to-BTM path in
`deployment/docker-compose/repository-to-btm.local.yml`. That evidence does not
claim Kubernetes runtime readiness. A later Kubernetes slice must add manifests
or charts, service accounts, config maps, network policy, resource limits,
readiness/liveness probes, image policy and validation commands before this
directory can be called deployable.

The handoff for that later workflow is recorded in
`docs/workflow/deployment-workflow-request.md`. This directory must remain a
planning root until that separate workflow adds and verifies Kubernetes
artifacts.
