# Kubernetes

## Status

Planned Kubernetes root.

No Kubernetes manifests exist yet. Kubernetes readiness must not be claimed
before service container, configuration, readiness and liveness evidence exists
for the participating services.

The repository contains a local Docker Compose descriptor for the
repository-to-BTM path in
`deployment/docker-compose/repository-to-btm.local.yml`. That descriptor does
not claim Kubernetes runtime readiness. A later Kubernetes slice must add
manifests or charts, service accounts, config maps, network policy, resource
limits, readiness/liveness probes, image policy and validation commands before
this directory can be called deployable.

No Kubernetes workflow handoff artifact is present in this repository at S15
closure. This directory must remain a planning root until a separate workflow
adds and verifies Kubernetes artifacts.
