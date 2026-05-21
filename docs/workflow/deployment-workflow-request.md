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
- Read `deployment/docker-compose/README.md` for verified local Compose
  commands and their limits.
- Read `docs/arc42/07-deployment-view.md`.
- Read `docs/adr/ADR-0017-target-microservices-service-landscape.md`.
- Read `docs/adr/ADR-0018-initial-logical-contracts.md`.
- Read service READMEs and Dockerfiles under the implemented service roots.
- Verify health endpoints and ports for all participating services.
- Verify `QUALITY.md` and deployment-specific optional checks.

## Future Workflow Target Scope

The future workflow should start with the repository-to-BTM service landscape
already covered by the local Compose descriptor:

- `forensic-gateway-service`
- `analysis-store-service`
- `repository-analysis-service`
- `java-ast-analysis-service`
- `joern-cpg-analysis-service`
- `btm-generation-service`

Before adding Swarm stack files or Kubernetes manifests, the future workflow
must verify for each participating service:

- service-owned Docker image source and build command;
- exposed ports and public or private network role;
- health, readiness and liveness behavior;
- required configuration, secrets and mounted volumes;
- resource requests or limits appropriate for the target orchestrator;
- rollback or cleanup command;
- validation command for rendered deployment configuration.

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

## Future Workflow Stop Conditions

The future deployment workflow must stop instead of adding deployment artifacts
when:

- any service image source or owner cannot be verified;
- a service health, readiness or liveness endpoint is missing or unclear;
- required secrets, volumes, networks or resource policies are unspecified;
- Swarm or Kubernetes validation commands cannot be executed locally;
- a manifest would imply production readiness that has not been tested.
