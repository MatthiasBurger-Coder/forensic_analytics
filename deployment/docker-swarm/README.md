# Docker Swarm

## Status

Planned Docker Swarm root.

No Swarm stack exists yet. Swarm readiness must not be claimed before service
container, network, volume and healthcheck evidence exists for the
participating services.

Verified local Docker Compose evidence exists for the repository-to-BTM path in
`deployment/docker-compose/repository-to-btm.local.yml`. That evidence does not
claim Swarm runtime readiness. A later Swarm slice must add an explicit stack
file, service placement/resource rules, secrets/config handling, healthcheck
validation and rollback commands before this directory can be called deployable.
