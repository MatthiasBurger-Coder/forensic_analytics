# Deployment

## Status

Deployment root for the target microservices ecosystem.

`docker-compose/repository-to-btm.local.yml` is the first verified local
runtime descriptor for the repository-to-BTM path. It covers only:

- `forensic-gateway-service`
- `analysis-store-service`
- `repository-analysis-service`
- `java-ast-analysis-service`
- `joern-cpg-analysis-service`
- `btm-generation-service`

The descriptor assumes the service `bootJar` tasks have already produced the
jar files copied by the service Dockerfiles.

Docker Swarm and Kubernetes remain planning roots. No Swarm or Kubernetes
runtime readiness is claimed until stack files or manifests, health/readiness
probes and validation commands exist.

`observability/` contains S10 policy material for service diagnostics,
redaction, correlation fields, diagnostic exposure and future deployment
observability configuration. It does not start external telemetry services or
claim runtime readiness.
