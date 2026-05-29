# Graph Replay Service

## Status

Planned service root. No implementation exists yet.

Slice 16 explicitly defers this service from repository-to-BTM pipeline
acceptance. The current accepted BTM path ends at generated BTM artifact
delivery through predecessor Gateway/Analysis Store integration and BTM
Generation owner APIs. Graph and replay views remain future projection
behavior.

This service will own graph and replay projections. Those projections are
rebuildable views and must not become the canonical source of forensic
evidence.

Before implementation, a future slice must define replay/graph contracts,
target owner-query access, deterministic missing-evidence behavior, projection
rebuild rules, storage ownership and tests proving that projections never
become source of truth.

## Deployment Readiness

`deployment/docker-compose/services/graph-replay-service.compose.yml` is a
profile-gated deployment marker for the planned service root. It verifies local
deployment documentation references without starting a runnable graph or replay
service. It does not define a Dockerfile, publish ports, claim a health
endpoint, create graph data or treat replay projections as canonical forensic
evidence.
