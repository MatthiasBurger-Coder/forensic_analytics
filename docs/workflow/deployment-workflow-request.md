# Separate Deployment Workflow Request

## Purpose

This file records the requested separate Docker Swarm and Kubernetes workflow.
It is a handoff package, not an active deployment workflow and not deployment
readiness evidence.

## Proposed Future Command

```text
workflow create
```

Proposed request:

```text
Create a separate Docker Swarm and Kubernetes deployment workflow for the
repository-to-BTM service landscape. Verify service-owned Dockerfiles,
health checks, resource policies, configuration, image ownership, secrets
handling, rollback commands and validation commands before adding stack files
or manifests. Keep Swarm and Kubernetes readiness separate from the existing
local Docker Compose evidence.
```

## Required Baseline For The Future Workflow

- Read `deployment/docker-compose/repository-to-btm.local.yml`.
- Read `deployment/docker-swarm/README.md`.
- Read `deployment/kubernetes/README.md`.
- Read service READMEs and Dockerfiles under the implemented service roots.
- Verify health endpoints and ports for all participating services.
- Verify `QUALITY.md` and deployment-specific optional checks.

## Forbidden In The Current Workflow

- Do not add Swarm stack files.
- Do not add Kubernetes manifests or charts.
- Do not claim deployment readiness.
- Do not add secrets or credentials.
- Do not make Docker, Swarm or Kubernetes required for the default Gradle gate.

## Future Workflow Starting Risks

- Some target services remain README-only planned roots.
- Service-private durable stores are not implemented.
- Image publication and registry policy are not defined.
- Swarm and Kubernetes validation tooling is not yet verified.
- External base image pulls may require network access.
