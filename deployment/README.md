# Deployment

## Status

Deployment root for the target microservices ecosystem.

## Root Classification

`deployment/` is the deployment artifact root. It is intentionally outside
`docs/arc42/` because it contains Docker Compose, Swarm, Kubernetes and
observability descriptors or descriptor documentation. arc42 may summarize
deployment views and readiness constraints, but deployment artifacts remain
here and require DevOps/runtime verification before readiness is claimed.

`docker-compose/forensic-analytics.local.yml` is the root local Compose entry
point for the deployment workflow. It owns the shared external Docker network
named `forensic_analytics` and is combined with service-specific Compose
fragments as those fragments are implemented and verified.

Create the network before validating or running the combined stack:

```bash
docker network create forensic_analytics
```

`docker-compose/repository-to-btm.local.yml` remains the first verified
transitional runtime descriptor for the repository-to-BTM path. It covers:

- `forensic-gateway-service`
- `analysis-store-service`
- `repository-analysis-service`
- `repository-source-service`
- `java-ast-analysis-service`
- `joern-cpg-analysis-service`
- `btm-generation-service`

All local Compose descriptors assume the relevant service `bootJar` tasks have
already produced the jar files copied by the service Dockerfiles. A descriptor
does not by itself prove image-build, container-startup, health-check, or GUI
interaction readiness; record those results only after executing the matching
commands.

Docker Swarm and Kubernetes remain planning roots. No Swarm or Kubernetes
runtime readiness is claimed until stack files or manifests, health/readiness
probes and validation commands exist.

`observability/` contains S10 policy material for service diagnostics,
redaction, correlation fields, diagnostic exposure and future deployment
observability configuration. It does not start external telemetry services or
claim runtime readiness.
