# Graph Replay Service

## Status

Planned service root. No implementation exists yet.

Slice 16 explicitly defers this service from repository-to-BTM pipeline
acceptance. The current accepted BTM path ends at generated BTM artifact
delivery through Gateway/Analysis Store/BTM Generation owner APIs. Graph and
replay views remain future projection behavior.

This service will own graph and replay projections. Those projections are
rebuildable views and must not become the canonical source of forensic
evidence.

Before implementation, a future slice must define replay/graph contracts,
Analysis Store owner-query access, deterministic missing-evidence behavior,
projection rebuild rules, storage ownership and tests proving that projections
never become source of truth.
